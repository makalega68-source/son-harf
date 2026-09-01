package com.sonharf.game

import com.sonharf.game.data.fetchWordSiegeBotLexicon
import com.sonharf.game.data.validateWordSiegeDictionaryWords
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal data class WordSiegeBotPlan(
    val move: WordSiegePracticeMove?,
    val lexiconCount: Int,
    val structuralCandidateCount: Int,
    val validCandidateCount: Int,
    val anchorCount: Int,
    val passReason: String? = null,
)

/**
 * Online-backed training planner.
 *
 * Long-match guard: a full-board alphabet is too broad for a capped dictionary feed.
 * Once the board grows, the old single `rack + every board letter` query could fill
 * its limit with structurally irrelevant words and starve playable anchor words.
 * We now request small anchor-focused feeds first, then merge a smaller broad feed.
 * Shared move validation remains authoritative; validation is never relaxed.
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
        val anchorCount = countPlayableAnchors(state)
        val lexicon = fetchPrioritizedLexicon(state, rack)
        return planFromLexicon(state, lexicon, difficulty, random, anchorCount) { words ->
            buildSet {
                words.chunked(600).forEach { addAll(validateWordSiegeDictionaryWords(it, "tr")) }
            }
        }
    }

    internal suspend fun planFromLexicon(
        state: WordSiegePracticeState,
        lexiconInput: Collection<String>,
        difficulty: TrainingBotDifficulty = TrainingBotDifficulty.MEDIUM,
        random: Random = Random.Default,
        anchorCount: Int = countPlayableAnchors(state),
        validateWords: suspend (List<String>) -> Set<String> = { it.toSet() },
    ): WordSiegeBotPlan {
        require(state.status == "playing" && state.currentOwner == 2) { "word_siege_bot_not_turn" }
        val rack = state.botRack.uppercase(trLocale)
        require(rack.isNotBlank()) { "word_siege_bot_empty_rack" }
        val lexicon = lexiconInput.asSequence()
            .map { it.trim().uppercase(trLocale) }
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
            return WordSiegeBotPlan(null, lexicon.size, 0, 0, anchorCount, "no_structural_candidate")
        }

        val wordsToValidate = structural.asSequence()
            .flatMap { it.move.formedWords.asSequence() }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .toList()
        val allowed = validateWords(wordsToValidate)
        val valid = structural.filter { candidate -> candidate.move.formedWords.all { it in allowed } }
        if (valid.isEmpty()) {
            return WordSiegeBotPlan(null, lexicon.size, structural.size, 0, anchorCount, "dictionary_rejected_all")
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
        return WordSiegeBotPlan(ranked[index].move, lexicon.size, structural.size, valid.size, anchorCount)
    }

    private suspend fun fetchPrioritizedLexicon(state: WordSiegePracticeState, rack: String): List<String> = coroutineScope {
        val boardLetters = state.board.mapNotNull { it.letter?.firstOrNull()?.uppercaseChar() }
        if (boardLetters.isEmpty()) return@coroutineScope fetchWordSiegeBotLexicon(rack, "tr", 1000)

        val anchorLetters = state.board.indices.asSequence()
            .filter { state.board[it].letter != null }
            .filter { index -> neighbors(index).any { state.board[it].letter == null } }
            .mapNotNull { state.board[it].letter?.firstOrNull()?.uppercaseChar() }
            .distinct()
            .take(8)
            .toList()

        val focused = anchorLetters.map { anchor ->
            async { fetchWordSiegeBotLexicon(rack + anchor, "tr", 220) }
        }
        val broad = async {
            val distinctBoardAlphabet = boardLetters.distinct().joinToString("")
            fetchWordSiegeBotLexicon(rack + distinctBoardAlphabet, "tr", 500)
        }
        (focused.awaitAll().flatten() + broad.await()).asSequence()
            .map { it.uppercase(trLocale) }
            .filter { it.length in 2..9 }
            .distinct()
            .take(2200)
            .toList()
    }

    internal fun countPlayableAnchors(state: WordSiegePracticeState): Int {
        if (state.board.none { it.letter != null }) return 1
        return state.board.indices.count { index ->
            state.board[index].letter == null && neighbors(index).any { state.board[it].letter != null }
        }
    }

    private fun neighbors(index: Int): List<Int> {
        val row = index / 9
        val col = index % 9
        return buildList(4) {
            if (row > 0) add(index - 9)
            if (row < 8) add(index + 9)
            if (col > 0) add(index - 1)
            if (col < 8) add(index + 1)
        }
    }

    private fun placementsForWord(
        state: WordSiePracticeState,
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
