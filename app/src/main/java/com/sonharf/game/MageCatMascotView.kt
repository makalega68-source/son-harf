package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Native 2D Mage Cat renderer.
 *
 * The purchased Mage Cat facial textures are baked offline into a single 3x3 WebP sprite
 * sheet. Runtime stays 2D-only: no FBX, Unity, Rive or 3D dependency is loaded on Android.
 * State changes are cheap source-rectangle swaps on one decoded bitmap.
 */
@Composable
fun MageCatHomeMascot(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    val ambient = rememberMageCatAmbientVisual()
    MageCatAnimatedImage(
        visual = ambient,
        modifier = modifier,
        prominence = MageCatProminence.AMBIENT,
    )
}

@Composable
internal fun MageCatMatchMascot(
    cue: MageCatCue?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    MageCatReactiveMascot(cue = cue, modifier = modifier, resultMode = false)
}

@Composable
internal fun MageCatResultMascot(
    cue: MageCatCue?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    MageCatReactiveMascot(cue = cue, modifier = modifier, resultMode = true)
}

private enum class MageCatVisual(val column: Int, val row: Int) {
    IDLE(0, 0),
    BLINK(1, 0),
    HAPPY(2, 0),
    EXCITED(0, 1),
    SAD(1, 1),
    ANGRY(2, 1),
    SURPRISED(0, 2),
    THINKING(1, 2),
    WINK(2, 2),
}

private fun visualForMotion(motion: MageCatMotion?): MageCatVisual = when (motion) {
    MageCatMotion.IDLE_BLINK -> MageCatVisual.BLINK
    MageCatMotion.IDLE_WAND_CHECK, MageCatMotion.THINKING -> MageCatVisual.THINKING
    MageCatMotion.HAPPY, MageCatMotion.WAND_CELEBRATE -> MageCatVisual.HAPPY
    MageCatMotion.PROUD, MageCatMotion.WINK -> MageCatVisual.WINK
    MageCatMotion.EXCITED, MageCatMotion.MAGIC_BURST -> MageCatVisual.EXCITED
    MageCatMotion.SAD, MageCatMotion.TIRED -> MageCatVisual.SAD
    MageCatMotion.ANGRY -> MageCatVisual.ANGRY
    MageCatMotion.SURPRISED -> MageCatVisual.SURPRISED
    MageCatMotion.IDLE_LOOK, null -> MageCatVisual.IDLE
}

@Composable
private fun rememberMageCatAmbientVisual(): MageCatVisual {
    var visual by remember { mutableStateOf(MageCatVisual.IDLE) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_700L)
            visual = MageCatVisual.BLINK
            delay(170L)
            visual = MageCatVisual.IDLE
            delay(2_150L)
            visual = MageCatVisual.THINKING
            delay(760L)
            visual = MageCatVisual.IDLE
            delay(2_900L)
            visual = MageCatVisual.WINK
            delay(480L)
            visual = MageCatVisual.IDLE
        }
    }
    return visual
}

@Composable
private fun MageCatReactiveMascot(
    cue: MageCatCue?,
    modifier: Modifier,
    resultMode: Boolean,
) {
    val ambient = rememberMageCatAmbientVisual()
    val prominence = cue?.prominence ?: MageCatProminence.AMBIENT
    val targetScale = when (prominence) {
        MageCatProminence.AMBIENT -> 1f
        MageCatProminence.REACTION -> if (resultMode) 1.08f else 1.10f
        MageCatProminence.HERO -> if (resultMode) 1.16f else 1.10f
    }
    val targetRotation = when (cue?.motion) {
        MageCatMotion.EXCITED, MageCatMotion.MAGIC_BURST, MageCatMotion.WAND_CELEBRATE -> -4f
        MageCatMotion.HAPPY, MageCatMotion.PROUD, MageCatMotion.WINK -> 2.5f
        MageCatMotion.SAD, MageCatMotion.TIRED -> -2.5f
        MageCatMotion.ANGRY -> 2f
        MageCatMotion.SURPRISED -> -1.5f
        else -> 0f
    }
    val targetOffsetY = when (cue?.motion) {
        MageCatMotion.EXCITED, MageCatMotion.MAGIC_BURST, MageCatMotion.WAND_CELEBRATE -> -8f
        MageCatMotion.SAD, MageCatMotion.TIRED -> 4f
        MageCatMotion.SURPRISED -> -4f
        else -> 0f
    }

    val scale by animateFloatAsState(targetScale, tween(260), label = "mageCatScale")
    val rotation by animateFloatAsState(targetRotation, tween(300), label = "mageCatRotation")
    val offsetY by animateFloatAsState(targetOffsetY, tween(240), label = "mageCatOffsetY")

    MageCatAnimatedImage(
        visual = cue?.motion?.let(::visualForMotion) ?: ambient,
        prominence = prominence,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
            translationY = offsetY
        },
    )
}

@Composable
private fun MageCatAnimatedImage(
    visual: MageCatVisual,
    prominence: MageCatProminence,
    modifier: Modifier,
) {
    val bitmap = ImageBitmap.imageResource(R.drawable.mage_cat_mimics_sheet)
    val idleTransition = rememberInfiniteTransition(label = "mageCatIdleBody")
    val bob by idleTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (prominence == MageCatProminence.HERO) -4f else -2.5f,
        animationSpec = infiniteRepeatable(tween(1_550), RepeatMode.Reverse),
        label = "mageCatBob",
    )
    val sway by idleTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1_900), RepeatMode.Reverse),
        label = "mageCatSway",
    )

    Canvas(
        modifier = modifier
            .alpha(0.99f)
            .graphicsLayer {
                translationY += bob
                rotationZ += sway
            }
            .semantics { contentDescription = "Mage Cat" },
    ) {
        val frameWidth = bitmap.width / 3
        val frameHeight = bitmap.height / 3
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(visual.column * frameWidth, visual.row * frameHeight),
            srcSize = IntSize(frameWidth, frameHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
    }
}

val MageCatHomeDefaultSize = 124.dp
internal val MageCatMatchDefaultSize = 84.dp
internal val MageCatResultDefaultSize = 136.dp
