package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Main app + V9 active-match arena + frameless confetti action overlay. */
@Composable
fun AuroraSonHarfAppWithCombo() {
    Box(Modifier.fillMaxSize()) {
        AuroraSonHarfApp()
        SketchGameOverlayV9()
        ComboOverlayV9()
    }
}
