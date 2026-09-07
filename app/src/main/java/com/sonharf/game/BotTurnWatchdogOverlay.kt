package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Safety net for bot turns.
 *
 * RefinedDuelOverlay starts the normal bot move. This watchdog continuously reconciles
 * a bot turn that survives because of a transient network/RPC failure. bot_take_turn is
 * server-authoritative and idempotent for an already-completed turn, so concurrent
 * recovery calls cannot award a second bot move.
 *
 * This composable is intentionally UI-less: RefinedDuelOverlay remains the single visual
 * owner of the duel screen while this loop only provides recovery semantics.
 */
@Composable
internal fun BotTurnWatchdogOverlay() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            val candidate = if (me == null) {
                null
            } else {
                withTimeoutOrNull(4_000L) {
                    runCatching {
                        SupabaseProvider.client.from("game_rooms").select {
                            filter {
                                eq("host_id", me)
                                eq("is_bot", true)
                                eq("bot_turn", true)
                            }
                        }.decodeList<GameRoomDto>()
                            .filter { it.status in setOf("playing", "final", "sudden_death") }
                            .maxWithOrNull(compareBy<GameRoomDto> { it.roundNo }.thenBy { it.validWordCount })
                    }.getOrNull()
                }
            }

            if (candidate != null) {
                val moved = withTimeoutOrNull(6_000L) {
                    runCatching { backend.botTakeTurn(candidate.id) }.getOrNull()
                }
                val stillThinking = moved?.let {
                    it.botTurn && it.status in setOf("playing", "final", "sudden_death")
                } ?: true
                delay(if (stillThinking) 900L else 300L)
            } else {
                delay(700L)
            }
        }
    }
}
