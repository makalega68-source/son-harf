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
        )
        val strong = WordSiegePracticeEngine.botTargetPercentile(
            neutral,
            playerRating = 1500,
            playerWins = 20,
            playerLosses = 5,
        )

        assertTrue(beginner < strong)
        assertTrue(beginner <= 40)
        assertTrue(strong <= 90)
    }

    @Test fun botEasesOffWhenAheadAndPushesMoreWhenBehind() {
        val balanced = state(moveCount = 6)
        val ahead = state(moveCount = 6, playerWordScore = 0, botWordScore = 20)
        val behind = state(moveCount = 6, playerWordScore = 20, botWordScore = 0)

        val base = WordSiegePracticeEngine.botTargetPercentile(balanced, 1100, 8, 8)
        val whenAhead = WordSiegePracticeEngine.botTargetPercentile(ahead, 1100, 8, 8)
        val whenBehind = WordSiegePracticeEngine.botTargetPercentile(behind, 1100, 8, 8)

        assertTrue(whenAhead < base)
        assertTrue(whenBehind > base)
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
