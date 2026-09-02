package com.sonharf.game

import org.junit.Assert.assertFalse
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

    @Test fun styleStoreUsesOneRememberedBackendAndSharesItWithFrames() {
        val shop = read("src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt")
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(shop.contains("runCatching { OnlineGameBackend() }.getOrNull()"))
        assertTrue(shop.contains("PurchasedProfileFramesStoreRow(backend = backend)"))
        assertTrue(frames.contains("PurchasedProfileFramesStoreRow(backend: OnlineGameBackend?)"))
        assertFalse(frames.contains("OnlineGameBackend()"))
    }

    @Test fun styleStoreHasRuntimeFailSafeForBackendAndAssets() {
        val shop = read("src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt")
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(shop.contains("withTimeout(STORE_LOAD_TIMEOUT_MS)"))
        assertTrue(shop.contains("güvenli modda") || shop.contains("safe mode"))
        assertTrue(frames.contains("SafeStyleDrawable"))
        assertTrue(frames.contains("catch (_: Throwable)"))
        assertTrue(frames.contains("BrokenImage"))
    }

    @Test fun purchasedVfxIsCosmeticAndBounded() {
        val src = read("src/main/java/com/sonharf/game/PurchasedVfxOverlay.kt")
        assertTrue(src.contains("1050"))
        assertTrue(src.contains("Cosmetic-only"))
    }
}
