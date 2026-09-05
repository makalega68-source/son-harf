package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileImageCoverageRegressionTest {
    @Test
    fun coreCompetitiveSurfacesKeepRealPhotoAndSafeFallbackSupport() {
        val runtime = projectFile("app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()
        val duel = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val competition = projectFile("app/src/main/java/com/sonharf/game/CompetitionHubScreen.kt").readText()

        assertTrue(runtime.contains("ProfilePhotoAvatar"))
        assertTrue(runtime.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(runtime.contains("if (bitmap != null)"))
        assertTrue(runtime.contains("name.take(1).uppercase()"))
        assertTrue(duel.contains("playerAvatarPath"))
        assertTrue(duel.contains("opponentAvatarPath"))
        assertTrue(duel.contains("FramedProfilePhotoAvatar"))
        assertTrue(duel.contains("playerFrameId"))
        assertTrue(duel.contains("ProfilePhotoAvatarRectWithGender"))
        assertTrue(siege.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(siege.contains("avatarVisibility"))
        assertTrue(competition.contains("ProfilePhotoAvatar"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
