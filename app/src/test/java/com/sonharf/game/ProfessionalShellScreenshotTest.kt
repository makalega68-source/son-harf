package com.sonharf.game

import androidx.compose.foundation.background
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

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun matchCenterActive_360x800() = captureMatchCenter("match_center_active_360x800", MatchCenterTab.ACTIVE)

    @Test
    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    fun matchCenterFinished_390x844() = captureMatchCenter("match_center_finished_390x844", MatchCenterTab.FINISHED)

    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi")
    fun matchCenterInvites_412x915() = captureMatchCenter("match_center_invites_412x915", MatchCenterTab.INVITES)

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun matchCenterEmpty_360x800() = captureMatchCenter("match_center_empty_360x800", MatchCenterTab.ACTIVE, empty = true)

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun productDetailBuy_360x800() = captureProductDetail("product_detail_buy_360x800", owned = false, equipped = false, balance = 150)

    @Test
    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    fun productDetailEquip_390x844() = captureProductDetail("product_detail_equip_390x844", owned = true, equipped = false, balance = 900)

    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi")
    fun productDetailEquipped_412x915() = captureProductDetail("product_detail_equipped_412x915", owned = true, equipped = true, balance = 900)

    private fun captureProductDetail(name: String, owned: Boolean, equipped: Boolean, balance: Int) {
        val product = com.sonharf.game.data.ShopItemDto(
            id = "keyboard_crystal",
            kind = "keyboard_theme",
            nameTr = "Kristal Klavye",
            nameEn = "Crystal Keyboard",
            descriptionTr = "Buz mavisi harf taşları ve yumuşak parıltı.",
            descriptionEn = "Ice-blue letter tiles with a soft glow.",
            diamondPrice = 210,
        )
        compose.setContent {
            GameTheme {
                Box(Modifier.fillMaxSize().background(GameColors.ElevatedBackground)) {
                    StoreProductDetailContent(product, owned, equipped, proActive = false, balance = balance, busy = false, onBuy = {}, onEquip = {}, onPro = {})
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun captureMatchCenter(name: String, tab: MatchCenterTab, empty: Boolean = false) {
        compose.setContent {
            GameTheme {
                Box(
                    Modifier.fillMaxSize().background(GameColors.AppBackground),
                ) {
                    MatchCenterContent(
                        tab = tab,
                        onTab = {},
                        active = if (empty) emptyList() else sampleActive,
                        finished = if (empty) emptyList() else sampleFinished,
                        invites = if (empty) emptyList() else sampleInvites,
                        loading = false,
                        busy = false,
                        notice = null,
                        onBack = {},
                        onRefresh = {},
                        onOpenMatch = {},
                        onQuickMatch = {},
                        onRematch = {},
                        onRespond = { _, _ -> },
                        now = java.time.Instant.parse("2026-09-24T12:00:00Z"),
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
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
        fun match(
            id: String,
            rival: String,
            myTurn: Boolean,
            waiting: Boolean = false,
            result: String? = null,
        ) = SiegeMatchCard(
            gameId = id,
            rivalId = if (waiting) null else "r-$id",
            rivalName = rival,
            rivalRating = if (waiting) null else 1284,
            rivalAvatar = null,
            waitingForRival = waiting,
            myTurn = myTurn,
            myWordScore = 42,
            myAreaScore = 36,
            rivalWordScore = 57,
            rivalAreaScore = 14,
            myMapControl = 29,
            rivalMapControl = 11,
            lastMoveAt = "2026-09-24T09:30:00Z",
            result = result,
        )

        val sampleActive = listOf(
            match("1", "Deniz", myTurn = true),
            match("2", "Ece", myTurn = false),
            match("3", "", myTurn = false, waiting = true),
        )
        val sampleFinished = listOf(
            match("4", "Mert", myTurn = false, result = "win"),
            match("5", "Selin Yıldırım Uzunsoyadlı", myTurn = false, result = "loss"),
        )
        val sampleInvites = listOf(SiegeInviteCard("i1", "Kaan", "tr"), SiegeInviteCard("i2", "Zeynep", "en"))

        // Layout fixture for rendering only; the app reads these values from the server.
        val sampleSummary = PlayHubSiegeSummary(
            activeGames = 3,
            yourTurnGames = 2,
            lastRivalId = "rival",
            lastRivalName = "Deniz",
        )
    }
}
