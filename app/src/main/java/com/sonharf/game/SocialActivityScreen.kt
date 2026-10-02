package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun SocialActivityScreen(backend: OnlineGameBackend, onBack: () -> Unit,
    onFriends: () -> Unit, onOpenGame: (WordSiegeGameDto) -> Unit, onOpenTarget: (String, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val foreground = rememberAppForeground()
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var friends by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var requests by remember { mutableIntStateOf(0) }
    var legacy by remember { mutableIntStateOf(0) }
    var invites by remember { mutableStateOf<List<WordSiegeInviteDto>>(emptyList()) }
    var series by remember { mutableStateOf<List<WordSiegeSeriesInviteDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var error by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val me = backend.currentUserId()
    suspend fun reload() = coroutineScope {
        error = false
        val classicTask = async { gameRequestResult { backend.getWordSiegeGames() } }
        val seriesTask = async { gameRequestResult { backend.getWordSiegeSeriesGames() } }
        val inviteTask = async { gameRequestResult { backend.getIncomingWordSiegeInvites() } }
        val fastInviteTask = async { gameRequestResult { backend.getIncomingWordSiegeSeriesInvites() } }
        val requestTask = async { gameRequestResult { backend.getIncomingFriendRequests() } }
        val legacyTask = async { gameRequestResult { backend.getIncomingGameInvites() } }
        gameRequestResult { backend.getFriends() }.onSuccess { friends = it.map { it.second } }.onFailure { error = true }
        val a = classicTask.await(); val b = seriesTask.await()
        games = (a.getOrElse { games.filter { it.gameMode == "classic" } } + b.getOrElse { games.filter { it.gameMode == "series" } }).distinctBy { it.id }
            .filter { me != null && me in listOf(it.playerOneId, it.playerTwoId) }
        inviteTask.await().onSuccess { invites = it }.onFailure { error = true }
        fastInviteTask.await().onSuccess { series = it }.onFailure { error = true }
        requestTask.await().onSuccess { requests = it.size }.onFailure { error = true }
        legacyTask.await().onSuccess { legacy = it.size }.onFailure { error = true }
        error = error || a.isFailure || b.isFailure
        (invites.map { it.senderId } + series.map { it.senderId } + games.mapNotNull { if (it.playerOneId == me) it.playerTwoId else it.playerOneId })
            .distinct().filterNot(profiles::containsKey).map { id -> async {
                gameRequestResult { backend.getProfile(id) }.getOrNull()?.let { id to it }
            } }.forEach { it.await()?.let { row -> profiles = profiles + row } }
        loading = false
    }
    LaunchedEffect(me, foreground, retry) {
        if (me == null || !foreground) return@LaunchedEffect
        while (true) { if (busy == null) reload(); delay(15_000) }
    }
    fun respond(id: String, fast: Boolean, accept: Boolean) {
        if (busy != null) return
        busy = id
        scope.launch {
            try {
                val result = gameRequestResult {
                    if (fast) backend.respondWordSiegeSeriesInvite(id, accept) else backend.respondWordSiegeInvite(id, accept)
                }
                result.onSuccess { game ->
                    if (accept && game != null) onOpenGame(game) else {
                        if (accept) notice = sh("Davetin süresi doldu", "Invitation expired")
                        reload()
                    }
                }.onFailure { notice = sh("Davet işlenemedi; yenileyip tekrar dene", "Could not process invitation; refresh and retry"); reload() }
            } finally { busy = null }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { MainScreenHeader(sh("Aktivite", "Activity"), "", onBack = onBack) }
        item { SocialInboxHistory(backend, onOpenTarget) }
        item { TextButton(onClick = { retry++ }, enabled = busy == null) { Text(sh("YENİLE", "REFRESH")) } }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Green) }
        if (error) item { Text(sh("Bazı aktiviteler güncellenemedi", "Some activity could not be refreshed"), color = Hf.Red) }
        notice?.let { item { Text(it, color = Hf.TextMuted) } }
        if (requests + legacy > 0) item {
            ActivityTile(sh("$requests arkadaşlık isteği · $legacy Son Harf daveti", "$requests friend requests · $legacy Last Letter invitations")) {
                TextButton(onClick = onFriends) { Text(sh("İSTEKLERİ AÇ", "OPEN REQUESTS")) }
            }
        }
        items(invites, key = { "invite:${it.id}" }) { invite ->
            ActivityTile(sh("${profiles[invite.senderId]?.displayName ?: "Oyuncu"} sana meydan okudu", "${profiles[invite.senderId]?.displayName ?: "Player"} challenged you")) {
                InviteActions(busy != null, { respond(invite.id, false, false) }, { respond(invite.id, false, true) })
            }
        }
        items(series, key = { "series:${it.id}" }) { invite ->
            ActivityTile(sh("${profiles[invite.senderId]?.displayName ?: "Oyuncu"} · Hızlı Düello daveti", "${profiles[invite.senderId]?.displayName ?: "Player"} · Quick Duel invitation")) {
                InviteActions(busy != null, { respond(invite.id, true, false) }, { respond(invite.id, true, true) })
            }
        }
        val turns = homeGames(games, me).filter { homeTurn(it, me) == HomeTurn.YOURS }
        items(turns, key = { "turn:${it.id}" }) { game ->
            val opponent = if (game.playerOneId == me) game.playerTwoId else game.playerOneId
            ActivityTile(sh("Sıra sende · ${profiles[opponent]?.displayName ?: "Rakip"}", "Your turn · ${profiles[opponent]?.displayName ?: "Rival"}")) {
                TextButton(onClick = { onOpenGame(game) }) { Text(sh("MAÇA DEVAM ET", "CONTINUE MATCH")) }
            }
        }
        items(friends.filter { it.presenceStatus == "online" }, key = { "friend:${it.id}" }) { friend ->
            ActivityTile(sh("${friend.displayName} çevrimiçi", "${friend.displayName} is online")) {
                TextButton(onClick = onFriends) { Text(sh("ARKADAŞLARI AÇ", "OPEN FRIENDS")) }
            }
        }
        items(games.filter { it.status == "finished" }.sortedByDescending { it.finishedAt ?: it.updatedAt }.take(10), key = { "result:${it.id}" }) { game ->
            val opponent = if (game.playerOneId == me) game.playerTwoId else game.playerOneId
            ActivityTile(sh("Maç bitti · ${profiles[opponent]?.displayName ?: "Rakip"}", "Match finished · ${profiles[opponent]?.displayName ?: "Rival"}")) {
                Text(socialDate(game.finishedAt ?: game.updatedAt), color = Hf.TextMuted, fontSize = 11.sp)
                TextButton(onClick = { onOpenGame(game) }) { Text(sh("SONUÇ VE RÖVANŞ", "RESULT & REMATCH")) }
            }
        }
        if (!loading && !error && requests + legacy == 0 && invites.isEmpty() && series.isEmpty() && turns.isEmpty() && friends.none { it.presenceStatus == "online" } && games.none { it.status == "finished" }) item {
            Text(sh("Yeni aktivite yok", "No new activity"), color = Hf.TextMuted)
        }
    }
}

@Composable
private fun InviteActions(busy: Boolean, onDecline: () -> Unit, onAccept: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDecline, enabled = !busy) { Text(sh("REDDET", "DECLINE"), color = Hf.Red) }
        Button(onClick = onAccept, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Hf.Green)) { Text(sh("KABUL ET", "ACCEPT")) }
    }
}

@Composable
internal fun ActivityTile(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Hf.Surface, border = BorderStroke(1.dp, Hf.Border)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Hf.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            content()
        }
    }
}
