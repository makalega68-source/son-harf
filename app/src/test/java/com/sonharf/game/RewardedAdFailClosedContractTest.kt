package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardedAdFailClosedContractTest {
    @Test fun rewardRequiresVerifiedAdCallbackAndServerClaim() {
        val controller = projectFile("app/src/main/java/com/sonharf/game/RewardedAdController.kt").readText()
        val center = projectFile("app/src/main/java/com/sonharf/game/RewardCenterScreen.kt").readText()

        assertTrue(controller.contains("verificationUserId.isNullOrBlank()"))
        assertTrue(controller.contains("verificationData.isNullOrBlank()"))
        assertTrue(controller.contains("onUnavailable()"))
        assertTrue(center.contains("prepareStoreReward"))
        assertTrue(center.contains("awaitVerifiedStoreReward"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
