package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedSettingsCollectionContractTest {
    @Test fun ownedCollectionUsesPurchasedPanelsWithoutChangingOwnershipFlow() {
        val source = File("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()
        assertTrue(source.contains("PurchasedUiAsset.PANEL_MEDIUM"))
        assertTrue(source.contains("PurchasedUiAsset.PANEL_LARGE"))
        assertTrue(source.contains("PurchasedUiAsset.REWARD_PANEL"))
        assertTrue(source.contains("PurchasedButton("))
        assertTrue(source.contains("backend.getInventory()"))
        assertTrue(source.contains("backend.getOwnedShopItems(nextOwned)"))
        assertTrue(source.contains("backend.getEquippedCosmetics()"))
        assertTrue(source.contains("backend.equipDefaultGameTheme()"))
        assertTrue(source.contains("backend.equipShopItem(itemId)"))
        assertTrue(source.contains("SonHarfCosmetics.applyAndPersist(context, next, owned)"))
        val summary = source.substringAfter("private fun ActiveStyleSummary(").substringBefore("private fun DefaultPremiumThemeTile(")
        assertFalse(summary.contains("Surface("))
        val tile = source.substringAfter("private fun CollectionTileShell(").substringBefore("private fun BoxScope.ActiveCheck")
        assertTrue(tile.contains("PurchasedPanel("))
        assertFalse(tile.contains("Surface("))
    }
}
