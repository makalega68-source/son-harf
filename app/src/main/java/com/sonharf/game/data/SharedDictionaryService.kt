package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
 * The authoritative dataset is public.dictionary_words. A complete active snapshot is fetched once per
 * language and indexed as an in-memory HashSet, so gameplay never performs a network request per tile.
 * Server-authoritative online moves continue to validate against the same table.
 */
object SharedDictionaryService {
    private val snapshots = ConcurrentHashMap<String, Set<String>>()
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.ENGLISH

    fun canonicalLanguage(language: String): String = if (language.lowercase(Locale.ROOT) == "en") "en" else "tr"

    fun normalize(word: String, language: String): String {
        val lang = canonicalLanguage(language)
        val trimmed = word.trim()
        return if (lang == "tr") trimmed.lowercase(turkishLocale) else trimmed.lowercase(englishLocale)
    }

    suspend fun preload(language: String): Set<String> {
        val lang = canonicalLanguage(language)
        snapshots[lang]?.let { return it }
        val payload = SupabaseProvider.client.postgrest.rpc(
            "get_dictionary_snapshot_v1",
            buildJsonObject { put("p_language", lang) },
        ).decodeSingle<DictionarySnapshotDto>()
        val indexed = payload.words.asSequence()
            .map { normalize(it, lang) }
            .filter { it.length in 3..12 }
            .toHashSet()
        snapshots[lang] = indexed
        return indexed
    }

    suspend fun isValidWord(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (normalized.length !in 3..12) return false
        return normalized in preload(language)
    }

    /** Synchronous bridge for the deterministic local practice engine. The first lookup loads one snapshot. */
    fun isValidWordBlocking(word: String, language: String): Boolean =
        runBlocking(Dispatchers.IO) { isValidWord(word, language) }

    fun isValidCached(word: String, language: String): Boolean? {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)
        if (normalized.length !in 3..12) return false
        return snapshots[lang]?.contains(normalized)
    }

    fun practiceCandidates(language: String, rack: String, limit: Int = 420): List<String> {
        val lang = canonicalLanguage(language)
        val words = snapshots[lang] ?: runBlocking(Dispatchers.IO) { preload(lang) }
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
