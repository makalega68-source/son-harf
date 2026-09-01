package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylePanFrameContractTest {
    @Test
    fun boardUsesBoundedTwoAxisPan() {
        listOf("WordSiegeExperience.kt", "WordSiegePracticeScreen.kt").forEach { name ->
            val source = File("src/main/java/com/sonharf/game/$name").readText()
            assertTrue(source.contains("detectDragGestures"))
            assertTrue(source.contains("dragAmount.x"))
            assertTrue(source.contains("dragAmount.y"))
            assertTrue(source.contains("coerceIn(minX, 0f)"))
            assertTrue(source.contains("coerceIn(minY, 0f)"))
            assertTrue(source.contains("maxOf(432.dp, viewportShortSide + 96.dp)"))
            assertTrue(source.contains("minX / 2f"))
            assertTrue(source.contains("minY / 2f"))
            assertTrue(source.contains("clipToBounds"))
        }
    }

    @Test
    fun portraitFramesAreOverlayOnly() {
        val runtime = File("src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()
        assertTrue(runtime.contains("premium_frame_royal_gold_v3"))
        assertTrue(runtime.contains("premium_frame_black_gold_v3"))
        assertTrue(runtime.contains("premium_frame_neon_v3"))
        assertTrue(runtime.contains("ContentScale.Fit"))
        assertTrue(runtime.contains("frameIdOverride"))
        assertFalse(runtime.substringAfter("internal fun ProfilePhotoAvatarRectWithGender").contains(".padding(frame.outerPadding)"))
    }

    @Test
    fun shopHasLargeRealPreviews() {
        val shop = File("src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        assertTrue(shop.contains("LARGE PREVIEW"))
        assertTrue(shop.contains("ProfilePhotoAvatarRectWithGender"))
        assertTrue(shop.contains("premium_victory_crown_preview_higgsfield"))
        assertTrue(shop.contains("premium_mascot_white_preview_higgsfield"))
        assertTrue(shop.contains("height(154.dp)"))
    }
}
