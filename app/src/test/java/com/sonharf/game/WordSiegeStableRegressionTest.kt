package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeStableRegressionTest {
    @Test fun latestStableSiegeKeepsPracticePanAreaProfilesAndCoreActions() {
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val engine = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()

        assertTrue(experience.contains("WordSiegePracticeScreen"))
        assertTrue(experience.contains("showPass"))
        assertTrue(experience.contains("showExchange"))
        assertTrue(experience.contains("horizontal"))
        assertTrue(experience.contains("ProfileDto"))
        assertTrue(experience.contains("statusBarsPadding"))
        assertTrue(pan.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(practice.contains("showPass"))
        assertTrue(practice.contains("showExchange"))
        assertTrue(engine.contains("exchange"))
        assertTrue(engine.contains("pass"))
        assertTrue(pan.contains("areaScore"))
        assertTrue(pan.contains("totalScore"))
        assertTrue(backend.contains("WordSiege"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
