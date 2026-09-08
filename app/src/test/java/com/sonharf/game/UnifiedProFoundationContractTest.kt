package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProFoundationContractTest {
    private fun projectFile(pathFromApp: String): File {
        val direct = File(pathFromApp)
        if (direct.exists()) return direct
        val underApp = File("app", pathFromApp)
        if (underApp.exists()) return underApp
        error("Could not resolve project file: $pathFromApp from ${File(".").absolutePath}")
    }

    private fun source(path: String): String = projectFile(path).readText()

    @Test
    fun bilingualFacadeKeepsCanonicalDictionaryAsSingleAuthority() {
        val engine = source("src/main/java/com/sonharf/game/data/DictionaryEngine.kt")
        assertTrue(engine.contains("SharedDictionaryService.isValidWordBlocking"))
        assertTrue(engine.contains("SharedDictionaryService.isValidWord(word, language)"))
        assertFalse(engine.contains("hashSetOf"))
        assertFalse(engine.contains("loadDictionary("))
    }

    @Test
    fun liveDuelRemainsServerAuthoritativeWithReadOnlyMascotLayer() {
        val shell = source("src/main/java/com/sonharf/game/LiveDuelRuntimeShell.kt")
        val mascot = source("src/main/java/com/sonharf/game/mascot/ReactiveMageCatOverlay.kt")
        assertTrue(shell.contains("RefinedDuelOverlay()"))
        assertTrue(shell.contains("BotTurnWatchdogOverlay()"))
        assertTrue(shell.contains("ReactiveMageCatOverlay(roomId)"))
        assertTrue(mascot.contains("backend.getRoom(roomId)"))
        assertFalse(mascot.contains("submitWord("))
        assertFalse(mascot.contains("claimTurnTimeout("))
        assertFalse(mascot.contains("botTakeTurn("))
    }

    @Test
    fun onboardingTeachesFairPlayWithoutCompetitivePaidPower() {
        val onboarding = source("src/main/java/com/sonharf/game/FirstRunOnboarding.kt")
        assertTrue(onboarding.contains("payments never grant match power"))
        assertTrue(onboarding.contains("hiçbir ödeme maç gücü vermez"))
        assertTrue(onboarding.contains("non-competitive modes"))
        assertTrue(onboarding.contains("rekabet dışı modlarda"))
        assertFalse(onboarding.contains("2x"))
        assertFalse(onboarding.contains("score multiplier", ignoreCase = true))
    }

    @Test
    fun existingInstallsAreNotForcedThroughNewOnboarding() {
        val prefs = source("src/main/java/com/sonharf/game/FirstRunLanguagePreferences.kt")
        val app = source("src/main/java/com/sonharf/game/StableV1App.kt")
        assertTrue(prefs.contains("getBoolean(ONBOARDING_REQUIRED, false)"))
        assertTrue(prefs.contains("putBoolean(ONBOARDING_REQUIRED, true)"))
        assertTrue(prefs.contains("completeOnboarding"))
        assertTrue(app.contains("FirstRunOnboarding"))
    }
}
