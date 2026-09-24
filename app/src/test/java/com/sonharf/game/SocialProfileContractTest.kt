package com.sonharf.game

import com.sonharf.game.data.FriendshipDto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialProfileContractTest {
    private fun row(a: String, b: String, status: String, by: String) =
        FriendshipDto(userId = minOf(a, b), friendId = maxOf(a, b), status = status, requestedBy = by, createdAt = "")

    @Test
    fun `relation comes from the friendship rows`() {
        assertEquals(PlayerRelation.SELF, playerRelation("me", "me", emptyList()))
        assertEquals(PlayerRelation.NONE, playerRelation("me", "x", emptyList()))
        assertEquals(PlayerRelation.FRIEND, playerRelation("me", "x", listOf(row("me", "x", "accepted", "x"))))
        assertEquals(PlayerRelation.REQUEST_SENT, playerRelation("me", "x", listOf(row("me", "x", "pending", "me"))))
        assertEquals(PlayerRelation.REQUEST_RECEIVED, playerRelation("me", "x", listOf(row("me", "x", "pending", "x"))))
        assertEquals(PlayerRelation.NONE, playerRelation(null, "x", listOf(row("me", "x", "accepted", "x"))))
    }

    @Test
    fun `direct messages group into threads, newest first`() {
        val messages = listOf(
            DirectMessageDto(1, "me", "a", "selam", ""),
            DirectMessageDto(2, "a", "me", "merhaba", ""),
            DirectMessageDto(3, "b", "me", "maç?", ""),
            DirectMessageDto(4, "x", "y", "başkası", ""),
        )
        val threads = directConversations("me", messages)
        assertEquals(listOf("b", "a"), threads.map { it.friendId })
        assertEquals("merhaba", threads[1].lastBody)
        assertEquals(false, threads[1].lastFromMe)
    }

    @Test
    fun `social has four tabs and the profile uses server-side social actions`() {
        val social = source("ProfessionalSocialScreen.kt")
        val sheet = source("PlayerProfileSheet.kt")
        assertTrue(social.contains("private enum class ProfessionalSocialTab { FRIENDS, INVITES, MESSAGES, RIVALS }"))
        assertTrue(social.contains("PlayerProfileSheet("))
        assertTrue(social.contains("DirectMessageSheet("))
        assertTrue(sheet.contains("backend.sendFriendRequest(playerId)"))
        assertTrue(sheet.contains("backend.inviteFriendToWordSiege(playerId"))
        assertTrue(sheet.contains("backend.blockUser(playerId)"))
        assertTrue(sheet.contains("backend.reportPlayer(playerId, reason)"))
        assertTrue(sheet.contains("\"report_player\""))
        // No level or invented statistics on another player's profile.
        assertTrue(!sheet.contains("Seviye"))
    }

    private fun source(name: String): String =
        sequenceOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Missing source file: $name")
}
