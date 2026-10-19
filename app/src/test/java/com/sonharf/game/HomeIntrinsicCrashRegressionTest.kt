package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The home game cards sit in a Row sized with IntrinsicSize.Min. A SubcomposeLayout inside it
 * (BoxWithConstraints, lazy lists, TabRow) crashes the app on launch, so the home file must not
 * use one.
 */
class HomeIntrinsicCrashRegressionTest {
    @Test
    fun homeCardsNeverSubcomposeInsideAnIntrinsicRow() {
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        assertFalse(home.contains("BoxWithConstraints("))
        assertFalse(home.contains("LazyRow("))
        assertFalse(home.contains("LazyColumn("))
    }
}
