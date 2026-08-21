package com.sonharf.game

import androidx.compose.runtime.*
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

/**
 * Mount the mature V10 arena only while a match is actually active.
 * Finished games are intentionally left to ComboOverlayV9, which owns the single
 * persistent result/share/challenge summary and remembers dismissals across launches.
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
                            it.status in setOf("playing", "quiz", "final", "sudden_death", "paused")
                    }
            }.getOrDefault(false)
            delay(500)
        }
    }

    if (active) SketchGameOverlayV10()
}
