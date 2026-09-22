package com.sonharf.game

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Production design tokens for the professional Kelime Kuşatması UI rebuild.
 *
 * This system intentionally does not inherit the previous yellow/cream theme or the later
 * Monster/neon theme. Gameplay/business logic remains outside this layer.
 */
internal object GameColors {
    val AppBackground = Color(0xFF101722)
    val ElevatedBackground = Color(0xFF151F2D)
    val PrimarySurface = Color(0xFF1C2939)
    val SecondarySurface = Color(0xFF243448)
    val LightSurface = Color(0xFFF5F7FA)

    val PrimaryBlue = Color(0xFF3D8BFF)
    val DeepBlue = Color(0xFF245DC1)
    val TacticalTurquoise = Color(0xFF20B6B0)
    val PlayGreen = Color(0xFF38C970)
    val PlayGreenDeep = Color(0xFF249C53)
    val Lavender = Color(0xFF9874E8)
    val RewardAmber = Color(0xFFF2A73B)
    val Danger = Color(0xFFE75D65)
    val PrestigeGold = Color(0xFFE8BC58)

    val TextPrimary = Color(0xFFF4F7FB)
    val TextSecondary = Color(0xFFA8B5C6)
    val TextTertiary = Color(0xFF728197)
    val TextDark = Color(0xFF17202D)
    val Border = Color(0xFF34475E)
    val Divider = Color(0xFF29394C)

    val HeroStart = Color(0xFF173664)
    val HeroMiddle = Color(0xFF174A66)
    val HeroEnd = Color(0xFF23526A)

    val Disabled = Color(0xFF2A3544)
    val DisabledContent = Color(0xFF728197)
}

internal object GameSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp

    val ScreenHorizontal = 16.dp
    val ScreenHorizontalNarrow = 12.dp
}

internal object GameShapes {
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(18.dp)
    val Hero = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(999.dp)
}

internal object GameElevation {
    val Flat = 0.dp
    val Low = 2.dp
    val Medium = 5.dp
    val High = 9.dp
}

internal val GameTypography = Typography(
    displaySmall = TextStyle(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
internal fun GameTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = GameColors.PrimaryBlue,
        secondary = GameColors.TacticalTurquoise,
        tertiary = GameColors.PlayGreen,
        background = GameColors.AppBackground,
        surface = GameColors.PrimarySurface,
        surfaceVariant = GameColors.SecondarySurface,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = GameColors.TextPrimary,
        onSurface = GameColors.TextPrimary,
        onSurfaceVariant = GameColors.TextSecondary,
        error = GameColors.Danger,
        onError = Color.White,
        outline = GameColors.Border,
        outlineVariant = GameColors.Divider,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = GameTypography,
        content = content,
    )
}
