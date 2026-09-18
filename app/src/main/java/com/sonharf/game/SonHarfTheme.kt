package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Kelime Kuşatması premium test-release visual system.
 *
 * This palette is deliberately asset-light: typography, cards, borders, tiles and states are
 * rendered as real Compose UI. Decorative imagery, when used later, must be text-free so no
 * baked text can ever overlap application text.
 */
internal object KelimeKusatmasiPalette {
    val Night = Color(0xFF0B1118)
    val Graphite = Color(0xFF111923)
    val Graphite2 = Color(0xFF17212D)
    val Graphite3 = Color(0xFF1E2A37)
    val Sage = Color(0xFF78A77A)
    val SageBright = Color(0xFF9BC99A)
    val Blue = Color(0xFF5E96C8)
    val BlueBright = Color(0xFF7CB0DE)
    val Gold = Color(0xFFD3AB58)
    val GoldLight = Color(0xFFF0D38D)
    val WarmWhite = Color(0xFFF1F0EB)
    val Muted = Color(0xFFA8B0BA)
    val Border = Color(0xFF2B3948)

    // Compatibility aliases for older screens while the redesign is applied gradually.
    val RoyalBlue: Color get() = Blue
    val DeepBlue: Color get() = Night
    val Turquoise: Color get() = SageBright
    val Orange: Color get() = Gold
    val Sky: Color get() = Graphite2
    val OffWhite: Color get() = Night
    val Slate: Color get() = Muted
    val PaleMint: Color get() = Graphite2
    val SoftIndigo: Color get() = Blue
    val SageGreen: Color get() = Sage
    val SoftBlue: Color get() = Blue
    val LightBeige: Color get() = Graphite2
    val Lavender: Color get() = BlueBright
    val SlateBlue: Color get() = Muted
    val WarmAccent: Color get() = Gold
}

internal object SonHarfTheme {
    // Test theme is dark-first and intentionally consistent across all primary surfaces.
    val IsDark: Boolean get() = true

    // Foundation layers.
    val Background: Color get() = KelimeKusatmasiPalette.Night
    val Surface: Color get() = KelimeKusatmasiPalette.Graphite
    val SurfaceSecondary: Color get() = KelimeKusatmasiPalette.Graphite2
    val SurfaceElevated: Color get() = KelimeKusatmasiPalette.Graphite3
    val NavigationSurface: Color get() = Color(0xFF0D151E)
    val ModalSurface: Color get() = Color(0xFF121C27)

    // Gameplay layers.
    val GameSurface: Color get() = Color(0xFF0E1721)
    val GameTile: Color get() = Color(0xFFE9ECE8)
    val GameTileBorder: Color get() = Color(0xFF465565)

    // Core identity: sage for the player/action, soft blue for the opponent/secondary state.
    val Primary: Color get() = KelimeKusatmasiPalette.Sage
    val PrimarySoft: Color get() = Color(0xFF1B3026)
    val SoftBlue: Color get() = KelimeKusatmasiPalette.Blue
    val Turquoise: Color get() = KelimeKusatmasiPalette.SageBright
    val ActionOrange: Color get() = KelimeKusatmasiPalette.Gold
    val Lavender: Color get() = KelimeKusatmasiPalette.BlueBright
    val Sand: Color get() = Color(0xFFB89A69)

    // Text and dividers.
    val TextPrimary: Color get() = KelimeKusatmasiPalette.WarmWhite
    val TextSecondary: Color get() = KelimeKusatmasiPalette.Muted
    val Border: Color get() = KelimeKusatmasiPalette.Border

    // Semantic states.
    val Success: Color get() = Color(0xFF86BC83)
    val SuccessSoft: Color get() = Color(0xFF1B3026)
    val Error: Color get() = Color(0xFFCF6A6A)
    val Warning: Color get() = Color(0xFFD6A65D)
    val DisabledBackground: Color get() = Color(0xFF242E39)
    val DisabledContent: Color get() = Color(0xFF697583)

    val OnPrimary: Color get() = Color(0xFF09110C)
    val OnSecondary: Color get() = Color(0xFF081018)
    val OnTertiary: Color get() = Color(0xFF161006)

    // Calm premium hero gradient. No generated image text is required.
    val HeroStart: Color get() = Color(0xFF172A27)
    val HeroMiddle: Color get() = Color(0xFF17303A)
    val HeroEnd: Color get() = Color(0xFF182331)

    // Compatibility naming used by existing cards/game screens.
    val Forest: Color get() = KelimeKusatmasiPalette.Sage
    val ForestDeep: Color get() = Color(0xFF0D1A17)

    // Gold is reserved for paid/prestige meaning, never standard navigation.
    val PremiumGold: Color get() = KelimeKusatmasiPalette.Gold
    val PremiumGoldLight: Color get() = KelimeKusatmasiPalette.GoldLight

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = SoftBlue
    val Purple: Color get() = Lavender
}
