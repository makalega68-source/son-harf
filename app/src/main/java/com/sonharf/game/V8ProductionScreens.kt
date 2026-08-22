package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonharf.game.data.*
import com.sonharf.game.ui.home.*
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val V8Bg = Color(0xFFF8FAFC)
private val V8White = Color.White
private val V8Blue = Color(0xFF0284C7)
private val V8BlueLight = Color(0xFFE0F2FE)
private val V8Text = Color(0xFF0F172A)
private val V8Muted = Color(0xFF64748B)
private val V8Border = Color(0xFFCBD5E1)
private val V8Green = Color(0xFF2E6F5E)
private val V8Coral = Color(0xFFE05A47)
private val V8Amber = Color(0xFFE5A93C)
private val V8Purple = Color(0xFF7C3AED)

@Composable
fun V8HomeScreen(
    state: FullHomeUiState,
    onStartGameMode: (String) -> Unit,
    onClaimDailyReward: () -> Unit,
    onOpenVipModal: () -> Unit,
    onInviteFriend: () -> Unit,
    onOpenFriendsList: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
    onClaimTaskReward: (String) -> Unit,
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(V8Bg), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = V8Blue) }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().background(V8Bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(onClick = onOpenProfile, shape = RoundedCornerShape(18.dp), color = V8White, border = BorderStroke(1.dp, V8Border)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    V8Avatar(state.userPhotoUrl, state.userName, 54)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.userName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = V8Text)
                        Text("Seviye ${state.level} • ${state.league}", color = V8Muted, fontSize = 13.sp)
                    }
                    Text("💎 ${state.diamonds}", color = V8Blue, fontWeight = FontWeight.Black)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V8InfoAction("Günlük Ödül", if (state.isDailyRewardAvailable) "+${state.dailyRewardDiamonds} elmas" else "Bugün alındı", Icons.Rounded.CardGiftcard, V8Blue, Modifier.weight(1f), state.isDailyRewardAvailable, onClaimDailyReward)
                V8InfoAction("VIP Teklif", "Reklamsız • 2x ödül", Icons.Rounded.WorkspacePremium, V8Purple, Modifier.weight(1f), true, onOpenVipModal)
            }
        }
        item {
            Text("Oyun Seçenekleri", fontWeight = FontWeight.Black, fontSize = 20.sp, color = V8Text)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onStartGameMode("1v1_RANKED") }, modifier = Modifier.fillMaxWidth().height(68.dp), colors = ButtonDefaults.buttonColors(containerColor = V8Blue), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text("1v1 Hızlı Karşılaşma", fontSize = 18.sp, fontWeight = FontWeight.Black); Text("Gerçek oyuncu ile sıra tabanlı", fontSize = 13.sp) }
                Icon(Icons.Rounded.ChevronRight, null)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V8GameCard("Lig Arenası", "Puan kazan, ligde yüksel", Icons.Rounded.Shield, V8Amber, Modifier.weight(1f)) { onStartGameMode("LEAGUE") }
                V8GameCard("Bot ile Pratik", "Kelime hızını geliştir", Icons.Rounded.SmartToy, V8Blue, Modifier.weight(1f)) { onStartGameMode("PRACTICE_BOT") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onInviteFriend, modifier = Modifier.weight(1f).height(54.dp)) { Icon(Icons.Rounded.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("Davet Et") }
                Button(onClick = onOpenFriendsList, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = V8BlueLight)) { Icon(Icons.Rounded.Groups, null, tint = V8Blue); Spacer(Modifier.width(6.dp)); Text("Arkadaşlar (${state.onlineFriendsCount})", color = V8Blue) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Görevler", Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 20.sp, color = V8Text)
                Text("Tamamlanan ödülü tek tek al", color = V8Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (state.tasks.isEmpty()) Text("Aktif görev yok.", color = V8Muted)
            else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 8.dp)) {
                items(state.tasks, key = { it.id }) { task ->
                    val done = task.current >= task.target
                    Surface(shape = RoundedCornerShape(16.dp), color = V8White, border = BorderStroke(1.dp, V8Border), modifier = Modifier.width(220.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(task.title, maxLines = 1, fontWeight = FontWeight.Bold, color = V8Text)
                            LinearProgressIndicator(progress = { if (task.target <= 0) 0f else (task.current.toFloat()/task.target).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = if (done) V8Green else V8Blue)
                            Text("${task.current}/${task.target} • +${task.rewardDiamonds} elmas", color = V8Muted, fontSize = 11.sp)
                            if (done) Button(onClick = { onClaimTaskReward(task.id) }, enabled = !task.isClaimed, modifier = Modifier.fillMaxWidth().height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = V8Green)) { Text(if (task.isClaimed) "Alındı" else "Ödülü Al", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
        if (state.notice.isNotBlank()) item { Text(state.notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = V8Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    }
}

@Composable private fun V8InfoAction(title:String, subtitle:String, icon:androidx.compose.ui.graphics.vector.ImageVector, accent:Color, modifier:Modifier, enabled:Boolean, onClick:()->Unit) {
    Surface(onClick=onClick, enabled=enabled, modifier=modifier.heightIn(min=106.dp), shape=RoundedCornerShape(16.dp), color=V8White, border=BorderStroke(1.dp,V8Border)) {
        Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) { Icon(icon,null,tint=accent); Text(title,fontWeight=FontWeight.Black,color=V8Text); Text(subtitle,color=V8Muted,fontSize=12.sp,lineHeight=16.sp) }
    }
}
@Composable private fun V8GameCard(title:String, subtitle:String, icon:androidx.compose.ui.graphics.vector.ImageVector, accent:Color, modifier:Modifier, onClick:()->Unit) {
    Surface(onClick=onClick, modifier=modifier.heightIn(min=112.dp), shape=RoundedCornerShape(18.dp), color=V8White, border=BorderStroke(1.dp,V8Border)) {
        Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(7.dp)) { Icon(icon,null,tint=accent,modifier=Modifier.size(28.dp)); Text(title,fontWeight=FontWeight.Black,fontSize=16.sp,color=V8Text); Text(subtitle,color=V8Muted,fontSize=12.sp,lineHeight=16.sp) }
    }
}

@Composable
fun V8ProfileScreen(onOpenPreferences: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var profile by remember { mutableStateOf<V6ProfileDto?>(null) }
    var avatar by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val uid = backend.currentUserId()
        if (uid == null) { notice = "Oturum bulunamadı."; loading = false; return@LaunchedEffect }
        profile = runCatching { v6LoadProfile(uid) }.getOrNull()
        loading = false
        avatar = runCatching { AvatarSignedUrl.resolve(profile?.avatarPath) }.getOrNull()
    }
    if (loading) { Box(Modifier.fillMaxSize().background(V8Bg), contentAlignment=Alignment.Center){ CircularProgressIndicator(color=V8Blue) }; return }
    val p=profile
    LazyColumn(Modifier.fillMaxSize().background(V8Bg), contentPadding=PaddingValues(18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment=Alignment.CenterVertically){ Text("PROFİL",Modifier.weight(1f),fontWeight=FontWeight.Black,fontSize=22.sp,color=V8Text); TextButton(onClick=onOpenPreferences){ Text("Gizlilik & Tercihler") } } }
        item { Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){ V8Avatar(avatar,p?.displayName?:"Oyuncu",104); Spacer(Modifier.height(10.dp)); Text(p?.displayName?:"Oyuncu",fontWeight=FontWeight.Black,fontSize=22.sp,color=V8Text); if(notice.isNotBlank()) Text(notice,color=V8Coral) } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ V8Metric("${p?.wins?:0}","Galibiyet",Modifier.weight(1f)); V8Metric("${p?.losses?:0}","Mağlubiyet",Modifier.weight(1f)); V8Metric("${p?.diamonds?:0}","Elmas",Modifier.weight(1f)) } }
    }
}
@Composable private fun V8Metric(value:String,label:String,modifier:Modifier){ Surface(modifier,shape=RoundedCornerShape(16.dp),color=V8White,border=BorderStroke(1.dp,V8Border)){ Column(Modifier.padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){ Text(value,fontWeight=FontWeight.Black,fontSize=20.sp,color=V8Blue); Text(label,fontSize=11.sp,color=V8Muted) } } }

@Composable
fun V8BattleScreen(onLeaveBattle: () -> Unit) {
    val backend=remember{OnlineGameBackend()}; val scope=rememberCoroutineScope()
    var meProfile by remember{ mutableStateOf<V6ProfileDto?>(null) }; var meVip by remember{ mutableStateOf(false) }; var meAvatar by remember{ mutableStateOf<String?>(null) }
    var opponent by remember{ mutableStateOf<V6ProfileDto?>(null) }; var opponentAvatar by remember{ mutableStateOf<String?>(null) }
    var room by remember{ mutableStateOf<GameRoomDto?>(null) }; var words by remember{ mutableStateOf<List<GameWordDto>>(emptyList()) }; var chat by remember{ mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var input by remember{ mutableStateOf("") }; var notice by remember{ mutableStateOf("Düelloya hazır") }; var matching by remember{ mutableStateOf(false) }; var busy by remember{ mutableStateOf(false) }; var showChat by remember{ mutableStateOf(false) }; var showWords by remember{ mutableStateOf(false) }
    var triviaRound by remember{ mutableStateOf<TriviaRoundDto?>(null) }; var triviaQuestion by remember{ mutableStateOf<TriviaQuestionDto?>(null) }

    suspend fun loadSelf(){ val id=backend.currentUserId()?:return; meProfile=runCatching{v6LoadProfile(id)}.getOrNull(); meAvatar=runCatching{AvatarSignedUrl.resolve(meProfile?.avatarPath)}.getOrNull(); meVip=runCatching{backend.getProfile(id).isVip}.getOrDefault(false) }
    suspend fun loadOpponent(r:GameRoomDto){ if(r.isBot){opponent=null;opponentAvatar=null;return}; val me=backend.currentUserId(); val id=if(r.hostId==me)r.guestId else r.hostId; opponent=id?.let{runCatching{v6LoadProfile(it)}.getOrNull()}; opponentAvatar=runCatching{AvatarSignedUrl.resolve(opponent?.avatarPath)}.getOrNull() }
    suspend fun findActive():GameRoomDto?{ val me=backend.currentUserId()?:return null; return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>().filter{(it.hostId==me||it.guestId==me)&&it.status in listOf("waiting","playing","quiz","final","sudden_death","paused")}.maxByOrNull{it.validWordCount} }
    LaunchedEffect(Unit){ loadSelf(); room=runCatching{findActive()}.getOrNull(); room?.let{loadOpponent(it)} }
    val active=room
    if(active==null){ V6BattleLobbyCompat(meProfile,meAvatar,matching,notice,onLeaveBattle,{ scope.launch{ if(busy)return@launch; busy=true; runCatching{backend.startRandomMatchmaking("tr")}.onSuccess{matching=true;notice="Rakip aranıyor…"}.onFailure{notice="Eşleşme başlatılamadı."}; busy=false; while(matching&&room==null){ val found=runCatching{backend.pollRandomMatchmakingRoom()}.getOrNull(); if(found!=null){room=found;loadOpponent(found);matching=false;break};delay(800)} } },{scope.launch{matching=false;runCatching{backend.cancelRandomMatchmaking()}}}); return }
    val me=backend.currentUserId()
    LaunchedEffect(active.id){ var heartbeatTick=0; while(isActive){ runCatching{backend.getRoom(active.id)}.onSuccess{room=it;loadOpponent(it)}; runCatching{backend.getWords(active.id)}.onSuccess{words=it}; runCatching{backend.getChat(active.id)}.onSuccess{chat=it}; heartbeatTick++; if(heartbeatTick%7==0&&!active.isBot) runCatching{backend.heartbeatRoom(active.id)}.onSuccess{room=it}; delay(700) } }
    LaunchedEffect(active.currentPlayerId,active.validWordCount,active.roundNo,words.size){ val req=words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar(); input=if(active.currentPlayerId==me&&active.status in listOf("playing","final","sudden_death")&&req!=null)req.toString() else "" }
    LaunchedEffect(active.turnDeadline,active.currentPlayerId,active.status){ if(active.status in listOf("playing","final","sudden_death")&&active.turnDeadline!=null){ while(isActive){ val left=runCatching{Instant.parse(active.turnDeadline).epochSecond-Instant.now().epochSecond}.getOrDefault(1); if(left<=0){ runCatching{backend.claimTurnTimeout(active.id)}.onSuccess{room=it}; break };delay(800) } } }
    LaunchedEffect(active.id,active.botTurn,active.status,active.validWordCount){ if(active.isBot&&active.botTurn&&active.status in listOf("playing","final","sudden_death")){ delay(900); runCatching{backend.botTakeTurn(active.id)}.onSuccess{room=it;notice=""}.onFailure{e->notice="Bot sırası yenileniyor…"; delay(900); runCatching{backend.botTakeTurn(active.id)}.onSuccess{room=it}.onFailure{notice="Bot bağlantısı yenileniyor…"} } } }
    LaunchedEffect(active.id,active.status,active.validWordCount){ if(active.status=="quiz"){ triviaRound=runCatching{backend.getActiveTriviaRound(active.id)}.getOrNull(); triviaQuestion=triviaRound?.let{runCatching{backend.getTriviaQuestion(it.questionId)}.getOrNull()}; if(active.isBot){ delay(1200); runCatching{backend.botAnswerTrivia(active.id)}.onSuccess{room=it} } } else { triviaRound=null;triviaQuestion=null } }

    if(active.status=="finished"){ V8Finished(active,me,onLeaveBattle); return }
    V8BattleArena(active,me,meProfile?.displayName?:"Sen",meAvatar,if(active.isBot)active.botName?:"KelimeBot" else opponent?.displayName?:"Rakip",opponentAvatar,words,input,notice,busy,meVip,{input=it.take(40)},{ val submitted=input.trim(); if(submitted.length<2)return@V8BattleArena; scope.launch{busy=true; runCatching{backend.submitWord(active.id,submitted)}.onSuccess{room=it;input="";notice="${submitted.uppercase()} kabul edildi."}.onFailure{e->val raw=e.message.orEmpty(); notice=when{ "not_your_turn" in raw->"Sıra rakibinde."; "wrong_start_letter" in raw->"Kelime doğru harfle başlamalı."; "word_already_used" in raw->"Bu kelime daha önce kullanıldı."; "not_in_dictionary" in raw->"Kelime sözlükte bulunamadı."; "turn_expired" in raw->"Süren doldu."; else->"Kelime gönderilemedi: sunucu durumunu yeniledim." }; room=runCatching{backend.getRoom(active.id)}.getOrNull()?:room};busy=false} },{scope.launch{runCatching{backend.forfeit(active.id)}.onSuccess{room=it}}},onLeaveBattle,{showChat=true},{if(meVip)showWords=true else notice="Çıkan Kelimeler VIP özelliğidir."},triviaQuestion,{index->scope.launch{val r=triviaRound?:return@launch; busy=true;runCatching{backend.answerTrivia(r.id,index)}.onSuccess{room=it;notice="Bilgi sorusu yanıtlandı."}.onFailure{notice="Yanıt gönderilemedi."};busy=false}})
    if(showChat) V8ChatSheet(chat,me,meProfile?.allowMatchChat!=false,{showChat=false}){text->scope.launch{runCatching{backend.sendChat(active.id,text)}.onSuccess{chat=runCatching{backend.getChat(active.id)}.getOrDefault(chat)}.onFailure{notice="Mesaj gönderilemedi."}}}
    if(showWords) AlertDialog(onDismissRequest={showWords=false},title={Text("Çıkan Kelimeler")},text={LazyColumn(Modifier.heightIn(max=420.dp)){items(words,key={it.id}){Text(it.word.uppercase(),Modifier.fillMaxWidth().padding(8.dp))}}},confirmButton={TextButton(onClick={showWords=false}){Text("Kapat")}})
}

@Composable private fun V6BattleLobbyCompat(profile:V6ProfileDto?,avatar:String?,matching:Boolean,notice:String,onBack:()->Unit,onRandom:()->Unit,onCancel:()->Unit){ LazyColumn(Modifier.fillMaxSize().background(V8Bg),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){ item{Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"Geri")};Text("SON HARF",Modifier.weight(1f),textAlign=TextAlign.Center,fontWeight=FontWeight.Black,fontSize=22.sp);Spacer(Modifier.width(48.dp))}}; item{Surface(shape=RoundedCornerShape(18.dp),color=V8White,border=BorderStroke(1.dp,V8Border)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){V8Avatar(avatar,profile?.displayName?:"Oyuncu",56);Spacer(Modifier.width(12.dp));Text(if(matching)"Rakip aranıyor…" else "Düelloya hazırsın",fontWeight=FontWeight.Bold)}}}; item{if(matching)OutlinedButton(onClick=onCancel,modifier=Modifier.fillMaxWidth().height(56.dp)){Text("EŞLEŞMEYİ İPTAL ET")}else Button(onClick=onRandom,modifier=Modifier.fillMaxWidth().height(64.dp),colors=ButtonDefaults.buttonColors(containerColor=V8Blue)){Text("1v1 HIZLI KARŞILAŞMA",fontWeight=FontWeight.Black)}}; item{Text(notice,Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=V8Muted)} } }

@Composable private fun V8BattleArena(room:GameRoomDto,me:String?,myName:String,myAvatar:String?,oppName:String,oppAvatar:String?,words:List<GameWordDto>,input:String,notice:String,busy:Boolean,vip:Boolean,onInput:(String)->Unit,onSubmit:()->Unit,onForfeit:()->Unit,onExit:()->Unit,onChat:()->Unit,onWords:()->Unit,trivia:TriviaQuestionDto?,onTrivia:(Int)->Unit){
    val host=me==room.hostId; val myScore=if(host)room.hostScore else room.guestScore; val oppScore=if(host)room.guestScore else room.hostScore; val myTurn=room.currentPlayerId==me&&room.status in listOf("playing","final","sudden_death"); val last=words.lastOrNull()?.word?.uppercase().orEmpty(); val req=words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar(); var seconds by remember(room.turnDeadline){mutableIntStateOf(45)}
    LaunchedEffect(room.turnDeadline,room.currentPlayerId,room.status){while(isActive&&room.turnDeadline!=null&&room.status in listOf("playing","final","sudden_death")){seconds=runCatching{(Instant.parse(room.turnDeadline).epochSecond-Instant.now().epochSecond).toInt().coerceAtLeast(0)}.getOrDefault(45);delay(1000)}}
    Column(Modifier.fillMaxSize().background(Color(0xFFF9F6F0)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text("🔥 Kelime Düellosu",Modifier.weight(1f),fontWeight=FontWeight.Black,fontSize=18.sp,color=V8Text);Surface(onClick=onWords,shape=RoundedCornerShape(12.dp),color=if(vip)V8Purple else V8White,border=BorderStroke(1.dp,V8Purple)){Text(if(vip)"📜 Çıkan Kelimeler" else "🔒 Çıkan Kelimeler",Modifier.padding(9.dp),color=if(vip)Color.White else V8Purple,fontSize=11.sp,fontWeight=FontWeight.Bold)};IconButton(onClick=onExit){Icon(Icons.Rounded.Close,"Çık")}}
        Surface(shape=RoundedCornerShape(14.dp),color=V8White){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Text("CANLI 1v1 • Klasik Son Harf",Modifier.weight(1f),color=V8Green,fontWeight=FontWeight.Bold);TextButton(onClick=onChat){Icon(Icons.Rounded.ChatBubble,null);Spacer(Modifier.width(4.dp));Text("Sohbet")}}}
        Surface(shape=RoundedCornerShape(20.dp),color=V8White,shadowElevation=3.dp){Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){V8Player(myName,myAvatar,myScore,myTurn,Modifier.weight(1f));Box(Modifier.size(64.dp).clip(CircleShape).background(if(seconds<=5)V8Coral else V8Amber),contentAlignment=Alignment.Center){Text("$seconds",color=Color.White,fontWeight=FontWeight.Black,fontSize=25.sp)};V8Player(oppName,oppAvatar,oppScore,!myTurn,Modifier.weight(1f))};Spacer(Modifier.height(8.dp));Text(when{room.status=="quiz"->"BİLGİ SORUSU";room.status=="paused"->"BAĞLANTI BEKLENİYOR";myTurn->"SIRA SİZDE";else->"RAKİP BEKLENİYOR…"},fontWeight=FontWeight.Black,color=if(myTurn)V8Green else V8Muted);Spacer(Modifier.height(7.dp));Text(if(last.isBlank())"İlk kelimeyi siz başlatın" else "Son Kelime: $last",color=V8Muted,fontSize=16.sp)} }
        if(room.status=="quiz"){Surface(shape=RoundedCornerShape(16.dp),color=V8White,border=BorderStroke(1.dp,V8Amber)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(trivia?.question?:"Bilgi sorusu yükleniyor…",fontWeight=FontWeight.Black,color=V8Text);listOf(trivia?.optionA,trivia?.optionB,trivia?.optionC,trivia?.optionD).forEachIndexed{i,o->if(o!=null)OutlinedButton(onClick={onTrivia(i)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(o)}}}}} else {Surface(Modifier.fillMaxWidth().heightIn(min=72.dp),shape=RoundedCornerShape(14.dp),color=V8White,border=BorderStroke(2.dp,V8Green.copy(alpha=.4f))){Column(Modifier.fillMaxWidth().padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(if(input.isBlank())if(req==null)"Kelime yazın…" else "'$req' ile başlayan kelime yazın…" else input,fontWeight=FontWeight.Black,fontSize=22.sp,color=if(input.isBlank())V8Muted else V8Text);if(notice.isNotBlank())Text(notice,color=V8Coral,fontSize=11.sp,textAlign=TextAlign.Center)}};Spacer(Modifier.weight(1f));V8Keyboard(myTurn&&!busy,myTurn&&!busy&&input.length>=2&&(req==null||input.firstOrNull()?.uppercaseChar()==req),{c->onInput(if(input.isEmpty()&&req!=null)"$req$c" else input+c)},{if(input.length>if(req==null)0 else 1)onInput(input.dropLast(1))},onSubmit)}
        TextButton(onClick=onForfeit,modifier=Modifier.align(Alignment.CenterHorizontally)){Icon(Icons.Rounded.Flag,null,tint=V8Coral);Text(" Pes Et",color=V8Coral,fontWeight=FontWeight.Bold)}
    }
}
@Composable private fun V8Player(name:String,url:String?,score:Int,active:Boolean,modifier:Modifier){Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){V8Avatar(url,name,50);Text(name,maxLines=1,fontWeight=FontWeight.Bold,fontSize=11.sp,color=V8Text);Text("$score puan",fontSize=10.sp,color=if(active)V8Green else V8Muted)}}
@Composable private fun V8Keyboard(enabled:Boolean,submitEnabled:Boolean,onKey:(Char)->Unit,onDelete:()->Unit,onSubmit:()->Unit){val r1=listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü');val r2=listOf('A','S','D','F','G','H','J','K','L','Ş','İ');val r3=listOf('Z','X','C','V','B','N','M','Ö','Ç');Surface(shape=RoundedCornerShape(16.dp),color=V8White,shadowElevation=2.dp){Column(Modifier.padding(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){listOf(r1,r2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){row.forEach{c->V8Key(c,Modifier.weight(1f),enabled){onKey(c)}}}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){Button(onClick=onDelete,enabled=enabled,modifier=Modifier.weight(1.7f).height(46.dp),colors=ButtonDefaults.buttonColors(containerColor=V8Coral),contentPadding=PaddingValues(0.dp)){Text("⌫ SİL",fontSize=11.sp)};r3.forEach{c->V8Key(c,Modifier.weight(1f),enabled){onKey(c)}};Button(onClick=onSubmit,enabled=submitEnabled,modifier=Modifier.weight(1.9f).height(46.dp),colors=ButtonDefaults.buttonColors(containerColor=V8Green),contentPadding=PaddingValues(0.dp)){Text("✓ ONAY",fontSize=11.sp)}}}}}
@Composable private fun V8Key(c:Char,modifier:Modifier,enabled:Boolean,onClick:()->Unit){Surface(onClick=onClick,enabled=enabled,modifier=modifier.height(46.dp),shape=RoundedCornerShape(8.dp),color=Color(0xFFECE7DE),border=BorderStroke(1.dp,Color(0xFFD6CFC4))){Box(contentAlignment=Alignment.Center){Text(c.toString(),fontWeight=FontWeight.Bold,fontSize=15.sp,color=V8Text)}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun V8ChatSheet(messages:List<ChatMessageDto>,me:String?,enabled:Boolean,onDismiss:()->Unit,onSend:(String)->Unit){var input by remember{mutableStateOf("")};val quick=listOf("İyi oyunlar!","Çok iyi kelime!","Hadi bakalım :)" );ModalBottomSheet(onDismissRequest=onDismiss,containerColor=V8White){Column(Modifier.fillMaxWidth().heightIn(min=330.dp,max=560.dp).padding(horizontal=16.dp,vertical=8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Oyun İçi Sohbet",Modifier.weight(1f),fontWeight=FontWeight.Black,fontSize=22.sp);IconButton(onClick=onDismiss){Icon(Icons.Rounded.Close,"Kapat")}};LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=6.dp)){items(quick){q->SuggestionChip(onClick={onSend(q)},enabled=enabled,label={Text(q)})}};HorizontalDivider();LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){items(messages,key={it.id}){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.senderId==me)Arrangement.End else Arrangement.Start){Surface(shape=RoundedCornerShape(12.dp),color=if(m.senderId==me)V8Blue else V8BlueLight){Text(m.body,Modifier.padding(10.dp),color=if(m.senderId==me)Color.White else V8Text)}}}};Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=input,onValueChange={input=it.take(300)},enabled=enabled,modifier=Modifier.weight(1f),singleLine=true,placeholder={Text("Mesaj yaz…")});Spacer(Modifier.width(7.dp));IconButton(onClick={val t=input.trim();if(t.isNotBlank()){onSend(t);input=""}},enabled=enabled,modifier=Modifier.size(48.dp).clip(CircleShape).background(V8Blue)){Icon(Icons.Rounded.Send,"Gönder",tint=Color.White)}}}}}
@Composable private fun V8Finished(room:GameRoomDto,me:String?,onExit:()->Unit){val won=room.winnerId==me;Box(Modifier.fillMaxSize().background(V8Bg).padding(24.dp),contentAlignment=Alignment.Center){Surface(shape=RoundedCornerShape(22.dp),color=V8White,border=BorderStroke(1.dp,V8Border)){Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(if(won)"🏆 Kazandınız!" else "Maç Tamamlandı",fontWeight=FontWeight.Black,fontSize=26.sp,color=V8Text);Spacer(Modifier.height(10.dp));Text("${room.hostScore} - ${room.guestScore}",fontSize=24.sp,fontWeight=FontWeight.Bold,color=V8Green);Spacer(Modifier.height(18.dp));Button(onClick=onExit){Text("Ana Sayfaya Dön")}}}}}

@Composable private fun V8Avatar(url:String?,name:String,size:Int){var failed by remember(url){mutableStateOf(false)};if(!url.isNullOrBlank()&&!failed){AsyncImage(model=url,contentDescription="$name profil fotoğrafı",contentScale=ContentScale.Crop,modifier=Modifier.size(size.dp).clip(CircleShape).background(V8BlueLight),onError={failed=true})}else Box(Modifier.size(size.dp).clip(CircleShape).background(V8BlueLight),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),fontWeight=FontWeight.Black,fontSize=(size/2.2).sp,color=V8Blue)}}
