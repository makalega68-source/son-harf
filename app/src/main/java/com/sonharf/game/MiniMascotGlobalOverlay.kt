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
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Global, non-gameplay-mutating mascot host.
 * The character is a rigged GLB rendered by Filament/SceneView, not a procedural drawing.
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

    // Organic idle variation: no clockwork blink/pose cadence.
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
            !inMatch -> (-10).dp
            motion == MascotMotion.HAPPY -> (-42).dp
            motion == MascotMotion.STREAK -> (-68).dp
            motion == MascotMotion.COLLECT -> (-28).dp
            else -> (-8).dp
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "mascotSafeX",
    )
    val y by animateDpAsState(
        targetValue = when {
            !inMatch -> (-106).dp
            motion == MascotMotion.SAD -> 54.dp
            motion == MascotMotion.STREAK -> (-54).dp
            motion == MascotMotion.HAPPY -> (-28).dp
            else -> 36.dp
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "mascotSafeY",
    )

    Box(Modifier.fillMaxSize()) {
        RiggedKittenScene(
            motion = motion,
            modifier = Modifier
                .align(if (inMatch) Alignment.CenterEnd else Alignment.CenterEnd)
                .offset(x = x, y = y)
                .size(if (inMatch) 94.dp else 132.dp),
        )
    }
}

@Composable
private fun RiggedKittenScene(
    motion: MascotMotion,
    modifier: Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/mascot_kitten.glb")

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        isOpaque = false,
    ) {
        modelInstance?.let { model ->
            ModelNode(
                modelInstance = model,
                animationName = motion.animationName,
                animationLoop = motion.looping,
                animationSpeed = when (motion) {
                    MascotMotion.HAPPY -> 1.08f
                    MascotMotion.STREAK -> 1.12f
                    MascotMotion.SAD -> 0.88f
                    else -> 1.0f
                },
                scaleToUnits = 1.0f,
                centerOrigin = Position(y = -0.45f),
                isEditable = false,
            )
        }
    }
}
