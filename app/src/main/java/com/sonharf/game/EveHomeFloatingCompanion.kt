package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Home-only floating companion layer.
 *
 * The real 3D Surface keeps a constant 112dp size; only its Compose position moves. This avoids
 * resizing/recreating the Vulkan surface while letting Eve roam over the home screen. A tap opens
 * the room. A long-press drag lets the player reposition Eve, then autonomous perimeter roaming
 * resumes.
 */
@Composable
internal fun EveHomeFloatingCompanion(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = { EveLivingRoomRuntime.show() },
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val mascotSize = 112.dp
        val mascotPx = with(density) { mascotSize.toPx() }
        val sideInsetPx = with(density) { 10.dp.toPx() }
        val topInsetPx = with(density) { 78.dp.toPx() }
        val bottomInsetPx = with(density) { 88.dp.toPx() }

        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val minX = sideInsetPx
        val maxX = (widthPx - mascotPx - sideInsetPx).coerceAtLeast(minX)
        val minY = topInsetPx
        val maxY = (heightPx - mascotPx - bottomInsetPx).coerceAtLeast(minY)

        val x = remember { Animatable(0f) }
        val y = remember { Animatable(0f) }
        var initialized by remember { mutableStateOf(false) }
        var dragging by remember { mutableStateOf(false) }

        LaunchedEffect(widthPx, heightPx) {
            if (!initialized) {
                x.snapTo(maxX)
                y.snapTo(minY + (maxY - minY) * 0.62f)
                initialized = true
            } else {
                x.snapTo(x.value.coerceIn(minX, maxX))
                y.snapTo(y.value.coerceIn(minY, maxY))
            }

            // Perimeter-biased path keeps the main OYNA area readable instead of wandering through
            // the center continuously. Movement is deliberately slow and pauses between waypoints.
            val waypoints = listOf(
                maxX to (minY + (maxY - minY) * 0.24f),
                maxX to (minY + (maxY - minY) * 0.72f),
                minX to (minY + (maxY - minY) * 0.72f),
                minX to (minY + (maxY - minY) * 0.24f),
            )
            var index = 0
            while (currentCoroutineContext().isActive) {
                if (!dragging) {
                    val (targetX, targetY) = waypoints[index % waypoints.size]
                    coroutineScope {
                        launch {
                            x.animateTo(
                                targetValue = targetX,
                                animationSpec = tween(4_800, easing = FastOutSlowInEasing),
                            )
                        }
                        launch {
                            y.animateTo(
                                targetValue = targetY,
                                animationSpec = tween(4_800, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                    index++
                    delay(2_200)
                } else {
                    delay(250)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                .size(mascotSize)
                .zIndex(20f)
                .clickable(onClick = onOpen)
                .pointerInput(widthPx, heightPx) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragging = true
                            scope.launch {
                                x.stop()
                                y.stop()
                            }
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, dragAmount ->
                        change.consume()
                        val nextX = (x.value + dragAmount.x).coerceIn(minX, maxX)
                        val nextY = (y.value + dragAmount.y).coerceIn(minY, maxY)
                        scope.launch {
                            x.snapTo(nextX)
                            y.snapTo(nextY)
                        }
                    }
                },
        ) {
            EveLive3DStage(
                modifier = Modifier.fillMaxSize(),
                compact = true,
            )
        }
    }
}
