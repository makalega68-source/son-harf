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
 * Legacy function name kept only for call-site compatibility.
 * The old leaf/sage backdrop is gone; this is the new abstract Siege Royale game backdrop.
 */
@Composable
internal fun SonHarfLeafBackdrop(modifier: Modifier = Modifier) {
    val dark = SonHarfTheme.IsDark
    Canvas(modifier) {
        val top = if (dark) Color(0xFF0A1730) else Color(0xFFD8E9FF)
        val middle = if (dark) Color(0xFF0E203B) else Color(0xFFF7FBFF)
        val bottom = if (dark) Color(0xFF0B172C) else Color(0xFFEAF3FF)
        drawRect(brush = Brush.verticalGradient(listOf(top, middle, bottom)))

        val blue = SonHarfTheme.Primary
        val aqua = SonHarfTheme.Turquoise
        val gold = SonHarfTheme.PremiumGold
        val violet = SonHarfTheme.Purple

        drawCircle(
            brush = Brush.radialGradient(
                listOf(blue.copy(alpha = if (dark) .24f else .18f), Color.Transparent),
                center = Offset(size.width * .88f, size.height * .12f),
                radius = size.minDimension * .70f,
            ),
            radius = size.minDimension * .70f,
            center = Offset(size.width * .88f, size.height * .12f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(aqua.copy(alpha = if (dark) .18f else .13f), Color.Transparent),
                center = Offset(size.width * .08f, size.height * .80f),
                radius = size.minDimension * .62f,
            ),
            radius = size.minDimension * .62f,
            center = Offset(size.width * .08f, size.height * .80f),
        )

        data class Marker(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        val markers = listOf(
            Marker(.035f, .12f, 12f, blue, if (dark) .22f else .16f),
            Marker(.080f, .20f, 7f, aqua, if (dark) .20f else .13f),
            Marker(.955f, .10f, 10f, gold, if (dark) .22f else .16f),
            Marker(.925f, .24f, 7f, violet, if (dark) .17f else .11f),
            Marker(.028f, .48f, 7f, gold, if (dark) .16f else .10f),
            Marker(.966f, .56f, 8f, aqua, if (dark) .18f else .12f),
            Marker(.060f, .79f, 7f, violet, if (dark) .17f else .10f),
            Marker(.935f, .81f, 8f, blue, if (dark) .17f else .11f),
        )
        markers.forEach { marker ->
            drawRoundRect(
                color = marker.color.copy(alpha = marker.alpha),
                topLeft = Offset(size.width * marker.x - marker.side / 2f, size.height * marker.y - marker.side / 2f),
                size = Size(marker.side, marker.side),
                cornerRadius = CornerRadius(marker.side * .30f, marker.side * .30f),
            )
        }
    }
}
