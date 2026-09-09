package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProBoosterContractTest {
    @Test
    fun rankedPremierKeepsCompatibilityApisButDoesNotExposePurchasableMatchPower() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/PremierBoosters.kt").readText()
        val overlay = projectFile("app/src/main/java/com/sonharf/game/PremierBoosterOverlay.kt").readText()
        val integration = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val premier = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val vip = projectFile("app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt").readText()
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val entitlements = projectFile("app/src/main/java/com/sonharf/game/data/VipEntitlements.kt").readText()

        // Compatibility API remains available in source for old clients/non-ranked future reuse.
        assertTrue(backend.contains("get_premier_booster_status_v1"))
        assertTrue(backend.contains("use_premier_hint_v1"))
        assertTrue(backend.contains("use_premier_swap_v1"))
        assertTrue(backend.contains("use_premier_multiplier_v1"))
        assertTrue(overlay.contains("usePremierHint"))
        assertTrue(overlay.contains("usePremierSwap"))
        assertTrue(overlay.contains("usePremierMultiplier"))

        // Ranked runtime must mount neither paid gameplay power nor mascot overlays.
        assertTrue(integration.contains("PremierWordDuelScreen()"))
        assertFalse(integration.contains("ReactiveMageCatOverlay()"))
        assertFalse(integration.contains("PremierBoosterOverlay()"))
        assertFalse(premier.contains("PremierBoosterUiState.requiredOverride"))
        assertFalse(premier.contains("com.sonharf.game.mascot"))

        assertFalse(vip.contains("2x Skor"))
        assertFalse(vip.contains("claimVipDailyHelpers"))
        assertTrue(vip.contains("ADİL REKABET"))
        assertTrue(entitlements.contains("rankedLiveAssist: Boolean = false"))

        // Store messaging must describe only fair PRO value, never a paid ranked advantage.
        assertTrue(shop.contains("Dereceli Premier maçlarında satın alınabilir oyun gücü yoktur"))
        assertTrue(shop.contains("Adil rekabet: PRO"))
        assertFalse(shop.contains("günlük İpucu, Harf Değiştirici ve 2x Skor"))
        assertFalse(shop.contains("daily Hint, Letter Swap and 2x Score"))
        assertFalse(shop.contains("helper boosters with PRO"))
    }

    @Test
    fun laterFairPlayMigrationNeutralizesTheLegacyBoosterRegressionServerSide() {
        val legacy = projectFile("supabase/migrations/20260908155955_unified_pro_boosters_and_turn20.sql").readText()
        val fairPlay = projectFile("supabase/migrations/20260909113000_restore_premier_fair_play_v1.sql").readText()

        // Preserve the independent 20-second server turn work from the legacy migration.
        assertTrue(legacy.contains("interval '20 seconds'"))
        assertTrue(legacy.contains("on delete restrict", ignoreCase = true))

        // But competitive power is explicitly inert in the later authoritative migration.
        assertTrue(fairPlay.contains("select p_default"))
        assertTrue(fairPlay.contains("select false"))
        assertTrue(fairPlay.contains("competitive_booster_disabled"))
        assertTrue(fairPlay.contains("alter column hint_count set default 0"))
        assertTrue(fairPlay.contains("alter column swap_count set default 0"))
        assertTrue(fairPlay.contains("alter column multiplier_count set default 0"))
        assertTrue(fairPlay.contains("'ranked_live_assist', false"))
        assertTrue(fairPlay.contains("'rewarded_ad_bypass', coalesce(v_vip, false)"))
    }

    @Test
    fun retiredClassicRuntimeCannotReenterStartupPath() {
        val startup = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val unified = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        assertTrue(startup.contains("UnifiedProApp("))
        assertTrue(unified.contains("UnifiedDestination.VIP -> UnifiedProVipScreen"))
        assertFalse(projectFileOrNull("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt")?.exists() == true)
        assertFalse(projectFileOrNull("app/src/main/java/com/sonharf/game/LiveDuelRuntimeShell.kt")?.exists() == true)
    }

    private fun projectFile(path: String): File {
        val file = projectFileOrNull(path)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }

    private fun projectFileOrNull(path: String): File? =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
}
