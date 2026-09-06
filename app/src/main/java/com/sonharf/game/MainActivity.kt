package com.sonharf.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.withTimeoutOrNull

internal val SonHarfBg: Color get() = SonHarfTheme.Background
internal val SonHarfSurface: Color get() = SonHarfTheme.Surface
internal val SonHarfSurface2: Color get() = SonHarfTheme.SurfaceSecondary
internal val SonHarfPurple: Color get() = SonHarfTheme.Purple
internal val SonHarfCyan: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFFFD36A) else Color(0xFF1687F8)
internal val SonHarfBlue: Color get() = SonHarfTheme.PrimaryBlue
internal val SonHarfGold = Color(0xFFF6C453)
internal val SonHarfGreen = Color(0xFF35C878)
internal val SonHarfText: Color get() = SonHarfTheme.TextPrimary
internal val SonHarfMuted: Color get() = SonHarfTheme.TextSecondary
internal val SonHarfPink = Color(0xFFFF5F57)

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
        SonHarfCosmetics.restore(this)
        WordMeaningRuntime.init(this)
        RemoteExperience.loadCached(this)
        AdPrivacyManager.requestConsent(this)

        val authDeepLink = intent.data?.scheme == "sonharf"
        val clearUnrememberedSession = !BuildConfig.DEBUG &&
            SupabaseProvider.configured &&
            !SonHarfPreferences.rememberLogin(this) &&
            !authDeepLink

        setContent {
            val appColors = if (SonHarfCosmetics.darkArenaTheme) {
                darkColorScheme(
                    primary = SonHarfBlue,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGreen,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onPrimary = Color(0xFF201600),
                    onSecondary = Color(0xFF201600),
                    onTertiary = Color(0xFF07140D),
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                    onSurfaceVariant = SonHarfText,
                    error = SonHarfPink,
                )
            } else {
                lightColorScheme(
                    primary = SonHarfBlue,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGreen,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onTertiary = Color.White,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                    onSurfaceVariant = SonHarfText,
                    error = SonHarfPink,
                )
            }
            MaterialTheme(
                colorScheme = appColors,
                typography = SonHarfTypography,
            ) {
                AppStartupGate(clearUnrememberedSession = clearUnrememberedSession)
            }
        }

        handleAuthDeepLink(intent)
    }
}

@Composable
private fun AppStartupGate(clearUnrememberedSession: Boolean) {
    var state by remember { mutableStateOf<StartupState>(StartupState.Loading) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(clearUnrememberedSession, retryKey) {
        state = StartupState.Loading
        val completed = withTimeoutOrNull(6_000) {
            if (clearUnrememberedSession) {
                runCatching { SupabaseProvider.client.auth.signOut() }
            }
            true
        } ?: false
        state = if (completed) StartupState.Ready else StartupState.Error
    }

    when (state) {
        StartupState.Loading -> StartupLoading()
        StartupState.Error -> StartupError(onRetry = { retryKey++ })
        StartupState.Ready -> Box(Modifier.fillMaxSize()) { GamePortalApp() }
    }
}

private sealed interface StartupState {
    data object Loading : StartupState
    data object Ready : StartupState
    data object Error : StartupState
}

@Composable
private fun StartupLoading() {
    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SonHarfBrandLogo(modifier = Modifier.fillMaxWidth(.58f), size = null)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = SonHarfBlue, strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text(sh("SON HARF hazırlanıyor…", "Preparing SON HARF…"), color = SonHarfText, fontWeight = FontWeight.Bold)
            Text(sh("Oturum ve ayarlar güvenli biçimde yükleniyor.", "Loading session and settings safely."), color = SonHarfMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StartupError(onRetry: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Refresh, null, Modifier.size(46.dp), tint = SonHarfBlue)
            Spacer(Modifier.height(14.dp))
            Text(sh("Başlatma tamamlanamadı", "Startup could not complete"), color = SonHarfText, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(sh("Bağlantını kontrol edip tekrar deneyebilirsin.", "Check your connection and try again."), color = SonHarfMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue), shape = RoundedCornerShape(14.dp)) {
                Text(sh("TEKRAR DENE", "TRY AGAIN"), fontWeight = FontWeight.Black)
            }
        }
    }
}
