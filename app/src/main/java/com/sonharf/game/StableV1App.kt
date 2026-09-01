package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import com.sonharf.game.data.SupabaseProvider

/**
 * V1 stabilization shell.
 *
 * Product scope is intentionally narrow:
 * app UI language -> verified auth -> core duel lobby -> live match -> result/rematch.
 * UI language is a local presentation preference and is deliberately independent
 * from Son Harf / Kelime Kuşatması match and dictionary language state.
 */
@Composable
fun StableV1App() {
    val context = LocalContext.current
    var languageSelected by remember { mutableStateOf(SonHarfPreferences.hasSelectedLanguage(context)) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }

    if (!languageSelected) {
        AppLanguageSelectionScreen { language ->
            SonHarfPreferences.setLanguage(context, language)
            languageSelected = true
        }
        return
    }

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
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

    Box(
        Modifier
            .fillMaxSize()
            .background(SonHarfBg),
    ) {
        SonHarfMainApp(onSignedOut = { authenticated = false })
    }
}
