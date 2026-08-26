package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

internal val SonHarfBg: Color get() = Color(0xFFF4FBFF)
internal val SonHarfSurface: Color get() = Color(0xFFFFFFFF)
internal val SonHarfSurface2: Color get() = Color(0xFFEAF8FF)
internal val SonHarfPurple = Color(0xFF4ABCEB)
internal val SonHarfCyan = Color(0xFF38C7F4)
internal val SonHarfBlue = Color(0xFF24AEE4)
internal val SonHarfGold = Color(0xFFFFC857)
internal val SonHarfGreen = Color(0xFF32C985)
internal val SonHarfText: Color get() = Color(0xFF173B57)
internal val SonHarfMuted: Color get() = Color(0xFF6D879A)
internal val SonHarfPink = Color(0xFFFF7891)

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

private fun parseRemoteColor(value: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)

enum class AppScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfPreferences.syncSound(this)
        SonHarfPreferences.syncUi(this)
        WordMeaningRuntime.init(this)
        RemoteExperience.loadCached(this)
        // Test/debug APKs must not force a fresh login on every launch.
        // Release/Google Play builds keep the existing authentication behavior.
        if (!BuildConfig.DEBUG && SupabaseProvider.configured && !SonHarfPreferences.rememberLogin(this)) {
            runBlocking { runCatching { SupabaseProvider.client.auth.signOut() } }
        }
        setContent {
            val remote = RemoteExperience.config
            val primary = parseRemoteColor(remote.primaryColor, SonHarfBlue)
            val secondary = parseRemoteColor(remote.secondaryColor, SonHarfCyan)
            val background = parseRemoteColor(remote.backgroundColor, SonHarfBg)
            val surface = parseRemoteColor(remote.surfaceColor, SonHarfSurface)
            val surfaceVariant = parseRemoteColor(remote.surfaceVariantColor, SonHarfSurface2)
            val text = parseRemoteColor(remote.textColor, SonHarfText)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = primary,
                    secondary = secondary,
                    tertiary = SonHarfGreen,
                    background = background,
                    surface = surface,
                    surfaceVariant = surfaceVariant,
                    onPrimary = Color.White,
                    onBackground = text,
                    onSurface = text,
                ),
                typography = SonHarfTypography,
            ) {
                Box(Modifier.fillMaxSize()) {
                    GamePortalApp()
                    PrivateRoomWaitingLayer()
                    if (FriendsQuickAccessState.open) FriendsQuickAccessOverlay()
                    GameInviteOverlay()
                    FriendRequestOverlay()
                }
            }
        }
    }
}
