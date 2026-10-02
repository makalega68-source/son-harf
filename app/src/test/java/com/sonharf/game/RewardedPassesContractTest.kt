package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RewardedPassesContractTest {
    @Test fun rewardedVideosAreServerCappedAndVerified() {
        val sql = repoFile("supabase/migrations/20260928120000_rewarded_passes_and_play_keyboards_v1.sql")
        // The rule table: keyboard a day (2 a week), 5 Quick Games (1 a day), +2 hints (2 a day).
        assertTrue(sql.contains("('keyboard_day',   2, 7, 0, 24)"))
        assertTrue(sql.contains("('quick_games',    1, 1, 5, 72)"))
        assertTrue(sql.contains("('hints_son_harf', 2, 1, 2, 168)"))
        // Hint videos are only for players without a mascot.
        assertTrue(sql.contains("raise exception 'mascot_owner'"))
        // Rewards are granted only through the SSV-fulfilled path.
        assertTrue(sql.contains("return public.grant_reward_pass_internal_v1(p_reward_type, v_proof, p_trial_item_id);"))
        assertTrue(sql.contains("revoke all on function public.grant_reward_pass_internal_v1(text, text, text) from public, anon, authenticated;"))
        // A Quick Game pass opens the paid mode on the server.
        assertTrue(sql.contains("or public.has_series_ad_pass_v1(p_user_id)"))
    }

    @Test fun keyboardsExceptWhiteAreGooglePlayProducts() {
        val sql = repoFile("supabase/migrations/20260928120000_rewarded_passes_and_play_keyboards_v1.sql")
        assertTrue(sql.contains("raise exception 'play_only';"))
        assertTrue(sql.contains("'keyboard_black_gold','keyboard_crystal','keyboard_midnight','keyboard_obsidian'"))
        val catalog = repoFile("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt")
        assertTrue(catalog.contains("const val KEYBOARD_LIST_PRICE_TRY = \"50 TL\""))
        assertFalse(catalog.contains("\"keyboard_premium_white\""))
        val verify = repoFile("supabase/functions/verify-play-purchase/index.ts")
        assertTrue(verify.contains("...keyboardProducts"))
        val shop = repoFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt")
        assertTrue(shop.contains("billing.queryOneTimeProducts(com.sonharf.game.billing.ProductCatalog.keyboardProducts)"))
        assertTrue(shop.contains("onClick = { tab = 8 }"))
        assertTrue(shop.contains("if (shown == 8) RewardCenterScreen("))
        assertTrue(shop.contains("oneTimePurchaseOfferDetails?.formattedPrice"))
    }

    @Test fun bankedHintsAndDayPassesReachTheGames() {
        assertTrue(repoFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").contains("RewardPassState.useHint(\"son_harf\")"))
        assertTrue(repoFile("app/src/main/java/com/sonharf/game/KelimeAtolyesiScreen.kt").contains("RewardPassState.useHint(\"kelime_atolyesi\")"))
        assertTrue(repoFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").contains("RewardPassState.useHint(\"siege\")"))
        val cosmetics = repoFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt")
        assertTrue(cosmetics.contains("get() = keyboardPaletteFor(activeKeyboardId)"))
        // Offsets from Postgres ("+00:00") need OffsetDateTime on Android 8.
        assertTrue(repoFile("app/src/main/java/com/sonharf/game/RewardVideo.kt").contains("OffsetDateTime.parse(it)"))
        assertTrue(repoFile("app/src/main/java/com/sonharf/game/WordSiegeEntryScreen.kt").contains("com.sonharf.game.data.RewardKeys.QUICK_GAMES"))
    }

    private fun repoFile(path: String): String {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file).readText()
    }
}
