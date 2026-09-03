package com.sonharf.game

import kotlin.random.Random

internal object WordSiegeBoardSpec {
    const val Size = 15
    const val CellCount = Size * Size
    const val LastIndex = CellCount - 1
    const val CenterRow = Size / 2
    const val CenterColumn = Size / 2
    const val CenterIndex = CenterRow * Size + CenterColumn
    const val HorizontalDelta = 1
    const val VerticalDelta = Size

    fun isValidIndex(index: Int): Boolean = index in 0 until CellCount
    fun row(index: Int): Int = index / Size
    fun column(index: Int): Int = index % Size
    fun index(row: Int, column: Int): Int = row * Size + column

    fun bonusAt(index: Int): String? {
        if (!isValidIndex(index)) return null
        val row = row(index)
        val column = column(index)
        return when {
            row to column in TripleWord -> "3K"
            row to column in TripleLetter -> "3H"
            row to column in DoubleWord -> "2K"
            row to column in DoubleLetter -> "2H"
            else -> null
        }
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

    private val TripleWord = setOf(
        0 to 0, 0 to 7, 0 to 14,
        7 to 0, 7 to 14,
        14 to 0, 14 to 7, 14 to 14,
    )

    private val TripleLetter = setOf(
        1 to 5, 1 to 9, 5 to 1, 5 to 5, 5 to 9, 5 to 13,
        9 to 1, 9 to 5, 9 to 9, 9 to 13, 13 to 5, 13 to 9,
    )

    private val DoubleWord = setOf(
        1 to 1, 1 to 13, 2 to 2, 2 to 12, 3 to 3, 3 to 11, 4 to 4, 4 to 10,
        7 to 7,
        10 to 4, 10 to 10, 11 to 3, 11 to 11, 12 to 2, 12 to 12, 13 to 1, 13 to 13,
    )

    private val DoubleLetter = setOf(
        0 to 3, 0 to 11, 3 to 0, 11 to 0, 14 to 3, 14 to 11, 3 to 14, 11 to 14,
        2 to 6, 2 to 8, 6 to 2, 8 to 2, 12 to 6, 12 to 8, 6 to 12, 8 to 12,
        3 to 7, 7 to 3, 7 to 11, 11 to 7,
        6 to 6, 6 to 8, 8 to 6, 8 to 8,
    )
}