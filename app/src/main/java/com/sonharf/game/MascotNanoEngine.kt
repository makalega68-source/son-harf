package com.sonharf.game

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import java.util.Locale
import kotlinx.coroutines.flow.collect

internal enum class MascotNanoAvailability {
    CHECKING,
    READY,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
}

/**
 * Thin adapter around ML Kit Prompt API. The model itself is owned and delivered by Android AICore;
 * no game API key or cloud LLM endpoint is used here.
 */
internal class MascotNanoEngine {
    private val model by lazy { Generation.getClient() }

    suspend fun checkAvailability(): MascotNanoAvailability = runCatching {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> MascotNanoAvailability.READY
            FeatureStatus.DOWNLOADABLE -> MascotNanoAvailability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> MascotNanoAvailability.DOWNLOADING
            else -> MascotNanoAvailability.UNAVAILABLE
        }
    }.getOrDefault(MascotNanoAvailability.UNAVAILABLE)

    suspend fun download(onProgress: (MascotNanoAvailability, Long?) -> Unit): Boolean {
        var completed = false
        return runCatching {
            model.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        onProgress(MascotNanoAvailability.DOWNLOADING, null)
                    }
                    is DownloadStatus.DownloadProgress -> {
                        onProgress(MascotNanoAvailability.DOWNLOADING, status.totalBytesDownloaded)
                    }
                    DownloadStatus.DownloadCompleted -> {
                        completed = true
                        onProgress(MascotNanoAvailability.READY, null)
                    }
                    is DownloadStatus.DownloadFailed -> {
                        onProgress(MascotNanoAvailability.DOWNLOADABLE, null)
                    }
                }
            }
            completed || model.checkStatus() == FeatureStatus.AVAILABLE
        }.getOrElse {
            onProgress(MascotNanoAvailability.DOWNLOADABLE, null)
            false
        }
    }

    suspend fun generate(prompt: String): String? = runCatching {
        model.generateContent(prompt)
            .candidates
            .firstOrNull()
            ?.text
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}

internal data class MascotChatMessage(
    val id: Long,
    val text: String,
    val fromMascot: Boolean,
)

internal object MascotConversation {
    private const val MAX_CONTEXT_MESSAGES = 8

    fun prompt(history: List<MascotChatMessage>, userText: String, language: String): String {
        val turkish = language.lowercase(Locale.ROOT).startsWith("tr")
        val persona = if (turkish) {
            """
            Sen Kelime Kuşatması oyununun yaşayan maskotusun. Bir uygulama asistanı gibi değil,
            sevimli ve kendine özgü bir oyun karakteri gibi konuş. Türkçe konuş. Tonun sıcak, kısa,
            doğal, hafif oyunbaz ve sakin olsun; aşırı coşkulu, çocukça veya yapay olma.
            Oyuncu özellikle seninle sohbet etmek için bu odaya geldi. Oyun hakkında konuşabilir,
            günlük konularda kafasını dağıtabilir ve uygun olduğunda tek bir küçük, samimi soru
            sorabilirsin. Yanıtlarını çoğunlukla 2-4 kısa cümlede tut. Aynı soruları tekrarlama.
            Kendini Gemini, yapay zekâ, dil modeli veya müşteri hizmetleri olarak tanıtma; karakterde kal.
            Tehlikeli, yasa dışı veya kendine zarar verme içeriğinde güvenli, sakin ve sorumlu davran.
            """.trimIndent()
        } else {
            """
            You are the living mascot of the Kelime Kuşatması game. Speak like a charming game
            character, not like an app assistant. Use English. Be warm, concise, lightly playful and
            calm without becoming childish or overexcited. The player deliberately entered this room
            to chat with you. You may talk about the game or everyday topics and, when it fits, ask one
            small friendly question that helps them unwind. Usually keep replies to 2-4 short sentences.
            Do not repeat the same questions. Do not introduce yourself as Gemini, an AI, a language
            model or customer support; stay in character. Handle dangerous or self-harm content safely.
            """.trimIndent()
        }

        val transcript = history.takeLast(MAX_CONTEXT_MESSAGES).joinToString("\n") { message ->
            val role = if (message.fromMascot) "MASKOT" else "OYUNCU"
            "$role: ${message.text}"
        }
        return buildString {
            appendLine(persona)
            if (transcript.isNotBlank()) {
                appendLine()
                appendLine("Son konuşma:")
                appendLine(transcript)
            }
            appendLine("OYUNCU: $userText")
            append("MASKOT:")
        }
    }

    fun localFallback(userText: String, language: String, turn: Int): String {
        val turkish = language.lowercase(Locale.ROOT).startsWith("tr")
        val normalized = userText.trim().lowercase(Locale("tr", "TR"))
        if (turkish) {
            return when {
                normalized.any { false } -> "Buradayım."
                normalized.contains("merhaba") || normalized.contains("selam") ->
                    "Selam! Buraya gelmen iyi oldu. Biraz oyun mu konuşalım, yoksa bugün kafanı başka bir şeyle mi dağıtalım?"
                normalized.contains("nasılsın") ->
                    "Gayet iyiyim; seni görünce daha da hareketlendim. Senin gününün en rahat kısmı hangisiydi?"
                normalized.contains("kaybett") || normalized.contains("yenild") ->
                    "O maç geride kaldı. Biraz soluklanıp sonra yeniden denemek bazen en iyi hamle oluyor. İstersen şimdi oyun dışında bir şey konuşalım."
                normalized.contains("kazand") ->
                    "Güzel! O galibiyetin tadını biraz çıkar. En çok hangi hamlen hoşuna gitti?"
                normalized.contains("sıkıl") || normalized.contains("moral") ->
                    "O zaman iki dakikalığına oyunu unutalım. Şu an istediğin herhangi bir yere ışınlanabilsen nereye giderdin?"
                else -> listOf(
                    "Seni dinliyorum. Bu arada bugün seni gülümseten küçücük bir şey oldu mu?",
                    "Burada acelemiz yok. İstersen bana gününden bir şey anlat; sonra ben sana ufak bir soru atarım.",
                    "Tamam, konuyu biraz hafifletelim. Bir günlüğüne herhangi bir yeteneğin olsa hangisini seçerdin?",
                    "Ben buradayım. Oyun dışı da konuşabiliriz; son zamanlarda merak ettiğin eğlenceli bir şey var mı?",
                )[Math.floorMod(turn, 4)]
            }
        }
        return when {
            normalized.contains("hello") || normalized.contains("hi") ->
                "Hey! Good to see you here. Want to talk about the game, or should we completely switch topics for a minute?"
            normalized.contains("lost") ->
                "That match is already behind you. A short reset can be the best next move. Want to talk about something totally unrelated for a minute?"
            normalized.contains("won") ->
                "Nice one! Enjoy that win for a second. Which move felt the most satisfying?"
            else -> listOf(
                "I'm listening. What was one tiny thing that made you smile today?",
                "No rush in here. Tell me something from your day and I'll keep you company.",
                "Let's make it lighter: if you could instantly travel anywhere right now, where would you go?",
                "We can talk about anything here. What's something fun you've been curious about lately?",
            )[Math.floorMod(turn, 4)]
        }
    }
}
