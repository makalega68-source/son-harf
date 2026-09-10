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

/** Fixed high-legibility gameplay palette from the same calm Son Harf color family. */
private object PremierUi {
    val Background = Color(0xFFF1F5F2)
    val Surface = Color(0xFFFFFDF7)
    val Ink = Color(0xFF26382F)
    val Muted = Color(0xFF65766D)
    val Ocean = Color(0xFF4F725E)
    val OceanDeep = Color(0xFF3F614E)
    val Sky = Color(0xFF4A6E83)
    val Ice = Color(0xFFEAF2EE)
    val Border = Color(0xFFCCD8D1)
    val Green = Color(0xFF4B765D)
    val GreenSoft = Color(0xFFDDE9E1)
    val Red = Color(0xFFA84F59)
    val RedSoft = Color(0xFFF4E4E5)
    val Gold = Color(0xFF8A6538)
    val GoldSoft = Color(0xFFF1E7D3)
}

private fun pt(language: String, tr: String, en: String): String = if (language == "en") en else tr
private fun premierLocale(language: String): Locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
private fun premierUpper(value: String, language: String): String = value.uppercase(premierLocale(language))

internal fun premierRemainingTurnSecondsFromMillis(remainingMillis: Long): Int {
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + 999L) / 1000L).coerceIn(1L, 20L).toInt()
}

internal fun premierRemainingTurnSeconds(deadline: Instant, now: Instant = Instant.now()): Int =
    premierRemainingTurnSecondsFromMillis(Duration.between(now, deadline).toMillis())

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
    var floatingMessage by remember { mutableStateOf<ChatMessageDto?>(null) }
    var moveFeedback by remember { mutableStateOf<PremierMoveFeedback?>(null) }
    var turnSeconds by remember { mutableIntStateOf(20) }

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
        if (previousRoomId != next.id) botChat = emptyList()
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

    LaunchedEffect(turnSeconds, stage) {
        if (stage == PremierStage.Playing && turnSeconds in 1..5) {
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

    LaunchedEffect(room?.id, room?.turnDeadline, room?.currentPlayerId, room?.status, room?.botTurn) {
        val active = room ?: return@LaunchedEffect
        if (active.status !in setOf("playing", "final", "sudden_death") || active.botTurn) {
            turnSeconds = 20
            return@LaunchedEffect
        }

        val deadline = active.turnDeadline?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (deadline == null) {
            turnSeconds = 20
            runCatching { backend.getRoom(active.id) }.getOrNull()?.let { synced ->
                if (synced != active) room = synced
            }
            return@LaunchedEffect
        }

        // Anchor the visible countdown to the database clock rather than the phone's wall
        // clock. A phone that is several seconds fast must still receive a full 20-second turn.
        turnSeconds = 20
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
                        synced.botTurn != active.botTurn
                )
            ) {
                room = synced
                notice = ""
                return@LaunchedEffect
            }

            val advanced = runCatching { backend.claimTurnTimeout(active.id) }.getOrNull()
            if (advanced != null) {
                room = advanced
                notice = pt(language, "Süre doldu. Sıra güncellendi.", "Time expired. Turn updated.")
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
                        floatingMessage = floatingMessage,
                        moveFeedback = moveFeedback,
                        onInput = { input = it },
                        onForfeit = { showForfeit = true },
                        onQuickChat = { showQuickChat = true },
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
                                        repeat(16) {
                                            delay(750)
                                            val next = runCatching { backend.findPremierActiveRoom() }.getOrNull()
                                            if (next != null && next.id != active.id) {
                                                adoptRoom(next, cinematic = true)
                                                return@repeat
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
                    Brush.linearGradient(listOf(PremierUi.OceanDeep, PremierUi.Ocean, PremierUi.Sky))
                ).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = profile?.avatarPath,
                        gender = profile?.gender,
                        name = profile?.displayName ?: pt(language, "Oyuncu", "Player"),
                        size = 58.dp,
                        accent = Color.White,
                        visible = profile?.avatarVisibility != "hidden",
                        showGenderBadge = false,
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
            PremierFeatureTile(Icons.Rounded.Bolt, pt(language, "HIZLI", "FAST"), pt(language, "20 sn tur", "20 sec turn"), Modifier.weight(1f))
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
            colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean, contentColor = Color.White),
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
        PremierVsPlayerCard(language, me?.displayName ?: pt(language, "Oyuncu", "Player"), me?.avatarPath, me?.gender, me?.avatarVisibility != "hidden", me?.rating ?: 1000, profileWinRate(me), PremierUi.Ocean)
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(99.dp), color = Color.Transparent) {
            Box(Modifier.background(Brush.horizontalGradient(listOf(PremierUi.Ocean, PremierUi.OceanDeep))).padding(horizontal = 27.dp, vertical = 10.dp)) {
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
private fun PremierVsPlayerCard(language: String, name: String, avatar: String?, gender: String?, visible: Boolean, rating: Int, winRate: Int, accent: Color, bot: Boolean = false) {
    Surface(modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(23.dp)), shape = RoundedCornerShape(23.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, accent.copy(alpha = .22f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (bot) PremierBotAvatar(size = 70.dp, accent = accent)
            else ProfilePhotoAvatarWithGender(avatar, gender, name, 66.dp, accent, visible, false)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = PremierUi.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    floatingMessage: ChatMessageDto?,
    moveFeedback: PremierMoveFeedback?,
    onInput: (String) -> Unit,
    onForfeit: () -> Unit,
    onQuickChat: () -> Unit,
    onSubmit: () -> Unit,
) {
    val amHost = meId == room.hostId
    val myScore = if (amHost) room.hostScore else room.guestScore
    val rivalScore = if (amHost) room.guestScore else room.hostScore
    val myRounds = if (amHost) room.hostRounds else room.guestRounds
    val rivalRounds = if (amHost) room.guestRounds else room.hostRounds
    val myStreak = if (amHost) room.hostStreak else room.guestStreak
    val rivalStreak = if (amHost) room.guestStreak else room.hostStreak
    val myTurn = room.currentPlayerId == meId && !room.botTurn && room.status in setOf("playing", "final", "sudden_death")
    val rivalName = if (room.isBot) room.botName ?: pt(language, "KelimeBot", "WordBot") else opponent?.displayName ?: pt(language, "Rakip", "Rival")
    val required = premierRequiredToken(room, words)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val veryCompact = maxHeight < 610.dp
        val compact = maxHeight < 700.dp
        val tall = maxHeight > 820.dp
        val targetSize = if (veryCompact) 78.dp else if (compact) 88.dp else if (tall) 118.dp else 104.dp
        val keyHeight = if (veryCompact) 38.dp else if (compact) 41.dp else if (tall) 50.dp else 46.dp
        val primaryGap = if (veryCompact) 3.dp else if (compact) 5.dp else 9.dp

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            PremierArenaHeader(language, room, me, opponent, rivalName, myScore, rivalScore, myRounds, rivalRounds, myStreak, rivalStreak, turnSeconds, onForfeit, onQuickChat)
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(primaryGap))
                PremierTurnBadge(language, myTurn, room.status)
                Spacer(Modifier.height(primaryGap))
                PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)
                Spacer(Modifier.height(primaryGap))
                Text(
                    if (required == "★") pt(language, "İlk kelime serbest", "Free opening word")
                    else pt(language, "“$required” ile başlayan bir kelime yaz", "Enter a word starting with “$required”"),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    color = PremierUi.OceanDeep,
                    fontSize = if (veryCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                if (!veryCompact) {
                    Spacer(Modifier.height(primaryGap))
                    PremierWordTrail(words, language)
                }
                Spacer(Modifier.weight(1f).heightIn(min = 2.dp))
                if (notice.isNotBlank()) {
                    Text(notice, color = PremierUi.OceanDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            // Keep the live input outside the flexible arena body. This guarantees visibility on
            // short screens after the global top banner consumes vertical space.
            PremierInputBar(
                language,
                input,
                required,
                myTurn,
                busy,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
            PremierKeyboard(language, input, enabled = myTurn && !busy, keyHeight = keyHeight, onInput = onInput, onSubmit = onSubmit)
        }

        AnimatedVisibility(visible = floatingMessage != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 132.dp, start = 22.dp, end = 22.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Sky.copy(alpha = .45f)), shadowElevation = 9.dp) {
                Text(floatingMessage?.body.orEmpty(), Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = PremierUi.OceanDeep, fontWeight = FontWeight.Black, fontSize = 13.sp)
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
    onForfeit: () -> Unit,
    onQuickChat: () -> Unit,
) {
    val totalScore = myScore + rivalScore
    val myFraction = if (totalScore <= 0) 0.5f else myScore.toFloat() / totalScore.toFloat()
    val danger = seconds in 1..5
    val timerStart = if (danger) PremierUi.RedSoft else PremierUi.Sky
    val timerEnd = if (danger) PremierUi.Red else PremierUi.OceanDeep

    Surface(shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), color = PremierUi.Surface, shadowElevation = 8.dp, border = BorderStroke(1.dp, PremierUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Surface(modifier = Modifier.clickable(onClick = onForfeit), shape = RoundedCornerShape(12.dp), color = PremierUi.RedSoft) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Flag, null, tint = PremierUi.Red, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(pt(language, "PES ET", "SURRENDER"), color = PremierUi.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(13.dp), color = PremierUi.Ice) {
                        Text("$myScore  —  $rivalScore", Modifier.padding(horizontal = 13.dp, vertical = 6.dp), color = PremierUi.OceanDeep, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Surface(
                        modifier = Modifier.clickable(onClick = onQuickChat),
                        shape = RoundedCornerShape(12.dp),
                        color = PremierUi.Ice,
                        border = BorderStroke(1.dp, PremierUi.Border),
                    ) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ChatBubbleOutline, pt(language, "Sohbet", "Chat"), tint = PremierUi.Ocean, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(pt(language, "SOHBET", "CHAT"), color = PremierUi.OceanDeep, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(PremierUi.RedSoft)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(myFraction.coerceIn(0.04f, 0.96f))
                        .background(Brush.horizontalGradient(listOf(PremierUi.Sky, PremierUi.Ocean)))
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PremierMiniPlayer(me?.displayName ?: pt(language, "Sen", "You"), me?.avatarPath, me?.gender, me?.avatarVisibility != "hidden", myRounds, myStreak, PremierUi.Ocean, false, Modifier.weight(1f))
                Surface(shape = CircleShape, color = Color.Transparent) {
                    Box(Modifier.size(60.dp).background(Brush.radialGradient(listOf(timerStart, if (danger) PremierUi.Red else PremierUi.Ocean, timerEnd)), CircleShape), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(seconds.toString().padStart(2, '0'), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Text("SEC", color = Color.White.copy(alpha = .75f), fontSize = 6.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                PremierMiniPlayer(rivalName, opponent?.avatarPath, opponent?.gender, opponent?.avatarVisibility != "hidden", rivalRounds, rivalStreak, PremierUi.OceanDeep, room.isBot, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PremierMiniPlayer(name: String, avatar: String?, gender: String?, visible: Boolean, rounds: Int, streak: Int, accent: Color, bot: Boolean, modifier: Modifier) {
    val isLeft = accent == PremierUi.Ocean
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isLeft) Arrangement.Start else Arrangement.End,
    ) {
        if (isLeft) {
            ProfilePhotoAvatarWithGender(avatar, gender, name, 58.dp, accent, visible, false)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 68.dp),
            horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End,
        ) {
            Text(name, color = PremierUi.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { i -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (i < rounds) PremierUi.Gold else PremierUi.Border)) }
            }
            if (streak >= 2) Text("🔥 $streak", color = PremierUi.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        if (!isLeft) {
            Spacer(Modifier.width(8.dp))
            if (bot) PremierBotAvatar(size = 58.dp, accent = accent)
            else ProfilePhotoAvatarWithGender(avatar, gender, name, 58.dp, accent, visible, false)
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
private fun PremierTargetCard(language: String, required: String, gameMode: String, round: Int, size: Dp) {
    val transition = rememberInfiniteTransition(label = "letter")
    val glow by transition.animateFloat(.25f, .55f, infiniteRepeatable(tween(1050), RepeatMode.Reverse), label = "glow")
    val targetBadge = when {
        required == "★" -> "—"
        gameMode == "expert" -> "x${round.coerceIn(1, 3)}"
        required.length == 1 -> "${com.sonharf.game.data.DictionaryEngine.getLetterPoint(required.first(), language)}P"
        else -> "x${round.coerceIn(1, 3)}"
    }
    Box(
        Modifier.size(size).shadow(16.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
            .background(Brush.radialGradient(listOf(PremierUi.Sky, PremierUi.Ocean, PremierUi.OceanDeep))),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = glow * .13f)))
        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(7.dp), shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = .20f)) {
            Text(targetBadge, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(required, color = Color.White, fontSize = (size.value * if (required.length > 1) .32f else .42f).sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            Text(if (required == "★") pt(language, "SERBEST", "FREE") else pt(language, "HEDEF", "TARGET"), color = Color.White.copy(alpha = .78f), fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
private fun PremierWordTrail(words: List<GameWordDto>, language: String) {
    if (words.isEmpty()) {
        Text(pt(language, "İlk zinciri sen başlatabilirsin.", "You can start the first chain."), color = PremierUi.Muted, fontSize = 10.sp)
        return
    }
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(words.takeLast(12).reversed(), key = { it.id }) { entry ->
            Surface(shape = RoundedCornerShape(11.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Border)) {
                Text(premierUpper(entry.normalizedWord.ifBlank { entry.word }, language), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = PremierUi.OceanDeep, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PremierInputBar(language: String, input: String, required: String, myTurn: Boolean, busy: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = PremierUi.Surface, border = BorderStroke(2.dp, if (myTurn) PremierUi.Ocean else PremierUi.Border), shadowElevation = if (myTurn) 5.dp else 0.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = if (myTurn) PremierUi.Ocean else PremierUi.Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                when {
                    busy -> pt(language, "Kontrol ediliyor…", "Checking…")
                    input.isNotBlank() -> input
                    required == "★" -> pt(language, "Kelimeyi yaz…", "Type a word…")
                    else -> pt(language, "$required ile başla…", "Start with $required…")
                },
                color = if (input.isBlank()) PremierUi.Muted else PremierUi.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PremierKeyboard(language: String, value: String, enabled: Boolean, keyHeight: Dp, onInput: (String) -> Unit, onSubmit: () -> Unit) {
    val rows = if (language == "en") listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P"),
        listOf("A","S","D","F","G","H","J","K","L"),
        listOf("Z","X","C","V","B","N","M"),
    ) else listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
        listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
        listOf("Z","X","C","V","B","N","M","Ö","Ç"),
    )
    Surface(color = PremierUi.Ice, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), shadowElevation = 10.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(keyHeight),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when { action -> PremierUi.Ocean; alt -> PremierUi.Border; else -> PremierUi.Surface },
            contentColor = when { action -> Color.White; alt -> PremierUi.Ink; else -> PremierUi.Ink },
            disabledContainerColor = PremierUi.Background,
            disabledContentColor = PremierUi.Muted.copy(alpha = .50f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (action) 4.dp else 1.dp),
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
