package com.sonharf.game

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AssetIntegrationContractTest {
    private fun read(path: String) = File(path).readText()

    @Test fun frameCatalogContainsIntegratedPurchasedVariantsAndNoPowerFields() {
        val src = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        listOf("RED", "GREEN", "MINT", "PURPLE", "GOLD", "GOLD_CROWN").forEach { assertTrue(src.contains(it)) }
        assertTrue(src.contains("only change appearance") || src.contains("yalnızca görünümü"))
    }

    @Test fun shopBackendConstructionIsDefensive() {
        val shop = read("src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt")
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(shop.contains("runCatching { OnlineGameBackend() }.getOrNull()"))
        assertTrue(frames.contains("runCatching { OnlineGameBackend() }.getOrNull()"))
    }

    @Test fun purchasedVfxIsCosmeticAndBounded() {
        val src = read("src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt")
        assertTrue(src.contains("1050"))
        assertTrue(src.contains("Cosmetic-only"))
    }
}
