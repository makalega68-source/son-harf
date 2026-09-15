package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProGoldProfileFrameRegressionTest {
    @Test
    fun proGoldFrameIsGrantedPermanentlyAndExposedSafely() {
        val migration = projectFile("supabase/migrations/20260915140500_pro_gold_profile_frame_v1.sql").readText()

        assertTrue(migration.contains("'frame_asset_gold'"))
        assertTrue(migration.contains("where coalesce(is_vip, false)"))
        assertTrue(migration.contains("trg_grant_pro_gold_profile_frame_v1"))
        assertTrue(migration.contains("on conflict do nothing"))
        assertTrue(migration.contains("get_public_profile_frame_v1"))
        assertTrue(migration.contains("grant execute on function public.get_public_profile_frame_v1(uuid) to authenticated"))
    }

    @Test
    fun applyButtonUsesTheRealServerEquipPath() {
        val collection = projectFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()

        assertTrue(collection.contains("backend.equipShopItem(itemId)"))
        assertTrue(collection.contains("sh(\"UYGULA\", \"APPLY\")"))
        assertTrue(collection.contains("SonHarfCosmetics.applyAndPersist(context, nextEquipped)"))
    }

    @Test
    fun everySharedProfilePhotoRendererCanShowTheEquippedFrameOutsideThePhotoCrop() {
        val runtime = projectFile("app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()
        val explicit = projectFile("app/src/main/java/com/sonharf/game/FramedProfileAvatar.kt").readText()

        assertTrue(runtime.contains("frameForAvatar"))
        assertTrue(runtime.contains("get_public_profile_frame_v1"))
        assertTrue(runtime.contains("val localFrameVersion = SonHarfCosmetics.profileFrameId"))
        assertTrue(runtime.contains("LaunchedEffect(avatarPath, explicit, localFrameVersion)"))
        assertTrue(runtime.contains("PurchasedProfileFrameOverlay(frameId = frameId, modifier = Modifier.size(frameSize))"))
        assertTrue(runtime.contains("Modifier.size(size).clip(CircleShape)"))
        assertTrue(runtime.contains("contentScale = ContentScale.Crop"))
        assertTrue(explicit.contains("frameId = frameId"))
    }

    @Test
    fun purchasedGoldKeepsTheGoldAccent() {
        val cosmetics = projectFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()

        assertTrue(cosmetics.contains("PurchasedFrameCatalog.GOLD,"))
        assertTrue(cosmetics.contains("PurchasedFrameCatalog.GOLD_CROWN -> SonHarfGold"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
