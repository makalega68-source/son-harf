package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeProfileRegressionTest {
    @Test
    fun practiceProfilesUseSharedRendererAndStableGenderCorrectBotPool() {
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val profileRuntime = projectFile("app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()

        listOf("Mesut", "İmran", "Ayaz", "Eren", "Esin", "Can", "Deniz", "Mert", "Selin", "Burak", "Elif", "Kerem", "Derya", "Arda", "Zeynep", "Emre", "Ceren").forEach {
            assertTrue("Missing bot name: $it", practice.contains("PracticeBotProfile(\"$it\""))
        }
        listOf("İmran", "Esin", "Selin", "Elif", "Derya", "Zeynep", "Ceren").forEach {
            assertTrue("Female bot mismatch: $it", practice.contains("PracticeBotProfile(\"$it\", \"kadın\")"))
        }

        assertTrue(practice.contains("val backend = remember { runCatching { OnlineGameBackend() }.getOrNull() }"))
        assertTrue(practice.contains("playerProfile = runCatching { b.getProfile(me) }.getOrNull()"))
        assertTrue(practice.contains("avatarPath = playerProfile?.avatarPath"))
        assertTrue(practice.contains("gender = playerProfile?.gender"))
        assertTrue(practice.contains("avatarVisible = playerProfile?.avatarVisibility != \"hidden\""))
        assertTrue(practice.contains("var botProfile by remember { mutableStateOf(WordSiegePracticeBots.random()) }"))
        assertTrue(practice.contains("botProfile = WordSiegePracticeBots.random()"))
        assertTrue(practice.contains("ProfilePhotoAvatarWithGender("))
        assertTrue(practice.contains("Text(\"BOT\""))

        assertTrue(profileRuntime.contains("SyntheticProfilePortrait"))
        assertTrue(profileRuntime.contains("Icons.Rounded.Face"))
        assertTrue(profileRuntime.contains("FramelessGenderSymbol"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
