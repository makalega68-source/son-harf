package com.sonharf.game.ui.premium

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Counts from previous value to [value] over [durationMs] (G5.0).
 * The rendered text uses tabular numerals via feature "tnum" so the layout
 * does not shift as digits change (see G5.9 simetri rules).
 */
@Composable
fun CountUpText(
    value: Int,
    modifier: Modifier = Modifier,
    durationMs: Int = 600,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    prefix: String = "",
    suffix: String = "",
    textAlign: TextAlign? = null,
) {
    val displayed = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value) {
        displayed.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = durationMs),
        )
    }
    val tabular = style.copy(
        fontFeatureSettings = "tnum",
    )
    Text(
        text = "$prefix${displayed.value.toInt()}$suffix",
        modifier = modifier,
        style = tabular,
        color = color,
        textAlign = textAlign,
    )
}
