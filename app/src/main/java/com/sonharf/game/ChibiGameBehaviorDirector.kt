package com.sonharf.game

/**
 * Event-driven Chibi behavior planner inspired by the interaction pattern used by strong
 * learning-game mascots: react to meaningful player events, vary the choreography, avoid
 * back-to-back repetition, escalate important moments, then return to neutral.
 *
 * This is Son Harf's own implementation and uses only Chibi's existing animation clips.
 */
internal enum class ChibiGameEvent {
    MATCH_START,
    AMBIENT,
    PLAYER_TURN,
    RIVAL_TURN,
    PLAYER_WORD,
    PLAYER_LONG_WORD,
    PLAYER_STREAK,
    RIVAL_WORD,
    TIME_WARNING,
    TIME_CRITICAL,
    WIN,
    LOSS,
}

internal enum class ChibiEmotion {
    CALM,
    CURIOUS,
    FOCUSED,
    HAPPY,
    EXCITED,
    URGENT,
    SUPPORTIVE,
}

internal data class ChibiBehaviorContext(
    val playerName: String? = null,
    val word: String? = null,
    val streak: Int = 0,
    val seconds: Int? = null,
)

internal data class ChibiBehaviorStep(
    val motion: MascotMotion,
    val durationMs: Long,
)

internal data class ChibiBehaviorPlan(
    val id: String,
    val event: ChibiGameEvent,
    val emotion: ChibiEmotion,
    val priority: Int,
    val messageTr: String,
    val messageEn: String,
    val steps: List<ChibiBehaviorStep>,
) {
    fun message(language: String): String =
        if (language.lowercase().startsWith("en")) messageEn else messageTr
}

/**
 * Per-match planner. Keep one instance for one room.
 *
 * The director deliberately does not choose the same choreography twice in a row and applies
 * event-level cooldowns so Chibi reacts like a character instead of an animation loop.
 */
internal class ChibiGameBehaviorDirector(
    private val language: String,
) {
    private val lastEventAt = mutableMapOf<ChibiGameEvent, Long>()
    private val recentPlanIds = mutableListOf<String>()
    private var cursor = 0

    fun plan(
        event: ChibiGameEvent,
        context: ChibiBehaviorContext = ChibiBehaviorContext(),
        nowMs: Long = System.currentTimeMillis(),
    ): ChibiBehaviorPlan? {
        val cooldown = cooldownMs(event)
        val previousAt = lastEventAt[event]
        if (
            previousAt != null &&
            nowMs - previousAt < cooldown &&
            event !in setOf(ChibiGameEvent.WIN, ChibiGameEvent.LOSS, ChibiGameEvent.TIME_CRITICAL)
        ) {
            return null
        }

        val candidates = variants(event, context)
        if (candidates.isEmpty()) return null

        val recent = recentPlanIds.takeLast(3).toSet()
        val nonRepeated = candidates.filterNot { it.id in recent }
        val pool = nonRepeated.ifEmpty { candidates }
        val selected = pool[cursor % pool.size]
        cursor += 1

        lastEventAt[event] = nowMs
        recentPlanIds += selected.id
        while (recentPlanIds.size > 4) recentPlanIds.removeAt(0)
        return selected
    }

    private fun cooldownMs(event: ChibiGameEvent): Long = when (event) {
        ChibiGameEvent.AMBIENT -> 7_500L
        ChibiGameEvent.PLAYER_TURN,
        ChibiGameEvent.RIVAL_TURN -> 1_800L
        ChibiGameEvent.PLAYER_WORD,
        ChibiGameEvent.RIVAL_WORD -> 1_200L
        ChibiGameEvent.PLAYER_LONG_WORD,
        ChibiGameEvent.PLAYER_STREAK -> 2_000L
        ChibiGameEvent.TIME_WARNING -> 8_000L
        ChibiGameEvent.TIME_CRITICAL -> 2_000L
        ChibiGameEvent.MATCH_START -> 10_000L
        ChibiGameEvent.WIN,
        ChibiGameEvent.LOSS -> 0L
    }

    private fun variants(
        event: ChibiGameEvent,
        context: ChibiBehaviorContext,
    ): List<ChibiBehaviorPlan> {
        val name = context.playerName?.trim()?.takeIf { it.isNotBlank() }
        val shortNameTr = name?.let { "$it, " }.orEmpty()
        val shortNameEn = name?.let { "$it, " }.orEmpty()
        val word = context.word?.trim()?.takeIf { it.isNotBlank() }
        val wordTr = word?.let { "“$it” iyi seçim!" } ?: "Güzel seçim!"
        val wordEn = word?.let { "“$it” was a good pick!" } ?: "Nice choice!"
        val streak = context.streak.coerceAtLeast(0)

        fun p(
            id: String,
            emotion: ChibiEmotion,
            priority: Int,
            tr: String,
            en: String,
            vararg steps: Pair<MascotMotion, Long>,
        ) = ChibiBehaviorPlan(
            id = id,
            event = event,
            emotion = emotion,
            priority = priority,
            messageTr = tr,
            messageEn = en,
            steps = steps.map { ChibiBehaviorStep(it.first, it.second) },
        )

        return when (event) {
            ChibiGameEvent.MATCH_START -> listOf(
                p(
                    "start-look",
                    ChibiEmotion.CURIOUS,
                    30,
                    "${shortNameTr}Chibi oyunda. Zinciri başlatalım.",
                    "${shortNameEn}Chibi's in. Let's start the chain.",
                    MascotMotion.GREETING to 750L,
                    MascotMotion.LOOK_AT_PLAYER to 750L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "start-run",
                    ChibiEmotion.HAPPY,
                    30,
                    "Tam zamanında geldik. İlk kelimeyi yakala.",
                    "Perfect timing. Catch the first word.",
                    MascotMotion.RUN to 650L,
                    MascotMotion.TURN_RIGHT to 550L,
                    MascotMotion.LOOK_AT_PLAYER to 650L,
                    MascotMotion.IDLE to 250L,
                ),
            )

            ChibiGameEvent.AMBIENT -> listOf(
                p(
                    "ambient-left",
                    ChibiEmotion.CALM,
                    5,
                    "",
                    "",
                    MascotMotion.TURN_LEFT to 650L,
                    MascotMotion.IDLE to 300L,
                ),
                p(
                    "ambient-right",
                    ChibiEmotion.CURIOUS,
                    5,
                    "",
                    "",
                    MascotMotion.TURN_RIGHT to 650L,
                    MascotMotion.IDLE to 300L,
                ),
                p(
                    "ambient-walk",
                    ChibiEmotion.CALM,
                    5,
                    "",
                    "",
                    MascotMotion.WALK to 800L,
                    MascotMotion.TURN_LEFT to 500L,
                    MascotMotion.IDLE to 300L,
                ),
            )

            ChibiGameEvent.PLAYER_TURN -> listOf(
                p(
                    "turn-focus",
                    ChibiEmotion.FOCUSED,
                    35,
                    "${shortNameTr}hamle sende. Son harfi yakala.",
                    "${shortNameEn}your move. Catch the final letter.",
                    MascotMotion.LOOK_AT_PLAYER to 450L,
                    MascotMotion.TURN_RIGHT to 400L,
                    MascotMotion.THINKING to 850L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "turn-think",
                    ChibiEmotion.FOCUSED,
                    35,
                    "Bir saniye… son harften başlayalım.",
                    "One second… start from the final letter.",
                    MascotMotion.TURN_RIGHT to 500L,
                    MascotMotion.THINKING to 950L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "turn-ready",
                    ChibiEmotion.CURIOUS,
                    35,
                    "Gözüm zincirde. Sıra sende.",
                    "Eyes on the chain. Your turn.",
                    MascotMotion.TURN_LEFT to 450L,
                    MascotMotion.GREETING to 450L,
                    MascotMotion.LOOK_AT_PLAYER to 700L,
                    MascotMotion.IDLE to 250L,
                ),
            )

            ChibiGameEvent.RIVAL_TURN -> listOf(
                p(
                    "rival-watch-left",
                    ChibiEmotion.CURIOUS,
                    20,
                    "Rakibi izliyorum…",
                    "Watching the rival…",
                    MascotMotion.TURN_LEFT to 700L,
                    MascotMotion.IDLE to 350L,
                ),
                p(
                    "rival-watch-right",
                    ChibiEmotion.CALM,
                    20,
                    "Sıra rakipte. Ben zinciri takip ediyorum.",
                    "Rival's turn. I'm tracking the chain.",
                    MascotMotion.TURN_RIGHT to 650L,
                    MascotMotion.WALK to 550L,
                    MascotMotion.IDLE to 300L,
                ),
            )

            ChibiGameEvent.PLAYER_WORD -> listOf(
                p(
                    "word-nod",
                    ChibiEmotion.HAPPY,
                    45,
                    wordTr,
                    wordEn,
                    MascotMotion.GREETING to 650L,
                    MascotMotion.TURN_LEFT to 450L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "word-hop",
                    ChibiEmotion.HAPPY,
                    45,
                    "Temiz hamle. Devam.",
                    "Clean move. Keep going.",
                    MascotMotion.RUN to 500L,
                    MascotMotion.LOOK_AT_PLAYER to 600L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "word-walk",
                    ChibiEmotion.HAPPY,
                    45,
                    "Bu oldu! Zincir yaşıyor.",
                    "That works! The chain is alive.",
                    MascotMotion.WALK to 550L,
                    MascotMotion.GREETING to 550L,
                    MascotMotion.IDLE to 250L,
                ),
            )

            ChibiGameEvent.PLAYER_LONG_WORD -> listOf(
                p(
                    "long-special",
                    ChibiEmotion.EXCITED,
                    62,
                    "O kelime güçlüydü! İşte bunu sevdim.",
                    "That was a strong word! I liked that.",
                    MascotMotion.RUN to 550L,
                    MascotMotion.GREETING to 600L,
                    MascotMotion.LOOK_AT_PLAYER to 650L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "long-spin",
                    ChibiEmotion.EXCITED,
                    62,
                    "Uzun ve temiz. Güzel oynadın!",
                    "Long and clean. Nice play!",
                    MascotMotion.TURN_RIGHT to 500L,
                    MascotMotion.RUN to 550L,
                    MascotMotion.GREETING to 600L,
                    MascotMotion.IDLE to 250L,
                ),
            )

            ChibiGameEvent.PLAYER_STREAK -> listOf(
                p(
                    "streak-burst",
                    ChibiEmotion.EXCITED,
                    72,
                    if (streak > 0) "$streak temiz hamle! Ritmi bozma." else "Ritmi yakaladın! Bozma.",
                    if (streak > 0) "$streak clean moves! Keep the rhythm." else "You've got the rhythm! Keep it.",
                    MascotMotion.RUN to 550L,
                    MascotMotion.TURN_LEFT to 450L,
                    MascotMotion.GREETING to 650L,
                    MascotMotion.LOOK_AT_PLAYER to 550L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "streak-celebrate",
                    ChibiEmotion.EXCITED,
                    72,
                    "Seri güzel gidiyor. Chibi onayladı.",
                    "Nice streak. Chibi approves.",
                    MascotMotion.TURN_RIGHT to 500L,
                    MascotMotion.RUN to 550L,
                    MascotMotion.GREETING to 650L,
                    MascotMotion.IDLE to 250L,
                ),
            )

            ChibiGameEvent.RIVAL_WORD -> listOf(
                p(
                    "rival-word",
                    ChibiEmotion.FOCUSED,
                    28,
                    "Rakip oynadı. Şimdi cevabı bulalım.",
                    "Rival played. Let's find the answer.",
                    MascotMotion.TURN_LEFT to 450L,
                    MascotMotion.TURN_RIGHT to 450L,
                    MascotMotion.THINKING to 750L,
                    MascotMotion.IDLE to 250L,
                ),
                p(
                    "rival-word-ready",
                    ChibiEmotion.CURIOUS,
                    28,
                    "Gördüm. Sıra bize geliyor.",
                    "Saw it. Our turn is coming.",
                    MascotMotion.TURN_RIGHT to 500L,
                    MascotMotion.LOOK_AT_PLAYER to 650L,
                    MascotMotion.IDLE to 250L,
                ),
            )

            ChibiGameEvent.TIME_WARNING -> listOf(
                p(
                    "time-ten",
                    ChibiEmotion.URGENT,
                    78,
                    "On saniye civarı. Sakin kal, güvenli kelime.",
                    "About ten seconds. Stay calm, safe word.",
                    MascotMotion.THINKING to 550L,
                    MascotMotion.TURN_RIGHT to 400L,
                    MascotMotion.LOOK_AT_PLAYER to 500L,
                    MascotMotion.IDLE to 200L,
                ),
                p(
                    "time-watch",
                    ChibiEmotion.URGENT,
                    78,
                    "Saat hızlandı. İlk sağlam kelimeyi seç.",
                    "Clock's moving. Pick the first solid word.",
                    MascotMotion.TURN_RIGHT to 400L,
                    MascotMotion.THINKING to 700L,
                    MascotMotion.IDLE to 200L,
                ),
            )

            ChibiGameEvent.TIME_CRITICAL -> listOf(
                p(
                    "critical-attack",
                    ChibiEmotion.URGENT,
                    92,
                    "Son saniyeler! Güvenli kelimeyi gönder.",
                    "Final seconds! Send the safe word.",
                    MascotMotion.CRITICAL to 900L,
                    MascotMotion.LOOK_AT_PLAYER to 450L,
                    MascotMotion.IDLE to 200L,
                ),
                p(
                    "critical-run",
                    ChibiEmotion.URGENT,
                    92,
                    "Hemen! Bildiğin kelimeyi kullan.",
                    "Now! Use the word you know.",
                    MascotMotion.RUN to 450L,
                    MascotMotion.CRITICAL to 850L,
                    MascotMotion.IDLE to 200L,
                ),
            )

            ChibiGameEvent.WIN -> listOf(
                p(
                    "win-flight-a",
                    ChibiEmotion.EXCITED,
                    100,
                    "Kazandık! Chibi tur atıyor!",
                    "We won! Chibi's taking a victory lap!",
                    MascotMotion.RUN to 550L,
                    MascotMotion.VICTORY to 1_250L,
                    MascotMotion.TURN_RIGHT to 500L,
                    MascotMotion.GREETING to 600L,
                    MascotMotion.LOOK_AT_PLAYER to 600L,
                    MascotMotion.IDLE to 300L,
                ),
                p(
                    "win-flight-b",
                    ChibiEmotion.EXCITED,
                    100,
                    "İşte bu! Bu galibiyet kutlanır.",
                    "That's it! This win deserves a celebration.",
                    MascotMotion.TURN_LEFT to 450L,
                    MascotMotion.RUN to 550L,
                    MascotMotion.VICTORY to 1_200L,
                    MascotMotion.GREETING to 600L,
                    MascotMotion.IDLE to 300L,
                ),
                p(
                    "win-flight-c",
                    ChibiEmotion.EXCITED,
                    100,
                    "Harika maç! Bir tur da Chibi'den.",
                    "Great match! One victory lap from Chibi.",
                    MascotMotion.RUN to 500L,
                    MascotMotion.TURN_RIGHT to 450L,
                    MascotMotion.VICTORY to 1_150L,
                    MascotMotion.LOOK_AT_PLAYER to 600L,
                    MascotMotion.IDLE to 300L,
                ),
            )

            ChibiGameEvent.LOSS -> listOf(
                p(
                    "loss-recover-a",
                    ChibiEmotion.SUPPORTIVE,
                    100,
                    "${shortNameTr}bu maç bitti. Rövanşta toparlarız.",
                    "${shortNameEn}this one's done. We'll reset for the rematch.",
                    MascotMotion.DEFEAT to 900L,
                    MascotMotion.TURN_RIGHT to 500L,
                    MascotMotion.LOOK_AT_PLAYER to 750L,
                    MascotMotion.IDLE to 300L,
                ),
                p(
                    "loss-recover-b",
                    ChibiEmotion.SUPPORTIVE,
                    100,
                    "Olmadı. Bir kez üzülüp hemen toparlanıyorum.",
                    "Not this one. One sad beat, then we reset.",
                    MascotMotion.DEFEAT to 850L,
                    MascotMotion.TURN_LEFT to 450L,
                    MascotMotion.GREETING to 450L,
                    MascotMotion.LOOK_AT_PLAYER to 650L,
                    MascotMotion.IDLE to 300L,
                ),
            )
        }
    }
}
