package com.sonharf.game

import androidx.compose.ui.graphics.Color

/** Canonical light-mode palette from Kelime Kuşatması Master GDD v3.0. */
internal object KelimeKusatmasiPalette {
    // Dynamic 2026 palette: energetic enough to feel like a game, controlled enough to stay premium.
    val RoyalBlue = Color(0xFF2F6BFF)
    val Turquoise = Color(0xFF20C9C3)
    val Coral = Color(0xFFFF6B6B)
    val WarmOrange = Color(0xFFFF9F43)
    val Gold = Color(0xFFF4C95D)
    val Navy = Color(0xFF22324D)
    val OffWhite = Color(0xFFF8F7F2)
    val SoftBlue = Color(0xFF6C8DFF)
    val LightBeige = Color(0xFFF1F3F8)
    val Lavender = Color(0xFF8B5CF6)
    val SlateBlue = Color(0xFF607086)
    val PaleMint = Color(0xFFDDF9F5)

    // Compatibility aliases for older screens while the app moves to the dynamic palette.
    val SageGreen = RoyalBlue
    val WarmAccent = Coral
}

/**
 * Kelime Kuşatması application-wide visual system.
 *
 * The GDD palette is applied to the light/default experience while the established dark arena
 * remains available as an optional cosmetic treatment. Semantic CTA colors retain sufficient
 * contrast instead of forcing every GDD ambient token into an action role.
 *
 * Cosmetic themes never change gameplay state, scoring or economy.
 */
internal object SonHarfTheme {
    private val dark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    val IsDark: Boolean get() = dark

    // Foundation layers.
    val Background: Color get() = if (dark) Color(0xFF101914) else KelimeKusatmasiPalette.OffWhite
    val Surface: Color get() = if (dark) Color(0xFF18241E) else Color.White
    val SurfaceSecondary: Color get() = if (dark) Color(0xFF213129) else Color(0xFFF0F3FA)
    val SurfaceElevated: Color get() = if (dark) Color(0xFF293A32) else Color(0xFFFFFFFF)
    val NavigationSurface: Color get() = if (dark) Color(0xFF1A2821) else Color(0xFFF2F5FB)
    val ModalSurface: Color get() = if (dark) Color(0xFF202E27) else KelimeKusatmasiPalette.OffWhite

    // Gameplay layers: pale mint map field and neutral light-beige letter surfaces.
    val GameSurface: Color get() = if (dark) Color(0xFF1A2B27) else Color(0xFFEAF0FF)
    val GameTile: Color get() = if (dark) Color(0xFF4A4336) else KelimeKusatmasiPalette.LightBeige
    val GameTileBorder: Color get() = if (dark) Color(0xFF817150) else Color(0xFF8EA9FF)

    // Brand/accent family. Primary remains a contrast-safe action shade; SageGreen is the
    // canonical ambient brand tone and is exposed through PrimarySoft/territory surfaces.
    val Primary: Color get() = if (dark) Color(0xFF557B67) else KelimeKusatmasiPalette.RoyalBlue
    val PrimarySoft: Color get() = if (dark) Color(0xFF293D32) else Color(0xFFDCE7FF)
    val SoftBlue: Color get() = if (dark) Color(0xFF55798A) else KelimeKusatmasiPalette.SoftBlue
    val Turquoise: Color get() = if (dark) Color(0xFF4C817C) else KelimeKusatmasiPalette.Turquoise
    val Lavender: Color get() = if (dark) Color(0xFF7C7196) else KelimeKusatmasiPalette.Lavender
    val Sand: Color get() = if (dark) Color(0xFFD3BE91) else KelimeKusatmasiPalette.LightBeige
    val WarmOrange: Color get() = if (dark) Color(0xFFC98243) else KelimeKusatmasiPalette.WarmOrange

    // Text and dividers.
    val TextPrimary: Color get() = if (dark) Color(0xFFF3F6F2) else KelimeKusatmasiPalette.Navy
    val TextSecondary: Color get() = if (dark) Color(0xFFB8C8BF) else KelimeKusatmasiPalette.SlateBlue
    val Border: Color get() = if (dark) Color(0xFF3C5448) else Color(0xFFD7DEEA)

    // Semantic states from the GDD family.
    val Success: Color get() = if (dark) Color(0xFF5C876C) else Color(0xFF16A6A0)
    val SuccessSoft: Color get() = if (dark) Color(0xFF294038) else KelimeKusatmasiPalette.PaleMint
    val Error: Color get() = if (dark) Color(0xFFB95C66) else KelimeKusatmasiPalette.Coral
    val Warning: Color get() = if (dark) Color(0xFFC19853) else KelimeKusatmasiPalette.WarmOrange
    val DisabledBackground: Color get() = if (dark) Color(0xFF2B3932) else Color(0xFFE7E5E0)
    val DisabledContent: Color get() = if (dark) Color(0xFF83948B) else KelimeKusatmasiPalette.SlateBlue.copy(alpha = .72f)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = KelimeKusatmasiPalette.Navy
    val OnTertiary: Color get() = KelimeKusatmasiPalette.Navy

    // Home/profile hero gradient uses the brand family without reverting to neon colors.
    val HeroStart: Color get() = if (dark) Color(0xFF20362C) else Color(0xFF2854D8)
    val HeroMiddle: Color get() = if (dark) Color(0xFF274044) else KelimeKusatmasiPalette.RoyalBlue
    val HeroEnd: Color get() = if (dark) Color(0xFF342F48) else Color(0xFF6E5AE8)

    // Premium leaderboard / CTA accents.
    val Forest: Color get() = if (dark) Color(0xFF173329) else KelimeKusatmasiPalette.RoyalBlue
    val ForestDeep: Color get() = if (dark) Color(0xFF10261F) else Color(0xFF1F4DD8)
    val PremiumGold: Color get() = KelimeKusatmasiPalette.Gold
    val PremiumGoldLight: Color get() = Color(0xFFFFD96B)

    // Compatibility aliases. Legacy screens keep compiling while resolving to the same system.
    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
