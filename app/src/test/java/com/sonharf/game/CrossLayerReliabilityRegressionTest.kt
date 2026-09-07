package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossLayerReliabilityRegressionTest {
    private fun source(path: String): String {
        val candidates = listOf(File(path), File("../$path"))
        return requireNotNull(candidates.firstOrNull(File::isFile)) { "Missing $path" }.readText()
    }

    @Test fun rememberMeAndDeepLinksAreEnforcedInEveryBuildType() {
        val activity = source("app/src/main/java/com/sonharf/game/MainActivity.kt")

        assertTrue(activity.contains("uri?.scheme != \"sonharf\" || uri.host != \"auth\""))
        assertTrue(activity.contains("it.scheme == \"sonharf\" && it.host == \"auth\""))
        assertFalse(activity.contains("val clearUnrememberedSession = !BuildConfig.DEBUG"))
        assertTrue(activity.contains("auth.signOut() }.isSuccess"))
    }

    @Test fun rejectedWordsClearImmediatelyWhileOnlyTimeoutsRestoreInput() {
        val refined = source("app/src/main/java/com/sonharf/game/RefinedDuelOverlay.kt")
        val online = source("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")

        assertTrue(refined.contains("val knownWordIds = words.mapTo(hashSetOf()) { it.id }"))
        assertTrue(refined.contains("it.id !in knownWordIds && it.playerId == me"))
        assertTrue(refined.contains("input = \"\""))
        assertTrue(refined.contains("TimeoutCancellationException) input = submitted"))
        assertTrue(online.contains("wordInput = \"\""))
        assertTrue(online.contains("TimeoutCancellationException) wordInput = submitted"))
        assertTrue(online.contains("Kelimen korundu; oyun durumu eşitleniyor."))
    }

    @Test fun privilegedRpcFunctionsAreNotLeftOpenToAnonymousUsers() {
        val migration = source("supabase/migrations/20260907090000_harden_anonymous_rpc_access.sql")

        assertTrue(migration.contains("from public, anon"))
        assertTrue(migration.contains("get_dictionary_snapshot_v3(text) from public, anon"))
        assertTrue(migration.contains("reject_terminal_soft_g_game_word() from public, anon, authenticated"))
        assertFalse(migration.contains("to anon"))
    }
}