package com.sonharf.game

import androidx.compose.animation.core.FastOutSlowInEasing
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
import kotlin.random.Random

/**
 * Single global mascot host.
 * The mascot is rendered with our own OpenGL code so the same character appears everywhere.
 * Legacy in-arena mascot calls are disabled by default in MiniMascot3D; this host is the owner.
 */
@Composable
internal fun MiniMascotGlobalOverlay() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var activeRoom by remember { mutableStateOf<GameRoomDto?>(null) }
    var motion by remember { mutableStateOf(MascotMotion.CURIOUS) }
    var lastWordCount by remember { mutableIntStateOf(-1) }
    var myAccepted by remember { mutableIntStateOf(0) }
    var reactionToken by remember { mutableIntStateOf(0) }

    fun react(next: MascotMotion) {
        motion = next
        reactionToken += 1
    }

    LaunchedEffect(Unit) {
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
                if (motion == MascotMotion.OPPONENT) motion = MascotMotion.IDLE
            } else if (previousRoomId != room.id || lastWordCount < 0) {
                lastWordCount = room.validWordCount
                myAccepted = 0
                react(MascotMotion.CURIOUS)
            } else {
                val rejected = room.lastEventPlayerId == uid && room.lastEvent in setOf(
                    "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired"
                )
                when {
                    rejected -> react(MascotMotion.SAD)
                    room.validWordCount > lastWordCount -> {
                        val newest = backend?.let { b ->
                            runCatching { b.getWords(room.id).lastOrNull() }.getOrNull()
                        }
                        if (newest?.playerId == uid) {
                            myAccepted += 1
                            react(if (myAccepted >= 3 && myAccepted % 3 == 0) MascotMotion.STREAK else MascotMotion.HAPPY)
                        } else if (newest != null) {
                            react(MascotMotion.COLLECT)
                        }
                    }
                    room.status == "final" && room.lastEventPlayerId == uid -> react(MascotMotion.VICTORY)
                    else -> if (motion == MascotMotion.IDLE && room.lastEventPlayerId != uid) {
                        motion = MascotMotion.OPPONENT
                    }
                }
                lastWordCount = room.validWordCount
            }
            delay(500)
        }
    }

    LaunchedEffect(activeRoom?.id) {
        while (true) {
            delay(Random.nextLong(4200L, 7600L))
            if (motion == MascotMotion.IDLE || motion == MascotMotion.OPPONENT) {
                react(MascotMotion.CURIOUS)
            }
        }
    }

    LaunchedEffect(reactionToken) {
        val state = motion
        if (state.looping || state.holdMs <= 0L) return@LaunchedEffect
        val token = reactionToken
        delay(state.holdMs)
        if (token == reactionToken) {
            motion = if (activeRoom != null) MascotMotion.OPPONENT else MascotMotion.IDLE
        }
    }

    val inMatch = activeRoom != null
    val x by animateDpAsState(
        targetValue = when {
            !inMatch -> (-22).dp
            motion == MascotMotion.HAPPY -> (-20).dp
            motion == MascotMotion.STREAK -> (-34).dp
            motion == MascotMotion.COLLECT -> (-18).dp
            else -> (-16).dp
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "heroMascotX",
    )
    val y by animateDpAsState(
        targetValue = when {
            !inMatch -> (-82).dp
            motion == MascotMotion.SAD -> 28.dp
            motion == MascotMotion.STREAK -> (-18).dp
            motion == MascotMotion.HAPPY -> (-8).dp
            else -> 24.dp
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "heroMascotY",
    )

    val mood = when (motion) {
        MascotMotion.HAPPY -> MiniMood.HAPPY
        MascotMotion.SAD -> MiniMood.SAD
        MascotMotion.STREAK, MascotMotion.VICTORY -> MiniMood.STREAK
        MascotMotion.COLLECT -> MiniMood.COLLECT
        MascotMotion.CURIOUS -> MiniMood.CUTE
        MascotMotion.IDLE, MascotMotion.OPPONENT -> MiniMood.IDLE
    }

    Box(Modifier.fillMaxSize()) {
        MiniMascot3D(
            mood = mood,
            enabled = true,
            modifier = Modifier
                .align(if (inMatch) Alignment.CenterStart else Alignment.Center)
                .offset(x = x, y = y)
                .size(if (inMatch) 188.dp else 218.dp),
        )
    }
}
