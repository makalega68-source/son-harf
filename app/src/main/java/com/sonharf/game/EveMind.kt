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

    /** Picks something fitting to talk about, never a topic she used in her last few lines. */
    fun idleTopic(s: EveSituation): EveTopic {
        val options = buildList {
            add(timeTopic(s.hour) to 1f)
            if (s.hour in 19..21) add(EveTopic.THRONE_HOUR to 1.6f)
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
        val fresh = options.filter { it.first !in recentTopics }.ifEmpty { options }
        val chosen = weighted(fresh)
        recentTopics.addLast(chosen)
        while (recentTopics.size > RECENT_TOPICS) recentTopics.removeFirst()
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

    private val recentTopics = ArrayDeque<EveTopic>()
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
        private const val KEY_POINTS = "bond_points"
        private const val KEY_MEETINGS = "meetings"
        private const val KEY_LAST_DAY = "last_day"
        private const val KEY_TAPS_TODAY = "taps_today"
        private const val KEY_SEEN_WINS = "seen_wins"
        private const val KEY_SEEN_STREAK = "seen_streak"
        private const val KEY_RECENT_LINES = "recent_lines"

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
            "Yine geldin, %s! Seni bekliyordum 🌿" to "You're back, %s! I was waiting for you 🌿",
            "Selam %s! Bugün hangi kelimeyi avlıyoruz? 🍃" to "Hi %s! Which word are we hunting today? 🍃",
            "Oo, %s! Tam da seni düşünüyordum 😊" to "Oh, %s! I was just thinking about you 😊",
            "Hoş geldin! Yapraklarım seni görünce hışırdadı 🍃" to "Welcome! My leaves rustled when I saw you 🍃",
            "Merhaba %s! Bugün enerjin harika görünüyor ✨" to "Hi %s! Your energy looks great today ✨",
            "Gel bakalım %s, bugün neler yapıyoruz? 🌿" to "Come on %s, what are we up to today? 🌿",
        )
        EveTopic.HELLO_BEST -> listOf(
            "Can dostum %s! Sensiz orman çok sessiz kalıyor 💚" to "My dear friend %s! The forest is so quiet without you 💚",
            "En sevdiğim oyuncu geldi! Merhaba %s 💚" to "My favourite player is here! Hi %s 💚",
            "%s, kuyruğumu sallamadan duramıyorum, hoş geldin! 🌿" to "%s, I can't stop wagging my tail, welcome! 🌿",
            "Seni görünce içim ısındı, %s. Hadi bir maç yapalım! 💚" to "Seeing you warms my heart, %s. Let's play a match! 💚",
            "Geldin mi? Oh be, şimdi her şey yerli yerinde 😊" to "You're here? Phew, now everything's just right 😊",
        )
        EveTopic.MISSED -> listOf(
            "%d gündür yoktun, %s! Seni çok özledim 🥺" to "You've been away %d days, %s! I missed you so much 🥺",
            "Neredeydin? Tam %d gün oldu… Gel, biraz kelime avlayalım 🌿" to "Where were you? It's been %d days… Come, let's hunt some words 🌿",
            "Uzun zaman oldu, %s! Sana yeni kelimeler sakladım 🍃" to "It's been a while, %s! I saved some new words for you 🍃",
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
            "Günaydın! Sabahları zihin kelime bulmakta pek keskin olur ☀️" to "Good morning! The mind is sharp for words in the morning ☀️",
            "Sabahın tazeliği tam oyun havası, %s 🌤️" to "Fresh morning air, perfect for a game, %s 🌤️",
        )
        EveTopic.NOON -> listOf(
            "İyi öğlenler %s! Kısa bir mola, tek bir maç? 🍃" to "Good afternoon %s! A short break, just one match? 🍃",
            "Öğleden sonra rehaveti mi? Bir kelime oyunu iyi gelir 😊" to "Afternoon slump? A word game will help 😊",
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
            "Yıldızlar çıktı. Sen de bir yıldız gibi parladın bugün ⭐" to "The stars are out. You shone like one today ⭐",
        )
        EveTopic.LATE -> listOf(
            "Bu saatte uyanık mısın? Ben de uyuyamadım 🦉" to "Awake at this hour? I couldn't sleep either 🦉",
            "Gece kuşları gibiyiz, %s 🌙 Ama uykunu da ihmal etme" to "We're like night owls, %s 🌙 But don't skip your sleep",
        )
        EveTopic.THRONE_HOUR -> listOf(
            "Taht Saati başladı! 22.00'ye kadar her oyunda ×2 XP var ⚡" to "Throne Hour is on! ×2 XP in every game until 22:00 ⚡",
            "Akşamın en güzel saati: ×2 XP! Kaçırma %s ⚡" to "The best hour of the evening: ×2 XP! Don't miss it %s ⚡",
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
            "Seninle olmak güzel 💚" to "It's nice being with you 💚",
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
            "Bu kadar sevgi fazla, kalbim pır pır 💚" to "That's so much love, my heart's fluttering 💚",
        )
        EveTopic.FLEE -> listOf(
            "Yakalayamazsın! 😄" to "Can't catch me! 😄",
            "Hihi, kaçtım! 🍃" to "Hehe, got away! 🍃",
            "Ebelemece mi oynuyoruz? Ebe sensin! 🌿" to "Are we playing tag? You're it! 🌿",
            "Buradayım, buradaa! 😊" to "Over here, over heeere! 😊",
            "Hop! Bana yetişemezsin 🌿" to "Hop! You can't keep up with me 🌿",
        )
        EveTopic.MOVED -> listOf(
            "Vay, uçtum! Burası da güzelmiş 🌿" to "Whee, I flew! Nice spot 🌿",
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
            "İpucu: Uzun kelimeler genelde daha çok puan getirir 💡" to "Tip: longer words usually score more 💡",
            "Son Harf'te kelimeni zor bir harfle bitirirsen rakibin zorlanır 😉" to "In Last Letter, ending on a tricky letter makes it hard for your rival 😉",
            "Kelime Kuşatması'nda bonus hücrelerini kaçırma, puanı katlıyorlar 💡" to "In Word Siege, don't miss the bonus cells, they multiply your score 💡",
            "Atölye turnuvası her 2 saatte bir açılıyor, ilk üçe girmeyi dene 🏆" to "The Workshop tournament opens every 2 hours, try for the top three 🏆",
            "Haftalık görevler taht yarışında sana ekstra XP kazandırır 🎯" to "Weekly missions earn you extra XP in the throne race 🎯",
            "Her galibiyet taht yarışında +85 XP daha getiriyor ⚡" to "Every win brings +85 more XP in the throne race ⚡",
        )
        EveTopic.WORD_FACT -> listOf(
            "Biliyor muydun? 'Kelime' sözcüğü Arapçadan dilimize geçmiş 📚" to "Did you know? The Turkish word 'kelime' comes from Arabic 📚",
            "Türkçede Ğ harfiyle başlayan hiçbir kelime yok 🤓" to "No Turkish word starts with the letter Ğ 🤓",
            "Türkçe eklemeli bir dil; bir köke art arda pek çok ek gelebilir 📚" to "Turkish is agglutinative: many suffixes can stack on one root 📚",
            "Ünlü uyumu sayesinde Türkçe kelimeler kulağa akıcı gelir 🎶" to "Vowel harmony makes Turkish words flow so nicely 🎶",
            "'Yaprak' öz Türkçe bir kelime, benim de en sevdiğim 🍃" to "'Yaprak' (leaf) is a native Turkish word, and my favourite 🍃",
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
            "Beni taht yarışında kazandın, bunu hiç unutmayacağım 💚" to "You won me in the throne race, I'll never forget it 💚",
            "Taht kürsüsünden gelen tek orman dostu benim 😊" to "I'm the only forest friend who came from the throne podium 😊",
            "Parça parça tamamlandım, şimdi hep yanındayım 🌿" to "I came together piece by piece, and now I'm always with you 🌿",
        )
        EveTopic.WIN_SEEN -> listOf(
            "Az önce kazandın, gördüm! Kuyruğum hâlâ sallanıyor 🎉" to "You just won, I saw it! My tail is still wagging 🎉",
            "Tebrikler %s! O galibiyet tam bir şölendi 🏆" to "Congratulations %s! That win was a feast 🏆",
            "Kazanmak sana çok yakışıyor 😄" to "Winning really suits you 😄",
        )
        EveTopic.LOSS_SEEN -> listOf(
            "Bu sefer olmadı ama sorun değil, bir sonrakini birlikte alırız 💚" to "Not this time, but it's fine, we'll take the next one together 💚",
            "Kaybetmek de öğrenmektir, %s. Ben hep yanındayım 🌿" to "Losing is learning too, %s. I'm always with you 🌿",
            "Moralini bozma, kısa bir moladan sonra yeniden deneriz 🍃" to "Chin up, we'll try again after a short break 🍃",
        )
        EveTopic.STREAK -> listOf(
            "Üst üste %d galibiyet! Durdurulamıyorsun 🔥" to "%d wins in a row! You're unstoppable 🔥",
            "Seri %d oldu, %s! Taht seni bekliyor 👑" to "That's a streak of %d, %s! The throne awaits 👑",
        )
        EveTopic.LONGEST -> listOf(
            "'%w' kelimeni hâlâ unutamadım, harikaydı 🤩" to "I still can't forget your word '%w', it was amazing 🤩",
            "En uzun kelimen '%w'… Onu geçebilecek misin? 😉" to "Your longest word is '%w'… Can you beat it? 😉",
        )
        EveTopic.THRONE_TOP -> listOf(
            "Taht yarışında bu hafta %d. sıradasın, harika gidiyorsun! 👑" to "You're %d. in this week's throne race, great going! 👑",
            "Kürsüdesin! %d. sıra… Böyle devam! 👑" to "You're on the podium! Rank %d… Keep it up! 👑",
        )
        EveTopic.THRONE_CLOSE -> listOf(
            "Taht yarışında %d. sıradasın, ilk üçe az kaldı 💪" to "You're %d. in the throne race, the top three is close 💪",
            "%d. sıradan kürsüye birkaç galibiyet var, %s 💪" to "From rank %d the podium is a few wins away, %s 💪",
        )
        EveTopic.THRONE_NONE -> listOf(
            "Bu hafta taht yarışına daha başlamadın, bir maçla açalım mı? 👑" to "You haven't started this week's throne race yet, shall we open with a match? 👑",
        )
        EveTopic.SUNDAY -> listOf(
            "Bugün pazar! Taht yarışı gece yarısı bitiyor, son bir hamle? ⏳" to "It's Sunday! The throne race ends at midnight, one last push? ⏳",
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
