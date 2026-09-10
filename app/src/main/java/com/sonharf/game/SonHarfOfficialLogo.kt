package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * User-facing Son Harf SH monogram.
 * Pure Compose by design: no raster decode path can affect startup or navigation.
 */
@Composable
fun SonHarfOfficialLogo(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val gap = (maxHeight.value * 0.08f).coerceIn(6f, 12f).dp
        val corner = (maxHeight.value * 0.18f).coerceIn(10f, 24f).dp
        val letterSize = (maxHeight.value * 0.46f).coerceIn(22f, 62f).sp
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SonHarfMonogramTile(
                letter = "S",
                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                corner = corner,
                letterSize = letterSize.value,
            )
            SonHarfMonogramTile(
                letter = "H",
                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                corner = corner,
                letterSize = letterSize.value,
            )
        }
    }
}

@Composable
private fun SonHarfMonogramTile(
    letter: String,
    modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp,
    letterSize: Float,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        color = Color(0xFFF9F5E8),
        border = BorderStroke(1.5.dp, Color(0xFF5E8069)),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                color = Color(0xFF3F614E),
                fontSize = letterSize.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
