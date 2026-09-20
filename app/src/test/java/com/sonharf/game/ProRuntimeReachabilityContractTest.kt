package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProRuntimeReachabilityContractTest {
    @Test fun activeShellExposesRealProToolsInsteadOfPurchaseOnlyLoop() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val pro = projectFile("app/src/main/java/com/sonharf/game/UnifiedProVipScreen.kt").readText()
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        assertTrue(shell.contains("PRO, PRIVATE_ROOM"))
        assertTrue(shell.contains("PremiumDestination.PRO -> UnifiedProVipScreen"))
        assertTrue(shell.contains("PremiumDestination.PRIVATE_ROOM -> PrivateRoomCenterScreen"))
        assertTrue(pro.contains("PremiumAnalysisCenterLauncher"))
        assertTrue(pro.contains("onPrivateRoom"))
        assertTrue(pro.contains("backend.getVipEntitlements()"))
        assertTrue(shop.contains("PRO ARAÇLARINI AÇ"))
        assertTrue(shop.contains("if (proActive) onPro() else showVip = true"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
