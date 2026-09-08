package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import java.time.Instant

private val activeClassicDuelStatuses = setOf("playing", "final", "sudden_death", "paused")

internal fun resolveTrackedDuelRoomId(
    currentRoomId: String,
    currentUserId: String?,
    roomResult: Result<GameRoomDto>,
): String? {
    if (currentUserId == null) return currentRoomId
    return roomResult.fold(
        onSuccess = { currentRoom ->
            currentRoom
                .takeIf {
                    (it.hostId == currentUserId || it.guestId == currentUserId) &&
                        it.status in activeClassicDuelStatuses
                }
                ?.id
        },
        onFailure = {
            // A failed request contains no authoritative room-state information.
            currentRoomId
        },
    )
}

/**
 * Keeps the verified V1 shell/lobby intact, but guarantees that an active
 * classic duel is rendered by RefinedDuelOverlay rather than the legacy
 * LightDuelArena surface.
 *
 * This wrapper intentionally owns no game mutations. It only detects whether
 * the authenticated player has a live room and switches the visible runtime
 * surface. Matchmaking, scoring and server authority remain in the existing
 * backend.
 */
@Composable
internal fun LiveDuelRuntimeShell(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var activeRoomId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            val currentRoomId = activeRoomId
            activeRoomId = when {
                !SupabaseProvider.configured -> null
                currentRoomId != null -> {
                    if (me == null) {
                        // A transient auth/session refresh must not tear down a running duel.
                        // Explicit sign-out disposes this shell through StableV1App.
                        currentRoomId
                    } else {
                        resolveTrackedDuelRoomId(
                            currentRoomId = currentRoomId,
                            currentUserId = me,
                            roomResult = runCatching { backend.getRoom(currentRoomId) },
                        )
                    }
                }
                me == null -> null
                else -> {
                    runCatching {
                        SupabaseProvider.client
                            .from("game_rooms")
                            .select()
                            .decodeList<GameRoomDto>()
                            .asSequence()
                            .filter {
                                (it.hostId == me || it.guestId == me) &&
                                    it.status in activeClassicDuelStatuses &&
                                    (it.isBot || it.guestId != null)
                            }
                            .maxByOrNull {
                                runCatching { Instant.parse(it.createdAt) }.getOrDefault(Instant.EPOCH)
                            }
                            ?.id
                    }.getOrNull()
                }
            }
            // The active overlay owns the live match refresh. Polling the whole room list
            // several times per second here was competing with word submissions and made
            // navigation feel unstable on slower phones.
            delay(if (activeRoomId == null) 1_250L else 2_000L)
        }
    }

    if (activeRoomId != null) {
        RefinedDuelOverlay()
        // Continuous recovery for transient bot RPC/network failures. The watchdog is
        // intentionally UI-less; RefinedDuelOverlay remains the only visible duel surface.
        BotTurnWatchdogOverlay()
    } else {
        MonsterExperienceApp(onSignedOut = onSignedOut)
    }
}
