package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiMasterGddV3ContractTest {
    @Test
    fun canonicalPaletteMatchesMasterGdd() {
        val theme = File("src/main/java/com/sonharf/game/SonHarfTheme.kt").readText()

        listOf(
            "0xFF8A9A86", // Sage Green
            "0xFFF9F8F6", // Off-White
            "0xFF7A9AEE", // Soft Blue
            "0xFF40E0D0", // Turquoise
            "0xFFF2EFE9", // Light Beige
            "0xFFB5A2FF", // Lavender
            "0xFF5C6F84", // Slate Blue
            "0xFFA3E4D7", // Pale Mint
            "0xFFE07A5F", // Controlled Warm Accent
        ).forEach { token -> assertTrue("Missing GDD palette token $token", theme.contains(token)) }
    }

    @Test
    fun modeHierarchyAndLanguageScopeStayFocused() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val firstRun = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()

        assertTrue(shell.contains("title = sh(\"KELİME KUŞATMASI\", \"KELİME KUŞATMASI\")"))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))
        assertTrue(firstRun.contains("selected == \"tr\""))
        assertTrue(firstRun.contains("selected == \"en\""))
        assertFalse(firstRun.contains("selected == \"es\""))
        assertFalse(firstRun.contains("selected == \"fr\""))
        assertFalse(firstRun.contains("selected == \"de\""))
    }

    @Test
    fun homeSurfacesProfileCoinProAndNotificationEntry() {
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()

        assertTrue(home.contains("FramedProfilePhotoAvatar("))
        assertTrue(home.contains("Son Coin"))
        assertTrue(home.contains("PRO ÜYE"))
        assertTrue(home.contains("Icons.Rounded.Notifications"))
        assertTrue(home.contains("Bildirimler ve davetler"))
    }

    @Test
    fun clubUsesDedicatedFullPageChatWithoutReplacingManagementSurface() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val club = File("src/main/java/com/sonharf/game/KelimeKusatmasiClubScreen.kt").readText()

        assertTrue(shell.contains("PremiumDestination.CLUB -> KelimeKusatmasiClubScreen()"))
        assertTrue(club.contains("Text(sh(\"KULÜP SOHBETİ\", \"CLUB CHAT\")"))
        assertTrue(club.contains("Modifier.fillMaxSize()"))
        assertTrue(club.contains("b.getClubMessages(current.clubId)"))
        assertTrue(club.contains("b.sendClubMessage(current.clubId, outgoing)"))
        assertTrue(club.contains("CompetitionHubScreen(onBack = { showClubCenter = false }, clubEntry = true)"))
        assertTrue(club.contains("onValueChange = { input = it.take(300) }"))
        assertTrue(club.contains("now - lastMessageSentAt < 1_500L"))
        assertTrue(club.contains("Mesajları çok hızlı gönderiyorsun."))
    }

    @Test
    fun economyKeepsVerifiedFramesAndZeroPayToWinPromise() {
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val shop = File("src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        listOf(
            "frame_round_starter_blue",
            "frame_round_starter_pink",
            "frame_round_starter_neutral",
            "frame_round_ocean",
            "frame_round_botanic",
            "frame_round_lilac",
            "frame_round_rose",
        ).forEach { id -> assertTrue("Missing profile frame $id", economy.contains("\"$id\"")) }

        assertTrue(economy.contains("vip_pro_frame_access"))
        assertTrue(shop.contains("ADİL OYUN SÖZÜ"))
        assertTrue(shop.contains("Mağaza ürünleri maç gücü, skor veya rating avantajı sağlamaz."))
    }

    @Test
    fun territoryScoringRemainsPermanentWordScorePlusTwoPerOwnedCube() {
        val rules = File("src/main/java/com/sonharf/game/WordSiegeFinalRules.kt").readText()

        assertTrue(rules.contains("const val CUBE_TRANSFER_POINTS: Int = 2"))
        assertTrue(rules.contains("wordScore + cubeTransfer(ownedCubes)"))
        assertTrue(rules.contains("Word points are permanent"))
        assertTrue(rules.contains("only a rival capture of one"))
    }
}
