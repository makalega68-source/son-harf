package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SonHarfBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp? = 52.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(if (size != null) 56.dp else 68.dp),
            shape = CircleShape,
            color = Color(0xFFEAF3FF),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF3A81A),
                    modifier = Modifier.size(if (size != null) 30.dp else 38.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "KELİME\nTAHTI",
            color = Color(0xFF142B4F),
            fontSize = if (size != null) 22.sp else 34.sp,
            lineHeight = if (size != null) 24.sp else 34.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Start,
        )
    }
}
