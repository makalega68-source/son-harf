from pathlib import Path

path = Path("app/src/test/java/com/sonharf/game/SonHarfResponsiveLayoutContractTest.kt")
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonHarfResponsiveLayoutContractTest {
    @Test
    fun `son harf arena adapts to narrow phones while keeping native ime`() {
        val screen = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()

        assertTrue(screen.contains("val narrowWidth = maxWidth < 390.dp"))
        assertTrue(screen.contains("val compact = compactHeight || narrowWidth"))
        assertTrue(screen.contains("val horizontalPadding = if (narrowWidth) 8.dp else 12.dp"))
        assertTrue(screen.contains("val panelMinHeight = if (compact) 118.dp else 150.dp"))
        assertTrue(screen.contains("compactHeight && narrowWidth -> 82.dp"))
        assertTrue(screen.contains("modifier = Modifier.width(if (narrow) 88.dp else 108.dp)"))
        assertTrue(screen.contains("width = if (compact) 38.dp else 46.dp"))
        assertTrue(screen.contains("height = if (compact) 34.dp else 40.dp"))
        assertTrue(screen.contains("minHeight: Dp = 150.dp"))
        assertTrue(screen.contains("modifier = modifier.heightIn(min = minHeight)"))

        // The real Android IME remains the gameplay keyboard; do not reintroduce a baked custom keyboard.
        assertTrue(screen.contains("LocalSoftwareKeyboardController.current"))
        assertTrue(screen.contains("focusRequester.requestFocus()"))
        assertTrue(screen.contains("keyboardController?.show()"))
        assertTrue(screen.contains(".imePadding()"))
        assertTrue(screen.contains("ImeAction.Done"))
        assertFalse(screen.contains("val palette = SonHarfCosmetics.keyboardPalette"))

        // Player cards must not reintroduce a level/XP surface.
        assertFalse(screen.contains("Seviye"))
        assertFalse(screen.contains("Level "))
        assertFalse(screen.contains(" XP"))
    }
}
''', encoding="utf-8")
print("Son Harf responsive contract written")
