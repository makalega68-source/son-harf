package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal suspend fun validateWordSiegeDictionaryWord(
    word: String,
    language: String = "tr",
): Boolean {
    if (!SupabaseProvider.configured) return false
    return SupabaseProvider.client.postgrest.rpc(
        "validate_word_siege_word_v1",
        buildJsonObject {
            put("p_word", word.trim())
            put("p_language", language)
        },
    ).decodeAs<Boolean>()
}

internal suspend fun fetchWordSiegeBotLexicon(
    availableLetters: String,
    language: String = "tr",
    limit: Int = 900,
): List<String> {
    if (!SupabaseProvider.configured) return emptyList()
    return SupabaseProvider.client.postgrest.rpc(
        "word_siege_bot_lexicon_v1",
        buildJsonObject {
            put("p_letters", availableLetters)
            put("p_language", language)
            put("p_limit", limit.coerceIn(50, 1200))
        },
    ).decodeList<String>()
}
