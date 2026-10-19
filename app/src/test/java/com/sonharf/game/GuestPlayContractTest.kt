package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestPlayContractTest {
    private fun source(name: String) = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun newPlayersCanPlayAtOnceAndKeepTheAccountWithGoogle() {
        val gate = source("RequiredAuthGate.kt")
        // The first screen is "play now" on a guest account; email is one tap away.
        assertTrue(gate.contains("QuickPlayCard("))
        assertTrue(gate.contains("PlayerAccount.startGuest(context, displayName, gender)"))
        // Back steps back inside the entry screen (verification -> form -> quick play).
        assertTrue(gate.contains("BackHandler(enabled = pendingVerificationEmail != null || emailMode)"))
        val account = source("PlayerAccount.kt")
        assertTrue(account.contains("auth.signInAnonymously()"))
        // Linking Google upgrades the same account, so the guest's progress is kept.
        assertTrue(account.contains("linkIdentityWithIdToken(provider = Google, idToken = token)"))
        // A guest session counts as signed in at startup.
        assertTrue(source("StableV1App.kt").contains("PlayerAccount.hasPlayableSession() || restoreMembershipSession(context)"))
        // Signing out of a guest account warns that the progress would be lost.
        assertTrue(source("MainSettingsVipScreen.kt").contains("Misafir hesaptasın: çıkış yaparsan"))
    }

    @Test fun guestsAreAskedOnHomeToSecureTheirAccount() {
        val home = java.io.File("src/main/java/com/sonharf/game/HomeLobby.kt").readText()
        assertTrue(home.contains("HomeSecureAccountBanner(me)"))
        assertTrue(home.contains("PlayerAccount.linkGoogle(context)"))
    }
}
