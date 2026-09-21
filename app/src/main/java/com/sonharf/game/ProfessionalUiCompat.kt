package com.sonharf.game

import androidx.compose.foundation.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsKabaddi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Narrow compatibility helpers for the professional Compose rebuild.
 * These stay visual-only and do not alter gameplay or backend behavior.
 */
internal fun Modifier.background(brush: Brush, shape: RoundedCornerShape): Modifier =
    clip(shape).drawBehind { drawRect(brush = brush) }

internal val Icons.Rounded.Swords: ImageVector
    get() = Icons.Rounded.SportsKabaddi
