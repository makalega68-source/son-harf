package com.sonharf.game

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

/** Yetişkin, yüksek okunabilirlikli Son Harf oyun paleti. */
private object PremierUi {
    val Background = Color(0xFFF3EEE5)
    val Surface = Color(0xFFFFFBF4)
    val Ink = Color(0xFF173247)
    val Muted = Color(0xFF6F7B7C)
    val Ocean = Color(0xFF4F8F96)
    val OceanDeep = Color(0xFF2F6970)
    val Sky = Color(0xFF8EB7B5)
    val Ice = Color(0xFFE6EFEB)
    val Border = Color(0xFFD8D0C4)
    val Green = Color(0xFF789B73)
    val GreenSoft = Color(0xFFE5ECDD)
    val Red = Color(0xFFC86459)
    val RedSoft = Color(0xFFF4DDD7)
    val Gold = Color(0xFFD1A13E)
    val GoldSoft = Color(0xFFF5E8BB)
    val Rival = Color(0xFFD27869)
    val RivalSoft = Color(0xFFF6E2DC)
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

        // Server play is authoritative. This path only repairs an already-stuck/transient
        // bot_turn row after a delayed realtime room update.
        delay(350)
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
                        onInput = { input = it },
                        onForfeit = { showForfeit = true },
                        onQuickChat = {
                            hasUnreadChat = false
                            showQuickChat = true
                        },
                        onSubmit = {
                            if (busy || input.isBlank()) return@PremierArena
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
                                            SonHarfSoundFx.scoreTick()
                                        } else {
                                            val reason = next.lastEvent.orEmpty()
                                            notice = ""
                                            moveFeedback = PremierMoveFeedback(
                                                accepted = false,
                                                message = "${pt(language, "YANLIŞ", "WRONG")} • ${validationMessage(language, reason)}",
                                            )
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
    onInput: (String) -> Unit,
    onForfeit: () -> Unit,
    onQuickChat: () -> Unit,
    onSubmit: () -> Unit,
) {
    val isPro = me?.isVip == true
    val amHost = meId == room.hostId
    val myScore = if (amHost) room.hostScore else room.guestScore
    val rivalScore = if (amHost) room.guestScore else room.hostScore
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
    val rivalName = if (room.isBot) room.botName ?: pt(language, "KelimeBot", "WordBot") else opponent?.displayName ?: pt(language, "Rakip", "Rival")
    val required = premierRequiredToken(room, words)
    val lastWord = words.lastOrNull()
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(myTurn) {
        if (myTurn) {
            delay(140)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background)))
            .imePadding()
    ) {
        val compact = maxHeight < 650.dp
        val targetSize = if (compact) 94.dp else 116.dp

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            PremierArenaHeader(
                language = language,
                room = room,
                me = me,
                opponent = opponent,
                rivalName = rivalName,
                myScore = myScore,
                rivalScore = rivalScore,
                myRounds = myRounds,
                rivalRounds = rivalRounds,
                myStreak = myStreak,
                rivalStreak = rivalStreak,
                seconds = turnSeconds,
                unreadChat = unreadChat,
                onForfeit = onForfeit,
                onQuickChat = onQuickChat,
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (reconnectGraceActive) PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                        else PremierTurnBadge(language, myTurn, room.status)
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PremierProWordPanel(
                            language = language,
                            title = pt(language, "Bulunan Kelimeler", "Found Words"),
                            icon = Icons.Rounded.MenuBook,
                            isPro = isPro,
                            entries = words.filter { it.playerId == meId }.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                        )
                        Column(
                            modifier = Modifier.width(targetSize + 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(pt(language, "Son Harf", "Last Letter"), color = PremierUi.Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)
                        }
                        PremierProWordPanel(
                            language = language,
                            title = pt(language, "Kelime Geçmişi", "Word History"),
                            icon = Icons.Rounded.History,
                            isPro = isPro,
                            entries = words.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    PremierLastWordBar(language = language, word = lastWord, meId = meId)
                }
                if (notice.isNotBlank()) {
                    item {
                        Text(
                            notice,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            color = PremierUi.OceanDeep,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            PremierInputBar(
                language = language,
                input = input,
                required = required,
                myTurn = myTurn,
                busy = busy,
                onInput = onInput,
                onSubmit = onSubmit,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .focusRequester(focusRequester),
            )

            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onForfeit,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremierUi.RedSoft, contentColor = PremierUi.Red),
                    border = BorderStroke(1.dp, PremierUi.Red.copy(alpha = .32f)),
                ) {
                    Icon(Icons.Rounded.Flag, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(pt(language, "Pes Et", "Surrender"), fontWeight = FontWeight.Black)
                }
                Box(Modifier.weight(1f)) {
                    Button(
                        onClick = onQuickChat,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean, contentColor = Color.White),
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(pt(language, "Sohbet", "Chat"), fontWeight = FontWeight.Black)
                    }
                    if (unreadChat) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(10.dp).clip(CircleShape).background(PremierUi.Red))
                    }
                }
            }
        }

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

        AnimatedVisibility(
            visible = floatingMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 154.dp, start = 22.dp, end = 22.dp),
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Ocean.copy(alpha = .32f)), shadowElevation = 7.dp) {
                Text(floatingMessage?.body.orEmpty(), Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = PremierUi.Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }

        AnimatedVisibility(
            visible = moveFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 26.dp),
        ) {
            val feedback = moveFeedback
            if (feedback != null) {
                val accent = if (feedback.accepted) PremierUi.Green else PremierUi.Red
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (feedback.accepted) PremierUi.GreenSoft else PremierUi.RedSoft,
                    border = BorderStroke(2.dp, accent),
                    shadowElevation = 12.dp,
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (feedback.accepted) Icons.Rounded.CheckCircle else Icons.Rounded.Close, null, tint = accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(feedback.message, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
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
    myScore: Int,
    rivalScore: Int,
    myRounds: Int,
    rivalRounds: Int,
    myStreak: Int,
    rivalStreak: Int,
    seconds: Int,
    unreadChat: Boolean,
    onForfeit: () -> Unit,
    onQuickChat: () -> Unit,
) {
    val myRating = me?.rating ?: 1000
    val rivalRating = if (room.isBot) myRating else opponent?.rating ?: 1000
    val danger = seconds in 1..5
    val progress = (room.roundWordCount.coerceIn(0, 10) / 10f)

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremierCalmPlayerCard(
                name = me?.displayName ?: pt(language, "Sen", "You"),
                avatar = me?.avatarPath,
                gender = me?.gender,
                visible = me?.avatarVisibility != "hidden",
                rating = myRating,
                score = myScore,
                streak = myStreak,
                accent = PremierUi.Ocean,
                soft = PremierUi.Ice,
                modifier = Modifier.weight(1f),
                language = language,
                nameColor = SonHarfCosmetics.playerNameColor,
            )
            Surface(
                modifier = Modifier.width(108.dp),
                shape = RoundedCornerShape(18.dp),
                color = PremierUi.Surface,
                border = BorderStroke(1.dp, PremierUi.Border),
                shadowElevation = 3.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(pt(language, "Raund ${room.roundNo} / 3", "Round ${room.roundNo} / 3"), color = PremierUi.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("$myRounds - $rivalRounds", color = PremierUi.Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text(pt(language, "2 raund kazanan\nmaçı kazanır", "First to 2 rounds\nwins the match"), color = PremierUi.Muted, fontSize = 7.sp, lineHeight = 9.sp, textAlign = TextAlign.Center)
                }
            }
            PremierCalmPlayerCard(
                name = rivalName,
                avatar = opponent?.avatarPath,
                gender = opponent?.gender,
                visible = opponent?.avatarVisibility != "hidden",
                rating = rivalRating,
                score = rivalScore,
                streak = rivalStreak,
                accent = PremierUi.Rival,
                soft = PremierUi.RivalSoft,
                modifier = Modifier.weight(1f),
                language = language,
                bot = room.isBot,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = PremierUi.Surface,
                border = BorderStroke(1.dp, PremierUi.Border),
            ) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = if (danger) PremierUi.Red else PremierUi.OceanDeep, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("00:${seconds.coerceAtLeast(0).toString().padStart(2, '0')}", color = if (danger) PremierUi.Red else PremierUi.Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { (seconds.coerceIn(0, PREMIER_TURN_SECONDS) / PREMIER_TURN_SECONDS.toFloat()) },
                        modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(99.dp)),
                        color = if (danger) PremierUi.Red else PremierUi.Ocean,
                        trackColor = PremierUi.Border.copy(alpha = .45f),
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = PremierUi.Surface,
                border = BorderStroke(1.dp, PremierUi.Border),
            ) {
                Column(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MenuBook, null, tint = PremierUi.Gold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pt(language, "Kelime ${room.roundWordCount.coerceIn(0, 10)} / 10", "Word ${room.roundWordCount.coerceIn(0, 10)} / 10"), color = PremierUi.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                        color = PremierUi.Green,
                        trackColor = PremierUi.Border.copy(alpha = .45f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PremierCalmPlayerCard(
    language: String,
    name: String,
    avatar: String?,
    gender: String?,
    visible: Boolean,
    rating: Int,
    score: Int,
    streak: Int,
    accent: Color,
    soft: Color,
    modifier: Modifier,
    bot: Boolean = false,
    nameColor: Color = PremierUi.Ink,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = soft,
        border = BorderStroke(1.dp, accent.copy(alpha = .32f)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bot) PremierBotAvatar(size = 42.dp, accent = accent)
                else ProfilePhotoAvatarRectWithGender(
                    avatarPath = if (visible) avatar else null,
                    gender = gender,
                    name = name,
                    width = 46.dp,
                    height = 40.dp,
                    accent = accent,
                )
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = nameColor, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(premierLeagueLabel(rating, language), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("🏆 $rating", color = PremierUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(pt(language, "Raund Puanı", "Round Score"), color = PremierUi.Muted, fontSize = 7.sp)
                    if (streak >= 2) Text("🔥 $streak", color = PremierUi.Red, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Text(score.toString(), color = PremierUi.Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun premierLeagueLabel(rating: Int, language: String): String {
    val name = ratingLeagueProgress(rating).leagueName
    val localized = when (name) {
        "BRONZ" -> pt(language, "Bronz", "Bronze")
        "GÜMÜŞ" -> pt(language, "Gümüş", "Silver")
        "ALTIN" -> pt(language, "Altın", "Gold")
        "PLATİN" -> pt(language, "Platin", "Platinum")
        "ELMAS" -> pt(language, "Elmas", "Diamond")
        else -> pt(language, "Efsane", "Legend")
    }
    return "$localized ${pt(language, "Lig", "League")}"
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
private fun PremierTurnBadge(language: String, myTurn: Boolean, status: String) {
    val active = status in setOf("playing", "final", "sudden_death")
    val accent = if (myTurn) PremierUi.Green else PremierUi.Gold
    Surface(shape = RoundedCornerShape(99.dp), color = if (myTurn) PremierUi.GreenSoft else PremierUi.GoldSoft, border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Text(
            when {
                !active -> pt(language, "MAÇ SENKRONİZE EDİLİYOR", "SYNCING MATCH")
                myTurn -> pt(language, "⚡ SENİN SIRAN", "⚡ YOUR TURN")
                else -> pt(language, "⏳ RAKİP DÜŞÜNÜYOR", "⏳ RIVAL IS THINKING")
            },
            Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .4.sp,
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
    val accent = when {
        danger -> PremierUi.Red
        lead > 0 -> PremierUi.Green
        lead < 0 -> PremierUi.Gold
        else -> PremierUi.Ocean
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF081624),
        border = BorderStroke(1.dp, accent.copy(alpha = .42f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                when {
                    danger -> pt(language, "KRİTİK 5 SANİYE", "CRITICAL 5 SECONDS")
                    lead > 0 -> pt(language, "BASKI SENDE +$lead", "YOUR PRESSURE +$lead")
                    lead < 0 -> pt(language, "GERİ DÖNÜŞ FIRSATI ${-lead}", "COMEBACK WINDOW ${-lead}")
                    else -> pt(language, "DENGE NOKTASI", "DEAD EVEN")
                },
                color = accent,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .5.sp,
            )
            Text(
                pt(language, "SERİ $myStreak : $rivalStreak", "STREAK $myStreak : $rivalStreak"),
                color = PremierUi.Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PremierTargetCard(language: String, required: String, gameMode: String, round: Int, size: Dp) {
    val transition = rememberInfiniteTransition(label = "letter")
    val glow by transition.animateFloat(.12f, .30f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow")
    Box(
        Modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF1F4E7), PremierUi.GreenSoft)))
            .then(Modifier.background(PremierUi.Green.copy(alpha = glow * .10f))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            required,
            color = PremierUi.Ink,
            fontSize = (size.value * if (required.length > 1) .31f else .48f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .6.sp,
        )
    }
}

@Composable
private fun PremierProWordPanel(
    language: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPro: Boolean,
    entries: List<GameWordDto>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 150.dp),
        shape = RoundedCornerShape(18.dp),
        color = PremierUi.Surface,
        border = BorderStroke(1.dp, PremierUi.Border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = PremierUi.OceanDeep, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, color = PremierUi.Ink, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!isPro) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Lock, null, tint = PremierUi.OceanDeep, modifier = Modifier.size(27.dp))
                Text(pt(language, "Sadece PRO üyeler görebilir", "Visible to PRO members"), color = PremierUi.Muted, fontSize = 7.sp, textAlign = TextAlign.Center, lineHeight = 9.sp)
                Surface(shape = RoundedCornerShape(99.dp), color = PremierUi.GoldSoft, border = BorderStroke(1.dp, PremierUi.Gold.copy(alpha = .5f))) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = PremierUi.Gold, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("PRO", color = PremierUi.Ink, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.weight(1f))
            } else if (entries.isEmpty()) {
                Spacer(Modifier.weight(1f))
                Text(pt(language, "Henüz kelime yok", "No words yet"), color = PremierUi.Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(1f))
            } else {
                entries.forEach { entry ->
                    val shown = premierUpper(entry.normalizedWord.ifBlank { entry.word }, language)
                    val points = DictionaryEngine.calculatePoints(entry.normalizedWord.ifBlank { entry.word }, language)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(shown, modifier = Modifier.weight(1f), color = PremierUi.Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("+$points", color = PremierUi.Green, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    HorizontalDivider(color = PremierUi.Border.copy(alpha = .55f), thickness = .5.dp)
                }
            }
        }
    }
}

@Composable
private fun PremierLastWordBar(language: String, word: GameWordDto?, meId: String?) {
    val raw = word?.normalizedWord?.ifBlank { word.word }.orEmpty()
    val shown = if (raw.isBlank()) pt(language, "Henüz kelime yok", "No word yet") else premierUpper(raw, language)
    val points = if (raw.isBlank()) 0 else DictionaryEngine.calculatePoints(raw, language)
    val mine = word?.playerId != null && word.playerId == meId
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PremierUi.Surface,
        border = BorderStroke(1.dp, PremierUi.Border),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = if (mine) PremierUi.Ocean else PremierUi.Rival, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(pt(language, "Son Yazılan Kelime:", "Last Word:"), color = PremierUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(shown, modifier = Modifier.weight(1f), color = PremierUi.Ink, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (points > 0) Text("+$points", color = PremierUi.Green, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremierInputBar(
    language: String,
    input: String,
    required: String,
    myTurn: Boolean,
    busy: Boolean,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = myTurn && !busy
    OutlinedTextField(
        value = input,
        onValueChange = { onInput(it.replace("\n", "")) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(19.dp),
        textStyle = LocalTextStyle.current.copy(color = PremierUi.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        placeholder = {
            Text(
                when {
                    busy -> pt(language, "Kontrol ediliyor…", "Checking…")
                    !myTurn -> pt(language, "Rakibin hamlesi bekleniyor…", "Waiting for rival…")
                    required == "★" -> pt(language, "Kelimeyi buraya yazın…", "Type your word here…")
                    else -> pt(language, "$required ile başlayan kelime yazın…", "Type a word starting with $required…")
                },
                color = PremierUi.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingIcon = {
            if (input.isNotEmpty()) {
                IconButton(onClick = { onInput("") }, enabled = enabled) {
                    Icon(Icons.Rounded.Close, pt(language, "Temizle", "Clear"), tint = PremierUi.Muted)
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { if (enabled && input.isNotBlank()) onSubmit() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PremierUi.Surface,
            unfocusedContainerColor = PremierUi.Surface,
            disabledContainerColor = PremierUi.Ice.copy(alpha = .72f),
            focusedBorderColor = PremierUi.Ocean,
            unfocusedBorderColor = PremierUi.Border,
            disabledBorderColor = PremierUi.Border,
            cursorColor = PremierUi.Ocean,
        ),
    )
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
private fun PremierResult(language: String, room: GameRoomDto, meId: String?, busy: Boolean, notice: String, onRematch: () -> Unit, onHome: () -> Unit) {
    val amHost = meId == room.hostId
    val myScore = if (amHost) room.hostScore else room.guestScore
    val rivalScore = if (amHost) room.guestScore else room.hostScore
    val won = when {
        room.isBot -> room.winnerId == meId && !room.winnerIsBot
        else -> room.winnerId == meId
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = if (won) PremierUi.GreenSoft else PremierUi.RedSoft) {
            Icon(if (won) Icons.Rounded.EmojiEvents else Icons.Rounded.SportsEsports, null, tint = if (won) PremierUi.Green else PremierUi.Red, modifier = Modifier.padding(22.dp).size(48.dp))
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
    return when {
        lower.contains("merhaba") || lower.contains("selam") || lower.contains("hello") || lower.contains("hi") ->
            pt(language, "Selam! Güzel bir maç olsun. 🤖", "Hi! Let's have a good match. 🤖")
        lower.contains("rövanş") || lower.contains("rematch") ->
            pt(language, "Maç bitince rövanşa hazırım.", "I'll be ready for a rematch when this ends.")
        lower.contains("tebrik") || lower.contains("bravo") || lower.contains("congrats") ->
            pt(language, "Teşekkürler! Sen de iyi gidiyorsun.", "Thanks! You're doing well too.")
        else -> {
            val replies = if (language == "en") {
                listOf("I'm here. Keep the chain going!", "Good luck on the next word.", "This match is getting interesting.")
            } else {
                listOf("Buradayım. Zinciri sürdür!", "Sıradaki kelimede bol şans.", "Maç giderek kızışıyor.")
            }
            replies[(message.hashCode() and Int.MAX_VALUE) % replies.size]
        }
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
