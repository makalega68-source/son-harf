package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CompetitionHubScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2, SonHarfBg))
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = SonHarfText)
            }
            Column(Modifier.weight(1f)) {
                Text(sh("REKABET MERKEZİ", "COMPETITION HUB"), color = SonHarfText, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(sh("Kulüp • Haftalık Kupa • Rakipler", "Club • Weekly Cup • Rivals"), color = SonHarfMuted, fontSize = 9.sp)
            }
            Text("⚔", fontSize = 25.sp)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = tab == 0,
                onClick = { tab = 0 },
                leadingIcon = { Icon(Icons.Rounded.Groups, null, Modifier.size(16.dp)) },
                label = { Text(sh("KULÜP", "CLUB"), fontWeight = FontWeight.Black, fontSize = 9.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == 1,
                onClick = { tab = 1 },
                leadingIcon = { Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(16.dp)) },
                label = { Text(sh("KUPA", "CUP"), fontWeight = FontWeight.Black, fontSize = 9.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == 2,
                onClick = { tab = 2 },
                leadingIcon = { Text("⚔", fontSize = 14.sp) },
                label = { Text(sh("RAKİPLER", "RIVALS"), fontWeight = FontWeight.Black, fontSize = 9.sp) },
                modifier = Modifier.weight(1f),
            )
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> ClubCompetitionTab()
                1 -> WeeklyTournamentTab()
                else -> RivalHistoryTab()
            }
        }
    }
}

@Composable
private fun ClubCompetitionTab() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var myClub by remember { mutableStateOf<MyClubDto?>(null) }
    var directory by remember { mutableStateOf<List<ClubDirectoryRowDto>>(emptyList()) }
    var members by remember { mutableStateOf<List<ClubMemberDto>>(emptyList()) }
    var memberProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var messages by remember { mutableStateOf<List<ClubMessageDto>>(emptyList()) }
    var clubMissions by remember { mutableStateOf<List<ClubWeeklyMissionDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var createOpen by remember { mutableStateOf(false) }
    var leaveConfirm by remember { mutableStateOf(false) }
    var messageInput by remember { mutableStateOf("") }

    suspend fun reload() {
        val b = backend ?: return
        loading = true
        runCatching { b.getMyClub() }
            .onSuccess { club ->
                myClub = club
                if (club == null) {
                    members = emptyList()
                    memberProfiles = emptyMap()
                    messages = emptyList()
                    clubMissions = emptyList()
                    directory = runCatching { b.getClubDirectory(50) }.getOrDefault(emptyList())
                } else {
                    directory = emptyList()
                    val nextMembers = runCatching { b.getClubMembers(club.clubId) }.getOrDefault(emptyList())
                    members = nextMembers
                    val nextProfiles = memberProfiles.toMutableMap()
                    for (member in nextMembers) {
                        if (!nextProfiles.containsKey(member.userId)) {
                            nextProfiles[member.userId] = runCatching { b.getProfile(member.userId) }.getOrNull()
                        }
                    }
                    val activeMemberIds = nextMembers.mapTo(mutableSetOf()) { it.userId }
                    memberProfiles = nextProfiles.filterKeys { it in activeMemberIds }
                    messages = runCatching { b.getClubMessages(club.clubId) }.getOrDefault(emptyList())
                    clubMissions = runCatching { b.getClubWeeklyMissions() }.getOrDefault(emptyList())
                }
            }
            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(myClub?.clubId) {
        while (myClub != null) {
            delay(3500)
            val club = myClub ?: break
            val b = backend ?: break
            messages = runCatching { b.getClubMessages(club.clubId) }.getOrDefault(messages)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfBlue) }

        if (notice.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = SonHarfGold.copy(alpha = .12f),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .35f)),
                ) {
                    Text(notice, Modifier.fillMaxWidth().padding(10.dp), color = SonHarfText, fontSize = 11.sp)
                }
            }
        }

        val club = myClub
        if (club == null && !loading) {
            item {
                CompetitionHero(
                    icon = "👥",
                    title = sh("BİR KULÜBE KATIL", "JOIN A CLUB"),
                    subtitle = sh(
                        "En fazla 30 oyuncu. Her PvP galibiyeti kulübüne +10, mağlubiyet +3 haftalık puan kazandırır.",
                        "Up to 30 players. Every PvP win adds +10 and a loss +3 weekly club points.",
                    ),
                )
            }
            item {
                Button(
                    onClick = { createOpen = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
                ) { Text(sh("＋ KULÜP OLUŞTUR", "＋ CREATE CLUB"), fontWeight = FontWeight.Black) }
            }
            item { Text(sh("KULÜP SIRALAMASI", "CLUB RANKING"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 13.sp) }

            items(directory, key = { it.clubId }) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .16f)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(44.dp), shape = RoundedCornerShape(13.dp), color = SonHarfBlue.copy(alpha = .10f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(row.tag.take(3), color = SonHarfBlue, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("[${row.tag}] ${row.name}", color = SonHarfText, fontWeight = FontWeight.Black, maxLines = 1)
                            Text("${row.memberCount}/${row.maxMembers} • ${row.weeklyPoints} ${sh("haftalık puan", "weekly points")}", color = SonHarfMuted, fontSize = 9.sp)
                            if (row.description.isNotBlank()) Text(row.description, color = SonHarfMuted, fontSize = 9.sp, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { backend?.joinClub(row.clubId) }
                                        .onSuccess { notice = sh("Kulübe katıldın.", "You joined the club."); reload() }
                                        .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                    busy = false
                                }
                            },
                            enabled = !busy && row.memberCount < row.maxMembers,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                        ) { Text(sh("KATIL", "JOIN"), fontSize = 9.sp, fontWeight = FontWeight.Black) }
                    }
                }
            }
        } else if (club != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(23.dp),
                    border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .25f)),
                ) {
                    Column(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(listOf(SonHarfBlue.copy(alpha = .13f), SonHarfSurface, SonHarfGold.copy(alpha = .08f)))
                        ).padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("[${club.tag}] ${club.name}", color = SonHarfText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Text(club.description.ifBlank { sh("Kulüp açıklaması yok.", "No club description.") }, color = SonHarfMuted, fontSize = 10.sp)
                            }
                            Text(if (club.role == "owner") "👑" else "🛡", fontSize = 27.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            CompetitionMetric("${club.weeklyPoints}", sh("HAFTALIK PUAN", "WEEKLY POINTS"), Modifier.weight(1f))
                            CompetitionMetric("#${club.weeklyRank}", sh("KULÜP SIRASI", "CLUB RANK"), Modifier.weight(1f))
                            CompetitionMetric("${club.memberCount}/${club.maxMembers}", sh("ÜYE", "MEMBERS"), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Text(sh("TAKIM SANDIĞI", "TEAM CHEST"), color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(
                    sh(
                        "Kulüpçe hedefe ulaş; ödülü almak için kendi katkı barajını da tamamla.",
                        "Reach the club goal together; complete your personal contribution requirement to claim.",
                    ),
                    color = SonHarfMuted,
                    fontSize = 9.sp,
                )
            }

            items(clubMissions, key = { "club-mission-${it.tier}" }) { mission ->
                ClubMissionCard(
                    mission = mission,
                    busy = busy,
                    onClaim = {
                        scope.launch {
                            busy = true
                            runCatching { backend?.claimClubWeeklyMission(mission.tier) }
                                .onSuccess { result ->
                                    if (result?.success == true) {
                                        SonHarfSoundFx.bonus()
                                        notice = sh(
                                            "Takım Sandığı ${mission.tier}: +${result.rewardCoin} Son Coin",
                                            "Team Chest ${mission.tier}: +${result.rewardCoin} Son Coin",
                                        )
                                    } else {
                                        notice = sh("Bu sandığı zaten aldın.", "You already claimed this chest.")
                                    }
                                    reload()
                                }
                                .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                            busy = false
                        }
                    },
                )
            }

            item { Text(sh("ÜYELER & KATKI", "MEMBERS & CONTRIBUTION"), color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black) }
            items(members, key = { it.userId }) { member ->
                Surface(shape = RoundedCornerShape(14.dp), color = SonHarfSurface, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .13f))) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatar(
                            avatarPath = memberProfiles[member.userId]?.avatarPath,
                            name = member.displayName,
                            size = 36.dp,
                            accent = if (member.role == "owner") SonHarfGold else SonHarfBlue,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(member.displayName, color = SonHarfText, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(5.dp))
                                Text(when (member.role) { "owner" -> "👑"; "moderator" -> "🛡"; else -> "" }, fontSize = 11.sp)
                            }
                            Text("${member.leagueName} • ${member.rating} rating", color = SonHarfMuted, fontSize = 9.sp)
                        }
                        Text("+${member.weeklyPoints}", color = SonHarfGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        if (club.role == "owner" && member.role != "owner") {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        runCatching { backend?.transferClubOwner(member.userId) }
                                            .onSuccess { notice = sh("Kulüp sahipliği devredildi.", "Club ownership transferred."); reload() }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                contentPadding = PaddingValues(horizontal = 5.dp),
                            ) { Text("👑", fontSize = 14.sp) }
                        }
                    }
                }
            }

            item { Text(sh("KULÜP SOHBETİ", "CLUB CHAT"), color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black) }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .16f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (messages.isEmpty()) {
                            Text(sh("İlk mesajı sen gönder.", "Send the first message."), Modifier.fillMaxWidth().padding(vertical = 16.dp), color = SonHarfMuted, textAlign = TextAlign.Center)
                        } else {
                            messages.takeLast(18).forEach { msg ->
                                val sender = members.firstOrNull { it.userId == msg.senderId }?.displayName ?: sh("Üye", "Member")
                                Column {
                                    Text(sender, color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    Text(msg.body, color = SonHarfText, fontSize = 12.sp)
                                }
                            }
                        }
                        HorizontalDivider(color = SonHarfMuted.copy(alpha = .12f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it.take(300) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text(sh("Kulübe yaz…", "Message club…")) },
                            )
                            IconButton(
                                onClick = {
                                    val outgoing = messageInput.trim()
                                    if (outgoing.isEmpty()) return@IconButton
                                    scope.launch {
                                        busy = true
                                        runCatching { backend?.sendClubMessage(club.clubId, outgoing) }
                                            .onSuccess {
                                                messageInput = ""
                                                messages = runCatching { backend?.getClubMessages(club.clubId).orEmpty() }.getOrDefault(messages)
                                            }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busy = false
                                    }
                                },
                                enabled = !busy && messageInput.isNotBlank(),
                            ) { Icon(Icons.Rounded.Send, sh("Gönder", "Send"), tint = SonHarfBlue) }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { leaveConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .45f)),
                ) { Text(sh("KULÜPTEN AYRIL", "LEAVE CLUB"), color = SonHarfPink, fontWeight = FontWeight.Bold) }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }

    if (createOpen) {
        CreateClubDialog(
            busy = busy,
            onDismiss = { if (!busy) createOpen = false },
            onCreate = { name, tag, description ->
                scope.launch {
                    busy = true
                    runCatching { backend?.createClub(name, tag, description) }
                        .onSuccess {
                            createOpen = false
                            notice = sh("Kulübün oluşturuldu.", "Your club was created.")
                            reload()
                        }
                        .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                    busy = false
                }
            },
        )
    }

    if (leaveConfirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) leaveConfirm = false },
            title = { Text(sh("Kulüpten ayrıl?", "Leave club?")) },
            text = {
                Text(
                    if (myClub?.role == "owner" && (myClub?.memberCount ?: 0) > 1)
                        sh("Önce sahipliği başka bir üyeye devretmelisin.", "Transfer ownership to another member first.")
                    else sh("Haftalık kulüp katkın geçmişte kalır; tekrar bir kulübe katılabilirsin.", "Your past weekly contribution stays recorded; you can join another club later.")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backend?.leaveClub() }
                                .onSuccess { leaveConfirm = false; notice = sh("Kulüpten ayrıldın.", "You left the club."); reload() }
                                .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                            busy = false
                        }
                    },
                    enabled = !busy && !(myClub?.role == "owner" && (myClub?.memberCount ?: 0) > 1),
                ) { Text(sh("AYRIL", "LEAVE")) }
            },
            dismissButton = { TextButton(onClick = { leaveConfirm = false }, enabled = !busy) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }
}

@Composable
private fun CreateClubDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sh("KULÜP OLUŞTUR", "CREATE CLUB"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = SonHarfGold.copy(alpha = .10f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .35f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            sh("KULÜP KURMA BEDELİ: 1.000 SON COIN", "CLUB CREATION FEE: 1,000 SON COIN"),
                            color = SonHarfGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            sh("Tek seferlik sosyal anti-spam bedeli. Maç gücü veya rating avantajı vermez.", "One-time social anti-spam fee. It gives no match power or rating advantage."),
                            color = SonHarfMuted,
                            fontSize = 8.sp,
                        )
                    }
                }
                Text(sh("En fazla 30 oyuncu.", "Maximum 30 players."), color = SonHarfMuted, fontSize = 9.sp)
                OutlinedTextField(name, { name = it.take(24) }, label = { Text(sh("Kulüp adı", "Club name")) }, singleLine = true)
                OutlinedTextField(tag, { tag = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6) }, label = { Text(sh("Etiket (2–6)", "Tag (2–6)")) }, singleLine = true)
                OutlinedTextField(description, { description = it.take(180) }, label = { Text(sh("Açıklama", "Description")) }, maxLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, tag, description) },
                enabled = !busy && name.trim().length >= 3 && tag.trim().length >= 2,
            ) { Text(if (busy) "…" else sh("1.000 SC İLE OLUŞTUR", "CREATE FOR 1,000 SC")) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(sh("VAZGEÇ", "CANCEL")) } },
    )
}

@Composable
private fun WeeklyTournamentTab() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var tournament by remember { mutableStateOf<WeeklyTournamentDto?>(null) }
    var leaderboard by remember { mutableStateOf<List<WeeklyTournamentLeaderboardRowDto>>(emptyList()) }
    var leaderboardProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var history by remember { mutableStateOf<List<WeeklyTournamentHistoryDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload() {
        val b = backend ?: return
        loading = true
        runCatching {
            tournament = b.getWeeklyTournament()
            val nextLeaderboard = b.getWeeklyTournamentLeaderboard(50)
            leaderboard = nextLeaderboard
            val nextProfiles = leaderboardProfiles.toMutableMap()
            for (row in nextLeaderboard) {
                if (!nextProfiles.containsKey(row.userId)) {
                    nextProfiles[row.userId] = runCatching { b.getProfile(row.userId) }.getOrNull()
                }
            }
            val activeLeaderboardIds = nextLeaderboard.mapTo(mutableSetOf()) { it.userId }
            leaderboardProfiles = nextProfiles.filterKeys { it in activeLeaderboardIds }
            history = b.getWeeklyTournamentHistory(12)
        }.onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfGold) }

        val t = tournament
        if (t != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .42f)),
                ) {
                    Column(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(listOf(SonHarfGold.copy(alpha = .15f), SonHarfSurface, SonHarfBlue.copy(alpha = .09f)))
                        ).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🏆", fontSize = 42.sp)
                        Text(t.name.uppercase(), color = SonHarfText, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text(
                            sh("Katılım ücretsiz • PvP galibiyet +3 • mağlubiyet +1", "Free entry • PvP win +3 • loss +1"),
                            color = SonHarfGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text("${t.weekStart} • ${t.playerCount} ${sh("oyuncu", "players")}", color = SonHarfMuted, fontSize = 9.sp)
                        if (!t.joined) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        runCatching { backend?.joinWeeklyTournament() }
                                            .onSuccess {
                                                notice = sh(
                                                    "Haftalık Kupaya katıldın. Bundan sonraki PvP maçların puan kazandırır.",
                                                    "You joined the Weekly Cup. Your next PvP matches earn points.",
                                                )
                                                reload()
                                            }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF2A210F)),
                            ) { Text(sh("ÜCRETSİZ KATIL", "JOIN FREE"), fontWeight = FontWeight.Black) }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                CompetitionMetric("${t.myPoints}", sh("PUAN", "POINTS"), Modifier.weight(1f))
                                CompetitionMetric("#${if (t.myRank == 0L) "—" else t.myRank}", sh("SIRAN", "RANK"), Modifier.weight(1f))
                                CompetitionMetric("${t.myWins}-${t.myLosses}", "W-L", Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Surface(shape = RoundedCornerShape(14.dp), color = SonHarfSurface, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sh("KUPA ÖDÜLLERİ", "CUP REWARDS"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("🥇 1.000 SC   •   🥈 600 SC   •   🥉 400 SC", color = SonHarfText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            sh(
                                "4–10: 150 SC • En az 1 maç oynayan diğer oyuncular: 50 SC",
                                "4–10: 150 SC • Other players with at least 1 match: 50 SC",
                            ),
                            color = SonHarfMuted,
                            fontSize = 9.sp,
                        )
                        Text(
                            sh(
                                "Maç oynamadan sıralama ve ödül kazanılmaz.",
                                "No ranking or reward is earned without playing a match.",
                            ),
                            color = SonHarfMuted,
                            fontSize = 8.sp,
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backend?.claimPreviousWeeklyTournamentReward() }
                                .onSuccess { reward ->
                                    if (reward != null) {
                                        notice = sh(
                                            "Ödül: #${reward.rank} • +${reward.rewardCoins} Son Coin",
                                            "Reward: #${reward.rank} • +${reward.rewardCoins} Son Coin",
                                        )
                                        reload()
                                    }
                                }
                                .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                            busy = false
                        }
                    },
                    enabled = !busy && history.any { it.rewardEligible },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .48f)),
                ) {
                    Text(
                        if (history.any { it.rewardEligible })
                            sh("KUPA ÖDÜLÜNÜ AL", "CLAIM CUP REWARD")
                        else
                            sh("ALINABİLİR ÖDÜL YOK", "NO REWARD TO CLAIM"),
                        color = SonHarfGold,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        if (notice.isNotBlank()) {
            item {
                Surface(shape = RoundedCornerShape(13.dp), color = SonHarfBlue.copy(alpha = .08f)) {
                    Text(notice, Modifier.fillMaxWidth().padding(10.dp), color = SonHarfText, fontSize = 10.sp)
                }
            }
        }

        item { Text(sh("CANLI SIRALAMA", "LIVE RANKING"), color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black) }
        items(leaderboard, key = { it.userId }) { row ->
            Surface(shape = RoundedCornerShape(14.dp), color = SonHarfSurface, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .13f))) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (row.rank) { 1L -> "🥇"; 2L -> "🥈"; 3L -> "🥉"; else -> "#${row.rank}" },
                        Modifier.width(42.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black,
                    )
                    ProfilePhotoAvatar(
                        avatarPath = leaderboardProfiles[row.userId]?.avatarPath,
                        name = row.displayName,
                        size = 34.dp,
                        accent = SonHarfGold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.displayName, color = SonHarfText, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${row.leagueName} • ${row.rating} rating • ${row.wins}W/${row.losses}L", color = SonHarfMuted, fontSize = 9.sp)
                    }
                    Text("${row.points} pt", color = SonHarfBlue, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(sh("KUPA GEÇMİŞİM", "MY CUP HISTORY"), color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }

        if (history.isEmpty()) {
            item {
                Text(
                    sh("Henüz tamamlanmış kupa geçmişin yok.", "You do not have completed cup history yet."),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    color = SonHarfMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(history, key = { it.tournamentId }) { h ->
                val played = h.matches > 0
                val rankText = if (h.finalRank > 0) "#${h.finalRank}" else "—"
                Card(
                    colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            h.rewardEligible -> SonHarfGold.copy(alpha = .45f)
                            h.rewardClaimed -> SonHarfGreen.copy(alpha = .28f)
                            else -> SonHarfMuted.copy(alpha = .13f)
                        },
                    ),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (h.finalRank) {
                                    1L -> "🥇"
                                    2L -> "🥈"
                                    3L -> "🥉"
                                    else -> "🏆"
                                },
                                fontSize = 22.sp,
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.name, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text(
                                    "${h.weekStart} • ${h.participantCount} ${sh("aktif oyuncu", "active players")}",
                                    color = SonHarfMuted,
                                    fontSize = 8.sp,
                                )
                            }
                            Text(rankText, color = if (played) SonHarfGold else SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            CompetitionMetric("${h.points}", sh("PUAN", "POINTS"), Modifier.weight(1f))
                            CompetitionMetric("${h.wins}-${h.losses}", "W-L", Modifier.weight(1f))
                            CompetitionMetric("${h.matches}", sh("MAÇ", "MATCHES"), Modifier.weight(1f))
                        }

                        Text(
                            when {
                                !played -> sh(
                                    "Maç oynamadığın için sıralama ve ödül oluşmadı.",
                                    "No ranking or reward because no match was played.",
                                )
                                h.rewardClaimed -> sh(
                                    "✓ +${h.rewardCoins} Son Coin alındı",
                                    "✓ +${h.rewardCoins} Son Coin claimed",
                                )
                                h.rewardEligible -> sh(
                                    "+${h.rewardCoins} Son Coin alınabilir",
                                    "+${h.rewardCoins} Son Coin available",
                                )
                                else -> sh("Ödül durumu kapalı.", "Reward unavailable.")
                            },
                            color = when {
                                h.rewardEligible -> SonHarfGold
                                h.rewardClaimed -> SonHarfGreen
                                else -> SonHarfMuted
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun RivalHistoryTab() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var rivals by remember { mutableStateOf<List<RivalHistoryDto>>(emptyList()) }
    var matchHistory by remember { mutableStateOf<List<MatchHistoryDto>>(emptyList()) }
    var playerProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var busyOpponent by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload(showLoading: Boolean = true) {
        val b = backend ?: return
        if (showLoading) loading = true
        runCatching {
            val nextRivals = b.getRivalHistory(30)
            val nextMatches = b.getMatchHistory(30)
            rivals = nextRivals
            matchHistory = nextMatches
            val nextProfiles = playerProfiles.toMutableMap()
            val activeIds = (nextRivals.map { it.opponentId } + nextMatches.map { it.opponentId }).toSet()
            for (userId in activeIds) {
                if (!nextProfiles.containsKey(userId)) {
                    nextProfiles[userId] = runCatching { b.getProfile(userId) }.getOrNull()
                }
            }
            playerProfiles = nextProfiles.filterKeys { it in activeIds }
        }.onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
        if (showLoading) loading = false
    }

    LaunchedEffect(Unit) {
        reload()
        while (true) {
            delay(12_000)
            reload(showLoading = false)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfBlue) }
        }

        item {
            CompetitionHero(
                icon = "⚔",
                title = sh("RÖVANŞ HATTI", "REMATCH LINE"),
                subtitle = sh(
                    "Son Harf ve Kelime Arenası rakiplerin tek geçmişte. Arkadaşın çevrimiçiyse doğrudan yeniden meydan oku.",
                    "Classic Son Harf and Word Arena rivals in one history. Challenge online friends again instantly.",
                ),
            )
        }

        if (notice.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = SonHarfGold.copy(alpha = .10f),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .28f)),
                ) {
                    Text(
                        notice,
                        Modifier.fillMaxWidth().padding(9.dp),
                        color = SonHarfText,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (!loading && rivals.isEmpty()) {
            item {
                Text(
                    sh(
                        "Henüz gerçek PvP rakip geçmişin yok.",
                        "You do not have real PvP rival history yet.",
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    color = SonHarfMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        items(rivals, key = { it.opponentId }) { rival ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (rival.canChallenge) SonHarfBlue.copy(alpha = .30f) else SonHarfMuted.copy(alpha = .13f),
                ),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatar(
                            avatarPath = playerProfiles[rival.opponentId]?.avatarPath,
                            name = rival.displayName,
                            size = 42.dp,
                            accent = if (rival.canChallenge) SonHarfBlue else SonHarfMuted,
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rival.displayName, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text(
                                "${rival.matches} ${sh("maç", "matches")} • ${rival.wins}W/${rival.losses}L" +
                                    if (rival.draws > 0) "/${rival.draws}D" else "",
                                color = SonHarfMuted,
                                fontSize = 9.sp,
                            )
                            Text(
                                sh(
                                    "Son mod: ${if (rival.lastMode == "arena") "Kelime Arenası" else "Son Harf"}",
                                    "Last mode: ${if (rival.lastMode == "arena") "Word Arena" else "Son Harf"}",
                                ),
                                color = SonHarfMuted,
                                fontSize = 8.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${rival.myPoints}:${rival.theirPoints}",
                                color = SonHarfText,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                            )
                            Text(
                                if (rival.presenceStatus == "online") "● ${sh("Çevrimiçi", "Online")}" else sh("Çevrimdışı", "Offline"),
                                color = if (rival.presenceStatus == "online") SonHarfGreen else SonHarfMuted,
                                fontSize = 8.sp,
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Surface(
                            Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = SonHarfSurface2,
                        ) {
                            Text(
                                "⚔ ${rival.classicMatches} Son Harf",
                                Modifier.padding(vertical = 7.dp),
                                color = SonHarfMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Surface(
                            Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = SonHarfSurface2,
                        ) {
                            Text(
                                "⚡ ${rival.arenaMatches} Arena",
                                Modifier.padding(vertical = 7.dp),
                                color = SonHarfMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (rival.canChallenge) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        busyOpponent = rival.opponentId
                                        runCatching { backend?.inviteFriend(rival.opponentId, SonHarfUiState.language) }
                                            .onSuccess {
                                                notice = sh(
                                                    "${rival.displayName}: Son Harf daveti gönderildi.",
                                                    "${rival.displayName}: Son Harf invite sent.",
                                                )
                                                SonHarfSoundFx.softNotify()
                                            }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busyOpponent = null
                                    }
                                },
                                enabled = busyOpponent == null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
                            ) {
                                Text("⚔ SON HARF", fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        busyOpponent = rival.opponentId
                                        runCatching { backend?.inviteFriendToWordArena(rival.opponentId, SonHarfUiState.language) }
                                            .onSuccess {
                                                notice = sh(
                                                    "${rival.displayName}: Arena daveti gönderildi.",
                                                    "${rival.displayName}: Arena invite sent.",
                                                )
                                                SonHarfSoundFx.softNotify()
                                            }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busyOpponent = null
                                    }
                                },
                                enabled = busyOpponent == null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .55f)),
                            ) {
                                Text("⚡ ARENA", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                        }
                    } else {
                        Text(
                            when {
                                !rival.isFriend -> sh(
                                    "Canlı meydan okuma için önce arkadaş olmalısınız.",
                                    "Become friends first to send a live challenge.",
                                )
                                rival.presenceStatus != "online" -> sh(
                                    "Arkadaşın çevrimiçi olduğunda meydan okuyabilirsin.",
                                    "You can challenge this friend when they are online.",
                                )
                                else -> sh(
                                    "Bu rakibe şu anda meydan okunamıyor.",
                                    "This rival cannot be challenged right now.",
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = SonHarfMuted,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(sh("SON MAÇLAR", "RECENT MATCHES"), color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }

        if (matchHistory.isEmpty()) {
            item {
                Text(
                    sh("Henüz tamamlanmış gerçek PvP maçın yok.", "You do not have completed real PvP matches yet."),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    color = SonHarfMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(matchHistory, key = { "${it.mode}-${it.matchId}" }) { match ->
                val resultColor = when (match.result) {
                    "win" -> SonHarfGreen
                    "loss" -> SonHarfPink
                    else -> SonHarfGold
                }
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = SonHarfSurface,
                    border = BorderStroke(1.dp, resultColor.copy(alpha = .22f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfilePhotoAvatar(
                            avatarPath = playerProfiles[match.opponentId]?.avatarPath,
                            name = match.displayName,
                            size = 38.dp,
                            accent = resultColor,
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(match.displayName, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                "${match.playedAt.take(10)} • ${match.language.uppercase()} • " +
                                    if (match.mode == "arena") sh("Arena", "Arena") else "Son Harf",
                                color = SonHarfMuted,
                                fontSize = 8.sp,
                            )
                            Text(
                                when (match.result) {
                                    "win" -> sh("GALİBİYET", "WIN")
                                    "loss" -> sh("MAĞLUBİYET", "LOSS")
                                    else -> sh("BERABERE", "DRAW")
                                },
                                color = resultColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${match.myScore}:${match.theirScore}",
                                color = SonHarfText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                (if (match.ratingDelta > 0) "+" else "") + match.ratingDelta + " rating",
                                color = if (match.ratingDelta >= 0) SonHarfGreen else SonHarfPink,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun CompetitionHero(icon: String, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .18f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(52.dp), shape = CircleShape, color = SonHarfBlue.copy(alpha = .10f)) {
                Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 25.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = SonHarfText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = SonHarfMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun ClubMissionCard(
    mission: ClubWeeklyMissionDto,
    busy: Boolean,
    onClaim: () -> Unit,
) {
    val clubProgress = (mission.clubPoints.toFloat() / mission.targetPoints.coerceAtLeast(1)).coerceIn(0f, 1f)
    val myProgress = (mission.myPoints.toFloat() / mission.minContribution.coerceAtLeast(1)).coerceIn(0f, 1f)
    val teamReady = mission.clubPoints >= mission.targetPoints
    val meReady = mission.myPoints >= mission.minContribution

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (mission.claimed) SonHarfGreen.copy(alpha = .08f) else SonHarfSurface
        ),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            1.dp,
            when {
                mission.claimed -> SonHarfGreen.copy(alpha = .42f)
                mission.eligible -> SonHarfGold.copy(alpha = .48f)
                else -> SonHarfMuted.copy(alpha = .13f)
            },
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = if (mission.claimed) SonHarfGreen.copy(alpha = .13f) else SonHarfGold.copy(alpha = .11f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            when (mission.tier) { 1 -> "📦"; 2 -> "🎁"; else -> "🏆" },
                            fontSize = 21.sp,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("SANDIK ${mission.tier}", "CHEST ${mission.tier}"),
                        color = SonHarfText,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                    )
                    Text(
                        sh(
                            "Takım ${mission.targetPoints} • Sen ${mission.minContribution} katkı",
                            "Club ${mission.targetPoints} • You ${mission.minContribution} contribution",
                        ),
                        color = SonHarfMuted,
                        fontSize = 9.sp,
                    )
                }
                Text("+${mission.rewardCoin} SC", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            Text(
                sh(
                    "Kulüp: ${mission.clubPoints.coerceAtMost(mission.targetPoints.toLong())}/${mission.targetPoints}",
                    "Club: ${mission.clubPoints.coerceAtMost(mission.targetPoints.toLong())}/${mission.targetPoints}",
                ),
                color = if (teamReady) SonHarfGreen else SonHarfMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { clubProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (teamReady) SonHarfGreen else SonHarfBlue,
                trackColor = SonHarfMuted.copy(alpha = .13f),
            )

            Text(
                sh(
                    "Katkın: ${mission.myPoints.coerceAtMost(mission.minContribution.toLong())}/${mission.minContribution}",
                    "Your contribution: ${mission.myPoints.coerceAtMost(mission.minContribution.toLong())}/${mission.minContribution}",
                ),
                color = if (meReady) SonHarfGreen else SonHarfMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { myProgress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = if (meReady) SonHarfGreen else SonHarfGold,
                trackColor = SonHarfMuted.copy(alpha = .13f),
            )

            Button(
                onClick = onClaim,
                enabled = mission.eligible && !mission.claimed && !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mission.claimed) SonHarfGreen else SonHarfGold,
                    contentColor = Color(0xFF2A210F),
                    disabledContainerColor = if (mission.claimed) SonHarfGreen.copy(alpha = .18f) else SonHarfMuted.copy(alpha = .10f),
                    disabledContentColor = if (mission.claimed) SonHarfGreen else SonHarfMuted,
                ),
            ) {
                Text(
                    when {
                        mission.claimed -> sh("✓ ALINDI", "✓ CLAIMED")
                        !teamReady -> sh("TAKIM HEDEFİ BEKLENİYOR", "WAITING FOR CLUB GOAL")
                        !meReady -> sh("KATKI GEREKİYOR", "CONTRIBUTION REQUIRED")
                        else -> sh("SANDIĞI AÇ", "OPEN CHEST")
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun CompetitionMetric(value: String, label: String, modifier: Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(13.dp),
        color = SonHarfSurface.copy(alpha = .90f),
        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .13f)),
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
            Text(label, color = SonHarfMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun friendlyCompetitionError(raw: String): String = when {
    "insufficient_club_creation_balance" in raw ->
        sh("Kulüp kurmak için 1.000 Son Coin gerekir.", "You need 1,000 Son Coin to create a club.")
    "club_name_or_tag_taken" in raw -> sh("Bu kulüp adı veya etiketi kullanılıyor.", "That club name or tag is already used.")
    "already_in_club" in raw -> sh("Zaten bir kulüptesin.", "You are already in a club.")
    "club_full" in raw -> sh("Kulüp dolu.", "The club is full.")
    "owner_required" in raw -> sh("Bu işlem için kulüp sahibi olmalısın.", "Club owner permission is required.")
    "transfer_owner_before_leaving" in raw -> sh("Ayrılmadan önce kulüp sahipliğini devret.", "Transfer club ownership before leaving.")
    "reward_already_claimed" in raw -> sh("Bu kupa ödülünü zaten aldın.", "You already claimed this cup reward.")
    "no_completed_tournament" in raw || "no_eligible_tournament_reward" in raw ->
        sh("En az 1 maç oynadığın alınabilir geçmiş kupa ödülü yok.", "There is no claimable past cup reward with at least 1 played match.")
    "club_required" in raw -> sh("Takım Sandığı için önce bir kulübe katıl.", "Join a club before using Team Chest.")
    "club_mission_locked" in raw -> sh("Kulüp hedefi henüz tamamlanmadı.", "The club goal is not complete yet.")
    "club_contribution_required" in raw -> sh("Bu sandık için kişisel katkı barajını tamamla.", "Complete your personal contribution requirement for this chest.")
    "unauthorized" in raw || "not_authenticated" in raw -> sh("Oturumunu yenileyip tekrar dene.", "Refresh your session and try again.")
    else -> sh("İşlem tamamlanamadı. Tekrar dene.", "The action could not be completed. Try again.")
}
