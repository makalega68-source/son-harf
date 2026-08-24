package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Kept for legacy HomeLobby.kt compile compatibility. The app no longer routes to that shell.
internal val PortalBg = Color(0xFFF4FBFF)
internal val PortalCard = Color.White
internal val PortalText = Color(0xFF173B57)
internal val PortalMuted = Color(0xFF6D879A)
internal val PortalBlue = Color(0xFF24AEE4)
internal val PortalGold = Color(0xFFFFC857)
internal val PortalGreen = Color(0xFF32C985)
internal val PortalRed = Color(0xFFFF7891)

/** Single premium shell with a verified real-time skeletal 3D mascot layer. */
@Composable
fun GamePortalApp() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        RemoteExperience.refresh(context.applicationContext)
    }

    Box(Modifier.fillMaxSize()) {
        ClassicPremiumApp()
        if (MascotPolicy.ENABLED && MascotPolicy.SKELETAL_ASSET_READY) {
            MascotBehaviorBridge()
            Mascot3DLayer(Modifier.fillMaxSize())
        }
    }
}
