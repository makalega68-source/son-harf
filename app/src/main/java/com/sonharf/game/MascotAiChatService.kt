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
        val clean = request.message.trim().lowercase()
        val en = request.language.lowercase().startsWith("en")
        val reply = when {
            clean.contains("varkhor") -> if (en) "That name scratches at an old seal... I remember violet fire, then silence." else "O isim eski bir mührü tırmalıyor... Mor bir ateş, sonra sessizlik hatırlıyorum."
            clean.contains("hik") || clean.contains("geçmiş") || clean.contains("past") || clean.contains("story") ->
                LetharaLore.randomWhisper(character, if (en) "en" else "tr", request.history.size + clean.length)
            clean.contains("kaybett") || clean.contains("lost") -> if (en) "The Word Weave bends, it does not end. We will bind the next word more carefully." else "Söz Dokusu bükülür, ama bitmez. Sonraki kelimeyi daha sağlam bağlarız."
            clean.contains("kazand") || clean.contains("won") -> if (en) "Ha! The seal sparked. Even the old stars noticed that victory." else "Hah! Mühür kıvılcımlandı. O zaferi eski yıldızlar bile fark etti."
            else -> if (en) {
                "${character.name} tilts their head. “I hear you. The Word Weave is quiet enough to listen.”"
            } else {
                "${character.name} başını hafifçe eğiyor. “Seni duyuyorum. Söz Dokusu bugün dinleyecek kadar sakin.”"
            }
        }
        return MascotChatResponse(reply = reply, mood = "calm", animation = "idle_breathe", usedFallback = true)
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
