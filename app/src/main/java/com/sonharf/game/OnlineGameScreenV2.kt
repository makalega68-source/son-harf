package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreenV2() {
    if (!SupabaseProvider.configured) { MissingBackendV2(); return }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("tr") }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Dilini seç ve rakibini bul.") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var requests by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var rematchRequested by remember { mutableStateOf(false) }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }
    var rematchJob by remember { mutableStateOf<Job?>(null) }

    fun friendly(t: Throwable): String {
        val raw=t.message.orEmpty()
        return when {
            "not_your_turn" in raw -> "Sıra rakibinde."
            "answers_locked" in raw -> "Şıklar 3 saniye sonra açılır."
            "blocked_relationship" in raw -> "Bu oyuncuyla etkileşim kapalı."
            else -> raw.substringBefore("URL:").trim().ifBlank { "Bağlantı hatası." }
        }
    }
    suspend fun refreshQuiz(r: GameRoomDto) {
        if (r.status=="quiz") {
            triviaRound=backend.getActiveTriviaRound(r.id)
            triviaQuestion=triviaRound?.let { backend.getTriviaQuestion(it.questionId) }
        } else { triviaRound=null; triviaQuestion=null }
    }
    fun observe(r: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel(); rematchJob?.cancel()
        matching=false; rematchRequested=false
        roomJob=scope.launch { backend.observeRoom(r.id).catch { status=friendly(it) }.collect { room=it; refreshQuiz(it) } }
        wordsJob=scope.launch { backend.observeWords(r.id).catch { status=friendly(it) }.collect { words=it } }
        chatJob=scope.launch { backend.observeChat(r.id).catch { status=friendly(it) }.collect { chat=it } }
    }
    suspend fun ensure() { backend.ensurePlayer(name); backend.setPresence("online") }
    suspend fun refreshSocial() { friends=backend.getFriends(); invites=backend.getIncomingGameInvites(); requests=backend.getIncomingFriendRequests() }

    DisposableEffect(Unit){ onDispose { roomJob?.cancel();wordsJob?.cancel();chatJob?.cancel();matchJob?.cancel();rematchJob?.cancel();scope.launch{runCatching{backend.cancelRandomMatchmaking()};runCatching{backend.setPresence("offline")}} } }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1020),SonHarfBg,Color(0xFF05070D))))) {
        val active=room
        if(active==null) {
            LobbyV2(name,{name=it.take(24)},language,{language=it},status,busy,matching,showFriends,friends,invites,requests,
                onRandom={ scope.launch { busy=true; runCatching{ensure();backend.startRandomMatchmaking(language)}.onSuccess{matching=true;status="Rakip aranıyor…"}.onFailure{status=friendly(it)};busy=false; if(matching){matchJob?.cancel();matchJob=launch{while(matching&&room==null){val found=runCatching{backend.pollRandomMatchmakingRoom()}.getOrNull();if(found!=null){room=found;status="Rakip bulundu!";observe(found);break};delay(1500)}}} } },
                onCancel={scope.launch{matching=false;matchJob?.cancel();backend.cancelRandomMatchmaking();status="Eşleşme iptal edildi."}},
                onFriends={scope.launch{busy=true;runCatching{ensure();refreshSocial()}.onSuccess{showFriends=!showFriends}.onFailure{status=friendly(it)};busy=false}},
                onInvite={id->scope.launch{runCatching{backend.inviteFriend(id,language);refreshSocial()}.onSuccess{status="Oyun daveti gönderildi."}.onFailure{status=friendly(it)}}},
                onAcceptRequest={id->scope.launch{runCatching{backend.respondFriendRequest(id,true);refreshSocial()}.onFailure{status=friendly(it)}}},
                onInviteResponse={id,accept->scope.launch{runCatching{backend.respondGameInvite(id,accept)}.onSuccess{joined->if(joined!=null){room=joined;observe(joined)};refreshSocial()}.onFailure{status=friendly(it)}}}
            )
        } else {
            val me=backend.currentUserId()
            val opponent=if(me==active.hostId) active.guestId else active.hostId
            GameV2(active,me,words,chat,triviaRound,triviaQuestion,wordInput,{wordInput=it.take(40)},chatInput,{chatInput=it.take(300)},status,busy,rematchRequested,
                onWord={scope.launch{busy=true;runCatching{backend.submitWord(active.id,wordInput)}.onSuccess{room=it;wordInput="";status=gameEvent(it,me)}.onFailure{status=friendly(it)};busy=false}},
                onTimeout={scope.launch{runCatching{backend.claimTurnTimeout(active.id)}.onSuccess{room=it;status=gameEvent(it,me)}}},
                onTrivia={idx->scope.launch{val q=triviaRound?:return@launch;runCatching{backend.answerTrivia(q.id,idx)}.onSuccess{room=it;refreshQuiz(it);status=gameEvent(it,me)}.onFailure{status=friendly(it)}}},
                onChat={scope.launch{runCatching{backend.sendChat(active.id,chatInput)}.onSuccess{chatInput=""}.onFailure{status=friendly(it)}}},
                onBlock={if(opponent!=null)scope.launch{runCatching{backend.blockUser(opponent)};status="Oyuncu engellendi."}},
                onReport={if(opponent!=null)scope.launch{runCatching{backend.reportUser(opponent,active.id,"Uygunsuz sohbet")};status="Rapor kaydedildi."}},
                onPhoto={if(opponent!=null)scope.launch{runCatching{backend.setPhotoAccess(opponent,true)};status="Fotoğraf bu oyuncuya açıldı."}},
                onFriend={if(opponent!=null)scope.launch{runCatching{backend.sendFriendRequest(opponent)};status="Arkadaşlık isteği gönderildi."}},
                onForfeit={scope.launch{runCatching{backend.forfeit(active.id)}.onSuccess{room=it}}},
                onExit={roomJob?.cancel();wordsJob?.cancel();chatJob?.cancel();room=null;words=emptyList();chat=emptyList();status="Yeni rakibini seç."},
                onRematch={ if(!rematchRequested){rematchRequested=true;status="Rövanş isteği gönderildi. Rakip bekleniyor…";rematchJob?.cancel();rematchJob=scope.launch{while(rematchRequested&&room?.id==active.id){val r=runCatching{backend.requestRematch(active.id)}.getOrNull();if(r!=null&&r.id!=active.id){room=r;words=emptyList();chat=emptyList();status="Rövanş başlıyor!";observe(r);break};delay(1500)}}} }
            )
        }
    }
}

@Composable private fun LobbyV2(name:String,onName:(String)->Unit,language:String,onLanguage:(String)->Unit,status:String,busy:Boolean,matching:Boolean,showFriends:Boolean,friends:List<Pair<FriendshipDto,ProfileDto>>,invites:List<GameInviteDto>,requests:List<Pair<FriendshipDto,ProfileDto>>,onRandom:()->Unit,onCancel:()->Unit,onFriends:()->Unit,onInvite:(String)->Unit,onAcceptRequest:(String)->Unit,onInviteResponse:(String,Boolean)->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Text("ONLINE DÜELLO",fontSize=30.sp,fontWeight=FontWeight.Black);Text("3 round • her round 10 geçerli kelime",color=SonHarfMuted)}
        item{Surface(color=SonHarfPurple.copy(alpha=.12f),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,SonHarfPurple.copy(alpha=.35f))){Text(status,Modifier.fillMaxWidth().padding(14.dp))}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){FilterChip(selected=language=="tr",onClick={onLanguage("tr")},label={Text("🇹🇷 Türkçe")});FilterChip(selected=language=="en",onClick={onLanguage("en")},label={Text("🇬🇧 English")})}}
        item{DarkFieldV2(name,onName,"Oyuncu adı")}
        item{if(!matching)Button(onClick=onRandom,enabled=!busy&&name.trim().length>=2,modifier=Modifier.fillMaxWidth().height(60.dp),shape=RoundedCornerShape(20.dp)){Text("⚡ RASTGELE RAKİP BUL",fontWeight=FontWeight.Black)} else Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(22.dp)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator();Spacer(Modifier.height(8.dp));Text("RAKİP ARANIYOR…",fontWeight=FontWeight.Black);TextButton(onClick=onCancel){Text("İPTAL ET")}}}}
        item{OutlinedButton(onClick=onFriends,enabled=!busy&&name.trim().length>=2,modifier=Modifier.fillMaxWidth().height(54.dp),border=BorderStroke(1.dp,SonHarfCyan)){Text("👥 ARKADAŞLARINLA OYNA",fontWeight=FontWeight.Black)}}
        if(showFriends){item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(22.dp)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){if(invites.isNotEmpty())Text("OYUN DAVETLERİ",color=SonHarfGold,fontWeight=FontWeight.Black);invites.forEach{inv->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${if(inv.language=="tr")"TR" else "EN"} daveti");Row{TextButton(onClick={onInviteResponse(inv.id,true)}){Text("Kabul")};TextButton(onClick={onInviteResponse(inv.id,false)}){Text("Reddet")}}}};if(requests.isNotEmpty())Text("ARKADAŞLIK İSTEKLERİ",color=SonHarfGold,fontWeight=FontWeight.Black);requests.forEach{(_,p)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(p.displayName);TextButton(onClick={onAcceptRequest(p.id)}){Text("Kabul")}}};Text("ARKADAŞLAR",color=SonHarfCyan,fontWeight=FontWeight.Black);if(friends.isEmpty())Text("Henüz arkadaş yok.",color=SonHarfMuted);friends.forEach{(_,p)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text(p.displayName,fontWeight=FontWeight.Bold);Text(when(p.presenceStatus){"online"->"Çevrimiçi";"in_game"->"Oyunda";else->"Çevrimdışı"},color=SonHarfMuted,fontSize=11.sp)};Button(onClick={onInvite(p.id)},enabled=p.presenceStatus=="online"){Text("Davet")}}}}}}}
    }
}

@Composable private fun GameV2(r:GameRoomDto,me:String?,words:List<GameWordDto>,chat:List<ChatMessageDto>,triviaRound:TriviaRoundDto?,triviaQuestion:TriviaQuestionDto?,wordInput:String,onWord:(String)->Unit,chatInput:String,onChat:(String)->Unit,status:String,busy:Boolean,rematchRequested:Boolean,onWord:()->Unit,onTimeout:()->Unit,onTrivia:(Int)->Unit,onChat:()->Unit,onBlock:()->Unit,onReport:()->Unit,onPhoto:()->Unit,onFriend:()->Unit,onForfeit:()->Unit,onExit:()->Unit,onRematch:()->Unit){
    val host=me==r.hostId; val myScore=if(host)r.hostScore else r.guestScore; val oppScore=if(host)r.guestScore else r.hostScore; val myRounds=if(host)r.hostRounds else r.guestRounds; val oppRounds=if(host)r.guestRounds else r.hostRounds; val myTurn=r.currentPlayerId==me&&r.status in listOf("playing","sudden_death","final"); val required=words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar(); var seconds by remember(r.turnDeadline){mutableStateOf(45)}
    LaunchedEffect(r.turnDeadline,r.currentPlayerId,r.status){while(r.turnDeadline!=null&&r.status in listOf("playing","sudden_death","final")){val d=runCatching{Instant.parse(r.turnDeadline)}.getOrNull();seconds=d?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0)?:45;if(seconds<=0){onTimeout();break};delay(1000)}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(22.dp)){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(if(r.status=="sudden_death")"ANİ ÖLÜM" else "ROUND ${r.roundNo}/3",color=SonHarfCyan,fontWeight=FontWeight.Black);Text(if(myTurn)"SIRA SENDE" else "RAKİBİN SIRASI",fontSize=20.sp,fontWeight=FontWeight.Black)};Surface(color=if(seconds<=10)Color(0xFF5A202C) else SonHarfPurple,shape=RoundedCornerShape(999.dp)){Text("$seconds sn",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontWeight=FontWeight.Black)}};Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("SEN $myScore  •  $myRounds round",fontWeight=FontWeight.Bold);Text("${r.roundWordCount}/10",color=SonHarfMuted);Text("$oppRounds round  •  $oppScore RAKİP",fontWeight=FontWeight.Bold)}}}}
        if(r.status=="quiz") item{TriviaV2(triviaRound,triviaQuestion,onTrivia)} else if(r.status!="finished") item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("SON HARF",color=SonHarfMuted,fontSize=12.sp);Text(required?.toString()?:"•",fontSize=64.sp,fontWeight=FontWeight.Black,color=SonHarfCyan);Text(if(required==null)"İlk kelimeyi yaz" else "$required ile başlayan kelime yaz",color=SonHarfMuted);Spacer(Modifier.height(14.dp));DarkFieldV2(wordInput,onWord,"Kelime");Spacer(Modifier.height(8.dp));Button(onClick=onWord,enabled=myTurn&&wordInput.trim().length>=2&&!busy,modifier=Modifier.fillMaxWidth()){Text("GÖNDER",fontWeight=FontWeight.Black)};Text("Doğru +3 • Hatalı/Süre −1 • 5 kusursuz doğru +3",color=SonHarfMuted,fontSize=10.sp,modifier=Modifier.padding(top=8.dp))}}}
        if(r.status=="finished") item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF11263A)),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){Text(if(r.winnerId==me)"KAZANDIN ✦" else "MAÇ BİTTİ",fontSize=24.sp,fontWeight=FontWeight.Black);Text("Round $myRounds - $oppRounds • Puan $myScore - $oppScore",color=SonHarfMuted);Button(onClick=onRematch,enabled=!rematchRequested,modifier=Modifier.fillMaxWidth()){Text(if(rematchRequested)"RAKİP BEKLENİYOR…" else "↻ TEKRAR OYNA",fontWeight=FontWeight.Black)};OutlinedButton(onClick=onFriend,modifier=Modifier.fillMaxWidth()){Text("+ ARKADAŞ EKLE")};TextButton(onClick=onExit){Text("LOBİYE DÖN")}}}}
        item{if(status.isNotBlank())Text(status,color=SonHarfMuted,fontSize=11.sp)}
        item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("CANLI SOHBET",color=SonHarfCyan,fontWeight=FontWeight.Black);Row{TextButton(onClick=onPhoto){Text("Fotoğraf",fontSize=10.sp)};TextButton(onClick=onReport){Text("Rapor",fontSize=10.sp)};TextButton(onClick=onBlock){Text("Engelle",color=Color(0xFFFF8894),fontSize=10.sp)}}};if(chat.isNotEmpty())Text(chat.takeLast(3).joinToString("\n"){(if(it.senderId==me)"Sen: " else "Rakip: ")+it.body},color=SonHarfMuted,fontSize=12.sp);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(chatInput,onChat,placeholder={Text("Mesaj yaz")},singleLine=true,modifier=Modifier.weight(1f));Button(onClick=onChat,enabled=chatInput.isNotBlank()){Text("➤")}}}}}
        if(r.status!="finished") item{OutlinedButton(onClick=onForfeit,modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,Color(0xFF6C3340))){Text("PES ET",color=Color(0xFFFF8894))}}
    }
}

@Composable private fun TriviaV2(round:TriviaRoundDto?,q:TriviaQuestionDto?,onAnswer:(Int)->Unit){if(round==null||q==null){Box(Modifier.fillMaxWidth().height(220.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()};return};var unlocked by remember(round.revealAt){mutableStateOf(false)};var s by remember(round.revealAt){mutableStateOf(3)};LaunchedEffect(round.revealAt){while(true){val r=runCatching{Instant.parse(round.revealAt)}.getOrNull();s=r?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0)?:0;unlocked=s<=0;if(unlocked)break;delay(250)}};Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxWidth().padding(18.dp)){Text("🧠 GENEL KÜLTÜR • +${round.bonusPoints}",color=SonHarfGold,fontWeight=FontWeight.Black);Spacer(Modifier.height(10.dp));Text(q.question,fontSize=20.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(10.dp));if(!unlocked)Text("Şıklar $s saniye sonra açılacak…",color=SonHarfCyan) else listOf(q.optionA,q.optionB,q.optionC,q.optionD).forEachIndexed{i,o->OutlinedButton(onClick={onAnswer(i)},modifier=Modifier.fillMaxWidth().padding(vertical=3.dp)){Text("${'A'+i}) $o",modifier=Modifier.fillMaxWidth())}}}}}
@Composable private fun DarkFieldV2(value:String,onValue:(String)->Unit,label:String){OutlinedTextField(value,onValue,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=SonHarfCyan,unfocusedBorderColor=SonHarfSurface2,focusedContainerColor=Color(0xFF0D1322),unfocusedContainerColor=Color(0xFF0D1322)))}
@Composable private fun MissingBackendV2(){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Supabase bağlantısı yapılandırılmalı.",textAlign=TextAlign.Center)}}
private fun gameEvent(r:GameRoomDto,me:String?):String=when(r.lastEvent){"streak_bonus"->if(r.lastEventPlayerId==me)"🔥 SÖZ FIRTINASI! +3 ekstra" else "Rakip seri yaptı!";"invalid_word","not_in_dictionary","wrong_start_letter","word_already_used"->"Geçersiz hamle: −1 ve sıra değişti.";"turn_expired"->"Süre doldu: −1 ve sıra değişti.";"quiz_started"->"🧠 Bonus soru! 3 saniye oku.";"match_finished"->"Maç tamamlandı.";"sudden_death_started"->"⚡ ANİ ÖLÜM! İlk hata kaybettirir.";else->"Hamle işlendi."}
