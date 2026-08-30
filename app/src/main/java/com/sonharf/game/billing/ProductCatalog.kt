package com.sonharf.game.billing

object ProductCatalog {
    const val VIP_MONTHLY = "vip_monthly"
    const val VIP_YEARLY = "vip_yearly"
    const val SEASON_PASS_MONTHLY = "season_pass_monthly"

    const val COINS_500 = "coins_500"
    const val COINS_1500 = "coins_1500"
    const val COINS_3500 = "coins_3500"
    const val COINS_8000 = "coins_8000"

    const val STARTER_STYLE_PACK = "starter_style_pack"
    /** Kept only so historical purchases can still be recognized by the verifier. */
    @Deprecated("Legacy visual product; no longer offered in the active shop")
    const val THEME_NEON = "theme_neon"

    val subscriptions = listOf(
        VIP_MONTHLY,
        VIP_YEARLY,
        SEASON_PASS_MONTHLY,
    )

    val consumableProducts = setOf(
        COINS_500,
        COINS_1500,
        COINS_3500,
        COINS_8000,
    )

    val oneTimeProducts = listOf(
        COINS_500,
        COINS_1500,
        COINS_3500,
        COINS_8000,
        STARTER_STYLE_PACK,
    )
}
