package com.sonharf.game.data

import java.util.Locale

/**
 * Unified Pro bilingual scoring facade. Acceptance remains owned by
 * SharedDictionaryService so local UI and server canonical rules cannot diverge.
 */
object DictionaryEngine {
    suspend fun isValidWord(word: String, language: String): Boolean =
        SharedDictionaryService.isValidWord(word, language)

    fun isValidWordBlocking(word: String, language: String): Boolean =
        SharedDictionaryService.isValidWordBlocking(word, language)

    fun normalize(word: String, language: String): String {
        val canonical = SharedDictionaryService.canonicalLanguage(language)
        val locale = if (canonical == "tr") Locale.forLanguageTag("tr-TR") else Locale.ROOT
        var cleaned = word.trim().uppercase(locale)
        if (canonical == "tr") {
            cleaned = cleaned.replace("Â", "A").replace("Î", "I").replace("Û", "U")
        }
        return cleaned.filter { it.isLetter() }
    }

    fun calculatePoints(word: String, language: String): Int =
        normalize(word, language).sumOf { getLetterPoint(it, language) }

    fun getLetterPoint(letter: Char, language: String): Int =
        if (SharedDictionaryService.canonicalLanguage(language) == "tr") {
            when (letter.toString().uppercase(Locale.forLanguageTag("tr-TR")).first()) {
                'A', 'E', 'İ', 'K', 'L', 'M', 'N', 'R', 'T' -> 1
                'I', 'S', 'U', 'Y' -> 2
                'B', 'D', 'O', 'Ü' -> 3
                'C', 'Ç', 'Ş', 'Z' -> 4
                'G', 'H', 'P' -> 5
                'F', 'Ö', 'V' -> 7
                'Ğ' -> 8
                'J' -> 10
                else -> 1
            }
        } else {
            when (letter.uppercaseChar()) {
                'E', 'A', 'I', 'O', 'N', 'R', 'T', 'L', 'S', 'U' -> 1
                'D', 'G' -> 2
                'B', 'C', 'M', 'P' -> 3
                'F', 'H', 'V', 'W', 'Y' -> 4
                'K' -> 5
                'J', 'X' -> 8
                'Q', 'Z' -> 10
                else -> 1
            }
        }
}
