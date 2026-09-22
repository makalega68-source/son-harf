package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Satın alınan aksiyon/spor UI kaynağından türetilen yetişkin görsel sistem.
 * Antrasit ve koyu zümrüt yüzeyler, kontrollü altın vurgu ve yüksek okunabilirlik
 * bütün ekranlarda aynı hiyerarşiyi kurar. Eski semantik isimler yalnızca derleme
 * uyumluluğu için alias olarak kalır.
 */
internal object KelimeKusatmasiPalette {
    val MonsterBlack = Color(0xFF071714)
    val MonsterSurface = Color(0xFF0E2521)
    val MonsterSurface2 = Color(0xFF14312B)
    val MonsterSurface3 = Color(0xFF1B3B33)
    val MonsterLime = Color(0xFF3FC486)
    val MonsterRed = Color(0xFFC94C4C)
    val MonsterPink = Color(0xFFB99445)
    val MonsterOrange = Color(0xFFD1AD58)
    val MonsterText = Color(0xFFF4F7F4)
    val MonsterMuted = Color(0xFF9DB0A9)
    val MonsterBorder = Color(0xFF315148)

    // Compatibility aliases for screens that still reference the previous palette names.
    val RoyalBlue: Color get() = MonsterLime
    val DeepBlue: Color get() = MonsterBlack
    val Turquoise: Color get() = MonsterLime
    val Orange: Color get() = MonsterRed
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
    val WarmAccent: Color get() = MonsterRed
}

/**
 * Application-wide Monster visual system.
 *
 * The purchased kit is a Figma design source. It is translated into the existing Compose
 * architecture without replacing gameplay, navigation, backend, scoring or authorization.
 */
internal object SonHarfTheme {
    private val alternateDark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    // Monster itself is a dark-first UI. Keeping this true makes every Material shell use the
    // correct dark color scheme even when the optional arena cosmetic is not equipped.
    val IsDark: Boolean get() = true

    // Foundation layers.
    val Background: Color get() = if (alternateDark) Color(0xFF050F0D) else KelimeKusatmasiPalette.MonsterBlack
    val Surface: Color get() = if (alternateDark) Color(0xFF0B1D1A) else KelimeKusatmasiPalette.MonsterSurface
    val SurfaceSecondary: Color get() = if (alternateDark) Color(0xFF102923) else KelimeKusatmasiPalette.MonsterSurface2
    val SurfaceElevated: Color get() = if (alternateDark) Color(0xFF17352E) else KelimeKusatmasiPalette.MonsterSurface3
    val NavigationSurface: Color get() = if (alternateDark) Color(0xFF06120F) else Color(0xFF091A17)
    val ModalSurface: Color get() = if (alternateDark) Color(0xFF0B1F1B) else Color(0xFF102923)

    // Gameplay layers: dark map field, bright readable letter tiles.
    val GameSurface: Color get() = if (alternateDark) Color(0xFF081512) else Color(0xFF0A1C18)
    val GameTile: Color get() = Color(0xFFF0EEE5)
    val GameTileBorder: Color get() = if (alternateDark) Color(0xFF4A5D56) else Color(0xFF3A534A)

    // Monster brand/accent family.
    val Primary: Color get() = if (alternateDark) Color(0xFF45C98B) else KelimeKusatmasiPalette.MonsterLime
    val PrimarySoft: Color get() = if (alternateDark) Color(0xFF15372C) else Color(0xFF173C31)
    val SoftBlue: Color get() = if (alternateDark) Color(0xFFC6A657) else KelimeKusatmasiPalette.MonsterPink
    val Turquoise: Color get() = if (alternateDark) Color(0xFF68D3A0) else Color(0xFF55CC91)
    val ActionOrange: Color get() = if (alternateDark) Color(0xFFD5B25E) else KelimeKusatmasiPalette.MonsterOrange
    val Lavender: Color get() = if (alternateDark) Color(0xFFB89A54) else KelimeKusatmasiPalette.MonsterPink
    val Sand: Color get() = if (alternateDark) Color(0xFFD9BF7B) else Color(0xFFCFAE63)

    // Text and dividers.
    val TextPrimary: Color get() = KelimeKusatmasiPalette.MonsterText
    val TextSecondary: Color get() = if (alternateDark) Color(0xFFA3B2AC) else KelimeKusatmasiPalette.MonsterMuted
    val Border: Color get() = if (alternateDark) Color(0xFF29473F) else KelimeKusatmasiPalette.MonsterBorder

    // Semantic states.
    val Success: Color get() = Color(0xFF45C98B)
    val SuccessSoft: Color get() = Color(0xFF15372C)
    val Error: Color get() = Color(0xFFC94C4C)
    val Warning: Color get() = Color(0xFFD5A34F)
    val DisabledBackground: Color get() = Color(0xFF263833)
    val DisabledContent: Color get() = Color(0xFF71847D)

    val OnPrimary: Color get() = Color(0xFF0B0C0E)
    val OnSecondary: Color get() = Color(0xFF0B0C0E)
    val OnTertiary: Color get() = Color(0xFF0B0C0E)

    // Tek yönlü koyu zümrüt hero geçişi; dikkat dağıtan neon/pembe kaldırıldı.
    val HeroStart: Color get() = Color(0xFF214D40)
    val HeroMiddle: Color get() = Color(0xFF15382F)
    val HeroEnd: Color get() = Color(0xFF0C2420)

    // Compatibility naming used by existing hero/game cards.
    val Forest: Color get() = Color(0xFF3FC486)
    val ForestDeep: Color get() = Color(0xFF071714)

    // Premium prestige remains gold; standard actions use Monster lime.
    val PremiumGold: Color get() = Color(0xFFC9A552)
    val PremiumGoldLight: Color get() = Color(0xFFE4C978)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
