package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cross-screen handoff for accepted Word Arena invitations.
 * The root shell observes [request] and opens the exact matched room.
 */
object WordArenaNavigation {
    var request by mutableIntStateOf(0)
        private set

    var roomId by mutableStateOf<String?>(null)
        private set

    fun requestRoom(id: String) {
        roomId = id
        request += 1
    }

    fun clearRoom() {
        roomId = null
    }
}
