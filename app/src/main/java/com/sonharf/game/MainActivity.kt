package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

internal val SonHarfBg: Color get() = if (SonHarfUiState.darkMode) Color(0xFF030B16) else Color(0xFFE8F6FF)
internal val SonHarfSurface: Color get() = if (SonHarfUiState.darkMode) Color(0xFF0A1626) else Color(0xFFF8FCFF)
internal val SonHarfSurface2: Color get() = if (SonHarfUiState.darkMode) Color(0xFF102238) else Color(0xFFDDF1FF)
internal val SonHarfPurple = Color(0xFF8B32E6)
internal val SonHarfCyan = Color(0xFF12BFEF)
internal val SonHarfBlue = Color(0xFF2D79E8)
internal val SonHarfGold = Color(0xFFFFB52E)
internal val SonHarfGreen = Color(0xFF20C979)
internal val SonHarfText: Color get() = if (SonHarfUiState.darkMode) Color(0xFFF5FAFF) else Color(0xFF101A2A)
internal val SonHarfMuted: Color get() = if (SonHarfUiState.darkMode) Color(0xFF8FA5BD) else Color(0xFF5E7085)
internal val SonHarfPink = Color(0xFFFF4386)

enum class AppScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfPreferences.syncSound(this)
        SonHarfPreferences.syncUi(this)
        if (SupabaseProvider.configured && !SonHarfPreferences.rememberLogin(this)) {
            runBlocking { runCatching { SupabaseProvider.client.auth.signOut() } }
        }
        setContent {
            val scheme = if (SonHarfUiState.darkMode) {
                darkColorScheme(
                    primary = SonHarfPurple,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGold,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
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
                    surfaceVariant = SonHarfSurface2,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                )
            }
            MaterialTheme(colorScheme = scheme) {
                Box {
                    AuroraSonHarfAppPrivateEnhanced()
                    WinnerFireworkOverlay()
                }
            }
        }
    }
}
