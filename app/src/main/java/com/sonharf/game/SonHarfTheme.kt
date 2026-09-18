package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Kelime Kuşatması premium visual system.
 *
 * The product now uses one light, high-energy design language everywhere: blue is the primary
 * action/brand color, turquoise communicates progress and success, purple communicates league /
 * prestige / PRO, orange communicates rewards and critical moments, and white keeps the product
 * breathable. Gameplay, navigation, scoring and backend behavior remain unchanged.
 */
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

    // Compatibility aliases retained so older screens inherit the new system without breakage.
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

/** Single source of truth for every app surface and game mode. */
internal object SonHarfTheme {
    // The approved Canva direction is light-first and intentionally consistent across the app.
    val IsDark: Boolean get() = false

    // Foundation layers.
    val Background: Color get() = KelimeKusatmasiPalette.Background
    val Surface: Color get() = KelimeKusatmasiPalette.White
    val SurfaceSecondary: Color get() = KelimeKusatmasiPalette.SurfaceSoft
    val SurfaceElevated: Color get() = Color(0xFFFFFFFF)
    val NavigationSurface: Color get() = Color(0xFFFBFDFF)
    val ModalSurface: Color get() = Color(0xFFFFFFFF)

    // Gameplay layers: tactical-map surface first, letter content second.
    val GameSurface: Color get() = Color(0xFFF2F6FC)
    val GameTile: Color get() = Color(0xFFFFFFFF)
    val GameTileBorder: Color get() = Color(0xFFCBD9EA)

    // Approved Canva accent family.
    val Primary: Color get() = KelimeKusatmasiPalette.Blue
    val PrimarySoft: Color get() = KelimeKusatmasiPalette.BlueSoft
    val SoftBlue: Color get() = Color(0xFF5B8DEF)
    val Turquoise: Color get() = KelimeKusatmasiPalette.Turquoise
    val ActionOrange: Color get() = KelimeKusatmasiPalette.Orange
    val Lavender: Color get() = KelimeKusatmasiPalette.Purple
    val Sand: Color get() = KelimeKusatmasiPalette.OrangeSoft

    // Text and dividers.
    val TextPrimary: Color get() = KelimeKusatmasiPalette.Ink
    val TextSecondary: Color get() = KelimeKusatmasiPalette.Muted
    val Border: Color get() = KelimeKusatmasiPalette.Border

    // Semantic states.
    val Success: Color get() = KelimeKusatmasiPalette.Turquoise
    val SuccessSoft: Color get() = KelimeKusatmasiPalette.TurquoiseSoft
    val Error: Color get() = Color(0xFFE5484D)
    val Warning: Color get() = KelimeKusatmasiPalette.Orange
    val DisabledBackground: Color get() = Color(0xFFE8EEF7)
    val DisabledContent: Color get() = Color(0xFF9AA8BA)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    // Premium hero: vivid but controlled blue -> turquoise -> purple.
    val HeroStart: Color get() = Color(0xFF2563EB)
    val HeroMiddle: Color get() = Color(0xFF12B8A6)
    val HeroEnd: Color get() = Color(0xFF7C3AED)

    // Compatibility names used by existing game cards and territory views.
    val Forest: Color get() = KelimeKusatmasiPalette.Turquoise
    val ForestDeep: Color get() = Color(0xFF123A56)

    // Prestige is purple in the approved system; gold remains available for trophy details only.
    val PremiumGold: Color get() = Color(0xFFF2B84B)
    val PremiumGoldLight: Color get() = Color(0xFFFFE4A8)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
