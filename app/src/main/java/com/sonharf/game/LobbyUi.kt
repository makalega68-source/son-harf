package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Menu materials only. Board, keyboard and purchased cosmetic renderers keep their own tokens. */
internal object LobbyPalette {
    val Ground: Color get() = if (SonHarfCosmetics.walnutTheme) Hf.Ground else HomeLobbyStyle.Ground
    val Paper: Color get() = if (SonHarfCosmetics.walnutTheme) Hf.Ivory else HomeLobbyStyle.Paper
    val Ink: Color get() = if (SonHarfCosmetics.walnutTheme) Hf.Text else HomeLobbyStyle.Ink
    val Muted: Color get() = if (SonHarfCosmetics.walnutTheme) Hf.TextMuted else if (SonHarfTheme.IsDark) HomeLobbyStyle.Muted else Color(0xFF6D6B59)
    val Line: Color get() = if (SonHarfCosmetics.walnutTheme) Hf.Border else HomeLobbyStyle.Line
    val Gold: Color get() = HomeLobbyStyle.Gold
    val Green = HomeLobbyStyle.Green
    val Accent: Color get() = if (SonHarfTheme.IsDark) Color(0xFF9ED4AF) else Green
    val Soft: Color get() = if (SonHarfTheme.IsDark) Color(0xFF304332) else Color(0xFFE5ECD9)
}

@Composable
internal fun LobbyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = LobbyPalette.Line,
    color: Color = LobbyPalette.Paper,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Surface(modifier = modifier, color = color, contentColor = LobbyPalette.Ink, shape = shape,
            border = BorderStroke(1.dp, borderColor), shadowElevation = 1.dp, content = content)
    } else {
        Surface(onClick = onClick, modifier = modifier, color = color, contentColor = LobbyPalette.Ink,
            shape = shape, border = BorderStroke(1.dp, borderColor), shadowElevation = 1.dp, content = content)
    }
}

/** Natural-width tabs stay readable on small phones and with larger system type. */
@Composable
internal fun LobbyTabs(
    labels: List<String>, selected: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Surface(onClick = { onSelect(index) }, shape = RoundedCornerShape(14.dp),
                color = if (active) LobbyPalette.Green else LobbyPalette.Paper,
                border = BorderStroke(1.dp, if (active) LobbyPalette.Green else LobbyPalette.Line)) {
                Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center) {
                    Text(label, color = if (active) Color.White else LobbyPalette.Ink,
                        fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}
