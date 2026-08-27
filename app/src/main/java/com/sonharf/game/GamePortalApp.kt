package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal val PortalBg = Color(0xFFF4FBFF)
internal val PortalCard = Color.White
internal val PortalText = Color(0xFF173B57)
internal val PortalMuted = Color(0xFF6D879A)
internal val PortalBlue = Color(0xFF24AEE4)
internal val PortalGold = Color(0xFFFFC857)
internal val PortalGreen = Color(0xFF32C985)
internal val PortalRed = Color(0xFFFF7891)

/**
 * Root shell. HOME owns a fixed transparent Eve slot beside the Son Harf card; Eve does not
 * roam over the interface. The center bottom-navigation action opens the full Eve room overlay,
 * while gameplay and the other product surfaces remain unobstructed.
 */
@Composable
fun GamePortalApp() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        EveMascotRuntime.startLivingBehavior()
        onDispose { EveMascotRuntime.stopLivingBehavior() }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        RemoteExperience.refresh(context.applicationContext)
    }

    Box(Modifier.fillMaxSize()) {
        ClassicPremiumApp()
        EveMatchReactionBridge()
        EveMatchReactionOverlay()

        if (EveLivingRoomRuntime.open) {
            Box(Modifier.fillMaxSize()) {
                EveForestScreen(onNavigateBack = { EveLivingRoomRuntime.hide() })
            }
        }
    }
}