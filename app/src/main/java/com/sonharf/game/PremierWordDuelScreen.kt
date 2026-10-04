package com.sonharf.game

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private enum class PremierStage { Loading, Lobby, Searching, Vs, Playing, Finished }
private data class PremierMoveFeedback(val accepted: Boolean, val message: String)
private const val PREMIER_TURN_SECONDS = 15
private const val PREMIER_RECONNECT_SECONDS = 60
/** Breather before every new round; the server adds it to the round's first turn. */
private const val PREMIER_ROUND_PREP_SECONDS = 20

/** Son Harf arena colours; the Black Theme swaps in graphite surfaces and black-gold tiles. */
private object PremierUi {
    val Background: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF101419) else if (SonHarfCosmetics.walnutTheme) Color(0xFFEFE3CC) else Color(0xFFE6ECF2)
    val Surface: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1C222A) else if (SonHarfCosmetics.walnutTheme) Color(0xFFFAF3E3) else Color(0xFFFFFFFF)
    val Ink: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFEEF2F6) else if (SonHarfCosmetics.walnutTheme) Color(0xFF3A2417) else Color(0xFF243142)
    val Muted: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFA3AFBD) else if (SonHarfCosmetics.walnutTheme) Color(0xFF7A6650) else Color(0xFF6B7A8C)
    val Ocean: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFB8C9DE) else if (SonHarfCosmetics.walnutTheme) Color(0xFF4A2E1C) else Color(0xFF2C3E55)
    val OceanDeep = Color(0xFF3E9F4D)
    val Sky = Color(0xFF5DADE2)
    val Ice: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF232A33) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF3E8D2) else Color(0xFFF3F6F9)
    val Border: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF313A46) else if (SonHarfCosmetics.walnutTheme) Color(0xFFCDB58E) else Color(0xFFD2DBE5)
    val Green = Color(0xFF3E9F4D)
    val GreenSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1F3525) else if (SonHarfCosmetics.walnutTheme) Color(0xFFE3EBCF) else Color(0xFFE1F2E3)
    val Red = Color(0xFFD0514A)
    val RedSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF3A2322) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF5DDD3) else Color(0xFFFBE4E2)
    val Gold = Color(0xFFE0A82E)
    val GoldSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF3A3020) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF5E3BD) else Color(0xFFFFF3D6)
}

private object PremierArenaSky {
    val BackgroundTop: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF101419) else if (SonHarfCosmetics.walnutTheme) Color(0xFFEFE3CC) else Color(0xFFE6ECF2)
    val BackgroundMid: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF12171D) else if (SonHarfCosmetics.walnutTheme) Color(0xFFEBDDC3) else Color(0xFFE9EEF3)
    val BackgroundBottom: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF151A21) else if (SonHarfCosmetics.walnutTheme) Color(0xFFE6D6B9) else Color(0xFFEDF1F5)
    val Surface: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1C222A) else if (SonHarfCosmetics.walnutTheme) Color(0xFFFAF3E3) else Color(0xFFFFFFFF)
    val SurfaceBlue: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF232A33) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF3E8D2) else Color(0xFFF3F6F9)
    val Ink: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFEEF2F6) else if (SonHarfCosmetics.walnutTheme) Color(0xFF3A2417) else Color(0xFF243142)
    val Muted: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFA3AFBD) else if (SonHarfCosmetics.walnutTheme) Color(0xFF7A6650) else Color(0xFF6B7A8C)
    val Ocean: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFB8C9DE) else if (SonHarfCosmetics.walnutTheme) Color(0xFF4A2E1C) else Color(0xFF2C3E55)
    val OceanDeep = Color(0xFF22324A)
    val Border: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF313A46) else if (SonHarfCosmetics.walnutTheme) Color(0xFFCDB58E) else Color(0xFFD2DBE5)
    val Rival = Color(0xFFD0514A)
    val RivalSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF3A2322) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF5DDD3) else Color(0xFFFBE4E2)
    val Green = Color(0xFF3E9F4D)
    val GreenSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1F3525) else if (SonHarfCosmetics.walnutTheme) Color(0xFFE3EBCF) else Color(0xFFE1F2E3)
    val Gold = Color(0xFFE0A82E)
    val GoldSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF3A3020) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF5E3BD) else Color(0xFFFFF3D6)
    val Red = Color(0xFFD0514A)
    val RedSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF3A2322) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF5DDD3) else Color(0xFFFBE4E2)
}

private fun pt(language: String, tr: String, en: String): String = if (language == "en") en else tr
private fun premierLocale(language: String): Locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
private fun premierUpper(value: String, language: String): String = value.uppercase(premierLocale(language))

internal fun premierRemainingTurnSecondsFromMillis(remainingMillis: Long): Int {
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + 999L) / 1000L).coerceIn(1L, PREMIER_TURN_SECONDS.toLong()).toInt()
}

internal fun premierRemainingTurnSeconds(deadline: Instant, now: Instant = Instant.now()): Int =
    premierRemainingTurnSecondsFromMillis(Duration.between(now, deadline).toMillis())

internal fun premierRemainingReconnectSecondsFromMillis(remainingMillis: Long): Int {
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + 999L) / 1000L).coerceIn(1L, PREMIER_RECONNECT_SECONDS.toLong()).toInt()
}

/** Two server timestamps name the same moment (formats may differ); unparseable ones compare as text. */
private fun premierSameInstant(a: String?, b: String?): Boolean {
    if (a == b) return true
    val left = parseServerInstant(a)
    val right = parseServerInstant(b)
    return left != null && left == right
}

/**
 * True when [after] moved the turn on from [before]. An RPC that answers with the room unchanged
 * (e.g. `claim_turn_timeout` while the server clock still has time left) is not progress: taking
 * it as progress left the countdown frozen on its last second.
 */
internal fun premierTurnStateChanged(before: GameRoomDto, after: GameRoomDto): Boolean =
    before.id != after.id ||
        !premierSameInstant(before.turnDeadline, after.turnDeadline) ||
        before.currentPlayerId != after.currentPlayerId ||
        before.status != after.status ||
        before.roundNo != after.roundNo ||
        before.validWordCount != after.validWordCount ||
        before.botTurn != after.botTurn ||
        before.winnerId != after.winnerId ||
        before.disconnectedPlayerId != after.disconnectedPlayerId ||
        !premierSameInstant(before.reconnectDeadline, after.reconnectDeadline)

/** Identifies one room/word-list disagreement, so giving up on it never unlocks a later one. */
private fun premierSyncGateKey(room: GameRoomDto, words: List<GameWordDto>): String =
    "${room.id}:${room.validWordCount}:${words.size}"

/** A live room only moves forward: to sudden death, then to a result. */
private fun premierStatusRank(room: GameRoomDto): Int = when {
    room.isPremierFinished() -> 2
    room.status == "sudden_death" -> 1
    else -> 0
}

/**
 * True when [candidate] is an older snapshot of the same room than [current]. The room poll, the
 * word poll and the screen's own RPC answers land out of order; accepting an older room briefly
 * brought back the previous turn (timer restarted, keyboard re-enabled).
 *
 * Ordered by: result/sudden death, round, accepted words (words.size always equals
 * valid_word_count), then, for a missed turn in the same round, the later turn deadline.
 */
internal fun premierRoomSnapshotIsOlder(candidate: GameRoomDto, current: GameRoomDto): Boolean {
    if (candidate.id != current.id) return false
    if (candidate.actionSeq != current.actionSeq) return candidate.actionSeq < current.actionSeq
    val rank = premierStatusRank(candidate).compareTo(premierStatusRank(current))
    if (rank != 0) return rank < 0
    if (current.isPremierFinished()) return false
    if (candidate.roundNo != current.roundNo) return candidate.roundNo < current.roundNo
    if (candidate.validWordCount != current.validWordCount) return candidate.validWordCount < current.validWordCount
    // Same round and word count: only a missed turn (timeout, rejected word) moves the turn.
    if (candidate.currentPlayerId == current.currentPlayerId && candidate.botTurn == current.botTurn) return false
    val candidateDeadline = parseServerInstant(candidate.turnDeadline)
    val currentDeadline = parseServerInstant(current.turnDeadline)
    if (candidateDeadline != null && currentDeadline != null) return candidateDeadline.isBefore(currentDeadline)
    // Against the AI a missed human turn hands the move to the AI (no deadline). Going back to the
    // human without a new word only happens when the AI itself missed.
    if (current.isBot && current.botTurn && !candidate.botTurn) return candidate.lastEvent != "bot_missed"
    if (current.isBot && !current.botTurn && candidate.botTurn) return current.lastEvent == "bot_missed"
    return false
}

@Composable
fun PremierWordDuelScreen() {
    if (!SupabaseProvider.configured) {
        PremierCenteredMessage(
            title = sh("Sunucu bağlantısı yok", "Server connection unavailable"),
            detail = sh("Supabase yapılandırması olmadan online düello başlatılamaz.", "Online duel cannot start without Supabase configuration."),
            action = sh("ANA MENÜ", "HOME"),
            onAction = { SonHarfUiState.homeRequest += 1 },
        )
        return
    }

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var stage by remember { mutableStateOf(PremierStage.Loading) }
    var language by remember { mutableStateOf(SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)) }
    var me by remember { mutableStateOf<ProfileDto?>(null) }
    var opponent by remember { mutableStateOf<ProfileDto?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var botChat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var botChatSequence by remember { mutableLongStateOf(-1L) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showQuickChat by remember { mutableStateOf(false) }
    var unreadChatCount by remember { mutableIntStateOf(0) }
    var floatingMessage by remember { mutableStateOf<ChatMessageDto?>(null) }
    var moveFeedback by remember { mutableStateOf<PremierMoveFeedback?>(null) }
    var turnSeconds by remember { mutableIntStateOf(PREMIER_TURN_SECONDS) }
    var botThinking by remember { mutableStateOf(false) }
    var prepSeconds by remember { mutableIntStateOf(0) }
    var botPrepSeconds by remember { mutableIntStateOf(0) }

    suspend fun ensureMe(): ProfileDto {
        if (backend.currentUserId() == null) backend.ensurePlayer(pt(language, "Oyuncu", "Player"))
        val id = requireNotNull(backend.currentUserId())
        return gameRequestResult { backend.getProfile(id) }
            .getOrElse { backend.ensurePlayer(pt(language, "Oyuncu", "Player")) }
            .also { me = it }
    }

    // Every room update of the running match goes through here, so an older snapshot (a slow
    // poll landing after a newer RPC answer) can never bring back the previous turn.
    fun acceptRoom(next: GameRoomDto): Boolean {
        val current = room
        if (current != null && next.id != current.id) return false
        if (current != null && premierRoomSnapshotIsOlder(next, current)) return false
        room = next
        return true
    }

    // The word list only grows within a match; an older poll must not shrink it.
    fun acceptWords(next: List<GameWordDto>) {
        if (next.any { it.roomId != room?.id }) return
        val current = words
        if (current.isNotEmpty() && next.size < current.size && current.first().roomId == room?.id) return
        words = next
    }
    var syncGateOpenFor by remember { mutableStateOf<String?>(null) }

    suspend fun adoptRoom(next: GameRoomDto, cinematic: Boolean) {
        val previousRoomId = room?.id
        room = next
        if (previousRoomId != next.id) {
            botChat = emptyList()
            words = emptyList()
            chat = emptyList()
            showQuickChat = false
            unreadChatCount = 0
        }
        language = SharedDictionaryService.canonicalLanguage(next.language)
        SonHarfUiState.language = language
        val loaded = coroutineScope {
            val opponentTask = async { gameRequestResult { backend.getPremierOpponent(next) }.getOrNull() }
            val wordsTask = async { gameRequestResult { backend.getWords(next.id) }.getOrDefault(emptyList()) }
            val chatTask = async { if (next.isBot) emptyList() else gameRequestResult { backend.getChat(next.id) }.getOrDefault(emptyList()) }
            Triple(opponentTask.await(), wordsTask.await(), chatTask.await())
        }
        if (room?.id != next.id) return
        opponent = loaded.first
        acceptWords(loaded.second)
        chat = loaded.third
        GameChatBadge.update(next.id, chat.map { it.id to (it.senderId != backend.currentUserId()) }, open = showQuickChat)
        unreadChatCount = GameChatBadge.unread
        stage = when {
            next.isPremierFinished() -> PremierStage.Finished
            cinematic -> PremierStage.Vs
            else -> PremierStage.Playing
        }
    }

    var sendingChat by remember { mutableStateOf(false) }
    // One way to send chat, used by the chat sheet and by the quick messages between rounds.
    fun sendChatMessage(message: String) {
        val active = room ?: return
        if (sendingChat || message.isBlank()) return
        sendingChat = true
        scope.launch {
            try {
            if (active.isBot) {
                val myId = backend.currentUserId().orEmpty()
                botChatSequence -= 1L
                botChat = botChat + ChatMessageDto(
                    id = botChatSequence,
                    roomId = active.id,
                    senderId = myId,
                    body = message.trim().take(300),
                    createdAt = Instant.now().toString(),
                )
                notice = ""
                SonHarfSoundFx.softNotify()
                delay(650)
                if (room?.id != active.id) return@launch
                botChatSequence -= 1L
                val reply = ChatMessageDto(
                    id = botChatSequence,
                    roomId = active.id,
                    senderId = "bot:${active.id}",
                    body = premierBotChatReply(language, message),
                    createdAt = Instant.now().toString(),
                )
                botChat = botChat + reply
                // The answer also pops up above the board, so it is seen without opening the chat.
                if (!showQuickChat) {
                    floatingMessage = reply
                    unreadChatCount += 1
                }
            } else {
                gameRequestResult {
                    backend.sendChat(active.id, message)
                    backend.getChat(active.id)
                }.onSuccess { refreshed ->
                    if (room?.id == active.id) {
                        chat = refreshed
                        GameChatBadge.update(active.id, refreshed.map { it.id to (it.senderId != backend.currentUserId()) }, open = showQuickChat)
                        unreadChatCount = GameChatBadge.unread
                        notice = ""
                    }
                }.onFailure { notice = premierError(language, it.message.orEmpty()) }
            }
            } finally { sendingChat = false }
        }
    }

    LaunchedEffect(Unit) {
        gameRequestResult {
            val player = ensureMe()
            val requested = SonHarfLaunchConfig.pendingRoomId
            SonHarfLaunchConfig.pendingRoomId = null
            val found = if (requested != null) backend.getRoom(requested).also {
                check(player.id == it.hostId || player.id == it.guestId) { "not_room_member" }
            } else backend.findPremierActiveRoom()
            val active = if (found?.isBot == true && found.isPremierLive()) {
                gameRequestResult { backend.resumePremierBotMatch(found.id) }.getOrDefault(found)
            } else {
                found
            }
            if (active != null) {
                adoptRoom(active, cinematic = false)
                notice = pt(language, "${player.displayName}, aktif maçına dönüldü.", "${player.displayName}, your active match was restored.")
            } else {
                stage = PremierStage.Lobby
            }
        }.onFailure {
            notice = pt(language, "Bağlantı kurulamadı.", "Could not connect.")
            stage = PremierStage.Lobby
        }
    }

    LaunchedEffect(room?.id) {
        val active = room ?: return@LaunchedEffect
        launch {
            backend.observeRoom(active.id)
                .catch { notice = pt(language, "Bağlantı yenileniyor…", "Reconnecting…") }
                .collect { next ->
                    if (acceptRoom(next) && next.isPremierFinished()) stage = PremierStage.Finished
                }
        }
        launch {
            backend.observeWords(active.id)
                .catch { }
                .collect { acceptWords(it) }
        }
        if (!active.isBot) launch {
            backend.observeChat(active.id)
                .catch { }
                .collect { next ->
                    if (room?.id != active.id) return@collect
                    val previousId = chat.lastOrNull()?.id
                    chat = next
                    GameChatBadge.update(active.id, next.map { it.id to (it.senderId != backend.currentUserId()) }, open = showQuickChat)
                    unreadChatCount = GameChatBadge.unread
                    val latest = next.lastOrNull()
                    if (latest != null && latest.id != previousId && latest.senderId != backend.currentUserId()) {
                        floatingMessage = latest

                    }
                }
        }
    }

    // The room and the word list are polled separately. When they disagree for a moment, fetch
    // both again; if fresh copies still disagree, stop gating so the player is never locked out.
    LaunchedEffect(stage, room?.id, room?.validWordCount, words.size) {
        val active = room ?: return@LaunchedEffect
        if (stage != PremierStage.Playing || words.size == active.validWordCount) return@LaunchedEffect
        delay(900)
        gameRequestResult { backend.getWords(active.id) }.getOrNull()?.let { acceptWords(it) }
        if (words.size == room?.validWordCount) return@LaunchedEffect
        gameRequestResult { backend.getRoom(active.id) }.getOrNull()?.let { acceptRoom(it) }
        delay(1_500)
        val latest = room ?: return@LaunchedEffect
        if (words.size != latest.validWordCount) syncGateOpenFor = premierSyncGateKey(latest, words)
    }

    LaunchedEffect(showQuickChat, chat, room?.id) {
        val active = room ?: return@LaunchedEffect
        if (!active.isBot) {
            GameChatBadge.update(active.id, chat.map { it.id to (it.senderId != backend.currentUserId()) }, open = showQuickChat)
            unreadChatCount = GameChatBadge.unread
        } else if (showQuickChat) unreadChatCount = 0
    }

    LaunchedEffect(floatingMessage?.id) {
        if (floatingMessage != null) {
            delay(2600)
            floatingMessage = null
        }
    }

    LaunchedEffect(moveFeedback) {
        if (moveFeedback != null) {
            delay(2200)
            moveFeedback = null
        }
    }

    LaunchedEffect(turnSeconds, stage, room?.disconnectedPlayerId, room?.currentPlayerId) {
        val reconnectGraceActive = room?.disconnectedPlayerId != null &&
            room?.disconnectedPlayerId == room?.currentPlayerId
        if (stage == PremierStage.Playing && !reconnectGraceActive && turnSeconds in 1..5) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(stage, room?.id) {
        if (stage == PremierStage.Vs && room != null) {
            delay(3000)
            if (room?.isPremierFinished() == true) stage = PremierStage.Finished else stage = PremierStage.Playing
        }
    }

    // Keyed on the round and the room's own word count, so a bot turn that follows another bot
    // turn (e.g. the bot opening a round right after it missed) still runs. The polled word list
    // is not a key: it lands a moment after the room row and used to restart the AI's round
    // break and "thinking" pause half-way.
    LaunchedEffect(stage, room?.id, room?.botTurn, room?.status, room?.roundNo, room?.validWordCount) {
        val active = room ?: return@LaunchedEffect
        val botPlayable = active.status in setOf("playing", "final", "sudden_death")
        // The AI moves only once the arena is on screen (never behind the VS screen).
        if (stage != PremierStage.Playing) return@LaunchedEffect
        if (!active.isBot || !active.botTurn || !botPlayable) return@LaunchedEffect

        // A new round that the bot opens starts with the same preparation break as any other.
        if (active.lastEvent == "round_started" || active.lastEvent == "sudden_death_started") {
            try {
                for (second in PREMIER_ROUND_PREP_SECONDS downTo 1) {
                    botPrepSeconds = second
                    delay(1_000)
                }
            } finally {
                botPrepSeconds = 0
            }
        }
        // The client asks the server bot to move (server play stays authoritative). It first
        // "thinks" for a human-like moment; no player clock runs during the bot's turn.
        botThinking = true
        try {
            delay(premierBotThinkMillis(active, words))
        } finally {
            botThinking = false
        }
        // Keep asking until the server bot moves; never leave the match waiting on a silent bot.
        var attempt = 0
        while (true) {
            val synced = gameRequestResult { backend.getRoom(active.id) }.getOrNull()
            if (synced != null && (
                    !synced.botTurn ||
                        synced.isPremierFinished() ||
                        synced.status !in setOf("playing", "final", "sudden_death")
                )
            ) {
                acceptRoom(synced)
                notice = ""
                return@LaunchedEffect
            }
            val advanced = gameRequestResult { backend.botTakeTurn(active.id) }.getOrNull()
            if (advanced != null) {
                acceptRoom(advanced)
                notice = ""
                return@LaunchedEffect
            }
            attempt += 1
            if (attempt >= 4) notice = pt(language, "Rakip hamlesi yeniden eşitleniyor…", "Resyncing rival move…")
            delay((700L + attempt * 250L).coerceAtMost(3_000L))
        }
    }

    // The match's first turn gets its full 15 seconds from the moment the arena shows.
    LaunchedEffect(stage, room?.id) {
        val active = room ?: return@LaunchedEffect
        if (stage != PremierStage.Playing) return@LaunchedEffect
        if (active.roundNo == 1 && active.validWordCount == 0 && !active.botTurn &&
            active.status in setOf("playing", "final", "sudden_death")
        ) {
            gameRequestResult { backend.activatePremierOpeningTurn(active.id) }.getOrNull()?.let { opened ->
                if (room?.id == opened.id) acceptRoom(opened)
            }
        }
    }

    LaunchedEffect(
        stage,
        room?.id,
        room?.turnDeadline,
        room?.currentPlayerId,
        room?.status,
        room?.botTurn,
        room?.disconnectedPlayerId,
        room?.reconnectDeadline,
    ) {
        val active = room ?: return@LaunchedEffect
        prepSeconds = 0
        // No clock (and no timeout claim) until the arena is on screen.
        if (stage != PremierStage.Playing) {
            turnSeconds = PREMIER_TURN_SECONDS
            return@LaunchedEffect
        }
        if (active.status !in setOf("playing", "final", "sudden_death") || active.botTurn) {
            turnSeconds = PREMIER_TURN_SECONDS
            return@LaunchedEffect
        }

        val reconnectDeadline = if (
            !active.isBot &&
            active.disconnectedPlayerId != null &&
            active.disconnectedPlayerId == active.currentPlayerId
        ) {
            com.sonharf.game.data.parseServerInstant(active.reconnectDeadline)
        } else {
            null
        }

        if (reconnectDeadline != null) {
            // The database clock is authoritative; phone wall-clock drift must not shorten reconnect grace.
            // The countdown ticks at once from the local estimate and is re-anchored when the server answers,
            // so a slow request never stalls the visible seconds.
            var initialReconnectMs = Duration.between(Instant.now(), reconnectDeadline).toMillis()
                .coerceIn(1_000L, PREMIER_RECONNECT_SECONDS * 1000L)
            var reconnectAnchor = SystemClock.elapsedRealtime()
            var reconnectClockResolved = false
            fun syncReconnectClock() {
                reconnectClockResolved = false
                launch {
                    val requestStartedAt = SystemClock.elapsedRealtime()
                    val reconnectClock = gameRequestResult { backend.getPremierReconnectClock(active.id) }.getOrNull()
                    val requestFinishedAt = SystemClock.elapsedRealtime()
                    if (reconnectClock != null) {
                        val halfRoundTripMs = ((requestFinishedAt - requestStartedAt) / 2L).coerceIn(0L, 750L)
                        initialReconnectMs = (reconnectClock.remainingMs - halfRoundTripMs).coerceAtLeast(0L)
                        reconnectAnchor = SystemClock.elapsedRealtime()
                    }
                    reconnectClockResolved = true
                }
            }
            syncReconnectClock()
            var reconnectAttempts = 0
            while (true) {
                val elapsedMs = SystemClock.elapsedRealtime() - reconnectAnchor
                val remaining = premierRemainingReconnectSecondsFromMillis(initialReconnectMs - elapsedMs)
                if (remaining > 0 || !reconnectClockResolved) {
                    turnSeconds = remaining.coerceAtLeast(1)
                    delay(250)
                    continue
                }

                turnSeconds = 1
                // Only a changed room is progress; an unchanged answer means the server still sees time left.
                val resolved = gameRequestResult { backend.heartbeatRoom(active.id) }.getOrNull()
                    ?.takeIf { premierTurnStateChanged(active, it) }
                    ?: gameRequestResult { backend.claimTurnTimeout(active.id) }.getOrNull()
                        ?.takeIf { premierTurnStateChanged(active, it) }
                if (resolved != null) {
                    acceptRoom(resolved)
                    notice = if (resolved.isPremierFinished()) {
                        pt(language, "Yeniden bağlanma süresi doldu. Maç sonuçlandı.", "Reconnect window expired. Match finished.")
                    } else {
                        ""
                    }
                    if (resolved.isPremierFinished()) stage = PremierStage.Finished
                    return@LaunchedEffect
                }

                reconnectAttempts += 1
                notice = pt(language, "Yeniden bağlanma durumu eşitleniyor…", "Syncing reconnect status…")
                // Re-anchor to the server clock (bounded back-off) and keep counting from what it reports.
                syncReconnectClock()
                delay(if (reconnectAttempts < 8) 600L else 2_000L)
            }
        }

        val deadline = com.sonharf.game.data.parseServerInstant(active.turnDeadline)
        if (deadline == null) {
            turnSeconds = PREMIER_TURN_SECONDS
            gameRequestResult { backend.getRoom(active.id) }.getOrNull()?.let { synced ->
                if (synced != active) acceptRoom(synced)
            }
            return@LaunchedEffect
        }

        // Anchor the visible countdown to the database clock rather than the phone wall clock.
        // Phone clock drift must not shorten the authoritative 15-second turn. The seconds tick at
        // once from the local estimate; the server's remaining_ms re-anchors them when it answers,
        // so neither a slow request nor clock skew can stall or freeze the countdown.
        var initialRemainingMs = Duration.between(Instant.now(), deadline).toMillis()
            .coerceIn(1_000L, (PREMIER_TURN_SECONDS + PREMIER_ROUND_PREP_SECONDS) * 1000L)
        var countdownAnchor = SystemClock.elapsedRealtime()
        var serverClockResolved = false
        fun syncTurnClock() {
            serverClockResolved = false
            launch {
                val requestStartedAt = SystemClock.elapsedRealtime()
                val serverClock = gameRequestResult { fetchPremierTurnClock(active.id) }.getOrNull()
                val requestFinishedAt = SystemClock.elapsedRealtime()
                if (serverClock != null) {
                    val halfRoundTripMs = ((requestFinishedAt - requestStartedAt) / 2L).coerceIn(0L, 750L)
                    initialRemainingMs = (serverClock.remainingMs - halfRoundTripMs).coerceAtLeast(0L)
                    countdownAnchor = SystemClock.elapsedRealtime()
                }
                serverClockResolved = true
            }
        }
        syncTurnClock()
        var claimAttempts = 0

        while (true) {
            val elapsedMs = SystemClock.elapsedRealtime() - countdownAnchor
            // A new round begins with preparation time on top of the 15-second turn: show it
            // separately so the turn clock itself always counts down from 15. Only the server
            // clock may open the break, so a skewed phone clock never flashes it.
            val prepMs = initialRemainingMs - elapsedMs - PREMIER_TURN_SECONDS * 1000L
            prepSeconds = if (serverClockResolved && prepMs > 0L) ((prepMs + 999L) / 1000L).toInt() else 0
            val remaining = premierRemainingTurnSecondsFromMillis(initialRemainingMs - elapsedMs)
            if (remaining > 0 || !serverClockResolved) {
                turnSeconds = remaining.coerceAtLeast(1)
                delay(250)
                continue
            }

            // Never present 00 as an actionable live turn. Keep the final visible tick while
            // the server confirms expiry or sends the next authoritative room state.
            turnSeconds = 1

            val synced = gameRequestResult { backend.getRoom(active.id) }.getOrNull()
            if (synced != null && premierTurnStateChanged(active, synced)) {
                acceptRoom(synced)
                notice = ""
                return@LaunchedEffect
            }

            val advanced = gameRequestResult { backend.claimTurnTimeout(active.id) }.getOrNull()
            if (advanced != null && premierTurnStateChanged(active, advanced)) {
                acceptRoom(advanced)
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

            // The server answered with the same room: by its clock the turn is not over yet (clock
            // skew, or our countdown ran early). Re-anchor to its remaining time and retry shortly.
            claimAttempts += 1
            if (advanced == null || claimAttempts >= 3) {
                notice = pt(language, "Maç yeniden eşitleniyor…", "Resyncing match…")
            }
            syncTurnClock()
            delay(if (claimAttempts < 8) 500L else 2_000L)
        }
    }

    DisposableEffect(room?.id, stage) {
        SonHarfUiState.inMatch = room?.isPremierLive() == true && stage in setOf(PremierStage.Vs, PremierStage.Playing)
        onDispose { SonHarfUiState.inMatch = false }
    }

    BackHandler {
        val active = room
        if (active != null && active.isPremierLive() && stage in setOf(PremierStage.Vs, PremierStage.Playing)) {
            showForfeit = true
        } else {
            SonHarfUiState.homeRequest += 1
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background)))
    ) {
        when (stage) {
            PremierStage.Loading -> PremierLoading(language)
            PremierStage.Lobby -> PremierLobby(
                language = language,
                profile = me,
                notice = notice,
                busy = busy,
                onLanguage = { next ->
                    language = next
                    SonHarfUiState.language = next
                    input = ""
                },
                onPlay = {
                    if (busy) return@PremierLobby
                    busy = true
                    scope.launch {
                        notice = ""
                        // The search outlives the default 12 s request guard: the server opens the AI duel
                        // at 15 s, so the guard used to end the first search just before a rival was ready.
                        gameRequestResult(timeoutMillis = 120_000L) {
                            // A stalled connection must never leave the play button locked.
                            kotlinx.coroutines.withTimeout(15_000) {
                                ensureMe()
                                backend.startRandomMatchmaking(language)
                            }
                            stage = PremierStage.Searching
                            var misses = 0
                            while (stage == PremierStage.Searching) {
                                // One slow or failed poll is retried; only a lasting outage ends the search.
                                val found = runCatching { kotlinx.coroutines.withTimeout(8_000) { backend.pollRandomMatchmakingRoom() } }
                                    .onFailure { if (it is kotlinx.coroutines.CancellationException && it !is kotlinx.coroutines.TimeoutCancellationException) throw it }
                                    .onSuccess { misses = 0 }
                                    .getOrElse { if (++misses >= 4) throw it; null }
                                if (found != null) {
                                    adoptRoom(found, cinematic = true)
                                    SonHarfSoundFx.softNotify()
                                    break
                                }
                                delay(750)
                            }
                        }.onFailure {
                            stage = PremierStage.Lobby
                            notice = if (it is kotlinx.coroutines.TimeoutCancellationException) {
                                pt(language, "Bağlantı yavaş. Tekrar OYNA'ya bas.", "Slow connection. Tap PLAY again.")
                            } else {
                                premierError(language, it.message.orEmpty())
                            }
                        }
                        busy = false
                    }
                },
                onHome = { SonHarfUiState.homeRequest += 1 },
            )
            PremierStage.Searching -> PremierSearching(
                language = language,
                me = me,
                onCancel = {
                    scope.launch {
                        stage = PremierStage.Lobby
                        gameRequestResult { backend.cancelRandomMatchmaking() }
                        notice = pt(language, "Eşleşme iptal edildi.", "Matchmaking cancelled.")
                    }
                },
            )
            PremierStage.Vs -> {
                val active = room
                if (active != null) PremierVsScreen(language, me, opponent, active)
            }
            PremierStage.Playing -> {
                val active = room
                if (active != null) {
                    // The letter to play comes from the word list and the turn from the room: accept input
                    // only while both describe the same move (the server keeps words.size == valid_word_count).
                    val boardSynced = words.size == active.validWordCount ||
                        syncGateOpenFor == premierSyncGateKey(active, words)
                    PremierArena(
                        language = language,
                        room = active,
                        boardSynced = boardSynced,
                        me = me,
                        opponent = opponent,
                        meId = backend.currentUserId(),
                        words = words,
                        input = input,
                        notice = notice,
                        busy = busy,
                        turnSeconds = turnSeconds,
                        unreadChat = unreadChatCount,
                        floatingMessage = floatingMessage,
                        moveFeedback = moveFeedback,
                        botThinking = botThinking,
                        onQuickMessage = { sendChatMessage(it) },
                        prepSeconds = maxOf(prepSeconds, botPrepSeconds),
                        onInput = { input = it },
                        onForfeit = { showForfeit = true },
                        onQuickChat = {
                            unreadChatCount = 0
                            GameChatBadge.markRead()
                            showQuickChat = true
                        },
                        onSubmit = {
                            val live = room ?: return@PremierArena
                            if (busy || input.isBlank() || !boardSynced ||
                                live.currentPlayerId != backend.currentUserId() || live.botTurn ||
                                !live.isPremierLive() || premierTurnStateChanged(active, live)) return@PremierArena
                            // Obvious slips are caught locally: a rejected word would cost the turn.
                            val localProblem = premierLocalRejection(
                                input,
                                premierRequiredToken(active, words),
                                words.map { it.normalizedWord.ifBlank { it.word } },
                                language,
                            )
                            if (localProblem != null) {
                                moveFeedback = PremierMoveFeedback(
                                    accepted = false,
                                    message = validationMessage(language, localProblem),
                                )
                                SonHarfSoundFx.wrongWord()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                return@PremierArena
                            }
                            busy = true
                            val candidate = input
                            scope.launch {
                                // Clear immediately when Send is pressed. The server remains authoritative
                                // for the result, but stale text must never survive into the rival turn.
                                input = ""
                                notice = ""
                                gameRequestResult { backend.submitPremierWord(active.id, candidate) }
                                    .onSuccess { next ->
                                        acceptRoom(next)
                                        val accepted = next.validWordCount > active.validWordCount
                                        if (accepted) {
                                            notice = ""
                                            moveFeedback = PremierMoveFeedback(
                                                accepted = true,
                                                message = "${pt(language, "DOĞRU", "CORRECT")} • ${premierUpper(candidate, language)}",
                                            )
                                            SonHarfSoundFx.wordAccepted()
                                        } else {
                                            val reason = next.lastEvent.orEmpty()
                                            notice = ""
                                            moveFeedback = PremierMoveFeedback(
                                                accepted = false,
                                                message = "${pt(language, "YANLIŞ", "WRONG")} • ${validationMessage(language, reason)}",
                                            )
                                            SonHarfSoundFx.wrongWord()
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    .onFailure { error ->
                                        // A dropped connection may still have delivered the word: re-check the room first.
                                        val synced = gameRequestResult { backend.getRoom(active.id) }.getOrNull()
                                        if (synced != null && synced.validWordCount > active.validWordCount) {
                                            acceptRoom(synced)
                                            notice = ""
                                            moveFeedback = PremierMoveFeedback(
                                                accepted = true,
                                                message = "${pt(language, "DOĞRU", "CORRECT")} • ${premierUpper(candidate, language)}",
                                            )
                                            SonHarfSoundFx.wordAccepted()
                                        } else {
                                            if (synced != null) acceptRoom(synced)
                                            notice = premierError(language, error.message.orEmpty())
                                        }
                                    }
                                busy = false
                            }
                        },
                    )
                }
            }
            PremierStage.Finished -> {
                val active = room
                if (active != null) PremierResult(
                    language = language,
                    room = active,
                    meId = backend.currentUserId(),
                    busy = busy,
                    notice = notice,
                    playerName = me?.displayName,
                    playerGender = me?.gender,
                    onRematch = {
                        if (busy) return@PremierResult
                        busy = true
                        scope.launch {
                            if (active.isBot) {
                                gameRequestResult { backend.restartBotMatch(active.id) }
                                    .onSuccess { adoptRoom(it, cinematic = true) }
                                    .onFailure { notice = premierError(language, it.message.orEmpty()) }
                            } else {
                                gameRequestResult { backend.requestRematch(active.id) }
                                    .onSuccess {
                                        notice = pt(language, "Rövanş teklifi gönderildi.", "Rematch request sent.")
                                        for (attempt in 0 until 16) {
                                            delay(750)
                                            val next = gameRequestResult { backend.findPremierActiveRoom() }.getOrNull()
                                            if (next != null && next.id != active.id) {
                                                adoptRoom(next, cinematic = true)
                                                break
                                            }
                                        }
                                    }
                                    .onFailure { notice = premierError(language, it.message.orEmpty()) }
                            }
                            busy = false
                        }
                    },
                    onHome = { SonHarfUiState.homeRequest += 1 },
                )
            }
        }
    }

    if (showForfeit) {
        HfConfirmDialog(
            title = pt(language, "Pes etmek istiyor musun?", "Surrender this match?"),
            message = pt(language, "Pes edersen maç hemen rakibin lehine biter. Bu işlem geri alınamaz.", "Surrendering ends the match immediately in your opponent's favor. This cannot be undone."),
            confirmText = pt(language, "EVET, PES ET", "YES, SURRENDER"),
            dismissText = pt(language, "OYUNDA KAL", "KEEP PLAYING"),
            onDismiss = { showForfeit = false },
            onConfirm = {
                showForfeit = false
                val active = room
                if (active != null) {
                    busy = true
                    scope.launch {
                        gameRequestResult { backend.forfeit(active.id) }
                            .onSuccess { acceptRoom(it); stage = PremierStage.Finished }
                            .onFailure { notice = premierError(language, it.message.orEmpty()) }
                        busy = false
                    }
                }
            },
        )
    }

    if (showQuickChat && room != null) {
        PremierChatSheet(
            language = language,
            messages = if (room?.isBot == true) botChat else chat,
            meId = backend.currentUserId(),
            isBot = room?.isBot == true,
            onDismiss = { showQuickChat = false },
            onSend = { message -> sendChatMessage(message) },
        )
    }
}

@Composable
internal fun PremierLobby(
    language: String,
    profile: ProfileDto?,
    notice: String,
    busy: Boolean,
    onLanguage: (String) -> Unit,
    onPlay: () -> Unit,
    onHome: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val owned = WordSiegeMascotOwnership.owned
    val skin = remember(MascotSkinChoice.version) { WordSiegeMascotBond(context).skinChoice?.takeIf { it in owned } ?: owned.minByOrNull { it.ordinal } }
    var waveKey by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { delay(600); waveKey = 1L }
    Column(
        Modifier.fillMaxSize().background(LobbyPalette.Ground).statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onHome) { Icon(Icons.Rounded.ArrowBack, null, tint = LobbyPalette.Ink) }
            HfGameArt(R.drawable.son_harf_game_icon, 64.dp, 64.dp, description = "Son Harf")
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("SON HARF", color = LobbyPalette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(pt(language, "1v1 KELİME DÜELLOSU", "1v1 WORD DUEL"), color = LobbyPalette.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            }
            PremierLanguageSwitch(language, onLanguage)
        }
        Spacer(Modifier.height(12.dp))
        // Everything above the play button scrolls if the phone is short; the button never moves.
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GameEventStage {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ProfilePhotoAvatarWithGender(profile?.avatarPath,profile?.gender,profile?.displayName?:pt(language,"Oyuncu","Player"),56.dp,visible=profile?.avatarVisibility!="hidden",frameId=SonHarfCosmetics.profileFrameId)
                    Column(Modifier.weight(1f)) {
                        Text(profile?.displayName?:pt(language,"Oyuncu","Player"),color=Color.White,fontSize=18.sp,style=premiumNameStyle(SonHarfCosmetics.nameStyleId),fontWeight=if(SonHarfCosmetics.nameStyleId==null)FontWeight.Black else null,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text("${profile?.rating?:1000} RP",color=EventGold,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    }
                    EventTag("1v1")
                }
                Text(pt(language,"Son harf senin hamlen.","The last letter is your next move."),color=Color.White,fontSize=28.sp,lineHeight=31.sp,fontWeight=FontWeight.Black)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(pt(language,"Zinciri koparma.\nÜç raundun galibi ol.","Keep the chain alive.\nWin the three-round duel."),color=Color.White.copy(alpha=.9f),fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold)
                        EventTag(pt(language,"15 SANİYE · 10 HAMLE","15 SECONDS · 10 TURNS"),gold=false)
                    }
                    if(skin!=null)WordSiegeMascot(moveId=null,lastMoveMine=false,pendingCells=emptyList(),playerTurn=true,requestedEmotion=WordSiegeMascotEmotion.HAPPY,modifier=Modifier.size(114.4.dp),actionKey=waveKey,action=WordSiegeMascotAction.WAVE,skin=skin)
                }
            }

            PremierHowToPlay(language)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremierFeatureTile(Icons.Rounded.Verified, pt(language, "ANA SÖZLÜK", "MASTER DICTIONARY"), pt(language, "TR + EN", "TR + EN"), Modifier.weight(1f))
                PremierFeatureTile(Icons.Rounded.Bolt, pt(language, "HIZLI", "FAST"), pt(language, "15 sn tur", "15 sec turn"), Modifier.weight(1f))
                PremierFeatureTile(Icons.Rounded.Groups, pt(language, "CANLI", "LIVE"), "1v1", Modifier.weight(1f))
            }
            if (notice.isNotBlank()) {
                Surface(shape = RoundedCornerShape(15.dp), color = LobbyPalette.Soft, border = BorderStroke(1.dp, LobbyPalette.Line)) {
                    Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = LobbyPalette.Ink, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val playPulse = rememberInfiniteTransition(label = "play").animateFloat(1f, 1.025f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "play-pulse")
        Box(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .graphicsLayer { if (!busy) { scaleX = playPulse.value; scaleY = playPulse.value } }
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PremierUi.Green)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF367C54), LobbyPalette.Green, Color(0xFF22553A))))
                .border(1.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(20.dp))
                .clickable(enabled = !busy, onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                Text(pt(language, "OYNA", "PLAY"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(pt(language, "Rakip bulunamazsa seviye uyumlu AI devreye girer.", "If no rival is found, a level-appropriate AI takes over."), Modifier.fillMaxWidth(), color = LobbyPalette.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

/**
 * The rule as a staircase: each word starts right under the last letter of the previous one, and
 * the shared letters are gold. Centred and symmetric.
 */
@Composable
private fun PremierHowToPlay(language: String) {
    val chain = if (language == "en") listOf("APPLE", "EAGLE", "EARTH") else listOf("KALEM", "MASA", "ARI")
    var step by remember(language){mutableIntStateOf(0)}
    LaunchedEffect(language){while(true){delay(1800);step=(step+1)%3}}
    Surface(shape=RoundedCornerShape(20.dp),color=LobbyPalette.Paper,border=BorderStroke(1.dp,PremierBoard.TileEdge)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text(pt(language,"ZİNCİRİ BÖYLE KUR","BUILD THE CHAIN"),color=LobbyPalette.Ink,fontSize=12.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                Icon(Icons.Rounded.Link,null,tint=PremierUi.Green,modifier=Modifier.size(22.dp))
            }
            chain.forEachIndexed { index,word->
                val active=index==step
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("${index+1}",Modifier.width(18.dp),color=PremierUi.Green,fontSize=12.sp,fontWeight=FontWeight.Black)
                    Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                        word.forEachIndexed { i,ch->
                            val link=i==word.lastIndex||index>0&&i==0
                            val lift by animateFloatAsState(if(active&&link)-4f else 0f,tween(350),label="rule-letter")
                            Box(Modifier.graphicsLayer{translationY=lift}.size(32.dp).shadow(2.dp,RoundedCornerShape(7.dp))
                                .clip(RoundedCornerShape(7.dp)).background(Brush.verticalGradient(if(link)listOf(Color(0xFFFFEAB0),EventGold)else listOf(PremierBoard.Tile,PremierBoard.BoardBottom)))
                                .border(1.dp,PremierBoard.TileEdge,RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center) {
                                Text(ch.toString(),color=if(link)EventInk else PremierBoard.TileInk,fontSize=17.sp,fontWeight=FontWeight.Black)
                            }
                        }
                    }
                    if(active)Icon(Icons.Rounded.ArrowBack,null,tint=PremierUi.Green,modifier=Modifier.size(18.dp))
                }
            }
            Text(pt(language,"KALEM → M ile MASA → A ile ARI","APPLE → E starts EAGLE → E starts EARTH"),color=PremierUi.Green,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Text(pt(language,"Son harften başla. Tekrar kullanmadan yeni kelime kur. Uzun kelimelerle daha çok puan kazan.","Start with the last letter. Build a new word without repeating. Longer words earn more points."),color=LobbyPalette.Muted,fontSize=12.sp,lineHeight=17.sp)
        }
    }
}

@Composable
private fun PremierFeatureTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(17.dp), color = LobbyPalette.Paper, border = BorderStroke(1.dp, PremierBoard.TileEdge),shadowElevation=2.dp) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = PremierUi.Green, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = LobbyPalette.Ink, fontWeight = FontWeight.Black, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
            Text(detail, color = LobbyPalette.Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PremierLanguageSwitch(language: String, onLanguage: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(99.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Border)) {
        Row(Modifier.padding(3.dp)) {
            listOf("tr" to "TR", "en" to "EN").forEach { (code, label) ->
                Surface(
                    modifier = Modifier.clickable { onLanguage(code) },
                    shape = RoundedCornerShape(99.dp),
                    color = if (language == code) PremierUi.Ocean else Color.Transparent,
                ) {
                    Text(label, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = if (language == code) Color.White else PremierUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PremierLoading(language: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = PremierUi.Ocean)
        Spacer(Modifier.height(14.dp))
        Text(pt(language, "Premier arena hazırlanıyor…", "Preparing Premier arena…"), color = PremierUi.Ink, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun PremierSearching(language: String, me: ProfileDto?, onCancel: () -> Unit) {
    val transition=rememberInfiniteTransition(label="search")
    val ripple=transition.animateFloat(0f,1f,infiniteRepeatable(tween(1800,easing=LinearEasing)),label="ripple")
    val wave=transition.animateFloat(0f,1f,infiniteRepeatable(tween(1600,easing=LinearEasing)),label="wave")
    val dots by transition.animateFloat(0f,3.99f,infiniteRepeatable(tween(1500,easing=LinearEasing)),label="dots")
    Column(Modifier.fillMaxSize().background(SonHarfTheme.Background).statusBarsPadding().navigationBarsPadding().padding(horizontal=18.dp,vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            HfGameArt(R.drawable.son_harf_game_icon,56.dp,56.dp,description="Son Harf")
            Text("SON HARF",color=PremierUi.Ink,fontSize=24.sp,fontWeight=FontWeight.Black)
        }
        Spacer(Modifier.weight(1f))
        GameEventStage {
            EventTag(pt(language,"DÜELLO ARENASI","DUEL ARENA"))
            Text(pt(language,"Rakip aranıyor","Finding a rival")+".".repeat(dots.toInt()),color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black)
            Row(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    ProfilePhotoAvatarWithGender(me?.avatarPath,me?.gender,me?.displayName?:pt(language,"Oyuncu","Player"),88.dp,accent=Hf.Green,visible=me?.avatarVisibility!="hidden",showGenderBadge=false,frameId=SonHarfCosmetics.profileFrameId)
                    Text(me?.displayName?:pt(language,"Sen","You"),color=Color.White,fontSize=14.sp,style=premiumNameStyle(SonHarfCosmetics.nameStyleId),fontWeight=if(SonHarfCosmetics.nameStyleId==null)FontWeight.Black else null,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("${me?.rating?:1000} RP",color=EventGold,fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
                Text("VS",Modifier.padding(horizontal=4.dp),color=EventGold,fontSize=26.sp,fontWeight=FontWeight.Black)
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(93.dp),contentAlignment=Alignment.Center) {
                        Canvas(Modifier.matchParentSize()) {
                            repeat(3){i->val t=(ripple.value+i/3f)%1f;drawCircle(EventGold.copy(alpha=(1f-t)*.45f),radius=size.minDimension*(.3f+.2f*t),style=Stroke(2.dp.toPx()))}
                        }
                        Box(Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(alpha=.10f)).border(1.dp,EventGold.copy(alpha=.5f),CircleShape),contentAlignment=Alignment.Center){Text("?",color=EventGold,fontSize=35.sp,fontWeight=FontWeight.Black)}
                    }
                    Text(pt(language,"Sıradaki rakip","Next rival"),color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Black)
                    Text(pt(language,"Bağlantı kuruluyor","Connecting"),color=EventGold,fontSize=10.sp)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp,Alignment.CenterHorizontally)) {
                pt(language,"SONHARF","LETTER").forEachIndexed{index,ch->
                    Box(Modifier.graphicsLayer{val phase=((wave.value*7-index)/2.2f).let{it-kotlin.math.floor(it)};translationY=-8.dp.toPx()*kotlin.math.sin(phase*Math.PI).toFloat().coerceAtLeast(0f)}){HfLetterTile(ch.toString(),30.dp,fontSize=17.sp)}
                }
            }
            Text(pt(language,"Rating'ine uygun rakip eşleştiriliyor. İlk kelimeyle zinciri başlat.","Finding a rival with a similar rating. Start the chain with your first word."),color=Color.White.copy(alpha=.85f),fontSize=12.sp,lineHeight=17.sp,textAlign=TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        Text(pt(language,"Rakip bulunamazsa AI ile düello başlar.","If no rival is found, an AI duel begins."),color=PremierUi.Muted,fontSize=11.sp,textAlign=TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        HfSecondaryButton(pt(language,"Vazgeç","Cancel"),onClick=onCancel,modifier=Modifier.fillMaxWidth())
    }
}

@Composable
internal fun PremierVsScreen(language: String, me: ProfileDto?, opponent: ProfileDto?, room: GameRoomDto) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Text(pt(language, "RAKİP BULUNDU!", "RIVAL FOUND!"), color = PremierUi.Ocean, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
        Text(pt(language, "Maç 3 saniye içinde başlıyor", "Match starts in 3 seconds"), color = PremierUi.Muted, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        PremierVsPlayerCard(language, me?.displayName ?: pt(language, "Oyuncu", "Player"), me?.avatarPath, me?.gender, me?.avatarVisibility != "hidden", me?.rating ?: 1000, profileWinRate(me), PremierUi.Green, nameColor = SonHarfCosmetics.playerNameColor, nameEmblem = true, frameId = rememberPlayerFrame(me?.id), userId = me?.id)
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(99.dp), color = Color.Transparent) {
            Box(Modifier.background(Brush.horizontalGradient(listOf(PremierUi.OceanDeep, Color(0xFF2C3E55)))).padding(horizontal = 27.dp, vertical = 10.dp)) {
                Text("VS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (room.isBot) {
            PremierVsPlayerCard(language, room.botName?.replace("KelimeBot", "KelimeAI")?.replace("WordBot", "WordAI") ?: pt(language, "KelimeAI", "WordAI"), null, null, true, (me?.rating ?: 1000), 50, PremierUi.Red, bot = true)
        } else {
            PremierVsPlayerCard(language, opponent?.displayName ?: pt(language, "Rakip", "Rival"), opponent?.avatarPath, opponent?.gender, opponent?.avatarVisibility != "hidden", opponent?.rating ?: 1000, profileWinRate(opponent), PremierUi.Red, frameId = rememberPlayerFrame(opponent?.id), userId = opponent?.id)
        }
        Spacer(Modifier.weight(1f))
        Text(pt(language, "Zincir başlıyor. İlk hamlene hazırlan.", "The chain begins. Get ready for your first move."), color = PremierUi.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun PremierVsPlayerCard(language: String, name: String, avatar: String?, gender: String?, visible: Boolean, rating: Int, winRate: Int, accent: Color, bot: Boolean = false, nameColor: Color = PremierUi.Ink, nameEmblem: Boolean = false, frameId: String? = null, userId: String? = null) {
    val entrance=remember{Animatable(0f)}
    LaunchedEffect(Unit){entrance.animateTo(1f,spring(dampingRatio=.82f,stiffness=260f))}
    GameEventStage(modifier=Modifier.graphicsLayer{alpha=entrance.value;translationX=(1f-entrance.value)*if(bot)-60f else 60f},gold=true) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if (bot) PremierBotAvatar(size = 70.dp, accent = accent, name = name)
            else ProfilePhotoAvatarWithGender(avatar,gender,name,70.dp,accent=accent,visible=visible,frameId=frameId)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(name,Modifier.weight(1f,fill=false),color=if(nameColor==PremierUi.Ink)EventInk else nameColor,fontSize=21.sp,style=premiumNameStyle(if(nameEmblem) SonHarfCosmetics.nameStyleId else null),maxLines=1,overflow=TextOverflow.Ellipsis)
                    if(nameEmblem&&!bot){Spacer(Modifier.width(4.dp));NameStyleEmblem(20.dp)}
                    if(!bot){Spacer(Modifier.width(4.dp));AdminBadge(userId,18.dp)}
                }
                Text(if(bot)pt(language,"AI RAKİP","AI RIVAL")else pt(language,"DÜELLO OYUNCUSU","DUEL PLAYER"),color=accent,fontSize=10.sp,fontWeight=FontWeight.Black)
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    if(bot)EventTag("AI")else {
                        PremierStatPill("$rating RP",EventInk)
                        PremierStatPill("%$winRate",accent)
                    }
                }
            }
        }
    }

}

@Composable
private fun PremierStatPill(text: String, accent: Color) {
    Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .09f)) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

/** Word-game palette: a calm teal board, cream letter tiles, one soft colour per player. */
private object PremierBoard {
    val BoardTop: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1E2127) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF7EEDC) else Color(0xFFF6F1E3)
    val BoardBottom: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF15171B) else if (SonHarfCosmetics.walnutTheme) Color(0xFFEADCC0) else Color(0xFFEFE7D2)
    val Tile: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1F2025) else if (SonHarfCosmetics.walnutTheme) Color(0xFFFAF3E3) else Color(0xFFF7E3A6)
    val TileEdge: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFB8903A) else if (SonHarfCosmetics.walnutTheme) Color(0xFFCDB58E) else Color(0xFFC9A560)
    val TileInk: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFF2C75C) else if (SonHarfCosmetics.walnutTheme) Color(0xFF2A2018) else Color(0xFF4A3217)
    val Gold = Color(0xFFE0A82E)
    val GoldEdge = Color(0xFFB07F1E)
    /** Text on a gold surface: always dark, so it reads in every theme. */
    val OnGold = Color(0xFF3A2400)
    val Ink: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFEEF2F6) else if (SonHarfCosmetics.walnutTheme) Color(0xFF3A2417) else Color(0xFF243142)
    val Muted: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFA3AFBD) else if (SonHarfCosmetics.walnutTheme) Color(0xFF7A6650) else Color(0xFF6B7A8C)
    val Mine = Color(0xFF3E9F4D)
    val MineSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1F3525) else if (SonHarfCosmetics.walnutTheme) Color(0xFFE3EBCF) else Color(0xFFE1F2E3)
    val Rival = Color(0xFFD0514A)
    val RivalSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF3A2322) else if (SonHarfCosmetics.walnutTheme) Color(0xFFF5DDD3) else Color(0xFFFBE4E2)
    val Danger = Color(0xFFD0514A)
    val Card: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1C222A) else if (SonHarfCosmetics.walnutTheme) Color(0xFFFAF3E3) else Color(0xFFFFFFFF)
    val CardBorder: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF313A46) else if (SonHarfCosmetics.walnutTheme) Color(0xFFCDB58E) else Color(0xFFD2DBE5)
}

/** Every turn is 15 seconds; a new round's first turn adds the preparation break on the server. */
internal fun premierTurnTotalSeconds(roundNo: Int, status: String): Int = PREMIER_TURN_SECONDS

/**
 * Checks a word locally before it is sent. A rejected word costs the turn, a point and the streak
 * on the server, so obvious slips (wrong opening, a repeat, too short, a Turkish word ending in Ğ)
 * are caught here and the player can simply correct them.
 */
internal fun premierLocalRejection(word: String, required: String, usedWords: Collection<String>, language: String): String? {
    val locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
    val clean = word.trim().lowercase(locale)
    if (clean.length < 2) return "invalid_length"
    if (required.isNotBlank() && required != "★" && !clean.startsWith(required.lowercase(locale))) return "wrong_start_letter"
    if (language != "en" && clean.endsWith("ğ")) return "ends_with_soft_g"
    if (usedWords.any { it.trim().lowercase(locale) == clean }) return "word_already_used"
    return null
}

@Composable
private fun PremierArena(
    language: String,
    room: GameRoomDto,
    /** False while the word list and the room describe different moves: input waits for both. */
    boardSynced: Boolean = true,
    me: ProfileDto?,
    opponent: ProfileDto?,
    meId: String?,
    words: List<GameWordDto>,
    input: String,
    notice: String,
    busy: Boolean,
    turnSeconds: Int,
    unreadChat: Int,
    floatingMessage: ChatMessageDto?,
    moveFeedback: PremierMoveFeedback?,
    botThinking: Boolean,
    prepSeconds: Int,
    onQuickMessage: (String) -> Unit,
    onInput: (String) -> Unit,
    onForfeit: () -> Unit,
    onQuickChat: () -> Unit,
    onSubmit: () -> Unit,
) {
    val isPro = me?.isVip == true
    val amHost = meId == room.hostId
    val myScore = if (amHost) room.hostScore else room.guestScore
    val rivalScore = if (amHost) room.guestScore else room.hostScore
    val myRoundScore = if (amHost) room.hostRoundScore else room.guestRoundScore
    val rivalRoundScore = if (amHost) room.guestRoundScore else room.hostRoundScore
    val myRoundWords = if (amHost) room.hostRoundWords else room.guestRoundWords
    val rivalRoundWords = if (amHost) room.guestRoundWords else room.hostRoundWords
    val myRounds = if (amHost) room.hostRounds else room.guestRounds
    val rivalRounds = if (amHost) room.guestRounds else room.hostRounds
    val myStreak = if (amHost) room.hostStreak else room.guestStreak
    val rivalStreak = if (amHost) room.guestStreak else room.hostStreak
    val reconnectGraceActive = !room.isBot &&
        room.disconnectedPlayerId != null &&
        room.disconnectedPlayerId == room.currentPlayerId &&
        room.reconnectDeadline != null
    val reconnectingMe = reconnectGraceActive && room.disconnectedPlayerId == meId
    val myTurn = room.currentPlayerId == meId && !room.botTurn && !reconnectingMe &&
        room.status in setOf("playing", "final", "sudden_death")
    val preparing = prepSeconds > 0
    val live = room.status in setOf("playing", "final", "sudden_death")
    val rivalName = if (room.isBot) room.botName?.replace("KelimeBot", "KelimeAI")?.replace("WordBot", "WordAI") ?: pt(language, "KelimeAI", "WordAI") else opponent?.displayName ?: pt(language, "Rakip", "Rival")
    val required = premierRequiredToken(room, words)
    val latestMove = words.lastOrNull()
    val latestPlayedWord = latestMove?.let { premierUpper(it.normalizedWord.ifBlank { it.word }, language) }.orEmpty()
    val latestMoveMine = latestMove != null && latestMove.playerId == meId

    // Mascot hints. Against a bot the hint is a real answer word: mascot owners get three free
    // ones, then banked (rewarded video) hints, then Son Coin. Against a real opponent a hint is
    // only a strategy tip (fair play), so it is free and never spends banked or bought hints.
    val hintContext = androidx.compose.ui.platform.LocalContext.current
    // Used, not left: ownership that loads after the match started still grants its free hints.
    var freeHintsUsed by remember(room.id) { mutableIntStateOf(0) }
    val hintsLeft = MascotHints.freeHintsLeft(realOpponent = !room.isBot, used = freeHintsUsed)
    var hintRequest by remember(room.id) { mutableStateOf<Pair<Int, String>?>(null) }
    fun showHintText(text: String) {
        hintRequest = ((hintRequest?.first ?: 0) + 1) to text
    }
    fun hintText(): String = if (room.isBot) {
        val prefix = if (required == "★") "" else required.lowercase(premierLocale(language))
        MascotHints.startWord(hintContext, room.language, prefix, words.map { it.normalizedWord.ifBlank { it.word } }.toSet())
    } else {
        MascotHints.tip((hintRequest?.first ?: 0) + 1 + words.size)
    }
    fun askFreeHint() {
        if (hintsLeft <= 0) return
        freeHintsUsed += 1
        showHintText(hintText())
    }
    // Free hints used up against a bot: one more costs Son Coin (server-checked).
    val hintScope = rememberCoroutineScope()
    var buyingHint by remember(room.id) { mutableStateOf(false) }
    fun buyHint() {
        if (buyingHint || !room.isBot) return
        buyingHint = true
        hintScope.launch {
            val bought = com.sonharf.game.data.GameHintBackend.buyHint("son_harf")
            if (bought != null) {
                showHintText(hintText())
            } else {
                showHintText(pt(language, "Jeton yetmedi... maç kazanıp biriktirelim mi?", "Not enough coins... let's win some matches?"))
            }
            buyingHint = false
        }
    }
    // Hints won with a rewarded video (outside the match) are spent before any coins, and only
    // where they reveal a word (against the AI).
    val bankedHints = if (room.isBot) RewardPassState.hints("son_harf") else 0
    fun useBankedHint() {
        if (buyingHint || !room.isBot) return
        buyingHint = true
        hintScope.launch {
            if (RewardPassState.useHint("son_harf")) {
                showHintText(hintText())
            } else {
                showHintText(pt(language, "İpucu şu an alınamadı, tekrar dene.", "Couldn't get a hint right now, try again."))
            }
            buyingHint = false
        }
    }
    // Without a mascot on screen the hint text is shown in the hint strip instead of a bubble.
    val mascotShown = WordSiegeMascotOwnership.hasAny
    var stripHint by remember(room.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(hintRequest?.first, mascotShown) {
        val text = hintRequest?.second
        if (text == null) {
            stripHint = null
            return@LaunchedEffect
        }
        stripHint = text
        delay(7_000)
        stripHint = null
    }

    // Real score changes from the server (streak and long-word bonuses included).
    var gainKey by remember(room.id) { mutableIntStateOf(0) }
    var myGain by remember(room.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var rivalGain by remember(room.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var seenMyScore by remember(room.id) { mutableIntStateOf(myScore) }
    var seenRivalScore by remember(room.id) { mutableIntStateOf(rivalScore) }
    LaunchedEffect(room.id, myScore, rivalScore) {
        val mine = myScore - seenMyScore
        val theirs = rivalScore - seenRivalScore
        seenMyScore = myScore
        seenRivalScore = rivalScore
        if (mine != 0) {
            gainKey += 1
            myGain = gainKey to mine
        }
        if (theirs != 0) {
            gainKey += 1
            rivalGain = gainKey to theirs
        }
    }
    val latestMoveScore = when {
        latestMove == null -> 0
        latestMoveMine -> myGain?.second?.coerceAtLeast(0) ?: 10
        else -> rivalGain?.second?.coerceAtLeast(0) ?: 10
    }

    // Sound: your turn, the rival's word, the last seconds.
    val initialMoveId = remember(room.id) { latestMove?.id }
    LaunchedEffect(latestMove?.id) {
        val move = latestMove ?: return@LaunchedEffect
        if (move.id != initialMoveId && move.playerId != meId) SonHarfSoundFx.rivalMove()
    }
    var wasMyTurn by remember(room.id) { mutableStateOf(myTurn) }
    LaunchedEffect(myTurn) {
        if (myTurn && !wasMyTurn) SonHarfSoundFx.turnStart()
        wasMyTurn = myTurn
    }
    LaunchedEffect(turnSeconds, myTurn, preparing) {
        if (myTurn && !preparing && !reconnectGraceActive && turnSeconds in 1..5) SonHarfSoundFx.clockTick()
    }

    // A rival's slip (a bot that could not find a word, or a rejected human word).
    var rivalSlip by remember(room.id) { mutableStateOf<String?>(null) }
    val slipSignature = when {
        room.lastEvent == "bot_missed" -> "bot:${room.roundNo}:$rivalScore:${words.size}"
        room.lastEvent in PREMIER_FAILURE_EVENTS && room.lastEventPlayerId != null && room.lastEventPlayerId != meId ->
            "rival:${room.lastEvent}:${room.roundNo}:$rivalScore:${words.size}"
        else -> null
    }
    val initialSlip = remember(room.id) { slipSignature }
    LaunchedEffect(slipSignature) {
        if (slipSignature == null || slipSignature == initialSlip) return@LaunchedEffect
        rivalSlip = if (room.lastEvent == "bot_missed") {
            pt(language, "$rivalName kelime bulamadı", "$rivalName found no word")
        } else {
            pt(language, "Rakip hata yaptı", "Rival slipped")
        }
        delay(2_300)
        rivalSlip = null
    }

    // Round results: a celebration or a bowed head, and the result shown during the break.
    var mascotRoundReaction by remember(room.id) { mutableStateOf<WordSiegeMascotEmotion?>(null) }
    var lastRoundWon by remember(room.id) { mutableStateOf<Boolean?>(null) }
    var seenMyRounds by remember(room.id) { mutableIntStateOf(myRounds) }
    var seenRivalRounds by remember(room.id) { mutableIntStateOf(rivalRounds) }
    LaunchedEffect(room.id, myRounds, rivalRounds) {
        val reaction = when {
            myRounds > seenMyRounds -> WordSiegeMascotEmotion.EXCITED
            rivalRounds > seenRivalRounds -> WordSiegeMascotEmotion.BOWED
            else -> null
        }
        val wonRound = myRounds > seenMyRounds
        seenMyRounds = myRounds
        seenRivalRounds = rivalRounds
        mascotRoundReaction = reaction
        if (reaction != null) {
            lastRoundWon = wonRound
            if (wonRound) SonHarfSoundFx.roundWon() else SonHarfSoundFx.roundLost()
            delay(2_600)
            mascotRoundReaction = null
        }
    }
    val mascotEmotion = when {
        mascotRoundReaction != null -> mascotRoundReaction
        // Ordinary correct words are just watched; only a strong word draws a proud look.
        moveFeedback?.accepted == true && latestMoveScore >= 12 -> WordSiegeMascotEmotion.PROUD
        moveFeedback?.accepted == false -> WordSiegeMascotEmotion.SAD
        myTurn && !preparing && turnSeconds in 1..5 -> WordSiegeMascotEmotion.STRESSED
        myTurn -> WordSiegeMascotEmotion.FOCUS
        else -> WordSiegeMascotEmotion.CALM
    }
    val mascotUrgency = if (myTurn && !preparing && turnSeconds in 1..5) (6 - turnSeconds) / 5f else 0f
    val mascotMomentum = (myScore - rivalScore) / maxOf(30, myScore + rivalScore).toFloat()
    // Streaks are detected from changes, so a new streak step or a broken streak is one signal.
    var mascotStreakSignal by remember(room.id) { mutableStateOf<WordSiegeMascotSignal?>(null) }
    var seenMyStreak by remember(room.id) { mutableIntStateOf(myStreak) }
    LaunchedEffect(room.id, myStreak) {
        val previous = seenMyStreak
        seenMyStreak = myStreak
        if (myStreak >= 3 && myStreak > previous) SonHarfSoundFx.streak()
        val next = when {
            myStreak >= 3 && myStreak > previous ->
                WordSiegeMascotSignal("streak:${room.id}:${room.roundNo}:$myStreak", WordSiegeMascotEvent.STREAK, count = myStreak)
            previous >= 3 && myStreak < previous ->
                WordSiegeMascotSignal("streak-lost:${room.id}:${words.size}", WordSiegeMascotEvent.STREAK_LOST)
            else -> null
        }
        mascotStreakSignal = next
        if (next != null) {
            delay(1_500)
            mascotStreakSignal = null
        }
    }
    val latestWordText = latestMove?.let { it.normalizedWord.ifBlank { it.word } }.orEmpty()
    // Moments the companion may respond to; it decides itself whether to speak.
    val mascotSignal = mascotStreakSignal ?: when {
        moveFeedback?.accepted == true -> WordSiegeMascotSignal(
            "ok:${room.id}:${room.validWordCount}",
            when {
                latestWordText.length >= 8 -> WordSiegeMascotEvent.RARE_WORD
                latestMoveScore >= 12 -> WordSiegeMascotEvent.BIG_PRAISE
                else -> WordSiegeMascotEvent.PRAISE
            },
            word = premierUpper(latestWordText, language),
        )
        moveFeedback?.accepted == false -> WordSiegeMascotSignal("no:${room.id}:${words.size}:${moveFeedback.message}", WordSiegeMascotEvent.COMFORT)
        myTurn && !preparing && turnSeconds in 1..5 -> WordSiegeMascotSignal("time:${room.id}:${room.roundNo}:${words.size}", WordSiegeMascotEvent.CRITICAL)
        latestMove != null && !latestMoveMine && latestMoveScore >= 12 ->
            WordSiegeMascotSignal("rival:${latestMove.id}", WordSiegeMascotEvent.RIVAL_STRONG)
        rivalScore - myScore >= 40 -> WordSiegeMascotSignal("behind:${room.id}:${room.roundNo}", WordSiegeMascotEvent.BEHIND)
        myScore - rivalScore >= 40 -> WordSiegeMascotSignal("ahead:${room.id}:${room.roundNo}", WordSiegeMascotEvent.AHEAD)
        else -> null
    }
    // Keeps the bot's chat replies aware of the match.
    SideEffect {
        PremierBotBrain.update(
            myScore = myScore,
            botScore = rivalScore,
            myStreak = myStreak,
            lastWord = latestPlayedWord,
            lastWordMine = latestMoveMine,
            round = room.roundNo,
        )
    }
    // The mascot's home perch is the slot beside the target tile; it can fly across the arena.
    var arenaOrigin by remember { mutableStateOf(Offset.Zero) }
    var arenaSize by remember { mutableStateOf(IntSize.Zero) }
    var mascotSlotCenter by remember { mutableStateOf<Offset?>(null) }
    val mascotTouches = remember { WordSiegeMascotTouchState() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                arenaOrigin = it.positionInRoot()
                arenaSize = it.size
            }
            .wordSiegeMascotTouchWatcher(mascotTouches)
    ) {
        val veryCompact = maxHeight < 610.dp
        val compact = maxHeight < 700.dp
        val tall = maxHeight > 820.dp
        val targetSize = if (veryCompact) 86.dp else if (compact) 100.dp else if (tall) 128.dp else 116.dp
        val mascotSize = if (veryCompact) 64.dp else if (compact) 72.dp else if (tall) 94.dp else 84.dp
        val keyHeight = if (veryCompact) 33.dp else if (compact) 35.dp else if (tall) 42.dp else 39.dp
        val primaryGap = if (veryCompact) 4.dp else if (compact) 6.dp else 10.dp

        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(PremierArenaSky.BackgroundTop, PremierArenaSky.BackgroundMid, PremierArenaSky.BackgroundBottom)
                    )
                )
                .statusBarsPadding()
        ) {
            // Top bar: surrender on the left, chat on the right, the round in the middle.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    TextButton(onClick = onForfeit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Icon(Icons.Rounded.Flag, null, tint = PremierBoard.Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(pt(language, "PES ET", "SURRENDER"), color = PremierBoard.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                PremierRoundPips(language, room.roundNo, myRounds, rivalRounds, room.status == "sudden_death")
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Box {
                        TextButton(onClick = onQuickChat, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Icon(Icons.Rounded.ChatBubbleOutline, null, tint = PremierBoard.Mine, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(pt(language, "SOHBET", "CHAT"), color = PremierBoard.Mine, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        ChatUnreadDot(unreadChat)
                    }
                }
            }
            // Mirrored player cards: you on the left, the rival on the right.
            PremierArenaHeader(
                language = language,
                room = room,
                me = me,
                opponent = opponent,
                rivalName = rivalName,
                myRoundScore = myRoundScore,
                rivalRoundScore = rivalRoundScore,
                myStreak = myStreak,
                rivalStreak = rivalStreak,
                myTurn = myTurn && !preparing,
                // The turn owner, not "not my turn": while my own connection is being rechecked on
                // my turn, the rival's card must not light up.
                rivalTurn = !preparing && live && (room.botTurn || (room.currentPlayerId != null && room.currentPlayerId != meId)),
                myGain = myGain,
                rivalGain = rivalGain,
            )
            // The turn clock as a bar across the screen.
            PremierPressureStrip(
                language = language,
                myScore = myRoundScore,
                rivalScore = rivalRoundScore,
                myStreak = myStreak,
                rivalStreak = rivalStreak,
                seconds = if (reconnectGraceActive || (!preparing && live)) turnSeconds else PREMIER_TURN_SECONDS,
                totalSeconds = if (reconnectGraceActive) PREMIER_RECONNECT_SECONDS else PREMIER_TURN_SECONDS,
                myTurn = myTurn && !preparing,
                active = live && !preparing,
                myWords = myRoundWords,
                rivalWords = rivalRoundWords,
            )
            Spacer(Modifier.height(primaryGap))
            // The board.
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                shadowElevation = 6.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(PremierBoard.BoardTop, PremierBoard.BoardBottom)))
                        .padding(horizontal = 12.dp, vertical = if (veryCompact) 6.dp else 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (reconnectGraceActive) {
                        PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                    } else {
                        PremierTurnBadge(language, myTurn && !preparing, room.status, botThinking && !preparing, rivalName)
                    }
                    Spacer(Modifier.weight(1f))
                    // Last word as tiles, its linking letters in gold. The right/wrong verdict is not
                    // repeated here (it held the new word back); the input, sound and mascot give it.
                    val slip = rivalSlip
                    Box(Modifier.fillMaxWidth().height(if (veryCompact) 44.dp else 52.dp), contentAlignment = Alignment.Center) {
                        PremierLastWordCard(
                            language = language,
                            word = latestPlayedWord,
                            mine = latestMoveMine,
                            linkLetters = if (latestPlayedWord.isBlank()) 0 else required.length,
                            veryCompact = veryCompact,
                        )
                    }
                    // A rival's slip is a small readable label here; the last word's tiles stay visible.
                    Box(Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.Center) {
                        if (slip != null) {
                            Text(
                                slip,
                                color = PremierBoard.Rival,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text("▼", color = PremierBoard.Ink.copy(alpha = .45f), fontSize = 12.sp)
                        }
                    }
                    // The letter to play, flanked by the round's word counts (mirrored).
                    Box(
                        modifier = Modifier.fillMaxWidth().height((if (targetSize > mascotSize) targetSize else mascotSize) + 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PremierTargetCard(
                            language = language,
                            required = required,
                            gameMode = room.gameMode,
                            round = room.roundNo,
                            size = targetSize,
                            active = myTurn && !preparing,
                            suddenDeath = room.status == "sudden_death",
                        )
                        Box(
                            Modifier.align(Alignment.CenterEnd).size(mascotSize).onGloballyPositioned { slot ->
                                val topLeft = slot.positionInRoot()
                                mascotSlotCenter = Offset(topLeft.x + slot.size.width / 2f, topLeft.y + slot.size.height / 2f)
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (!veryCompact) PremierWordTrail(words, language, isPro, meId)
                }
            }
            // Keep the live input and custom keyboard outside the flexible arena body.
            PremierInputBar(
                language,
                input,
                required,
                myTurn && !preparing && boardSynced,
                busy,
                usedWords = words,
                shakeKey = if (moveFeedback?.accepted == false) moveFeedback.message else null,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
            // One fixed-height strip for the notice and the hint chip, right above the keyboard: showing or hiding either never
            // resizes the arena above, so the centre card stays still between turns.
            val hintVisible = myTurn && !preparing && (hintsLeft > 0 || bankedHints > 0 || room.isBot)
            Box(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                val shownHint = stripHint
                if (shownHint != null) {
                    Text(
                        shownHint,
                        color = PremierBoard.Ink,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(if (hintVisible) .6f else 1f),
                    )
                } else if (notice.isNotBlank()) {
                    Text(
                        notice,
                        color = PremierBoard.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(if (hintVisible) .6f else 1f),
                    )
                }
                if (hintVisible) {
                    Surface(
                        onClick = {
                            when {
                                hintsLeft > 0 -> askFreeHint()
                                bankedHints > 0 -> useBankedHint()
                                room.isBot -> buyHint()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        shape = RoundedCornerShape(99.dp),
                        color = PremierBoard.Gold.copy(alpha = .18f),
                        border = BorderStroke(1.dp, PremierBoard.Gold.copy(alpha = .7f)),
                    ) {
                        Text(
                            if (hintsLeft > 0) pt(language, "💡 İpucu ($hintsLeft)", "💡 Hint ($hintsLeft)")
                            else if (bankedHints > 0) pt(language, "💡 İpucu ($bankedHints)", "💡 Hint ($bankedHints)")
                            else pt(language, "💡 +1 İpucu · ${com.sonharf.game.data.GameHintBackend.HINT_PRICE} SC", "💡 +1 Hint · ${com.sonharf.game.data.GameHintBackend.HINT_PRICE} SC"),
                            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            color = PremierBoard.Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            PremierKeyboard(language, input, enabled = myTurn && !busy && !preparing && boardSynced, keyHeight = keyHeight, onInput = onInput, onSubmit = onSubmit)
        }

        val slotCenter = mascotSlotCenter
        val mascotAnchors = if (slotCenter == null) emptyList() else listOf(Offset(.5f, .5f))
        val mascotDensity = LocalDensity.current
        val mascotBoxPx = with(mascotDensity) { mascotSize.toPx() }
        WordSiegeMascotCompanion(
            anchors = mascotAnchors,
            mascotSize = mascotSize,
            moveId = latestMove?.id,
            lastMoveMine = latestMoveMine,
            playerTurn = myTurn,
            modifier = Modifier.offset {
                androidx.compose.ui.unit.IntOffset(
                    ((slotCenter?.x ?: 0f) - arenaOrigin.x - mascotBoxPx / 2f).toInt(),
                    ((slotCenter?.y ?: 0f) - arenaOrigin.y - mascotBoxPx / 2f).toInt(),
                )
            }.size(mascotSize).clipToBounds(),
            moveScore = latestMoveScore,
            requestedEmotion = mascotEmotion,
            urgency = mascotUrgency,
            momentum = mascotMomentum,
            // Watch the keyboard on our turn, the rival's card on theirs.
            idleGazeX = if (myTurn) -.2f else .35f,
            idleGazeY = if (myTurn) .75f else -.85f,
            typingKey = input.length,
            signal = mascotSignal,
            playerName = me?.displayName,
            touches = mascotTouches,
            playerGender = me?.gender,
            hint = hintRequest,
            // Above the perch is the last-word row: speak below it so the rival's word stays readable.
            bubblePlacement = WordSiegeMascotBubblePlacement.PREFER_BELOW,
        )

        ArenaMoveImpact(eventKey = latestMove?.id?.toString(),
            label = if (latestMoveScore >= 12) pt(language, "ZİNCİR GÜÇLENİYOR · +$latestMoveScore", "CHAIN POWER · +$latestMoveScore")
                else pt(language, "${latestPlayedWord.uppercase(premierLocale(language))} · +$latestMoveScore", "${latestPlayedWord.uppercase(premierLocale(language))} · +$latestMoveScore"),
            accent = if (latestMoveMine) PremierBoard.Mine else PremierBoard.Danger,
            modifier = Modifier.matchParentSize(), bannerTop = 112.dp)
        ArenaCriticalFrame(turnSeconds, live && !preparing && !reconnectGraceActive, Modifier.matchParentSize())

        // Turn and accepted-word feedback live on the target tile itself (a light sweep), so no
        // screen-centred rings are drawn over the board.

        // Between rounds: a breather with a countdown and quick chat.
        AnimatedVisibility(
            visible = preparing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            PremierRoundPrep(
                language = language,
                round = room.roundNo,
                suddenDeath = room.status == "sudden_death",
                seconds = prepSeconds,
                lastRoundWon = lastRoundWon,
                myRounds = myRounds,
                rivalRounds = rivalRounds,
                youStart = room.currentPlayerId == meId,
                onQuickMessage = onQuickMessage,
                onOpenChat = onQuickChat,
            )
        }
        // Drawn after the round break so a message is read on top of it, never behind it.
        AnimatedVisibility(
            visible = floatingMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 118.dp, start = 22.dp, end = 22.dp),
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = PremierBoard.Card, border = BorderStroke(1.dp, PremierBoard.CardBorder), shadowElevation = 8.dp) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ChatBubbleOutline, null, tint = PremierBoard.Rival, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(floatingMessage?.body.orEmpty(), color = PremierBoard.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Tension: the screen edge beats red on the player's last five seconds.
        PremierHeartbeatEdge(active = myTurn && live && !preparing && turnSeconds in 1..5, seconds = turnSeconds)
    }
}

@Composable
private fun PremierHeartbeatEdge(active: Boolean, seconds: Int) {
    if (!active) return
    val beat = rememberInfiniteTransition(label = "heartbeat")
    val pulse by beat.animateFloat(
        0f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 700; 0f at 0; 1f at 120; .35f at 240; .85f at 340; 0f at 700 }),
        label = "heartbeat-pulse",
    )
    val strength = (6 - seconds) / 5f
    Canvas(Modifier.fillMaxSize()) {
        val edge = size.minDimension * .22f
        val color = Color(0xFFE5304A).copy(alpha = (.18f + .32f * strength) * pulse)
        drawRect(Brush.verticalGradient(listOf(color, Color.Transparent), endY = edge))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, color), startY = size.height - edge))
        drawRect(Brush.horizontalGradient(listOf(color, Color.Transparent), endX = edge))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, color), startX = size.width - edge))
    }
}

@Composable
private fun PremierStreakFlame(streak: Int, language: String, modifier: Modifier) {
    androidx.compose.animation.AnimatedVisibility(
        visible = streak >= 2,
        modifier = modifier,
        enter = androidx.compose.animation.scaleIn(spring(dampingRatio = .45f)) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
    ) {
        val settle = remember { Animatable(.96f) }
        LaunchedEffect(streak) {
            settle.snapTo(.96f)
            settle.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
        // A slim pill that fits the timer strip's reserved line.
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = PremierUi.GoldSoft,
            border = BorderStroke(.7.dp, PremierUi.Gold.copy(alpha = .45f)),
            modifier = Modifier.graphicsLayer { scaleX = settle.value; scaleY = settle.value },
        ) {
            Text(
                pt(language, "$streak SERİ", "$streak STREAK"),
                Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
                color = PremierBoard.Ink,
                style = TextStyle(
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                ),
                maxLines = 1,
            )
        }
    }
}

private val PREMIER_FAILURE_EVENTS = setOf(
    "invalid_word", "not_in_dictionary", "wrong_start_letter", "word_already_used", "ends_with_soft_g",
    "abbreviation_not_allowed", "proper_noun_not_allowed", "turn_expired", "timeout",
)

/** The bot's awareness of the match, so its chat replies fit the moment. */
private object PremierBotBrain {
    var myScore = 0
        private set
    var botScore = 0
        private set
    var myStreak = 0
        private set
    var lastWord = ""
        private set
    var lastWordMine = false
        private set
    var round = 1
        private set

    fun update(myScore: Int, botScore: Int, myStreak: Int, lastWord: String, lastWordMine: Boolean, round: Int) {
        this.myScore = myScore
        this.botScore = botScore
        this.myStreak = myStreak
        this.lastWord = lastWord
        this.lastWordMine = lastWordMine
        this.round = round
    }
}

// Snappy: the whole word lands in well under half a second.
private const val PREMIER_WORD_TILE_STAGGER_MS = 40L
private const val PREMIER_WORD_TILE_DROP_MS = 200L

/** How long the played word's tiles take to finish dropping in. */
private fun premierWordLandMillis(letters: Int): Long =
    (letters - 1).coerceAtLeast(0) * PREMIER_WORD_TILE_STAGGER_MS + PREMIER_WORD_TILE_DROP_MS

/** A human-like pause before the bot answers: quick for easy letters, longer when it "thinks". */
private fun premierBotThinkMillis(room: GameRoomDto, words: List<GameWordDto>): Long {
    val required = premierRequiredToken(room, words)
    val hardLetter = required.lowercase(Locale.ROOT) in setOf("ğ", "j", "ı", "ü", "z", "l", "v", "ö", "x", "q", "y", "k")
    // The bot waits until the word just played has fully landed and been readable for a moment.
    val lastWord = words.lastOrNull()?.let { it.normalizedWord.ifBlank { it.word } }.orEmpty()
    val landed = premierWordLandMillis(lastWord.length) + 450L
    val base = if (hardLetter) 1_000L else 500L
    return landed + base + kotlin.random.Random.nextLong(0L, 800L)
}

/** Three round pips (gold = you, coral = rival) around the round number. */
@Composable
private fun PremierRoundPips(language: String, round: Int, myRounds: Int, rivalRounds: Int, suddenDeath: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (suddenDeath) pt(language, "ANİ ÖLÜM", "SUDDEN DEATH") else pt(language, "RAUND $round/3", "ROUND $round/3"),
            color = if (suddenDeath) PremierBoard.Danger else PremierBoard.Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 3.dp)) {
            repeat(3) { index ->
                val color = when {
                    index < myRounds -> PremierBoard.Mine
                    index >= 3 - rivalRounds -> PremierBoard.Rival
                    else -> PremierBoard.CardBorder
                }
                Box(Modifier.size(width = 16.dp, height = 5.dp).clip(RoundedCornerShape(99.dp)).background(color))
            }
        }
    }
}

/** A round's word count beside the turn clock: label, n/10 and ten small progress segments. */
@Composable
private fun PremierWordCountChip(label: String, words: Int, color: Color, active: Boolean, modifier: Modifier = Modifier) {
    val count = words.coerceIn(0, 10)
    val glow by animateFloatAsState(if (active) 1f else 0f, tween(300), label = "count-glow")
    Surface(
        modifier = modifier.width(66.dp),
        shape = RoundedCornerShape(14.dp),
        color = PremierBoard.Card,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) color else PremierBoard.CardBorder),
        shadowElevation = (2f + 4f * glow).dp,
    ) {
        Column(
            Modifier
                .background(Brush.verticalGradient(listOf(color.copy(alpha = .10f + .08f * glow), PremierBoard.Card)))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, maxLines = 1)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = PremierBoard.Ink, fontSize = 16.sp)) { append("$count") }
                    withStyle(SpanStyle(color = PremierBoard.Muted, fontSize = 10.sp)) { append("/10") }
                },
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                repeat(10) { index ->
                    Box(
                        Modifier
                            .size(width = 3.5.dp, height = 4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (index < count) color else PremierBoard.CardBorder)
                    )
                }
            }
        }
    }
}

/** The break between rounds: last round's result, the score, a countdown and quick chat. */
@Composable
private fun PremierRoundPrep(
    language: String,
    round: Int,
    suddenDeath: Boolean,
    seconds: Int,
    lastRoundWon: Boolean?,
    myRounds: Int,
    rivalRounds: Int,
    youStart: Boolean,
    onQuickMessage: (String) -> Unit,
    onOpenChat: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "prep")
    val beat by pulse.animateFloat(1f, 1.08f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "prep-beat")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF22324A), Color(0xFF2C3E55)))).pointerInput(Unit) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
            if (lastRoundWon != null) {
                Text(
                    if (lastRoundWon) pt(language, "Raundu kazandın 🏆", "You won the round 🏆") else pt(language, "Raund rakibin", "Rival took the round"),
                    color = if (lastRoundWon) PremierBoard.Gold else PremierBoard.RivalSoft,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text("$myRounds : $rivalRounds", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(
                if (suddenDeath) pt(language, "ANİ ÖLÜM", "SUDDEN DEATH") else pt(language, "RAUND $round", "ROUND $round"),
                color = Color.White.copy(alpha = .75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(pt(language, "Hazırlan", "Get ready"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Box(
                Modifier.padding(vertical = 10.dp).size(96.dp).graphicsLayer { scaleX = beat; scaleY = beat }
                    .background(PremierBoard.Tile, RoundedCornerShape(22.dp))
                    .border(3.dp, PremierBoard.TileEdge, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$seconds", color = PremierBoard.TileInk, fontSize = 44.sp, fontWeight = FontWeight.Black)
            }
            Text(
                if (youStart) pt(language, "Yeni raundu sen başlatıyorsun", "You open the new round")
                else pt(language, "Yeni raundu rakibin başlatıyor", "Your rival opens the new round"),
                color = Color.White.copy(alpha = .85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            // Chat is open during the break.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(premierQuickMessages(language)) { quick ->
                    Surface(
                        onClick = { onQuickMessage(quick) },
                        shape = RoundedCornerShape(99.dp),
                        color = Color.White.copy(alpha = .16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .35f)),
                    ) {
                        Text(quick, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onOpenChat,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremierBoard.Tile, contentColor = PremierBoard.TileInk),
            ) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(pt(language, "SOHBETİ AÇ", "OPEN CHAT"), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PremierArenaHeader(
    language: String,
    room: GameRoomDto,
    me: ProfileDto?,
    opponent: ProfileDto?,
    rivalName: String,
    myRoundScore: Int,
    rivalRoundScore: Int,
    myStreak: Int,
    rivalStreak: Int,
    myTurn: Boolean,
    rivalTurn: Boolean,
    myGain: Pair<Int, Int>?,
    rivalGain: Pair<Int, Int>?,
) {
    val cardHeight = 74.dp
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremierSymmetricPlayerCard(
            language = language,
            name = me?.displayName ?: pt(language, "Sen", "You"),
            avatar = me?.avatarPath,
            gender = me?.gender,
            visible = me?.avatarVisibility != "hidden",
            score = myRoundScore,
            streak = myStreak,
            active = myTurn,
            gain = myGain,
            accent = PremierBoard.Mine,
            soft = PremierBoard.MineSoft,
            mirrored = false,
            modifier = Modifier.weight(1f).height(cardHeight),
            nameColor = SonHarfCosmetics.playerNameColor,
            nameStyleId = SonHarfCosmetics.nameStyleId,
            frameId = rememberPlayerFrame(me?.id),
        )
        PremierSymmetricPlayerCard(
            language = language,
            name = rivalName,
            avatar = opponent?.avatarPath,
            gender = opponent?.gender,
            visible = opponent?.avatarVisibility != "hidden",
            score = rivalRoundScore,
            streak = rivalStreak,
            active = rivalTurn,
            gain = rivalGain,
            accent = PremierBoard.Rival,
            soft = PremierBoard.RivalSoft,
            mirrored = true,
            modifier = Modifier.weight(1f).height(cardHeight),
            bot = room.isBot,
            frameId = if (room.isBot) null else rememberPlayerFrame(opponent?.id),
            mascot = if (room.isBot) null else rememberRivalMascot(opponent?.id),
        )
    }
}

/** A player card; the rival's is the mirror image of yours. The active player's card breathes. */
@Composable
private fun PremierSymmetricPlayerCard(
    language: String,
    name: String,
    avatar: String?,
    gender: String?,
    visible: Boolean,
    score: Int,
    streak: Int,
    active: Boolean,
    gain: Pair<Int, Int>?,
    accent: Color,
    soft: Color,
    mirrored: Boolean,
    modifier: Modifier,
    bot: Boolean = false,
    nameColor: Color = PremierArenaSky.Ink,
    nameStyleId: String? = null,
    frameId: String? = null,
    mascot: WordSiegeMascotSkin? = null,
) {
    val shownScore by animateIntAsState(score, tween(450), label = "score-count")
    val breath by animateFloatAsState(if (active) .85f else .45f,
        tween(220, easing = FastOutSlowInEasing), label = "card-focus")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(gain?.first) {
        if (gain != null) {
            pop.snapTo(.0f)
            pop.animateTo(1f, tween(900))
        }
    }
    val avatarView: @Composable () -> Unit = {
        if (bot) {
            PremierBotAvatar(size = 54.dp, accent = accent, name = name)
        } else {
            ProfilePhotoAvatarRectWithGender(
                avatarPath = if (visible) avatar else null,
                gender = gender,
                name = name,
                width = 54.dp,
                height = 54.dp,
                accent = accent,
                frameId = frameId,
            )
        }
    }
    // The rival's own mascot rides on their card; you see it whether or not you own a mascot.
    val avatarWithMascot: @Composable () -> Unit = {
        Box {
            avatarView()
            if (mascot != null) {
                WordSiegeMascot(
                    moveId = null,
                    lastMoveMine = false,
                    pendingCells = emptyList(),
                    playerTurn = active,
                    modifier = Modifier.align(if (mirrored) Alignment.TopStart else Alignment.TopEnd).offset(x = if (mirrored) (-12).dp else 12.dp, y = (-8).dp).size(28.dp),
                    hat = WordSiegeMascotHat.NONE,
                    skin = mascot,
                )
            }
        }
    }
    val info: @Composable (Modifier) -> Unit = { infoModifier ->
        Column(infoModifier, horizontalAlignment = if (mirrored) Alignment.End else Alignment.Start) {
            Text(
                name,
                color = if (nameColor == PremierUi.Ink || nameColor == PremierArenaSky.Ink) PremierBoard.Ink else nameColor,
                fontSize = 12.sp,
                style = premiumNameStyle(nameStyleId),
                fontWeight = if (nameStyleId == null) FontWeight.Black else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (streak >= 2) pt(language, "$streak seri", "$streak streak") else pt(language, "Raund Puanı", "Round Score"),
                color = if (streak >= 2) PremierBoard.GoldEdge else PremierBoard.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
    val scoreView: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Text(
                shownScore.toString(),
                color = PremierBoard.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer {
                    val bump = if (pop.value < 1f) 1f + .22f * kotlin.math.sin(pop.value * Math.PI.toFloat()) else 1f
                    scaleX = bump
                    scaleY = bump
                },
            )
            if (gain != null && pop.value < 1f && gain.second != 0) {
                Text(
                    if (gain.second > 0) "+${gain.second}" else gain.second.toString(),
                    color = if (gain.second > 0) accent else PremierBoard.Danger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.graphicsLayer {
                        translationY = -28f - pop.value * 26f
                        alpha = (1f - pop.value).coerceIn(0f, 1f)
                    },
                )
            }
        }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (active) soft else PremierBoard.Card,
        border = BorderStroke(if (active) 1.2.dp else .75.dp, if (active) accent.copy(alpha = breath) else PremierBoard.CardBorder),
        shadowElevation = if (active) 2.dp else .5.dp,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!mirrored) {
                avatarWithMascot()
                Spacer(Modifier.width(7.dp))
                info(Modifier.weight(1f))
                scoreView()
            } else {
                scoreView()
                info(Modifier.weight(1f))
                Spacer(Modifier.width(7.dp))
                avatarWithMascot()
            }
        }
    }
}

@Composable
private fun PremierMiniPlayer(name: String, avatar: String?, gender: String?, visible: Boolean, rounds: Int, streak: Int, accent: Color, bot: Boolean, modifier: Modifier, nameColor: Color = PremierUi.Ink) {
    val isLeft = accent == PremierUi.Ocean
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isLeft) Arrangement.Start else Arrangement.End,
    ) {
        if (isLeft) {
            ProfilePhotoAvatarRectWithGender(
                avatarPath = if (visible) avatar else null,
                gender = gender,
                name = name,
                width = 70.dp,
                height = 54.dp,
                accent = accent,
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 68.dp),
            horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End,
        ) {
            Text(name, color = nameColor, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { i -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (i < rounds) PremierUi.Gold else PremierUi.Border)) }
            }
            if (streak >= 2) Text("🔥 $streak", color = PremierUi.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        if (!isLeft) {
            Spacer(Modifier.width(8.dp))
            if (bot) PremierBotAvatar(size = 58.dp, accent = accent, name = name)
            else ProfilePhotoAvatarRectWithGender(
                avatarPath = if (visible) avatar else null,
                gender = gender,
                name = name,
                width = 70.dp,
                height = 54.dp,
                accent = accent,
            )
        }
    }
}

@Composable
private fun PremierBotAvatar(size: Dp, accent: Color, name:String) {
    ProfilePhotoAvatarWithGender(null,botGenderForName(name),name,size,accent=accent)
}

@Composable
private fun PremierTurnBadge(language: String, myTurn: Boolean, status: String, botThinking: Boolean = false, rivalName: String = "") {
    val active = status in setOf("playing", "final", "sudden_death")
    val pulse by animateFloatAsState(if (active && myTurn) 1f else .85f,
        tween(220, easing = FastOutSlowInEasing), label = "turn-focus")
    val dots = if (active && botThinking && !myTurn) {
        val transition = rememberInfiniteTransition(label = "turn-badge")
        val value by transition.animateFloat(0f, 3.99f, infiniteRepeatable(tween(1_200)), label = "thinking-dots")
        value
    } else 0f
    val label = when {
        !active -> pt(language, "ARENA SENKRONİZE EDİLİYOR", "SYNCING ARENA")
        myTurn -> pt(language, "HAMLE SIRASI SENDE", "YOUR TURN")
        botThinking -> pt(language, "$rivalName düşünüyor", "$rivalName is thinking") + ".".repeat(dots.toInt())
        else -> pt(language, "HAMLE SIRASI RAKİPTE", "RIVAL'S TURN")
    }
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = if (myTurn) PremierBoard.Gold.copy(alpha = pulse) else PremierBoard.Ink.copy(alpha = .08f),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            // Dark ink on the gold badge in every theme (the Black Theme's gold ink vanished on gold).
            color = if (myTurn) PremierBoard.OnGold else PremierBoard.Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .6.sp,
            maxLines = 1,
        )
    }
}

@Composable
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

/** The turn clock as a bar across the screen: it drains toward the centre, seconds in the middle. */
@Composable
private fun PremierPressureStrip(
    language: String,
    myScore: Int,
    rivalScore: Int,
    myStreak: Int,
    rivalStreak: Int,
    seconds: Int,
    totalSeconds: Int = PREMIER_TURN_SECONDS,
    myTurn: Boolean = true,
    active: Boolean = true,
    myWords: Int = 0,
    rivalWords: Int = 0,
) {
    val danger = active && myTurn && seconds in 1..5
    val progress by animateFloatAsState(
        if (!active) 1f else (seconds.coerceIn(0, totalSeconds).toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f),
        tween(900, easing = LinearEasing),
        label = "turn-bar",
    )
    val barColor = when {
        !active -> PremierBoard.CardBorder
        danger -> PremierBoard.Danger
        seconds <= 8 -> PremierBoard.Gold
        myTurn -> PremierBoard.Mine
        else -> PremierBoard.Rival
    }
    val flash = if (danger) {
        val transition = rememberInfiniteTransition(label = "bar-pulse")
        val value by transition.animateFloat(.72f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "bar-flash")
        value
    } else 1f
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PremierWordCountChip(pt(language, "SEN", "YOU"), myWords, PremierBoard.Mine, active = active && myTurn)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .drawBehind {
                            val r = size.height / 2f
                            drawRoundRect(Color(0xFFD8E0E8), cornerRadius = CornerRadius(r, r))
                            // Drains symmetrically toward the middle.
                            val w = size.width * progress
                            drawRoundRect(
                                barColor.copy(alpha = if (danger) flash else 1f),
                                topLeft = Offset((size.width - w) / 2f, 0f),
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(r, r),
                            )
                        }
                )
                if (active) {
                    Surface(shape = CircleShape, color = PremierUi.Surface, border = BorderStroke(2.dp, barColor), shadowElevation = 2.dp) {
                        // A fixed square with the digits centred on both axes (no font padding).
                        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "${seconds.coerceAtLeast(0)}",
                                color = if (danger) PremierBoard.Danger else PremierBoard.Ink,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                                ),
                            )
                        }
                    }
                }
            }
            // The line is always reserved so the board does not jump when it appears. It carries the
            // critical warning, or else the player's streak (in the layout, never over the cards).
            Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                if (!danger && myStreak >= 2) {
                    PremierStreakFlame(streak = myStreak, language = language, modifier = Modifier)
                } else {
                    Text(
                        pt(language, "KRİTİK 5 SANİYE", "CRITICAL 5 SECONDS"),
                        color = PremierBoard.Danger.copy(alpha = if (danger) flash else 0f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        PremierWordCountChip(pt(language, "RAKİP", "RIVAL"), rivalWords, PremierBoard.Rival, active = active && !myTurn)
    }
}

/** A cream letter tile with a raised edge, like a real word-game tile. */
@Composable
private fun PremierLetterTile(
    letter: String,
    size: Dp,
    gold: Boolean = false,
    modifier: Modifier = Modifier,
    fontScale: Float = .5f,
) {
    // One face with a thin, even rim on all four sides (the old tile had only a bottom lip, so
    // its top edge looked faded) and the letter centred on the face's own middle.
    val shape = RoundedCornerShape(size * .2f)
    Box(
        modifier
            .size(size)
            .background(arenaTileBrush(if (gold) PremierBoard.Gold else PremierBoard.Tile), shape)
            .clip(shape).arenaTileFinish()
            .border(1.2.dp, if (gold) PremierBoard.GoldEdge else PremierBoard.TileEdge, shape),
        contentAlignment = Alignment.Center,
    ) {
        val letterSize = (size.value * fontScale).sp
        Text(
            letter,
            color = if (gold) PremierBoard.OnGold else PremierBoard.TileInk,
            fontSize = letterSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                lineHeight = letterSize,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}

/** A diagonal band of light that sweeps across a tile; [position] runs from -0.4 to 1.4. */
@Composable
private fun PremierTileShine(position: Float, modifier: Modifier) {
    Canvas(modifier) {
        val x = size.width * position
        val band = size.width * .35f
        drawRect(
            brush = Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = .28f), Color.Transparent),
                start = Offset(x - band, 0f),
                end = Offset(x + band, size.height),
            ),
            size = size,
        )
    }
}

/** The letter to play: one polished tile; a light sweep marks each new letter and your turn. */
@Composable
private fun PremierTargetCard(
    language: String,
    required: String,
    gameMode: String,
    round: Int,
    size: Dp,
    seconds: Int = PREMIER_TURN_SECONDS,
    totalSeconds: Int = PREMIER_TURN_SECONDS,
    active: Boolean = true,
    suddenDeath: Boolean = false,
) {
    // A light sweep crosses the tile whenever a new letter arrives, and again gently on your turn.
    val shine = remember(required) { Animatable(-.4f) }
    LaunchedEffect(required, active) {
        shine.snapTo(-.4f)
        shine.animateTo(1.4f, tween(950, easing = FastOutSlowInEasing))
    }
    // A new letter settles in.
    val drop = remember(required) { Animatable(0f) }
    LaunchedEffect(required) { drop.animateTo(1f, spring(dampingRatio = .82f, stiffness = 360f)) }
    val tile = size * .84f
    val shape = RoundedCornerShape(tile * .24f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(tile)
                    .graphicsLayer {
                        val s = .7f + .3f * drop.value
                        scaleX = s
                        scaleY = s
                        alpha = drop.value.coerceIn(0f, 1f)
                    }
                    .shadow(10.dp, shape, ambientColor = PremierBoard.GoldEdge, spotColor = PremierBoard.GoldEdge)
                    .clip(shape)
                    .background(
                        // Black Theme: a glossy black tile with a gold rim, like the Black Theme board.
                        if (SonHarfCosmetics.darkArenaTheme) Brush.verticalGradient(listOf(Color(0xFF3B3C43), Color(0xFF1F2025), Color(0xFF0E0F12)))
                        else Brush.verticalGradient(listOf(Color(0xFFFFF8E1), Color(0xFFF7E4AA), Color(0xFFECCB78))),
                    )
                    .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFFBEFC8), Color(0xFFB8913F))), shape),
                contentAlignment = Alignment.Center,
            ) {
                // Glossy top light, like a polished wooden tile.
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(.42f)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (SonHarfCosmetics.darkArenaTheme) .16f else .55f), Color.Transparent))),
                )
                // Sized in dp so the system font size can never push the letter out of its tile.
                val letterSize = with(LocalDensity.current) {
                    (tile * when {
                        required.length > 2 -> .24f
                        required.length > 1 -> .34f
                        else -> .5f
                    }).toSp()
                }
                Text(
                    required,
                    color = PremierBoard.TileInk,
                    fontSize = letterSize,
                    lineHeight = letterSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    softWrap = false,
                )
                PremierTileShine(shine.value, Modifier.matchParentSize())
            }
        }
        Text(
            when {
                suddenDeath -> pt(language, "ANİ ÖLÜM", "SUDDEN DEATH")
                required == "★" -> pt(language, "SERBEST KELİME", "FREE WORD")
                gameMode == "expert" -> pt(language, "HEDEF • x${round.coerceIn(1, 3)}", "TARGET • x${round.coerceIn(1, 3)}")
                else -> pt(language, "HEDEF HARF", "TARGET LETTER")
            },
            color = if (suddenDeath) PremierBoard.Danger else PremierBoard.Ink.copy(alpha = .7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** The word just played as tiles that drop in one by one; the linking letters are gold. */
@Composable
private fun PremierLastWordCard(
    language: String,
    word: String,
    mine: Boolean,
    linkLetters: Int,
    veryCompact: Boolean,
) {
    val latestPlayedWord = word
    if (latestPlayedWord.isBlank()) {
        Text(
            latestPlayedWord.ifBlank { pt(language, "İLK KELİME SERBEST", "FREE OPENING WORD") },
            color = PremierBoard.Ink.copy(alpha = .8f),
            fontSize = if (veryCompact) 14.sp else 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        return
    }
    val letters = latestPlayedWord.toList()
    val tileSize = when {
        letters.size > 11 -> 22.dp
        letters.size > 8 -> 26.dp
        veryCompact -> 30.dp
        else -> 34.dp
    }
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(6.dp), color = (if (mine) PremierBoard.Mine else PremierBoard.Rival)) {
            Text(
                if (mine) pt(language, "SEN", "YOU") else pt(language, "RAKİP", "RIVAL"),
                Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(4.dp))
        letters.forEachIndexed { index, char ->
            // Staggered drop: each tile falls in a moment after the previous one.
            val fall = remember(latestPlayedWord, index) { Animatable(0f) }
            LaunchedEffect(latestPlayedWord, index) {
                delay(index * PREMIER_WORD_TILE_STAGGER_MS)
                fall.animateTo(1f, tween(PREMIER_WORD_TILE_DROP_MS.toInt(), easing = FastOutSlowInEasing))
            }
            PremierLetterTile(
                letter = char.toString(),
                size = tileSize,
                gold = index >= letters.size - linkLetters,
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - fall.value) * -40f
                    alpha = fall.value.coerceIn(0f, 1f)
                },
            )
        }
    }
}

@Composable
private fun PremierWordTrail(words: List<GameWordDto>, language: String, isPro: Boolean = false, meId: String? = null) {
    if (words.isEmpty()) {
        Text(pt(language, "İlk zinciri sen başlatabilirsin.", "You can start the first chain."), color = PremierBoard.Ink.copy(alpha = .6f), fontSize = 10.sp)
        return
    }
    // Seeing the words played in the match is a PRO perk; others see how many and a lock.
    if (!isPro) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = PremierBoard.Ink.copy(alpha = .5f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                pt(language, "Çıkan ${words.size} kelimeyi görmek PRO'ya özel", "Seeing the ${words.size} played words is PRO"),
                color = PremierBoard.Ink.copy(alpha = .55f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        return
    }
    val ordered = words.reversed()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isPro) {
            Text(
                pt(language, "PRO • Tüm oynanan kelimeler (${words.size})", "PRO • All played words (${words.size})"),
                color = PremierBoard.Ink.copy(alpha = .55f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        LazyRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(ordered, key = { it.id }) { entry ->
                val mine = entry.playerId != null && entry.playerId == meId
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, (if (mine) PremierBoard.Mine else PremierBoard.Rival).copy(alpha = .8f)),
                ) {
                    Text(
                        premierUpper(entry.normalizedWord.ifBlank { entry.word }, language),
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = PremierBoard.Ink.copy(alpha = .9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Your word as tiles that pop in as you type; a mistake shakes the rack. */
@Composable
private fun PremierInputBar(
    language: String,
    input: String,
    required: String,
    myTurn: Boolean,
    busy: Boolean,
    usedWords: List<GameWordDto> = emptyList(),
    shakeKey: String? = null,
    modifier: Modifier = Modifier,
) {
    val problem = if (myTurn && input.length >= 2) {
        premierLocalRejection(input, required, usedWords.map { it.normalizedWord.ifBlank { it.word } }, language)
    } else if (myTurn && input.isNotEmpty() && required != "★" && !premierUpper(input, language).startsWith(required.take(input.length))) {
        "wrong_start_letter"
    } else {
        null
    }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey != null) {
            for (offset in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(offset, tween(45))
        }
    }
    val accent = when {
        problem != null -> PremierBoard.Danger
        myTurn -> PremierBoard.Mine
        else -> PremierBoard.CardBorder
    }
    Surface(
        modifier = modifier.fillMaxWidth().graphicsLayer { translationX = shake.value },
        shape = RoundedCornerShape(16.dp),
        color = PremierBoard.Card,
        border = BorderStroke(if (myTurn) 2.dp else 1.dp, accent),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                if (input.isNotBlank() && !busy) {
                    val shown = premierUpper(input, language)
                    val prefix = if (required == "★") 0 else required.length
                    val tile = when {
                        shown.length > 11 -> 24.dp
                        shown.length > 8 -> 28.dp
                        else -> 32.dp
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        shown.forEachIndexed { index, char ->
                            val pop = remember(index) { Animatable(.4f) }
                            LaunchedEffect(index) { pop.animateTo(1f, spring(dampingRatio = .45f, stiffness = 600f)) }
                            PremierLetterTile(
                                letter = char.toString(),
                                size = tile,
                                gold = index < prefix,
                                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                            )
                        }
                    }
                } else {
                    Text(
                        when {
                            busy -> pt(language, "DOĞRULANIYOR…", "VERIFYING…")
                            !myTurn -> pt(language, "Rakibin hamlesini bekle…", "Wait for your rival…")
                            required == "★" -> pt(language, "KELİMENİ YAZ…", "TYPE YOUR WORD…")
                            else -> pt(language, "$required İLE BAŞLAYAN KELİMEYİ YAZ…", "TYPE A WORD STARTING WITH $required…")
                        },
                        color = PremierBoard.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // A fixed one-line slot: a rejection note never grows the bar or pushes the board.
            Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) {
                if (problem != null) {
                    Text(
                        validationMessage(language, problem),
                        color = PremierBoard.Danger,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremierKeyboard(language: String, value: String, enabled: Boolean, keyHeight: Dp, onInput: (String) -> Unit, onSubmit: () -> Unit) {
    val palette = SonHarfCosmetics.keyboardPalette
    val rows = if (language == "en") listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P"),
        listOf("A","S","D","F","G","H","J","K","L"),
        listOf("Z","X","C","V","B","N","M"),
    ) else listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
        listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
        listOf("Z","X","C","V","B","N","M","Ö","Ç"),
    )
    Surface(
        color = palette.background,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        // A painted panel brings its own frame and must not sit on a shadow box.
        border = if (palette.panelImage == null) BorderStroke(1.dp, PremierUi.Border) else null,
        shadowElevation = if (palette.panelImage == null) 3.dp else 0.dp,
    ) {
        // A painted panel adds its crown band and frame; on compact screens keep both slim so the
        // board keeps its room.
        val painted = palette.panelImage != null
        val crownBand = if (keyHeight <= 35.dp) 22.dp else 30.dp
        Column(
            Modifier.fillMaxWidth()
                .keyboardTray(palette, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp), crownBand)
                .padding(start = if (painted) 2.dp else 6.dp, end = if (painted) 2.dp else 6.dp, top = 7.dp, bottom = if (painted) 6.dp else 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val widest = rows.first().size
            rows.forEachIndexed { index, row ->
                val last = index == rows.lastIndex
                Row(Modifier.fillMaxWidth().padding(horizontal = if (index == 1) 7.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Like the Android keyboard: letters keep row-one width, backspace sits on the right.
                    if (last) Spacer(Modifier.weight((widest - row.size - 1.6f).coerceAtLeast(.1f)))
                    row.forEach { key ->
                        PremierKey(key, enabled && value.length < 30, Modifier.weight(1f), keyHeight = keyHeight) {
                            SonHarfSoundFx.typingClick()
                            onInput((value + key).take(30))
                        }
                    }
                    if (last) {
                        PremierBackspaceKey(
                            enabled = enabled && value.isNotEmpty(),
                            modifier = Modifier.weight(1.6f),
                            keyHeight = keyHeight,
                            onDelete = { onInput(it) },
                            value = value,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PremierKey(pt(language, "TEMİZLE", "CLEAR"), enabled && value.isNotEmpty(), Modifier.weight(1.3f), keyHeight = keyHeight, alt = true) { onInput(""); SonHarfSoundFx.tap() }
                PremierKey(pt(language, "GÖNDER  ➤", "SEND  ➤"), enabled && value.length >= 2, Modifier.weight(2.7f), keyHeight = keyHeight, action = true) { onSubmit(); SonHarfSoundFx.tap() }
            }
        }
    }
}

/** Android-style backspace: tap deletes one letter, holding it keeps deleting. */
@Composable
private fun PremierBackspaceKey(enabled: Boolean, modifier: Modifier, keyHeight: Dp, value: String, onDelete: (String) -> Unit) {
    val palette = SonHarfCosmetics.keyboardPalette
    val current by rememberUpdatedState(value)
    val deleteNow by rememberUpdatedState(onDelete)
    val isEnabled by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(keyHeight)
            .graphicsLayer { scaleX = if (pressed) .96f else 1f; scaleY = if (pressed) .96f else 1f }
            .keyFace(palette, KeyKind.ALT, enabled, 6.dp, rememberSkinImage(palette.keyImage), pressed)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (!isEnabled) return@detectTapGestures
                        pressed = true
                        deleteNow(current.dropLast(1))
                        SonHarfSoundFx.tap()
                        val repeat = scope.launch {
                            delay(420)
                            while (isEnabled && current.isNotEmpty()) {
                                deleteNow(current.dropLast(1))
                                delay(70)
                            }
                        }
                        tryAwaitRelease()
                        repeat.cancel()
                        pressed = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = "Sil",
            tint = if (enabled) palette.text else palette.text.copy(alpha = .42f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PremierKey(label: String, enabled: Boolean, modifier: Modifier, keyHeight: Dp, alt: Boolean = false, action: Boolean = false, onClick: () -> Unit) {
    val palette = SonHarfCosmetics.keyboardPalette
    val kind = when { action -> KeyKind.ACTION; alt -> KeyKind.ALT; else -> KeyKind.LETTER }
    SkinKey(kind, enabled, 6.dp, modifier.height(keyHeight), onClick) {
        Text(label, color = palette.labelColor(kind, enabled), fontSize = if (label.length > 5) 9.sp else 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremierChatSheet(
    language: String,
    messages: List<ChatMessageDto>,
    meId: String?,
    isBot: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Newest message sits at the bottom, right above the input, and older ones move up; the list
    // is laid out bottom-up so it stays anchored when the keyboard opens and shrinks it.
    val recent = messages.takeLast(50).asReversed()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, containerColor = PremierUi.Surface,
        // The sheet has its own window; mark it secure directly instead of relying on inheritance.
        properties = ModalBottomSheetProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
    ) {
        SecureChatContent()
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = PremierBoard.Mine, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(pt(language, "Maç Sohbeti", "Match Chat"), color = PremierBoard.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (isBot) pt(language, "AI ile serbestçe yazış.", "Chat freely with the AI.")
                        else pt(language, "Rakibinle gerçek zamanlı mesajlaş.", "Message your rival in real time."),
                        color = PremierBoard.Muted,
                        fontSize = 10.sp,
                    )
                }
            }

            if (messages.isEmpty()) {
                Surface(shape = RoundedCornerShape(14.dp), color = PremierUi.Ice) {
                    Text(
                        pt(language, "Henüz mesaj yok. İlk mesajı sen gönder.", "No messages yet. Send the first one."),
                        Modifier.fillMaxWidth().padding(14.dp),
                        color = PremierBoard.Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 280.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(recent, key = { it.id }) { message ->
                        val mine = message.senderId == meId
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (mine) 14.dp else 4.dp,
                                    bottomEnd = if (mine) 4.dp else 14.dp,
                                ),
                                color = if (mine) PremierBoard.Mine else PremierUi.Ice,
                                shadowElevation = 1.dp,
                            ) {
                                Text(
                                    message.body,
                                    Modifier.widthIn(max = 280.dp).padding(horizontal = 12.dp, vertical = 9.dp),
                                    color = if (mine) Color.White else PremierBoard.Ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            // Quick messages: one tap, also usable in the break between rounds.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(premierQuickMessages(language)) { quick ->
                    Surface(
                        onClick = { onSend(quick) },
                        shape = RoundedCornerShape(99.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PremierBoard.TileEdge),
                    ) {
                        Text(quick, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = PremierBoard.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(300) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(pt(language, "Mesaj yaz…", "Type a message…")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremierBoard.Mine,
                        unfocusedBorderColor = PremierBoard.TileEdge,
                        focusedContainerColor = PremierUi.Ice,
                        unfocusedContainerColor = PremierUi.Ice,
                        focusedTextColor = PremierBoard.Ink,
                        unfocusedTextColor = PremierBoard.Ink,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            onSend(text)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PremierBoard.Mine),
                ) {
                    Icon(Icons.Rounded.Send, pt(language, "Gönder", "Send"))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun premierQuickMessages(language: String): List<String> = if (language == "en") {
    listOf("👋 Hi!", "👍 Good game", "🔥 Nice word!", "😅 That was close", "🍀 Good luck", "😄")
} else {
    listOf("👋 Selam!", "👍 İyi oyun", "🔥 Güzel kelime!", "😅 Kıl payı", "🍀 Bol şans", "😄")
}

@Composable
private fun PremierResult(language: String, room: GameRoomDto, meId: String?, busy: Boolean, notice: String, onRematch: () -> Unit, onHome: () -> Unit, playerName: String? = null, playerGender: String? = null) {
    val amHost = meId == room.hostId
    val myScore = if (amHost) room.hostScore else room.guestScore
    val rivalScore = if (amHost) room.guestScore else room.hostScore
    val won = when {
        room.isBot -> room.winnerId == meId && !room.winnerIsBot
        else -> room.winnerId == meId
    }
    val mascotOutcome = when {
        won -> WordSiegeMascotOutcome.WIN
        room.winnerId == null && !room.winnerIsBot -> WordSiegeMascotOutcome.DRAW
        else -> WordSiegeMascotOutcome.LOSS
    }
    // The result is heard once: a fanfare for a win, a gentle phrase otherwise.
    LaunchedEffect(room.id) {
        when (mascotOutcome) {
            WordSiegeMascotOutcome.WIN -> SonHarfSoundFx.victory()
            WordSiegeMascotOutcome.LOSS -> SonHarfSoundFx.defeat()
            WordSiegeMascotOutcome.DRAW -> SonHarfSoundFx.bonus()
        }
    }
    if (mascotOutcome != WordSiegeMascotOutcome.DRAW) {
        // Every player: the victory or defeat clip full screen, the result underneath.
        MatchResultScreen(
            won = won,
            title = if (won) pt(language, "KAZANDIN!", "YOU WON!") else pt(language, "KAYBETTİN", "YOU LOST"),
            subtitle = notice.ifBlank { null },
            mine = ResultScore(pt(language, "SEN", "YOU"), "$myScore"),
            rival = ResultScore(pt(language, "RAKİP", "RIVAL"), "$rivalScore"),
            primaryLabel = if (busy) "…" else pt(language, "RÖVANŞ", "REMATCH"),
            onPrimary = { if (!busy) onRematch() },
            secondaryLabel = pt(language, "ANA SAYFA", "HOME"),
            onSecondary = onHome,
        )
        return
    }
    val glowTransition = rememberInfiniteTransition(label = "result-glow")
    val glow by glowTransition.animateFloat(.6f, 1f, infiniteRepeatable(tween(1_100), RepeatMode.Reverse), label = "result-glow-value")
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(150.dp).graphicsLayer { scaleX = glow; scaleY = glow }.background(
                    Brush.radialGradient(listOf((if (won) PremierUi.Gold else PremierUi.Red).copy(alpha = .45f), Color.Transparent)),
                    CircleShape,
                )
            )
            Surface(shape = CircleShape, color = if (won) PremierUi.GoldSoft else PremierUi.RedSoft, border = BorderStroke(2.dp, if (won) PremierUi.Gold else PremierUi.Red)) {
                Icon(if (won) Icons.Rounded.EmojiEvents else Icons.Rounded.SportsEsports, null, tint = if (won) PremierUi.Gold else PremierUi.Red, modifier = Modifier.padding(22.dp).size(52.dp))
            }
            // Taç Zaferi: the purchased crown comes down onto the victory, as in Kelime Tahtı.
            if (won && SonHarfCosmetics.crownVictory) {
                CrownVictoryCelebration(eventKey = "sonharf:${room.id}", modifier = Modifier.size(190.dp), compact = true)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(if (won) pt(language, "ZAFER", "VICTORY") else pt(language, "MAÇ BİTTİ", "MATCH OVER"), color = if (won) PremierUi.Ocean else PremierUi.Red, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Text(if (won) pt(language, "Rakibini geride bıraktın.", "You outplayed your rival.") else pt(language, "Yeni maçta geri dön.", "Come back stronger next match."), color = PremierUi.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Border)) {
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                PremierResultMetric(pt(language, "SKOR", "SCORE"), "$myScore")
                Box(Modifier.width(1.dp).height(52.dp).background(PremierUi.Border))
                PremierResultMetric(pt(language, "RAKİP", "RIVAL"), "$rivalScore")
                Box(Modifier.width(1.dp).height(52.dp).background(PremierUi.Border))
                PremierResultMetric(pt(language, "ROUND", "ROUNDS"), "${if (amHost) room.hostRounds else room.guestRounds}-${if (amHost) room.guestRounds else room.hostRounds}")
            }
        }
        if (notice.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(notice, color = PremierUi.OceanDeep, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRematch, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean)) {
            Icon(Icons.Rounded.Replay, null)
            Spacer(Modifier.width(7.dp))
            Text(if (busy) pt(language, "BEKLENİYOR…", "WAITING…") else pt(language, "HEMEN RÖVANŞ", "INSTANT REMATCH"), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(9.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, PremierUi.Border)) {
            Text(pt(language, "ANA MENÜ", "HOME"), color = PremierUi.Muted, fontWeight = FontWeight.Black)
        }
    }
    // The mascot flies in to celebrate a win (or to comfort after a loss), then perches above.
    WordSiegeMascotCompanion(
        anchors = listOf(Offset(.84f, .14f), Offset(.16f, .14f)),
        mascotSize = 84.dp,
                        ambientScenes = true,
        moveId = null,
        lastMoveMine = false,
        playerTurn = false,
        modifier = Modifier.matchParentSize().statusBarsPadding(),
        outcome = mascotOutcome,
        playerName = playerName,
        playerGender = playerGender,
        greet = false,
        stageY = .2f,
        celebrationScale = 1.9f,
    )
    }
}

@Composable
private fun PremierResultMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = PremierUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = PremierUi.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PremierCenteredMessage(title: String, detail: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.CloudOff, null, tint = PremierUi.Ocean, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = PremierUi.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(detail, color = PremierUi.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean)) { Text(action, fontWeight = FontWeight.Black) }
    }
}

private fun profileWinRate(profile: ProfileDto?): Int {
    if (profile == null) return 0
    val total = profile.wins + profile.losses
    return if (total <= 0) 0 else ((profile.wins.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
}

private fun premierRequiredToken(room: GameRoomDto, words: List<GameWordDto>): String {
    val last = words.lastOrNull()?.normalizedWord?.trim().orEmpty()
    if (last.isBlank()) return "★"
    val count = if (room.gameMode == "expert") room.roundNo.coerceIn(1, 3) else 1
    return premierUpper(last.takeLast(count), room.language)
}

private fun premierBotChatReply(language: String, message: String): String {
    val lower = message.lowercase(premierLocale(language))
    // The bot reads the match: the score, its lead, the last word and the player's streak.
    val lead = PremierBotBrain.botScore - PremierBotBrain.myScore
    val lastWord = PremierBotBrain.lastWord
    fun pick(vararg options: Pair<String, String>): String {
        val (tr, en) = options[kotlin.random.Random.nextInt(options.size)]
        return pt(language, tr, en)
    }
    val tokens = lower.split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }.toSet()
    return when {
        lower.contains("hile") || lower.contains("cheat") ->
            pick(
                "Hile yok, sadece çok kelime okudum. 📖" to "No cheating, I've just read a lot of words. 📖",
                "Hile mi? Sadece sözlüğü ezberledim. 🤓" to "Cheating? I just memorised the dictionary. 🤓",
            )
        "merhaba" in tokens || "selam" in tokens || "hello" in tokens || "hi" in tokens || "hey" in tokens ->
            pick(
                "Selam! Güzel bir maç olsun. 🤖" to "Hi! Let's have a good match. 🤖",
                "Merhaba! Sözlüğümü ısıttım, hazırım. 📚" to "Hello! My dictionary is warmed up, I'm ready. 📚",
            )
        lower.contains("rövanş") || lower.contains("rematch") ->
            pick("Maç bitince rövanşa hazırım. 😏" to "I'll be ready for a rematch when this ends. 😏")
        lower.contains("tebrik") || lower.contains("bravo") || lower.contains("congrats") || lower.contains("güzel") || lower.contains("nice") ->
            pick(
                "Teşekkürler! Sen de iyi gidiyorsun." to "Thanks! You're doing well too.",
                "Sağ ol! Ama asıl hamlem daha gelmedi. 😉" to "Thanks! But my best move is yet to come. 😉",
            )
        lower.contains("zor") || lower.contains("hard") ->
            pick(
                "Hile yok, sadece çok kelime okudum. 📖" to "No cheating, I've just read a lot of words. 📖",
                "Zor harfleri severim… sen de sevmeye başla. 😈" to "I love tricky letters… you should too. 😈",
            )
        "bot" in tokens || "robot" in tokens || lower.contains("yapay") || "ai" in tokens ->
            pick(
                "Evet, yapay zekâyım. Ama kelimelere gönülden bağlıyım. 🤖💙" to "Yes, I'm an AI. But I truly love words. 🤖💙",
                "Yapay zekâyım ama kaybetmekten gerçekten nefret ederim. 😅" to "I'm an AI, but I truly hate losing. 😅",
            )
        lead >= 10 ->
            pick(
                "Skor tabelasına bir bak istersen… 😏" to "Maybe take a look at the scoreboard… 😏",
                "Bugün sözlük benden yana gibi. 📚" to "The dictionary seems to be on my side today. 📚",
                "Toparlanmak için hâlâ vaktin var, merak etme." to "You still have time to recover, don't worry.",
            )
        lead <= -10 ->
            pick(
                "Tamam, bugün çok iyisin. Ama pes etmem! 😤" to "Okay, you're really good today. But I won't give up! 😤",
                "Nasıl bu kadar hızlısın?! 😲" to "How are you this fast?! 😲",
                "Devrelerim ısınmaya başladı… 🔥" to "My circuits are heating up… 🔥",
            )
        PremierBotBrain.myStreak >= 3 ->
            pick("Serin çok iyi, ama onu bozacağım. 😈" to "Nice streak, but I'm going to break it. 😈")
        lastWord.isNotBlank() && PremierBotBrain.lastWordMine ->
            pick(
                "$lastWord mı? Hmm, iyi seçim. 🤔" to "$lastWord? Hmm, good pick. 🤔",
                "$lastWord… not ettim. 📝" to "$lastWord… noted. 📝",
            )
        lastWord.isNotBlank() ->
            pick(
                "$lastWord'den sonra ne yazacaksın bakalım? 😏" to "Let's see what you play after $lastWord. 😏",
                "Son harfi beğendin mi? Özenle seçtim. 😉" to "Like that last letter? I picked it carefully. 😉",
            )
        else -> pick(
            "Buradayım. Zinciri sürdür!" to "I'm here. Keep the chain going!",
            "Sıradaki kelimede bol şans." to "Good luck on the next word.",
            "Maç giderek kızışıyor. 🔥" to "This match is getting interesting. 🔥",
        )
    }
}

private fun validationMessage(language: String, reason: String): String = when (reason) {
    "invalid_length" -> pt(language, "En az 2 harf olmalı.", "At least 2 letters.")
    "invalid_characters" -> pt(language, "Geçersiz karakter var.", "Invalid characters.")
    "not_in_dictionary", "invalid_word" -> pt(language, "Sözlükte yok.", "Not in the dictionary.")
    "abbreviation_not_allowed" -> pt(language, "Kısaltmalar kullanılamaz.", "Abbreviations are not allowed.")
    "proper_noun_not_allowed" -> pt(language, "Özel adlar kullanılamaz.", "Proper nouns are not allowed.")
    "not_game_allowed" -> pt(language, "Oyunda geçerli değil.", "Not allowed in play.")
    "ends_with_soft_g" -> pt(language, "Ğ ile bitemez.", "Cannot end with Ğ.")
    "wrong_start_letter" -> pt(language, "Hedef harfle başlamalı.", "Must start with the target.")
    "word_already_used" -> pt(language, "Daha önce kullanıldı.", "Already used.")
    "turn_expired" -> pt(language, "Süren doldu.", "Your turn expired.")
    else -> pt(language, "Hamle kabul edilmedi.", "Move was not accepted.")
}

private fun premierError(language: String, raw: String): String = when {
    "player_already_in_game" in raw -> pt(language, "Aktif maçın bulundu.", "Your active match was found.")
    "not_your_turn" in raw -> pt(language, "Sıra rakibinde.", "It is your rival's turn.")
    "maintenance_mode" in raw -> pt(language, "Oyun kısa süreli bakımda.", "The game is under brief maintenance.")
    "matchmaking_disabled" in raw -> pt(language, "Eşleşme geçici olarak kapalı.", "Matchmaking is temporarily disabled.")
    "not_in_dictionary" in raw -> validationMessage(language, "not_in_dictionary")
    "wrong_start_letter" in raw -> validationMessage(language, "wrong_start_letter")
    "word_already_used" in raw -> validationMessage(language, "word_already_used")
    else -> pt(language, "Bağlantı yenileniyor. Tekrar deneyebilirsin.", "Connection refreshed. You can try again.")
}
