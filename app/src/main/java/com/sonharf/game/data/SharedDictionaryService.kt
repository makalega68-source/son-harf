package com.sonharf.game.data

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
 * Online/server-backed modes use public.dictionary_words. A complete active snapshot is fetched once per
 * language and indexed as an in-memory HashSet. Local practice never requires network access: it uses an
 * already-cached canonical snapshot when available and otherwise falls back to a bundled practice lexicon.
 * Server-authoritative online moves continue to validate against the canonical table.
 */
object SharedDictionaryService {
    private val snapshots = ConcurrentHashMap<String, Set<String>>()
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.ENGLISH

    private val offlinePracticeTurkish by lazy {
        setOf(
            "ada", "adım", "aile", "akıl", "alan", "altın", "ana", "anne", "ara", "arı", "at", "ateş",
            "baba", "bağ", "bal", "baş", "bilgi", "bir", "biz", "bul", "buz",
            "cam", "can", "cep", "çay", "çiçek", "çizgi", "çocuk",
            "dal", "dam", "deniz", "ders", "dil", "dost", "duvar",
            "el", "elma", "emek", "ev", "fikir", "gemi", "genç", "göl", "gün", "güneş",
            "harf", "hava", "hayat", "hız", "ışık", "isim", "insan", "iş",
            "kale", "kalem", "kalp", "kan", "kapı", "kar", "kart", "kedi", "kelam", "kelime", "kitap", "kol", "köprü", "kuş",
            "masa", "masal", "mart", "merak", "metal", "mor", "mutlu", "nar", "not", "oda", "okul", "oyun",
            "renk", "resim", "saat", "sana", "ses", "sıra", "sinema", "soru", "su", "tahta", "tam", "tarla", "ter", "top", "tur",
            "umut", "uzun", "var", "yaz", "yeni", "yol", "yıldız", "zaman"
        ).mapTo(hashSetOf()) { normalize(it, "tr") }
    }

    private val offlinePracticeEnglish by lazy {
        setOf(
            "air", "apple", "art", "book", "bridge", "card", "cat", "cloud", "day", "dear", "desk", "door",
            "earth", "family", "game", "garden", "gain", "green", "hand", "home", "house", "idea", "lake", "late", "letter",
            "light", "line", "music", "name", "orange", "page", "planet", "plan", "play", "rain", "read", "road", "room",
            "star", "story", "table", "time", "tree", "water", "window", "word", "world"
        ).mapTo(hashSetOf()) { normalize(it, "en") }
    }

    fun canonicalLanguage(language: String): String = if (language.lowercase(Locale.ROOT) == "en") "en" else "tr"

    fun normalize(word: String, language: String): String {
        val lang = canonicalLanguage(language)
        val trimmed = word.trim()
        return if (lang == "tr") trimmed.lowercase(turkishLocale) else trimmed.lowercase(englishLocale)
    }

    private fun offlinePracticeWords(language: String): Set<String> =
        if (canonicalLanguage(language) == "en") offlinePracticeEnglish else offlinePracticeTurkish

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
        isValidCachedNormalized(normalized, language)?.let { return it }
        return normalized in preload(language)
    }

    /**
     * Deterministic, network-free bridge for local practice.
     * A canonical snapshot already loaded by another mode is preferred. If none exists, the bundled
     * practice lexicon is used and no Supabase/RPC call is attempted.
     */
    fun isValidWordBlocking(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (normalized.length !in 3..12) return false
        isValidCachedNormalized(normalized, language)?.let { return it }
        return normalized in offlinePracticeWords(language)
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

    /** Bot candidates for local practice. This path is intentionally network-free. */
    fun practiceCandidates(language: String, rack: String, limit: Int = 420): List<String> {
        val lang = canonicalLanguage(language)
        val words = snapshots[lang] ?: offlinePracticeWords(lang)
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
