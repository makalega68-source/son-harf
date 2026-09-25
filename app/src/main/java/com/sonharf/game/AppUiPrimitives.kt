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
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ChevronLeft,
                    contentDescription = sh("Geri", "Back"),
                    tint = MainUi.Gold,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = MainUi.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MainUi.Muted,
                fontSize = 10.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp),
            ) {
                Icon(
                    actionIcon,
                    contentDescription = actionDescription,
                    tint = MainUi.Gold,
                    modifier = Modifier.size(26.dp),
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
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
