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

        assertTrue(intro.contains("R.raw.intro_welcome"))
        assertTrue(intro.contains("Dilini seç / Choose your language"))
        assertTrue(intro.contains("TÜRKÇE"))
        assertTrue(intro.contains("ENGLISH"))
        // Logo 40% smaller than on the home screen, dropping in near the end.
        assertTrue(intro.contains("private val IntroLogoWidth = (320 * .6f).dp"))
        assertTrue(intro.contains("player.positionMs >= duration - INTRO_LOGO_LEAD_MS"))
        assertTrue(prefs.contains("language_complete"))
        assertTrue(prefs.contains("SonHarfPreferences.setLanguage"))
        val raw = listOf(File("src/main/res/raw/intro_welcome.mp4"), File("app/src/main/res/raw/intro_welcome.mp4")).first { it.exists() }
        assertTrue(raw.length() > 1_000_000L)
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
