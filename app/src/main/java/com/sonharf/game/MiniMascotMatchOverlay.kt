package com.sonharf.game

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

/**
 * Event-driven controller for the free Mini mascot.
 * It never mutates match state. It only observes the live room and reacts visually.
 */
@Composable
internal fun MiniMascotMatchOverlay() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    val me = backend.currentUserId() ?: return
    var mood by remember { mutableStateOf(MiniMood.IDLE) }
    var activeRoomId by remember { mutableStateOf<String?>(null) }
    var lastSeenWordCount by remember { mutableIntStateOf(-1) }
    var acceptedByMe by remember { mutableIntStateOf(0) }
    var moodToken by remember { mutableIntStateOf(0) }

    fun setMood(next: MiniMood) {
        mood = next
        moodToken += 1
    }

    LaunchedEffect(Unit) {
        var idleTicks = 0
        while (true) {
            val room = runCatching {
                SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
                    .filter {
                        (it.hostId == me || it.guestId == me) &&
                            it.status in setOf("playing", "quiz", "final", "sudden_death", "paused")
                    }
                    .maxByOrNull { it.validWordCount }
            }.getOrNull()

            if (room == null) {
                activeRoomId = null
                lastSeenWordCount = -1
                acceptedByMe = 0
                mood = MiniMood.IDLE
            } else {
                if (activeRoomId != room.id) {
                    activeRoomId = room.id
                    lastSeenWordCount = room.validWordCount
                    acceptedByMe = 0
                    setMood(MiniMood.CUTE)
                } else {
                    val rejected = room.lastEventPlayerId == me && room.lastEvent in setOf(
                        "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired"
                    )
                    if (rejected) {
                        setMood(MiniMood.SAD)
                    } else if (room.validWordCount > lastSeenWordCount) {
                        val newest = runCatching { backend.getWords(room.id).lastOrNull() }.getOrNull()
                        if (newest?.playerId == me) {
                            acceptedByMe += 1
                            setMood(if (acceptedByMe >= 3 && acceptedByMe % 3 == 0) MiniMood.STREAK else MiniMood.HAPPY)
                        } else if (newest != null) {
                            setMood(MiniMood.COLLECT)
                        }
                    } else {
                        idleTicks += 1
                        if (idleTicks >= 22) {
                            idleTicks = 0
                            setMood(MiniMood.CUTE)
                        }
                    }
                    lastSeenWordCount = room.validWordCount
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(moodToken) {
        if (mood == MiniMood.IDLE) return@LaunchedEffect
        val token = moodToken
        delay(if (mood == MiniMood.STREAK) 2300 else 1800)
        if (token == moodToken) mood = MiniMood.IDLE
    }

    val travelX by animateDpAsState(
        targetValue = when (mood) {
            MiniMood.HAPPY -> 118.dp
            MiniMood.STREAK -> 145.dp
            MiniMood.COLLECT -> 78.dp
            else -> 8.dp
        },
        animationSpec = tween(520),
        label = "miniTravelX",
    )
    val travelY by animateDpAsState(
        targetValue = when (mood) {
            MiniMood.SAD -> 48.dp
            MiniMood.CUTE -> (-18).dp
            MiniMood.STREAK -> (-34).dp
            else -> 12.dp
        },
        animationSpec = tween(420),
        label = "miniTravelY",
    )

    Box(Modifier.fillMaxSize()) {
        MiniMascot3D(
            mood = mood,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = travelX, y = travelY)
                .size(116.dp),
        )
    }
}
