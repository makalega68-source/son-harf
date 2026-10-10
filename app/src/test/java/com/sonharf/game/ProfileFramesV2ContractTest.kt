package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFramesV2ContractTest {
    private fun source(path: String) = File(path).readText()

    @Test fun simpleRingsSellForCoinsAndCrestsThroughGooglePlay() {
        val catalog = source("src/main/java/com/sonharf/game/ProfileFrameCollection.kt")
        val products = source("src/main/java/com/sonharf/game/billing/ProductCatalog.kt")
        val store = source("src/main/java/com/sonharf/game/ProfileFrameStore.kt")
        val edge = source("../supabase/functions/verify-play-purchase/index.ts")
        val migration = source("../supabase/migrations/20260927110000_profile_frames_v2.sql")
        listOf("frame_round_starter_blue", "frame_round_pearl", "frame_round_ocean", "frame_round_rose", "frame_round_lilac", "frame_round_botanic").forEach {
            assertTrue(catalog.contains("\"$it\""))
            assertTrue(migration.contains("'$it'"))
        }
        listOf("profile_frame_gold_crest", "profile_frame_emerald", "profile_frame_amethyst", "profile_frame_pink_blossom", "profile_frame_blue_royal").forEach {
            assertTrue(products.contains("\"$it\""))
            assertTrue(edge.contains("\"$it\""))
            assertTrue(migration.contains("('$it', true"))
        }
        assertTrue(store.contains("manager.launchProduct(activity, product)"))
        assertTrue(store.contains("PlayPurchaseVerification.verify(productId, purchase.purchaseToken)"))
        assertTrue(store.contains("b.purchaseShopItem(frame.id)"))
        // Play-only crests are inactive shop rows, so coins can never buy them.
        assertTrue(migration.contains("0, false, false, 311"))
        assertTrue(migration.contains("kind = 'profile_frame' and diamond_price > 0"))
        assertTrue(migration.contains("insert into public.user_inventory(user_id,item_id)"))
    }

    @Test fun proMembersGetTheRoyalGoldFrameAutomaticallyAndLoseItWithPro() {
        val catalog = source("src/main/java/com/sonharf/game/ProfileFrameCollection.kt")
        val store = source("src/main/java/com/sonharf/game/ProfileFrameStore.kt")
        val migration = source("../supabase/migrations/20260927120000_pro_royal_gold_frame.sql")
        val edge = source("../supabase/functions/verify-play-purchase/index.ts")
        assertTrue(catalog.contains("val proFrame = Frame(\"frame_round_golden_avatar\", R.drawable.profile_frame_round_golden_avatar"))
        assertTrue(store.contains("frame == ProfileFrameCollection.proFrame -> onPro()"))
        assertTrue(migration.contains("set profile_frame_id = 'frame_round_golden_avatar'"))
        assertTrue(migration.contains("delete from public.user_inventory where user_id = new.id and item_id = 'frame_round_golden_avatar'"))
        assertTrue(!edge.contains("profile_frame_royal_gold"))
    }

    @Test fun equippedFrameIsDrawnAroundTheAvatarAndCanBeRemoved() {
        val avatar = source("src/main/java/com/sonharf/game/FramedProfileAvatar.kt")
        val profile = source("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt")
        // Photo and frame share the 72% inner size so the 1.38x frame art fits the avatar slot without spilling.
        assertTrue(avatar.contains("ProfileFrameArt(frameId = legacyFrameId, size = size * .72f)"))
        assertTrue(avatar.contains("size = if (framed) size * .72f else size"))
        assertTrue(avatar.contains("Modifier.size(size)"))
        assertTrue(profile.contains("\"profile_frame\" -> sh(\"Çerçevesiz\", \"No frame\")"))
        listOf(
            "profile_frame_round_pearl", "profile_frame_round_golden_avatar", "profile_frame_premium_emerald", "profile_frame_premium_amethyst",
            "profile_frame_premium_sakura", "profile_frame_premium_sapphire", "profile_frame_premium_gold",
        ).forEach { assertTrue("missing $it", File("src/main/res/drawable-nodpi/$it.png").isFile) }
    }
}
