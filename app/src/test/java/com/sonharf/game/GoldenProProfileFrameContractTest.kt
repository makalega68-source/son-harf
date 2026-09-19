package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GoldenProProfileFrameContractTest {
    private fun read(path: String) = File(path).readText()

    @Test
    fun goldenAvatarArtworkUsesStandard512SquareCanvas() {
        val bytes = File("src/main/res/drawable-nodpi/profile_frame_round_golden_avatar.png").readBytes()
        assertTrue(bytes.size >= 24)
        assertEquals(0x89504E47.toInt(), ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.BIG_ENDIAN).int)
        val width = ByteBuffer.wrap(bytes, 16, 4).order(ByteOrder.BIG_ENDIAN).int
        val height = ByteBuffer.wrap(bytes, 20, 4).order(ByteOrder.BIG_ENDIAN).int
        assertEquals(512, width)
        assertEquals(512, height)
    }

    @Test
    fun goldenAvatarIsRenderedByTheCentralProfileFrameCatalog() {
        val src = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")
        assertTrue(src.contains("GOLDEN_AVATAR = \"frame_round_golden_avatar\""))
        assertTrue(src.contains("GOLDEN_AVATAR -> R.drawable.profile_frame_round_golden_avatar"))
    }

    @Test
    fun proAvatarUsesTheNewAutomaticGoldArtworkWithoutReactivatingLegacyOverlay() {
        val v2 = read("src/main/java/com/sonharf/game/ProfileFramesV2.kt")
        val wrapper = read("src/main/java/com/sonharf/game/FramedProfileAvatar.kt")
        assertTrue(v2.contains("R.drawable.profile_frame_pro_gold"))
        assertTrue(v2.contains("if (isPro) proVisual else defaultVisual"))
        assertTrue(wrapper.contains("ProfileFrameAvatarPathV2("))
        assertTrue(!wrapper.contains("PurchasedProfileFrameOverlay("))
    }

    @Test
    fun profileCollectionCanRenderAndEquipOwnedFrames() {
        val collection = read("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt")
        assertTrue(collection.contains("backend.getInventory()"))
        assertTrue(collection.contains("backend.getOwnedShopItems(nextOwned)"))
        assertTrue(collection.contains("StoreProductPreview(item = item"))
        assertTrue(collection.contains("backend.equipShopItem(itemId)"))
        assertTrue(collection.contains("items.chunked(2)"))
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
