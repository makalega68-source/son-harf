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
    fun portedScreensUseTheNavyPalette() {
        listOf("InviteFriends.kt", "RequiredAuthGate.kt", "WordSiegeGameUi.kt", "PremierWordDuelScreen.kt", "MascotStore.kt").forEach { name ->
            val text = source(name)
            listOf("0xFFEAF6F8", "0xFF14B8B0", "0xFF8B6CF0", "0xFFEAF8FF", "0xFF1B0F3B", "0xFF3A1A78").forEach { light ->
                assertFalse("$name still uses $light", text.contains(light))
            }
        }
    }

    private fun source(name: String): String =
        listOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .first(File::exists)
            .readText()
}
