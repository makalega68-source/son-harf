package com.sonharf.game

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sonharf.game.mascot.*
import com.sonharf.game.data.GameRoomDto
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Isolated visual fixtures; no authentication bypass or backend actions. */
class MageCatRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nineExpressionsDecodeAndRenderOnAndroid() {
        compose.setContent {
            MaterialTheme {
                Surface {
                    Column(Modifier.padding(16.dp)) {
                        Text("SON HARF · 2D ifadeler", style = MaterialTheme.typography.titleLarge)
                        MageCatMood.entries.chunked(3).forEach { moods ->
                            Row {
                                moods.forEach { mood ->
                                    MageCatCompanion(size = 96.dp, moodOverride = mood, animateIdle = false)
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription("Mage Cat").fetchSemanticsNodes().size == 9
        }
        compose.onAllNodesWithContentDescription("Mage Cat").assertCountEquals(9)
        for (index in 0..8) compose.onAllNodesWithContentDescription("Mage Cat")[index].assertIsDisplayed()
        saveScreenshot("nine-expressions.png")
    }

    @Test fun narrowDockSupportsLargeTextAndShowsVictory() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Surface(Modifier.width(320.dp)) {
                        ReactiveMageCatOverlay(
                            CompanionSnapshot("fixture", 40, 20, false, finished = true, won = true),
                        )
                    }
                }
            }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription("Mage Cat").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithContentDescription("Mage Cat").assertIsDisplayed()
        compose.onNodeWithText("MAÇ TAMAMLANDI").assertIsDisplayed()
        compose.onNodeWithText("Zafer senin! Harika oynadın.").assertIsDisplayed()
        saveScreenshot("narrow-dock-large-text.png")
    }

    @Test fun compactArenaKeepsMascotInputAndKeyboardSeparate() {
        compose.setContent {
            MaterialTheme {
                Surface(Modifier.width(360.dp).height(640.dp)) {
                    PremierArena(
                        language = "tr",
                        room = GameRoomDto(id = "fixture", code = "TEST", hostId = "host", guestId = "guest",
                            status = "playing", currentPlayerId = "host", hostScore = 12, guestScore = 9),
                        me = null, opponent = null, meId = "host", words = emptyList(),
                        input = "KALEM", notice = "", busy = false, turnSeconds = 14,
                        mascotRejectedWords = 0, floatingMessage = null,
                        onInput = {}, onForfeit = {}, onQuickChat = {}, onSubmit = {},
                    )
                }
            }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription("Mage Cat").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithContentDescription("Mage Cat").assertIsDisplayed()
        compose.onNodeWithText("SIRA SENDE").assertIsDisplayed()
        compose.onNodeWithText("KALEM").assertIsDisplayed()
        compose.onNodeWithText("Ğ").assertIsDisplayed()
        compose.onNodeWithText("GÖNDER  ➤").assertIsDisplayed()
        val mascot = compose.onNodeWithContentDescription("Mage Cat").fetchSemanticsNode().boundsInRoot
        val input = compose.onNodeWithText("KALEM").fetchSemanticsNode().boundsInRoot
        val keyboard = compose.onNodeWithText("Ğ").fetchSemanticsNode().boundsInRoot
        assertTrue("Mascot overlaps word input", mascot.bottom < input.top)
        assertTrue("Word input overlaps keyboard", input.bottom < keyboard.top)
        saveScreenshot("compact-arena.png")
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "mascot-verification").apply { mkdirs() }
        File(folder, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
