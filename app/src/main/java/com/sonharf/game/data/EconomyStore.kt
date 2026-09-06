package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val supportedProfileFrameIds = setOf(
    "frame_asset_red",
    "frame_asset_green",
    "frame_asset_mint",
    "frame_asset_purple",
    "frame_asset_gold",
    "frame_asset_gold_crown",
    "frame_asset_christmas",
    "frame_asset_halloween",
)

private val supportedGameThemeIds = setOf("theme_dark_arena")

@Serializable
data class ShopItemDto(
    val id: String,
    val kind: String,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("description_tr") val descriptionTr: String = "",
    @SerialName("description_en") val descriptionEn: String = "",
    @SerialName("diamond_price") val diamondPrice: Int,
    @SerialName("vip_only") val vipOnly: Boolean = false,
    val active: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class InventoryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("item_id") val itemId: String,
)

@Serializable
data class EquippedCosmeticsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_frame_id") val profileFrameId: String? = null,
    @SerialName("name_style_id") val nameStyleId: String? = null,
    @SerialName("game_theme_id") val gameThemeId: String? = null,
    @SerialName("keyboard_theme_id") val keyboardThemeId: String? = null,
    @SerialName("victory_effect_id") val victoryEffectId: String? = null,
    @SerialName("emoji_pack_id") val emojiPackId: String? = null,
    @SerialName("mascot_id") val mascotId: String? = null,
)

suspend fun OnlineGameBackend.getShopItems(): List<ShopItemDto> =
    SupabaseProvider.client.from("shop_items").select().decodeList<ShopItemDto>()
        .filter { item ->
            item.active && when (item.kind) {
                "profile_frame" -> item.id in supportedProfileFrameIds
                "game_theme" -> item.id in supportedGameThemeIds
                else -> true
            }
        }
        .sortedBy { it.sortOrder }

suspend fun OnlineGameBackend.getInventory(): Set<String> {
    val me = currentUserId() ?: return emptySet()
    return SupabaseProvider.client.from("user_inventory")
        .select { filter { eq("user_id", me) } }
        .decodeList<InventoryDto>().map { it.itemId }.toSet()
}

suspend fun OnlineGameBackend.getEquippedCosmetics(): EquippedCosmeticsDto? {
    val me = currentUserId() ?: return null
    return SupabaseProvider.client.from("user_equipped_cosmetics")
        .select { filter { eq("user_id", me) } }
        .decodeList<EquippedCosmeticsDto>().firstOrNull()
}

suspend fun OnlineGameBackend.purchaseShopItem(itemId: String) {
    SupabaseProvider.client.postgrest.rpc("purchase_shop_item", buildJsonObject { put("p_item_id", itemId) })
}

suspend fun OnlineGameBackend.equipShopItem(itemId: String) {
    SupabaseProvider.client.postgrest.rpc("equip_shop_item", buildJsonObject { put("p_item_id", itemId) })
}

suspend fun OnlineGameBackend.claimVipMonthlyDiamonds() {
    SupabaseProvider.client.postgrest.rpc("claim_vip_monthly_diamonds")
}
