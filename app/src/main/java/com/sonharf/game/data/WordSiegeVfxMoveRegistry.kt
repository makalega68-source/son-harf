package com.sonharf.game.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Cosmetic-only cache for the already-fetched Word Siege move DTOs.
 *
 * VFX reads this cache by move id so presentation can use authoritative move metadata
 * (word score and capture counts) without adding extra network calls or changing gameplay state.
 */
internal object WordSiegeVfxMoveRegistry {
    private val moves = ConcurrentHashMap<Long, WordSiegeMoveDto>()

    fun remember(items: List<WordSiegeMoveDto>) {
        items.forEach { move -> moves[move.id] = move }
    }

    fun get(moveId: Long): WordSiegeMoveDto? = moves[moveId]
}
