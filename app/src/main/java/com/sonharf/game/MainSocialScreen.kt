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
    onSiege: () -> Unit = onPlay,
    initialTab: Int = 0,
    onOpenSiege: (WordSiegeGameDto) -> Unit = { game -> WordSiegeLaunchConfig.open(game); onSiege() },
) {
    val scope = rememberCoroutineScope()
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    if (tab == 2) { LatestRivalsScreen(backend, onBack = { tab = 0 }, onOpenSiege = onOpenSiege); return }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var friendships by remember { mutableStateOf<List<FriendshipDto>>(emptyList()) }
    var requests by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    // Requests sent in this session show as pending at once, even before the list reloads.
    var sentRequests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var autoTabDone by remember { mutableStateOf(false) }

    fun friendRequestError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            "pro_friend_list_required" in raw -> {
                sh("Arkadaşlık isteği şu anda gönderilemiyor.", "Friend request is currently unavailable.")
            }
            "blocked_relationship" in raw -> sh("Bu oyuncuyla engelleme olduğu için istek gönderilemiyor.", "You can't send a request because of a block.")
            "cannot_friend_self" in raw -> sh("Kendine istek gönderemezsin.", "You can't add yourself.")
            else -> sh("İstek gönderilemedi. Bağlantını kontrol edip tekrar dene.", "Request could not be sent. Check your connection and try again.")
        }
    }

    suspend fun reload() = coroutineScope {
        loading = true
        val friendTask = async { gameRequestResult { backend.getFriends() }.getOrDefault(friends) }
        val friendshipTask = async {
            gameRequestResult { backend.getFriendships() }
                .onFailure { notice = sh("Arkadaş listesi yüklenemedi. Yenilemek için sayfayı tekrar aç.", "Friend list could not be loaded. Reopen the page to refresh.") }
                .getOrDefault(emptyList())
        }
        val requestTask = async { gameRequestResult { backend.getIncomingFriendRequests() }.getOrDefault(emptyList()) }
        friends = friendTask.await().sortedByDescending { it.second.isRecentlyOnline() }
        friendships = friendshipTask.await()
        requests = requestTask.await()
        loading = false
        // Someone waiting for an answer is the first thing to show.
        if (!autoTabDone) {
            autoTabDone = true
            if (initialTab == 0 && requests.isNotEmpty()) tab = 1
        }
    }

    val foreground = rememberAppForeground()
    LaunchedEffect(foreground) {
        if (!foreground) return@LaunchedEffect
        reload()
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            if (busyKey == null) reload()
        }
    }

    val onlineCount = friends.count { it.second.isRecentlyOnline() }
    val incomingCount = requests.size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Arkadaşlar", "Friends"),
                subtitle = sh("Birlikte oyna, rekabeti paylaş", "Play together, share the rivalry"),
            )
        }

        item(key = "social_refresh") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(sh("Bir davet, yeni bir rekabet", "One invitation, a new rivalry"), color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { reload() } }, enabled = !loading && busyKey == null) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sh("Yenile", "Refresh"), fontSize = 11.sp)
                }
            }
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
                    onClick = onSiege,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue, contentColor = Color.White),
                ) {
                    Icon(Icons.Rounded.Shield, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                OutlinedButton(
                    // Search, "EKLE" and incoming requests all live on the Requests tab.
                    onClick = { tab = 1 },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .55f)),
                ) {
                    Icon(Icons.Rounded.GroupAdd, null, tint = MainUi.Gold, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sh("ARKADAŞ DAVETİ", "FRIEND INVITE"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }

        item {
            LobbyTabs(
                labels = listOf(sh("Arkadaşlar", "Friends"),
                    sh("İstekler", "Requests") + if (incomingCount > 0) " ($incomingCount)" else "",
                    sh("Rakipler", "Rivals")),
                selected = tab, onSelect = { tab = it },
            )
        }

        when (tab) {
            0 -> {
                run {
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
                        busy = busyKey != null,
                        onInvite = {
                            if (busyKey != null) return@MainFriendCard
                            busyKey = friend.id
                            scope.launch {
                                gameRequestResult { backend.inviteFriendToWordSiege(friend.id, SonHarfUiState.language) }
                                    .onSuccess { invite ->
                                        WordSiegeLaunchConfig.awaitInvite(invite.id)
                                        notice = sh("${friend.displayName} Kelime Tahtı'na davet edildi.", "${friend.displayName} was invited to Word Throne.")
                                        SonHarfSoundFx.softNotify()
                                    }
                                    .onFailure { notice = sh("Oyun daveti gönderilemedi veya bekleyen bir davet var.", "Game invite could not be sent or one is already pending.") }
                                busyKey = null
                            }
                        },
                        onRemove = {
                            if (busyKey != null) return@MainFriendCard
                            busyKey = friend.id
                            scope.launch {
                                gameRequestResult { backend.removeFriend(friend.id) }
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
                                        Text("${index + 1}", color = MainUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(22.dp))
                                        ProfilePhotoAvatar(friend.avatarPath, friend.displayName, 30.dp, visible = friend.avatarVisibility != "hidden", accent = if (friend.isVip) MainUi.Gold else MainUi.Blue, gender = friend.gender)
                                        Spacer(Modifier.width(8.dp))
                                        Text(friend.displayName, color = MainUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                                        Text(friend.rating.toString(), color = MainUi.Blue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }

            1 -> {
                item(key = "friend_code") { PlayerInviteCard(backend) }
                item(key = "share_invite") { InviteFriendsCard(playerName = null) }
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(sh("OYUNCU BUL", "FIND PLAYER"), color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it.take(24) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text(sh("En az 2 harf yaz", "Type at least 2 characters"), fontSize = 11.sp) },
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
                                        results = gameRequestResult { backend.searchPlayers(query, 20) }.getOrDefault(emptyList())
                                        notice = if (results.isEmpty()) sh("Eşleşen oyuncu bulunamadı.", "No matching player found.") else null
                                        busyKey = null
                                    }
                                },
                                enabled = query.trim().length >= 2 && busyKey == null,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(13.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue, contentColor = Color.White),
                            ) { Text(if (busyKey == "search") "…" else sh("ARA", "SEARCH"), fontWeight = FontWeight.Black) }
                        }
                    }
                }

                items(results, key = { it.id }) { player ->
                    val relation = friendships.firstOrNull { it.userId == player.id || it.friendId == player.id }
                    val relationStatus = relation?.status ?: if (player.id in sentRequests) "pending" else null
                    Surface(shape = RoundedCornerShape(17.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 44.dp, accent = if (player.isVip) MainUi.Gold else MainUi.Blue, visible = player.avatarVisibility != "hidden", userId = player.id)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${ratingLeagueProgress(player.rating).leagueName} • ${player.rating}", color = MainUi.Muted, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    if (relationStatus != null || busyKey != null) return@Button
                                    scope.launch {
                                        busyKey = player.id
                                        gameRequestResult { backend.sendFriendRequest(player.id) }
                                            .onSuccess {
                                                sentRequests = sentRequests + player.id
                                                notice = sh("İstek gönderildi.", "Request sent.")
                                                reload()
                                            }
                                            .onFailure { notice = friendRequestError(it) }
                                        busyKey = null
                                    }
                                },
                                enabled = relationStatus == null && busyKey == null,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    when (relationStatus) {
                                        "accepted" -> sh("ARKADAŞ", "FRIEND")
                                        "pending" -> sh("BEKLİYOR", "PENDING")
                                        else -> if (busyKey == player.id) "…" else sh("EKLE", "ADD")
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }

                item { MainSectionTitle(sh("ARKADAŞLIK İSTEKLERİ", "FRIEND REQUESTS")) }
                if (requests.isEmpty() && !loading) {
                    item {
                        Text(
                            sh("Bekleyen istek yok.", "Friend requests you receive appear here. Once you accept, you are added to each other's friend list."),
                            color = MainUi.Muted,
                            fontSize = 10.sp,
                        )
                    }
                }
                items(requests, key = { it.second.id }) { (_, player) ->
                    Surface(shape = RoundedCornerShape(17.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .28f))) {
                        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 44.dp, accent = MainUi.Blue, visible = player.avatarVisibility != "hidden", userId = player.id)
                            Spacer(Modifier.width(9.dp))
                            Text(player.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(
                                onClick = {
                                    if (busyKey != null) return@IconButton
                                    scope.launch {
                                        busyKey = player.id
                                        gameRequestResult { backend.respondFriendRequest(player.id, false) }
                                            .onSuccess { notice = sh("İstek reddedildi.", "Request declined.") }
                                            .onFailure { notice = sh("İşlem tamamlanamadı, tekrar dene.", "Could not complete, try again.") }
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
                                        gameRequestResult { backend.respondFriendRequest(player.id, true) }
                                            .onSuccess { notice = sh("Arkadaşlık isteği kabul edildi.", "Friend request accepted.") }
                                            .onFailure { notice = sh("İstek kabul edilemedi, tekrar dene.", "Request could not be accepted, try again.") }
                                        reload()
                                        busyKey = null
                                    }
                                },
                            ) { Icon(Icons.Rounded.Check, sh("Kabul et", "Accept"), tint = MainUi.Green) }
                        }
                    }
                }

                if (requests.isEmpty() && results.isEmpty() && !loading) {
                    item {
                        Text(sh("Bekleyen arkadaşlık isteğin yok.", "No pending friend requests."), Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MainUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            else -> Unit
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MainUi.BlueSoft) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = MainUi.Text, fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
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
    val online = friend.isRecentlyOnline()
    val playing = online && friend.presenceStatus == "in_game"
    Surface(shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, if (online) MainUi.Green.copy(alpha = .28f) else MainUi.Border)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                ProfilePhotoAvatarWithGender(friend.avatarPath, friend.gender, friend.displayName, 48.dp, accent = if (friend.isVip) MainUi.Gold else MainUi.Green, visible = friend.avatarVisibility != "hidden", frameId = rememberPlayerFrame(friend.id))
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
                        Text("PRO", color = MainUi.Gold, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    if (playing) sh("Oyunda", "Playing") else if (online) sh("Oynamaya hazır", "Ready to play") else sh("Çevrimdışı", "Offline"),
                    color = if (online) MainUi.Green else MainUi.Muted,
                    fontSize = 9.sp,
                )
                Text("${ratingLeagueProgress(friend.rating).leagueName} • ${friend.rating}", color = MainUi.Muted, fontSize = 8.sp)
            }
            Button(
                onClick = onInvite,
                enabled = !busy && !playing,
                colors = ButtonDefaults.buttonColors(containerColor = MainUi.Green, contentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
            ) { Text(if (busy) "…" else if (playing) sh("OYUNDA", "PLAYING") else sh("DAVET ET", "INVITE"), fontSize = 8.sp, fontWeight = FontWeight.Black) }
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
            Text(body, color = MainUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
            TextButton(onClick = onAction) { Text(action, color = MainUi.Blue, fontWeight = FontWeight.Black) }
        }
    }
}
