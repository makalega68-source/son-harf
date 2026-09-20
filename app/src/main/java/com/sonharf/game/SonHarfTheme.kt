package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Kelime Kuşatması visual system, rebuilt from the two purchased Casual Game UI #02 packs.
 *
 * Important licensing note: the repository is public, while the purchased pack license forbids
 * publishing original/easily extractable source assets outside a finished product. Therefore the
 * pack's visual language (blue/green/orange/purple action families, glossy game surfaces,
 * high-legibility panels and reward hierarchy) is reproduced as native Compose styling instead of
 * committing the vendor PNG/PSD sources to this repository.
 */
internal object KelimeKusatmasiPalette {
    val Ink = Color(0xFF102A56)
    val Muted = Color(0xFF6B7C9E)
    val Background = Color(0xFFF5F8FC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFF0F5FC)
    val SurfaceRaised = Color(0xFFFAFCFF)
    val Border = Color(0xFFD9E4F2)

    val GameGreen = Color(0xFF52AD56)
    val GameGreenDeep = Color(0xFF378C45)
    val GameGreenSoft = Color(0xFFE8F6EA)
    val GameBlue = Color(0xFF4D83DA)
    val GameBlueDeep = Color(0xFF2D5FAB)
    val GameBlueSoft = Color(0xFFEAF1FF)
    val GamePurple = Color(0xFF8A62D3)
    val GamePurpleSoft = Color(0xFFF1EBFF)
    val GameOrange = Color(0xFFF5A623)
    val GameOrangeSoft = Color(0xFFFFF2D8)
    val GameRed = Color(0xFFD95A62)
    val GameRedSoft = Color(0xFFFCE8EA)
    val GameTurquoise = Color(0xFF38B8BD)
    val Gold = Color(0xFFD89A22)
    val GoldLight = Color(0xFFF7D77D)

    // Compatibility aliases retained so older screens compile while inheriting the new system.
    val RoyalBlue: Color get() = GameBlue
    val DeepBlue: Color get() = GameBlueDeep
    val Turquoise: Color get() = GameTurquoise
    val Orange: Color get() = GameOrange
    val Sky: Color get() = GameBlueSoft
    val OffWhite: Color get() = Background
    val Slate: Color get() = Muted
    val PaleMint: Color get() = GameGreenSoft
    val SoftIndigo: Color get() = GamePurple
    val SageGreen: Color get() = GameGreen
    val SoftBlue: Color get() = GameBlue
    val LightBeige: Color get() = GameOrangeSoft
    val Lavender: Color get() = GamePurple
    val SlateBlue: Color get() = Muted
    val WarmAccent: Color get() = GameOrange
}

/**
 * Application-wide bright game UI.
 *
 * The game remains visually energetic, but the central reading surface is deliberately light and
 * calm so long word-game sessions stay legible. Purchased-pack colors are reserved for actions,
 * rewards and competitive status rather than filling every surface.
 */
internal object SonHarfTheme {
    private val alternateDark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    // The refreshed product is light-first. The optional arena cosmetic may still opt into dark
    // gameplay-specific surfaces without forcing the whole application into a black theme.
    val IsDark: Boolean get() = false

    val Background: Color get() = if (alternateDark) Color(0xFFF1F4F8) else KelimeKusatmasiPalette.Background
    val Surface: Color get() = KelimeKusatmasiPalette.Surface
    val SurfaceSecondary: Color get() = KelimeKusatmasiPalette.SurfaceSoft
    val SurfaceElevated: Color get() = KelimeKusatmasiPalette.SurfaceRaised
    val NavigationSurface: Color get() = Color(0xFFFCFDFF)
    val ModalSurface: Color get() = Color(0xFFFFFFFF)

    // Gameplay layers. Word Siege keeps its own map ownership palette; these values are shell-safe.
    val GameSurface: Color get() = Color(0xFFF2F6FA)
    val GameTile: Color get() = Color(0xFFFFFBF1)
    val GameTileBorder: Color get() = Color(0xFFD6C6A7)

    val Primary: Color get() = KelimeKusatmasiPalette.GameGreen
    val PrimarySoft: Color get() = KelimeKusatmasiPalette.GameGreenSoft
    val SoftBlue: Color get() = KelimeKusatmasiPalette.GameBlue
    val Turquoise: Color get() = KelimeKusatmasiPalette.GameTurquoise
    val ActionOrange: Color get() = KelimeKusatmasiPalette.GameOrange
    val Lavender: Color get() = KelimeKusatmasiPalette.GamePurple
    val Sand: Color get() = KelimeKusatmasiPalette.GameOrangeSoft

    val TextPrimary: Color get() = KelimeKusatmasiPalette.Ink
    val TextSecondary: Color get() = KelimeKusatmasiPalette.Muted
    val Border: Color get() = KelimeKusatmasiPalette.Border

    val Success: Color get() = KelimeKusatmasiPalette.GameGreenDeep
    val SuccessSoft: Color get() = KelimeKusatmasiPalette.GameGreenSoft
    val Error: Color get() = KelimeKusatmasiPalette.GameRed
    val Warning: Color get() = KelimeKusatmasiPalette.GameOrange
    val DisabledBackground: Color get() = Color(0xFFE9EEF5)
    val DisabledContent: Color get() = Color(0xFF9BA8BA)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    // Main Kelime Kuşatması hero: premium blue -> turquoise -> green, kept darker than cards so
    // white text and the primary CTA remain readable.
    val HeroStart: Color get() = Color(0xFF2E5FA7)
    val HeroMiddle: Color get() = Color(0xFF3D83C5)
    val HeroEnd: Color get() = Color(0xFF4A9B69)

    val Forest: Color get() = KelimeKusatmasiPalette.GameGreenDeep
    val ForestDeep: Color get() = Color(0xFF255E36)

    val PremiumGold: Color get() = KelimeKusatmasiPalette.Gold
    val PremiumGoldLight: Color get() = KelimeKusatmasiPalette.GoldLight

    val PrimaryBlue: Color get() = KelimeKusatmasiPalette.GameBlue
    val PrimaryBlueSoft: Color get() = KelimeKusatmasiPalette.GameBlueSoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
