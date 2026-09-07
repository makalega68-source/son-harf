package com.sonharf.game

import androidx.compose.runtime.Composable

/**
 * Compatibility entry point kept for existing navigation.
 * The obsolete 9x9 HP/damage/fog/bridge Siege implementation was removed on 2026-09-07.
 * All routes now open the canonical 15x15 server-backed Word Siege experience.
 */
@Composable
internal fun WordSiegeGameScreen(onExit: () -> Unit) {
    WordSiegeExperienceScreen(onExit = onExit)
}
