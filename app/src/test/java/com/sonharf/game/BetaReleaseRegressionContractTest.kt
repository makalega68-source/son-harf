package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaReleaseRegressionContractTest {
    @Test
    fun currentModesAndCoreNavigationRemainPresent() {
        val premium = projectFile("app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt").readText()
        val main = projectFile("app/src/main/java/com/sonharf/game/MainExperienceApp.kt").readText()
        val monster = projectFile("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt").readText()
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()

        assertTrue(premium.contains("ClassicPremiumGame"))
        assertTrue(main.contains("CompetitionHubScreen"))
        assertTrue(monster.contains("MonsterStyleStoreScreen"))
        assertTrue(siege.contains("WordSiege"))
    }

    @Test
    fun monetizationRemainsCosmeticAndServerVerified() {
        val billing = projectFile("app/src/main/java/com/sonharf/game/billing/PlayPurchaseVerification.kt").readText()
        val migration = projectFile("supabase/migrations/20260829_vip_fair_play_social.sql").readText()
        val market = projectFile("supabase/migrations/20260829_market_v2.sql").readText()

        assertTrue(billing.contains("verify-play-purchase"))
        assertTrue(migration.contains("fair_play"))
        assertTrue(migration.contains("freezer_count',0"))
        assertTrue(market.contains("apply_verified_play_purchase_v1"))
    }

    @Test
    fun activeStoreRoutesAndFramesRemainReal() {
        val main = projectFile("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt").readText()
        val store = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val frameRuntime = projectFile("app/src/main/java/com/sonharf/game/SonHarfCosmetics.kt").readText()

        assertTrue(main.contains("MonsterDestination.STYLE -> MonsterStyleStoreScreen()"))
        assertTrue(store.contains("theme_monster_blue"))
        assertTrue(frameRuntime.contains("frame_starter"))
    }

    @Test
    fun inviteAndCompetitionPathsRemainActive() {
        val competition = projectFile("app/src/main/java/com/sonharf/game/CompetitionHubScreen.kt").readText()
        val teamInvite = projectFile("app/src/main/java/com/sonharf/game/TeamArenaScreen.kt").readText()

        assertTrue(competition.contains("GameInviteModalCoordinator"))
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
        assertTrue(vip.lowercase().contains("never grants time, score, moves, rating or live decision advantages"))
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
        assertTrue(gradle.contains("Production release blocked"))
        assertTrue(workflow.contains("HAS_RELEASE_CONFIG"))
        assertTrue(stableWorkflow.contains("CURRENT_BUILD_OK"))
        assertTrue(rollbackWorkflow.contains("CURRENT_BUILD_OK"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
