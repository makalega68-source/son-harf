package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Legacy preview name; now renders Eve exclusively. */
@Composable
internal fun MascotCardPreview(modifier: Modifier = Modifier) {
    Eve3DStage(modifier = modifier, compact = true)
}
