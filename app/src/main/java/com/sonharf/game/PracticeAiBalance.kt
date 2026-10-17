package com.sonharf.game

import android.content.Context

/**
 * Remembers who won the last match against the AI, so matches take turns: after an AI win the
 * next one is the player's to take, and after a player win the AI plays to win.
 */
internal object PracticeAiBalance {
    private const val PREFS = "practice_ai_balance"
    private const val KEY = "ai_won_last"

    /** null until the first finished match. */
    @Volatile
    var aiWonLast: Boolean? = null
        private set

    fun restore(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        aiWonLast = if (prefs.contains(KEY)) prefs.getBoolean(KEY, false) else null
    }

    /** A draw keeps the previous turn. */
    fun record(context: Context, winnerOwner: Int?) {
        val aiWon = when (winnerOwner) {
            2 -> true
            1 -> false
            else -> return
        }
        aiWonLast = aiWon
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, aiWon).apply()
    }
}
