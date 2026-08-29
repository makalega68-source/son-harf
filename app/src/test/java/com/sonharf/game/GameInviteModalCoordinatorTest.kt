package com.sonharf.game

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GameInviteModalCoordinatorTest {

    @Before
    fun resetBefore() {
        resetCoordinator()
    }

    @After
    fun resetAfter() {
        resetCoordinator()
    }

    @Test
    fun activeDialogIsNotPreempted() {
        GameInviteModalCoordinator.setPending(GameInviteModalKind.TEAM_ARENA, true)
        assertEquals(GameInviteModalKind.TEAM_ARENA, GameInviteModalCoordinator.activeKind)

        GameInviteModalCoordinator.setPending(GameInviteModalKind.CLASSIC, true)
        GameInviteModalCoordinator.setPending(GameInviteModalKind.WORD_ARENA, true)

        assertEquals(GameInviteModalKind.TEAM_ARENA, GameInviteModalCoordinator.activeKind)
    }

    @Test
    fun nextDialogUsesPriorityAfterActiveCompletes() {
        GameInviteModalCoordinator.setPending(GameInviteModalKind.TEAM_ARENA, true)
        GameInviteModalCoordinator.setPending(GameInviteModalKind.WORD_ARENA, true)
        GameInviteModalCoordinator.setPending(GameInviteModalKind.CLASSIC, true)

        GameInviteModalCoordinator.setPending(GameInviteModalKind.TEAM_ARENA, false)
        assertEquals(GameInviteModalKind.CLASSIC, GameInviteModalCoordinator.activeKind)

        GameInviteModalCoordinator.setPending(GameInviteModalKind.CLASSIC, false)
        assertEquals(GameInviteModalKind.WORD_ARENA, GameInviteModalCoordinator.activeKind)

        GameInviteModalCoordinator.setPending(GameInviteModalKind.WORD_ARENA, false)
        assertNull(GameInviteModalCoordinator.activeKind)
    }

    private fun resetCoordinator() {
        GameInviteModalKind.entries.forEach { GameInviteModalCoordinator.clear(it) }
    }
}
