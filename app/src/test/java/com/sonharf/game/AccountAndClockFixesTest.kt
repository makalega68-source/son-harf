package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountAndClockFixesTest {
    private fun src(name: String) = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun anotherAccountsCosmeticsAreClearedOnThisDevice() {
        val caches = src("AccountCaches.kt")
        assertTrue(caches.contains("SonHarfCosmetics.clearLocal(context)"))
        assertTrue(caches.contains("WordSiegeMascotOwnership.clearLocal(context)"))
        assertTrue(src("HomeLobby.kt").contains("AccountCaches.ensureFor(lobbyContext, me)"))
        assertTrue(src("MainSettingsVipScreen.kt").contains("AccountCaches.signedOut(context)"))
    }

    @Test fun atelierShowsNoMascotWithoutOne() {
        val atelier = src("KelimeAtolyesiScreen.kt")
        assertTrue(atelier.contains("else if (skin != null) WordSiegeMascot("))
        assertFalse(atelier.contains("?: WordSiegeMascotSkin.ORB"))
    }

    @Test fun throneAvatarHasOneFrame() {
        assertTrue(src("ThroneExperience.kt").contains("showGenderBadge=false,ring=false"))
    }

    @Test fun roundBreakResumesTheServerClock() {
        val duel = src("PremierWordDuelScreen.kt")
        assertTrue(duel.contains("room?.turnDeadline?.let { com.sonharf.game.data.parseServerInstant(it) },"))
        assertTrue(duel.contains("val resumed = turnClockAnchor?.takeIf { it.roomId == active.id && it.deadline == deadline }"))
        assertTrue(duel.contains("turnClockAnchor = PremierTurnClockAnchor(active.id, deadline, initialRemainingMs, countdownAnchor)"))
    }
}
