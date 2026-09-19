package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEffectsRuntimeContractTest {
    @Test
    fun `crown victory and vip emoji are real equipped cosmetics`() {
        val siege = repoFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val effects = repoFile("app/src/main/java/com/sonharf/game/PremiumReactionCosmetics.kt").readText()
        val store = repoFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val owned = repoFile("app/src/main/java/com/sonharf/game/OwnedStylePolicy.kt").readText()

        assertTrue(siege.contains("won && SonHarfCosmetics.crownVictory"))
        assertTrue(siege.contains("CrownVictoryCelebration("))
        assertTrue(siege.contains("eventKey = \"siege:${'$'}{game.id}\""))
        assertTrue(siege.contains("SonHarfCosmetics.emojiPackId == \"emoji_vip\""))
        assertTrue(siege.contains("VipEmojiReactionRow(enabled = !busy, onSend = onSend)"))
        assertTrue(siege.contains("backend.sendWordSiegeMessage(gameId, text)"))

        assertTrue(effects.contains("internal val VipEmojiReactions"))
        assertTrue(effects.contains("R.drawable.store_art_victory_crown"))
        assertTrue(store.contains("StoreTab(sh(\"EFEKTLER\", \"EFFECTS\")"))
        assertTrue(store.contains("\"victory_effect\" -> id == \"victory_crown\""))
        assertTrue(store.contains("\"emoji_pack\" -> id == \"emoji_vip\""))
        assertTrue(owned.contains("\"victory_effect\" -> id == \"victory_crown\""))
        assertTrue(owned.contains("\"emoji_pack\" -> id == \"emoji_vip\""))
    }

    @Test
    fun `premium effects remain cosmetic and preserve typed chat`() {
        val siege = repoFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val effects = repoFile("app/src/main/java/com/sonharf/game/PremiumReactionCosmetics.kt").readText()

        assertTrue(siege.contains("if (chatInput.trim() == text) chatInput = \"\""))
        assertTrue(effects.contains("normal chat transport"))
        assertFalse(effects.contains("playerOneWordScore"))
        assertFalse(effects.contains("playerTwoWordScore"))
        assertFalse(effects.contains("matchmaking"))
        assertFalse(effects.contains("purchaseShopItem"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
