package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayKeyboardAvatarContractTest {

    @Test
    fun wordHuntKeepsKeyboardAndCompactsForIme() {
        val source = projectFile("app/src/main/java/com/sonharf/game/DailyCipherScreen.kt").readText()

        assertTrue(source.contains("LocalSoftwareKeyboardController"))
        assertTrue(source.contains("guessFocusRequester.requestFocus()"))
        assertTrue(source.contains("imeVisible"))
        assertTrue(source.contains("readOnly = busy || s == null"))
        assertTrue(source.contains("height(38.dp)"))
    }

    @Test
    fun wordArenaKeepsKeyboardAndShowsBothPlayerPhotos() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordArenaScreen.kt").readText()

        assertTrue(source.contains("inputFocusRequester.requestFocus()"))
        assertTrue(source.contains("showKeyboardOnFocus = true"))
        assertTrue(source.contains("ProfilePhotoAvatarWithGender("))
        assertTrue(source.contains("myAvatarPath"))
        assertTrue(source.contains("opponentAvatarPath"))
    }

    @Test
    fun dailyArenaKeepsKeyboardAndShowsRankingPhotos() {
        val source = projectFile("app/src/main/java/com/sonharf/game/DailyArenaScreen.kt").readText()

        assertTrue(source.contains("inputFocusRequester.requestFocus()"))
        assertTrue(source.contains("leaderboardProfiles"))
        assertTrue(source.contains("ProfilePhotoAvatar("))
        assertTrue(source.contains("Modifier.fillMaxSize().imePadding()"))
    }

    @Test
    fun teamArenaKeepsKeyboardAndShowsMemberPhotosWithoutRefetchingEveryPoll() {
        val source = projectFile("app/src/main/java/com/sonharf/game/TeamArenaScreen.kt").readText()

        assertTrue(source.contains("inputFocusRequester.requestFocus()"))
        assertTrue(source.contains("memberProfiles"))
        assertTrue(source.contains("ProfilePhotoAvatar("))
        assertTrue(source.contains("if (!nextProfiles.containsKey(member.userId))"))
        assertTrue(source.contains("Modifier.fillMaxSize().imePadding()"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(
            File(path),
            File("../$path"),
        )
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
