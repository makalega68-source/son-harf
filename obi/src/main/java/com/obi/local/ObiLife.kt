package com.obi.local

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Pure everyday helpers (lists, money, dates, games, bond). No Android types, so all of it is unit tested. */
internal object ObiLife {
    private val tr = Locale("tr", "TR")
    val months = listOf("ocak", "şubat", "mart", "nisan", "mayıs", "haziran", "temmuz", "ağustos", "eylül", "ekim", "kasım", "aralık")
    private val monthPattern = months.joinToString("|")
    private val dayNames = listOf("pazartesi", "salı", "çarşamba", "perşembe", "cuma", "cumartesi", "pazar")

    fun lower(text: String) = text.trim().lowercase(tr)
    fun title(text: String) = text.replaceFirstChar { it.titlecase(tr) }

    // ---- Repeating alarms -------------------------------------------------------------------

    /** "daily", "weekdays" or "weekly:1".."weekly:7" (ISO day), or null for a one-off. */
    fun repeatRule(t: String): String? {
        if (t.contains("hafta içi") || t.contains("hafta ici")) return "weekdays"
        if (Regex("her\\s+(?:gün|gun|sabah|akşam|aksam|gece|öğlen|oglen)").containsMatchIn(t)) return "daily"
        val day = dayNames.indexOfFirst { Regex("her\\s+$it(?!\\p{L})").containsMatchIn(t) }
        return if (day >= 0) "weekly:${day + 1}" else null
    }

    fun repeatLabel(rule: String?): String = when {
        rule == null -> ""
        rule == "daily" -> "her gün"
        rule == "weekdays" -> "hafta içi"
        rule.startsWith("weekly:") -> "her ${dayNames.getOrElse(rule.removePrefix("weekly:").toInt() - 1) { "hafta" }}"
        else -> ""
    }

    fun stripRepeat(text: String): String = text
        .replace(Regex("(?i)hafta\\s+i[çc]i(?:\\s+her\\s+g[üu]n)?"), "")
        .replace(Regex("(?i)her\\s+(?:g[üu]n|${dayNames.joinToString("|")})(?!\\p{L})"), "")
        .replace(Regex("\\s{2,}"), " ").trim()

    /** Next time strictly after [now] that matches [rule] at hour:minute. */
    fun nextOccurrence(rule: String, hour: Int, minute: Int, now: LocalDateTime): LocalDateTime {
        var candidate = now.toLocalDate().atTime(hour, minute)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        repeat(8) {
            val dow = candidate.dayOfWeek
            val ok = when {
                rule == "weekdays" -> dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
                rule.startsWith("weekly:") -> dow.value == rule.removePrefix("weekly:").toIntOrNull()
                else -> true
            }
            if (ok) return candidate
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    /** "2. alarmı sil", "alarm 2 sil", "hatırlatıcı 3'ü sil" → 2 / 3; "tüm alarmları sil" → 0. */
    fun alarmToDelete(t: String): Int? {
        if (!t.contains("sil") && !t.contains("iptal")) return null
        if (!Regex("alarm|hatırlatıcı|hatirlatici|hatırlatma").containsMatchIn(t)) return null
        if (t.contains("tüm") || t.contains("hepsini") || t.contains("bütün")) return 0
        val number = Regex("(\\d{1,2})").find(t)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return number.takeIf { it >= 1 }
    }

    fun isAlarmList(t: String): Boolean =
        listOf("alarmlarım", "alarmlarim", "alarmları göster", "hatırlatıcılarım", "hatirlaticilarim", "hatırlatmalarım", "kurulu alarm").any(t::contains)

    // ---- Notes ------------------------------------------------------------------------------

    /** "3. notu sil", "not 3 sil" → 3; "notları temizle" / "tüm notları sil" → 0. */
    fun noteToDelete(t: String): Int? {
        if (t.contains("notları temizle") || t.contains("notlari temizle") || (t.contains("tüm notları") && t.contains("sil"))) return 0
        val match = Regex("(\\d{1,3})\\s*\\.?\\s*not\\w*\\s+sil|not\\w*\\s+(\\d{1,3})\\w*\\s+sil").find(t) ?: return null
        return (match.groupValues[1].ifBlank { match.groupValues[2] }).toIntOrNull()?.takeIf { it >= 1 }
    }

    fun noteSearch(t: String): String? =
        Regex("not(?:larımda|larimda|larda)\\s+(.+?)\\s+(?:ara|bul)$").find(t)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    // ---- Shopping list ----------------------------------------------------------------------

    sealed interface ListCommand {
        data class Add(val items: List<String>) : ListCommand
        data class Remove(val item: String) : ListCommand
        data object Show : ListCommand
        data object Clear : ListCommand
    }

    fun listCommand(t: String): ListCommand? {
        Regex("^(?:alışveriş\\s+)?listeye\\s+(.+?)\\s+ekle$").find(t)?.let { m ->
            val items = m.groupValues[1].split(Regex("\\s*(?:,|\\sve\\s)\\s*")).map { it.trim() }.filter { it.isNotBlank() }
            if (items.isNotEmpty()) return ListCommand.Add(items)
        }
        Regex("^listeden\\s+(.+?)\\s+(?:sil|çıkar|cikar)$").find(t)?.let { return ListCommand.Remove(it.groupValues[1].trim()) }
        Regex("^(.+?)\\s+(?:alındı|alindi)$").find(t)?.let { return ListCommand.Remove(it.groupValues[1].trim()) }
        if (t.contains("listeyi temizle") || t.contains("listeyi sil")) return ListCommand.Clear
        if (t == "listem" || t == "liste" || listOf("listemi göster", "listeyi göster", "alışveriş listes", "alisveris listes").any(t::contains)) return ListCommand.Show
        return null
    }

    // ---- Expenses ---------------------------------------------------------------------------

    data class Expense(val label: String, val amount: Double)

    fun expense(raw: String): Expense? {
        val t = lower(raw)
        if (t.contains("?") || t.contains("kaç") || t.contains("ne kadar")) return null
        val money = Regex("(\\d{1,7}(?:[.,]\\d{1,2})?)\\s*(?:tl|lira|₺)(?!\\p{L})").find(t) ?: return null
        val amount = money.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        val keyword = t.contains("harcadım") || t.contains("harcadim") || t.startsWith("harcama")
        val rest = t.removeRange(money.range).replace(Regex("harcama[:\\s]*|harcad[ıi]m|ödedim|odedim"), " ")
            .replace(Regex("\\s{2,}"), " ").trim(' ', ':', ',', '-')
        val plainForm = Regex("^[\\p{L}\\s]{2,30}$").matches(rest) && t.endsWith(money.value.trim())
        if (!keyword && !plainForm) return null
        return Expense(title(rest.ifBlank { "Harcama" }), amount)
    }

    fun isExpenseSummary(t: String): Boolean =
        listOf("harcamalarım", "harcamalarim", "ne harcadım", "ne harcadim", "bu ay harcama", "harcama özeti").any(t::contains)

    fun money(amount: Double): String =
        if (amount % 1.0 == 0.0) "${amount.toLong()} TL" else String.format(tr, "%.2f TL", amount)

    // ---- Birthdays of others and countdowns ---------------------------------------------------

    data class DatedLabel(val label: String, val day: Int, val month: Int, val year: Int? = null)

    /** "annemin doğum günü 5 mart" → ("annemin", 5, 3). The user's own "doğum günüm" is handled elsewhere. */
    fun otherBirthday(t: String): DatedLabel? {
        val m = Regex("^(.{2,30}?)\\s+do[ğg]um\\s+g[üu]n[üu]\\s+(\\d{1,2})\\s*($monthPattern)").find(t) ?: return null
        val day = m.groupValues[2].toInt().takeIf { it in 1..31 } ?: return null
        return DatedLabel(m.groupValues[1].trim(), day, months.indexOf(m.groupValues[3]) + 1)
    }

    fun isBirthdayList(t: String): Boolean = t.contains("doğum günleri") || t.contains("dogum gunleri")

    /** "geri sayım tatil 15 temmuz" or "geri sayım: sınav 3 haziran 2027". */
    fun countdown(t: String): DatedLabel? {
        val m = Regex("geri\\s+say[ıi]m[:\\s]+(.+?)\\s+(\\d{1,2})\\s*($monthPattern)(?:\\s+(\\d{4}))?").find(t) ?: return null
        val day = m.groupValues[2].toInt().takeIf { it in 1..31 } ?: return null
        return DatedLabel(m.groupValues[1].trim(), day, months.indexOf(m.groupValues[3]) + 1, m.groupValues[4].toIntOrNull())
    }

    /** The word asked about in "tatile kaç gün kaldı", or "" for a general "kaç gün kaldı". */
    fun countdownQuery(t: String): String? {
        if (!t.contains("kaç gün kaldı") && !t.contains("kac gun kaldi") && !t.contains("geri sayımlar") && !t.contains("geri sayimlar")) return null
        return Regex("([\\p{L}]+)\\s+ka[çc]\\s+g[üu]n").find(t)?.groupValues?.get(1) ?: ""
    }

    fun matchesLabel(label: String, word: String): Boolean {
        if (word.isBlank()) return true
        val stem = word.take(maxOf(3, minOf(word.length, label.length) - 1))
        return lower(label).startsWith(lower(stem).take(4))
    }

    /** Days until the next [day]/[month] (this year or next), or the fixed date when a year is set. */
    fun daysUntil(target: DatedLabel, today: LocalDate): Long {
        val year = target.year
        if (year != null) return ChronoUnit.DAYS.between(today, safeDate(year, target.month, target.day))
        var date = safeDate(today.year, target.month, target.day)
        if (date.isBefore(today)) date = safeDate(today.year + 1, target.month, target.day)
        return ChronoUnit.DAYS.between(today, date)
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate =
        LocalDate.of(year, month, 1).let { it.withDayOfMonth(minOf(day, it.lengthOfMonth())) }

    // ---- Small settings ---------------------------------------------------------------------

    fun savedCity(t: String): String? =
        Regex("^şehrim(?:i)?\\s+([\\p{L}]{2,40})(?:\\s+yap)?$").find(t)?.groupValues?.get(1)

    /** "yatma saatim 23" / "uyku saatim 23:30" → minutes after midnight. */
    fun bedtime(t: String): Int? {
        val m = Regex("(?:yatma|uyku)\\s+saatim\\s+(\\d{1,2})(?:[:.](\\d{2}))?").find(t) ?: return null
        val hour = m.groupValues[1].toInt().takeIf { it in 0..23 } ?: return null
        val minute = m.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: 0
        return hour * 60 + minute
    }

    /** Asleep from bedtime until 07:00. */
    fun isNight(minuteOfDay: Int, bedtime: Int): Boolean =
        if (bedtime >= 7 * 60) minuteOfDay >= bedtime || minuteOfDay < 7 * 60 else minuteOfDay in bedtime until 7 * 60

    // ---- Mood -------------------------------------------------------------------------------

    val moodFaces = listOf("😞", "😕", "🙂", "😄", "🤩")

    fun moodReply(score: Int): String = when (score) {
        1 -> "Zor bir gün olmuş. Yanındayım; yarın daha yumuşak geçsin 💜"
        2 -> "Biraz yorucu geçmiş. Kendine küçük bir iyilik yapmayı unutma."
        3 -> "İdare eder bir gün. Bazen bu da gayet yeterli."
        4 -> "Güzel bir gün! Neyin iyi geldiğini hatırlayalım ✨"
        else -> "Harika bir gün! Bu enerjiyi kutlayalım 🎉"
    }

    fun weeklyMood(scores: Map<LocalDate, Int>, today: LocalDate): String {
        val week = (0L..6L).mapNotNull { scores[today.minusDays(it)] }
        if (week.isEmpty()) return "Bu hafta ruh hâlini hiç kaydetmedik. Akşamları ‘Bugün nasıldın?’ satırından seçebilirsin."
        val average = week.average()
        val face = moodFaces[(average.toInt() - 1).coerceIn(0, 4)]
        val note = when {
            average >= 4 -> "Çok güzel bir haftaydı!"
            average >= 3 -> "Dengeli bir hafta geçirdin."
            else -> "Biraz zorlu bir hafta. Kendine nazik ol 💜"
        }
        return "Bu hafta ${week.size} gün kaydettik, ortalama $face (${String.format(tr, "%.1f", average)}/5). $note"
    }

    // ---- English words ----------------------------------------------------------------------

    data class Word(val en: String, val tr: String, val example: String)

    val words = listOf(
        Word("apple", "elma", "An apple a day keeps the doctor away."), Word("bridge", "köprü", "We walked across the bridge."),
        Word("brave", "cesur", "She was brave enough to try."), Word("calm", "sakin", "Stay calm and breathe."),
        Word("cloud", "bulut", "A cloud covered the sun."), Word("curious", "meraklı", "Cats are curious animals."),
        Word("dream", "rüya / hayal", "Follow your dream."), Word("early", "erken", "I woke up early today."),
        Word("enough", "yeterli", "That is enough for now."), Word("forest", "orman", "The forest is quiet at night."),
        Word("friend", "arkadaş", "A good friend listens."), Word("gentle", "nazik", "Be gentle with yourself."),
        Word("grateful", "minnettar", "I am grateful for today."), Word("habit", "alışkanlık", "Reading is a good habit."),
        Word("honest", "dürüst", "Thank you for being honest."), Word("journey", "yolculuk", "Life is a journey."),
        Word("kind", "iyi kalpli", "It was kind of you to help."), Word("laugh", "gülmek", "We laughed all evening."),
        Word("lucky", "şanslı", "Today is my lucky day."), Word("moon", "ay", "The moon is bright tonight."),
        Word("neighbour", "komşu", "Our neighbour has a dog."), Word("ocean", "okyanus", "The ocean is deep."),
        Word("patient", "sabırlı", "Be patient, it takes time."), Word("peace", "huzur", "I need some peace and quiet."),
        Word("proud", "gururlu", "I am proud of you."), Word("quiet", "sessiz", "The library is quiet."),
        Word("rain", "yağmur", "Take an umbrella, it may rain."), Word("remember", "hatırlamak", "Do you remember me?"),
        Word("river", "nehir", "The river flows to the sea."), Word("shy", "utangaç", "He is a little shy."),
        Word("simple", "basit", "Keep it simple."), Word("sky", "gökyüzü", "The sky is clear today."),
        Word("smile", "gülümseme", "Your smile is lovely."), Word("strong", "güçlü", "You are stronger than you think."),
        Word("sunrise", "gün doğumu", "We watched the sunrise."), Word("thirsty", "susamış", "Drink water if you are thirsty."),
        Word("tired", "yorgun", "I am tired after work."), Word("tomorrow", "yarın", "See you tomorrow."),
        Word("travel", "seyahat etmek", "I love to travel."), Word("tree", "ağaç", "Birds sing in the tree."),
        Word("warm", "sıcak / ılık", "The tea is warm."), Word("weather", "hava durumu", "The weather is nice."),
        Word("wish", "dilek", "Make a wish!"), Word("wonder", "merak etmek", "I wonder what is next."),
        Word("young", "genç", "She is young and happy."), Word("busy", "meşgul", "I am busy right now."),
        Word("careful", "dikkatli", "Be careful on the stairs."), Word("delicious", "lezzetli", "This soup is delicious."),
        Word("excited", "heyecanlı", "I am excited about the trip."), Word("free", "özgür / boş", "Are you free tonight?"),
        Word("garden", "bahçe", "Flowers grow in the garden."), Word("healthy", "sağlıklı", "Eat healthy food."),
        Word("island", "ada", "We visited a small island."), Word("library", "kütüphane", "I study at the library."),
        Word("mistake", "hata", "Everyone makes mistakes."), Word("promise", "söz vermek", "I promise to call you."),
        Word("ready", "hazır", "Are you ready?"), Word("share", "paylaşmak", "Let's share the cake."),
        Word("surprise", "sürpriz", "What a nice surprise!"), Word("together", "birlikte", "We work together."),
    )

    fun wordsOfDay(day: LocalDate): List<Word> {
        val start = Math.floorMod(day.toEpochDay() * 3, words.size.toLong()).toInt()
        return (0 until 3).map { words[(start + it) % words.size] }
    }

    data class Quiz(val word: Word, val options: List<String>)

    fun quiz(day: LocalDate, index: Int, seed: Long): Quiz {
        val word = wordsOfDay(day)[Math.floorMod(index, 3)]
        val others = words.filter { it != word }.shuffled(kotlin.random.Random(seed)).take(2).map { it.tr }
        return Quiz(word, (others + word.tr).shuffled(kotlin.random.Random(seed + 1)))
    }

    // ---- Games ------------------------------------------------------------------------------

    val guessWords = listOf(
        "kalem", "bulut", "deniz", "çiçek", "kitap", "masal", "elmas", "güneş", "kuzey", "orman",
        "sabah", "akşam", "bahar", "limon", "kiraz", "çorba", "balık", "salon", "tahta", "sevgi",
        "şeker", "dolap", "fener", "ceviz", "bilgi", "radyo", "tavuk", "pasta", "keman", "armut",
        "havuç", "kumaş", "duvar", "ördek", "tabak", "çanta", "silgi", "kepçe", "yayla", "köprü",
    ).filter { it.length == 5 }.distinct()

    /** Wordle-style marks: 🟩 right place, 🟨 in the word, ⬜ not in the word. */
    fun guessMarks(answer: String, guess: String): String {
        val a = lower(answer); val g = lower(guess)
        val marks = Array(g.length) { "⬜" }
        val left = a.toMutableList()
        g.forEachIndexed { i, c -> if (i < a.length && a[i] == c) { marks[i] = "🟩"; left.remove(c) } }
        g.forEachIndexed { i, c -> if (marks[i] != "🟩" && left.remove(c)) marks[i] = "🟨" }
        return marks.joinToString("")
    }

    data class Riddle(val question: String, val answers: List<String>, val hint: String)

    val riddles = listOf(
        Riddle("Ağzı var dili yok, durmadan konuşur. Nedir?", listOf("radyo", "televizyon"), "Kulaklarını aç!"),
        Riddle("Ne kadar çok alırsan o kadar büyür. Nedir?", listOf("çukur", "delik"), "Kürekle yapılır."),
        Riddle("Gece gelir, sabah gider. Gökyüzünde ışıl ışıl. Nedir?", listOf("yıldız", "ay"), "Gökyüzüne bak."),
        Riddle("Benim bir gözüm var ama göremem. Nedir?", listOf("iğne"), "Dikiş yaparken kullanılır."),
        Riddle("Kanadı yok uçar, ağzı yok konuşur, gözü yok ağlar. Nedir?", listOf("bulut"), "Yağmur getirir."),
        Riddle("Kırıldıkça kullanılır. Nedir?", listOf("yumurta"), "Kahvaltıda olur."),
        Riddle("Hep önündedir ama göremezsin. Nedir?", listOf("gelecek", "yarın"), "Zamanla ilgili."),
        Riddle("Ne kadar çok kurursa o kadar ıslanır. Nedir?", listOf("havlu"), "Banyoda bulunur."),
        Riddle("Dişi var ısırmaz. Nedir?", listOf("tarak"), "Saçla ilgili."),
        Riddle("Elleri var ama alkışlayamaz. Nedir?", listOf("saat"), "Tik tak yapar."),
        Riddle("Ayağı yok yürür, sesi yok bağırır. Ne olabilir?", listOf("zaman", "saat"), "Durmaz, hep ilerler."),
        Riddle("Adını söylersen bozulur. Nedir?", listOf("sessizlik"), "Şşş…"),
        Riddle("Ben giderim o gider, ben dururum o durur. Nedir?", listOf("gölge", "gölgem"), "Güneşli günde yanındadır."),
        Riddle("Tepesi beyaz, içi kırmızı… Kış meyvesi değil, dağ! Nedir?", listOf("volkan", "yanardağ"), "Ateş püskürür."),
        Riddle("Anahtarları var ama kapı açamaz. Nedir?", listOf("piyano", "klavye"), "Müzik ya da yazı."),
    )

    fun riddleCorrect(riddle: Riddle, answer: String): Boolean {
        val a = lower(answer)
        return riddle.answers.any { a.contains(it) }
    }

    // ---- Bond and wardrobe ------------------------------------------------------------------

    data class Unlock(val points: Int, val skin: WordSiegeMascotSkin? = null, val hat: WordSiegeMascotHat? = null) {
        val name: String get() = skin?.titleTr ?: hatName(hat ?: WordSiegeMascotHat.NONE)
    }

    val unlocks = listOf(
        Unlock(0, skin = WordSiegeMascotSkin.ORB),
        Unlock(10, hat = WordSiegeMascotHat.PARTY),
        Unlock(25, skin = WordSiegeMascotSkin.PINK),
        Unlock(45, hat = WordSiegeMascotHat.FLOWER),
        Unlock(70, skin = WordSiegeMascotSkin.CAT),
        Unlock(100, hat = WordSiegeMascotHat.BERET),
        Unlock(140, skin = WordSiegeMascotSkin.BIRD),
        Unlock(190, hat = WordSiegeMascotHat.WIZARD),
        Unlock(250, skin = WordSiegeMascotSkin.ROBOT),
        Unlock(320, hat = WordSiegeMascotHat.TOP_HAT),
        Unlock(400, skin = WordSiegeMascotSkin.ASTRONAUT),
        Unlock(500, skin = WordSiegeMascotSkin.DEVIL_BLUE),
        Unlock(620, skin = WordSiegeMascotSkin.DEVIL_RED),
        Unlock(750, hat = WordSiegeMascotHat.CROWN),
    )

    fun hatName(hat: WordSiegeMascotHat): String = when (hat) {
        WordSiegeMascotHat.NONE -> "Şapkasız"
        WordSiegeMascotHat.PARTY -> "Parti şapkası"
        WordSiegeMascotHat.CROWN -> "Taç"
        WordSiegeMascotHat.BERET -> "Bere"
        WordSiegeMascotHat.FLOWER -> "Çiçek tacı"
        WordSiegeMascotHat.WIZARD -> "Büyücü şapkası"
        WordSiegeMascotHat.TOP_HAT -> "Silindir şapka"
    }

    fun unlocked(points: Int): List<Unlock> = unlocks.filter { it.points <= points }
    fun nextUnlock(points: Int): Unlock? = unlocks.firstOrNull { it.points > points }
    fun justUnlocked(before: Int, after: Int): Unlock? = unlocks.lastOrNull { it.points in (before + 1)..after }

    // ---- Celebrations -----------------------------------------------------------------------

    data class Celebration(val message: String, val hat: WordSiegeMascotHat)

    fun celebration(day: LocalDate): Celebration? {
        val md = day.monthValue to day.dayOfMonth
        val mothers = day.withDayOfMonth(1).withMonth(5).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY))
        val fathers = day.withDayOfMonth(1).withMonth(6).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.SUNDAY))
        return when {
            md == (1 to 1) -> Celebration("Mutlu yıllar! Yeni yılda bol gülümseme 🎆", WordSiegeMascotHat.PARTY)
            md == (12 to 31) -> Celebration("Yılın son günü! Bu yılın en güzel anı neydi? 🎉", WordSiegeMascotHat.PARTY)
            md == (2 to 14) -> Celebration("Sevgiler Günü! Bugün sevdiklerine bir kalp gönder 💜", WordSiegeMascotHat.FLOWER)
            md == (4 to 23) -> Celebration("23 Nisan Ulusal Egemenlik ve Çocuk Bayramı kutlu olsun! 🎈", WordSiegeMascotHat.PARTY)
            md == (5 to 19) -> Celebration("19 Mayıs Gençlik ve Spor Bayramı kutlu olsun! 🇹🇷", WordSiegeMascotHat.PARTY)
            md == (8 to 30) -> Celebration("30 Ağustos Zafer Bayramı kutlu olsun! 🇹🇷", WordSiegeMascotHat.CROWN)
            md == (10 to 29) -> Celebration("Cumhuriyet Bayramı kutlu olsun! 🇹🇷🎉", WordSiegeMascotHat.PARTY)
            day == mothers -> Celebration("Anneler Günü! Bugün annene sarılmayı unutma 🌷", WordSiegeMascotHat.FLOWER)
            day == fathers -> Celebration("Babalar Günü! Babana güzel bir söz söyle 🎩", WordSiegeMascotHat.TOP_HAT)
            else -> null
        }
    }
}
