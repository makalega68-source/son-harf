package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedFinalSurfaceContractTest {
    @Test fun homeCommandDeckUsesPurchasedPanelsAndCtas() {
        val source = File("src/main/java/com/sonharf/game/PremiumHomePolish.kt").readText()
        val deck = source.substringAfter("internal fun PremiumHomeCommandDeckPolished(").substringBefore("internal fun PremiumWeeklyBestPolished(")
        assertTrue(deck.contains("PurchasedUiAsset.PANEL_LARGE"))
        assertTrue(deck.contains("PurchasedAvatarFrame("))
        assertTrue(deck.contains("PurchasedButton("))
        assertTrue(deck.contains("PurchasedProgress("))
        assertFalse(deck.contains("ButtonDefaults.buttonColors"))
        assertFalse(deck.contains("Brush.linearGradient"))
    }

    @Test fun paidProfileFrameStoreCardsUsePurchasedGamePanels() {
        val source = File("src/main/java/com/sonharf/game/ProfileFramesV2.kt").readText()
        val store = source.substringAfter("internal fun ProfileFramesV2StoreRow(")
        assertTrue(store.contains("PurchasedSectionHeader("))
        assertTrue(store.contains("PurchasedUiAsset.PANEL_MEDIUM"))
        assertTrue(store.contains("PurchasedButton("))
        assertTrue(store.contains("billing.launchProduct(host, product)"))
        assertFalse(store.contains("ButtonDefaults.buttonColors"))
    }

    @Test fun proBodyUsesPurchasedPackageInsteadOfMaterialHeroCards() {
        val source = File("src/main/java/com/sonharf/game/UnifiedProVipScreen.kt").readText()
        assertTrue(source.contains("PurchasedUiAsset.SEASON_BANNER"))
        assertTrue(source.contains("PurchasedUiAsset.ICON_CROWN"))
        assertTrue(source.contains("PurchasedButtonStyle.PURPLE"))
        assertTrue(source.contains("PurchasedUiAsset.REWARD_PANEL"))
        assertTrue(source.contains("VipPurchaseDialog("))
        assertFalse(source.contains("Brush.linearGradient"))
        assertFalse(source.contains("ButtonDefaults.buttonColors"))
    }
}
