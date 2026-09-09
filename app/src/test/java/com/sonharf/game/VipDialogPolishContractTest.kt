package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VipDialogPolishContractTest {

    @Test
    fun vipDialogKeepsBillingFlowAndExplicitlyPromisesFairPlay() {
        val source = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()

        assertTrue(source.contains("BillingManager("))
        assertTrue(source.contains("PlayPurchaseVerification.verify"))
        assertTrue(source.contains("manager.launchProduct(activity, product)"))
        assertTrue(source.contains("ProductCatalog.VIP_YEARLY"))
        assertTrue(source.contains("ProductCatalog.VIP_MONTHLY"))
        assertFalse(source.contains("rememberInfiniteTransition"))

        assertTrue(source.contains("ADİL REKABET"))
        assertTrue(source.contains("FAIR PLAY"))
        assertTrue(source.contains("gives no score, target-letter, or word advantage"))
        assertFalse(source.contains("2x SKOR"))
        assertFalse(source.contains("2x SCORE"))
        assertFalse(source.contains("server validated and consumed atomically"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
