package com.sonharf.game

import com.sonharf.game.data.GrowthDashboardDto

/**
 * Context-aware controller for the single active Son Harf mascot.
 *
 * AI is optional. The app always has a local, free fallback and caches the latest
 * home response so navigating between screens does not burn AI quota.
 */
internal object MascotHomeAiDirector {
    private const val CACHE_MS = 15 * 60 * 1000L

    var cachedReply: String = ""
        private set
    var cachedMotion: MascotMotion = MascotMotion.GREETING
        private set
    private var cachedAt: Long = 0L

    fun hasFreshCache(now: Long = System.currentTimeMillis()): Boolean =
        cachedReply.isNotBlank() && now - cachedAt < CACHE_MS

    fun cache(response: MascotChatResponse, now: Long = System.currentTimeMillis()) {
        cachedReply = response.reply.trim().replace(Regex("\\s+"), " ").take(110)
        cachedMotion = motionFor(response)
        cachedAt = now
    }

    fun motionFor(response: MascotChatResponse): MascotMotion {
        return when (response.animation.lowercase()) {
            "greeting" -> MascotMotion.GREETING
            "thinking" -> MascotMotion.THINKING
            "victory" -> MascotMotion.VICTORY
            "encouraging" -> MascotMotion.LOOK_AT_PLAYER
            "alert" -> MascotMotion.CRITICAL
            "look_at_player" -> MascotMotion.LOOK_AT_PLAYER
            "walk" -> MascotMotion.WALK
            "run" -> MascotMotion.RUN
            else -> when (response.mood.lowercase()) {
                "celebrating", "happy" -> MascotMotion.VICTORY
                "thinking", "curious" -> MascotMotion.THINKING
                "encouraging", "supportive" -> MascotMotion.LOOK_AT_PLAYER
                else -> MascotMotion.IDLE
            }
        }
    }

    fun homeRequest(
        playerName: String?,
        growth: GrowthDashboardDto?,
        language: String,
    ): MascotChatRequest {
        val en = language.lowercase().startsWith("en")
        val context = buildList {
            growth?.let {
                add("Verified level: ${it.level}.")
                add("Verified wins: ${it.wins}.")
                add("Verified losses: ${it.losses}.")
                add("Verified current win streak: ${it.currentWinStreak}.")
                add("Verified best win streak: ${it.bestStreak}.")
                add("Verified matches today: ${it.matchesToday}.")
                add("Verified league: ${it.leagueName}.")
            }
            add(
                if (en)
                    "This is the home screen. Give one short useful or playful line before the player starts a word game."
                else
                    "Burası ana menü. Oyuncu kelime oyununa başlamadan önce kısa, faydalı veya eğlenceli tek cümle söyle."
            )
            add(
                if (en)
                    "No lore, no story, no sales, no guilt. Maximum 10 words when possible."
                else
                    "Hikâye, evren, satış ve suçluluk dili kullanma. Mümkünse en fazla 10 kelime."
            )
        }.joinToString(" ")

        return MascotChatRequest(
            message = if (en)
                "Say a context-aware home greeting now."
            else
                "Şimdi bağlama uygun kısa bir ana menü cümlesi söyle.",
            history = emptyList(),
            language = language,
            playerName = playerName,
            companionName = if (en) "Companion" else "Dost",
            gameContext = context,
            mascotId = MascotCatalog.CHIBI_WIZARD_ID,
            mascotTitle = if (en) "Single game companion" else "Tek oyun yardımcısı",
            mascotPersonality = if (en)
                "Playful, concise, supportive, competitive but never aggressive."
            else
                "Eğlenceli, kısa, destekleyici; rekabetçi ama asla saldırgan değil.",
            loreContext = null,
            playerWins = growth?.wins,
            playerLosses = growth?.losses,
            bestStreak = growth?.bestStreak,
        )
    }
}
