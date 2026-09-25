package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * KELİME TAHTI Higgsfield premium palette: graphite ground, ivory text, champagne-gold detail.
 * In gameplay green only ever means the player and red only ever means the rival.
 * Older semantic names stay as aliases so every screen resolves from this one source.
 */
internal object KelimeKusatmasiPalette {
    val MonsterBlack = Color(0xFFE6ECF2)
    val MonsterSurface = Color(0xFFFFFFFF)
    val MonsterSurface2 = Color(0xFFF3F6F9)
    val MonsterSurface3 = Color(0xFFEAF0F5)
    val MonsterLime = Color(0xFF3E9F4D)
    val MonsterRed = Color(0xFFD0514A)
    val MonsterPink = Color(0xFFE0A82E)
    val MonsterOrange = Color(0xFFE0A82E)
    val MonsterText = Color(0xFF243142)
    val MonsterMuted = Color(0xFF6B7A8C)
    val MonsterBorder = Color(0xFFD2DBE5)

    val Ivory = Color(0xFFFFFFFF)
    val Champagne = Color(0xFFE0A82E)
    val PlayerGreen = Color(0xFF3E9F4D)
    val PlayerGreenLight = Color(0xFF52B360)
    val RivalRed = Color(0xFFD0514A)
    val Muted = Color(0xFF9AA7B5)
    val Disabled = Color(0xFFB5C0CC)
    val InkOnIvory = Color(0xFF243142)

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

    // Light "Kelimelik tarzı" board-game UI (theme D): light ground, dark text, green actions.
    val IsDark: Boolean get() = false

    // Foundation layers.
    val Background: Color get() = KelimeKusatmasiPalette.MonsterBlack
    val Surface: Color get() = KelimeKusatmasiPalette.MonsterSurface
    val SurfaceSecondary: Color get() = KelimeKusatmasiPalette.MonsterSurface2
    val SurfaceElevated: Color get() = KelimeKusatmasiPalette.MonsterSurface3
    val NavigationSurface: Color get() = Color(0xFFFFFFFF)
    val ModalSurface: Color get() = Color(0xFFFFFFFF)

    // Gameplay layers: graphite field, ivory letter tiles.
    val GameSurface: Color get() = Color(0xFFE6ECF2)
    val GameTile: Color get() = Color(0xFFF7E3A6)
    val GameTileBorder: Color get() = Color(0xFFC9A560)

    // Accent family: player green for actions, champagne gold for detail and prestige.
    val Primary: Color get() = KelimeKusatmasiPalette.MonsterLime
    val PrimarySoft: Color get() = Color(0xFFE1F2E3)
    val SoftBlue: Color get() = KelimeKusatmasiPalette.Champagne
    val Turquoise: Color get() = KelimeKusatmasiPalette.PlayerGreenLight
    val ActionOrange: Color get() = KelimeKusatmasiPalette.MonsterOrange
    val Lavender: Color get() = KelimeKusatmasiPalette.MonsterPink
    val Sand: Color get() = Color(0xFFF7E3A6)

    // Text and dividers.
    val TextPrimary: Color get() = KelimeKusatmasiPalette.MonsterText
    val TextSecondary: Color get() = KelimeKusatmasiPalette.MonsterMuted
    val Border: Color get() = KelimeKusatmasiPalette.MonsterBorder

    // Semantic states.
    val Success: Color get() = KelimeKusatmasiPalette.PlayerGreen
    val SuccessSoft: Color get() = Color(0xFFE1F2E3)
    val Error: Color get() = KelimeKusatmasiPalette.RivalRed
    val Warning: Color get() = KelimeKusatmasiPalette.Champagne
    val DisabledBackground: Color get() = Color(0xFFE3E9EF)
    val DisabledContent: Color get() = KelimeKusatmasiPalette.Disabled

    val OnPrimary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnSecondary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnTertiary: Color get() = KelimeKusatmasiPalette.Ivory
    val OnGold: Color get() = KelimeKusatmasiPalette.InkOnIvory

    // Graphite hero with a faint emerald cast; ivory text sits on it.
    val HeroStart: Color get() = Color(0xFFFFFFFF)
    val HeroMiddle: Color get() = Color(0xFFF5F8FB)
    val HeroEnd: Color get() = Color(0xFFEAF0F5)

    // Compatibility naming used by existing hero/game cards.
    val Forest: Color get() = KelimeKusatmasiPalette.PlayerGreen
    val ForestDeep: Color get() = Color(0xFF2C3E55)

    // Premium prestige is champagne gold.
    val PremiumGold: Color get() = KelimeKusatmasiPalette.Champagne
    val PremiumGoldLight: Color get() = Color(0xFFF2C14E)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
