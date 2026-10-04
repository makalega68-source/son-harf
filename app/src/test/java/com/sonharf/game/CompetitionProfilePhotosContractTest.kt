package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionProfilePhotosContractTest {

    @Test
    fun competitionSurfacesUseProfilePhotosAndCacheProfiles() {
        val source = projectFile("app/src/main/java/com/sonharf/game/CompetitionHubScreen.kt").readText()

        assertTrue(source.contains("memberProfiles"))
        assertTrue(source.contains("leaderboardProfiles"))
        assertTrue(source.contains("ProfilePhotoAvatar("))
        assertTrue(source.contains("if (!nextProfiles.containsKey(member.userId))"))
        assertTrue(source.contains("if (!nextProfiles.containsKey(row.userId))"))

        // Rival history moved to its own screen; it keeps the profile cache and only fetches unseen rivals.
        val rivals = projectFile("app/src/main/java/com/sonharf/game/LatestRivalsScreen.kt").readText()
        assertTrue(source.contains("LatestRivalsScreen(it)"))
        assertTrue(rivals.contains("FramedProfilePhotoAvatar("))
        assertTrue(rivals.contains("filter { userId -> !nextProfiles.containsKey(userId) }"))
        assertTrue(rivals.contains(".distinctBy { it.opponentId }"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
