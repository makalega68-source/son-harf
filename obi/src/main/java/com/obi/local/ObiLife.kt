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
        val match = Regex("(\\d{1,3})\\s*\\.?\\s*not[\\p{L}'’]*\\s+sil|not[\\p{L}'’]*\\s+(\\d{1,3})[\\p{L}'’]*\\s+sil").find(t) ?: return null
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
        if (t == "listem" || t == "liste" || t == "alışveriş" || listOf("alışveriş listes", "alisveris listes").any(t::contains)) return ListCommand.Show
        if (Regex("liste(?:m|mi|yi|m[ıi]z)?(?!\\p{L})").containsMatchIn(t) &&
            listOf("göster", "getir", "aç", "oku", "ne var", "neler var", "söyle", "bak").any(t::contains)) return ListCommand.Show
        return null
    }

    /** Items typed after Obi asked "Ne ekleyeyim?": "süt ekmek kahve" or "süt, zeytin yağı". */
    fun listItems(t: String): List<String> {
        val parts = if (t.contains(',') || t.contains(" ve ")) t.split(Regex("\\s*(?:,|\\sve\\s)\\s*")) else t.split(Regex("\\s+"))
        return parts.map { it.trim(' ', '.', '!') }.filter { it.isNotBlank() }.take(20)
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

    /** A mood answer: "4", "4/5", an emoji or words such as "iyiyim", "idare eder", "berbat". */
    fun moodScore(raw: String): Int? {
        val t = lower(raw)
        Regex("^([1-5])(?:\\s*/\\s*5)?$").find(t)?.let { return it.groupValues[1].toInt() }
        moodFaces.forEachIndexed { i, face -> if (t.contains(face)) return i + 1 }
        return when {
            listOf("çok kötü", "berbat", "rezalet", "felaket").any(t::contains) -> 1
            listOf("iyi değil", "kötü", "üzgün", "yorgun", "moralim bozuk", "mutsuz").any(t::contains) -> 2
            listOf("idare", "fena değil", "normal", "eh işte", "orta").any(t::contains) -> 3
            listOf("harika", "süper", "mükemmel", "çok iyi", "muhteşem", "şahane").any(t::contains) -> 5
            listOf("iyi", "güzel", "keyifli", "mutlu").any(t::contains) -> 4
            else -> null
        }
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

    /** What to wear from "Pofi'yi giy", "bere tak", "şapkayı çıkar"; null when the text is not about clothes. */
    data class Wear(val skin: WordSiegeMascotSkin? = null, val hat: WordSiegeMascotHat? = null)

    private val hatWords = listOf(
        "parti" to WordSiegeMascotHat.PARTY, "taç" to WordSiegeMascotHat.CROWN, "tac" to WordSiegeMascotHat.CROWN,
        "bere" to WordSiegeMascotHat.BERET, "çiçek" to WordSiegeMascotHat.FLOWER, "büyücü" to WordSiegeMascotHat.WIZARD,
        "silindir" to WordSiegeMascotHat.TOP_HAT,
    )

    fun wear(t: String): Wear? {
        if (Regex("şapka[\\p{L}'’]*\\s+(?:çıkar|cikar)").containsMatchIn(t) || t.contains("şapkasız")) return Wear(hat = WordSiegeMascotHat.NONE)
        val verb = Regex("(?<!\\p{L})(?:giy\\p{L}*|ol|olsana|olur musun|tak|takar\\p{L}*|tak\\p{L}*|dönüş\\p{L}*)(?!\\p{L})").containsMatchIn(t)
        if (!verb) return null
        val skin = WordSiegeMascotSkin.entries.filter { it != WordSiegeMascotSkin.GOLD_KING }
            .firstOrNull { Regex("(?<!\\p{L})${lower(it.titleTr)}").containsMatchIn(t) }
        val hat = if (t.contains("tak") || t.contains("şapka")) hatWords.firstOrNull { t.contains(it.first) }?.second else null
        return if (skin != null || hat != null) Wear(skin, hat) else null
    }

    fun isWardrobe(t: String): Boolean = listOf("dolap", "kıyafet", "kostüm", "neler giyebil").any(t::contains)

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

    // ---- Friendship ---------------------------------------------------------------------------

    fun bondLevel(points: Int): String = when {
        points >= 500 -> "Ayrılmaz ikili 💞"
        points >= 200 -> "Can dostu 💜"
        points >= 80 -> "Yakın arkadaş 🤝"
        points >= 20 -> "Arkadaş 🙂"
        else -> "Yeni tanıştık 👋"
    }

    fun friendMilestone(days: Long): String? = when {
        days == 7L -> "Tam 1 haftadır arkadaşız! 🎉"
        days == 30L -> "1 aydır birlikteyiz! Seni tanımak çok güzel 💜"
        days == 100L -> "100 gündür arkadaşız! 🎆 Yanımda olduğun için teşekkürler."
        days >= 365L && days % 365L == 0L -> "Bugün arkadaşlığımızın ${days / 365L}. yılı! 🎂"
        else -> null
    }

    /** Something the user told about themselves: shown back as "en sevdiğin renk: mavi". */
    data class Fact(val key: String, val value: String)

    private val questionWords = setOf("ne", "neydi", "nedir", "hangisi", "hangi", "kim", "neresi", "kaç", "biliyor", "musun", "mi", "mı")

    fun fact(text: String): Fact? {
        val t = lower(text).trim().trimEnd('.', '!')
        if (t.endsWith("?") || t.split(' ').any { it in questionWords }) return null
        Regex("^(?:benim\\s+)?en sevdiğim\\s+(\\p{L}{2,20})\\s+(.{2,40})$").find(t)?.let {
            return Fact("en sevdiğin ${it.groupValues[1]}", it.groupValues[2].removeSuffix("dır").removeSuffix("dir").trim())
        }
        Regex("^(?:benim\\s+)?(memleketim|mesleğim|takımım|burcum)\\s+(.{2,30})$").find(t)?.let {
            val key = mapOf("memleketim" to "memleketin", "mesleğim" to "mesleğin", "takımım" to "takımın", "burcum" to "burcun")
            return Fact(key.getValue(it.groupValues[1]), it.groupValues[2].trim())
        }
        Regex("^(\\d{1,3})\\s+yaşındayım$").find(t)?.let { return Fact("yaşın", it.groupValues[1]) }
        Regex("^(kedimin|köpeğimin|kuşumun|balığımın)\\s+adı\\s+(\\p{L}{2,20})$").find(t)?.let {
            val owner = mapOf("kedimin" to "kedinin", "köpeğimin" to "köpeğinin", "kuşumun" to "kuşunun", "balığımın" to "balığının")
                .getValue(it.groupValues[1])
            return Fact("$owner adı", it.groupValues[2].replaceFirstChar { c -> c.titlecase(tr) })
        }
        Regex("^(?:ben\\s+)?(.{2,30}?)\\s+(?:çok\\s+)?(?:severim|seviyorum|bayılırım)$").find(t)?.let {
            val what = it.groupValues[1].trim()
            if (what in setOf("seni", "sizi", "obi", "obiyi", "obi'yi", "onu")) return null
            return Fact("sevdiğin", what)
        }
        Regex("^(?:ben\\s+)?(.{2,30}?)\\s+(?:hiç\\s+)?(?:sevmem|sevmiyorum|nefret ederim)$").find(t)?.let {
            return Fact("sevmediğin", it.groupValues[1].trim())
        }
        return null
    }

    /** "en sevdiğim renk ne?" → "en sevdiğin renk"; "hakkımda ne biliyorsun" → "*" (everything). */
    fun factQuestion(text: String): String? {
        val t = lower(text).trim()
        if (listOf("hakkımda ne biliyorsun", "beni ne kadar tanıyorsun", "beni tanıyor musun", "benim hakkımda", "neleri hatırlıyorsun").any(t::contains)) return "*"
        Regex("en sevdiğim\\s+(\\p{L}{2,20})\\s+(?:ne|neydi|nedir|hangisi)").find(t)?.let { return "en sevdiğin ${it.groupValues[1]}" }
        if (t.contains("memleketim") && (t.contains("neresi") || t.contains("ne"))) return "memleketin"
        if (t.contains("kaç yaşındayım")) return "yaşın"
        if (t.contains("burcum ne")) return "burcun"
        return null
    }

    enum class Feeling { LOVE, HUG, PRAISE, THANKS, SAD, ANGRY, BORED, TIRED, HAPPY }

    fun feeling(text: String): Feeling? {
        val t = lower(text)
        fun any(vararg words: String) = words.any(t::contains)
        return when {
            any("seni seviyorum", "seni çok seviyorum", "seni sevdim", "canımsın", "bir tanesin", "seni seviyom", "love you") -> Feeling.LOVE
            any("sarıl", "kucakla", "sarilalim", "sarilir") -> Feeling.HUG
            any("aferin", "harikasın", "süpersin", "akıllısın", "çok iyisin", "tatlısın", "çok tatlı", "en iyisisin") -> Feeling.PRAISE
            any("teşekkür", "sağ ol", "sağol", "eyvallah", "tşk") -> Feeling.THANKS
            any("moralim bozuk", "canım sıkkın", "kötü hissediyorum", "ağlıyorum", "mutsuzum", "yalnızım", "çok üzgünüm", "üzgünüm bugün", "kötüyüm", "keyfim yok") -> Feeling.SAD
            any("sinirliyim", "kızgınım", "çok sinirlendim", "delirdim", "sinir oldum") -> Feeling.ANGRY
            any("sıkıldım", "sıkılıyorum", "canım sıkıldı") -> Feeling.BORED
            any("yorgunum", "çok yoruldum", "uykum var", "uykum geldi", "bitkinim") -> Feeling.TIRED
            any("çok mutluyum", "mutluyum", "başardım", "sevinçliyim", "harika bir gün", "çok iyiyim") -> Feeling.HAPPY
            else -> null
        }
    }

    /** Full-screen effect asked for by name ("konfeti at", "kar yağdır"), as an ObiEffect name. */
    fun effectCommand(text: String): String? {
        val t = lower(text)
        return when {
            t.contains("konfeti") -> "CONFETTI"
            t.contains("havai fişek") || t.contains("havai fisek") -> "FIREWORKS"
            t.contains("kalp yağdır") || t.contains("kalp yagdir") || t.contains("kalpler") -> "HEARTS"
            t.contains("yıldız yağdır") || t.contains("yıldızlar") || t.contains("yildiz yagdir") -> "STARS"
            t.contains("kar yağdır") || t.contains("kar yagdir") || t == "kar yağsın" -> "SNOW"
            t.contains("baloncuk") -> "BUBBLES"
            t.contains("çiçek yağdır") || t.contains("yaprak yağdır") || t.contains("çiçekler") -> "PETALS"
            t.contains("şov yap") || t.contains("sov yap") || t.contains("animasyon yap") || t.contains("bir şov") || t.contains("beni eğlendir") -> "RANDOM"
            else -> null
        }
    }

    /** Asking Obi to make a face ("kalp gözlü ol", "gözlük tak", "mimiklerini göster"), as an ObiFace name. */
    fun faceCommand(text: String): String? {
        val t = lower(text)
        return when {
            t.contains("mimik") || t.contains("yüz ifade") || t.contains("surat yap") -> "ALL"
            t.contains("kalp göz") -> "HEART_EYES"
            t.contains("yıldız göz") -> "STAR_EYES"
            t.contains("gözlük tak") || t.contains("havalı ol") || t.contains("güneş gözlüğü") -> "COOL"
            t.contains("başın dönsün") || t.contains("sersemle") -> "DIZZY"
            t == "şaşır" || t.contains("şaşırmış") -> "SHOCKED"
            t == "sinirlen" || t.contains("kızgın ol") || t.contains("sinirli ol") -> "STEAM"
            t == "ağla" || t.contains("ağlar mısın") -> "CRY"
            t == "düşün" || t.contains("kafan karış") -> "CONFUSED"
            t == "gül" || t.contains("kahkaha") -> "LAUGH"
            t.contains("fikir bul") || t.contains("aklına bir şey") -> "IDEA"
            else -> null
        }
    }

    // ---- Night mode ---------------------------------------------------------------------------

    /** "gece modu 23-7", "gece modu 22:30 07:00" → (start, end) in minutes of the day; null when no times are given. */
    fun nightRange(text: String): Pair<Int, Int>? {
        val m = Regex("(\\d{1,2})(?:[:.](\\d{2}))?\\s*(?:-|–|ile|ila|'?(?:den|dan|ten|tan)|\\s)\\s*(\\d{1,2})(?:[:.](\\d{2}))?").find(lower(text)) ?: return null
        val sh = m.groupValues[1].toInt(); val sm = m.groupValues[2].toIntOrNull() ?: 0
        val eh = m.groupValues[3].toInt(); val em = m.groupValues[4].toIntOrNull() ?: 0
        if (sh > 23 || eh > 23 || sm > 59 || em > 59) return null
        return (sh * 60 + sm) to (eh * 60 + em)
    }

    fun isInRange(minute: Int, start: Int, end: Int): Boolean =
        if (start <= end) minute in start until end else minute >= start || minute < end

    fun clock(minutes: Int): String = String.format(tr, "%02d:%02d", minutes / 60, minutes % 60)

    // ---- Children ---------------------------------------------------------------------------

    data class MathQuestion(val text: String, val answer: Int)

    /** Small sums for children: up to 10 at first, up to 20 later in the round. */
    fun mathQuestion(seed: Long, index: Int): MathQuestion {
        val r = java.util.Random(seed * 31 + index)
        val top = if (index < 2) 10 else 20
        val a = 1 + r.nextInt(top - 1)
        val b = 1 + r.nextInt(top - 1)
        return if (r.nextBoolean() || a == b) MathQuestion("$a + $b = ?", a + b)
        else MathQuestion("${maxOf(a, b)} − ${minOf(a, b)} = ?", maxOf(a, b) - minOf(a, b))
    }

    /** Spoken or typed numbers for answers: "7", "yedi", "on iki". */
    fun numberAnswer(text: String): Int? {
        val t = lower(text).trim(' ', '.', '!')
        t.toIntOrNull()?.let { return it }
        val ones = listOf("sıfır", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
        val parts = t.split(' ')
        var total = 0
        for (p in parts) {
            when {
                p == "on" -> total += 10
                p == "yirmi" -> total += 20
                p == "otuz" -> total += 30
                ones.indexOf(p) >= 0 -> total += ones.indexOf(p)
                else -> return null
            }
        }
        return total
    }

    val tales = listOf(
        "🌙 Uykucu Yıldız\nGökyüzünde Pırıl adında minik bir yıldız varmış. Her gece en son o yanarmış, çünkü hep uyuyakalırmış. Bir gece Obi ona bir ninni söylemiş ve demiş ki: ‘Erken uyursan erken parlarsın!’ Pırıl o gece erkenden uyumuş ve ertesi akşam gökyüzünün ilk yıldızı olmuş. Herkes ona el sallamış. Pırıl o kadar mutlu olmuş ki ışığı iki kat parlamış. ✨",
        "🐢 Acelesi Olmayan Kaplumbağa\nTosbi adında bir kaplumbağa ormandaki yarışmaya katılmak istemiş. Herkes ‘Sen çok yavaşsın’ demiş. Tosbi yolda düşen bir kuş yavrusunu yuvasına taşımış, susayan bir çiçeği sulamış. Yarışı son bitirmiş ama orman ona ‘En iyi kalpli’ madalyasını vermiş. Tosbi öğrenmiş ki hız değil, iyilik insanı kazandırır. 🏅",
        "☁️ Yağmur Olmak İsteyen Bulut\nPofuduk adında beyaz bir bulut, hep diğer bulutlar gibi yağmur olmak istermiş. Bir gün kuruyan bir tarlanın üstünden geçerken çiçeklerin üzgün olduğunu görmüş. Bütün gücüyle sıkmış kendini ve minik damlalar dökülmüş. Çiçekler başlarını kaldırıp teşekkür etmiş. O günden sonra Pofuduk her yaz o tarlaya uğrarmış. 🌷",
        "🦊 Paylaşmayı Öğrenen Tilki\nKızıl Tilki bütün elmaları kendine saklarmış. Bir kış günü elmaları bitmiş ve çok acıkmış. Tavşan ona kendi havucunu uzatmış. Kızıl Tilki o kadar duygulanmış ki baharda topladığı elmaları bütün ormanla paylaşmış. Paylaştıkça arkadaşları çoğalmış, sofrası hiç boş kalmamış. 🍎",
        "🚀 Obi Aya Gidiyor\nBir gece Obi pencereden Ay'a bakmış ve ‘Acaba orada da arkadaş var mı?’ diye merak etmiş. Kartondan bir roket yapmış, içine bir sepet kurabiye koymuş. Hayalinde Ay'a varınca yalnız bir Ay tavşanıyla tanışmış. Kurabiyeleri paylaşmışlar ve kraterlerde zıplamışlar. Sabah uyandığında Obi gülümsüyormuş: Arkadaşlık uzakta bile olsa insanı ısıtırmış. 🌕",
    )

    // ---- Pronunciation ----------------------------------------------------------------------

    /** "elma nasıl okunur", "telaffuz: necessary", "strawberry nasıl söylenir" → the word(s). */
    fun pronounceWord(text: String): String? {
        val t = lower(text).trim().trimEnd('?', '.', '!')
        Regex("^(?:telaffuz(?:u)?|seslendir|oku)\\s*[:]?\\s+(.{1,40})$").find(t)?.let { return it.groupValues[1].trim(' ', '"', '\'') }
        Regex("^[\"']?(.{1,40}?)[\"']?(?:['’](?:[ıiuü]|y[ıiuü]|n[ıiuü]))?\\s+(?:nasıl\\s+(?:okunur|söylenir|telaffuz edilir)|kelimesini\\s+(?:oku|söyle|seslendir)|nasıl\\s+okunuyor)$").find(t)?.let {
            return it.groupValues[1].trim(' ', '"', '\'').takeIf { w -> w.split(' ').size <= 4 }
        }
        return null
    }

    /** Turkish syllables: a consonant before a vowel starts a new syllable ("ar-ka-daş", "Türk-çe"). */
    fun syllables(word: String): List<String> {
        val vowels = "aeıioöuüâîû"
        val w = lower(word).filter { it.isLetter() }
        if (w.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var start = 0
        val vowelIdx = w.indices.filter { w[it] in vowels }
        for (k in 1 until vowelIdx.size) {
            val prev = vowelIdx[k - 1]; val cur = vowelIdx[k]
            val cut = if (cur - prev == 1) cur else cur - 1
            out += w.substring(start, cut); start = cut
        }
        out += w.substring(start)
        return out.filter { it.isNotEmpty() }
    }

    // ---- Weekly summary -----------------------------------------------------------------------

    fun weeklySummary(moodLine: String, spent: Double, notes: Int, days: Long, level: String): String = buildString {
        append("📅 Haftalık özetin\n")
        append("• Ruh hâli: $moodLine\n")
        append("• Harcama (7 gün): ${if (spent > 0) money(spent) else "kayıt yok"}\n")
        append("• Notların: $notes\n")
        append("• $days gündür arkadaşız — $level")
    }
}
