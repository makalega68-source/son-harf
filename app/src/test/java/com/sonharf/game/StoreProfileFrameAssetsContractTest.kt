package com.sonharf.game

import org.junit.Assert.assertEquals
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
        "profile_frame_wing_silver.webp",
        "profile_frame_wing_blue.webp",
        "profile_frame_flower_pink_blossom.webp",
        "profile_frame_wing_pink.webp",
        "profile_frame_wing_gold.webp",
        "profile_frame_wing_aurora.webp",
    )

    private fun littleEndian24(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16)

    @Test
    fun realStoreFrameArtworkUsesOneStandard512WebpCanvas() {
        drawableFiles.forEach { fileName ->
            val bytes = File("src/main/res/drawable-nodpi/$fileName").readBytes()
            assertTrue("$fileName too small", bytes.size > 1_000)
            assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
            assertEquals("WEBP", String(bytes, 8, 4, Charsets.US_ASCII))
            assertEquals("VP8X", String(bytes, 12, 4, Charsets.US_ASCII))
            assertEquals(512, littleEndian24(bytes, 24) + 1)
            assertEquals(512, littleEndian24(bytes, 27) + 1)
        }
    }

    @Test
    fun simplifiedVectorRecreationsAreNotPackaged() {
        drawableFiles.forEach { fileName ->
            val xmlName = fileName.removeSuffix(".webp") + ".xml"
            assertTrue("legacy vector still present: $xmlName", !File("src/main/res/drawable/$xmlName").exists())
        }
    }

    @Test
    fun catalogAndBackendExposeEveryNewStoreFrame() {
        val ui = File("src/main/java/com/sonharf/game/PurchasedStyleUi.kt").readText()
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        frameIds.forEach { id ->
            assertTrue("UI missing $id", ui.contains("\"$id\""))
            assertTrue("EconomyStore missing $id", economy.contains("\"$id\""))
        }
        assertTrue(ui.contains("painterResource(spec.drawable)"))
    }

    @Test
    fun sonCoinPurchaseIsServerAuthoritativeThenEquipsOwnedFrame() {
        val ui = File("src/main/java/com/sonharf/game/PurchasedStyleUi.kt").readText()
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        assertTrue(ui.contains("b.purchaseShopItem(spec.id)"))
        assertTrue(ui.contains("b.equipShopItem(spec.id)"))
        assertTrue(economy.contains("rpc(\"purchase_shop_item\""))
        assertTrue(economy.contains("rpc(\"equip_shop_item\""))
    }

    @Test
    fun realArtworkGetsVerifiedAvatarOpeningGeometry() {
        val avatar = File("src/main/java/com/sonharf/game/FramedProfileAvatar.kt").readText()
        assertTrue(avatar.contains("frameId?.startsWith(\"frame_wing_\")"))
        assertTrue(avatar.contains("frame_flower_pink_blossom"))
        assertTrue(avatar.contains("frame_round_golden_avatar"))
        assertTrue(avatar.contains("2.064516f"))
        assertTrue(avatar.contains("requiredSize(frameSize)"))
    }

    @Test
    fun migrationListsExpectedPricesAndDoesNotTouchProGolden() {
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
