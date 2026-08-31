package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonusFlowReliabilityContractTest {

    @Test
    fun bonusNumericEstimateResultAndResumeStayServerAuthoritative() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        val migration = projectFile("supabase/migrations/20260831233000_bilbakalim_numeric_estimate_v2.sql").readText()

        assertTrue(screen.contains("while (true)"))
        assertTrue(screen.contains("backend.finishTriviaResult(q.id)"))
        assertTrue(screen.contains("backend.claimTriviaTimeout(expectedRound)"))
        assertTrue(screen.contains("triviaSelection = q.id to estimate.toLong()"))
        assertTrue(backend.contains("getMyTriviaAnswer"))
        assertTrue(arena.contains("KeyboardType.Number"))
        assertTrue(arena.contains("won -> LGreen"))
        assertTrue(arena.contains("else -> LRed"))
        assertTrue(arena.contains("triviaResolved -> 3; else -> 10"))
        assertFalse(arena.substringAfter("private fun LightBonusCard(").substringBefore("private fun LightInputBar").contains("question.optionA"))
        assertTrue(migration.contains("host_dist:=abs(host_est-v_correct)"))
        assertTrue(migration.contains("guest_dist:=abs(guest_est-v_correct)"))
        assertTrue(migration.contains("award int:=10"))
    }

    @Test
    fun databaseUsesOneTenSecondClockAndLiveAdminControls() {
        val legacyMigration = projectFile(
            "supabase/migrations/20260831_bonus_flow_timer_admin_enforcement.sql"
        ).readText()
        val numericMigration = projectFile(
            "supabase/migrations/20260831233000_bilbakalim_numeric_estimate_v2.sql"
        ).readText()

        assertTrue(numericMigration.contains("interval '10 seconds'"))
        assertTrue(numericMigration.contains("on conflict(round_id,player_id) do nothing"))
        assertTrue(numericMigration.contains("invalid_numeric_estimate"))
        assertTrue(numericMigration.contains("question_kind='bil_bakalim'"))
        assertTrue(legacyMigration.contains("'matchmaking_enabled'"))
        assertTrue(legacyMigration.contains("'chat_enabled'"))
        assertTrue(legacyMigration.contains("'trivia_enabled'"))
        assertTrue(legacyMigration.contains("'maintenance_mode'"))
        assertTrue(legacyMigration.contains("as restrictive"))
    }

    @Test
    fun rememberedPasswordIsEncryptedAndLatestLogoIsUsed() {
        val vault = projectFile(
            "app/src/main/java/com/sonharf/game/RememberedCredentialVault.kt"
        ).readText()
        val auth = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val logo = projectFile("app/src/main/java/com/sonharf/game/SonHarfBrandLogo.kt").readText()
        val activeHome = projectFile("app/src/main/java/com/sonharf/game/MainExperienceApp.kt").readText()
        val legacyHome = projectFile("app/src/main/java/com/sonharf/game/PremiumMasterHome.kt").readText()

        assertTrue(vault.contains("AndroidKeyStore"))
        assertTrue(vault.contains("AES/GCM/NoPadding"))
        assertTrue(auth.contains("RememberedCredentialVault.save"))
        assertTrue(logo.contains("R.drawable.son_harf_splash_logo"))
        assertTrue(activeHome.contains("MainDestination.HOME -> MainHomeScreen("))
        assertTrue(activeHome.contains("SonHarfBrandLogo("))
        assertTrue(legacyHome.contains("SonHarfBrandLogo("))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}