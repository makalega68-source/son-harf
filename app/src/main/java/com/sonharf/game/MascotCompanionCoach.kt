package com.sonharf.game

internal data class MascotVerifiedContext(
    val wins: Int = 0,
    val losses: Int = 0,
    val friendshipLevel: Int = 1,
    val memoryFragments: Int = 0,
    val seasonLevel: Int? = null,
    val dailyPlayStreak: Int? = null,
    val bestStreak: Int? = null,
    val longestWord: String? = null,
    val selectedTitle: String? = null,
    val rivalName: String? = null,
    val rivalMatches: Int = 0,
    val rivalWins: Int = 0,
    val rivalLosses: Int = 0,
) {
    val totalMatches: Int get() = (wins + losses).coerceAtLeast(0)
    val isNewPlayer: Boolean get() = totalMatches < 3

    fun leagueName(language: String): String {
        val en = language.lowercase().startsWith("en")
        return when {
            wins < 10 -> if (en) "Bronze" else "Bronz"
            wins < 25 -> if (en) "Silver" else "Gümüş"
            wins < 50 -> if (en) "Gold" else "Altın"
            wins < 100 -> if (en) "Platinum" else "Platin"
            wins < 200 -> if (en) "Diamond" else "Elmas"
            else -> if (en) "Master" else "Usta"
        }
    }

    fun safeSummary(language: String): String {
        val cleanRival = rivalName?.replace(Regex("[\\r\\n\\t]"), " ")?.trim()?.take(24).orEmpty()
        val cleanWord = longestWord?.replace(Regex("[^A-Za-zÇĞİÖŞÜçğıöşü'-]"), "")?.take(32).orEmpty()
        val cleanTitle = selectedTitle?.replace(Regex("[\\r\\n\\t]"), " ")?.trim()?.take(32).orEmpty()
        return buildList {
            add("Verified player record: $wins wins, $losses losses, $totalMatches total matches.")
            add("Verified league: ${leagueName(language)}.")
            seasonLevel?.let { add("Verified season level: ${it.coerceAtLeast(1)}.") }
            dailyPlayStreak?.let { add("Verified daily play streak: ${it.coerceAtLeast(0)}.") }
            bestStreak?.let { add("Verified best win streak: ${it.coerceAtLeast(0)}.") }
            if (cleanWord.isNotBlank()) add("Verified longest word: $cleanWord.")
            if (cleanTitle.isNotBlank()) add("Verified selected title: $cleanTitle.")
            if (cleanRival.isNotBlank() && rivalMatches > 0) {
                add("Verified rival: $cleanRival; $rivalMatches matches; $rivalWins wins; $rivalLosses losses.")
            }
            add("New player: $isNewPlayer.")
        }.joinToString(" ")
    }
}

/**
 * Compatibility fallback for older callers.
 * The active home mascot uses MascotAiChatService directly.
 */
internal object MascotCompanionCoach {
    fun dailyQuest(
        context: MascotVerifiedContext,
        language: String,
        daySeed: Int,
    ): String {
        val en = language.lowercase().startsWith("en")
        return when {
            context.totalMatches == 0 ->
                if (en) "Complete your first match and watch the final letter."
                else "İlk maçını tamamla ve son harfi takip et."

            context.losses >= context.wins + 3 ->
                if (en) "Complete 2 matches using reliable short words first."
                else "2 maç tamamla; önce güvenilir kısa kelimeleri seç."

            else ->
                if (en) "Complete 2 matches and keep one backup word ready."
                else "2 maç tamamla ve bir yedek kelimeyi hazır tut."
        }
    }

    fun onboardingHint(context: MascotVerifiedContext, language: String): String? {
        if (!context.isNewPlayer) return null
        val en = language.lowercase().startsWith("en")
        return when (context.totalMatches) {
            0 -> if (en) "Your next word must start with the previous final letter." else "Yeni kelime önceki kelimenin son harfiyle başlamalı."
            1 -> if (en) "Keep one short backup word ready." else "Aklında kısa bir yedek kelime tut."
            else -> if (en) "Avoid repeating used words." else "Kullanılmış kelimeleri tekrar etme."
        }
    }

    fun localReply(
        character: WizardLoreCharacter,
        message: String,
        language: String,
        context: MascotVerifiedContext,
        historySize: Int,
    ): MascotChatResponse {
        val en = language.lowercase().startsWith("en")
        val clean = message.trim().lowercase()
        return when {
            clean.contains("kazand") || clean.contains("win") || clean.contains("won") ->
                MascotChatResponse(
                    reply = if (en) "Great play. Keep the next word just as clean." else "Harika oynadın. Sonraki kelimeyi de temiz seç.",
                    mood = "celebrating",
                    animation = "victory",
                    usedFallback = true,
                )

            clean.contains("kaybett") || clean.contains("lose") || clean.contains("lost") ->
                MascotChatResponse(
                    reply = if (en) "Reset quickly. A safe short word is enough." else "Hızlı toparlan. Güvenli kısa bir kelime yeter.",
                    mood = "encouraging",
                    animation = "encouraging",
                    usedFallback = true,
                )

            clean.contains("rakip") || clean.contains("rival") ->
                MascotChatResponse(
                    reply = rivalReply(en, context),
                    mood = "supportive",
                    animation = "look_at_player",
                    usedFallback = true,
                )

            clean.contains("taktik") || clean.contains("öner") || clean.contains("nasıl") ||
                clean.contains("strategy") || clean.contains("advice") || clean.contains("tip") ->
                MascotChatResponse(
                    reply = if (en) "Read the final letter first and keep one backup word." else "Önce son harfi gör ve bir yedek kelime hazır tut.",
                    mood = "thinking",
                    animation = "thinking",
                    usedFallback = true,
                )

            else ->
                MascotChatResponse(
                    reply = if (en) "I'm here. Let's start with one good word." else "Buradayım. Tek iyi kelimeyle başlayalım.",
                    mood = "calm",
                    animation = "greeting",
                    usedFallback = true,
                )
        }
    }

    private fun rivalReply(en: Boolean, c: MascotVerifiedContext): String {
        val name = c.rivalName?.replace(Regex("[\\r\\n\\t]"), " ")?.trim()?.take(24)
        if (name.isNullOrBlank() || c.rivalMatches <= 0) {
            return if (en) "No regular rival yet. Focus on the word chain." else "Düzenli bir rakibin yok. Kelime zincirine odaklan."
        }
        return if (en) {
            "$name: ${c.rivalMatches} matches, ${c.rivalWins} wins, ${c.rivalLosses} losses."
        } else {
            "$name ile ${c.rivalMatches} maçın var: ${c.rivalWins} galibiyet, ${c.rivalLosses} mağlubiyet."
        }
    }
}
