package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Legacy entry point retained only for source compatibility.
 * The previous Son Harf pet implementation has been removed; all 3D mascot rendering now uses Eve.
 */
@Composable
internal fun Mascot3DLayer(modifier: Modifier = Modifier) {
    Eve3DStage(modifier = modifier)
}
