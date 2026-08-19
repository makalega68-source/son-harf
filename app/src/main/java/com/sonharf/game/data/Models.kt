package com.sonharf.game.data

data class PlayerProfile(
    val id: String,
    val displayName: String,
    val rating: Int = 1000,
    val vip: Boolean = false,
    val coins: Int = 0
)

data class GameRoom(
    val id: String,
    val code: String,
    val hostId: String,
    val guestId: String? = null,
    val status: String = "waiting",
    val turnSeconds: Int = 45
)

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderId: String,
    val body: String,
    val createdAt: String
)

data class MatchResult(
    val id: String,
    val winnerId: String?,
    val loserId: String?,
    val wordCount: Int,
    val createdAt: String
)
