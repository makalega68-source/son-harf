package com.sonharf.game

internal const val WORD_SIEGE_CAPTURE_POINTS_PER_CUBE = 2

internal data class WordSiegeCaptureBatch(
    val updateKey: String,
    val owner: Int,
    val indices: List<Int>,
) {
    init {
        require(owner == 1 || owner == 2) { "capture owner must be 1 or 2" }
    }

    val points: Int
        get() = indices.size * WORD_SIEGE_CAPTURE_POINTS_PER_CUBE
}

internal fun wordSiegeCaptureBatch(
    updateKey: String,
    previousOwners: List<Int>,
    currentOwners: List<Int>,
    capturingOwner: Int,
): WordSiegeCaptureBatch? {
    if (updateKey.isBlank() || capturingOwner !in 1..2) return null
    if (previousOwners.size != currentOwners.size) return null

    val captured = currentOwners.indices
        .asSequence()
        .filter(WordSiegeBoardSpec::isValidIndex)
        .filter { index ->
            currentOwners[index] == capturingOwner && previousOwners[index] != capturingOwner
        }
        .toList()

    return captured.takeIf { it.isNotEmpty() }?.let {
        WordSiegeCaptureBatch(updateKey = updateKey, owner = capturingOwner, indices = it)
    }
}

internal fun wordSiegeDisplayedScore(actualScore: Int, pendingCapturePoints: Int): Int =
    (actualScore - pendingCapturePoints.coerceAtLeast(0)).coerceAtLeast(0)

/**
 * Keeps the last consumed server board as a visual baseline.
 * The initial update is consumed by construction, so open/reconnect never replay old cubes.
 */
internal class WordSiegeCaptureTracker(
    initialUpdateKey: String?,
    initialOwners: List<Int>,
) {
    private var baselineOwners: List<Int> = initialOwners.toList()
    private val consumedUpdateKeys = mutableSetOf<String>().apply {
        initialUpdateKey?.takeIf(String::isNotBlank)?.let(::add)
    }

    fun preview(
        updateKey: String?,
        currentOwners: List<Int>,
        capturingOwner: Int?,
        expectedCaptured: Int,
    ): WordSiegeCaptureBatch? {
        val key = updateKey?.takeIf(String::isNotBlank) ?: return null
        val owner = capturingOwner?.takeIf { it in 1..2 } ?: return null
        if (expectedCaptured <= 0 || key in consumedUpdateKeys) return null

        val batch = wordSiegeCaptureBatch(
            updateKey = key,
            previousOwners = baselineOwners,
            currentOwners = currentOwners,
            capturingOwner = owner,
        ) ?: return null

        // Move metadata and board row can arrive on different realtime ticks.
        if (batch.indices.size < expectedCaptured) return null
        return batch
    }

    fun consume(batch: WordSiegeCaptureBatch, currentOwners: List<Int>) {
        if (currentOwners.size != baselineOwners.size) return
        consumedUpdateKeys += batch.updateKey
        baselineOwners = currentOwners.toList()
    }
}
