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

/** Harf Yolu'na özel, mavi-turkuaz-beyaz hareketli arka plan. */
@Composable
internal fun HarfYoluBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "harfYoluBackdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "harfYoluBackdropPhase",
    )

    Canvas(modifier = modifier) {
        val blue = Color(0xFF278DC3)
        val turquoise = Color(0xFF22BFC4)
        val paleBlue = Color(0xFFEAF8FC)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFF7FCFE),
                    paleBlue.copy(alpha = .72f),
                    Color.White,
                ),
            ),
        )

        val driftX = size.width * (.018f * phase)
        val driftY = size.height * (.014f * phase)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blue.copy(alpha = .16f), Color.Transparent),
                center = Offset(size.width * .92f + driftX, size.height * .15f + driftY),
                radius = size.minDimension * .58f,
            ),
            radius = size.minDimension * .58f,
            center = Offset(size.width * .92f + driftX, size.height * .15f + driftY),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(turquoise.copy(alpha = .17f), Color.Transparent),
                center = Offset(size.width * .06f - driftX, size.height * .77f - driftY),
                radius = size.minDimension * .64f,
            ),
            radius = size.minDimension * .64f,
            center = Offset(size.width * .06f - driftX, size.height * .77f - driftY),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = .95f), Color.Transparent),
                center = Offset(size.width * .50f, size.height * .46f),
                radius = size.minDimension * .52f,
            ),
            radius = size.minDimension * .52f,
            center = Offset(size.width * .50f, size.height * .46f),
        )

        val tiles = listOf(
            Triple(.03f, .11f, .060f),
            Triple(.13f, .055f, .035f),
            Triple(.94f, .09f, .056f),
            Triple(.985f, .25f, .035f),
            Triple(.02f, .34f, .030f),
            Triple(.97f, .46f, .034f),
            Triple(.035f, .67f, .052f),
            Triple(.96f, .72f, .046f),
            Triple(.10f, .89f, .034f),
            Triple(.88f, .93f, .040f),
        )
        tiles.forEachIndexed { index, (x, y, scale) ->
            val side = size.minDimension * scale
            val motion = if (index % 2 == 0) phase else 1f - phase
            val center = Offset(
                x = size.width * x + (motion - .5f) * side * .30f,
                y = size.height * y + (motion - .5f) * side * .42f,
            )
            val color = if (index % 2 == 0) blue else turquoise
            drawRoundRect(
                color = color.copy(alpha = if (index < 3) .15f else .10f),
                topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * .25f, side * .25f),
            )
            val inset = side * .20f
            drawRoundRect(
                color = Color.White.copy(alpha = .48f),
                topLeft = Offset(center.x - side / 2f + inset, center.y - side / 2f + inset),
                size = Size(side - inset * 2f, side - inset * 2f),
                cornerRadius = CornerRadius(side * .14f, side * .14f),
            )
        }
    }
}
