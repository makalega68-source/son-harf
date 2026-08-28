package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAssetContractTest {
    @Test
    fun warmBeginningsIsTheOnlyBundledAudioFile() {
        val rawDir = listOf(
            File("src/main/res/raw"),
            File("app/src/main/res/raw"),
        ).firstOrNull(File::isDirectory)

        assertNotNull("Android raw resource directory is missing", rawDir)

        val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
        val audioFiles = requireNotNull(rawDir)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .sortedBy { it.name }

        assertEquals(listOf("warm_beginnings.mp3"), audioFiles.map { it.name })
        assertTrue("Warm Beginnings audio looks empty", audioFiles.single().length() > 1_000_000L)
    }

    @Test
    fun backgroundMusicRuntimeUsesOnlyWarmBeginnings() {
        val source = listOf(
            File("src/main/java/com/sonharf/game/SonHarfBackgroundMusic.kt"),
            File("app/src/main/java/com/sonharf/game/SonHarfBackgroundMusic.kt"),
        ).firstOrNull(File::isFile)

        assertNotNull("Background music runtime is missing", source)
        val text = requireNotNull(source).readText()
        assertTrue(text.contains("R.raw.warm_beginnings"))
    }
}
