package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class GoalRowDto(
    val id: String,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("description_tr") val descriptionTr: String,
    @SerialName("description_en") val descriptionEn: String,
    val target: Int,
    @SerialName("reward_diamonds") val rewardDiamonds: Int,
    val progress: Int,
    val claimed: Boolean,
)

@Serializable
data class AppNewsDto(
    val id: Long,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("body_tr") val bodyTr: String,
    @SerialName("body_en") val bodyEn: String,
    @SerialName("published_at") val publishedAt: String,
    val active: Boolean = true,
)

suspend fun OnlineGameBackend.setPreferredGameMode(mode: String): String =
    SupabaseProvider.client.postgrest.rpc(
        "set_game_mode_v1",
        buildJsonObject { put("p_mode", if (mode == "expert") "expert" else "normal") },
    ).decodeSingle()

suspend fun OnlineGameBackend.getPreferredGameMode(): String =
    SupabaseProvider.client.postgrest.rpc("get_game_mode_v1").decodeSingle()

suspend fun OnlineGameBackend.getGoals(): List<GoalRowDto> =
    SupabaseProvider.client.postgrest.rpc("get_goals_v1").decodeList()

suspend fun OnlineGameBackend.claimGoal(goalId: String): Int =
    SupabaseProvider.client.postgrest.rpc(
        "claim_goal_v1",
        buildJsonObject { put("p_goal_id", goalId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getAppNews(): List<AppNewsDto> =
    SupabaseProvider.client.from("app_news").select().decodeList<AppNewsDto>()
        .filter { it.active }
        .sortedByDescending { it.publishedAt }

suspend fun OnlineGameBackend.getMyGameHistory(): List<GameRoomDto> =
    SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
        .sortedByDescending { it.id }
