package com.sonharf.game.data

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cosmetic-only cache for the already-fetched Word Siege move DTOs.
 *
 * VFX reads this cache by move id so presentation can use authoritative move metadata
 * (word score and capture counts) without adding extra network calls or changing gameplay state.
 */
internal object WordSiegeVfxMoveRegistry {
    private val moves = ConcurrentHashMap<Long, WordSiegeMoveDto>()
    private val latestSeenByGame = ConcurrentHashMap<String, Long>()
    private val _comboEvents = MutableSharedFlow<WordSiegeMoveDto>(extraBufferCapacity = 8)
    val comboEvents = _comboEvents.asSharedFlow()

    fun remember(items: List<WordSiegeMoveDto>) {
        items.groupBy(WordSiegeMoveDto::gameId).forEach { (gameId, gameMoves) ->
            val previousMax = latestSeenByGame[gameId]
            gameMoves.forEach { move -> moves[move.id] = move }
            val currentMax = gameMoves.maxOfOrNull(WordSiegeMoveDto::id) ?: return@forEach

            // First observation only establishes the baseline so reopening a match never replays
            // old combo VFX. Only later authoritative moves can emit a fresh combo signal.
            if (previousMax != null) {
                gameMoves.asSequence()
                    .filter { move -> move.id > previousMax && move.formedWords.size >= 2 }
                    .sortedBy(WordSiegeMoveDto::id)
                    .forEach { move -> _comboEvents.tryEmit(move) }
            }
            latestSeenByGame[gameId] = maxOf(previousMax ?: currentMax, currentMax)
        }
    }

    fun get(moveId: Long): WordSiegeMoveDto? = moves[moveId]
}
