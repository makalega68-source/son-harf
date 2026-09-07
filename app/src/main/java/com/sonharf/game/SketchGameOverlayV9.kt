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
import java.time.Instant

/**
 * Legacy compatibility mount for the refined duel arena.
 *
 * The active V1 route is owned by LiveDuelRuntimeShell. This source remains buildable
 * without restoring the old broad bot-watchdog scan: when it is ever composed, it
 * resolves the concrete active room and passes that room id to the recovery watchdog.
 */
@Composable
fun SketchGameOverlayV9() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    var activeRoomId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            activeRoomId = if (me == null) {
                null
            } else {
                runCatching {
                    SupabaseProvider.client.from("game_rooms")
                        .select()
                        .decodeList<GameRoomDto>()
                        .asSequence()
                        .filter {
                            (it.hostId == me || it.guestId == me) &&
                                it.status in setOf("playing", "final", "sudden_death", "paused")
                        }
                        .maxByOrNull {
                            runCatching { Instant.parse(it.createdAt) }.getOrDefault(Instant.EPOCH)
                        }
                        ?.id
                }.getOrNull()
            }
            delay(if (activeRoomId == null) 2_500L else 1_500L)
        }
    }

    activeRoomId?.let { roomId ->
        Box(Modifier.fillMaxSize()) {
            RefinedDuelOverlay()
            BotTurnWatchdogOverlay(roomId = roomId)
        }
    }
}
