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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.Job
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
    playerNameOverride: String? = null,
    behaviorContextOverride: EveBehaviorContext? = null,
) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    var companionName by remember { mutableStateOf(store.name) }
    var playerName by remember { mutableStateOf(sh("Oyuncu", "Player")) }
    var renameOpen by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(companionName) }
    var routineJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(playerNameOverride) {
        playerNameOverride?.trim()?.takeIf { it.isNotBlank() }?.let {
            playerName = it.take(32)
            return@LaunchedEffect
        }
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

    val routineScope = rememberCoroutineScope()

    fun restartHomeRoutine(afterMs: Long = 0L, reason: String = "baseline") {
        routineJob?.cancel()
        routineJob = routineScope.launch {
            if (BuildConfig.DEBUG) {
                Log.i(EVE_HOME_ROUTINE_LOG, "restart reason=$reason afterMs=$afterMs")
            }
            if (afterMs > 0L) delay(afterMs)

            if (BuildConfig.DEBUG) Log.i(EVE_HOME_ROUTINE_LOG, "phase=DIGGING")
            EveMascotRuntime.homeDigging()
            delay(routineTiming.digMs)

            if (BuildConfig.DEBUG) Log.i(EVE_HOME_ROUTINE_LOG, "phase=SITTING")
            EveMascotRuntime.homeSitting()
            delay(routineTiming.sitMs)

            if (BuildConfig.DEBUG) Log.i(EVE_HOME_ROUTINE_LOG, "phase=SLEEPING")
            EveMascotRuntime.homeSleeping()
        }
    }

    DisposableEffect(routineTiming) {
        // HOME owns one deterministic routine job. Contextual/touch reactions cancel this exact
        // Job before taking control, eliminating timer races with TextureSurface/Compose frames.
        EveMascotRuntime.stopLivingBehavior()
        restartHomeRoutine()
        onDispose {
            routineJob?.cancel()
            routineJob = null
            EveMascotRuntime.startLivingBehavior()
        }
    }

    // Re-evaluate needs and recent conversation tone while HOME is visible. This is intentionally
    // low-frequency: Eve can express a need, but never nags or steals focus continuously.
    LaunchedEffect(store, behaviorContextOverride) {
        while (currentCoroutineContext().isActive) {
            val contextSnapshot = behaviorContextOverride ?: store.behaviorContext()
            val firedIntent = EveMascotRuntime.updateContext(contextSnapshot)
            if (firedIntent != null) {
                // Cancel the exact baseline Job immediately. Only after the contextual reaction
                // has owned Eve for its full window do we restart DIG -> SIT -> SLEEP from zero.
                restartHomeRoutine(
                    afterMs = firedIntent.homeHoldMs(),
                    reason = "context:$firedIntent",
                )
            }
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

        val reactionX = remember { Animatable(0f) }
        val reactionY = remember { Animatable(0f) }
        val presenceX = remember { Animatable(0f) }
        val presenceY = remember { Animatable(0f) }

        // The 3D viewport itself now follows the approved mockup proportions: ~43% screen width
        // with a tall portrait viewport so ears/head can reach upward while paws overlap the cards.
        val mascotWidth = (maxWidth * 0.43f).coerceIn(150.dp, 188.dp)
        val mascotHeight = mascotWidth * 1.55f
        val promptHeight = 46.dp
        val labelHeight = 24.dp
        val containerWidth = mascotWidth
        val containerHeight = promptHeight + mascotHeight + labelHeight

        val mascotPx = with(density) { containerWidth.toPx() }
        val containerHeightPx = with(density) { containerHeight.toPx() }
        val sideInsetPx = with(density) { 4.dp.toPx() }
        val topInsetPx = with(density) { 38.dp.toPx() }
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

            // Keep contextual approach in Eve's left-side territory so the primary cards remain
            // readable while she comes closer to the player.
            val focusX = ((widthPx - mascotPx) * 0.28f).coerceIn(minX, maxX)
            val focusY = (heightPx * 0.25f).coerceIn(minY, maxY)
            val targetX = focusX - x.value
            val targetY = focusY - y.value

            when (homeIntent) {
                EveHomeIntent.NORMAL -> coroutineScope {
                    launch { presenceX.animateTo(0f, tween(520, easing = FastOutSlowInEasing)) }
                    launch { presenceY.animateTo(0f, tween(520, easing = FastOutSlowInEasing)) }
                }

                EveHomeIntent.SLEEP -> coroutineScope {
                    launch { presenceX.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                    launch { presenceY.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                }

                EveHomeIntent.CELEBRATE -> coroutineScope {
                    launch { presenceX.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                    launch { presenceY.animateTo(-approachLiftPx, tween(360, easing = FastOutSlowInEasing)) }
                }

                EveHomeIntent.APPROACH_LOOK,
                EveHomeIntent.ASK_PET,
                EveHomeIntent.ASK_FOOD,
                EveHomeIntent.COMFORT -> {
                    coroutineScope {
                        launch { presenceX.animateTo(targetX, tween(900, easing = FastOutSlowInEasing)) }
                        launch {
                            presenceY.animateTo(targetY - approachLiftPx, tween(620, easing = FastOutSlowInEasing))
                            presenceY.animateTo(targetY, tween(280, easing = FastOutSlowInEasing))
                        }
                    }
                }
            }
        }

        // TextureSurface must remain a genuinely transparent Android layer. Scale/rotation via
        // Compose graphics layers can expose its rectangular backing, so reactions use only layout
        // translation here; all articulation remains in the accepted rigged GLB clips.
        LaunchedEffect(motionEffect, motionVersion, jumpPx, sleepySettlePx) {
            reactionX.stop()
            reactionY.stop()

            when (motionEffect) {
                EveMotionEffect.NONE -> coroutineScope {
                    launch { reactionX.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
                    launch { reactionY.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
                }

                EveMotionEffect.BOUNCE -> {
                    reactionY.animateTo(-jumpPx, tween(170, easing = FastOutSlowInEasing))
                    reactionY.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                }

                EveMotionEffect.WIGGLE -> {
                    reactionX.snapTo(0f)
                    val wigglePx = driftPx * 0.72f
                    for (target in listOf(wigglePx, -wigglePx, wigglePx * 0.65f, -wigglePx * 0.65f, 0f)) {
                        reactionX.animateTo(target, tween(95))
                    }
                }

                EveMotionEffect.SAD_SETTLE -> {
                    reactionY.animateTo(sleepySettlePx, tween(460, easing = FastOutSlowInEasing))
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
                .offset {
                    IntOffset(
                        (x.value + presenceX.value + reactionX.value).roundToInt(),
                        (y.value + presenceY.value + reactionY.value).roundToInt(),
                    )
                }
                .width(containerWidth)
                .height(containerHeight)
                .zIndex(50f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Speech is laid out physically above TextureSurface instead of inside its Android
            // rendering bounds. This guarantees the bubble remains visible on real devices.
            Box(
                modifier = Modifier
                    .width(containerWidth)
                    .height(promptHeight),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (homePromptText.isNotBlank()) {
                    Surface(
                        modifier = Modifier.width(138.dp),
                        color = Color.White.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 5.dp,
                    ) {
                        Text(
                            text = homePromptText.take(64),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            color = Color(0xFF163B58),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                } else if (
                    EveMascotRuntime.behaviorState == EveBehaviorState.RESTING &&
                    EveMascotRuntime.animation == EveAnimationCue.REST
                ) {
                    Text(
                        text = "💤",
                        fontSize = 22.sp,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(mascotWidth)
                    .height(mascotHeight),
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

                    // TextureSurface is an Android-backed rendering child. A dedicated Compose
                    // hit-target above it guarantees taps/long-press drag are received by HOME
                    // instead of relying on parent event propagation through the 3D surface.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                store.markInteraction()
                                EveMascotRuntime.homeTouchHappy(playerName)
                                restartHomeRoutine(
                                    afterMs = routineTiming.happyReactionMs,
                                    reason = "touch",
                                )
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
                    )

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
