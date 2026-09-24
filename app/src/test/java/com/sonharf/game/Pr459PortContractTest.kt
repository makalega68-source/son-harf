package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards how the PR #459 work lives in the navy professional shell. */
class Pr459PortContractTest {
    @Test
    fun inGameMascotIsBackInEveryGame() {
        listOf("PremierWordDuelScreen.kt", "WordSiegePanMatch.kt", "WordSiegePracticeBoard.kt").forEach { name ->
            val screen = source(name)
            assertTrue("$name must show the mascot", screen.contains("WordSiegeMascotCompanion("))
            assertFalse("$name must not hide the mascot", screen.contains("if (mascotMoment)"))
        }
    }

    @Test
    fun mascotsInviteAndLoginArePortedIntoTheShell() {
        assertTrue(source("EconomyShopScreen.kt").contains("MascotStoreSection()"))
        assertTrue(source("ProfessionalSocialScreen.kt").contains("InviteFriendsCard("))
        val startup = source("StableV1App.kt")
        assertTrue(startup.contains("ProfessionalUnifiedApp(onSignedOut = { authenticated = false })"))
        assertTrue(startup.contains("restoreMembershipSession(context)"))
        assertTrue(startup.contains("WordSiegeMascotOwnership.restore(context)"))
        assertFalse(startup.contains("PremiumUnifiedProApp("))
    }

    @Test
    fun appUsesTheLightKelimeTahtiPalette() {
        val design = source("GameDesignSystem.kt")
        assertTrue(design.contains("val AppBackground = Color(0xFFEAF6F8)"))
        assertTrue(design.contains("val TextPrimary = Color(0xFF0B1B33)"))
        assertTrue(design.contains("lightColorScheme("))
        assertTrue(source("SonHarfTheme.kt").contains("val IsDark: Boolean get() = false"))
    }

    private fun source(name: String): String =
        listOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .first(File::exists)
            .readText()
}
