package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRoyalRegressionContractTest {
    private fun src(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun timeoutRecoveryCannotRemainOneShotLocked() {
        val online = src("OnlineGameScreenV6.kt")
        val authority = src("ClassicServerAuthority.kt")
        val duel = src("LightDuelUi.kt")
        assertTrue(online.contains("repeat(12) { attempt ->"))
        assertTrue(online.contains("backend.claimTurnTimeout(active.id)"))
        assertTrue(online.contains("backend.getRoom(active.id)"))
        assertTrue(online.contains("acceptServerRoom(updated)"))
        assertTrue(authority.contains("room.lastEvent.orEmpty()"))
        assertTrue(authority.contains("room.hostScore.toString()"))
        assertTrue(duel.contains("timeoutSignalKey = null"))
    }

    @Test fun royalCollectionUsesExistingCosmeticIdsOnly() {
        val style = src("PurchasedStyleUi.kt")
        assertTrue(style.contains("Royal Ruby"))
        assertTrue(style.contains("Royal Emerald"))
        assertTrue(style.contains("Royal Ice"))
        assertTrue(style.contains("Royal Violet"))
        assertTrue(style.contains("Royal Gold"))
        assertTrue(style.contains("frame_asset_red"))
        assertTrue(style.contains("frame_asset_gold"))
    }

    @Test fun gameplayRpcContractsRemainPresent() {
        val backend = File("src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        assertTrue(backend.contains("submit_word_v3"))
        assertTrue(backend.contains("claim_turn_timeout"))
        assertTrue(backend.contains("join_random_matchmaking"))
        assertTrue(backend.contains("heartbeat_room"))
    }
}
