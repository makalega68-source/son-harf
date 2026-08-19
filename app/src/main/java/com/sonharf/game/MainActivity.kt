package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val SonHarfBg = Color(0xFF030711)
internal val SonHarfSurface = Color(0xFF0A1220)
internal val SonHarfSurface2 = Color(0xFF101A2B)
internal val SonHarfPurple = Color(0xFF9B35FF)
internal val SonHarfCyan = Color(0xFF12C8FF)
internal val SonHarfBlue = Color(0xFF2D6BFF)
internal val SonHarfGold = Color(0xFFFFB527)
internal val SonHarfGreen = Color(0xFF24D47C)
internal val SonHarfText = Color(0xFFF7F9FF)
internal val SonHarfMuted = Color(0xFF8792A8)
internal val SonHarfPink = Color(0xFFFF3E83)

enum class AppScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfPreferences.syncSound(this)
        setContent {
            val dark = SonHarfPreferences.darkModeEnabled(this)
            val scheme = if (dark) {
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
                    primary = Color(0xFF6D35E8),
                    secondary = Color(0xFF007EAA),
                    tertiary = Color(0xFF9B6500),
                    background = Color(0xFFF4F7FC),
                    surface = Color.White,
                    onBackground = Color(0xFF111827),
                    onSurface = Color(0xFF111827),
                )
            }
            MaterialTheme(colorScheme = scheme) { AuroraSonHarfApp() }
        }
    }
}
