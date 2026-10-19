package com.sonharf.game

internal class UnreadChatTracker {
    private data class Conversation(var seen: Long = 0, var newest: Long = 0, var unread: Int = 0)
    private val conversations = linkedMapOf<String, Conversation>()

    fun update(room: String, messages: List<Pair<Long, Boolean>>, open: Boolean): Int {
        val conversation = conversations.getOrPut(room) { Conversation() }
        val newest = messages.maxOfOrNull { it.first } ?: 0
        if (newest < conversation.newest) {
            if (open) markRead(room)
            return conversation.unread
        }
        conversation.newest = maxOf(conversation.newest, newest)
        if (open) conversation.seen = maxOf(conversation.seen, conversation.newest)
        conversation.unread = messages.distinctBy { it.first }.count { (id, incoming) -> incoming && id > conversation.seen }
        return conversation.unread
    }

    fun markRead(room: String) {
        conversations[room]?.let { it.seen = maxOf(it.seen, it.newest); it.unread = 0 }
    }

    fun restore(room: String, seenId: Long) {
        conversations.getOrPut(room) { Conversation() }.seen = seenId.coerceAtLeast(0)
    }

    fun seenId(room: String): Long = conversations[room]?.seen ?: 0

    fun count(room: String): Int = conversations[room]?.unread ?: 0
}
