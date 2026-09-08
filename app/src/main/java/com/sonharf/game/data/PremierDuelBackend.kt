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

suspend fun OnlineGameBackend.getPremierOpponent(room: GameRoomDto): ProfileDto? {
    if (room.isBot) return null
    val me = currentUserId() ?: return null
    val opponentId = if (room.hostId == me) room.guestId else room.hostId
    return opponentId?.let { runCatching { getProfile(it) }.getOrNull() }
}

/**
 * Submit through the deployed v3 RPC, then collapse the transient bot_turn state before returning
 * control to the arena. The database trigger usually advances the bot immediately; getRoom observes
 * that authoritative result. botTakeTurn is only the recovery path when the transient row remains.
 */
suspend fun OnlineGameBackend.submitPremierWord(roomId: String, word: String): GameRoomDto {
    val submitted = submitWord(roomId, word)
    if (!submitted.isBot || !submitted.botTurn || submitted.isPremierFinished()) return submitted

    val synced = runCatching { getRoom(roomId) }.getOrNull()
    if (synced != null && (!synced.botTurn || synced.isPremierFinished())) return synced

    return runCatching { botTakeTurn(roomId) }
        .getOrElse { synced ?: submitted }
}

fun GameRoomDto.isPremierLive(): Boolean = status in setOf("playing", "quiz", "final", "sudden_death", "paused")

fun GameRoomDto.isPremierFinished(): Boolean = status in setOf("finished", "completed", "forfeited", "surrendered") || winnerId != null
