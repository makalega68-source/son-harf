package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSessionCleanupContractTest {
    @Test fun rememberLoginDefaultsOffAndLogoutClearsEncryptedCredential() {
        val prefs = projectFile("app/src/main/java/com/sonharf/game/SonHarfPreferences.kt").readText()
        val settings = projectFile("app/src/main/java/com/sonharf/game/MainSettingsVipScreen.kt").readText()

        assertTrue(prefs.contains("getBoolean(REMEMBER_LOGIN, false)"))
        assertFalse(prefs.contains("getBoolean(REMEMBER_LOGIN, true)"))
        assertTrue(settings.contains("RememberedCredentialVault.clear(context)"))
        assertTrue(settings.contains("SonHarfPreferences.setRememberLogin(context, false)"))
        assertTrue(settings.indexOf("RememberedCredentialVault.clear(context)") < settings.indexOf("SonHarfPreferences.setRememberLogin(context, false)"))
    }

    @Test fun successfulAccountDeletionClearsLocalLoginMaterial() {
        val profile = projectFile("app/src/main/java/com/sonharf/game/FinalProfileScreen.kt").readText()
        val deleteCall = profile.indexOf("AccountDeletion.deleteCurrentAccount()")
        val clearVault = profile.indexOf("RememberedCredentialVault.clear(context)", deleteCall)
        val clearRemember = profile.indexOf("SonHarfPreferences.setRememberLogin(context, false)", clearVault)

        assertTrue(deleteCall >= 0)
        assertTrue(clearVault > deleteCall)
        assertTrue(clearRemember > clearVault)
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
