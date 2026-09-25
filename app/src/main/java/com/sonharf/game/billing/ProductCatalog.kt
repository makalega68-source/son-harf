package com.sonharf.game.billing

object ProductCatalog {
    const val VIP_MONTHLY = "vip_monthly"
    const val VIP_YEARLY = "vip_yearly"
    const val SEASON_PASS_MONTHLY = "season_pass_monthly"

    const val COINS_500 = "coins_500"
    const val COINS_1500 = "coins_1500"
    const val COINS_3500 = "coins_3500"
    const val COINS_8000 = "coins_8000"

    // Kelime Tahtı permanent premium products.
    const val SERIES_GAME = "series_game"
    const val LETTER_TABLE = "letter_table"
    const val SCORE_CALCULATOR = "score_calculator"
    const val PRO_LIFETIME = "pro_lifetime"

    const val SERIES_GAME_FALLBACK_PRICE_TRY = "129 TL"
    const val LETTER_TABLE_FALLBACK_PRICE_TRY = "65 TL"
    const val SCORE_CALCULATOR_FALLBACK_PRICE_TRY = "129 TL"
    const val PRO_LIFETIME_FALLBACK_PRICE_TRY = "479 TL"

    // Mascot characters: permanent one-time products, 680 TL each (price set in Play Console).
    const val MASCOT_KLASIK = "mascot_klasik"
    const val MASCOT_PEMBE = "mascot_pembe"
    const val MASCOT_MAVI_SEYTANCIK = "mascot_mavi_seytancik"
    const val MASCOT_KIRMIZI_SEYTANCIK = "mascot_kirmizi_seytancik"
    const val MASCOT_TEKIR = "mascot_tekir"
    const val MASCOT_ROBOT = "mascot_robot"
    const val MASCOT_ASTRONOT = "mascot_astronot"
    const val MASCOT_LIST_PRICE_TRY = "680 TL"

    val mascotProducts = listOf(
        MASCOT_KLASIK,
        MASCOT_PEMBE,
        MASCOT_MAVI_SEYTANCIK,
        MASCOT_KIRMIZI_SEYTANCIK,
        MASCOT_TEKIR,
        MASCOT_ROBOT,
        MASCOT_ASTRONOT,
    )

    /** Legacy identifiers retained only so old receipts can be recognized safely. Frames are not sold or rendered. */
    @Deprecated("Profile frames were retired from the game")
    const val PROFILE_FRAME_OCEAN = "profile_frame_ocean"
    @Deprecated("Profile frames were retired from the game")
    const val PROFILE_FRAME_BOTANIC = "profile_frame_botanic"
    @Deprecated("Profile frames were retired from the game")
    const val PROFILE_FRAME_LILAC = "profile_frame_lilac"
    @Deprecated("Profile frames were retired from the game")
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
    )
}
