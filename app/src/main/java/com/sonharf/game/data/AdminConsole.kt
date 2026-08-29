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
    val message: String = "",
    val enabled: Boolean = false,
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
    SupabaseProvider.client.postgrest.rpc("admin_get_announcement_v1").decodeSingle()

suspend fun OnlineGameBackend.adminSetAnnouncement(message: String, enabled: Boolean) {
    SupabaseProvider.client.postgrest.rpc(
        "admin_set_announcement_v1",
        buildJsonObject { put("p_message", message.take(500)); put("p_enabled", enabled) },
    )
}
