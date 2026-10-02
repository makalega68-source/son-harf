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
        assertTrue(fx.substringAfter("fun typingClick()").substringBefore("fun scoreTick()").contains("buzz("))
        assertTrue(fx.contains("fun wrongWord() { buzz("))
    }

    @Test fun onlyTheTappedCollectionCardShowsProgress() {
        val profile = repoFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt")
        assertTrue(profile.contains("pending = pendingId == item.id"))
        assertFalse(profile.contains("enabled = !loading && !busy,"))
    }

    @Test fun sonHarfLobbyShowsTheProfileFrame() {
        val screen = repoFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
        val lobby = screen.substringAfter("internal fun PremierLobby(").substringBefore("private fun PremierHowToPlay(")
        assertTrue(lobby.contains("ProfilePhotoAvatarWithGender("))
        assertTrue(Regex("frameId\\s*=\\s*SonHarfCosmetics.profileFrameId").containsMatchIn(lobby))
    }

    @Test fun siegeHeaderCardsHaveRoomForTheirScores() {
        val ui = repoFile("app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt")
        assertTrue(ui.contains("modifier = modifier.height(126.dp)"))
        assertTrue(ui.contains("horizontalAlignment = Alignment.CenterHorizontally,\n        ) {\n            Text(\n                label,"))
    }

    @Test fun welcomeDoesNotBlockEntryOnAMediaClock() {
        val intro = repoFile("app/src/main/java/com/sonharf/game/IntroWelcome.kt")
        assertFalse(intro.contains("ChromaKeyVideo"))
        assertFalse(intro.contains("delay("))
        assertTrue(intro.contains("Button(onClick = { onContinue(selected) }"))
    }

    private fun repoFile(path: String): String {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file).readText()
    }
}

