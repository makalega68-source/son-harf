package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import java.time.Instant
import kotlin.math.ceil

/**
 * Client-side projection of the server-owned Son Harf turn state.
 * It never advances a turn; it only decides whether a server snapshot is newer
 * and exposes the phase the UI is allowed to render.
 */
internal enum class ClassicTurnPhase {
    WAITING,
    CONNECTING,
    MY_TURN,
    SUBMITTING,
    OPPONENT_TURN,
    RECONNECTING,
    FINISHED,
}

internal data class ClassicTurnToken(
    val matchId: String,
    val roundId: Int,
    val revision: Int,
    val playerId: String?,
    val serverEndsAt: String?,
) {
    val requestId: String = listOf(matchId, roundId, revision, playerId.orEmpty(), serverEndsAt.orEmpty()).joinToString(":")
}

internal fun classicTurnToken(room: GameRoomDto): ClassicTurnToken =
    ClassicTurnToken(
        matchId = room.id,
        roundId = room.roundNo,
        revision = room.validWordCount,
        playerId = room.currentPlayerId,
        serverEndsAt = room.turnDeadline,
    )

private fun deadlineEpochMs(value: String?): Long? =
    runCatching { value?.let { Instant.parse(it).toEpochMilli() } }.getOrNull()

/**
 * Existing backend schema has no standalone revision column. roundNo +
 * validWordCount + authoritative deadline forms the monotonic client revision.
 * A timeout keeps validWordCount stable but moves the deadline forward, while an
 * accepted word increments validWordCount. This is enough to reject late polling
 * responses without changing the server contract.
 */
internal fun classicShouldAcceptRoom(current: GameRoomDto?, next: GameRoomDto): Boolean {
    current ?: return true
    if (current.id != next.id) return false
    if (current.status in setOf("finished", "cancelled") && next.status !in setOf("finished", "cancelled")) return false
    if (next.status in setOf("finished", "cancelled")) return true
    if (next.roundNo < current.roundNo) return false
    if (next.roundNo > current.roundNo) return true
    if (next.validWordCount < current.validWordCount) return false
    if (next.validWordCount > current.validWordCount) return true

    val currentDeadline = deadlineEpochMs(current.turnDeadline)
    val nextDeadline = deadlineEpochMs(next.turnDeadline)
    if (currentDeadline != null && nextDeadline != null && nextDeadline < currentDeadline) return false
    if (currentDeadline != null && nextDeadline == null && next.status in setOf("playing", "final", "sudden_death")) return false

    // Exact duplicates are harmless; same-revision heartbeat/reconnect fields may
    // still legitimately change, so equal sequence snapshots remain acceptable.
    return true
}

internal fun classicTurnPhase(
    room: GameRoomDto,
    me: String?,
    submittingToken: ClassicTurnToken? = null,
): ClassicTurnPhase {
    if (room.status == "waiting") return ClassicTurnPhase.WAITING
    if (room.status in setOf("finished", "cancelled")) return ClassicTurnPhase.FINISHED
    if (room.disconnectedPlayerId == me && room.reconnectDeadline != null) return ClassicTurnPhase.RECONNECTING
    val currentPlayer = room.currentPlayerId ?: return ClassicTurnPhase.CONNECTING
    val live = room.status in setOf("playing", "final", "sudden_death")
    if (!live) return ClassicTurnPhase.CONNECTING
    val token = classicTurnToken(room)
    if (currentPlayer == me && submittingToken == token) return ClassicTurnPhase.SUBMITTING
    return if (currentPlayer == me) ClassicTurnPhase.MY_TURN else ClassicTurnPhase.OPPONENT_TURN
}

internal data class ClassicDeadlineAnchor(
    val deadlineEpochMs: Long,
    val wallEpochMsAtAnchor: Long,
    val elapsedRealtimeMsAtAnchor: Long,
)

internal fun classicDeadlineAnchor(
    serverEndsAt: String?,
    wallEpochMsNow: Long,
    elapsedRealtimeMsNow: Long,
): ClassicDeadlineAnchor? {
    val deadline = deadlineEpochMs(serverEndsAt) ?: return null
    return ClassicDeadlineAnchor(deadline, wallEpochMsNow, elapsedRealtimeMsNow)
}

internal fun classicRemainingMs(anchor: ClassicDeadlineAnchor, elapsedRealtimeMsNow: Long): Long {
    val monotonicNow = anchor.wallEpochMsAtAnchor + (elapsedRealtimeMsNow - anchor.elapsedRealtimeMsAtAnchor).coerceAtLeast(0L)
    return (anchor.deadlineEpochMs - monotonicNow).coerceAtLeast(0L)
}

internal fun classicShownSeconds(remainingMs: Long): Int =
    if (remainingMs <= 0L) 0 else ceil(remainingMs / 1000.0).toInt().coerceAtLeast(1)
