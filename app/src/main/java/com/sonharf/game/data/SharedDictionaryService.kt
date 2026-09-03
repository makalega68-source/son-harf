package com.sonharf.game.data

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class DictionarySnapshotDto(
    val language: String,
    val words: List<String>,
)

/**
 * Canonical dictionary gateway shared by every word-game mode.
 *
 * public.dictionary_words is authoritative via get_dictionary_snapshot_v3. The complete active,
 * game-allowed snapshot is cached independently per language. A persisted snapshot is restored first
 * for offline continuity, then a network refresh is attempted so a stale cache never becomes
 * permanently authoritative. There is no reduced practice-only fallback lexicon.
 */
object SharedDictionaryService {
    private const val PREFS = "son_harf_dictionary_snapshot_v3"
    private const val WORDS_PREFIX = "words_"
    private const val MIN_CANONICAL_LENGTH = 2
    private const val MAX_CANONICAL_LENGTH = 32
    private val snapshots = ConcurrentHashMap<String, Set<String>>()
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.ENGLISH

    fun canonicalLanguage(language: String): String = if (language.lowercase(Locale.ROOT) == "en") "en" else "tr"

    fun normalize(word: String, language: String): String {
        val lang = canonicalLanguage(language)
        val trimmed = word.trim()
        return if (lang == "tr") trimmed.lowercase(turkishLocale) else trimmed.lowercase(englishLocale)
    }

    private fun inCanonicalLength(word: String): Boolean = word.length in MIN_CANONICAL_LENGTH..MAX_CANONICAL_LENGTH

    fun hasSnapshot(language: String): Boolean = snapshots.containsKey(canonicalLanguage(language))

    /** Restore the last complete canonical snapshot without network access. */
    fun restorePersisted(context: Context, language: String): Boolean {
        val lang = canonicalLanguage(language)
        if (snapshots.containsKey(lang)) return true
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(WORDS_PREFIX + lang, null)
            .orEmpty()
        if (raw.isBlank()) return false
        val indexed = raw.lineSequence()
            .map { normalize(it, lang) }
            .filter(::inCanonicalLength)
            .toHashSet()
        if (indexed.isEmpty()) return false
        snapshots[lang] = indexed
        return true
    }

    private fun persist(context: Context, language: String, words: Set<String>) {
        val lang = canonicalLanguage(language)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(WORDS_PREFIX + lang, words.sorted().joinToString("\n"))
            .apply()
    }

    private suspend fun fetchCanonical(language: String): Set<String> {
        val lang = canonicalLanguage(language)
        val payload = SupabaseProvider.client.postgrest.rpc(
            "get_dictionary_snapshot_v3",
            buildJsonObject { put("p_language", lang) },
        ).decodeSingle<DictionarySnapshotDto>()
        require(payload.language == lang) { "canonical_dictionary_language_mismatch" }
        val indexed = payload.words.asSequence()
            .map { normalize(it, lang) }
            .filter(::inCanonicalLength)
            .toHashSet()
        require(indexed.isNotEmpty()) { "canonical_dictionary_empty" }
        snapshots[lang] = indexed
        return indexed
    }

    suspend fun preload(language: String): Set<String> {
        val lang = canonicalLanguage(language)
        snapshots[lang]?.let { return it }
        return fetchCanonical(lang)
    }

    /**
     * Restore locally first for instant/offline availability, then refresh from the authoritative
     * backend. If refresh fails, the previously verified local snapshot remains usable.
     */
    suspend fun preloadCanonical(context: Context, language: String): Set<String> {
        val lang = canonicalLanguage(language)
        restorePersisted(context, lang)
        val refreshed = runCatching { fetchCanonical(lang) }.getOrNull()
        if (refreshed != null) {
            persist(context, lang, refreshed)
            return refreshed
        }
        return snapshots[lang] ?: throw IllegalStateException("canonical_dictionary_unavailable")
    }

    suspend fun isValidWord(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (!inCanonicalLength(normalized)) return false
        isValidCachedNormalized(normalized, language)?.let { return it }
        return normalized in preload(language)
    }

    /**
     * Synchronous bridge used by local practice after the screen has loaded/restored the canonical
     * snapshot. With no snapshot we fail closed and let UI report dictionary-unavailable.
     */
    fun isValidWordBlocking(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (!inCanonicalLength(normalized)) return false
        return isValidCachedNormalized(normalized, language) ?: false
    }

    fun isValidCached(word: String, language: String): Boolean? {
        val normalized = normalize(word, language)
        if (!inCanonicalLength(normalized)) return false
        return isValidCachedNormalized(normalized, language)
    }

    private fun isValidCachedNormalized(normalized: String, language: String): Boolean? {
        val lang = canonicalLanguage(language)
        return snapshots[lang]?.contains(normalized)
    }

    /** Bot candidates use exactly the same loaded canonical snapshot as human validation. */
    fun practiceCandidates(language: String, rack: String, limit: Int = 420): List<String> {
        val lang = canonicalLanguage(language)
        val words = snapshots[lang] ?: return emptyList()
        val locale = if (lang == "tr") turkishLocale else englishLocale
        val rackUpper = rack.uppercase(locale)
        return words.asSequence()
            .filter { it.length in 2..7 }
            .map { it.uppercase(locale) }
            .filter { candidate -> missingLetters(candidate, rackUpper) <= 1 }
            .sortedWith(compareByDescending<String> { it.length }.thenBy { it })
            .take(limit.coerceIn(50, 1000))
            .toList()
    }

    private fun missingLetters(word: String, rack: String): Int {
        val available = rack.groupingBy { it }.eachCount().toMutableMap()
        var missing = 0
        word.forEach { letter ->
            val count = available[letter] ?: 0
            if (count > 0) available[letter] = count - 1 else missing += 1
        }
        return missing
    }

    internal fun installSnapshotForTests(language: String, words: Collection<String>) {
        val lang = canonicalLanguage(language)
        snapshots[lang] = words.asSequence()
            .map { normalize(it, lang) }
            .filter(::inCanonicalLength)
            .toHashSet()
    }

    internal fun clearForTests() = snapshots.clear()
}
