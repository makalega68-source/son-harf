package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class WordArenaMatchmakingDto(
    val status: String = "idle",
    @SerialName("room_id") val roomId: String? = null,
)

@Serializable
data class WordArenaRoomDto(
    val id: String,
    @SerialName("host_id") val hostId: String,
    @SerialName("guest_id") val guestId: String,
    val language: String = "tr",
    val letters: String,
    val status: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("host_score") val hostScore: Int = 0,
    @SerialName("guest_score") val guestScore: Int = 0,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("result_applied") val resultApplied: Boolean = false,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class WordArenaWordDto(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val word: String,
    @SerialName("normalized_word") val normalizedWord: String,
    @SerialName("base_points") val basePoints: Int,
    val combo: Int = 1,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class WordArenaInviteDto(
    @SerialName("invite_id") val inviteId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val language: String = "tr",
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class WordArenaInviteCreatedDto(
    @SerialName("invite_id") val inviteId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class WordArenaActionDto(
    val status: String = "idle",
    @SerialName("room_id") val roomId: String? = null,
)

@Serializable
data class WordArenaSubmitDto(
    val accepted: Boolean = false,
    val status: String = "playing",
    val word: String? = null,
    @SerialName("normalized_word") val normalizedWord: String? = null,
    @SerialName("base_points") val basePoints: Int = 0,
    val combo: Int = 1,
    @SerialName("provisional_score") val provisionalScore: Int = 0,
)

suspend fun OnlineGameBackend.joinWordArena(language: String): WordArenaMatchmakingDto =
    SupabaseProvider.client.postgrest.rpc(
        "join_word_arena_v1",
        buildJsonObject {
            put("p_language", if (language.lowercase() == "en") "en" else "tr")
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.pollWordArena(): WordArenaMatchmakingDto =
    SupabaseProvider.client.postgrest.rpc("poll_word_arena_v1").decodeSingle()

suspend fun OnlineGameBackend.cancelWordArena() {
    SupabaseProvider.client.postgrest.rpc("cancel_word_arena_v1")
}

suspend fun OnlineGameBackend.getWordArenaRoom(roomId: String): WordArenaRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_word_arena_room_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getWordArenaWords(roomId: String): List<WordArenaWordDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_word_arena_words_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeList()

suspend fun OnlineGameBackend.submitWordArena(roomId: String, word: String): WordArenaSubmitDto =
    SupabaseProvider.client.postgrest.rpc(
        "submit_word_arena_v1",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_word", word.trim())
        },
    ).decodeSingle()


suspend fun OnlineGameBackend.inviteFriendToWordArena(friendId: String, language: String): WordArenaInviteCreatedDto =
    SupabaseProvider.client.postgrest.rpc(
        "invite_friend_to_word_arena_v1",
        buildJsonObject {
            put("p_friend_id", friendId)
            put("p_language", if (language.lowercase() == "en") "en" else "tr")
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getIncomingWordArenaInvites(): List<WordArenaInviteDto> =
    SupabaseProvider.client.postgrest.rpc("get_incoming_word_arena_invites_v1").decodeList()

suspend fun OnlineGameBackend.respondWordArenaInvite(inviteId: String, accept: Boolean): WordArenaActionDto =
    SupabaseProvider.client.postgrest.rpc(
        "respond_word_arena_invite_v1",
        buildJsonObject {
            put("p_invite_id", inviteId)
            put("p_accept", accept)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.requestWordArenaRematch(roomId: String): WordArenaActionDto =
    SupabaseProvider.client.postgrest.rpc(
        "request_word_arena_rematch_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.pollWordArenaRematch(roomId: String): WordArenaActionDto =
    SupabaseProvider.client.postgrest.rpc(
        "poll_word_arena_rematch_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.cancelWordArenaRematch(roomId: String): Boolean =
    SupabaseProvider.client.postgrest.rpc(
        "cancel_word_arena_rematch_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()
