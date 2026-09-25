package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto

/** Final Kelime Kuşatması rules shared by online UI and local BOT practice. */
internal enum class WordSiegeOrientation { HORIZONTAL, VERTICAL }

internal object WordSiegeFinalRules {
    const val CUBE_TRANSFER_POINTS: Int = 2
    const val OPPONENT_CAPTURE_LOSS_POINTS: Int = 1

    fun detectOrientation(
        board: List<WordSiegeCellDto>,
        placementIndices: Collection<Int>,
    ): WordSiegeOrientation {
        val indices = placementIndices.distinct().sorted()
        require(indices.isNotEmpty()) { "word_siege_invalid_placements" }
        require(indices.all(WordSiegeBoardSpec::isValidIndex)) { "word_siege_invalid_cell" }

        if (indices.size > 1) {
            val anchor = indices.first()
            val sameRow = indices.all { WordSiegeBoardSpec.row(it) == WordSiegeBoardSpec.row(anchor) }
            val sameColumn = indices.all { WordSiegeBoardSpec.column(it) == WordSiegeBoardSpec.column(anchor) }
            require(sameRow || sameColumn) { "word_siege_not_in_one_line" }
            return if (sameRow) WordSiegeOrientation.HORIZONTAL else WordSiegeOrientation.VERTICAL
        }

        val index = indices.single()
        val row = WordSiegeBoardSpec.row(index)
        val column = WordSiegeBoardSpec.column(index)
        fun occupied(candidate: Int): Boolean = board.getOrNull(candidate)?.letter != null
        val horizontalNeighbor =
            (column > 0 && occupied(index - WordSiegeBoardSpec.HorizontalDelta)) ||
                (column < WordSiegeBoardSpec.Size - 1 && occupied(index + WordSiegeBoardSpec.HorizontalDelta))
        val verticalNeighbor =
            (row > 0 && occupied(index - WordSiegeBoardSpec.VerticalDelta)) ||
                (row < WordSiegeBoardSpec.Size - 1 && occupied(index + WordSiegeBoardSpec.VerticalDelta))

        return when {
            verticalNeighbor && !horizontalNeighbor -> WordSiegeOrientation.VERTICAL
            else -> WordSiegeOrientation.HORIZONTAL
        }
    }

    fun cubeTransfer(cubesOwned: Int): Int =
        cubesOwned.coerceAtLeast(0) * CUBE_TRANSFER_POINTS

    fun currentTerritoryScore(wordScore: Int, ownedCubes: Int): Int =
        wordScore + cubeTransfer(ownedCubes)

    fun opponentCaptureLoss(cubesLost: Int): Int =
        cubesLost.coerceAtLeast(0) * OPPONENT_CAPTURE_LOSS_POINTS

    fun scoreWithTerritoryLedger(wordScore: Int, territoryScore: Int): Int =
        wordScore.coerceAtLeast(0) + territoryScore.coerceAtLeast(0)

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
        var territoryScore = 0
        moves.forEach { move ->
            if (move.playerId == playerId) {
                territoryScore += cubeTransfer(move.neutralCaptured + move.opponentCaptured)
            } else {
                territoryScore -= opponentCaptureLoss(move.opponentCaptured)
            }
        }
        return territoryScore.coerceAtLeast(0)
    }
}
