package com.sonharf.game

import java.util.Locale
import kotlin.random.Random

/*
 * Kelime Atölyesi (Word Workshop): a 60-second round with a 7-letter pool and three short tasks.
 * The engine is plain Kotlin so every rule is unit-tested without a device.
 *
 * Rules the engine guarantees:
 *  - A pool and its tasks are generated from real dictionary words: every open task always has
 *    at least one unplayed word that can be built from the current pool.
 *  - One accepted word completes every open task it satisfies at once (e.g. a 4-letter word
 *    containing "A" can finish both "4 harfli kelime kur" and "A harfini kullan").
 *  - A word scores only once per round; invalid or repeated words change nothing.
 *  - Letters used by an accepted word are replaced in place, and the refill keeps every
 *    remaining task solvable.
 */

internal enum class AtelierTaskKind { LENGTH, LETTER }

internal data class AtelierTask(
    val kind: AtelierTaskKind,
    val length: Int = 0,
    val letter: Char = ' ',
    val done: Boolean = false,
) {
    fun matches(word: String): Boolean = when (kind) {
        AtelierTaskKind.LENGTH -> word.length == length
        AtelierTaskKind.LETTER -> letter in word
    }
}

internal data class AtelierTile(val id: Long, val letter: Char)

internal enum class AtelierReject { TOO_SHORT, ALREADY_USED, NOT_IN_DICTIONARY, ROUND_OVER }

internal data class AtelierState(
    val pool: List<AtelierTile>,
    val tasks: List<AtelierTask>,
    val picked: List<Long> = emptyList(),
    val score: Int = 0,
    val words: List<String> = emptyList(),
    val nextTileId: Long,
    val over: Boolean = false,
) {
    /** The word currently laid in the slot, in the order the tiles were picked. */
    val word: String get() = picked.mapNotNull { id -> pool.firstOrNull { it.id == id }?.letter }.joinToString("")
    val allTasksDone: Boolean get() = tasks.all { it.done }
    val completedTasks: Int get() = tasks.count { it.done }

    fun pick(tileId: Long): AtelierState =
        if (over || tileId in picked || pool.none { it.id == tileId }) this else copy(picked = picked + tileId)

    fun unpickAt(slotIndex: Int): AtelierState =
        if (over || slotIndex !in picked.indices) this else copy(picked = picked.filterIndexed { i, _ -> i != slotIndex })

    fun clearPicks(): AtelierState = if (picked.isEmpty()) this else copy(picked = emptyList())
}

internal data class AtelierSubmit(
    val state: AtelierState,
    val reject: AtelierReject? = null,
    val word: String = "",
    val wordPoints: Int = 0,
    val taskPoints: Int = 0,
    val completed: List<Int> = emptyList(),
    /** Tile ids that were replaced by fresh letters (for the refill animation). */
    val freshTiles: Set<Long> = emptySet(),
) {
    val gained: Int get() = wordPoints + taskPoints
}

internal class KelimeAtolyesiEngine(
    dictionary: Set<String>,
    language: String,
    private val random: Random = Random.Default,
) {
    val language: String = if (language.lowercase(Locale.ROOT) == "en") "en" else "tr"
    private val alphabet = if (this.language == "en") EN_ALPHABET else TR_ALPHABET
    private val words: Set<String> =
        dictionary.filterTo(hashSetOf()) { it.length in MIN_WORD..POOL_SIZE && it.all { c -> c in alphabet } }
    private val index: List<Pair<String, IntArray>> = words.map { it to counts(it) }
    private val seeds: List<String> = index.map { it.first }.filter { it.length == POOL_SIZE }
        .ifEmpty { index.map { it.first }.filter { it.length >= POOL_SIZE - 2 } }
    private val weights: List<Pair<Char, Double>> =
        (if (this.language == "en") EN_FREQUENCY else TR_FREQUENCY).filter { it.first in alphabet }

    val playable: Boolean get() = seeds.isNotEmpty()

    fun isWord(word: String): Boolean = word in words

    /** A fresh round: pool and tasks built from real words, all three tasks solvable. */
    fun newRound(): AtelierState {
        repeat(ROUND_TRIES) {
            val letters = seedLetters() ?: return@repeat
            val formable = formable(letters, emptySet())
            if (formable.size < MIN_FORMABLE) return@repeat
            val tasks = pickTasks(letters, formable) ?: return@repeat
            val pool = letters.mapIndexed { i, c -> AtelierTile(i.toLong() + 1, c) }
            return AtelierState(pool = pool, tasks = tasks, nextTileId = pool.size.toLong() + 1)
        }
        throw IllegalStateException("atelier_round_unavailable")
    }

    fun submit(state: AtelierState): AtelierSubmit {
        if (state.over) return AtelierSubmit(state, AtelierReject.ROUND_OVER)
        val word = state.word
        if (word.length < MIN_WORD) return AtelierSubmit(state, AtelierReject.TOO_SHORT, word)
        if (word in state.words) return AtelierSubmit(state, AtelierReject.ALREADY_USED, word)
        if (word !in words) return AtelierSubmit(state, AtelierReject.NOT_IN_DICTIONARY, word)

        val completed = state.tasks.indices.filter { !state.tasks[it].done && state.tasks[it].matches(word) }
        val tasks = state.tasks.mapIndexed { i, task -> if (i in completed) task.copy(done = true) else task }
        val used = state.words + word
        val wordPoints = word.length * WORD_LETTER_POINTS
        val taskPoints = completed.size * TASK_POINTS

        val keptTiles = state.pool.filter { it.id !in state.picked }
        val open = tasks.filter { !it.done }
        var nextId = state.nextTileId
        val refill = refillLetters(keptTiles.map { it.letter }, state.picked.size, open, used.toSet())
        val pool: List<AtelierTile>
        val fresh = mutableSetOf<Long>()
        if (refill != null) {
            val queue = ArrayDeque(refill)
            pool = state.pool.map { tile ->
                if (tile.id in state.picked) AtelierTile(nextId++, queue.removeFirst()).also { fresh += it.id } else tile
            }
        } else {
            // The kept letters cannot carry the open tasks any more: lay a whole new pool for them.
            val letters = poolFor(open, used.toSet()) ?: seedLetters().orEmpty()
            pool = letters.map { AtelierTile(nextId++, it).also { tile -> fresh += tile.id } }
        }
        val next = state.copy(
            pool = pool,
            tasks = tasks,
            picked = emptyList(),
            score = state.score + wordPoints + taskPoints,
            words = used,
            nextTileId = nextId,
        )
        return AtelierSubmit(next, null, word, wordPoints, taskPoints, completed, fresh)
    }

    /** Ends the round (time up or all tasks done); remaining seconds become a bonus when all tasks are done. */
    fun finish(state: AtelierState, secondsLeft: Int): AtelierState {
        if (state.over) return state
        val bonus = if (state.allTasksDone) secondsLeft.coerceAtLeast(0) * TIME_BONUS_PER_SECOND else 0
        return state.copy(over = true, picked = emptyList(), score = state.score + bonus)
    }

    /** Words that can be built from [letters] and have not been played yet. */
    fun formable(letters: List<Char>, used: Set<String>): List<String> {
        val pool = counts(letters.joinToString(""))
        return index.filter { (word, wc) -> word !in used && fits(wc, pool) }.map { it.first }
    }

    /** True when every open task has at least one unplayed word buildable from [letters]. */
    fun solvable(letters: List<Char>, tasks: List<AtelierTask>, used: Set<String>): Boolean {
        val formable = formable(letters, used)
        if (formable.isEmpty()) return false
        return tasks.filter { !it.done }.all { task -> formable.any { task.matches(it) } }
    }

    private fun pickTasks(letters: List<Char>, formable: List<String>): List<AtelierTask>? {
        val byLength = formable.groupBy { it.length }
        val shortLengths = (MIN_WORD..4).filter { (byLength[it]?.size ?: 0) >= 2 }
        val shortLength = shortLengths.randomOrNull(random) ?: return null
        val longLength = (5..POOL_SIZE).filter { (byLength[it]?.size ?: 0) >= 1 }.randomOrNull(random) ?: return null
        val letter = letters.distinct().filter { c -> formable.count { c in it } >= 2 }.randomOrNull(random) ?: return null
        return listOf(
            AtelierTask(AtelierTaskKind.LENGTH, length = shortLength),
            AtelierTask(AtelierTaskKind.LETTER, letter = letter),
            AtelierTask(AtelierTaskKind.LENGTH, length = longLength),
        )
    }

    private fun seedLetters(): List<Char>? {
        val seed = seeds.randomOrNull(random) ?: return null
        val letters = seed.toMutableList()
        while (letters.size < POOL_SIZE) letters += weightedLetter()
        letters.shuffle(random)
        return letters
    }

    private fun refillLetters(kept: List<Char>, need: Int, open: List<AtelierTask>, used: Set<String>): List<Char>? {
        repeat(REFILL_TRIES) {
            val fresh = guidedLetters(kept, need, open, used)
            if (solvable(kept + fresh, open, used)) return fresh
        }
        return null
    }

    /** For each open task (random order) adds the letters one real word for it still needs, then fills by frequency. */
    private fun guidedLetters(kept: List<Char>, need: Int, open: List<AtelierTask>, used: Set<String>): List<Char> {
        val result = mutableListOf<Char>()
        for (task in open.shuffled(random)) {
            val have = counts((kept + result).joinToString(""))
            val room = need - result.size
            val candidates = index.filter { (word, wc) -> word !in used && task.matches(word) && missing(wc, have) <= room }
            val (_, wc) = candidates.randomOrNull(random) ?: continue
            for (i in wc.indices) repeat((wc[i] - have[i]).coerceAtLeast(0)) { result += alphabet[i] }
        }
        while (result.size < need) result += weightedLetter()
        result.shuffle(random)
        return result
    }

    private fun poolFor(open: List<AtelierTask>, used: Set<String>): List<Char>? {
        repeat(ROUND_TRIES) {
            val letters = seedLetters() ?: return null
            if (solvable(letters, open, used)) return letters
        }
        return null
    }

    private fun weightedLetter(): Char {
        val total = weights.sumOf { it.second }
        var roll = random.nextDouble() * total
        for ((letter, weight) in weights) {
            roll -= weight
            if (roll <= 0) return letter
        }
        return weights.last().first
    }

    private fun counts(text: String): IntArray {
        val result = IntArray(alphabet.length)
        for (c in text) {
            val i = alphabet.indexOf(c)
            if (i >= 0) result[i]++
        }
        return result
    }

    private fun fits(word: IntArray, pool: IntArray): Boolean {
        for (i in word.indices) if (word[i] > pool[i]) return false
        return true
    }

    private fun missing(word: IntArray, pool: IntArray): Int {
        var total = 0
        for (i in word.indices) total += (word[i] - pool[i]).coerceAtLeast(0)
        return total
    }

    companion object {
        const val POOL_SIZE = 7
        const val MIN_WORD = 3
        const val ROUND_SECONDS = 60
        const val WORD_LETTER_POINTS = 10
        const val TASK_POINTS = 50
        const val TIME_BONUS_PER_SECOND = 5
        private const val MIN_FORMABLE = 6
        private const val ROUND_TRIES = 600
        private const val REFILL_TRIES = 120
        const val TR_ALPHABET = "abcçdefgğhıijklmnoöprsştuüvyz"
        const val EN_ALPHABET = "abcdefghijklmnopqrstuvwxyz"

        // Approximate letter frequencies of running text, used for filler letters.
        private val TR_FREQUENCY = listOf(
            'a' to 11.9, 'e' to 8.9, 'i' to 8.6, 'n' to 7.2, 'r' to 6.9, 'l' to 5.9, 'ı' to 5.1, 'k' to 4.7,
            'd' to 4.7, 'm' to 3.7, 'y' to 3.3, 'u' to 3.2, 't' to 3.0, 's' to 3.0, 'b' to 2.8, 'o' to 2.5,
            'ü' to 1.9, 'ş' to 1.8, 'z' to 1.5, 'g' to 1.3, 'ç' to 1.2, 'h' to 1.2, 'ğ' to 1.1, 'v' to 1.0,
            'c' to 1.0, 'p' to 0.9, 'ö' to 0.8, 'f' to 0.4,
        )
        private val EN_FREQUENCY = listOf(
            'e' to 12.7, 't' to 9.1, 'a' to 8.2, 'o' to 7.5, 'i' to 7.0, 'n' to 6.7, 's' to 6.3, 'h' to 6.1,
            'r' to 6.0, 'd' to 4.3, 'l' to 4.0, 'c' to 2.8, 'u' to 2.8, 'm' to 2.4, 'w' to 2.4, 'f' to 2.2,
            'g' to 2.0, 'y' to 2.0, 'p' to 1.9, 'b' to 1.5, 'v' to 1.0, 'k' to 0.8,
        )

        /** Upper-case display with the right locale (Turkish i → İ, ı → I). */
        fun display(letter: Char, language: String): String =
            letter.toString().uppercase(if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR"))

        fun display(word: String, language: String): String =
            word.uppercase(if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR"))
    }
}
