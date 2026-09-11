package com.sonharf.game.data

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class DictionarySnapshotDto(
    val language: String,
    val words: List<String>,
)

@Serializable
private data class TdkAutocompleteDto(
    val madde: String,
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
 * public.dictionary_words remains the single source of truth for human game validation. Turkish AI
 * opponents use an additional fail-closed presentation gate: a canonical word must also be an exact
 * headword in TDK's official autocomplete dataset before a bot may visibly play it. This prevents the
 * broad spell-check corpus from making bots choose generated-looking inflections while preserving the
 * wider human-play dictionary.
 */
object SharedDictionaryService {
    private const val PREFS = "son_harf_dictionary_snapshot_v4"
    private const val WORDS_PREFIX = "words_"
    private const val BOT_WORDS_PREFIX = "bot_words_"
    private const val MIN_SNAPSHOT_LENGTH = 2
    private const val MAX_SNAPSHOT_LENGTH = 15
    private const val MAX_ONLINE_LENGTH = 30
    private const val TDK_AUTOCOMPLETE_URL = "https://sozluk.gov.tr/autocomplete.json"
    private const val TDK_BOT_MINIMUM_WORDS = 5_000
    private val snapshots = ConcurrentHashMap<String, Set<String>>()
    private val botSnapshots = ConcurrentHashMap<String, Set<String>>()
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.ENGLISH
    private val json = Json { ignoreUnknownKeys = true }

    // Entries confirmed as dictionary-source leakage must never be accepted, even from a persisted
    // snapshot produced before the backend correction reached the device.
    private val turkishKnownInvalidWords = setOf(
        "amlat",
    )

    // Bot-only presentation policy. These can remain valid human-play words; practice/AI opponents
    // simply do not choose them. Keeping this separate from dictionary validity preserves player choice.
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

    private fun isKnownInvalidNormalized(word: String, language: String): Boolean =
        canonicalLanguage(language) == "tr" && word in turkishKnownInvalidWords

    /**
     * True for words an automated opponent may visibly play.
     *
     * Turkish bots fail closed: if the TDK-headword snapshot is unavailable, they do not invent or
     * guess words. Human dictionary validity remains completely separate.
     */
    fun isBotAllowedWord(word: String, language: String): Boolean {
        val lang = canonicalLanguage(language)
        val normalized = normalize(word, lang)
        if (isKnownInvalidNormalized(normalized, lang)) return false
        if (lang != "tr") return true
        if (normalized in turkishBotExcludedWords) return false
        return botSnapshots[lang]?.contains(normalized) == true
    }

    fun hasSnapshot(language: String): Boolean = snapshots.containsKey(canonicalLanguage(language))

    fun hasBotSnapshot(language: String): Boolean {
        val lang = canonicalLanguage(language)
        return if (lang == "tr") {
            botSnapshots[lang]?.isNotEmpty() == true
        } else {
            snapshots[lang]?.isNotEmpty() == true
        }
    }

    /** Restore the last complete, previously verified canonical snapshot without network access. */
    fun restorePersisted(context: Context, language: String): Boolean {
        val lang = canonicalLanguage(language)
        if (!snapshots.containsKey(lang)) {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(WORDS_PREFIX + lang, null)
                .orEmpty()
            if (raw.isBlank()) return false
            val indexed = raw.lineSequence()
                .map { normalize(it, lang) }
                .filter(::inSnapshotLength)
                .filter { validCharacters(it, lang) }
                .filterNot { lang == "tr" && it.endsWith('ğ') }
                .filterNot { isKnownInvalidNormalized(it, lang) }
                .toHashSet()
            if (indexed.isEmpty()) return false
            snapshots[lang] = indexed
        }

        if (lang == "tr") {
            restorePersistedBotSnapshot(context, lang)
        } else {
            snapshots[lang]?.let { botSnapshots[lang] = it }
        }
        return true
    }

    private fun restorePersistedBotSnapshot(context: Context, language: String): Boolean {
        val lang = canonicalLanguage(language)
        if (lang != "tr") return false
        if (botSnapshots[lang]?.isNotEmpty() == true) return true
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(BOT_WORDS_PREFIX + lang, null)
            .orEmpty()
        if (raw.isBlank()) return false
        val canonical = snapshots[lang] ?: return false
        val indexed = raw.lineSequence()
            .map { normalize(it, lang) }
            .filter(::inSnapshotLength)
            .filter { validCharacters(it, lang) }
            .filter { it in canonical }
            .filterNot { it.endsWith('ğ') }
            .filterNot { isKnownInvalidNormalized(it, lang) }
            .filterNot { it in turkishBotExcludedWords }
            .toHashSet()
        if (indexed.isEmpty()) return false
        botSnapshots[lang] = indexed
        return true
    }

    private fun persist(context: Context, language: String, words: Set<String>) {
        val lang = canonicalLanguage(language)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(WORDS_PREFIX + lang, words.sorted().joinToString("\n"))
            .apply()
    }

    private fun persistBotSnapshot(context: Context, language: String, words: Set<String>) {
        val lang = canonicalLanguage(language)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(BOT_WORDS_PREFIX + lang, words.sorted().joinToString("\n"))
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
            .filterNot { isKnownInvalidNormalized(it, lang) }
            .toHashSet()
        require(indexed.isNotEmpty()) { "canonical_dictionary_empty" }
        snapshots[lang] = indexed
        return indexed
    }

    private suspend fun fetchVerifiedTurkishBotSnapshot(canonicalWords: Set<String>): Set<String> {
        var lastFailure: Throwable? = null
        for (attempt in 0 until 3) {
            try {
                return fetchVerifiedTurkishBotSnapshotOnce(canonicalWords)
            } catch (failure: Throwable) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                lastFailure = failure
                if (attempt < 2) delay(300L * (attempt + 1))
            }
        }
        throw lastFailure ?: IllegalStateException("tdk_bot_dictionary_unavailable")
    }

    private suspend fun fetchVerifiedTurkishBotSnapshotOnce(canonicalWords: Set<String>): Set<String> = withContext(Dispatchers.IO) {
        val connection = (URL(TDK_AUTOCOMPLETE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "tr-TR,tr;q=0.9")
            setRequestProperty("User-Agent", "SonHarf/Android")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("tdk_autocomplete_http_${connection.responseCode}")
            }
            val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val entries = json.decodeFromString<List<TdkAutocompleteDto>>(payload)
            val verified = buildTurkishBotSnapshot(canonicalWords, entries.asSequence().map { it.madde }.asIterable())
            require(verified.size >= TDK_BOT_MINIMUM_WORDS) { "tdk_bot_dictionary_incomplete" }
            verified
        } finally {
            connection.disconnect()
        }
    }

    internal fun buildTurkishBotSnapshot(canonicalWords: Set<String>, tdkHeadwords: Iterable<String>): Set<String> {
        val normalizedCanonical = canonicalWords.asSequence()
            .map { normalize(it, "tr") }
            .filter(::inSnapshotLength)
            .filter { validCharacters(it, "tr") }
            .filterNot { it.endsWith('ğ') }
            .filterNot { isKnownInvalidNormalized(it, "tr") }
            .toHashSet()
        return tdkHeadwords.asSequence()
            .map { normalize(it, "tr") }
            .filter(::inSnapshotLength)
            .filter { validCharacters(it, "tr") }
            .filter { it in normalizedCanonical }
            .filterNot { it.endsWith('ğ') }
            .filterNot { isKnownInvalidNormalized(it, "tr") }
            .filterNot { it in turkishBotExcludedWords }
            .toHashSet()
    }

    suspend fun preload(language: String): Set<String> {
        val lang = canonicalLanguage(language)
        snapshots[lang]?.let { return it }
        return fetchCanonical(lang)
    }

    /**
     * Restore locally first for instant/offline availability, then refresh from the authoritative
     * backend. Turkish bot words are independently refreshed against official TDK headwords and are
     * persisted for offline play. If no verified bot snapshot exists, practice fails closed instead of
     * letting an automated opponent expose an unverified corpus entry.
     */
    suspend fun preloadCanonical(context: Context, language: String): Set<String> {
        val lang = canonicalLanguage(language)
        restorePersisted(context, lang)
        val refreshed = runCatching { fetchCanonical(lang) }.getOrNull()
        val canonical = refreshed ?: snapshots[lang] ?: throw IllegalStateException("canonical_dictionary_unavailable")
        if (refreshed != null) persist(context, lang, refreshed)

        if (lang == "tr") {
            val botRefreshed = runCatching { fetchVerifiedTurkishBotSnapshot(canonical) }.getOrNull()
            if (botRefreshed != null) {
                botSnapshots[lang] = botRefreshed
                persistBotSnapshot(context, lang, botRefreshed)
            } else {
                botSnapshots[lang]?.let { previous ->
                    val stillCanonical = previous.filterTo(hashSetOf()) { it in canonical }
                    if (stillCanonical.isNotEmpty()) botSnapshots[lang] = stillCanonical
                }
            }
            if (!hasBotSnapshot(lang)) throw IllegalStateException("verified_bot_dictionary_unavailable")
        } else {
            botSnapshots[lang] = canonical
        }
        return canonical
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
        if (isKnownInvalidNormalized(normalized, lang)) {
            return GameWordValidationDto(false, "known_invalid_entry", normalized, normalized.take(1), normalized.takeLast(1), normalized.length)
        }
        if (lang == "tr" && normalized.endsWith('ğ')) {
            return GameWordValidationDto(false, "ends_with_soft_g", normalized, normalized.take(1), normalized.takeLast(1), normalized.length)
        }
        return SupabaseProvider.client.postgrest.rpc(
            "validate_game_word_v2",
            buildJsonObject {
                put("p_word", normalized)
                put("p_language", lang)
            },
        ).decodeSingle()
    }

    /** Snapshot validation for offline/practice surfaces. Online duel must use validateAuthoritative. */
    suspend fun isValidWord(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (!inSnapshotLength(normalized) || !validCharacters(normalized, language)) return false
        if (isKnownInvalidNormalized(normalized, language)) return false
        if (canonicalLanguage(language) == "tr" && normalized.endsWith('ğ')) return false
        isValidCachedNormalized(normalized, language)?.let { return it }
        return normalized in preload(language)
    }

    fun isValidWordBlocking(word: String, language: String): Boolean {
        val normalized = normalize(word, language)
        if (!inSnapshotLength(normalized) || !validCharacters(normalized, language)) return false
        if (isKnownInvalidNormalized(normalized, language)) return false
        if (canonicalLanguage(language) == "tr" && normalized.endsWith('ğ')) return false
        return isValidCachedNormalized(normalized, language) ?: false
    }

    fun isValidCached(word: String, language: String): Boolean? {
        val normalized = normalize(word, language)
        if (!inSnapshotLength(normalized) || !validCharacters(normalized, language)) return false
        if (isKnownInvalidNormalized(normalized, language)) return false
        if (canonicalLanguage(language) == "tr" && normalized.endsWith('ğ')) return false
        return isValidCachedNormalized(normalized, language)
    }

    private fun isValidCachedNormalized(normalized: String, language: String): Boolean? {
        val lang = canonicalLanguage(language)
        return snapshots[lang]?.contains(normalized)
    }

    /** Bot candidates are canonical words and, for Turkish, exact TDK headwords. */
    fun practiceCandidates(language: String, rack: String, limit: Int = 420): List<String> {
        val lang = canonicalLanguage(language)
        val words = if (lang == "tr") botSnapshots[lang] else snapshots[lang] ?: return emptyList()
        if (words == null) return emptyList()
        val locale = if (lang == "tr") turkishLocale else englishLocale
        val rackUpper = rack.uppercase(locale)
        return words.asSequence()
            .filter { it.length in 2..7 }
            .filter { isBotAllowedWord(it, lang) }
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

    internal fun installSnapshotForTests(
        language: String,
        words: Collection<String>,
        botWords: Collection<String>? = null,
    ) {
        val lang = canonicalLanguage(language)
        val canonical = words.asSequence()
            .map { normalize(it, lang) }
            .filter(::inSnapshotLength)
            .filter { validCharacters(it, lang) }
            .filterNot { lang == "tr" && it.endsWith('ğ') }
            .filterNot { isKnownInvalidNormalized(it, lang) }
            .toHashSet()
        snapshots[lang] = canonical
        botSnapshots[lang] = (botWords ?: words).asSequence()
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
