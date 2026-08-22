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

internal val SonHarfBg: Color get() = if (SonHarfUiState.darkMode) Color(0xFFF7FBFF) else Color(0xFFF7FBFF)
internal val SonHarfSurface: Color get() = if (SonHarfUiState.darkMode) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)
internal val SonHarfSurface2: Color get() = if (SonHarfUiState.darkMode) Color(0xFFEAF7FF) else Color(0xFFEAF7FF)
internal val SonHarfPurple = Color(0xFF56BDE8)
internal val SonHarfCyan = Color(0xFF55C2F0)
internal val SonHarfBlue = Color(0xFF3DAFE0)
internal val SonHarfGold = Color(0xFF55C2F0)
internal val SonHarfGreen = Color(0xFF39B978)
internal val SonHarfText: Color get() = Color(0xFF16324A)
internal val SonHarfMuted: Color get() = Color(0xFF698296)
internal val SonHarfPink = Color(0xFFEF7C8E)

private val SonHarfNeonTypography = Typography(
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
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
                    primary = SonHarfGold,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGreen,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                ),
                typography = SonHarfNeonTypography,
            ) {
                Box {
                    GamePortalApp()
                    WinnerFireworkOverlay()
                    FriendsQuickAccessOverlay()
                    GameInviteOverlay()
                    FriendRequestOverlay()
                }
            }
        }
    }
}
