package com.sonharf.game

import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
internal data class EveChatTurn(val role: String, val text: String)

@Serializable
internal data class EveChatRequest(
    val message: String,
    val history: List<EveChatTurn> = emptyList(),
    val language: String = "tr",
    @SerialName("player_name") val playerName: String? = null,
    @SerialName("companion_name") val companionName: String? = null,
    @SerialName("game_context") val gameContext: String? = null,
)

@Serializable
internal data class EveChatResponse(
    val reply: String,
    val mood: String = "calm",
    val animation: String = "idle_breathe",
    @SerialName("memory_note") val memoryNote: String? = null,
)

internal object EveAiChatService {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(OkHttp)

    suspend fun chat(request: EveChatRequest): EveChatResponse {
        check(SupabaseProvider.configured) { "Supabase yapılandırılmamış." }
        val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken
            ?: error("Maskotla konuşmak için oturum açmalısın.")
        val cleanRequest = request.copy(
            message = request.message.trim().take(1000),
            history = request.history.takeLast(12).map {
                it.copy(role = if (it.role == "assistant") "assistant" else "user", text = it.text.take(900))
            },
            playerName = request.playerName?.trim()?.take(32),
            companionName = request.companionName?.trim()?.take(18),
            gameContext = request.gameContext?.trim()?.take(1200),
        )
        val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/eve-chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", BuildConfig.SUPABASE_KEY)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(cleanRequest))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val code = runCatching { json.decodeFromString<EveErrorResponse>(body).error }.getOrNull().orEmpty()
            error(when (code) {
                "ai_not_configured" -> "Gemini anahtarı sunucuda doğrulanamadı."
                "invalid_session", "unauthorized" -> "Oturum doğrulanamadı. Lütfen tekrar giriş yap."
                "free_quota_reached", "free_provider_quota_reached" -> "Bugünkü ücretsiz maskot sohbet hakkı doldu. Yarın yeniden açılacak."
                "quota_check_failed", "server_not_configured" -> "Ücretsiz sohbet sistemi şu anda hazırlanıyor. Biraz sonra tekrar dene."
                else -> "Maskot şu anda cevap veremiyor. Biraz sonra tekrar dene."
            })
        }
        val decoded = json.decodeFromString<EveChatResponse>(body)
        return decoded.copy(reply = decoded.reply.take(700))
    }
}

@Serializable
private data class EveErrorResponse(val error: String = "")
