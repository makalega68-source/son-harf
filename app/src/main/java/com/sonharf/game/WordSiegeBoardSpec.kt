package com.sonharf.game

import kotlin.random.Random

internal object WordSiegeBoardSpec {
    // Keep the 15x15 storage contract for existing online matches. The playable identity is
    // differentiated through a custom siege-map bonus topology instead of the classic word-board layout.
    const val Size = 15
    const val CellCount = Size * Size
    const val LastIndex = CellCount - 1
    const val CenterRow = Size / 2
    const val CenterColumn = Size / 2
    const val CenterIndex = CenterRow * Size + CenterColumn
    const val HorizontalDelta = 1
    const val VerticalDelta = Size

    const val CenterBonus = "4K"
    const val StarBonus = "3Y"
    const val StarBonusPoints = 25

    fun isValidIndex(index: Int): Boolean = index in 0 until CellCount
    fun row(index: Int): Int = index / Size
    fun column(index: Int): Int = index % Size
    fun index(row: Int, column: Int): Int = row * Size + column

    fun bonusAt(index: Int): String? {
        if (!isValidIndex(index)) return null
        if (index == CenterIndex) return CenterBonus
        val row = row(index)
        val column = column(index)
        return when {
            row to column in SiegeMajorZones -> "3K"
            row to column in SiegeWatchZones -> "3H"
            row to column in SiegeFortZones -> "2K"
            row to column in SiegeTacticalZones -> "2H"
            else -> null
        }
    }

    /**
     * Returns the bonus layout for a newly-created match.
     * The layout is a Kelime Kuşatması-specific territory topology rather than the
     * conventional corner/diagonal word-board pattern. Existing matches remain safe because
     * their persisted cells continue to carry their own bonus values.
     */
    fun newGameBonuses(random: Random = Random.Default): List<String?> {
        val bonuses = MutableList<String?>(CellCount) { index -> bonusAt(index) }
        val candidates = (0 until CellCount).filter { index ->
            index != CenterIndex && bonuses[index] == null
        }
        if (candidates.isNotEmpty()) {
            bonuses[candidates[random.nextInt(candidates.size)]] = StarBonus
        }
        return bonuses
    }

    fun displayBonusLabel(bonus: String?): String = when (bonus) {
        StarBonus -> "★★★"
        else -> bonus.orEmpty()
    }

    fun canonicalBag(language: String): String = if (language.lowercase() == "en") {
        buildString {
            append('E'.toString().repeat(12)); append('A'.toString().repeat(9)); append('I'.toString().repeat(9)); append('O'.toString().repeat(8))
            append('N'.toString().repeat(6)); append('R'.toString().repeat(6)); append('T'.toString().repeat(6)); append('L'.toString().repeat(4))
            append('S'.toString().repeat(4)); append('U'.toString().repeat(4)); append('D'.toString().repeat(4)); append('G'.toString().repeat(3))
            append('B'.toString().repeat(2)); append('C'.toString().repeat(2)); append('M'.toString().repeat(2)); append('P'.toString().repeat(2))
            append('F'.toString().repeat(2)); append('H'.toString().repeat(2)); append('V'.toString().repeat(2)); append('W'.toString().repeat(2))
            append('Y'.toString().repeat(2)); append("KJXQZ")
        }
    } else {
        buildString {
            append('A'.toString().repeat(12)); append('B'.toString().repeat(2)); append('C'.toString().repeat(2)); append('Ç'.toString().repeat(2))
            append('D'.toString().repeat(2)); append('E'.toString().repeat(8)); append("FGĞH"); append('I'.toString().repeat(4))
            append('İ'.toString().repeat(7)); append('J'); append('K'.toString().repeat(7)); append('L'.toString().repeat(7))
            append('M'.toString().repeat(4)); append('N'.toString().repeat(5)); append('O'.toString().repeat(3)); append("ÖP")
            append('R'.toString().repeat(6)); append('S'.toString().repeat(3)); append('Ş'.toString().repeat(2)); append('T'.toString().repeat(5))
            append('U'.toString().repeat(3)); append('Ü'.toString().repeat(2)); append('V'); append('Y'.toString().repeat(2)); append('Z'.toString().repeat(2))
        }
    }

    fun shuffledBag(language: String, random: Random = Random.Default): String =
        canonicalBag(language).toMutableList().apply { shuffle(random) }.joinToString("")

    /** High-value siege lanes: deliberately avoid the classic corner and mid-edge pattern. */
    private val SiegeMajorZones = setOf(
        1 to 7, 7 to 1, 7 to 13, 13 to 7,
    )

    /** Letter-focused watch points placed on an outer tactical ring. */
    private val SiegeWatchZones = setOf(
        2 to 4, 2 to 10, 4 to 2, 4 to 12,
        10 to 2, 10 to 12, 12 to 4, 12 to 10,
    )

    /** Word-focused fort positions surrounding the central crown zone. */
    private val SiegeFortZones = setOf(
        3 to 6, 3 to 8, 6 to 3, 6 to 11,
        8 to 3, 8 to 11, 11 to 6, 11 to 8,
    )

    /** Lower-value tactical positions that create multiple attack routes rather than diagonals. */
    private val SiegeTacticalZones = setOf(
        1 to 3, 1 to 11, 3 to 1, 3 to 13,
        11 to 1, 11 to 13, 13 to 3, 13 to 11,
        5 to 5, 5 to 9, 9 to 5, 9 to 9,
    )
}
