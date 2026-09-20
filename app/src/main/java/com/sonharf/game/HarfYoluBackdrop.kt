package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Harf Yolu background aligned with the shared mature word-game design system.
 * It is intentionally static: gameplay keeps the visual priority and low/mid devices avoid a
 * permanent animation cost. Decorative tiles stay at the edges and never carry embedded text.
 */
@Composable
internal fun HarfYoluBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val paper = Color(0xFFF6F4EE)
        val surface = Color(0xFFFFFEFA)
        val sage = Color(0xFF77977F)
        val forest = Color(0xFF285943)
        val mistBlue = Color(0xFF6F8794)
        val gold = Color(0xFFB58A39)

        drawRect(
            brush = Brush.verticalGradient(
                listOf(surface, paper, Color(0xFFF0F2EC)),
            ),
        )

        // Quiet edge glows separate the puzzle from the app shell without an arcade look.
        val topRight = Offset(size.width * .94f, size.height * .12f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mistBlue.copy(alpha = .09f), Color.Transparent),
                center = topRight,
                radius = size.minDimension * .46f,
            ),
            radius = size.minDimension * .46f,
            center = topRight,
        )
        val bottomLeft = Offset(size.width * .06f, size.height * .82f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(sage.copy(alpha = .10f), Color.Transparent),
                center = bottomLeft,
                radius = size.minDimension * .52f,
            ),
            radius = size.minDimension * .52f,
            center = bottomLeft,
        )

        data class EdgeTile(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        listOf(
            EdgeTile(.035f, .10f, 13f, forest, .10f),
            EdgeTile(.095f, .055f, 8f, sage, .10f),
            EdgeTile(.955f, .16f, 11f, mistBlue, .10f),
            EdgeTile(.975f, .42f, 8f, gold, .08f),
            EdgeTile(.025f, .58f, 10f, sage, .08f),
            EdgeTile(.935f, .78f, 12f, forest, .08f),
            EdgeTile(.085f, .93f, 8f, gold, .07f),
        ).forEach { tile ->
            drawRoundRect(
                color = tile.color.copy(alpha = tile.alpha),
                topLeft = Offset(size.width * tile.x - tile.side / 2f, size.height * tile.y - tile.side / 2f),
                size = Size(tile.side, tile.side),
                cornerRadius = CornerRadius(tile.side * .22f, tile.side * .22f),
            )
        }
    }
}
