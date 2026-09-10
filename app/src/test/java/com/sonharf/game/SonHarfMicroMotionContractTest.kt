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

        // Native Compose implementation only; the purchased Unity package is a visual reference,
        // not a runtime dependency of the Android app.
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

        // Phase 1 is intentionally limited to small scale deltas on existing entry points.
        assertTrue(home.contains("sonHarfPressScale(pressedScale = 0.985f)"))
        assertTrue(home.contains("sonHarfPressScale(pressedScale = 0.99f)"))
        assertTrue(home.contains("sonHarfPressScale(pressedScale = 0.96f)"))
        assertTrue(home.contains("sonHarfPressScale(pressedScale = 0.94f)"))

        // Core product structure must remain intact.
        assertTrue(home.contains("Premier 1v1 kelime düellosu"))
        assertTrue(home.contains("KELİME KUŞATMASI"))
        assertTrue(home.contains("HARF YOLU"))
        assertTrue(home.contains("HAFTANIN ZİRVESİ"))
    }
}
