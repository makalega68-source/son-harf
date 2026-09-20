package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterLadderUxRegressionTest {
    @Test
    fun harfYoluKeepsFiveMoveEngineButShowsOnlyFourIntermediateRows() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertTrue(source.contains("const val MOVE_COUNT = 5"))
        assertTrue(source.contains("for (move in 1 until LetterLadderEngine.MOVE_COUNT)"))
        assertFalse(source.contains("for (move in 1..LetterLadderEngine.MOVE_COUNT)"))
        assertTrue(source.contains("path.size == LetterLadderEngine.MOVE_COUNT + 1"))
        assertTrue(source.contains("sh(\"HEDEF\", \"TARGET\")"))
        assertFalse(source.contains("verticalScroll("))
        assertFalse(source.contains("rememberScrollState"))
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
        assertTrue(source.contains("işaretli kutudaki harfi değiştir"))
    }

    @Test
    fun harfYoluShowsLiveInputAndRetainsUndoDictionaryAndDeadEndGuards() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertTrue(source.contains("activeInput = input.uppercase(locale).takeIf { isActiveEntry }"))
        assertTrue(source.contains("activeInput?.padEnd(LetterLadderEngine.WORD_LENGTH, ' ')"))
        assertTrue(source.contains("completionPath("))
        assertTrue(source.contains("viableNextMoveIndices("))
        assertTrue(source.contains("POSITION_ALREADY_USED"))
        assertTrue(source.contains("NOT_DICTIONARY"))
        assertTrue(source.contains("GERİ AL"))
        assertTrue(source.contains("UNDO"))
    }

    @Test
    fun sharedWordKeyboardIsAndroidLikeAndContainsOnlyGameRelevantActions() {
        val keyboardFile = projectFile("app/src/main/java/com/sonharf/game/SharedInputPrimitives.kt").readText()
        val wordKeyboard = keyboardFile.substringBefore("internal fun EmbeddedNumberKeyboard")

        assertTrue(wordKeyboard.contains("listOf(\"Q\",\"W\",\"E\",\"R\",\"T\",\"Y\",\"U\",\"I\",\"O\",\"P\",\"Ğ\",\"Ü\")"))
        assertTrue(wordKeyboard.contains("listOf(\"A\",\"S\",\"D\",\"F\",\"G\",\"H\",\"J\",\"K\",\"L\",\"Ş\",\"İ\")"))
        assertTrue(wordKeyboard.contains("listOf(\"Z\",\"X\",\"C\",\"V\",\"B\",\"N\",\"M\",\"Ö\",\"Ç\")"))
        assertTrue(wordKeyboard.contains("label = \"⌫\""))
        assertTrue(wordKeyboard.contains("sh(\"ONAYLA\", \"CONFIRM\")"))
        assertTrue(wordKeyboard.contains("sh(\"GÖNDER\", \"SEND\")"))
        assertFalse(wordKeyboard.contains("label = \"TEMİZLE\""))
        assertFalse(wordKeyboard.contains("label = \"CLEAR\""))
        assertFalse(wordKeyboard.contains("label = \",\""))
        assertFalse(wordKeyboard.contains("label = \"?123\""))
    }

    @Test
    fun harfYoluUsesCalmStaticBackdropAndDedicatedFeedback() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val backdrop = projectFile("app/src/main/java/com/sonharf/game/HarfYoluBackdrop.kt").readText()
        val sound = projectFile("app/src/main/java/com/sonharf/game/SonHarfSoundFx.kt").readText()

        assertTrue(source.contains("HarfYoluBackdrop(Modifier.fillMaxSize())"))
        assertFalse(backdrop.contains("rememberInfiniteTransition"))
        assertTrue(backdrop.contains("0xFFF6F4EE"))
        assertTrue(backdrop.contains("0xFF285943"))
        assertTrue(backdrop.contains("0xFF77977F"))
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
