package com.sonharf.game.domain

import java.util.Locale

data class ChainEntry(val player: Int, val word: String)

data class GameState(
    val currentPlayer: Int = 1,
    val chain: List<ChainEntry> = emptyList(),
    val winner: Int? = null,
    val message: String = "İlk kelimeyi yaz"
)

class WordChainEngine {
    private val tr = Locale.forLanguageTag("tr-TR")

    fun submit(state: GameState, rawWord: String): GameState {
        if (state.winner != null) return state
        val word = rawWord.trim().lowercase(tr)
        if (word.length < 2 || !word.all { it.isLetter() }) {
            return state.copy(message = "Geçerli bir kelime yaz")
        }
        if (state.chain.any { it.word == word }) {
            return state.copy(message = "Bu kelime daha önce kullanıldı")
        }
        val previous = state.chain.lastOrNull()?.word
        if (previous != null && previous.last() != word.first()) {
            return state.copy(message = "Kelime '${previous.last().uppercaseChar()}' ile başlamalı")
        }
        val next = if (state.currentPlayer == 1) 2 else 1
        return state.copy(
            currentPlayer = next,
            chain = state.chain + ChainEntry(state.currentPlayer, word),
            message = "Sıra Oyuncu $next'de"
        )
    }

    fun forfeit(state: GameState): GameState {
        val winner = if (state.currentPlayer == 1) 2 else 1
        return state.copy(winner = winner, message = "Oyuncu $winner kazandı")
    }
}
