package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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

private enum class ProfessionalSocialTab { FRIENDS, INVITES, MESSAGES, RIVALS }

@Composable
internal fun ProfessionalSocialScreen(
    backend: OnlineGameBackend,
    onPlay: () -> Unit,
    onSiege: () -> Unit = onPlay,
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(ProfessionalSocialTab.FRIENDS) }
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
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var onlineOnly by remember { mutableStateOf(false) }
    var openProfileId by remember { mutableStateOf<String?>(null) }
    var messageFriend by remember { mutableStateOf<ProfileDto?>(null) }
    var conversations by remember { mutableStateOf<List<DirectConversation>>(emptyList()) }
    var siegeRivals by remember { mutableStateOf<List<SiegeRivalDto>>(emptyList()) }

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
        val conversationTask = async { runCatching { backend.getDirectConversations() }.getOrDefault(emptyList()) }
        val siegeRivalTask = async { runCatching { backend.getSiegeRivals(20) }.getOrDefault(emptyList()) }

        friends = friendTask.await()
        friendships = friendshipTask.await()
        requests = requestTask.await()
        invites = legacyInviteTask.await()
        siegeInvites = siegeInviteTask.await()
        rivals = rivalTask.await()
        matchHistory = historyTask.await()
        archRival = archTask.await()
        conversations = conversationTask.await()
        siegeRivals = siegeRivalTask.await()

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

    val onlineFriends = friends.filter { it.second.presenceStatus == "online" }
    val incomingCount = requests.size + invites.size + siegeInvites.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            GameTopBar(
                title = gameText("Sosyal", "Social"),
                subtitle = gameText("Arkadaşlar, davetler ve rekabet", "Friends, invites and rivalry"),
                trailing = {
                    GameIconButton(
                        icon = Icons.Rounded.PersonSearch,
                        description = gameText("Oyuncu Bul", "Find Player"),
                        onClick = { showSearch = !showSearch },
                    )
                },
            )
        }

        if (loading) item {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = GameColors.PrimaryBlue, trackColor = GameColors.SecondarySurface)
        }

        item(key = "metrics") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialMetric(Modifier.weight(1f), friends.size.toString(), gameText("Arkadaş", "Friends"), GameColors.PrimaryBlue)
                SocialMetric(Modifier.weight(1f), onlineFriends.size.toString(), gameText("Çevrimiçi", "Online"), GameColors.PlayGreen)
                SocialMetric(Modifier.weight(1f), incomingCount.toString(), gameText("Yeni", "New"), GameColors.RewardAmber)
            }
        }

        item(key = "primary_actions") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GamePrimaryButton(
                    text = gameText("KUŞATMA OYNA", "PLAY SIEGE"),
                    onClick = onSiege,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Shield,
                )
                GameSecondaryButton(
                    text = gameText("OYUNCU BUL", "FIND PLAYER"),
                    onClick = { showSearch = !showSearch },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.PersonSearch,
                )
            }
        }

        if (showSearch) {
            item(key = "search") {
                PlayerSearchCard(
                    query = query,
                    onQuery = { query = it.take(24) },
                    busy = busyKey == "search",
                    onSearch = {
                        if (query.trim().length < 2 || busyKey != null) return@PlayerSearchCard
                        scope.launch {
                            busyKey = "search"
                            results = runCatching { backend.searchPlayers(query, 20) }.getOrDefault(emptyList())
                            notice = if (results.isEmpty()) gameText("Eşleşen oyuncu bulunamadı.", "No matching player found.") else null
                            busyKey = null
                        }
                    },
                )
            }
            items(results, key = { "search:${it.id}" }) { player ->
                val relation = friendships.firstOrNull { it.userId == player.id || it.friendId == player.id }
                SearchPlayerRow(
                    onOpen = { openProfileId = player.id },
                    player = player,
                    relationStatus = relation?.status,
                    busy = busyKey == player.id,
                    onAdd = {
                        if (relation != null || busyKey != null) return@SearchPlayerRow
                        scope.launch {
                            busyKey = player.id
                            runCatching { backend.sendFriendRequest(player.id) }
                                .onSuccess { notice = gameText("Arkadaşlık isteği gönderildi.", "Friend request sent."); reload() }
                                .onFailure { notice = gameText("İstek gönderilemedi.", "Request could not be sent.") }
                            busyKey = null
                        }
                    },
                )
            }
        }

        item(key = "tabs") {
            SegmentedGameTabs(
                labels = listOf(
                    gameText("Arkadaşlar", "Friends"),
                    if (incomingCount > 0) gameText("Davetler $incomingCount", "Invites $incomingCount") else gameText("Davetler", "Invites"),
                    gameText("Mesajlar", "Messages"),
                    gameText("Rakipler", "Rivals"),
                ),
                selectedIndex = tab.ordinal,
                onSelected = { tab = ProfessionalSocialTab.entries[it] },
            )
        }

        when (tab) {
            ProfessionalSocialTab.FRIENDS -> {
                val visibleFriends = if (onlineOnly) onlineFriends else friends
                if (friends.isNotEmpty()) item(key = "online-filter") {
                    FilterChip(
                        selected = onlineOnly,
                        onClick = { onlineOnly = !onlineOnly },
                        label = { Text(gameText("Yalnızca çevrimiçi (${onlineFriends.size})", "Online only (${onlineFriends.size})")) },
                        leadingIcon = { Icon(Icons.Rounded.Circle, null, tint = GameColors.PlayGreen, modifier = Modifier.size(10.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GameColors.PrimarySurface,
                            labelColor = GameColors.TextSecondary,
                            selectedContainerColor = GameColors.PlayGreen.copy(alpha = .18f),
                            selectedLabelColor = GameColors.TextPrimary,
                        ),
                    )
                }
                if (proChecked && !isPro) {
                    item { ProfessionalFriendLock { vipDialog = true } }
                } else if (visibleFriends.isEmpty() && !loading) {
                    item {
                        GameEmptyState(
                            icon = if (onlineOnly) Icons.Rounded.WifiOff else Icons.Rounded.GroupAdd,
                            title = if (onlineOnly) gameText("Çevrimiçi arkadaş yok", "No friends online") else gameText("Henüz arkadaşın yok", "No friends yet"),
                            body = gameText("Oyuncu Bul ile yeni rakip ve arkadaşlar ekleyebilirsin.", "Use Find Player to add new friends and rivals."),
                            actionText = gameText("OYUNCU BUL", "FIND PLAYER"),
                            onAction = { showSearch = true },
                        )
                    }
                } else {
                    items(visibleFriends, key = { "friend:${it.second.id}" }) { (_, friend) ->
                        ProfessionalFriendRow(
                            friend = friend,
                            busy = busyKey == friend.id,
                            onOpen = { openProfileId = friend.id },
                            onInvite = {
                                if (busyKey != null) return@ProfessionalFriendRow
                                scope.launch {
                                    busyKey = friend.id
                                    runCatching { backend.inviteFriendToWordSiege(friend.id, SonHarfUiState.language) }
                                        .onSuccess { notice = gameText("${friend.displayName} Kelime Kuşatması'na davet edildi.", "${friend.displayName} was invited to Word Siege.") }
                                        .onFailure { notice = gameText("Davet gönderilemedi veya bekleyen bir davet var.", "Invite could not be sent or one is already pending.") }
                                    busyKey = null
                                }
                            },
                            onRemove = {
                                if (busyKey != null) return@ProfessionalFriendRow
                                scope.launch {
                                    busyKey = friend.id
                                    runCatching { backend.removeFriend(friend.id) }
                                        .onSuccess { notice = gameText("Arkadaş listesi güncellendi.", "Friend list updated."); reload() }
                                        .onFailure { notice = gameText("Arkadaş kaldırılamadı.", "Friend could not be removed.") }
                                    busyKey = null
                                }
                            },
                        )
                    }
                }
            }

            ProfessionalSocialTab.MESSAGES -> {
                if (proChecked && !isPro) {
                    item { ProfessionalFriendLock { vipDialog = true } }
                } else if (conversations.isEmpty() && !loading) {
                    item {
                        GameEmptyState(
                            icon = Icons.Rounded.ChatBubbleOutline,
                            title = gameText("Henüz mesajın yok", "No messages yet"),
                            body = gameText("Bir arkadaşının profilinden MESAJ ile sohbet başlatabilirsin.", "Start a chat from a friend's profile with MESSAGE."),
                        )
                    }
                }
                if (!(proChecked && !isPro)) {
                    items(conversations, key = { "dm:${it.friendId}" }) { conversation ->
                        val friend = friends.firstOrNull { it.second.id == conversation.friendId }?.second
                        ConversationRow(
                            name = friend?.displayName ?: gameText("Oyuncu", "Player"),
                            preview = conversation.lastBody,
                            fromMe = conversation.lastFromMe,
                            enabled = friend != null,
                            onOpen = { friend?.let { messageFriend = it } },
                        )
                    }
                }
            }

            ProfessionalSocialTab.INVITES -> {
                if (requests.isEmpty() && invites.isEmpty() && siegeInvites.isEmpty() && !loading) {
                    item {
                        GameEmptyState(
                            icon = Icons.Rounded.MarkEmailRead,
                            title = gameText("Bekleyen istek yok", "No pending requests"),
                            body = gameText("Yeni arkadaşlık ve oyun davetleri burada görünür.", "Friend and game invitations appear here."),
                        )
                    }
                }

                if (requests.isNotEmpty()) item { GameSectionHeader(gameText("Arkadaşlık İstekleri", "Friend Requests")) }
                items(requests, key = { "request:${it.second.id}" }) { (_, player) ->
                    FriendRequestRow(
                        player = player,
                        busy = busyKey == player.id,
                        onDecline = {
                            if (busyKey != null) return@FriendRequestRow
                            scope.launch {
                                busyKey = player.id
                                runCatching { backend.respondFriendRequest(player.id, false) }
                                reload(); busyKey = null
                            }
                        },
                        onAccept = {
                            if (busyKey != null) return@FriendRequestRow
                            scope.launch {
                                busyKey = player.id
                                runCatching { backend.respondFriendRequest(player.id, true) }
                                    .onSuccess { notice = gameText("Arkadaşlık isteği kabul edildi.", "Friend request accepted.") }
                                reload(); busyKey = null
                            }
                        },
                    )
                }

                if (siegeInvites.isNotEmpty()) item { GameSectionHeader(gameText("Kelime Kuşatması Davetleri", "Word Siege Invites")) }
                items(siegeInvites, key = { "siege:${it.id}" }) { invite ->
                    GameInviteCard(
                        name = inviteProfiles[invite.senderId]?.displayName ?: gameText("Oyuncu", "Player"),
                        avatarPath = inviteProfiles[invite.senderId]?.avatarPath,
                        title = gameText("Kelime Kuşatması", "Word Siege"),
                        language = invite.language,
                        accent = GameColors.TacticalTurquoise,
                        busy = busyKey == "siege:${invite.id}",
                        onDecline = {
                            if (busyKey != null) return@GameInviteCard
                            scope.launch {
                                busyKey = "siege:${invite.id}"
                                runCatching { backend.respondWordSiegeInvite(invite.id, false) }
                                reload(); busyKey = null
                            }
                        },
                        onAccept = {
                            if (busyKey != null) return@GameInviteCard
                            scope.launch {
                                busyKey = "siege:${invite.id}"
                                runCatching { backend.respondWordSiegeInvite(invite.id, true) }
                                    .onSuccess { game -> if (game != null) onSiege() }
                                    .onFailure { notice = gameText("Davet artık kullanılamıyor.", "Invite is no longer available."); reload() }
                                busyKey = null
                            }
                        },
                    )
                }

                if (invites.isNotEmpty()) item { GameSectionHeader(gameText("Son Harf Davetleri", "Last Letter Invites")) }
                items(invites, key = { "last:${it.id}" }) { invite ->
                    GameInviteCard(
                        name = inviteProfiles[invite.senderId]?.displayName ?: gameText("Oyuncu", "Player"),
                        avatarPath = inviteProfiles[invite.senderId]?.avatarPath,
                        title = gameText("Son Harf", "Last Letter"),
                        language = invite.language,
                        accent = GameColors.PrimaryBlue,
                        busy = busyKey == "last:${invite.id}",
                        onDecline = {
                            if (busyKey != null) return@GameInviteCard
                            scope.launch {
                                busyKey = "last:${invite.id}"
                                runCatching { backend.respondGameInvite(invite.id, false) }
                                reload(); busyKey = null
                            }
                        },
                        onAccept = {
                            if (busyKey != null) return@GameInviteCard
                            scope.launch {
                                busyKey = "last:${invite.id}"
                                runCatching { backend.respondGameInvite(invite.id, true) }
                                    .onSuccess { room -> if (room != null) onPlay() }
                                    .onFailure { notice = gameText("Davet artık kullanılamıyor.", "Invite is no longer available."); reload() }
                                busyKey = null
                            }
                        },
                    )
                }
            }

            ProfessionalSocialTab.RIVALS -> {
                if (siegeRivals.isNotEmpty()) {
                    item { GameSectionHeader(gameText("Kuşatma Rakipleri", "Siege Rivals")) }
                    items(siegeRivals, key = { "siege-rival:${it.opponentId}" }) { rival ->
                        SiegeRivalRow(
                            rival = rival,
                            busy = busyKey == "siege-rival:${rival.opponentId}",
                            onOpen = { openProfileId = rival.opponentId },
                            onRematch = {
                                if (busyKey != null) return@SiegeRivalRow
                                scope.launch {
                                    busyKey = "siege-rival:${rival.opponentId}"
                                    notice = if (rival.isFriend) {
                                        runCatching { backend.inviteFriendToWordSiege(rival.opponentId, SonHarfUiState.language) }.fold(
                                            onSuccess = { gameText("Rövanş daveti gönderildi.", "Rematch invite sent.") },
                                            onFailure = { gameText("Rövanş daveti gönderilemedi.", "Rematch invite could not be sent.") },
                                        )
                                    } else {
                                        runCatching { backend.sendFriendRequest(rival.opponentId) }.fold(
                                            onSuccess = { gameText("Rövanş için önce arkadaşlık isteği gönderildi.", "A friend request was sent first for the rematch.") },
                                            onFailure = { gameText("İstek gönderilemedi.", "Request could not be sent.") },
                                        )
                                    }
                                    busyKey = null
                                }
                            },
                        )
                    }
                }
                archRival?.let { rival -> item { ArchRivalCard(rival) { openProfileId = rival.opponentId } } }
                if (rivals.isNotEmpty()) item { GameSectionHeader(gameText("Rakip Geçmişi", "Rival History")) }
                items(rivals, key = { "rival:${it.opponentId}" }) { rival ->
                    RivalRow(
                        rival = rival,
                        busy = busyKey == rival.opponentId,
                        onOpen = { openProfileId = rival.opponentId },
                        onRematch = {
                            if (busyKey != null) return@RivalRow
                            scope.launch {
                                busyKey = rival.opponentId
                                if (rival.isFriend) {
                                    runCatching { backend.inviteFriendToWordSiege(rival.opponentId, SonHarfUiState.language) }
                                        .onSuccess { notice = gameText("Rövanş daveti gönderildi.", "Rematch invite sent.") }
                                        .onFailure { notice = gameText("Rövanş daveti gönderilemedi.", "Rematch invite could not be sent.") }
                                } else {
                                    runCatching { backend.sendFriendRequest(rival.opponentId) }
                                        .onSuccess { notice = gameText("Önce arkadaşlık isteği gönderildi.", "A friend request was sent first.") }
                                        .onFailure { notice = gameText("İstek gönderilemedi.", "Request could not be sent.") }
                                }
                                busyKey = null
                            }
                        },
                    )
                }
                if (matchHistory.isNotEmpty()) item { GameSectionHeader(gameText("Son Maçlar", "Recent Matches")) }
                items(matchHistory.take(12), key = { "match:${it.matchId}" }) { MatchHistoryRow(it) }
                if (rivals.isEmpty() && matchHistory.isEmpty() && !loading) {
                    item {
                        GameEmptyState(
                            icon = Icons.Rounded.SportsKabaddi,
                            title = gameText("Rakip geçmişin henüz yok", "No rival history yet"),
                            body = gameText("İlk gerçek oyuncu maçından sonra rakiplerin burada görünür.", "Rivals appear after your first real-player match."),
                            actionText = gameText("KUŞATMA OYNA", "PLAY SIEGE"),
                            onAction = onSiege,
                        )
                    }
                }
            }
        }

        notice?.let { message ->
            item(key = "notice") {
                Surface(shape = GameShapes.Medium, color = GameColors.PrimaryBlue.copy(alpha = .12f), border = BorderStroke(1.dp, GameColors.PrimaryBlue.copy(alpha = .24f))) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = GameColors.TextPrimary, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }

    if (vipDialog) {
        VipPurchaseDialog(
            onVerified = { isPro = true; vipDialog = false },
            onDismiss = { vipDialog = false },
        )
    }

    openProfileId?.let { id ->
        // Head-to-head: the last 10 siege matches when there are any, else the duel history.
        val siege = siegeRivals.firstOrNull { it.opponentId == id }
        val record = rivals.firstOrNull { it.opponentId == id }
        PlayerProfileSheet(
            backend = backend,
            playerId = id,
            friendships = friendships,
            headToHead = siege?.let { PlayerHeadToHead(minOf(it.matches, 10), it.last10Wins, it.last10Losses) }
                ?: record?.let { PlayerHeadToHead(it.matches, it.wins, it.losses) },
            onMessage = { friend ->
                openProfileId = null
                messageFriend = friend
            },
            onChanged = { scope.launch { reload() } },
            onDismiss = { openProfileId = null },
        )
    }

    messageFriend?.let { friend ->
        DirectMessageSheet(backend = backend, friend = friend) {
            messageFriend = null
            scope.launch { reload() }
        }
    }
}

@Composable
private fun SocialMetric(modifier: Modifier, value: String, label: String, accent: Color) {
    Surface(modifier = modifier, shape = GameShapes.Medium, color = GameColors.PrimarySurface, border = BorderStroke(1.dp, GameColors.Border)) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = GameColors.TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PlayerSearchCard(query: String, onQuery: (String) -> Unit, busy: Boolean, onSearch: () -> Unit) {
    GameSurface {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            GameSectionHeader(gameText("Oyuncu Bul", "Find Player"))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(gameText("En az 2 harf yaz", "Type at least 2 characters")) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                shape = GameShapes.Medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GameColors.TextPrimary,
                    unfocusedTextColor = GameColors.TextPrimary,
                    focusedBorderColor = GameColors.PrimaryBlue,
                    unfocusedBorderColor = GameColors.Border,
                    focusedContainerColor = GameColors.ElevatedBackground,
                    unfocusedContainerColor = GameColors.ElevatedBackground,
                ),
            )
            GamePrimaryButton(
                text = if (busy) "…" else gameText("ARA", "SEARCH"),
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                enabled = query.trim().length >= 2 && !busy,
                icon = Icons.Rounded.Search,
            )
        }
    }
}

@Composable
private fun SearchPlayerRow(player: ProfileDto, relationStatus: String?, busy: Boolean, onOpen: () -> Unit = {}, onAdd: () -> Unit) {
    GameSurface {
        Row(Modifier.clickable(onClick = onOpen).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 44.dp, accent = if (player.isVip) GameColors.PrestigeGold else GameColors.PrimaryBlue, visible = player.avatarVisibility != "hidden")
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(player.displayName, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${ratingLeagueProgress(player.rating).leagueName} • ${player.rating} RP", color = GameColors.TextSecondary, fontSize = 9.sp)
            }
            Button(
                onClick = onAdd,
                enabled = relationStatus == null && !busy,
                shape = GameShapes.Medium,
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.PrimaryBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Text(when (relationStatus) { "accepted" -> gameText("ARKADAŞ", "FRIEND"); "pending" -> gameText("BEKLİYOR", "PENDING"); else -> if (busy) "…" else gameText("EKLE", "ADD") }, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfessionalFriendLock(onUpgrade: () -> Unit) {
    GameEmptyState(
        icon = Icons.Rounded.Lock,
        title = gameText("Arkadaş listesi PRO'ya özel", "Friend list is PRO-only"),
        body = gameText("Mevcut üyelik kuralı korunuyor. PRO ile arkadaş listesini ve yönetimini açabilirsin.", "The current membership rule is preserved. PRO unlocks friend-list management."),
        actionText = gameText("PRO'YU İNCELE", "EXPLORE PRO"),
        onAction = onUpgrade,
    )
}

@Composable
private fun ProfessionalFriendRow(friend: ProfileDto, busy: Boolean, onOpen: () -> Unit, onInvite: () -> Unit, onRemove: () -> Unit) {
    GameSurface {
        Row(Modifier.clickable(onClick = onOpen).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                ProfilePhotoAvatarWithGender(friend.avatarPath, friend.gender, friend.displayName, 46.dp, accent = if (friend.isVip) GameColors.PrestigeGold else GameColors.PrimaryBlue, visible = friend.avatarVisibility != "hidden")
                Box(Modifier.align(Alignment.BottomEnd).size(11.dp).padding(1.dp)) {
                    Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = if (friend.presenceStatus == "online") GameColors.PlayGreen else GameColors.TextSecondary) {}
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.displayName, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${ratingLeagueProgress(friend.rating).leagueName} • ${friend.rating} RP", color = GameColors.TextSecondary, fontSize = 9.sp)
            }
            IconButton(onClick = onInvite, enabled = !busy) {
                Icon(Icons.Rounded.SportsEsports, gameText("Davet et", "Invite"), tint = GameColors.TacticalTurquoise)
            }
            IconButton(onClick = onRemove, enabled = !busy) {
                Icon(Icons.Rounded.PersonRemove, gameText("Arkadaştan çıkar", "Remove friend"), tint = GameColors.Danger)
            }
        }
    }
}

@Composable
private fun FriendRequestRow(player: ProfileDto, busy: Boolean, onDecline: () -> Unit, onAccept: () -> Unit) {
    GameSurface(borderColor = GameColors.PrimaryBlue.copy(alpha = .32f)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(player.avatarPath, player.gender, player.displayName, 44.dp, accent = GameColors.PrimaryBlue, visible = player.avatarVisibility != "hidden")
            Spacer(Modifier.width(9.dp))
            Text(player.displayName, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onDecline, enabled = !busy) { Icon(Icons.Rounded.Close, gameText("Reddet", "Decline"), tint = GameColors.Danger) }
            IconButton(onClick = onAccept, enabled = !busy) { Icon(Icons.Rounded.Check, gameText("Kabul et", "Accept"), tint = GameColors.PlayGreen) }
        }
    }
}

@Composable
private fun GameInviteCard(
    name: String,
    avatarPath: String?,
    title: String,
    language: String,
    accent: Color,
    busy: Boolean,
    onDecline: () -> Unit,
    onAccept: () -> Unit,
) {
    GameSurface(borderColor = accent.copy(alpha = .32f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatar(avatarPath, name, 42.dp, visible = true, accent = accent)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$title • ${if (language == "en") "English" else "Türkçe"}", color = GameColors.TextSecondary, fontSize = 9.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameDangerButton(gameText("REDDET", "DECLINE"), onDecline, Modifier.weight(1f), enabled = !busy)
                GamePrimaryButton(if (busy) "…" else gameText("KABUL ET", "ACCEPT"), onAccept, Modifier.weight(1f), enabled = !busy)
            }
        }
    }
}

@Composable
private fun ArchRivalCard(rival: ArchRivalDto, onOpen: () -> Unit) {
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = GameShapes.Large, color = GameColors.RewardAmber.copy(alpha = .10f), border = BorderStroke(1.dp, GameColors.RewardAmber.copy(alpha = .35f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GameColors.RewardAmber.copy(alpha = .14f)) {
                Icon(Icons.Rounded.Swords, null, tint = GameColors.RewardAmber, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(gameText("EZELİ RAKİP", "ARCH RIVAL"), color = GameColors.RewardAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(rival.displayName, color = GameColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(gameText("Aranızda ${rival.matches} maç: ${rival.wins}–${rival.losses}", "${rival.matches} matches between you: ${rival.wins}–${rival.losses}"), color = GameColors.TextSecondary, fontSize = 10.sp)
            }
            Text("${rival.myPoints}:${rival.theirPoints}", color = GameColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RivalRow(rival: RivalHistoryDto, busy: Boolean, onOpen: () -> Unit, onRematch: () -> Unit) {
    GameSurface {
        Row(Modifier.clickable(onClick = onOpen).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = if (rival.presenceStatus == "online") GameColors.PlayGreen.copy(alpha = .12f) else GameColors.SecondarySurface) {
                Icon(Icons.Rounded.Person, null, tint = if (rival.presenceStatus == "online") GameColors.PlayGreen else GameColors.TextSecondary, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(rival.displayName, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${rival.matches} ${gameText("maç", "matches")} • ${rival.wins}W ${rival.losses}L • ${rival.myPoints}:${rival.theirPoints}", color = GameColors.TextSecondary, fontSize = 8.5.sp)
            }
            GameSecondaryButton(if (busy) "…" else gameText("RÖVANŞ", "REMATCH"), onRematch, enabled = !busy)
        }
    }
}

@Composable
private fun MatchHistoryRow(match: MatchHistoryDto) {
    val won = match.result == "win"
    val draw = match.result == "draw"
    val accent = when { won -> GameColors.PlayGreen; draw -> GameColors.RewardAmber; else -> GameColors.Danger }
    GameSurface {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = GameShapes.Small, color = accent.copy(alpha = .12f)) {
                Text(when { won -> "G"; draw -> "B"; else -> "M" }, Modifier.padding(horizontal = 9.dp, vertical = 7.dp), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(match.displayName, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${match.myScore}-${match.theirScore} • ${if (match.ratingDelta >= 0) "+" else ""}${match.ratingDelta} rating", color = GameColors.TextSecondary, fontSize = 8.5.sp)
            }
            if (match.isFriend) Icon(Icons.Rounded.People, null, tint = GameColors.PrimaryBlue, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun ConversationRow(name: String, preview: String, fromMe: Boolean, enabled: Boolean, onOpen: () -> Unit) {
    GameSurface {
        Row(Modifier.clickable(enabled = enabled, onClick = onOpen).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GameColors.PrimaryBlue.copy(alpha = .14f)) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = GameColors.PrimaryBlue, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (if (fromMe) gameText("Sen: ", "You: ") else "") + preview,
                    color = GameColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GameColors.TextTertiary)
        }
    }
}

@Composable
private fun SiegeRivalRow(rival: SiegeRivalDto, busy: Boolean, onOpen: () -> Unit, onRematch: () -> Unit) {
    GameSurface {
        Row(Modifier.clickable(onClick = onOpen).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GameColors.TacticalTurquoise.copy(alpha = .14f)) {
                Icon(Icons.Rounded.GridView, null, tint = GameColors.TacticalTurquoise, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(rival.displayName, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    gameText(
                        "Son ${minOf(rival.matches, 10)} maç: ${rival.last10Wins}–${rival.last10Losses} • toplam ${rival.matches}",
                        "Last ${minOf(rival.matches, 10)}: ${rival.last10Wins}–${rival.last10Losses} • ${rival.matches} total",
                    ),
                    color = GameColors.TextSecondary,
                    fontSize = 10.sp,
                )
            }
            GameSecondaryButton(if (busy) "…" else gameText("RÖVANŞ", "REMATCH"), onRematch, enabled = !busy)
        }
    }
}
