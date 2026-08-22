package com.sonharf.game

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

/**
 * Single top-level home/login/match host for Mini.
 * It observes game state but never mutates match data.
 */
@Composable
internal fun MiniMascotGlobalOverlay() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var activeRoom by remember { mutableStateOf<GameRoomDto?>(null) }
    var mood by remember { mutableStateOf(MiniMood.CUTE) }
    var lastWordCount by remember { mutableIntStateOf(-1) }
    var myAccepted by remember { mutableIntStateOf(0) }
    var reactionToken by remember { mutableIntStateOf(0) }

    fun react(next: MiniMood) {
        mood = next
        reactionToken += 1
    }

    LaunchedEffect(Unit) {
        var idleTicks = 0
        while (true) {
            val uid = backend?.currentUserId()
            val room = if (uid == null || !SupabaseProvider.configured) null else runCatching {
                SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
                    .filter {
                        (it.hostId == uid || it.guestId == uid) &&
                            it.status in setOf("playing", "quiz", "final", "sudden_death", "paused")
                    }
                    .maxByOrNull { it.validWordCount }
            }.getOrNull()

            val previousRoomId = activeRoom?.id
            activeRoom = room

            if (room == null) {
                lastWordCount = -1
                myAccepted = 0
                idleTicks += 1
                if (idleTicks == 1 || idleTicks >= 16) {
                    idleTicks = 0
                    react(MiniMood.CUTE)
                }
            } else {
                idleTicks = 0
                if (previousRoomId != room.id || lastWordCount < 0) {
                    lastWordCount = room.validWordCount
                    myAccepted = 0
                    react(MiniMood.CUTE)
                } else {
                    val rejected = room.lastEventPlayerId == uid && room.lastEvent in setOf(
                        "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired"
                    )
                    when {
                        rejected -> react(MiniMood.SAD)
                        room.validWordCount > lastWordCount -> {
                            val newest = runCatching { backend.getWords(room.id).lastOrNull() }.getOrNull()
                            if (newest?.playerId == uid) {
                                myAccepted += 1
                                react(if (myAccepted >= 3 && myAccepted % 3 == 0) MiniMood.STREAK else MiniMood.HAPPY)
                            } else if (newest != null) {
                                react(MiniMood.COLLECT)
                            }
                        }
                    }
                    lastWordCount = room.validWordCount
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(reactionToken) {
        if (mood == MiniMood.IDLE) return@LaunchedEffect
        val token = reactionToken
        delay(if (mood == MiniMood.STREAK) 2400 else 1900)
        if (token == reactionToken) mood = MiniMood.IDLE
    }

    val inMatch = activeRoom != null
    val x by animateDpAsState(
        targetValue = when {
            !inMatch -> (-12).dp
            mood == MiniMood.HAPPY -> 88.dp
            mood == MiniMood.STREAK -> 116.dp
            mood == MiniMood.COLLECT -> 54.dp
            else -> 10.dp
        },
        animationSpec = tween(520),
        label = "miniGlobalX",
    )
    val y by animateDpAsState(
        targetValue = when {
            !inMatch -> (-88).dp
            mood == MiniMood.SAD -> 12.dp
            mood == MiniMood.CUTE -> (-68).dp
            mood == MiniMood.STREAK -> (-92).dp
            else -> (-52).dp
        },
        animationSpec = tween(460),
        label = "miniGlobalY",
    )

    Box(Modifier.fillMaxSize()) {
        val alignment = if (inMatch) Alignment.CenterStart else Alignment.CenterEnd
        val size = if (inMatch) 132.dp else 156.dp
        Surface(
            modifier = Modifier
                .align(alignment)
                .offset(x = x, y = y)
                .size(size),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = if (inMatch) .12f else .70f),
            border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = if (inMatch) .18f else .38f)),
            shadowElevation = if (inMatch) 0.dp else 3.dp,
        ) {
            Box(Modifier.fillMaxSize().background(Color.Transparent).padding(4.dp)) {
                MiniMascot3D(mood = mood, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
