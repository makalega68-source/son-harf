package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the professional shell at the three reference phone sizes. The PNGs are written to
 * app/build/outputs/roborazzi and uploaded by CI for visual review; the test fails if a
 * screen cannot be composed or drawn at a size.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProfessionalShellScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun playTab_360x800() = capturePlayTab("play_tab_360x800")

    @Test
    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    fun playTab_390x844() = capturePlayTab("play_tab_390x844")

    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi")
    fun playTab_412x915() = capturePlayTab("play_tab_412x915")

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun playTabNewPlayer_360x800() = capturePlayTab("play_tab_new_player_360x800", PlayHubSiegeSummary())

    @Test
    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    fun playTabEnglish_390x844() {
        val previous = SonHarfUiState.language
        SonHarfUiState.language = "en"
        try {
            capturePlayTab("play_tab_en_390x844")
        } finally {
            SonHarfUiState.language = previous
        }
    }

    private fun capturePlayTab(name: String, summary: PlayHubSiegeSummary = sampleSummary) {
        compose.setContent { ShellFrame { PlayHubPreview(summary) } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Composable
    private fun ShellFrame(content: @Composable () -> Unit) {
        GameTheme {
            Scaffold(
                containerColor = GameColors.AppBackground,
                bottomBar = { GameBottomNavigation(selected = GameMainTab.PLAY, onSelect = {}) },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { content() }
            }
        }
    }

    @Composable
    private fun PlayHubPreview(summary: PlayHubSiegeSummary) {
        PlayHubContent(
            summary = summary,
            notice = null,
            rematchBusy = false,
            onQuickMatch = {},
            onPractice = {},
            onMyGames = {},
            onFriends = {},
            onPrivateRoom = {},
            onRematch = {},
            onLastLetter = {},
            onLetterPath = {},
        )
    }

    private companion object {
        // Layout fixture for rendering only; the app reads these values from the server.
        val sampleSummary = PlayHubSiegeSummary(
            activeGames = 3,
            yourTurnGames = 2,
            lastRivalId = "rival",
            lastRivalName = "Deniz",
        )
    }
}
