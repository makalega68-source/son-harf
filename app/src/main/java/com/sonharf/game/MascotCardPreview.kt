package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility preview for older callers.
 * Always resolves through the canonical selected-Seal runtime.
 */
@Composable
internal fun MascotCardPreview(modifier: Modifier = Modifier) {
    MascotLive3DStage(
        modifier = modifier,
        mascotId = MascotSelectionRuntime.selectedId,
        motion = MascotMotion.IDLE,
    )
}
