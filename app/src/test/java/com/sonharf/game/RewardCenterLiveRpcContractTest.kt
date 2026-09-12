package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardCenterLiveRpcContractTest {
    @Test
    fun rewardCenterUsesCurrentServerControlledRpcSurface() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/RewardCenter.kt").readText()
        val screen = projectFile("app/src/main/java/com/sonharf/game/RewardCenterScreen.kt").readText()
        val store = projectFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()

        assertTrue(backend.contains("get_store_reward_status_v1"))
        assertTrue(backend.contains("claim_store_rewarded_ad_v1"))
        assertTrue(backend.contains("equip_style_trial_v2"))
        assertTrue(backend.contains("open_piggy_bank_v2"))
        assertFalse(backend.contains("rpc(\"claim_rewarded_ad\""))
        assertFalse(backend.contains("rpc(\"open_reward_chest\""))

        assertTrue(store.contains("@SerialName(\"trial_mode\")"))
        assertTrue(store.contains("@SerialName(\"trial_value\")"))
        assertTrue(screen.contains("piggyBonusSc"))
        assertTrue(screen.contains("openPiggyBank()"))
        assertTrue(screen.contains("claimRewardedAd(rewardType, responseId, trialItemId)"))
        assertFalse(screen.contains("openRewardChest()"))
        assertFalse(screen.contains("rewardType == \"chest\""))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
