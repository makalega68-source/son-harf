package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StoreProfileFrameAssetsContractTest {
    private val frameIds = listOf(
        "frame_wing_silver",
        "frame_wing_blue",
        "frame_flower_pink_blossom",
        "frame_wing_pink",
        "frame_wing_gold",
        "frame_wing_aurora",
    )

    private val drawableFiles = listOf(
        "profile_frame_wing_silver.xml",
        "profile_frame_wing_blue.xml",
        "profile_frame_flower_pink_blossom.xml",
        "profile_frame_wing_pink.xml",
        "profile_frame_wing_gold.xml",
        "profile_frame_wing_aurora.xml",
    )

    @Test
    fun retiredFrameVectorsRemainAvailableForProvenanceAndRollback() {
        drawableFiles.forEach { fileName ->
            val xml = File("src/main/res/drawable/$fileName").readText()
            assertTrue("$fileName width", xml.contains("android:width=\"512dp\""))
            assertTrue("$fileName height", xml.contains("android:height=\"512dp\""))
            assertTrue("$fileName viewport width", xml.contains("android:viewportWidth=\"512\""))
            assertTrue("$fileName viewport height", xml.contains("android:viewportHeight=\"512\""))
        }
    }

    @Test
    fun retiredFrameCatalogDataMayRemainButRuntimeRenderingIsDisabled() {
        val ui = File("src/main/java/com/sonharf/game/PurchasedStyleUi.kt").readText()
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val avatar = File("src/main/java/com/sonharf/game/FramedProfileAvatar.kt").readText()
        frameIds.forEach { id ->
            assertTrue("UI provenance missing $id", ui.contains("\"$id\""))
            assertTrue("EconomyStore provenance missing $id", economy.contains("\"$id\""))
        }
        assertTrue(ui.contains("PROFILE_FRAMES_RETIRED = true"))
        assertTrue(ui.contains("if (PROFILE_FRAMES_RETIRED) return"))
        assertFalse(avatar.contains("PurchasedProfileFrameOverlay("))
    }

    @Test
    fun historicalServerPurchaseHooksRemainAuditableButAreUnreachableFromRetiredRow() {
        val ui = File("src/main/java/com/sonharf/game/PurchasedStyleUi.kt").readText()
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        assertTrue(ui.contains("b.purchaseShopItem(spec.id)"))
        assertTrue(ui.contains("b.equipShopItem(spec.id)"))
        assertTrue(economy.contains("rpc(\"purchase_shop_item\""))
        assertTrue(economy.contains("rpc(\"equip_shop_item\""))
        assertTrue(ui.contains("if (PROFILE_FRAMES_RETIRED) return"))
    }

    @Test
    fun avatarDoesNotReserveDecorativeFrameClearanceAfterRetirement() {
        val avatar = File("src/main/java/com/sonharf/game/FramedProfileAvatar.kt").readText()
        assertTrue(avatar.contains("val legacyFrameId = frameId"))
        assertFalse(avatar.contains("frameId?.startsWith(\"frame_wing_\")"))
        assertFalse(avatar.contains("frame_flower_pink_blossom"))
        assertFalse(avatar.contains("PurchasedProfileFrameOverlay("))
    }

    @Test
    fun historicalMigrationStillDocumentsOriginalPricesWithoutMutatingProGolden() {
        val migration = File("../supabase/migrations/20260915183000_store_wing_blossom_frames_v1.sql").readText()
        val expected = mapOf(
            "frame_wing_silver" to 180,
            "frame_wing_blue" to 240,
            "frame_flower_pink_blossom" to 220,
            "frame_wing_pink" to 240,
            "frame_wing_gold" to 260,
            "frame_wing_aurora" to 320,
        )
        expected.forEach { (id, price) ->
            assertTrue("migration missing $id", migration.contains("'$id'"))
            assertTrue("migration missing $id price", migration.contains("$price, false, true"))
        }
        assertTrue(migration.contains("Golden PRO remains a separate entitlement"))
        assertTrue(!migration.contains("'frame_round_golden_avatar'"))
    }
}
