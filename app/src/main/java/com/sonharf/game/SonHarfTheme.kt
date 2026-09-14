package com.sonharf.game

import androidx.compose.ui.graphics.Color

/** Canonical light-mode palette from Kelime Kuşatması Master GDD v3.0. */
internal object KelimeKusatmasiPalette {
    val SageGreen = Color(0xFF8A9A86)
    val OffWhite = Color(0xFFF9F8F6)
    val SoftBlue = Color(0xFF7A9AEE)
    val Turquoise = Color(0xFF40E0D0)
    val LightBeige = Color(0xFFF2EFE9)
    val Lavender = Color(0xFFB5A2FF)
    val SlateBlue = Color(0xFF5C6F84)
    val PaleMint = Color(0xFFA3E4D7)
    val WarmAccent = Color(0xFFE07A5F)
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
    val Surface: Color get() = if (dark) Color(0xFF18241E) else KelimeKusatmasiPalette.OffWhite
    val SurfaceSecondary: Color get() = if (dark) Color(0xFF213129) else KelimeKusatmasiPalette.LightBeige
    val SurfaceElevated: Color get() = if (dark) Color(0xFF293A32) else Color(0xFFF6F3EE)
    val NavigationSurface: Color get() = if (dark) Color(0xFF1A2821) else KelimeKusatmasiPalette.LightBeige
    val ModalSurface: Color get() = if (dark) Color(0xFF202E27) else KelimeKusatmasiPalette.OffWhite

    // Gameplay layers: pale mint map field and neutral light-beige letter surfaces.
    val GameSurface: Color get() = if (dark) Color(0xFF1A2B27) else KelimeKusatmasiPalette.PaleMint
    val GameTile: Color get() = if (dark) Color(0xFF4A4336) else KelimeKusatmasiPalette.LightBeige
    val GameTileBorder: Color get() = if (dark) Color(0xFF817150) else KelimeKusatmasiPalette.SageGreen

    // Brand/accent family. Primary remains a contrast-safe action shade; SageGreen is the
    // canonical ambient brand tone and is exposed through PrimarySoft/territory surfaces.
    val Primary: Color get() = if (dark) Color(0xFF557B67) else Color(0xFF526652)
    val PrimarySoft: Color get() = if (dark) Color(0xFF293D32) else KelimeKusatmasiPalette.SageGreen
    val SoftBlue: Color get() = if (dark) Color(0xFF55798A) else KelimeKusatmasiPalette.SoftBlue
    val Turquoise: Color get() = if (dark) Color(0xFF4C817C) else KelimeKusatmasiPalette.Turquoise
    val Lavender: Color get() = if (dark) Color(0xFF7C7196) else KelimeKusatmasiPalette.Lavender
    val Sand: Color get() = if (dark) Color(0xFFD3BE91) else KelimeKusatmasiPalette.LightBeige

    // Text and dividers.
    val TextPrimary: Color get() = if (dark) Color(0xFFF3F6F2) else Color(0xFF263D36)
    val TextSecondary: Color get() = if (dark) Color(0xFFB8C8BF) else KelimeKusatmasiPalette.SlateBlue
    val Border: Color get() = if (dark) Color(0xFF3C5448) else Color(0xFFD9D8D3)

    // Semantic states from the GDD family.
    val Success: Color get() = if (dark) Color(0xFF5C876C) else Color(0xFF4F8177)
    val SuccessSoft: Color get() = if (dark) Color(0xFF294038) else KelimeKusatmasiPalette.PaleMint
    val Error: Color get() = if (dark) Color(0xFFB95C66) else KelimeKusatmasiPalette.WarmAccent
    val Warning: Color get() = if (dark) Color(0xFFC19853) else Color(0xFF9B733B)
    val DisabledBackground: Color get() = if (dark) Color(0xFF2B3932) else Color(0xFFE7E5E0)
    val DisabledContent: Color get() = if (dark) Color(0xFF83948B) else KelimeKusatmasiPalette.SlateBlue.copy(alpha = .72f)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color(0xFF17332D)
    val OnTertiary: Color get() = Color(0xFF17332D)

    // Home/profile hero gradient uses the brand family without reverting to neon colors.
    val HeroStart: Color get() = if (dark) Color(0xFF20362C) else Color(0xFF667A63)
    val HeroMiddle: Color get() = if (dark) Color(0xFF274044) else Color(0xFF617BA0)
    val HeroEnd: Color get() = if (dark) Color(0xFF342F48) else Color(0xFF8175A6)

    // Premium leaderboard / CTA accents.
    val Forest: Color get() = if (dark) Color(0xFF173329) else Color(0xFF245A49)
    val ForestDeep: Color get() = if (dark) Color(0xFF10261F) else Color(0xFF16483B)
    val PremiumGold: Color get() = Color(0xFFD7B35C)
    val PremiumGoldLight: Color get() = Color(0xFFF2D98A)

    // Compatibility aliases. Legacy screens keep compiling while resolving to the same system.
    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
