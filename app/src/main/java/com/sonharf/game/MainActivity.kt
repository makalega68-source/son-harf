package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

internal val SonHarfBg: Color get() = Color(0xFFF8FAFC)
internal val SonHarfSurface: Color get() = Color(0xFFFFFFFF)
internal val SonHarfSurface2: Color get() = Color(0xFFF1F5F9)
internal val SonHarfPurple = Color(0xFF0369A1)
internal val SonHarfCyan = Color(0xFF0284C7)
internal val SonHarfBlue = Color(0xFF0284C7)
internal val SonHarfGold = Color(0xFFD97706)
internal val SonHarfGreen = Color(0xFF16A34A)
internal val SonHarfText: Color get() = Color(0xFF0F172A)
internal val SonHarfMuted: Color get() = Color(0xFF475569)
internal val SonHarfPink = Color(0xFFDC2626)

private val SonHarfAccessibleTypography = Typography(
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    titleLarge = TextStyle(fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
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
                colorScheme = lightColorScheme(
                    primary = SonHarfCyan,
                    secondary = SonHarfBlue,
                    tertiary = SonHarfGreen,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onPrimary = Color.White,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                ),
                typography = SonHarfAccessibleTypography,
            ) {
                Box {
                    AccessibleLightV3SonHarfApp()
                }
            }
        }
    }
}
