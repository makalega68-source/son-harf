package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonCenterLiveRpcContractTest {
    @Test
    fun `season center is server driven and claims only through live rpc`() {
        val data = repoFile("app/src/main/java/com/sonharf/game/data/SeasonStore.kt").readText()

        assertTrue(data.contains("get_store_season_v1"))
        assertTrue(data.contains("claim_store_season_reward_v1"))
        listOf(
            "season_id",
            "starts_at",
            "ends_at",
            "duration_days",
            "premium_active",
            "reward_type",
            "reward_key",
            "premium_access",
        ).forEach { field -> assertTrue("Missing server field $field", data.contains(field)) }
        assertTrue(data.contains("val level: Int"))
        assertTrue(data.contains("val rewards: List<SeasonRewardDto>"))
        assertTrue(data.contains("val unlocked: Boolean"))
        assertTrue(data.contains("val claimed: Boolean"))
    }

    @Test
    fun `season claim never mutates wallet or inventory from client`() {
        val data = repoFile("app/src/main/java/com/sonharf/game/data/SeasonStore.kt").readText()
        val combined = data

        assertFalse(combined.contains("from(\"profiles\")"))
        assertFalse(combined.contains("from(\"user_inventory\")"))
        assertFalse(combined.contains("diamond_ledger"))
        assertFalse(combined.contains("insert("))
        assertFalse(combined.contains("update("))
    }

    @Test
    fun `season center keeps fair play copy accessible cta and store without the season shelf`() {
        val shop = repoFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val pass = repoFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()

        assertTrue(shop.contains("initialTab.coerceIn(0, 3)"))
        // The store now sells only PRO and mascots, so the season pass is off the shelf.
        assertFalse(shop.contains("sh(\"Sezon\", \"Season\")"))
        assertFalse(shop.contains("SeasonCenterContent()"))
        assertTrue(pass.contains("rating, time, joker power or match advantages"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}