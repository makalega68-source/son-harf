package com.sonharf.game

internal object GameplayExcitementRules {
    const val TURN_SECONDS = 45
    const val CORRECT_WORD_POINTS = 3
    const val INVALID_WORD_POINTS = -1
    const val STREAK_TARGET = 5
    const val STREAK_BONUS_POINTS = 3

    fun wordsToNextTrivia(validWordCount: Int): Int {
        val remainder = validWordCount.coerceAtLeast(0) % STREAK_TARGET
        return if (remainder == 0) STREAK_TARGET else STREAK_TARGET - remainder
    }
}
