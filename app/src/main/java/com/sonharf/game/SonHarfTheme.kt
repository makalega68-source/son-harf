package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the equipped application-wide visual system.
 * Style selections are cosmetic only and must never alter gameplay state.
 */
internal object SonHarfTheme {
    private val dark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    val Background: Color get() = if (dark) Color(0xFF070A12) else Color(0xFFF5F8FC)
    val Surface: Color get() = if (dark) Color(0xFF111722) else Color(0xFFFFFFFF)
    val SurfaceSecondary: Color get() = if (dark) Color(0xFF1A2331) else Color(0xFFEEF5FF)
    val PrimaryBlue: Color get() = if (dark) Color(0xFFF0B84D) else Color(0xFF1769E0)
    val PrimaryBlueSoft: Color get() = if (dark) Color(0xFF2C2417) else Color(0xFFE8F1FF)
    val TextPrimary: Color get() = if (dark) Color(0xFFF5F7FC) else Color(0xFF142033)
    val TextSecondary: Color get() = if (dark) Color(0xFFAEB9C9) else Color(0xFF66758A)
    val Border: Color get() = if (dark) Color(0xFF3A4658) else Color(0xFFD5E0EA)
    val Success: Color get() = if (dark) Color(0xFF45C982) else Color(0xFF2FAE68)
    val Error: Color get() = if (dark) Color(0xFFFF6670) else Color(0xFFE64B55)
    val Warning: Color get() = if (dark) Color(0xFFF0B84D) else Color(0xFFF3A81A)
    val DisabledBackground: Color get() = if (dark) Color(0xFF253040) else Color(0xFFDDE7F3)
    val DisabledContent: Color get() = if (dark) Color(0xFF7D8999) else Color(0xFF66758A)
    val Purple: Color get() = if (dark) Color(0xFFA98BEA) else Color(0xFF7659D6)
}
