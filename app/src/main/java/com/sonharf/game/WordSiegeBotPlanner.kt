package com.sonharf.game

import com.sonharf.game.data.fetchWordSiegeBotLexicon
import com.sonharf.game.data.validateWordSiegeDictionaryWords
import java.util.Locale
import kotlin.random.Random

internal data class WordSiegeBotPlan(
    val move: WordSiegePracticeMove?,
    val lexiconCount: Int,
    val structuralCandidateCount: Int,
    val validCandidateCount: Int,
)

/**
 * Online-backed training planner.
 *
 * Candidate words come from the same main dictionary used by human validation.
 * Geometry / connectivity / all newly formed word extraction is delegated to
 * WordSiegePracticeEngine.validateMove, so the bot does not get a relaxed rule set.
 * Expected invalid placements are filtered; unexpected exceptions are deliberately
 * allowed to escape so a code/server error can never be disguised as PASS.
 */
internal object WordSiegeBotPlanner {
    private val trLocale = Locale.forLanguageTag("tr-TR")

    suspend fun plan(
        state: WordSiegePracticeState,
        difficulty: TrainingBotDifficulty = TrainingBotDifficulty.MEDIUM,
        random: Random = Random.Default,
    ): WordSiegeBotPlan {
        require(state.status == "playing" && state.currentOwner == 2) { "word_siege_bot_not_turn" }
        val rack = state.botRack.uppercase(trLocale)
        require(rack.isNotBlank()) { "word_siege_bot_empty_rack" }
        val boardLetters = state.board.mapNotNull { it.letter?.firstOrNull() }.joinToString("")
        val lexicon = fetchWordSiegeBotLexicon(rack + boardLetters, "tr", 1000)
            .asSequence()
            .map { it.uppercase(trLocale) }
            .filter { it.length in 2..9 }
            .distinct()
            .toList()

        data class Structural(val move: WordSiegePracticeMove, val score: Int)
        val structural = ArrayList<Structural>()
        val seen = HashSet<String>()

        for (word in lexicon) {
            for (horizontal in listOf(true, false)) {
                for (start in 0..80) {
                    val placements = placementsForWord(state, rack, word, start, horizontal) ?: continue
                    val key = buildString {
                        append(horizontal).append(':')
                        placements.entries.sortedBy { it.key }.forEach { append(it.key).append('=').append(it.value).append(',') }
                    }
                    if (!seen.add(key)) continue
                    val candidate = try {
                        WordSiegePracticeEngine.validateMove(state, 2, placements, horizontal) { true }
                    } catch (_: WordSiegePracticeError) {
                        continue
                    }
                    structural += Structural(candidate, WordSiegePracticeEngine.botCandidateScore(state, candidate))
                }
            }
        }

        if (structural.isEmpty()) {
            return WordSiegeBotPlan(null, lexicon.size, 0, 0)
        }

        val wordsToValidate = structural.asSequence()
            .flatMap { it.move.formedWords.asSequence() }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .toList()
        val allowed = buildSet {
            wordsToValidate.chunked(600).forEach { addAll(validateWordSiegeDictionaryWords(it, "tr")) }
        }
        val valid = structural.filter { candidate -> candidate.move.formedWords.all { it in allowed } }
        if (valid.isEmpty()) {
            return WordSiegeBotPlan(null, lexicon.size, structural.size, 0)
        }

        val ranked = valid.sortedByDescending { it.score }
        val window = when (difficulty) {
            TrainingBotDifficulty.EASY -> minOf(6, ranked.size)
            TrainingBotDifficulty.MEDIUM -> minOf(3, ranked.size)
            TrainingBotDifficulty.HARD -> minOf(2, ranked.size)
        }
        val index = when (difficulty) {
            TrainingBotDifficulty.EASY -> random.nextInt(window)
            TrainingBotDifficulty.MEDIUM -> if (window == 1 || random.nextInt(100) < 65) 0 else random.nextInt(window)
            TrainingBotDifficulty.HARD -> if (window == 1 || random.nextInt(100) < 85) 0 else 1
        }
        return WordSiegeBotPlan(ranked[index].move, lexicon.size, structural.size, valid.size)
    }

    private fun placementsForWord(
        state: WordSiegePracticeState,
        rack: String,
        word: String,
        start: Int,
        horizontal: Boolean,
    ): Map<Int, Int>? {
        val delta = if (horizontal) 1 else 9
        if (horizontal && start % 9 + word.length > 9) return null
        if (!horizontal && start / 9 + word.length > 9) return null
        val used = mutableSetOf<Int>()
        val placements = linkedMapOf<Int, Int>()
        word.forEachIndexed { offset, letter ->
            val index = start + offset * delta
            val existing = state.board[index].letter?.firstOrNull()?.uppercaseChar()
            when {
                existing == letter -> Unit
                existing != null -> return null
                else -> {
                    val rackIndex = rack.indices.firstOrNull { it !in used && rack[it] == letter } ?: return null
                    used += rackIndex
                    placements[index] = rackIndex
                }
            }
        }
        return placements.takeIf { it.isNotEmpty() }
    }
}
