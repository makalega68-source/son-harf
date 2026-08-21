package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonharf.game.data.*
import com.sonharf.game.ui.home.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private object V6C {
    val bg = Color(0xFFF8FAFC)
    val white = Color.White
    val blue = Color(0xFF0284C7)
    val blueDark = Color(0xFF0369A1)
    val blueLight = Color(0xFFE0F2FE)
    val text = Color(0xFF0F172A)
    val muted = Color(0xFF64748B)
    val border = Color(0xFFCBD5E1)
    val amber = Color(0xFFD97706)
    val green = Color(0xFF16A34A)
    val red = Color(0xFFDC2626)
    val fire = Color(0xFFEA580C)
}

@Serializable
private data class V6Profile(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("allow_match_chat") val allowMatchChat: Boolean = true,
    val diamonds: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
)

private suspend fun v6Profile(id: String): V6Profile? =
    SupabaseProvider.client.from("profiles").select { filter { eq("id", id) } }.decodeList<V6Profile>().firstOrNull()

// -----------------------------------------------------------------------------
// HOME: same V3 light UI, but avatar paths are converted to authenticated signed URLs.
// -----------------------------------------------------------------------------
@Composable
fun V6HomeRoute(
    onStartGameMode: (String) -> Unit,
    onOpenLeague: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val backend = remember { OnlineGameBackend() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(FullHomeUiState()) }
    var showVip by remember { mutableStateOf(false) }

    suspend fun reload(spinner: Boolean) {
        if (spinner) state = state.copy(isLoading = true)
        val uid = backend.currentUserId() ?: run {
            state = state.copy(isLoading = false, notice = "Oturum bulunamadı.")
            return
        }
        val p = runCatching { v6Profile(uid) }.getOrNull()
        val growth = runCatching { backend.getGrowthDashboard() }.getOrNull()
        val ownAvatar = AvatarSignedUrl.resolve(p?.avatarPath)
        val board = runCatching { backend.getLeaderboardV3("tr", "week", 3) }.getOrDefault(emptyList())
        val top = board.mapIndexed { i, row ->
            TopPlayerUiModel(i + 1, row.displayName, row.wins, AvatarSignedUrl.resolve(row.avatarUrl))
        }
        val friends = runCatching { backend.getFriends() }.getOrDefault(emptyList())
        val goals = runCatching { backend.getGoals() }.getOrDefault(emptyList())
        state = FullHomeUiState(
            userName = p?.displayName ?: growth?.displayName ?: "Son Harf Oyuncusu",
            userPhotoUrl = ownAvatar,
            level = growth?.level ?: 1,
            diamonds = p?.diamonds ?: 0,
            league = "${growth?.leagueName ?: "BRONZ"} Lig",
            topPlayers = top,
            isDailyRewardAvailable = growth?.dailyClaimed == false,
            dailyRewardDiamonds = growth?.dailyReward ?: 40,
            onlineFriendsCount = friends.count { it.second.presenceStatus == "online" },
            tasks = goals.map { DailyTaskUiModel(it.id, it.titleTr, it.progress, it.target, it.rewardDiamonds, it.claimed) },
            isLoading = false,
            isActionBusy = state.isActionBusy,
            notice = state.notice,
        )
    }

    LaunchedEffect(Unit) {
        reload(true)
        while (isActive) { delay(15_000); reload(false) }
    }

    FullHomeScreen(
        state = state,
        onStartGameMode = onStartGameMode,
        onClaimDailyReward = {
            if (!state.isActionBusy) scope.launch {
                state = state.copy(isActionBusy = true, notice = "")
                runCatching { backend.claimDailyCheckin() }
                    .onSuccess { state = state.copy(notice = if (it > 0) "+$it elmas hesabına işlendi." else "Bugünkü ödül zaten alınmış.") }
                    .onFailure { state = state.copy(notice = "Günlük ödül alınamadı.") }
                reload(false); state = state.copy(isActionBusy = false)
            }
        },
        onOpenVipModal = { showVip = true },
        onInviteFriend = { SonHarfShare.challenge(context, state.userName) },
        onOpenFriendsList = { FriendsQuickAccessState.open = true },
        onOpenLeaderboard = onOpenLeague,
        onOpenProfile = onOpenProfile,
        onClaimTaskReward = { goalId -> scope.launch {
            state = state.copy(isActionBusy = true, notice = "")
            runCatching { backend.claimGoal(goalId) }
                .onSuccess { state = state.copy(notice = "Görev ödülü hesabına işlendi.") }
                .onFailure { state = state.copy(notice = "Görev ödülü alınamadı.") }
            reload(false); state = state.copy(isActionBusy = false)
        } },
    )
    if (showVip) VipPurchaseDialog(onVerified = { scope.launch { reload(false) } }, onDismiss = { showVip = false })
}

// -----------------------------------------------------------------------------
// BATTLE: resilient polling, working keyboard, working chat, signed avatars.
// -----------------------------------------------------------------------------
@Composable
fun V6BattleScreen(onLeaveBattle: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var meProfile by remember { mutableStateOf<V6Profile?>(null) }
    var opponent by remember { mutableStateOf<V6Profile?>(null) }
    var meAvatar by remember { mutableStateOf<String?>(null) }
    var oppAvatar by remember { mutableStateOf<String?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var matching by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    suspend fun loadMe() {
        val id = backend.currentUserId() ?: return
        meProfile = v6Profile(id)
        meAvatar = AvatarSignedUrl.resolve(meProfile?.avatarPath)
    }
    suspend fun loadOpponent(r: GameRoomDto) {
        if (r.isBot) { opponent = null; oppAvatar = null; return }
        val me = backend.currentUserId()
        val id = if (r.hostId == me) r.guestId else r.hostId
        opponent = id?.let { v6Profile(it) }
        oppAvatar = AvatarSignedUrl.resolve(opponent?.avatarPath)
    }
    suspend fun findActive(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting","playing","quiz","final","sudden_death","paused") }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        loadMe()
        room = runCatching { findActive() }.getOrNull()
        room?.let { loadOpponent(it) }
    }

    val active = room
    if (active == null) {
        V6Lobby(meProfile, meAvatar, matching, notice, onLeaveBattle,
            onRandom = {
                scope.launch {
                    busy = true
                    runCatching { backend.startRandomMatchmaking("tr") }
                        .onSuccess {
                            matching = true; notice = "Rakip aranıyor…"
                            while (matching && room == null) {
                                val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                                if (found != null) { room = found; loadOpponent(found); matching = false; break }
                                delay(800)
                            }
                        }.onFailure { notice = "Eşleşme başlatılamadı." }
                    busy = false
                }
            },
            onCancel = { scope.launch { matching = false; runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi" } },
        )
        return
    }

    val me = backend.currentUserId()

    // Never-ending resilient poller: a temporary network failure no longer kills the screen state.
    LaunchedEffect(active.id) {
        while (isActive) {
            runCatching { backend.getRoom(active.id) }.onSuccess { fresh ->
                room = fresh
                runCatching { loadOpponent(fresh) }
            }
            runCatching { backend.getWords(active.id) }.onSuccess { words = it }
            runCatching { backend.getChat(active.id) }.onSuccess { chat = it }
            delay(700)
        }
    }

    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) {
        val last = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
        input = if (active.currentPlayerId == me && last != null) last.toString() else ""
    }

    LaunchedEffect(active.id, active.botTurn, active.status, active.validWordCount) {
        if (active.isBot && active.botTurn && active.status in listOf("playing","final","sudden_death")) {
            delay(800)
            runCatching { backend.botTakeTurn(active.id) }
                .onSuccess { room = it }
                .onFailure { notice = "Bot sırası yenileniyor…" }
        }
    }

    V6Arena(
        room = active,
        me = me,
        meName = meProfile?.displayName ?: "Sen",
        meAvatar = meAvatar,
        oppName = if (active.isBot) active.botName ?: "KelimeBot" else opponent?.displayName ?: "Rakip",
        oppAvatar = oppAvatar,
        words = words,
        input = input,
        notice = notice,
        busy = busy,
        onInput = { input = it.take(40) },
        onSubmit = {
            val submitted = input.trim()
            if (submitted.length < 2) return@V6Arena
            scope.launch {
                busy = true
                runCatching { backend.submitWord(active.id, submitted) }
                    .onSuccess { room = it; input = ""; notice = "${submitted.uppercase()} kabul edildi" }
                    .onFailure { e ->
                        notice = when {
                            "not_your_turn" in e.message.orEmpty() -> "Sıra rakibinde."
                            "wrong_start_letter" in e.message.orEmpty() -> "Kelime doğru harfle başlamalı."
                            "word_already_used" in e.message.orEmpty() -> "Bu kelime daha önce kullanıldı."
                            "not_in_dictionary" in e.message.orEmpty() -> "Kelime sözlükte bulunamadı."
                            else -> "Kelime gönderilemedi. Tekrar dene."
                        }
                    }
                busy = false
            }
        },
        onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
        onExit = onLeaveBattle,
        onChat = { showChat = true },
    )

    if (showChat) {
        V6Chat(
            messages = chat,
            me = me,
            enabled = meProfile?.allowMatchChat != false,
            onDismiss = { showChat = false },
            onSend = { text ->
                scope.launch {
                    val clean = text.trim().take(300)
                    if (clean.isBlank()) return@launch
                    runCatching { backend.sendChat(active.id, clean) }
                        .onSuccess {
                            chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                            notice = "Mesaj gönderildi."
                        }
                        .onFailure { notice = "Mesaj gönderilemedi. Sohbet iznini kontrol et." }
                }
            },
        )
    }
}

@Composable
private fun V6Lobby(profile: V6Profile?, avatar: String?, matching: Boolean, notice: String, onBack: () -> Unit, onRandom: () -> Unit, onCancel: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(V6C.bg), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") }; Spacer(Modifier.weight(1f)); Text("SON HARF", fontWeight = FontWeight.Black, fontSize = 22.sp, color = V6C.text); Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp)) } }
        item { Surface(shape = RoundedCornerShape(18.dp), color = V6C.white, border = BorderStroke(1.dp,V6C.border)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { V6Avatar(avatar, profile?.displayName ?: "Oyuncu",54); Spacer(Modifier.width(12.dp)); Column { Text(profile?.displayName ?: "Oyuncu",fontWeight=FontWeight.Bold,color=V6C.text); Text(if(matching)"Rakip aranıyor…" else "Düelloya hazırsın",color=V6C.muted) } } } }
        if (matching) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color=V6C.blue) } }
            item { OutlinedButton(onClick=onCancel,modifier=Modifier.fillMaxWidth().height(56.dp),border=BorderStroke(1.dp,V6C.red)){Text("EŞLEŞMEYİ İPTAL ET",color=V6C.red)} }
        } else item { Button(onClick=onRandom,modifier=Modifier.fillMaxWidth().height(64.dp),colors=ButtonDefaults.buttonColors(containerColor=V6C.blue),shape=RoundedCornerShape(16.dp)){Icon(Icons.Rounded.Bolt,null);Spacer(Modifier.width(8.dp));Text("1v1 HIZLI KARŞILAŞMA",fontWeight=FontWeight.Black)} }
        item { Text(notice,Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=V6C.muted,fontSize=12.sp) }
    }
}

@Composable
private fun V6Arena(room: GameRoomDto, me: String?, meName: String, meAvatar: String?, oppName: String, oppAvatar: String?, words: List<GameWordDto>, input: String, notice: String, busy: Boolean, onInput:(String)->Unit, onSubmit:()->Unit, onForfeit:()->Unit, onExit:()->Unit, onChat:()->Unit) {
    val host = me == room.hostId
    val myScore = if(host) room.hostScore else room.guestScore
    val oppScore = if(host) room.guestScore else room.hostScore
    val streak = if(host) room.hostStreak else room.guestStreak
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing","final","sudden_death")
    val lastWord = words.lastOrNull()?.word?.uppercase().orEmpty()
    val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }
    LaunchedEffect(room.turnDeadline,room.currentPlayerId,room.status) {
        while(isActive && room.turnDeadline!=null && room.status in listOf("playing","final","sudden_death")) {
            seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond-Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            delay(1000)
        }
    }
    Column(Modifier.fillMaxSize().background(V6C.bg).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick=onForfeit,border=BorderStroke(1.dp,V6C.red)){Icon(Icons.Rounded.Flag,null,tint=V6C.red);Spacer(Modifier.width(4.dp));Text("Pes Et",color=V6C.red)};Spacer(Modifier.weight(1f));if(streak>1)Text("🔥 ${streak}x Seri",color=V6C.fire,fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));IconButton(onClick=onExit){Icon(Icons.Rounded.Close,"Ayrıl")}}
        Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=V6C.white,border=BorderStroke(1.dp,V6C.border)){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){V6Player(meName,meAvatar,myScore,myTurn,Modifier.weight(1f));Box(Modifier.size(54.dp).clip(CircleShape).background(V6C.blueLight),contentAlignment=Alignment.Center){Text("$seconds",color=V6C.blueDark,fontWeight=FontWeight.Black,fontSize=19.sp)};V6Player(oppName,oppAvatar,oppScore,!myTurn,Modifier.weight(1f))}}
        Surface(Modifier.fillMaxWidth().weight(1f),shape=RoundedCornerShape(20.dp),color=V6C.white,border=BorderStroke(1.dp,V6C.border)){Box(Modifier.fillMaxSize().padding(14.dp)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(if(room.status=="quiz")"Bilgi sorusu turu" else if(myTurn)"Sıra Sende!" else "Rakibin Sırası…",fontWeight=FontWeight.Bold,color=if(myTurn)V6C.green else V6C.muted);Spacer(Modifier.height(8.dp));Text(if(required==null)"İlk kelimeyi yaz" else "Başlangıç Harfi: '$required'",color=V6C.amber,fontWeight=FontWeight.Bold);Spacer(Modifier.height(14.dp));V6Tiles(if(input.isBlank())required?.toString().orEmpty() else input);Spacer(Modifier.height(12.dp));if(lastWord.isNotBlank())Text("Son Kelime: $lastWord",color=V6C.muted);Spacer(Modifier.height(8.dp));Text(notice,color=V6C.muted,fontSize=12.sp,textAlign=TextAlign.Center)};IconButton(onClick=onChat,modifier=Modifier.align(Alignment.BottomEnd).size(46.dp).clip(RoundedCornerShape(13.dp)).background(V6C.blue)){Icon(Icons.Rounded.ChatBubble,"Sohbet",tint=Color.White)}}}
        V6Keyboard(enabled=myTurn&&!busy,submitEnabled=myTurn&&!busy&&input.length>=2&&(required==null||input.firstOrNull()?.uppercaseChar()==required),onKey={c-> val next=if(input.isEmpty()&&required!=null)"$required$c" else input+c;onInput(next)},onDelete={if(input.length>(if(required==null)0 else 1))onInput(input.dropLast(1))},onSubmit=onSubmit)
    }
}

@Composable private fun V6Player(name:String,avatar:String?,score:Int,active:Boolean,modifier:Modifier){Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){V6Avatar(avatar,name,48);Text(name,maxLines=1,fontSize=11.sp,fontWeight=FontWeight.Bold,color=V6C.text);Text("$score puan",fontSize=10.sp,color=if(active)V6C.blueDark else V6C.muted)}}
@Composable private fun V6Avatar(url:String?,name:String,size:Int){if(!url.isNullOrBlank())AsyncImage(model=url,contentDescription="$name profil fotoğrafı",contentScale=ContentScale.Crop,modifier=Modifier.size(size.dp).clip(CircleShape).background(V6C.blueLight)) else Box(Modifier.size(size.dp).clip(CircleShape).background(V6C.blueLight),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),color=V6C.blueDark,fontWeight=FontWeight.Black,fontSize=(size/2.2).sp)}}
@Composable private fun V6Tiles(word:String){Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){word.take(9).forEachIndexed{i,c->Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(if(i==0||(i==word.take(9).lastIndex&&word.length>1))V6C.amber else V6C.blue),contentAlignment=Alignment.Center){Text(c.toString(),color=Color.White,fontWeight=FontWeight.Black,fontSize=20.sp)}}}}
@Composable private fun V6Keyboard(enabled:Boolean,submitEnabled:Boolean,onKey:(Char)->Unit,onDelete:()->Unit,onSubmit:()->Unit){val rows=listOf(listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü'),listOf('A','S','D','F','G','H','J','K','L','Ş','İ'));Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)){rows.forEach{r->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){r.forEach{c->V6Key(c,Modifier.weight(1f),enabled){onKey(c)}}}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){Button(onClick=onSubmit,enabled=submitEnabled,modifier=Modifier.weight(2f).height(46.dp),colors=ButtonDefaults.buttonColors(containerColor=V6C.blue),contentPadding=PaddingValues(0.dp)){Text("ONAY",fontWeight=FontWeight.Black,fontSize=12.sp)};listOf('Z','X','C','V','B','N','M','Ö','Ç').forEach{c->V6Key(c,Modifier.weight(1f),enabled){onKey(c)}};OutlinedButton(onClick=onDelete,enabled=enabled,modifier=Modifier.weight(1.7f).height(46.dp),border=BorderStroke(1.dp,V6C.red),contentPadding=PaddingValues(0.dp)){Text("SİL",color=V6C.red,fontWeight=FontWeight.Bold,fontSize=11.sp)}}}}
@Composable private fun V6Key(c:Char,modifier:Modifier,enabled:Boolean,onClick:()->Unit){Surface(onClick=onClick,enabled=enabled,modifier=modifier.height(46.dp),color=V6C.white,shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,V6C.border)){Box(contentAlignment=Alignment.Center){Text(c.toString(),color=if(enabled)V6C.text else V6C.muted,fontWeight=FontWeight.Bold,fontSize=15.sp)}}}

@Composable
private fun V6Chat(messages:List<ChatMessageDto>,me:String?,enabled:Boolean,onDismiss:()->Unit,onSend:(String)->Unit){var input by remember{mutableStateOf("")};val quick=listOf("İyi oyunlar!","Çok iyi kelime!","Hadi bakalım :)","Tebrikler!");ModalBottomSheet(onDismissRequest=onDismiss,containerColor=V6C.white){Column(Modifier.fillMaxWidth().heightIn(min=420.dp,max=650.dp).padding(horizontal=16.dp,vertical=8.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Oyun İçi Sohbet",Modifier.weight(1f),fontWeight=FontWeight.Bold,fontSize=20.sp,color=V6C.text);IconButton(onClick=onDismiss){Icon(Icons.Rounded.Close,"Kapat")}};LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=8.dp)){items(quick){q->SuggestionChip(onClick={onSend(q)},enabled=enabled,label={Text(q)})}};HorizontalDivider(color=V6C.border);LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=10.dp)){items(messages,key={it.id}){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.senderId==me)Arrangement.End else Arrangement.Start){Surface(shape=RoundedCornerShape(12.dp),color=if(m.senderId==me)V6C.blue else V6C.blueLight){Text(m.body,Modifier.padding(horizontal=12.dp,vertical=8.dp),color=if(m.senderId==me)Color.White else V6C.text)}}}};Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=input,onValueChange={input=it.take(300)},enabled=enabled,modifier=Modifier.weight(1f),singleLine=true,placeholder={Text("Mesaj yaz…")});Spacer(Modifier.width(8.dp));IconButton(onClick={val t=input.trim();if(t.isNotBlank()){onSend(t);input=""}},enabled=enabled,modifier=Modifier.size(48.dp).clip(CircleShape).background(V6C.blue)){Icon(Icons.Rounded.Send,"Gönder",tint=Color.White)}};Spacer(Modifier.height(8.dp))}}

// -----------------------------------------------------------------------------
// LEADERBOARD: completely light-theme aligned, with signed private avatars.
// -----------------------------------------------------------------------------
private data class V6BoardUi(val row:LeaderboardV3Row,val avatar:String?)

@Composable
fun V6LeaderboardScreen(onBack:()->Unit){val backend=remember{OnlineGameBackend()};var language by remember{mutableStateOf("tr")};var period by remember{mutableStateOf("week")};var rows by remember{mutableStateOf<List<V6BoardUi>>(emptyList())};var loading by remember{mutableStateOf(true)};val me=backend.currentUserId();LaunchedEffect(language,period){loading=true;rows=runCatching{backend.getLeaderboardV3(language,period,50)}.getOrDefault(emptyList()).map{V6BoardUi(it,AvatarSignedUrl.resolve(it.avatarUrl))};loading=false};val mine=rows.indexOfFirst{it.row.userId==me};val wins=rows.getOrNull(mine)?.row?.wins?:0;val league=when{wins>=200->"EFSANE";wins>=100->"ELMAS";wins>=50->"PLATİN";wins>=25->"ALTIN";wins>=10->"GÜMÜŞ";else->"BRONZ"};LazyColumn(Modifier.fillMaxSize().background(V6C.bg),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"Geri",tint=V6C.text)};Text("LİG",Modifier.weight(1f),textAlign=TextAlign.Center,fontWeight=FontWeight.Black,fontSize=22.sp,color=V6C.text);Spacer(Modifier.width(48.dp))}};item{Surface(shape=RoundedCornerShape(22.dp),color=V6C.blueLight,border=BorderStroke(1.5.dp,V6C.blue.copy(alpha=.35f)),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.Shield,null,tint=V6C.blue,modifier=Modifier.size(72.dp));Text("$league LİG",color=V6C.blueDark,fontSize=24.sp,fontWeight=FontWeight.Black);Text(if(mine>=0)"SIRALAMAN: ${mine+1}" else "Bu dönemde henüz sıran yok",color=V6C.text,fontWeight=FontWeight.Bold)}}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=language=="tr",onClick={language="tr"},label={Text("🇹🇷 TR")},modifier=Modifier.weight(1f));FilterChip(selected=language=="en",onClick={language="en"},label={Text("🇬🇧 EN")},modifier=Modifier.weight(1f))}};item{Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(V6C.white).padding(4.dp)){listOf("week" to "BU HAFTA","month" to "BU AY","total" to "TOPLAM").forEach{(k,t)->Button(onClick={period=k},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=if(period==k)V6C.blue else Color.Transparent,contentColor=if(period==k)Color.White else V6C.text),shape=RoundedCornerShape(10.dp)){Text(t,fontSize=10.sp,fontWeight=FontWeight.Bold)}}}};if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth(),color=V6C.blue)};item{Text("LİDERLER",fontWeight=FontWeight.Black,fontSize=16.sp,color=V6C.text)};itemsIndexed(rows,key={_,u->u.row.userId}){i,u->val mineRow=u.row.userId==me;Surface(shape=RoundedCornerShape(16.dp),color=if(mineRow)V6C.blueLight else V6C.white,border=BorderStroke(1.dp,if(mineRow)V6C.blue else V6C.border),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(when(i){0->"🥇";1->"🥈";2->"🥉";else->"${i+1}."},Modifier.width(42.dp),textAlign=TextAlign.Center);V6Avatar(u.avatar,u.row.displayName,42);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(u.row.displayName,fontWeight=if(mineRow)FontWeight.Black else FontWeight.Bold,color=V6C.text);Text("${u.row.wins} galibiyet • %${if(u.row.winRate%1.0==0.0)u.row.winRate.toInt() else u.row.winRate}",fontSize=11.sp,color=V6C.muted)};if(mineRow)Text("SEN",color=V6C.blue,fontWeight=FontWeight.Black,fontSize=10.sp)}}};if(!loading&&rows.isEmpty())item{Text("Bu dönemde sıralama henüz oluşmadı.",Modifier.fillMaxWidth().padding(28.dp),textAlign=TextAlign.Center,color=V6C.muted)}}}
