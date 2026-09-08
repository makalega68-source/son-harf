package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class PremierBoosterStatusDto(
    @SerialName("hint_count") val hintCount: Int = 0,
    @SerialName("swap_count") val swapCount: Int = 0,
    @SerialName("multiplier_count") val multiplierCount: Int = 0,
    @SerialName("required_override") val requiredOverride: String? = null,
    @SerialName("multiplier_armed") val multiplierArmed: Boolean = false,
    @SerialName("my_turn") val myTurn: Boolean = false,
    @SerialName("turn_deadline") val turnDeadline: String? = null,
)

@Serializable
data class PremierHintResultDto(
    val success: Boolean = false,
    val whisper: String = "",
    @SerialName("word_length") val wordLength: Int = 0,
    val remaining: Int = 0,
)

@Serializable
data class PremierSwapResultDto(
    val success: Boolean = false,
    @SerialName("required_override") val requiredOverride: String = "",
    val remaining: Int = 0,
)

@Serializable
data class PremierMultiplierResultDto(
    val success: Boolean = false,
    @SerialName("multiplier_armed") val multiplierArmed: Boolean = false,
    val remaining: Int = 0,
)

suspend fun OnlineGameBackend.getPremierBoosterStatus(roomId: String): PremierBoosterStatusDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_premier_booster_status_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.usePremierHint(roomId: String): PremierHintResultDto =
    SupabaseProvider.client.postgrest.rpc(
        "use_premier_hint_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.usePremierSwap(roomId: String): PremierSwapResultDto =
    SupabaseProvider.client.postgrest.rpc(
        "use_premier_swap_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.usePremierMultiplier(roomId: String): PremierMultiplierResultDto =
    SupabaseProvider.client.postgrest.rpc(
        "use_premier_multiplier_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()
