package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

internal val SonHarfBg: Color get() = Color(0xFF090F1A)
internal val SonHarfSurface: Color get() = Color(0xFF10172B)
internal val SonHarfSurface2: Color get() = Color(0xFF131D35)
internal val SonHarfPurple = Color(0xFF7B2FFF)
internal val SonHarfCyan = Color(0xFF00E5FF)
internal val SonHarfBlue = Color(0xFF168CFF)
internal val SonHarfGold = Color(0xFFFFC107)
internal val SonHarfGreen = Color(0xFF41E38A)
internal val SonHarfText: Color get() = Color(0xFFF6F8FF)
internal val SonHarfMuted: Color get() = Color(0xFF91A1BE)
internal val SonHarfPink = Color(0xFFFF4D6D)

private val SonHarfNeonTypography = Typography(
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp),
    titleLarge = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
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
                TotalNeonSonHarfApp()
            }
        }
    }
}
