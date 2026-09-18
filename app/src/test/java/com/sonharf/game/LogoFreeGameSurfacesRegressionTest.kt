package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoFreeGameSurfacesRegressionTest {
    @Test
    fun activeGameSurfacesUseTypographyAndFunctionalIconsInsteadOfLogoArtwork() {
        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val siege = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val ladder = File("src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val lastLetter = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val visibleSources = listOf(entries, home, siege, ladder, lastLetter)
        visibleSources.forEach { source ->
            assertFalse(source.contains("R.drawable.kelime_kusatma_logo_hd"))
            assertFalse(source.contains("R.drawable.harf_yolu_logo"))
            assertFalse(source.contains("R.drawable.son_harf_app_icon_master"))
        }
        assertTrue(entries.contains("PremiumSiegeEntryScreen"))
        assertTrue(entries.contains("PremiumLastLetterEntryScreen"))
        assertTrue(entries.contains("PremiumLetterPathEntryScreen"))
        assertTrue(siege.contains("KELİME KUŞATMASI"))
        assertTrue(ladder.contains("Icons.Rounded.Route"))
    }
}
