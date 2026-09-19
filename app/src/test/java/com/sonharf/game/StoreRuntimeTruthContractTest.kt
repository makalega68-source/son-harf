package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRuntimeTruthContractTest {
    @Test
    fun currentlySellableStyleFamiliesHaveRealRuntimeImplementations() {
        val cosmetics = projectFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()
        val economy = projectFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()

        assertTrue(cosmetics.contains("theme_black"))
        listOf(
            "keyboard_crystal",
            "keyboard_obsidian",
            "keyboard_midnight",
            "keyboard_black_gold",
            "keyboard_premium_white",
        ).forEach { id -> assertTrue("Missing keyboard runtime for $id", cosmetics.contains(id)) }
        listOf("name_cyan", "name_sapphire", "name_amethyst", "name_aurelia")
            .forEach { id -> assertTrue("Missing name style runtime for $id", cosmetics.contains(id)) }

        assertTrue(economy.contains("supportedGameThemeIds = setOf(\"theme_black\", \"theme_dark_arena\")"))
    }

    @Test
    fun serverCatalogDisablesAnythingWithoutVerifiedRuntimeSupport() {
        val migration = projectFile("supabase/migrations/20260920002000_store_runtime_truth_v1.sql").readText()

        assertTrue(migration.contains("update public.shop_items"))
        assertTrue(migration.contains("set active = false"))
        assertTrue(migration.contains("shop_items_runtime_sale_guard_v1"))
        assertTrue(migration.contains("kind = 'game_theme' and id = 'theme_black'"))
        listOf(
            "keyboard_crystal",
            "keyboard_obsidian",
            "keyboard_midnight",
            "keyboard_black_gold",
            "keyboard_premium_white",
            "name_cyan",
            "name_sapphire",
            "name_amethyst",
            "name_aurelia",
        ).forEach { id -> assertTrue("Sellable allowlist is missing $id", migration.contains("'$id'")) }

        assertFalse("Mascots must not be silently allowlisted before runtime integration", migration.contains("kind = 'mascot' and"))
        assertFalse("Profile frames are currently retired and must not be sellable", migration.contains("kind = 'profile_frame' and"))
        assertFalse("VIP emoji is not mounted in a live chat surface yet", migration.contains("kind = 'emoji_pack' and"))
        assertFalse("Crown victory is not mounted as a purchased victory result yet", migration.contains("kind = 'victory_effect' and"))
    }

    @Test
    fun seasonPassCopyAndRewardsOnlyPromiseDeliverableValue() {
        val migration = projectFile("supabase/migrations/20260920002000_store_runtime_truth_v1.sql").readText()
        val card = projectFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()

        assertTrue(migration.contains("season_id = 'launch-2026'"))
        assertTrue(migration.contains("reward_type = 'son_coin'"))
        assertTrue(migration.contains("amount = 100"))
        listOf("title", "badge", "nameplate", "word_effect", "victory_effect", "vs_intro", "final_style")
            .forEach { kind -> assertTrue("Season migration is missing $kind", migration.contains("'$kind'")) }

        assertTrue(card.contains("daha fazla Son Coin"))
        assertTrue(card.contains("more Son Coins"))
        assertFalse(card.contains("özel Style"))
        assertFalse(card.contains("exclusive Style"))
        assertFalse(card.contains("sezon unvanları"))
        assertFalse(card.contains("season titles"))
    }

    @Test
    fun proRankedLiveAssistIsServerAuthoritativelyDisabled() {
        val migration = projectFile("supabase/migrations/20260920002000_store_runtime_truth_v1.sql").readText()
        assertTrue(migration.contains("'ranked_live_assist', false"))
        assertFalse(migration.contains("'ranked_live_assist', true"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing project file: $path")
}
