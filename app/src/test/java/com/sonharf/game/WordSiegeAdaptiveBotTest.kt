package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeAdaptiveBotTest {
    @Test fun newPlayersGetSofterBotThanExperiencedStrongPlayers() {
        val neutral = state(moveCount = 6)
        val beginner = WordSiegePracticeEngine.botTargetPercentile(
            neutral,
            playerRating = 1000,
            playerWins = 0,
            playerLosses = 0,
            aiWonLast = null,
        )
        val strong = WordSiegePracticeEngine.botTargetPercentile(
            neutral,
            playerRating = 1500,
            playerWins = 20,
            playerLosses = 5,
            aiWonLast = null,
        )

        assertTrue(beginner < strong)
        assertTrue(beginner <= 45)
        assertTrue(strong <= 92)
    }

    @Test fun aiKeepsTheScoreCloseEasingOffAheadAndPushingBehind() {
        val balanced = state(moveCount = 6)
        val ahead = state(moveCount = 6, playerWordScore = 0, botWordScore = 20)
        val behind = state(moveCount = 6, playerWordScore = 20, botWordScore = 0)

        val base = WordSiegePracticeEngine.botTargetPercentile(balanced, 1100, 8, 8, aiWonLast = null)
        val whenAhead = WordSiegePracticeEngine.botTargetPercentile(ahead, 1100, 8, 8, aiWonLast = null)
        val whenBehind = WordSiegePracticeEngine.botTargetPercentile(behind, 1100, 8, 8, aiWonLast = null)

        assertTrue(whenAhead < base)
        assertTrue(whenBehind > base)
    }

    @Test fun afterAnAiWinThePlayerGetsTheNextOne() {
        val game = state(moveCount = 6)
        val afterAiWin = WordSiegePracticeEngine.botTargetPercentile(game, 1100, 8, 8, aiWonLast = true)
        val afterPlayerWin = WordSiegePracticeEngine.botTargetPercentile(game, 1100, 8, 8, aiWonLast = false)
        val fresh = WordSiegePracticeEngine.botTargetPercentile(game, 1100, 8, 8, aiWonLast = null)
        assertTrue(afterAiWin < fresh)
        assertTrue(afterPlayerWin > fresh)
    }

    @Test fun adaptiveChoiceVariesInsideTheRequestedSkillBand() {
        val choices = (1L..30L).map {
            WordSiegePracticeEngine.adaptiveCandidateIndex(60, 50, it)
        }.toSet()

        assertTrue("AI should vary between similarly strong legal moves", choices.size > 1)
        assertTrue(choices.all { it in 24..34 })
    }

    private fun state(
        moveCount: Int,
        playerWordScore: Int = 0,
        botWordScore: Int = 0,
    ) = WordSiegePracticeState(
        board = List(81) { WordSiegeCellDto() },
        bag = "ELMALİSTEKARTONUR",
        playerRack = "KALEMTR",
        botRack = "MASASİN",
        currentOwner = 2,
        moveCount = moveCount,
        playerWordScore = playerWordScore,
        botWordScore = botWordScore,
    )
}
