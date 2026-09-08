package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Persists non-sensitive player preferences through narrow RPCs so profile
 * server-authoritative fields remain protected by the existing database guards.
 */
suspend fun OnlineGameBackend.syncPreferredLanguage(language: String) {
    val canonical = SharedDictionaryService.canonicalLanguage(language)
    SupabaseProvider.client.postgrest.rpc(
        "set_preferred_language_v1",
        buildJsonObject { put("p_language", canonical) },
    )
}
