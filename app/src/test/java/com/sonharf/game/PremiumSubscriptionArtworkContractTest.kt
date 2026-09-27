package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumSubscriptionArtworkContractTest {
    @Test
    fun `subscription products use distinct transparent text-free artwork`() {
        val drawables = listOf(
            "premium_vip_monthly.png",
            "premium_vip_yearly.png",
            "premium_season_pass.png",
        )
        val contents = drawables.map { fileName ->
            val bytes = repoFile("app/src/main/res/drawable-nodpi/$fileName").readBytes()
            // Painted, text-free plan art: transparent RGBA PNG; plan copy comes from the UI.
            assertTrue("Subscription artwork must be a transparent PNG: $fileName", bytes[1] == 'P'.code.toByte() && bytes[25].toInt() == 6)
            bytes.toList()
        }
        assertTrue("Each subscription product must have distinct artwork", contents.toSet().size == contents.size)
    }

    @Test
    fun `PRO monthly yearly and Season Pass map to exact artwork while billing stays authoritative`() {
        val vip = repoFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()
        val season = repoFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()
        val billing = repoFile("app/src/main/java/com/sonharf/game/billing/BillingManager.kt").readText()

        assertTrue(vip.contains("ProductCatalog.VIP_MONTHLY"))
        assertTrue(vip.contains("ProductCatalog.VIP_YEARLY"))
        assertTrue(vip.contains("R.drawable.premium_vip_monthly"))
        assertTrue(vip.contains("R.drawable.premium_vip_yearly"))
        assertTrue(vip.contains("selectedPurchasable = BillingManager.hasPurchasableOffer(selectedProduct)"))
        assertTrue(vip.contains("enabled = !busy && selectedPurchasable"))
        assertTrue(vip.contains("PlayPurchaseVerification.verify"))

        assertTrue(season.contains("ProductCatalog.SEASON_PASS_MONTHLY"))
        assertTrue(season.contains("R.drawable.premium_season_pass"))
        assertTrue(season.contains("enabled = !busy && BillingManager.hasPurchasableOffer(product)"))
        assertTrue(season.contains("PlayPurchaseVerification.verify"))

        assertTrue(billing.contains("if (!hasPurchasableOffer(productDetails))"))
        assertTrue(billing.contains("BillingClient.BillingResponseCode.ITEM_UNAVAILABLE"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
