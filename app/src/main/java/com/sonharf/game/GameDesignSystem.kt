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
 * Uses the navy professional palette; the purchased Black Theme switches to a deeper black one.
 * Gameplay/business logic remains outside this layer.
 */
/** Default navy palette of the professional shell. */
internal object GameNavyPalette {
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
    val SonHarfBackground = Color(0xFFF3EEE5)
    val SonHarfSurface = Color(0xFFFFFBF4)
    val SonHarfInk = Color(0xFF173247)
    val SonHarfMuted = Color(0xFF6F7B7C)
    val SonHarfOcean = Color(0xFF4F8F96)
    val SonHarfOceanDeep = Color(0xFF2F6970)
    val SonHarfSky = Color(0xFF8EB7B5)
    val SonHarfIce = Color(0xFFE6EFEB)
    val SonHarfBorder = Color(0xFFD8D0C4)
    val SonHarfGreen = Color(0xFF789B73)
    val SonHarfGreenSoft = Color(0xFFE5ECDD)
    val SonHarfRed = Color(0xFFC86459)
    val SonHarfRedSoft = Color(0xFFF4DDD7)
    val SonHarfGold = Color(0xFFD1A13E)
    val SonHarfGoldSoft = Color(0xFFF5E8BB)
    val SonHarfRival = Color(0xFFD27869)
    val SonHarfRivalSoft = Color(0xFFF6E2DC)
    val Disabled = Color(0xFF2A3544)
    val DisabledContent = Color(0xFF728197)
}

/** Deeper black palette used when the player equips the purchased Black Theme (theme_black). */
internal object GameDarkPalette {
    val AppBackground = Color(0xFF07090D)
    val ElevatedBackground = Color(0xFF0D1117)
    val PrimarySurface = Color(0xFF141A22)
    val SecondarySurface = Color(0xFF1C242E)
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
    val Border = Color(0xFF2B3440)
    val Divider = Color(0xFF1F2731)
    val HeroStart = Color(0xFF10151C)
    val HeroMiddle = Color(0xFF16202A)
    val HeroEnd = Color(0xFF1E2A33)
    val SonHarfBackground = Color(0xFFF3EEE5)
    val SonHarfSurface = Color(0xFFFFFBF4)
    val SonHarfInk = Color(0xFF173247)
    val SonHarfMuted = Color(0xFF6F7B7C)
    val SonHarfOcean = Color(0xFF4F8F96)
    val SonHarfOceanDeep = Color(0xFF2F6970)
    val SonHarfSky = Color(0xFF8EB7B5)
    val SonHarfIce = Color(0xFFE6EFEB)
    val SonHarfBorder = Color(0xFFD8D0C4)
    val SonHarfGreen = Color(0xFF789B73)
    val SonHarfGreenSoft = Color(0xFFE5ECDD)
    val SonHarfRed = Color(0xFFC86459)
    val SonHarfRedSoft = Color(0xFFF4DDD7)
    val SonHarfGold = Color(0xFFD1A13E)
    val SonHarfGoldSoft = Color(0xFFF5E8BB)
    val SonHarfRival = Color(0xFFD27869)
    val SonHarfRivalSoft = Color(0xFFF6E2DC)
    val Disabled = Color(0xFF1E252E)
    val DisabledContent = Color(0xFF728197)
}

/**
 * Production design tokens. The equipped game theme is read from [SonHarfCosmetics], so buying
 * and equipping Black Theme restyles every professional screen, not just legacy ones.
 */
internal object GameColors {
    val isDark: Boolean get() = SonHarfCosmetics.darkArenaTheme
    val AppBackground: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.AppBackground else GameNavyPalette.AppBackground
    val ElevatedBackground: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.ElevatedBackground else GameNavyPalette.ElevatedBackground
    val PrimarySurface: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.PrimarySurface else GameNavyPalette.PrimarySurface
    val SecondarySurface: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.SecondarySurface else GameNavyPalette.SecondarySurface
    val LightSurface: Color get() = GameNavyPalette.LightSurface
    val PrimaryBlue: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.PrimaryBlue else GameNavyPalette.PrimaryBlue
    val DeepBlue: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.DeepBlue else GameNavyPalette.DeepBlue
    val TacticalTurquoise: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TacticalTurquoise else GameNavyPalette.TacticalTurquoise
    val PlayGreen: Color get() = GameNavyPalette.PlayGreen
    val PlayGreenDeep: Color get() = GameNavyPalette.PlayGreenDeep
    val Lavender: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Lavender else GameNavyPalette.Lavender
    val RewardAmber: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.RewardAmber else GameNavyPalette.RewardAmber
    val Danger: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Danger else GameNavyPalette.Danger
    val PrestigeGold: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.PrestigeGold else GameNavyPalette.PrestigeGold
    val TextPrimary: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TextPrimary else GameNavyPalette.TextPrimary
    val TextSecondary: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TextSecondary else GameNavyPalette.TextSecondary
    val TextTertiary: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TextTertiary else GameNavyPalette.TextTertiary
    val TextDark: Color get() = GameNavyPalette.TextDark
    val Border: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Border else GameNavyPalette.Border
    val Divider: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Divider else GameNavyPalette.Divider
    val HeroStart: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.HeroStart else GameNavyPalette.HeroStart
    val HeroMiddle: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.HeroMiddle else GameNavyPalette.HeroMiddle
    val HeroEnd: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.HeroEnd else GameNavyPalette.HeroEnd
    val SonHarfBackground: Color get() = GameNavyPalette.SonHarfBackground
    val SonHarfSurface: Color get() = GameNavyPalette.SonHarfSurface
    val SonHarfInk: Color get() = GameNavyPalette.SonHarfInk
    val SonHarfMuted: Color get() = GameNavyPalette.SonHarfMuted
    val SonHarfOcean: Color get() = GameNavyPalette.SonHarfOcean
    val SonHarfOceanDeep: Color get() = GameNavyPalette.SonHarfOceanDeep
    val SonHarfSky: Color get() = GameNavyPalette.SonHarfSky
    val SonHarfIce: Color get() = GameNavyPalette.SonHarfIce
    val SonHarfBorder: Color get() = GameNavyPalette.SonHarfBorder
    val SonHarfGreen: Color get() = GameNavyPalette.SonHarfGreen
    val SonHarfGreenSoft: Color get() = GameNavyPalette.SonHarfGreenSoft
    val SonHarfRed: Color get() = GameNavyPalette.SonHarfRed
    val SonHarfRedSoft: Color get() = GameNavyPalette.SonHarfRedSoft
    val SonHarfGold: Color get() = GameNavyPalette.SonHarfGold
    val SonHarfGoldSoft: Color get() = GameNavyPalette.SonHarfGoldSoft
    val SonHarfRival: Color get() = GameNavyPalette.SonHarfRival
    val SonHarfRivalSoft: Color get() = GameNavyPalette.SonHarfRivalSoft
    val Disabled: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Disabled else GameNavyPalette.Disabled
    val DisabledContent: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.DisabledContent else GameNavyPalette.DisabledContent
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
