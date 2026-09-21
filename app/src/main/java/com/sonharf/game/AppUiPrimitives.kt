package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared application palette. Every mode resolves from the new Siege Royale theme source. */
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

/** Larger, game-oriented radius hierarchy. */
internal object MainUiShape {
    val Control = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(20.dp)
    val Hero = RoundedCornerShape(28.dp)
    val Tile = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(99.dp)
}

// Legacy tokens intentionally resolve to the same application-wide palette.
internal val PortalBg: Color get() = SonHarfTheme.Background
internal val PortalCard: Color get() = SonHarfTheme.Surface
internal val PortalText: Color get() = SonHarfTheme.TextPrimary
internal val PortalMuted: Color get() = SonHarfTheme.TextSecondary
internal val PortalBlue: Color get() = SonHarfTheme.Primary
internal val PortalGold: Color get() = SonHarfTheme.PremiumGold
internal val PortalGreen: Color get() = SonHarfTheme.Success
internal val PortalRed: Color get() = SonHarfTheme.Error

/** Text-free background: bright game lobby chrome without fantasy scenery or embedded copy. */
@Composable
internal fun PremiumScreenBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    if (SonHarfTheme.IsDark) Color(0xFF0A1730) else Color(0xFFD8E9FF),
                    SonHarfTheme.Background,
                    if (SonHarfTheme.IsDark) Color(0xFF0E1D36) else Color(0xFFF6FAFF),
                    SonHarfTheme.Background,
                ),
            ),
        ),
    )
}

/** Glossy game panel inspired by the purchased UI pack, rendered natively so text stays real UI. */
@Composable
internal fun PremiumCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val edge = accent ?: SonHarfTheme.Primary
    Surface(
        modifier = modifier,
        shape = MainUiShape.Card,
        color = Color.Transparent,
        border = BorderStroke(1.dp, edge.copy(alpha = if (SonHarfTheme.IsDark) .34f else .22f)),
        shadowElevation = 5.dp,
    ) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SonHarfTheme.Surface,
                            SonHarfTheme.SurfaceElevated,
                            SonHarfTheme.SurfaceSecondary.copy(alpha = if (SonHarfTheme.IsDark) .55f else .68f),
                        ),
                    ),
                )
                .padding(16.dp),
            content = content,
        )
    }
}

/** Main CTA uses the new emerald game-action gradient instead of the old flat sage button. */
@Composable
internal fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = MainUiShape.Control,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (enabled) SonHarfTheme.PlayGreenDeep.copy(alpha = .82f) else SonHarfTheme.Border,
        ),
        shadowElevation = if (enabled) 6.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) {
                        Brush.verticalGradient(listOf(SonHarfTheme.PlayGreen, SonHarfTheme.PlayGreenDeep))
                    } else {
                        Brush.verticalGradient(listOf(SonHarfTheme.DisabledBackground, SonHarfTheme.DisabledBackground))
                    },
                    MainUiShape.Control,
                )
                .padding(horizontal = 18.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) Color.White else SonHarfTheme.DisabledContent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .45.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun PremiumAccentPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = MainUiShape.Pill,
        border = BorderStroke(1.dp, color.copy(alpha = .34f)),
        shadowElevation = 2.dp,
    ) {
        Box(
            Modifier.background(
                Brush.horizontalGradient(
                    listOf(color.copy(alpha = .18f), color.copy(alpha = .07f)),
                ),
                MainUiShape.Pill,
            ),
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .45.sp,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
internal fun MainSectionTitle(title: String) {
    Text(
        text = title,
        color = MainUi.Text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = .45.sp,
    )
}

@Composable
internal fun MainSectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = MainUi.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .45.sp,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onAction,
            color = SonHarfTheme.PrimarySoft,
            shape = MainUiShape.Pill,
            border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .22f)),
        ) {
            Text(
                text = action,
                color = MainUi.Blue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                color = SonHarfTheme.PrimarySoft,
                border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .22f)),
                shadowElevation = 3.dp,
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
            Text(title, color = MainUi.Text, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MainUi.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        if (actionIcon != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = MainUiShape.Control,
                color = SonHarfTheme.Surface,
                border = BorderStroke(1.dp, SonHarfTheme.Border),
                shadowElevation = 3.dp,
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
        color = Color.Transparent,
        border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .16f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(listOf(SonHarfTheme.Surface, SonHarfTheme.SurfaceElevated)),
                )
                .padding(14.dp),
        ) {
            Text(value, color = MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(label, color = MainUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
