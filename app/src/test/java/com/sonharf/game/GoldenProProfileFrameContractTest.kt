package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GoldenProProfileFrameContractTest {
    private fun read(path: String) = File(path).readText()

    private fun littleEndian24(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16)

    @Test
    fun goldenAvatarArtworkUsesStandard512SquareCanvas() {
        val bytes = File("src/main/res/drawable-nodpi/profile_frame_round_golden_avatar.webp").readBytes()
        assertTrue(bytes.size > 1_000)
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WEBP", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals("VP8X", String(bytes, 12, 4, Charsets.US_ASCII))
        assertEquals(512, littleEndian24(bytes, 24) + 1)
        assertEquals(512, littleEndian24(bytes, 27) + 1)
    }

    @Test
    fun goldenAvatarIsRenderedByTheCentralProfileFrameCatalog() {
        val src = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(src.contains("GOLDEN_AVATAR = \"frame_round_golden_avatar\""))
        assertTrue(src.contains("GOLDEN_AVATAR -> R.drawable.profile_frame_round_golden_avatar"))
        val avatar = read("src/main/java/com/sonharf/game/FramedProfileAvatar.kt")
        assertTrue(avatar.contains("frame_round_golden_avatar"))
        assertTrue(avatar.contains("requiredSize(frameSize)"))
    }

    @Test
    fun profileCollectionCanRenderAndEquipOwnedFrames() {
        val collection = read("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt")
        assertTrue(collection.contains("backend.getInventory()"))
        assertTrue(collection.contains("backend.getOwnedShopItems(nextOwned)"))
        assertTrue(collection.contains("PurchasedProfileFrameOverlay(frameId = item.id"))
        assertTrue(collection.contains("onEquip = { equipStyle(item.id) }"))
        assertTrue(collection.contains("backend.equipShopItem(itemId)"))
    }

    @Test
    fun migrationMakesCanonicalGoldenTheProRewardAndMigratesLegacyOwnership() {
        val migration = read("../supabase/migrations/20260915174439_pro_golden_avatar_frame_v2.sql")
        assertTrue(migration.contains("'frame_round_golden_avatar'"))
        assertTrue(migration.contains("where item_id = 'frame_asset_gold'"))
        assertTrue(migration.contains("delete from public.user_inventory"))
        assertTrue(migration.contains("sync_pro_golden_avatar_frame_v2"))
        assertTrue(migration.contains("set profile_frame_id = 'frame_round_golden_avatar'"))
        assertTrue(migration.contains("set search_path = ''"))
    }
}
