package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRecoveryContractTest {
    @Test fun resetEmailLinkRequiresDedicatedPasswordUpdateFlow() {
        val authGate = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MainActivity.kt").readText()
        val recovery = projectFile("app/src/main/java/com/sonharf/game/PasswordRecoveryScreen.kt").readText()
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertTrue(authGate.contains("resetPasswordForEmail"))
        assertTrue(authGate.contains("redirectUrl = \"sonharf://auth\""))
        assertTrue(manifest.contains("android:scheme=\"sonharf\" android:host=\"auth\""))

        assertTrue(main.contains("isPasswordRecoveryDeepLink"))
        assertTrue(main.contains("queryType.equals(\"recovery\""))
        assertTrue(main.contains("passwordRecoveryRequested = true"))
        assertTrue(main.contains("PasswordRecoveryScreen"))
        assertTrue(main.contains("must never be promoted to a normal remembered login"))

        assertTrue(recovery.contains("SupabaseProvider.client.auth.updateUser"))
        assertTrue(recovery.contains("this.password = newPassword"))
        assertTrue(recovery.contains("RememberedCredentialVault.clear(context)"))
        assertTrue(recovery.contains("SonHarfPreferences.setRememberLogin(context, false)"))
        assertTrue(recovery.contains("SupabaseProvider.client.auth.signOut()"))
        assertFalse(recovery.contains("setRememberLogin(context, true"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
