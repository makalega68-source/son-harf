package com.sonharf.game

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

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            val currentRoomId = activeRoomId
            activeRoomId = if (me == null || !SupabaseProvider.configured) {
                null
            } else if (currentRoomId != null) {
                runCatching { backend.getRoom(currentRoomId) }
                    .getOrNull()
                    ?.takeIf {
                        (it.hostId == me || it.guestId == me) &&
                            it.status in setOf("playing", "final", "sudden_death", "paused")
                    }
                    ?.id
            } else {
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
            delay(if (activeRoomId == null) 1_250L else 2_000L)
        }
    }

    if (activeRoomId != null) {
        Box {
            RefinedDuelOverlay()
            MageCatMatchMascot(
                cue = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 118.dp, end = 14.dp)
                    .size(MageCatMatchDefaultSize),
            )
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
