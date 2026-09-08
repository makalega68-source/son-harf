package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProBoosterContractTest {
    @Test
    fun premierBoostersAreServerAuthoritativeAndVisibleInUnifiedPro() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/PremierBoosters.kt").readText()
        val overlay = projectFile("app/src/main/java/com/sonharf/game/PremierBoosterOverlay.kt").readText()
        val integration = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val vip = projectFile("app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt").readText()
        val entitlements = projectFile("app/src/main/java/com/sonharf/game/data/VipEntitlements.kt").readText()

        assertTrue(backend.contains("get_premier_booster_status_v1"))
        assertTrue(backend.contains("use_premier_hint_v1"))
        assertTrue(backend.contains("use_premier_swap_v1"))
        assertTrue(backend.contains("use_premier_multiplier_v1"))
        assertTrue(backend.contains("multiplier_armed"))
        assertTrue(overlay.contains("usePremierHint"))
        assertTrue(overlay.contains("usePremierSwap"))
        assertTrue(overlay.contains("usePremierMultiplier"))
        assertTrue(integration.contains("PremierBoosterOverlay()"))
        assertTrue(vip.contains("2x Skor"))
        assertTrue(vip.contains("claimVipDailyHelpers"))
        assertTrue(entitlements.contains("\"multiplier_count\""))
    }

    @Test
    fun authoritativeMigrationLocksTimingInventoryAndScoreMultiplier() {
        val migration = projectFile("supabase/migrations/20260908155955_unified_pro_boosters_and_turn20.sql").readText()

        assertTrue(migration.contains("interval '20 seconds'"))
        assertTrue(migration.contains("interval '15 seconds'"))
        assertTrue(migration.contains("add_points:=add_points*2"))
        assertTrue(migration.contains("on delete restrict", ignoreCase = true))
        assertTrue(migration.contains("use_premier_hint_v1"))
        assertTrue(migration.contains("use_premier_swap_v1"))
        assertTrue(migration.contains("use_premier_multiplier_v1"))
        assertTrue(migration.contains("revoke all on function public.use_premier_multiplier_v1(uuid) from public,anon"))
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
