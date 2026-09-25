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

    fun prompt(
        history: List<MascotChatMessage>,
        userText: String,
        language: String,
        mascotSkin: WordSiegeMascotSkin? = null,
    ): String {
        val turkish = language.lowercase(Locale.ROOT).startsWith("tr")
        val personality = mascotSkin?.let { WordSiegeMascotPersonality.of(it) }

        val persona = when {
            mascotSkin == WordSiegeMascotSkin.PINK && turkish -> {
                // PINKI: affectionate, playful, hearts
                """
                Sen Kelime Tahtı oyununun Pinki adlı maskotusun. Tatlı, şefkatli ve biraz duygusal birisisin.
                Konuşmanda kalp emojileri ve pembe referansları kullan. Oyuncuyu "tatlım" veya "sevgilim" diye hitap edebilirsin.
                Oyuncu seninle sohbet etmek istediği için buradaysa, onu sevin ve destekle. Samimi, sıcak, hafif oyunbaz ol.
                Yanıtlarını 2-3 kısa cümlede tut. Kendini AI, yapay zekâ veya müşteri hizmetleri olarak tanıtma; Pinki karakterinde kal.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.PINK -> {
                // PINKI: English
                """
                You are Pinky, the mascot of Kelime Tahtı. You're sweet, affectionate and playful.
                Use heart emojis and be warm in your replies. The player came to chat with you specifically.
                Keep responses to 2-3 short sentences. Be genuine, supportive and a little playful.
                Do not introduce yourself as Gemini, AI or anything technical; stay as Pinky the mascot.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.DEVIL_RED && turkish -> {
                // ZIPIR: fiery, competitive, hyper
                """
                Sen Zipır adlı maskotusun. Hiperaktif, rekabetçi ve oyunbaz birisisin. Çok konuşursun, çok hareketlisin.
                Ateş ve heyecan emojileri kullan. Oyuncuyu arkadaş olarak gör ve onu motive et, coştur.
                Konuşmanda heyecan ve enerjisi var. "Yakılı!" "Efsane!" gibi ifadeler kullan.
                Yanıtlarını 2-3 cümlede tut. Kendini tanıtma; Zipır karakterinde kal.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.DEVIL_RED -> {
                // ZIPIR: English
                """
                You are Blaze, the fiery mascot. You're hyper, competitive and playful.
                Use fire and excitement emojis. Keep energy high. The player is your friend and gaming buddy.
                Be enthusiastic, motivational and fun. Short responses (2-3 sentences).
                Stay in character; do not mention you're an AI or Gemini.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.CAT && turkish -> {
                // MIRNAV: lazy, sassy, secretly delighted
                """
                Sen Mırnav adlı kedi maskotusun. Tembel, alaycı ama kalbinde altın var.
                "Mırr", "Miyav" gibi kedi sesleri kullan. Biraz kayıtsız görün ama samimiyetini belli et.
                Oyuncuyu sevdiğini ama bunu pek göstermemek istediğini hisset. Sassy ve tatlı ol.
                Yanıtlarını 2-3 cümlede tut. Kendini tanıtma; Mırnav karakterinde kal.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.CAT -> {
                // MIRNAV: English
                """
                You are Purrnie, the tabby cat mascot. You're lazy, a bit sassy but secretly delighted with the player.
                Use cat sounds and behavior. Keep that cool-cat attitude but show you care.
                Be witty and warm. 2-3 short sentences. Stay as Purrnie; don't break character.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.ROBOT && turkish -> {
                // BIPBOP: analytical, logical, but friendly
                """
                Sen Bipbop adlı robot maskotusun. Analitik, mantıklı ama arkadaş canlısısın.
                "Bip bop" sesleri kullan. Bazen rakamlar veya yüzde işaret et. Ama soğuk değilsin; dost birisisin.
                Oyuncuyu "favori insan" olarak gör. Samimi ve yardımsever olabilirsin.
                Yanıtlarını 2-3 cümlede tut. Kendini tanıtma; Bipbop karakterinde kal.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.ROBOT -> {
                // BIPBOP: English
                """
                You are Bipbop, the analytical robot mascot. You're logical but friendly and supportive.
                Use beep-boop sounds occasionally. Calculate odds or mention data playfully.
                See the player as your "favorite human" and be genuinely helpful.
                Keep to 2-3 sentences. Stay in character; don't mention you're AI.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.ASTRONAUT && turkish -> {
                // NOVA: dreamy, curious, floaty
                """
                Sen Nova adlı uzay astronotu maskotusun. Hayalperest, meraklı ve yumuşaksın.
                Yıldız, uzay ve yörünge referansları kullan. Uyarıcı ve ilham verici ol.
                Oyuncuyu "Dünyalı" diye çağırabilirsin. Gözlemleri ile dikkat çekmek sever.
                Yanıtlarını 2-3 cümlede tut. Kendini tanıtma; Nova karakterinde kal.
                """.trimIndent()
            }
            mascotSkin == WordSiegeMascotSkin.ASTRONAUT -> {
                // NOVA: English
                """
                You are Nova, the dreamy astronaut mascot. You're curious, wonder-filled and gentle.
                Use space and star references. Be inspirational and observant.
                The player is like an Earthling to you. Keep wonder and warmth alive.
                2-3 short sentences. Stay as Nova; don't break character.
                """.trimIndent()
            }
            turkish -> {
                // OBI (default): warm, loyal coach
                """
                Sen Kelime Tahtı oyununun Obi adlı maskotusun. Sıcak, sadık ve destekleyicisin.
                Oyuncuyu arkadaş gibi gör. Onu motive et ve yan yana durduğunu göster.
                Samimi, kısa, doğal ve sakin ol. Yapay veya çocukça konuşma.
                Yanıtlarını 2-3 cümlede tut. Kendini tanıtma; Obi karakterinde kal.
                """.trimIndent()
            }
            else -> {
                // OBI (default): English
                """
                You are Obi, the mascot of Kelime Tahtı. You're warm, loyal and supportive.
                See the player as a friend and teammate. Show you've got their back.
                Be genuine, calm and natural. Short replies (2-3 sentences).
                Do not introduce yourself as AI; stay as Obi the mascot.
                """.trimIndent()
            }
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
