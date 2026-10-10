package com.sonharf.game

import android.content.Context
import com.sonharf.game.data.SharedDictionaryService
import java.util.Locale
import kotlin.random.Random

/**
 * Hints the mascot gives in games.
 *
 * A hint is a clear answer: a real, everyday word the player can play now. Against a real opponent
 * both players get the same three per match, so it stays even.
 */
internal object MascotHints {
    const val HINTS_PER_MATCH = 3

    /** Mascot advantage: owning a mascot gives three free hints every match; others buy them. */
    val freeHints: Int get() = if (WordSiegeMascotOwnership.hasAny) HINTS_PER_MATCH else 0

    /**
     * Free hints left in a match after [used] of them. Computed from hints used (not a count fixed
     * at match start), so mascot ownership that loads mid-match still grants its free hints.
     * Against a real opponent a hint is only a strategy tip (fair play), so every player gets the
     * tips for free and banked or bought hints are never spent on them.
     */
    fun freeHintsLeft(realOpponent: Boolean, used: Int, ownsMascot: Boolean = WordSiegeMascotOwnership.hasAny): Int {
        val allowance = if (realOpponent || ownsMascot) HINTS_PER_MATCH else 0
        return (allowance - used).coerceAtLeast(0)
    }

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

    /** The whole word in capitals: hints give the clear answer, not a puzzle. */
    internal fun full(word: String, language: String): String {
        val locale = if (SharedDictionaryService.canonicalLanguage(language) == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
        return word.uppercase(locale)
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
        // English has no everyday list: its hints come from the bundled dictionary, which a game
        // screen may not have loaded yet (Son Harf validates on the server), so load it here.
        if (!SharedDictionaryService.hasSnapshot(lang)) SharedDictionaryService.restorePersisted(context, lang)
        val word = pickWord(prefix, common(context, lang), SharedDictionaryService.snapshot(lang).orEmpty(), exclude)
            ?: return sh("Bu harfle zor bir kelime… kısa ve bildik bir şey dene!", "A tricky letter… try something short and familiar!")
        // A hint is a clear answer: the whole word, ready to type.
        val shown = full(word, lang)
        return sh("Cevap: $shown — hemen yaz!", "Answer: $shown — type it now!")
    }

    /** A hint built from a player's letters (practice, solo): the start of a word they can make. */
    fun fromLetters(language: String, candidates: List<String>, exclude: Set<String> = emptySet()): String {
        val word = candidates.filter { it !in exclude }.sortedByDescending { it.length }.take(6).randomOrNull()
            ?: return sh("Harflerini karıştır, gözden kaçan bir kelime çıkabilir!", "Shuffle your letters, a word may pop out!")
        val shown = full(word, language)
        return sh("Cevap: $shown", "Answer: $shown")
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

/**
 * Hints used in each online match, kept on the device. The count used to live only in the
 * screen, so leaving a match and coming back gave all three hints again.
 */
internal object MatchHintLedger {
    private const val PREFS = "match_hints_used"
    private const val MAX_ENTRIES = 200

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun used(context: Context, matchKey: String): Int = prefs(context).getInt(matchKey, 0)

    fun record(context: Context, matchKey: String, used: Int) {
        val store = prefs(context)
        val editor = store.edit()
        // Old matches are of no use: start afresh once the list grows long.
        if (!store.contains(matchKey) && store.all.size >= MAX_ENTRIES) editor.clear()
        editor.putInt(matchKey, used).apply()
    }
}
