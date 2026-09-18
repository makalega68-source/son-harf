package com.sonharf.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Harf Yolu uses the same premium blue / turquoise / purple / orange / white brand system. */
@Composable
internal fun HarfYoluBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "harfYoluBackdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6400, easing = LinearEasing), RepeatMode.Reverse),
        label = "harfYoluBackdropPhase",
    )
    val pulse by transition.animateFloat(
        initialValue = .76f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse),
        label = "harfYoluBackdropPulse",
    )

    Canvas(modifier = modifier) {
        val blue = Color(0xFF2563EB)
        val turquoise = Color(0xFF12B8A6)
        val orange = Color(0xFFF97316)
        val purple = Color(0xFF7C3AED)

        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.White, Color(0xFFF8FBFF), Color(0xFFF2F7FF), Color(0xFFF8F5FF), Color.White),
            ),
        )

        fun glow(color: Color, x: Float, y: Float, radius: Float, alpha: Float) {
            val center = Offset(size.width * x, size.height * y)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha * pulse), Color.Transparent),
                    center = center,
                    radius = size.minDimension * radius,
                ),
                radius = size.minDimension * radius,
                center = center,
            )
        }

        glow(blue, .94f, .13f, .56f, .13f)
        glow(turquoise, .05f, .75f, .62f, .14f)
        glow(orange, .04f, .34f, .33f, .07f)
        glow(purple, .96f, .80f, .40f, .09f)

        data class Tile(val x: Float, val y: Float, val scale: Float, val color: Color)
        val tiles = listOf(
            Tile(.03f, .11f, .060f, blue), Tile(.13f, .055f, .035f, purple),
            Tile(.94f, .09f, .056f, turquoise), Tile(.985f, .25f, .035f, orange),
            Tile(.02f, .34f, .030f, orange), Tile(.97f, .46f, .034f, purple),
            Tile(.035f, .67f, .052f, turquoise), Tile(.96f, .72f, .046f, blue),
            Tile(.10f, .89f, .034f, purple), Tile(.88f, .93f, .040f, orange),
        )
        tiles.forEachIndexed { index, tile ->
            val side = size.minDimension * tile.scale
            val motion = if (index % 2 == 0) phase else 1f - phase
            val center = Offset(
                size.width * tile.x + (motion - .5f) * side * .30f,
                size.height * tile.y + (motion - .5f) * side * .38f,
            )
            drawRoundRect(
                color = tile.color.copy(alpha = (if (index < 4) .15f else .10f) * pulse),
                topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * .28f, side * .28f),
            )
        }
    }
}
