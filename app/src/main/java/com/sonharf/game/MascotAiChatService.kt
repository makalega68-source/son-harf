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
    val animation: String = "idle",
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
        val en = request.language.lowercase().startsWith("en")
        val clean = request.message.trim().lowercase()
        val wins = request.playerWins ?: 0
        val losses = request.playerLosses ?: 0
        val total = (wins + losses).coerceAtLeast(0)
        val streak = request.bestStreak ?: 0

        val response = when {
            clean.contains("kazand") || clean.contains("yendim") || clean.contains("win") || clean.contains("won") ->
                MascotChatResponse(
                    reply = if (en) "Great move. Keep the next word just as clean." else "Harika oynadın. Sonraki kelimeyi de temiz seç.",
                    mood = "celebrating",
                    animation = "victory",
                    usedFallback = true,
                )

            clean.contains("kaybett") || clean.contains("yenild") || clean.contains("lose") || clean.contains("lost") ->
                MascotChatResponse(
                    reply = if (en) "Reset quickly. A safe short word is enough." else "Hızlı toparlan. Güvenli kısa bir kelime yeter.",
                    mood = "encouraging",
                    animation = "encouraging",
                    usedFallback = true,
                )

            clean.contains("taktik") || clean.contains("öner") || clean.contains("nasıl") ||
                clean.contains("tip") || clean.contains("advice") || clean.contains("strategy") ->
                MascotChatResponse(
                    reply = if (en) "Read the final letter first; keep one backup word." else "Önce son harfi gör; bir yedek kelime hazır tut.",
                    mood = "supportive",
                    animation = "thinking",
                    usedFallback = true,
                )

            clean.contains("rakip") || clean.contains("rival") ->
                MascotChatResponse(
                    reply = if (en) "Watch the chain, not the rival. Your word decides." else "Rakibe değil zincire bak. Kararı kelimen verir.",
                    mood = "supportive",
                    animation = "look_at_player",
                    usedFallback = true,
                )

            total == 0 ->
                MascotChatResponse(
                    reply = if (en) "I'm Chibi. Start simple and watch the final letter." else "Ben Chibi. Basit başla, son harfi takip et.",
                    mood = "happy",
                    animation = "greeting",
                    usedFallback = true,
                )

            losses >= wins + 3 ->
                MascotChatResponse(
                    reply = if (en) "Today, choose reliable words before risky ones." else "Bugün riskten önce güvenilir kelimeleri seç.",
                    mood = "encouraging",
                    animation = "encouraging",
                    usedFallback = true,
                )

            streak >= 3 ->
                MascotChatResponse(
                    reply = if (en) "You know how to build a streak. Keep it calm." else "Seri kurmayı biliyorsun. Sakin oyna, devam et.",
                    mood = "happy",
                    animation = "victory",
                    usedFallback = true,
                )

            else ->
                MascotChatResponse(
                    reply = if (en) "I'm Chibi. One clean word and we're moving." else "Ben Chibi. Temiz bir kelimeyle akışı başlatalım.",
                    mood = "calm",
                    animation = "greeting",
                    usedFallback = true,
                )
        }
        return response.copy(reply = response.reply.take(110))
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
            val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/chibi-chat") {
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
