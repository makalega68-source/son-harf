package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeStableRegressionTest {
    @Test fun latestStableSiegeKeepsDictionaryProfilesActionsBoardAndNewConquestRules() {
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val engine = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()
        val sharedDictionary = projectFile("app/src/main/java/com/sonharf/game/data/SharedDictionaryService.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()
        val zoneRules = projectFile("app/src/main/java/com/sonharf/game/WordSiegeZoneRules.kt").readText()

        assertTrue(experience.contains("WordSiegePracticeScreen"))
        assertTrue(experience.contains("showPass"))
        assertTrue(experience.contains("showExchange"))
        assertTrue(experience.contains("WordSiegeFinalRules.detectOrientation"))
        assertTrue(experience.contains("ProfileDto"))
        assertTrue(experience.contains("statusBarsPadding"))

        assertTrue(pan.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(pan.contains("WordSiegeLiveRivalryBar"))
        assertTrue(pan.contains("WordSiegeTempoBanner"))
        assertTrue(pan.contains("WordSiegePracticeBoard"))
        assertTrue(pan.contains("Torba ${'$'}{game.bag.length}"))
        assertFalse(pan.contains("SÜRE YOK"))
        assertFalse(pan.contains("earnedCubePoints"))

        assertTrue(practice.contains("showPass"))
        assertTrue(practice.contains("showExchange"))
        assertTrue(practice.contains("WordSiegeLiveRivalryBar"))
        assertTrue(practice.contains("WordSiegeTempoBanner"))
        assertTrue(practice.contains("Torba ${'$'}{state.bag.length}"))

        assertTrue(engine.contains("exchange"))
        assertTrue(engine.contains("pass"))
        assertTrue(engine.contains("SharedDictionaryService.isValidWordBlocking"))
        assertTrue(engine.contains("SharedDictionaryService.practiceCandidates"))
        assertTrue(engine.contains("WordSiegeZoneRules.claimZones"))
        assertTrue(engine.contains("WordSiegeZoneRules.wordScore"))
        assertTrue(engine.contains("botHandicapFactor"))
        assertFalse(engine.contains("practiceDictionary"))

        assertTrue(sharedDictionary.contains("get_dictionary_snapshot_v3"))
        assertTrue(sharedDictionary.contains("MIN_CANONICAL_LENGTH = 2"))
        assertTrue(sharedDictionary.contains("ConcurrentHashMap"))

        assertTrue(zoneRules.contains("NormalZonePoints = 2"))
        assertTrue(zoneRules.contains("FortressZonePoints = 4"))
        assertTrue(zoneRules.contains("ConquestMeterMax = 3"))
        assertTrue(zoneRules.contains("TurnSeconds = 45"))
        assertTrue(backend.contains("WordSiege"))
        assertTrue(backend.contains("player_one_conquest_meter"))
        assertTrue(backend.contains("turn_deadline"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
