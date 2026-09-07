package com.sonharf.game

internal fun wordSiegePracticeMoveNotice(
    move: WordSiegePracticeMove,
    turkish: Boolean,
): String {
    val zoneScore = move.flippedZoneIds.sumOf(WordSiegeZoneRules::zoneValue)
    val fire = when {
        move.onslaughtTriggered -> if (turkish) " • 🔥 YIKIM HAMLESİ HAZIR" else " • 🔥 ONSLAUGHT READY"
        move.onslaughtConsumed -> if (turkish) " • 🔥 ×2" else " • 🔥 ×2"
        else -> ""
    }
    return if (turkish) {
        "+${move.wordScore} kelime • ${move.flippedZoneIds.size} bölge +$zoneScore$fire"
    } else {
        "+${move.wordScore} word • ${move.flippedZoneIds.size} zones +$zoneScore$fire"
    }
}
