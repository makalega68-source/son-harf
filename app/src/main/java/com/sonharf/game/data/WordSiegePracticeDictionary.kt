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
