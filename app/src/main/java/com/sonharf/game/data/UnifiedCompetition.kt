package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class UnifiedMissionDto(
    @SerialName("mission_id") val missionId: String,
    val scope: String,
    @SerialName("period_start") val periodStart: String,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("mode_key") val modeKey: String,
    val target: Int,
    val progress: Int,
    @SerialName("reward_coins") val rewardCoins: Int,
    val completed: Boolean,
    val claimed: Boolean,
    @SerialName("route_order") val routeOrder: Int,
)

@Serializable
data class UnifiedMissionClaimDto(
    val success: Boolean = false,
    @SerialName("mission_id") val missionId: String = "",
    @SerialName("reward_coins") val rewardCoins: Int = 0,
    val balance: Int = 0,
)

suspend fun OnlineGameBackend.getUnifiedMissions(): List<UnifiedMissionDto> =
    SupabaseProvider.client.postgrest
        .rpc("get_unified_missions_v1")
        .decodeList()

suspend fun OnlineGameBackend.claimUnifiedMission(missionId: String): UnifiedMissionClaimDto =
    SupabaseProvider.client.postgrest
        .rpc(
            "claim_unified_mission_v1",
            buildJsonObject { put("p_mission_id", missionId) },
        )
        .decodeSingle()

suspend fun OnlineGameBackend.logUnifiedEvent(name: String, value: String? = null) {
    SupabaseProvider.client.postgrest.rpc(
        "log_app_event_v1",
        buildJsonObject {
            put("p_event_name", name)
            if (!value.isNullOrBlank()) put("p_event_value", value)
        },
    )
}
