package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the shipped Mavi Beyaz Arena visual system.
 * Style selections are cosmetic only and must never alter gameplay state.
 */
internal object SonHarfTheme {
    // Son Harf'in bütün aktif ekranlarında kullanılan tek marka paleti.
    val Background = Color(0xFFF4F6FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFEEF1FF)
    val PrimaryBlue = Color(0xFF1769E0)
    val PrimaryBlueSoft = Color(0xFFE8F1FF)
    val BrandPurple = Color(0xFF6B4FD3)
    val BrandPurpleSoft = Color(0xFFF0EBFF)
    val BrandGold = Color(0xFFF3B51B)
    val BrandGoldSoft = Color(0xFFFFF5D6)
    val TextPrimary = Color(0xFF142033)
    val TextSecondary = Color(0xFF66758A)
    val Border = Color(0xFFD5E0EA)
    val Success = Color(0xFF2FAE68)
    val Error = Color(0xFFE64B55)
    val Warning = BrandGold
    val DisabledBackground = Color(0xFFDDE7F3)
    val DisabledContent = Color(0xFF66758A)
    val Purple = BrandPurple

    const val MinTouchTarget = 48
    const val ButtonHeight = 56
    const val CardCorner = 20
    const val ControlCorner = 16
}
