package com.sonharf.game.billing

/**
 * Google Play product ids used by Son Harf.
 *
 * Product ids are identifiers only. Never hard-code real-money prices in the client;
 * all localized prices must come from Google Play ProductDetails.
 */
object ProductCatalog {
    const val VIP_MONTHLY = "vip_monthly"
    const val VIP_YEARLY = "vip_yearly"
    const val SEASON_PASS = "season_pass"

    // Legacy id kept for restore/verification of already-created Play products.
    const val SEASON_PASS_MONTHLY = "season_pass_monthly"

    const val COINS_500 = "coins_500"
    const val COINS_1500 = "coins_1500"
    const val COINS_3500 = "coins_3500"
    const val COINS_8000 = "coins_8000"

    const val STARTER_STYLE_PACK = "starter_style_pack"
    const val PREMIUM_STYLE_PACK = "premium_style_pack"
    const val SEASON_PACK = "season_pack"
    const val VIP_WELCOME_PACK = "vip_welcome_pack"

    /** Historical visual product; recognized only for restore compatibility. */
    @Deprecated("Legacy visual product; no longer offered in the active shop")
    const val THEME_NEON = "theme_neon"

    val activeSubscriptions = listOf(
        VIP_MONTHLY,
        VIP_YEARLY,
    )

    val subscriptions = listOf(
        VIP_MONTHLY,
        VIP_YEARLY,
        SEASON_PASS,
        SEASON_PASS_MONTHLY,
    )

    val consumableProducts = setOf(
        COINS_500,
        COINS_1500,
        COINS_3500,
        COINS_8000,
    )

    // Active offers only. Legacy restore ids intentionally stay outside this list.
    val oneTimeProducts = listOf(
        COINS_500,
        COINS_1500,
        COINS_3500,
        COINS_8000,
        STARTER_STYLE_PACK,
        PREMIUM_STYLE_PACK,
        SEASON_PACK,
        VIP_WELCOME_PACK,
    )

    val activeOneTimeProducts = oneTimeProducts

    val restorableProductIds = (subscriptions + oneTimeProducts + listOf(THEME_NEON)).toSet()

    fun isVip(productId: String): Boolean =
        productId == VIP_MONTHLY || productId == VIP_YEARLY

    fun isSeasonPass(productId: String): Boolean =
        productId == SEASON_PASS || productId == SEASON_PASS_MONTHLY

    fun isKnown(productId: String): Boolean = productId in restorableProductIds
}
