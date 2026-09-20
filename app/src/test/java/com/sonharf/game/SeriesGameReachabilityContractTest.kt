package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesGameReachabilityContractTest {
    @Test fun purchasedSeriesGameHasARealRuntimeScreenAndSiegeEntry() {
        val series = projectFile("app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt").readText()
        val entry = projectFile("app/src/main/java/com/sonharf/game/WordSiegeEntryScreen.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val store = projectFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()

        assertTrue(series.contains("internal fun WordSiegeSeriesScreen"))
        assertTrue(series.contains("verifiedAccess: Boolean = false"))
        assertTrue(series.contains("findOrCreateWordSiegeSeriesGame"))
        assertTrue(entry.contains("SERİ / HIZLI OYUN"))
        assertTrue(entry.contains("mode = WordSiegeEntryMode.SERIES"))
        assertTrue(entry.contains("WordSiegeSeriesScreen(verifiedAccess = true)"))
        assertTrue(entry.contains("onOpenStore"))
        assertTrue(shell.contains("PremiumDestination.SIEGE -> WordSiegeEntryScreen"))
        assertTrue(store.contains("showSeriesGame"))
        assertTrue(store.contains("WordSiegeSeriesScreen(verifiedAccess = true"))
        assertTrue(store.contains("owned = entitlements.seriesGameAccess"))
        assertFalse(store.contains("owned = false"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
