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

/** Harf Yolu's restrained professional intelligence/puzzle backdrop. */
@Composable
internal fun HarfYoluBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "harfYoluBackdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "harfYoluBackdropPhase",
    )
    val pulse by transition.animateFloat(
        initialValue = .72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "harfYoluBackdropPulse",
    )

    Canvas(modifier = modifier) {
        val blue = GameColors.PrimaryBlue
        val turquoise = GameColors.TacticalTurquoise
        val lavender = GameColors.Lavender
        val amber = GameColors.RewardAmber

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    GameColors.AppBackground,
                    GameColors.ElevatedBackground,
                    GameColors.AppBackground,
                ),
            ),
        )

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

        glow(blue, .88f + .012f * phase, .10f, .58f, .12f)
        glow(turquoise, .08f - .010f * phase, .70f, .62f, .11f)
        glow(lavender, .92f, .84f - .010f * phase, .42f, .08f)
        glow(amber, .05f, .34f + .008f * phase, .32f, .045f)

        data class Tile(val x: Float, val y: Float, val scale: Float, val color: Color)
        val tiles = listOf(
            Tile(.04f, .12f, .050f, blue),
            Tile(.15f, .055f, .030f, lavender),
            Tile(.94f, .11f, .048f, turquoise),
            Tile(.98f, .30f, .028f, amber),
            Tile(.03f, .65f, .043f, turquoise),
            Tile(.96f, .72f, .039f, blue),
            Tile(.10f, .89f, .028f, lavender),
            Tile(.88f, .93f, .030f, amber),
        )

        tiles.forEachIndexed { index, tile ->
            val side = size.minDimension * tile.scale
            val motion = if (index % 2 == 0) phase else 1f - phase
            val center = Offset(
                x = size.width * tile.x + (motion - .5f) * side * .28f,
                y = size.height * tile.y + (motion - .5f) * side * .38f,
            )
            drawRoundRect(
                color = tile.color.copy(alpha = .105f * pulse),
                topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * .24f, side * .24f),
            )
            val inset = side * .22f
            drawRoundRect(
                color = GameColors.TextPrimary.copy(alpha = .035f),
                topLeft = Offset(center.x - side / 2f + inset, center.y - side / 2f + inset),
                size = Size(side - inset * 2f, side - inset * 2f),
                cornerRadius = CornerRadius(side * .12f, side * .12f),
            )
        }
    }
}
