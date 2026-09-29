package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLanguageBrandContractTest {
    @Test fun authEntryUsesCurrentProductBrandAndBilingualFormCopy() {
        val auth = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val startup = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val confirmSignup = projectFile("supabase/templates/confirm-signup.html").readText()

        assertTrue(auth.contains("\"KELİME TAHTI\""))
        assertTrue(auth.contains("R.drawable.kelime_tahti_brand_logo"))
        assertFalse(auth.contains("R.drawable.son_harf_gold_teal_logo"))
        assertFalse(auth.contains("Continue the Word, Beat Your Rival"))

        assertTrue(auth.contains("sh(\"ÜYE OL\", \"REGISTER\")"))
        assertTrue(auth.contains("sh(\"GİRİŞ YAP\", \"SIGN IN\")"))
        assertTrue(auth.contains("sh(\"Oyuncu adı\", \"Player name\")"))
        assertTrue(auth.contains("sh(\"E-posta\", \"Email\")"))
        assertTrue(auth.contains("sh(\"Şifre\", \"Password\")"))
        assertTrue(auth.contains("sh(\"Erkek\", \"Male\")"))
        assertTrue(auth.contains("sh(\"Kadın\", \"Female\")"))
        assertTrue(auth.contains("sh(\"Diğer\", \"Other\")"))
        assertTrue(auth.contains("Incorrect email or password"))
        assertTrue(auth.contains("Verification email sent"))

        assertTrue(auth.contains("sh(\"E-postanı doğrula\", \"Verify your email\")"))
        assertTrue(auth.contains("Word Throne. If the link does not work"))
        assertTrue(auth.contains("sh(\"Doğrulama kodu\", \"Verification code\")"))
        assertTrue(auth.contains("private const val OTP_MIN_LENGTH = 6"))
        assertTrue(auth.contains("private const val OTP_MAX_LENGTH = 8"))
        assertTrue(auth.contains("otpCode.length !in OTP_MIN_LENGTH..OTP_MAX_LENGTH"))
        assertTrue(auth.contains("it.filter(Char::isDigit).take(OTP_MAX_LENGTH)"))
        assertTrue(auth.contains("otpCode.length in OTP_MIN_LENGTH..OTP_MAX_LENGTH"))
        assertFalse(auth.contains("6 haneli"))
        assertFalse(auth.contains("6-digit"))
        assertFalse(auth.contains("take(6)"))
        assertFalse(auth.contains("otpCode.length == 6"))
        assertTrue(auth.contains("sh(\"KODU DOĞRULA\", \"VERIFY CODE\")"))
        assertTrue(auth.contains("sh(\"KODU YENİDEN GÖNDER\", \"RESEND CODE\")"))
        assertTrue(auth.contains("sh(\"E-POSTA ADRESİNİ DEĞİŞTİR\", \"CHANGE EMAIL ADDRESS\")"))
        assertFalse(auth.contains("Son Harf otomatik açılır"))

        assertTrue(confirmSignup.contains("Kelime Tahtı"))
        assertTrue(confirmSignup.contains("KELİME TAHTI"))
        assertTrue(confirmSignup.contains("şu doğrulama kodunu gir"))
        assertFalse(confirmSignup.contains("Son Harf"))
        assertFalse(confirmSignup.contains("6 haneli"))

        // The first-run brand is the Kelime Tahtı logo that drops into the welcome clip.
        assertTrue(File("src/main/java/com/sonharf/game/IntroWelcome.kt").readText().contains("R.drawable.kelime_tahti_brand_logo"))
        assertFalse(startup.contains("SonHarfOfficialLogo(modifier"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
