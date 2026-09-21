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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared semantic palette. Both game modes and all shell screens resolve from one source. */
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
    val Control = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(16.dp)
    val Hero = RoundedCornerShape(20.dp)
    val Tile = RoundedCornerShape(9.dp)
    val Pill = RoundedCornerShape(99.dp)
}

// Compatibility tokens used by older screens, all bound to the same native theme.
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
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    SonHarfTheme.Background,
                    Color(0xFF171E2B),
                    SonHarfTheme.Background,
                ),
            ),
        ),
    ) {
        NativePackBackdrop(Modifier.matchParentSize())
    }
}

/** Atomic card primitive used by the rebuilt Native Android surfaces. */
@Composable
internal fun AppCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MainUiShape.Card,
        color = MainUi.Surface,
        border = BorderStroke(1.dp, accent?.copy(alpha = .30f) ?: MainUi.Border),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Atomic primary/secondary action primitive; default action is the approved mint green. */
@Composable
internal fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = SonHarfTheme.Primary,
    contentColor: Color = SonHarfTheme.OnPrimary,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = MainUiShape.Control,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SonHarfTheme.DisabledBackground,
            disabledContentColor = SonHarfTheme.DisabledContent,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = .25.sp)
    }
}

@Composable
internal fun PremiumCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) = AppCard(modifier = modifier, accent = accent, content = content)

@Composable
internal fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = AppButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)

@Composable
internal fun PremiumAccentPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = .14f),
        shape = MainUiShape.Pill,
        border = BorderStroke(1.dp, color.copy(alpha = .35f)),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

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
            color = MainUi.SurfaceSoft,
            shape = MainUiShape.Pill,
            border = BorderStroke(1.dp, MainUi.Border),
        ) {
            Text(
                text = action,
                color = SonHarfTheme.SoftBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
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
                    tint = SonHarfTheme.SoftBlue,
                    modifier = Modifier.padding(14.dp).size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MainUi.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        if (actionIcon != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = MainUiShape.Control,
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
                shadowElevation = 1.dp,
            ) {
                Icon(
                    actionIcon,
                    contentDescription = actionDescription,
                    tint = SonHarfTheme.SoftBlue,
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
        Column(Modifier.padding(14.dp)) {
            Text(value, color = MainUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(label, color = MainUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
