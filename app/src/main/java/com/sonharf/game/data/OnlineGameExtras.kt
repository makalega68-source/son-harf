package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class UserBlockDto(
    @SerialName("blocker_id") val blockerId: String,
    @SerialName("blocked_id") val blockedId: String,
    @SerialName("created_at") val createdAt: String? = null,
)

data class LeaderboardEntry(
    val profile: ProfileDto,
    val matches: Int,
    val winRate: Int,
)

suspend fun OnlineGameBackend.createPrivateRoom(language: String): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "create_room",
        buildJsonObject { put("p_language", language) },
    ).decodeSingle()

suspend fun OnlineGameBackend.joinPrivateRoom(code: String): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "join_room_by_code",
        buildJsonObject { put("p_code", code.trim().uppercase()) },
    ).decodeSingle()

suspend fun OnlineGameBackend.inviteFriendToPrivateRoom(roomId: String, friendId: String): GameInviteDto =
    SupabaseProvider.client.postgrest.rpc(
        "invite_friend_to_private_room",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_friend_id", friendId)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.unblockUser(userId: String) {
    SupabaseProvider.client.postgrest.rpc(
        "unblock_user",
        buildJsonObject { put("p_blocked_id", userId) },
    )
}

suspend fun OnlineGameBackend.getBlockedUsers(): List<ProfileDto> {
    val me = currentUserId() ?: return emptyList()
    val rows = SupabaseProvider.client.from("user_blocks")
        .select { filter { eq("blocker_id", me) } }
        .decodeList<UserBlockDto>()
    return rows.mapNotNull { row -> runCatching { getProfile(row.blockedId) }.getOrNull() }
}

suspend fun OnlineGameBackend.getLeaderboard(limit: Int = 50): List<LeaderboardEntry> =
    SupabaseProvider.client.from("profiles")
        .select()
        .decodeList<ProfileDto>()
        .map { p ->
            val matches = p.wins + p.losses
            LeaderboardEntry(p, matches, if (matches == 0) 0 else (p.wins * 100 / matches))
        }
        .sortedWith(compareByDescending<LeaderboardEntry> { it.profile.wins }.thenByDescending { it.winRate })
        .take(limit)
