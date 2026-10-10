package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuPerformanceContractTest {
    private fun source(name: String) = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun menuListsNeverDownloadBoardsBagsOrRacks() {
        val backend = source("data/WordSiegeBackend.kt")
        val columns = backend.substringAfter("private val wordSiegeSummaryColumns = Columns.list(").substringBefore(")")
        listOf("\"board\"", "\"bag\"", "\"player_one_rack\"", "\"player_two_rack\"").forEach { assertFalse(it, columns.contains(it)) }
        assertTrue(columns.contains("\"player_one_word_score\"") && columns.contains("\"turn_deadline\""))
        assertTrue(backend.contains("isIn(\"status\", listOf(\"waiting\", \"playing\"))"))
        assertTrue(backend.contains("limit(finishedLimit)"))
        // Menus use the summaries; only the board screens keep the full rows.
        listOf("MyGamesScreen.kt", "HomeSessions.kt", "LatestRivalsScreen.kt", "ProfileRecordsSection.kt").forEach { screen ->
            val text = source(screen)
            assertTrue(screen, text.contains("getWordSiegeGameSummaries(\"classic\""))
            assertFalse(screen, text.contains("backend.getWordSiegeGames()"))
        }
        assertTrue(source("HomeSessions.kt").contains("getWordSiegeGameSummaries(\"classic\", finishedLimit = 0)"))
        assertTrue(source("WordSiegeExperience.kt").contains("backend.getWordSiegeGames()"))
    }

    @Test fun profilesAndFramesAreBatchedAndShared() {
        val online = source("data/OnlineGameBackend.kt")
        assertTrue(online.contains("filter { isIn(\"id\", chunk) }"))
        assertTrue(online.contains(".chunked(50)"))
        assertTrue(source("SharedRequests.kt").contains("inFlight.computeIfAbsent(key)"))
        assertTrue(source("ProfileFrameCollection.kt").contains("return requests.get(userId) { fetch(userId) }"))
        assertTrue(source("ProfilePhotoRuntime.kt").contains("PlayerIdentityCache.get(it)"))
        assertTrue(source("MyGamesScreen.kt").contains("backend.getProfilesParallel(missing)"))
    }

    @Test fun signaturesUseDistinctReadableTypefacesInMatchCards() {
        val style = source("CosmeticRuntime.kt").substringAfter("internal fun premiumNameStyle(")
        val families = listOf("name_aurelia", "name_amethyst", "name_sapphire", "name_cyan").map { id ->
            style.substringAfter("\"$id\" -> system(\"").substringBefore("\"")
        }
        assertEquals(4, families.distinct().size)
        assertTrue(source("WordSiegeGameUi.kt").contains("style = premiumNameStyle(nameStyleId)"))
        assertTrue(source("WordSiegePanMatch.kt").contains("nameStyleId = SonHarfCosmetics.nameStyleId"))
        assertTrue(source("WordSiegePracticeScreen.kt").contains("nameStyleId = if (isBot) null else SonHarfCosmetics.nameStyleId"))
        val duel = source("PremierWordDuelScreen.kt")
        assertTrue(duel.contains("nameStyleId = SonHarfCosmetics.nameStyleId,"))
        assertEquals(2, Regex("premiumNameStyle\\(SonHarfCosmetics\\.nameStyleId\\)").findAll(duel).count())
    }

    @Test fun chatSurfacesAreSecureAndThroneBackReturnsToThrone() {
        assertTrue(source("PremierWordDuelScreen.kt").contains("ModalBottomSheetProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn)"))
        listOf("WordSiegeSeriesScreen.kt", "WordSiegePracticeScreen.kt", "WordSiegeExperience.kt").forEach {
            assertTrue(it, source(it).contains("DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn)"))
        }
        assertTrue(source("ChatDialogTitle.kt").contains("SecureChatContent()"))
        val hub = source("CompetitionHubScreen.kt")
        assertTrue(hub.contains("androidx.activity.compose.BackHandler { showThrone = true }"))
        assertFalse(hub.contains("BackHandler(enabled = tab != 0)"))
    }
}
