package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto

/**
 * Deterministic territory layer for Word Siege.
 *
 * The 15x15 board is divided into 25 fixed 3x3 zones. Word validation and Scrabble-style
 * scoring stay outside this object; this object only owns territory/conquest semantics.
 */
internal object WordSiegeZoneRules {
    const val ZoneSize = 3
    const val ZonesPerAxis = WordSiegeBoardSpec.Size / ZoneSize
    const val ZoneCount = ZonesPerAxis * ZonesPerAxis
    const val NormalZonePoints = 2
    const val FortressZonePoints = 4
    const val ConquestMeterMax = 3
    const val TurnSeconds = 45
    const val UrgentTurnSeconds = 10

    /** Five fixed fortresses: four inner corners plus the center. */
    val FortressZoneIds: Set<Int> = setOf(
        zoneId(1, 1),
        zoneId(1, 3),
        zoneId(2, 2),
        zoneId(3, 1),
        zoneId(3, 3),
    )

    fun zoneId(zoneRow: Int, zoneColumn: Int): Int = zoneRow * ZonesPerAxis + zoneColumn

    fun zoneIdForIndex(index: Int): Int {
        require(WordSiegeBoardSpec.isValidIndex(index)) { "word_siege_invalid_cell" }
        return zoneId(
            WordSiegeBoardSpec.row(index) / ZoneSize,
            WordSiegeBoardSpec.column(index) / ZoneSize,
        )
    }

    fun zoneIndices(zoneId: Int): List<Int> {
        require(zoneId in 0 until ZoneCount) { "word_siege_invalid_zone" }
        val zoneRow = zoneId / ZonesPerAxis
        val zoneColumn = zoneId % ZonesPerAxis
        val firstRow = zoneRow * ZoneSize
        val firstColumn = zoneColumn * ZoneSize
        return buildList(ZoneSize * ZoneSize) {
            repeat(ZoneSize) { rowOffset ->
                repeat(ZoneSize) { columnOffset ->
                    add(WordSiegeBoardSpec.index(firstRow + rowOffset, firstColumn + columnOffset))
                }
            }
        }
    }

    fun isFortress(zoneId: Int): Boolean = zoneId in FortressZoneIds

    fun zoneValue(zoneId: Int): Int = if (isFortress(zoneId)) FortressZonePoints else NormalZonePoints

    /**
     * A move touches the zone containing each newly placed tile and any orthogonally adjacent zone.
     * This implements the "inside or next to a zone" rule without diagonal spillover.
     */
    fun touchedZoneIds(placementIndices: Collection<Int>): Set<Int> = buildSet {
        placementIndices.forEach { index ->
            if (!WordSiegeBoardSpec.isValidIndex(index)) return@forEach
            add(zoneIdForIndex(index))
            val row = WordSiegeBoardSpec.row(index)
            val column = WordSiegeBoardSpec.column(index)
            if (row > 0) add(zoneIdForIndex(WordSiegeBoardSpec.index(row - 1, column)))
            if (row < WordSiegeBoardSpec.Size - 1) add(zoneIdForIndex(WordSiegeBoardSpec.index(row + 1, column)))
            if (column > 0) add(zoneIdForIndex(WordSiegeBoardSpec.index(row, column - 1)))
            if (column < WordSiegeBoardSpec.Size - 1) add(zoneIdForIndex(WordSiegeBoardSpec.index(row, column + 1)))
        }
    }

    /** A zone is owned only when all nine cells agree. Mixed legacy territory is neutral. */
    fun zoneOwner(board: List<WordSiegeCellDto>, zoneId: Int): Int {
        if (board.size != WordSiegeBoardSpec.CellCount) return 0
        val owners = zoneIndices(zoneId).map { board[it].owner }.distinct()
        return owners.singleOrNull()?.takeIf { it in 1..2 } ?: 0
    }

    fun ownedZoneCount(board: List<WordSiegeCellDto>, owner: Int): Int =
        (0 until ZoneCount).count { zoneOwner(board, it) == owner }

    fun zoneScore(board: List<WordSiegeCellDto>, owner: Int): Int =
        (0 until ZoneCount).sumOf { zoneId ->
            if (zoneOwner(board, zoneId) == owner) zoneValue(zoneId) else 0
        }

    fun previewZoneGain(board: List<WordSiegeCellDto>, owner: Int, placementIndices: Collection<Int>): Int =
        touchedZoneIds(placementIndices).sumOf { zoneId ->
            if (zoneOwner(board, zoneId) != owner) zoneValue(zoneId) else 0
        }

    data class ClaimResult(
        val board: List<WordSiegeCellDto>,
        val flippedZoneIds: Set<Int>,
        val neutralZones: Int,
        val opponentZones: Int,
    )

    /**
     * Applies whole-zone ownership after the word engine has accepted and placed the letters.
     * Existing letters/bonuses are preserved; only owner changes.
     */
    fun claimZones(
        beforeBoard: List<WordSiegeCellDto>,
        boardWithPlacedLetters: List<WordSiegeCellDto>,
        owner: Int,
        placementIndices: Collection<Int>,
    ): ClaimResult {
        require(owner in 1..2) { "word_siege_invalid_owner" }
        require(beforeBoard.size == WordSiegeBoardSpec.CellCount) { "word_siege_invalid_board" }
        require(boardWithPlacedLetters.size == WordSiegeBoardSpec.CellCount) { "word_siege_invalid_board" }

        val touched = touchedZoneIds(placementIndices)
        val flipped = linkedSetOf<Int>()
        var neutral = 0
        var opponent = 0
        val next = boardWithPlacedLetters.toMutableList()

        touched.sorted().forEach { zoneId ->
            val previousOwner = zoneOwner(beforeBoard, zoneId)
            if (previousOwner == owner) return@forEach
            if (previousOwner == 0) neutral += 1 else opponent += 1
            zoneIndices(zoneId).forEach { index ->
                next[index] = next[index].copy(owner = owner)
            }
            flipped += zoneId
        }

        return ClaimResult(
            board = next,
            flippedZoneIds = flipped,
            neutralZones = neutral,
            opponentZones = opponent,
        )
    }

    data class ConquestProgress(
        val meter: Int,
        val onslaughtActive: Boolean,
        val triggered: Boolean,
    )

    /** One successful territory-taking move advances one step, matching the supplied SiegeEngine. */
    fun advanceConquestMeter(currentMeter: Int, flippedAnyZone: Boolean): ConquestProgress {
        if (!flippedAnyZone) {
            return ConquestProgress(currentMeter.coerceIn(0, ConquestMeterMax - 1), false, false)
        }
        val next = currentMeter.coerceIn(0, ConquestMeterMax - 1) + 1
        return if (next >= ConquestMeterMax) {
            ConquestProgress(meter = 0, onslaughtActive = true, triggered = true)
        } else {
            ConquestProgress(meter = next, onslaughtActive = false, triggered = false)
        }
    }

    fun wordScore(rawWordScore: Int, onslaughtActive: Boolean): Int =
        rawWordScore.coerceAtLeast(0) * if (onslaughtActive) 2 else 1

    fun totalScore(permanentWordScore: Int, currentZoneScore: Int): Int =
        permanentWordScore.coerceAtLeast(0) + currentZoneScore.coerceAtLeast(0)
}
