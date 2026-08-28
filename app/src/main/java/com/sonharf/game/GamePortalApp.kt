package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal val PortalBg = Color(0xFFF7F9FC)
internal val PortalCard = Color.White
internal val PortalText = Color(0xFF182235)
internal val PortalMuted = Color(0xFF718096)
internal val PortalBlue = Color(0xFF1769E0)
internal val PortalGold = Color(0xFFF3A81A)
internal val PortalGreen = Color(0xFF22B95F)
internal val PortalRed = Color(0xFFE64B55)

/**
 * Active Son Harf shell.
 * The product is word-game focused with simple navigation and a light UI.
 */
@Composable
fun GamePortalApp() {
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        RemoteExperience.refresh(context.applicationContext)
    }

    Box(Modifier.fillMaxSize()) {
        LightWordThemeApp()
    }
}
