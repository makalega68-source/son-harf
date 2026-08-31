package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonStyleRewardsContractTest {

    @Test
    fun seasonUsesExistingServerClaimAndDoesNotInventStyleRewards() {
        val season = projectFile("app/src/main/java/com/sonharf/game/SeasonCenterScreen.kt").readText()
        val purchase = projectFile("app/src/main/java/com/sonharf/game/SeasonPassPurchaseCard.kt").readText()

        assertTrue(season.contains("backend.getMetaProgressV2()"))
        assertTrue(season.contains("backend.claimSeasonReward(tier, false)"))
        assertTrue(season.contains("backend.claimSeasonReward(tier, true)"))
        assertTrue(season.contains("Sezon Bileti görünüm ve ilerleme ödülleri verir; maç gücü vermez."))
        assertTrue(purchase.contains("ProductCatalog.SEASON_PASS_MONTHLY"))
        assertTrue(purchase.contains("PlayPurchaseVerification.verify"))
        assertTrue(purchase.contains("seasonPassPrice(product)"))
        assertTrue(purchase.contains("Google Play'de henüz kullanılamıyor"))
        assertFalse(purchase.contains("özel profil çerçevesi", ignoreCase = true))
        assertFalse(purchase.contains("sezon unvanı", ignoreCase = true))
    }

    @Test
    fun styleIsPurchasedInShopButEquippedFromProfile() {
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val profileStyle = projectFile("app/src/main/java/com/sonharf/game/ProfileStyleInventoryScreen.kt").readText()

        assertTrue(shop.contains("purchaseShopItem(item.id)"))
        assertTrue(shop.contains("SAHİPSİN"))
        assertTrue(shop.contains("PROFİLDE UYGULA"))
        assertFalse(shop.contains("equipShopItem("))
        assertTrue(profileStyle.contains("getInventory()"))
        assertTrue(profileStyle.contains("getEquippedCosmetics()"))
        assertTrue(profileStyle.contains("equipShopItem(item.id)"))
        assertTrue(profileStyle.contains("SonHarfCosmetics.apply(equipped)"))
    }

    @Test
    fun rewardsUseRealServerStateAndStayOptionalAndNonPower() {
        val rewards = projectFile("app/src/main/java/com/sonharf/game/RewardCenterScreen.kt").readText()

        assertTrue(rewards.contains("getRewardCenterStatus()"))
        assertTrue(rewards.contains("getUnifiedMissions()"))
        assertTrue(rewards.contains("claimDailyCheckin()"))
        assertTrue(rewards.contains("claimRewardedAd(rewardType, responseId)"))
        assertTrue(rewards.contains("openRewardChest()"))
        assertTrue(rewards.contains("Ads never appear during matches."))
        assertTrue(rewards.contains("Rewards never give match advantages"))
        assertTrue(rewards.contains("Son Coin added to your wallet"))
        assertFalse(rewards.contains("Mock"))
    }

    @Test
    fun homeShowsSeasonTargetAndActiveStyleAndEquippedKeyboardIsApplied() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MainExperienceApp.kt").readText()
        val profile = projectFile("app/src/main/java/com/sonharf/game/MainPlayerProfileScreen.kt").readText()
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/EmbeddedGameKeyboard.kt").readText()
        val cosmetics = projectFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()

        assertTrue(main.contains("SEZON YAKIN HEDEFİ"))
        assertTrue(main.contains("seasonRemaining"))
        assertTrue(main.contains("SonHarfCosmetics.profileAccent"))
        assertTrue(profile.contains("GÖRÜNÜMÜMÜ DÜZENLE"))
        assertTrue(profile.contains("SonHarfCosmetics.profileAccent"))
        assertTrue(keyboard.contains("SonHarfCosmetics.keyboardPalette"))
        assertTrue(keyboard.contains("palette.background"))
        assertTrue(keyboard.contains("palette.key"))
        assertTrue(keyboard.contains("palette.action"))
        assertTrue(cosmetics.contains("keyboardThemeId"))
        assertTrue(cosmetics.contains("keyboardPaletteFor"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
