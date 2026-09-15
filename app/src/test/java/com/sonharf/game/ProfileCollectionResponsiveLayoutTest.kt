package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCollectionResponsiveLayoutTest {
    @Test
    fun ownedStyleCardsStayInsideThePhoneViewport() {
        val source = projectFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()

        assertFalse(source.contains("LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp))"))
        assertFalse(source.contains("modifier = Modifier.width(248.dp)"))
        assertTrue(source.contains("styles.forEach { item ->"))
        assertTrue(source.contains("modifier = Modifier.fillMaxWidth(),\n                    onEquip = { equipStyle(item.id) }"))
        assertTrue(source.contains("modifier = Modifier.fillMaxWidth().padding(14.dp)"))
        assertTrue(source.contains("PurchasedProfileFrameOverlay(frameId = item.id, modifier = Modifier.size(82.dp))"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
