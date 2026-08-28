package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChibiAiContractTest {
    @Test
    fun appUsesChibiChatAndNeverEveChat() {
        val service = sourceFile(
            "src/main/java/com/sonharf/game/MascotAiChatService.kt",
            "app/src/main/java/com/sonharf/game/MascotAiChatService.kt",
        ).readText()
        assertTrue(service.contains("/functions/v1/chibi-chat"))
        assertFalse(service.contains("/functions/v1/eve-chat"))
    }

    @Test
    fun chibiEdgeFunctionUsesFreeTierModelsAndCurrentGameNames() {
        val edge = sourceFile(
            "../supabase/functions/chibi-chat/index.ts",
            "supabase/functions/chibi-chat/index.ts",
        ).readText()
        assertTrue(edge.contains("gemini-3.1-flash-lite"))
        assertTrue(edge.contains("gemini-2.5-flash-lite"))
        assertTrue(edge.contains("Bil Bakalım"))
        assertTrue(edge.contains("Your name is Chibi"))
        assertTrue(edge.contains("consume_chibi_ai_free_quota"))
        assertFalse(edge.contains("Lethara"))
        assertFalse(edge.contains("Neris"))
    }

    @Test
    fun bilBakalimAndArenaStayImeSafe() {
        val bil = sourceFile(
            "src/main/java/com/sonharf/game/BilBakalimExcitementScreen.kt",
            "app/src/main/java/com/sonharf/game/BilBakalimExcitementScreen.kt",
        ).readText()
        val arena = sourceFile(
            "src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
            "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
        ).readText()
        assertTrue(bil.contains(".imePadding()"))
        assertTrue(bil.contains("CEVABI KİLİTLE"))
        assertTrue(arena.contains(".imePadding()"))
        assertTrue(arena.contains("imeVisible"))
        assertTrue(arena.contains("heightIn(min = 60.dp)"))
    }

    private fun sourceFile(vararg candidates: String): File =
        candidates.asSequence().map(::File).firstOrNull(File::isFile)
            ?: error("Required source file is missing: ${candidates.joinToString()}")
}
