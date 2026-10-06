package com.sonharf.game

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Unread chat in the open Siege game: messages from the rival that arrived after the chat was
 * last opened. The game screens poll the chat in the background and show the count on the
 * chat button, so a new message is noticed without opening the chat.
 */
internal object GameChatBadge {
    var unread by mutableIntStateOf(0)
        private set
    private val tracker = UnreadChatTracker()
    private var gameId: String? = null
    private var preferences: android.content.SharedPreferences? = null
    private val restored = mutableSetOf<String>()

    fun init(context: android.content.Context) {
        preferences = context.applicationContext.getSharedPreferences("chat_read_watermarks", android.content.Context.MODE_PRIVATE)
    }

    private fun key(game: String): String {
        val user = if (com.sonharf.game.data.SupabaseProvider.configured)
            com.sonharf.game.data.OnlineGameBackend().currentUserId().orEmpty() else "local"
        return "$user:$game"
    }

    fun select(game: String) {
        val conversation = key(game)
        gameId = conversation
        if (restored.add(conversation)) tracker.restore(conversation, preferences?.getLong(conversation, 0) ?: 0)
        unread = tracker.count(conversation)
    }

    fun update(game: String, messageIds: List<Pair<Long, Boolean>>, open: Boolean) {
        select(game)
        val conversation = requireNotNull(gameId)
        unread = tracker.update(conversation, messageIds, open)
        if (open) preferences?.edit()?.putLong(conversation, tracker.seenId(conversation))?.apply()
    }

    fun markRead() {
        gameId?.let { room ->
            tracker.markRead(room)
            preferences?.edit()?.putLong(room, tracker.seenId(room))?.apply()
        }
        unread = 0
    }

}

/** Unread chat on a chat button: the app's shared notification badge, small size. */
@Composable
internal fun BoxScope.ChatUnreadDot(count: Int) {
    CornerNotificationBadge(count, small = true)
}
