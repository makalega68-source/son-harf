package com.sonharf.game.billing

object ProductCatalog {
    const val VIP_MONTHLY = "vip_monthly"
    const val VIP_YEARLY = "vip_yearly"
    const val SEASON_PASS_MONTHLY = "season_pass_monthly"

    const val COINS_500 = "coins_500"
    const val COINS_1500 = "coins_1500"
    const val COINS_3500 = "coins_3500"
    const val COINS_8000 = "coins_8000"

    // Kelime Kuşatması permanent premium products.
    const val SERIES_GAME = "series_game"
    const val LETTER_TABLE = "letter_table"
    const val SCORE_CALCULATOR = "score_calculator"
    const val PRO_LIFETIME = "pro_lifetime"

    const val SERIES_GAME_FALLBACK_PRICE_TRY = "129 TL"
    const val LETTER_TABLE_FALLBACK_PRICE_TRY = "65 TL"
    const val SCORE_CALCULATOR_FALLBACK_PRICE_TRY = "129 TL"
    const val PRO_LIFETIME_FALLBACK_PRICE_TRY = "479 TL"

    // Profile Frames V2. Google Play one-time products; ownership is permanent and server-authoritative.
    const val PROFILE_FRAME_PINK_BLOSSOM = "profile_frame_pink_blossom"
    const val PROFILE_FRAME_BLUE_ROYAL = "profile_frame_blue_royal"
    const val PROFILE_FRAME_AMETHYST = "profile_frame_amethyst"
    const val PROFILE_FRAME_EMERALD = "profile_frame_emerald"
    const val PROFILE_FRAME_FALLBACK_PRICE_TRY = "150 TL"

    val profileFrameProducts = listOf(
        PROFILE_FRAME_PINK_BLOSSOM,
        PROFILE_FRAME_BLUE_ROYAL,
        PROFILE_FRAME_AMETHYST,
        PROFILE_FRAME_EMERALD,
    )

    /** Legacy identifiers retained only so old receipts can be recognized safely. Old artwork stays retired. */
    @Deprecated("Legacy profile frame product; not offered by Profile Frames V2")
    const val PROFILE_FRAME_OCEAN = "profile_frame_ocean"
    @Deprecated("Legacy profile frame product; not offered by Profile Frames V2")
    const val PROFILE_FRAME_BOTANIC = "profile_frame_botanic"
    @Deprecated("Legacy profile frame product; not offered by Profile Frames V2")
    const val PROFILE_FRAME_LILAC = "profile_frame_lilac"
    @Deprecated("Legacy profile frame product; not offered by Profile Frames V2")
    const val PROFILE_FRAME_ROSE = "profile_frame_rose"

    /** Historical entitlement only. The active shop must not offer it until a matching real asset exists. */
    @Deprecated("No verified runtime artwork is connected; historical purchases remain recognizable")
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

    val permanentPremiumProducts = setOf(
        SERIES_GAME,
        LETTER_TABLE,
        SCORE_CALCULATOR,
        PRO_LIFETIME,
    )

    /** Only products with a complete, truthful runtime delivery path belong in the active shop query. */
    val oneTimeProducts = listOf(
        SERIES_GAME,
        LETTER_TABLE,
        SCORE_CALCULATOR,
        PRO_LIFETIME,
        COINS_500,
        COINS_1500,
        COINS_3500,
        COINS_8000,
    ) + profileFrameProducts
}
