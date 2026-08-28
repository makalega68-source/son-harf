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
            wins < 10 -> if (en) "Bronze Seal" else "Bronz Mührü"
            wins < 25 -> if (en) "Silver Seal" else "Gümüş Mührü"
            wins < 50 -> if (en) "Gold Seal" else "Altın Mührü"
            wins < 100 -> if (en) "Platinum Seal" else "Platin Mührü"
            wins < 200 -> if (en) "Diamond Seal" else "Elmas Mührü"
            else -> if (en) "Master Seal" else "Usta Mührü"
        }
    }

    fun safeSummary(language: String): String {
        val cleanRival = rivalName?.replace(Regex("[\\r\\n\\t]"), " ")?.trim()?.take(24).orEmpty()
        val cleanWord = longestWord?.replace(Regex("[^A-Za-zÇĞİÖŞÜçğıöşü'-]"), "")?.take(32).orEmpty()
        val cleanTitle = selectedTitle?.replace(Regex("[\\r\\n\\t]"), " ")?.trim()?.take(32).orEmpty()
        return buildList {
            add("Verified player record: $wins wins, $losses losses, $totalMatches total matches.")
            add("Verified league: ${leagueName(language)}.")
            add("Verified friendship level: ${friendshipLevel.coerceAtLeast(1)}.")
            add("Verified memory fragments: ${memoryFragments.coerceIn(0, 120)}/120.")
            seasonLevel?.let { add("Verified season level: ${it.coerceAtLeast(1)}.") }
            dailyPlayStreak?.let { add("Verified daily play streak: ${it.coerceAtLeast(0)}.") }
            bestStreak?.let { add("Verified best win streak: ${it.coerceAtLeast(0)}.") }
            if (cleanWord.isNotBlank()) add("Verified longest word: $cleanWord.")
            if (cleanTitle.isNotBlank()) add("Verified selected title: $cleanTitle.")
            if (cleanRival.isNotBlank() && rivalMatches > 0) {
                add("Verified arch rival: $cleanRival; $rivalMatches matches; $rivalWins wins; $rivalLosses losses against them.")
            }
            add("New player: $isNewPlayer.")
        }.joinToString(" ")
    }
}

internal object MascotCompanionCoach {
    fun dailyQuest(
        context: MascotVerifiedContext,
        language: String,
        daySeed: Int,
    ): String {
        val en = language.lowercase().startsWith("en")
        return when {
            context.totalMatches == 0 ->
                if (en) "Complete your first duel. Watch the final letter before choosing your word."
                else "İlk düellonu tamamla. Kelimeni seçmeden önce son harfi kontrol et."

            context.isNewPlayer ->
                if (en) "Complete 1 duel and use at least one word you know is safe and valid."
                else "1 düello tamamla ve en az bir kez geçerli olduğundan emin olduğun güvenli bir kelime kullan."

            context.friendshipLevel < 3 ->
                if (en) "Complete 1 duel and make one companion interaction in the Seal Room."
                else "1 düello tamamla ve Mühür Odası'nda yoldaşınla bir bağ etkileşimi yap."

            context.losses >= context.wins + 3 ->
                if (en) "Complete 2 duels. Favor reliable short words over risky long ones."
                else "2 düello tamamla. Riskli uzun kelimeler yerine güvenilir kısa kelimeleri tercih et."

            context.rivalMatches >= 3 && context.rivalLosses > context.rivalWins ->
                if (en) "Complete 2 duels and practice keeping two safe options ready for the next final letter."
                else "2 düello tamamla ve sıradaki son harf için iki güvenli kelimeyi hazır tutmayı dene."

            else -> when (Math.floorMod(daySeed + context.wins + context.friendshipLevel, 3)) {
                0 -> if (en) "Win 1 duel and strengthen today's seal echo." else "1 düello kazan ve bugünkü mühür yankısını güçlendir."
                1 -> if (en) "Complete 3 duels without rushing the final five seconds." else "Son 5 saniyeye kalmadan 3 düelloyu tamamlamayı dene."
                else -> if (en) "Complete 2 duels and protect your Word Weave rhythm." else "2 düello tamamla ve Söz Dokusu ritmini koru."
            }
        }
    }

    fun onboardingHint(context: MascotVerifiedContext, language: String): String? {
        if (!context.isNewPlayer) return null
        val en = language.lowercase().startsWith("en")
        return when (context.totalMatches) {
            0 -> if (en) {
                "First seal: the next word must begin with the previous word's final letter. Validity matters more than speed."
            } else {
                "İlk mühür: yeni kelime, önceki kelimenin son harfiyle başlamalı. Hızdan önce geçerliliği düşün."
            }
            1 -> if (en) {
                "Second seal: keep one short backup word in mind before the timer becomes critical."
            } else {
                "İkinci mühür: süre kritik hale gelmeden önce aklında kısa bir yedek kelime tut."
            }
            else -> if (en) {
                "Third seal: avoid repeating used words and watch the chain before you commit."
            } else {
                "Üçüncü mühür: kullanılmış kelimeleri tekrar etme ve göndermeden önce zinciri kontrol et."
            }
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
        val intent = when {
            clean.contains("varkhor") -> "varkhor"
            clean.contains("hik") || clean.contains("geçmiş") || clean.contains("story") || clean.contains("past") -> "story"
            clean.contains("rakip") || clean.contains("rival") -> "rival"
            clean.contains("taktik") || clean.contains("öner") || clean.contains("nasıl") ||
                clean.contains("strategy") || clean.contains("advice") || clean.contains("tip") -> "advice"
            clean.contains("kaybett") || clean.contains("yenild") || clean.contains("lost") || clean.contains("lose") -> "loss"
            clean.contains("kazand") || clean.contains("yendim") || clean.contains("won") || clean.contains("win") -> "win"
            else -> "default"
        }

        val reply = when (intent) {
            "varkhor" -> personaVarkhor(character.key, en)
            "story" -> LetharaLore.randomWhisper(character, if (en) "en" else "tr", historySize + clean.length)
            "advice" -> coachingReply(character.key, en, context)
            "loss" -> personaLoss(character.key, en)
            "win" -> personaWin(character.key, en)
            "rival" -> rivalReply(character.key, en, context)
            else -> personaDefault(character.key, character.name, en)
        }

        val mood = when (intent) {
            "win" -> "celebrating"
            "loss" -> "encouraging"
            "story", "varkhor" -> "curious"
            "advice", "rival" -> "supportive"
            else -> if (character.key == "mivo") "happy" else "calm"
        }
        return MascotChatResponse(
            reply = reply,
            mood = mood,
            animation = "idle_breathe",
            memoryNote = "",
            usedFallback = true,
        )
    }

    private fun coachingReply(key: String, en: Boolean, c: MascotVerifiedContext): String {
        val core = when {
            c.isNewPlayer -> if (en) {
                "Read the final letter first; choose a valid word you trust before chasing speed."
            } else {
                "Önce son harfi gör; hız aramadan önce geçerli olduğuna güvendiğin kelimeyi seç."
            }
            c.losses >= c.wins + 3 -> if (en) {
                "Your record says consistency matters more than risk right now. Keep two short backup words ready."
            } else {
                "Kayıtların şu an riskten çok istikrarın önemli olduğunu söylüyor. İki kısa yedek kelimeyi hazır tut."
            }
            c.bestStreak != null && c.bestStreak >= 3 -> if (en) {
                "You have already built a ${c.bestStreak}-win streak before. Protect the chain; do not force a spectacular word."
            } else {
                "Daha önce ${c.bestStreak} maçlık seri kurmuşsun. Zinciri koru; gösterişli kelimeyi zorlaman gerekmiyor."
            }
            else -> if (en) {
                "Keep one safe word and one ambitious word ready for the next final letter. Use the safe one when time is low."
            } else {
                "Sıradaki son harf için bir güvenli, bir iddialı kelime hazır tut. Süre azalınca güvenli olanı kullan."
            }
        }
        return personaPrefix(key, en) + core
    }

    private fun rivalReply(key: String, en: Boolean, c: MascotVerifiedContext): String {
        val name = c.rivalName?.replace(Regex("[\\r\\n\\t]"), " ")?.trim()?.take(24)
        if (name.isNullOrBlank() || c.rivalMatches <= 0) {
            return personaPrefix(key, en) + if (en) {
                "No arch rival is written into your seal yet. Repeated duels will reveal one."
            } else {
                "Mührüne kazınmış bir ezeli rakip henüz yok. Tekrarlanan düellolar zamanla birini ortaya çıkarır."
            }
        }
        return personaPrefix(key, en) + if (en) {
            "$name appears in ${c.rivalMatches} verified duels: ${c.rivalWins} wins, ${c.rivalLosses} losses. Study the chain, not the name."
        } else {
            "$name ile doğrulanmış ${c.rivalMatches} düellon var: ${c.rivalWins} galibiyet, ${c.rivalLosses} mağlubiyet. İsme değil zincire odaklan."
        }
    }

    private fun personaPrefix(key: String, en: Boolean): String = when (key) {
        "lyra" -> if (en) "A small star flickers. " else "Küçük bir yıldız kıvılcımlanıyor. "
        "kael" -> if (en) "The seal-shield steadies. " else "Mühür kalkanı sabitleniyor. "
        "neris" -> if (en) "A shadow settles beside the words. " else "Kelimelerin yanına bir gölge yerleşiyor. "
        "ryvan" -> if (en) "A spark runs across the air. " else "Havada bir kıvılcım dolaşıyor. "
        "mivo" -> if (en) "A mischievous rune hops once. " else "Yaramaz bir rün bir kez zıplıyor. "
        "selen" -> if (en) "The quiet seal answers softly. " else "Sessiz mühür usulca karşılık veriyor. "
        else -> ""
    }

    private fun personaVarkhor(key: String, en: Boolean): String = when (key) {
        "lyra" -> if (en) "Varkhor's name dims a star I almost remember. Violet fire... then silence." else "Varkhor'un adı neredeyse hatırladığım bir yıldızı söndürüyor. Mor ateş... sonra sessizlik."
        "kael" -> if (en) "Varkhor. My shield remembers that name before I do." else "Varkhor. Kalkanım o adı benden önce hatırlıyor."
        "neris" -> if (en) "Shadows do not hide Varkhor's name. They hide what came after it." else "Gölgeler Varkhor'un adını saklamıyor. Ondan sonra olanı saklıyor."
        "ryvan" -> if (en) "That name tastes like thunder held too long." else "O isim fazla uzun tutulmuş bir gök gürültüsü gibi."
        "mivo" -> if (en) "I used to joke about that name. I do not remember why no one laughed." else "O isimle eskiden dalga geçerdim. Kimsenin neden gülmediğini hatırlamıyorum."
        "selen" -> if (en) "I hear Varkhor in a future that should not exist." else "Varkhor'u var olmaması gereken bir geleceğin içinde duyuyorum."
        else -> if (en) "That name scratches at an old seal." else "O isim eski bir mührü tırmalıyor."
    }

    private fun personaWin(key: String, en: Boolean): String = when (key) {
        "lyra" -> if (en) "The stars noticed that one. Keep the next seal just as clean." else "Yıldızlar bunu fark etti. Sonraki mührü de böyle temiz tut."
        "kael" -> if (en) "A solid victory. Protect the chain before chasing glory." else "Sağlam bir zafer. Şöhretten önce zinciri koru."
        "neris" -> if (en) "Good. Quiet wins leave fewer openings." else "İyi. Sessiz kazanılan zaferler daha az açık bırakır."
        "ryvan" -> if (en) "Now that was a strike! Do not let the next storm rush you." else "İşte bu bir darbeydi! Sonraki fırtınanın seni acele ettirmesine izin verme."
        "mivo" -> if (en) "Ha! Even the runes are pretending not to smile." else "Hah! Rünler bile gülmüyormuş gibi yapıyor."
        "selen" -> if (en) "I saw a path where that word failed. You chose the better one." else "O kelimenin düştüğü bir yol görmüştüm. Sen daha iyisini seçtin."
        else -> if (en) "The seal sparked." else "Mühür kıvılcımlandı."
    }

    private fun personaLoss(key: String, en: Boolean): String = when (key) {
        "lyra" -> if (en) "One star went dark, not the sky. We read the next final letter better." else "Bir yıldız söndü, gökyüzü değil. Sonraki son harfi daha iyi okuruz."
        "kael" -> if (en) "A shield can crack without falling. Reset and guard the next chain." else "Bir kalkan düşmeden çatlayabilir. Yeniden kurul ve sonraki zinciri koru."
        "neris" -> if (en) "Losses leave useful shadows. Look at where the chain narrowed." else "Mağlubiyetler faydalı gölgeler bırakır. Zincirin nerede daraldığına bak."
        "ryvan" -> if (en) "Annoying. Good. Use the anger to sharpen, not rush." else "Sinir bozucu. Güzel. Öfkeyi aceleye değil keskinliğe çevir."
        "mivo" -> if (en) "That seal tripped over its own robe. We try again." else "O mühür kendi cübbesine takıldı. Bir daha deneriz."
        "selen" -> if (en) "I saw this loss among many futures. It was never the last one." else "Bu mağlubiyeti birçok geleceğin arasında görmüştüm. Hiçbirinde son değildi."
        else -> if (en) "The Word Weave bent, not broke." else "Söz Dokusu büküldü, kırılmadı."
    }

    private fun personaDefault(key: String, name: String, en: Boolean): String = when (key) {
        "lyra" -> if (en) "A star answers. I am listening, Remembrancer." else "Bir yıldız karşılık veriyor. Dinliyorum, Hatırlatıcı."
        "kael" -> if (en) "Speak. I will hold the seal while you do." else "Söyle. Sen konuşurken mührü ben tutarım."
        "neris" -> if (en) "Speak softly. Shadows remember more than they admit." else "Sessiz söyle. Gölgeler kabul ettiklerinden fazlasını hatırlar."
        "ryvan" -> if (en) "Say it. The storm is listening too." else "Söyle. Fırtına da dinliyor."
        "mivo" -> if (en) "Go on. If the rune explodes, we blame Varkhor." else "Devam et. Rün patlarsa Varkhor'u suçlarız."
        "selen" -> if (en) "I am listening. Some answers arrive before the question ends." else "Dinliyorum. Bazı cevaplar soru bitmeden gelir."
        else -> if (en) "$name listens through the Word Weave." else "$name Söz Dokusu'nun içinden seni dinliyor."
    }
}
