package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
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
    val rarity: String = "STANDARD",
    @SerialName("preview_asset_key") val previewAssetKey: String? = null,
    @SerialName("collection_key") val collectionKey: String? = null,
    @SerialName("trial_mode") val trialMode: String? = null,
    @SerialName("trial_value") val trialValue: Int? = null,
)

@Serializable
data class StoreCatalogConfigDto(
    @SerialName("product_id") val productId: String,
    val enabled: Boolean = true,
    @SerialName("badge_tr") val badgeTr: String? = null,
    @SerialName("badge_en") val badgeEn: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 100,
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
    @SerialName("avatar_background_id") val avatarBackgroundId: String? = null,
    @SerialName("nameplate_id") val nameplateId: String? = null,
    @SerialName("badge_id") val badgeId: String? = null,
    @SerialName("title_style_id") val titleStyleId: String? = null,
    @SerialName("vs_intro_id") val vsIntroId: String? = null,
    @SerialName("word_effect_id") val wordEffectId: String? = null,
    @SerialName("emote_id") val emoteId: String? = null,
)

suspend fun OnlineGameBackend.getShopItems(): List<ShopItemDto> =
    SupabaseProvider.client.from("shop_items").select().decodeList<ShopItemDto>()
        .filter { item ->
            item.active && (item.kind != "profile_frame" || item.id in supportedProfileFrameIds)
        }
        .sortedBy { it.sortOrder }

suspend fun OnlineGameBackend.getStoreCatalogConfig(): List<StoreCatalogConfigDto> =
    SupabaseProvider.client.from("store_catalog_config").select().decodeList<StoreCatalogConfigDto>()
        .filter { it.enabled }
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

suspend fun OnlineGameBackend.trackStoreEvent(
    eventName: String,
    productId: String? = null,
    metadata: JsonObject = buildJsonObject {},
) {
    SupabaseProvider.client.postgrest.rpc(
        "track_store_event_v1",
        buildJsonObject {
            put("p_event_name", eventName)
            if (productId != null) put("p_product_id", productId)
            put("p_metadata", metadata)
        },
    )
}
