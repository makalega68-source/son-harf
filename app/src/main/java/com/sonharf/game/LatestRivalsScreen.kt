package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class LatestRivalMatch(val opponentId: String, val name: String, val mine: Int, val theirs: Int,
    val date: String, val mode: String, val siege: WordSiegeGameDto? = null)

internal fun latestRivalMatches(games: List<WordSiegeGameDto>, history: List<MatchHistoryDto>, me: String?): List<LatestRivalMatch> {
    if (me == null) return emptyList()
    val siege = games.filter { it.status == "finished" && me in listOf(it.playerOneId, it.playerTwoId) }.mapNotNull { g ->
        val first = g.playerOneId == me
        val opponent = (if (first) g.playerTwoId else g.playerOneId)?.takeIf { it != me } ?: return@mapNotNull null
        LatestRivalMatch(opponent, "", if(first) g.playerOneWordScore+g.playerOneAreaScore else g.playerTwoWordScore+g.playerTwoAreaScore,
            if(first) g.playerTwoWordScore+g.playerTwoAreaScore else g.playerOneWordScore+g.playerOneAreaScore,
            g.finishedAt ?: g.updatedAt, if(g.gameMode=="series") "series" else "siege",g)
    }
    return (siege + history.map { LatestRivalMatch(it.opponentId,it.displayName,it.myScore,it.theirScore,it.playedAt,it.mode) })
        .sortedByDescending { runCatching { com.sonharf.game.data.requireServerInstant(it.date).toEpochMilli() }.getOrDefault(0L) }
        .distinctBy { it.opponentId }
}

@Composable
internal fun LatestRivalsScreen(backend: OnlineGameBackend, onBack: (() -> Unit)? = null,
    onOpenSiege: ((WordSiegeGameDto) -> Unit)? = null) {
    var rows by remember { mutableStateOf<List<LatestRivalMatch>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String,ProfileDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    val scope=rememberCoroutineScope()
    val foreground=rememberAppForeground()
    LaunchedEffect(foreground,retry) {
        if(!foreground) return@LaunchedEffect
        while(true) {
            gameRequestResult { coroutineScope {
                val classic=async { backend.getWordSiegeGameSummaries("classic") }
                val series=async { backend.getWordSiegeGameSummaries("series") }
                val history=async { backend.getMatchHistory(50) }
                rows=latestRivalMatches(classic.await()+series.await(),history.await(),backend.currentUserId())
                loading=false
                // Cached rival profiles are reused; each refresh only asks for rivals not seen yet.
                val nextProfiles=profiles.toMutableMap()
                val missing=rows.map { it.opponentId }.filter { userId -> !nextProfiles.containsKey(userId) }
                backend.getProfilesParallel(missing).forEach { nextProfiles[it.id]=it }
                profiles=nextProfiles
            } }.onSuccess { failed=false }.onFailure { failed=true }
            loading=false
            delay(30_000)
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { MainScreenHeader(sh("Rakip geçmişi","Rival history"),sh("Her rakiple son karşılaşman","Your latest match with each rival"),onBack=onBack) }
        if(loading)item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if(failed)item { TextButton(onClick={retry++}) { Text(sh("Geçmiş yüklenemedi · Yenile","History unavailable · Retry")) } }
        notice?.let { item { Text(it,color=LobbyPalette.Muted,fontSize=12.sp) } }
        if(!loading && !failed && rows.isEmpty())item { Text(sh("Henüz rakip geçmişin yok.","No rival history yet."),color=LobbyPalette.Muted) }
        items(rows,key={it.opponentId}) { row ->
            val profile=profiles[row.opponentId]
            LobbyCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    FramedProfilePhotoAvatar(profile?.avatarPath,profile?.gender,profile?.displayName ?: row.name,52.dp,
                        rememberPlayerFrame(row.opponentId),visible=profile?.avatarVisibility!="hidden",isPro=profile?.isVip==true)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(profile?.displayName ?: row.name.ifBlank { sh("Rakip","Rival") },Modifier.weight(1f,fill=false),fontWeight=FontWeight.Bold,maxLines=1)
                            AdminBadge(row.opponentId,16.dp)
                        }
                        Text("${row.mine} : ${row.theirs}",fontSize=18.sp,fontWeight=FontWeight.Bold,color=LobbyPalette.Accent)
                        Text((when(row.mode){"siege"->sh("Kuşatma","Siege");"series"->sh("Hızlı Düello","Quick Duel");"arena"->sh("Kelime Arenası","Word Arena");else->"Son Harf"})+" · "+socialDate(row.date),fontSize=11.sp,color=LobbyPalette.Muted)
                    }
                    TextButton(enabled=busy==null,onClick={
                        if(row.siege!=null && onOpenSiege!=null) onOpenSiege(row.siege)
                        else scope.launch {
                            busy=row.opponentId
                            gameRequestResult { backend.inviteFriendToWordSiege(row.opponentId,SonHarfUiState.language) }
                                .onSuccess { notice=sh("Rövanş daveti gönderildi.","Rematch invitation sent.") }
                                .onFailure { notice=sh("Davet gönderilemedi. Arkadaşlığınızı ve bekleyen davetlerini kontrol et.","Could not invite. Check friendship and pending invitations.") }
                            busy=null
                        }
                    }) { Text(if(row.siege!=null && onOpenSiege!=null) sh("Sonuç","Result") else sh("Rövanş","Rematch"),fontSize=12.sp) }
                }
            }
        }
    }
}
