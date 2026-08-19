package com.sonharf.game.data

interface GameService {
    suspend fun createRoom(hostId: String): GameRoom
    suspend fun joinRoom(code: String, guestId: String): GameRoom
    suspend fun sendWord(roomId: String, playerId: String, word: String): Result<Unit>
    suspend fun sendChat(roomId: String, playerId: String, message: String): Result<Unit>
    suspend fun leaveRoom(roomId: String, playerId: String): Result<Unit>
}

class OfflineGameService : GameService {
    override suspend fun createRoom(hostId: String): GameRoom =
        GameRoom(id = "local-room", code = "TEST01", hostId = hostId)

    override suspend fun joinRoom(code: String, guestId: String): GameRoom =
        GameRoom(id = "local-room", code = code, hostId = "host", guestId = guestId, status = "playing")

    override suspend fun sendWord(roomId: String, playerId: String, word: String): Result<Unit> = Result.success(Unit)
    override suspend fun sendChat(roomId: String, playerId: String, message: String): Result<Unit> = Result.success(Unit)
    override suspend fun leaveRoom(roomId: String, playerId: String): Result<Unit> = Result.success(Unit)
}
