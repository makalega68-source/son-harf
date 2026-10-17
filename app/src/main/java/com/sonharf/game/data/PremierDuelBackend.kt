package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from

/** Small Premier-specific queries layered on the existing authoritative room APIs. */
suspend fun OnlineGameBackend.findPremierActiveRoom(): GameRoomDto? {
    val me = currentUserId() ?: return null
    return SupabaseProvider.client.from("game_rooms")
        .select()
        .decodeList<GameRoomDto>()
        .asSequence()
        .filter { room ->
            (room.hostId == me || room.guestId == me) &&
                room.status in setOf("playing", "quiz", "final", "sudden_death", "paused") &&
                (room.isBot || room.guestId != null)
        }
        .maxByOrNull { it.createdAt }
}

/**
 * Read-only post-match lookup used by presentation surfaces after Premier has already left live play.
 * It never mutates match state and deliberately filters to rooms whose authoritative result is final.
 */
suspend fun OnlineGameBackend.findPremierLatestFinishedRoom(): GameRoomDto? {
    val me = currentUserId() ?: return null
    return SupabaseProvider.client.from("game_rooms")
        .select()
        .decodeList<GameRoomDto>()
        .asSequence()
        .filter { room ->
            (room.hostId == me || room.guestId == me) &&
                room.isPremierFinished() &&
                (room.isBot || room.guestId != null)
        }
        .maxByOrNull { it.createdAt }
}

suspend fun OnlineGameBackend.getPremierOpponent(room: GameRoomDto): ProfileDto? {
    if (room.isBot) return null
    val me = currentUserId() ?: return null
    val opponentId = if (room.hostId == me) room.guestId else room.hostId
    return opponentId?.let { runCatching { getProfile(it) }.getOrNull() }
}

/**
 * Submit through the deployed v3 RPC and return the authoritative room. The AI's reply is NOT
 * requested here: the arena asks for it after its "thinking" pause and, when the word ended a
 * round, after the round break. Asking here too made the AI answer instantly at a round change
 * and raced the arena's own request, so the last two moves arrived out of order.
 */
suspend fun OnlineGameBackend.submitPremierWord(roomId: String, word: String): GameRoomDto {
    val submitted = submitWord(roomId, word)
    if (!submitted.isBot || !submitted.botTurn || submitted.isPremierFinished()) return submitted
    return runCatching { getRoom(roomId) }.getOrNull() ?: submitted
}

fun GameRoomDto.isPremierLive(): Boolean = status in setOf("playing", "quiz", "final", "sudden_death", "paused")

fun GameRoomDto.isPremierFinished(): Boolean = status in setOf("finished", "completed", "forfeited", "surrendered") || winnerId != null
