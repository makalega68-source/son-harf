package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonPassRuntimeContractTest {
    @Test fun seasonPassClaimsServerRewardsAndIsOffTheStoreShelf() {
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val purchase = projectFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/SeasonStore.kt").readText()

        assertFalse(shop.contains("SeasonCenterContent()"))
        assertTrue(purchase.contains("ProductCatalog.SEASON_PASS_MONTHLY"))
        assertTrue(purchase.contains("PlayPurchaseVerification.verify(productId, purchase.purchaseToken)"))
        assertTrue(backend.contains("get_store_season_v1"))
        assertTrue(backend.contains("claim_store_season_reward_v1"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
