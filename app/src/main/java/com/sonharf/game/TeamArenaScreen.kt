package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.ceil

@Composable
fun TeamArenaScreen(
    initialRoomId: String? = null,
    onExit: () -> Unit,
) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val me = backend?.currentUserId()

    var roomId by remember(initialRoomId) { mutableStateOf(initialRoomId) }
    var room by remember { mutableStateOf<TeamArenaRoomDto?>(null) }
    var members by remember { mutableStateOf<List<TeamArenaMemberDto>>(emptyList()) }
    var words by remember { mutableStateOf<List<TeamArenaWordDto>>(emptyList()) }
    var friends by remember { mutableStateOf<List<SocialProfileDto>>(emptyList()) }
    var inviteTeam by remember { mutableStateOf<Int?>(null) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    suspend fun reload() {
        val b = backend ?: return
        val id = roomId ?: return
        runCatching {
            val next = b.getTeamArenaRoom(id)
            room = next
            members = b.getTeamArenaMembers(id)
            words = if (next.status in setOf("playing", "finished")) {
                b.getTeamArenaWords(id)
            } else {
                emptyList()
            }
        }.onFailure {
            notice = teamArenaError(it.message.orEmpty())
        }
    }

    LaunchedEffect(initialRoomId) {
        val b = backend
        if (roomId == null && b != null) {
            val active = runCatching { b.getMyActiveTeamArena() }.getOrNull()
            if (active?.active == true && !active.roomId.isNullOrBlank()) {
                roomId = active.roomId
            }
        }
        if (roomId != null) reload()
        loading = false
    }

    LaunchedEffect(roomId) {
        val id = roomId ?: return@LaunchedEffect
        while (roomId == id) {
            reload()
            val state = room?.status
            if (state !in setOf("lobby", "playing")) break
            delay(if (state == "playing") 700L else 1100L)
        }
    }

    LaunchedEffect(room?.isHost, room?.status, members.size) {
        val b = backend
        if (b != null && room?.isHost == true && room?.status == "lobby") {
            friends = runCatching { b.getAcceptedFriendProfiles().map { it.second } }
                .getOrDefault(emptyList())
        }
    }

    LaunchedEffect(room?.status, room?.endsAt) {
        val active = room ?: return@LaunchedEffect
        if (active.status != "playing") return@LaunchedEffect
        val end = active.endsAt?.let(::teamArenaEpochMs) ?: return@LaunchedEffect
        var lastTick = -1
        while (room?.status == "playing") {
            nowMs = System.currentTimeMillis()
            val seconds = ceil((end - nowMs).coerceAtLeast(0L) / 1000.0).toInt()
            if (seconds in 1..10 && seconds != lastTick) {
                SonHarfSoundFx.countdown()
                if (seconds <= 5) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                lastTick = seconds
            }
            if (nowMs >= end) {
                reload()
                break
            }
            delay(150L)
        }
    }

    DisposableEffect(room?.status) {
        if (room?.status == "playing") SonHarfUiState.inMatch = true
        onDispose {
            if (room?.status == "playing") SonHarfUiState.inMatch = false
        }
    }

    fun closeScreen() {
        val current = room
        val id = roomId
        val b = backend
        if (current?.status == "lobby" && id != null && b != null) {
            scope.launch {
                busy = true
                val result = if (current.isHost) {
                    runCatching { b.cancelTeamArenaLobby(id) }
                } else {
                    runCatching { b.leaveTeamArenaLobby(id) }
                }
                result.onFailure { notice = teamArenaError(it.message.orEmpty()) }
                busy = false
                if (result.isSuccess) {
                    roomId = null
                    room = null
                    members = emptyList()
                    words = emptyList()
                    onExit()
                }
            }
        } else {
            onExit()
        }
    }

    BackHandler { closeScreen() }

    val active = room
    val myMember = members.firstOrNull { it.userId == me }
    val myTeam = active?.myTeam ?: myMember?.team ?: 1
    val startMs = active?.startsAt?.let(::teamArenaEpochMs) ?: 0L
    val endMs = active?.endsAt?.let(::teamArenaEpochMs) ?: 0L
    val prepSeconds = if (active?.status == "playing" && nowMs < startMs) {
        ceil((startMs - nowMs) / 1000.0).toInt().coerceAtLeast(1)
    } else 0
    val remainingSeconds = if (active?.status == "playing" && nowMs >= startMs) {
        ceil((endMs - nowMs).coerceAtLeast(0L) / 1000.0).toInt()
    } else 60

    fun createLobby() {
        val b = backend ?: return
        if (busy) return
        scope.launch {
            busy = true
            notice = ""
            runCatching { b.createTeamArena(SonHarfUiState.language) }
                .onSuccess {
                    roomId = it.roomId
                    SonHarfSoundFx.softNotify()
                    reload()
                }
                .onFailure { error ->
                    if ("team_arena_already_active" in error.message.orEmpty()) {
                        val existing = runCatching { b.getMyActiveTeamArena() }.getOrNull()
                        if (existing?.active == true && !existing.roomId.isNullOrBlank()) {
                            roomId = existing.roomId
                            reload()
                        } else {
                            notice = teamArenaError(error.message.orEmpty())
                        }
                    } else {
                        notice = teamArenaError(error.message.orEmpty())
                    }
                }
            busy = false
        }
    }

    fun submitWord() {
        val b = backend ?: return
        val id = roomId ?: return
        val clean = input.trim()
        if (clean.length !in 3..10 || busy || prepSeconds > 0 || remainingSeconds <= 0) return
        scope.launch {
            busy = true
            notice = ""
            runCatching { b.submitTeamArenaWord(id, clean) }
                .onSuccess { result ->
                    if (result.accepted) {
                        input = ""
                        SonHarfSoundFx.wordAccepted()
                        if (result.combo >= 2) {
                            SonHarfSoundFx.bonus()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            notice = sh(
                                "🔥 ${result.combo}× combo • takım +${result.basePoints}",
                                "🔥 ${result.combo}× combo • team +${result.basePoints}",
                            )
                        }
                        reload()
                    } else if (result.status == "finished") {
                        reload()
                    }
                }
                .onFailure {
                    notice = teamArenaError(it.message.orEmpty())
                    SonHarfSoundFx.warning()
                }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, Color(0xFFF7F9FC), Color(0xFFF1F6FC)))
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = ::closeScreen) {
                Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"))
            }
            Column(Modifier.weight(1f)) {
                Text(sh("TAKIM ARENASI", "TEAM ARENA"), fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(
                    sh("2v2 • 4 arkadaş • aynı harfler • 60 saniye", "2v2 • 4 friends • same letters • 60 seconds"),
                    color = SonHarfMuted,
                    fontSize = 9.sp,
                )
            }
            Icon(Icons.Rounded.Groups, null, tint = SonHarfGold, modifier = Modifier.size(27.dp))
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfBlue)

        when {
            active == null -> TeamArenaIntro(busy, notice, ::createLobby)

            active.status == "lobby" -> TeamArenaLobby(
                room = active,
                members = members,
                me = me,
                busy = busy,
                notice = notice,
                onInvite = { inviteTeam = it },
                onReady = {
                    val b = backend
                    val id = roomId
                    if (b != null && id != null) {
                        scope.launch {
                            busy = true
                            runCatching { b.setTeamArenaReady(id, !(myMember?.ready ?: false)) }
                                .onSuccess {
                                    SonHarfSoundFx.softNotify()
                                    reload()
                                }
                                .onFailure { notice = teamArenaError(it.message.orEmpty()) }
                            busy = false
                        }
                    }
                },
                onStart = {
                    val b = backend
                    val id = roomId
                    if (b != null && id != null) {
                        scope.launch {
                            busy = true
                            runCatching { b.startTeamArena(id) }
                                .onSuccess {
                                    nowMs = System.currentTimeMillis()
                                    SonHarfSoundFx.countdown()
                                    reload()
                                }
                                .onFailure { notice = teamArenaError(it.message.orEmpty()) }
                            busy = false
                        }
                    }
                },
                onCancelOrLeave = {
                    val b = backend
                    val id = roomId
                    if (b != null && id != null) {
                        scope.launch {
                            busy = true
                            val result = if (active.isHost) {
                                runCatching { b.cancelTeamArenaLobby(id) }
                            } else {
                                runCatching { b.leaveTeamArenaLobby(id) }
                            }
                            result.onSuccess {
                                roomId = null
                                room = null
                                members = emptyList()
                                words = emptyList()
                                onExit()
                            }.onFailure { notice = teamArenaError(it.message.orEmpty()) }
                            busy = false
                        }
                    }
                },
            )

            active.status == "playing" -> TeamArenaPlaying(
                room = active,
                members = members,
                words = words,
                myTeam = myTeam,
                prepSeconds = prepSeconds,
                remainingSeconds = remainingSeconds,
                input = input,
                busy = busy,
                notice = notice,
                onInput = { input = it.filter(Char::isLetter).take(10).uppercase() },
                onSubmit = ::submitWord,
            )

            active.status == "finished" -> TeamArenaFinished(
                room = active,
                words = words,
                myTeam = myTeam,
                busy = busy,
                notice = notice,
                onRematch = {
                    val b = backend
                    val oldRoomId = roomId
                    if (b != null && oldRoomId != null) {
                        scope.launch {
                            busy = true
                            notice = ""
                            runCatching { b.createTeamArenaRematch(oldRoomId) }
                                .onSuccess { result ->
                                    roomId = result.roomId
                                    room = null
                                    members = emptyList()
                                    words = emptyList()
                                    notice = sh(
                                        "Rövanş lobisi hazır • ${result.invitedCount}/3 davet gönderildi.",
                                        "Rematch lobby ready • ${result.invitedCount}/3 invites sent.",
                                    )
                                    SonHarfSoundFx.softNotify()
                                    reload()
                                }
                                .onFailure { notice = teamArenaError(it.message.orEmpty()) }
                            busy = false
                        }
                    }
                },
                onNewLobby = {
                    roomId = null
                    room = null
                    members = emptyList()
                    words = emptyList()
                    notice = ""
                },
                onHome = onExit,
            )

            else -> TeamArenaIntro(
                busy = busy,
                notice = if (notice.isBlank()) sh("Lobi kapandı.", "Lobby closed.") else notice,
                onCreate = {
                    roomId = null
                    room = null
                    createLobby()
                },
            )
        }
    }

    val selectedTeam = inviteTeam
    if (selectedTeam != null && active?.isHost == true && active.status == "lobby") {
        TeamArenaFriendPicker(
            team = selectedTeam,
            friends = friends,
            memberIds = members.map { it.userId }.toSet(),
            busy = busy,
            onDismiss = { if (!busy) inviteTeam = null },
            onInvite = { friend ->
                val b = backend
                val id = roomId
                if (b != null && id != null) {
                    scope.launch {
                        busy = true
                        runCatching { b.inviteFriendToTeamArena(id, friend.id, selectedTeam) }
                            .onSuccess {
                                notice = sh(
                                    "${friend.displayName} • Takım ${if (selectedTeam == 1) "A" else "B"} daveti gönderildi.",
                                    "${friend.displayName} • Team ${if (selectedTeam == 1) "A" else "B"} invite sent.",
                                )
                                SonHarfSoundFx.softNotify()
                                inviteTeam = null
                            }
                            .onFailure { notice = teamArenaError(it.message.orEmpty()) }
                        busy = false
                    }
                }
            },
        )
    }
}

@Composable
private fun TeamArenaIntro(
    busy: Boolean,
    notice: String,
    onCreate: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .28f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("👥⚡", fontSize = 45.sp)
                    Text(sh("2v2 TAKIM DÜELLOSU", "2v2 TEAM DUEL"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        sh(
                            "Bir lobi kur, üç çevrimiçi arkadaşını iki takıma dağıt. Herkes aynı harflerle 60 saniye yarışır.",
                            "Create a lobby and place three online friends into two teams. Everyone plays the same letters for 60 seconds.",
                        ),
                        color = SonHarfMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                    TeamArenaRule("✓", sh("Takım skoru ortak", "Shared team score"))
                    TeamArenaRule("✓", sh("Takım içinde aynı kelime yalnız 1 kez", "Each word scores once per team"))
                    TeamArenaRule("✓", sh("Rakip kelimeleri finalde açılır", "Opponent words reveal at the finish"))
                    TeamArenaRule("⚖", sh("Rating / lig / Son Coin etkisi yok", "No rating / league / Son Coin impact"))
                    Button(
                        onClick = onCreate,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
                    ) {
                        Icon(Icons.Rounded.Groups, null)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (busy) "…" else sh("TAKIM LOBİSİ OLUŞTUR", "CREATE TEAM LOBBY"),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
        if (notice.isNotBlank()) item { TeamArenaNotice(notice) }
    }
}

@Composable
private fun TeamArenaLobby(
    room: TeamArenaRoomDto,
    members: List<TeamArenaMemberDto>,
    me: String?,
    busy: Boolean,
    notice: String,
    onInvite: (Int) -> Unit,
    onReady: () -> Unit,
    onStart: () -> Unit,
    onCancelOrLeave: () -> Unit,
) {
    val mine = members.firstOrNull { it.userId == me }
    val allReady = room.memberCount == 4L && room.readyCount == 4L

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            TeamArenaNotice(
                sh(
                    "${room.memberCount}/4 oyuncu • ${room.readyCount}/4 hazır • Lobby kabul edilince diğer canlı modlar kilitlenir.",
                    "${room.memberCount}/4 players • ${room.readyCount}/4 ready • Other live modes lock after joining.",
                )
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                TeamArenaTeamCard(
                    title = sh("TAKIM A", "TEAM A"),
                    team = 1,
                    members = members,
                    hostCanInvite = room.isHost,
                    onInvite = { onInvite(1) },
                    modifier = Modifier.weight(1f),
                )
                TeamArenaTeamCard(
                    title = sh("TAKIM B", "TEAM B"),
                    team = 2,
                    members = members,
                    hostCanInvite = room.isHost,
                    onInvite = { onInvite(2) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (!room.isHost && mine != null) {
            item {
                Button(
                    onClick = onReady,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mine.ready) SonHarfGreen else SonHarfBlue
                    ),
                ) {
                    Text(
                        if (mine.ready) sh("✓ HAZIRIM", "✓ READY") else sh("HAZIR OL", "READY UP"),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        if (room.isHost) {
            item {
                Button(
                    onClick = onStart,
                    enabled = allReady && !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            busy -> "…"
                            room.memberCount < 4 -> sh("4 OYUNCUYU TAMAMLA", "FILL 4 PLAYERS")
                            room.readyCount < 4 -> sh("HERKESİ HAZIRLA", "WAIT FOR READY")
                            else -> sh("2v2 MAÇI BAŞLAT", "START 2v2 MATCH")
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        if (notice.isNotBlank()) item { TeamArenaNotice(notice) }

        item {
            OutlinedButton(
                onClick = onCancelOrLeave,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .55f)),
            ) {
                Text(
                    if (room.isHost) sh("LOBİYİ İPTAL ET", "CANCEL LOBBY") else sh("LOBİDEN AYRIL", "LEAVE LOBBY"),
                    color = SonHarfPink,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TeamArenaTeamCard(
    title: String,
    team: Int,
    members: List<TeamArenaMemberDto>,
    hostCanInvite: Boolean,
    onInvite: () -> Unit,
    modifier: Modifier,
) {
    val teamMembers = members.filter { it.team == team }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (team == 1) SonHarfBlue.copy(alpha = .055f) else SonHarfGold.copy(alpha = .065f),
        border = BorderStroke(
            1.dp,
            if (team == 1) SonHarfBlue.copy(alpha = .25f) else SonHarfGold.copy(alpha = .30f),
        ),
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                title,
                color = if (team == 1) SonHarfBlue else SonHarfGold,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
            )
            (1..2).forEach { seat ->
                val member = teamMembers.firstOrNull { it.seat == seat }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SonHarfSurface,
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (member == null) {
                            Text("＋", color = SonHarfMuted, fontSize = 20.sp)
                            Text(sh("Boş", "Empty"), color = SonHarfMuted, fontSize = 8.sp)
                        } else {
                            Text(if (member.isHost) "👑" else if (member.ready) "✓" else "…", fontSize = 16.sp)
                            Text(
                                member.displayName,
                                color = SonHarfText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                            )
                            Text(
                                if (member.ready) sh("Hazır", "Ready") else sh("Bekliyor", "Waiting"),
                                color = if (member.ready) SonHarfGreen else SonHarfMuted,
                                fontSize = 8.sp,
                            )
                        }
                    }
                }
            }
            if (hostCanInvite && teamMembers.size < 2) {
                TextButton(onClick = onInvite) {
                    Text(sh("+ DAVET", "+ INVITE"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun TeamArenaPlaying(
    room: TeamArenaRoomDto,
    members: List<TeamArenaMemberDto>,
    words: List<TeamArenaWordDto>,
    myTeam: Int,
    prepSeconds: Int,
    remainingSeconds: Int,
    input: String,
    busy: Boolean,
    notice: String,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val myScore = if (myTeam == 1) room.teamAScore else room.teamBScore
    val opponentScore = if (myTeam == 1) room.teamBScore else room.teamAScore
    val teammateNames = members.filter { it.team == myTeam }.joinToString(" + ") { it.displayName }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SonHarfSurface,
                border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .24f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        if (myTeam == 1) sh("TAKIM A", "TEAM A") else sh("TAKIM B", "TEAM B"),
                        color = SonHarfBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                    )
                    Text(teammateNames, color = SonHarfMuted, fontSize = 9.sp)
                    Text(
                        if (prepSeconds > 0)
                            sh("$prepSeconds SANİYE SONRA", "STARTS IN $prepSeconds")
                        else
                            sh("$remainingSeconds SANİYE", "$remainingSeconds SECONDS"),
                        color = if (remainingSeconds <= 10) SonHarfPink else SonHarfText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TeamArenaMetric("$myScore", sh("BİZ", "US"), Modifier.weight(1f))
                        TeamArenaMetric("$opponentScore", sh("RAKİP", "RIVALS"), Modifier.weight(1f))
                        TeamArenaMetric("${words.size}", sh("KELİME", "WORDS"), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            ) {
                room.letters.orEmpty().forEach { ch ->
                    Surface(
                        modifier = Modifier.size(29.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = SonHarfBlue.copy(alpha = .10f),
                        border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .25f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(ch.uppercase(), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && prepSeconds == 0 && remainingSeconds > 0,
                singleLine = true,
                label = { Text(sh("3–10 harfli kelime", "3–10 letter word")) },
                trailingIcon = {
                    TextButton(
                        onClick = onSubmit,
                        enabled = input.length in 3..10 && !busy && prepSeconds == 0 && remainingSeconds > 0,
                    ) {
                        Text(sh("GÖNDER", "SEND"), fontWeight = FontWeight.Black)
                    }
                },
            )
        }

        item {
            Text(sh("TAKIM KELİMELERİ", "TEAM WORDS"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(
                sh(
                    "Takım arkadaşınla aynı kelime ikinci kez puan yazmaz. Rakip kelimeleri finalde açılır.",
                    "A teammate cannot score a team word twice. Rival words reveal at the finish.",
                ),
                color = SonHarfMuted,
                fontSize = 8.sp,
            )
        }

        items(words.reversed(), key = { "${it.team}-${it.normalizedWord}-${it.createdAt}" }) {
            TeamArenaWordRow(it)
        }

        if (notice.isNotBlank()) item { TeamArenaNotice(notice) }
    }
}

@Composable
private fun TeamArenaFinished(
    room: TeamArenaRoomDto,
    words: List<TeamArenaWordDto>,
    myTeam: Int,
    busy: Boolean,
    notice: String,
    onRematch: () -> Unit,
    onNewLobby: () -> Unit,
    onHome: () -> Unit,
) {
    val draw = room.winnerTeam == null
    val won = room.winnerTeam == myTeam
    val myScore = if (myTeam == 1) room.teamAScore else room.teamBScore
    val rivalScore = if (myTeam == 1) room.teamBScore else room.teamAScore

    LaunchedEffect(room.roomId) {
        when {
            draw -> SonHarfSoundFx.softNotify()
            won -> SonHarfSoundFx.victory()
            else -> SonHarfSoundFx.defeat()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, if (won) SonHarfGreen.copy(alpha = .35f) else SonHarfGold.copy(alpha = .30f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(if (draw) "🤝" else if (won) "🏆" else "⚔", fontSize = 42.sp)
                    Text(
                        when {
                            draw -> sh("BERABERE", "DRAW")
                            won -> sh("TAKIMIN KAZANDI!", "YOUR TEAM WON!")
                            else -> sh("BU KEZ RAKİP TAKIM", "RIVAL TEAM WINS")
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text("$myScore : $rivalScore", fontSize = 27.sp, fontWeight = FontWeight.Black, color = SonHarfBlue)
                    Text(
                        sh(
                            "Sosyal 2v2 modu rating, lig veya Son Coin değiştirmez.",
                            "Social 2v2 does not change rating, league, or Son Coin.",
                        ),
                        color = SonHarfMuted,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { Text(sh("TAKIM A KELİMELERİ", "TEAM A WORDS"), color = SonHarfBlue, fontWeight = FontWeight.Black, fontSize = 11.sp) }
        items(words.filter { it.team == 1 }, key = { "a-${it.normalizedWord}-${it.createdAt}" }) { TeamArenaWordRow(it) }

        item { Text(sh("TAKIM B KELİMELERİ", "TEAM B WORDS"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp) }
        items(words.filter { it.team == 2 }, key = { "b-${it.normalizedWord}-${it.createdAt}" }) { TeamArenaWordRow(it) }

        if (notice.isNotBlank()) item { TeamArenaNotice(notice) }

        if (room.isHost) {
            item {
                Button(
                    onClick = onRematch,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold),
                ) {
                    Text(
                        if (busy) "…" else sh("AYNI TAKIMLARLA RÖVANŞ", "REMATCH SAME TEAMS"),
                        color = Color(0xFF2A210F),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        } else {
            item {
                Text(
                    sh(
                        "Rövanşı önceki lobi sahibi başlatabilir; davet gelirse doğrudan kabul edebilirsin.",
                        "The previous lobby host can start the rematch; accept the invite when it arrives.",
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
                onClick = onNewLobby,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(sh("FARKLI TAKIM LOBİSİ", "NEW TEAM LOBBY"), fontWeight = FontWeight.Bold)
            }
        }
        item {
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text(sh("ANA SAYFA", "HOME"))
            }
        }
    }
}

@Composable
private fun TeamArenaFriendPicker(
    team: Int,
    friends: List<SocialProfileDto>,
    memberIds: Set<String>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onInvite: (SocialProfileDto) -> Unit,
) {
    val eligible = friends.filter { it.id !in memberIds && it.presenceStatus == "online" }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonHarfSurface,
        title = {
            Text(
                sh(
                    "TAKIM ${if (team == 1) "A" else "B"} • DAVET",
                    "TEAM ${if (team == 1) "A" else "B"} • INVITE",
                ),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (eligible.isEmpty()) {
                    Text(
                        sh("Davet edilebilecek çevrimiçi arkadaş yok.", "No online friend is currently available to invite."),
                        color = SonHarfMuted,
                        fontSize = 12.sp,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(eligible, key = { it.id }) { friend ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { onInvite(friend) },
                                shape = RoundedCornerShape(14.dp),
                                color = SonHarfSurface2,
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    SocialAvatar(friend.avatarPath, friend.gender, friend.displayName, 42.dp, accent = if (friend.isVip) SonHarfGold else SonHarfBlue)
                                    Column(Modifier.weight(1f)) {
                                        Text(friend.displayName, fontWeight = FontWeight.Bold, color = SonHarfText)
                                        Text("● ${sh("Çevrimiçi", "Online")}", color = SonHarfGreen, fontSize = 9.sp)
                                    }
                                    Text(sh("DAVET", "INVITE"), color = SonHarfBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(sh("KAPAT", "CLOSE")) }
        },
    )
}

@Composable
private fun TeamArenaWordRow(word: TeamArenaWordDto) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SonHarfSurface,
        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .10f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(word.word.uppercase(), color = SonHarfText, fontWeight = FontWeight.Bold)
                Text(word.displayName, color = SonHarfMuted, fontSize = 8.sp)
            }
            if (word.combo > 1) Text("${word.combo}× ", color = SonHarfGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("+${word.basePoints}", color = SonHarfBlue, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun TeamArenaMetric(value: String, label: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = SonHarfSurface2) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(label, color = SonHarfMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TeamArenaRule(icon: String, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, Modifier.width(24.dp), color = SonHarfGreen, fontWeight = FontWeight.Black)
        Text(text, color = SonHarfText, fontSize = 11.sp)
    }
}

@Composable
private fun TeamArenaNotice(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = SonHarfGold.copy(alpha = .09f)) {
        Text(
            text,
            Modifier.fillMaxWidth().padding(9.dp),
            color = SonHarfText,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun teamArenaError(raw: String): String = when {
    "team_arena_needs_four_players" in raw ->
        sh("Maç için toplam 4 oyuncu gerekir.", "A total of 4 players is required.")
    "team_arena_not_all_ready" in raw ->
        sh("Tüm oyuncuların hazır olması gerekir.", "All players must be ready.")
    "team_full" in raw ->
        sh("Bu takım dolu veya bekleyen davetleri var.", "This team is full or has pending invites.")
    "friend_offline" in raw ->
        sh("Bu arkadaş şu an çevrimdışı.", "This friend is currently offline.")
    "friend_in_game" in raw || "player_already_in_game" in raw ->
        sh("Oyunculardan biri başka bir maçta.", "One of the players is in another match.")
    "friend_team_arena_active" in raw || "team_arena_already_active" in raw ->
        sh("Oyunculardan biri başka bir Takım Arenası lobisinde.", "One player is in another Team Arena lobby.")
    "team_arena_team_duplicate_word" in raw ->
        sh("Takımın bu kelimeyi zaten kullandı.", "Your team already used this word.")
    "team_arena_letters_mismatch" in raw ->
        sh("Kelime yalnız verilen harflerden oluşmalı.", "Use only the given letters.")
    "team_arena_invalid_word" in raw ->
        sh("Sözlükte geçerli bir kelime değil.", "This is not a valid dictionary word.")
    "team_arena_word_length" in raw ->
        sh("Kelime 3–10 harf olmalı.", "Word must be 3–10 letters.")
    "team_arena_not_started" in raw ->
        sh("Hazırlık geri sayımı henüz bitmedi.", "The ready countdown is not finished yet.")
    "team_arena_rematch_requires_finished" in raw ->
        sh("Rövanş yalnız bitmiş bir 2v2 maçtan başlatılabilir.", "A rematch can only start from a finished 2v2 match.")
    "team_arena_rematch_requires_four_players" in raw ->
        sh("Rövanş için önceki maçta 4 oyuncu bulunmalı.", "The previous match must contain all 4 players.")
    "team_arena_rematch_already_created" in raw ->
        sh("Bu maçın rövanşı zaten oluşturuldu.", "A rematch has already been created for this match.")
    "team_arena_lobby_closed" in raw ->
        sh("Bu lobi kapandı veya süresi doldu.", "This lobby is closed or expired.")
    "team_slot_taken" in raw ->
        sh("Davet edilen koltuk artık dolu.", "The invited seat is no longer available.")
    "not_friends" in raw ->
        sh("Takım Arenası yalnız arkadaşlarla oynanır.", "Team Arena is available to friends only.")
    "blocked_relationship" in raw ->
        sh("Bu oyuncuyla davet işlemi kullanılamıyor.", "Invites are unavailable with this player.")
    else -> sh("Takım Arenası işlemi tamamlanamadı.", "Team Arena action could not be completed.")
}

private fun teamArenaEpochMs(value: String): Long =
    runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
