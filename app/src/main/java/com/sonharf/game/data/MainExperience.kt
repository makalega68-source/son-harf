package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Small, existing-schema adapters used by the active non-match application shell. */
suspend fun OnlineGameBackend.searchPlayers(
    query: String,
    limit: Int = 20,
): List<ProfileDto> {
    val clean = query
        .trim()
        .filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".-_" }
        .take(24)
    if (clean.length < 2) return emptyList()

    val me = currentUserId()
    return SupabaseProvider.client.from("profiles")
        .select {
            filter { ilike("display_name", "%$clean%") }
            limit(count = limit.coerceIn(1, 30).toLong())
        }
        .decodeList<ProfileDto>()
        .filterNot { it.id == me }
}

suspend fun OnlineGameBackend.removeFriend(friendId: String) {
    SupabaseProvider.client.postgrest.rpc(
        "remove_friend",
        buildJsonObject { put("p_friend_id", friendId) },
    )
}

suspend fun OnlineGameBackend.setAvatarVisibility(hidden: Boolean): ProfileDto =
    SupabaseProvider.client.postgrest.rpc(
        "set_avatar_visibility",
        buildJsonObject { put("p_hidden", hidden) },
    ).decodeSingle()
