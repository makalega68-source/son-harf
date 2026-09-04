package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import java.time.Instant

internal enum class ClassicTurnPhase {
    WAITING,
    CONNECTING,
    MY_TURN,
    SUBMITTING,
    OPPONENT_TURN,
    RECONNECTING,
    FINISHED,
}

internal data class ClassicMonotonicDeadlineAnchor(
    val serverDeadlineEpochMs: Long,
    val wallEpochMsAtAnchor: Long,
    val elapsedRealtimeMsAtAnchor: Long,
) {
    fun remainingMs(elapsedRealtimeNowMs: Long): Long =
        (serverDeadlineEpochMs - wallEpochMsAtAnchor -
            (elapsedRealtimeNowMs - elapsedRealtimeMsAtAnchor)).coerceAtLeast(0L)

    fun displaySeconds(elapsedRealtimeNowMs: Long): Int {
        val remaining = remainingMs(elapsedRealtimeNowMs)
        return if (remaining <= 0L) 0 else ((remaining + 999L) / 1000L).toInt()
    }
}

internal data class ClassicSnapshotVersion(
    val roomId: String,
    val status: String,
    val roundNo: Int,
    val validWordCount: Int,
    val deadlineEpochMs: Long?,
    val currentPlayerId: String?,
    val lastEvent: String?,
    val lastEventPlayerId: String?,
    val hostScore: Int,
    val guestScore: Int,
)

internal fun classicDeadlineEpochMs(deadline: String?): Long? =
    deadline?.let { value -> runCatching { Instant.parse(value).toEpochMilli() }.getOrNull() }

internal fun classicDeadlineEventKey(room: GameRoomDto): String =
    listOf(
        room.id,
        room.status,
        room.roundNo.toString(),
        room.validWordCount.toString(),
        room.currentPlayerId.orEmpty(),
        room.turnDeadline.orEmpty(),
    ).joinToString("|")

internal fun GameRoomDto.classicSnapshotVersion(): ClassicSnapshotVersion = ClassicSnapshotVersion(
    roomId = id,
    status = status,
    roundNo = roundNo,
    validWordCount = validWordCount,
    deadlineEpochMs = classicDeadlineEpochMs(turnDeadline),
    currentPlayerId = currentPlayerId,
    lastEvent = lastEvent,
    lastEventPlayerId = lastEventPlayerId,
    hostScore = hostScore,
    guestScore = guestScore,
)

internal fun shouldAcceptClassicSnapshot(
    current: ClassicSnapshotVersion,
    incoming: ClassicSnapshotVersion,
): Boolean {
    if (current.roomId != incoming.roomId) return true
    if (current.status == "finished" && incoming.status != "finished") return false
    if (incoming.roundNo != current.roundNo) return incoming.roundNo > current.roundNo
    if (incoming.validWordCount != current.validWordCount) return incoming.validWordCount > current.validWordCount

    val currentDeadline = current.deadlineEpochMs
    val incomingDeadline = incoming.deadlineEpochMs
    if (current.currentPlayerId != incoming.currentPlayerId) {
        if (currentDeadline != null && incomingDeadline != null) return incomingDeadline > currentDeadline
        return incoming.status == "finished" || incoming.status != current.status
    }
    if (currentDeadline != null && incomingDeadline != null && incomingDeadline < currentDeadline) return false

    val exactDuplicate = current == incoming
    return !exactDuplicate
}

internal fun shouldAcceptClassicSnapshot(current: GameRoomDto, incoming: GameRoomDto): Boolean =
    shouldAcceptClassicSnapshot(current.classicSnapshotVersion(), incoming.classicSnapshotVersion())

internal fun classicTurnPhase(
    room: GameRoomDto?,
    me: String?,
    submitting: Boolean,
    reconnecting: Boolean,
): ClassicTurnPhase = when {
    room == null -> ClassicTurnPhase.CONNECTING
    room.status == "finished" -> ClassicTurnPhase.FINISHED
    reconnecting || room.status == "paused" || room.disconnectedPlayerId != null -> ClassicTurnPhase.RECONNECTING
    room.status == "waiting" || room.guestId == null && !room.isBot -> ClassicTurnPhase.WAITING
    submitting -> ClassicTurnPhase.SUBMITTING
    room.currentPlayerId == me -> ClassicTurnPhase.MY_TURN
    else -> ClassicTurnPhase.OPPONENT_TURN
}
