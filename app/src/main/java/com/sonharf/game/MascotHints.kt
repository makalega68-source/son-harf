package com.sonharf.game

import android.content.Context
import com.sonharf.game.data.SharedDictionaryService
import java.util.Locale
import kotlin.random.Random

/**
 * Hints the mascot gives in games.
 *
 * Fair play: against a real opponent the mascot never names a word; it only gives strategy tips.
 * Against a bot, in practice and in solo modes it reveals the start of a real, everyday word.
 */
internal object MascotHints {
    const val HINTS_PER_MATCH = 3

    @Volatile private var commonTr: Set<String>? = null

    private fun common(context: Context, language: String): Set<String> {
        if (SharedDictionaryService.canonicalLanguage(language) != "tr") return emptySet()
        return commonTr ?: synchronized(this) {
            commonTr ?: runCatching {
                context.applicationContext.assets.open("mascot_common_tr.txt").bufferedReader().useLines { lines ->
                    lines.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                }
            }.getOrDefault(emptySet()).also { commonTr = it }
        }
    }

    /** Candidate words starting with [prefix]: everyday words first, then the loaded dictionary. */
    internal fun pickWord(
        prefix: String,
        common: Set<String>,
        dictionary: Set<String>,
        exclude: Set<String>,
        random: Random = Random.Default,
    ): String? {
        fun from(pool: Collection<String>) = pool.filter {
            it.startsWith(prefix) && it.length in maxOf(3, prefix.length + 2)..7 && it !in exclude
        }
        val everyday = from(common.filter { dictionary.isEmpty() || it in dictionary })
        if (everyday.isNotEmpty()) return everyday.random(random)
        return from(dictionary).filter { it.length <= 6 }.ifEmpty { from(dictionary) }.randomOrNull(random)
    }

    /** "KA _ _ _": the first letters and one blank per hidden letter. */
    internal fun pattern(word: String, shown: Int, language: String): String {
        val locale = if (SharedDictionaryService.canonicalLanguage(language) == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
        val head = word.take(shown).uppercase(locale)
        return head + " _".repeat((word.length - shown).coerceAtLeast(0))
    }

    /** A hint for the next word starting with [prefix] (Son Harf against a bot). */
    fun startWord(context: Context, language: String, prefix: String, exclude: Set<String>): String {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        val word = pickWord(prefix, common(context, lang), SharedDictionaryService.snapshot(lang).orEmpty(), exclude)
            ?: return sh("Bu harfle zor bir kelime… kısa ve bildik bir şey dene!", "A tricky letter… try something short and familiar!")
        val shown = (prefix.length + 1).coerceAtMost(word.length - 1)
        val hint = pattern(word, shown, lang)
        return sh("Şuna ne dersin: $hint (${word.length} harf)", "How about: $hint (${word.length} letters)")
    }

    /** A hint built from a player's letters (practice, solo): the start of a word they can make. */
    fun fromLetters(language: String, candidates: List<String>, exclude: Set<String> = emptySet()): String {
        val word = candidates.filter { it !in exclude }.sortedByDescending { it.length }.take(6).randomOrNull()
            ?: return sh("Harflerini karıştır, gözden kaçan bir kelime çıkabilir!", "Shuffle your letters, a word may pop out!")
        val hint = pattern(word, (word.length / 2).coerceAtLeast(1), language)
        return sh("Harflerinle bunu kurabilirsin: $hint", "You can build this with your letters: $hint")
    }

    /** A hint from the player's rack (Kuşatma practice): half of an everyday word the rack can make. */
    fun fromRack(context: Context, language: String, rack: String): String {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        val locale = if (lang == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
        val rackCounts = rack.uppercase(locale).groupingBy { it }.eachCount()
        val full = SharedDictionaryService.practiceCandidates(lang, rack, 1_000)
            .filter { it.length >= 3 && it.groupingBy { c -> c }.eachCount().all { (c, n) -> (rackCounts[c] ?: 0) >= n } }
            .map { it.lowercase(locale) }
        val everyday = common(context, lang)
        val pool = full.filter { it in everyday }.ifEmpty { full }
        return fromLetters(lang, pool)
    }

    private val tips = listOf(
        "Kelimeni az kelimeyle başlayan bir harfle bitir, rakibin zorlansın." to "End your word on a letter few words start with to squeeze your rival.",
        "Süre azsa kısa ve emin bir kelime yaz; puanı sonra toparlarsın." to "Short on time? Play a short, safe word and catch up later.",
        "Uzun kelimeler bonus getirir ama önce emin olduğunu yaz." to "Long words earn bonuses, but play the one you're sure of first.",
        "Aynı kelime tekrar edilemez; kullanılanları şeritten kontrol et." to "Words can't repeat; check the used ones in the strip.",
        "Rakibin sık bitirdiği harfleri aklında tut, cevabını önceden hazırla." to "Remember the letters your rival ends on and prepare your answer.",
    )

    /** A strategy tip with no word in it (real opponents: fair play). */
    fun tip(seed: Int): String = tips[Math.floorMod(seed, tips.size)].let { (tr, en) -> sh(tr, en) }
}
