package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Kelime Kuşatması'nın ortak, yetişkin ve okunaklı görsel paleti.
 *
 * Hedef; kelime oyunlarına uygun sakin bir zemin, stratejik rekabeti taşıyan koyu yeşil,
 * sınırlı mavi/terracotta vurgular ve yalnız prestij anlarında kullanılan yumuşak altındır.
 * Eski semantik isimler derleme uyumluluğu için korunur; neon/oyuncak görünüm kullanılmaz.
 */
internal object KelimeKusatmasiPalette {
    val Paper = Color(0xFFF6F4EE)
    val Surface = Color(0xFFFFFEFA)
    val SurfaceSoft = Color(0xFFF0F2EC)
    val SurfaceRaised = Color(0xFFE8ECE5)
    val Forest = Color(0xFF285943)
    val ForestDeep = Color(0xFF173B2E)
    val Sage = Color(0xFF77977F)
    val MistBlue = Color(0xFF6F8794)
    val Teal = Color(0xFF4F7C74)
    val Terracotta = Color(0xFFAA6255)
    val SoftGold = Color(0xFFB58A39)
    val Sand = Color(0xFFD5C5A7)
    val Ink = Color(0xFF18322A)
    val Muted = Color(0xFF66766F)
    val Border = Color(0xFFD9DED7)

    // Compatibility aliases used by older screens.
    val MonsterBlack: Color get() = Paper
    val MonsterSurface: Color get() = Surface
    val MonsterSurface2: Color get() = SurfaceSoft
    val MonsterSurface3: Color get() = SurfaceRaised
    val MonsterLime: Color get() = Forest
    val MonsterRed: Color get() = Terracotta
    val MonsterPink: Color get() = MistBlue
    val MonsterOrange: Color get() = SoftGold
    val MonsterText: Color get() = Ink
    val MonsterMuted: Color get() = Muted
    val MonsterBorder: Color get() = Border

    val RoyalBlue: Color get() = Forest
    val DeepBlue: Color get() = ForestDeep
    val Turquoise: Color get() = Teal
    val Orange: Color get() = SoftGold
    val Sky: Color get() = MistBlue
    val OffWhite: Color get() = Paper
    val Slate: Color get() = Muted
    val PaleMint: Color get() = SurfaceSoft
    val SoftIndigo: Color get() = MistBlue
    val SageGreen: Color get() = Sage
    val SoftBlue: Color get() = MistBlue
    val LightBeige: Color get() = Sand
    val Lavender: Color get() = Color(0xFF8A7C98)
    val SlateBlue: Color get() = MistBlue
    val WarmAccent: Color get() = Terracotta
}

/**
 * Application-wide visual system. Gameplay, backend and data-flow remain untouched.
 * The optional dark arena cosmetic is still respected only where it was already supported.
 */
internal object SonHarfTheme {
    private val alternateDark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    // The default product is deliberately light and calm; game screens may keep mode-specific
    // presentation when that is required for legibility.
    val IsDark: Boolean get() = alternateDark

    val Background: Color get() = if (alternateDark) Color(0xFF101512) else KelimeKusatmasiPalette.Paper
    val Surface: Color get() = if (alternateDark) Color(0xFF18201B) else KelimeKusatmasiPalette.Surface
    val SurfaceSecondary: Color get() = if (alternateDark) Color(0xFF202923) else KelimeKusatmasiPalette.SurfaceSoft
    val SurfaceElevated: Color get() = if (alternateDark) Color(0xFF28322C) else KelimeKusatmasiPalette.SurfaceRaised
    val NavigationSurface: Color get() = if (alternateDark) Color(0xFF151C18) else Color(0xFFFBFAF6)
    val ModalSurface: Color get() = if (alternateDark) Color(0xFF1B241E) else KelimeKusatmasiPalette.Surface

    // Shared game layers use warm neutral tiles and restrained contrast.
    val GameSurface: Color get() = if (alternateDark) Color(0xFF18201B) else Color(0xFFEEF1EB)
    val GameTile: Color get() = if (alternateDark) Color(0xFFF0EEE6) else Color(0xFFFBF5E8)
    val GameTileBorder: Color get() = if (alternateDark) Color(0xFF59655E) else Color(0xFFC9C3B5)

    val Primary: Color get() = if (alternateDark) Color(0xFF8FB89B) else KelimeKusatmasiPalette.Forest
    val PrimarySoft: Color get() = if (alternateDark) Color(0xFF24382D) else Color(0xFFE1EADF)
    val SoftBlue: Color get() = if (alternateDark) Color(0xFF89A1AC) else KelimeKusatmasiPalette.MistBlue
    val Turquoise: Color get() = if (alternateDark) Color(0xFF7FA99F) else KelimeKusatmasiPalette.Teal
    val ActionOrange: Color get() = if (alternateDark) Color(0xFFD4A85A) else KelimeKusatmasiPalette.SoftGold
    val Lavender: Color get() = if (alternateDark) Color(0xFFA89DB1) else Color(0xFF8A7C98)
    val Sand: Color get() = if (alternateDark) Color(0xFFC4B38F) else KelimeKusatmasiPalette.Sand

    val TextPrimary: Color get() = if (alternateDark) Color(0xFFF2F4F1) else KelimeKusatmasiPalette.Ink
    val TextSecondary: Color get() = if (alternateDark) Color(0xFFB8C2BC) else KelimeKusatmasiPalette.Muted
    val Border: Color get() = if (alternateDark) Color(0xFF35423A) else KelimeKusatmasiPalette.Border

    val Success: Color get() = if (alternateDark) Color(0xFF7FB08A) else Color(0xFF3F7C53)
    val SuccessSoft: Color get() = if (alternateDark) Color(0xFF23382A) else Color(0xFFE1ECDD)
    val Error: Color get() = if (alternateDark) Color(0xFFD17872) else Color(0xFFA4554F)
    val Warning: Color get() = if (alternateDark) Color(0xFFD2A45B) else Color(0xFFB9823A)
    val DisabledBackground: Color get() = if (alternateDark) Color(0xFF2A302C) else Color(0xFFE7E9E4)
    val DisabledContent: Color get() = if (alternateDark) Color(0xFF808A84) else Color(0xFF8B948F)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    // Home hero: layered forest rather than saturated arcade gradients.
    val HeroStart: Color get() = if (alternateDark) Color(0xFF183C2E) else Color(0xFF1E4B3A)
    val HeroMiddle: Color get() = if (alternateDark) Color(0xFF244B3A) else Color(0xFF2B5D48)
    val HeroEnd: Color get() = if (alternateDark) Color(0xFF385B48) else Color(0xFF54765F)

    val Forest: Color get() = Primary
    val ForestDeep: Color get() = if (alternateDark) Color(0xFF14271F) else KelimeKusatmasiPalette.ForestDeep

    val PremiumGold: Color get() = if (alternateDark) Color(0xFFD0AD62) else KelimeKusatmasiPalette.SoftGold
    val PremiumGoldLight: Color get() = if (alternateDark) Color(0xFFE5CB91) else Color(0xFFD6B66F)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
