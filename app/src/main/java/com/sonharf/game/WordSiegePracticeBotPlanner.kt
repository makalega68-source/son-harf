package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A resilient second-pass planner for practice matches.
 *
 * The original adaptive planner searches words that can be produced from the rack alone. Once the
 * board contains letters, a legal word can use one or more existing board letters; searching only
 * the rack can therefore miss perfectly legal moves and make the bot pass repeatedly. This helper
 * keeps the adaptive planner as the first choice and, only when it finds no move, expands the
 * candidate pool with verified board-anchor letters. Every candidate still goes through
 * WordSiegePracticeEngine.applyMove(), including canonical dictionary and bot-word filtering.
 */
internal suspend fun resilientPracticeBotMove(
    state: WordSiegePracticeState,
    playerRating: Int,
    playerWins: Int,
    playerLosses: Int,
): WordSiegePracticeMove? = withContext(Dispatchers.Default) {
    WordSiegePracticeEngine.bestBotMove(
        state = state,
        playerRating = playerRating,
        playerWins = playerWins,
        playerLosses = playerLosses,
    )?.let { return@withContext it }

    if (state.currentOwner != 2 || state.status != "playing") return@withContext null
    val rack = state.botRack
    if (rack.isEmpty()) return@withContext null

    val searchRacks = buildList {
        add(rack)
        state.board.asSequence()
            .mapNotNull { it.letter?.firstOrNull() }
            .distinct()
            .take(10)
            .forEach { anchor -> add(rack + anchor) }
    }

    val words = linkedSetOf<String>()
    searchRacks.forEach { searchRack ->
        if (words.size < 360) {
            SharedDictionaryService.practiceCandidates(
                language = state.language,
                rack = searchRack,
                limit = 90,
            ).forEach { word ->
                if (words.size < 360) words += word
            }
        }
    }

    val candidates = mutableListOf<WordSiegePracticeMove>()
    words.forEach { word ->
        listOf(true, false).forEach { horizontal ->
            (0 until WordSiegeBoardSpec.CellCount).forEach startLoop@{ start ->
                val placements = resilientPlacementsForWord(state, rack, word, start, horizontal)
                    ?: return@startLoop
                val candidate = runCatching {
                    WordSiegePracticeEngine.applyMove(state, 2, placements)
                }.getOrNull() ?: return@startLoop
                candidates += candidate.second
            }
        }
    }

    if (candidates.isEmpty()) return@withContext null
    val ordered = candidates
        .distinctBy { it.placements }
        .sortedWith(
            compareBy<WordSiegePracticeMove> {
                it.wordScore + WordSiegeFinalRules.cubeTransfer(it.capturedCells)
            }.thenBy { it.primaryWord }
                .thenBy { it.placements.keys.minOrNull() ?: -1 },
        )
    val percentile = WordSiegePracticeEngine.botTargetPercentile(
        state = state,
        playerRating = playerRating,
        playerWins = playerWins,
        playerLosses = playerLosses,
    )
    val targetIndex = ((ordered.lastIndex * percentile) / 100).coerceIn(0, ordered.lastIndex)
    ordered[targetIndex]
}

private fun resilientPlacementsForWord(
    state: WordSiegePracticeState,
    rack: String,
    word: String,
    start: Int,
    horizontal: Boolean,
): Map<Int, Int>? {
    val delta = if (horizontal) WordSiegeBoardSpec.HorizontalDelta else WordSiegeBoardSpec.VerticalDelta
    if (!WordSiegeBoardSpec.isValidIndex(start)) return null
    if (horizontal && WordSiegeBoardSpec.column(start) + word.length > WordSiegeBoardSpec.Size) return null
    if (!horizontal && WordSiegeBoardSpec.row(start) + word.length > WordSiegeBoardSpec.Size) return null

    val used = mutableSetOf<Int>()
    val placements = linkedMapOf<Int, Int>()
    var usedBoardLetter = false
    word.forEachIndexed { offset, letter ->
        val index = start + offset * delta
        if (!WordSiegeBoardSpec.isValidIndex(index)) return null
        val existing = state.board[index].letter?.firstOrNull()
        when {
            existing == letter -> usedBoardLetter = true
            existing != null -> return null
            else -> {
                val rackIndex = rack.indices.firstOrNull { it !in used && rack[it] == letter } ?: return null
                used += rackIndex
                placements[index] = rackIndex
            }
        }
    }

    // After the opening move, the fallback is specifically for anchor-based legal plays.
    val boardHasLetters = state.board.any { it.letter != null }
    if (boardHasLetters && !usedBoardLetter) return null
    return placements.takeIf { it.isNotEmpty() }
}

internal fun practiceBotExchangeIndices(state: WordSiegePracticeState): Set<Int> {
    if (state.currentOwner != 2 || state.status != "playing") return emptySet()
    val count = minOf(3, state.botRack.length, state.bag.length)
    return (0 until count).toSet()
}
