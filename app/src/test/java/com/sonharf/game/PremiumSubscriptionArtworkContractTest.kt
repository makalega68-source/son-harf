package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumSubscriptionArtworkContractTest {
    @Test
    fun `subscription products use distinct transparent text-free artwork`() {
        val drawables = listOf(
            "premium_vip_monthly.xml",
            "premium_vip_yearly.xml",
            "premium_season_pass.xml",
        )
        val contents = drawables.map { fileName ->
            val vector = repoFile("app/src/main/res/drawable/$fileName").readText()
            assertTrue("Missing vector root for $fileName", vector.contains("<vector"))
            assertFalse("Subscription artwork must not bake text: $fileName", vector.contains("<text"))
            assertFalse("Subscription artwork must not bake runtime plan copy: $fileName", vector.contains("MONTHLY") || vector.contains("YEARLY") || vector.contains("SEASON PASS"))
            vector
        }
        assertTrue("Each subscription product must have distinct artwork", contents.toSet().size == contents.size)
    }

    @Test
    fun `PRO monthly yearly and Season Pass map to exact artwork while billing stays authoritative`() {
        val vip = repoFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()
        val season = repoFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()

        assertTrue(vip.contains("ProductCatalog.VIP_MONTHLY"))
        assertTrue(vip.contains("ProductCatalog.VIP_YEARLY"))
        assertTrue(vip.contains("R.drawable.premium_vip_monthly"))
        assertTrue(vip.contains("R.drawable.premium_vip_yearly"))
        assertTrue(vip.contains("selectedProduct != null"))
        assertTrue(vip.contains("PlayPurchaseVerification.verify"))

        assertTrue(season.contains("ProductCatalog.SEASON_PASS_MONTHLY"))
        assertTrue(season.contains("R.drawable.premium_season_pass"))
        assertTrue(season.contains("enabled = !busy && product != null"))
        assertTrue(season.contains("PlayPurchaseVerification.verify"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
