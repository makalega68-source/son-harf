package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveScreenRegressionContractTest {
    private fun src(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun activeRoutesRemainAuthoritative() {
        val portal = src("GamePortalApp.kt")
        val stable = src("StableV1App.kt")
        val monster = src("MonsterExperienceApp.kt")
        val runtime = src("LiveDuelRuntimeShell.kt")
        val online = src("OnlineGameScreenV6.kt")
        val siege = src("WordSiegePanMatch.kt")
        assertTrue(portal.contains("StableV1App"))
        assertTrue(stable.contains("LiveDuelRuntimeShell"))
        assertTrue(runtime.contains("RefinedDuelOverlay()"))
        assertTrue(runtime.contains("MonsterExperienceApp("))
        assertTrue(monster.contains("OnlineGameScreenV6"))
        assertTrue(online.contains("LightDuelArena("))
        assertTrue(siege.contains("WordSiegePracticeScreen("))
        assertFalse(runtime.contains("TargetNeonGameScreen("))
        assertFalse(online.contains("TargetNeonGameScreen("))
    }

    @Test fun practiceHasNoLegacy28msScoreLoop() {
        val practice = src("WordSiegePracticeScreen.kt")
        assertFalse(practice.contains("delay(28)"))
        assertTrue(practice.contains("animateIntAsState"))
    }

    @Test fun practiceViewportContract() {
        val viewport = src("WordSiegeBoardViewport.kt")
        val board = src("WordSiegePracticeBoard.kt")
        assertTrue(viewport.contains("WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.86f"))
        assertTrue(viewport.contains("WORD_SIEGE_PRACTICE_MIN_SCALE = 0.78f"))
        assertTrue(viewport.contains("WORD_SIEGE_PRACTICE_MAX_SCALE = 1.24f"))
        assertTrue(board.contains("WordSiegeBoardViewportMode.FIT"))
        assertTrue(board.contains("detectTransformGestures"))
        assertTrue(board.contains("clampWordSiegeBoardPan"))
        assertTrue(board.contains("clipToBounds"))
        assertFalse(board.contains("SmallFloatingActionButton("))
        assertFalse(board.contains("CenterFocusStrong"))
    }

    @Test fun lastMoveHighlightIsOneShotAndDeduped() {
        val viewport = src("WordSiegeBoardViewport.kt")
        val practice = src("WordSiegePracticeBoard.kt")
        val match = src("WordSiegePanMatch.kt")
        assertTrue(viewport.contains("WORD_SIEGE_LAST_MOVE_ENTER_MS = 180"))
        assertTrue(viewport.contains("WORD_SIEGE_LAST_MOVE_HOLD_MS = 1_200"))
        assertTrue(viewport.contains("WORD_SIEGE_LAST_MOVE_EXIT_MS = 200"))
        assertTrue(practice.contains("consumedHighlightKey"))
        assertTrue(match.contains("observedMoveId"))
        assertFalse(practice.contains("infiniteRepeatable"))
    }

    @Test fun exactTurkishKeyboardAndActionsRemain() {
        val classic = src("LightDuelUi.kt")
        val expectedRows = listOf(
            "listOf(\"Q\", \"W\", \"E\", \"R\", \"T\", \"Y\", \"U\", \"I\", \"O\", \"P\", \"Ğ\", \"Ü\")",
            "listOf(\"A\", \"S\", \"D\", \"F\", \"G\", \"H\", \"J\", \"K\", \"L\", \"Ş\", \"İ\")",
            "listOf(\"Z\", \"X\", \"C\", \"V\", \"B\", \"N\", \"M\", \"Ö\", \"Ç\")",
        )
        expectedRows.forEach { assertTrue(classic.contains(it)) }
        assertTrue(classic.contains("sh(\"SİL\", \"DELETE\")"))
        assertTrue(classic.contains("sh(\"TEMİZLE\", \"CLEAR\")"))
        assertTrue(classic.contains("sh(\"GÖNDER\", \"SEND\")"))
        assertTrue(classic.contains("● SOHBET"))
        assertTrue(classic.contains("★ BONUS"))
        assertTrue(classic.contains("Icons.Rounded.MoreVert"))
        assertTrue(classic.contains("DropdownMenu("))
        assertFalse(classic.contains("BasicTextField"))
    }

    @Test fun typographyContractTargetsArePresent() {
        val classic = src("LightDuelUi.kt")
        val practice = src("WordSiegePracticeScreen.kt")
        assertTrue(classic.contains("fontSize = 15.sp"))
        assertTrue(classic.contains("fontSize = 9.sp"))
        assertTrue(classic.contains("fontSize = if (value.isBlank()) 16.sp else 20.sp"))
        assertTrue(practice.contains("size = 46.dp"))
        assertTrue(practice.contains("fontSize = 24.sp"))
        assertTrue(practice.contains("fontSize = 14.sp"))
        assertTrue(practice.contains("fontSize = 10.sp"))
        assertTrue(practice.contains("height(if (compact) 70.dp else 78.dp)"))
    }

    @Test fun scoringStillUsesTwoPointTransferWithoutWordRollback() {
        assertEquals(2, WordSiegeFinalRules.CUBE_TRANSFER_POINTS)
        assertEquals(24, WordSiegeFinalRules.netScore(wordScore = 20, earnedCubePoints = 4, opponentEarnedCubePoints = 99))
        assertEquals(16, WordSiegeFinalRules.currentTerritoryScore(wordScore = 10, ownedCubes = 3))
    }
}
