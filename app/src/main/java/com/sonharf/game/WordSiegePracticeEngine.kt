package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto
import java.util.Locale

internal const val WordSiegePracticeYou = "practice-you"
internal const val WordSiegePracticeBot = "practice-bot"

internal data class WordSiegePracticeState(
    val board: List<WordSiegeCellDto>,
    val bag: String,
    val playerRack: String,
    val botRack: String,
    val currentOwner: Int = 1,
    val playerWordScore: Int = 0,
    val botWordScore: Int = 0,
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

/** Local, connection-free practice rules. Online matches still validate against the server dictionary. */
internal object WordSiegePracticeEngine {
    private val trLocale = Locale.forLanguageTag("tr-TR")

    // Deliberately friendly opening racks make the first practice game immediately playable.
    fun newGame(): WordSiegePracticeState = WordSiegePracticeState(
        board = List(81) { index ->
            WordSiegeCellDto(
                bonus = when (index) {
                    0, 8, 72, 80 -> "3K"
                    4, 36, 44, 76 -> "3H"
                    10, 16, 64, 70, 40 -> "2K"
                    20, 24, 56, 60 -> "2H"
                    else -> null
                },
            )
        },
        playerRack = "KALEMTR",
        botRack = "MASASİN",
        bag = "ELMALİSTEKARTONURBİLGİSAYARÇİÇEKDENİZKÖPRÜYILDIZTURUNCU",
    )

    fun rackFor(state: WordSiegePracticeState, owner: Int): String =
        if (owner == 1) state.playerRack else state.botRack

    fun applyMove(
        state: WordSiegePracticeState,
        owner: Int,
        placements: Map<Int, Int>,
        horizontal: Boolean,
    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> {
        requireActiveTurn(state, owner)
        if (placements.size !in 1..7) fail("word_siege_invalid_placements")
        val rack = rackFor(state, owner)
        if (placements.keys.any { it !in 0..80 }) fail("word_siege_invalid_cell")
        if (placements.values.distinct().size != placements.size || placements.values.any { it !in rack.indices }) {
            fail("word_siege_invalid_rack_tile")
        }
        if (placements.keys.any { state.board[it].letter != null }) fail("word_siege_cell_occupied")

        val indices = placements.keys.sorted()
        val anchor = indices.first()
        if (indices.size > 1) {
            if (horizontal && indices.any { it / 9 != anchor / 9 }) fail("word_siege_not_in_one_row")
            if (!horizontal && indices.any { it % 9 != anchor % 9 }) fail("word_siege_not_in_one_column")
        }

        fun letterAt(index: Int): Char? = placements[index]?.let(rack::getOrNull) ?: state.board[index].letter?.firstOrNull()
        val mainCells = collectCells(anchor, if (horizontal) 1 else 9, ::letterAt)
        if (!indices.all(mainCells::contains)) fail("word_siege_gap_between_tiles")

        val hasBoardLetter = state.board.any { it.letter != null }
        if (!hasBoardLetter && 40 !in indices) fail("word_siege_first_word_must_cover_center")
        var connected = hasBoardLetter && mainCells.any { state.board[it].letter != null }
        val words = mutableListOf<String>()
        var primary: String? = null
        var score = 0
        val captured = linkedSetOf<Int>()
        var board = state.board.toMutableList()

        fun acceptWord(cells: List<Int>) {
            if (cells.size < 2) return
            val word = cells.joinToString("") { letterAt(it)?.toString().orEmpty() }
            if (!isPracticeWord(word)) fail("word_siege_invalid_word:$word")
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
            val cross = collectCells(index, if (horizontal) 9 else 1, ::letterAt)
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
            playerArea = playerArea,
            botArea = botArea,
            consecutivePasses = 0,
            moveCount = state.moveCount + 1,
            lastAction = "word:${primary.orEmpty()}",
        )
        val finished = if (nextBag.isEmpty() && nextRack.isEmpty()) finish(next, "rack_empty") else next
        return finished to WordSiegePracticeMove(placements, horizontal, primary.orEmpty(), words, score, captured.size)
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
        val nextBag = (state.bag.drop(draw.length) + returned).toList().sorted().joinToString("")
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

    fun bestBotMove(state: WordSiegePracticeState): WordSiegePracticeMove? {
        if (state.currentOwner != 2 || state.status != "playing") return null
        val rack = state.botRack
        var best: Pair<WordSiegePracticeState, WordSiegePracticeMove>? = null
        botWords.forEach { word ->
            listOf(true, false).forEach { horizontal ->
                (0..80).forEach startLoop@{ start ->
                    val placements = placementsForWord(state, rack, word, start, horizontal) ?: return@startLoop
                    val candidate = runCatching { applyMove(state, 2, placements, horizontal) }.getOrNull() ?: return@startLoop
                    if (best == null || candidate.second.wordScore + candidate.second.capturedCells * 2 > best!!.second.wordScore + best!!.second.capturedCells * 2) {
                        best = candidate
                    }
                }
            }
        }
        return best?.second
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
            if (previous !in 0..80 || (delta == 1 && previous / 9 != start / 9) || letterAt(previous) == null) break
            start = previous
        }
        val result = mutableListOf<Int>()
        var current = start
        while (current in 0..80 && !(delta == 1 && current / 9 != start / 9) && letterAt(current) != null) {
            result += current
            val next = current + delta
            if (next !in 0..80 || (delta == 1 && next / 9 != current / 9)) break
            current = next
        }
        return result
    }

    private fun finish(state: WordSiegePracticeState, reason: String, forcedWinner: Int? = null): WordSiegePracticeState {
        val winner = forcedWinner ?: when {
            state.playerWordScore + state.playerArea > state.botWordScore + state.botArea -> 1
            state.botWordScore + state.botArea > state.playerWordScore + state.playerArea -> 2
            state.playerArea > state.botArea -> 1
            state.botArea > state.playerArea -> 2
            else -> null
        }
        return state.copy(status = "finished", winnerOwner = winner, lastAction = reason)
    }

    private fun requireActiveTurn(state: WordSiegePracticeState, owner: Int) {
        if (state.status != "playing") fail("word_siege_not_playing")
        if (state.currentOwner != owner) fail("word_siege_not_your_turn")
    }

    private fun isPracticeWord(word: String): Boolean {
        val normalized = word.trim().uppercase(trLocale)
        return normalized.length in 2..9 && normalized.all { it in "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ" } &&
            normalized.firstOrNull() != 'Ğ' && normalized.lastOrNull() != 'Ğ'
    }

    private fun letterValue(letter: String): Int = when (letter) {
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

    private val botWords = listOf(
        "MASA", "KALEM", "KALE", "ELMA", "SİMA", "İSİM", "LİMAN", "MİNİ", "SİNEK",
        "KART", "KARE", "KASA", "SIR", "SIRA", "ARA", "ARI", "TARİH", "NAR", "NİSAN",
        "TERİM", "METİN", "SİLİ", "LİSTE", "LİMAN", "KİLİT", "KİRA", "KİRAZ", "KİTAP",
    )
}
