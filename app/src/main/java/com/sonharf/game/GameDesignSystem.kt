package com.sonharf.game

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
 * Uses the light "Kelime Tahtı" palette approved in PR #459 (turquoise, sky blue, lavender).
 * Gameplay/business logic remains outside this layer.
 */
/** Default light "Kelime Tahtı" palette. */
internal object GameLightPalette {
    val AppBackground = Color(0xFFEAF6F8)
    val ElevatedBackground = Color(0xFFFFFFFF)
    val PrimarySurface = Color(0xFFFFFFFF)
    val SecondarySurface = Color(0xFFE0F3F5)
    val LightSurface = Color(0xFFF5F7FA)
    val PrimaryBlue = Color(0xFF14B8B0)
    val DeepBlue = Color(0xFF0E9A93)
    val TacticalTurquoise = Color(0xFF22C3C9)
    val PlayGreen = Color(0xFF38C970)
    val PlayGreenDeep = Color(0xFF249C53)
    val Lavender = Color(0xFF8B6CF0)
    val RewardAmber = Color(0xFFFF8A2A)
    val Danger = Color(0xFFE8622C)
    val PrestigeGold = Color(0xFFC89C39)
    val TextPrimary = Color(0xFF0B1B33)
    val TextSecondary = Color(0xFF3B4B66)
    val TextTertiary = Color(0xFF5E6D84)
    val TextDark = Color(0xFF0B1B33)
    val Border = Color(0xFFC3D6E4)
    val Divider = Color(0xFFD6E3EE)
    val HeroStart = Color(0xFF14B8B0)
    val HeroMiddle = Color(0xFF3E7BFA)
    val HeroEnd = Color(0xFF8B6CF0)
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
    val Disabled = Color(0xFFDDE5EE)
    val DisabledContent = Color(0xFF5E6D84)
}

/** Navy palette used when the player equips the purchased Black Theme (theme_black). */
internal object GameDarkPalette {
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
    val TextDark = Color(0xFF0B1B33)
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

/**
 * Production design tokens. The equipped game theme is read from [SonHarfCosmetics], so buying
 * and equipping Black Theme restyles every professional screen, not just legacy ones.
 */
internal object GameColors {
    val isDark: Boolean get() = SonHarfCosmetics.darkArenaTheme
    val AppBackground: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.AppBackground else GameLightPalette.AppBackground
    val ElevatedBackground: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.ElevatedBackground else GameLightPalette.ElevatedBackground
    val PrimarySurface: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.PrimarySurface else GameLightPalette.PrimarySurface
    val SecondarySurface: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.SecondarySurface else GameLightPalette.SecondarySurface
    val LightSurface: Color get() = GameLightPalette.LightSurface
    val PrimaryBlue: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.PrimaryBlue else GameLightPalette.PrimaryBlue
    val DeepBlue: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.DeepBlue else GameLightPalette.DeepBlue
    val TacticalTurquoise: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TacticalTurquoise else GameLightPalette.TacticalTurquoise
    val PlayGreen: Color get() = GameLightPalette.PlayGreen
    val PlayGreenDeep: Color get() = GameLightPalette.PlayGreenDeep
    val Lavender: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Lavender else GameLightPalette.Lavender
    val RewardAmber: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.RewardAmber else GameLightPalette.RewardAmber
    val Danger: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Danger else GameLightPalette.Danger
    val PrestigeGold: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.PrestigeGold else GameLightPalette.PrestigeGold
    val TextPrimary: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TextPrimary else GameLightPalette.TextPrimary
    val TextSecondary: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TextSecondary else GameLightPalette.TextSecondary
    val TextTertiary: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.TextTertiary else GameLightPalette.TextTertiary
    val TextDark: Color get() = GameLightPalette.TextDark
    val Border: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Border else GameLightPalette.Border
    val Divider: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Divider else GameLightPalette.Divider
    val HeroStart: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.HeroStart else GameLightPalette.HeroStart
    val HeroMiddle: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.HeroMiddle else GameLightPalette.HeroMiddle
    val HeroEnd: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.HeroEnd else GameLightPalette.HeroEnd
    val SonHarfBackground: Color get() = GameLightPalette.SonHarfBackground
    val SonHarfSurface: Color get() = GameLightPalette.SonHarfSurface
    val SonHarfInk: Color get() = GameLightPalette.SonHarfInk
    val SonHarfMuted: Color get() = GameLightPalette.SonHarfMuted
    val SonHarfOcean: Color get() = GameLightPalette.SonHarfOcean
    val SonHarfOceanDeep: Color get() = GameLightPalette.SonHarfOceanDeep
    val SonHarfSky: Color get() = GameLightPalette.SonHarfSky
    val SonHarfIce: Color get() = GameLightPalette.SonHarfIce
    val SonHarfBorder: Color get() = GameLightPalette.SonHarfBorder
    val SonHarfGreen: Color get() = GameLightPalette.SonHarfGreen
    val SonHarfGreenSoft: Color get() = GameLightPalette.SonHarfGreenSoft
    val SonHarfRed: Color get() = GameLightPalette.SonHarfRed
    val SonHarfRedSoft: Color get() = GameLightPalette.SonHarfRedSoft
    val SonHarfGold: Color get() = GameLightPalette.SonHarfGold
    val SonHarfGoldSoft: Color get() = GameLightPalette.SonHarfGoldSoft
    val SonHarfRival: Color get() = GameLightPalette.SonHarfRival
    val SonHarfRivalSoft: Color get() = GameLightPalette.SonHarfRivalSoft
    val Disabled: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.Disabled else GameLightPalette.Disabled
    val DisabledContent: Color get() = if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.DisabledContent else GameLightPalette.DisabledContent
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
    val scheme = if (GameColors.isDark) darkColorScheme(
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
    ) else lightColorScheme(
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
