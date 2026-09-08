package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridMonetizationContractTest {
    private fun projectFile(pathFromApp: String): File {
        val direct = File(pathFromApp)
        if (direct.exists()) return direct
        val underApp = File("app", pathFromApp)
        if (underApp.exists()) return underApp
        error("Could not resolve project file: $pathFromApp")
    }

    private fun source(path: String) = projectFile(path).readText()

    @Test
    fun retiredAbsolutePayToWinBanIsNotAProductInvariant() {
        val model = source("../docs/MONETIZATION_MODEL.md")
        val onboarding = source("src/main/java/com/sonharf/game/FirstRunOnboarding.kt")

        assertTrue(model.contains("hybrid Freemium / Free-to-Play + advertising + Premium/PRO", ignoreCase = true))
        assertTrue(model.contains("Gameplay-affecting monetization is **allowed in principle**"))
        assertTrue(model.contains("mode-scoped and server-authoritative"))
        assertFalse(onboarding.contains("payments never grant match power"))
        assertFalse(onboarding.contains("hiçbir ödeme maç gücü vermez"))
    }

    @Test
    fun firstProHelperRolloutIsHintAndSwapOnly() {
        val migration = source("../supabase/migrations/20260908170000_hybrid_freemium_pro_v2.sql")
        val rewards = source("src/main/java/com/sonharf/game/RewardCenterScreen.kt")
        val api = source("src/main/java/com/sonharf/game/data/VipEntitlements.kt")

        assertTrue(migration.contains("swap_count = public.vip_joker_wallet.swap_count + 1"))
        assertTrue(migration.contains("hint_count = public.vip_joker_wallet.hint_count + 1"))
        assertTrue(migration.contains("values(v_uid, 0, 1, 1, 0, now())"))
        assertTrue(migration.contains("'ranked_live_assist',false"))
        assertTrue(rewards.contains("claimVipDailyHelpers()"))
        assertTrue(rewards.contains("support edilen oyun modlarında").not())
        assertTrue(rewards.contains("desteklenen oyun modlarında"))
        assertTrue(api.contains("claim_vip_daily_jokers_v7"))
    }

    @Test
    fun storeWindowAndPermanentOwnershipAreServerEnforced() {
        val migration = source("../supabase/migrations/20260908170000_hybrid_freemium_pro_v2.sql")

        assertTrue(migration.contains("available_from <= now()"))
        assertTrue(migration.contains("available_until is null or available_until > now()"))
        assertTrue(migration.contains("on delete restrict"))
        assertTrue(migration.contains("unreviewed_purchase_shop_item_definition"))
        assertTrue(migration.contains("shop_items_owned_read"))
    }

    @Test
    fun productKeepsOneUserFacingCurrencyWithoutRenamingLegacySchemaInPlace() {
        val model = source("../docs/MONETIZATION_MODEL.md")
        val rewards = source("src/main/java/com/sonharf/game/RewardCenterScreen.kt")

        assertTrue(model.contains("Son Coin is the single user-facing soft currency"))
        assertTrue(model.contains("database column name `diamonds` is a legacy implementation detail"))
        assertTrue(rewards.contains("SON COIN"))
    }
}
