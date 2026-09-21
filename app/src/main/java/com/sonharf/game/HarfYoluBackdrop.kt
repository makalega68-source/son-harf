package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Harf Yolu now uses the same new Siege Royale family, with a quieter puzzle-specific backdrop. */
@Composable
internal fun HarfYoluBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val dark = SonHarfTheme.IsDark
        val top = if (dark) Color(0xFF101D33) else Color(0xFFE2EEFF)
        val middle = if (dark) Color(0xFF13243C) else Color(0xFFF9FCFF)
        val bottom = if (dark) Color(0xFF0D192C) else Color(0xFFEAF3FF)
        drawRect(brush = Brush.verticalGradient(listOf(top, middle, bottom)))

        val blue = SonHarfTheme.Primary
        val aqua = SonHarfTheme.Turquoise
        val violet = SonHarfTheme.Purple
        val amber = SonHarfTheme.ActionOrange

        drawCircle(
            brush = Brush.radialGradient(
                listOf(aqua.copy(alpha = if (dark) .18f else .13f), Color.Transparent),
                center = Offset(size.width * .92f, size.height * .13f),
                radius = size.minDimension * .58f,
            ),
            radius = size.minDimension * .58f,
            center = Offset(size.width * .92f, size.height * .13f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(blue.copy(alpha = if (dark) .15f else .10f), Color.Transparent),
                center = Offset(size.width * .08f, size.height * .84f),
                radius = size.minDimension * .60f,
            ),
            radius = size.minDimension * .60f,
            center = Offset(size.width * .08f, size.height * .84f),
        )

        data class Marker(val x: Float, val y: Float, val side: Float, val color: Color, val alpha: Float)
        val markers = listOf(
            Marker(.035f, .12f, 10f, blue, if (dark) .20f else .14f),
            Marker(.090f, .21f, 7f, aqua, if (dark) .18f else .11f),
            Marker(.965f, .10f, 9f, violet, if (dark) .18f else .12f),
            Marker(.940f, .30f, 6f, amber, if (dark) .16f else .10f),
            Marker(.030f, .62f, 7f, violet, if (dark) .15f else .09f),
            Marker(.960f, .74f, 8f, blue, if (dark) .16f else .10f),
        )
        markers.forEach { marker ->
            drawRoundRect(
                color = marker.color.copy(alpha = marker.alpha),
                topLeft = Offset(size.width * marker.x - marker.side / 2f, size.height * marker.y - marker.side / 2f),
                size = Size(marker.side, marker.side),
                cornerRadius = CornerRadius(marker.side * .28f, marker.side * .28f),
            )
        }
    }
}
