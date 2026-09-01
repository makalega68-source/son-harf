package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Color-only variants for the purchased Monster layout.
 * Layout, spacing and gameplay stay identical; only visual tokens change.
 */
data class SonHarfThemePalette(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSoft: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val accentText: Color,
    val live: Color,
    val coral: Color,
    val orange: Color,
    val green: Color,
    val gold: Color,
)

object SonHarfThemeCatalog {
    const val BLUE_WHITE_ID = "theme_monster_blue"
    const val CHARCOAL_IVORY_ID = "theme_monster_charcoal_ivory"
    const val SAPPHIRE_ICE_ID = "theme_monster_sapphire_ice"

    /** Existing purchased layout palette. */
    val blueWhite = SonHarfThemePalette(
        id = BLUE_WHITE_ID,
        nameTr = "Mavi Beyaz Arena",
        nameEn = "Blue White Arena",
        background = Color(0xFFF7FAFF),
        surface = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFF0F5FC),
        surfaceSoft = Color(0xFFE8EEF7),
        text = Color(0xFF10213A),
        muted = Color(0xFF62758F),
        border = Color(0xFFD5E2F0),
        accent = Color(0xFF1677FF),
        accentText = Color(0xFFFFFFFF),
        live = Color(0xFFFF4D4F),
        coral = Color(0xFFFF6B61),
        orange = Color(0xFFF59E0B),
        green = Color(0xFF168A55),
        gold = Color(0xFFD68A00),
    )

    /** Premium dark: charcoal/fume surfaces, ivory typography and champagne accent. */
    val charcoalIvory = SonHarfThemePalette(
        id = CHARCOAL_IVORY_ID,
        nameTr = "Füme Fildişi",
        nameEn = "Charcoal Ivory",
        background = Color(0xFF202225),
        surface = Color(0xFF2B2D31),
        surfaceRaised = Color(0xFF34373D),
        surfaceSoft = Color(0xFF3E424A),
        text = Color(0xFFF6EFE4),
        muted = Color(0xFFC7BFB2),
        border = Color(0xFF565B63),
        accent = Color(0xFFE1BD73),
        accentText = Color(0xFF1A1B1E),
        live = Color(0xFFFF6F61),
        coral = Color(0xFFFF8173),
        orange = Color(0xFFE9A54D),
        green = Color(0xFF62D5A0),
        gold = Color(0xFFE1BD73),
    )

    /** Premium cool: deep sapphire surfaces, ice-white typography and glacial cyan accent. */
    val sapphireIce = SonHarfThemePalette(
        id = SAPPHIRE_ICE_ID,
        nameTr = "Safir Buz",
        nameEn = "Sapphire Ice",
        background = Color(0xFF0E1A2B),
        surface = Color(0xFF14243A),
        surfaceRaised = Color(0xFF1A304D),
        surfaceSoft = Color(0xFF23405F),
        text = Color(0xFFF4F8FF),
        muted = Color(0xFFAFC0D6),
        border = Color(0xFF3B5878),
        accent = Color(0xFF66D1FF),
        accentText = Color(0xFF071522),
        live = Color(0xFFFF6878),
        coral = Color(0xFFFF7D8A),
        orange = Color(0xFFFFB45C),
        green = Color(0xFF4DDBA0),
        gold = Color(0xFFF4C66D),
    )

    val premiumPalettes: List<SonHarfThemePalette> = listOf(blueWhite, charcoalIvory, sapphireIce)

    /**
     * Unknown/null theme keeps a neutral, readable blue-white base.
     * Ownership is enforced by the backend equip RPC; this is only rendering fallback.
     */
    fun forId(id: String?): SonHarfThemePalette = when (id) {
        CHARCOAL_IVORY_ID -> charcoalIvory
        SAPPHIRE_ICE_ID -> sapphireIce
        BLUE_WHITE_ID -> blueWhite
        else -> blueWhite
    }

    fun known(id: String): Boolean = premiumPalettes.any { it.id == id }
}
