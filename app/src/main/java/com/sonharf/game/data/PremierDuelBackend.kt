package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
 * Premier submission uses the server's v4 wrapper so a bot response and the refreshed room state are
 * returned in the same authoritative call. This avoids exposing the transient bot_turn=true row to
 * the arena after the human opening move.
 */
suspend fun OnlineGameBackend.submitPremierWord(roomId: String, word: String): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "submit_word_v4",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_word", word)
        },
    ).decodeSingle()

fun GameRoomDto.isPremierLive(): Boolean = status in setOf("playing", "quiz", "final", "sudden_death", "paused")

fun GameRoomDto.isPremierFinished(): Boolean = status in setOf("finished", "completed", "forfeited", "surrendered") || winnerId != null
