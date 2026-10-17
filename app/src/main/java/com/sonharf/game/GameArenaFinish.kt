package com.sonharf.game

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/** Subtle material light shared by game tiles, without an extra layer or running animator. */
internal fun arenaTileBrush(base: Color): Brush = Brush.verticalGradient(
    listOf(lerp(base, Color.White, .08f), base, lerp(base, Color.Black, .045f)),
)

internal fun Modifier.arenaTileFinish(): Modifier = drawWithCache {
    val inset = 7.dp.toPx().coerceAtMost(size.minDimension * .18f)
    val stroke = .65.dp.toPx()
    onDrawWithContent {
        drawContent()
        drawLine(Color.White.copy(alpha = .23f), Offset(inset, stroke),
            Offset(size.width - inset, stroke), stroke)
        drawLine(Color.Black.copy(alpha = .07f), Offset(inset, size.height - stroke),
            Offset(size.width - inset, size.height - stroke), stroke)
    }
}
