package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCalmThemeContractTest {
    @Test
    fun requiredAuthGateUsesProfessionalDarkGamePalette() {
        val auth = source("RequiredAuthGate.kt")

        listOf(
            "private object AuthUi",
            "val Background = GameColors.AppBackground",
            "val Surface = GameColors.PrimarySurface",
            "val SurfaceSoft = GameColors.SecondarySurface",
            "val Primary = GameColors.PrimaryBlue",
            "val Turquoise = GameColors.TacticalTurquoise",
            "val Lavender = GameColors.Lavender",
            "val Sand = GameColors.PrestigeGold",
            "val Text = GameColors.TextPrimary",
            "val Muted = GameColors.TextSecondary",
            "val Border = GameColors.Border",
            "val Success = GameColors.PlayGreen",
            "val Warning = GameColors.RewardAmber",
            "val Error = GameColors.Danger",
            "darkColorScheme(",
            "selectedContainerColor = AuthUi.PrimarySoft",
            "containerColor = if (register) AuthUi.Turquoise else AuthUi.Primary",
        ).forEach { token -> assertTrue("Missing professional auth theme token: $token", auth.contains(token)) }

        listOf(
            "Color(0xFFF4F7F2)",
            "Color(0xFFFFFDF7)",
            "Color(0xFFEAF2EE)",
            "Color(0xFF4F725E)",
            "lightColorScheme(",
        ).forEach { legacy -> assertFalse("Legacy light auth color still present: $legacy", auth.contains(legacy)) }
    }

    @Test
    fun passwordRecoveryUsesProfessionalResponsiveShellWithoutWeakeningSecurityFlow() {
        val recovery = source("PasswordRecoveryScreen.kt")

        listOf(
            "GameTheme {",
            "color = GameColors.AppBackground",
            "GameTopBar(",
            "GameSurface(",
            "GamePrimaryButton(",
            "GameTertiaryButton(",
            "verticalScroll(rememberScrollState())",
            ".imePadding()",
            "GameColors.PrimaryBlue",
            "GameColors.PlayGreen",
            "GameColors.Danger",
        ).forEach { token -> assertTrue("Missing professional recovery token: $token", recovery.contains(token)) }

        listOf(
            "Color(0xFFF4F7F2)",
            "Color(0xFFFFFDF7)",
            "Color(0xFF26382F)",
            "Color(0xFF65766D)",
            "SonHarfBrandLogo(",
        ).forEach { legacy -> assertFalse("Legacy recovery UI still present: $legacy", recovery.contains(legacy)) }

        listOf(
            "SupabaseProvider.client.auth.updateUser",
            "RememberedCredentialVault.clear(context)",
            "SonHarfPreferences.setRememberLogin(context, false)",
            "SupabaseProvider.client.auth.signOut()",
            "password.length < 6",
            "password != confirmPassword",
        ).forEach { token -> assertTrue("Recovery security behavior missing: $token", recovery.contains(token)) }
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
