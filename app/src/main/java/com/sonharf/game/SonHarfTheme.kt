package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Compatibility surface for older screen code. Values resolve to the canonical Color.kt tokens.
 * New UI code should prefer SonHarfTheme semantic properties instead of hard-coded colours.
 */
internal object KelimeKusatmasiPalette {
    val Blue: Color get() = AppColors.Primary
    val BlueDeep: Color get() = AppColors.PrimaryDeep
    val BlueSoft: Color get() = AppColors.PrimarySoft
    val Turquoise: Color get() = AppColors.Secondary
    val TurquoiseDeep: Color get() = AppColors.SecondaryDeep
    val TurquoiseSoft: Color get() = AppColors.SecondarySoft
    val Purple: Color get() = AppColors.Accent
    val PurpleDeep = Color(0xFF75418D)
    val PurpleSoft: Color get() = AppColors.AccentSoft
    val Orange: Color get() = AppColors.Warning
    val OrangeDeep = Color(0xFFB96319)
    val OrangeSoft: Color get() = AppColors.WarningSoft
    val White: Color get() = AppColors.TextPrimary
    val Background: Color get() = AppColors.Background
    val SurfaceSoft: Color get() = AppColors.SurfaceSecondary
    val Ink: Color get() = AppColors.TextPrimary
    val Muted: Color get() = AppColors.TextSecondary
    val Border: Color get() = AppColors.Border

    val RoyalBlue: Color get() = AppColors.Secondary
    val DeepBlue: Color get() = AppColors.SecondaryDeep
    val Sky: Color get() = AppColors.SecondarySoft
    val OffWhite: Color get() = AppColors.Background
    val Slate: Color get() = AppColors.TextSecondary
    val PaleMint: Color get() = AppColors.PrimarySoft
    val SoftIndigo: Color get() = AppColors.Accent
    val SageGreen: Color get() = AppColors.Primary
    val SoftBlue: Color get() = AppColors.Secondary
    val LightBeige: Color get() = AppColors.WarningSoft
    val Lavender: Color get() = AppColors.Accent
    val SlateBlue: Color get() = AppColors.Secondary
    val WarmAccent: Color get() = AppColors.Warning
}

/** Premium Black Theme remains cosmetic-only and is activated only by server-authoritative ownership. */
internal object BlackThemePalette {
    val Background = Color(0xFF090D14)
    val Surface = Color(0xFF101723)
    val SurfaceSecondary = Color(0xFF172032)
    val SurfaceElevated = Color(0xFF1B2638)
    val Navigation = Color(0xFF0C111B)
    val Modal = Color(0xFF131B29)
    val GameSurface = Color(0xFF0E1520)
    val GameTile = Color(0xFF1B2637)
    val GameTileBorder = Color(0xFF35445E)
    val Primary = Color(0xFF39D982)
    val PrimarySoft = Color(0xFF153425)
    val Secondary = Color(0xFF55A9E2)
    val SecondarySoft = Color(0xFF172C3D)
    val Accent = Color(0xFFB16BCC)
    val Warning = Color(0xFFF08C30)
    val Text = Color(0xFFF5F7F8)
    val Muted = Color(0xFFA8B2B7)
    val Border = Color(0xFF2D3A52)
    val SuccessSoft = Color(0xFF153425)
    val Error = Color(0xFFFF6C79)
    val Disabled = Color(0xFF1C2431)
    val DisabledText = Color(0xFF6E7A88)
}

/** Single semantic theme source for the application shell and both game modes. */
internal object SonHarfTheme {
    val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive

    val Background: Color get() = if (IsDark) BlackThemePalette.Background else AppColors.Background
    val Surface: Color get() = if (IsDark) BlackThemePalette.Surface else AppColors.Surface
    val SurfaceSecondary: Color get() = if (IsDark) BlackThemePalette.SurfaceSecondary else AppColors.SurfaceSecondary
    val SurfaceElevated: Color get() = if (IsDark) BlackThemePalette.SurfaceElevated else AppColors.SurfaceElevated
    val NavigationSurface: Color get() = if (IsDark) BlackThemePalette.Navigation else AppColors.Navigation
    val ModalSurface: Color get() = if (IsDark) BlackThemePalette.Modal else AppColors.Modal

    val GameSurface: Color get() = if (IsDark) BlackThemePalette.GameSurface else AppColors.GameSurface
    val GameTile: Color get() = if (IsDark) BlackThemePalette.GameTile else AppColors.GameTile
    val GameTileBorder: Color get() = if (IsDark) BlackThemePalette.GameTileBorder else AppColors.GameTileBorder

    // Exact product contract: green primary action, blue secondary action, lavender accent, amber reward.
    val Primary: Color get() = if (IsDark) BlackThemePalette.Primary else AppColors.Primary
    val PrimarySoft: Color get() = if (IsDark) BlackThemePalette.PrimarySoft else AppColors.PrimarySoft
    val SoftBlue: Color get() = if (IsDark) BlackThemePalette.Secondary else AppColors.Secondary
    val Turquoise: Color get() = if (IsDark) BlackThemePalette.Secondary else AppColors.Secondary
    val ActionOrange: Color get() = if (IsDark) BlackThemePalette.Warning else AppColors.Warning
    val Lavender: Color get() = if (IsDark) BlackThemePalette.Accent else AppColors.Accent
    val Sand: Color get() = if (IsDark) Color(0xFF352718) else AppColors.WarningSoft
    val PlayGreen: Color get() = Primary
    val PlayGreenDeep: Color get() = if (IsDark) Color(0xFF2AAA64) else AppColors.PrimaryDeep

    val TextPrimary: Color get() = if (IsDark) BlackThemePalette.Text else AppColors.TextPrimary
    val TextSecondary: Color get() = if (IsDark) BlackThemePalette.Muted else AppColors.TextSecondary
    val Border: Color get() = if (IsDark) BlackThemePalette.Border else AppColors.Border

    val Success: Color get() = Primary
    val SuccessSoft: Color get() = if (IsDark) BlackThemePalette.SuccessSoft else AppColors.PrimarySoft
    val Error: Color get() = if (IsDark) BlackThemePalette.Error else AppColors.Error
    val Warning: Color get() = ActionOrange
    val DisabledBackground: Color get() = if (IsDark) BlackThemePalette.Disabled else Color(0xFF262E3D)
    val DisabledContent: Color get() = if (IsDark) BlackThemePalette.DisabledText else Color(0xFF697685)

    val OnPrimary: Color get() = Color(0xFF08120D)
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    // Dark competitive hero gradient; restrained so gameplay and statistics remain dominant.
    val HeroStart: Color get() = if (IsDark) Color(0xFF111A26) else Color(0xFF19243A)
    val HeroMiddle: Color get() = if (IsDark) Color(0xFF142A2A) else Color(0xFF1D3A36)
    val HeroEnd: Color get() = if (IsDark) Color(0xFF17213A) else Color(0xFF1C3148)

    // Compatibility aliases used by existing screens.
    val Forest: Color get() = Primary
    val ForestDeep: Color get() = if (IsDark) Color(0xFF10291E) else AppColors.PrimaryDeep
    val PremiumGold: Color get() = if (IsDark) Color(0xFFF2B44A) else AppColors.Gold
    val PremiumGoldLight: Color get() = if (IsDark) Color(0xFF3E321C) else AppColors.GoldSoft
    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = SoftBlue
    val Purple: Color get() = Lavender
}
