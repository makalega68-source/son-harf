package com.sonharf.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Son Harf tactile press-motion primitive.
 *
 * Observes pointer press/release without consuming the gesture, so existing Button/clickable
 * semantics, ripple, accessibility and navigation behavior remain authoritative.
 * Older call sites intentionally requested very tiny 1-2% deltas; clamp them to a still-clean
 * but visibly readable 4.5% press response so players can actually perceive the action.
 */
fun Modifier.sonHarfPressScale(
    pressedScale: Float = 0.98f,
    pressDurationMs: Int = 70,
    releaseDurationMs: Int = 155,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val visiblePressedScale = pressedScale.coerceAtMost(0.955f)
    val scale by animateFloatAsState(
        targetValue = if (pressed) visiblePressedScale else 1f,
        animationSpec = tween(durationMillis = if (pressed) pressDurationMs else releaseDurationMs),
        label = "sonHarfPressScale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                try {
                    waitForUpOrCancellation()
                } finally {
                    pressed = false
                }
            }
        }
}
