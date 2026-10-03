package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared app-shell palette, resolved from the single Kelime Tahtı theme source. */
internal object MainUi {
    val Background: Color get() = LobbyPalette.Ground
    val Surface: Color get() = LobbyPalette.Paper
    val SurfaceSoft: Color get() = LobbyPalette.Paper
    val SurfaceRaised: Color get() = LobbyPalette.Soft
    val Navigation: Color get() = LobbyPalette.Paper
    val Modal: Color get() = LobbyPalette.Paper
    val GameSurface: Color get() = SonHarfTheme.GameSurface
    val Tile: Color get() = SonHarfTheme.GameTile
    val TileBorder: Color get() = SonHarfTheme.GameTileBorder
    val Text: Color get() = LobbyPalette.Ink
    val Muted: Color get() = LobbyPalette.Muted
    val Blue: Color get() = LobbyPalette.Green
    val BlueDeep: Color get() = SonHarfTheme.ForestDeep
    val BlueSoft: Color get() = LobbyPalette.Soft
    val GrayBlue: Color get() = SonHarfTheme.SoftBlue
    val Cyan: Color get() = SonHarfTheme.Turquoise
    val Orange: Color get() = SonHarfTheme.ActionOrange
    val Border: Color get() = LobbyPalette.Line
    val Green: Color get() = SonHarfTheme.Success
    val Gold: Color get() = LobbyPalette.Gold
    val Red: Color get() = SonHarfTheme.Error
    val Purple: Color get() = SonHarfTheme.Lavender
}

/** Bütün ekranlarda aynı simetrik köşe ve kontrol ölçüleri kullanılır. */
internal object MainUiShape {
    val Control = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(16.dp)
    val Hero = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(99.dp)
}

// Independent/legacy mode tokens resolve to the same application-wide palette.
internal val PortalBg: Color get() = if (SonHarfTheme.IsDark) SonHarfTheme.Background else SonHarfTheme.Background.copy(alpha = .95f)
internal val PortalCard: Color get() = SonHarfTheme.Surface
internal val PortalText: Color get() = LobbyPalette.Ink
internal val PortalMuted: Color get() = LobbyPalette.Muted
internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue
internal val PortalGold: Color get() = SonHarfTheme.PremiumGold
internal val PortalGreen: Color get() = SonHarfTheme.Success
internal val PortalRed: Color get() = SonHarfTheme.Error

@Composable
internal fun MainSectionTitle(title: String) {
    Text(
        text = title,
        color = MainUi.Text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = .35.sp,
    )
}

@Composable
internal fun MainSectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = MainUi.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .35.sp,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onAction,
            color = MainUi.Orange.copy(alpha = .11f),
            shape = MainUiShape.Pill,
        ) {
            Text(
                text = action,
                color = MainUi.Orange,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
internal fun MainScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String = "",
    onAction: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) {
            Surface(shape = RoundedCornerShape(16.dp), color = LobbyPalette.Paper,
                border = BorderStroke(1.dp, LobbyPalette.Line)) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, sh("Geri", "Back"), tint = LobbyPalette.Ink)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = LobbyPalette.Ink, fontSize = 27.sp, lineHeight = 31.sp,
                fontWeight = FontWeight.Black)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = LobbyPalette.Muted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        if (actionIcon != null && onAction != null) {
            IconButton(onClick = onAction, modifier = Modifier.size(48.dp)) {
                Icon(actionIcon, actionDescription, tint = LobbyPalette.Gold)
            }
        }
    }
}

@Composable
internal fun MainMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MainUiShape.Card,
        color = MainUi.Surface,
        border = BorderStroke(1.dp, MainUi.Border),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = MainUi.Text,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = MainUi.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
