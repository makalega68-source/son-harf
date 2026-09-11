package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PracticeBotCanonicalFallbackWords = listOf(
    "AD", "AK", "AL", "AN", "AR", "AS", "AT", "AV", "AY", "AZ",
    "BU", "DA", "DE", "EL", "EN", "ER", "ET", "EV", "İL", "İN",
    "İP", "İŞ", "İT", "Kİ", "NE", "OY", "ÖN", "SU", "YA",
    "ADA", "ANA", "ARA", "ARI", "ATA", "AYA", "BAL", "BAR", "BAŞ",
    "BEL", "BEN", "BİR", "BOL", "BOŞ", "CAN", "ÇAY", "DAL", "DAR",
    "DİL", "DİŞ", "DÜN", "GEL", "GÖL", "GÜL", "HAL", "KAR", "KAŞ",
    "KEL", "KOL", "KÖY", "KUŞ", "MAL", "MOR", "NAL", "NAR", "ODA",
    "OKU", "OL", "ON", "OT", "ÖL", "ÖZ", "PAZ", "SAÇ", "SAL", "SEN",
    "SES", "SIR", "SİL", "SOL", "SON", "TAŞ", "TEN", "TER", "TOP",
    "VAR", "VER", "YAR", "YAZ", "YEL", "YER", "YOL", "YÜZ",
)

/**
 * A resilient second-pass planner for practice matches.
 *
 * The adaptive planner prefers the verified bot dictionary. If that snapshot is missing on a fresh
 * install or because the external TDK endpoint is unavailable, a small conservative common-word
 * fallback is still tested against the canonical game dictionary before it can be played. This keeps
 * the bot functional without allowing invented words into the match.
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
            .take(14)
            .forEach { anchor -> add(rack + anchor) }
    }

    val words = linkedSetOf<String>()
    searchRacks.forEach { searchRack ->
        if (words.size < 700) {
            SharedDictionaryService.practiceCandidates(
                language = state.language,
                rack = searchRack,
                limit = 180,
            ).forEach { word ->
                if (words.size < 700) words += word
            }
        }
    }

    // Do not depend on a successfully downloaded verified-bot snapshot. These common words still
    // have to pass canonical dictionary validation inside WordSiegePracticeEngine.applyMove().
    words += PracticeBotCanonicalFallbackWords

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
