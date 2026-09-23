package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal const val WORD_SIEGE_CAPTURE_FLIGHT_MS = 720
internal const val WORD_SIEGE_CAPTURE_STAGGER_MS = 95L

internal data class WordSiegeCaptureEffect(
    val batch: WordSiegeCaptureBatch,
    val targetInWindow: Offset,
    val accent: Color,
    val onCubeArrived: (Int) -> Unit,
    val onFinished: () -> Unit,
)

internal fun wordSiegeCaptureCellCenterInWindow(
    index: Int,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
    viewportOriginInWindow: Offset,
): Offset {
    if (!wordSiegeCapturePositionReady(viewportOriginInWindow)) return Offset.Unspecified
    return viewportOriginInWindow + wordSiegeCellCenterInViewport(index, transform, cellSizePx)
}

internal fun wordSiegeCaptureArcPoint(start: Offset, end: Offset, progress: Float): Offset {
    val t = progress.coerceIn(0f, 1f)
    val oneMinus = 1f - t
    val lift = maxOf(
        42f,
        abs(end.y - start.y) * .10f + abs(end.x - start.x) * .055f,
    )
    val control = Offset(
        x = (start.x + end.x) / 2f,
        y = minOf(start.y, end.y) - lift,
    )
    return Offset(
        x = oneMinus * oneMinus * start.x + 2f * oneMinus * t * control.x + t * t * end.x,
        y = oneMinus * oneMinus * start.y + 2f * oneMinus * t * control.y + t * t * end.y,
    )
}

internal fun wordSiegeCaptureStaggerDelayMs(ordinal: Int): Long =
    ordinal.coerceAtLeast(0) * WORD_SIEGE_CAPTURE_STAGGER_MS

internal fun wordSiegeCapturePositionReady(position: Offset): Boolean =
    position.x.isFinite() && position.y.isFinite()

@Composable
internal fun WordSiegeCaptureFlightOverlay(
    effect: WordSiegeCaptureEffect,
    sourcePositionsInWindow: Map<Int, Offset>,
    anchorOriginInWindow: Offset,
) {
    val allSourcesReady = effect.batch.indices.all { index ->
        sourcePositionsInWindow[index]?.let(::wordSiegeCapturePositionReady) == true
    }
    val ready = allSourcesReady &&
        wordSiegeCapturePositionReady(effect.targetInWindow) &&
        wordSiegeCapturePositionReady(anchorOriginInWindow)

    val frozenSources = remember(effect.batch.updateKey, ready) {
        if (ready) sourcePositionsInWindow.toMap() else emptyMap()
    }
    val frozenTarget = remember(effect.batch.updateKey, ready) {
        if (ready) effect.targetInWindow else Offset.Unspecified
    }
    val progresses = remember(effect.batch.updateKey) {
        effect.batch.indices.map { Animatable(0f) }
    }
    var started by remember(effect.batch.updateKey) { mutableStateOf(false) }
    val onCubeArrived by rememberUpdatedState(effect.onCubeArrived)
    val onFinished by rememberUpdatedState(effect.onFinished)

    LaunchedEffect(effect.batch.updateKey, ready) {
        if (!ready || started) return@LaunchedEffect
        started = true
        coroutineScope {
            progresses.forEachIndexed { ordinal, animation ->
                launch {
                    delay(wordSiegeCaptureStaggerDelayMs(ordinal))
                    animation.snapTo(0f)
                    animation.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = WORD_SIEGE_CAPTURE_FLIGHT_MS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                    onCubeArrived(effect.batch.indices[ordinal])
                }
            }
        }
        onFinished()
    }

    if (!ready || frozenSources.isEmpty()) return

    val popupHalfPx = with(LocalDensity.current) { 36.dp.toPx() }

    effect.batch.indices.forEachIndexed { ordinal, index ->
        val progress = progresses[ordinal].value
        if (progress <= 0.001f || progress >= 0.9999f) return@forEachIndexed

        val start = frozenSources.getValue(index)
        val point = wordSiegeCaptureArcPoint(start, frozenTarget, progress)
        val earlier = wordSiegeCaptureArcPoint(start, frozenTarget, (progress - .055f).coerceAtLeast(0f))
        val velocity = point - earlier
        val localPoint = point - anchorOriginInWindow

        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(
                x = (localPoint.x - popupHalfPx).roundToInt(),
                y = (localPoint.y - popupHalfPx).roundToInt(),
            ),
            properties = PopupProperties(focusable = false),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        alpha = when {
                            progress < .10f -> (progress / .10f).coerceIn(0f, 1f)
                            progress > .86f -> ((1f - progress) / .14f).coerceIn(0f, 1f)
                            else -> 1f
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    if (progress < .26f) {
                        val flash = (1f - progress / .20f).coerceIn(0f, 1f)
                        drawCircle(
                            color = effect.accent.copy(alpha = .28f * flash),
                            radius = 18.dp.toPx() + 8.dp.toPx() * (1f - flash),
                            center = center,
                        )
                    }

                    val length = sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
                    val unit = if (length > .001f) {
                        Offset(velocity.x / length, velocity.y / length)
                    } else {
                        Offset.Zero
                    }
                    repeat(5) { particle ->
                        val distance = (particle + 1) * 5.5f
                        drawCircle(
                            color = effect.accent.copy(alpha = .34f * (1f - particle / 5f)),
                            radius = (3.1f - particle * .38f).dp.toPx(),
                            center = center - unit * distance,
                        )
                    }
                    drawCircle(
                        color = effect.accent.copy(alpha = .16f),
                        radius = 15.dp.toPx(),
                        center = center,
                    )
                }
                Text(
                    text = "+$WORD_SIEGE_CAPTURE_POINTS_PER_CUBE",
                    color = effect.accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
