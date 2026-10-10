package com.sonharf.game

internal enum class AtelierMatchPhase { WARMUP, STRATEGY, FINAL }

internal fun atelierMatchPhase(seconds: Int, remaining: Int): AtelierMatchPhase {
    val safeDuration = seconds.coerceAtLeast(1)
    val elapsed = (safeDuration - remaining.coerceIn(0, safeDuration))
    return when {
        elapsed * 3 < safeDuration -> AtelierMatchPhase.WARMUP
        elapsed * 3 < safeDuration * 2 -> AtelierMatchPhase.STRATEGY
        else -> AtelierMatchPhase.FINAL
    }
}

internal fun atelierRemainingSeconds(deadlineMs: Long, nowMs: Long): Int =
    ((deadlineMs - nowMs).coerceAtLeast(0L).let { (it + 999L) / 1000L }).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
