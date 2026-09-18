package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthStartupFlowRegressionTest {
    @Test
    fun languageSelectionFlowsDirectlyIntoLoginForm() {
        val stable = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val auth = File("src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()

        assertTrue(stable.contains("FirstRunLanguageScreen"))
        assertTrue(stable.contains("CompactAuthGate"))
        assertTrue(auth.contains("var register by remember { mutableStateOf(false) }"))
        assertTrue(auth.contains("Text(\"GİRİŞ YAP\""))
        assertTrue(auth.contains("Text(\"ÜYE OL\""))
        assertFalse(auth.contains("showForm"))
        assertFalse(auth.contains("if (!showForm)"))
        assertFalse(auth.contains("Giriş ekranına dön"))
    }
}
