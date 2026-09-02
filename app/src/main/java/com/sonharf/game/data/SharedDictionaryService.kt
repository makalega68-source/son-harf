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
 * Canonical dictionary gateway for every word-game mode.
 *
 * The single source of truth is public.dictionary_words via get_dictionary_snapshot_v2.
 * The v2 RPC deliberately returns one PostgREST record (language + words[]) instead of a scalar
 * jsonb object so supabase-kt decodeSingle has the same response contract as normal row queries.
 * Once fetched, the complete active snapshot is kept in memory and persisted on-device so local
 * practice can continue offline with the same vocabulary as the main game. There is deliberately
 * no reduced practice-only lexicon: a missing canonical snapshot is an unavailable-dictionary state,
 * never evidence that an otherwise valid Turkish word is invalid.
 */
object SharedDictionaryService {
    private const val PREFS = "son_harf_dictionary_snapshot_v2"
    private const val WORDS_PREFIX = "words_"
    private val snapshots = ConcurrentHashMap<String, Set<String>>()
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.ENGLISH

    fun canonicalLanguage(language: String): String = if (language.lowercase(Locale.ROOT) == "en") "en" else "tr"

    fun normalize(word: String, language: String): String {
        val lang = canonicalLanguage(language)
        val trimmed = word.trim()
        return if (lang == "tr") trimmed.lowercase(turkishLocale) else trimmed.lowercase(englishLocale)
    }

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
            .filter { it.length in 3..12 }
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

    suspend fun preload(language: String): Set<String> {
        val lang = canonicalLanguage(language)
        snapshots[lang]?.let { return it }
        val payload = SupabaseProvider.client.postgrest.rpc(
            "get_dictionary_snapshot_v2",
            buildJsonObject { put("p_language", lang) },
        ).decodeSingle<DictionarySnapshotDto>()
        require(payload.language == lang) { "canonical_dictionary_language_mismatch" }
        val indexed = payload.words.asSequence()
            .map { normalize(it, lang) }
            .filter { it.length in 3..12 }
            .toHashSet()
        require(indexed.isNotEmpty()) { "canonical_dictionary_empty" }
        snapshots[lang] = indexed
        return indexed
    }

    /** Restore locally first; otherwise fetch and persist the complete canonical dictionary. */
    suspend fun preloadCanonical(context: Context, language: String): Set<String> {
        val lang = canonicalLanguage(language)
        if (restorePersisted(context, lang)) return snapshots.getValue(lang)
        val words = preload(lang)
        persist(context, lang, words)
        return words
    }

    suspend fun isValidWord(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (normalized.length !in 3..12) return false
        isValidCachedNormalized(normalized, language)?.let { return it }
        return normalized in preload(language)
    }

    /**
     * Synchronous bridge used by the local practice engine after the screen has loaded/restored the
     * canonical snapshot. With no snapshot we fail closed so UI can report dictionary-unavailable;
     * we never consult a smaller, divergent word list.
     */
    fun isValidWordBlocking(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (normalized.length !in 3..12) return false
        return isValidCachedNormalized(normalized, language) ?: false
    }

    fun isValidCached(word: String, language: String): Boolean? {
        val normalized = normalize(word, language)
        if (normalized.length !in 3..12) return false
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
            .filter { it.length in 3..7 }
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
        snapshots[lang] = words.asSequence().map { normalize(it, lang) }.filter { it.length in 3..12 }.toHashSet()
    }

    internal fun clearForTests() = snapshots.clear()
}
