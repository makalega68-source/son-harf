from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new)

backend_path = Path("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt")
backend = backend_path.read_text(encoding="utf-8")
backend = replace_once(
    backend,
    '''@Serializable
data class GameWordDto(''',
    '''@Serializable
data class PremierReconnectClockDto(
    @SerialName("remaining_ms") val remainingMs: Long = 0,
    @SerialName("reconnect_deadline") val reconnectDeadline: String? = null,
)

@Serializable
data class GameWordDto(''',
    "reconnect DTO",
)
backend = replace_once(
    backend,
    '''    suspend fun heartbeatRoom(roomId: String): GameRoomDto =
        supabase.postgrest.rpc("heartbeat_room", buildJsonObject { put("p_room_id", roomId) }).decodeSingle()

    suspend fun botTakeTurn(roomId: String): GameRoomDto =''',
    '''    suspend fun heartbeatRoom(roomId: String): GameRoomDto =
        supabase.postgrest.rpc("heartbeat_room", buildJsonObject { put("p_room_id", roomId) }).decodeSingle()

    suspend fun getPremierReconnectClock(roomId: String): PremierReconnectClockDto =
        supabase.postgrest.rpc(
            "get_premier_reconnect_clock_v1",
            buildJsonObject { put("p_room_id", roomId) },
        ).decodeSingle()

    suspend fun botTakeTurn(roomId: String): GameRoomDto =''',
    "reconnect clock RPC",
)
backend_path.write_text(backend, encoding="utf-8")

screen_path = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
screen = screen_path.read_text(encoding="utf-8")
screen = replace_once(
    screen,
    '''    LaunchedEffect(turnSeconds, stage, room?.disconnectedPlayerId) {
        if (stage == PremierStage.Playing && room?.disconnectedPlayerId == null && turnSeconds in 1..5) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }''',
    '''    LaunchedEffect(turnSeconds, stage, room?.disconnectedPlayerId, room?.currentPlayerId) {
        val reconnectGraceActive = room?.disconnectedPlayerId != null &&
            room?.disconnectedPlayerId == room?.currentPlayerId
        if (stage == PremierStage.Playing && !reconnectGraceActive && turnSeconds in 1..5) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }''',
    "reconnect haptics",
)
screen = replace_once(
    screen,
    '''            // This countdown is presentation-only. The database reconnect_deadline remains authoritative.
            val initialReconnectMs = Duration.between(Instant.now(), reconnectDeadline).toMillis().coerceAtLeast(0L)
            val reconnectAnchor = SystemClock.elapsedRealtime()''',
    '''            // The database clock is authoritative; phone wall-clock drift must not shorten reconnect grace.
            val requestStartedAt = SystemClock.elapsedRealtime()
            val reconnectClock = runCatching { backend.getPremierReconnectClock(active.id) }.getOrNull()
            val requestFinishedAt = SystemClock.elapsedRealtime()
            val halfRoundTripMs = ((requestFinishedAt - requestStartedAt) / 2L).coerceIn(0L, 750L)
            val initialReconnectMs = if (reconnectClock != null) {
                (reconnectClock.remainingMs - halfRoundTripMs).coerceAtLeast(0L)
            } else {
                PREMIER_RECONNECT_SECONDS * 1000L
            }
            val reconnectAnchor = SystemClock.elapsedRealtime()''',
    "reconnect server clock",
)
screen = replace_once(
    screen,
    '''                PremierTurnBadge(language, myTurn, room.status)
                if (reconnectGraceActive) {
                    Spacer(Modifier.height(primaryGap))
                    PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                }
                Spacer(Modifier.height(primaryGap))''',
    '''                if (reconnectGraceActive) {
                    PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                } else {
                    PremierTurnBadge(language, myTurn, room.status)
                }
                Spacer(Modifier.height(primaryGap))''',
    "reconnect badge",
)
screen_path.write_text(screen, encoding="utf-8")

print("Premier reconnect server-clock polish applied")
