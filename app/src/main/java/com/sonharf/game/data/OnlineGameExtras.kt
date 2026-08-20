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
    val rankingScore: Int = 0,
)

@Serializable
private data class LanguageLeaderboardRow(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val wins: Int = 0,
    val losses: Int = 0,
    @SerialName("total_matches") val totalMatches: Int = 0,
    @SerialName("win_rate") val winRate: Int = 0,
    @SerialName("ranking_score") val rankingScore: Int = 0,
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
            val matches = p.totalMatches.takeIf { it > 0 } ?: (p.wins + p.losses)
            LeaderboardEntry(p, matches, if (matches == 0) 0 else (p.wins * 100 / matches), p.rating)
        }
        .sortedWith(compareByDescending<LeaderboardEntry> { it.profile.wins }.thenByDescending { it.winRate })
        .take(limit)

suspend fun OnlineGameBackend.getLanguageLeaderboard(
    language: String,
    period: String = "total",
    limit: Int = 50,
): List<LeaderboardEntry> {
    val lang = if (language.lowercase() == "en") "en" else "tr"
    val normalizedPeriod = when (period.lowercase()) {
        "week" -> "week"
        "month" -> "month"
        else -> "total"
    }
    val rows = SupabaseProvider.client.postgrest.rpc(
        "get_language_leaderboard",
        buildJsonObject {
            put("p_language", lang)
            put("p_period", normalizedPeriod)
            put("p_limit", limit.coerceIn(1, 100))
        },
    ).decodeList<LanguageLeaderboardRow>()

    return rows.map { row ->
        val profile = ProfileDto(
            id = row.userId,
            displayName = row.displayName,
            avatarUrl = row.avatarUrl,
            wins = row.wins,
            losses = row.losses,
            totalMatches = row.totalMatches,
        )
        LeaderboardEntry(profile, row.totalMatches, row.winRate, row.rankingScore)
    }
}
