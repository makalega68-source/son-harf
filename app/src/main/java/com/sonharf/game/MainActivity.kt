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

internal val SonHarfBg: Color get() = Color(0xFF030613)
internal val SonHarfSurface: Color get() = Color(0xFF081127)
internal val SonHarfSurface2: Color get() = Color(0xFF0E1732)
internal val SonHarfPurple = Color(0xFF7A35FF)
internal val SonHarfCyan = Color(0xFF00E9FF)
internal val SonHarfBlue = Color(0xFF2A75FF)
internal val SonHarfGold = Color(0xFFFFB817)
internal val SonHarfGreen = Color(0xFF41E38A)
internal val SonHarfText: Color get() = Color(0xFFF7F8FF)
internal val SonHarfMuted: Color get() = Color(0xFF8D98B8)
internal val SonHarfPink = Color(0xFFFF3FCF)

// Preserve the accessibility sizing introduced before the visual redesign.
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
                }
            }
        }
    }
}
