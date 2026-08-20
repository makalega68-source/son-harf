package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val SonHarfBg: Color get() = if (SonHarfUiState.darkMode) Color(0xFF030711) else Color(0xFFF4F7FC)
internal val SonHarfSurface: Color get() = if (SonHarfUiState.darkMode) Color(0xFF0A1220) else Color(0xFFFFFFFF)
internal val SonHarfSurface2: Color get() = if (SonHarfUiState.darkMode) Color(0xFF101A2B) else Color(0xFFE7EDF7)
internal val SonHarfPurple = Color(0xFF9B35FF)
internal val SonHarfCyan = Color(0xFF12C8FF)
internal val SonHarfBlue = Color(0xFF2D6BFF)
internal val SonHarfGold = Color(0xFFFFB527)
internal val SonHarfGreen = Color(0xFF24D47C)
internal val SonHarfText: Color get() = if (SonHarfUiState.darkMode) Color(0xFFF7F9FF) else Color(0xFF101522)
internal val SonHarfMuted: Color get() = if (SonHarfUiState.darkMode) Color(0xFF8792A8) else Color(0xFF5F6B7E)
internal val SonHarfPink = Color(0xFFFF3E83)

enum class AppScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfPreferences.syncSound(this)
        SonHarfPreferences.syncUi(this)
        setContent {
            val scheme = if (SonHarfUiState.darkMode) {
                darkColorScheme(
                    primary = SonHarfPurple,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGold,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF7130C8),
                    secondary = Color(0xFF007EA8),
                    tertiary = Color(0xFF9B6500),
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                )
            }
            MaterialTheme(colorScheme = scheme) { AuroraSonHarfAppPrivateEnhanced() }
        }
    }
}
