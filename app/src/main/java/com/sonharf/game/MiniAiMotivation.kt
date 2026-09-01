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
import kotlin.math.absoluteValue

@Serializable
private data class MiniMotivationRequest(
    val message: String,
    val history: List<String> = emptyList(),
    val language: String = "tr",
    @SerialName("game_context") val gameContext: String,
)

@Serializable
private data class MiniMotivationResponse(val reply: String = "")

internal object MiniAiMotivation {
    private val localWinsTr = listOf(
        "Tebrik ederim! Çok iyi oynadın. 🎉",
        "Harika maçtı! 👏",
        "Bugün formundasın! 🔥",
        "Güzel galibiyet, tebrikler! 🎉",
        "Çok iyi iş çıkardın! 👏",
        "Bu maç senindi! ✨",
        "Kelime zincirini çok iyi taşıdın! 🎯",
        "Temiz galibiyet, tebrikler! 🏆",
    )
    private val localWinsEn = listOf(
        "Great game! Well played. 🎉",
        "Nice win! 👏",
        "You're in great form today! 🔥",
        "Well played, congratulations! 🎉",
        "Excellent work! 👏",
        "That match was yours! ✨",
    )
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(OkHttp)
    @Volatile private var lastShown: String? = null

    internal fun localMatchWin(language: String = "tr", seed: Int = (System.nanoTime() xor Thread.currentThread().id).toInt()): String {
        val pool = if (language == "en") localWinsEn else localWinsTr
        val candidates = pool.filterNot { it == lastShown }.ifEmpty { pool }
        val selected = candidates[seed.absoluteValue % candidates.size]
        lastShown = selected
        return selected
    }

    internal fun shouldAttemptAi(matchId: String): Boolean =
        (matchId.hashCode().toLong().absoluteValue % 20L) == 0L

    internal fun sanitizeAiReply(raw: String): String? {
        val clean = raw.trim().lineSequence().firstOrNull()?.trim().orEmpty()
        if (clean.isBlank()) return null
        val clipped = clean.take(96)
        val punctuation = clipped.indexOfFirst { it == '.' || it == '!' || it == '?' }
        val singleSentence = if (punctuation >= 0) clipped.take(punctuation + 1) else clipped
        return singleSentence.trim().takeIf { it.length in 2..96 }
    }

    suspend fun maybeAiMatchWin(matchId: String, language: String): String? {
        if (!shouldAttemptAi(matchId) || !SupabaseProvider.configured) return null
        val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken ?: return null
        return runCatching {
            val request = MiniMotivationRequest(
                message = if (language == "en")
                    "MATCH_WIN: One short, warm congratulation. Maximum 8 words. No pressure."
                else
                    "MATCH_WIN: En fazla 8 kelimelik kısa, samimi bir tebrik. Baskı yok.",
                language = language,
                gameContext = "Verified event: player won the Son Harf match. Return one short sentence only.",
            )
            val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/eve-chat") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_KEY)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            if (!response.status.isSuccess()) return@runCatching null
            val body = response.bodyAsText()
            val reply = json.decodeFromString<MiniMotivationResponse>(body).reply
            sanitizeAiReply(reply)
        }.getOrNull()?.also { lastShown = it }
    }
}
