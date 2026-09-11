package com.sonharf.game

import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeZoneExplanationTest {
    @Test fun crownAndRewardExplainActualScoringRules() {
        val crown = wordSiegePracticeZoneExplanation(WordSiegeBoardSpec.CenterBonus, turkish = true)
        val reward = wordSiegePracticeZoneExplanation(WordSiegeBoardSpec.StarBonus, turkish = true)

        assertTrue(crown.contains("İlk kelime"))
        assertTrue(crown.contains("4 katına"))
        assertTrue(reward.contains("+${WordSiegeBoardSpec.StarBonusPoints}"))
    }
}
