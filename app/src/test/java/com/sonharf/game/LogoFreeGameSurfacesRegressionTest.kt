package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoFreeGameSurfacesRegressionTest {
    @Test
    fun activeGameSurfacesUseTypographyAndFunctionalIconsInsteadOfLogoArtwork() {
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val lastLetter = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        listOf(home, siege, ladder, lastLetter).forEach { source ->
            assertFalse(source.contains("R.drawable.kelime_kusatma_logo_hd"))
            assertFalse(source.contains("R.drawable.harf_yolu_logo"))
            assertFalse(source.contains("R.drawable.son_harf_app_icon_master"))
        }
        assertTrue(home.contains("Icons.Rounded.GridView"))
        assertTrue(home.contains("Icons.Rounded.Bolt"))
        assertTrue(home.contains("Icons.Rounded.Route"))
        assertTrue(siege.contains("KELİME KUŞATMASI"))
        assertTrue(ladder.contains("Icons.Rounded.Route"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
