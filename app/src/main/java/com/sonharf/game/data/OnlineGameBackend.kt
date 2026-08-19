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
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_visibility") val avatarVisibility: String = "hidden",
    @SerialName("allow_match_chat") val allowMatchChat: Boolean = true,
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
    val language: String = "tr",
    @SerialName("host_score") val hostScore: Int = 0,
    @SerialName("guest_score") val guestScore: Int = 0,
    @SerialName("host_streak") val hostStreak: Int = 0,
    @SerialName("guest_streak") val guestStreak: Int = 0,
    @SerialName("valid_word_count") val validWordCount: Int = 0,
    @SerialName("final_moves_remaining") val finalMovesRemaining: Int = 0,
    @SerialName("last_event") val lastEvent: String? = null,
    @SerialName("last_event_player_id") val lastEventPlayerId: String? = null,
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
data class TriviaQuestionDto(
    val id: Long,
    val language: String,
    val question: String,
    @SerialName("option_a") val optionA: String,
    @SerialName("option_b") val optionB: String,
    @SerialName("option_c") val optionC: String,
    @SerialName("option_d") val optionD: String
)

@Serializable
data class TriviaRoundDto(
    val id: String,
    @SerialName("room_id") val roomId: String,
    val milestone: Int,
    @SerialName("bonus_points") val bonusPoints: Int,
    @SerialName("question_id") val questionId: Long,
    @SerialName("reveal_at") val revealAt: String,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null
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

    suspend fun createRoom(language: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "create_room",
            parameters = buildJsonObject { put("p_language", language) }
        ).decodeSingle<GameRoomDto>()

    suspend fun joinRoom(code: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "join_room_by_code",
            parameters = buildJsonObject { put("p_code", code.trim().uppercase()) }
        ).decodeSingle<GameRoomDto>()

    suspend fun submitWord(roomId: String, word: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "submit_word",
            parameters = buildJsonObject {
                put("p_room_id", roomId)
                put("p_word", word.trim())
            }
        ).decodeSingle<GameRoomDto>()

    suspend fun claimTurnTimeout(roomId: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "claim_turn_timeout",
            parameters = buildJsonObject { put("p_room_id", roomId) }
        ).decodeSingle<GameRoomDto>()

    suspend fun answerTrivia(roundId: String, answerIndex: Int): GameRoomDto =
        supabase.postgrest.rpc(
            function = "answer_trivia",
            parameters = buildJsonObject {
                put("p_round_id", roundId)
                put("p_answer_index", answerIndex)
            }
        ).decodeSingle<GameRoomDto>()

    suspend fun forfeit(roomId: String): GameRoomDto =
        supabase.postgrest.rpc(
            function = "forfeit_room",
            parameters = buildJsonObject { put("p_room_id", roomId) }
        ).decodeSingle<GameRoomDto>()

    suspend fun sendChat(roomId: String, text: String) {
        val userId = requireNotNull(currentUserId()) { "No authenticated user" }
        val body = text.trim().take(300)
        require(body.isNotEmpty())
        supabase.from("chat_messages").insert(ChatWrite(roomId, userId, body))
    }

    suspend fun blockUser(userId: String) {
        supabase.postgrest.rpc(
            function = "block_user",
            parameters = buildJsonObject { put("p_blocked_id", userId) }
        )
    }

    suspend fun unblockUser(userId: String) {
        supabase.postgrest.rpc(
            function = "unblock_user",
            parameters = buildJsonObject { put("p_blocked_id", userId) }
        )
    }

    suspend fun setPhotoAccess(viewerId: String, allowed: Boolean) {
        supabase.postgrest.rpc(
            function = "set_photo_access",
            parameters = buildJsonObject {
                put("p_viewer_id", viewerId)
                put("p_allowed", allowed)
            }
        )
    }

    suspend fun getRoom(roomId: String): GameRoomDto =
        supabase.from("game_rooms").select { filter { eq("id", roomId) } }.decodeSingle<GameRoomDto>()

    suspend fun getWords(roomId: String): List<GameWordDto> =
        supabase.from("game_words").select { filter { eq("room_id", roomId) } }
            .decodeList<GameWordDto>().sortedBy { it.id }

    suspend fun getChat(roomId: String): List<ChatMessageDto> =
        supabase.from("chat_messages").select { filter { eq("room_id", roomId) } }
            .decodeList<ChatMessageDto>().sortedBy { it.id }

    suspend fun getActiveTriviaRound(roomId: String): TriviaRoundDto? =
        supabase.from("trivia_rounds").select { filter { eq("room_id", roomId) } }
            .decodeList<TriviaRoundDto>()
            .filter { it.resolvedAt == null }
            .maxByOrNull { it.milestone }

    suspend fun getTriviaQuestion(questionId: Long): TriviaQuestionDto =
        supabase.from("trivia_questions").select { filter { eq("id", questionId) } }
            .decodeSingle<TriviaQuestionDto>()

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
