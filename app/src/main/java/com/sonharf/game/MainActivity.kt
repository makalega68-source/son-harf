package com.sonharf.game

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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

// G5.9 typography scale.
// Body/label uses Inter-equivalent (system sans, regular/medium/semibold).
// Titles use Montserrat-equivalent (system sans, extrabold/black). When TTFs
// are dropped into res/font/, swap FontFamily.SansSerif for a FontFamily
// composed of R.font.montserrat_* / R.font.inter_* — nothing else needs to
// change because every screen resolves through MaterialTheme.typography.
private val SonHarfTypography = Typography(
    // Body / label: 15sp text, 12sp caption.
    bodyLarge = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium,
    ),
    // Titles: 18 / 24 / 32.
    titleSmall = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.3.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
    ),
)

enum class AppScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

class MainActivity : ComponentActivity() {
    private fun bestEffortStartup(name: String, block: () -> Unit) {
        runCatching(block).onFailure { Log.e("SonHarfStartup", "$name failed; continuing launch", it) }
    }

    private fun handleAuthDeepLink(intent: Intent) {
        val uri = intent.data
        if (!SupabaseProvider.configured || uri?.scheme != "sonharf" || uri.host != "auth") return
        bestEffortStartup("auth deeplink") {
            SupabaseProvider.client.handleDeeplinks(
                intent = intent,
                onSessionSuccess = { session ->
                    val verifiedEmail = session.user?.email.orEmpty()
                    if (verifiedEmail.isNotBlank()) {
                        bestEffortStartup("remember login") {
                            SonHarfPreferences.setRememberLogin(this, true, verifiedEmail)
                        }
                    }
                    runOnUiThread { recreate() }
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeepLink(intent)
    }

    override fun onStart() {
        super.onStart()
        bestEffortStartup("background music") { SonHarfBackgroundMusic.start(this) }
    }

    override fun onStop() {
        runCatching { SonHarfBackgroundMusic.pause() }
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { SonHarfBackgroundMusic.release() }
        runCatching { SonHarfSoundFx.release() }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nothing optional is allowed to prevent the first frame from being rendered. Audio,
        // cached cosmetics/experience data and privacy SDKs are useful, but a device-specific
        // initialization failure must degrade that feature instead of crashing the whole app.
        bestEffortStartup("sound effects") { SonHarfSoundFx.init(this) }
        bestEffortStartup("sound preferences") { SonHarfPreferences.syncSound(this) }
        bestEffortStartup("ui preferences") { SonHarfPreferences.syncUi(this) }
        bestEffortStartup("cosmetics") { SonHarfCosmetics.restore(this) }
        bestEffortStartup("remote experience cache") { RemoteExperience.loadCached(this) }
        bestEffortStartup("ad privacy") { AdPrivacyManager.requestConsent(this) }

        val authDeepLink = intent.data?.let { it.scheme == "sonharf" && it.host == "auth" } == true
        val rememberLogin = runCatching { SonHarfPreferences.rememberLogin(this) }.getOrDefault(false)
        val clearUnrememberedSession = SupabaseProvider.configured && !rememberLogin && !authDeepLink

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
                com.sonharf.game.ui.vfx.VfxLayerHost {
                    AppStartupGate(clearUnrememberedSession = clearUnrememberedSession)
                }
            }
        }

        handleAuthDeepLink(intent)
    }
}

@Composable
private fun AppStartupGate(clearUnrememberedSession: Boolean) {
    var state by remember { mutableStateOf<StartupState>(StartupState.Loading) }
    LaunchedEffect(clearUnrememberedSession) {
        state = StartupState.Loading
        withTimeoutOrNull(1_500) {
            if (clearUnrememberedSession) {
                runCatching { SupabaseProvider.client.auth.signOut() }
            }
        }
        // Session cleanup is best-effort: a slow/offline backend must never block app launch.
        state = StartupState.Ready
    }

    when (state) {
        StartupState.Loading -> StartupLoading()
        StartupState.Ready -> Box(Modifier.fillMaxSize()) { StableV1App() }
    }
}

private sealed interface StartupState {
    data object Loading : StartupState
    data object Ready : StartupState
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
            Text(sh("Kelime Tahtı hazırlanıyor…", "Preparing Kelime Tahtı…"), color = SonHarfText, fontWeight = FontWeight.Bold)
            Text(sh("Oturum ve ayarlar güvenli biçimde yükleniyor.", "Loading session and settings safely."), color = SonHarfMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
