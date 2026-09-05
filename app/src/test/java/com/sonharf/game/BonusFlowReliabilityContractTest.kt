package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonusFlowReliabilityContractTest {

    @Test
    fun bonusSelectionResultAndResumeStayServerAuthoritative() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val secureBackend = projectFile("app/src/main/java/com/sonharf/game/data/SecureMatchBackend.kt").readText()

        assertTrue(screen.contains("while (true)"))
        assertTrue(screen.contains("backend.finishTriviaResult(q.id)"))
        assertTrue(screen.contains("backend.claimTriviaTimeout(expectedRound)"))
        assertTrue(screen.contains("backend.answerBilBakalimNumeric(q.id, estimate)"))
        assertFalse(screen.contains("backend.triggerBilBakalimBonus(active.id)"))
        assertTrue(secureBackend.contains("answer_bilbakalim_numeric_v4"))
        assertTrue(secureBackend.contains("trigger_bilbakalim_bonus_v2"))
        assertTrue(arena.contains("KeyboardType.Number"))
        assertTrue(arena.contains("DOĞRU CEVAP"))
        assertTrue(arena.contains("round.resolvedAt == null"))
        assertTrue(arena.contains("triviaRound?.resultUntil"))
        assertTrue(arena.contains("triviaRound?.answerDeadline"))
        assertTrue(arena.contains("onTriviaTimeout()"))
    }

    @Test
    fun databaseUsesOneTenSecondClockAndLiveAdminControls() {
        val migration = projectFile(
            "supabase/migrations/20260831_bonus_flow_timer_admin_enforcement.sql"
        ).readText()

        assertTrue(migration.contains("clock_timestamp() + interval '10 seconds'"))
        assertTrue(migration.contains("set is_correct=(answer_index=v_correct)"))
        assertTrue(migration.contains("raise exception 'invalid_trivia_option'"))
        assertTrue(migration.contains("'matchmaking_enabled'"))
        assertTrue(migration.contains("'chat_enabled'"))
        assertTrue(migration.contains("'trivia_enabled'"))
        assertTrue(migration.contains("'maintenance_mode'"))
        assertTrue(migration.contains("as restrictive"))
    }

    @Test
    fun rememberedPasswordIsEncryptedAndLatestLogoAssetRemainsAvailable() {
        val vault = projectFile(
            "app/src/main/java/com/sonharf/game/RememberedCredentialVault.kt"
        ).readText()
        val auth = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val logo = projectFile("app/src/main/java/com/sonharf/game/SonHarfBrandLogo.kt").readText()
        val activeHome = projectFile("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt").readText()
        val legacyHome = projectFile("app/src/main/java/com/sonharf/game/PremiumMasterHome.kt").readText()

        assertTrue(vault.contains("AndroidKeyStore"))
        assertTrue(vault.contains("AES/GCM/NoPadding"))
        assertTrue(auth.contains("RememberedCredentialVault.save"))
        assertTrue(logo.contains("R.drawable.son_harf_splash_logo"))
        assertTrue(activeHome.contains("MonsterDestination.HOME -> MonsterHomeScreen("))
        assertTrue(activeHome.contains("SON HARF"))
        assertTrue(legacyHome.contains("SonHarfBrandLogo("))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
