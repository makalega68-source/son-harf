package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

/**
 * Safety net for bot turns.
 *
 * RefinedDuelOverlay already starts the normal bot move. This watchdog only reconciles
 * a bot turn that survives because of a transient network/RPC failure. bot_take_turn is
 * server-authoritative and idempotent for an already-completed turn, so concurrent
 * recovery calls cannot award a second bot move.
 */
@Composable
internal fun BotTurnWatchdogOverlay() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }
    var botThinking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            val candidate = if (me == null) null else runCatching {
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

            botThinking = candidate != null
            if (candidate != null) {
                val moved = runCatching { backend.botTakeTurn(candidate.id) }.getOrNull()
                if (moved != null) {
                    botThinking = moved.botTurn && moved.status in setOf("playing", "final", "sudden_death")
                }
                delay(if (botThinking) 900L else 300L)
            } else {
                delay(700L)
            }
        }
    }

    if (botThinking) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 36.dp)
                    .width(68.dp)
                    .height(56.dp),
                color = Color.White,
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, Color(0xFFD4DCE7)),
                shadowElevation = 1.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "BOT …",
                        color = Color(0xFF677386),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
