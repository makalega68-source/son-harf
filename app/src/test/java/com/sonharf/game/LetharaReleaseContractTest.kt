package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LetharaReleaseContractTest {
    @Test
    fun mainActivityUsesResizeForAndroidIme() {
        val manifest = sourceFile(
            "src/main/AndroidManifest.xml",
            "app/src/main/AndroidManifest.xml",
        ).readText()

        assertTrue(
            "MainActivity must use adjustResize so the companion input remains visible above IME",
            manifest.contains("android:windowSoftInputMode=\"adjustResize\""),
        )
        assertFalse(
            "Production MainActivity must not return to adjustNothing",
            manifest.contains("android:windowSoftInputMode=\"adjustNothing\""),
        )
    }

    @Test
    fun productionCompanionChatKeepsImeInsetsAndSendAction() {
        val source = sourceFile(
            "src/main/java/com/sonharf/game/MascotCompanionScreen.kt",
            "app/src/main/java/com/sonharf/game/MascotCompanionScreen.kt",
        ).readText()

        assertTrue(source.contains("Modifier.fillMaxWidth().imePadding().padding(8.dp)"))
        assertTrue(source.contains("imeAction = ImeAction.Send"))
        assertTrue(source.contains("showKeyboardOnFocus = true"))
        assertTrue(source.contains("keyboardActions = KeyboardActions"))
        assertTrue(source.contains("onSend = { if (input.isNotBlank() && !sending) send() }"))
    }

    @Test
    fun compatibilityPreviewCannotRouteBackToLegacyEve() {
        val source = sourceFile(
            "src/main/java/com/sonharf/game/MascotCardPreview.kt",
            "app/src/main/java/com/sonharf/game/MascotCardPreview.kt",
        ).readText()

        assertTrue(source.contains("MascotLive3DStage"))
        assertTrue(source.contains("MascotSelectionRuntime.selectedId"))
        assertFalse(source.contains("Eve3DStage("))
    }

    private fun sourceFile(vararg candidates: String): File =
        candidates.asSequence()
            .map(::File)
            .firstOrNull(File::isFile)
            ?: error("Required release-contract source file is missing: ${candidates.joinToString()}")
}
