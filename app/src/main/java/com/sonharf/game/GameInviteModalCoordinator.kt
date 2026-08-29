package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class GameInviteModalKind {
    CLASSIC,
    WORD_ARENA,
    TEAM_ARENA,
}

/**
 * Serializes game invitation dialogs so the global overlays never stack multiple AlertDialogs.
 * Pending invites remain owned by their individual overlays; this object only decides visibility.
 */
object GameInviteModalCoordinator {
    private val priority = listOf(
        GameInviteModalKind.CLASSIC,
        GameInviteModalKind.WORD_ARENA,
        GameInviteModalKind.TEAM_ARENA,
    )

    private val pending = linkedSetOf<GameInviteModalKind>()

    var activeKind by mutableStateOf<GameInviteModalKind?>(null)
        private set

    @Synchronized
    fun setPending(kind: GameInviteModalKind, hasPending: Boolean) {
        if (hasPending) {
            pending.add(kind)
        } else {
            pending.remove(kind)
            if (activeKind == kind) activeKind = null
        }
        selectNextIfNeeded()
    }

    @Synchronized
    fun clear(kind: GameInviteModalKind) {
        pending.remove(kind)
        if (activeKind == kind) activeKind = null
        selectNextIfNeeded()
    }

    @Synchronized
    private fun selectNextIfNeeded() {
        if (activeKind != null) return
        activeKind = priority.firstOrNull { it in pending }
    }
}
