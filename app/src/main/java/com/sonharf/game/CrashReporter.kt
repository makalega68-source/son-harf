package com.sonharf.game

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * A crash is written to a small file the moment it happens (no network on a dying thread); the
 * next launch sends it to the server's error log, where the admin panel lists it.
 */
internal object CrashReporter {
    private const val FILE = "pending_crash.txt"
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val dir = context.applicationContext.filesDir
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(dir, FILE).writeText(
                    System.currentTimeMillis().toString() + "\n" +
                        (error.javaClass.name + ": " + error.message.orEmpty()).take(1_500) + "\n" +
                        "thread=" + thread.name + "\n" + Log.getStackTraceString(error).take(12_000),
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    suspend fun uploadPending(context: Context): Unit = withContext(Dispatchers.IO) {
        if (SupabaseProvider.configured) runCatching { uploadExitReasons(context) }
        val file = File(context.applicationContext.filesDir, FILE)
        if (!file.isFile || !SupabaseProvider.configured) return@withContext
        val lines = runCatching { file.readText() }.getOrNull()?.lines() ?: return@withContext
        val at = lines.getOrNull(0)?.toLongOrNull()?.let { java.time.Instant.ofEpochMilli(it).toString() }
        runCatching {
            SupabaseProvider.client.postgrest.rpc("report_client_error_v1", buildJsonObject {
                put("p_kind", "crash")
                put("p_message", lines.getOrNull(1).orEmpty())
                put("p_stack", lines.drop(2).joinToString("\n"))
                put("p_app_version", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")")
                put("p_device", Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE)
                put("p_occurred_at", at)
            })
        }.onSuccess { file.delete() }
        Unit
    }
}

private const val EXIT_PREFS = "crash_reporter"
private const val EXIT_SEEN = "exit_seen_at"

/**
 * Freezes (ANR), native crashes and low-memory kills never reach the uncaught-exception handler:
 * Android 11+ keeps them as process exit records, which are sent once on the next launch.
 */
private suspend fun uploadExitReasons(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val app = context.applicationContext
    val prefs = app.getSharedPreferences(EXIT_PREFS, Context.MODE_PRIVATE)
    // First run with this reporter: the last day's exits are still worth seeing (the hint crash).
    val seen = prefs.getLong(EXIT_SEEN, System.currentTimeMillis() - 86_400_000L)
    val manager = app.getSystemService(android.app.ActivityManager::class.java) ?: return
    val reasons = manager.getHistoricalProcessExitReasons(app.packageName, 0, 5)
        .filter { it.timestamp > seen }
    if (reasons.isEmpty()) return
    val reported = setOf(
        android.app.ApplicationExitInfo.REASON_ANR,
        android.app.ApplicationExitInfo.REASON_CRASH_NATIVE,
        android.app.ApplicationExitInfo.REASON_LOW_MEMORY,
        android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        android.app.ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
    )
    for (info in reasons.sortedBy { it.timestamp }) {
        if (info.reason !in reported) continue
        val kind = when (info.reason) {
            android.app.ApplicationExitInfo.REASON_ANR -> "anr"
            android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "native_crash"
            android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> "low_memory"
            else -> "exit_${info.reason}"
        }
        // An ANR trace names the stuck code; keep the main thread's part, it is what froze.
        val trace = runCatching {
            info.traceInputStream?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty().let { text ->
            val main = text.indexOf("\"main\"")
            if (main >= 0) text.substring(main).take(12_000) else text.take(12_000)
        }
        SupabaseProvider.client.postgrest.rpc("report_client_error_v1", buildJsonObject {
            put("p_kind", kind)
            put("p_message", (info.description ?: kind).take(1_500) + " · pss=" + info.pss / 1024 + "MB")
            put("p_stack", trace)
            put("p_app_version", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")")
            put("p_device", Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE)
            put("p_occurred_at", java.time.Instant.ofEpochMilli(info.timestamp).toString())
        })
    }
    prefs.edit().putLong(EXIT_SEEN, reasons.maxOf { it.timestamp }).apply()
}

/** Sends a crash left over from the last run, once signed in. */
@Composable
internal fun CrashReportUploader() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CrashReporter.uploadPending(context) }
}
