package com.sonharf.game

import androidx.compose.runtime.Composable

/**
 * Compatibility bridge while the Son Harf game is rebuilt from a clean slate.
 * The previous duel runtime, session recovery and classic game UI are intentionally removed.
 */
@Composable
fun LiveDuelRuntimeShell(onSignedOut: () -> Unit) {
    MonsterExperienceApp(onSignedOut = onSignedOut)
}
