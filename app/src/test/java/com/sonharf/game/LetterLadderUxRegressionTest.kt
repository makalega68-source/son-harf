package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterLadderUxRegressionTest {
    @Test
    fun harfYoluStaysFixedAlignedAndUsesReliableHinting() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(source.contains("verticalScroll("))
        assertFalse(source.contains("rememberScrollState"))
        assertFalse(source.contains("number: Int"))
        assertTrue(source.contains("completionPath("))
        assertTrue(source.contains("viableNextMoveIndices("))
        assertTrue(source.contains("Bu hamle çıkmaza götürüyor"))
        assertTrue(source.contains("turuncu işaretli kutudaki harfi değiştir"))
        assertTrue(source.contains("compact = true"))
        assertTrue(source.contains("keySound = { SonHarfSoundFx.puzzleKey() }"))
    }

    @Test
    fun harfYoluHintIsSingleUseAndRevealsOnlyThePosition() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(source.contains("next.uppercase(locale)"))
        assertFalse(source.contains("val from = current[changed]"))
        assertFalse(source.contains("val to = next[changed]"))
        assertTrue(source.contains("var hintUsed by remember { mutableStateOf(false) }"))
        assertTrue(source.contains("enabled = !hintUsed"))
        assertTrue(source.contains("hintedIndex = hintIndex"))
        assertTrue(source.contains("İPUCU 1/1"))
        assertTrue(source.contains("turuncu işaretli kutudaki harfi değiştir"))
        assertFalse(source.contains("sarı işaretli"))
    }

    @Test
    fun harfYoluShowsOnlyFourIntermediateCubeRows() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertTrue(source.contains("for (move in 1 until LetterLadderEngine.MOVE_COUNT)"))
        assertFalse(source.contains("for (move in 1..LetterLadderEngine.MOVE_COUNT)"))
        assertTrue(source.contains("path.size == LetterLadderEngine.MOVE_COUNT + 1"))
    }

    @Test
    fun harfYoluShowsLiveKeyboardInputInTheNextPlayableRow() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/HarfYoluKeyboard.kt").readText()

        assertTrue(source.contains("activeInput = input.uppercase(locale).takeIf { isActiveEntry }"))
        assertTrue(source.contains("activeInput?.padEnd(LetterLadderEngine.WORD_LENGTH, ' ')"))
        assertTrue(keyboard.contains("onValueChange((value + key).take(maxLength))"))
    }

    @Test
    fun harfYoluRemovesResetAndShowsNewGameOnlyAfterCompletion() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(source.contains("SIFIRLA"))
        assertFalse(source.contains("RESET"))
        assertFalse(source.contains("fun resetCurrent()"))
        assertTrue(source.contains("YENİ OYUN"))
        assertTrue(source.contains("NEW GAME"))
        assertTrue(source.contains("if (!completed)"))
        assertTrue(source.contains("} else {\n                    Button("))
    }

    @Test
    fun harfYoluUsesItsOwnProfessionalDynamicBackdrop() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val backdrop = projectFile("app/src/main/java/com/sonharf/game/HarfYoluBackdrop.kt").readText()

        assertTrue(source.contains("HarfYoluBackdrop(Modifier.fillMaxSize())"))
        assertFalse(source.contains("FirstRunLanguageBackdrop(Modifier.fillMaxSize())"))
        assertTrue(backdrop.contains("rememberInfiniteTransition"))
        assertTrue(backdrop.contains("GameColors.AppBackground"))
        assertTrue(backdrop.contains("GameColors.PrimaryBlue"))
        assertTrue(backdrop.contains("GameColors.TacticalTurquoise"))
        assertTrue(backdrop.contains("GameColors.RewardAmber"))
        assertTrue(backdrop.contains("GameColors.Lavender"))
    }

    @Test
    fun harfYoluUsesIsolatedProfessionalKeyboardAndQuietDedicatedFeedback() {
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/HarfYoluKeyboard.kt").readText()
        val sound = projectFile("app/src/main/java/com/sonharf/game/SonHarfSoundFx.kt").readText()

        assertTrue(keyboard.contains("Harf Yolu keyboard aligned with the professional app palette"))
        assertTrue(keyboard.contains("HarfYoluKeyboardUi"))
        assertFalse(keyboard.contains("SonHarfCosmetics.keyboardPalette"))
        assertTrue(keyboard.contains("GameColors.ElevatedBackground"))
        assertTrue(keyboard.contains("GameColors.PrimarySurface"))
        assertTrue(keyboard.contains("GameColors.PlayGreen"))
        assertTrue(keyboard.contains("GameColors.Lavender"))
        assertTrue(keyboard.contains("keySound()"))
        assertTrue(keyboard.contains("actionSound()"))
        assertTrue(keyboard.contains("34.dp"))
        assertTrue(sound.contains("fun puzzleKey()"))
        assertTrue(sound.contains("fun puzzleError()"))
        assertTrue(sound.contains("fun puzzleHint()"))
    }

    @Test
    fun harfYoluKeyboardActionsFollowSelectedLanguageWithoutSystemExtras() {
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/HarfYoluKeyboard.kt").readText()

        assertTrue(keyboard.contains("val isEnglish = language.equals(\"en\", ignoreCase = true)"))
        assertTrue(keyboard.contains("label = \"⌫\""))
        assertTrue(keyboard.contains("label = if (isEnglish) \"SEND\" else \"GÖNDER\""))
        assertFalse(keyboard.contains("TEMİZLE"))
        assertFalse(keyboard.contains("CLEAR"))
        assertFalse(keyboard.contains("?123"))
        assertFalse(keyboard.contains("GIF"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
