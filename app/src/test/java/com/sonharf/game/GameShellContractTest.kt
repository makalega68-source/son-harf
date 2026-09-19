package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameShellContractTest {
    @Test fun activeShellOwnsTheThreePlayableDestinations() {
        val stable = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        assertTrue(stable.contains("PremiumUnifiedProApp"))
        assertTrue(shell.contains("OnlineGameScreenV6"))
        assertTrue(shell.contains("WordSiegeExperienceScreen"))
        assertTrue(shell.contains("LetterLadderGameScreen"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
