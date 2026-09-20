package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Single app-shell palette resolved from the Kelime Kuşatması design system. */
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
    val GreenSoft: Color get() = SonHarfTheme.Success.copy(alpha = .10f).compositeOver(SonHarfTheme.Surface)
    val Gold: Color get() = SonHarfTheme.PremiumGold
    val GoldSoft: Color get() = SonHarfTheme.PremiumGold.copy(alpha = .12f).compositeOver(SonHarfTheme.Surface)
    val Red: Color get() = SonHarfTheme.Error
    val Purple: Color get() = SonHarfTheme.Lavender
}

/** Reusable geometry. One change here propagates to the whole product shell. */
internal object MainUiShape {
    val Control = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(18.dp)
    val Hero = RoundedCornerShape(22.dp)
    val Modal = RoundedCornerShape(22.dp)
    val Tile = RoundedCornerShape(10.dp)
    val Pill = RoundedCornerShape(99.dp)
}

internal object MainUiSpace {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
}

internal object MainUiElevation {
    val Flat = 0.dp
    val Card = 1.dp
    val Dialog = 4.dp
}

// Independent/legacy mode tokens resolve to the same application-wide palette.
internal val PortalBg: Color get() = SonHarfTheme.Background
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
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = .15.sp,
    )
}

@Composable
internal fun MainSectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = MainUi.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = .15.sp,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onAction,
            color = MainUi.BlueSoft,
            shape = MainUiShape.Pill,
        ) {
            Text(
                text = action,
                color = MainUi.Blue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
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
                shadowElevation = MainUiElevation.Card,
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
            Text(title, color = MainUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MainUi.Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (actionIcon != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = MainUiShape.Control,
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
                shadowElevation = MainUiElevation.Card,
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
    MainGameCard(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, color = MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(label, color = MainUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun MainGameCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MainUiShape.Card,
        color = MainUi.Surface,
        border = BorderStroke(1.dp, MainUi.Border),
        shadowElevation = if (elevated) MainUiElevation.Dialog else MainUiElevation.Card,
    ) {
        Box(content = content)
    }
}

@Composable
internal fun MainGameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = MainUiShape.Control,
        colors = ButtonDefaults.buttonColors(
            containerColor = MainUi.Blue,
            contentColor = Color.White,
            disabledContainerColor = SonHarfTheme.DisabledBackground,
            disabledContentColor = SonHarfTheme.DisabledContent,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun MainSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MainUiShape.Control,
        border = BorderStroke(1.dp, MainUi.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MainUi.Blue),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun MainDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MainUiShape.Control,
        border = BorderStroke(1.dp, MainUi.Red.copy(alpha = .55f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MainUi.Red),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun MainBadge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MainUi.Blue,
) {
    Surface(modifier = modifier, shape = MainUiShape.Pill, color = accent.copy(alpha = .10f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun MainProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = MainUi.Blue,
    height: Dp = 6.dp,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.height(height),
        color = accent,
        trackColor = MainUi.SurfaceRaised,
    )
}
