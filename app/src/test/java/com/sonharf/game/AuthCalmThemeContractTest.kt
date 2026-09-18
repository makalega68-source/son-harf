package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCalmThemeContractTest {
    @Test
    fun requiredAuthGateUsesPremiumCanvaPaletteWithoutLegacyCalmThemeColors() {
        val auth = source("RequiredAuthGate.kt")

        listOf(
            "private object AuthUi",
            "val Background: Color get() = SonHarfTheme.Background",
            "val Surface: Color get() = SonHarfTheme.Surface",
            "val SurfaceSoft: Color get() = SonHarfTheme.SurfaceSecondary",
            "val Primary: Color get() = SonHarfTheme.Primary",
            "val SoftBlue: Color get() = SonHarfTheme.SoftBlue",
            "val Turquoise: Color get() = SonHarfTheme.Turquoise",
            "val Lavender: Color get() = SonHarfTheme.Purple",
            "val Sand: Color get() = SonHarfTheme.Sand",
            "val Border: Color get() = SonHarfTheme.Border",
            "selectedContainerColor = AuthUi.PrimarySoft",
            "containerColor = if (register) AuthUi.Turquoise else AuthUi.Primary",
            "SonHarfLeafBackdrop(Modifier.matchParentSize())",
        ).forEach { token -> assertTrue("Missing premium auth theme token: $token", auth.contains(token)) }

        listOf(
            "Color(0xFF4F725E)",
            "Color(0xFF4A6E83)",
            "Color(0xFF477B78)",
            "Color(0xFF7B6B95)",
            "Color(0xFFD7C49F)",
            "Color(0xFFCCD8D1)",
        ).forEach { legacy -> assertFalse("Legacy calm auth color still present: $legacy", auth.contains(legacy)) }
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
