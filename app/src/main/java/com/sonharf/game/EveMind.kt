package com.sonharf.game

import kotlin.random.Random

/**
 * Eve's mind, kept free of Android so it can be unit-tested: her friendship with the player,
 * what she remembers of their games, and how she picks what to say and do next. It runs on the
 * device only (no cloud AI); [EveMemory] is backed by SharedPreferences in the app.
 */
internal interface EveMemory {
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
}

/** What Eve can talk about. */
internal enum class EveTopic {
    HELLO_NEW, HELLO_FRIEND, HELLO_BEST, MISSED, BOND_FRIEND, BOND_BEST,
    MORNING, NOON, EVENING, NIGHT, LATE, THRONE_HOUR,
    TAP_NEW, TAP_FRIEND, TAP_BEST, TICKLE, FLEE, MOVED, WAKE, POUNCE,
    TIP, WORD_FACT, IDLE, OWNED_STORY,
    WIN_SEEN, LOSS_SEEN, STREAK, LONGEST,
    THRONE_TOP, THRONE_CLOSE, THRONE_NONE, SUNDAY, MONDAY, TOURNAMENT_LIVE,
}

/** The moment Eve is in: clock, the week's throne race and the player's game memory. */
internal data class EveSituation(
    val hour: Int,
    /** 1 = Monday … 7 = Sunday. */
    val dayOfWeek: Int,
    /** This week's throne rank, 0 when the player has not played yet. */
    val throneRank: Int = 0,
    val tournamentLive: Boolean = false,
    /** Server competition time is Europe/Istanbul, regardless of the device clock zone. */
    val throneHourActive: Boolean = false,
    val winStreak: Int = 0,
    val longestWord: String = "",
)

/** A greeting decision: what to say, and whether the friendship just reached a new level. */
internal data class EveMeeting(val meetings: Int, val daysAway: Long, val levelUp: Int?)

internal class EveMind(private val memory: EveMemory, private val random: Random = Random.Default) {

    // ---- friendship ------------------------------------------------------------------------

    val points: Int get() = memory.getInt(KEY_POINTS, 0)

    /** 0 = just met, 1 = friends, 2 = best friends. */
    val level: Int get() = levelOf(points)

    /** Registers today's first meeting (once a day); returns how long the player was away. */
    fun meet(today: Long): EveMeeting {
        val last = memory.getLong(KEY_LAST_DAY, -1L)
        if (last == today) return EveMeeting(memory.getInt(KEY_MEETINGS, 0), 0L, null)
        val meetings = memory.getInt(KEY_MEETINGS, 0) + 1
        memory.putInt(KEY_MEETINGS, meetings)
        memory.putLong(KEY_LAST_DAY, today)
        memory.putInt(KEY_TAPS_TODAY, 0)
        val levelUp = addPoints(4)
        return EveMeeting(meetings, if (last < 0L) 0L else today - last, levelUp)
    }

    /** A friendly tap; only the first few each day count towards the friendship. */
    fun pet(): Int? {
        val taps = memory.getInt(KEY_TAPS_TODAY, 0)
        memory.putInt(KEY_TAPS_TODAY, taps + 1)
        return if (taps < DAILY_TAP_POINTS) addPoints(1) else null
    }

    /** Adds friendship points; returns the new level when it just went up. */
    fun addPoints(amount: Int): Int? {
        val before = level
        memory.putInt(KEY_POINTS, (points + amount).coerceIn(0, MAX_POINTS))
        return level.takeIf { it > before }
    }

    // ---- game memory -----------------------------------------------------------------------

    /**
     * Compares the shared game memory (wins and streak, recorded by the in-game mascot) with what
     * Eve saw last time, and returns what happened since: a win, a loss, or nothing.
     */
    fun gameNews(wins: Int, winStreak: Int): EveTopic? {
        val seenWins = memory.getInt(KEY_SEEN_WINS, -1)
        val seenStreak = memory.getInt(KEY_SEEN_STREAK, -1)
        memory.putInt(KEY_SEEN_WINS, wins)
        memory.putInt(KEY_SEEN_STREAK, winStreak)
        if (seenWins < 0) return null
        return when {
            wins > seenWins -> if (winStreak >= 2) EveTopic.STREAK else EveTopic.WIN_SEEN
            winStreak == 0 && seenStreak > 0 -> EveTopic.LOSS_SEEN
            else -> null
        }
    }

    // ---- what to say -----------------------------------------------------------------------

    fun helloTopic(meeting: EveMeeting): EveTopic = when {
        meeting.levelUp == 2 -> EveTopic.BOND_BEST
        meeting.levelUp == 1 -> EveTopic.BOND_FRIEND
        meeting.daysAway >= 2 -> EveTopic.MISSED
        level >= 2 -> EveTopic.HELLO_BEST
        level == 1 -> EveTopic.HELLO_FRIEND
        else -> EveTopic.HELLO_NEW
    }

    fun tapTopic(): EveTopic = when (level) {
        2 -> EveTopic.TAP_BEST
        1 -> EveTopic.TAP_FRIEND
        else -> EveTopic.TAP_NEW
    }

    /** Picks a timely topic and limits repeated announcements, including across app restarts. */
    fun idleTopic(s: EveSituation, nowMillis: Long = System.currentTimeMillis()): EveTopic {
        val options = buildList {
            add(timeTopic(s.hour) to 1f)
            if (s.throneHourActive) add(EveTopic.THRONE_HOUR to 1.6f)
            if (s.dayOfWeek == 7) add(EveTopic.SUNDAY to 1.3f)
            if (s.dayOfWeek == 1) add(EveTopic.MONDAY to 1f)
            when {
                s.throneRank in 1..3 -> add(EveTopic.THRONE_TOP to 1.3f)
                s.throneRank in 4..10 -> add(EveTopic.THRONE_CLOSE to 1.1f)
                s.throneRank == 0 -> add(EveTopic.THRONE_NONE to .9f)
            }
            if (s.tournamentLive) add(EveTopic.TOURNAMENT_LIVE to 1.4f)
            if (s.winStreak >= 2) add(EveTopic.STREAK to .8f)
            if (s.longestWord.length >= 5) add(EveTopic.LONGEST to .7f)
            add(EveTopic.TIP to 1f)
            add(EveTopic.WORD_FACT to 1f)
            add(EveTopic.IDLE to 1.2f)
            add(EveTopic.OWNED_STORY to .35f + .2f * level)
        }
        val eligible = options.filter { (topic, _) ->
            topic !in ANNOUNCEMENT_TOPICS ||
                nowMillis - memory.getLong("topic_at_${topic.name}", Long.MIN_VALUE / 2) >= ANNOUNCEMENT_COOLDOWN_MS
        }.ifEmpty { options.filter { it.first !in ANNOUNCEMENT_TOPICS } }
        val fresh = eligible.filter { it.first !in recentTopics }.ifEmpty { eligible }
        val chosen = weighted(fresh)
        if (chosen in ANNOUNCEMENT_TOPICS) memory.putLong("topic_at_${chosen.name}", nowMillis)
        recentTopics.addLast(chosen)
        while (recentTopics.size > RECENT_TOPICS) recentTopics.removeFirst()
        memory.putString(KEY_RECENT_TOPICS, recentTopics.joinToString("|") { it.name })
        return chosen
    }

    /**
     * A (Turkish, English) line for [topic] that she has not said recently. "%s" becomes the
     * player's first name (or drops out with its comma), "%d" a number and "%w" a word.
     */
    fun line(topic: EveTopic, name: String?, number: Int = 0, word: String = ""): Pair<String, String> {
        val pool = EveLinesBook.of(topic)
        val recent = memory.getString(KEY_RECENT_LINES, "").split('|').filter { it.isNotEmpty() }
        val keys = pool.indices.map { "${topic.ordinal}.$it" }
        val unused = keys.indices.filter { keys[it] !in recent }
        val index = if (unused.isNotEmpty()) {
            unused[random.nextInt(unused.size)]
        } else {
            // Everything was said lately: take the one said longest ago.
            keys.indices.minByOrNull { recent.indexOf(keys[it]) } ?: 0
        }
        memory.putString(KEY_RECENT_LINES, (recent + keys[index]).takeLast(RECENT_LINES).joinToString("|"))
        val (tr, en) = pool[index]
        return fill(tr, name, number, word) to fill(en, name, number, word)
    }

    // ---- what to do ------------------------------------------------------------------------

    /** Picks one of [count] idle acts by [weights], never one of the last few acts. */
    fun pickAct(weights: FloatArray): Int {
        val options = weights.indices.filter { weights[it] > 0f && it !in recentActs }
            .ifEmpty { weights.indices.filter { weights[it] > 0f } }
        val chosen = weighted(options.map { it to weights[it] })
        recentActs.addLast(chosen)
        while (recentActs.size > RECENT_ACTS) recentActs.removeFirst()
        return chosen
    }

    private val recentTopics = ArrayDeque(
        memory.getString(KEY_RECENT_TOPICS, "").split('|').mapNotNull { value ->
            EveTopic.entries.firstOrNull { it.name == value }
        }.takeLast(RECENT_TOPICS)
    )
    private val recentActs = ArrayDeque<Int>()

    private fun <T> weighted(options: List<Pair<T, Float>>): T {
        val total = options.sumOf { it.second.toDouble() }.toFloat()
        var roll = random.nextFloat() * total
        for ((value, weight) in options) {
            roll -= weight
            if (roll <= 0f) return value
        }
        return options.last().first
    }

    private fun timeTopic(hour: Int): EveTopic = when (hour) {
        in 5..10 -> EveTopic.MORNING
        in 11..16 -> EveTopic.NOON
        in 17..21 -> EveTopic.EVENING
        in 22..23, 0 -> EveTopic.NIGHT
        else -> EveTopic.LATE
    }

    companion object {
        const val MAX_POINTS = 1_000
        private const val DAILY_TAP_POINTS = 6
        private const val RECENT_LINES = 40
        private const val RECENT_TOPICS = 4
        private const val RECENT_ACTS = 3
        private const val ANNOUNCEMENT_COOLDOWN_MS = 45 * 60 * 1_000L
        private val ANNOUNCEMENT_TOPICS = setOf(
            EveTopic.THRONE_HOUR, EveTopic.SUNDAY, EveTopic.MONDAY,
            EveTopic.THRONE_TOP, EveTopic.THRONE_CLOSE, EveTopic.THRONE_NONE,
            EveTopic.TOURNAMENT_LIVE, EveTopic.STREAK, EveTopic.LONGEST,
        )
        private const val KEY_POINTS = "bond_points"
        private const val KEY_MEETINGS = "meetings"
        private const val KEY_LAST_DAY = "last_day"
        private const val KEY_TAPS_TODAY = "taps_today"
        private const val KEY_SEEN_WINS = "seen_wins"
        private const val KEY_SEEN_STREAK = "seen_streak"
        private const val KEY_RECENT_LINES = "recent_lines"
        private const val KEY_RECENT_TOPICS = "recent_topics"

        fun levelOf(points: Int): Int = when {
            points >= 120 -> 2
            points >= 30 -> 1
            else -> 0
        }

        /** Fills placeholders; without a name, ", %s" / "%s, " drop out and the line stays natural. */
        fun fill(text: String, name: String?, number: Int, word: String): String {
            val named = if (name.isNullOrBlank()) {
                text.replace(", %s", "").replace("%s, ", "").replace(" %s", "").replace("%s", "")
                    .replaceFirstChar { it.uppercase() }
            } else {
                text.replace("%s", name)
            }
            return named.replace("%d", number.toString()).replace("%w", word).replace("  ", " ").trim()
        }
    }
}

/**
 * Everything Eve can say, as (Turkish, English) pairs. Written as natural spoken Turkish: the
 * player's name is never given a suffix (it stands alone, set off by a comma), so the lines read
 * right with any name. Facts and tips only state what is true of the game and the language.
 */
internal object EveLinesBook {
    fun of(topic: EveTopic): List<Pair<String, String>> = when (topic) {
        EveTopic.HELLO_NEW -> listOf(
            "Merhaba %s! Ben Eve. Ormandan kelime toplamaya geldim 🌿" to "Hi %s! I'm Eve. I came from the forest to gather words 🌿",
            "Selam %s! Seni tanımak çok güzel 🍃" to "Hi %s! It's lovely to meet you 🍃",
            "Hoş geldin %s! Biraz utangacım ama sana hemen alışırım 😊" to "Welcome %s! I'm a bit shy, but I'll warm up to you fast 😊",
            "Merhaba! Taht kürsüsünden buraya kadar yürüdüm, yorgunum ama mutluyum 🌿" to "Hello! I walked all the way from the throne podium, tired but happy 🌿",
        )
        EveTopic.HELLO_FRIEND -> listOf(
            "Yine geldin, %s! Seni görmek güzel 🌿" to "You're back, %s! Good to see you 🌿",
            "Selam %s! Bugün hangi kelimeyi avlıyoruz? 🍃" to "Hi %s! Which word are we hunting today? 🍃",
            "Oo, %s! Birlikte yeni kelimeler bulalım mı? 😊" to "Oh, %s! Shall we find some new words together? 😊",
            "Hoş geldin! Yapraklarım seni görünce hışırdadı 🍃" to "Welcome! My leaves rustled when I saw you 🍃",
            "Merhaba %s! Bir kelime macerasına hazır mısın? ✨" to "Hi %s! Ready for a word adventure? ✨",
            "Gel bakalım %s, bugün neler yapıyoruz? 🌿" to "Come on %s, what are we up to today? 🌿",
        )
        EveTopic.HELLO_BEST -> listOf(
            "Can dostum %s! Yine yan yana olmak güzel 💚" to "My dear friend %s! It's lovely to be together again 💚",
            "Oyun arkadaşım geldi! Merhaba %s 💚" to "My game partner is here! Hi %s 💚",
            "%s, kuyruğumu sallamadan duramıyorum, hoş geldin! 🌿" to "%s, I can't stop wagging my tail, welcome! 🌿",
            "Seni görünce içim ısındı, %s. Hadi bir maç yapalım! 💚" to "Seeing you warms my heart, %s. Let's play a match! 💚",
            "Geldin mi? Oh be, şimdi her şey yerli yerinde 😊" to "You're here? Phew, now everything's just right 😊",
        )
        EveTopic.MISSED -> listOf(
            "Yeniden hoş geldin, %s! Kaldığımız yerden devam edelim mi? 🌿" to "Welcome back, %s! Shall we pick up where we left off? 🌿",
            "Bir süredir görüşemedik. Seni görmek güzel, %s 🍃" to "It's been a little while. Good to see you, %s 🍃",
            "Uzun zaman oldu, %s! Yeni kelimeler bulmaya hazırım 🍃" to "It's been a while, %s! I'm ready to find new words 🍃",
        )
        EveTopic.BOND_FRIEND -> listOf(
            "Artık resmen arkadaşız, %s! 💚" to "We're officially friends now, %s! 💚",
            "Sana alıştım galiba, %s. Arkadaş mıyız artık? 😊" to "I think I've got used to you, %s. Friends now? 😊",
        )
        EveTopic.BOND_BEST -> listOf(
            "Sen benim can dostumsun artık, %s 💚" to "You're my best friend now, %s 💚",
            "Ormanda kimseye bu kadar bağlanmamıştım. Can dostum oldun 💚" to "I've never been this close to anyone in the forest. You're my best friend 💚",
        )
        EveTopic.MORNING -> listOf(
            "Günaydın %s! Kahvaltıdan önce küçük bir kelime ısınması? ☀️" to "Good morning %s! A little word warm-up before breakfast? ☀️",
            "Günaydın! Yeni bir gün, yeni kelimeler ☀️" to "Good morning! New day, new words ☀️",
            "Sabahın tazeliği tam oyun havası, %s 🌤️" to "Fresh morning air, perfect for a game, %s 🌤️",
        )
        EveTopic.NOON -> listOf(
            "İyi öğlenler %s! Kısa bir mola, tek bir maç? 🍃" to "Good afternoon %s! A short break, just one match? 🍃",
            "Öğleden sonra rehaveti mi? Kısa bir kelime molası verebiliriz 😊" to "Afternoon slump? We could take a quick word break 😊",
            "Güneş tepede, ben de keyifteyim 🌞" to "The sun is high and I'm in a good mood 🌞",
        )
        EveTopic.EVENING -> listOf(
            "İyi akşamlar %s! Günün nasıl geçti? 🌙" to "Good evening %s! How was your day? 🌙",
            "Akşam serinliği geldi, kelimeler de canlandı 🌙" to "The evening cool is here and the words came alive 🌙",
            "Akşamları ormanda kuşlar susar, ben de kelime düşünürüm 🌿" to "In the evening the birds go quiet and I think about words 🌿",
        )
        EveTopic.NIGHT -> listOf(
            "Geç oldu %s, son bir maç, sonra uyku? 🌙" to "It's late %s, one last match and then sleep? 🌙",
            "Gece kelimeleri daha gizemli olur, değil mi? ✨" to "Words feel more mysterious at night, don't they? ✨",
            "Yıldızlar çıktı. Bugün aklında kalan bir kelime var mı? ⭐" to "The stars are out. Is there a word from today that stuck with you? ⭐",
        )
        EveTopic.LATE -> listOf(
            "Bu saatte uyanık mısın? Ben de uyuyamadım 🦉" to "Awake at this hour? I couldn't sleep either 🦉",
            "Gece kuşları gibiyiz, %s 🌙 Ama uykunu da ihmal etme" to "We're like night owls, %s 🌙 But don't skip your sleep",
        )
        EveTopic.THRONE_HOUR -> listOf(
            "Taht Saati devam ediyor: uygun maç ve Atölye oyunlarında XP iki katına çıkıyor ⚡" to "Throne Hour is on: XP doubles for eligible matches and Workshop games ⚡",
            "İstanbul saatiyle 22.00'ye kadar Taht Saati sürüyor, %s ⚡" to "Throne Hour runs until 22:00 Istanbul time, %s ⚡",
        )
        EveTopic.TAP_NEW -> listOf(
            "Hı! Biraz ürktüm 😳" to "Oh! You startled me a little 😳",
            "Hihi, yavaş ol, daha yeni tanışıyoruz 😊" to "Hehe, easy, we've only just met 😊",
            "Kulaklarım gıdıklandı! 🌱" to "That tickles my ears! 🌱",
        )
        EveTopic.TAP_FRIEND -> listOf(
            "Hihi, yine mi sen? 😄" to "Hehe, you again? 😄",
            "Oyun oynamak mı istiyorsun? Hadi bakalım! 🍃" to "Want to play? Let's go! 🍃",
            "Bak, patimi sallıyorum 🐾" to "Look, I'm waving my paw 🐾",
            "Bugün hangi oyundan başlayalım? 🌿" to "Which game shall we start with today? 🌿",
            "Ne var ne yok, %s? 😊" to "What's up, %s? 😊",
        )
        EveTopic.TAP_BEST -> listOf(
            "En sevdiğim dokunuş! 💚" to "My favourite pat! 💚",
            "Seni her gördüğümde kuyruğum kendiliğinden sallanıyor 🌿" to "Every time I see you my tail wags by itself 🌿",
            "Biz harika bir takımız, %s 💚" to "We make a great team, %s 💚",
            "İyi ki varsın, %s 💚" to "I'm so glad you're here, %s 💚",
        )
        EveTopic.TICKLE -> listOf(
            "Hahaha, dur, gıdıklanıyorum! 🤭" to "Hahaha, stop, that tickles! 🤭",
            "Tamam tamam, pes ediyorum! 😆" to "Okay okay, I give up! 😆",
            "Biraz soluklanayım, sonra yine oynarız 🍃" to "Let me catch my breath, then we can play again 🍃",
        )
        EveTopic.FLEE -> listOf(
            "Biraz gezip geri geldim! 🍃" to "I took a little stroll and came back! 🍃",
            "Şuraya da bir bakayım dedim 🍃" to "I thought I'd take a look over here 🍃",
            "Hareket etmeyi seviyorum; sen neler yapıyorsun? 🌿" to "I like moving around; what are you up to? 🌿",
            "Buradayım, merak etme 😊" to "I'm right here, don't worry 😊",
            "Küçük bir tur attım, yine yanındayım 🌿" to "I took a little loop; I'm back beside you 🌿",
        )
        EveTopic.MOVED -> listOf(
            "Buradan manzara güzelmiş 🌿" to "This is a lovely spot 🌿",
            "Yeni yerimi beğendim 😊" to "I like my new spot 😊",
            "Hop! Beni buraya mı koydun? 🍃" to "Hop! You put me here? 🍃",
            "Manzara buradan daha güzel, haklısın 😊" to "The view is nicer from here, you're right 😊",
        )
        EveTopic.WAKE -> listOf(
            "Hı? Uyuyakalmışım… Buradayım! 😊" to "Hm? I dozed off… I'm here! 😊",
            "Rüyamda kelime yağmuru vardı 😴" to "I dreamt it was raining words 😴",
            "Uyandım, uyandım! Ne kaçırdım? 😊" to "I'm up, I'm up! What did I miss? 😊",
        )
        EveTopic.POUNCE -> listOf(
            "Hop! Yakaladım! 🍃" to "Pounce! Got it! 🍃",
            "Bir yaprak gördüm sandım 😄" to "I thought I saw a leaf 😄",
            "Kelime avındayım! 🌿" to "I'm hunting for words! 🌿",
        )
        EveTopic.TIP -> listOf(
            "Kuşatma'da kelimeni bonus hücresinden geçirmek puanını artırabilir 💡" to "In Word Siege, routing a word through a bonus square can raise its score 💡",
            "Son Harf'te kelimeni zor bir harfle bitirirsen rakibin zorlanır 😉" to "In Last Letter, ending on a tricky letter makes it hard for your rival 😉",
            "Kelime Kuşatması'nda bonus hücrelerini kaçırma, puanı katlıyorlar 💡" to "In Word Siege, don't miss the bonus cells, they multiply your score 💡",
            "Atölye turnuvası her 2 saatte bir açılıyor, ilk üçe girmeyi dene 🏆" to "The Workshop tournament opens every 2 hours, try for the top three 🏆",
            "Taht yarışında sıralamanı görmek için Taht ekranına bakabilirsin 👑" to "Check the Throne screen to see your place in the weekly race 👑",
            "Geçerli maçlardaki galibiyetler Taht XP'sine katkı sağlar ⚡" to "Wins in eligible matches add to your Throne XP ⚡",
        )
        EveTopic.WORD_FACT -> listOf(
            "Biliyor muydun? 'Kelime' sözcüğü Arapçadan dilimize geçmiş 📚" to "Did you know? The Turkish word 'kelime' comes from Arabic 📚",
            "Türkçede Ğ harfiyle başlayan hiçbir kelime yok 🤓" to "No Turkish word starts with the letter Ğ 🤓",
            "Türkçe eklemeli bir dil; bir köke art arda pek çok ek gelebilir 📚" to "Turkish is agglutinative: many suffixes can stack on one root 📚",
            "Ünlü uyumu sayesinde Türkçe kelimeler kulağa akıcı gelir 🎶" to "Vowel harmony makes Turkish words flow so nicely 🎶",
            "'Yaprak' deyince hemen kulaklarımı dikiyorum 🍃" to "The word 'leaf' always makes my ears perk up 🍃",
        )
        EveTopic.IDLE -> listOf(
            "Şu yapraklar çok lezzetli 🌿" to "These leaves are delicious 🌿",
            "Orman sessiz, tam oyun havası 🍃" to "The forest is quiet, perfect for a game 🍃",
            "Bir kelebek gördüm sandım 🦋" to "I thought I saw a butterfly 🦋",
            "Kulaklarımla rüzgârı dinliyorum 🍃" to "I'm listening to the wind with my ears 🍃",
            "Bugün hangi kelimeyi bulacaksın acaba? 🤔" to "I wonder which word you'll find today 🤔",
            "Burada senin yanında olmak güzel 😊" to "It's nice being here with you 😊",
        )
        EveTopic.OWNED_STORY -> listOf(
            "Taht yolculuğunda bir araya geldik; şimdi burada birlikteyiz 💚" to "The Throne journey brought us together; now we're here side by side 💚",
            "Taht kürsüsünden gelen orman dostun benim 😊" to "I'm your forest friend from the Throne podium 😊",
            "Parça parça tamamlandım, şimdi hep yanındayım 🌿" to "I came together piece by piece, and now I'm always with you 🌿",
        )
        EveTopic.WIN_SEEN -> listOf(
            "Son gelişinden beri bir galibiyet daha almışsın. Tebrikler! 🎉" to "You've picked up another win since your last visit. Well done! 🎉",
            "Tebrikler %s! Yeni galibiyetini kutlayalım 🏆" to "Congratulations %s! Let's celebrate your new win 🏆",
            "Bir galibiyet daha! Güzel gidiyor 😄" to "Another win! Nicely done 😄",
        )
        EveTopic.LOSS_SEEN -> listOf(
            "Bu maç zor geçmiş olabilir. İstersen bir sonrakinde yeni bir yol deneriz 💚" to "That match may have been tough. We can try a new approach next time 💚",
            "Her maç farklı, %s. Hazır olduğunda yeniden deneyebiliriz 🌿" to "Every match is different, %s. We can try again when you're ready 🌿",
            "Moralini bozma, kısa bir moladan sonra yeniden deneriz 🍃" to "Chin up, we'll try again after a short break 🍃",
        )
        EveTopic.STREAK -> listOf(
            "Üst üste %d galibiyet! Durdurulamıyorsun 🔥" to "%d wins in a row! You're unstoppable 🔥",
            "%d galibiyetlik seri yakaladın, %s. Güzel gidiyorsun 👑" to "You've built a %d-win streak, %s. Nicely done 👑",
        )
        EveTopic.LONGEST -> listOf(
            "Kaydettiğim en uzun kelimen '%w'. Güzel seçim! 🌿" to "The longest word I've seen from you is '%w'. Nice choice! 🌿",
            "Şimdiye dek gördüğüm en uzun kelimen '%w'. Daha uzunu gelir mi? 🍃" to "The longest word I've seen from you is '%w'. Will there be a longer one? 🍃",
        )
        EveTopic.THRONE_TOP -> listOf(
            "Bu hafta Taht'ta %d. sıradasın. Kürsüdesin! 👑" to "You're #%d in this week's Throne race. You're on the podium! 👑",
            "Kürsüdesin! %d. sıra… Böyle devam! 👑" to "You're on the podium! Rank %d… Keep it up! 👑",
        )
        EveTopic.THRONE_CLOSE -> listOf(
            "Taht yarışında %d. sıradasın. Kürsüye çıkmak için hâlâ zaman var 🌿" to "You're #%d in the Throne race. There's still time to reach the podium 🌿",
            "Şu an %d. sıradasın, %s. Bir sonraki maç yeni bir fırsat 🍃" to "You're currently #%d, %s. The next match is another chance 🍃",
        )
        EveTopic.THRONE_NONE -> listOf(
            "Bu haftaki Taht sıralamanda henüz yer görünmüyor. Bir maç oynayalım mı? 👑" to "You don't have a place in this week's Throne ranking yet. Shall we play a match? 👑",
        )
        EveTopic.SUNDAY -> listOf(
            "Taht haftası İstanbul saatiyle gece yarısı bitiyor. Sıralamana göz atabilirsin ⏳" to "The Throne week ends at midnight Istanbul time. You can check your standing ⏳",
        )
        EveTopic.MONDAY -> listOf(
            "Yeni hafta, yeni taht yarışı! Herkes sıfırdan başlıyor 🌱" to "New week, new throne race! Everyone starts from zero 🌱",
        )
        EveTopic.TOURNAMENT_LIVE -> listOf(
            "Atölye turnuvası şu an açık, katılmak ister misin? 🏆" to "The Workshop tournament is live now, want to join? 🏆",
            "Turnuva başladı! Kelime Atölyesi'nde seni bekliyorlar 🏆" to "The tournament has started! They're waiting for you in the Word Workshop 🏆",
        )
    }
}
