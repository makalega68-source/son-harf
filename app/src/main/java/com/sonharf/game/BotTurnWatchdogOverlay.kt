package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Recovery-only safety net for a bot turn.
 *
 * RefinedDuelOverlay remains the normal bot-move owner. This watchdog only retries a
 * bot turn that survives a transient RPC/network failure. It watches the one active
 * room supplied by LiveDuelRuntimeShell instead of repeatedly scanning game_rooms.
 * LaunchedEffect(roomId) is automatically cancelled when the match leaves composition
 * or the active room changes.
 */
@Composable
internal fun BotTurnWatchdogOverlay(roomId: String) {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }

    LaunchedEffect(roomId) {
        while (true) {
            val me = backend.currentUserId()
            val candidate = if (me == null) {
                null
            } else {
                withTimeoutOrNull(4_000L) {
                    runCatching { backend.getRoom(roomId) }
                        .getOrNull()
                        ?.takeIf {
                            it.hostId == me &&
                                it.isBot &&
                                it.botTurn &&
                                it.status in setOf("playing", "final", "sudden_death")
                        }
                }
            }

            if (candidate != null) {
                val moved = withTimeoutOrNull(6_000L) {
                    runCatching { backend.botTakeTurn(candidate.id) }.getOrNull()
                }
                val stillThinking = moved?.let {
                    it.botTurn && it.status in setOf("playing", "final", "sudden_death")
                } ?: true
                delay(if (stillThinking) 1_200L else 700L)
            } else {
                // RefinedDuelOverlay is already polling the room; this watchdog can stay quiet.
                delay(1_500L)
            }
        }
    }
}
