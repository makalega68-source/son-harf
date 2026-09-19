package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2PremiumFinalAcceptanceContractTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir"))
        repeat(7) {
            if (File(d, "app/src/main").exists()) return d
            d = d.parentFile ?: return@repeat
        }
        error("repo root not found")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test fun proPurchaseIsLifetimeOnlyAndHasNoProhibitedFairPlayOrRestoreUi() {
        val s = source("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt")
        assertTrue("ProductCatalog.PRO_LIFETIME" in s)
        assertTrue("oneTimePurchaseOfferDetails" in s)
        assertFalse("ADİL REKABET" in s)
        assertFalse("FAIR PLAY" in s)
        assertFalse("Restore Purchases" in s)
        assertFalse("Manage subscription" in s)
        assertFalse("Aboneliği yönet" in s)
    }

    @Test fun seriesLockedStateRoutesToExactPermanentSeriesProduct() {
        val dialog = source("app/src/main/java/com/sonharf/game/PremiumProductPurchaseDialog.kt")
        assertTrue("ProductCatalog.SERIES_GAME" in dialog)
        assertTrue("SERIES_GAME_FALLBACK_PRICE_TRY" in dialog)
        val series = source("app/src/main/java/com/sonharf/game/WordSiegeSeriesScreen.kt")
        assertTrue("productId = ProductCatalog.SERIES_GAME" in series)
        assertTrue("backend.getVipEntitlements()" in series)
        assertTrue("SERİ OYUN'U AÇ" in series)
    }

    @Test fun featuredStoreShowsPremiumProductsAndCoinSheetDoesNotDuplicateThem() {
        val s = source("app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt")
        assertTrue("showPremiumProducts = true" in s)
        assertTrue("showCoinPacks = false" in s)
        assertTrue("showPremiumProducts = false" in s)
        assertTrue("showCoinPacks = true" in s)
        assertFalse("Maskot" in s)
        assertFalse("Mascot" in s)
    }

    @Test fun proPageListsExactBundleWithoutLegacyAnalysisOrPrivateRoomSalesCopy() {
        val s = source("app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt")
        listOf(
            "Reklamsız kullanım",
            "Puan Hesaplayıcı",
            "Harf Tablosu",
            "Seri Oyun",
            "Arkadaş Listesi",
            "Son Harf tam kelime geçmişi",
            "50 aktif oyun",
            "PRO profil çerçevesi",
            "PRO rozeti ve prestij",
            "100 Son Coin",
        ).forEach { assertTrue("Missing PRO benefit: $it", it in s) }
        assertFalse("Gelişmiş istatistik ve maç analizi" in s)
        assertFalse("Özel oda erişimi" in s)
    }

    @Test fun proAdBannerIsCollapsedFromAuthoritativeProState() {
        val banner = source("app/src/main/java/com/sonharf/game/NonGameBannerAd.kt")
        assertTrue("isPremium" in banner)
        assertTrue("canReserveBanner" in banner)
        assertTrue("if (!slotVisible) return" in banner)
        val shell = source("app/src/main/java/com/sonharf/game/PremiumCanvaAppV2.kt")
        assertTrue("backend.getVipEntitlements()" in shell)
        assertTrue("premium?.isPro == true" in shell)
        assertTrue("SonHarfTopAdBanner(isPremium = isPro)" in shell)
    }

    @Test fun proProfileFrameIsServerAuthoritativeAndCosmetic() {
        val s = source("app/src/main/java/com/sonharf/game/ProfileExperienceV2.kt")
        assertTrue("backend.getVipEntitlements().isPro" in s)
        assertTrue("if (proActive)" in s)
        assertTrue("BorderStroke(4.dp, SonHarfGold)" in s)
        assertTrue("Text(\n                                \"PRO\"" in s)
        assertTrue("ProfileAvatarV2(avatarBytes" in s)
    }

    @Test fun seriesBackendSourceIsSeparateAndServerTimed() {
        val sql = source("supabase/migrations/20260919043000_word_siege_series_game_v1.sql")
        listOf("(3,5,10)", "game_mode='series'", "turn_deadline", "series_three_missed_turns", "series_auto_pass", "word_siege_series_reset_actor_v1").forEach {
            assertTrue("Missing Series backend contract marker: $it", it in sql)
        }
        assertTrue("g.game_mode='classic'" in sql)
    }
}
