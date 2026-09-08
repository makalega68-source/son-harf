package com.sonharf.game

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Shared Kuşatma readability tokens. Keep board values legible in both practice and online modes. */
internal object WordSiegeBoardAccessibility {
    val BoardLetterPoint: TextUnit = WordSiegePracticeReadability.LetterPointSp.sp
    val BoardBonus: TextUnit = WordSiegePracticeReadability.BonusSp.sp
    val RackPoint: TextUnit = WordSiegePracticeReadability.RackPointSp.sp
}
