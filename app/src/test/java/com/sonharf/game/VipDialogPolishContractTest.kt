package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VipDialogPolishContractTest {

    @Test
    fun vipDialogKeepsBillingFlowAndDropsHeavyPulseUi() {
        val source = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()

        assertTrue(source.contains("BillingManager("))
        assertTrue(source.contains("PlayPurchaseVerification.verify"))
        assertTrue(source.contains("manager.launchProduct(activity, product)"))
        assertTrue(source.contains("ProductCatalog.VIP_YEARLY"))
        assertTrue(source.contains("ProductCatalog.VIP_MONTHLY"))
        assertFalse(source.contains("rememberInfiniteTransition"))
        assertTrue(source.lowercase().contains("never grants time, score, moves, rating or live decision advantages"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
