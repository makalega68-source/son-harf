package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.ceil

@Composable
fun WordArenaScreen(
    initialRoomId: String? = null,
    onExit: () -> Unit,
) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val me = backend?.currentUserId()

    var matchmaking by remember { mutableStateOf("idle") }
    var roomId by remember { mutableStateOf<String?>(null) }
    var room by remember { mutableStateOf<WordArenaRoomDto?>(null) }
    var words by remember { mutableStateOf<List<WordArenaWordDto>>(emptyList()) }
    var myProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var rematchStatus by remember { mutableStateOf("idle") }
    var clockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var botMode by remember { mutableStateOf(false) }

    if (botMode) {
        WordDuelBotScreen(onExit = { botMode = false })
        return
    }

    suspend fun refreshRoom(id: String) {
        val b = backend ?: return
        val next = b.getWordArenaRoom(id)
        room = next
        words = b.getWordArenaWords(id)
        if (myProfile == null && me != null) {
            myProfile = runCatching { b.getProfile(me) }.getOrNull()
        }
        val opponentId = if (next.hostId == me) next.guestId else next.hostId
        if (opponentProfile?.id != opponentId) {
            opponentProfile = runCatching { b.getProfile(opponentId) }.getOrNull()
        }
        matchmaking = if (next.status == "finished") "finished" else "matched"
    }

    suspend fun enterMatchedRoom(id: String) {
        rematchStatus = "idle"
        matchmaking = "matched"
        roomId = id
        room = null
        words = emptyList()
        opponentProfile = null
        input = ""
        refreshRoom(id)
    }

    fun startMatchmaking() {
        val b = backend ?: return
        scope.launch {
            busy = true
            notice = ""
            runCatching { b.joinWordArena(SonHarfUiState.language) }
                .onSuccess { result ->
                    matchmaking = result.status
                    result.roomId?.let { enterMatchedRoom(it) }
                }
                .onFailure { notice = friendlyArenaError(it.message.orEmpty()) }
            busy = false
        }
    }

    fun leaveScreen() {
        val finishedRoom = room?.takeIf { it.status == "finished" }?.id
        scope.launch {
            if (matchmaking == "waiting") runCatching { backend?.cancelWordArena() }
            if (rematchStatus == "waiting" && finishedRoom != null) {
                runCatching { backend?.cancelWordArenaRematch(finishedRoom) }
            }
            onExit()
        }
    }

    LaunchedEffect(initialRoomId) {
        val incomingRoom = initialRoomId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        runCatching { enterMatchedRoom(incomingRoom) }
            .onFailure { notice = friendlyArenaError(it.message.orEmpty()) }
    }

    BackHandler { leaveScreen() }

    DisposableEffect(Unit) {
        onDispose { SonHarfUiState.inMatch = false }
    }

    LaunchedEffect(matchmaking) {
        if (matchmaking != "waiting") return@LaunchedEffect
        val b = backend ?: return@LaunchedEffect
        while (matchmaking == "waiting") {
            delay(800)
            runCatching { b.pollWordArena() }
                .onSuccess { result ->
                    matchmaking = result.status
                    if (result.roomId != null) {
                        roomId = result.roomId
                        refreshRoom(result.roomId)
                    }
                }
                .onFailure { notice = friendlyArenaError(it.message.orEmpty()) }
        }
    }

    LaunchedEffect(roomId) {
        val id = roomId ?: return@LaunchedEffect
        while (true) {
            runCatching { refreshRoom(id) }
                .onFailure { notice = friendlyArenaError(it.message.orEmpty()) }
            if (room?.status == "finished") break
            delay(550)
        }
    }

    LaunchedEffect(rematchStatus, room?.id) {
        val finishedRoom = room?.takeIf { it.status == "finished" }?.id ?: return@LaunchedEffect
        if (rematchStatus != "waiting") return@LaunchedEffect
        val b = backend ?: return@LaunchedEffect

        while (rematchStatus == "waiting") {
            delay(900)
            runCatching { b.pollWordArenaRematch(finishedRoom) }
                .onSuccess { result ->
                    when {
                        result.status == "matched" && !result.roomId.isNullOrBlank() -> {
                            enterMatchedRoom(result.roomId)
                        }
                        result.status == "expired" -> {
                            rematchStatus = "expired"
                            notice = sh("Rövanş isteğinin süresi doldu.", "Rematch request expired.")
                        }
                    }
                }
                .onFailure {
                    rematchStatus = "idle"
                    notice = friendlyArenaError(it.message.orEmpty())
                }
        }
    }

    LaunchedEffect(room?.status) {
        SonHarfUiState.inMatch = room?.status == "playing"
        val r = room ?: return@LaunchedEffect
        if (r.status == "finished") {
            val won = r.winnerId == me
            when {
                r.winnerId == null -> SonHarfSoundFx.softNotify()
                won -> SonHarfSoundFx.victory()
                else -> SonHarfSoundFx.defeat()
            }
            runCatching { words = backend?.getWordArenaWords(r.id).orEmpty() }
        }
    }

    LaunchedEffect(room?.id) {
        if (room == null) return@LaunchedEffect
        while (room != null && room?.status == "playing") {
            clockMs = System.currentTimeMillis()
            delay(200)
        }
    }

    LaunchedEffect(notice) {
        if (notice.isNotBlank()) {
            delay(1800)
            notice = ""
        }
    }

    val active = room
    val startMs = active?.startsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    val endMs = active?.endsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    val preStartSeconds = startMs?.let { ceil(((it - clockMs).coerceAtLeast(0L)) / 1000.0).toInt() } ?: 0
    val secondsLeft = endMs?.let { ceil(((it - clockMs).coerceAtLeast(0L)) / 1000.0).toInt() } ?: 0
    val playingNow = active?.status == "playing" && preStartSeconds <= 0 && secondsLeft > 0

    LaunchedEffect(secondsLeft, playingNow) {
        if (playingNow && secondsLeft in 1..10) {
            SonHarfSoundFx.countdown()
            if (secondsLeft <= 5) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        when {
            active == null && matchmaking == "waiting" -> ArenaWaitingScreen(
                onCancel = {
                    scope.launch {
                        busy = true
                        runCatching { backend?.cancelWordArena() }
                        matchmaking = "idle"
                        busy = false
                    }
                },
                onBack = ::leaveScreen,
                onBot = {
                    scope.launch {
                        runCatching { backend?.cancelWordArena() }
                        matchmaking = "idle"
                        botMode = true
                    }
                },
                busy = busy,
            )

            active == null -> ArenaIntroScreen(
                onBack = ::leaveScreen,
                onPlay = ::startMatchmaking,
                onBot = { botMode = true },
                busy = busy,
                notice = notice,
            )

            active.status == "finished" -> ArenaResultScreen(
                room = active,
                words = words,
                me = me,
                myName = myProfile?.displayName ?: sh("Sen", "You"),
                opponentName = opponentProfile?.displayName ?: sh("Rakip", "Opponent"),
                myAvatarPath = myProfile?.avatarPath,
                myGender = myProfile?.gender,
                myRating = myProfile?.rating,
                opponentAvatarPath = opponentProfile?.avatarPath,
                opponentGender = opponentProfile?.gender,
                opponentRating = opponentProfile?.rating,
                rematchStatus = rematchStatus,
                rematchBusy = busy,
                onRematch = {
                    scope.launch {
                        busy = true
                        notice = ""
                        runCatching { backend?.requestWordArenaRematch(active.id) }
                            .onSuccess { result ->
                                when {
                                    result?.status == "matched" && !result.roomId.isNullOrBlank() -> {
                                        enterMatchedRoom(result.roomId)
                                    }
                                    result?.status == "waiting" -> {
                                        rematchStatus = "waiting"
                                        SonHarfSoundFx.softNotify()
                                    }
                                    else -> {
                                        notice = sh("Rövanş başlatılamadı.", "Rematch could not be started.")
                                    }
                                }
                            }
                            .onFailure { notice = friendlyArenaError(it.message.orEmpty()) }
                        busy = false
                    }
                },
                onNewOpponent = {
                    scope.launch {
                        busy = true
                        if (rematchStatus == "waiting") {
                            runCatching { backend?.cancelWordArenaRematch(active.id) }
                        }
                        rematchStatus = "idle"
                        room = null
                        roomId = null
                        words = emptyList()
                        opponentProfile = null
                        matchmaking = "idle"
                        busy = false
                        startMatchmaking()
                    }
                },
                onExit = ::leaveScreen,
            )

            else -> ArenaPlayScreen(
                room = active,
                words = words,
                me = me,
                myName = myProfile?.displayName ?: sh("Sen", "You"),
                opponentName = opponentProfile?.displayName ?: sh("Rakip", "Opponent"),
                myAvatarPath = myProfile?.avatarPath,
                myGender = myProfile?.gender,
                opponentAvatarPath = opponentProfile?.avatarPath,
                opponentGender = opponentProfile?.gender,
                myRating = myProfile?.rating,
                opponentRating = opponentProfile?.rating,
                preStartSeconds = preStartSeconds,
                secondsLeft = secondsLeft,
                input = input,
                onInput = {
                    val next = it.take(20)
                    if (next.length > input.length) SonHarfSoundFx.typingClick()
                    input = next
                },
                notice = notice,
                busy = busy,
                onSubmit = {
                    val id = active.id
                    val submitted = input.trim()
                    if (submitted.isBlank() || !playingNow || busy) return@ArenaPlayScreen
                    input = ""
                    scope.launch {
                        busy = true
                        runCatching { backend?.submitWordArena(id, submitted) }
                            .onSuccess { result ->
                                if (result?.accepted == true) {
                                    SonHarfSoundFx.wordAccepted()
                                    if (result.combo >= 2) {
                                        SonHarfSoundFx.bonus()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    notice = if (result.combo >= 2)
                                        sh("+${result.basePoints} • ×${result.combo} COMBO", "+${result.basePoints} • ×${result.combo} COMBO")
                                    else
                                        sh("+${result.basePoints} puan", "+${result.basePoints} points")
                                    refreshRoom(id)
                                }
                            }
                            .onFailure {
                                SonHarfSoundFx.warning()
                                notice = friendlyArenaError(it.message.orEmpty())
                            }
                        busy = false
                    }
                },
                onExit = ::leaveScreen,
            )
        }
    }
}

@Composable
private fun ArenaIntroScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onBot: () -> Unit,
    busy: Boolean,
    notice: String,
) {
    LazyColumn(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(SonHarfBg, Color(0xFFF0F5FF), SonHarfBg))
        ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back")) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sh("KELİME DÜELLOSU", "WORD DUEL"), color = SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(sh("60 saniyelik eşzamanlı düello", "60-second simultaneous duel"), color = SonHarfMuted, fontSize = 10.sp)
                }
                Spacer(Modifier.size(48.dp))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .28f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().background(
                        Brush.radialGradient(listOf(SonHarfBlue.copy(alpha = .18f), SonHarfSurface, SonHarfSurface))
                    ).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(Modifier.size(82.dp), shape = CircleShape, color = SonHarfBlue.copy(alpha = .12f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Bolt, null, tint = SonHarfBlue, modifier = Modifier.size(48.dp))
                        }
                    }
                    Text(sh("AYNI HARFLER. AYNI SÜRE.", "SAME LETTERS. SAME TIME."), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Text(
                        sh(
                            "Rakibinle aynı harfleri alırsın. 60 saniyede olabildiğince çok geçerli kelime üret.",
                            "You and your opponent get the same letters. Build as many valid words as possible in 60 seconds.",
                        ),
                        color = SonHarfMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            ArenaRuleCard("2×", sh("BENZERSİZ KELİME", "UNIQUE WORD"), sh("Rakibinin bulamadığı kelime finalde 2× puan.", "A word your opponent misses scores 2× at the end."))
        }
        item {
            ArenaRuleCard("⚡", sh("HIZLI COMBO", "FAST COMBO"), sh("8 saniye içinde art arda kelime gir; combo puanı kazan.", "Submit another word within 8 seconds to build combo points."))
        }
        item {
            ArenaRuleCard("↗", sh("UZUN KELİME", "LONG WORD"), sh("Uzun kelimeler daha fazla temel puan getirir.", "Longer words earn more base points."))
        }

        if (notice.isNotBlank()) {
            item { Text(notice, Modifier.fillMaxWidth(), color = SonHarfPink, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
        }

        item {
            Button(
                onClick = onPlay,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "…" else sh("CANLI RAKİP", "LIVE RIVAL"), fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
        item {
            OutlinedButton(
                onClick = onBot,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, SonHarfBlue.copy(alpha = .45f)),
            ) {
                Text(sh("BOTLA OYNA", "PLAY BOT"), color = SonHarfBlue, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ArenaRuleCard(icon: String, title: String, text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SonHarfSurface,
        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), shape = RoundedCornerShape(15.dp), color = SonHarfBlue.copy(alpha = .10f)) {
                Box(contentAlignment = Alignment.Center) { Text(icon, color = SonHarfBlue, fontWeight = FontWeight.Black, fontSize = 18.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(text, color = SonHarfMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ArenaWaitingScreen(onCancel: () -> Unit, onBack: () -> Unit, onBot: () -> Unit, busy: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = SonHarfBlue, strokeWidth = 5.dp, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(22.dp))
        Text(sh("RAKİP ARANIYOR", "FINDING OPPONENT"), color = SonHarfText, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(sh("Rating seviyene yakın oyuncu aranıyor.", "Looking for a player near your rating."), color = SonHarfMuted, fontSize = 11.sp)
        Spacer(Modifier.height(22.dp))
        OutlinedButton(onClick = onBot, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(sh("BOTLA HEMEN OYNA", "PLAY BOT NOW"), fontWeight = FontWeight.Black)
        }
        TextButton(onClick = onCancel, enabled = !busy) { Text(sh("ARAMAYI İPTAL ET", "CANCEL SEARCH")) }
        TextButton(onClick = onBack) { Text(sh("ANA SAYFAYA DÖN", "BACK HOME")) }
    }
}

@Composable
private fun ArenaPlayScreen(
    room: WordArenaRoomDto,
    words: List<WordArenaWordDto>,
    me: String?,
    myName: String,
    opponentName: String,
    myAvatarPath: String?,
    myGender: String?,
    opponentAvatarPath: String?,
    opponentGender: String?,
    myRating: Int?,
    opponentRating: Int?,
    preStartSeconds: Int,
    secondsLeft: Int,
    input: String,
    onInput: (String) -> Unit,
    notice: String,
    busy: Boolean,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
) {
    val host = room.hostId == me
    val myScore = if (host) room.hostScore else room.guestScore
    val opponentScore = if (host) room.guestScore else room.hostScore
    val myWords = words.filter { it.userId == me }
    val opponentWordsNow = words.filter { it.userId != me }
    val playing = preStartSeconds <= 0 && secondsLeft > 0
    val danger = secondsLeft in 1..10
    val inputFocusRequester = remember { FocusRequester() }
    val softwareKeyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(room.id, room.status, busy, preStartSeconds, secondsLeft) {
        if (room.status == "playing") {
            delay(120)
            runCatching { inputFocusRequester.requestFocus() }
            softwareKeyboard?.show()
        } else {
            softwareKeyboard?.hide()
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Rounded.ArrowBack, sh("Çık", "Exit")) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(sh("KELİME DÜELLOSU", "WORD DUEL"), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(sh("Benzersiz kelime finalde 2×", "Unique words score 2× at finish"), color = SonHarfGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(48.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArenaScoreCard(myName, myScore, true, myAvatarPath, myGender, SonHarfBlue, Modifier.weight(1f))
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = if (danger) SonHarfPink.copy(alpha = .14f) else SonHarfBlue.copy(alpha = .10f),
                border = BorderStroke(2.dp, if (danger) SonHarfPink else SonHarfBlue),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (preStartSeconds > 0) "$preStartSeconds" else "$secondsLeft",
                        color = if (danger) SonHarfPink else SonHarfText,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            ArenaScoreCard(opponentName, opponentScore, false, opponentAvatarPath, opponentGender, SonHarfPink, Modifier.weight(1f))
        }

        CompetitionLeadStrip(
            myScore = myScore,
            opponentScore = opponentScore,
            myAction = if (myWords.isNotEmpty()) sh("Sen ${myWords.size} kelime buldun.", "You found ${myWords.size} words.") else null,
            opponentAction = if (opponentWordsNow.isNotEmpty()) sh("Rakip ${opponentWordsNow.size} kelime buldu.", "Rival found ${opponentWordsNow.size} words.") else null,
        )

        if (preStartSeconds > 0) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SonHarfGold.copy(alpha = .11f),
                border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .35f)),
            ) {
                Text(
                    sh("$preStartSeconds… HAZIR OL!", "$preStartSeconds… GET READY!"),
                    Modifier.fillMaxWidth().padding(12.dp),
                    textAlign = TextAlign.Center,
                    color = SonHarfGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                )
            }
        }

        Text(sh("HARFLER", "LETTERS"), color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(room.letters.toList()) { letter ->
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = SonHarfSurface,
                    border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .28f)),
                    shadowElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(letter.uppercase(), color = SonHarfBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Card(
            Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .13f)),
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sh("KELİMELERİN", "YOUR WORDS"), color = SonHarfText, fontWeight = FontWeight.Black)
                    Text("${myWords.size}", color = SonHarfBlue, fontWeight = FontWeight.Black)
                }
                if (myWords.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(sh("İlk kelimeyi yaz.", "Enter your first word."), color = SonHarfMuted)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(myWords.reversed(), key = { it.id }) { w ->
                            Surface(shape = RoundedCornerShape(12.dp), color = SonHarfBlue.copy(alpha = .07f)) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(w.word.uppercase(), color = SonHarfText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (w.combo >= 2) Text("×${w.combo}", color = SonHarfGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.width(7.dp))
                                    Text("+${w.basePoints}", color = SonHarfBlue, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (notice.isNotBlank()) {
            Text(notice, Modifier.fillMaxWidth(), color = if ("+" in notice) SonHarfGreen else SonHarfPink, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }

        OutlinedTextField(
            value = input,
            onValueChange = { value -> if (playing && !busy) onInput(value) },
            enabled = true,
            modifier = Modifier.fillMaxWidth().focusRequester(inputFocusRequester),
            singleLine = true,
            label = { Text(sh("Kelime", "Word")) },
            placeholder = {
                Text(
                    when {
                        preStartSeconds > 0 -> sh("Başlamayı bekle…", "Wait for start…")
                        !playing -> sh("Süre doldu…", "Time is up…")
                        else -> sh("Harflerden kelime üret…", "Build a word from the letters…")
                    }
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, showKeyboardOnFocus = true),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            trailingIcon = {
                IconButton(onClick = onSubmit, enabled = playing && !busy && input.isNotBlank()) {
                    Text("➤", color = SonHarfBlue, fontSize = 21.sp)
                }
            },
        )
    }
}

@Composable
private fun ArenaScoreCard(
    name: String,
    score: Int,
    mine: Boolean,
    avatarPath: String?,
    gender: String?,
    accent: Color,
    modifier: Modifier,
) {
    Surface(
        modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (mine) SonHarfBlue.copy(alpha = .09f) else SonHarfSurface,
        border = BorderStroke(1.dp, if (mine) SonHarfBlue.copy(alpha = .35f) else SonHarfMuted.copy(alpha = .15f)),
    ) {
        Column(Modifier.padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = 32.dp,
                accent = accent,
            )
            Spacer(Modifier.height(3.dp))
            Text(name, color = SonHarfText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("$score", color = if (mine) SonHarfBlue else SonHarfText, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(if (mine) sh("SEN", "YOU") else sh("RAKİP", "RIVAL"), color = SonHarfMuted, fontSize = 6.sp)
        }
    }
}

@Composable
private fun ArenaResultScreen(
    room: WordArenaRoomDto,
    words: List<WordArenaWordDto>,
    me: String?,
    myName: String,
    opponentName: String,
    myAvatarPath: String?,
    myGender: String?,
    myRating: Int?,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentRating: Int?,
    rematchStatus: String,
    rematchBusy: Boolean,
    onRematch: () -> Unit,
    onNewOpponent: () -> Unit,
    onExit: () -> Unit,
) {
    val host = room.hostId == me
    val myScore = if (host) room.hostScore else room.guestScore
    val opponentScore = if (host) room.guestScore else room.hostScore
    val myWords = words.filter { it.userId == me }
    val opponentId = if (host) room.guestId else room.hostId
    val opponentWords = words.filter { it.userId == opponentId }
    val opponentSet = opponentWords.map { it.normalizedWord }.toSet()
    val mySet = myWords.map { it.normalizedWord }.toSet()
    val won = room.winnerId == me
    val draw = room.winnerId == null

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CompetitionVsCard(
                myName = myName,
                opponentName = opponentName,
                myAvatarPath = myAvatarPath,
                opponentAvatarPath = opponentAvatarPath,
                myGender = myGender,
                opponentGender = opponentGender,
                myRating = myRating,
                opponentRating = opponentRating,
                centerText = "${myScore}–${opponentScore}",
            )
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (draw) "🤝" else if (won) "🏆" else "⚔", fontSize = 48.sp)
                Text(
                    if (draw) sh("BERABERE", "DRAW") else if (won) sh("KAZANDIN!", "YOU WON!") else sh("BU KEZ RAKİP", "RIVAL WINS"),
                    color = if (won) SonHarfGold else SonHarfText,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                )
                Text("$myScore  —  $opponentScore", color = SonHarfBlue, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    if (draw) sh("Beraberlik • rating değişmez", "Draw • rating unchanged") else if (won) sh("Galibiyet ödülü ve rating sonucu işlendi", "Win reward and rating result applied") else sh("Maç ödülü ve rating sonucu işlendi", "Match reward and rating result applied"),
                    color = SonHarfGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item {
            Text(sh("KELİME KARŞILAŞTIRMASI", "WORD COMPARISON"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }

        item {
            ArenaResultWordList(
                title = myName,
                list = myWords,
                otherSet = opponentSet,
            )
        }
        item {
            ArenaResultWordList(
                title = opponentName,
                list = opponentWords,
                otherSet = mySet,
            )
        }

        item {
            Surface(shape = RoundedCornerShape(15.dp), color = SonHarfGold.copy(alpha = .10f)) {
                Text(
                    sh("★ Benzersiz kelimeler final skorunda 2× sayıldı.", "★ Unique words counted 2× in the final score."),
                    Modifier.fillMaxWidth().padding(11.dp),
                    color = SonHarfGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        item {
            Button(
                onClick = onRematch,
                enabled = !rematchBusy && rematchStatus != "waiting",
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    when {
                        rematchBusy -> "…"
                        rematchStatus == "waiting" -> sh("RAKİBİN BEKLENİYOR…", "WAITING FOR RIVAL…")
                        else -> sh("↻ RÖVANŞ", "↻ REMATCH")
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                )
            }
            if (rematchStatus == "waiting") {
                Spacer(Modifier.height(5.dp))
                Text(
                    sh(
                        "Rakibin 2 dakika içinde Rövanş derse aynı oyuncuyla yeni Düello açılır.",
                        "If your rival accepts within 2 minutes, a new Duel opens with the same player.",
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    color = SonHarfMuted,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onNewOpponent,
                enabled = !rematchBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(sh("YENİ RAKİP BUL", "FIND NEW RIVAL"))
            }
        }
        item {
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text(sh("ANA SAYFA", "HOME"))
            }
        }
    }
}

@Composable
private fun ArenaResultWordList(
    title: String,
    list: List<WordArenaWordDto>,
    otherSet: Set<String>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("$title • ${list.size}", color = SonHarfText, fontWeight = FontWeight.Black)
            if (list.isEmpty()) {
                Text(sh("Kelime yok", "No words"), color = SonHarfMuted, fontSize = 10.sp)
            } else {
                list.sortedByDescending { it.basePoints }.forEach { word ->
                    val unique = word.normalizedWord !in otherSet
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(word.word.uppercase(), color = SonHarfText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (unique) {
                            Surface(shape = RoundedCornerShape(8.dp), color = SonHarfGold.copy(alpha = .13f)) {
                                Text("2×", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = SonHarfGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (unique) "+${word.basePoints * 2}" else "+${word.basePoints}",
                            color = if (unique) SonHarfGold else SonHarfBlue,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun friendlyArenaError(raw: String): String = when {
    "team_arena_active" in raw || "team_arena_already_active" in raw ->
        sh("Takım Arenası maçın sürüyor. Önce 2v2 maçı bitir.", "Your Team Arena match is active. Finish the 2v2 match first.")
    "daily_arena_active" in raw ->
        sh("Önce aktif Resmî Koşuyu bitir.", "Finish your active Official Run first.")
    "player_already_in_game" in raw -> sh("Önce aktif Son Harf maçını bitir.", "Finish your active Son Harf match first.")
    "arena_not_started" in raw -> sh("Düello henüz başlamadı.", "The duel has not started yet.")
    "arena_word_length" in raw -> sh("Kelime 3–10 harf olmalı.", "Words must be 3–10 letters.")
    "arena_letters_mismatch" in raw -> sh("Kelime yalnız verilen harflerden oluşmalı.", "Use only the provided letters.")
    "arena_invalid_word" in raw -> sh("Bu kelime sözlükte yok.", "That word is not in the dictionary.")
    "arena_duplicate_word" in raw -> sh("Bu kelimeyi zaten kullandın.", "You already used that word.")
    "word_arena_match_active" in raw -> sh("Önce aktif Kelime Düellosu maçını bitir.", "Finish your active Word Duel match first.")
    "match_not_finished" in raw -> sh("Rövanş için maçın tamamlanması gerekir.", "The match must finish before a rematch.")
    "arena_room_not_found" in raw -> sh("Düello odası bulunamadı.", "Duel room was not found.")
    "blocked_relationship" in raw -> sh("Bu oyuncuyla Düello başlatılamıyor.", "A Duel cannot be started with this player.")
    "unauthorized" in raw || "not_authenticated" in raw -> sh("Oturumunu yenileyip tekrar dene.", "Refresh your session and try again.")
    else -> sh("İşlem tamamlanamadı. Tekrar dene.", "The action could not be completed. Try again.")
}
