package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getEquippedCosmetics

/**
 * Production wrapper for the approved mockup UI.
 *
 * Keeps the accumulated membership/auth contract and global social/celebration
 * systems alive while the individual mockup screens are progressively wired to
 * their production data/services. Legacy navigation roots are intentionally not
 * mounted here.
 */
@Composable
fun IntegratedNeonSonHarfApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }

    // Preserve the production post-auth bootstrap without mounting any legacy UI.
    // Game-mode overlays and cosmetic rendering depend on these singleton states
    // being hydrated before the user reaches gameplay/profile surfaces.
    LaunchedEffect(authenticated) {
        if (!authenticated) return@LaunchedEffect
        val mode = runCatching { backend?.getPreferredGameMode() }.getOrNull()
        if (mode != null) SonHarfGameModeState.mode = mode
        SonHarfCosmetics.apply(runCatching { backend?.getEquippedCosmetics() }.getOrNull())
    }

    if (!authChecked) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF030613)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = SonHarfCyan)
        }
        return
    }

    if (!authenticated) {
        RequiredAuthGate { authenticated = true }
        return
    }

    Box(Modifier.fillMaxSize()) {
        MockupSonHarfApp()
        // v0.8.4 winner celebration retained above the new interface.
        WinnerFireworkOverlay()
        // v0.8.6 friend-only direct-chat quick access retained globally.
        FriendsQuickAccessOverlay()
    }
}
