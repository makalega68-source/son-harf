package com.sonharf.game

internal fun wordSiegePracticeMoveNotice(
    move: WordSiegePracticeMove,
    turkish: Boolean,
): String {
    val areaScore = move.capturedCells.coerceAtLeast(0) * WordSiegeFinalRules.CUBE_TRANSFER_POINTS
    return if (turkish) {
        "+${move.wordScore} kelime • Alan +$areaScore"
    } else {
        "+${move.wordScore} word • Area +$areaScore"
    }
}
