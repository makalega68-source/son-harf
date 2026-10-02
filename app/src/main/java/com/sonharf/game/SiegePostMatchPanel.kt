package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
internal fun SiegePostMatchPanel(game: WordSiegeGameDto, onReplay: () -> Unit, replayBusy: Boolean, onContinue: (WordSiegeGameDto) -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    val me = backend.currentUserId()
    val opponentId = if (game.playerOneId == me) game.playerTwoId else game.playerOneId
    var friends by remember(game.id) { mutableStateOf<List<ProfileDto>?>(null) }
    var history by remember(game.id) { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var missions by remember(game.id) { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }
    var pro by remember(game.id) { mutableStateOf(false) }
    var loaded by remember(game.id) { mutableStateOf(false) }
    var busy by remember(game.id) { mutableStateOf(false) }
    var notice by remember(game.id) { mutableStateOf<String?>(null) }
    var invitationSent by remember(game.id) { mutableStateOf(false) }
    var pendingInvite by remember(game.id) { mutableStateOf<String?>(null) }
    var retry by remember(game.id) { mutableIntStateOf(0) }
    LaunchedEffect(game.id, retry) {
        coroutineScope {
            pro = gameRequestResult { me?.let { backend.getProfile(it).isVip } ?: false }.getOrDefault(false)
            val f = async { if (pro) gameRequestResult { backend.getFriends() }.getOrNull()?.map { it.second } else emptyList() }
            val h = async { gameRequestResult { if (game.gameMode == "series") backend.getWordSiegeSeriesGames() else backend.getWordSiegeGames() }.getOrNull() }
            val m = async { gameRequestResult { backend.getUnifiedMissions() }.getOrNull() }
            friends = f.await()
            h.await()?.let { history = it }
            missions = m.await().orEmpty().filter { it.modeKey == "word_siege" && it.completed && !it.claimed }
            loaded = true
        }
    }
    val openRematch by rememberUpdatedState(onContinue)
    val foreground = rememberAppForeground()
    LaunchedEffect(pendingInvite, foreground) {
        val id = pendingInvite ?: return@LaunchedEffect
        if (!foreground) return@LaunchedEffect
        while (true) {
            val state = if (game.gameMode == "series") gameRequestResult { backend.getOutgoingWordSiegeSeriesInvites() }
                .getOrNull()?.firstOrNull { it.id == id }?.let { it.status to it.gameId }
            else gameRequestResult { backend.getOutgoingWordSiegeInvites() }
                .getOrNull()?.firstOrNull { it.id == id }?.let { it.status to it.gameId }
            if (state?.first == "accepted" && state.second != null) {
                val ready = gameRequestResult { backend.getWordSiegeGame(requireNotNull(state.second)) }.getOrNull()
                if (ready != null && ready.status == "playing") {
                    WordSiegeLaunchConfig.awaitedInviteIds.remove(id)
                    pendingInvite = null
                    openRematch(ready)
                    return@LaunchedEffect
                }
            }
            if (state?.first in listOf("declined", "expired", "cancelled")) {
                WordSiegeLaunchConfig.awaitedInviteIds.remove(id)
                pendingInvite = null; invitationSent = false
                notice = sh("Rövanş daveti kapandı", "Rematch invitation closed")
                return@LaunchedEffect
            }
            delay(2_000)
        }
    }
    val rivalGames = history.filter { it.status == "finished" && me != null && opponentId != null &&
        setOf(it.playerOneId, it.playerTwoId) == setOf(me, opponentId) }
    val friend = friends?.firstOrNull { it.id == opponentId }
    ActivityTile(sh("BİR MAÇ DAHA", "ONE MORE MATCH")) {
        if (rivalGames.isNotEmpty()) {
            Text(sh("Rakip geçmişi · ${rivalGames.size} maç", "Rival history · ${rivalGames.size} matches"), color = Hf.TextMuted, fontSize = 12.sp)
            Text("${rivalGames.count { it.winnerId == me }} : ${rivalGames.count { it.winnerId == opponentId }}", color = Hf.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
            if (rivalGames.size >= 2) Text(sh("EZELİ RAKİP", "ARCH RIVAL"), color = Hf.Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReplay, enabled = !busy && !replayBusy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Hf.Green)) {
                Text(if (replayBusy) "…" else sh("YENİDEN OYNA", "PLAY AGAIN"), fontSize = 11.sp)
            }
            if (opponentId != null) OutlinedButton(onClick = {
                if (busy) return@OutlinedButton
                busy = true
                scope.launch {
                    try {
                        if (friend != null) {
                            gameRequestResult {
                                if (game.gameMode == "series") {
                                    val invite = backend.inviteFriendToWordSiegeSeries(opponentId, game.language, game.turnDurationMinutes ?: 5)
                                    WordSiegeLaunchConfig.awaitInvite(invite.id)
                                    pendingInvite = invite.id
                                } else {
                                    val invite = backend.inviteFriendToWordSiege(opponentId, game.language)
                                    WordSiegeLaunchConfig.awaitInvite(invite.id)
                                    pendingInvite = invite.id
                                }
                            }.onSuccess { invitationSent = true; notice = sh("Rövanş daveti gönderildi", "Rematch invitation sent") }
                                .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
                        } else {
                            gameRequestResult { backend.sendFriendRequest(opponentId) }
                                .onSuccess { invitationSent = true; notice = sh("Arkadaşlık isteği gönderildi", "Friend request sent") }
                                .onFailure { notice = if (it.message.orEmpty().contains("pro_friend_list_required")) sh("Arkadaş ekleme PRO özelliğidir", "Adding friends requires PRO") else sh("İstek gönderilemedi veya zaten bekliyor", "Request failed or is already pending") }
                        }
                    } finally { busy = false }
                }
            }, enabled = loaded && pro && friends != null && !busy && !replayBusy && !invitationSent, modifier = Modifier.weight(1f)) {
                Text(if (invitationSent) sh("GÖNDERİLDİ", "SENT") else if (friend != null) sh("RÖVANŞ", "REMATCH") else sh("ARKADAŞ EKLE", "ADD FRIEND"), fontSize = 11.sp)
            }
        }
        if (!pro && loaded) Text(sh("Arkadaş davetleri ve rövanş · PRO", "Friend invitations and rematches · PRO"), color = Hf.Gold, fontSize = 11.sp)
        if (pro && loaded && friends == null) TextButton(onClick = { retry++ }) { Text(sh("Arkadaşlar yüklenemedi · Yenile", "Friends unavailable · Retry")) }
        notice?.let { Text(it, color = Hf.TextMuted, fontSize = 12.sp) }
        missions.forEach { mission ->
            Text((if (SonHarfUiState.isEnglish) mission.titleEn else mission.titleTr) + " · +${mission.rewardCoins} Son Coin", color = Hf.Gold, fontSize = 12.sp)
            TextButton(enabled = !busy && !replayBusy, onClick = {
                busy = true
                scope.launch {
                    try {
                        gameRequestResult { backend.claimUnifiedMission(mission.missionId) }
                            .onSuccess { result ->
                                if (result.success) {
                                    notice = sh("+${result.rewardCoins} Son Coin alındı", "+${result.rewardCoins} Son Coins claimed")
                                    missions = missions.filterNot { it.missionId == mission.missionId }
                                } else notice = sh("Ödül artık alınabilir değil", "Reward is no longer claimable")
                            }.onFailure { notice = sh("Ödül alınamadı; tekrar dene", "Reward could not be claimed; retry") }
                    } finally { busy = false }
                }
            }) { Text(sh("ÖDÜLÜ AL", "CLAIM REWARD")) }
        }
    }
    HomeLeague(backend)
}
