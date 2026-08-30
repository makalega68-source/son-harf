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
import com.sonharf.game.data.SupabaseProvider

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
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }

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
