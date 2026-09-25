package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * KELİME TAHTI light palette: warm cream ground, dark text, green actions and gold detail.
 * In gameplay green only ever means the player and red only ever means the rival.
 * Older semantic names stay as aliases so every screen resolves from this one source.
 */
internal object KelimeKusatmasiPalette {
    val MonsterBlack = Color(0xFFF8F3E9)
    val MonsterSurface = Color(0xFFFFFDF8)
    val MonsterSurface2 = Color(0xFFF1E9DB)
    val MonsterSurface3 = Color(0xFFE8DDCC)
    val MonsterLime = Color(0xFF246C4C)
    val MonsterRed = Color(0xFFAD4242)
    val MonsterPink = Color(0xFF95651C)
    val MonsterOrange = Color(0xFF95651C)
    val MonsterText = Color(0xFF202C27)
    val MonsterMuted = Color(0xFF52645A)
    val MonsterBorder = Color(0xFFD6C9B6)

    val Ivory = Color(0xFFF0EDE3)
    val Champagne = Color(0xFF95651C)
    val PlayerGreen = Color(0xFF246C4C)
    val PlayerGreenLight = Color(0xFF40A878)
    val RivalRed = Color(0xFFAD4242)
    val Muted = Color(0xFF68716D)
    val Disabled = Color(0xFF59605D)
    val InkOnIvory = Color(0xFF202C27)

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
 * Warm foundation with dark text and champagne-gold lines. Gameplay, navigation, backend,
 * scoring and authorization are untouched; only visible colors resolve from here. The purchased
 * Black Theme deepens the ground to near-black.
 */
internal object SonHarfTheme {
    private val alternateDark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    // The dark arena cosmetic alone selects the dark Material scheme.
    val IsDark: Boolean get() = alternateDark

    // Foundation layers.
    val Background: Color get() = if (alternateDark) Color(0xFF0E1110) else KelimeKusatmasiPalette.MonsterBlack
    val Surface: Color get() = if (alternateDark) Color(0xFF222827) else KelimeKusatmasiPalette.MonsterSurface
    val SurfaceSecondary: Color get() = if (alternateDark) Color(0xFF161A19) else KelimeKusatmasiPalette.MonsterSurface2
    val SurfaceElevated: Color get() = if (alternateDark) Color(0xFF2A302F) else KelimeKusatmasiPalette.MonsterSurface3
    val NavigationSurface: Color get() = if (alternateDark) Color(0xFF0B0D0D) else Color(0xFFFFFBF4)
    val ModalSurface: Color get() = if (alternateDark) Color(0xFF222827) else Color(0xFFFFFDF8)

    // Gameplay layers: graphite field, ivory letter tiles.
    val GameSurface: Color get() = if (alternateDark) Color(0xFF0E1110) else Color(0xFFEDE4D5)
    val GameTile: Color get() = KelimeKusatmasiPalette.Ivory
    val GameTileBorder: Color get() = Color(0xFFD6CFBE)

    // Accent family: player green for actions, champagne gold for detail and prestige.
    val Primary: Color get() = KelimeKusatmasiPalette.MonsterLime
    val PrimarySoft: Color get() = if (alternateDark) Color(0xFF1F3A2D) else Color(0xFFDDEDE1)
    val SoftBlue: Color get() = KelimeKusatmasiPalette.Champagne
    val Turquoise: Color get() = KelimeKusatmasiPalette.PlayerGreenLight
    val ActionOrange: Color get() = KelimeKusatmasiPalette.MonsterOrange
    val Lavender: Color get() = KelimeKusatmasiPalette.MonsterPink
    val Sand: Color get() = Color(0xFFD9C28F)

    // Text and dividers.
    val TextPrimary: Color get() = if (alternateDark) KelimeKusatmasiPalette.Ivory else KelimeKusatmasiPalette.MonsterText
    val TextSecondary: Color get() = if (alternateDark) Color(0xFFA9AFAB) else KelimeKusatmasiPalette.MonsterMuted
    val Border: Color get() = if (alternateDark) Color(0xFF5C5239) else KelimeKusatmasiPalette.MonsterBorder

    // Semantic states.
    val Success: Color get() = KelimeKusatmasiPalette.PlayerGreen
    val SuccessSoft: Color get() = if (alternateDark) Color(0xFF1F3A2D) else Color(0xFFDDEDE1)
    val Error: Color get() = KelimeKusatmasiPalette.RivalRed
    val Warning: Color get() = KelimeKusatmasiPalette.Champagne
    val DisabledBackground: Color get() = if (alternateDark) Color(0xFF2A302F) else Color(0xFFE8DDCC)
    val DisabledContent: Color get() = if (alternateDark) KelimeKusatmasiPalette.Disabled else Color(0xFF69756E)

    val OnPrimary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnSecondary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnTertiary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnGold: Color get() = KelimeKusatmasiPalette.InkOnIvory

    // Light hero uses the same ink as the surrounding cards.
    val HeroStart: Color get() = Color(0xFFE1EFDF)
    val HeroMiddle: Color get() = Color(0xFFF1EDDE)
    val HeroEnd: Color get() = Color(0xFFF7E7CE)

    // Compatibility naming used by existing hero/game cards.
    val Forest: Color get() = KelimeKusatmasiPalette.PlayerGreen
    val ForestDeep: Color get() = Color(0xFF202C27)

    // Premium prestige is champagne gold.
    val PremiumGold: Color get() = KelimeKusatmasiPalette.Champagne
    val PremiumGoldLight: Color get() = Color(0xFFE2CFA0)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
