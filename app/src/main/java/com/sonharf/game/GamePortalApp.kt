package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Kept for legacy HomeLobby.kt compile compatibility. The app no longer routes to that shell.
internal val PortalBg = Color(0xFFF4FBFF)
internal val PortalCard = Color.White
internal val PortalText = Color(0xFF173B57)
internal val PortalMuted = Color(0xFF6D879A)
internal val PortalBlue = Color(0xFF24AEE4)
internal val PortalGold = Color(0xFFFFC857)
internal val PortalGreen = Color(0xFF32C985)
internal val PortalRed = Color(0xFFFF7891)

/**
 * Single premium shell. The mascot owns a reserved rail instead of floating above controls,
 * so it cannot cover gameplay, buttons, word input, score or navigation.
 */
@Composable
fun GamePortalApp() {
    if (!MascotPolicy.ENABLED) {
        ClassicPremiumApp()
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val railWidth = if (maxWidth < 420.dp) 86.dp else 104.dp
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) { ClassicPremiumApp() }
            MascotReservedRail(Modifier.width(railWidth).fillMaxHeight())
        }
    }
}
