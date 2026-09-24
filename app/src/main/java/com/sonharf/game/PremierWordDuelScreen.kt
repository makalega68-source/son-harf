package com.sonharf.game

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

private enum class PremierStage { Loading, Lobby, Searching, Vs, Playing, Finished }
private data class PremierMoveFeedback(val accepted: Boolean, val message: String)
private const val PREMIER_TURN_SECONDS = 15
private const val PREMIER_RECONNECT_SECONDS = 60
/** Breather before every new round; the server adds it to the round's first turn. */
private const val PREMIER_ROUND_PREP_SECONDS = 20

/** Yetişkin, yüksek okunabilirlikli Son Harf oyun paleti. */
private object PremierUi {
    val Background = Color(0xFF06101D)
    val Surface = Color(0xFF0D1B2A)
    val Ink = Color(0xFFF4F7FB)
    val Muted = Color(0xFF9AAFC4)
    val Ocean = Color(0xFF00D6C9)
    val OceanDeep = Color(0xFF2F6BFF)
    val Sky = Color(0xFF8B5CF6)
    val Ice = Color(0xFF13273A)
    val Border = Color(0xFF27445E)
    val Green = Color(0xFF24D6A3)
    val GreenSoft = Color(0xFF0D3A32)
    val Red = Color(0xFFFF5C5C)
    val RedSoft = Color(0xFF3A1A23)
    val Gold = Color(0xFFFFB64A)
    val GoldSoft = Color(0xFF3A2A13)
}

private object PremierArenaSky {
    val BackgroundTop = Color(0xFFEAF8FF)
    val BackgroundMid = Color(0xFFDDF3FC)
    val BackgroundBottom = Color(0xFFCFEAF7)
    val Surface = Color(0xFFF9FDFF)
    val SurfaceBlue = Color(0xFFE6F5FB)
    val Ink = Color(0xFF17364D)
    val Muted = Color(0xFF647D8D)
    val Ocean = Color(0xFF4E98AA)
    val OceanDeep = Color(0xFF2E6F82)
    val Border = Color(0xFFB8D6E1)
    val Rival = Color(0xFFD77B70)
    val RivalSoft = Color(0xFFFBE8E4)
    val Green = Color(0xFF6F9E7B)
    val GreenSoft = Color(0xFFE8F3E9)
    val Gold = Color(0xFFC89C39)
    val GoldSoft = Color(0xFFF7ECCA)
    val Red = Color(0xFFC65C55)
    val RedSoft = Color(0xFFF8E2DF)
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
    var hasUnreadChat by remember { mutableStateOf(false) }
    var floatingMessage by remember { mutableStateOf<ChatMessageDto?>(null) }
    var moveFeedback by remember { mutableStateOf<PremierMoveFeedback?>(null) }
    var turnSeconds by remember { mutableIntStateOf(PREMIER_TURN_SECONDS) }
    var botThinking by remember { mutableStateOf(false) }
    var prepSeconds by remember { mutableIntStateOf(0) }
    var botPrepSeconds by remember { mutableIntStateOf(0) }

    suspend fun ensureMe(): ProfileDto {
        if (backend.currentUserId() == null) backend.ensurePlayer(pt(language, "Oyuncu", "Player"))
        val id = requireNotNull(backend.currentUserId())
        return runCatching { backend.getProfile(id) }
            .getOrElse { backend.ensurePlayer(pt(language, "Oyuncu", "Player")) }
            .also { me = it }
    }

    suspend fun adoptRoom(next: GameRoomDto, cinematic: Boolean) {
        val previousRoomId = room?.id
        room = next
        if (previousRoomId != next.id) {
            botChat = emptyList()
            hasUnreadChat = false
        }
        language = SharedDictionaryService.canonicalLanguage(next.language)
        SonHarfUiState.language = language
        opponent = backend.getPremierOpponent(next)
        words = runCatching { backend.getWords(next.id) }.getOrDefault(emptyList())
        chat = if (next.isBot) emptyList() else runCatching { backend.getChat(next.id) }.getOrDefault(emptyList())
        stage = when {
            next.isPremierFinished() -> PremierStage.Finished
            cinematic -> PremierStage.Vs
            else -> PremierStage.Playing
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val player = ensureMe()
            val found = backend.findPremierActiveRoom()
            val active = if (found?.isBot == true && found.isPremierLive()) {
                runCatching { backend.resumePremierBotMatch(found.id) }.getOrDefault(found)
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
                    room = next
                    if (next.isPremierFinished()) stage = PremierStage.Finished
                }
        }
        launch {
            backend.observeWords(active.id)
                .catch { }
                .collect { words = it }
        }
        if (!active.isBot) launch {
            backend.observeChat(active.id)
                .catch { }
                .collect { next ->
                    val previousId = chat.lastOrNull()?.id
                    chat = next
                    val latest = next.lastOrNull()
                    if (latest != null && latest.id != previousId && latest.senderId != backend.currentUserId()) {
                        floatingMessage = latest
                        hasUnreadChat = !showQuickChat
                    }
                }
        }
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

    LaunchedEffect(room?.id, room?.botTurn, room?.status) {
        val active = room ?: return@LaunchedEffect
        val botPlayable = active.status in setOf("playing", "final", "sudden_death")
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
        for (attempt in 0 until 4) {
            val synced = runCatching { backend.getRoom(active.id) }.getOrNull()
            if (synced != null && (
                    !synced.botTurn ||
                        synced.isPremierFinished() ||
                        synced.status !in setOf("playing", "final", "sudden_death")
                )
            ) {
                room = synced
                notice = ""
                return@LaunchedEffect
            }
            val advanced = runCatching { backend.botTakeTurn(active.id) }.getOrNull()
            if (advanced != null) {
                room = advanced
                notice = ""
                return@LaunchedEffect
            }
            delay(700L + attempt * 250L)
        }
        notice = pt(language, "Rakip hamlesi yeniden eşitleniyor…", "Resyncing rival move…")
    }

    LaunchedEffect(
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
            // The database clock is authoritative; phone wall-clock drift must not shorten reconnect grace.
            val requestStartedAt = SystemClock.elapsedRealtime()
            val reconnectClock = runCatching { backend.getPremierReconnectClock(active.id) }.getOrNull()
            val requestFinishedAt = SystemClock.elapsedRealtime()
            val halfRoundTripMs = ((requestFinishedAt - requestStartedAt) / 2L).coerceIn(0L, 750L)
            val initialReconnectMs = if (reconnectClock != null) {
                (reconnectClock.remainingMs - halfRoundTripMs).coerceAtLeast(0L)
            } else {
                PREMIER_RECONNECT_SECONDS * 1000L
            }
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
            // A new round begins with preparation time on top of the 15-second turn: show it
            // separately so the turn clock itself always counts down from 15.
            val prepMs = initialRemainingMs - elapsedMs - PREMIER_TURN_SECONDS * 1000L
            prepSeconds = if (prepMs > 0L) ((prepMs + 999L) / 1000L).toInt() else 0
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
                    scope.launch {
                        busy = true
                        notice = ""
                        runCatching {
                            ensureMe()
                            backend.startRandomMatchmaking(language)
                            stage = PremierStage.Searching
                            while (stage == PremierStage.Searching) {
                                val found = backend.pollRandomMatchmakingRoom()
                                if (found != null) {
                                    adoptRoom(found, cinematic = true)
                                    SonHarfSoundFx.softNotify()
                                    break
                                }
                                delay(750)
                            }
                        }.onFailure {
                            stage = PremierStage.Lobby
                            notice = premierError(language, it.message.orEmpty())
                        }
                        busy = false
                    }
                },
                onHome = { SonHarfUiState.homeRequest += 1 },
            )
            PremierStage.Searching -> PremierSearching(
                language = language,
                onCancel = {
                    scope.launch {
                        stage = PremierStage.Lobby
                        runCatching { backend.cancelRandomMatchmaking() }
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
                    PremierArena(
                        language = language,
                        room = active,
                        me = me,
                        opponent = opponent,
                        meId = backend.currentUserId(),
                        words = words,
                        input = input,
                        notice = notice,
                        busy = busy,
                        turnSeconds = turnSeconds,
                        unreadChat = hasUnreadChat,
                        floatingMessage = floatingMessage,
                        moveFeedback = moveFeedback,
                        botThinking = botThinking,
                        prepSeconds = maxOf(prepSeconds, botPrepSeconds),
                        onInput = { input = it },
                        onForfeit = { showForfeit = true },
                        onQuickChat = {
                            hasUnreadChat = false
                            showQuickChat = true
                        },
                        onSubmit = {
                            if (busy || input.isBlank()) return@PremierArena
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
                            scope.launch {
                                busy = true
                                val candidate = input
                                // Clear immediately when Send is pressed. The server remains authoritative
                                // for the result, but stale text must never survive into the rival turn.
                                input = ""
                                notice = ""
                                runCatching { backend.submitPremierWord(active.id, candidate) }
                                    .onSuccess { next ->
                                        room = next
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
                                    .onFailure { notice = premierError(language, it.message.orEmpty()) }
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
                        scope.launch {
                            busy = true
                            if (active.isBot) {
                                runCatching { backend.restartBotMatch(active.id) }
                                    .onSuccess { adoptRoom(it, cinematic = true) }
                                    .onFailure { notice = premierError(language, it.message.orEmpty()) }
                            } else {
                                runCatching { backend.requestRematch(active.id) }
                                    .onSuccess {
                                        notice = pt(language, "Rövanş teklifi gönderildi.", "Rematch request sent.")
                                        for (attempt in 0 until 16) {
                                            delay(750)
                                            val next = runCatching { backend.findPremierActiveRoom() }.getOrNull()
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
        AlertDialog(
            onDismissRequest = { showForfeit = false },
            icon = { Icon(Icons.Rounded.Flag, null, tint = PremierUi.Red) },
            title = { Text(pt(language, "Pes etmek istiyor musun?", "Surrender this match?"), fontWeight = FontWeight.Black) },
            text = { Text(pt(language, "Pes edersen maç hemen rakibin lehine biter. Bu işlem geri alınamaz.", "Surrendering ends the match immediately in your opponent's favor. This cannot be undone.")) },
            confirmButton = {
                Button(
                    onClick = {
                        showForfeit = false
                        val active = room ?: return@Button
                        scope.launch {
                            busy = true
                            runCatching { backend.forfeit(active.id) }
                                .onSuccess { room = it; stage = PremierStage.Finished }
                                .onFailure { notice = premierError(language, it.message.orEmpty()) }
                            busy = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Red),
                ) { Text(pt(language, "EVET, PES ET", "YES, SURRENDER"), fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showForfeit = false }) { Text(pt(language, "VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showQuickChat && room != null) {
        PremierChatSheet(
            language = language,
            messages = if (room?.isBot == true) botChat else chat,
            meId = backend.currentUserId(),
            isBot = room?.isBot == true,
            onDismiss = { showQuickChat = false },
            onSend = { message ->
                val active = room ?: return@PremierChatSheet
                scope.launch {
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
                        botChatSequence -= 1L
                        botChat = botChat + ChatMessageDto(
                            id = botChatSequence,
                            roomId = active.id,
                            senderId = "bot:${active.id}",
                            body = premierBotChatReply(language, message),
                            createdAt = Instant.now().toString(),
                        )
                        if (!showQuickChat) hasUnreadChat = true
                    } else {
                        runCatching {
                            backend.sendChat(active.id, message)
                            backend.getChat(active.id)
                        }.onSuccess { refreshed ->
                            chat = refreshed
                            notice = ""
                        }.onFailure { notice = premierError(language, it.message.orEmpty()) }
                    }
                }
            },
        )
    }
}

@Composable
private fun PremierLobby(
    language: String,
    profile: ProfileDto?,
    notice: String,
    busy: Boolean,
    onLanguage: (String) -> Unit,
    onPlay: () -> Unit,
    onHome: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onHome) { Icon(Icons.Rounded.ArrowBack, null, tint = PremierUi.Ink) }
            Column(Modifier.weight(1f)) {
                Text("SON HARF", color = PremierUi.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(pt(language, "PREMIER 1v1 DÜELLO", "PREMIER 1v1 DUEL"), color = PremierUi.Ocean, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            }
            PremierLanguageSwitch(language, onLanguage)
        }

        Surface(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
        ) {
            Column(
                Modifier.background(
                    Brush.linearGradient(listOf(Color(0xFF0C2250), PremierUi.OceanDeep))
                ).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarRectWithGender(
                        avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath,
                        gender = profile?.gender,
                        name = profile?.displayName ?: pt(language, "Oyuncu", "Player"),
                        width = 76.dp,
                        height = 58.dp,
                        accent = Color.White,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.displayName ?: pt(language, "Oyuncu", "Player"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("🏆 ${profile?.rating ?: 1000} RP  •  ${profileWinRate(profile)}%", color = Color.White.copy(alpha = .82f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = .16f)) {
                        Text("V4", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
                Text(
                    pt(language, "Kelimeyi sürdür, rakibini geç.", "Keep the word chain alive. Outplay your rival."),
                    color = Color.White,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    pt(language, "Sunucu doğrulamalı ana sözlük • 3 round • canlı skor • rövanş", "Server-verified master dictionary • 3 rounds • live score • rematch"),
                    color = Color.White.copy(alpha = .78f),
                    fontSize = 12.sp,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremierFeatureTile(Icons.Rounded.Verified, pt(language, "ANA SÖZLÜK", "MASTER DICTIONARY"), pt(language, "TR + EN", "TR + EN"), Modifier.weight(1f))
            PremierFeatureTile(Icons.Rounded.Bolt, pt(language, "HIZLI", "FAST"), pt(language, "15 sn tur", "15 sec turn"), Modifier.weight(1f))
            PremierFeatureTile(Icons.Rounded.Groups, pt(language, "CANLI", "LIVE"), "1v1", Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))
        if (notice.isNotBlank()) {
            Surface(shape = RoundedCornerShape(15.dp), color = PremierUi.Ice, border = BorderStroke(1.dp, PremierUi.Border)) {
                Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = PremierUi.OceanDeep, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Button(
            onClick = onPlay,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(68.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean, contentColor = PremierUi.Ink),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(pt(language, "OYNA", "PLAY"), fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
        }
        Text(pt(language, "Rakip bulunamazsa seviye uyumlu bot devreye girer.", "If no rival is found, a level-appropriate bot takes over."), Modifier.fillMaxWidth(), color = PremierUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PremierFeatureTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(17.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Border)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = PremierUi.Ocean, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = PremierUi.Ink, fontWeight = FontWeight.Black, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
            Text(detail, color = PremierUi.Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
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
                    Text(label, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = if (language == code) PremierUi.Ink else PremierUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
private fun PremierSearching(language: String, onCancel: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "search")
    val pulse by transition.animateFloat(0.82f, 1f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "pulse")
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size((132 * pulse).dp).clip(CircleShape).background(Brush.radialGradient(listOf(PremierUi.Sky.copy(alpha = .45f), PremierUi.Ice, Color.Transparent))),
            contentAlignment = Alignment.Center,
        ) {
            Surface(shape = CircleShape, color = PremierUi.Surface, border = BorderStroke(2.dp, PremierUi.Ocean)) {
                Icon(Icons.Rounded.Groups, null, tint = PremierUi.Ocean, modifier = Modifier.padding(27.dp).size(42.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(pt(language, "RAKİP ARANIYOR", "SEARCHING FOR RIVAL"), color = PremierUi.Ink, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(pt(language, "Rating ve dil eşleşmesi yapılıyor…", "Matching rating and language…"), color = PremierUi.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, PremierUi.Border)) {
            Text(pt(language, "İPTAL", "CANCEL"), color = PremierUi.Muted, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremierVsScreen(language: String, me: ProfileDto?, opponent: ProfileDto?, room: GameRoomDto) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Text(pt(language, "RAKİP BULUNDU!", "RIVAL FOUND!"), color = PremierUi.Ocean, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
        Text(pt(language, "Maç 3 saniye içinde başlıyor", "Match starts in 3 seconds"), color = PremierUi.Muted, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        PremierVsPlayerCard(language, me?.displayName ?: pt(language, "Oyuncu", "Player"), me?.avatarPath, me?.gender, me?.avatarVisibility != "hidden", me?.rating ?: 1000, profileWinRate(me), PremierUi.Ocean, nameColor = SonHarfCosmetics.playerNameColor)
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(99.dp), color = Color.Transparent) {
            Box(Modifier.background(Brush.horizontalGradient(listOf(PremierUi.OceanDeep, Color(0xFF0C2250)))).padding(horizontal = 27.dp, vertical = 10.dp)) {
                Text("VS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (room.isBot) {
            PremierVsPlayerCard(language, room.botName ?: pt(language, "KelimeBot", "WordBot"), null, null, true, (me?.rating ?: 1000), 50, PremierUi.OceanDeep, bot = true)
        } else {
            PremierVsPlayerCard(language, opponent?.displayName ?: pt(language, "Rakip", "Rival"), opponent?.avatarPath, opponent?.gender, opponent?.avatarVisibility != "hidden", opponent?.rating ?: 1000, profileWinRate(opponent), PremierUi.OceanDeep)
        }
        Spacer(Modifier.weight(1f))
        Text(pt(language, "Sunucu kilidi aktif • Adil oyun", "Server lock active • Fair play"), color = PremierUi.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun PremierVsPlayerCard(language: String, name: String, avatar: String?, gender: String?, visible: Boolean, rating: Int, winRate: Int, accent: Color, bot: Boolean = false, nameColor: Color = PremierUi.Ink) {
    Surface(modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(23.dp)), shape = RoundedCornerShape(23.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, accent.copy(alpha = .22f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (bot) PremierBotAvatar(size = 70.dp, accent = accent)
            else ProfilePhotoAvatarRectWithGender(
                avatarPath = if (visible) avatar else null,
                gender = gender,
                name = name,
                width = 84.dp,
                height = 64.dp,
                accent = accent,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = nameColor, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (bot) pt(language, "ADAPTİF BOT", "ADAPTIVE BOT") else pt(language, "PREMIER OYUNCU", "PREMIER PLAYER"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PremierStatPill("🏆 $rating RP", accent)
                    PremierStatPill("%$winRate", PremierUi.Green)
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

/** Calm duel palette: soft neutrals, one gentle colour per player, red only for the last seconds. */
private object PremierCalm {
    val Card = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFD9E6EC)
    val Ink = Color(0xFF1F3A4D)
    val Muted = Color(0xFF6E8795)
    val Mine = Color(0xFF3D9AA8)
    val MineSoft = Color(0xFFE4F3F5)
    val Rival = Color(0xFFC98A7F)
    val RivalSoft = Color(0xFFF7ECEA)
    val Amber = Color(0xFFD5A247)
    val Danger = Color(0xFFD4665B)
    val Track = Color(0xFFE3ECF0)
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
    me: ProfileDto?,
    opponent: ProfileDto?,
    meId: String?,
    words: List<GameWordDto>,
    input: String,
    notice: String,
    busy: Boolean,
    turnSeconds: Int,
    unreadChat: Boolean,
    floatingMessage: ChatMessageDto?,
    moveFeedback: PremierMoveFeedback?,
    botThinking: Boolean,
    prepSeconds: Int,
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
    val rivalName = if (room.isBot) room.botName ?: pt(language, "KelimeBot", "WordBot") else opponent?.displayName ?: pt(language, "Rakip", "Rival")
    val required = premierRequiredToken(room, words)
    val latestMove = words.lastOrNull()
    val latestPlayedWord = latestMove?.let { premierUpper(it.normalizedWord.ifBlank { it.word }, language) }.orEmpty()
    val latestMoveMine = latestMove != null && latestMove.playerId == meId

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
        latestMoveMine -> myGain?.second?.coerceAtLeast(0) ?: 3
        else -> rivalGain?.second?.coerceAtLeast(0) ?: 3
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
            pt(language, "$rivalName kelime bulamadı • -1", "$rivalName found no word • -1")
        } else {
            pt(language, "Rakip hata yaptı • -1", "Rival slipped • -1")
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
        moveFeedback?.accepted == true && latestMoveScore >= 6 -> WordSiegeMascotEmotion.PROUD
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
                latestMoveScore >= 6 -> WordSiegeMascotEvent.BIG_PRAISE
                else -> WordSiegeMascotEvent.PRAISE
            },
            word = premierUpper(latestWordText, language),
        )
        moveFeedback?.accepted == false -> WordSiegeMascotSignal("no:${room.id}:${words.size}:${moveFeedback.message}", WordSiegeMascotEvent.COMFORT)
        myTurn && !preparing && turnSeconds in 1..5 -> WordSiegeMascotSignal("time:${room.id}:${room.roundNo}:${words.size}", WordSiegeMascotEvent.CRITICAL)
        latestMove != null && !latestMoveMine && latestMoveScore >= 6 ->
            WordSiegeMascotSignal("rival:${latestMove.id}", WordSiegeMascotEvent.RIVAL_STRONG)
        rivalScore - myScore >= 25 -> WordSiegeMascotSignal("behind:${room.id}:${room.roundNo}", WordSiegeMascotEvent.BEHIND)
        myScore - rivalScore >= 30 -> WordSiegeMascotSignal("ahead:${room.id}:${room.roundNo}", WordSiegeMascotEvent.AHEAD)
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
    // The mascot's home perch is the slot beside the target; it can fly across the arena.
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
        val targetSize = if (veryCompact) 74.dp else if (compact) 86.dp else if (tall) 112.dp else 100.dp
        val mascotSize = if (veryCompact) 64.dp else if (compact) 72.dp else if (tall) 94.dp else 84.dp
        val keyHeight = if (veryCompact) 36.dp else if (compact) 39.dp else if (tall) 48.dp else 44.dp
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
            PremierArenaHeader(
                language = language,
                room = room,
                me = me,
                opponent = opponent,
                rivalName = rivalName,
                myRoundScore = myRoundScore,
                rivalRoundScore = rivalRoundScore,
                myRounds = myRounds,
                rivalRounds = rivalRounds,
                myStreak = myStreak,
                rivalStreak = rivalStreak,
                myTurn = myTurn && !preparing,
                rivalTurn = !myTurn && !preparing && room.status in setOf("playing", "final", "sudden_death"),
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(primaryGap))
                if (reconnectGraceActive) {
                    PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                } else {
                    PremierTurnBadge(language, myTurn && !preparing, room.status, botThinking && !preparing, rivalName)
                }
                Spacer(Modifier.height(primaryGap))
                // The one place to look: the letter to play and the clock around it.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((if (targetSize > mascotSize) targetSize else mascotSize) + 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PremierTargetCard(
                        language = language,
                        required = required,
                        gameMode = room.gameMode,
                        round = room.roundNo,
                        size = targetSize,
                        seconds = if (reconnectGraceActive || (myTurn && !preparing)) turnSeconds else PREMIER_TURN_SECONDS,
                        totalSeconds = if (reconnectGraceActive) PREMIER_RECONNECT_SECONDS else PREMIER_TURN_SECONDS,
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
                Spacer(Modifier.height(primaryGap))
                // One line: feedback for a moment, otherwise the last word with its linking letter.
                val feedback = moveFeedback
                val slip = rivalSlip
                Box(Modifier.fillMaxWidth().height(if (veryCompact) 26.dp else 30.dp), contentAlignment = Alignment.Center) {
                    when {
                        feedback != null -> Text(
                            feedback.message,
                            color = if (feedback.accepted) PremierCalm.Mine else PremierCalm.Danger,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        slip != null -> Text(slip, color = PremierCalm.Rival, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        else -> PremierLastWordCard(
                            language = language,
                            word = latestPlayedWord,
                            mine = latestMoveMine,
                            linkLetters = if (latestPlayedWord.isBlank()) 0 else required.length,
                            veryCompact = veryCompact,
                        )
                    }
                }
                if (!compact) {
                    Spacer(Modifier.height(primaryGap))
                    PremierPressureStrip(language, myRoundScore, rivalRoundScore, myStreak, rivalStreak, if (myTurn && !preparing) turnSeconds else 0)
                }
                if (!veryCompact) {
                    Spacer(Modifier.height(primaryGap))
                    PremierWordTrail(words, language, isPro, meId)
                }
                Spacer(Modifier.weight(1f).heightIn(min = 2.dp))
                if (notice.isNotBlank()) {
                    Text(notice, color = PremierCalm.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onForfeit,
                    modifier = Modifier.weight(1f).height(if (veryCompact) 32.dp else 36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Rounded.Flag, null, tint = PremierCalm.Muted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(pt(language, "PES ET", "SURRENDER"), color = PremierCalm.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Box(Modifier.weight(1f)) {
                    TextButton(
                        onClick = onQuickChat,
                        modifier = Modifier.fillMaxWidth().height(if (veryCompact) 32.dp else 36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, tint = PremierCalm.Mine, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(pt(language, "SOHBET", "CHAT"), color = PremierCalm.Mine, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    if (unreadChat) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                                .size(9.dp).clip(CircleShape).background(PremierCalm.Danger)
                        )
                    }
                }
            }
            // Keep the live input and custom keyboard outside the flexible arena body.
            PremierInputBar(
                language,
                input,
                required,
                myTurn && !preparing,
                busy,
                usedWords = words,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
            PremierKeyboard(language, input, enabled = myTurn && !busy && !preparing, keyHeight = keyHeight, onInput = onInput, onSubmit = onSubmit)
        }

        val slotCenter = mascotSlotCenter
        val mascotAnchors = if (slotCenter == null || arenaSize.width == 0 || arenaSize.height == 0) {
            emptyList()
        } else {
            val home = Offset(
                (slotCenter.x - arenaOrigin.x) / arenaSize.width,
                (slotCenter.y - arenaOrigin.y) / arenaSize.height,
            )
            listOf(home, Offset(1f - home.x, home.y))
        }
        WordSiegeMascotCompanion(
            anchors = mascotAnchors,
            mascotSize = mascotSize,
            moveId = latestMove?.id,
            lastMoveMine = latestMoveMine,
            playerTurn = myTurn,
            modifier = Modifier.matchParentSize(),
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
        )

        if (myTurn && room.validWordCount > 0) {
            PurchasedVictoryVfx(
                eventKey = "turn:${room.id}:${room.validWordCount}",
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (moveFeedback?.accepted == true) {
            PurchasedVictoryVfx(
                eventKey = "accepted:${room.id}:${room.validWordCount}:${moveFeedback.message}",
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(visible = floatingMessage != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 96.dp, start = 22.dp, end = 22.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = PremierCalm.Card, border = BorderStroke(1.dp, PremierCalm.CardBorder), shadowElevation = 6.dp) {
                Text(floatingMessage?.body.orEmpty(), Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = PremierCalm.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Between rounds: a calm breather with a countdown before the next round begins.
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

/** A human-like pause before the bot answers: quick for easy letters, longer when it "thinks". */
private fun premierBotThinkMillis(room: GameRoomDto, words: List<GameWordDto>): Long {
    val required = premierRequiredToken(room, words)
    val hardLetter = required.lowercase(Locale.ROOT) in setOf("ğ", "j", "ı", "ü", "z", "l", "v", "ö", "x", "q", "y", "k")
    val base = if (hardLetter) 1_700L else 1_050L
    return base + kotlin.random.Random.nextLong(0L, 1_600L)
}

/** The break between rounds: last round's result, the score and a big countdown. */
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
) {
    Box(Modifier.fillMaxSize().background(Color(0xE6F4F9FB)), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = PremierCalm.Card,
            border = BorderStroke(1.dp, PremierCalm.CardBorder),
            shadowElevation = 10.dp,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Column(Modifier.padding(horizontal = 26.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (lastRoundWon != null) {
                    Text(
                        if (lastRoundWon) pt(language, "Raundu kazandın 🏆", "You won the round 🏆") else pt(language, "Raund rakibin", "Rival took the round"),
                        color = if (lastRoundWon) PremierCalm.Mine else PremierCalm.Rival,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text("$myRounds : $rivalRounds", color = PremierCalm.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (suddenDeath) pt(language, "ANİ ÖLÜM", "SUDDEN DEATH") else pt(language, "RAUND $round", "ROUND $round"),
                    color = PremierCalm.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Text(pt(language, "Hazırlan", "Get ready"), color = PremierCalm.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    "$seconds",
                    color = PremierCalm.Mine,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (youStart) pt(language, "Yeni raundu sen başlatıyorsun", "You open the new round")
                    else pt(language, "Yeni raundu rakibin başlatıyor", "Your rival opens the new round"),
                    color = PremierCalm.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
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
    myRounds: Int,
    rivalRounds: Int,
    myStreak: Int,
    rivalStreak: Int,
    myTurn: Boolean,
    rivalTurn: Boolean,
) {
    val cardHeight = 60.dp
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            accent = PremierCalm.Mine,
            soft = PremierCalm.MineSoft,
            modifier = Modifier.weight(1f).height(cardHeight),
            nameColor = SonHarfCosmetics.playerNameColor,
        )
        Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (room.status == "sudden_death") pt(language, "ANİ ÖLÜM", "SUDDEN") else pt(language, "RAUND ${room.roundNo}/3", "ROUND ${room.roundNo}/3"),
                color = if (room.status == "sudden_death") PremierCalm.Danger else PremierCalm.Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text("$myRounds : $rivalRounds", color = PremierCalm.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        PremierSymmetricPlayerCard(
            language = language,
            name = rivalName,
            avatar = opponent?.avatarPath,
            gender = opponent?.gender,
            visible = opponent?.avatarVisibility != "hidden",
            score = rivalRoundScore,
            streak = rivalStreak,
            active = rivalTurn,
            accent = PremierCalm.Rival,
            soft = PremierCalm.RivalSoft,
            modifier = Modifier.weight(1f).height(cardHeight),
            bot = room.isBot,
        )
    }
}

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
    accent: Color,
    soft: Color,
    modifier: Modifier,
    bot: Boolean = false,
    nameColor: Color = PremierArenaSky.Ink,
) {
    val shownScore by animateIntAsState(score, tween(400), label = "score-count")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (active) soft else PremierCalm.Card,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) accent else PremierCalm.CardBorder),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (bot) {
                PremierBotAvatar(size = 34.dp, accent = accent)
            } else {
                ProfilePhotoAvatarRectWithGender(
                    avatarPath = if (visible) avatar else null,
                    gender = gender,
                    name = name,
                    width = 34.dp,
                    height = 34.dp,
                    accent = accent,
                )
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    color = if (nameColor == PremierUi.Ink || nameColor == PremierArenaSky.Ink) PremierCalm.Ink else nameColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (streak >= 2) Text("🔥 $streak", color = PremierCalm.Amber, fontSize = 9.sp, fontWeight = FontWeight.Black)
                else Text(pt(language, "Raund Puanı", "Round Score"), color = PremierCalm.Muted, fontSize = 8.sp, maxLines = 1)
            }
            Text(shownScore.toString(), color = PremierCalm.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
            if (bot) PremierBotAvatar(size = 58.dp, accent = accent)
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
private fun PremierBotAvatar(size: Dp, accent: Color) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = PremierUi.Ice,
        border = BorderStroke(3.dp, accent),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.SmartToy, null, tint = accent, modifier = Modifier.size(size * .42f))
                Text("BOT", color = accent, fontSize = (size.value * .12f).sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PremierTurnBadge(language: String, myTurn: Boolean, status: String, botThinking: Boolean = false, rivalName: String = "") {
    val active = status in setOf("playing", "final", "sudden_death")
    val accent = if (myTurn) PremierCalm.Mine else PremierCalm.Rival
    val label = when {
        !active -> pt(language, "ARENA SENKRONİZE EDİLİYOR", "SYNCING ARENA")
        myTurn -> pt(language, "⚡ HAMLE SENDE • SALDIR", "⚡ YOUR MOVE • STRIKE")
        botThinking -> pt(language, "$rivalName düşünüyor…", "$rivalName is thinking…")
        else -> pt(language, "◉ RAKİP HAMLESİ • HAZIR OL", "◉ RIVAL MOVE • STAY READY")
    }
    Text(
        label,
        color = accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = .6.sp,
        maxLines = 1,
    )
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

/** A single quiet status line: who leads the round, and the streaks. */
@Composable
private fun PremierPressureStrip(
    language: String,
    myScore: Int,
    rivalScore: Int,
    myStreak: Int,
    rivalStreak: Int,
    seconds: Int,
) {
    val lead = myScore - rivalScore
    val danger = seconds in 1..5
    Text(
        when {
            danger -> pt(language, "KRİTİK 5 SANİYE", "CRITICAL 5 SECONDS")
            lead > 0 -> pt(language, "Raundda öndesin +$lead", "You lead the round +$lead")
            lead < 0 -> pt(language, "Raundda ${-lead} puan gerideysin", "${-lead} behind this round")
            else -> pt(language, "Raund dengede", "Round is level")
        } + if (myStreak >= 2 || rivalStreak >= 2) pt(language, "  •  Seri $myStreak : $rivalStreak", "  •  Streak $myStreak : $rivalStreak") else "",
        color = if (danger) PremierCalm.Danger else PremierCalm.Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

/** The letter to play on a plain white disc, circled by the turn clock; the seconds sit below it. */
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
    val danger = active && seconds in 1..5
    val progress by animateFloatAsState(
        (seconds.coerceIn(0, totalSeconds).toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f),
        tween(300),
        label = "clock",
    )
    val ringColor = when {
        !active -> PremierCalm.Track
        seconds <= 5 -> PremierCalm.Danger
        seconds <= 8 -> PremierCalm.Amber
        else -> PremierCalm.Mine
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(size + 12.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.matchParentSize().drawBehind {
                    val stroke = 6.dp.toPx()
                    val inset = stroke / 2f
                    drawArc(
                        color = PremierCalm.Track,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(this.size.width - stroke, this.size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                    if (active) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(this.size.width - stroke, this.size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
            )
            Surface(
                modifier = Modifier.size(size),
                shape = CircleShape,
                color = PremierCalm.Card,
                shadowElevation = 3.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        required,
                        color = PremierCalm.Ink,
                        fontSize = (size.value * if (required.length > 1) .28f else .41f).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        when {
                            suddenDeath -> pt(language, "ANİ ÖLÜM", "SUDDEN DEATH")
                            required == "★" -> pt(language, "SERBEST", "FREE")
                            gameMode == "expert" -> "x${round.coerceIn(1, 3)}"
                            else -> pt(language, "HEDEF HARF", "TARGET LETTER")
                        },
                        color = if (suddenDeath) PremierCalm.Danger else PremierCalm.Muted,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
        if (active) {
            Spacer(Modifier.width(14.dp))
            // The clock, big and in one place.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$seconds",
                    color = if (danger) PremierCalm.Danger else PremierCalm.Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(pt(language, "saniye", "seconds"), color = PremierCalm.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** The word just played, sliding in, with the letters that link to the next word highlighted. */
@Composable
private fun PremierLastWordCard(
    language: String,
    word: String,
    mine: Boolean,
    linkLetters: Int,
    veryCompact: Boolean,
) {
    val accent = if (mine) PremierCalm.Mine else PremierCalm.Rival
    AnimatedContent(
        targetState = word,
        transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
        label = "last-word",
    ) { shown ->
        val latestPlayedWord = shown
        Text(
            buildAnnotatedString {
                val text = latestPlayedWord.ifBlank { pt(language, "İLK KELİME SERBEST", "FREE OPENING WORD") }
                val link = if (latestPlayedWord.isBlank()) 0 else linkLetters.coerceIn(0, text.length)
                append(text.dropLast(link))
                withStyle(SpanStyle(color = PremierCalm.Amber)) { append(text.takeLast(link)) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            color = if (latestPlayedWord.isBlank()) PremierCalm.Muted else accent,
            fontSize = if (veryCompact) 14.sp else 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun PremierWordTrail(words: List<GameWordDto>, language: String, isPro: Boolean = false, meId: String? = null) {
    if (words.isEmpty()) {
        Text(pt(language, "İlk zinciri sen başlatabilirsin.", "You can start the first chain."), color = PremierCalm.Muted, fontSize = 10.sp)
        return
    }
    // Pro users see the full played-word history so they can avoid repeats;
    // everyone else keeps the compact trailing preview.
    val ordered = if (isPro) words.reversed() else words.takeLast(12).reversed()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isPro) {
            Text(
                pt(language, "PRO • Tüm oynanan kelimeler (${words.size})", "PRO • All played words (${words.size})"),
                color = PremierCalm.Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        LazyRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            items(ordered, key = { it.id }) { entry ->
                val mine = entry.playerId != null && entry.playerId == meId
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (mine) PremierCalm.MineSoft else PremierCalm.RivalSoft,
                ) {
                    Text(
                        premierUpper(entry.normalizedWord.ifBlank { entry.word }, language),
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = PremierCalm.Ink.copy(alpha = .75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Your word as letter tiles; the required opening letters are marked, a mistake shows in red. */
@Composable
private fun PremierInputBar(
    language: String,
    input: String,
    required: String,
    myTurn: Boolean,
    busy: Boolean,
    usedWords: List<GameWordDto> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val problem = if (myTurn && input.length >= 2) {
        premierLocalRejection(input, required, usedWords.map { it.normalizedWord.ifBlank { it.word } }, language)
    } else if (myTurn && input.isNotEmpty() && required != "★" && !premierUpper(input, language).startsWith(required.take(input.length))) {
        "wrong_start_letter"
    } else {
        null
    }
    val accent = when {
        problem != null -> PremierCalm.Danger
        myTurn -> PremierCalm.Mine
        else -> PremierCalm.CardBorder
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PremierCalm.Card,
        border = BorderStroke(if (myTurn) 2.dp else 1.dp, accent),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
                if (input.isNotBlank() && !busy) {
                    val shown = premierUpper(input, language)
                    val prefix = if (required == "★") 0 else required.length
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        items(shown.length) { i ->
                            val lead = i < prefix
                            Box(
                                Modifier
                                    .size(width = 22.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (lead) PremierCalm.MineSoft else Color(0xFFF3F6F8)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(shown[i].toString(), color = PremierCalm.Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
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
                        color = PremierCalm.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (problem != null) {
                Text(
                    validationMessage(language, problem),
                    color = PremierCalm.Danger,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 3.dp),
                )
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
        border = BorderStroke(1.dp, PremierUi.Border),
        shadowElevation = 14.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            rows.forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = if (index == 1) 7.dp else if (index == 2) 16.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { key ->
                        PremierKey(key, enabled && value.length < 30, Modifier.weight(1f), keyHeight = keyHeight) {
                            SonHarfSoundFx.typingClick()
                            onInput((value + key).take(30))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PremierKey("⌫", enabled && value.isNotEmpty(), Modifier.weight(1f), keyHeight = keyHeight, alt = true) { onInput(value.dropLast(1)); SonHarfSoundFx.tap() }
                PremierKey(pt(language, "TEMİZLE", "CLEAR"), enabled && value.isNotEmpty(), Modifier.weight(1.45f), keyHeight = keyHeight, alt = true) { onInput(""); SonHarfSoundFx.tap() }
                PremierKey(pt(language, "GÖNDER  ➤", "SEND  ➤"), enabled && value.length >= 2, Modifier.weight(2.2f), keyHeight = keyHeight, action = true) { onSubmit(); SonHarfSoundFx.tap() }
            }
        }
    }
}

@Composable
private fun PremierKey(label: String, enabled: Boolean, modifier: Modifier, keyHeight: Dp, alt: Boolean = false, action: Boolean = false, onClick: () -> Unit) {
    val palette = SonHarfCosmetics.keyboardPalette
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(keyHeight),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when { action -> palette.action; alt -> palette.keyAlt; else -> palette.key },
            contentColor = if (action) palette.actionText else palette.text,
            disabledContainerColor = if (alt) palette.keyAlt.copy(alpha = .55f) else palette.key.copy(alpha = .55f),
            disabledContentColor = palette.text.copy(alpha = .42f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (action) 7.dp else 2.dp),
        border = BorderStroke(1.dp, when { action -> palette.action.copy(alpha = .82f); alt -> palette.secondaryBorder.copy(alpha = .55f); else -> palette.border }),
    ) {
        Text(label, fontSize = if (label.length > 5) 9.sp else 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PremierUi.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = PremierUi.Ocean, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(pt(language, "Maç Sohbeti", "Match Chat"), color = PremierUi.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (isBot) pt(language, "Bot ile serbestçe yazış.", "Chat freely with the bot.")
                        else pt(language, "Rakibinle gerçek zamanlı mesajlaş.", "Message your rival in real time."),
                        color = PremierUi.Muted,
                        fontSize = 10.sp,
                    )
                }
            }

            if (messages.isEmpty()) {
                Surface(shape = RoundedCornerShape(14.dp), color = PremierUi.Background) {
                    Text(
                        pt(language, "Henüz mesaj yok. İlk mesajı sen gönder.", "No messages yet. Send the first one."),
                        Modifier.fillMaxWidth().padding(14.dp),
                        color = PremierUi.Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(messages.takeLast(50), key = { it.id }) { message ->
                        val mine = message.senderId == meId
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (mine) PremierUi.GreenSoft else PremierUi.Ice,
                                border = BorderStroke(1.dp, if (mine) PremierUi.Green.copy(alpha = .22f) else PremierUi.Border),
                            ) {
                                Text(
                                    message.body,
                                    Modifier.widthIn(max = 280.dp).padding(horizontal = 12.dp, vertical = 9.dp),
                                    color = PremierUi.Ink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
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
                        focusedBorderColor = PremierUi.Ocean,
                        unfocusedBorderColor = PremierUi.Border,
                        focusedContainerColor = PremierUi.Surface,
                        unfocusedContainerColor = PremierUi.Surface,
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
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PremierUi.Ocean),
                ) {
                    Icon(Icons.Rounded.Send, pt(language, "Gönder", "Send"))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
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
                "Evet, botum. Ama kelimelere gönülden bağlıyım. 🤖💙" to "Yes, I'm a bot. But I truly love words. 🤖💙",
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
    "invalid_length" -> pt(language, "Kelime 2-30 harf arasında olmalı.", "Word must be 2-30 letters.")
    "invalid_characters" -> pt(language, "Bu dil için geçersiz karakter var.", "The word contains invalid characters for this language.")
    "not_in_dictionary", "invalid_word" -> pt(language, "Kelime ana sözlükte yok.", "Word is not in the master dictionary.")
    "abbreviation_not_allowed" -> pt(language, "Kısaltmalar kullanılamaz.", "Abbreviations are not allowed.")
    "proper_noun_not_allowed" -> pt(language, "Özel adlar kullanılamaz.", "Proper nouns are not allowed.")
    "not_game_allowed" -> pt(language, "Bu kelime oyun için uygun değil.", "This word is not allowed in gameplay.")
    "ends_with_soft_g" -> pt(language, "Ğ ile biten kelimeler kullanılamaz.", "Words ending with Ğ are not allowed.")
    "wrong_start_letter" -> pt(language, "Kelime hedef harf/harflerle başlamalı.", "Word must start with the target letter/letters.")
    "word_already_used" -> pt(language, "Bu kelime daha önce kullanıldı.", "This word has already been used.")
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
