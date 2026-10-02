package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunLanguageSelectorContractTest {
    @Test
    fun introAndLanguagePrecedeSignInAndSignedInPlayersGoStraightHome() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val intro = projectFile("app/src/main/java/com/sonharf/game/IntroWelcome.kt").readText()
        val prefs = projectFile("app/src/main/java/com/sonharf/game/FirstRunLanguagePreferences.kt").readText()

        // The stored session is checked first; only signed-out players see the intro and sign-in.
        assertTrue(shell.indexOf("hasVerifiedMembershipSession") < shell.indexOf("IntroWelcomeScreen {"))
        assertTrue(shell.indexOf("IntroWelcomeScreen {") < shell.indexOf("CompactAuthGate { authenticated = true }"))
        assertTrue(shell.contains("FirstRunLanguagePreferences.complete(context, language)"))
        // Signing out shows the welcome again.
        assertTrue(shell.contains("introDone = false\n            authenticated = false"))
        assertTrue(!shell.contains("private fun FirstRunLanguageScreen("))

        assertTrue(!intro.contains("ChromaKeyVideo"))
        assertTrue(!intro.contains("intro_welcome"))
        assertTrue(intro.contains("onContinue(selected)"))
        assertTrue(intro.contains("TÜRKÇE"))
        assertTrue(intro.contains("ENGLISH"))
        assertTrue(prefs.contains("language_complete"))
        assertTrue(prefs.contains("SonHarfPreferences.setLanguage"))
        assertTrue(!projectFile("app/src/main/res/raw").resolve("intro_welcome.mp4").exists())

    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
