package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProRuntimeContractTest {
    @Test
    fun startupRoutesOnlyToAdultApkV2ShellAndPrimaryKelimeKusatmasi() {
        val startup = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val active = File("src/main/java/com/sonharf/game/PremiumAdultApp.kt").readText()
        val legacyV2 = File("src/main/java/com/sonharf/game/PremiumCanvaAppV2.kt").readText()
        val integration = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()

        assertTrue(startup.contains("PremiumAdultApp("))
        assertFalse(startup.contains("PremiumCanvaAppV2(onSignedOut"))
        assertFalse(startup.contains("UnifiedProApp("))
        assertFalse(startup.contains("LiveDuelRuntimeShell("))
        assertFalse(startup.contains("MonsterExperienceApp("))

        assertTrue(active.contains("AdultDestination.SIEGE -> WordSiegeExperienceScreen"))
        assertTrue(active.contains("AdultDestination.LAST_LETTER -> OnlineGameScreenV6()"))
        assertTrue(active.contains("AdultDestination.LETTER_PATH -> LetterLadderGameScreen"))
        assertTrue(active.contains("AdultDestination.SHOP -> PremiumStoreScreen"))
        assertTrue(active.contains("Kelime Kuşatması"))
        assertTrue(active.contains("Harf Yolu"))
        assertTrue(active.contains("Ana Sayfa"))
        assertTrue(active.contains("Sosyal"))
        assertTrue(active.contains("Mağaza"))
        assertTrue(active.contains("Profil"))
        assertFalse(active.contains("MageCat"))

        // The user's APK-v2 implementation remains in-repo as a rollback/reference source, but is inactive.
        assertTrue(legacyV2.contains("PremiumV2Destination.SIEGE -> WordSiegeExperienceScreen"))

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
