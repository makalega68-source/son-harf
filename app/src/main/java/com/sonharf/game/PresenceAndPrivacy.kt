package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import kotlinx.coroutines.delay
import java.time.Instant

internal fun ProfileDto.isRecentlyOnline(now: Long = System.currentTimeMillis()): Boolean =
    presenceIsFresh(presenceStatus, lastSeenAt, now)

internal fun presenceIsFresh(status: String, seen: String?, now: Long): Boolean {
    val stamp = runCatching { com.sonharf.game.data.requireServerInstant(seen).toEpochMilli() }.getOrNull() ?: return false
    return status in setOf("online", "in_game") && now - stamp in -30_000L..90_000L
}

internal object AppPresenceState { @Volatile var foreground = true }

@Composable
internal fun AppPresence(backend: OnlineGameBackend, playing: Boolean) {
    val foreground = rememberAppForeground()
    SideEffect { AppPresenceState.foreground = foreground }
    LaunchedEffect(foreground, playing) {
        if (!foreground) {
            gameRequestResult { backend.setPresence("offline") }
            return@LaunchedEffect
        }
        while (true) {
            gameRequestResult { backend.setPresence(if (playing) "in_game" else "online") }
            delay(30_000)
        }
    }
}

private tailrec fun Context.chatActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.chatActivity()
    else -> null
}

/** Ref-count windows so nested chat surfaces cannot clear each other's protection. */
private object SecureChatWindows {
    private data class Lease(var count: Int, val wasSecure: Boolean)
    private val leases = java.util.WeakHashMap<Window, Lease>()
    fun acquire(window: Window) {
        val lease = leases[window]
        if (lease != null) { lease.count++; return }
        leases[window] = Lease(1, window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    fun release(window: Window) {
        val lease = leases[window] ?: return
        if (--lease.count > 0) return
        leases.remove(window)
        if (!lease.wasSecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

@Composable
internal fun SecureChatContent() {
    val view = LocalView.current
    val activity = LocalContext.current.chatActivity()
    val dialog = (view.parent as? DialogWindowProvider)?.window
    DisposableEffect(activity, dialog) {
        val windows = listOfNotNull(activity?.window, dialog).distinct()
        windows.forEach(SecureChatWindows::acquire)
        onDispose { windows.forEach(SecureChatWindows::release) }
    }
}
