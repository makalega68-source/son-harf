package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Startup-safe Son Harf wordmark.
 *
 * Keep this logo independent from raster decoding so a damaged drawable can never block app launch.
 * The launcher icon has the same palette in a vector drawable.
 */
@Composable
fun SonHarfBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp? = 52.dp,
) {
    val logoModifier = if (size == null) {
        modifier.aspectRatio(2.15f)
    } else {
        modifier.height(size)
    }

    Surface(
        modifier = logoModifier,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF9F5E8),
        border = BorderStroke(2.dp, Color(0xFF5E8069)),
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SON",
                color = Color(0xFF3F614E),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                text = " HARF",
                color = Color(0xFF4A6E83),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}
