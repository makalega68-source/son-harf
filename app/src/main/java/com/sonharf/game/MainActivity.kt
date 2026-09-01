package com.sonharf.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.runBlocking

// Global Monster-derived palette. Product mechanics and Son Harf identity stay
// intact; the application chrome now uses one consistent purchased-kit system.
internal val SonHarfBg: Color get() = Color(0xFF101114)
internal val SonHarfSurface: Color get() = Color(0xFF181A1F)
internal val SonHarfSurface2: Color get() = Color(0xFF25272E)
internal val SonHarfPurple = Color(0xFF9A86FF)
internal val SonHarfCyan = Color(0xFFEAFB17)
internal val SonHarfBlue = Color(0xFFEAFB17)
internal val SonHarfGold = Color(0xFFFFC857)
internal val SonHarfGreen = Color(0xFF47C77A)
internal val SonHarfText: Color get() = Color(0xFFF7F7F8)
internal val SonHarfMuted: Color get() = Color(0xFF8E929D)
internal val SonHarfPink = Color(0xFFFF5B4D)

private val SonHarfTypography = Typography(
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
    private fun handleAuthDeepLink(intent: Intent) {
        if (!SupabaseProvider.configured || intent.data?.scheme != "sonharf") return
        SupabaseProvider.client.handleDeeplinks(
            intent = intent,
            onSessionSuccess = { session ->
                val verifiedEmail = session.user?.email.orEmpty()
                if (verifiedEmail.isNotBlank()) {
                    SonHarfPreferences.setRememberLogin(this, true, verifiedEmail)
                }
                runOnUiThread { recreate() }
            },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeepLink(intent)
    }

    override fun onStart() {
        super.onStart()
        SonHarfBackgroundMusic.start(this)
    }

    override fun onStop() {
        SonHarfBackgroundMusic.pause()
        super.onStop()
    }

    override fun onDestroy() {
        SonHarfBackgroundMusic.release()
        SonHarfSoundFx.release()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfSoundFx.init(this)
        SonHarfPreferences.syncSound(this)
        SonHarfPreferences.syncUi(this)
        WordMeaningRuntime.init(this)
        RemoteExperience.loadCached(this)
        AdPrivacyManager.requestConsent(this)
        if (!BuildConfig.DEBUG && SupabaseProvider.configured && !SonHarfPreferences.rememberLogin(this)) {
            runBlocking { runCatching { SupabaseProvider.client.auth.signOut() } }
        }
        handleAuthDeepLink(intent)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = SonHarfBlue,
                    secondary = SonHarfPurple,
                    tertiary = SonHarfGreen,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onPrimary = Color(0xFF101114),
                    onSecondary = Color.White,
                    onTertiary = Color(0xFF07120B),
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                    onSurfaceVariant = SonHarfText,
                    error = Color(0xFFFF5B4D),
                ),
                typography = SonHarfTypography,
            ) {
                Box(Modifier.fillMaxSize()) {
                    GamePortalApp()
                }
            }
        }
    }
}
