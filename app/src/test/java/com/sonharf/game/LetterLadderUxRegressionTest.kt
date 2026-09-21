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
        val sharedKeyboard = projectFile("app/src/main/java/com/sonharf/game/SharedInputPrimitives.kt").readText()
        val compatibilityWrapper = projectFile("app/src/main/java/com/sonharf/game/HarfYoluKeyboard.kt").readText()
        assertTrue(source.contains("activeInput = input.uppercase(locale).takeIf { isActiveEntry }"))
        assertTrue(source.contains("activeInput?.padEnd(LetterLadderEngine.WORD_LENGTH, ' ')"))
        assertTrue(sharedKeyboard.contains("onValueChange((value + key).take(maxLength))"))
        assertTrue(compatibilityWrapper.contains("EmbeddedWordKeyboard("))
        assertFalse(sharedKeyboard.contains("TEMİZLE"))
        assertFalse(sharedKeyboard.contains("CLEAR"))
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
        assertTrue(source.contains("} else {\n                    PurchasedButton("))
        assertTrue(source.contains("style = PurchasedButtonStyle.PURPLE"))
        assertTrue(source.contains("leadingAsset = PurchasedUiAsset.ICON_REPEAT"))
    }

    @Test
    fun harfYoluUsesStaticSiegeRoyalePuzzleBackdrop() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val backdrop = projectFile("app/src/main/java/com/sonharf/game/HarfYoluBackdrop.kt").readText()
        assertTrue(source.contains("HarfYoluBackdrop(Modifier.fillMaxSize())"))
        assertFalse(source.contains("FirstRunLanguageBackdrop(Modifier.fillMaxSize())"))
        assertFalse(backdrop.contains("rememberInfiniteTransition"))
        assertTrue(backdrop.contains("SonHarfTheme.Primary"))
        assertTrue(backdrop.contains("SonHarfTheme.Turquoise"))
        assertTrue(backdrop.contains("SonHarfTheme.Purple"))
        assertTrue(backdrop.contains("SonHarfTheme.ActionOrange"))
        assertTrue(backdrop.contains("0xFFE2EEFF"))
        assertTrue(backdrop.contains("0xFFEAF3FF"))
        assertFalse(backdrop.contains("0xFF365F53"))
        assertFalse(backdrop.contains("0xFF718693"))
        assertFalse(backdrop.contains("0xFFAD6A57"))
        assertFalse(backdrop.contains("0xFFF4F2EC"))
    }

    @Test
    fun harfYoluKeyboardUsesSharedMinimalRendererAndQuietDedicatedFeedback() {
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/HarfYoluKeyboard.kt").readText()
        val shared = projectFile("app/src/main/java/com/sonharf/game/SharedInputPrimitives.kt").readText()
        val sound = projectFile("app/src/main/java/com/sonharf/game/SonHarfSoundFx.kt").readText()
        assertTrue(keyboard.contains("Rendering is delegated to the shared game keyboard"))
        assertTrue(keyboard.contains("submitLabel = sh(\"ONAYLA\", \"CONFIRM\")"))
        assertTrue(keyboard.contains("33.dp"))
        assertTrue(keyboard.contains("keySound = keySound"))
        assertTrue(keyboard.contains("actionSound = actionSound"))
        assertTrue(shared.contains("PurchasedPanel("))
        assertTrue(shared.contains("PurchasedUiAsset.BUTTON_BLUE"))
        assertTrue(shared.contains("PurchasedUiAsset.BUTTON_GREEN"))
        assertTrue(shared.contains("val palette = SonHarfCosmetics.keyboardPalette"))
        assertTrue(shared.contains("overlayColor = palette.key"))
        assertTrue(shared.contains("keySound()"))
        assertTrue(shared.contains("actionSound()"))
        assertFalse(shared.contains("TEMİZLE"))
        assertFalse(shared.contains("CLEAR"))
        assertTrue(sound.contains("fun puzzleKey()"))
        assertTrue(sound.contains("fun puzzleError()"))
        assertTrue(sound.contains("fun puzzleHint()"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
