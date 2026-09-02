package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeStableRegressionTest {
    @Test fun latestStableSiegeKeepsPracticePanProfilesActionsAndFinalRules() {
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val engine = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()
        val sharedDictionary = projectFile("app/src/main/java/com/sonharf/game/data/SharedDictionaryService.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()
        val rules = projectFile("app/src/main/java/com/sonharf/game/WordSiegeFinalRules.kt").readText()

        assertTrue(experience.contains("WordSiegePracticeScreen"))
        assertTrue(experience.contains("showPass"))
        assertTrue(experience.contains("showExchange"))
        assertTrue(experience.contains("WordSiegeFinalRules.detectOrientation"))
        assertTrue(experience.contains("ProfileDto"))
        assertTrue(experience.contains("statusBarsPadding"))
        assertTrue(pan.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(pan.contains("Yön otomatik algılanır"))
        assertTrue(practice.contains("showPass"))
        assertTrue(practice.contains("showExchange"))
        assertTrue(practice.contains("Yön otomatik algılanır"))
        assertTrue(practice.contains("BoxWithConstraints"))
        assertTrue(practice.contains("navigationBarsPadding"))
        assertTrue(practice.contains("val boardSize = minOf(maxWidth"))
        assertTrue(!practice.contains("LazyColumn("))
        assertTrue(practice.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(practice.contains("backend.getProfile(me)"))
        listOf("Mesut", "İmran", "Ayaz", "Eren", "Esin", "Can", "Deniz", "Mert", "Selin", "Burak", "Elif", "Kerem", "Derya", "Arda", "Zeynep", "Emre", "Ceren").forEach { assertTrue(practice.contains("\"$it\"")) }
        listOf("İmran", "Esin", "Selin", "Elif", "Derya", "Zeynep", "Ceren").forEach { name -> assertTrue(practice.contains("PracticeBotProfile(\"$name\", \"kadın\")")) }
        assertTrue(engine.contains("exchange"))
        assertTrue(engine.contains("pass"))
        assertTrue(engine.contains("SharedDictionaryService.isValidWordBlocking"))
        assertTrue(engine.contains("SharedDictionaryService.practiceCandidates"))
        assertTrue(!engine.contains("practiceDictionary"))
        assertTrue(sharedDictionary.contains("get_dictionary_snapshot_v1"))
        assertTrue(sharedDictionary.contains("ConcurrentHashMap"))
        assertTrue(rules.contains("CUBE_TRANSFER_POINTS: Int = 2"))
        assertTrue(rules.contains("wordScore + earnedCubePoints - opponentEarnedCubePoints"))
        assertTrue(backend.contains("WordSiege"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
