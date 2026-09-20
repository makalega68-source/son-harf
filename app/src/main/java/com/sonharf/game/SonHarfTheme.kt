package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Adult, restrained word-game palette used by the APK-v2 product line.
 * The palette deliberately avoids bright rainbow/casual-game treatment: warm neutral surfaces,
 * forest/sage as the primary action colour, muted slate-blue for secondary emphasis, terracotta
 * for rivalry/destructive emphasis and limited antique-gold for prestige.
 */
internal object KelimeKusatmasiPalette {
    val Blue = Color(0xFF365F53)
    val BlueDeep = Color(0xFF23443B)
    val BlueSoft = Color(0xFFE5ECE8)
    val Turquoise = Color(0xFF4F7B6E)
    val TurquoiseDeep = Color(0xFF355C51)
    val TurquoiseSoft = Color(0xFFE7EFEB)
    val Purple = Color(0xFF6E7F8C)
    val PurpleDeep = Color(0xFF4D5F6C)
    val PurpleSoft = Color(0xFFE9EDF0)
    val Orange = Color(0xFFAD6A57)
    val OrangeDeep = Color(0xFF8E5140)
    val OrangeSoft = Color(0xFFF3E8E2)
    val White = Color(0xFFFFFEFA)
    val Background = Color(0xFFF4F2EC)
    val SurfaceSoft = Color(0xFFECEDE8)
    val Ink = Color(0xFF202A28)
    val Muted = Color(0xFF69736F)
    val Border = Color(0xFFD5DAD4)

    val RoyalBlue: Color get() = Blue
    val DeepBlue: Color get() = BlueDeep
    val Sky: Color get() = PurpleSoft
    val OffWhite: Color get() = Background
    val Slate: Color get() = Muted
    val PaleMint: Color get() = TurquoiseSoft
    val SoftIndigo: Color get() = Purple
    val SageGreen: Color get() = Turquoise
    val SoftBlue: Color get() = Purple
    val LightBeige: Color get() = Color(0xFFECE5D8)
    val Lavender: Color get() = Purple
    val SlateBlue: Color get() = Purple
    val WarmAccent: Color get() = Orange
}

/** Premium Black Theme. Cosmetic only; gameplay and scoring are unchanged. */
internal object BlackThemePalette {
    val Background = Color(0xFF101412)
    val Surface = Color(0xFF171D1A)
    val SurfaceSecondary = Color(0xFF1E2622)
    val SurfaceElevated = Color(0xFF242D28)
    val Navigation = Color(0xFF121714)
    val Modal = Color(0xFF1B221E)
    val GameSurface = Color(0xFF151C18)
    val GameTile = Color(0xFF202A25)
    val GameTileBorder = Color(0xFF3C4A43)
    val Primary = Color(0xFF78A493)
    val PrimarySoft = Color(0xFF20372E)
    val Turquoise = Color(0xFF82B09F)
    val Purple = Color(0xFF8C9AA5)
    val Orange = Color(0xFFC98770)
    val Text = Color(0xFFF5F4EF)
    val Muted = Color(0xFFA7B0AB)
    val Border = Color(0xFF354039)
    val SuccessSoft = Color(0xFF20352D)
    val Disabled = Color(0xFF252B28)
    val DisabledText = Color(0xFF737C77)
}

/** Single source of truth for every application surface and game mode. */
internal object SonHarfTheme {
    val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive

    val Background: Color get() = if (IsDark) BlackThemePalette.Background else KelimeKusatmasiPalette.Background
    val Surface: Color get() = if (IsDark) BlackThemePalette.Surface else KelimeKusatmasiPalette.White
    val SurfaceSecondary: Color get() = if (IsDark) BlackThemePalette.SurfaceSecondary else KelimeKusatmasiPalette.SurfaceSoft
    val SurfaceElevated: Color get() = if (IsDark) BlackThemePalette.SurfaceElevated else Color(0xFFF8F7F2)
    val NavigationSurface: Color get() = if (IsDark) BlackThemePalette.Navigation else Color(0xFFF8F7F2)
    val ModalSurface: Color get() = if (IsDark) BlackThemePalette.Modal else Color(0xFFFFFEFA)

    val GameSurface: Color get() = if (IsDark) BlackThemePalette.GameSurface else Color(0xFFEBEEE9)
    val GameTile: Color get() = if (IsDark) BlackThemePalette.GameTile else Color(0xFFFFFEFA)
    val GameTileBorder: Color get() = if (IsDark) BlackThemePalette.GameTileBorder else Color(0xFFC8D0C9)

    val Primary: Color get() = if (IsDark) BlackThemePalette.Primary else KelimeKusatmasiPalette.Blue
    val PrimarySoft: Color get() = if (IsDark) BlackThemePalette.PrimarySoft else KelimeKusatmasiPalette.BlueSoft
    val SoftBlue: Color get() = if (IsDark) Color(0xFF8DA1AF) else Color(0xFF718693)
    val Turquoise: Color get() = if (IsDark) BlackThemePalette.Turquoise else KelimeKusatmasiPalette.Turquoise
    val ActionOrange: Color get() = if (IsDark) BlackThemePalette.Orange else KelimeKusatmasiPalette.Orange
    val Lavender: Color get() = if (IsDark) BlackThemePalette.Purple else KelimeKusatmasiPalette.Purple
    val Sand: Color get() = if (IsDark) Color(0xFF352B22) else Color(0xFFECE5D8)

    val TextPrimary: Color get() = if (IsDark) BlackThemePalette.Text else KelimeKusatmasiPalette.Ink
    val TextSecondary: Color get() = if (IsDark) BlackThemePalette.Muted else KelimeKusatmasiPalette.Muted
    val Border: Color get() = if (IsDark) BlackThemePalette.Border else KelimeKusatmasiPalette.Border

    val Success: Color get() = Turquoise
    val SuccessSoft: Color get() = if (IsDark) BlackThemePalette.SuccessSoft else KelimeKusatmasiPalette.TurquoiseSoft
    val Error: Color get() = if (IsDark) Color(0xFFD96D6D) else Color(0xFFB84E4E)
    val Warning: Color get() = ActionOrange
    val DisabledBackground: Color get() = if (IsDark) BlackThemePalette.Disabled else Color(0xFFE1E4DF)
    val DisabledContent: Color get() = if (IsDark) BlackThemePalette.DisabledText else Color(0xFF929B96)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    // Main product hero stays nearly monochrome; secondary accents must not turn the home screen into a rainbow.
    val HeroStart: Color get() = if (IsDark) Color(0xFF1D2D28) else Color(0xFF29483F)
    val HeroMiddle: Color get() = if (IsDark) Color(0xFF244037) else Color(0xFF365F53)
    val HeroEnd: Color get() = if (IsDark) Color(0xFF2D403B) else Color(0xFF516B62)

    val Forest: Color get() = Turquoise
    val ForestDeep: Color get() = if (IsDark) Color(0xFF14241F) else Color(0xFF23443B)
    val PremiumGold: Color get() = if (IsDark) Color(0xFFD9B56C) else Color(0xFFB68A3E)
    val PremiumGoldLight: Color get() = if (IsDark) Color(0xFF44371E) else Color(0xFFE9D8AE)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
