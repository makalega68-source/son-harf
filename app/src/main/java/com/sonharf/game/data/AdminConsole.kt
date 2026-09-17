package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class AdminDashboardDto(
    @SerialName("total_users") val totalUsers: Long = 0,
    @SerialName("active_now") val activeNow: Long = 0,
    @SerialName("active_today") val activeToday: Long = 0,
    @SerialName("active_7d") val active7d: Long = 0,
    @SerialName("vip_users") val vipUsers: Long = 0,
    @SerialName("active_subscriptions") val activeSubscriptions: Long = 0,
    @SerialName("matches_total") val matchesTotal: Long = 0,
    @SerialName("matches_today") val matchesToday: Long = 0,
    @SerialName("active_rooms") val activeRooms: Long = 0,
    @SerialName("stale_rooms") val staleRooms: Long = 0,
    @SerialName("queue_waiting") val queueWaiting: Long = 0,
    @SerialName("verified_purchases") val verifiedPurchases: Long = 0,
    @SerialName("gross_revenue_minor") val grossRevenueMinor: Long = 0,
    @SerialName("revenue_currency") val revenueCurrency: String = "TRY",
    @SerialName("unpriced_purchases") val unpricedPurchases: Long = 0,
    @SerialName("son_harf_opens") val sonHarfOpens: Long = 0,
    @SerialName("bil_bakalim_opens") val bilBakalimOpens: Long = 0,
    @SerialName("my_is_vip") val myIsVip: Boolean = false,
    @SerialName("free_test_purchases") val freeTestPurchases: Boolean = false,
)

@Serializable
data class AdminTopProductDto(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("purchase_count") val purchaseCount: Long = 0,
    @SerialName("revenue_minor") val revenueMinor: Long = 0,
    val currency: String = "TRY",
    @SerialName("price_configured") val priceConfigured: Boolean = false,
)

@Serializable
data class AdminTopStoreItemDto(
    @SerialName("item_id") val itemId: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("acquisition_count") val acquisitionCount: Long = 0,
)

@Serializable
data class AdminMonthlyRevenueDto(
    val month: String,
    @SerialName("revenue_minor") val revenueMinor: Long = 0,
    val currency: String = "TRY",
)

@Serializable
data class AdminAnnouncementDto(
    @SerialName("message_tr") val messageTr: String = "",
    @SerialName("message_en") val messageEn: String = "",
    val enabled: Boolean = false,
    val maintenance: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AdminHealthDto(
    @SerialName("metric_key") val metricKey: String,
    val title: String,
    val status: String,
    @SerialName("metric_value") val metricValue: Long = 0,
    val detail: String,
)

@Serializable
data class AdminOwnerAccountDto(
    @SerialName("user_id") val userId: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("lifetime_vip") val lifetimeVip: Boolean = false,
    @SerialName("unlimited_diamonds") val unlimitedDiamonds: Boolean = false,
    @SerialName("unlimited_son_coin") val unlimitedSonCoin: Boolean = false,
    val active: Boolean = false,
    @SerialName("current_diamonds") val currentDiamonds: Int = 0,
    val rating: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AdminCapacityDto(
    @SerialName("metric_key") val metricKey: String,
    val title: String,
    val status: String,
    @SerialName("used_value") val usedValue: Long = 0,
    @SerialName("limit_value") val limitValue: Long = 0,
    @SerialName("percent_used") val percentUsed: Int = 0,
    val unit: String = "",
    val detail: String = "",
    @SerialName("resolve_url") val resolveUrl: String = "",
)

@Serializable
data class AdminPlayerSearchDto(
    @SerialName("user_id") val userId: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_vip") val isVip: Boolean,
    val diamonds: Int = 0,
    val rating: Int = 0,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("is_owner_account") val isOwnerAccount: Boolean = false,
)

@Serializable
data class AdminSeasonalThemeDto(
    val id: Long,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    val enabled: Boolean = false,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val multiplier: Int = 2,
    @SerialName("word_count") val wordCount: Long = 0,
)

suspend fun OnlineGameBackend.adminSearchPlayers(query: String): List<AdminPlayerSearchDto> =
    SupabaseProvider.client.postgrest.rpc(
        "admin_search_players_v1",
        buildJsonObject { put("p_query", query.trim()) },
    ).decodeList()

suspend fun OnlineGameBackend.adminSetPlayerVip(userId: String, enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_player_vip_v1",
        buildJsonObject {
            put("p_user_id", userId)
            put("p_enabled", enabled)
        },
    )
}

@Serializable
data class AdminGameControlDto(
    @SerialName("config_key") val configKey: String,
    val title: String,
    val detail: String,
    val enabled: Boolean,
)

suspend fun OnlineGameBackend.getAdminGameControls(): List<AdminGameControlDto> =
    SupabaseProvider.client.postgrest.rpc("admin_game_controls_v1").decodeList()

suspend fun OnlineGameBackend.adminSetGameControl(key: String, enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_game_control_v1",
        buildJsonObject {
            put("p_key", key)
            put("p_enabled", enabled)
        },
    )
}

suspend fun OnlineGameBackend.getAdminSeasonalThemes(): List<AdminSeasonalThemeDto> =
    SupabaseProvider.client.postgrest.rpc("admin_ui_list_seasonal_themes_v1").decodeList()

suspend fun OnlineGameBackend.adminSetSeasonalThemeActive(themeId: Long, enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_ui_set_seasonal_theme_active_v1",
        buildJsonObject {
            put("p_theme_id", themeId)
            put("p_enabled", enabled)
        },
    )
}

suspend fun OnlineGameBackend.adminAddSeasonalWord(themeId: Long, language: String, word: String) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_ui_add_seasonal_word_v1",
        buildJsonObject {
            put("p_theme_id", themeId)
            put("p_language", language.lowercase().take(2))
            put("p_word", word.trim().take(64))
        },
    )
}

suspend fun OnlineGameBackend.getAdminOwnerAccounts(): List<AdminOwnerAccountDto> =
    SupabaseProvider.client.postgrest.rpc("admin_owner_accounts_v1").decodeList()

suspend fun OnlineGameBackend.adminSetOwnerAccount(
    email: String,
    lifetimeVip: Boolean,
    unlimitedDiamonds: Boolean,
    unlimitedSonCoin: Boolean,
    active: Boolean,
) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_owner_account_v1",
        buildJsonObject {
            put("p_email", email.trim())
            put("p_lifetime_vip", lifetimeVip)
            put("p_unlimited_diamonds", unlimitedDiamonds)
            put("p_unlimited_son_coin", unlimitedSonCoin)
            put("p_active", active)
        },
    )
}

suspend fun OnlineGameBackend.getAdminCapacity(): List<AdminCapacityDto> =
    SupabaseProvider.client.postgrest.rpc("admin_capacity_v1").decodeList()

suspend fun OnlineGameBackend.getAdminDashboard(): AdminDashboardDto =
    SupabaseProvider.client.postgrest.rpc("admin_dashboard_v1").decodeSingle()

suspend fun OnlineGameBackend.getAdminTopProducts(): List<AdminTopProductDto> =
    SupabaseProvider.client.postgrest.rpc("admin_top_products_v1").decodeList()

suspend fun OnlineGameBackend.getAdminTopStoreItems(): List<AdminTopStoreItemDto> =
    SupabaseProvider.client.postgrest.rpc("admin_top_store_items_v1").decodeList()

suspend fun OnlineGameBackend.getAdminHealth(): List<AdminHealthDto> =
    SupabaseProvider.client.postgrest.rpc("admin_health_v1").decodeList()

suspend fun OnlineGameBackend.adminSetMyVip(enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_my_vip_v1",
        buildJsonObject { put("p_enabled", enabled) },
    )
}

suspend fun OnlineGameBackend.adminSetFreePurchases(enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_free_purchases_v1",
        buildJsonObject { put("p_enabled", enabled) },
    )
}

suspend fun OnlineGameBackend.adminSetProductPrice(productId: String, priceMinor: Long, currency: String = "TRY") {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_product_price_v1",
        buildJsonObject {
            put("p_product_id", productId)
            put("p_gross_price_minor", priceMinor)
            put("p_currency", currency.uppercase())
        },
    )
}

suspend fun OnlineGameBackend.adminRepair(action: String) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_repair_v1",
        buildJsonObject { put("p_action", action) },
    )
}

suspend fun OnlineGameBackend.adminGrantTestProduct(productId: String) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_grant_test_product_v1",
        buildJsonObject { put("p_product_id", productId) },
    )
}

suspend fun OnlineGameBackend.getAdminMonthlyRevenue(): List<AdminMonthlyRevenueDto> =
    SupabaseProvider.client.postgrest.rpc("admin_monthly_revenue_v1").decodeList()

suspend fun OnlineGameBackend.getAdminAnnouncement(): AdminAnnouncementDto =
    SupabaseProvider.client.postgrest.rpc("admin_get_announcement_v2").decodeSingle()

suspend fun OnlineGameBackend.adminSetAnnouncement(
    messageTr: String,
    messageEn: String,
    enabled: Boolean,
    maintenance: Boolean,
) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_announcement_v2",
        buildJsonObject {
            put("p_message_tr", messageTr.take(500))
            put("p_message_en", messageEn.take(500))
            put("p_enabled", enabled)
            put("p_maintenance", maintenance)
        },
    )
}


@Serializable
data class AdminAccessDto(
    val authorized: Boolean = false,
    @SerialName("admin_role") val adminRole: String = "",
    @SerialName("lifetime_vip") val lifetimeVip: Boolean = false,
    @SerialName("unlimited_diamonds") val unlimitedDiamonds: Boolean = false,
    @SerialName("unlimited_son_coin") val unlimitedSonCoin: Boolean = false,
)

@Serializable
data class AdminPlayerOpsDto(
    @SerialName("user_id") val userId: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_vip") val isVip: Boolean = false,
    val diamonds: Int = 0,
    val rating: Int = 0,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("blocked_until") val blockedUntil: String? = null,
    @SerialName("is_owner_account") val isOwnerAccount: Boolean = false,
)

@Serializable
data class AdminStoreCatalogDto(
    @SerialName("product_id") val productId: String,
    @SerialName("gross_price_minor") val grossPriceMinor: Long = 0,
    val currency: String = "TRY",
    val enabled: Boolean = true,
    @SerialName("badge_tr") val badgeTr: String? = null,
    @SerialName("badge_en") val badgeEn: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 100,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AdminAuditEntryDto(
    val id: Long,
    @SerialName("created_at") val createdAt: String,
    @SerialName("admin_email") val adminEmail: String = "",
    val action: String,
    @SerialName("target_type") val targetType: String = "",
    @SerialName("target_id") val targetId: String? = null,
    @SerialName("before_data") val beforeData: String? = null,
    @SerialName("after_data") val afterData: String? = null,
    val outcome: String = "success",
    @SerialName("error_text") val errorText: String? = null,
)

@Serializable
data class AdminSystemEventDto(
    val id: Long,
    val severity: String,
    val source: String,
    @SerialName("event_type") val eventType: String,
    val details: String = "{}",
    @SerialName("created_at") val createdAt: String,
)

suspend fun OnlineGameBackend.isCurrentUserAdmin(): Boolean =
    runCatching {
        SupabaseProvider.client.postgrest.rpc("admin_access_v1").decodeSingle<AdminAccessDto>().authorized
    }.getOrDefault(false)

suspend fun OnlineGameBackend.adminSearchPlayersV2(query: String): List<AdminPlayerOpsDto> =
    SupabaseProvider.client.postgrest.rpc(
        "admin_search_players_v2",
        buildJsonObject { put("p_query", query.trim()) },
    ).decodeList()

suspend fun OnlineGameBackend.adminAdjustPlayerDiamonds(userId: String, delta: Int) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_adjust_player_diamonds_v1",
        buildJsonObject { put("p_user_id", userId); put("p_delta", delta) },
    )
}

suspend fun OnlineGameBackend.adminSetPlayerBlocked(userId: String, blocked: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_player_blocked_v1",
        buildJsonObject { put("p_user_id", userId); put("p_blocked", blocked) },
    )
}

suspend fun OnlineGameBackend.getAdminStoreCatalog(): List<AdminStoreCatalogDto> =
    SupabaseProvider.client.postgrest.rpc("admin_store_catalog_v1").decodeList()

suspend fun OnlineGameBackend.adminSetStoreEnabled(productId: String, enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_store_enabled_v1",
        buildJsonObject { put("p_product_id", productId); put("p_enabled", enabled) },
    )
}

suspend fun OnlineGameBackend.getAdminAuditV2(): List<AdminAuditEntryDto> =
    SupabaseProvider.client.postgrest.rpc("admin_audit_v2").decodeList()

suspend fun OnlineGameBackend.getAdminRecentErrors(): List<AdminSystemEventDto> =
    SupabaseProvider.client.postgrest.rpc("admin_recent_errors_v1").decodeList()


@Serializable
data class AdminShopItemDto(
    @SerialName("item_id") val itemId: String,
    val kind: String,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("diamond_price") val diamondPrice: Int = 0,
    @SerialName("vip_only") val vipOnly: Boolean = false,
    val active: Boolean = true,
    val rarity: String = "STANDARD",
    @SerialName("updated_hint") val updatedHint: String = "",
)

@Serializable
data class AdminTestInventoryDto(
    @SerialName("item_id") val itemId: String,
    val kind: String,
    @SerialName("name_tr") val nameTr: String,
    val quantity: Int = 1,
    @SerialName("is_equipped") val isEquipped: Boolean = false,
    @SerialName("acquired_at") val acquiredAt: String? = null,
)

suspend fun OnlineGameBackend.getAdminShopItems(): List<AdminShopItemDto> =
    SupabaseProvider.client.postgrest.rpc("admin_shop_items_v1").decodeList()

suspend fun OnlineGameBackend.adminSetShopItem(itemId: String, active: Boolean, diamondPrice: Int) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_shop_item_v1",
        buildJsonObject {
            put("p_item_id", itemId)
            put("p_active", active)
            put("p_diamond_price", diamondPrice)
        },
    )
}

suspend fun OnlineGameBackend.getAdminTestInventory(userId: String): List<AdminTestInventoryDto> =
    SupabaseProvider.client.postgrest.rpc(
        "admin_test_inventory_v1",
        buildJsonObject { put("p_user_id", userId) },
    ).decodeList()

suspend fun OnlineGameBackend.adminGrantTestItem(userId: String, itemId: String) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_grant_test_item_v1",
        buildJsonObject { put("p_user_id", userId); put("p_item_id", itemId) },
    )
}
