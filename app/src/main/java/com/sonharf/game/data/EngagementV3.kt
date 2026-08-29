package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class DailyCipherStatusDto(
    @SerialName("challenge_date") val challengeDate: String,
    val language: String,
    val attempts: Int = 0,
    @SerialName("max_attempts") val maxAttempts: Int = 6,
    val guesses: List<String> = emptyList(),
    val feedbacks: List<String> = emptyList(),
    val won: Boolean = false,
    val finished: Boolean = false,
    val answer: String? = null,
    @SerialName("reward_coins") val rewardCoins: Int = 0,
)

@Serializable
data class MasteryMilestoneDto(
    val id: String,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("description_tr") val descriptionTr: String,
    @SerialName("description_en") val descriptionEn: String,
    val progress: Int = 0,
    val target: Int = 1,
    @SerialName("reward_coins") val rewardCoins: Int = 0,
    val unlocked: Boolean = false,
    val claimed: Boolean = false,
)

@Serializable
data class MatchHistoryDto(
    @SerialName("match_id") val matchId: String,
    val mode: String,
    @SerialName("opponent_id") val opponentId: String,
    @SerialName("display_name") val displayName: String,
    val result: String,
    @SerialName("my_score") val myScore: Int = 0,
    @SerialName("their_score") val theirScore: Int = 0,
    @SerialName("rating_delta") val ratingDelta: Int = 0,
    val language: String = "tr",
    @SerialName("played_at") val playedAt: String,
    @SerialName("is_friend") val isFriend: Boolean = false,
    @SerialName("presence_status") val presenceStatus: String = "offline",
    @SerialName("can_challenge") val canChallenge: Boolean = false,
)

@Serializable
data class PersonalRecordsDto(
    @SerialName("real_pvp_matches") val realPvpMatches: Int = 0,
    @SerialName("real_pvp_wins") val realPvpWins: Int = 0,
    @SerialName("real_pvp_losses") val realPvpLosses: Int = 0,
    @SerialName("real_pvp_draws") val realPvpDraws: Int = 0,
    @SerialName("classic_matches") val classicMatches: Int = 0,
    @SerialName("arena_matches") val arenaMatches: Int = 0,
    @SerialName("current_rating") val currentRating: Int = 1000,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("valid_words") val validWords: Int = 0,
    @SerialName("longest_word") val longestWord: String = "",
    @SerialName("longest_word_length") val longestWordLength: Int = 0,
    @SerialName("best_classic_score") val bestClassicScore: Int = 0,
    @SerialName("best_arena_score") val bestArenaScore: Int = 0,
    @SerialName("biggest_win_margin") val biggestWinMargin: Int = 0,
    @SerialName("favorite_rival_name") val favoriteRivalName: String = "",
    @SerialName("favorite_rival_matches") val favoriteRivalMatches: Int = 0,
)

@Serializable
data class RivalHistoryDto(
    @SerialName("opponent_id") val opponentId: String,
    @SerialName("display_name") val displayName: String,
    val matches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    @SerialName("my_points") val myPoints: Int = 0,
    @SerialName("their_points") val theirPoints: Int = 0,
    @SerialName("classic_matches") val classicMatches: Int = 0,
    @SerialName("arena_matches") val arenaMatches: Int = 0,
    @SerialName("last_mode") val lastMode: String = "classic",
    @SerialName("last_played_at") val lastPlayedAt: String? = null,
    @SerialName("is_friend") val isFriend: Boolean = false,
    @SerialName("presence_status") val presenceStatus: String = "offline",
    @SerialName("can_challenge") val canChallenge: Boolean = false,
)

@Serializable
data class ArchRivalDto(
    @SerialName("opponent_id") val opponentId: String,
    @SerialName("display_name") val displayName: String,
    val matches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    @SerialName("my_points") val myPoints: Int = 0,
    @SerialName("their_points") val theirPoints: Int = 0,
    @SerialName("last_played_at") val lastPlayedAt: String? = null,
)

@Serializable
data class WeeklyPodRowDto(
    @SerialName("global_rank") val globalRank: Int,
    @SerialName("pod_rank") val podRank: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val rating: Int = 1000,
    @SerialName("is_me") val isMe: Boolean = false,
)

suspend fun OnlineGameBackend.getDailyCipherStatus(language: String): DailyCipherStatusDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_daily_cipher_status_v1",
        buildJsonObject { put("p_language", if (language.lowercase() == "en") "en" else "tr") },
    ).decodeSingle()

suspend fun OnlineGameBackend.submitDailyCipherGuess(language: String, guess: String): DailyCipherStatusDto =
    SupabaseProvider.client.postgrest.rpc(
        "submit_daily_cipher_guess_v1",
        buildJsonObject {
            put("p_language", if (language.lowercase() == "en") "en" else "tr")
            put("p_guess", guess.trim())
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getMasteryPath(): List<MasteryMilestoneDto> =
    SupabaseProvider.client.postgrest.rpc("get_mastery_path_v1").decodeList()

suspend fun OnlineGameBackend.claimMasteryReward(milestoneId: String): Int =
    SupabaseProvider.client.postgrest.rpc(
        "claim_mastery_reward_v1",
        buildJsonObject { put("p_milestone_id", milestoneId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getMatchHistory(limit: Int = 30): List<MatchHistoryDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_match_history_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) },
    ).decodeList()

suspend fun OnlineGameBackend.getPersonalRecords(): PersonalRecordsDto =
    SupabaseProvider.client.postgrest.rpc("get_personal_records_v1").decodeSingle()

suspend fun OnlineGameBackend.getRivalHistory(limit: Int = 20): List<RivalHistoryDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_rival_history_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 50)) },
    ).decodeList()

suspend fun OnlineGameBackend.getArchRival(): ArchRivalDto? =
    SupabaseProvider.client.postgrest.rpc("get_arch_rival_v1").decodeList<ArchRivalDto>().firstOrNull()

suspend fun OnlineGameBackend.getWeeklyPod(language: String): List<WeeklyPodRowDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_weekly_pod_v1",
        buildJsonObject { put("p_language", if (language.lowercase() == "en") "en" else "tr") },
    ).decodeList()
