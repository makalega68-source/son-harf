package com.sonharf.game.data

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class DictionarySnapshotDto(
    val language: String,
    val words: List<String>,
)

// Both dictionary RPCs return one JSON object, not a PostgREST row array.
internal inline fun <reified T> PostgrestResult.decodeDictionaryRpc(): T = decodeAs()

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
 * Single canonical dictionary gateway for every word game.
 *
 * Turkish and English both come from public.dictionary_words through the v5 snapshot/validation RPCs.
 * The Turkish corpus is populated only from official TDK headwords; English is populated from the
 * pinned SCOWL/English Speller Database source. Individual game modes must not ship or query their
 * own word lists.
 */
object SharedDictionaryService {
    private const val PREFS = "son_harf_dictionary_snapshot_v5"
    private const val WORDS_PREFIX = "words_"
    private const val MIN_WORD_LENGTH = 2
    private const val MAX_WORD_LENGTH = 30

    private val snapshots = ConcurrentHashMap<String, Set<String>>()
    private val botSnapshots = ConcurrentHashMap<String, Set<String>>()
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.ENGLISH

    // Presentation-only bot filter. These words remain valid if they exist in the master dictionary;
    // automated opponents simply avoid choosing them.
    private val turkishBotExcludedWords = setOf(
        "am",
        "penis",
        "sik",
        "sikmek",
        "sikiş",
        "sikişmek",
        "yarak",
        "yarrak",
        "göt",
        "taşak",
        "taşşak",
        "vajina",
        "vulva",
        "klitoris",
        "dildo",
        "porno",
        "pornografi",
        "orospu",
        "pezevenk",
    )

    fun canonicalLanguage(language: String): String =
        if (language.lowercase(Locale.ROOT) == "en") "en" else "tr"

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

    private fun validCharacters(word: String, language: String): Boolean =
        when (canonicalLanguage(language)) {
            "en" -> word.all { it in 'a'..'z' }
            else -> word.all { it in "abcçdefgğhıijklmnoöprsştuüvyz" }
        }

    private fun validNormalized(word: String, language: String): Boolean =
        word.length in MIN_WORD_LENGTH..MAX_WORD_LENGTH && validCharacters(word, language)

    private fun install(language: String, words: Set<String>) {
        val lang = canonicalLanguage(language)
        snapshots[lang] = words
        botSnapshots[lang] = if (lang == "tr") {
            words.filterNotTo(hashSetOf()) { it in turkishBotExcludedWords }
        } else {
            words
        }
    }

    fun isBotAllowedWord(word: String, language: String): Boolean {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)
        if (!validNormalized(normalized, lang)) return false
        return botSnapshots[lang]?.contains(normalized) == true
    }

    fun hasSnapshot(language: String): Boolean =
        snapshots[canonicalLanguage(language)]?.isNotEmpty() == true

    fun hasBotSnapshot(language: String): Boolean =
        botSnapshots[canonicalLanguage(language)]?.isNotEmpty() == true

    /** Restore the last complete v5 master snapshot without network access. */
    fun restorePersisted(context: Context, language: String): Boolean {
        val lang = canonicalLanguage(language)
        if (snapshots[lang]?.isNotEmpty() == true) return true

        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(WORDS_PREFIX + lang, null)
            .orEmpty()
        if (raw.isBlank()) return false

        val indexed = raw.lineSequence()
            .map { normalize(it, lang) }
            .filter { validNormalized(it, lang) }
            .toHashSet()
        if (indexed.isEmpty()) return false

        install(lang, indexed)
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
            "get_dictionary_snapshot_v5",
            buildJsonObject { put("p_language", lang) },
        ).decodeDictionaryRpc<DictionarySnapshotDto>()

        require(payload.language == lang) { "canonical_dictionary_language_mismatch" }

        val indexed = payload.words.asSequence()
            .map { normalize(it, lang) }
            .filter { validNormalized(it, lang) }
            .toHashSet()

        require(indexed.isNotEmpty()) { "canonical_dictionary_empty" }
        install(lang, indexed)
        return indexed
    }

    suspend fun preload(language: String): Set<String> {
        val lang = canonicalLanguage(language)
        snapshots[lang]?.let { return it }
        return fetchCanonical(lang)
    }

    /**
     * Restore locally first for fast startup, then refresh from the single authoritative backend.
     * Old v4 and earlier caches use a different preference namespace and are therefore never reused.
     */
    suspend fun preloadCanonical(context: Context, language: String): Set<String> {
        val lang = canonicalLanguage(language)
        restorePersisted(context, lang)
        val refreshed = runCatching { fetchCanonical(lang) }.getOrNull()
        val canonical = refreshed
            ?: snapshots[lang]
            ?: throw IllegalStateException("canonical_dictionary_unavailable")
        if (refreshed != null) persist(context, lang, refreshed)
        return canonical
    }

    /** Authoritative membership check for every server-backed word submission. */
    suspend fun validateAuthoritative(word: String, language: String): GameWordValidationDto {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)

        if (!validNormalized(normalized, lang)) {
            return GameWordValidationDto(
                valid = false,
                reason = if (normalized.length !in MIN_WORD_LENGTH..MAX_WORD_LENGTH) {
                    "invalid_length"
                } else {
                    "invalid_characters"
                },
                normalizedWord = normalized,
                firstLetter = normalized.take(1),
                lastLetter = normalized.takeLast(1),
                charLength = normalized.length,
            )
        }

        return SupabaseProvider.client.postgrest.rpc(
            "validate_game_word_v3",
            buildJsonObject {
                put("p_word", normalized)
                put("p_language", lang)
            },
        ).decodeDictionaryRpc()
    }

    /** Snapshot validation for offline/practice surfaces. */
    suspend fun isValidWord(word: String, language: String): Boolean {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)
        if (!validNormalized(normalized, lang)) return false
        isValidCachedNormalized(normalized, lang)?.let { return it }
        return normalized in preload(lang)
    }

    fun isValidWordBlocking(word: String, language: String): Boolean {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)
        if (!validNormalized(normalized, lang)) return false
        return isValidCachedNormalized(normalized, lang) ?: false
    }

    fun isValidCached(word: String, language: String): Boolean? {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)
        if (!validNormalized(normalized, lang)) return false
        return isValidCachedNormalized(normalized, lang)
    }

    private fun isValidCachedNormalized(normalized: String, language: String): Boolean? =
        snapshots[canonicalLanguage(language)]?.contains(normalized)

    /** Bot candidates always originate from the same master dictionary snapshot. */
    fun practiceCandidates(language: String, rack: String, limit: Int = 420): List<String> {
        val lang = canonicalLanguage(language)
        val words = botSnapshots[lang] ?: return emptyList()
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

    /**
     * Compatibility helper for existing tests. In v5 the canonical Turkish snapshot is already TDK-only,
     * so this simply returns the exact intersection and applies the presentation-only bot exclusion.
     */
    internal fun buildTurkishBotSnapshot(
        canonicalWords: Set<String>,
        tdkHeadwords: Iterable<String>,
    ): Set<String> {
        val canonical = canonicalWords.asSequence()
            .map { normalize(it, "tr") }
            .filter { validNormalized(it, "tr") }
            .toHashSet()

        return tdkHeadwords.asSequence()
            .map { normalize(it, "tr") }
            .filter { it in canonical }
            .filterNot { it in turkishBotExcludedWords }
            .toHashSet()
    }

    internal fun installSnapshotForTests(
        language: String,
        words: Collection<String>,
        botWords: Collection<String>? = null,
    ) {
        val lang = canonicalLanguage(language)
        val canonical = words.asSequence()
            .map { normalize(it, lang) }
            .filter { validNormalized(it, lang) }
            .toHashSet()
        snapshots[lang] = canonical

        val requestedBotWords = botWords ?: words
        botSnapshots[lang] = requestedBotWords.asSequence()
            .map { normalize(it, lang) }
            .filter { it in canonical }
            .filterNot { lang == "tr" && it in turkishBotExcludedWords }
            .toHashSet()
    }

    internal fun clearForTests() {
        snapshots.clear()
        botSnapshots.clear()
    }
}
