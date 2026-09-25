package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class MascotProgressDto(
    @SerialName("mascot_id") val mascotId: String = "",
    @SerialName("pet_name") val petName: String? = null,
    @SerialName("total_xp") val totalXp: Int = 0,
    val level: Int = 1,
    val happiness: Int = 50,
    val fullness: Int = 50,
    val energy: Int = 50,
    @SerialName("normal_fruit_used_today") val normalFruitUsedToday: Int = 0,
    @SerialName("normal_fruit_daily_limit") val normalFruitDailyLimit: Int = 3,
)

@Serializable
data class MascotRoomStateDto(
    @SerialName("friendship_xp") val friendshipXp: Int = 0,
    @SerialName("friendship_level") val friendshipLevel: Int = 1,
    @SerialName("loved_today") val lovedToday: Boolean = false,
    @SerialName("played_today") val playedToday: Boolean = false,
    @SerialName("groomed_today") val groomedToday: Boolean = false,
    @SerialName("daily_bond_completed") val dailyBondCompleted: Boolean = false,
)

@Serializable
data class MascotCareResultDto(
    val happiness: Int = 0,
    val fullness: Int = 0,
    val energy: Int = 0,
    @SerialName("friendship_xp") val friendshipXp: Int = 0,
    @SerialName("friendship_level") val friendshipLevel: Int = 1,
    @SerialName("friendship_gained") val friendshipGained: Int = 0,
    @SerialName("daily_bond_completed") val dailyBondCompleted: Boolean = false,
    @SerialName("daily_bonus_awarded") val dailyBonusAwarded: Boolean = false,
)

@Serializable
data class MascotFeedResultDto(
    @SerialName("xp_gained") val xpGained: Int = 0,
    val level: Int = 1,
    val fullness: Int = 0,
    val happiness: Int = 0,
    val energy: Int = 0,
    @SerialName("inventory_left") val inventoryLeft: Int = 0,
    @SerialName("normal_fruit_used_today") val normalFruitUsedToday: Int = 0,
)

@Serializable
data class MascotFruitBuyDto(
    @SerialName("inventory_quantity") val inventoryQuantity: Int = 0,
    @SerialName("son_coin_balance") val balance: Int = 0,
)

@Serializable
data class HintBuyDto(
    val success: Boolean = false,
    @SerialName("son_coin_spent") val spent: Int = 0,
    @SerialName("son_coin_balance") val balance: Int = 0,
)

/** Mascot room: care (love / play / groom), fruit feeding and the friendship level. */
object MascotRoomBackend {
    const val HINT_PRICE = 25

    suspend fun progress(mascotId: String): MascotProgressDto? =
        SupabaseProvider.client.postgrest.rpc("get_mascot_progress_v1", buildJsonObject { put("p_mascot_id", mascotId) })
            .decodeList<MascotProgressDto>().firstOrNull()

    suspend fun room(mascotId: String): MascotRoomStateDto? =
        SupabaseProvider.client.postgrest.rpc("get_mascot_room_state_v2", buildJsonObject { put("p_mascot_id", mascotId) })
            .decodeList<MascotRoomStateDto>().firstOrNull()

    suspend fun care(mascotId: String, action: String): MascotCareResultDto? =
        SupabaseProvider.client.postgrest.rpc(
            "care_mascot_v2",
            buildJsonObject { put("p_mascot_id", mascotId); put("p_action", action) },
        ).decodeList<MascotCareResultDto>().firstOrNull()

    suspend fun feed(mascotId: String, fruitId: String): MascotFeedResultDto? =
        SupabaseProvider.client.postgrest.rpc(
            "feed_mascot_v1",
            buildJsonObject { put("p_mascot_id", mascotId); put("p_fruit_id", fruitId) },
        ).decodeList<MascotFeedResultDto>().firstOrNull()

    suspend fun buyFruit(fruitId: String): MascotFruitBuyDto? =
        SupabaseProvider.client.postgrest.rpc(
            "buy_mascot_fruit_v1",
            buildJsonObject { put("p_fruit_id", fruitId); put("p_quantity", 1) },
        ).decodeList<MascotFruitBuyDto>().firstOrNull()

    /** Spends [HINT_PRICE] Son Coin on one extra hint; null when it could not be bought. */
    suspend fun buyHint(game: String): HintBuyDto? = runCatching {
        SupabaseProvider.client.postgrest.rpc("buy_game_hint_v1", buildJsonObject { put("p_game", game) })
            .decodeList<HintBuyDto>().firstOrNull()?.takeIf { it.success }
    }.getOrNull()
}
