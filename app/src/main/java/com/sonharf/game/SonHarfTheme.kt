package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Monster UI kit'ten türetilen ana görsel sistem.
 *
 * Satın alınan Figma kaynağının gerçek karakteri korunur: antrasit/siyah yüzeyler,
 * neon limon ana aksiyon rengi, kırmızı-pembe enerji vurguları ve yüksek kontrastlı
 * beyaz tipografi. Eski semantik isimler derleme uyumluluğu için alias olarak kalır.
 */
internal object KelimeKusatmasiPalette {
    val MonsterBlack = Color(0xFF0D0F12)
    val MonsterSurface = Color(0xFF15171C)
    val MonsterSurface2 = Color(0xFF1B1E24)
    val MonsterSurface3 = Color(0xFF22252D)
    val MonsterLime = Color(0xFFEFFF19)
    val MonsterRed = Color(0xFFFF3B30)
    val MonsterPink = Color(0xFFFF245C)
    val MonsterOrange = Color(0xFFFF5A36)
    val MonsterText = Color(0xFFF7F8FA)
    val MonsterMuted = Color(0xFF9AA0AA)
    val MonsterBorder = Color(0xFF2B2F37)

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
    val Background: Color get() = if (alternateDark) Color(0xFF090A0C) else KelimeKusatmasiPalette.MonsterBlack
    val Surface: Color get() = if (alternateDark) Color(0xFF111318) else KelimeKusatmasiPalette.MonsterSurface
    val SurfaceSecondary: Color get() = if (alternateDark) Color(0xFF171A20) else KelimeKusatmasiPalette.MonsterSurface2
    val SurfaceElevated: Color get() = if (alternateDark) Color(0xFF1D2027) else KelimeKusatmasiPalette.MonsterSurface3
    val NavigationSurface: Color get() = if (alternateDark) Color(0xFF0B0D10) else Color(0xFF111318)
    val ModalSurface: Color get() = if (alternateDark) Color(0xFF12151A) else Color(0xFF181A20)

    // Gameplay layers: dark map field, bright readable letter tiles.
    val GameSurface: Color get() = if (alternateDark) Color(0xFF101217) else Color(0xFF12151A)
    val GameTile: Color get() = Color(0xFFF3F4F2)
    val GameTileBorder: Color get() = if (alternateDark) Color(0xFF444951) else Color(0xFF3A3F48)

    // Monster brand/accent family.
    val Primary: Color get() = if (alternateDark) Color(0xFFE7FF00) else KelimeKusatmasiPalette.MonsterLime
    val PrimarySoft: Color get() = if (alternateDark) Color(0xFF252B0D) else Color(0xFF292F10)
    val SoftBlue: Color get() = if (alternateDark) Color(0xFFFF4772) else KelimeKusatmasiPalette.MonsterPink
    val Turquoise: Color get() = if (alternateDark) Color(0xFFD9F900) else Color(0xFFDFFF00)
    val ActionOrange: Color get() = if (alternateDark) Color(0xFFFF4D37) else KelimeKusatmasiPalette.MonsterRed
    val Lavender: Color get() = if (alternateDark) Color(0xFFFF3A6D) else KelimeKusatmasiPalette.MonsterPink
    val Sand: Color get() = if (alternateDark) Color(0xFFFF7356) else KelimeKusatmasiPalette.MonsterOrange

    // Text and dividers.
    val TextPrimary: Color get() = KelimeKusatmasiPalette.MonsterText
    val TextSecondary: Color get() = if (alternateDark) Color(0xFFA5AAB3) else KelimeKusatmasiPalette.MonsterMuted
    val Border: Color get() = if (alternateDark) Color(0xFF262A31) else KelimeKusatmasiPalette.MonsterBorder

    // Semantic states.
    val Success: Color get() = Color(0xFFC8F000)
    val SuccessSoft: Color get() = Color(0xFF242A0E)
    val Error: Color get() = Color(0xFFFF4B55)
    val Warning: Color get() = Color(0xFFFF8A3D)
    val DisabledBackground: Color get() = Color(0xFF262930)
    val DisabledContent: Color get() = Color(0xFF6F7580)

    val OnPrimary: Color get() = Color(0xFF0B0C0E)
    val OnSecondary: Color get() = Color(0xFF0B0C0E)
    val OnTertiary: Color get() = Color(0xFF0B0C0E)

    // Signature Monster red -> pink hero gradient.
    val HeroStart: Color get() = Color(0xFFFF4A22)
    val HeroMiddle: Color get() = Color(0xFFFF3435)
    val HeroEnd: Color get() = Color(0xFFFF245C)

    // Compatibility naming used by existing hero/game cards.
    val Forest: Color get() = Color(0xFFFF3B30)
    val ForestDeep: Color get() = Color(0xFF13151A)

    // Premium prestige remains gold; standard actions use Monster lime.
    val PremiumGold: Color get() = Color(0xFFD9AD45)
    val PremiumGoldLight: Color get() = Color(0xFFF4D77F)

    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
