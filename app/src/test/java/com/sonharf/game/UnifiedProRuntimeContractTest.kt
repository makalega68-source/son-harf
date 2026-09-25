package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProRuntimeContractTest {
    @Test
    fun startupRoutesOnlyToProfessionalShellAndPrimaryKelimeKusatmasi() {
        val startup = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val professional = File("src/main/java/com/sonharf/game/ProfessionalUnifiedApp.kt").readText()
        val home = File("src/main/java/com/sonharf/game/ProfessionalHomeScreen.kt").readText()
        val integration = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()

        assertTrue(startup.contains("ProfessionalUnifiedApp("))
        assertFalse(startup.contains("LiveDuelRuntimeShell("))
        assertFalse(startup.contains("MonsterExperienceApp("))
        assertFalse(startup.contains("\n    UnifiedProApp(onSignedOut"))
        assertFalse(startup.contains("\n    PremiumUnifiedProApp(onSignedOut"))

        assertTrue(home.contains("KELİME KUŞATMASI"))
        assertTrue(home.contains("HEMEN OYNA"))
        assertTrue(professional.contains("ProfessionalDestination.SIEGE -> WordSiegeEntryScreen"))
        assertTrue(professional.contains("ProfessionalDestination.LAST_LETTER -> OnlineGameScreenV6()"))
        assertTrue(professional.contains("ProfessionalDestination.LETTER_PATH -> LetterLadderGameScreen"))
        assertFalse(professional.contains("MageCat"))

        assertTrue(integration.contains("PremierWordDuelScreen()"))
        assertFalse(integration.contains("ReactiveMageCatOverlay()"))
        assertFalse(integration.contains("PremierBoosterOverlay()"))
        assertTrue(integration.contains("Ranked Premier is skill-only"))
        assertFalse(File("src/main/java/com/sonharf/game/mascot").exists())
        assertFalse(File("src/main/res/drawable-nodpi/mage_cat_runtime.webp").exists())
        assertFalse(File("src/main/java/com/sonharf/game/LiveDuelRuntimeShell.kt").exists())
        assertFalse(File("src/main/java/com/sonharf/game/MonsterExperienceApp.kt").exists())
        assertFalse(File("src/main/java/com/sonharf/game/ProfHammyCompanion.kt").exists())
        assertFalse(File("src/main/java/com/sonharf/game/ProfHammyPremiumHomeCard.kt").exists())
        assertFalse(File("src/main/res/drawable-nodpi/prof_hammy_hero.webp").exists())
    }
}
