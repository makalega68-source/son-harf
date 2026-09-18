package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared app-shell palette, resolved from the single Kelime Kuşatması theme source. */
internal object MainUi {
    // Default pages stay slightly translucent so the purchased-kit-inspired vector backdrop can
    // provide depth without becoming a full-screen image layer.
    val Background: Color get() = if (SonHarfTheme.IsDark) SonHarfTheme.Background else SonHarfTheme.Background.copy(alpha = .95f)
    val Surface: Color get() = SonHarfTheme.Surface
    val SurfaceSoft: Color get() = SonHarfTheme.SurfaceSecondary
    val SurfaceRaised: Color get() = SonHarfTheme.SurfaceElevated
    val Navigation: Color get() = SonHarfTheme.NavigationSurface
    val Modal: Color get() = SonHarfTheme.ModalSurface
    val GameSurface: Color get() = SonHarfTheme.GameSurface
    val Tile: Color get() = SonHarfTheme.GameTile
    val TileBorder: Color get() = SonHarfTheme.GameTileBorder
    val Text: Color get() = SonHarfTheme.TextPrimary
    val Muted: Color get() = SonHarfTheme.TextSecondary
    val Blue: Color get() = SonHarfTheme.Primary
    val BlueDeep: Color get() = SonHarfTheme.ForestDeep
    val BlueSoft: Color get() = SonHarfTheme.PrimarySoft
    val GrayBlue: Color get() = SonHarfTheme.SoftBlue
    val Cyan: Color get() = SonHarfTheme.Turquoise
    val Orange: Color get() = SonHarfTheme.ActionOrange
    val Border: Color get() = SonHarfTheme.Border
    val Green: Color get() = SonHarfTheme.Success
    val Gold: Color get() = SonHarfTheme.PremiumGold
    val Red: Color get() = SonHarfTheme.Error
    val Purple: Color get() = SonHarfTheme.Lavender
}

/** Compact radii mirror the purchased sports-dashboard kit while remaining touch-friendly. */
internal object MainUiShape {
    val Control = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(18.dp)
    val Hero = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(99.dp)
}

// Independent/legacy mode tokens resolve to the same application-wide palette.
internal val PortalBg: Color get() = if (SonHarfTheme.IsDark) SonHarfTheme.Background else SonHarfTheme.Background.copy(alpha = .95f)
internal val PortalCard: Color get() = SonHarfTheme.Surface
internal val PortalText: Color get() = SonHarfTheme.TextPrimary
internal val PortalMuted: Color get() = SonHarfTheme.TextSecondary
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = MainUiShape.Control,
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
                shadowElevation = 1.dp,
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = sh("Geri", "Back"),
                    tint = MainUi.Blue,
                    modifier = Modifier.padding(14.dp).size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = MainUi.Muted, fontSize = 10.sp)
        }
        if (actionIcon != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = MainUiShape.Control,
                color = MainUi.SurfaceSoft,
                border = BorderStroke(1.dp, MainUi.Border),
                shadowElevation = 1.dp,
            ) {
                Icon(
                    actionIcon,
                    contentDescription = actionDescription,
                    tint = MainUi.Blue,
                    modifier = Modifier.padding(14.dp).size(20.dp),
                )
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
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(5.dp)
                        .padding(0.dp),
                )
                Text(value, color = MainUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(3.dp))
            Text(label, color = MainUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
