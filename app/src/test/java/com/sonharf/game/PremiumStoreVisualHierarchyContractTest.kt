package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumStoreVisualHierarchyContractTest {
    @Test
    fun `premium product artwork and typography stay readable`() {
        val store = repoFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()

        assertTrue(store.contains("Modifier.size(78.dp)"))
        assertTrue(store.contains("heightIn(min = 104.dp)"))
        assertTrue(store.contains("fontSize = 16.sp, lineHeight = 19.sp"))
        assertTrue(store.contains("fontSize = 11.sp, lineHeight = 15.sp"))
        assertTrue(store.contains("defaultMinSize(minWidth = 78.dp, minHeight = 40.dp)"))

        listOf(
            "R.drawable.premium_series_game",
            "R.drawable.premium_letter_table",
            "R.drawable.premium_score_calculator",
            "R.drawable.premium_pro",
        ).forEach { drawable ->
            assertTrue("Missing premium artwork mapping: $drawable", store.contains(drawable))
        }
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
