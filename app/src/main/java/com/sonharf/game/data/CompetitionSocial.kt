package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ClubDirectoryRowDto(
    @SerialName("club_id") val clubId: String,
    val name: String,
    val tag: String,
    val description: String = "",
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_members") val maxMembers: Int = 30,
    @SerialName("weekly_points") val weeklyPoints: Long = 0,
    @SerialName("owner_name") val ownerName: String = "",
)

@Serializable
data class MyClubDto(
    @SerialName("club_id") val clubId: String,
    val name: String,
    val tag: String,
    val description: String = "",
    val role: String = "member",
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_members") val maxMembers: Int = 30,
    @SerialName("weekly_points") val weeklyPoints: Long = 0,
    @SerialName("weekly_rank") val weeklyRank: Long = 0,
)

@Serializable
data class ClubMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val role: String = "member",
    val rating: Int = 1000,
    @SerialName("league_name") val leagueName: String = "BRONZ",
    @SerialName("weekly_points") val weeklyPoints: Long = 0,
    @SerialName("joined_at") val joinedAt: String,
)

@Serializable
data class ClubMessageDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class ClubMessageWrite(
    @SerialName("club_id") val clubId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
)

@Serializable
data class WeeklyTournamentDto(
    @SerialName("tournament_id") val tournamentId: String,
    val name: String,
    @SerialName("week_start") val weekStart: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val joined: Boolean = false,
    @SerialName("my_points") val myPoints: Long = 0,
    @SerialName("my_wins") val myWins: Long = 0,
    @SerialName("my_losses") val myLosses: Long = 0,
    @SerialName("my_rank") val myRank: Long = 0,
    @SerialName("player_count") val playerCount: Long = 0,
)

@Serializable
data class WeeklyTournamentLeaderboardRowDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val rating: Int = 1000,
    @SerialName("league_name") val leagueName: String = "BRONZ",
    val points: Long = 0,
    val wins: Long = 0,
    val losses: Long = 0,
    val rank: Long = 0,
)

@Serializable
data class ClubWeeklyMissionDto(
    val tier: Int,
    @SerialName("target_points") val targetPoints: Int,
    @SerialName("reward_coin") val rewardCoin: Int,
    @SerialName("min_contribution") val minContribution: Int,
    @SerialName("club_points") val clubPoints: Long = 0,
    @SerialName("my_points") val myPoints: Long = 0,
    val claimed: Boolean = false,
    val eligible: Boolean = false,
    @SerialName("week_start") val weekStart: String,
    @SerialName("week_end") val weekEnd: String,
)

@Serializable
data class ClubWeeklyMissionClaimDto(
    val success: Boolean = false,
    val tier: Int = 0,
    @SerialName("reward_coin") val rewardCoin: Int = 0,
    val balance: Int = 0,
)

@Serializable
data class WeeklyTournamentHistoryDto(
    @SerialName("tournament_id") val tournamentId: String,
    val name: String,
    @SerialName("week_start") val weekStart: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val points: Long = 0,
    val wins: Long = 0,
    val losses: Long = 0,
    val matches: Long = 0,
    @SerialName("final_rank") val finalRank: Long = 0,
    @SerialName("participant_count") val participantCount: Long = 0,
    @SerialName("reward_coins") val rewardCoins: Int = 0,
    @SerialName("reward_claimed") val rewardClaimed: Boolean = false,
    @SerialName("reward_eligible") val rewardEligible: Boolean = false,
)

@Serializable
data class TournamentRewardClaimDto(
    val success: Boolean = false,
    val rank: Int = 0,
    @SerialName("reward_coins") val rewardCoins: Int = 0,
    val balance: Int = 0,
)

suspend fun OnlineGameBackend.getClubDirectory(limit: Int = 30): List<ClubDirectoryRowDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_club_directory_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) },
    ).decodeList()

suspend fun OnlineGameBackend.getMyClub(): MyClubDto? =
    SupabaseProvider.client.postgrest.rpc("get_my_club_v1").decodeList<MyClubDto>().firstOrNull()

suspend fun OnlineGameBackend.getClubMembers(clubId: String): List<ClubMemberDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_club_members_v1",
        buildJsonObject { put("p_club_id", clubId) },
    ).decodeList()

suspend fun OnlineGameBackend.createClub(name: String, tag: String, description: String): String =
    SupabaseProvider.client.postgrest.rpc(
        "create_club_v1",
        buildJsonObject {
            put("p_name", name.trim())
            put("p_tag", tag.trim())
            put("p_description", description.trim())
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.joinClub(clubId: String): String =
    SupabaseProvider.client.postgrest.rpc(
        "join_club_v1",
        buildJsonObject { put("p_club_id", clubId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.leaveClub() {
    SupabaseProvider.client.postgrest.rpc("leave_club_v1")
}

suspend fun OnlineGameBackend.transferClubOwner(userId: String) {
    SupabaseProvider.client.postgrest.rpc(
        "transfer_club_owner_v1",
        buildJsonObject { put("p_user_id", userId) },
    )
}

suspend fun OnlineGameBackend.getClubMessages(clubId: String): List<ClubMessageDto> =
    SupabaseProvider.client.from("club_messages")
        .select { filter { eq("club_id", clubId) } }
        .decodeList<ClubMessageDto>()
        .sortedBy { it.createdAt }
        .takeLast(100)

suspend fun OnlineGameBackend.sendClubMessage(clubId: String, text: String) {
    val me = currentUserId() ?: error("not_authenticated")
    val body = text.trim()
    require(body.isNotEmpty() && body.length <= 300)
    SupabaseProvider.client.from("club_messages")
        .insert(ClubMessageWrite(clubId, me, body))
}

suspend fun OnlineGameBackend.getWeeklyTournament(): WeeklyTournamentDto =
    SupabaseProvider.client.postgrest.rpc("get_weekly_tournament_v1").decodeSingle()

suspend fun OnlineGameBackend.joinWeeklyTournament(): String =
    SupabaseProvider.client.postgrest.rpc("join_weekly_tournament_v1").decodeSingle()

suspend fun OnlineGameBackend.getWeeklyTournamentLeaderboard(limit: Int = 50): List<WeeklyTournamentLeaderboardRowDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_weekly_tournament_leaderboard_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) },
    ).decodeList()

suspend fun OnlineGameBackend.getClubWeeklyMissions(): List<ClubWeeklyMissionDto> =
    SupabaseProvider.client.postgrest.rpc("get_club_weekly_missions_v1").decodeList()

suspend fun OnlineGameBackend.claimClubWeeklyMission(tier: Int): ClubWeeklyMissionClaimDto =
    SupabaseProvider.client.postgrest.rpc(
        "claim_club_weekly_mission_v1",
        buildJsonObject { put("p_tier", tier.coerceIn(1, 3)) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getWeeklyTournamentHistory(limit: Int = 12): List<WeeklyTournamentHistoryDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_weekly_tournament_history_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 52)) },
    ).decodeList()

suspend fun OnlineGameBackend.claimPreviousWeeklyTournamentReward(): TournamentRewardClaimDto =
    SupabaseProvider.client.postgrest.rpc("claim_previous_weekly_tournament_reward_v1").decodeSingle()
