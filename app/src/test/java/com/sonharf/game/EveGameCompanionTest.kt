package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EveGameCompanionTest {
    @Test fun eveTalksAboutTheMatchInHerOwnVoice() {
        val eve = WordSiegeMascotPersonality.EVE
        val own = listOf(
            WordSiegeMascotTopic.GREET_FIRST, WordSiegeMascotTopic.PRAISE, WordSiegeMascotTopic.BIG_PRAISE,
            WordSiegeMascotTopic.RARE_WORD, WordSiegeMascotTopic.COMFORT, WordSiegeMascotTopic.CRITICAL,
            WordSiegeMascotTopic.BEHIND, WordSiegeMascotTopic.RIVAL_STRONG, WordSiegeMascotTopic.AHEAD,
            WordSiegeMascotTopic.STREAK, WordSiegeMascotTopic.WIN, WordSiegeMascotTopic.LOSS, WordSiegeMascotTopic.DRAW,
        )
        own.forEach { topic -> assertTrue("$topic", eve.lines(topic, emptyList()).isNotEmpty()) }
        WordSiegeMascotTopic.entries.forEach { topic ->
            eve.lines(topic, emptyList()).forEach { (tr, en) ->
                assertTrue(tr.isNotBlank() && en.isNotBlank())
                // She never speaks as Obi, and a name is never glued to a Turkish suffix.
                assertFalse(tr, tr.contains("Obi") || en.contains("Obi"))
                assertFalse(tr, Regex("%s[A-Za-zÇĞİÖŞÜçğıöşü'’]").containsMatchIn(tr))
                // Only the placeholders the mascot mind fills: %s name, %n number, %w word.
                assertFalse(tr, Regex("%[^snw]").containsMatchIn(tr))
                assertFalse(en, Regex("%[^snw]").containsMatchIn(en))
            }
        }
    }

    @Test fun eveTakesTheMascotsPlaceInsteadOfExtraLanes() {
        val companion = File("src/main/java/com/sonharf/game/WordSiegeMascotCompanion.kt").readText()
        assertTrue(companion.contains("val eve = forcedSkin == null && EveOwnership.activeForCurrentUser(context)"))
        assertTrue(companion.contains("if (requireOwnership && ownedSkins.isEmpty() && !throneKing && !eve) return"))
        assertTrue(companion.contains("EveMascotBody("))
        // The game screens keep their own layout; Eve comes only through the mascot companion.
        for (screen in listOf("PremierWordDuelScreen.kt", "WordSiegePracticeScreen.kt", "WordSiegePracticeBoard.kt", "WordSiegePanMatch.kt")) {
            val source = File("src/main/java/com/sonharf/game/$screen").readText()
            assertFalse(screen, source.contains("EveGameCompanion"))
            assertFalse(screen, source.contains("eve_game_placement"))
            assertFalse(screen, source.contains("eve_still"))
        }
        val practice = File("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        assertTrue(practice.contains("anchors = WordSiegeBoardMascotPerches"))
        // A slot too small for a bubble (Son Harf) shows no clipped bubble tail.
        assertTrue(companion.contains("if (bubbleRoom) speech?.let { text ->"))
    }

    @Test fun evesThreeDViewSkipsCostlyPasses() {
        // The live model stays light on busy game screens: no SSAO, bloom, MSAA or shadow maps.
        val eve = File("src/main/java/com/sonharf/game/EveHomeFriend.kt").readText()
        assertTrue(eve.contains("view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = false }"))
        assertTrue(eve.contains("view.bloomOptions = view.bloomOptions.apply { enabled = false }"))
        assertTrue(eve.contains("view.setShadowingEnabled(false)"))
        assertTrue(eve.contains("view = view,"))
    }

    @Test fun eveDoesNotWalkInTheSonHarfSlot() {
        val companion = File("src/main/java/com/sonharf/game/WordSiegeMascotCompanion.kt").readText()
        assertTrue(companion.contains("val eveStaysPut = eve && areaHeight <= baseSizePx * 1.8f"))
        assertTrue(companion.contains("if (eveStaysPut) return@flyTo"))
    }
}
