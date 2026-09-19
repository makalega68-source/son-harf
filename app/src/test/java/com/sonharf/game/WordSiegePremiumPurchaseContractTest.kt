package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePremiumPurchaseContractTest {

    @Test
    fun lockedToolsRouteToTheirPermanentProductsAndRefreshBackendEntitlements() {
        val panel = projectFile("app/src/main/java/com/sonharf/game/WordSiegePremiumPanel.kt").readText()
        val dialog = projectFile("app/src/main/java/com/sonharf/game/PremiumProductPurchaseDialog.kt").readText()

        assertTrue(panel.contains("ProductCatalog.SCORE_CALCULATOR"))
        assertTrue(panel.contains("ProductCatalog.LETTER_TABLE"))
        assertTrue(panel.contains("PremiumProductPurchaseDialog("))
        assertTrue(panel.contains("entitlementRefreshKey += 1"))
        assertTrue(panel.contains("backend.getVipEntitlements()"))

        assertTrue(dialog.contains("queryOneTimeProducts(listOf(productId))"))
        assertTrue(dialog.contains("manager.launchProduct(activity, selected)"))
        assertTrue(dialog.contains("PlayPurchaseVerification.verify(productId, purchase.purchaseToken)"))
        assertTrue(dialog.contains("ProductCatalog.SERIES_GAME"))
        assertTrue(dialog.contains("productId in setOf(ProductCatalog.SERIES_GAME, ProductCatalog.LETTER_TABLE, ProductCatalog.SCORE_CALCULATOR)"))
        assertTrue(dialog.contains("manager.restorePurchases(setOf(productId))"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
