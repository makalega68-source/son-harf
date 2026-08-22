package com.sonharf.game.billing

object ProductCatalog {
    const val VIP_MONTHLY = "vip_monthly"
    const val VIP_YEARLY = "vip_yearly"
    const val SEASON_PASS_MONTHLY = "season_pass_monthly"
    const val COINS_500 = "coins_500"
    const val COINS_1500 = "coins_1500"
    const val THEME_NEON = "theme_neon"

    val subscriptions = listOf(VIP_MONTHLY, VIP_YEARLY, SEASON_PASS_MONTHLY)
    val oneTimeProducts = listOf(COINS_500, COINS_1500, THEME_NEON)
}
