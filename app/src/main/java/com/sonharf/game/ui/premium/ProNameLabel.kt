package com.sonharf.game.ui.premium

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Displays a player name without a visual PRO badge.
 * [isPro] stays in the API for source compatibility with existing callers.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ProNameLabel(
    name: String,
    isPro: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
) {
    Text(
        text = name,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
    )
}
