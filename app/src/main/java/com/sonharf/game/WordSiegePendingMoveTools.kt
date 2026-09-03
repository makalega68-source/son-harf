package com.sonharf.game

/** Pure helpers for Kuşatma pre-submit controls. They never mutate server/game state. */
internal fun wordSiegeUndoPendingPlacement(
    placements: Map<Int, Int>,
    boardIndex: Int? = null,
): Map<Int, Int> {
    if (placements.isEmpty()) return placements
    val key = boardIndex?.takeIf(placements::containsKey) ?: placements.keys.last()
    return placements - key
}

/**
 * Returns a visual rack order only. Original rack indices are retained so backend placement
 * references remain correct after shuffling.
 */
internal fun wordSiegeShuffledRackIndices(
    rackLength: Int,
    seed: Int,
): List<Int> {
    val indices = (0 until rackLength.coerceAtLeast(0)).toMutableList()
    if (indices.size < 2) return indices
    var state = seed
    for (i in indices.lastIndex downTo 1) {
        state = state * 1664525 + 1013904223
        val j = ((state.toLong() and 0x7fffffffL) % (i + 1)).toInt()
        val tmp = indices[i]
        indices[i] = indices[j]
        indices[j] = tmp
    }
    return indices
}
