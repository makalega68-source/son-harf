package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreEconomyTest {
    @Test fun tierComesFromTheServerTierFirstThenRarity() {
        assertEquals(StoreTier.STARTER, StoreTier.from("starter", "STANDARD"))
        assertEquals(StoreTier.LEGENDARY, StoreTier.from("legendary", "EPIC"))
        assertEquals(StoreTier.EPIC, StoreTier.from(null, "EPIC"))
        assertEquals(StoreTier.COMMON, StoreTier.from(null, "STANDARD"))
        assertEquals(StoreTier.PRESTIGE, StoreTier.from("prestige", null))
    }

    @Test fun progressShowsCoinShareAndWinRequirement() {
        val p = StoreGoalProgress.of(price = 10_000, balance = 7_850, minWins = 100, wins = 75)
        assertEquals(78, p.percent)
        assertEquals(2_150, p.coinsMissing)
        assertFalse(p.requirementMet)
        val done = StoreGoalProgress.of(price = 500, balance = 900, minWins = null, wins = 0)
        assertEquals(100, done.percent)
        assertEquals(0, done.coinsMissing)
        assertTrue(done.requirementMet)
    }

    @Test fun migrationKeepsBalancesAndEnforcesRequirementsOnTheServer() {
        val sql = listOf(File("../supabase/migrations/20260929120000_economy_rebalance_v1.sql"), File("supabase/migrations/20260929120000_economy_rebalance_v1.sql"))
            .first(File::exists).readText()
        assertTrue(sql.contains("requirement_not_met"))
        assertTrue(sql.contains("piggy_daily_limit"))
        assertTrue(sql.contains("array[5, 5, 10, 10, 15, 15, 20]"))
        assertTrue(sql.contains("v_grant integer := 150"))
        // No balance is lowered by the rebalance: the only diamonds writes are grants/purchases.
        assertFalse(Regex("""update public\.profiles\s+set diamonds\s*=\s*\d""").containsMatchIn(sql))
        assertFalse(sql.contains("set diamonds = 0"))
    }
}
