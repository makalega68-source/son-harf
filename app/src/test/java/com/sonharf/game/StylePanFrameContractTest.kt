package com.sonharf.game
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class StylePanFrameContractTest {
 @Test fun boardUsesBoundedTwoAxisPan() { listOf("WordSiegeExperience.kt","WordSiegePracticeScreen.kt").forEach { name -> val s=File("src/main/java/com/sonharf/game/$name").readText(); assertTrue(s.contains("detectDragGestures")); assertTrue(s.contains("dragAmount.x")); assertTrue(s.contains("dragAmount.y")); assertTrue(s.contains("coerceIn(minX, 0f)")); assertTrue(s.contains("coerceIn(minY, 0f)")); assertTrue(s.contains("maxOf(432.dp, viewportShortSide + 96.dp)")); assertTrue(s.contains("minX / 2f")); assertTrue(s.contains("minY / 2f")); assertTrue(s.contains("clipToBounds")) } }
 @Test fun portraitFramesAreOverlayOnly() { val s=File("src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText(); assertTrue(s.contains("premium_frame_royal_gold_v3")); assertTrue(s.contains("premium_frame_black_gold_v3")); assertTrue(s.contains("premium_frame_neon_v3")); assertTrue(s.contains("ContentScale.Fit")); assertTrue(s.contains("frameIdOverride")); assertFalse(s.substringAfter("internal fun ProfilePhotoAvatarRectWithGender").contains(".padding(frame.outerPadding)")) }
 @Test fun shopHasLargeRealPreviews() { val s=File("src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText(); assertTrue(s.contains("LARGE PREVIEW")); assertTrue(s.contains("ProfilePhotoAvatarRectWithGender")); assertTrue(s.contains("premium_victory_crown_preview_higgsfield")); assertTrue(s.contains("premium_mascot_white_preview_higgsfield")); assertTrue(s.contains("height(154.dp)")) }
}
