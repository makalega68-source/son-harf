package com.sonharf.game

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

internal data class EveHomeRoutineTiming(
    val digMs: Long = 60_000L,
    val sitMs: Long = 60_000L,
    val happyReactionMs: Long = 3_200L,
)

private const val EVE_HOME_ROUTINE_LOG = "EVE_HOME_ROUTINE"

/**
 * Home-only living companion layer.
 *
 * The real GLB is rendered on a transparent compact TextureSurface. The layer is deliberately
 * mounted above the Scaffold by ClassicPremiumApp, so Eve may overlap the blue game card and even
 * the bottom navigation. She stays near her current anchor with subtle drift instead of visibly
 * sliding across the screen without an in-place walk clip. Long-press dragging lets the player
 * place her anywhere in the allowed screen area.
 */
@Composable
internal fun EveHomeFloatingCompanion(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = { EveLivingRoomRuntime.show() },
    routineTiming: EveHomeRoutineTiming = EveHomeRoutineTiming(),
) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    var companionName by remember { mutableStateOf(store.name) }
    var playerName by remember { mutableStateOf(sh("Oyuncu", "Player")) }
    var renameOpen by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(companionName) }
    var routineGeneration by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (SupabaseProvider.configured) {
            runCatching {
                val backend = OnlineGameBackend()
                val userId = backend.currentUserId()
                userId?.let { backend.getProfile(it)?.displayName }
            }.getOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { playerName = it.take(32) }
        }
    }

    DisposableEffect(Unit) {
        // HOME owns a deterministic routine; suspend random idle selection while this overlay lives.
        EveMascotRuntime.stopLivingBehavior()
        onDispose {
            EveMascotRuntime.startLivingBehavior()
        }
    }

    LaunchedEffect(routineGeneration, routineTiming) {
        if (routineGeneration > 0) {
            delay(routineTiming.happyReactionMs)
        }

        if (BuildConfig.DEBUG) Log.i(EVE_HOME_ROUTINE_LOG, "phase=DIGGING")
        EveMascotRuntime.homeDigging()
        delay(routineTiming.digMs)

        if (BuildConfig.DEBUG) Log.i(EVE_HOME_ROUTINE_LOG, "phase=SITTING")
        EveMascotRuntime.homeSitting()
        delay(routineTiming.sitMs)

        if (BuildConfig.DEBUG) Log.i(EVE_HOME_ROUTINE_LOG, "phase=SLEEPING")
        EveMascotRuntime.homeSleeping()
    }

    // Re-evaluate needs and recent conversation tone while HOME is visible. This is intentionally
    // low-frequency: Eve can express a need, but never nags or steals focus continuously.
    LaunchedEffect(store) {
        while (currentCoroutineContext().isActive) {
            EveMascotRuntime.updateContext(store.behaviorContext())
            delay(5_500L)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val motionEffect = EveMascotRuntime.motionEffect
        val motionVersion = EveMascotRuntime.motionVersion
        val homeIntent = EveMascotRuntime.homeIntent
        val homeIntentVersion = EveMascotRuntime.homeIntentVersion
        val homePromptText = EveMascotRuntime.homePromptText

        val reactionY = remember { Animatable(0f) }
        val reactionRotation = remember { Animatable(0f) }
        val reactionScale = remember { Animatable(1f) }
        val presenceX = remember { Animatable(0f) }
        val presenceY = remember { Animatable(0f) }
        val presenceRotation = remember { Animatable(0f) }
        val presenceScale = remember { Animatable(1f) }

        val mascotSize = 148.dp
        val labelHeight = 24.dp
        val containerWidth = 154.dp
        val containerHeight = mascotSize + labelHeight

        val mascotPx = with(density) { containerWidth.toPx() }
        val containerHeightPx = with(density) { containerHeight.toPx() }
        val sideInsetPx = with(density) { 4.dp.toPx() }
        val topInsetPx = with(density) { 86.dp.toPx() }
        val bottomInsetPx = with(density) { 4.dp.toPx() }
        val driftPx = with(density) { 9.dp.toPx() }
        val jumpPx = with(density) { 22.dp.toPx() }
        val sleepySettlePx = with(density) { 7.dp.toPx() }
        val approachLiftPx = with(density) { 6.dp.toPx() }

        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val minX = sideInsetPx
        val maxX = (widthPx - mascotPx - sideInsetPx).coerceAtLeast(minX)
        val minY = topInsetPx.coerceAtMost((heightPx - containerHeightPx).coerceAtLeast(0f))
        val maxY = (heightPx - containerHeightPx - bottomInsetPx).coerceAtLeast(minY)

        val x = remember { Animatable(0f) }
        val y = remember { Animatable(0f) }

        /**
         * "Approach the player" is a controlled HOME compositor move: Eve travels toward the
         * screen center and grows in apparent size, then holds a camera-facing safe GLB clip.
         *
         * We intentionally do NOT play the accepted GLB's root-translating Walk clip here. Until
         * Blender exports an in-place walk, doing so would cause foot/root drift out of the fixed
         * viewport. This gives a stable near-camera approach without corrupting the rig transform.
         */
        LaunchedEffect(
            homeIntent,
            homeIntentVersion,
            widthPx,
            heightPx,
            mascotPx,
            containerHeightPx,
        ) {
            presenceX.stop()
            presenceY.stop()
            presenceRotation.stop()
            presenceScale.stop()

            val focusX = ((widthPx - mascotPx) * 0.5f).coerceIn(minX, maxX)
            val focusY = (heightPx * 0.25f).coerceIn(minY, maxY)
            val targetX = focusX - x.value
            val targetY = focusY - y.value

            when (homeIntent) {
                EveHomeIntent.NORMAL -> coroutineScope {
                    launch { presenceX.animateTo(0f, tween(520, easing = FastOutSlowInEasing)) }
                    launch { presenceY.animateTo(0f, tween(520, easing = FastOutSlowInEasing)) }
                    launch { presenceRotation.animateTo(0f, tween(360, easing = FastOutSlowInEasing)) }
                    launch { presenceScale.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
                }

                EveHomeIntent.SLEEP -> coroutineScope {
                    launch { presenceX.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                    launch { presenceY.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                    launch { presenceRotation.animateTo(0f, tween(360, easing = FastOutSlowInEasing)) }
                    launch { presenceScale.animateTo(0.96f, tween(460, easing = FastOutSlowInEasing)) }
                }

                EveHomeIntent.CELEBRATE -> coroutineScope {
                    launch { presenceX.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                    launch { presenceY.animateTo(-approachLiftPx, tween(360, easing = FastOutSlowInEasing)) }
                    launch { presenceRotation.animateTo(0f, tween(260)) }
                    launch { presenceScale.animateTo(1.09f, tween(420, easing = FastOutSlowInEasing)) }
                }

                EveHomeIntent.APPROACH_LOOK,
                EveHomeIntent.ASK_PET,
                EveHomeIntent.ASK_FOOD,
                EveHomeIntent.COMFORT -> {
                    val targetScale = when (homeIntent) {
                        EveHomeIntent.ASK_PET -> 1.30f
                        EveHomeIntent.ASK_FOOD -> 1.23f
                        EveHomeIntent.COMFORT -> 1.27f
                        else -> 1.28f
                    }
                    val targetRotation = when (homeIntent) {
                        EveHomeIntent.ASK_PET -> 5.5f
                        EveHomeIntent.ASK_FOOD -> -2.5f
                        EveHomeIntent.COMFORT -> 2.0f
                        else -> 0f
                    }

                    coroutineScope {
                        launch { presenceX.animateTo(targetX, tween(900, easing = FastOutSlowInEasing)) }
                        launch {
                            presenceY.animateTo(targetY - approachLiftPx, tween(620, easing = FastOutSlowInEasing))
                            presenceY.animateTo(targetY, tween(280, easing = FastOutSlowInEasing))
                        }
                        launch { presenceRotation.animateTo(targetRotation, tween(760, easing = FastOutSlowInEasing)) }
                        launch { presenceScale.animateTo(targetScale, tween(900, easing = FastOutSlowInEasing)) }
                    }
                }
            }
        }

        // Reaction motion transforms the transparent TextureSurface as one composited layer. It
        // never rewrites ModelNode position/rotation/scale, which keeps SceneView's normalized GLB
        // transform intact. The underlying skeletal pose still comes from the real GLB clips.
        LaunchedEffect(motionEffect, motionVersion, jumpPx, sleepySettlePx) {
            reactionY.stop()
            reactionRotation.stop()
            reactionScale.stop()

            when (motionEffect) {
                EveMotionEffect.NONE -> coroutineScope {
                    launch { reactionY.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
                    launch { reactionRotation.animateTo(0f, tween(160, easing = FastOutSlowInEasing)) }
                    launch { reactionScale.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
                }

                EveMotionEffect.BOUNCE -> coroutineScope {
                    launch {
                        reactionY.animateTo(-jumpPx, tween(170, easing = FastOutSlowInEasing))
                        reactionY.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                    }
                    launch {
                        reactionScale.animateTo(1.045f, tween(160, easing = FastOutSlowInEasing))
                        reactionScale.animateTo(1f, tween(330, easing = FastOutSlowInEasing))
                    }
                }

                EveMotionEffect.WIGGLE -> {
                    reactionRotation.snapTo(0f)
                    for (target in listOf(7f, -7f, 5f, -5f, 2.5f, 0f)) {
                        reactionRotation.animateTo(target, tween(95))
                    }
                }

                EveMotionEffect.SAD_SETTLE -> coroutineScope {
                    launch { reactionY.animateTo(sleepySettlePx, tween(460, easing = FastOutSlowInEasing)) }
                    launch { reactionScale.animateTo(0.97f, tween(460, easing = FastOutSlowInEasing)) }
                }
            }
        }
        var anchorX by remember { mutableStateOf(0f) }
        var anchorY by remember { mutableStateOf(0f) }
        var initialized by remember { mutableStateOf(false) }
        var dragging by remember { mutableStateOf(false) }

        LaunchedEffect(widthPx, heightPx, renameOpen) {
            if (!initialized) {
                // Starts where the approved home composition leaves room for Eve: left of the
                // blue Son Harf card, with enough size to overlap its edge.
                anchorX = minX
                anchorY = minY + (maxY - minY) * 0.46f
                x.snapTo(anchorX)
                y.snapTo(anchorY)
                initialized = true
            } else {
                anchorX = anchorX.coerceIn(minX, maxX)
                anchorY = anchorY.coerceIn(minY, maxY)
                x.snapTo(x.value.coerceIn(minX, maxX))
                y.snapTo(y.value.coerceIn(minY, maxY))
            }

            while (currentCoroutineContext().isActive) {
                if (!dragging && !renameOpen && homeIntent == EveHomeIntent.NORMAL) {
                    // Tiny organic drift around the current anchor. Large autonomous screen travel
                    // would look like foot sliding because the accepted GLB walk clip has root
                    // translation and is intentionally not used until an in-place Blender export.
                    val targetX = (anchorX + Random.nextDouble(-driftPx.toDouble(), driftPx.toDouble()).toFloat())
                        .coerceIn(minX, maxX)
                    val targetY = (anchorY + Random.nextDouble(-driftPx.toDouble(), driftPx.toDouble()).toFloat())
                        .coerceIn(minY, maxY)
                    val durationMs = Random.nextInt(2_700, 4_201)

                    coroutineScope {
                        launch {
                            x.animateTo(
                                targetValue = targetX,
                                animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
                            )
                        }
                        launch {
                            y.animateTo(
                                targetValue = targetY,
                                animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                    delay(Random.nextLong(120L, 401L))
                } else {
                    delay(180L)
                }
            }
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                .width(containerWidth)
                .height(containerHeight)
                .graphicsLayer {
                    translationX = presenceX.value
                    translationY = presenceY.value + reactionY.value
                    rotationZ = presenceRotation.value + reactionRotation.value
                    scaleX = presenceScale.value * reactionScale.value
                    scaleY = presenceScale.value * reactionScale.value
                }
                .zIndex(50f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(mascotSize)
                    .clickable {
                        store.markInteraction()
                        // Cancels the current dig/sit/sleep timer via LaunchedEffect key change.
                        // Eve reacts happily first, then the 60s -> 60s -> sleep routine restarts.
                        routineGeneration++
                        EveMascotRuntime.homeTouchHappy(playerName)
                    }
                    .pointerInput(widthPx, heightPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragging = true
                                scope.launch {
                                    x.stop()
                                    y.stop()
                                }
                            },
                            onDragEnd = {
                                anchorX = x.value.coerceIn(minX, maxX)
                                anchorY = y.value.coerceIn(minY, maxY)
                                dragging = false
                            },
                            onDragCancel = {
                                anchorX = x.value.coerceIn(minX, maxX)
                                anchorY = y.value.coerceIn(minY, maxY)
                                dragging = false
                            },
                        ) { _, dragAmount ->
                            val nextX = (x.value + dragAmount.x).coerceIn(minX, maxX)
                            val nextY = (y.value + dragAmount.y).coerceIn(minY, maxY)
                            scope.launch {
                                x.snapTo(nextX)
                                y.snapTo(nextY)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Keep HOME's TextureSurface completely out of composition while the rename dialog
                // is active. That preserves the earlier safety assumption that HOME TextureSurface
                // is not alive during IME interaction.
                if (!renameOpen) {
                    EveLive3DStage(
                        modifier = Modifier.fillMaxSize(),
                        compact = true,
                    )

                    if (homePromptText.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-2).dp),
                            color = Color.White.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 3.dp,
                        ) {
                            Text(
                                text = homePromptText.take(64),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                color = Color(0xFF163B58),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(1.dp))
            Text(
                text = "$companionName ✎",
                modifier = Modifier.clickable {
                    draftName = companionName
                    renameOpen = true
                },
                color = Color(0xFF163B58),
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = {
                draftName = companionName
                renameOpen = false
            },
            title = {
                Text(sh("Maskotun adını değiştir", "Rename companion"))
            },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(18) },
                    singleLine = true,
                    label = { Text(sh("İsim", "Name")) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.name = draftName
                        companionName = store.name
                        renameOpen = false
                        EveMascotRuntime.happyReaction()
                    },
                ) {
                    Text(sh("Kaydet", "Save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        draftName = companionName
                        renameOpen = false
                    },
                ) {
                    Text(sh("İptal", "Cancel"))
                }
            },
        )
    }

    // Retain the existing callback contract for callers that explicitly want to open the room.
    // The production home uses the center bottom-bar paw for room navigation; tapping Eve reacts.
    @Suppress("UNUSED_VARIABLE")
    val retainedOpenCallback = onOpen
}
