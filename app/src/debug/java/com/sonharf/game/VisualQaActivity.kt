package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
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
private fun QaSafeArea(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        content()
    }
}

@Composable
private fun VisualQaScreen(screen: String) {
    val backend = remember { OnlineGameBackend() }
    when (screen) {
        "store" -> QaSafeArea {
            PremiumStoreScreen(
                initialTab = 0,
                onBack = {},
                onMembershipChanged = {},
                onCollection = {},
            )
        }
        "profile" -> QaSafeArea { MainPlayerProfileScreen(backend, {}, {}, {}, {}, {}) }
        "social" -> QaSafeArea { MainSocialScreen(backend = backend, onPlay = {}, onSiege = {}) }
        "siege" -> QaSafeArea { WordSiegePracticeScreen(onExit = {}, matchmakingFallback = false) }
        "sonharf" -> QaSafeArea {
            PremierLobby(
                language = "tr",
                profile = null,
                notice = "",
                busy = false,
                onLanguage = {},
                onPlay = {},
                onHome = {},
            )
        }
        "harfyolu" -> QaSafeArea { LetterLadderGameScreen(onExit = {}) }
        "leaderboard" -> QaSafeArea { CompetitionHubScreen(onBack = {}) }
        "retention" -> QaSafeArea {
            MainRetentionScreen(
                backend = backend,
                onBack = {},
                onPlay = {},
                onDailyChallenge = {},
            )
        }
        "pro" -> QaSafeArea {
            PremiumStoreScreen(
                initialTab = 3,
                onBack = {},
                onMembershipChanged = {},
                onCollection = {},
            )
        }
        "settings" -> QaSafeArea { MainSettingsScreen(backend, {}, {}, {}) }
        else -> PremiumAdultApp(onSignedOut = {})
    }
}
