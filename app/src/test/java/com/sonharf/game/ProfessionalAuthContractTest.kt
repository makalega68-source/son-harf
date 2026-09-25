package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalAuthContractTest {
    @Test
    fun `required auth keeps professional shell and verified membership flow`() {
        val source = repoFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()

        assertTrue(source.contains("GameColors.AppBackground"))
        assertTrue(source.contains("GameColors.PrimaryBlue"))
        assertTrue(source.contains("GameColors.TacticalTurquoise"))
        assertTrue(source.contains("kelime_kusatma_logo_hd"))
        assertTrue(source.contains("KELİME KUŞATMASI"))
        assertTrue(source.contains("verticalScroll(scrollState)"))
        assertTrue(source.contains(".imePadding()"))
        assertTrue(source.contains("SupabaseProvider.client.auth.signInWith(Email)"))
        assertTrue(source.contains("SupabaseProvider.client.auth.signUpWith(Email"))
        assertTrue(source.contains("SupabaseProvider.client.auth.verifyEmailOtp"))
        assertTrue(source.contains("SupabaseProvider.client.auth.resetPasswordForEmail"))
        assertTrue(source.contains("complete_profile_identity_v2"))
        assertFalse(source.contains("MainUi."))
        assertFalse(source.contains("SonHarfTheme."))
        assertFalse(source.contains("Monster"))
    }

    @Test
    fun `password recovery keeps professional theme and secure session cleanup`() {
        val source = repoFile("app/src/main/java/com/sonharf/game/PasswordRecoveryScreen.kt").readText()

        assertTrue(source.contains("GameTheme"))
        assertTrue(source.contains("GameColors.AppBackground"))
        assertTrue(source.contains("verticalScroll(rememberScrollState())"))
        assertTrue(source.contains(".imePadding()"))
        assertTrue(source.contains("SupabaseProvider.client.auth.updateUser"))
        assertTrue(source.contains("RememberedCredentialVault.clear(context)"))
        assertTrue(source.contains("SupabaseProvider.client.auth.signOut()"))
        assertFalse(source.contains("MainUi."))
        assertFalse(source.contains("SonHarfTheme."))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
