package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal val PortalBg = LetharaPalette.Night
internal val PortalCard = Color(0xFF101D39)
internal val PortalText = LetharaPalette.Text
internal val PortalMuted = LetharaPalette.Muted
internal val PortalBlue = LetharaPalette.Cyan
internal val PortalGold = LetharaPalette.Gold
internal val PortalGreen = LetharaPalette.Green
internal val PortalRed = LetharaPalette.Red

/**
 * Root shell for the active Son Harf product.
 * Eve remains archived and is not started, rendered, or exposed by active navigation.
 */
@Composable
fun GamePortalApp() {
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        RemoteExperience.refresh(context.applicationContext)
    }

    Box(Modifier.fillMaxSize()) {
        ClassicPremiumApp()
    }
}
