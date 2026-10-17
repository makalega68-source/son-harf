package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class MatchListRow(
    val id: String, val kind: String, val rivalId: String?, val rivalFallback: String,
    val mine: Int, val theirs: Int, val finished: Boolean, val won: Boolean?,
    val turn: HomeTurn?, val timer: String, val date: String,
)

internal fun matchListRows(games: List<WordSiegeGameDto>, rooms: List<GameRoomDto>, me: String?): List<MatchListRow> {
    if (me == null) return emptyList()
    val siege = games.distinctBy { it.id }.filter { me in listOf(it.playerOneId,it.playerTwoId) && it.status in setOf("waiting","playing","finished") }.map { g ->
        val first = g.playerOneId == me
        val one = g.playerOneWordScore + g.playerOneAreaScore
        val two = g.playerTwoWordScore + g.playerTwoAreaScore
        MatchListRow(g.id, if (g.gameMode == "series") "series" else "siege", if (first) g.playerTwoId else g.playerOneId,
            sh("Rakip bekleniyor", "Waiting for rival"), if (first) one else two, if (first) two else one,
            g.status == "finished", g.winnerId?.let { it == me }, homeTurn(g, me),
            if (g.gameMode == "series") "${g.turnDurationMinutes ?: 5} ${sh("dk", "min")}" else "${g.turnDurationHours} ${sh("saat", "hours")}",
            if (g.status == "finished") g.finishedAt ?: g.updatedAt else g.turnDeadline ?: g.updatedAt)
    }
    // A Son Harf search that never found a rival is not a game: hide it after 15 minutes.
    val staleBefore = System.currentTimeMillis() - 15 * 60_000L
    val last = rooms.distinctBy { it.id }.filter { me in listOf(it.hostId,it.guestId) && it.status in setOf("waiting","playing","quiz","final","sudden_death","paused","finished") }
        .filterNot { it.status == "waiting" && it.guestId == null && !it.isBot &&
            (runCatching { com.sonharf.game.data.requireServerInstant(it.createdAt).toEpochMilli() }.getOrNull() ?: Long.MAX_VALUE) < staleBefore }
        .map { g ->
        val host = g.hostId == me
        MatchListRow(g.id, "son_harf", if (host) g.guestId else g.hostId,
            if (g.isBot) g.botName ?: "AI" else sh("Rakip bekleniyor", "Waiting for rival"),
            if (host) g.hostScore else g.guestScore, if (host) g.guestScore else g.hostScore,
            g.status == "finished", if (g.winnerIsBot) false else g.winnerId?.let { it == me },
            when { g.status == "finished" -> null; g.status in setOf("waiting","paused") -> HomeTurn.WAITING
                g.currentPlayerId == me && !g.botTurn -> HomeTurn.YOURS; else -> HomeTurn.RIVAL },
            "Son Harf", g.createdAt)
    }
    return (siege + last).sortedWith(compareBy<MatchListRow> { if (it.turn == HomeTurn.YOURS) 0 else 1 }.thenByDescending { it.date })
}

@Composable
internal fun MyGamesScreen(backend: OnlineGameBackend, onOpen: (String, String) -> Unit, initialTab: Int = 0) {
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<GameRoomDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String,ProfileDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val me = backend.currentUserId()
    val foreground = rememberAppForeground()
    LaunchedEffect(me, foreground, retry) {
        if (!foreground) return@LaunchedEffect
        while (true) {
            coroutineScope {
                val a = async { gameRequestResult { backend.getWordSiegeGameSummaries("classic") } }
                val b = async { gameRequestResult { backend.getWordSiegeGameSummaries("series") } }
                val c = async { gameRequestResult { backend.getLastLetterRooms() } }
                val classic=a.await(); val series=b.await(); val last=c.await()
                games=classic.getOrElse { games.filter { it.gameMode != "series" } } + series.getOrElse { games.filter { it.gameMode == "series" } }
                last.onSuccess { rooms=it }
                failed=classic.isFailure || series.isFailure || last.isFailure
                loading=false
                val ids=matchListRows(games,rooms,me).mapNotNull { it.rivalId }.distinct().filterNot(profiles::containsKey)
                profiles=profiles + backend.getProfilesParallel(ids).associateBy { it.id }

            }
            delay(15_000)
        }
    }
    val matches=matchListRows(games,rooms,me)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(horizontal=16.dp,vertical=10.dp)) {
            MainScreenHeader(sh("Oyunlarım","My games"), "", actionIcon=Icons.Rounded.Refresh,
                actionDescription=sh("Yenile","Refresh"),onAction={retry++})
        }
        TabRow(selectedTabIndex=tab,containerColor=if(SonHarfCosmetics.petrolMenus)Color.Transparent else LobbyPalette.Ground,contentColor=LobbyPalette.Accent) {
            listOf(sh("Aktif","Active"),sh("Biten","Finished"),sh("Davet","Invites")).forEachIndexed { i,label ->
                Tab(selected=tab==i,onClick={tab=i},text={Text(label,fontSize=16.sp,fontWeight=FontWeight.Bold)})
            }
        }
        if (tab==2) {
            GameInvitesScreen(backend,onOpen)
        } else {
            val shown=matches.filter { it.finished == (tab==1) }
            LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=16.dp)) {
                if(loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if(failed) item { TextButton(onClick={retry++}) { Text(sh("Maçlar yenilenemedi · Tekrar dene","Games unavailable · Retry")) } }
                if(!loading && !failed && shown.isEmpty()) item { Text(if(tab==0)sh("Devam eden oyunun yok.","No active games.")else sh("Henüz biten oyunun yok.","No finished games yet."),Modifier.padding(24.dp),color=LobbyPalette.Muted) }
                items(shown,key={"${it.kind}:${it.id}"}) { match ->
                    CompactMatchRow(match,profiles[match.rivalId]) { onOpen(match.kind,match.id) }
                    HorizontalDivider(color=LobbyPalette.Line.copy(alpha=.6f))
                }
            }
        }
    }
}

/** "Kazandın" with a lower score means the rival ran out of time or resigned; say so. */
internal fun matchResultLabel(match: MatchListRow): String = when {
    match.won == true && match.mine <= match.theirs -> sh("Kazandın\n(hükmen)", "Won\n(forfeit)")
    match.won == true -> sh("Kazandın", "Won")
    match.won == false && match.mine >= match.theirs -> sh("Kaybettin\n(süre)", "Lost\n(time)")
    match.won == false -> sh("Kaybettin", "Lost")
    match.mine == match.theirs -> sh("Berabere", "Draw")
    else -> sh("Bitti", "Finished")
}

@Composable
internal fun CompactMatchRow(match: MatchListRow, rival: ProfileDto?, onClick: () -> Unit) {
    val accent=if(match.finished && match.won==false) Hf.Red else LobbyPalette.Accent
    Row(Modifier.fillMaxWidth().background(if(match.turn==HomeTurn.YOURS)LobbyPalette.Soft else LobbyPalette.Paper)
        .clickable(onClick=onClick).padding(horizontal=16.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        FramedProfilePhotoAvatar(avatarPath=rival?.avatarPath,gender=rival?.gender,name=rival?.displayName ?: match.rivalFallback,
            size=58.dp,frameId=rememberPlayerFrame(rival?.id),visible=rival?.avatarVisibility!="hidden",isPro=rival?.isVip==true)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(rival?.displayName ?: match.rivalFallback,Modifier.weight(1f,fill=false),color=LobbyPalette.Ink,fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                AdminBadge(rival?.id,16.dp)
            }
            Text(sh("Sen ${match.mine} · Rakip ${match.theirs}","You ${match.mine} · Rival ${match.theirs}"),color=LobbyPalette.Ink,fontSize=14.sp)
            Text((when(match.kind){"son_harf"->"Son Harf";"series"->sh("Hızlı Düello","Quick Duel");else->sh("Kuşatma","Siege")})+" · "+match.timer,color=LobbyPalette.Muted,fontSize=12.sp)
            Text(if(match.finished) socialDate(match.date) else when(match.turn){HomeTurn.YOURS->sh("Sıra sende","Your turn");HomeTurn.RIVAL->sh("Rakibin sırası","Their turn");else->sh("Rakip bekleniyor","Waiting for rival")},color=accent,fontSize=12.sp)
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(if(!match.finished)Icons.Rounded.ChevronRight else if(match.won==true)Icons.Rounded.CheckCircle else if(match.won==false)Icons.Rounded.Cancel else Icons.Rounded.RemoveCircle,
                null,tint=accent,modifier=Modifier.size(28.dp))
            if(match.finished) Text(matchResultLabel(match),color=accent,fontSize=11.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
internal fun GameInvitesScreen(backend: OnlineGameBackend, onOpen: (String,String)->Unit) {
    data class Invite(val id:String,val kind:String,val sender:String)
    var invites by remember { mutableStateOf<List<Invite>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String,ProfileDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    val scope=rememberCoroutineScope()
    val foreground=rememberAppForeground()
    LaunchedEffect(foreground,retry) {
        if(!foreground)return@LaunchedEffect
        while(true) {
            gameRequestResult { coroutineScope {
                // The three invite lists load together; sender profiles come in one batched query.
                val siege=async { backend.getIncomingWordSiegeInvites().map { Invite(it.id,"siege",it.senderId) } }
                val series=async { backend.getIncomingWordSiegeSeriesInvites().map { Invite(it.id,"series",it.senderId) } }
                val last=async { backend.getIncomingGameInvites().map { Invite(it.id,"son_harf",it.senderId) } }
                val next=siege.await()+series.await()+last.await()
                val missing=next.map { it.sender }.distinct().filterNot(profiles::containsKey)
                if(missing.isNotEmpty()) profiles=profiles+backend.getProfilesParallel(missing).associateBy { it.id }
                next
            } }.onSuccess { invites=it;error=null }.onFailure { error=sh("Davetler alınamadı. Yeniden dene.","Could not load invitations. Retry.") }
            loading=false;delay(15_000)
        }
    }
    fun respond(i:Invite,accept:Boolean) {
        if(busy!=null)return
        busy=i.id
        scope.launch {
            gameRequestResult {
                when(i.kind) {
                    "siege"->backend.respondWordSiegeInvite(i.id,accept)?.let { if(accept)onOpen("siege",it.id) }
                    "series"->backend.respondWordSiegeSeriesInvite(i.id,accept)?.let { if(accept)onOpen("series",it.id) }
                    else->backend.respondGameInvite(i.id,accept)?.let { if(accept)onOpen("son_harf",it.id) }
                }
            }.onSuccess { invites=invites.filterNot { it.id==i.id && it.kind==i.kind };retry++ }
                .onFailure { error=sh("Davet yanıtlanamadı. Tekrar dene.","Could not respond. Try again.") }
            busy=null
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(loading)item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { item { TextButton(onClick={retry++}) { Text(it,color=Hf.Red) } } }
        if(!loading && error==null && invites.isEmpty())item { Text(sh("Bekleyen oyun davetin yok.","No pending game invitations."),color=LobbyPalette.Muted) }
        items(invites,key={"${it.kind}:${it.id}"}) { i -> LobbyCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(profiles[i.sender]?.displayName ?: sh("Oyuncu","Player"),fontWeight=FontWeight.Bold)
                Text(when(i.kind){"son_harf"->"Son Harf";"series"->sh("Hızlı Düello","Quick Duel");else->sh("Kuşatma","Siege")},color=LobbyPalette.Muted)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(enabled=busy==null,onClick={respond(i,false)}) { Text(sh("Reddet","Decline")) }
                    Button(enabled=busy==null,onClick={respond(i,true)}) { Text(sh("Kabul et","Accept")) }
                }
            }
        } }
    }
}
