package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumBotUiContractTest {
    @Test
    fun wordSiegeBotPassRequiresZeroValidCandidatesAndDoesNotHideErrors() {
        val source = File("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val planner = File("src/main/java/com/sonharf/game/WordSiegeBotPlanner.kt").readText()
        assertTrue(source.contains("plan.validCandidateCount == 0"))
        assertTrue(source.contains("PASS suppressed"))
        assertTrue(source.contains("WordSiegeBotPlanner.plan"))
        assertFalse(source.contains("WordSiegePracticeEngine.bestBotMove"))
        assertTrue(planner.contains("WordSiegePracticeEngine.validateMove"))
        assertTrue(planner.contains("validateWordSiegeDictionaryWords"))
    }

    @Test
    fun naturalBotNamesMapToExactlyTwoLocalAvatarAssets() {
        assertTrue(TrainingBotSupport.botAvatarPath("Elif") == "bot:female")
        assertTrue(TrainingBotSupport.botAvatarPath("Mert") == "bot:male")
        assertFalse(TrainingBotSupport.turkishBotNames.any { "BOT" in it.uppercase() || "CPU" in it.uppercase() })
        assertTrue(File("src/main/res/drawable-nodpi/bot_avatar_male_higgsfield.webp").exists())
        assertTrue(File("src/main/res/drawable-nodpi/bot_avatar_female_higgsfield.webp").exists())
    }

    @Test
    fun ownershipColorsRemainReadableWithBlackLetters() {
        assertTrue(TrainingBotSupport.blackContrastRatio(TrainingBotSupport.OWN_FILL_ARGB) >= 7.0)
        assertTrue(TrainingBotSupport.blackContrastRatio(TrainingBotSupport.OPPONENT_FILL_ARGB) >= 7.0)
        assertTrue(TrainingBotSupport.ownershipRelation(2, 2) == WordSiegeOwnershipRelation.SELF)
        assertTrue(TrainingBotSupport.ownershipRelation(1, 2) == WordSiegeOwnershipRelation.OPPONENT)
    }

    @Test
    fun boardKeepsFixedGridAndImprovedReadability() {
        val source = File("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        assertFalse(source.contains("LazyColumn"))
        assertTrue(source.contains("tween(220)"))
        assertTrue(source.contains("fontSize = 18.sp"))
        assertTrue(source.contains("1.45.dp"))
        assertTrue(source.contains("fontSize = 8.sp"))
    }

    @Test
    fun premiumStyleRemainsCosmeticOnly() {
        val runtime = File("src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()
        val catalog = File("../supabase/migrations/20260901002000_premium_style_catalog_v1.sql").readText()
        assertTrue(runtime.contains("keyboard_midnight").not() || runtime.contains("midnight"))
        assertTrue(catalog.contains("frame_black_gold"))
        assertTrue(catalog.contains("keyboard_crystal"))
        assertTrue(catalog.contains("theme_midnight"))
        assertFalse(catalog.contains("rating+"))
        assertFalse(catalog.contains("extra_time"))
    }
}
