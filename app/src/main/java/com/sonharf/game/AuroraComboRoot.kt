package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Keeps the existing Aurora app intact and layers the live streak action system above active matches. */
@Composable
fun AuroraSonHarfAppWithCombo() {
    Box(Modifier.fillMaxSize()) {
        AuroraSonHarfApp()
        OnlineGameScreenComboOverlayOnly()
    }
}
