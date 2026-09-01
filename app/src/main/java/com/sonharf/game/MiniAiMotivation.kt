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
    private val localLossesTr = listOf(
        "Çok yakındı. Bir tane daha?",
        "Bu sefer olmadı, rövanş?",
        "Pes etmek yok, yeniden deneyelim.",
        "İyi mücadeleydi, bir maç daha?",
        "Az farkla kaçtı. Rövanşa ne dersin?",
        "Güzel denemeydi, tekrar hazır mısın?",
    )
    private val localLossesEn = listOf(
        "That was close. One more?",
        "Not this time. Rematch?",
        "Keep going, let's try again.",
        "Good fight. One more match?",
        "So close. How about a rematch?",
        "Nice try. Ready to go again?",
    )
    private val localReturnsTr = listOf(
        "Tekrar hoş geldin! Hazırsan devam. 👋",
        "Seni yeniden görmek güzel! ✨",
        "Hoş geldin, kaldığın yerden devam. 🎯",
        "Geri döndün! Güzel bir maç seni bekliyor. 👋",
        "Tekrar buradasın, hazırsan başlayalım. ⚡",
    )
    private val localReturnsEn = listOf(
        "Welcome back! Continue when you're ready. 👋",
        "Great to see you again! ✨",
        "Welcome back, pick up where you left off. 🎯",
        "You're back! A good match awaits. 👋",
        "Back again? Let's play when you're ready. ⚡",
    )
    private val localContinuesTr = listOf(
        "Bir maç daha? ⚡",
        "Rövanşa hazır mısın? 🎯",
        "Hazırsan zinciri sürdürelim. ✨",
        "Yeni maç, yeni fırsat. 👏",
        "Devam etmek istersen oyun hazır. 🎮",
    )
    private val localContinuesEn = listOf(
        "One more match? ⚡",
        "Ready for a rematch? 🎯",
        "Keep the chain going when you're ready. ✨",
        "New match, new chance. 👏",
        "The game is ready if you are. 🎮",
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

    internal fun localMatchLoss(language: String = "tr", seed: Int = (System.nanoTime() xor Thread.currentThread().id).toInt()): String {
        val pool = if (language == "en") localLossesEn else localLossesTr
        val candidates = pool.filterNot { it == lastShown }.ifEmpty { pool }
        val selected = candidates[seed.absoluteValue % candidates.size]
        lastShown = selected
        return selected
    }

    internal fun localPlayerReturned(language: String = "tr", seed: Int = (System.nanoTime() xor Thread.currentThread().id).toInt()): String {
        val pool = if (language == "en") localReturnsEn else localReturnsTr
        val candidates = pool.filterNot { it == lastShown }.ifEmpty { pool }
        val selected = candidates[seed.absoluteValue % candidates.size]
        lastShown = selected
        return selected
    }

    internal fun localMatchContinue(language: String = "tr", seed: Int = (System.nanoTime() xor Thread.currentThread().id).toInt()): String {
        val pool = if (language == "en") localContinuesEn else localContinuesTr
        val candidates = pool.filterNot { it == lastShown }.ifEmpty { pool }
        val selected = candidates[seed.absoluteValue % candidates.size]
        lastShown = selected
        return selected
    }

    internal fun shouldAttemptAi(eventKey: String, divisor: Int = 20): Boolean {
        val safeDivisor = divisor.coerceAtLeast(2)
        return (eventKey.hashCode().toLong().absoluteValue % safeDivisor.toLong()) == 0L
    }

    internal fun sanitizeAiReply(raw: String): String? {
        val clean = raw.trim().lineSequence().firstOrNull()?.trim().orEmpty()
        if (clean.isBlank()) return null
        val clipped = clean.take(96)
        val punctuation = clipped.indexOfFirst { it == '.' || it == '!' || it == '?' }
        val singleSentence = if (punctuation >= 0) clipped.take(punctuation + 1) else clipped
        return singleSentence.trim().takeIf { it.length in 2..96 }
    }

    suspend fun maybeAiPlayerReturned(eventKey: String, language: String): String? {
        if (!shouldAttemptAi("return:$eventKey", divisor = 40) || !SupabaseProvider.configured) return null
        val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken ?: return null
        return runCatching {
            val request = MiniMotivationRequest(
                message = if (language == "en")
                    "PLAYER_RETURNED: Maximum 8 words. Warm welcome, no pressure."
                else
                    "PLAYER_RETURNED: En fazla 8 kelime. Sıcak karşılama, baskı yok.",
                language = language,
                gameContext = "Verified event: player returned after at least six hours away. One short sentence only.",
            )
            val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/eve-chat") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_KEY)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            if (!response.status.isSuccess()) return@runCatching null
            sanitizeAiReply(json.decodeFromString<MiniMotivationResponse>(response.bodyAsText()).reply)
        }.getOrNull()?.also { lastShown = it }
    }

    suspend fun maybeAiMatchContinue(matchId: String, language: String): String? {
        if (!shouldAttemptAi("continue:$matchId", divisor = 40) || !SupabaseProvider.configured) return null
        val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken ?: return null
        return runCatching {
            val request = MiniMotivationRequest(
                message = if (language == "en")
                    "MATCH_CONTINUE: Maximum 8 words. Friendly invitation for another match, no pressure."
                else
                    "MATCH_CONTINUE: En fazla 8 kelime. Yeni maça samimi davet, baskı yok.",
                language = language,
                gameContext = "Verified event: Son Harf match ended. Invite the player to continue with one short sentence only.",
            )
            val response = client.post("${BuildConfig.SUPABASE_URL}/functions/v1/eve-chat") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_KEY)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            if (!response.status.isSuccess()) return@runCatching null
            sanitizeAiReply(json.decodeFromString<MiniMotivationResponse>(response.bodyAsText()).reply)
        }.getOrNull()?.also { lastShown = it }
    }

    suspend fun maybeAiMatchLoss(matchId: String, language: String): String? {
        if (!shouldAttemptAi(matchId) || !SupabaseProvider.configured) return null
        val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken ?: return null
        return runCatching {
            val request = MiniMotivationRequest(
                message = if (language == "en")
                    "MATCH_LOSS: One short, warm encouragement. Maximum 8 words. No blame or pressure."
                else
                    "MATCH_LOSS: En fazla 8 kelimelik kısa, samimi destek. Suçlama veya baskı yok.",
                language = language,
                gameContext = "Verified event: player lost the Son Harf match. Return one short sentence only.",
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
