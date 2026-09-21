package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared logical color tokens. Visual chrome is supplied by PurchasedGameTheme assets. */
internal object MainUi {
    val Background: Color get() = SonHarfTheme.Background
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

internal object MainUiShape {
    val Control = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(20.dp)
    val Hero = RoundedCornerShape(28.dp)
    val Tile = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(99.dp)
}

internal val PortalBg: Color get() = SonHarfTheme.Background
internal val PortalCard: Color get() = SonHarfTheme.Surface
internal val PortalText: Color get() = SonHarfTheme.TextPrimary
internal val PortalMuted: Color get() = SonHarfTheme.TextSecondary
internal val PortalBlue: Color get() = SonHarfTheme.Primary
internal val PortalGold: Color get() = SonHarfTheme.PremiumGold
internal val PortalGreen: Color get() = SonHarfTheme.Success
internal val PortalRed: Color get() = SonHarfTheme.Error

@Composable
internal fun PremiumScreenBackground(modifier: Modifier = Modifier) {
    PurchasedGameBackdrop(modifier)
}

/** Real purchased panel wrapper; no Material Card/Surface skin is drawn here. */
@Composable
internal fun PremiumCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    PurchasedPanel(
        modifier = modifier,
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
internal fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PurchasedButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = PurchasedButtonStyle.PRIMARY,
        enabled = enabled,
    )
}

@Composable
internal fun PremiumAccentPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 34.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            color = Color(0xFF62442E),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .35.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun MainSectionTitle(title: String) {
    PurchasedSectionHeader(title = title)
}

@Composable
internal fun MainSectionTitle(title: String, action: String, onAction: () -> Unit) {
    PurchasedSectionHeader(title = title, action = action, onAction = onAction)
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
            PurchasedIconButton(
                asset = PurchasedUiAsset.ICON_REPEAT,
                onClick = onBack,
                modifier = Modifier.size(54.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        PurchasedPanel(
            modifier = Modifier.weight(1f).heightIn(min = 76.dp),
            asset = PurchasedUiAsset.BANNER,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = Color.White.copy(alpha = .92f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
        if (actionIcon != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            PurchasedIconButton(
                asset = PurchasedUiAsset.ICON_SETTINGS,
                onClick = onAction,
                modifier = Modifier.size(54.dp),
            )
        }
    }
}

@Composable
internal fun MainMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 92.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color(0xFF563A2A), fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(label, color = Color(0xFF7C5D48), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
