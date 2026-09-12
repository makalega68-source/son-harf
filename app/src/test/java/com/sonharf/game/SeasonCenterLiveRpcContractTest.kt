package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonCenterLiveRpcContractTest {
    @Test
    fun `season center is server driven and claims only through live rpc`() {
        val data = repoFile("app/src/main/java/com/sonharf/game/data/SeasonStore.kt").readText()
        val screen = repoFile("app/src/main/java/com/sonharf/game/SeasonCenterScreen.kt").readText()

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

        assertTrue(screen.contains("items = currentSeason.rewards"))
        assertTrue(screen.contains("b.claimStoreSeasonReward(reward)"))
        assertTrue(screen.contains("reward.rewardType"))
        assertTrue(screen.contains("reward.rewardKey"))
        assertTrue(screen.contains("reward.unlocked"))
        assertTrue(screen.contains("reward.premiumAccess"))
        assertTrue(screen.contains("reward.claimed"))

        listOf("season_word_master", "season_blue_badge", "season_blue_nameplate", "season_blue_word_fx", "season_blue_victory_fx", "season_blue_vs_intro", "season_blue_final_style")
            .forEach { liveRewardKey -> assertFalse("Live reward key must not be hard-coded in UI", screen.contains(liveRewardKey)) }
        assertFalse(screen.contains("listOf(1, 3, 5"))
        assertFalse(screen.contains("listOf(1,3,5"))
    }

    @Test
    fun `season claim never mutates wallet or inventory from client`() {
        val data = repoFile("app/src/main/java/com/sonharf/game/data/SeasonStore.kt").readText()
        val screen = repoFile("app/src/main/java/com/sonharf/game/SeasonCenterScreen.kt").readText()
        val combined = data + screen

        assertFalse(combined.contains("from(\"profiles\")"))
        assertFalse(combined.contains("from(\"user_inventory\")"))
        assertFalse(combined.contains("diamond_ledger"))
        assertFalse(combined.contains("insert("))
        assertFalse(combined.contains("update("))
        assertTrue(screen.contains("kalıcı koleksiyonuna eklendi"))
        assertTrue(screen.contains("permanent collection"))
    }

    @Test
    fun `season center keeps fair play copy accessible cta and existing shop navigation`() {
        val screen = repoFile("app/src/main/java/com/sonharf/game/SeasonCenterScreen.kt").readText()
        val shop = repoFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val pass = repoFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()

        assertTrue(shop.contains("initialTab.coerceIn(0, 2)"))
        assertTrue(shop.contains("Text(sh(\"SEZON\", \"SEASON\")"))
        assertTrue(shop.contains("else -> SeasonCenterContent()"))
        assertTrue(screen.contains("SeasonPassPurchaseCard"))
        assertTrue(screen.contains("heightIn(min = 48.dp)"))
        assertTrue(screen.contains("maç gücü, rating, süre veya rekabet avantajı vermez"))
        assertTrue(screen.contains("never grant match power, rating, time or competitive advantages"))
        assertTrue(pass.contains("rating, time, joker power or match advantages"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
