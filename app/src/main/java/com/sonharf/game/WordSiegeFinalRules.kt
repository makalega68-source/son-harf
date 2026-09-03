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

    fun cubeTransfer(cubesOwned: Int): Int =
        cubesOwned.coerceAtLeast(0) * CUBE_TRANSFER_POINTS

    /**
     * Word points are permanent. Territory contributes only the value of cubes the player owns now.
     * The opponent value is kept in the signature for source compatibility with existing callers.
     */
    fun netScore(
        wordScore: Int,
        earnedCubePoints: Int,
        @Suppress("UNUSED_PARAMETER") opponentEarnedCubePoints: Int,
    ): Int = wordScore + earnedCubePoints.coerceAtLeast(0)

    /**
     * Returns the points represented by cubes currently owned by [playerId].
     * Capturing a neutral cube by the rival does not reduce this value; only a rival capture of one
     * of the player's cubes does. Each currently owned cube is worth exactly two points.
     */
    fun earnedCubePoints(moves: Iterable<com.sonharf.game.data.WordSiegeMoveDto>, playerId: String?): Int {
        if (playerId == null) return 0
        var ownedCubes = 0
        moves.forEach { move ->
            if (move.playerId == playerId) {
                ownedCubes += move.neutralCaptured + move.opponentCaptured
            } else {
                ownedCubes -= move.opponentCaptured
            }
        }
        return cubeTransfer(ownedCubes)
    }
}
