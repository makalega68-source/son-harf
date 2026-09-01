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
import com.sonharf.game.data.SupabaseProvider

/**
 * V1 stabilization shell.
 *
 * Verified auth remains unchanged. After authentication the active product shell
 * is the Monster-inspired Son Harf experience. Legacy visual shells stay in the
 * repository only as rollback material.
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
            Modifier.fillMaxSize().background(MonsterUi.Background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MonsterUi.Accent)
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
            .background(MonsterUi.Background),
    ) {
        MonsterExperienceApp(onSignedOut = { authenticated = false })
    }
}
