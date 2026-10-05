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

/** Sends a crash left over from the last run, once signed in. */
@Composable
internal fun CrashReportUploader() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CrashReporter.uploadPending(context) }
}
