package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonHarfMicroMotionContractTest {
    @Test
    fun microMotionStaysSmallNativeAndNonDecorative() {
        val motion = File("src/main/java/com/sonharf/game/SonHarfMicroMotion.kt").readText()
        val home = File("src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        // Native Compose implementation only; no Unity runtime is introduced for UI polish.
        assertFalse(motion.contains("UnityPlayer"))
        assertFalse(motion.contains("com.unity3d"))
        assertFalse(home.contains("UnityPlayer"))
        assertFalse(home.contains("com.unity3d"))

        // No endless idle pulse: motion happens only while the pointer is pressed/released.
        assertTrue(motion.contains("awaitFirstDown(requireUnconsumed = false)"))
        assertTrue(motion.contains("waitForUpOrCancellation()"))
        assertFalse(motion.contains("rememberInfiniteTransition"))
        assertFalse(motion.contains("infiniteRepeatable"))

        // Preserve click/ripple/accessibility semantics by observing without consuming gestures.
        assertTrue(motion.contains("pressedScale: Float = 0.98f"))
        assertTrue(motion.contains("graphicsLayer"))

        // Premium home keeps restrained press deltas on its primary interactive surfaces.
        assertTrue(home.contains("sonHarfPressScale(pressedScale = 0.985f)"))

        // Core product structure remains intact after Kelime Kuşatması becomes the primary game.
        assertTrue(home.contains("HomeGameModes("))
        assertTrue(home.contains("KELİME KUŞATMASI"))
        assertTrue(home.contains("KUŞATMAYA GİR"))
        assertTrue(home.contains("title = sh(\"Son Harf\", \"Last Letter\")"))
        assertTrue(home.contains("title = sh(\"Kelime Yolu\", \"Word Path\")"))
        assertFalse(home.contains("players = weeklyTop"))
        assertTrue(home.contains("PremiumProfileHero("))
        assertFalse(home.contains("PremiumPlayButton(onClick = onSiege)"))
    }
}
