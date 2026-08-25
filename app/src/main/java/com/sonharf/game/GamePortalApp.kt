package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
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

/** Main Son Harf shell plus the dedicated Eve AI companion surface. */
@Composable
fun GamePortalApp() {
    val context = LocalContext.current
    var eveOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RemoteExperience.refresh(context.applicationContext)
    }

    Box(Modifier.fillMaxSize()) {
        ClassicPremiumApp()

        if (!eveOpen) {
            FloatingActionButton(
                onClick = { eveOpen = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 86.dp),
                containerColor = PortalBlue,
                contentColor = Color.White,
            ) {
                Text("Eve ✦", fontWeight = FontWeight.Black)
            }
        }

        if (eveOpen) {
            EveMascotScreen(onClose = { eveOpen = false })
        }
    }
}
