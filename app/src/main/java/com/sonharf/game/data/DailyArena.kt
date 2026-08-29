package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class DailyArenaStartDto(
    @SerialName("run_id") val runId: String,
    val status: String,
    @SerialName("challenge_date") val challengeDate: String,
    val language: String,
    val letters: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val score: Int = 0,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("longest_word") val longestWord: String = "",
    @SerialName("best_combo") val bestCombo: Int = 0,
)

@Serializable
data class DailyArenaStatusDto(
    @SerialName("challenge_date") val challengeDate: String,
    val language: String,
    @SerialName("run_id") val runId: String? = null,
    val status: String = "not_started",
    val letters: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val score: Int = 0,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("longest_word") val longestWord: String = "",
    @SerialName("best_combo") val bestCombo: Int = 0,
    @SerialName("reward_coins") val rewardCoins: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("my_rank") val myRank: Long = 0,
    @SerialName("player_count") val playerCount: Long = 0,
)

@Serializable
data class DailyArenaWordDto(
    val word: String,
    @SerialName("normalized_word") val normalizedWord: String,
    @SerialName("base_points") val basePoints: Int = 0,
    val combo: Int = 1,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class DailyArenaSubmitDto(
    val accepted: Boolean = false,
    val status: String = "playing",
    val word: String? = null,
    @SerialName("normalized_word") val normalizedWord: String? = null,
    @SerialName("base_points") val basePoints: Int = 0,
    val combo: Int = 1,
    val score: Int = 0,
    @SerialName("word_count") val wordCount: Int = 0,
)

@Serializable
data class DailyArenaLeaderboardDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val score: Int = 0,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("best_combo") val bestCombo: Int = 0,
    @SerialName("longest_word") val longestWord: String = "",
    val rank: Long = 0,
    @SerialName("is_me") val isMe: Boolean = false,
)

suspend fun OnlineGameBackend.getDailyArenaStatus(language: String): DailyArenaStatusDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_daily_arena_status_v1",
        buildJsonObject { put("p_language", if (language.lowercase() == "en") "en" else "tr") },
    ).decodeSingle()

suspend fun OnlineGameBackend.startDailyArena(language: String): DailyArenaStartDto =
    SupabaseProvider.client.postgrest.rpc(
        "start_daily_arena_v1",
        buildJsonObject { put("p_language", if (language.lowercase() == "en") "en" else "tr") },
    ).decodeSingle()

suspend fun OnlineGameBackend.submitDailyArenaWord(runId: String, word: String): DailyArenaSubmitDto =
    SupabaseProvider.client.postgrest.rpc(
        "submit_daily_arena_word_v1",
        buildJsonObject {
            put("p_run_id", runId)
            put("p_word", word.trim())
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getDailyArenaWords(runId: String): List<DailyArenaWordDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_daily_arena_words_v1",
        buildJsonObject { put("p_run_id", runId) },
    ).decodeList()

suspend fun OnlineGameBackend.getDailyArenaLeaderboard(language: String, limit: Int = 50): List<DailyArenaLeaderboardDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_daily_arena_leaderboard_v1",
        buildJsonObject {
            put("p_language", if (language.lowercase() == "en") "en" else "tr")
            put("p_limit", limit.coerceIn(1, 100))
        },
    ).decodeList()
