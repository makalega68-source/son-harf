package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopButtonStateContractTest {

    @Test
    fun shopSerializesPurchaseActionsAndKeepsRealOwnershipStateVisible() {
        val source = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        assertTrue(source.contains("var busy by remember { mutableStateOf<String?>(null) }"))
        assertTrue(source.contains("if (b == null || busy != null) return@VerifiedStoreProductCard"))
        assertTrue(source.contains("busy = product.id"))
        assertTrue(source.contains("busy = null"))
        assertTrue(source.contains("enabled = !busy && !equipped && !lockedByPro"))
        assertTrue(source.contains("equipped -> gameText(\"AKTİF\", \"ACTIVE\")"))
        assertTrue(source.contains("owned -> gameText(\"SAHİPSİN\", \"OWNED\")"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
