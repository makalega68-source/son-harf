package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCalmThemeContractTest {
    @Test
    fun requiredAuthGateUsesCalmLayeredPaletteWithoutLegacyBrightBlueLilac() {
        val auth = source("RequiredAuthGate.kt")

        listOf(
            "private object AuthUi",
            "val Background = Color(0xFF171C1B)",
            "val Surface = Color(0xFF222827)",
            "val SurfaceSoft = Color(0xFF1D2322)",
            "val Primary = Color(0xFF32845E)",
            "val SoftBlue = Color(0xFFC5AA73)",
            "val Turquoise = Color(0xFF40A878)",
            "val Lavender = Color(0xFFC5AA73)",
            "val Sand = Color(0xFFC5AA73)",
            "val Border = Color(0xFF5C5239)",
            "selectedContainerColor = AuthUi.PrimarySoft",
            "containerColor = if (register) AuthUi.Turquoise else AuthUi.Primary",
        ).forEach { token -> assertTrue("Missing calm auth theme token: $token", auth.contains(token)) }

        listOf(
            "Color(0xFF1769E0)",
            "Color(0xFF6A4FD8)",
            "Color(0xFF8CB8F3)",
            "Color(0xFFB8D4F7)",
            "Color(0xFF173B77)",
        ).forEach { legacy -> assertFalse("Legacy auth color still present: $legacy", auth.contains(legacy)) }
    }

    @Test
    fun authBehaviorContractsRemainPresentDuringVisualRetheme() {
        val auth = source("RequiredAuthGate.kt")

        listOf(
            "signInWith(Email)",
            "signUpWith(Email",
            "verifyEmailOtp(",
            "resetPasswordForEmail(",
            "RememberedCredentialVault.load(context)",
            "RememberedCredentialVault.save(context, email, password)",
            "hasVerifiedMembershipSession()",
            "complete_profile_identity_v2",
        ).forEach { token -> assertTrue("Auth behavior contract missing: $token", auth.contains(token)) }
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
