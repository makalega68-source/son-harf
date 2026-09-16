package com.sonharf.game.ui.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Display a player name plus a Pro badge when [isPro] is true (G4.7).
 * The badge is sized to ~1.2x the surrounding text's line height so it
 * sits centered on the baseline no matter which style is passed.
 */
@Composable
fun ProNameLabel(
    name: String,
    isPro: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = style,
            color = color,
            maxLines = maxLines,
        )
        if (isPro) {
            Spacer(Modifier.width(6.dp))
            // Badge at 1.2x the surrounding font size.
            val badgeHeight = (style.fontSize.value.coerceAtLeast(12f) * 1.2f).toInt().coerceAtLeast(14)
            ProBadge(sizeDp = badgeHeight)
        }
    }
}
