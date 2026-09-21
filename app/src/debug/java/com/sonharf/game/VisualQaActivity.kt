package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sonharf.game.data.OnlineGameBackend

/** Debug-source-only visual harness. Never compiled into release builds. */
class VisualQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty().lowercase()
        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    VisualQaScreen(screen)
                }
            }
        }
    }
}

@Composable
private fun VisualQaScreen(screen: String) {
    val backend = remember { OnlineGameBackend() }
    when (screen) {
        "store" -> PremiumStoreScreen(
            initialTab = 0,
            onBack = {},
            onMembershipChanged = {},
            onCollection = {},
        )
        "profile" -> MainPlayerProfileScreen(backend, {}, {}, {}, {}, {})
        "social" -> MainSocialScreen(backend = backend, onPlay = {}, onSiege = {})
        "siege" -> WordSiegePracticeScreen(onExit = {}, matchmakingFallback = false)
        "sonharf" -> PremierLobby(
            language = "tr",
            profile = null,
            notice = "",
            busy = false,
            onLanguage = {},
            onPlay = {},
            onHome = {},
        )
        "harfyolu" -> LetterLadderGameScreen(onExit = {})
        "leaderboard" -> CompetitionHubScreen(onBack = {})
        "retention" -> MainRetentionScreen(
            backend = backend,
            onBack = {},
            onPlay = {},
            onDailyChallenge = {},
        )
        "pro" -> PremiumStoreScreen(
            initialTab = 3,
            onBack = {},
            onMembershipChanged = {},
            onCollection = {},
        )
        "settings" -> MainSettingsScreen(backend, {}, {}, {})
        else -> PremiumAdultApp(onSignedOut = {})
    }
}
