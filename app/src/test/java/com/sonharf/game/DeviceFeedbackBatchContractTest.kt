package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeviceFeedbackBatchContractTest {
    @Test fun gamesVibrateWhenTheSettingIsOn() {
        // Without the permission every vibration call failed silently.
        assertTrue(repoFile("app/src/main/AndroidManifest.xml").contains("android.permission.VIBRATE"))
        val fx = repoFile("app/src/main/java/com/sonharf/game/SonHarfSoundFx.kt")
        assertTrue(fx.contains("if (!SonHarfPreferences.vibrationEnabled(context)) return"))
        assertTrue(fx.contains("fun typingClick() { buzz("))
        assertTrue(fx.contains("fun wrongWord() { buzz("))
    }

    @Test fun onlyTheTappedCollectionCardShowsProgress() {
        val profile = repoFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt")
        assertTrue(profile.contains("pending = pendingId == item.id"))
        assertFalse(profile.contains("enabled = !loading && !busy,"))
    }

    @Test fun sonHarfLobbyShowsTheProfileFrame() {
        val screen = repoFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
        assertTrue(screen.contains("size = 52.dp,\n                                accent = Color.White,\n                                showGenderBadge = false,\n                                frameId = SonHarfCosmetics.profileFrameId,"))
    }

    @Test fun siegeHeaderCardsHaveRoomForTheirScores() {
        val ui = repoFile("app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt")
        assertTrue(ui.contains("modifier = modifier.height(110.dp)"))
        assertTrue(ui.contains("horizontalAlignment = Alignment.CenterHorizontally,\n        ) {\n            Text(\n                label,"))
    }

    @Test fun introClipIsSmallerWithAnEdgeColourFill() {
        val intro = repoFile("app/src/main/java/com/sonharf/game/IntroWelcome.kt")
        assertTrue(intro.contains("scale = INTRO_VIDEO_SCALE,"))
        assertTrue(intro.contains("matte = true,"))
        val video = repoFile("app/src/main/java/com/sonharf/game/ChromaKeyVideo.kt")
        assertTrue(video.contains("private fun sampleMatte(sx: Float, sy: Float)"))
    }

    private fun repoFile(path: String): String {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file).readText()
    }
}
