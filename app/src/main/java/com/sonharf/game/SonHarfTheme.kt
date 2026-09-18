package com.sonharf.game

import androidx.compose.ui.graphics.Color

/** Approved light premium palette. */
internal object KelimeKusatmasiPalette {
    val Blue = Color(0xFF2563EB)
    val BlueDeep = Color(0xFF1748C7)
    val BlueSoft = Color(0xFFEAF1FF)
    val Turquoise = Color(0xFF12B8A6)
    val TurquoiseDeep = Color(0xFF0E8F83)
    val TurquoiseSoft = Color(0xFFE6FAF7)
    val Purple = Color(0xFF7C3AED)
    val PurpleDeep = Color(0xFF5B21B6)
    val PurpleSoft = Color(0xFFF1EAFE)
    val Orange = Color(0xFFF97316)
    val OrangeDeep = Color(0xFFD95D0B)
    val OrangeSoft = Color(0xFFFFF0E6)
    val White = Color(0xFFFFFFFF)
    val Background = Color(0xFFF6F9FF)
    val SurfaceSoft = Color(0xFFF0F5FD)
    val Ink = Color(0xFF10213D)
    val Muted = Color(0xFF64748B)
    val Border = Color(0xFFDCE6F3)

    val RoyalBlue: Color get() = Blue
    val DeepBlue: Color get() = BlueDeep
    val Sky: Color get() = BlueSoft
    val OffWhite: Color get() = Background
    val Slate: Color get() = Muted
    val PaleMint: Color get() = TurquoiseSoft
    val SoftIndigo: Color get() = Purple
    val SageGreen: Color get() = Turquoise
    val SoftBlue: Color get() = Blue
    val LightBeige: Color get() = OrangeSoft
    val Lavender: Color get() = Purple
    val SlateBlue: Color get() = Muted
    val WarmAccent: Color get() = Orange
}

/** Premium Black Theme. Cosmetic only; gameplay and scoring are unchanged. */
internal object BlackThemePalette {
    val Background = Color(0xFF090B10)
    val Surface = Color(0xFF121722)
    val SurfaceSecondary = Color(0xFF19202D)
    val SurfaceElevated = Color(0xFF1F2836)
    val Navigation = Color(0xFF0D1118)
    val Modal = Color(0xFF161C27)
    val GameSurface = Color(0xFF101722)
    val GameTile = Color(0xFF1A2230)
    val GameTileBorder = Color(0xFF344154)
    val Primary = Color(0xFF3B82F6)
    val PrimarySoft = Color(0xFF182A46)
    val Turquoise = Color(0xFF1FD1C2)
    val Purple = Color(0xFF9B6CFF)
    val Orange = Color(0xFFFF8A34)
    val Text = Color(0xFFF8FAFC)
    val Muted = Color(0xFF9CAABC)
    val Border = Color(0xFF2B3647)
    val SuccessSoft = Color(0xFF12332F)
    val Disabled = Color(0xFF202735)
    val DisabledText = Color(0xFF6F7B8D)
}

/** Single source of truth for every application surface and game mode. */
internal object SonHarfTheme {
    val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive

    val Background: Color get() = if (IsDark) BlackThemePalette.Background else KelimeKusatmasiPalette.Background
    val Surface: Color get() = if (IsDark) BlackThemePalette.Surface else KelimeKusatmasiPalette.White
    val SurfaceSecondary: Color get() = if (IsDark) BlackThemePalette.SurfaceSecondary else KelimeKusatmasiPalette.SurfaceSoft
    val SurfaceElevated: Color get() = if (IsDark) BlackThemePalette.SurfaceElevated else Color(0xFFFFFFFF)
    val NavigationSurface: Color get() = if (IsDark) BlackThemePalette.Navigation else Color(0xFFFBFDFF)
    val ModalSurface: Color get() = if (IsDark) BlackThemePalette.Modal else Color(0xFFFFFFFF)

    val GameSurface: Color get() = if (IsDark) BlackThemePalette.GameSurface else Color(0xFFF2F6FC)
    val GameTile: Color get() = if (IsDark) BlackThemePalette.GameTile else Color(0xFFFFFFFF)
    val GameTileBorder: Color get() = if (IsDark) BlackThemePalette.GameTileBorder else Color(0xFFCBD9EA)

    val Primary: Color get() = if (IsDark) BlackThemePalette.Primary else KelimeKusatmasiPalette.Blue
    val PrimarySoft: Color get() = if (IsDark) BlackThemePalette.PrimarySoft else KelimeKusatmasiPalette.BlueSoft
    val SoftBlue: Color get() = if (IsDark) Color(0xFF6EA3FF) else Color(0xFF5B8DEF)
    val Turquoise: Color get() = if (IsDark) BlackThemePalette.Turquoise else KelimeKusatmasiPalette.Turquoise
    val ActionOrange: Color get() = if (IsDark) BlackThemePalette.Orange else KelimeKusatmasiPalette.Orange
    val Lavender: Color get() = if (IsDark) BlackThemePalette.Purple else KelimeKusatmasiPalette.Purple
    val Sand: Color get() = if (IsDark) Color(0xFF352417) else KelimeKusatmasiPalette.OrangeSoft

    val TextPrimary: Color get() = if (IsDark) BlackThemePalette.Text else KelimeKusatmasiPalette.Ink
    val TextSecondary: Color get() = if (IsDark) BlackThemePalette.Muted else KelimeKusatmasiPalette.Muted
    val Border: Color get() = if (IsDark) BlackThemePalette.Border else KelimeKusatmasiPalette.Border

    val Success: Color get() = Turquoise
    val SuccessSoft: Color get() = if (IsDark) BlackThemePalette.SuccessSoft else KelimeKusatmasiPalette.TurquoiseSoft
    val Error: Color get() = if (IsDark) Color(0xFFFF6670) else Color(0xFFE5484D)
    val Warning: Color get() = ActionOrange
    val DisabledBackground: Color get() = if (IsDark) BlackThemePalette.Disabled else Color(0xFFE8EEF7)
    val DisabledContent: Color get() = if (IsDark) BlackThemePalette.DisabledText else Color(0xFF9AA8BA)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    val HeroStart: Color get() = if (IsDark) Color(0xFF0E1A2D) else Color(0xFF2563EB)
    val HeroMiddle: Color get() = if (IsDark) Color(0xFF123C42) else Color(0xFF12B8A6)
    val HeroEnd: Color get() = if (IsDark) Color(0xFF2B1748) else Color(0xFF7C3AED)

    val Forest: Color get() = Turquoise
    val ForestDeep: Color get() = if (IsDark) Color(0xFF081B22) else Color(0xFF123A56)
    val PremiumGold: Color get() = if (IsDark) Color(0xFFFFC55A) else Color(0xFFF2B84B)
    val PremiumGoldLight: Color get() = if (IsDark) Color(0xFF4A3617) else Color(0xFFFFE4A8)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
