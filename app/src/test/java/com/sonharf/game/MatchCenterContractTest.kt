package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchCenterContractTest {
    @Test
    fun `match center has active finished and invites tabs on real siege data`() {
        val screen = source("MatchCenterScreen.kt")
        assertTrue(screen.contains("internal enum class MatchCenterTab { ACTIVE, FINISHED, INVITES }"))
        assertTrue(screen.contains("backend.getWordSiegeGames()"))
        assertTrue(screen.contains("backend.getIncomingWordSiegeInvites()"))
        assertTrue(screen.contains("backend.respondWordSiegeInvite(invite.inviteId, accept)"))
        assertTrue(screen.contains("backend.inviteFriendToWordSiege("))
        // Total = word points + territory points, both as the server stores them.
        assertTrue(screen.contains("val myTotal: Int get() = myWordScore + myAreaScore"))
        // Siege games do not change rating on the server, so no rating change is shown.
        assertFalse(screen.contains("ratingDelta"))
    }

    @Test
    fun `home continue card and PLAY open the match center and specific matches`() {
        val shell = source("ProfessionalUnifiedApp.kt")
        val home = source("ProfessionalHomeScreen.kt")
        assertTrue(shell.contains("ProfessionalDestination.MATCHES -> MatchCenterScreen("))
        assertTrue(shell.contains("onContinueMatch = { gameId -> openSiegeMatch(gameId) }"))
        assertTrue(shell.contains("onMyGames = { openMatches() }"))
        assertTrue(shell.contains("initialGameId = siegeGameId"))
        assertTrue(home.contains("HomeContinueCard("))
        assertTrue(home.contains("backend.getWeeklyTournament()"))
        assertTrue(source("ProfessionalWordSiegeExperience.kt").contains("val openedDirectly = initialGameId != null"))
    }

    private fun source(name: String): String =
        sequenceOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Missing source file: $name")
}
