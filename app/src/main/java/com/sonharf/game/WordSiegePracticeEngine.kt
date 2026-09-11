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
        val bonuses = WordSiegeBoardSpec.newGameBonuses(random)
        return WordSiegePracticeState(
            board = List(WordSiegeBoardSpec.CellCount) { index ->
                WordSiegeCellDto(bonus = bonuses[index])
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
        if (placements.values.distinct().size != placements.size || placements.values.any { it !in rack.indices }) fail("word_siege_invalid_rack_tile")
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
            if (owner == 2 && !SharedDictionaryService.isBotAllowedWord(word, state.language)) fail("word_siege_bot_filtered_word:$word")
            words += word
            if (primary == null) primary = word
            score += scoreWord(state.board, placements, rack, cells)
            cells.forEach { index ->
                val cell = board[index]
                if (cell.letter != null && cell.owner !in setOf(0, owner) && captured.add(index)) board[index] = cell.copy(owner = owner)
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

        // The surprise reward is a one-time move bonus, not a permanent power-up.
        val starBonus = placements.keys.count { index ->
            val cell = state.board[index]
            cell.letter == null && !cell.bonusUsed && cell.bonus == WordSiegeBoardSpec.StarBonus
        } * WordSiegeBoardSpec.StarBonusPoints
        score += starBonus

        placements.forEach { (index, rackIndex) ->
            board[index] = board[index].copy(letter = rack[rackIndex].toString(), owner = owner, bonusUsed = true)
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
        val next = state.copy(currentOwner = other(owner), consecutivePasses = (state.consecutivePasses + 1).coerceAtMost(2), moveCount = state.moveCount + 1, lastAction = "pass")
        return if (next.consecutivePasses >= 2) finish(next, "consecutive_passes") else next
    }

    fun exchange(state: WordSiegePracticeState, owner: Int, rackIndices: Set<Int>): WordSiegePracticeState {
        requireActiveTurn(state, owner)
        val rack = rackFor(state, owner)
        if (rackIndices.isEmpty() || rackIndices.size > 7 || rackIndices.any { it !in rack.indices } || state.bag.length < rackIndices.size) fail("word_siege_invalid_exchange")
        val returned = rack.filterIndexed { index, _ -> index in rackIndices }
        val remain = rack.filterIndexed { index, _ -> index !in rackIndices }
        val draw = state.bag.take(rackIndices.size)
        val nextBag = (state.bag.drop(draw.length) + returned).toList().shuffled().joinToString("")
        return state.copy(bag = nextBag, playerRack = if (owner == 1) remain + draw else state.playerRack, botRack = if (owner == 2) remain + draw else state.botRack, currentOwner = other(owner), consecutivePasses = 0, moveCount = state.moveCount + 1, lastAction = "exchange")
    }

    fun forfeit(state: WordSiegePracticeState, owner: Int): WordSiegePracticeState = finish(state.copy(lastAction = "forfeit"), "forfeit", other(owner))

    fun bestBotMove(state: WordSiegePracticeState, playerRating: Int = 1000, playerWins: Int = 0, playerLosses: Int = 0): WordSiegePracticeMove? {
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
        val ordered = candidates.distinctBy { it.placements }.sortedWith(compareBy<WordSiegePracticeMove> { moveStrength(it) }.thenBy { it.primaryWord }.thenBy { it.placements.keys.minOrNull() ?: -1 })
        val percentile = botTargetPercentile(state, playerRating, playerWins, playerLosses)
        val targetIndex = ((ordered.lastIndex * percentile) / 100).coerceIn(0, ordered.lastIndex)
        return ordered[targetIndex]
    }

    internal fun botTargetPercentile(state: WordSiegePracticeState, playerRating: Int, playerWins: Int, playerLosses: Int): Int {
        val games = playerWins + playerLosses
        var percentile = when {
            games < 3 -> 32
            playerRating < 900 -> 36
            playerRating < 1100 -> 44
            playerRating < 1300 -> 55
            playerRating < 1550 -> 66
            else -> 76
        }
        val playerTotal = totalScore(state, 1)
        val botTotal = totalScore(state, 2)
        percentile += when {
            botTotal - playerTotal >= 18 -> -18
            botTotal - playerTotal >= 8 -> -10
            playerTotal - botTotal >= 18 -> 12
            playerTotal - botTotal >= 8 -> 7
            else -> 0
        }
        return percentile.coerceIn(22, 84)
    }

    fun totalScore(state: WordSiegePracticeState, owner: Int): Int {
        val word = if (owner == 1) state.playerWordScore else state.botWordScore
        val area = if (owner == 1) state.playerArea else state.botArea
        return WordSiegeFinalRules.currentTerritoryScore(word, area)
    }

    private fun finish(state: WordSiegePracticeState, reason: String, forcedWinner: Int? = null): WordSiegePracticeState {
        val p = totalScore(state, 1)
        val b = totalScore(state, 2)
        val winner = forcedWinner ?: when { p > b -> 1; b > p -> 2; else -> null }
        return state.copy(status = "finished", winnerOwner = winner, lastAction = reason)
    }

    private fun requireActiveTurn(state: WordSiegePracticeState, owner: Int) {
        if (state.status != "playing" || state.currentOwner != owner) fail("word_siege_not_your_turn")
    }

    private fun other(owner: Int): Int = if (owner == 1) 2 else 1
    private fun fail(code: String): Nothing = throw WordSiegePracticeError(code)

    private fun moveStrength(move: WordSiegePracticeMove): Int = move.wordScore + move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS

    private fun placementsForWord(state: WordSiegePracticeState, rack: String, word: String, start: Int, horizontal: Boolean): Map<Int, Int>? {
        val normalized = word.uppercase(Locale.forLanguageTag(if (state.language == "tr") "tr-TR" else "en-US"))
        val row = WordSiegeBoardSpec.row(start)
        val column = WordSiegeBoardSpec.column(start)
        val endRow = if (horizontal) row else row + normalized.length - 1
        val endColumn = if (horizontal) column + normalized.length - 1 else column
        if (endRow >= WordSiegeBoardSpec.Size || endColumn >= WordSiegeBoardSpec.Size) return null
        val used = mutableSetOf<Int>()
        val result = linkedMapOf<Int, Int>()
        normalized.forEachIndexed { offset, char ->
            val idx = WordSiegeBoardSpec.index(if (horizontal) row else row + offset, if (horizontal) column + offset else column)
            val existing = state.board[idx].letter?.firstOrNull()
            if (existing != null) {
                if (existing != char) return null
            } else {
                val rackIndex = rack.indices.firstOrNull { it !in used && rack[it] == char } ?: return null
                used += rackIndex
                result[idx] = rackIndex
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun collectCells(anchor: Int, delta: Int, letterAt: (Int) -> Char?): List<Int> {
        var start = anchor
        while (true) {
            val previous = start - delta
            if (!WordSiegeBoardSpec.isValidIndex(previous)) break
            if (delta == WordSiegeBoardSpec.HorizontalDelta && WordSiegeBoardSpec.row(previous) != WordSiegeBoardSpec.row(start)) break
            if (letterAt(previous) == null) break
            start = previous
        }
        val result = mutableListOf<Int>()
        var current = start
        while (WordSiegeBoardSpec.isValidIndex(current) && letterAt(current) != null) {
            result += current
            val next = current + delta
            if (!WordSiegeBoardSpec.isValidIndex(next)) break
            if (delta == WordSiegeBoardSpec.HorizontalDelta && WordSiegeBoardSpec.row(next) != WordSiegeBoardSpec.row(current)) break
            current = next
        }
        return result
    }

    private fun scoreWord(board: List<WordSiegeCellDto>, placements: Map<Int, Int>, rack: String, cells: List<Int>): Int {
        var wordMultiplier = 1
        var sum = 0
        cells.forEach { index ->
            val cell = board[index]
            val letter = placements[index]?.let(rack::getOrNull)?.toString() ?: cell.letter.orEmpty()
            var value = WordSiegeFinalRules.letterValue(letter)
            if (index in placements && !cell.bonusUsed) {
                when (cell.bonus) {
                    "2H" -> value *= 2
                    "3H" -> value *= 3
                    "2K" -> wordMultiplier *= 2
                    "3K" -> wordMultiplier *= 3
                    WordSiegeBoardSpec.CenterBonus -> wordMultiplier *= 4
                }
            }
            sum += value
        }
        return sum * wordMultiplier
    }
}
