package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun MainSocialScreen(
    backend: OnlineGameBackend,
    onPlay: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var friendships by remember { mutableStateOf<List<FriendshipDto>>(emptyList()) }
    var requests by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var inviteProfiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var rivals by remember { mutableStateOf<List<RivalHistoryDto>>(emptyList()) }
    var matchHistory by remember { mutableStateOf<List<MatchHistoryDto>>(emptyList()) }
    var archRival by remember { mutableStateOf<ArchRivalDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }

    suspend fun reload() = coroutineScope {
        loading = true
        val friendTask = async { runCatching { backend.getFriends() }.getOrDefault(emptyList()) }
        val friendshipTask = async { runCatching { backend.getFriendships() }.getOrDefault(emptyList()) }
        val requestTask = async { runCatching { backend.getIncomingFriendRequests() }.getOrDefault(emptyList()) }
        val inviteTask = async { runCatching { backend.getIncomingGameInvites() }.getOrDefault(emptyList()) }
        val rivalTask = async { runCatching { backend.getRivalHistory(30) }.getOrDefault(emptyList()) }
        val historyTask = async { runCatching { backend.getMatchHistory(30) }.getOrDefault(emptyList()) }
        val archTask = async { runCatching { backend.getArchRival() }.getOrNull() }
        friends = friendTask.await()
        friendships = friendshipTask.await()
        requests = requestTask.await()
        invites = inviteTask.await()
        rivals = rivalTask.await()
        matchHistory = historyTask.await()
        archRival = archTask.await()
        val senders = linkedMapOf<String, ProfileDto>()
        invites.map { it.senderId }.distinct().forEach { id ->
            runCatching { backend.getProfile(id) }.getOrNull()?.let { senders[id] = it }
        }
        inviteProfiles = senders
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val onlineCount = friends.count { it.second.presenceStatus == "online" }
    val incomingCount = requests.size + invites.size
    val me = backend.currentUserId()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Sosyal", "Social"),
                subtitle = sh("Arkadaşların, davetlerin ve ezeli rakiplerin", "Friends, invitations and rivals"),
            )
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainMetricCard(friends.size.toString(), sh("Arkadaş", "Friends"), Modifier.weight(1f))
                MainMetricCard(onlineCount.toString(), sh("Çevrimiçi", "Online"), Modifier.weight(1f))
                MainMetricCard(incomingCount.toString(), sh("Yeni istek", "New requests"), Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                ) {
                    Icon(Icons.Rounded.SportsEsports, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sh("MAÇA DAVET", "GAME INVITE"), fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .55f)),
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = MainUi.Gold, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sh("ÖZEL ODA", "PRIVATE ROOM"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }

        item {
            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
            ) {
                listOf(
                    sh("ARKADAŞLAR", "FRIENDS"),
                    sh("İSTEKLER", "REQUESTS"),
                    sh("RAKİPLER", "RIVALS"),
                ).forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label, color = if (tab == index) MainUi.Blue else MainUi.Muted, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                if (index == 1 && incomingCount > 0) {
                                    Spacer(Modifier.width(5.dp))
                                    Surface(shape = CircleShape, color = MainUi.Red) {
                                        Text(incomingCount.toString(), Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }

        when (tab) {
            0 -> {
                if (friends.isEmpty() && !loading) {
                    item {
                        MainSocialEmpty(
                            icon = Icons.Rounded.GroupAdd,
                            title = sh("Henüz arkadaşın yok", "No friends yet"),
                            body = sh("İstekler sekmesinden oyuncu adıyla arama yapabilirsin.", "Search by player name in the Requests tab."),
                            action = sh("OYUNCU BUL", "FIND PLAYERS"),
                        ) { tab = 1 }
                    }
                }

                items(friends, key = { it.second.id }) { (_, friend) ->
                    MainFriendCard(
                        friend = friend,
                        busy = busyKey == friend.id,
                        onInvite = {
                            if (busyKey != null) return@MainFriendCard
                            scope.launch {
                                busyKey = friend.id
                                runCatching { backend.inviteFriend(friend.id, SonHarfUiState.language) }
                                    .onSuccess {
                                        notice = if (friend.presenceStatus == "online") sh("${friend.displayName} davet edildi.", "${friend.displayName} was invited.")
                                        else sh("Davet çevrimdışı arkadaşına iletilecek.", "The invite will reach your offline friend.")
                                        SonHarfSoundFx.softNotify()
                                    }
                                    .onFailure { notice = sh("Davet gönderilemedi.", "Invite could not be sent.") }
                                busyKey = null
                            }
                        },
                        onRemove = {
                            if (busyKey != null) return@MainFriendCard
                            scope.launch {
                                busyKey = friend.id
                                runCatching { backend.removeFriend(friend.id) }
                                    .onSuccess { notice = sh("Arkadaş listesi güncellendi.", "Friend list updated."); reload() }
                                    .onFailure { notice = sh("Arkadaş kaldırılamadı.", "Friend could not be removed.") }
                                busyKey = null
                            }
                        },
                    )
                }

                if (friends.isNotEmpty()) {
                    item {
                        MainSectionTitle(sh("ARKADAŞ SIRALAMASI", "FRIEND RANKING"))
                        Spacer(Modifier.height(7.dp))
                        Surface(shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                friends.map { it.second }.sortedByDescending { it.rating }.take(8).forEachIndexed { index, friend ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}", color = MainUi.Muted, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(22.dp))
                                        ProfilePhotoAvatarRectWithGender(friend.avatarPath, friend.gender, friend.displayName, 28.dp, 34.dp, if (friend.isVip) MainUi.Gold else MainUi.Blue, friend.avatarVisibility != "hidden")
                                        Spacer(Modifier.width(8.dp))
                                        Text(friend.displayName, color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                                        Text(friend.rating.toString(), color = MainUi.Blue, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(sh("OYUNCU BUL", "FIND PLAYER"), color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it.take(24) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text(sh("En az 2 harf yaz", "Type at least 2 characters"), fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MainUi.Blue,
                                    unfocusedBorderColor = MainUi.Border,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                ),
                            )
                            Button(
                                onClick = {
                                    if (query.trim().length < 2 || busyKey != null) return@Button
                                    scope.launch {
                                        busyKey = "search"
                                        results = runCatching { backend.searchPlayers(query, 20) }.getOrDefault(emptyList())
                                        notice = if (results.isEmpty()) sh("Eşleşen oyuncu bulunamadı.", "No matching player found.") else null
                                        busyKey = null
                                    }
                                },
                                enabled = query.trim().length >= 2 && busyKey == null,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(13.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                            ) { Text(if (busyKey == "search") "…" else sh("ARA", "SEARCH"), fontWeight = FontWeight.Black) }
                        }
                    }
                }

                items(results, key = { it.id }) { player ->
                    val relation = friendships.firstOrNull { it.userId == player.id || it.friendId == player.id }
                    Surface(shape = RoundedCornerShape(17.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 44.dp, accent = if (player.isVip) MainUi.Gold else MainUi.Blue, visible = player.avatarVisibility != "hidden")
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${ratingLeagueProgress(player.rating).leagueName} • ${player.rating}", color = MainUi.Muted, fontSize = 13.sp)
                            }
                            Button(
                                onClick = {
                                    if (relation != null || busyKey != null) return@Button
                                    scope.launch {
                                        busyKey = player.id
                                        runCatching { backend.sendFriendRequest(player.id) }
                                            .onSuccess { notice = sh("Arkadaşlık isteği gönderildi.", "Friend request sent."); reload() }
                                            .onFailure { notice = sh("İstek gönderilemedi.", "Request could not be sent.") }
                                        busyKey = null
                                    }
                                },
                                enabled = relation == null && busyKey == null,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    when (relation?.status) {
                                        "accepted" -> sh("ARKADAŞ", "FRIEND")
                                        "pending" -> sh("BEKLİYOR", "PENDING")
                                        else -> if (busyKey == player.id) "…" else sh("EKLE", "ADD")
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }

                if (requests.isNotEmpty()) item { MainSectionTitle(sh("ARKADAŞLIK İSTEKLERİ", "FRIEND REQUESTS")) }
                items(requests, key = { it.second.id }) { (_, player) ->
                    Surface(shape = RoundedCornerShape(17.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .28f))) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 44.dp, accent = MainUi.Blue, visible = player.avatarVisibility != "hidden")
                            Spacer(Modifier.width(9.dp))
                            Text(player.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(
                                onClick = {
                                    if (busyKey != null) return@IconButton
                                    scope.launch {
                                        busyKey = player.id
                                        runCatching { backend.respondFriendRequest(player.id, false) }
                                        reload()
                                        busyKey = null
                                    }
                                },
                            ) { Icon(Icons.Rounded.Close, sh("Reddet", "Decline"), tint = MainUi.Red) }
                            IconButton(
                                onClick = {
                                    if (busyKey != null) return@IconButton
                                    scope.launch {
                                        busyKey = player.id
                                        runCatching { backend.respondFriendRequest(player.id, true) }
                                            .onSuccess { notice = sh("Arkadaşlık isteği kabul edildi.", "Friend request accepted.") }
                                        reload()
                                        busyKey = null
                                    }
                                },
                            ) { Icon(Icons.Rounded.Check, sh("Kabul et", "Accept"), tint = MainUi.Green) }
                        }
                    }
                }

                if (invites.isNotEmpty()) item { MainSectionTitle(sh("MAÇ DAVETLERİ", "GAME INVITATIONS")) }
                items(invites, key = { it.id }) { invite ->
                    val sender = inviteProfiles[invite.senderId]
                    Surface(shape = RoundedCornerShape(17.dp), color = MainUi.BlueSoft, border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .25f))) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfilePhotoAvatarRectWithGender(sender?.avatarPath, sender?.gender, sender?.displayName ?: sh("Oyuncu", "Player"), 38.dp, 46.dp, MainUi.Blue, sender?.avatarVisibility != "hidden")
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(sender?.displayName ?: sh("Maç daveti", "Game invite"), color = MainUi.Text, fontWeight = FontWeight.Black)
                                    Text(if (invite.language == "en") "English" else "Türkçe", color = MainUi.Muted, fontSize = 13.sp)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (busyKey != null) return@OutlinedButton
                                        scope.launch {
                                            busyKey = invite.id
                                            runCatching { backend.respondGameInvite(invite.id, false) }
                                            reload()
                                            busyKey = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text(sh("REDDET", "DECLINE"), color = MainUi.Red, fontSize = 13.sp, fontWeight = FontWeight.Black) }
                                Button(
                                    onClick = {
                                        if (busyKey != null) return@Button
                                        scope.launch {
                                            busyKey = invite.id
                                            runCatching { backend.respondGameInvite(invite.id, true) }
                                                .onSuccess { room -> if (room != null) onPlay() }
                                                .onFailure { notice = sh("Davet artık kullanılamıyor.", "The invite is no longer available."); reload() }
                                            busyKey = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                                ) { Text(if (busyKey == invite.id) "…" else sh("KABUL ET", "ACCEPT"), fontSize = 13.sp, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }

                if (requests.isEmpty() && invites.isEmpty() && results.isEmpty() && !loading) {
                    item {
                        Text(sh("Bekleyen istek veya davet yok.", "There are no pending requests or invitations."), Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MainUi.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            else -> {
                archRival?.let { rival ->
                    item {
                        Surface(shape = RoundedCornerShape(20.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .48f))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text(sh("EZELİ RAKİP", "ARCH RIVAL"), color = MainUi.Gold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = MainUi.Gold.copy(alpha = .12f)) {
                                        Icon(Icons.Rounded.MilitaryTech, null, Modifier.padding(10.dp).size(24.dp), tint = MainUi.Gold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(rival.displayName, color = MainUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                        Text("${rival.matches} ${sh("maç", "matches")} • ${rival.wins} ${sh("galibiyet", "wins")} • ${rival.losses} ${sh("mağlubiyet", "losses")}", color = MainUi.Muted, fontSize = 13.sp, maxLines = 2)
                                    }
                                    Text("${rival.myPoints}:${rival.theirPoints}", color = MainUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                if (rivals.isNotEmpty()) item { MainSectionTitle(sh("RAKİP GEÇMİŞİ", "RIVAL HISTORY")) }
                items(rivals, key = { it.opponentId }) { rival ->
                    Surface(shape = RoundedCornerShape(17.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = if (rival.presenceStatus == "online") MainUi.Green.copy(alpha = .10f) else MainUi.SurfaceSoft) {
                                Icon(Icons.Rounded.Person, null, tint = if (rival.presenceStatus == "online") MainUi.Green else MainUi.Muted, modifier = Modifier.padding(8.dp).size(20.dp))
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(rival.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, maxLines = 1)
                                Text("${rival.matches} ${sh("maç", "matches")} • ${rival.wins} ${sh("galibiyet", "wins")} • ${rival.losses} ${sh("mağlubiyet", "losses")} • ${rival.myPoints}:${rival.theirPoints}", color = MainUi.Muted, fontSize = 13.sp, maxLines = 2)
                            }
                            Button(
                                onClick = {
                                    if (busyKey != null) return@Button
                                    scope.launch {
                                        busyKey = rival.opponentId
                                        if (rival.isFriend) {
                                            runCatching { backend.inviteFriend(rival.opponentId, SonHarfUiState.language) }
                                                .onSuccess { notice = sh("Rövanş daveti gönderildi.", "Rematch invite sent.") }
                                                .onFailure { notice = sh("Rövanş daveti gönderilemedi.", "Rematch invite could not be sent.") }
                                        } else {
                                            runCatching { backend.sendFriendRequest(rival.opponentId) }
                                                .onSuccess { notice = sh("Önce arkadaşlık isteği gönderildi.", "A friend request was sent first.") }
                                                .onFailure { notice = sh("İstek gönderilemedi.", "Request could not be sent.") }
                                        }
                                        busyKey = null
                                    }
                                },
                                enabled = busyKey == null,
                                shape = RoundedCornerShape(11.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Text(if (busyKey == rival.opponentId) "…" else sh("RÖVANŞ", "REMATCH"), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                if (matchHistory.isNotEmpty()) item { MainSectionTitle(sh("SON MAÇLAR", "RECENT MATCHES")) }
                items(matchHistory.take(12), key = { it.matchId }) { match ->
                    val won = match.result == "win"
                    val draw = match.result == "draw"
                    Surface(shape = RoundedCornerShape(16.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(10.dp), color = when { won -> MainUi.Green.copy(alpha = .11f); draw -> MainUi.Gold.copy(alpha = .11f); else -> MainUi.Red.copy(alpha = .09f) }) {
                                Text(
                                    when { won -> "G"; draw -> "B"; else -> "M" },
                                    Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                    color = when { won -> MainUi.Green; draw -> MainUi.Gold; else -> MainUi.Red },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(match.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, maxLines = 1)
                                Text("${match.myScore}-${match.theirScore} • ${if (match.ratingDelta >= 0) "+" else ""}${match.ratingDelta} rating", color = MainUi.Muted, fontSize = 13.sp)
                            }
                            if (match.isFriend) Icon(Icons.Rounded.People, null, tint = MainUi.Blue, modifier = Modifier.size(17.dp))
                        }
                    }
                }

                if (rivals.isEmpty() && matchHistory.isEmpty() && !loading) {
                    item {
                        MainSocialEmpty(
                            icon = Icons.Rounded.SportsKabaddi,
                            title = sh("Rakip geçmişin henüz yok", "No rival history yet"),
                            body = sh("İlk gerçek oyuncu maçından sonra rakiplerin burada görünür.", "Rivals appear here after your first real-player match."),
                            action = sh("OYNA", "PLAY"),
                            onAction = onPlay,
                        )
                    }
                }
            }
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MainUi.BlueSoft) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = MainUi.Text, fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun MainFriendCard(
    friend: ProfileDto,
    busy: Boolean,
    onInvite: () -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val online = friend.presenceStatus == "online"
    Surface(shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, if (online) MainUi.Green.copy(alpha = .28f) else MainUi.Border)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                ProfilePhotoAvatarRectWithGender(friend.avatarPath, friend.gender, friend.displayName, 44.dp, 54.dp, if (friend.isVip) MainUi.Gold else MainUi.Blue, friend.avatarVisibility != "hidden")
                Box(
                    Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(if (online) MainUi.Green else MainUi.Muted),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(friend.displayName, color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (friend.isVip) {
                        Spacer(Modifier.width(5.dp))
                        Text("VIP", color = MainUi.Gold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    if (online) sh("Çevrimiçi", "Online") else sh("Çevrimdışı", "Offline"),
                    color = if (online) MainUi.Green else MainUi.Muted,
                    fontSize = 13.sp,
                )
                Text("${ratingLeagueProgress(friend.rating).leagueName} • ${friend.rating}", color = MainUi.Muted, fontSize = 13.sp)
            }
            Button(
                onClick = onInvite,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
            ) { Text(if (busy) "…" else sh("DAVET", "INVITE"), fontSize = 13.sp, fontWeight = FontWeight.Black) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, sh("Daha fazla", "More"), tint = MainUi.Muted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(sh("Arkadaşlıktan çıkar", "Remove friend"), color = MainUi.Red) },
                        onClick = { menu = false; onRemove() },
                        leadingIcon = { Icon(Icons.Rounded.PersonRemove, null, tint = MainUi.Red) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSocialEmpty(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = MainUi.BlueSoft) {
                Icon(icon, null, tint = MainUi.Blue, modifier = Modifier.padding(12.dp).size(28.dp))
            }
            Text(title, color = MainUi.Text, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(body, color = MainUi.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            TextButton(onClick = onAction) { Text(action, color = MainUi.Blue, fontWeight = FontWeight.Black) }
        }
    }
}
