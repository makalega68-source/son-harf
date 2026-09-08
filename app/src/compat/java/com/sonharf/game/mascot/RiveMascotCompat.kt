package com.sonharf.game.mascot

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit

private val RIVE_COMPAT_MAP: Map<String, Triple<String, String, String>> = mapOf(
    "gut" to Triple("gut_expression.riv", "State Machine 1", "Expression"),
    "sprout" to Triple("sprout.riv", "State Machine 1", "mood"),
    "o11y" to Triple("o11y.riv", "State Machine 1", "mood")
)

private fun compatMoodToNumber(mood: Mood): Float = when (mood) {
    Mood.IDLE -> 0f
    Mood.HAPPY, Mood.EXCITED -> 1f
    Mood.ANGRY, Mood.PANIC -> 2f
    Mood.SAD, Mood.CRYING -> 3f
    Mood.WINK -> 4f
    Mood.TIRED -> 0f
}

@Composable
fun RiveMascotFigure(key: String, mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    val cfg = RIVE_COMPAT_MAP[key]
    if (cfg == null) {
        MageCatFigure(mood, MascotBrain.eyesVariant, size, modifier = modifier)
        return
    }
    val (asset, smName, inputName) = cfg

    AndroidView(
        modifier = modifier.size(size),
        factory = { ctx ->
            RiveAnimationView(ctx).apply {
                runCatching {
                    val bytes = ctx.assets.open(asset).use { it.readBytes() }
                    setRiveBytes(
                        bytes = bytes,
                        autoplay = true,
                        fit = Fit.CONTAIN,
                        stateMachineName = smName
                    )
                }
            }
        },
        update = { view ->
            runCatching {
                view.setNumberState(smName, inputName, compatMoodToNumber(mood))
            }
        }
    )
}

@Composable
fun EquippedMascotFigure(equippedKey: String, mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    if (equippedKey == "magecat" || equippedKey.isBlank()) {
        MageCatFigure(mood, MascotBrain.eyesVariant, size, modifier = modifier)
    } else {
        RiveMascotFigure(equippedKey, mood, size, modifier)
    }
}
