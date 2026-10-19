package com.sonharf.game

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

/**
 * Whether this player holds the throne. The server already marks the week's owner with the
 * throne frame and its expiry; the same answer turns the whole gold package on for that week.
 */
internal object ThroneChampion {
    private const val PREFS = "throne_champion"
    private const val KEY_UNTIL = "until"

    fun restore(context: Context) {
        SonHarfCosmetics.throneUntil = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_UNTIL, 0L)
    }

    suspend fun refresh(context: Context) {
        if (!SupabaseProvider.configured) return
        val me = runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.id }.getOrNull() ?: return
        PublicFrames.invalidate(me)
        val frame = runCatching { PublicFrames.get(me) }.getOrElse { return }
        val now = System.currentTimeMillis()
        // Without an expiry the next check (every few minutes) decides again.
        val until = if (frame == ProfileFrameCollection.throneFrame.id) PublicFrames.rewardDeadline(me) ?: (now + 10 * 60_000L) else 0L
        SonHarfCosmetics.throneUntil = until
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_UNTIL, until).apply()
    }
}

/** Keeps the throne owner's gold rewards in step with the server while the app is open. */
@Composable
internal fun ThroneChampionWatcher() {
    val context = LocalContext.current
    val foreground = rememberAppForeground()
    LaunchedEffect(foreground) {
        if (!foreground) return@LaunchedEffect
        while (true) {
            ThroneChampion.refresh(context)
            delay(5 * 60_000L)
        }
    }
}
