package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Canonical native Android palette for Kelime Kuşatması and Son Harf.
 *
 * These values are the product contract. Gameplay ownership colours may use semantic aliases,
 * but the application chrome, cards, actions and typography must resolve from this object.
 */
internal object AppColors {
    val Background = Color(0xFF141923)
    val Surface = Color(0xFF1E2538)
    val SurfaceSecondary = Color(0xFF242D43)
    val SurfaceElevated = Color(0xFF29334B)
    val Navigation = Color(0xFF171D2A)
    val Modal = Color(0xFF20283B)

    val GameSurface = Color(0xFF171E2B)
    val GameTile = Color(0xFF252F45)
    val GameTileBorder = Color(0xFF3A4966)

    val Primary = Color(0xFF2ECC71)
    val PrimaryDeep = Color(0xFF24A65A)
    val PrimarySoft = Color(0xFF18382B)

    val Secondary = Color(0xFF3498DB)
    val SecondaryDeep = Color(0xFF2479B5)
    val SecondarySoft = Color(0xFF1A3045)

    val Accent = Color(0xFF9B59B6)
    val AccentSoft = Color(0xFF32233F)

    val Warning = Color(0xFFE67E22)
    val WarningSoft = Color(0xFF3D2A1C)

    val TextPrimary = Color(0xFFECF0F1)
    val TextSecondary = Color(0xFF95A5A6)
    val Border = Color(0xFF34415B)

    val Error = Color(0xFFE05A67)
    val ErrorSoft = Color(0xFF3C2029)
    val Gold = Color(0xFFE6A23C)
    val GoldSoft = Color(0xFF3B321F)
}
