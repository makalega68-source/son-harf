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
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.runBlocking

internal val SonHarfBg: Color get() = SonHarfCosmetics.gamePalette.background
internal val SonHarfSurface: Color get() = SonHarfCosmetics.gamePalette.surface
internal val SonHarfSurface2: Color get() = SonHarfCosmetics.gamePalette.surfaceSoft
internal val SonHarfPurple: Color get() = SonHarfCosmetics.gamePalette.secondary
internal val SonHarfCyan: Color get() = SonHarfCosmetics.gamePalette.accent
internal val SonHarfBlue: Color get() = SonHarfCosmetics.gamePalette.accent
internal val SonHarfGold = Color(0xFFF3A81A)
internal val SonHarfGreen = Color(0xFF22B95F)
internal val SonHarfText: Color get() = SonHarfCosmetics.gamePalette.text
internal val SonHarfMuted: Color get() = SonHarfCosmetics.gamePalette.muted
internal val SonHarfPink = Color(0xFFE95B72)

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
            val palette = SonHarfCosmetics.gamePalette
            val colorScheme = if (SonHarfCosmetics.isMidnight) {
                darkColorScheme(
                    primary = palette.accent,
                    secondary = palette.secondary,
                    tertiary = SonHarfGreen,
                    background = palette.background,
                    surface = palette.surface,
                    surfaceVariant = palette.surfaceSoft,
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onTertiary = Color.White,
                    onBackground = palette.text,
                    onSurface = palette.text,
                    onSurfaceVariant = palette.text,
                    outline = palette.border,
                    error = Color(0xFFFF6B78),
                )
            } else {
                lightColorScheme(
                    primary = palette.accent,
                    secondary = palette.secondary,
                    tertiary = SonHarfGreen,
                    background = palette.background,
                    surface = palette.surface,
                    surfaceVariant = palette.surfaceSoft,
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onTertiary = Color.White,
                    onBackground = palette.text,
                    onSurface = palette.text,
                    onSurfaceVariant = palette.text,
                    outline = palette.border,
                    error = Color(0xFFD83A48),
                )
            }
            MaterialTheme(
                colorScheme = colorScheme,
                typography = SonHarfTypography,
            ) {
                Box(Modifier.fillMaxSize()) {
                    GamePortalApp()
                }
            }
        }
    }
}
