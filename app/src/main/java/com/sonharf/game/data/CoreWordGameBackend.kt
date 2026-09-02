package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class CoreWordCandidateDto(
    val word: String,
    @SerialName("normalized_word") val normalizedWord: String,
)

/** Client-side word validation for Son Harf uses the same canonical snapshot as Word Siege. */
suspend fun OnlineGameBackend.validateCoreWord(word: String, language: String): Boolean =
    SharedDictionaryService.isValidWord(word, language)

suspend fun OnlineGameBackend.getCoreWordCandidates(
    letters: String,
    language: String,
    limit: Int = 160,
): List<CoreWordCandidateDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_core_word_candidates_v1",
        buildJsonObject {
            put("p_letters", letters)
            put("p_language", SharedDictionaryService.canonicalLanguage(language))
            put("p_limit", limit.coerceIn(10, 300))
        },
    ).decodeList()
