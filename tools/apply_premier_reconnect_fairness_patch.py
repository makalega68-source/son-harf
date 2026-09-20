from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
text = path.read_text(encoding="utf-8")

old = '''private const val PREMIER_TURN_SECONDS = 15'''
new = '''private const val PREMIER_TURN_SECONDS = 15
private const val PREMIER_RECONNECT_SECONDS = 60'''
if text.count(old) != 1:
    raise SystemExit("turn constant anchor mismatch")
text = text.replace(old, new)

old = '''internal fun premierRemainingTurnSeconds(deadline: Instant, now: Instant = Instant.now()): Int =
    premierRemainingTurnSecondsFromMillis(Duration.between(now, deadline).toMillis())'''
new = '''internal fun premierRemainingTurnSeconds(deadline: Instant, now: Instant = Instant.now()): Int =
    premierRemainingTurnSecondsFromMillis(Duration.between(now, deadline).toMillis())

internal fun premierRemainingReconnectSecondsFromMillis(remainingMillis: Long): Int {
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + 999L) / 1000L).coerceIn(1L, PREMIER_RECONNECT_SECONDS.toLong()).toInt()
}'''
if text.count(old) != 1:
    raise SystemExit("remaining helper anchor mismatch")
text = text.replace(old, new)

old = '''    LaunchedEffect(turnSeconds, stage) {
        if (stage == PremierStage.Playing && turnSeconds in 1..5) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }'''
new = '''    LaunchedEffect(turnSeconds, stage, room?.disconnectedPlayerId) {
        if (stage == PremierStage.Playing && room?.disconnectedPlayerId == null && turnSeconds in 1..5) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }'''
if text.count(old) != 1:
    raise SystemExit("haptic anchor mismatch")
text = text.replace(old, new)

start_marker = '''    LaunchedEffect(room?.id, room?.turnDeadline, room?.currentPlayerId, room?.status, room?.botTurn) {'''
end_marker = '''\n    DisposableEffect(room?.id, stage) {'''
start = text.find(start_marker)
if start < 0:
    raise SystemExit("turn effect start missing")
end = text.find(end_marker, start)
if end < 0:
    raise SystemExit("turn effect end missing")
new_effect = '''    LaunchedEffect(
        room?.id,
        room?.turnDeadline,
        room?.currentPlayerId,
        room?.status,
        room?.botTurn,
        room?.disconnectedPlayerId,
        room?.reconnectDeadline,
    ) {
        val active = room ?: return@LaunchedEffect
        if (active.status !in setOf("playing", "final", "sudden_death") || active.botTurn) {
            turnSeconds = PREMIER_TURN_SECONDS
            return@LaunchedEffect
        }

        val reconnectDeadline = if (
            !active.isBot &&
            active.disconnectedPlayerId != null &&
            active.disconnectedPlayerId == active.currentPlayerId
        ) {
            active.reconnectDeadline?.let { runCatching { Instant.parse(it) }.getOrNull() }
        } else {
            null
        }

        if (reconnectDeadline != null) {
            // This countdown is presentation-only. The database reconnect_deadline remains authoritative.
            val initialReconnectMs = Duration.between(Instant.now(), reconnectDeadline).toMillis().coerceAtLeast(0L)
            val reconnectAnchor = SystemClock.elapsedRealtime()
            while (true) {
                val elapsedMs = SystemClock.elapsedRealtime() - reconnectAnchor
                val remaining = premierRemainingReconnectSecondsFromMillis(initialReconnectMs - elapsedMs)
                if (remaining > 0) {
                    turnSeconds = remaining
                    delay(250)
                    continue
                }

                turnSeconds = 1
                val resolved = runCatching { backend.heartbeatRoom(active.id) }.getOrNull()
                if (resolved != null) {
                    room = resolved
                    notice = if (resolved.isPremierFinished()) {
                        pt(language, "Yeniden bağlanma süresi doldu. Maç sonuçlandı.", "Reconnect window expired. Match finished.")
                    } else {
                        ""
                    }
                    if (resolved.isPremierFinished()) stage = PremierStage.Finished
                    return@LaunchedEffect
                }

                notice = pt(language, "Yeniden bağlanma durumu eşitleniyor…", "Syncing reconnect status…")
                delay(1000)
            }
        }

        val deadline = active.turnDeadline?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (deadline == null) {
            turnSeconds = PREMIER_TURN_SECONDS
            runCatching { backend.getRoom(active.id) }.getOrNull()?.let { synced ->
                if (synced != active) room = synced
            }
            return@LaunchedEffect
        }

        // Anchor the visible countdown to the database clock rather than the phone wall clock.
        // Phone clock drift must not shorten the authoritative 15-second turn.
        turnSeconds = PREMIER_TURN_SECONDS
        val requestStartedAt = SystemClock.elapsedRealtime()
        val serverClock = runCatching { fetchPremierTurnClock(active.id) }.getOrNull()
        val requestFinishedAt = SystemClock.elapsedRealtime()
        val halfRoundTripMs = ((requestFinishedAt - requestStartedAt) / 2L).coerceIn(0L, 750L)
        val fallbackRemainingMs = Duration.between(Instant.now(), deadline).toMillis().coerceAtLeast(0L)
        val initialRemainingMs = if (serverClock != null) {
            (serverClock.remainingMs - halfRoundTripMs).coerceAtLeast(0L)
        } else {
            fallbackRemainingMs
        }
        val countdownAnchor = SystemClock.elapsedRealtime()

        while (true) {
            val elapsedMs = SystemClock.elapsedRealtime() - countdownAnchor
            val remaining = premierRemainingTurnSecondsFromMillis(initialRemainingMs - elapsedMs)
            if (remaining > 0) {
                turnSeconds = remaining
                delay(250)
                continue
            }

            // Never present 00 as an actionable live turn. Keep the final visible tick while
            // the server confirms expiry or sends the next authoritative room state.
            turnSeconds = 1

            val synced = runCatching { backend.getRoom(active.id) }.getOrNull()
            if (synced != null && (
                    synced.turnDeadline != active.turnDeadline ||
                        synced.currentPlayerId != active.currentPlayerId ||
                        synced.status != active.status ||
                        synced.botTurn != active.botTurn ||
                        synced.disconnectedPlayerId != active.disconnectedPlayerId ||
                        synced.reconnectDeadline != active.reconnectDeadline
                )
            ) {
                room = synced
                notice = ""
                return@LaunchedEffect
            }

            val advanced = runCatching { backend.claimTurnTimeout(active.id) }.getOrNull()
            if (advanced != null) {
                room = advanced
                val reconnectProtected = !advanced.isBot &&
                    advanced.disconnectedPlayerId != null &&
                    advanced.disconnectedPlayerId == advanced.currentPlayerId &&
                    advanced.reconnectDeadline != null
                notice = if (reconnectProtected) {
                    pt(language, "Oyuncu yeniden bağlanıyor…", "Player is reconnecting…")
                } else {
                    pt(language, "Süre doldu. Sıra güncellendi.", "Time expired. Turn updated.")
                }
                return@LaunchedEffect
            }

            notice = pt(language, "Maç yeniden eşitleniyor…", "Resyncing match…")
            delay(1000)
        }
    }
'''
text = text[:start] + new_effect + text[end:]

old = '''    val myStreak = if (amHost) room.hostStreak else room.guestStreak
    val rivalStreak = if (amHost) room.guestStreak else room.hostStreak
    val myTurn = room.currentPlayerId == meId && !room.botTurn && room.status in setOf("playing", "final", "sudden_death")
    val rivalName = if (room.isBot) room.botName ?: pt(language, "KelimeBot", "WordBot") else opponent?.displayName ?: pt(language, "Rakip", "Rival")'''
new = '''    val myStreak = if (amHost) room.hostStreak else room.guestStreak
    val rivalStreak = if (amHost) room.guestStreak else room.hostStreak
    val reconnectGraceActive = !room.isBot &&
        room.disconnectedPlayerId != null &&
        room.disconnectedPlayerId == room.currentPlayerId &&
        room.reconnectDeadline != null
    val reconnectingMe = reconnectGraceActive && room.disconnectedPlayerId == meId
    val myTurn = room.currentPlayerId == meId && !room.botTurn && !reconnectingMe &&
        room.status in setOf("playing", "final", "sudden_death")
    val rivalName = if (room.isBot) room.botName ?: pt(language, "KelimeBot", "WordBot") else opponent?.displayName ?: pt(language, "Rakip", "Rival")'''
if text.count(old) != 1:
    raise SystemExit("arena state anchor mismatch")
text = text.replace(old, new)

old = '''                PremierTurnBadge(language, myTurn, room.status)
                Spacer(Modifier.height(primaryGap))
                PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)'''
new = '''                PremierTurnBadge(language, myTurn, room.status)
                if (reconnectGraceActive) {
                    Spacer(Modifier.height(primaryGap))
                    PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                }
                Spacer(Modifier.height(primaryGap))
                PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)'''
if text.count(old) != 1:
    raise SystemExit("arena banner anchor mismatch")
text = text.replace(old, new)

anchor = '''@Composable
private fun PremierTargetCard(language: String, required: String, gameMode: String, round: Int, size: Dp) {'''
banner = '''@Composable
private fun PremierReconnectBanner(language: String, reconnectingMe: Boolean, seconds: Int) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = PremierUi.GoldSoft,
        border = BorderStroke(1.dp, PremierUi.Gold.copy(alpha = .35f)),
    ) {
        Text(
            if (reconnectingMe) {
                pt(
                    language,
                    "Bağlantın yeniden doğrulanıyor • ${seconds.coerceAtLeast(1)} sn",
                    "Revalidating your connection • ${seconds.coerceAtLeast(1)} sec",
                )
            } else {
                pt(
                    language,
                    "Rakip yeniden bağlanıyor • ${seconds.coerceAtLeast(1)} sn",
                    "Rival is reconnecting • ${seconds.coerceAtLeast(1)} sec",
                )
            },
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = PremierUi.Gold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PremierTargetCard(language: String, required: String, gameMode: String, round: Int, size: Dp) {'''
if text.count(anchor) != 1:
    raise SystemExit("target card anchor mismatch")
text = text.replace(anchor, banner)

path.write_text(text, encoding="utf-8")
print("Premier reconnect fairness UI patch applied")
