package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaReleaseRegressionContractTest {

    @Test
    fun loginAndClassicArenaMobileFixesRemainActive() {
        val auth = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val classic = projectFile("app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt").readText()

        assertFalse(auth.contains("son_harf_login_bg"))
        assertFalse(classic.contains(".verticalScroll(rememberScrollState())"))
        assertFalse(classic.contains("words.takeLast(3)"))
        assertTrue(classic.contains("val current = words.last()"))
        assertTrue(classic.contains("wordFocusRequester.requestFocus()"))
        assertTrue(classic.contains("ProfilePhotoAvatarWithGender("))
    }

    @Test
    fun nativeKeyboardAndImeLayoutFixesRemainAcrossLiveModes() {
        val cipher = projectFile("app/src/main/java/com/sonharf/game/DailyCipherScreen.kt").readText()
        val wordArena = projectFile("app/src/main/java/com/sonharf/game/WordArenaScreen.kt").readText()
        val dailyArena = projectFile("app/src/main/java/com/sonharf/game/DailyArenaScreen.kt").readText()
        val teamArena = projectFile("app/src/main/java/com/sonharf/game/TeamArenaScreen.kt").readText()

        assertTrue(cipher.contains("imeVisible"))
        assertTrue(cipher.contains("height(38.dp)"))
        assertTrue(cipher.contains("guessFocusRequester.requestFocus()"))
        assertTrue(wordArena.contains("inputFocusRequester.requestFocus()"))
        assertTrue(dailyArena.contains("inputFocusRequester.requestFocus()"))
        assertTrue(teamArena.contains("inputFocusRequester.requestFocus()"))
    }

    @Test
    fun teamArenaRecoveryAndInviteSerializationRemainActive() {
        val teamArena = projectFile("app/src/main/java/com/sonharf/game/TeamArenaScreen.kt").readText()
        val classicInvite = projectFile("app/src/main/java/com/sonharf/game/GameInviteOverlay.kt").readText()
        val wordInvite = projectFile("app/src/main/java/com/sonharf/game/WordArenaInviteOverlay.kt").readText()
        val teamInvite = projectFile("app/src/main/java/com/sonharf/game/TeamArenaInviteOverlay.kt").readText()

        assertTrue(teamArena.contains("fun closeScreen()"))
        assertTrue(teamArena.contains("cancelTeamArenaLobby"))
        assertTrue(teamArena.contains("leaveTeamArenaLobby"))
        assertTrue(teamArena.contains("team_arena_already_active"))
        assertTrue(classicInvite.contains("GameInviteModalCoordinator"))
        assertTrue(wordInvite.contains("GameInviteModalCoordinator"))
        assertTrue(teamInvite.contains("GameInviteModalCoordinator"))
    }

    @Test
    fun socialPhotosShopAndVipPolishRemainActive() {
        val competition = projectFile("app/src/main/java/com/sonharf/game/CompetitionHubScreen.kt").readText()
        val shop = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val vip = projectFile("app/src/main/java/com/sonharf/game/VipPurchaseDialog.kt").readText()

        assertTrue(competition.contains("memberProfiles"))
        assertTrue(competition.contains("leaderboardProfiles"))
        assertTrue(competition.contains("playerProfiles"))
        assertTrue(competition.contains("ProfilePhotoAvatar("))
        assertTrue(shop.contains("val anotherItemBusy = busy != null && busy != item.id"))
        assertTrue(vip.contains("BillingManager("))
        assertTrue(vip.contains("PlayPurchaseVerification.verify"))
        assertFalse(vip.contains("rememberInfiniteTransition"))
        assertTrue(vip.contains("no competitive power"))
    }

    @Test
    fun clubFeeAndReleaseGuardrailsRemainEnforced() {
        val migration = projectFile("supabase/migrations/20260829_zzzzzzzz_club_creation_fee_v1.sql").readText()
        val gradle = projectFile("app/build.gradle.kts").readText()
        val workflow = projectFile(".github/workflows/android.yml").readText()
        val stableWorkflow = projectFile(".github/workflows/mark-stable.yml").readText()
        val rollbackWorkflow = projectFile(".github/workflows/rollback-last-green.yml").readText()

        assertTrue(migration.contains("v_cost constant integer:=1000"))
        assertTrue(migration.contains("set diamonds=diamonds-v_cost"))
        assertTrue(migration.contains("security invoker"))
        assertTrue(migration.contains("revoke all on function public.create_club_v1"))
        assertTrue(gradle.contains("targetSdk = 36"))
        assertTrue(gradle.contains("Production release blocked: configure SON_HARF_ADMOB_APP_ID"))
        assertTrue(gradle.contains("Production release blocked: release keystore"))
        assertTrue(workflow.contains("assembleRelease bundleRelease"))
        assertTrue(workflow.contains("app-release.aab"))
        assertTrue(stableWorkflow.contains("workflow_dispatch"))
        assertTrue(rollbackWorkflow.contains("workflow_dispatch"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
