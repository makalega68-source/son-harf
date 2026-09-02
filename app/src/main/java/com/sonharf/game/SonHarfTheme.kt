package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the shipped Mavi Beyaz Arena visual system.
 * Style selections are cosmetic only and must never alter gameplay state.
 */
internal object SonHarfTheme {
    val Background = Color(0xFFF5F8FC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFEEF5FF)
    val PrimaryBlue = Color(0xFF1769E0)
    val PrimaryBlueSoft = Color(0xFFE8F1FF)
    val TextPrimary = Color(0xFF142033)
    val TextSecondary = Color(0xFF66758A)
    val Border = Color(0xFFD5E0EA)
    val Success = Color(0xFF2FAE68)
    val Error = Color(0xFFE64B55)
    val Warning = Color(0xFFF3A81A)
    val DisabledBackground = Color(0xFFDDE7F3)
    val DisabledContent = Color(0xFF66758A)
    val Purple = Color(0xFF7659D6)
}
