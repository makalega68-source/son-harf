package com.sonharf.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Mage Cat presentation layer.
 *
 * Runtime 3D dependencies are intentionally avoided. The renderer accepts the existing
 * behavior/director contract and resolves every motion to an offline drawable. Until the
 * final pose PNG set is added, every semantic visual safely resolves to the single verified
 * anthracite preview resource; this avoids referencing missing R.drawable symbols.
 */
@Composable
fun MageCatHomeMascot(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    MageCatImage(visual = MageCatVisual.IDLE, modifier = modifier)
}

/** Match mascot stays small; it reacts only when the director produces a cue. */
@Composable
internal fun MageCatMatchMascot(
    cue: MageCatCue?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    MageCatReactiveMascot(cue = cue, modifier = modifier, resultMode = false)
}

/** Result mascot uses the same renderer contract with stronger HERO presentation. */
@Composable
internal fun MageCatResultMascot(
    cue: MageCatCue?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    MageCatReactiveMascot(cue = cue, modifier = modifier, resultMode = true)
}

private enum class MageCatVisual {
    IDLE,
    HAPPY,
    EXCITED,
    DEFEAT,
}

private fun visualForMotion(motion: MageCatMotion?): MageCatVisual = when (motion) {
    MageCatMotion.HAPPY,
    MageCatMotion.PROUD,
    MageCatMotion.WAND_CELEBRATE,
    -> MageCatVisual.HAPPY

    MageCatMotion.EXCITED,
    MageCatMotion.MAGIC_BURST,
    -> MageCatVisual.EXCITED

    MageCatMotion.SAD,
    MageCatMotion.TIRED,
    -> MageCatVisual.DEFEAT

    MageCatMotion.IDLE_LOOK,
    MageCatMotion.IDLE_BLINK,
    MageCatMotion.IDLE_WAND_CHECK,
    null,
    -> MageCatVisual.IDLE
}

@Composable
private fun MageCatReactiveMascot(
    cue: MageCatCue?,
    modifier: Modifier,
    resultMode: Boolean,
) {
    val prominence = cue?.prominence ?: MageCatProminence.AMBIENT
    val targetScale = when (prominence) {
        MageCatProminence.AMBIENT -> 1f
        MageCatProminence.REACTION -> if (resultMode) 1.06f else 1.10f
        MageCatProminence.HERO -> if (resultMode) 1.14f else 1.08f
    }
    val targetRotation = when (cue?.motion) {
        MageCatMotion.EXCITED, MageCatMotion.MAGIC_BURST, MageCatMotion.WAND_CELEBRATE -> -4f
        MageCatMotion.HAPPY, MageCatMotion.PROUD -> 2.5f
        MageCatMotion.SAD, MageCatMotion.TIRED -> -2.5f
        else -> 0f
    }
    val targetOffsetY = when (cue?.motion) {
        MageCatMotion.EXCITED, MageCatMotion.MAGIC_BURST, MageCatMotion.WAND_CELEBRATE -> -8f
        MageCatMotion.SAD, MageCatMotion.TIRED -> 4f
        else -> 0f
    }

    val scale = animateFloatAsState(targetScale, tween(260), label = "mageCatScale")
    val rotation = animateFloatAsState(targetRotation, tween(300), label = "mageCatRotation")
    val offsetY = animateFloatAsState(targetOffsetY, tween(240), label = "mageCatOffsetY")

    MageCatImage(
        visual = visualForMotion(cue?.motion),
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            rotationZ = rotation.value
            translationY = offsetY.value
        },
    )
}

private fun drawableForVisual(visual: MageCatVisual): Int = when (visual) {
    // Only a resource that is already present on this branch is referenced here.
    // Final offline-rendered PNGs will replace these entries one by one after asset validation.
    MageCatVisual.IDLE,
    MageCatVisual.HAPPY,
    MageCatVisual.EXCITED,
    MageCatVisual.DEFEAT,
    -> R.drawable.mage_cat_preview_anthracite
}

@Composable
private fun MageCatImage(
    visual: MageCatVisual,
    modifier: Modifier,
) {
    Image(
        painter = painterResource(drawableForVisual(visual)),
        contentDescription = "Mage Cat",
        contentScale = ContentScale.Fit,
        modifier = modifier.alpha(0.98f),
    )
}

val MageCatHomeDefaultSize = 104.dp
internal val MageCatMatchDefaultSize = 68.dp
internal val MageCatResultDefaultSize = 136.dp
