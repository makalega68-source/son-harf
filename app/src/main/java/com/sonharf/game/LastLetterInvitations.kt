package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.sonharf.game.data.GameInviteDto

/** Only invitations explicitly sent in this session may automatically open a match. */
internal object LastLetterInvitations {
    private val senders = mutableMapOf<String, String>()
    var revision by mutableIntStateOf(0)
        private set

    fun await(invite: GameInviteDto) {
        senders[invite.id] = invite.senderId
        revision++
    }

    fun pendingFor(userId: String): Set<String> = senders.filterValues { it == userId }.keys.toSet()
    fun remove(id: String) { senders.remove(id) }
    fun clear() { senders.clear(); revision++ }
}
