package com.sonharf.game

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Shared Kelime Tahtı readability tokens.
 *
 * These values are intentionally kept above the old compact sizing so letter-point values and
 * multiplier labels remain legible on a physical phone without changing board geometry or game
 * rules. Online and practice modes consume the same source of truth.
 */
internal object WordSiegeBoardAccessibility {
    val BoardLetterPoint: TextUnit = 14.sp
    val BoardBonus: TextUnit = 18.sp
    val RackPoint: TextUnit = 14.sp
}
