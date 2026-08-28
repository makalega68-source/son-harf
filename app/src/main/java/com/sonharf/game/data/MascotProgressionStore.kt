package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class MascotProgressDto(
    @SerialName("mascot_id") val mascotId: String,
    @SerialName("pet_name") val petName: String,
    @SerialName("total_xp") val totalXp: Int,
    val level: Int,
    val happiness: Int,
    val fullness: Int,
    val energy: Int,
    @SerialName("memory_fragments") val memoryFragments: Int,
    @SerialName("normal_fruit_used_today") val normalFruitUsedToday: Int = 0,
    @SerialName("normal_fruit_daily_limit") val normalFruitDailyLimit: Int = 3,
)

@Serializable
data class MascotFruitDto(
    val id: String,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("description_tr") val descriptionTr: String,
    @SerialName("description_en") val descriptionEn: String,
    @SerialName("xp_reward") val xpReward: Int,
    @SerialName("son_coin_price") val sonCoinPrice: Int,
    @SerialName("is_magic") val isMagic: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    val active: Boolean = true,
)

@Serializable
data class MascotFruitInventoryDto(
    @SerialName("fruit_id") val fruitId: String,
    val quantity: Int,
)

@Serializable
data class MascotFeedResultDto(
    val success: Boolean = true,
    @SerialName("mascot_id") val mascotId: String,
    @SerialName("fruit_id") val fruitId: String,
    @SerialName("xp_gained") val xpGained: Int,
    @SerialName("total_xp") val totalXp: Int,
    val level: Int,
    @SerialName("memory_fragments") val memoryFragments: Int,
    val fullness: Int,
    val happiness: Int,
    val energy: Int,
    @SerialName("inventory_left") val inventoryLeft: Int = 0,
    @SerialName("normal_fruit_used_today") val normalFruitUsedToday: Int = 0,
)

@Serializable
data class MascotCareResultDto(
    val success: Boolean = true,
    @SerialName("mascot_id") val mascotId: String,
    val action: String,
    val happiness: Int,
    val fullness: Int,
    val energy: Int,
)

@Serializable
data class MascotFruitPurchaseDto(
    val success: Boolean = true,
    @SerialName("fruit_id") val fruitId: String,
    val quantity: Int,
    @SerialName("inventory_quantity") val inventoryQuantity: Int,
    @SerialName("son_coin_spent") val sonCoinSpent: Int,
    @SerialName("son_coin_balance") val sonCoinBalance: Int,
)

suspend fun OnlineGameBackend.getMascotProgress(mascotId: String): MascotProgressDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_mascot_progress_v1",
        buildJsonObject { put("p_mascot_id", mascotId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.renameMascot(mascotId: String, name: String): MascotProgressDto =
    SupabaseProvider.client.postgrest.rpc(
        "rename_mascot_v1",
        buildJsonObject {
            put("p_mascot_id", mascotId)
            put("p_pet_name", name.trim().take(18))
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getMascotFruitCatalog(): List<MascotFruitDto> =
    SupabaseProvider.client.from("mascot_fruit_catalog")
        .select()
        .decodeList<MascotFruitDto>()
        .filter { it.active }
        .sortedBy { it.sortOrder }

suspend fun OnlineGameBackend.getMascotFruitInventory(): List<MascotFruitInventoryDto> {
    val me = currentUserId() ?: return emptyList()
    return SupabaseProvider.client.from("user_mascot_fruit_inventory")
        .select { filter { eq("user_id", me) } }
        .decodeList<MascotFruitInventoryDto>()
}

suspend fun OnlineGameBackend.buyMascotFruit(fruitId: String, quantity: Int = 1): MascotFruitPurchaseDto =
    SupabaseProvider.client.postgrest.rpc(
        "buy_mascot_fruit_v1",
        buildJsonObject {
            put("p_fruit_id", fruitId)
            put("p_quantity", quantity.coerceIn(1, 20))
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.feedMascot(mascotId: String, fruitId: String): MascotFeedResultDto =
    SupabaseProvider.client.postgrest.rpc(
        "feed_mascot_v1",
        buildJsonObject {
            put("p_mascot_id", mascotId)
            put("p_fruit_id", fruitId)
        },
    ).decodeSingle()


suspend fun OnlineGameBackend.careForMascot(mascotId: String, action: String): MascotCareResultDto =
    SupabaseProvider.client.postgrest.rpc(
        "care_mascot_v1",
        buildJsonObject {
            put("p_mascot_id", mascotId)
            put("p_action", action)
        },
    ).decodeSingle()
