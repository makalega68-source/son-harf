package com.sonharf.game.data

import com.sonharf.game.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.signInAnonymously
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_vip") val isVip: Boolean = false,
    val diamonds: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0
)

@Serializable
data class GameRoomDto(
    val id: String,
    val code: String,
    @SerialName("host_id") val hostId: String,
    @SerialName("guest_id") val guestId: String? = null,
    val status: String,
    @SerialName("current_player_id") val currentPlayerId: String? = null,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("turn_deadline") val turnDeadline: String? = null
)

@Serializable
data class GameWordDto(
    val id: Long,
    @SerialName("room_id") val roomId: String,
    @SerialName("player_id") val playerId: String,
    val word: String,
    @SerialName("normalized_word") val normalizedWord: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ChatMessageDto(
    val id: Long,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class ProfileWrite(
    val id: String,
    @SerialName("display_name") val displayName: String
)

@Serializable
private data class ChatWrite(
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String
)

object SupabaseProvider {
    val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        require(configured) { "Supabase publishable key is not configured" }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}

class OnlineGameBackend(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {
    suspend fun ensurePlayer(displayName: String): ProfileDto {
        if (supabase.auth.currentUserOrNull() == null) {
            supabase.auth.signInAnonymously()
        }
        val userId = requireNotNull(supabase.auth.currentUserOrNull()?.id) { "No authenticated user" }
        val safeName = displayName.trim().ifBlank { "Oyuncu" }.take(24)
        return supabase.from("profiles")
            .upsert(ProfileWrite(userId, safeName)) { select() }
            .decodeSingle<ProfileDto>()
    }

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun createRoom(): GameRoomDto =
        supabase.postgrest.rpc("create_room").decodeSingle<GameRoomDto>()

    suspend fun joinRoom(code: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "join_room_by_code",
            parameters = buildJsonObject {
                put("p_code", code.trim().uppercase())
            }
        ).decodeSingle<GameRoomDto>()

    suspend fun submitWord(roomId: String, word: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "submit_word",
            parameters = buildJsonObject {
                put("p_room_id", roomId)
                put("p_word", word.trim())
            }
        ).decodeSingle<GameRoomDto>()

    suspend fun forfeit(roomId: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "forfeit_room",
            parameters = buildJsonObject {
                put("p_room_id", roomId)
            }
        ).decodeSingle<GameRoomDto>()

    suspend fun sendChat(roomId: String, text: String) {
        val userId = requireNotNull(currentUserId()) { "No authenticated user" }
        val body = text.trim().take(300)
        require(body.isNotEmpty())
        supabase.from("chat_messages").insert(ChatWrite(roomId, userId, body))
    }

    suspend fun getRoom(roomId: String): GameRoomDto =
        supabase.from("game_rooms").select {
            filter { eq("id", roomId) }
        }.decodeSingle<GameRoomDto>()

    suspend fun getWords(roomId: String): List<GameWordDto> =
        supabase.from("game_words").select {
            filter { eq("room_id", roomId) }
        }.decodeList<GameWordDto>().sortedBy { it.id }

    suspend fun getChat(roomId: String): List<ChatMessageDto> =
        supabase.from("chat_messages").select {
            filter { eq("room_id", roomId) }
        }.decodeList<ChatMessageDto>().sortedBy { it.id }

    fun observeRoom(roomId: String, intervalMs: Long = 700): Flow<GameRoomDto> = flow {
        var previous: GameRoomDto? = null
        while (currentCoroutineContext().isActive) {
            val next = getRoom(roomId)
            if (next != previous) {
                emit(next)
                previous = next
            }
            delay(intervalMs)
        }
    }

    fun observeWords(roomId: String, intervalMs: Long = 700): Flow<List<GameWordDto>> = flow {
        var previous: List<GameWordDto> = emptyList()
        while (currentCoroutineContext().isActive) {
            val next = getWords(roomId)
            if (next != previous) {
                emit(next)
                previous = next
            }
            delay(intervalMs)
        }
    }

    fun observeChat(roomId: String, intervalMs: Long = 900): Flow<List<ChatMessageDto>> = flow {
        var previous: List<ChatMessageDto> = emptyList()
        while (currentCoroutineContext().isActive) {
            val next = getChat(roomId)
            if (next != previous) {
                emit(next)
                previous = next
            }
            delay(intervalMs)
        }
    }
}
