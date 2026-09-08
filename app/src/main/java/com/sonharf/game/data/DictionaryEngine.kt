package com.sonharf.game.data

import java.util.Locale

/**
 * Small compatibility facade for UI/gameplay code that needs bilingual scoring
 * without creating a second dictionary authority.
 *
 * Word acceptance always delegates to [SharedDictionaryService], which is backed
 * by the canonical Supabase dictionary snapshot used by every active game mode.
 */
object DictionaryEngine {
    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    fun normalize(word: String, language: String): String {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        val locale = if (lang == "tr") turkishLocale else Locale.ROOT
        return SharedDictionaryService.normalize(word, lang).uppercase(locale)
    }

    fun isValidWord(word: String, language: String): Boolean =
        SharedDictionaryService.isValidWordBlocking(word, language)

    suspend fun isValidWordAsync(word: String, language: String): Boolean =
        SharedDictionaryService.isValidWord(word, language)

    fun calculatePoints(word: String, language: String): Int =
        normalize(word, language).sumOf { getLetterPoint(it, language) }

    fun getLetterPoint(letter: Char, language: String): Int {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        val normalized = normalize(letter.toString(), lang).firstOrNull() ?: return 0
        return if (lang == "tr") {
            when (normalized) {
                'A', 'E', 'İ', 'K', 'L', 'M', 'N', 'R', 'T' -> 1
                'I', 'S', 'U', 'Y' -> 2
                'B', 'D', 'O', 'Ü' -> 3
                'C', 'Ç', 'Ş', 'Z' -> 4
                'G', 'H', 'P' -> 5
                'F', 'Ö', 'V' -> 7
                'Ğ' -> 8
                'J' -> 10
                else -> 0
            }
        } else {
            when (normalized) {
                'E', 'A', 'I', 'O', 'N', 'R', 'T', 'L', 'S', 'U' -> 1
                'D', 'G' -> 2
                'B', 'C', 'M', 'P' -> 3
                'F', 'H', 'V', 'W', 'Y' -> 4
                'K' -> 5
                'J', 'X' -> 8
                'Q', 'Z' -> 10
                else -> 0
            }
        }
    }
}
