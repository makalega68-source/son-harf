package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VipDialogPolishContractTest {

    @Test
    fun vipDialogKeepsVerifiedLifetimeBillingAndNoPayToWinBenefits() {
        val source = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()

        assertTrue(source.contains("BillingManager("))
        assertTrue(source.contains("PlayPurchaseVerification.verify"))
        assertTrue(source.contains("manager.launchProduct(activity, details)"))
        assertTrue(source.contains("ProductCatalog.PRO_LIFETIME"))
        assertTrue(source.contains("queryOneTimeProducts"))
        assertTrue(source.contains("oneTimePurchaseOfferDetails"))
        assertFalse(source.contains("ProductCatalog.VIP_YEARLY"))
        assertFalse(source.contains("ProductCatalog.VIP_MONTHLY"))
        assertFalse(source.contains("querySubscriptions("))
        assertFalse(source.contains("rememberInfiniteTransition"))

        assertTrue(source.contains("PUAN HESAPLAYICI"))
        assertTrue(source.contains("HARF TABLOSU"))
        assertTrue(source.contains("SERİ OYUN"))
        assertTrue(source.contains("50 AKTİF OYUN"))
        assertTrue(source.contains("100 SON COIN"))
        assertFalse(source.contains("2x SKOR"))
        assertFalse(source.contains("2x SCORE"))
        assertFalse(source.contains("gizli rakip harf"))
        assertFalse(source.contains("hidden opponent letters"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
