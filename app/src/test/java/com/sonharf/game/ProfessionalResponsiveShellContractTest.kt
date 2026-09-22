package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalResponsiveShellContractTest {
    @Test
    fun `bottom navigation and segmented tabs stay compact on narrow phones`() {
        val navigation = source("GameNavigationComponents.kt")

        assertTrue(navigation.contains("navigationBarsPadding().height(62.dp)"))
        assertTrue(navigation.contains("modifier = Modifier.size(23.dp)"))
        assertTrue(navigation.contains("fontSize = 10.sp"))
        assertTrue(navigation.contains("if (labels.size <= 3)"))
        assertTrue(navigation.contains("horizontalScroll(rememberScrollState())"))
        assertTrue(navigation.contains("Modifier.widthIn(min = 92.dp)"))
        assertFalse(navigation.contains("height(80.dp)"))
        assertFalse(navigation.contains("Modifier.size(50.dp)"))
    }

    @Test
    fun `scrollable meta screens protect short 360 by 800 layouts`() {
        val requiredLazyScreens = listOf(
            "ProfessionalHomeScreen.kt",
            "ProfessionalProfileScreen.kt",
            "ProfessionalSocialScreen.kt",
            "EconomyShopScreen.kt",
            "ProfessionalRetentionScreen.kt",
            "ProfessionalLeaderboardScreen.kt",
        )
        requiredLazyScreens.forEach { name ->
            val screen = source(name)
            assertTrue("$name must use lazy scrolling for short phones", screen.contains("LazyColumn("))
            assertTrue("$name must fill available viewport", screen.contains("fillMaxSize()"))
        }

        val home = source("ProfessionalHomeScreen.kt")
        assertTrue(home.contains("widthIn(max = 620.dp)"))
        assertTrue(home.contains("GameSpacing.ScreenHorizontal"))

        val recovery = source("PasswordRecoveryScreen.kt")
        assertTrue(recovery.contains("verticalScroll(rememberScrollState())"))
        assertTrue(recovery.contains(".imePadding()"))
        assertTrue(recovery.contains(".navigationBarsPadding()"))
    }

    @Test
    fun `gameplay surfaces keep board and custom input priorities`() {
        val harfYolu = source("LetterLadderGame.kt")
        val harfYoluKeyboard = source("HarfYoluKeyboard.kt")
        val siege = source("WordSiegePanMatch.kt")
        val duel = source("PremierWordDuelScreen.kt")

        assertTrue(harfYolu.contains("Modifier.weight(1f)"))
        assertTrue(harfYolu.contains("compact = true"))
        assertFalse(harfYolu.contains("verticalScroll("))
        assertTrue(harfYoluKeyboard.contains("Modifier.fillMaxWidth()"))
        assertTrue(harfYoluKeyboard.contains("height = keyHeight"))

        assertTrue(siege.contains("WordSiegeBoard"))
        assertTrue(duel.contains("Premier"))
        assertFalse(duel.contains("?123"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
