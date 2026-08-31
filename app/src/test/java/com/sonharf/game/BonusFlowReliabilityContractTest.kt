package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonusFlowReliabilityContractTest {

    @Test
    fun bonusSelectionResultAndResumeStayServerAuthoritative() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()

        assertTrue(screen.contains("while (true)"))
        assertTrue(screen.contains("backend.finishTriviaResult(q.id)"))
        assertTrue(screen.contains("backend.claimTriviaTimeout(expectedRound)"))
        assertTrue(screen.contains("triviaSelection = q.id to estimate.toLong()"))
        assertTrue(backend.contains("getMyTriviaAnswer"))
        assertTrue(arena.contains("correct -> LGreen"))
        assertTrue(arena.contains("resolved && selected -> LRed"))
        assertTrue(arena.contains("else -> 10"))
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
    fun rememberedPasswordIsEncryptedAndLatestLogoIsUsed() {
        val vault = projectFile(
            "app/src/main/java/com/sonharf/game/RememberedCredentialVault.kt"
        ).readText()
        val auth = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val logo = projectFile("app/src/main/java/com/sonharf/game/SonHarfBrandLogo.kt").readText()
        val home = projectFile("app/src/main/java/com/sonharf/game/PremiumMasterHome.kt").readText()

        assertTrue(vault.contains("AndroidKeyStore"))
        assertTrue(vault.contains("AES/GCM/NoPadding"))
        assertTrue(auth.contains("RememberedCredentialVault.save"))
        assertTrue(logo.contains("R.drawable.son_harf_splash_logo"))
        assertTrue(home.contains("SonHarfBrandLogo("))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
