package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getAdminAccess

/**
 * V1 stabilization shell.
 *
 * Product scope is intentionally narrow:
 * verified auth -> core duel lobby -> live match -> result/rematch.
 * Experimental game modes and legacy visual shells remain in the repository
 * but are not part of the active V1 navigation path.
 */
@Composable
fun StableV1App() {
    val context = LocalContext.current
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var adminAuthorized by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var tutorial by remember { mutableStateOf<FirstPlayerTutorialKind?>(null) }
    var automaticTutorial by remember { mutableStateOf(false) }
    var showHelpChooser by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }

    LaunchedEffect(authenticated) {
        adminAuthorized = if (authenticated && SupabaseProvider.configured) {
            runCatching { OnlineGameBackend().getAdminAccess().authorized }.getOrDefault(false)
        } else {
            false
        }
        if (!adminAuthorized) showAdminPanel = false

        if (!authenticated) return@LaunchedEffect
        when {
            shouldAutoShowTutorial(SonHarfPreferences.sonHarfTutorialCompleted(context)) -> {
                automaticTutorial = true
                tutorial = FirstPlayerTutorialKind.SON_HARF
            }
            shouldAutoShowTutorial(SonHarfPreferences.wordSiegeTutorialCompleted(context)) -> {
                automaticTutorial = true
                tutorial = FirstPlayerTutorialKind.WORD_SIEGE
            }
        }
    }

    if (!authChecked) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFFF7F9FC)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = SonHarfBlue)
        }
        return
    }

    if (!authenticated) {
        RequiredAuthGate {
            authenticated = true
        }
        return
    }

    fun completeCurrentTutorial() {
        val current = tutorial ?: return
        if (!automaticTutorial) {
            tutorial = null
            return
        }

        when (current) {
            FirstPlayerTutorialKind.SON_HARF -> SonHarfPreferences.setSonHarfTutorialCompleted(context)
            FirstPlayerTutorialKind.WORD_SIEGE -> SonHarfPreferences.setWordSiegeTutorialCompleted(context)
        }

        if (
            current == FirstPlayerTutorialKind.SON_HARF &&
            shouldAutoShowTutorial(SonHarfPreferences.wordSiegeTutorialCompleted(context))
        ) {
            tutorial = FirstPlayerTutorialKind.WORD_SIEGE
        } else {
            tutorial = null
            automaticTutorial = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SonHarfBg),
    ) {
        if (showAdminPanel && adminAuthorized) {
            AdminConsoleScreen(onBack = { showAdminPanel = false })
        } else {
            SonHarfMainApp(onSignedOut = {
                tutorial = null
                showHelpChooser = false
                showAdminPanel = false
                adminAuthorized = false
                automaticTutorial = false
                authenticated = false
            })

            if (tutorial == null) {
                FloatingActionButton(
                    onClick = { showHelpChooser = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 6.dp, end = 8.dp)
                        .size(42.dp),
                    containerColor = MainUi.Surface,
                    contentColor = MainUi.Blue,
                ) {
                    Icon(
                        Icons.Rounded.HelpOutline,
                        contentDescription = sh("Nasıl Oynanır?", "How to Play?"),
                        modifier = Modifier.size(22.dp),
                    )
                }

                if (adminAuthorized) {
                    ExtendedFloatingActionButton(
                        onClick = { showAdminPanel = true },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(top = 6.dp, start = 8.dp),
                        containerColor = MainUi.Text,
                        contentColor = MainUi.Surface,
                        icon = {
                            Icon(
                                Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                            )
                        },
                        text = { Text(sh("Admin Paneli", "Admin Panel")) },
                    )
                }
            }
        }
    }

    tutorial?.let { kind ->
        FirstPlayerTutorial(
            kind = kind,
            onSkip = ::completeCurrentTutorial,
            onDone = ::completeCurrentTutorial,
        )
    }

    if (showHelpChooser && tutorial == null && !showAdminPanel) {
        TutorialHelpChooser(
            onDismiss = { showHelpChooser = false },
            onSonHarf = {
                showHelpChooser = false
                automaticTutorial = false
                tutorial = FirstPlayerTutorialKind.SON_HARF
            },
            onWordSiege = {
                showHelpChooser = false
                automaticTutorial = false
                tutorial = FirstPlayerTutorialKind.WORD_SIEGE
            },
        )
    }
}
