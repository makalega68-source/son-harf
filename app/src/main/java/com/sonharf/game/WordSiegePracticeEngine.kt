package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.WordSiegeCellDto
import java.util.Locale
import kotlin.random.Random

internal const val WordSiegePracticeYou = "practice-you"
internal const val WordSiegePracticeBot = "practice-bot"

internal data class WordSiegePracticeState(
    val board: List<WordSiegeCellDto>,
    val bag: String,
    val playerRack: String,
    val botRack: String,
    val language: String = "tr",
    val currentOwner: Int = 1,
    val playerWordScore: Int = 0,
    val botWordScore: Int = 0,
    val playerAreaScore: Int = 0,
    val botAreaScore: Int = 0,
    val playerArea: Int = 0,
    val botArea: Int = 0,
    val consecutivePasses: Int = 0,
    val moveCount: Int = 0,
    val status: String = "playing",
    val winnerOwner: Int? = null,
    val lastAction: String? = null,
)

internal data class WordSiegePracticeMove(
    val placements: Map<Int, Int>,
    val horizontal: Boolean,
    val primaryWord: String,
    val formedWords: List<String>,
    val wordScore: Int,
    val capturedCells: Int,
)

internal class WordSiegePracticeError(val code: String) : IllegalArgumentException(code)

/** Local practice rules backed by the same canonical dictionary service used by Son Harf. */
internal object WordSiegePracticeEngine {
    fun newGame(language: String = "tr", random: Random = Random.Default): WordSiegePracticeState {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        val shuffled = WordSiegeBoardSpec.shuffledBag(lang, random)
        return WordSiegePracticeState(
            board = List(WordSiegeBoardSpec.CellCount) { index ->
                WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index))
            },
            playerRack = shuffled.take(7),
            botRack = shuffled.drop(7).take(7),
            bag = shuffled.drop(14),
            language = lang,
        )
    }

    fun rackFor(state: WordSiegePracticeState, owner: Int): String =
        if (owner == 1) state.playerRack else state.botRack

    fun applyMove(
        state: WordSiegePracticeState,
        owner: Int,
        placements: Map<Int, Int>,
    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> {
        val horizontal = WordSiegeFinalRules.detectOrientation(state.board, placements.keys) == WordSiegeOrientation.HORIZONTAL
        return applyMoveResolved(state, owner, placements, horizontal)
    }

    /** Compatibility overload: direction is deliberately ignored; final rules always auto-detect it. */
    fun applyMove(
        state: WordSiegePracticeState,
        owner: Int,
        placements: Map<Int, Int>,
        @Suppress("UNUSED_PARAMETER") horizontal: Boolean,
    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> = applyMove(state, owner, placements)

    private fun applyMoveResolved(
        state: WordSiegePracticeState,
        owner: Int,
        placements: Map<Int, Int>,
        horizontal: Boolean,
    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> {
        requireActiveTurn(state, owner)
        if (placements.size !in 1..7) fail("word_siege_invalid_placements")
        val rack = rackFor(state, owner)
        if (placements.keys.any { !WordSiegeBoardSpec.isValidIndex(it) }) fail("word_siege_invalid_cell")
        if (placements.values.distinct().size != placements.size || placements.values.any { it !in rack.indices }) {
            fail("word_siege_invalid_rack_tile")
        }
        if (placements.keys.any { state.board[it].letter != null }) fail("word_siege_cell_occupied")

        val indices = placements.keys.sorted()
        val anchor = indices.first()
        if (indices.size > 1) {
            if (horizontal && indices.any { WordSiegeBoardSpec.row(it) != WordSiegeBoardSpec.row(anchor) }) fail("word_siege_not_in_one_row")
            if (!horizontal && indices.any { WordSiegeBoardSpec.column(it) != WordSiegeBoardSpec.column(anchor) }) fail("word_siege_not_in_one_column")
        }

        fun letterAt(index: Int): Char? = placements[index]?.let(rack::getOrNull) ?: state.board[index].letter?.firstOrNull()
        val mainCells = collectCells(anchor, if (horizontal) WordSiegeBoardSpec.HorizontalDelta else WordSiegeBoardSpec.VerticalDelta, ::letterAt)
        if (!indices.all(mainCells::contains)) fail("word_siege_gap_between_tiles")

        val hasBoardLetter = state.board.any { it.letter != null }
        if (!hasBoardLetter && WordSiegeBoardSpec.CenterIndex !in indices) fail("word_siege_first_word_must_cover_center")
        var connected = hasBoardLetter && mainCells.any { state.board[it].letter != null }
        val words = mutableListOf<String>()
        var primary: String? = null
        var score = 0
        val captured = linkedSetOf<Int>()
        val board = state.board.toMutableList()

        fun acceptWord(cells: List<Int>) {
            if (cells.size < 2) return
            val word = cells.joinToString("") { letterAt(it)?.toString().orEmpty() }
            if (!SharedDictionaryService.isValidWordBlocking(word, state.language)) fail("word_siege_invalid_word:$word")
            words += word
            if (primary == null) primary = word
            score += scoreWord(state.board, placements, rack, cells)
            cells.forEach { index ->
                val cell = board[index]
                if (cell.letter != null && cell.owner !in setOf(0, owner) && captured.add(index)) {
                    board[index] = cell.copy(owner = owner)
                }
            }
        }

        acceptWord(mainCells)
        indices.forEach { index ->
            val cross = collectCells(index, if (horizontal) WordSiegeBoardSpec.VerticalDelta else WordSiegeBoardSpec.HorizontalDelta, ::letterAt)
            if (cross.size > 1) {
                connected = connected || hasBoardLetter
                acceptWord(cross)
            }
        }
        if (words.isEmpty()) fail("word_siege_word_required")
        if (hasBoardLetter && !connected) fail("word_siege_move_must_connect")

        placements.forEach { (index, rackIndex) ->
            board[index] = board[index].copy(
                letter = rack[rackIndex].toString(),
                owner = owner,
                bonusUsed = true,
            )
        }
        val remainingRack = rack.filterIndexed { index, _ -> index !in placements.values }
        val drawCount = (7 - remainingRack.length).coerceAtLeast(0)
        val draw = state.bag.take(drawCount)
        val nextRack = remainingRack + draw
        val nextBag = state.bag.drop(draw.length)
        val gainedCells = board.indices.count { index -> state.board[index].owner != owner && board[index].owner == owner }
        val playerArea = board.count { it.owner == 1 }
        val botArea = board.count { it.owner == 2 }
        val next = state.copy(
            board = board,
            bag = nextBag,
            playerRack = if (owner == 1) nextRack else state.playerRack,
            botRack = if (owner == 2) nextRack else state.botRack,
            currentOwner = other(owner),
            playerWordScore = state.playerWordScore + if (owner == 1) score else 0,
            botWordScore = state.botWordScore + if (owner == 2) score else 0,
            playerAreaScore = WordSiegeFinalRules.cubeTransfer(playerArea),
            botAreaScore = WordSiegeFinalRules.cubeTransfer(botArea),
            playerArea = playerArea,
            botArea = botArea,
            consecutivePasses = 0,
            moveCount = state.moveCount + 1,
            lastAction = "word:${primary.orEmpty()}",
        )
        val finished = if (nextBag.isEmpty() && nextRack.isEmpty()) finish(next, "rack_empty") else next
        return finished to WordSiegePracticeMove(placements, horizontal, primary.orEmpty(), words, score, gainedCells)
    }

    fun pass(state: WordSiegePracticeState, owner: Int): WordSiegePracticeState {
        requireActiveTurn(state, owner)
        val next = state.copy(
            currentOwner = other(owner),
            consecutivePasses = (state.consecutivePasses + 1).coerceAtMost(2),
            moveCount = state.moveCount + 1,
            lastAction = "pass",
        )
        return if (next.consecutivePasses >= 2) finish(next, "consecutive_passes") else next
    }

    fun exchange(state: WordSiegePracticeState, owner: Int, rackIndices: Set<Int>): WordSiegePracticeState {
        requireActiveTurn(state, owner)
        val rack = rackFor(state, owner)
        if (rackIndices.isEmpty() || rackIndices.size > 7 || rackIndices.any { it !in rack.indices } || state.bag.length < rackIndices.size) {
            fail("word_siege_invalid_exchange")
        }
        val returned = rack.filterIndexed { index, _ -> index in rackIndices }
        val remain = rack.filterIndexed { index, _ -> index !in rackIndices }
        val draw = state.bag.take(rackIndices.size)
        val nextBag = (state.bag.drop(draw.length) + returned).toList().shuffled().joinToString("")
        return state.copy(
            bag = nextBag,
            playerRack = if (owner == 1) remain + draw else state.playerRack,
            botRack = if (owner == 2) remain + draw else state.botRack,
            currentOwner = other(owner),
            consecutivePasses = 0,
            moveCount = state.moveCount + 1,
            lastAction = "exchange",
        )
    }

    fun forfeit(state: WordSiegePracticeState, owner: Int): WordSiegePracticeState =
        finish(state.copy(lastAction = "forfeit"), "forfeit", other(owner))

    /**
     * Picks a legal move from a skill percentile rather than always choosing the absolute best move.
     * New/inexperienced players get a forgiving bot. Rating, record and the live score gradually raise
     * or lower the target without ever making the practice bot perfect.
     */
    fun bestBotMove(
        state: WordSiegePracticeState,
        playerRating: Int = 1000,
        playerWins: Int = 0,
        playerLosses: Int = 0,
    ): WordSiegePracticeMove? {
        if (state.currentOwner != 2 || state.status != "playing") return null
        val rack = state.botRack
        val candidates = mutableListOf<WordSiegePracticeMove>()
        SharedDictionaryService.practiceCandidates(state.language, rack).forEach { word ->
            listOf(true, false).forEach { horizontal ->
                (0 until WordSiegeBoardSpec.CellCount).forEach startLoop@{ start ->
                    val placements = placementsForWord(state, rack, word, start, horizontal) ?: return@startLoop
                    val candidate = runCatching { applyMove(state, 2, placements) }.getOrNull() ?: return@startLoop
                    candidates += candidate.second
                }
            }
        }
        if (candidates.isEmpty()) return null

        val ordered = candidates
            .distinctBy { it.placements }
            .sortedWith(
                compareBy<WordSiegePracticeMove> { moveStrength(it) }
                    .thenBy { it.primaryWord }
                    .thenBy { it.placements.keys.minOrNull() ?: -1 },
            )
        val percentile = botTargetPercentile(state, playerRating, playerWins, playerLosses)
        val targetIndex = ((ordered.lastIndex * percentile) / 100).coerceIn(0, ordered.lastIndex)
        return ordered[targetIndex]
    }

    internal fun botTargetPercentile(
        state: WordSiegePracticeState,
        playerRating: Int,
        playerWins: Int,
        playerLosses: Int,
    ): Int {
        val wins = playerWins.coerceAtLeast(0)
        val losses = playerLosses.coerceAtLeast(0)
        val games = wins + losses
        val winRate = if (games == 0) 50 else (wins * 100) / games
        var target = when {
            games < 3 -> 32
            playerRating < 900 -> 36
            playerRating < 1050 -> 45
            playerRating < 1200 -> 57
            playerRating < 1400 -> 69
            else -> 81
        }

        if (games >= 5 && winRate >= 60) target += 7
        if (games >= 5 && winRate <= 35) target -= 7

        val botLead = totalScore(state, 2) - totalScore(state, 1)
        when {
            botLead >= 16 -> target -= 14
            botLead >= 8 -> target -= 8
            botLead <= -16 -> target += 7
            botLead <= -8 -> target += 4
        }
        if (state.moveCount < 4) target -= 5

        return target.coerceIn(25, 90)
    }

    private fun moveStrength(move: WordSiegePracticeMove): Int =
        move.wordScore + WordSiegeFinalRules.cubeTransfer(move.capturedCells)

    private fun placementsForWord(
        state: WordSiegePracticeState,
        rack: String,
        word: String,
        start: Int,
        horizontal: Boolean,
    ): Map<Int, Int>? {
        val delta = if (horizontal) WordSiegeBoardSpec.HorizontalDelta else WordSiegeBoardSpec.VerticalDelta
        if (horizontal && WordSiegeBoardSpec.column(start) + word.length > WordSiegeBoardSpec.Size) return null
        if (!horizontal && WordSiegeBoardSpec.row(start) + word.length > WordSiegeBoardSpec.Size) return null
        val used = mutableSetOf<Int>()
        val placements = linkedMapOf<Int, Int>()
        word.forEachIndexed { offset, letter ->
            val index = start + offset * delta
            val existing = state.board[index].letter?.firstOrNull()
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

    private fun scoreWord(
        board: List<WordSiegeCellDto>,
        placements: Map<Int, Int>,
        rack: String,
        cells: List<Int>,
    ): Int {
        var total = 0
        var multiplier = 1
        cells.forEach { index ->
            val cell = board[index]
            val letter = placements[index]?.let(rack::getOrNull)?.toString() ?: cell.letter.orEmpty()
            var value = letterValue(letter)
            val bonus = if (cell.letter == null && !cell.bonusUsed) cell.bonus else null
            if (bonus == "2H") value *= 2
            if (bonus == "3H") value *= 3
            if (bonus == "2K") multiplier *= 2
            if (bonus == "3K") multiplier *= 3
            total += value
        }
        return total * multiplier
    }

    private fun collectCells(anchor: Int, delta: Int, letterAt: (Int) -> Char?): List<Int> {
        if (letterAt(anchor) == null) return emptyList()
        var start = anchor
        while (true) {
            val previous = start - delta
            if (!WordSiegeBoardSpec.isValidIndex(previous) ||
                (delta == WordSiegeBoardSpec.HorizontalDelta && WordSiegeBoardSpec.row(previous) != WordSiegeBoardSpec.row(start)) ||
                letterAt(previous) == null
            ) break
            start = previous
        }
        val result = mutableListOf<Int>()
        var current = start
        while (WordSiegeBoardSpec.isValidIndex(current) &&
            !(delta == WordSiegeBoardSpec.HorizontalDelta && WordSiegeBoardSpec.row(current) != WordSiegeBoardSpec.row(start)) &&
            letterAt(current) != null
        ) {
            result += current
            val next = current + delta
            if (!WordSiegeBoardSpec.isValidIndex(next) ||
                (delta == WordSiegeBoardSpec.HorizontalDelta && WordSiegeBoardSpec.row(next) != WordSiegeBoardSpec.row(current))
            ) break
            current = next
        }
        return result
    }

    private fun finish(state: WordSiegePracticeState, reason: String, forcedWinner: Int? = null): WordSiegePracticeState {
        val winner = forcedWinner ?: when {
            totalScore(state, 1) > totalScore(state, 2) -> 1
            totalScore(state, 2) > totalScore(state, 1) -> 2
            state.playerArea > state.botArea -> 1
            state.botArea > state.playerArea -> 2
            else -> null
        }
        return state.copy(status = "finished", winnerOwner = winner, lastAction = reason)
    }

    fun totalScore(state: WordSiegePracticeState, owner: Int): Int = if (owner == 1) {
        WordSiegeFinalRules.currentTerritoryScore(state.playerWordScore, state.playerArea)
    } else {
        WordSiegeFinalRules.currentTerritoryScore(state.botWordScore, state.botArea)
    }

    private fun requireActiveTurn(state: WordSiegePracticeState, owner: Int) {
        if (state.status != "playing") fail("word_siege_not_playing")
        if (state.currentOwner != owner) fail("word_siege_not_your_turn")
    }

    private fun letterValue(letter: String): Int = when (letter.uppercase(Locale.forLanguageTag("tr-TR"))) {
        "A", "E", "İ", "K", "L", "N", "R", "T" -> 1
        "I", "M", "O", "S", "U" -> 2
        "B", "D", "Ü", "Y" -> 3
        "C", "Ç", "Ş", "Z" -> 4
        "G", "H", "P" -> 5
        "F", "Ö", "V" -> 7
        "Ğ" -> 8
        "J" -> 10
        else -> 1
    }

    private fun other(owner: Int): Int = if (owner == 1) 2 else 1
    private fun fail(code: String): Nothing = throw WordSiegePracticeError(code)
}