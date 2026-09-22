package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProRuntimeReachabilityContractTest {
    @Test fun activeShellExposesRealProToolsInsteadOfPurchaseOnlyLoop() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/ProfessionalUnifiedApp.kt").readText()
        val pro = projectFile("app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt").readText()
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        assertTrue(shell.contains("ProfessionalDestination.PRO -> UnifiedProVipScreen"))
        assertTrue(shell.contains("ProfessionalDestination.PRIVATE_ROOM -> PrivateRoomCenterScreen"))
        assertTrue(shell.contains("onPrivateRoom = { destination = ProfessionalDestination.PRIVATE_ROOM }"))
        assertTrue(shell.contains("ProfessionalDestination.SERIES -> WordSiegeSeriesScreen"))
        assertTrue(shell.contains("onSeries = { openGame(ProfessionalDestination.SERIES, siegeLanguage) }"))
        assertTrue(shell.contains("verifiedAccess = true"))
        assertTrue(shell.contains("onExit = { leaveGame(ProfessionalDestination.PRO) }"))
        assertTrue(pro.contains("PremiumAnalysisCenterLauncher"))
        assertTrue(pro.contains("onPrivateRoom"))
        assertTrue(pro.contains("onSeries"))
        assertTrue(pro.contains("e?.seriesGameAccess == true"))
        assertTrue(pro.contains("SERİ OYUNLARI"))
        assertTrue(pro.contains("backend.getVipEntitlements()"))
        assertTrue(shop.contains("Open PRO tools"))
        assertTrue(shop.contains("if (proActive) onPro() else showVip = true"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
