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

        assertTrue(auth.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(auth.contains("sh(\"KELİME KUŞATMASI\", \"WORD SIEGE\")"))
        assertTrue(auth.contains("tactical territory battle"))
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
        assertTrue(auth.contains("Word Siege. If the link does not work"))
        assertTrue(auth.contains("sh(\"6 haneli doğrulama kodu\", \"6-digit verification code\")"))
        assertTrue(auth.contains("sh(\"KODU DOĞRULA\", \"VERIFY CODE\")"))
        assertTrue(auth.contains("sh(\"KODU YENİDEN GÖNDER\", \"RESEND CODE\")"))
        assertTrue(auth.contains("sh(\"E-POSTA ADRESİNİ DEĞİŞTİR\", \"CHANGE EMAIL ADDRESS\")"))
        assertFalse(auth.contains("Son Harf otomatik açılır"))

        assertTrue(startup.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(startup.contains("KELİME KUŞATMASI / WORD SIEGE"))
        assertFalse(startup.contains("SonHarfOfficialLogo(modifier"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
