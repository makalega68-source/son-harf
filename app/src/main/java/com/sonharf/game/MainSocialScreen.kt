package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var isPro by remember { mutableStateOf(false) }
    var proChecked by remember { mutableStateOf(false) }
    var vipDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var friendships by remember { mutableStateOf<List<FriendshipDto>>(emptyList()) }
    var requests by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var siegeInvites by remember { mutableStateOf<List<WordSiegeInviteDto>>(emptyList()) }
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
        val legacyInviteTask = async { runCatching { backend.getIncomingGameInvites() }.getOrDefault(emptyList()) }
        val siegeInviteTask = async { runCatching { backend.getIncomingWordSiegeInvites() }.getOrDefault(emptyList()) }
        val rivalTask = async { runCatching { backend.getRivalHistory(30) }.getOrDefault(emptyList()) }
        val historyTask = async { runCatching { backend.getMatchHistory(30) }.getOrDefault(emptyList()) }
        val archTask = async { runCatching { backend.getArchRival() }.getOrNull() }
        friends = friendTask.await()
        friendships = friendshipTask.await()
        requests = requestTask.await()
        invites = legacyInviteTask.await()
        siegeInvites = siegeInviteTask.await()
        rivals = rivalTask.await()
        matchHistory = historyTask.await()
        archRival = archTask.await()
        val senders = linkedMapOf<String, ProfileDto>()
        (invites.map { it.senderId } + siegeInvites.map { it.senderId }).distinct().forEach { id ->
            runCatching { backend.getProfile(id) }.getOrNull()?.let { senders[id] = it }
        }
        inviteProfiles = senders
        loading = false
    }

    LaunchedEffect(Unit) {
        isPro = runCatching { backend.currentUserId()?.let { backend.getProfile(it).isVip } ?: false }.getOrDefault(false)
        proChecked = true
        reload()
    }

    val onlineCount = friends.count { it.second.presenceStatus == "online" }
    val incomingCount = requests.size + invites.size + siegeInvites.size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item { PurchasedSectionHeader(sh("SOSYAL MERKEZ", "SOCIAL HUB")) }

        if (loading) {
            item {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(5.dp),
                    color = Color(0xFF58B957),
                    trackColor = Color(0xFFDEC59B),
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SocialMetric(friends.size.toString(), sh("Arkadaş", "Friends"), PurchasedUiAsset.NAV_SOCIAL, Modifier.weight(1f))
                SocialMetric(onlineCount.toString(), sh("Çevrimiçi", "Online"), PurchasedUiAsset.ICON_CHAT, Modifier.weight(1f))
                SocialMetric(incomingCount.toString(), sh("Yeni", "New"), PurchasedUiAsset.ICON_GIFT, Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PurchasedButton(
                    text = sh("KUŞATMA OYNA", "PLAY SIEGE"),
                    onClick = onSiege,
                    modifier = Modifier.weight(1f),
                    style = PurchasedButtonStyle.PRIMARY,
                    leadingAsset = PurchasedUiAsset.ICON_SWORDS,
                )
                PurchasedButton(
                    text = sh("OYUNCU BUL", "FIND PLAYER"),
                    onClick = { tab = 1 },
                    modifier = Modifier.weight(1f),
                    style = PurchasedButtonStyle.SECONDARY,
                    leadingAsset = PurchasedUiAsset.NAV_SOCIAL,
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SocialTab(sh("ARKADAŞLAR", "FRIENDS"), PurchasedUiAsset.NAV_SOCIAL, tab == 0, Modifier.weight(1f)) { tab = 0 }
                SocialTab(sh("İSTEKLER", "REQUESTS"), PurchasedUiAsset.ICON_GIFT, tab == 1, Modifier.weight(1f), incomingCount) { tab = 1 }
                SocialTab(sh("RAKİPLER", "RIVALS"), PurchasedUiAsset.ICON_SWORDS, tab == 2, Modifier.weight(1f)) { tab = 2 }
            }
        }

        when (tab) {
            0 -> {
                if (proChecked && !isPro) {
                    item { ProFriendListLock(onUpgrade = { vipDialog = true }) }
                } else {
                    if (friends.isEmpty() && !loading) {
                        item {
                            MainSocialEmpty(
                                asset = PurchasedUiAsset.NAV_SOCIAL,
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
                                    runCatching { backend.inviteFriendToWordSiege(friend.id, SonHarfUiState.language) }
                                        .onSuccess {
                                            notice = sh("${friend.displayName} Kelime Kuşatması'na davet edildi.", "${friend.displayName} was invited to Word Siege.")
                                            SonHarfSoundFx.softNotify()
                                        }
                                        .onFailure { notice = sh("Kuşatma daveti gönderilemedi veya bekleyen bir davet var.", "Siege invite could not be sent or one is already pending.") }
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
                        item { PurchasedSectionHeader(sh("ARKADAŞ SIRALAMASI", "FRIEND RANKING")) }
                        item {
                            PurchasedPanel(
                                modifier = Modifier.fillMaxWidth(),
                                asset = PurchasedUiAsset.PANEL_LARGE,
                                contentPadding = PaddingValues(13.dp),
                            ) {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    friends.map { it.second }.sortedByDescending { it.rating }.take(8).forEachIndexed { index, friend ->
                                        PurchasedLeaderboardFriendRow(index, friend)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    PurchasedPanel(
                        modifier = Modifier.fillMaxWidth(),
                        asset = PurchasedUiAsset.PANEL_MEDIUM,
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PurchasedAsset(PurchasedUiAsset.NAV_SOCIAL, Modifier.size(40.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(sh("OYUNCU BUL", "FIND PLAYER"), color = Color(0xFF4A2D20), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it.take(24) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text(sh("En az 2 harf yaz", "Type at least 2 characters"), fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6B3CA6),
                                    unfocusedBorderColor = Color(0xFF9A7352),
                                    focusedContainerColor = Color(0xFFFFF1CF),
                                    unfocusedContainerColor = Color(0xFFFFF1CF),
                                ),
                            )
                            PurchasedButton(
                                text = if (busyKey == "search") "…" else sh("ARA", "SEARCH"),
                                onClick = {
                                    if (query.trim().length < 2 || busyKey != null) return@PurchasedButton
                                    scope.launch {
                                        busyKey = "search"
                                        results = runCatching { backend.searchPlayers(query, 20) }.getOrDefault(emptyList())
                                        notice = if (results.isEmpty()) sh("Eşleşen oyuncu bulunamadı.", "No matching player found.") else null
                                        busyKey = null
                                    }
                                },
                                enabled = query.trim().length >= 2 && busyKey == null,
                                modifier = Modifier.fillMaxWidth(),
                                style = PurchasedButtonStyle.SECONDARY,
                            )
                        }
                    }
                }

                items(results, key = { it.id }) { player ->
                    val relation = friendships.firstOrNull { it.userId == player.id || it.friendId == player.id }
                    SocialPlayerRow(
                        player = player,
                        actionText = when (relation?.status) {
                            "accepted" -> sh("ARKADAŞ", "FRIEND")
                            "pending" -> sh("BEKLİYOR", "PENDING")
                            else -> if (busyKey == player.id) "…" else sh("EKLE", "ADD")
                        },
                        actionEnabled = relation == null && busyKey == null,
                        onAction = {
                            if (relation != null || busyKey != null) return@SocialPlayerRow
                            scope.launch {
                                busyKey = player.id
                                runCatching { backend.sendFriendRequest(player.id) }
                                    .onSuccess { notice = sh("Arkadaşlık isteği gönderildi.", "Friend request sent."); reload() }
                                    .onFailure { notice = sh("İstek gönderilemedi.", "Request could not be sent.") }
                                busyKey = null
                            }
                        },
                    )
                }

                if (requests.isNotEmpty()) item { PurchasedSectionHeader(sh("ARKADAŞLIK İSTEKLERİ", "FRIEND REQUESTS")) }
                items(requests, key = { it.second.id }) { (_, player) ->
                    PurchasedPanel(
                        modifier = Modifier.fillMaxWidth(),
                        asset = PurchasedUiAsset.PANEL_SMALL,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PurchasedAvatarFrame(Modifier.size(54.dp)) {
                                ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 43.dp, accent = MainUi.Blue, visible = player.avatarVisibility != "hidden")
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(player.displayName, color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1)
                            PurchasedIconButton(PurchasedUiAsset.ICON_CLOSE, onClick = {
                                if (busyKey != null) return@PurchasedIconButton
                                scope.launch {
                                    busyKey = player.id
                                    runCatching { backend.respondFriendRequest(player.id, false) }
                                    reload()
                                    busyKey = null
                                }
                            })
                            PurchasedIconButton(PurchasedUiAsset.ICON_CHECK, onClick = {
                                if (busyKey != null) return@PurchasedIconButton
                                scope.launch {
                                    busyKey = player.id
                                    runCatching { backend.respondFriendRequest(player.id, true) }
                                        .onSuccess { notice = sh("Arkadaşlık isteği kabul edildi.", "Friend request accepted.") }
                                    reload()
                                    busyKey = null
                                }
                            })
                        }
                    }
                }

                if (siegeInvites.isNotEmpty()) item { PurchasedSectionHeader(sh("KELİME KUŞATMASI DAVETLERİ", "WORD SIEGE INVITATIONS")) }
                items(siegeInvites, key = { "siege:${it.id}" }) { invite ->
                    val sender = inviteProfiles[invite.senderId]
                    SocialInviteCard(
                        sender = sender,
                        modeTitle = sh("Kelime Kuşatması", "Word Siege"),
                        language = invite.language,
                        busy = busyKey == "siege:${invite.id}",
                        onDecline = {
                            if (busyKey != null) return@SocialInviteCard
                            scope.launch {
                                busyKey = "siege:${invite.id}"
                                runCatching { backend.respondWordSiegeInvite(invite.id, false) }
                                reload()
                                busyKey = null
                            }
                        },
                        onAccept = {
                            if (busyKey != null) return@SocialInviteCard
                            scope.launch {
                                busyKey = "siege:${invite.id}"
                                runCatching { backend.respondWordSiegeInvite(invite.id, true) }
                                    .onSuccess { game ->
                                        if (game != null) {
                                            notice = sh("Kuşatma maçı hazır.", "Siege match is ready.")
                                            onSiege()
                                        }
                                    }
                                    .onFailure { notice = sh("Kuşatma daveti artık kullanılamıyor.", "The Siege invite is no longer available."); reload() }
                                busyKey = null
                            }
                        },
                    )
                }

                if (invites.isNotEmpty()) item { PurchasedSectionHeader(sh("SON HARF DAVETLERİ", "LAST LETTER INVITATIONS")) }
                items(invites, key = { "legacy:${it.id}" }) { invite ->
                    val sender = inviteProfiles[invite.senderId]
                    SocialInviteCard(
                        sender = sender,
                        modeTitle = sh("Son Harf", "Last Letter"),
                        language = invite.language,
                        busy = busyKey == "legacy:${invite.id}",
                        onDecline = {
                            if (busyKey != null) return@SocialInviteCard
                            scope.launch {
                                busyKey = "legacy:${invite.id}"
                                runCatching { backend.respondGameInvite(invite.id, false) }
                                reload()
                                busyKey = null
                            }
                        },
                        onAccept = {
                            if (busyKey != null) return@SocialInviteCard
                            scope.launch {
                                busyKey = "legacy:${invite.id}"
                                runCatching { backend.respondGameInvite(invite.id, true) }
                                    .onSuccess { room -> if (room != null) onPlay() }
                                    .onFailure { notice = sh("Son Harf daveti artık kullanılamıyor.", "The Last Letter invite is no longer available."); reload() }
                                busyKey = null
                            }
                        },
                    )
                }

                if (requests.isEmpty() && invites.isEmpty() && siegeInvites.isEmpty() && results.isEmpty() && !loading) {
                    item {
                        MainSocialEmpty(
                            asset = PurchasedUiAsset.ICON_GIFT,
                            title = sh("Bekleyen istek yok", "No pending requests"),
                            body = sh("Yeni davetler ve arkadaşlık istekleri burada görünür.", "New invitations and friend requests appear here."),
                            action = sh("OYUNCU ARA", "SEARCH PLAYERS"),
                            onAction = {},
                        )
                    }
                }
            }

            else -> {
                archRival?.let { rival ->
                    item {
                        PurchasedPanel(
                            modifier = Modifier.fillMaxWidth(),
                            asset = PurchasedUiAsset.PANEL_LARGE,
                            contentPadding = PaddingValues(16.dp),
                        ) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PurchasedAsset(PurchasedUiAsset.ICON_SWORDS, Modifier.size(56.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(sh("EZELİ RAKİP", "ARCH RIVAL"), color = Color(0xFF6B3CA6), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        Text(rival.displayName, color = Color(0xFF4A2D20), fontSize = 17.sp, fontWeight = FontWeight.Black)
                                        Text("${rival.matches} ${sh("maç", "matches")} • ${rival.wins}-${rival.losses}", color = Color(0xFF765746), fontSize = 9.sp)
                                    }
                                    Text("${rival.myPoints}:${rival.theirPoints}", color = Color(0xFF4A2D20), fontSize = 22.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                if (rivals.isNotEmpty()) item { PurchasedSectionHeader(sh("RAKİP GEÇMİŞİ", "RIVAL HISTORY")) }
                items(rivals, key = { it.opponentId }) { rival ->
                    PurchasedPanel(
                        modifier = Modifier.fillMaxWidth(),
                        asset = PurchasedUiAsset.PANEL_SMALL,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PurchasedAsset(PurchasedUiAsset.NAV_PROFILE, Modifier.size(46.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(rival.displayName, color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, maxLines = 1)
                                Text("${rival.matches} ${sh("maç", "matches")} • ${rival.wins}-${rival.losses} • ${rival.myPoints}:${rival.theirPoints}", color = Color(0xFF765746), fontSize = 8.5.sp)
                            }
                            PurchasedButton(
                                text = if (busyKey == rival.opponentId) "…" else sh("RÖVANŞ", "REMATCH"),
                                onClick = {
                                    if (busyKey != null) return@PurchasedButton
                                    scope.launch {
                                        busyKey = rival.opponentId
                                        if (rival.isFriend) {
                                            runCatching { backend.inviteFriendToWordSiege(rival.opponentId, SonHarfUiState.language) }
                                                .onSuccess { notice = sh("Kelime Kuşatması rövanş daveti gönderildi.", "Word Siege rematch invite sent.") }
                                                .onFailure { notice = sh("Rövanş daveti gönderilemedi veya bekleyen bir davet var.", "Rematch invite could not be sent or one is already pending.") }
                                        } else {
                                            runCatching { backend.sendFriendRequest(rival.opponentId) }
                                                .onSuccess { notice = sh("Önce arkadaşlık isteği gönderildi.", "A friend request was sent first.") }
                                                .onFailure { notice = sh("İstek gönderilemedi.", "Request could not be sent.") }
                                        }
                                        busyKey = null
                                    }
                                },
                                enabled = busyKey == null,
                                modifier = Modifier.width(104.dp),
                                style = PurchasedButtonStyle.WARNING,
                            )
                        }
                    }
                }

                if (matchHistory.isNotEmpty()) item { PurchasedSectionHeader(sh("SON MAÇLAR", "RECENT MATCHES")) }
                items(matchHistory.take(12), key = { it.matchId }) { match ->
                    val won = match.result == "win"
                    val draw = match.result == "draw"
                    PurchasedPanel(
                        modifier = Modifier.fillMaxWidth(),
                        asset = PurchasedUiAsset.PANEL_SMALL,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PurchasedAsset(
                                when { won -> PurchasedUiAsset.ICON_CHECK; draw -> PurchasedUiAsset.ICON_TROPHY; else -> PurchasedUiAsset.ICON_CLOSE },
                                Modifier.size(38.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(match.displayName, color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, maxLines = 1)
                                Text("${match.myScore}-${match.theirScore} • ${if (match.ratingDelta >= 0) "+" else ""}${match.ratingDelta} rating", color = Color(0xFF765746), fontSize = 8.5.sp)
                            }
                            if (match.isFriend) PurchasedAsset(PurchasedUiAsset.NAV_SOCIAL, Modifier.size(30.dp))
                        }
                    }
                }

                if (rivals.isEmpty() && matchHistory.isEmpty() && !loading) {
                    item {
                        MainSocialEmpty(
                            asset = PurchasedUiAsset.ICON_SWORDS,
                            title = sh("Rakip geçmişin henüz yok", "No rival history yet"),
                            body = sh("İlk gerçek oyuncu maçından sonra rakiplerin burada görünür.", "Rivals appear here after your first real-player match."),
                            action = sh("KUŞATMA OYNA", "PLAY SIEGE"),
                            onAction = onSiege,
                        )
                    }
                }
            }
        }

        notice?.let { message ->
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Text(message, Modifier.fillMaxWidth(), color = Color(0xFF4A2D20), fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (vipDialog) {
        VipPurchaseDialog(
            onVerified = { isPro = true; vipDialog = false },
            onDismiss = { vipDialog = false },
        )
    }
}

@Composable
private fun SocialMetric(value: String, label: String, asset: PurchasedUiAsset, modifier: Modifier = Modifier) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 96.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(8.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PurchasedAsset(asset, Modifier.size(30.dp))
            Text(value, color = Color(0xFF4A2D20), fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color(0xFF765746), fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SocialTab(
    text: String,
    asset: PurchasedUiAsset,
    selected: Boolean,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    onClick: () -> Unit,
) {
    PurchasedPanel(
        modifier = modifier.clickable(onClick = onClick),
        asset = if (selected) PurchasedUiAsset.PANEL_LARGE else PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                PurchasedAsset(asset, Modifier.size(30.dp))
                if (badge > 0) {
                    Text(
                        badge.toString(),
                        modifier = Modifier.align(Alignment.TopEnd),
                        color = Color(0xFFB4433E),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Text(text, color = if (selected) Color(0xFF6B3CA6) else Color(0xFF654A3D), fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun PurchasedLeaderboardFriendRow(index: Int, friend: ProfileDto) {
    Box(Modifier.fillMaxWidth().heightIn(min = 49.dp)) {
        PurchasedAsset(PurchasedUiAsset.LEADERBOARD_ROW, Modifier.matchParentSize())
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", color = Color(0xFF654A3D), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(22.dp))
            PurchasedAvatarFrame(Modifier.size(38.dp)) {
                ProfilePhotoAvatar(friend.avatarPath, friend.displayName, 29.dp, visible = friend.avatarVisibility != "hidden", accent = if (friend.isVip) MainUi.Gold else MainUi.Blue)
            }
            Spacer(Modifier.width(7.dp))
            Text(friend.displayName, color = Color(0xFF4A2D20), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            Text(friend.rating.toString(), color = Color(0xFF6B3CA6), fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SocialPlayerRow(player: ProfileDto, actionText: String, actionEnabled: Boolean, onAction: () -> Unit) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PurchasedAvatarFrame(Modifier.size(56.dp)) {
                ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 43.dp, accent = if (player.isVip) MainUi.Gold else MainUi.Blue, visible = player.avatarVisibility != "hidden")
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(player.displayName, color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${ratingLeagueProgress(player.rating).leagueName} • ${player.rating}", color = Color(0xFF765746), fontSize = 9.sp)
            }
            PurchasedButton(
                text = actionText,
                onClick = onAction,
                enabled = actionEnabled,
                modifier = Modifier.width(96.dp),
                style = PurchasedButtonStyle.SECONDARY,
            )
        }
    }
}

@Composable
private fun SocialInviteCard(
    sender: ProfileDto?,
    modeTitle: String,
    language: String,
    busy: Boolean,
    onDecline: () -> Unit,
    onAccept: () -> Unit,
) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(13.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAvatarFrame(Modifier.size(56.dp)) {
                    ProfilePhotoAvatar(sender?.avatarPath, sender?.displayName ?: sh("Oyuncu", "Player"), 43.dp, visible = sender?.avatarVisibility != "hidden", accent = Color(0xFF567A64))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(sender?.displayName ?: sh("Oyun daveti", "Game invite"), color = Color(0xFF4A2D20), fontWeight = FontWeight.Black)
                    Text("$modeTitle • ${if (language == "en") "English" else "Türkçe"}", color = Color(0xFF765746), fontSize = 9.sp)
                }
                PurchasedAsset(PurchasedUiAsset.ICON_GAMES, Modifier.size(40.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PurchasedButton(
                    text = sh("REDDET", "DECLINE"),
                    onClick = onDecline,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    style = PurchasedButtonStyle.DANGER,
                )
                PurchasedButton(
                    text = if (busy) "…" else sh("KABUL ET", "ACCEPT"),
                    onClick = onAccept,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    style = PurchasedButtonStyle.PRIMARY,
                )
            }
        }
    }
}

@Composable
private fun ProFriendListLock(onUpgrade: () -> Unit) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(64.dp))
            Text(sh("Arkadaş listesi Pro'ya özel", "Friend list is Pro-only"), color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(sh("Pro üyelikle arkadaşlarını kaydet, davet et ve haftalık sıralamada rakip ol.", "Save friends, invite them and race in the weekly ranking with Pro."), color = Color(0xFF765746), fontSize = 10.sp, textAlign = TextAlign.Center)
            PurchasedButton(
                text = sh("PRO'YA GEÇ", "GO PRO"),
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
                style = PurchasedButtonStyle.PURPLE,
                leadingAsset = PurchasedUiAsset.ICON_CROWN,
            )
        }
    }
}

@Composable
private fun MainFriendCard(friend: ProfileDto, busy: Boolean, onInvite: () -> Unit, onRemove: () -> Unit) {
    val online = friend.presenceStatus == "online"
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PurchasedAvatarFrame(Modifier.size(60.dp)) {
                ProfilePhotoAvatarWithGender(friend.avatarPath, friend.gender, friend.displayName, 47.dp, accent = if (friend.isVip) MainUi.Gold else MainUi.Blue, visible = friend.avatarVisibility != "hidden")
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(friend.displayName, color = Color(0xFF4A2D20), fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (friend.isVip) {
                        Spacer(Modifier.width(4.dp))
                        PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(22.dp))
                    }
                }
                Text(if (online) sh("● Çevrimiçi", "● Online") else sh("○ Çevrimdışı", "○ Offline"), color = if (online) Color(0xFF4D9A4D) else Color(0xFF765746), fontSize = 9.sp)
                Text("${ratingLeagueProgress(friend.rating).leagueName} • ${friend.rating}", color = Color(0xFF765746), fontSize = 8.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                PurchasedButton(
                    text = if (busy) "…" else sh("KUŞAT", "SIEGE"),
                    onClick = onInvite,
                    enabled = !busy,
                    modifier = Modifier.width(94.dp),
                    style = PurchasedButtonStyle.WARNING,
                    leadingAsset = PurchasedUiAsset.ICON_SWORDS,
                )
                PurchasedIconButton(PurchasedUiAsset.ICON_CLOSE, onClick = onRemove, modifier = Modifier.size(42.dp))
            }
        }
    }
}

@Composable
private fun MainSocialEmpty(asset: PurchasedUiAsset, title: String, body: String, action: String, onAction: () -> Unit) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PurchasedAsset(asset, Modifier.size(56.dp))
            Text(title, color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(body, color = Color(0xFF765746), fontSize = 10.sp, textAlign = TextAlign.Center)
            PurchasedButton(text = action, onClick = onAction, modifier = Modifier.fillMaxWidth(), style = PurchasedButtonStyle.SECONDARY)
        }
    }
}
