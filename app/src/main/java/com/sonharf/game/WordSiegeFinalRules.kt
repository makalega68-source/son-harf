package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto

/** Final Word Siege rules shared by online UI and local BOT practice. */
internal enum class WordSiegeOrientation { HORIZONTAL, VERTICAL }

internal object WordSiegeFinalRules {
    const val CUBE_TRANSFER_POINTS: Int = 2

    fun detectOrientation(
        board: List<WordSiegeCellDto>,
        placementIndices: Collection<Int>,
    ): WordSiegeOrientation {
        val indices = placementIndices.distinct().sorted()
        require(indices.isNotEmpty()) { "word_siege_invalid_placements" }
        require(indices.all { it in 0..80 }) { "word_siege_invalid_cell" }

        if (indices.size > 1) {
            val anchor = indices.first()
            val sameRow = indices.all { it / 9 == anchor / 9 }
            val sameColumn = indices.all { it % 9 == anchor % 9 }
            require(sameRow || sameColumn) { "word_siege_not_in_one_line" }
            return if (sameRow) WordSiegeOrientation.HORIZONTAL else WordSiegeOrientation.VERTICAL
        }

        val index = indices.single()
        val row = index / 9
        val col = index % 9
        fun occupied(candidate: Int): Boolean = board.getOrNull(candidate)?.letter != null
        val horizontalNeighbor =
            (col > 0 && occupied(index - 1)) || (col < 8 && occupied(index + 1))
        val verticalNeighbor =
            (row > 0 && occupied(index - 9)) || (row < 8 && occupied(index + 9))

        return when {
            verticalNeighbor && !horizontalNeighbor -> WordSiegeOrientation.VERTICAL
            else -> WordSiegeOrientation.HORIZONTAL
        }
    }

    fun cubeTransfer(cubesGained: Int): Int =
        cubesGained.coerceAtLeast(0) * CUBE_TRANSFER_POINTS

    fun netScore(wordScore: Int, earnedCubePoints: Int, opponentEarnedCubePoints: Int): Int =
        wordScore + earnedCubePoints - opponentEarnedCubePoints

    fun earnedCubePoints(moves: Iterable<com.sonharf.game.data.WordSiegeMoveDto>, playerId: String?): Int {
        if (playerId == null) return 0
        return moves.asSequence()
            .filter { it.playerId == playerId }
            .sumOf { cubeTransfer(it.neutralCaptured + it.opponentCaptured) }
    }
}
