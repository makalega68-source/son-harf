package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

internal val SonHarfBg: Color get() = if (SonHarfUiState.darkMode) Color(0xFF050816) else Color(0xFFF2F7FF)
internal val SonHarfSurface: Color get() = if (SonHarfUiState.darkMode) Color(0xFF0B1024) else Color(0xFFFFFFFF)
internal val SonHarfSurface2: Color get() = if (SonHarfUiState.darkMode) Color(0xFF111936) else Color(0xFFE8F0FF)
internal val SonHarfPurple = Color(0xFF7B2FFF)
internal val SonHarfCyan = Color(0xFF00E5FF)
internal val SonHarfBlue = Color(0xFF178BFF)
internal val SonHarfGold = Color(0xFFFFC107)
internal val SonHarfGreen = Color(0xFF41E38A)
internal val SonHarfText: Color get() = if (SonHarfUiState.darkMode) Color(0xFFF8FBFF) else Color(0xFF10162A)
internal val SonHarfMuted: Color get() = if (SonHarfUiState.darkMode) Color(0xFF9AA8C2) else Color(0xFF5D6A82)
internal val SonHarfPink = Color(0xFFFF4D8D)

private val SonHarfAccessibleTypography = Typography(
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 17.sp),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
)

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
                    primary = Color(0xFF6331C7),
                    secondary = Color(0xFF007F96),
                    tertiary = Color(0xFF9A6A00),
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                )
            }
            MaterialTheme(colorScheme = scheme, typography = SonHarfAccessibleTypography) {
                Box {
                    NeonSonHarfApp()
                    WinnerFireworkOverlay()
                    FriendsQuickAccessOverlay()
                }
            }
        }
    }
}
