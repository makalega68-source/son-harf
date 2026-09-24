package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * KELİME TAHTI görsel sistemi: açık turkuaz/beyaz zemin, turkuaz-mavi eylemler,
 * turuncu vurgu, eflatun premium ve koyu lacivert metin. Eski semantik isimler
 * yalnızca derleme uyumluluğu için alias olarak kalır.
 */
internal object KelimeKusatmasiPalette {
    val MonsterBlack = Color(0xFFEAF6F8)
    val MonsterSurface = Color(0xFFFFFFFF)
    val MonsterSurface2 = Color(0xFFE0F3F5)
    val MonsterSurface3 = Color(0xFFEEEBFC)
    val MonsterLime = Color(0xFF14B8B0)
    val MonsterRed = Color(0xFFE8622C)
    val MonsterPink = Color(0xFF8B6CF0)
    val MonsterOrange = Color(0xFFFF8A2A)
    val MonsterText = Color(0xFF0B1B33)
    val MonsterMuted = Color(0xFF3B4B66)
    val MonsterBorder = Color(0xFFC3D6E4)

    // Compatibility aliases for screens that still reference the previous palette names.
    val RoyalBlue: Color get() = MonsterLime
    val DeepBlue: Color get() = MonsterText
    val Turquoise: Color get() = MonsterLime
    val Orange: Color get() = MonsterOrange
    val Sky: Color get() = MonsterSurface2
    val OffWhite: Color get() = MonsterBlack
    val Slate: Color get() = MonsterMuted
    val PaleMint: Color get() = MonsterSurface2
    val SoftIndigo: Color get() = MonsterPink
    val SageGreen: Color get() = MonsterLime
    val SoftBlue: Color get() = MonsterPink
    val LightBeige: Color get() = MonsterSurface2
    val Lavender: Color get() = MonsterPink
    val SlateBlue: Color get() = MonsterMuted
    val WarmAccent: Color get() = MonsterOrange
}

/**
 * Application-wide KELİME TAHTI visual system.
 *
 * Light turquoise foundation with dark navy text. Gameplay, navigation, backend, scoring
 * and authorization are untouched; only visible colors resolve from here.
 */
internal object SonHarfTheme {
    private val alternateDark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    // Light-first UI: every Material shell uses the light color scheme with dark text.
    val IsDark: Boolean get() = false

    // Foundation layers.
    val Background: Color get() = if (alternateDark) Color(0xFFE2EEF8) else KelimeKusatmasiPalette.MonsterBlack
    val Surface: Color get() = KelimeKusatmasiPalette.MonsterSurface
    val SurfaceSecondary: Color get() = if (alternateDark) Color(0xFFDDE9F7) else KelimeKusatmasiPalette.MonsterSurface2
    val SurfaceElevated: Color get() = KelimeKusatmasiPalette.MonsterSurface3
    val NavigationSurface: Color get() = Color(0xFFFFFFFF)
    val ModalSurface: Color get() = Color(0xFFFFFFFF)

    // Gameplay layers: calm light field, white readable letter tiles.
    val GameSurface: Color get() = if (alternateDark) Color(0xFFDCE8F6) else Color(0xFFDDF1F4)
    val GameTile: Color get() = Color(0xFFFFFFFF)
    val GameTileBorder: Color get() = Color(0xFF9DB5C8)

    // Brand/accent family: turquoise, blue, lilac, orange.
    val Primary: Color get() = KelimeKusatmasiPalette.MonsterLime
    val PrimarySoft: Color get() = Color(0xFFD2F2F0)
    val SoftBlue: Color get() = Color(0xFF3D7BEF)
    val Turquoise: Color get() = Color(0xFF22C3C9)
    val ActionOrange: Color get() = KelimeKusatmasiPalette.MonsterOrange
    val Lavender: Color get() = KelimeKusatmasiPalette.MonsterPink
    val Sand: Color get() = Color(0xFFFFB463)

    // Text and dividers.
    val TextPrimary: Color get() = KelimeKusatmasiPalette.MonsterText
    val TextSecondary: Color get() = KelimeKusatmasiPalette.MonsterMuted
    val Border: Color get() = KelimeKusatmasiPalette.MonsterBorder

    // Semantic states.
    val Success: Color get() = Color(0xFF12A89F)
    val SuccessSoft: Color get() = Color(0xFFD2F2F0)
    val Error: Color get() = Color(0xFFE8622C)
    val Warning: Color get() = Color(0xFFFF9F2E)
    val DisabledBackground: Color get() = Color(0xFFDDE5EE)
    val DisabledContent: Color get() = Color(0xFF5E6D84)

    val OnPrimary: Color get() = Color(0xFF0B1B33)
    val OnSecondary: Color get() = Color(0xFF0B1B33)
    val OnTertiary: Color get() = Color(0xFF0B1B33)

    // Deep navy → blue hero; white text is used only on this dark surface.
    val HeroStart: Color get() = Color(0xFF0C2250)
    val HeroMiddle: Color get() = Color(0xFF16398A)
    val HeroEnd: Color get() = Color(0xFF1D4FB0)

    // Compatibility naming used by existing hero/game cards.
    val Forest: Color get() = Color(0xFF22C3C9)
    val ForestDeep: Color get() = Color(0xFF0B1B33)

    // Premium prestige is lilac; standard actions use turquoise/blue, key actions orange.
    val PremiumGold: Color get() = Color(0xFF8B6CF0)
    val PremiumGoldLight: Color get() = Color(0xFFB7A3FF)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
