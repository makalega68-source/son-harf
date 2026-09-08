package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import java.time.Instant

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
            delay(if (activeRoomId == null) 1_250L else 2_000L)
        }
    }

    val roomId = activeRoomId
    if (roomId != null) {
        Box(Modifier.fillMaxSize()) {
            RefinedDuelOverlay()
            MageCatLiveDuelOverlay(roomId)
            BotTurnWatchdogOverlay()
        }
    } else {
        MonsterExperienceApp(onSignedOut = onSignedOut)
    }
}
