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
internal val SonHarfCyan: Color get() = SonHarfTheme.Turquoise
internal val SonHarfBlue: Color get() = SonHarfTheme.PrimaryBlue
internal val SonHarfGold: Color get() = SonHarfTheme.PremiumGold
internal val SonHarfGreen: Color get() = SonHarfTheme.Success
internal val SonHarfText: Color get() = SonHarfTheme.TextPrimary
internal val SonHarfMuted: Color get() = SonHarfTheme.TextSecondary
internal val SonHarfPink: Color get() = SonHarfTheme.Error

internal val SonHarfTypography = Typography(
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

internal val SonHarfShapes = Shapes(
    extraSmall = MainUiShape.Control,
    small = MainUiShape.Control,
    medium = MainUiShape.Card,
    large = MainUiShape.Hero,
    extraLarge = MainUiShape.Hero,
)

enum class AppScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

internal fun isPasswordRecoveryDeepLink(intent: Intent): Boolean {
    val uri = intent.data ?: return false
    if (uri.scheme != "sonharf" || uri.host != "auth") return false

    val queryType = uri.getQueryParameter("type")
    if (queryType.equals("recovery", ignoreCase = true)) return true

    return uri.fragment
        ?.split('&')
        ?.any { part ->
            val key = part.substringBefore('=', missingDelimiterValue = "")
            val value = part.substringAfter('=', missingDelimiterValue = "")
            key == "type" && value.equals("recovery", ignoreCase = true)
        } == true
}

class MainActivity : ComponentActivity() {
    companion object {
        /**
         * "Remember me" cleanup is a process-start policy, not an Activity-recreation policy.
         * Keeping this in process memory prevents rotations/configuration changes from signing
         * out an active non-remembered session while still re-applying the policy after process death.
         */
        private var startupSessionPolicyApplied = false
        private const val PASSWORD_RECOVERY_STATE_KEY = "password_recovery_requested"
    }

    private var passwordRecoveryRequested by mutableStateOf(false)

    private fun bestEffortStartup(name: String, block: () -> Unit) {
        runCatching(block).onFailure { Log.e("SonHarfStartup", "$name failed; continuing launch", it) }
    }

    private fun handleAuthDeepLink(intent: Intent) {
        val uri = intent.data
        if (!SupabaseProvider.configured || uri?.scheme != "sonharf" || uri.host != "auth") return
        val recoveryRequested = isPasswordRecoveryDeepLink(intent)
        bestEffortStartup("auth deeplink") {
            SupabaseProvider.client.handleDeeplinks(
                intent = intent,
                onSessionSuccess = { session ->
                    if (recoveryRequested) {
                        // A password-recovery session must never be promoted to a normal remembered login.
                        runOnUiThread { passwordRecoveryRequested = true }
                    } else {
                        val verifiedEmail = session.user?.email.orEmpty()
                        if (verifiedEmail.isNotBlank()) {
                            bestEffortStartup("remember login") {
                                SonHarfPreferences.setRememberLogin(this, true, verifiedEmail)
                            }
                        }
                        runOnUiThread { recreate() }
                    }
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
        bestEffortStartup("mascot icon") { MascotLauncherIcon.onAppOpened(this) }
    }

    override fun onStop() {
        runCatching { SonHarfBackgroundMusic.pause() }
        // Not while rotating or finishing into another screen of ours: only a real trip away.
        if (!isChangingConfigurations) runCatching { MascotLauncherIcon.onAppBackground(this) }
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { SonHarfBackgroundMusic.release() }
        runCatching { SonHarfSoundFx.release() }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PASSWORD_RECOVERY_STATE_KEY, passwordRecoveryRequested)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passwordRecoveryRequested = savedInstanceState?.getBoolean(PASSWORD_RECOVERY_STATE_KEY) == true

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
        val applySessionPolicy = !startupSessionPolicyApplied
        startupSessionPolicyApplied = true
        val clearUnrememberedSession = SupabaseProvider.configured && applySessionPolicy && !rememberLogin && !authDeepLink

        setContent {
            val appColors = if (SonHarfTheme.IsDark) {
                darkColorScheme(
                    primary = SonHarfBlue,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGreen,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    surfaceVariant = SonHarfSurface2,
                    onPrimary = SonHarfTheme.OnPrimary,
                    onSecondary = SonHarfTheme.OnSecondary,
                    onTertiary = SonHarfTheme.OnTertiary,
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
                shapes = SonHarfShapes,
            ) {
                AppStartupGate(
                    clearUnrememberedSession = clearUnrememberedSession,
                    passwordRecoveryRequested = passwordRecoveryRequested,
                    onPasswordRecoveryFinished = {
                        passwordRecoveryRequested = false
                        if (isPasswordRecoveryDeepLink(intent)) {
                            setIntent(Intent(intent).apply { data = null })
                        }
                    },
                )
            }
        }

        handleAuthDeepLink(intent)
    }
}

@Composable
private fun AppStartupGate(
    clearUnrememberedSession: Boolean,
    passwordRecoveryRequested: Boolean,
    onPasswordRecoveryFinished: () -> Unit,
) {
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
        StartupState.Ready -> Box(Modifier.fillMaxSize()) {
            if (passwordRecoveryRequested) {
                PasswordRecoveryScreen(onFinished = onPasswordRecoveryFinished)
            } else {
                StableV1App()
            }
        }
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
            Text(sh("Kelime Tahtı hazırlanıyor…", "Preparing Word Throne…"), color = SonHarfText, fontWeight = FontWeight.Bold)
            Text(sh("Oturum ve ayarlar güvenli biçimde yükleniyor.", "Loading session and settings safely."), color = SonHarfMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
