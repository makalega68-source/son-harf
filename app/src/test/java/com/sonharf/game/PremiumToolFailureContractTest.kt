package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumToolFailureContractTest {
    @Test fun entitlementTransportFailureNeverPretendsPurchasedToolsAreLocked() {
        val panel = projectFile("app/src/main/java/com/sonharf/game/WordSiegePremiumPanel.kt").readText()

        assertTrue(panel.contains("VipEntitlementsDto?"))
        assertTrue(panel.contains("entitlementError"))
        assertTrue(panel.contains("Preserve the last known entitlement"))
        assertTrue(panel.contains("Premium erişimi doğrulanamadı"))
        assertTrue(panel.contains("entitlementRetry++"))
        assertFalse(panel.contains("getOrDefault(VipEntitlementsDto())"))
        assertTrue(panel.contains("fontSize = 10.sp"))
        assertTrue(panel.contains("HARFLER"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
