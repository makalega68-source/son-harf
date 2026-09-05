package com.sonharf.game

import kotlin.math.abs

internal object ClassicCompetitionRules {
    const val URGENT_SECONDS = 10
    const val HAPTIC_SECONDS = 5
    const val LONG_WORD_LENGTH = 7
    const val STRONG_WORD_SCORE_DELTA = 20
    const val CRITICAL_SCORE_GAP = 10
    const val LEAD_OVERLAY_MS = 820L
    const val ACTION_OVERLAY_MS = 780L

    fun leader(myScore: Int, opponentScore: Int): Int = when {
        myScore > opponentScore -> 1
        myScore < opponentScore -> -1
        else -> 0
    }

    fun scoreDifferenceText(myScore: Int, opponentScore: Int, language: String): String {
        val diff = myScore - opponentScore
        return when {
            diff > 0 -> if (language == "en") "+$diff AHEAD" else "+$diff ÖNDESİN"
            diff < 0 -> if (language == "en") "${abs(diff)} POINTS BEHIND" else "${abs(diff)} PUAN GERİDESİN"
            else -> if (language == "en") "TIED" else "BERABERE"
        }
    }

    fun leadChangeText(previousLeader: Int, currentLeader: Int, language: String): String? {
        if (previousLeader == currentLeader) return null
        return when (currentLeader) {
            1 -> if (language == "en") "YOU TOOK THE LEAD" else "ÖNE GEÇTİN"
            -1 -> if (language == "en") "OPPONENT TOOK THE LEAD" else "RAKİP ÖNE GEÇTİ"
            0 -> if (previousLeader != 0) {
                if (language == "en") "SCORES TIED" else "SKORLAR EŞİT"
            } else null
            else -> null
        }
    }

    fun isUrgent(seconds: Int): Boolean = seconds in 0..URGENT_SECONDS
    fun shouldHaptic(seconds: Int): Boolean = seconds in 1..HAPTIC_SECONDS
    fun isLongWord(word: String): Boolean = word.trim().length >= LONG_WORD_LENGTH
    fun isStrongScoreDelta(delta: Int): Boolean = delta >= STRONG_WORD_SCORE_DELTA
    fun isCritical(myScore: Int, opponentScore: Int, finalMovesRemaining: Int): Boolean =
        abs(myScore - opponentScore) <= CRITICAL_SCORE_GAP || finalMovesRemaining in 1..4

    fun comboLabel(streak: Int, language: String): String? = when {
        streak >= 5 -> if (language == "en") "$streak WORD STREAK" else "$streak KELİME SERİSİ"
        streak >= 3 -> if (language == "en") "$streak WORD COMBO" else "$streak KELİME COMBO"
        else -> null
    }

    fun inputStartsWithRequired(input: String, required: String, language: String): Boolean? {
        if (input.isBlank() || required == "•") return null
        return gameUppercase(input.trim().take(1), language) == required
    }

    fun timerCadenceMs(seconds: Int): Long = when {
        seconds <= 3 -> 260L
        seconds <= 5 -> 360L
        seconds <= 10 -> 520L
        else -> 900L
    }
}
