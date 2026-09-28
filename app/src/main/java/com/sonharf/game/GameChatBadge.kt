package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unread chat in the open Siege game: messages from the rival that arrived after the chat was
 * last opened. The game screens poll the chat in the background and show the count on the
 * chat button, so a new message is noticed without opening the chat.
 */
internal object GameChatBadge {
    var unread by mutableIntStateOf(0)
        private set
    private var seenId by mutableLongStateOf(0L)
    private var gameId: String? = null

    /** A new message list for [game]; [open] means the chat is on screen and everything is read. */
    fun update(game: String, messageIds: List<Pair<Long, Boolean>>, open: Boolean) {
        if (gameId != game) {
            gameId = game
            // Messages already there when the game is opened count as read.
            seenId = messageIds.maxOfOrNull { it.first } ?: 0L
        }
        if (open) seenId = maxOf(seenId, messageIds.maxOfOrNull { it.first } ?: 0L)
        unread = messageIds.count { (id, fromRival) -> fromRival && id > seenId }
    }

    fun markRead() {
        unread = 0
    }
}

/** A small red count in the corner of a chat button. */
@Composable
internal fun BoxScope.ChatUnreadDot(count: Int) {
    if (count <= 0) return
    Box(
        Modifier.align(Alignment.TopEnd).padding(2.dp).defaultMinSize(16.dp, 16.dp).background(Color(0xFFE5304A), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 9) "9+" else "$count", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 3.dp))
    }
}
