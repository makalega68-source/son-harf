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

/** Harf Yolu'na özel, düşük kontrastlı zümrüt-altın hareketli arka plan. */
@Composable
internal fun HarfYoluBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "harfYoluBackdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "harfYoluBackdropPhase",
    )
    val pulse by transition.animateFloat(
        initialValue = .72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "harfYoluBackdropPulse",
    )

    Canvas(modifier = modifier) {
        val blue = Color(0xFF14B8B0)
        val turquoise = Color(0xFF22C3C9)
        val orange = Color(0xFF8B6CF0)
        val purple = Color(0xFF8B6CF0)
        val paleBlue = Color(0xFFE0F3F5)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEAF6F8),
                    Color(0xFFE0F3F5),
                    paleBlue.copy(alpha = .86f),
                    Color(0xFFFFFFFF),
                    Color(0xFFEAF6F8),
                ),
            ),
        )

        val driftX = size.width * (.020f * phase)
        val driftY = size.height * (.016f * phase)

        fun glow(color: Color, x: Float, y: Float, radius: Float, alpha: Float) {
            val center = Offset(size.width * x, size.height * y)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha * pulse), Color.Transparent),
                    center = center,
                    radius = size.minDimension * radius,
                ),
                radius = size.minDimension * radius,
                center = center,
            )
        }

        glow(turquoise, .05f - .012f * phase, .74f - .010f * phase, .64f, .18f)
        glow(orange, .04f + .010f * phase, .34f, .34f, .10f)
        glow(purple, .96f - .010f * phase, .80f, .40f, .12f)
        glow(Color(0xFFEEEBFC), .50f, .47f, .54f, .62f)

        data class Tile(val x: Float, val y: Float, val scale: Float, val color: Color)
        val tiles = listOf(
            Tile(.02f, .34f, .030f, orange),
            Tile(.97f, .46f, .034f, purple),
            Tile(.035f, .67f, .052f, turquoise),
            Tile(.96f, .72f, .046f, blue),
            Tile(.10f, .89f, .034f, purple),
            Tile(.88f, .93f, .040f, orange),
            Tile(.02f, .93f, .026f, turquoise),
            Tile(.98f, .60f, .024f, orange),
        )

        tiles.forEachIndexed { index, tile ->
            val side = size.minDimension * tile.scale
            val motion = if (index % 2 == 0) phase else 1f - phase
            val center = Offset(
                x = size.width * tile.x + (motion - .5f) * side * .34f + if (index % 3 == 0) driftX * .15f else 0f,
                y = size.height * tile.y + (motion - .5f) * side * .46f + if (index % 3 == 1) driftY * .18f else 0f,
            )
            drawRoundRect(
                color = tile.color.copy(alpha = (if (index < 4) .16f else .11f) * pulse),
                topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * .25f, side * .25f),
            )
            val inset = side * .20f
            drawRoundRect(
                color = Color(0xFF0B1B33).copy(alpha = .16f),
                topLeft = Offset(center.x - side / 2f + inset, center.y - side / 2f + inset),
                size = Size(side - inset * 2f, side - inset * 2f),
                cornerRadius = CornerRadius(side * .14f, side * .14f),
            )
        }
    }
}
