package com.sonharf.game

/** Lightweight score preview used before a Kuşatma move is submitted. */
internal data class WordSiegeMovePreview(
    val wordScore: Int,
    val capturedCells: Int,
) {
    val areaScore: Int get() = capturedCells.coerceAtLeast(0) * 2
    val totalScore: Int get() = wordScore.coerceAtLeast(0) + areaScore
}

internal fun wordSiegeMovePreviewLabel(preview: WordSiegeMovePreview, turkish: Boolean): String =
    if (turkish) "Kelime +${preview.wordScore} • Alan +${preview.areaScore} → Toplam +${preview.totalScore}"
    else "Word +${preview.wordScore} • Area +${preview.areaScore} → Total +${preview.totalScore}"
