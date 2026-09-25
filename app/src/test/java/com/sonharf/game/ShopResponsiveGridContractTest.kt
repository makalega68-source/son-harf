package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopResponsiveGridContractTest {
    @Test
    fun `shop uses one column on narrow phones and two columns on production widths`() {
        val source = repoFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        assertTrue(source.contains("LocalConfiguration.current.screenWidthDp >= 380"))
        assertTrue(source.contains("filtered.chunked(if (twoColumnProducts) 2 else 1)"))
        assertTrue(source.contains("compact = twoColumnProducts"))
        assertTrue(source.contains("StoreProductPreview(item, Modifier.fillMaxWidth().height(92.dp))"))
        assertTrue(source.contains("if (twoColumnProducts && group.size == 1) Spacer(Modifier.weight(1f))"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
