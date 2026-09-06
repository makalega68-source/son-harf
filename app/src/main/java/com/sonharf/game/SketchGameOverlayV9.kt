package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

/**
 * Mount the refined duel arena only while the word-duel surface owns the match.
 * Quiz rounds are intentionally excluded: BilBakalimBonusOverlay is the single UI
 * owner for the server-synchronised quiz flow and must remain unobstructed.
 */
@Composable
fun SketchGameOverlayV9() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    var active by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            active = if (me == null) false else runCatching {
                SupabaseProvider.client.from("game_rooms")
                    .select()
                    .decodeList<GameRoomDto>()
                    .any {
                        (it.hostId == me || it.guestId == me) &&
                            it.status in setOf("playing", "final", "sudden_death", "paused")
                    }
            }.getOrDefault(false)
            delay(500)
        }
    }

    if (active) {
        Box(Modifier.fillMaxSize()) {
            RefinedDuelOverlay()
            BotTurnWatchdogOverlay()
        }
    }
}
