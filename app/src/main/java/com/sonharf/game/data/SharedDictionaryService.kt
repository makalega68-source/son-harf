package com.sonharf.game.data

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class DictionarySnapshotDto(
    val language: String,
    val words: List<String>,
)

@Serializable
data class GameWordValidationDto(
    val valid: Boolean,
    val reason: String,
    @SerialName("normalized_word") val normalizedWord: String,
    @SerialName("first_letter") val firstLetter: String,
    @SerialName("last_letter") val lastLetter: String,
    @SerialName("char_length") val charLength: Int,
)

/**
 * Canonical dictionary gateway shared by all Son Harf word modes.
 *
 * public.dictionary_words is the single source of truth. V4 keeps Turkish and English isolated,
 * language-normalized and game-filtered. The 2..15 snapshot is an offline continuity cache for board
 * modes; online Premier duel validation is authoritative through validate_game_word_v2 and supports
 * words up to 30 characters. No reduced or synthetic fallback lexicon is accepted.
 */
object SharedDictionaryService {
    private const val PREFS = "son_harf_dictionary_snapshot_v4"
    private const val WORDS_PREFIX = "words_"
    private const val MIN_SNAPSHOT_LENGTH = 2
    private const val MAX_SNAPSHOT_LENGTH = 15
    private const val MAX_ONLINE_LENGTH = 30
    private val snapshots = ConcurrentHashMap<String, Set<String>>()
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.ENGLISH

    fun canonicalLanguage(language: String): String = if (language.lowercase(Locale.ROOT) == "en") "en" else "tr"

    fun normalize(word: String, language: String): String {
        val lang = canonicalLanguage(language)
        val nfc = Normalizer.normalize(word, Normalizer.Form.NFC)
        val cleaned = nfc
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace('\u00A0', ' ')
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .trim()
        val lower = if (lang == "tr") cleaned.lowercase(turkishLocale) else cleaned.lowercase(englishLocale)
        return Normalizer.normalize(lower, Normalizer.Form.NFC)
    }

    private fun validCharacters(word: String, language: String): Boolean = when (canonicalLanguage(language)) {
        "en" -> word.all { it in 'a'..'z' }
        else -> word.all { it in "abcçdefgğhıijklmnoöprsştuüvyz" }
    }

    private fun inSnapshotLength(word: String): Boolean = word.length in MIN_SNAPSHOT_LENGTH..MAX_SNAPSHOT_LENGTH

    fun hasSnapshot(language: String): Boolean = snapshots.containsKey(canonicalLanguage(language))

    /** Restore the last complete, previously verified canonical snapshot without network access. */
    fun restorePersisted(context: Context, language: String): Boolean {
        val lang = canonicalLanguage(language)
        if (snapshots.containsKey(lang)) return true
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(WORDS_PREFIX + lang, null)
            .orEmpty()
        if (raw.isBlank()) return false
        val indexed = raw.lineSequence()
            .map { normalize(it, lang) }
            .filter(::inSnapshotLength)
            .filter { validCharacters(it, lang) }
            .filterNot { lang == "tr" && it.endsWith('ğ') }
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
            "get_dictionary_snapshot_v4",
            buildJsonObject { put("p_language", lang) },
        ).decodeSingle<DictionarySnapshotDto>()
        require(payload.language == lang) { "canonical_dictionary_language_mismatch" }
        val indexed = payload.words.asSequence()
            .map { normalize(it, lang) }
            .filter(::inSnapshotLength)
            .filter { validCharacters(it, lang) }
            .filterNot { lang == "tr" && it.endsWith('ğ') }
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

    /** Authoritative online validation used by Premier duel and any server-backed word submission. */
    suspend fun validateAuthoritative(word: String, language: String): GameWordValidationDto {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)
        if (normalized.length !in MIN_SNAPSHOT_LENGTH..MAX_ONLINE_LENGTH || !validCharacters(normalized, lang)) {
            return GameWordValidationDto(
                valid = false,
                reason = if (normalized.length !in MIN_SNAPSHOT_LENGTH..MAX_ONLINE_LENGTH) "invalid_length" else "invalid_characters",
                normalizedWord = normalized,
                firstLetter = normalized.take(1),
                lastLetter = normalized.takeLast(1),
                charLength = normalized.length,
            )
        }
        if (lang == "tr" && normalized.endsWith('ğ')) {
            return GameWordValidationDto(false, "ends_with_soft_g", normalized, normalized.take(1), normalized.takeLast(1), normalized.length)
        }
        return SupabaseProvider.client.postgrest.rpc(
            "validate_game_word_v2",
            buildJsonObject {
                put("p_word", word.trim())
                put("p_language", lang)
            },
        ).decodeSingle()
    }

    /** Snapshot validation for offline/practice surfaces. Online duel must use validateAuthoritative. */
    suspend fun isValidWord(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (!inSnapshotLength(normalized) || !validCharacters(normalized, language)) return false
        if (canonicalLanguage(language) == "tr" && normalized.endsWith('ğ')) return false
        isValidCachedNormalized(normalized, language)?.let { return it }
        return normalized in preload(language)
    }

    fun isValidWordBlocking(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (!inSnapshotLength(normalized) || !validCharacters(normalized, language)) return false
        if (canonicalLanguage(language) == "tr" && normalized.endsWith('ğ')) return false
        return isValidCachedNormalized(normalized, language) ?: false
    }

    fun isValidCached(word: String, language: String): Boolean? {
        val normalized = normalize(word, language)
        if (!inSnapshotLength(normalized) || !validCharacters(normalized, language)) return false
        if (canonicalLanguage(language) == "tr" && normalized.endsWith('ğ')) return false
        return isValidCachedNormalized(normalized, language)
    }

    private fun isValidCachedNormalized(normalized: String, language: String): Boolean? {
        val lang = canonicalLanguage(language)
        return snapshots[lang]?.contains(normalized)
    }

    /** Bot candidates use exactly the same loaded canonical snapshot as human practice validation. */
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
            .filter(::inSnapshotLength)
            .filter { validCharacters(it, lang) }
            .filterNot { lang == "tr" && it.endsWith('ğ') }
            .toHashSet()
    }

    internal fun clearForTests() = snapshots.clear()
}
