package com.sonharf.game

import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class MascotChatTurn(val role: String, val text: String)

@Serializable
internal data class MascotChatRequest(
    val message: String,
    val history: List<MascotChatTurn> = emptyList(),
    val language: String = "tr",
    @SerialName("player_name") val playerName: String? = null,
    @SerialName("companion_name") val companionName: String? = null,
    @SerialName("game_context") val gameContext: String? = null,
    @SerialName("mascot_id") val mascotId: String? = null,
    @SerialName("mascot_title") val mascotTitle: String? = null,
    @SerialName("mascot_personality") val mascotPersonality: String? = null,
    @SerialName("lore_context") val loreContext: String? = null,
    @SerialName("player_wins") val playerWins: Int? = null,
    @SerialName("player_losses") val playerLosses: Int? = null,
    @SerialName("friendship_level") val friendshipLevel: Int? = null,
    @SerialName("memory_fragments") val memoryFragments: Int? = null,
    @SerialName("season_level") val seasonLevel: Int? = null,
    @SerialName("daily_play_streak") val dailyPlayStreak: Int? = null,
    @SerialName("best_streak") val bestStreak: Int? = null,
    @SerialName("longest_word") val longestWord: String? = null,
    @SerialName("selected_title") val selectedTitle: String? = null,
    @SerialName("rival_name") val rivalName: String? = null,
    @SerialName("rival_matches") val rivalMatches: Int? = null,
    @SerialName("rival_wins") val rivalWins: Int? = null,
    @SerialName("rival_losses") val rivalLosses: Int? = null,
)

@Serializable
internal data class MascotChatResponse(
    val reply: String,
    val mood: String = "calm",
    val animation: String = "idle_breathe",
    @SerialName("memory_note") val memoryNote: String? = null,
    @SerialName("used_fallback") val usedFallback: Boolean = false,
)

internal object MascotAiChatService {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 8_000
            requestTimeoutMillis = 22_000
            socketTimeoutMillis = 22_000
        }
    }

    private fun localFallback(request: MascotChatRequest): MascotChatResponse {
        val character = LetharaLore.characterForMascot(request.mascotId)
        val context = MascotVerifiedContext(
            wins = request.playerWins ?: 0,
            losses = request.playerLosses ?: 0,
            friendshipLevel = request.friendshipLevel ?: 1,
            memoryFragments = request.memoryFragments ?: 0,
            seasonLevel = request.seasonLevel,
            dailyPlayStreak = request.dailyPlayStreak,
            bestStreak = request.bestStreak,
            longestWord = request.longestWord,
            selectedTitle = request.selectedTitle,
            rivalName = request.rivalName,
            rivalMatches = request.rivalMatches ?: 0,
            rivalWins = request.rivalWins ?: 0,
            rivalLosses = request.rivalLosses ?: 0,
        )
        return MascotCompanionCoach.localReply(
            character = character,
            message = request.message,
            language = request.language,
            context = context,
            historySize = request.history.size,
        )
    }

    suspend fun chat(request: MascotChatRequest): MascotChatResponse {
        if (!SupabaseProvider.configured) return localFallback(request)
        val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken ?: return localFallback(request)
        val cleanRequest = request.copy(
            message = request.message.trim().take(700),
            history = request.history.takeLast(10).map { it.copy(text = it.text.take(600)) },
            playerName = request.playerName?.trim()?.take(32),
            companionName = request.companionName?.trim()?.take(18),
            gameContext = request.gameContext?.trim()?.take(1000),
            mascotId = request.mascotId?.take(40),
            mascotTitle = request.mascotTitle?.take(80),
            mascotPersonality = request.mascotPersonality?.take(180),
            loreContext = request.loreContext?.take(1400),
            playerWins = request.playerWins?.coerceIn(0, 1_000_000),
            playerLosses = request.playerLosses?.coerceIn(0, 1_000_000),
            friendshipLevel = request.friendshipLevel?.coerceIn(1, 30),
            memoryFragments = request.memoryFragments?.coerceIn(0, 120),
            seasonLevel = request.seasonLevel?.coerceIn(1, 10_000),
            dailyPlayStreak = request.dailyPlayStreak?.coerceIn(0, 100_000),
            bestStreak = request.bestStreak?.coerceIn(0, 100_000),
            longestWord = request.longestWord?.trim()?.take(32),
            selectedTitle = request.selectedTitle?.trim()?.take(32),
            rivalName = request.rivalName?.trim()?.take(24),
            rivalMatches = request.rivalMatches?.coerceIn(0, 1_000_000),
            rivalWins = request.rivalWins?.coerceIn(0, 1_000_000),
            rivalLosses = request.rivalLosses?.coerceIn(0, 1_000_000),
        )
        return runCatching {
            val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/eve-chat") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_KEY)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(cleanRequest))
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) return@runCatching localFallback(cleanRequest)
            val decoded = json.decodeFromString<MascotChatResponse>(body)
            decoded.copy(reply = decoded.reply.take(600))
        }.getOrElse { localFallback(cleanRequest) }
    }
}
