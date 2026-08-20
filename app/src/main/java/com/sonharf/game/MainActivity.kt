package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

// Both modes stay inside the approved dark-neon identity; the toggle changes density/contrast,
// rather than falling back to the old bright interface.
internal val SonHarfBg: Color get() = if (SonHarfUiState.darkMode) Color(0xFF02040E) else Color(0xFF07101F)
internal val SonHarfSurface: Color get() = if (SonHarfUiState.darkMode) Color(0xFF071027) else Color(0xFF0D1A31)
internal val SonHarfSurface2: Color get() = if (SonHarfUiState.darkMode) Color(0xFF0E1732) else Color(0xFF152544)
internal val SonHarfPurple = Color(0xFF7A35FF)
internal val SonHarfCyan = Color(0xFF00E9FF)
internal val SonHarfBlue = Color(0xFF2A75FF)
internal val SonHarfGold = Color(0xFFFFB817)
internal val SonHarfGreen = Color(0xFF41E38A)
internal val SonHarfText: Color get() = Color(0xFFF7F8FF)
internal val SonHarfMuted: Color get() = if (SonHarfUiState.darkMode) Color(0xFF8D98B8) else Color(0xFFA8B4CF)
internal val SonHarfPink = Color(0xFFFF3FCF)

private val SonHarfNeonTypography = Typography(
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
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = SonHarfPurple,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGold,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                ),
                typography = SonHarfNeonTypography,
            ) {
                Box {
                    FullHistoryNeonApp()
                    WinnerFireworkOverlay()
                    FriendsQuickAccessOverlay()
                    GameInviteOverlay()
                    FriendRequestOverlay()
                }
            }
        }
    }
}
