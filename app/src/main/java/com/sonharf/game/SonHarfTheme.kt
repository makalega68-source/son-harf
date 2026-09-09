package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the equipped application-wide visual system.
 * Style selections are cosmetic only and must never alter gameplay state.
 *
 * The palette is intentionally layered: backgrounds, cards, navigation,
 * dialogs, gameplay surfaces and interactive accents each have their own
 * semantic token instead of sharing one flat brand color.
 */
internal object SonHarfTheme {
    private val dark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    val IsDark: Boolean get() = dark

    // Foundation layers: sage-tinted off-white / deep desaturated sage.
    val Background: Color get() = if (dark) Color(0xFF101713) else Color(0xFFF4F7F2)
    val Surface: Color get() = if (dark) Color(0xFF17221D) else Color(0xFFFFFDF7)
    val SurfaceSecondary: Color get() = if (dark) Color(0xFF21302A) else Color(0xFFEAF2EE)
    val SurfaceElevated: Color get() = if (dark) Color(0xFF26343A) else Color(0xFFEFF4F6)
    val NavigationSurface: Color get() = if (dark) Color(0xFF1B2923) else Color(0xFFEEF3F0)
    val ModalSurface: Color get() = if (dark) Color(0xFF202B25) else Color(0xFFFAF7F0)

    // Gameplay layers: mint board surface + warm sand tiles.
    val GameSurface: Color get() = if (dark) Color(0xFF182A27) else Color(0xFFEDF4F2)
    val GameTile: Color get() = if (dark) Color(0xFF4B4437) else Color(0xFFF1E7D3)
    val GameTileBorder: Color get() = if (dark) Color(0xFF7C6E50) else Color(0xFFC6AD7D)

    // Calm accent family.
    val Primary: Color get() = if (dark) Color(0xFFA9C7B4) else Color(0xFF4F725E) // sage
    val PrimarySoft: Color get() = if (dark) Color(0xFF273B30) else Color(0xFFDDE9E1)
    val SoftBlue: Color get() = if (dark) Color(0xFFA4BFCE) else Color(0xFF4A6E83)
    val Turquoise: Color get() = if (dark) Color(0xFF8EBBB4) else Color(0xFF477B78)
    val Lavender: Color get() = if (dark) Color(0xFFB9ABD6) else Color(0xFF7B6B95)
    val Sand: Color get() = if (dark) Color(0xFFD1BC91) else Color(0xFFD7C49F)

    // Text and dividers.
    val TextPrimary: Color get() = if (dark) Color(0xFFF2F5F0) else Color(0xFF26382F)
    val TextSecondary: Color get() = if (dark) Color(0xFFB7C5BD) else Color(0xFF65766D)
    val Border: Color get() = if (dark) Color(0xFF3D5147) else Color(0xFFCCD8D1)

    // Semantic states stay recognizable but use desaturated, calmer tones.
    val Success: Color get() = if (dark) Color(0xFF91C8A4) else Color(0xFF4B765D)
    val Error: Color get() = if (dark) Color(0xFFD99CA2) else Color(0xFFA84F59)
    val Warning: Color get() = if (dark) Color(0xFFD3B57C) else Color(0xFF8A6538)
    val DisabledBackground: Color get() = if (dark) Color(0xFF29372F) else Color(0xFFE2E8E3)
    val DisabledContent: Color get() = if (dark) Color(0xFF7F9187) else Color(0xFF809087)

    // Content colors for filled controls.
    val OnPrimary: Color get() = if (dark) Color(0xFF18261F) else Color.White
    val OnSecondary: Color get() = if (dark) Color(0xFF15222A) else Color.White
    val OnTertiary: Color get() = if (dark) Color(0xFF17231E) else Color.White

    // Soft hero gradient used by the home/profile summary layer.
    val HeroStart: Color get() = if (dark) Color(0xFF20332A) else Color(0xFF607E6B)
    val HeroMiddle: Color get() = if (dark) Color(0xFF263B3D) else Color(0xFF557A83)
    val HeroEnd: Color get() = if (dark) Color(0xFF332F43) else Color(0xFF756B8E)

    // Compatibility aliases. Existing screens keep compiling while every
    // legacy token now resolves to the new layered calm palette.
    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
