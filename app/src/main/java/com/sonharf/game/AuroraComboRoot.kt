package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Main app + final active-match arena + short-lived action animations. */
@Composable
fun AuroraSonHarfAppWithCombo() {
    Box(Modifier.fillMaxSize()) {
        AuroraSonHarfApp()
        SketchGameOverlayV8()
        OnlineGameScreenComboOverlayOnly()
    }
}
