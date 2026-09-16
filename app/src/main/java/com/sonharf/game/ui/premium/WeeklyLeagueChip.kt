package com.sonharf.game.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.WeeklyLeaguePositionDto
import com.sonharf.game.data.getWeeklyLeaguePosition

/**
 * G4.2 — Compact chip showing the caller's league + this-week delta.
 * Renders nothing until the first fetch completes. When Bronze, the
 * chip hides the down-arrow because Bronze doesn't demote (spec).
 */
@Composable
fun WeeklyLeagueChip(
    modifier: Modifier = Modifier,
    language: String = "tr",
    refreshTick: Int = 0,
) {
    val backend = remember { OnlineGameBackend() }
    var pos by remember { mutableStateOf<WeeklyLeaguePositionDto?>(null) }

    LaunchedEffect(refreshTick) {
        pos = runCatching { backend.getWeeklyLeaguePosition() }.getOrNull()
    }

    val p = pos ?: return
    val (arrow, color) = when {
        p.delta > 0 -> "▲" to SonHarfTheme.GoldBright
        p.delta < 0 && !p.isBronze -> "▼" to Color(0xFFE85555)
        else -> "•" to SonHarfTheme.PremiumTextSecondary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(SonHarfTheme.PremiumPanel.copy(alpha = 0.8f))
            .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = leagueLabel(p.leagueNow, language),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),
            color = leagueColor(p.leagueNow),
        )
        Text(
            text = arrow,
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black),
            color = color,
        )
        val deltaText = when {
            p.delta > 0 -> "+${p.delta}"
            p.delta < 0 -> "${p.delta}"
            else -> "±0"
        }
        Text(
            text = deltaText,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

private fun leagueLabel(name: String, language: String): String {
    if (language == "en") {
        return when (name) {
            "BRONZ" -> "BRONZE"
            "GÜMÜŞ" -> "SILVER"
            "ALTIN" -> "GOLD"
            "PLATİN" -> "PLATINUM"
            "ELMAS" -> "DIAMOND"
            "EFSANE" -> "LEGEND"
            else -> name
        }
    }
    return name
}

private fun leagueColor(name: String): Color = when (name) {
    "BRONZ" -> Color(0xFFCD7F32)
    "GÜMÜŞ" -> Color(0xFFC0C0C0)
    "ALTIN" -> SonHarfTheme.GoldBright
    "PLATİN" -> Color(0xFF9AE0FF)
    "ELMAS" -> SonHarfTheme.DiamondBlue
    "EFSANE" -> SonHarfTheme.KusatmaPurple
    else -> SonHarfTheme.PremiumTextPrimary
}
