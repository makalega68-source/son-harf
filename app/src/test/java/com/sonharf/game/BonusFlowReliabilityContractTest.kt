package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonusFlowReliabilityContractTest {

    @Test
    fun bilBakalimIsRemovedFromTheActiveDuelPath() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val refined = projectFile("app/src/main/java/com/sonharf/game/RefinedDuelOverlay.kt").readText()

        assertFalse(screen.contains("backend.triggerBilBakalimBonus(active.id)"))
        assertFalse(arena.contains("SmallAction(\"★ BONUS\""))
        assertFalse(refined.contains("RefinedTriviaDialog("))
        assertFalse(refined.contains("status in setOf(\"playing\", \"quiz\""))
    }

    @Test
    fun legacyQuizOverlayCannotEnterTheActiveRuntime() {
        val mount = projectFile("app/src/main/java/com/sonharf/game/SketchGameOverlayV9.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/LiveDuelRuntimeShell.kt").readText()

        assertTrue(mount.contains("setOf(\"playing\", \"final\", \"sudden_death\", \"paused\")"))
        assertFalse(shell.contains("\"quiz\""))
    }

    @Test
    fun databaseDisablesTriviaAndReturnsBeforeBotThinking() {
        val migration = projectFile(
            "supabase/migrations/20260907103000_remove_bilbakalim_and_release_bot_submit.sql"
        ).readText()

        assertTrue(migration.contains("where key = 'trivia_enabled'"))
        assertTrue(migration.contains("select public.submit_word_v3_core_v1(p_room_id, p_word)"))
        assertFalse(migration.contains("r := public.bot_take_turn(r.id)"))
        assertFalse(migration.contains("start_bilbakalim_round_v1(r.id"))
        assertTrue(migration.contains("where status = 'quiz'"))
        assertTrue(migration.contains("from public, anon"))
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