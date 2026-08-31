package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto

internal enum class WordSiegeDirection { HORIZONTAL, VERTICAL }

internal fun detectWordSiegeDirection(
    board: List<WordSiegeCellDto>,
    placementIndices: Collection<Int>,
): WordSiegeDirection? {
    val indices = placementIndices.distinct().sorted()
    if (indices.isEmpty() || indices.any { it !in 0..80 }) return null

    if (indices.size > 1) {
        val anchor = indices.first()
        val sameRow = indices.all { it / 9 == anchor / 9 }
        val sameColumn = indices.all { it % 9 == anchor % 9 }
        return when {
            sameRow -> WordSiegeDirection.HORIZONTAL
            sameColumn -> WordSiegeDirection.VERTICAL
            else -> null
        }
    }

    val index = indices.single()
    val horizontalSpan = existingSpan(board, index, horizontal = true)
    val verticalSpan = existingSpan(board, index, horizontal = false)
    return when {
        horizontalSpan == 0 && verticalSpan == 0 -> null
        horizontalSpan >= verticalSpan -> WordSiegeDirection.HORIZONTAL
        else -> WordSiegeDirection.VERTICAL
    }
}

private fun existingSpan(board: List<WordSiegeCellDto>, index: Int, horizontal: Boolean): Int {
    val row = index / 9
    val column = index % 9
    var count = 0

    fun occupied(target: Int): Boolean = board.getOrNull(target)?.letter != null

    if (horizontal) {
        var c = column - 1
        while (c >= 0 && occupied(row * 9 + c)) { count++; c-- }
        c = column + 1
        while (c <= 8 && occupied(row * 9 + c)) { count++; c++ }
    } else {
        var r = row - 1
        while (r >= 0 && occupied(r * 9 + column)) { count++; r-- }
        r = row + 1
        while (r <= 8 && occupied(r * 9 + column)) { count++; r++ }
    }
    return count
}
