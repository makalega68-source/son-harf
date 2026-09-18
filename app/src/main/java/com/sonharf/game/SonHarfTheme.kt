package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Kelime Kuşatması'nın ana renk sistemi.
 *
 * Satın alınan kompakt spor-dashboard UI kitinin yüksek kontrastlı kart ve vurgu yaklaşımı,
 * ürünün mavi + turkuaz + turuncu marka yönüne uyarlanır. İsim alias'ları eski ekranların
 * derlenmesini korur; tüm yeni görsel kararlar bu tek kaynaktan çözülür.
 */
internal object KelimeKusatmasiPalette {
    val RoyalBlue = Color(0xFF1559D6)
    val DeepBlue = Color(0xFF0A347A)
    val Turquoise = Color(0xFF15C7C4)
    val Orange = Color(0xFFFF8A24)
    val Sky = Color(0xFFEAF4FF)
    val OffWhite = Color(0xFFF8FBFF)
    val Slate = Color(0xFF53677D)
    val PaleMint = Color(0xFFDDF8F5)
    val SoftIndigo = Color(0xFF728BE8)

    // Compatibility aliases for older screens that still use the former semantic names.
    val SageGreen: Color get() = RoyalBlue
    val SoftBlue: Color get() = SoftIndigo
    val LightBeige: Color get() = Sky
    val Lavender: Color get() = SoftIndigo
    val SlateBlue: Color get() = Slate
    val WarmAccent: Color get() = Orange
}

/**
 * Application-wide visual system.
 *
 * The purchased kit is a Figma design source rather than executable Android code. Its compact
 * cards, strong hierarchy and high-contrast action language are therefore translated into the
 * existing Compose architecture instead of replacing navigation, gameplay or backend behavior.
 * Cosmetic themes never change gameplay state, scoring, economy or authorization.
 */
internal object SonHarfTheme {
    private val dark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    val IsDark: Boolean get() = dark

    // Foundation layers: cool white/sky in the default experience; deep navy in dark arena.
    val Background: Color get() = if (dark) Color(0xFF061525) else KelimeKusatmasiPalette.OffWhite
    val Surface: Color get() = if (dark) Color(0xFF0B2037) else Color.White
    val SurfaceSecondary: Color get() = if (dark) Color(0xFF102C49) else KelimeKusatmasiPalette.Sky
    val SurfaceElevated: Color get() = if (dark) Color(0xFF163A59) else Color(0xFFF1F7FF)
    val NavigationSurface: Color get() = if (dark) Color(0xFF091D33) else Color(0xFFEDF6FF)
    val ModalSurface: Color get() = if (dark) Color(0xFF0D2741) else Color.White

    // Gameplay remains highly legible while adopting the blue/turquoise identity.
    val GameSurface: Color get() = if (dark) Color(0xFF0A263D) else KelimeKusatmasiPalette.PaleMint
    val GameTile: Color get() = if (dark) Color(0xFFEAF4FF) else Color(0xFFFCFEFF)
    val GameTileBorder: Color get() = if (dark) Color(0xFF25D6CE) else Color(0xFF6B91C5)

    // Brand/accent family.
    val Primary: Color get() = if (dark) Color(0xFF2F7CF6) else KelimeKusatmasiPalette.RoyalBlue
    val PrimarySoft: Color get() = if (dark) Color(0xFF17385C) else Color(0xFFDCEAFF)
    val SoftBlue: Color get() = if (dark) Color(0xFF4B8DDB) else KelimeKusatmasiPalette.SoftIndigo
    val Turquoise: Color get() = if (dark) Color(0xFF25D6CE) else KelimeKusatmasiPalette.Turquoise
    val ActionOrange: Color get() = if (dark) Color(0xFFFF9F40) else KelimeKusatmasiPalette.Orange
    val Lavender: Color get() = if (dark) Color(0xFF8796F4) else KelimeKusatmasiPalette.SoftIndigo
    val Sand: Color get() = if (dark) Color(0xFFFFC387) else Color(0xFFFFE7D0)

    // Text and dividers.
    val TextPrimary: Color get() = if (dark) Color(0xFFF5FAFF) else Color(0xFF10233F)
    val TextSecondary: Color get() = if (dark) Color(0xFFAFC6DA) else KelimeKusatmasiPalette.Slate
    val Border: Color get() = if (dark) Color(0xFF274A65) else Color(0xFFD5E5F2)

    // Semantic states remain distinct from brand colors for accessibility.
    val Success: Color get() = if (dark) Color(0xFF25C7B8) else Color(0xFF159E93)
    val SuccessSoft: Color get() = if (dark) Color(0xFF123D3E) else KelimeKusatmasiPalette.PaleMint
    val Error: Color get() = if (dark) Color(0xFFFF7180) else Color(0xFFD94D5C)
    val Warning: Color get() = if (dark) Color(0xFFFF9F40) else Color(0xFFE86F0E)
    val DisabledBackground: Color get() = if (dark) Color(0xFF20344A) else Color(0xFFE6EDF5)
    val DisabledContent: Color get() = if (dark) Color(0xFF7890A5) else KelimeKusatmasiPalette.Slate.copy(alpha = .70f)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = if (dark) Color(0xFF042E38) else Color(0xFF063F4A)
    val OnTertiary: Color get() = Color.White

    // High-energy hero gradient inspired by the purchased dashboard hierarchy.
    val HeroStart: Color get() = if (dark) Color(0xFF0B2C68) else Color(0xFF0A347A)
    val HeroMiddle: Color get() = if (dark) Color(0xFF1559D6) else Color(0xFF1767D9)
    val HeroEnd: Color get() = if (dark) Color(0xFF0E8F9D) else Color(0xFF15AEB1)

    // Legacy naming kept for existing play-card code; values now resolve to the blue family.
    val Forest: Color get() = if (dark) Color(0xFF0E4F9D) else Color(0xFF1559D6)
    val ForestDeep: Color get() = if (dark) Color(0xFF072C62) else Color(0xFF0A347A)

    // Premium gold remains reserved for PRO/prestige; orange is the normal action accent.
    val PremiumGold: Color get() = Color(0xFFD9AD45)
    val PremiumGoldLight: Color get() = Color(0xFFF4D77F)

    // Compatibility aliases. Legacy screens keep compiling while resolving to the same system.
    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
