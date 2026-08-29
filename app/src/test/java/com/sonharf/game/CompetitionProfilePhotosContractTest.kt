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
        assertTrue(source.contains("playerProfiles"))
        assertTrue(source.contains("ProfilePhotoAvatar("))
        assertTrue(source.contains("if (!nextProfiles.containsKey(member.userId))"))
        assertTrue(source.contains("if (!nextProfiles.containsKey(row.userId))"))
        assertTrue(source.contains("if (!nextProfiles.containsKey(userId))"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
