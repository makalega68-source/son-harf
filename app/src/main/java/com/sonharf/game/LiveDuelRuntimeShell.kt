package com.sonharf.game

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Keeps the verified V1 shell/lobby intact, but guarantees that an active
 * classic duel is rendered by RefinedDuelOverlay rather than the legacy
 * LightDuelArena surface.
 *
 * This wrapper intentionally owns no game mutations. It only detects whether
 * the authenticated player has a live room and switches the visible runtime
 * surface. Matchmaking, scoring and server authority remain in the existing
 * backend.
 *
 * Mage Cat is rendered here as a resilient active-shell layer so the purchased
 * mascot cannot silently disappear when legacy home implementations are not on
 * the shipped navigation path. The mascot is non-interactive and does not own
 * gameplay state.
 */
@Composable
internal fun LiveDuelRuntimeShell(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var activeRoomId by remember { mutableStateOf<String?>(null) }
    var finishedHoldStartedAt by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            val currentRoomId = activeRoomId
            activeRoomId = if (me == null || !SupabaseProvider.configured) {
                finishedHoldStartedAt = null
                null
            } else if (currentRoomId != null) {
                val snapshot = runCatching { backend.getRoom(currentRoomId) }.getOrNull()
                when {
                    snapshot == null || (snapshot.hostId != me && snapshot.guestId != me) -> {
                        finishedHoldStartedAt = null
                        null
                    }
                    snapshot.status in setOf("playing", "final", "sudden_death", "paused") -> {
                        finishedHoldStartedAt = null
                        snapshot.id
                    }
                    snapshot.status == "finished" -> {
                        val now = SystemClock.elapsedRealtime()
                        val started = finishedHoldStartedAt ?: now.also { finishedHoldStartedAt = it }
                        if (now - started < 3_600L) snapshot.id else {
                            finishedHoldStartedAt = null
                            null
                        }
                    }
                    else -> {
                        finishedHoldStartedAt = null
                        null
                    }
                }
            } else {
                finishedHoldStartedAt = null
                runCatching {
                    SupabaseProvider.client
                        .from("game_rooms")
                        .select()
                        .decodeList<GameRoomDto>()
                        .asSequence()
                        .filter {
                            (it.hostId == me || it.guestId == me) &&
                                it.status in setOf("playing", "final", "sudden_death", "paused") &&
                                (it.isBot || it.guestId != null)
                        }
                        .maxByOrNull {
                            runCatching { Instant.parse(it.createdAt) }.getOrDefault(Instant.EPOCH)
                        }
                        ?.id
                }.getOrNull()
            }
            // The active overlay owns the live match refresh. Polling the whole room list
            // several times per second here was competing with word submissions and made
            // navigation feel unstable on slower phones.
            delay(if (activeRoomId == null) 1_250L else 700L)
        }
    }

    val roomId = activeRoomId
    if (roomId != null) {
        Box {
            RefinedDuelOverlay()
            MageCatLiveDuelOverlay(roomId)
        }
        // Continuous recovery for transient bot RPC/network failures. The watchdog is
        // intentionally UI-less; RefinedDuelOverlay remains the only visible duel surface.
        BotTurnWatchdogOverlay()
    } else {
        Box {
            MonsterExperienceApp(onSignedOut = onSignedOut)
            MageCatHomeMascot(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 74.dp)
                    .size(MageCatHomeDefaultSize),
            )
        }
    }
}
