package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest

/**
 * Series owners may select already-accepted friends for a Series invite without
 * gaining access to the general PRO friend-list surface.
 */
suspend fun OnlineGameBackend.getSeriesFriends(): List<Pair<FriendshipDto, ProfileDto>> {
    val me = currentUserId() ?: return emptyList()
    val rows = SupabaseProvider.client.postgrest.rpc("get_series_friendships_v2")
        .decodeList<FriendshipDto>()
    return rows.mapNotNull { friendship ->
        val other = if (friendship.userId == me) friendship.friendId else friendship.userId
        runCatching { friendship to getProfile(other) }.getOrNull()
    }
}
