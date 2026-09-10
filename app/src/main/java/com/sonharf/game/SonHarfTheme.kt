package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the equipped application-wide visual system.
 * Style selections are cosmetic only and must never alter gameplay state.
 */
internal object SonHarfTheme {
    private val dark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    val IsDark: Boolean get() = dark
    val Background: Color get() = if (dark) Color(0xFF071E1A) else Color(0xFFF7FAF3)
    val Surface: Color get() = if (dark) Color(0xFF102F29) else Color(0xFFFFFCF4)
    val SurfaceSecondary: Color get() = if (dark) Color(0xFF183C34) else Color(0xFFE8F2EA)
    val PrimaryBlue: Color get() = if (dark) Color(0xFFE1B956) else Color(0xFF286A59)
    val PrimaryBlueSoft: Color get() = if (dark) Color(0xFF352D19) else Color(0xFFDDECE3)
    val SecondaryAccent: Color get() = if (dark) Color(0xFF73B9A3) else Color(0xFF4B91A6)
    val TextPrimary: Color get() = if (dark) Color(0xFFF6F3E9) else Color(0xFF173F37)
    val TextSecondary: Color get() = if (dark) Color(0xFFB5C9C1) else Color(0xFF687F77)
    val Border: Color get() = if (dark) Color(0xFF426158) else Color(0xFFBCD2C6)
    val Success: Color get() = if (dark) Color(0xFF74C99F) else Color(0xFF3A8C68)
    val Error: Color get() = if (dark) Color(0xFFFF6670) else Color(0xFFE64B55)
    val Warning: Color get() = if (dark) Color(0xFFE1B956) else Color(0xFFD5A93F)
    val DisabledBackground: Color get() = if (dark) Color(0xFF27463E) else Color(0xFFDCE8E0)
    val DisabledContent: Color get() = if (dark) Color(0xFF7E9990) else Color(0xFF6D817A)
    val Purple: Color get() = if (dark) Color(0xFFB9A4D9) else Color(0xFF8B77A8)
}
