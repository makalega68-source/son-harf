package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCanvaArtworkContractTest {
    @Test
    fun `store artwork stays transparent text free and product specific`() {
        val preview = repoFile("app/src/main/java/com/sonharf/game/StoreProductPreview.kt").readText()
        val drawables = listOf(
            "store_art_theme_black.xml",
            "store_art_keyboard_crystal.xml",
            "store_art_keyboard_obsidian.xml",
            "store_art_keyboard_midnight.xml",
            "store_art_keyboard_black_gold.xml",
            "store_art_keyboard_premium_white.xml",
            "store_art_name_cyan.xml",
            "store_art_name_sapphire.xml",
            "store_art_name_amethyst.xml",
            "store_art_name_aurelia.xml",
            "store_art_victory_crown.xml",
            "store_art_emoji_vip.xml",
        )

        drawables.forEach { fileName ->
            val vector = repoFile("app/src/main/res/drawable/$fileName").readText()
            assertTrue("Missing vector root for $fileName", vector.contains("<vector"))
            assertFalse("Decorative store artwork must not bake text: $fileName", vector.contains("<text"))
        }

        listOf(
            "R.drawable.store_art_theme_black",
            "R.drawable.store_art_keyboard_crystal",
            "R.drawable.store_art_keyboard_obsidian",
            "R.drawable.store_art_keyboard_midnight",
            "R.drawable.store_art_keyboard_black_gold",
            "R.drawable.store_art_keyboard_premium_white",
            "R.drawable.store_art_name_cyan",
            "R.drawable.store_art_name_sapphire",
            "R.drawable.store_art_name_amethyst",
            "R.drawable.store_art_name_aurelia",
            "R.drawable.store_art_victory_crown",
            "R.drawable.store_art_emoji_vip",
        ).forEach { mapping -> assertTrue("Missing store artwork mapping $mapping", preview.contains(mapping)) }

        assertTrue(preview.contains("Color.Transparent"))
    }

    @Test
    fun `live Black Theme and every purchasable keyboard have runtime presentation`() {
        val runtime = repoFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()
        val catalog = repoFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val store = repoFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val owned = repoFile("app/src/main/java/com/sonharf/game/OwnedStylePolicy.kt").readText()

        listOf("theme_black", "theme_dark_arena").forEach { id ->
            assertTrue(runtime.contains("\"$id\""))
            assertTrue(catalog.contains("\"$id\""))
            assertTrue(store.contains("\"$id\""))
            assertTrue(owned.contains("\"$id\""))
        }

        listOf(
            "keyboard_crystal",
            "keyboard_obsidian",
            "keyboard_midnight",
            "keyboard_black_gold",
            "keyboard_premium_white",
        ).forEach { id ->
            assertTrue("Runtime palette missing for $id", runtime.contains("\"$id\""))
            assertTrue("Store readiness missing for $id", store.contains("\"$id\""))
            assertTrue("Owned-style support missing for $id", owned.contains("\"$id\""))
        }
    }

    @Test
    fun `crown victory and vip emoji are exposed only with real runtime behavior`() {
        val store = repoFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val owned = repoFile("app/src/main/java/com/sonharf/game/OwnedStylePolicy.kt").readText()
        val siege = repoFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val effects = repoFile("app/src/main/java/com/sonharf/game/PremiumReactionCosmetics.kt").readText()

        assertTrue(store.contains("\"victory_effect\" -> id == \"victory_crown\""))
        assertTrue(store.contains("\"emoji_pack\" -> id == \"emoji_vip\""))
        assertTrue(owned.contains("\"victory_effect\" -> id == \"victory_crown\""))
        assertTrue(owned.contains("\"emoji_pack\" -> id == \"emoji_vip\""))
        assertTrue(siege.contains("won && SonHarfCosmetics.crownVictory"))
        assertTrue(siege.contains("CrownVictoryCelebration("))
        assertTrue(siege.contains("SonHarfCosmetics.emojiPackId == \"emoji_vip\""))
        assertTrue(siege.contains("VipEmojiReactionRow("))
        assertTrue(effects.contains("R.drawable.store_art_victory_crown"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
