package com.sonharf.game

import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeLaunchConfig
import org.junit.Assert.*
import org.junit.After
import org.junit.Test

class PortraitAndInviteLaunchTest {
    @After fun reset() {
        WordSiegeLaunchConfig.pendingGameId = null
        WordSiegeLaunchConfig.pendingGameMode = "classic"
        WordSiegeLaunchConfig.classicTurnHours = 12
    }

    @Test fun portraitAliasesUseTheCorrectSuppliedImageAndNeverAnEmptyFallback() {
        for (gender in listOf("kadın", "kadin", "female", "woman", " KADIN ")) {
            assertEquals(R.drawable.default_profile_female, defaultProfilePortraitRes(gender))
        }
        for (gender in listOf(null, "", "erkek", "male", "man", "diğer")) {
            assertEquals(R.drawable.default_profile_male, defaultProfilePortraitRes(gender))
        }
    }

    @Test fun acceptedClassicRoomIsPassedThroughTheEntranceExactlyOnce() {
        val game = WordSiegeGameDto("accepted-room", "sender", "receiver", "playing", turnDurationHours = 24)
        WordSiegeLaunchConfig.open(game)
        assertEquals(24, WordSiegeLaunchConfig.classicTurnHours)
        assertNull(WordSiegeLaunchConfig.consumeGameId("series"))
        assertEquals("accepted-room", WordSiegeLaunchConfig.consumeGameId("classic"))
        assertNull(WordSiegeLaunchConfig.consumeGameId("classic"))
    }

    @Test fun acceptedSeriesRoomCannotAccidentallyOpenInTheClassicLobby() {
        val game = WordSiegeGameDto("quick-room", "sender", "receiver", "playing", gameMode = "series")
        WordSiegeLaunchConfig.open(game)
        assertEquals("series", WordSiegeLaunchConfig.pendingGameMode)
        assertNull(WordSiegeLaunchConfig.consumeGameId("classic"))
        assertEquals("quick-room", WordSiegeLaunchConfig.consumeGameId("series"))
    }
}
