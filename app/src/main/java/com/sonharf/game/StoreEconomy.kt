package com.sonharf.game

/**
 * Collection tiers shown on store cards. The tier and any achievement requirement come from the
 * server (shop_items.metadata); the server also enforces them at purchase, so this is display only.
 */
internal enum class StoreTier(val tr: String, val en: String, val rank: Int) {
    STARTER("BAŞLANGIÇ", "STARTER", 0),
    COMMON("SIRADAN", "COMMON", 1),
    RARE("ENDER", "RARE", 2),
    EPIC("DESTANSI", "EPIC", 3),
    LEGENDARY("EFSANEVİ", "LEGENDARY", 4),
    PRESTIGE("PRESTİJ", "PRESTIGE", 5),
    ;

    val label: String get() = if (SonHarfUiState.isEnglish) en else tr

    companion object {
        fun from(economyTier: String?, rarity: String?): StoreTier = when (economyTier?.lowercase()) {
            "starter" -> STARTER
            "common" -> COMMON
            "rare" -> RARE
            "epic" -> EPIC
            "legendary" -> LEGENDARY
            "prestige" -> PRESTIGE
            else -> when (rarity?.uppercase()) {
                "RARE" -> RARE
                "EPIC" -> EPIC
                "LEGENDARY" -> LEGENDARY
                "PRESTIGE" -> PRESTIGE
                else -> COMMON
            }
        }
    }
}

/** How close a player is to an item: coin share and the win requirement, both clamped to 0..1. */
internal data class StoreGoalProgress(
    val coinFraction: Float,
    val coinsMissing: Int,
    val winsNeeded: Int?,
    val winsHave: Int,
) {
    val requirementMet: Boolean get() = winsNeeded == null || winsHave >= winsNeeded
    val percent: Int get() = (coinFraction * 100).toInt()

    companion object {
        fun of(price: Int, balance: Int?, minWins: Int?, wins: Int?): StoreGoalProgress {
            val have = (balance ?: 0).coerceAtLeast(0)
            val fraction = if (price <= 0) 1f else (have.toFloat() / price).coerceIn(0f, 1f)
            return StoreGoalProgress(fraction, (price - have).coerceAtLeast(0), minWins?.takeIf { it > 0 }, (wins ?: 0).coerceAtLeast(0))
        }
    }
}
