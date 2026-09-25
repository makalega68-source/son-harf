package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * KELİME TAHTI Higgsfield premium palette: graphite ground, ivory text, champagne-gold detail.
 * In gameplay green only ever means the player and red only ever means the rival.
 * Older semantic names stay as aliases so every screen resolves from this one source.
 */
internal object KelimeKusatmasiPalette {
    val MonsterBlack = Color(0xFF171C1B)
    val MonsterSurface = Color(0xFF222827)
    val MonsterSurface2 = Color(0xFF1D2322)
    val MonsterSurface3 = Color(0xFF2A302F)
    val MonsterLime = Color(0xFF32845E)
    val MonsterRed = Color(0xFFC85A54)
    val MonsterPink = Color(0xFFC5AA73)
    val MonsterOrange = Color(0xFFC5AA73)
    val MonsterText = Color(0xFFF0EDE3)
    val MonsterMuted = Color(0xFFA9AFAB)
    val MonsterBorder = Color(0xFF5C5239)

    val Ivory = Color(0xFFF0EDE3)
    val Champagne = Color(0xFFC5AA73)
    val PlayerGreen = Color(0xFF32845E)
    val PlayerGreenLight = Color(0xFF40A878)
    val RivalRed = Color(0xFFC85A54)
    val Muted = Color(0xFF68716D)
    val Disabled = Color(0xFF59605D)
    val InkOnIvory = Color(0xFF171C1B)

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
 * Graphite foundation with ivory text and champagne-gold lines. Gameplay, navigation, backend,
 * scoring and authorization are untouched; only visible colors resolve from here. The purchased
 * Black Theme deepens the ground to near-black.
 */
internal object SonHarfTheme {
    private val alternateDark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    // Dark-first UI: every Material shell uses the dark color scheme with ivory text.
    val IsDark: Boolean get() = true

    // Foundation layers.
    val Background: Color get() = if (alternateDark) Color(0xFF0E1110) else KelimeKusatmasiPalette.MonsterBlack
    val Surface: Color get() = KelimeKusatmasiPalette.MonsterSurface
    val SurfaceSecondary: Color get() = if (alternateDark) Color(0xFF161A19) else KelimeKusatmasiPalette.MonsterSurface2
    val SurfaceElevated: Color get() = KelimeKusatmasiPalette.MonsterSurface3
    val NavigationSurface: Color get() = if (alternateDark) Color(0xFF0B0D0D) else Color(0xFF131716)
    val ModalSurface: Color get() = Color(0xFF222827)

    // Gameplay layers: graphite field, ivory letter tiles.
    val GameSurface: Color get() = if (alternateDark) Color(0xFF0E1110) else Color(0xFF171C1B)
    val GameTile: Color get() = KelimeKusatmasiPalette.Ivory
    val GameTileBorder: Color get() = Color(0xFFD6CFBE)

    // Accent family: player green for actions, champagne gold for detail and prestige.
    val Primary: Color get() = KelimeKusatmasiPalette.MonsterLime
    val PrimarySoft: Color get() = Color(0xFF1F3A2D)
    val SoftBlue: Color get() = KelimeKusatmasiPalette.Champagne
    val Turquoise: Color get() = KelimeKusatmasiPalette.PlayerGreenLight
    val ActionOrange: Color get() = KelimeKusatmasiPalette.MonsterOrange
    val Lavender: Color get() = KelimeKusatmasiPalette.MonsterPink
    val Sand: Color get() = Color(0xFFD9C28F)

    // Text and dividers.
    val TextPrimary: Color get() = KelimeKusatmasiPalette.MonsterText
    val TextSecondary: Color get() = KelimeKusatmasiPalette.MonsterMuted
    val Border: Color get() = KelimeKusatmasiPalette.MonsterBorder

    // Semantic states.
    val Success: Color get() = KelimeKusatmasiPalette.PlayerGreen
    val SuccessSoft: Color get() = Color(0xFF1F3A2D)
    val Error: Color get() = KelimeKusatmasiPalette.RivalRed
    val Warning: Color get() = KelimeKusatmasiPalette.Champagne
    val DisabledBackground: Color get() = Color(0xFF2A302F)
    val DisabledContent: Color get() = KelimeKusatmasiPalette.Disabled

    val OnPrimary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnSecondary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnTertiary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnGold: Color get() = KelimeKusatmasiPalette.InkOnIvory

    // Graphite hero with a faint emerald cast; ivory text sits on it.
    val HeroStart: Color get() = Color(0xFF1B2120)
    val HeroMiddle: Color get() = Color(0xFF222827)
    val HeroEnd: Color get() = Color(0xFF1C2A23)

    // Compatibility naming used by existing hero/game cards.
    val Forest: Color get() = KelimeKusatmasiPalette.PlayerGreen
    val ForestDeep: Color get() = Color(0xFF171C1B)

    // Premium prestige is champagne gold.
    val PremiumGold: Color get() = KelimeKusatmasiPalette.Champagne
    val PremiumGoldLight: Color get() = Color(0xFFE2CFA0)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
