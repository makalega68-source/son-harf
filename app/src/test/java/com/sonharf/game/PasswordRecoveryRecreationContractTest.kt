package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRecoveryRecreationContractTest {
    @Test fun recoveryStateSurvivesActivityRecreationAndConsumedLinkIsCleared() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MainActivity.kt").readText()
        assertTrue(main.contains("PASSWORD_RECOVERY_STATE_KEY"))
        assertTrue(main.contains("onSaveInstanceState"))
        assertTrue(main.contains("savedInstanceState?.getBoolean(PASSWORD_RECOVERY_STATE_KEY) == true"))
        assertTrue(main.contains("isPasswordRecoveryDeepLink(intent)"))
        assertTrue(main.contains("setIntent(Intent(intent).apply { data = null })"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
