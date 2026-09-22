package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Decorative first-run language backdrop for the professional production theme.
 *
 * The center remains quiet so the real Compose logo, language chips and continue button stay
 * readable and interactive. Decoration is edge-only and contains no embedded text or controls.
 */
@Composable
internal fun FirstRunLanguageBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to GameColors.AppBackground,
                    0.42f to GameColors.ElevatedBackground,
                    0.78f to Color(0xFF132132),
                    1.00f to Color(0xFF101A28),
                ),
            ),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GameColors.PrimaryBlue.copy(alpha = .18f), Color.Transparent),
                center = Offset(size.width * 1.02f, size.height * .16f),
                radius = size.minDimension * .62f,
            ),
            radius = size.minDimension * .62f,
            center = Offset(size.width * 1.02f, size.height * .16f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GameColors.TacticalTurquoise.copy(alpha = .15f), Color.Transparent),
                center = Offset(size.width * -.04f, size.height * .80f),
                radius = size.minDimension * .68f,
            ),
            radius = size.minDimension * .68f,
            center = Offset(size.width * -.04f, size.height * .80f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GameColors.Lavender.copy(alpha = .11f), Color.Transparent),
                center = Offset(size.width * .96f, size.height * .86f),
                radius = size.minDimension * .46f,
            ),
            radius = size.minDimension * .46f,
            center = Offset(size.width * .96f, size.height * .86f),
        )

        data class Tile(
            val x: Float,
            val y: Float,
            val scale: Float,
            val angle: Float,
            val color: Color,
            val alpha: Float,
        )

        val blue = GameColors.PrimaryBlue
        val deepBlue = GameColors.DeepBlue
        val turquoise = GameColors.TacticalTurquoise
        val green = GameColors.PlayGreen
        val amber = GameColors.RewardAmber
        val lavender = GameColors.Lavender

        listOf(
            Tile(.035f, .085f, .082f, -12f, turquoise, .20f),
            Tile(.135f, .035f, .050f, 8f, blue, .15f),
            Tile(.925f, .075f, .074f, 14f, blue, .20f),
            Tile(.985f, .205f, .050f, -9f, turquoise, .16f),
            Tile(.015f, .305f, .046f, 10f, amber, .13f),
            Tile(.970f, .405f, .055f, -14f, deepBlue, .17f),
            Tile(.030f, .650f, .072f, 13f, green, .15f),
            Tile(.115f, .755f, .042f, -7f, blue, .12f),
            Tile(.965f, .675f, .066f, -11f, lavender, .16f),
            Tile(.900f, .825f, .043f, 9f, amber, .12f),
            Tile(.045f, .915f, .058f, -8f, turquoise, .14f),
            Tile(.830f, .955f, .050f, 11f, blue, .11f),
        ).forEach { tile ->
            drawBackdropTile(tile.x, tile.y, tile.scale, tile.angle, tile.color, tile.alpha)
        }

        listOf(
            .055f to .195f,
            .950f to .300f,
            .070f to .505f,
            .945f to .555f,
            .060f to .825f,
            .930f to .930f,
        ).forEachIndexed { index, (x, y) ->
            val side = size.minDimension * if (index % 2 == 0) .018f else .014f
            drawRoundRect(
                color = GameColors.TextSecondary.copy(alpha = if (index % 2 == 0) .13f else .08f),
                topLeft = Offset(size.width * x - side / 2f, size.height * y - side / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * .28f, side * .28f),
            )
        }
    }
}

private fun DrawScope.drawBackdropTile(
    x: Float,
    y: Float,
    scale: Float,
    angle: Float,
    color: Color,
    alpha: Float,
) {
    val side = size.minDimension * scale
    val center = Offset(size.width * x, size.height * y)
    rotate(angle, pivot = center) {
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
            size = Size(side, side),
            cornerRadius = CornerRadius(side * .22f, side * .22f),
        )
        val inset = side * .19f
        drawRoundRect(
            color = GameColors.TextPrimary.copy(alpha = alpha * .22f),
            topLeft = Offset(center.x - side / 2f + inset, center.y - side / 2f + inset),
            size = Size(side - inset * 2f, side - inset * 2f),
            cornerRadius = CornerRadius(side * .12f, side * .12f),
        )
    }
}
