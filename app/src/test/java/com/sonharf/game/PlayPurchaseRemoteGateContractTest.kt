package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPurchaseRemoteGateContractTest {
    @Test
    fun disabledRemoteProductsFailClosedBeforePlayGrant() {
        val source = projectFile("supabase/functions/verify-play-purchase/index.ts").readText()
        assertTrue(source.contains("from(\"store_catalog_config\")"))
        assertTrue(source.contains(".eq(\"product_id\", productId)"))
        assertTrue(source.contains("store_catalog_unavailable"))
        assertTrue(source.contains("product_disabled"))
        assertTrue(source.indexOf("product_disabled") < source.indexOf("googleAuth.getClient()"))
        assertTrue(source.indexOf("product_disabled") < source.indexOf("apply_verified_play_purchase_v2"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
