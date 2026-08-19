package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreenV5() {
    if (!SupabaseProvider.configured) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sunucu bağlantısı yapılandırılmamış.") }; return }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var language by remember { mutableStateOf("tr") }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var privateCode by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Hazır") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var showPrivate by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var roomJob by remember { mutableStateOf<Job?>(null) }; var wordsJob by remember { mutableStateOf<Job?>(null) }; var chatJob by remember { mutableStateOf<Job?>(null) }; var matchJob by remember { mutableStateOf<Job?>(null) }

    fun friendly(raw: String) = when {
        "player_already_in_game" in raw -> "Aktif maçın bulundu. Maça dönülüyor…"
        "not_your_turn" in raw -> "Sıra rakibinde."
        "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" in raw -> "Kelime son harfle başlamalı."
        "not_in_dictionary" in raw -> "Bu kelime sözlükte bulunamadı."
        "invalid_word" in raw -> "Bu kelime geçerli değil."
        "turn_expired" in raw -> "Süren doldu. −1 puan."
        "room_not_available" in raw -> "Bu oda artık kullanılamıyor."
        "vip_required" in raw -> "Özel oda açmak için VIP gerekli."
        else -> "Bağlantı sorunu. Yeniden deneniyor."
    }
    fun eventMessage(e: String?) = when(e) { "word_already_used"->"Bu kelime daha önce kullanıldı."; "wrong_start_letter"->"Kelime son harfle başlamalı."; "not_in_dictionary"->"Bu kelime sözlükte bulunamadı."; "invalid_word"->"Bu kelime geçerli değil."; "turn_expired"->"Süren doldu. −1 puan."; else->"" }
    fun failedEvent(e: String?) = e in setOf("word_already_used","wrong_start_letter","not_in_dictionary","invalid_word","turn_expired")
    suspend fun ensureProfile(): ProfileDto { if (backend.currentUserId()==null) backend.ensurePlayer("Oyuncu"); val id=requireNotNull(backend.currentUserId()); return runCatching{backend.getProfile(id)}.getOrElse{backend.ensurePlayer("Oyuncu")}.also{profile=it} }
    suspend fun activeRoom(): GameRoomDto? { val me=backend.currentUserId()?:return null; return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>().filter{(it.hostId==me||it.guestId==me)&&it.status in listOf("waiting","playing","quiz","final","sudden_death","paused")}.maxByOrNull{it.validWordCount} }
    suspend fun refreshQuiz(r:GameRoomDto){ if(r.status=="quiz"){triviaRound=backend.getActiveTriviaRound(r.id); triviaQuestion=triviaRound?.let{backend.getTriviaQuestion(it.questionId)}}else{triviaRound=null;triviaQuestion=null} }
    suspend fun refreshOpponent(r:GameRoomDto){ if(r.isBot){opponentProfile=null;return}; val me=backend.currentUserId(); val oid=if(r.hostId==me)r.guestId else r.hostId; opponentProfile=oid?.let{runCatching{backend.getProfile(it)}.getOrNull()} }
    fun observe(r:GameRoomDto){ roomJob?.cancel();wordsJob?.cancel();chatJob?.cancel();matchJob?.cancel();matching=false; scope.launch{refreshOpponent(r)}; roomJob=scope.launch{backend.observeRoom(r.id).catch{notice=friendly(it.message.orEmpty())}.collect{room=it;refreshQuiz(it);refreshOpponent(it)}}; wordsJob=scope.launch{backend.observeWords(r.id).catch{notice=friendly(it.message.orEmpty())}.collect{words=it}}; if(!r.isBot)chatJob=scope.launch{backend.observeChat(r.id).catch{notice=friendly(it.message.orEmpty())}.collect{chat=it}} }

    LaunchedEffect(Unit){ busy=true; runCatching{ensureProfile()}.onSuccess{p->val old=runCatching{activeRoom()}.getOrNull();if(old!=null){room=old;observe(old);notice="${p.displayName}, aktif maçına dönüldü."}else notice="${p.displayName}, düelloya hazırsın."}.onFailure{notice=friendly(it.message.orEmpty())};busy=false }
    DisposableEffect(Unit){onDispose{roomJob?.cancel();wordsJob?.cancel();chatJob?.cancel();matchJob?.cancel()}}
    val active=room
    if(active==null){
        ReferenceDuelLobby(playerName=profile?.displayName?:"Oyuncu",language=language,matching=matching,notice=notice,showPrivate=showPrivate,showFriends=showFriends,privateCode=privateCode,friends=friends,invites=invites,
            onLanguage={language=it;SonHarfSoundFx.tap()},onPrivateCode={privateCode=it.filter(Char::isLetterOrDigit).uppercase().take(6)},
            onRandom={scope.launch{busy=true;runCatching{ensureProfile();backend.startRandomMatchmaking(language)}.onSuccess{matching=true;notice="Rakip aranıyor…"}.onFailure{if("player_already_in_game" in it.message.orEmpty()){val old=runCatching{activeRoom()}.getOrNull();if(old!=null){room=old;observe(old)}else notice=friendly(it.message.orEmpty())}else notice=friendly(it.message.orEmpty())};busy=false;if(matching)matchJob=launch{while(matching&&room==null){val found=runCatching{backend.pollRandomMatchmakingRoom()}.getOrNull();if(found!=null){room=found;observe(found);SonHarfSoundFx.softNotify();break};delay(900)}}}},
            onCancel={scope.launch{matching=false;matchJob?.cancel();runCatching{backend.cancelRandomMatchmaking()};notice="Eşleşme iptal edildi."}},
            onPrivate={showPrivate=!showPrivate;showFriends=false},onFriends={scope.launch{friends=runCatching{backend.getFriends()}.getOrDefault(emptyList());invites=runCatching{backend.getIncomingGameInvites()}.getOrDefault(emptyList());showFriends=!showFriends;showPrivate=false}},
            onCreate={scope.launch{busy=true;runCatching{backend.createPrivateRoom(language)}.onSuccess{room=it;observe(it)}.onFailure{notice=friendly(it.message.orEmpty())};busy=false}},onJoin={scope.launch{busy=true;runCatching{backend.joinPrivateRoom(privateCode)}.onSuccess{room=it;language=it.language;observe(it)}.onFailure{notice=friendly(it.message.orEmpty())};busy=false}},
            onInvite={id->scope.launch{runCatching{backend.inviteFriend(id,language)};notice="Davet gönderildi."}},onInviteResponse={id,accept->scope.launch{runCatching{backend.respondGameInvite(id,accept)}.onSuccess{if(it!=null){room=it;observe(it)}}}})
    } else {
        val me=backend.currentUserId()
        LaunchedEffect(active.currentPlayerId,active.validWordCount,active.roundNo){wordInput=""}
        LaunchedEffect(active.id){while(true){if(!active.isBot&&active.status!="waiting")runCatching{backend.heartbeatRoom(active.id)}.onSuccess{room=it};delay(5000)}}
        LaunchedEffect(active.id,active.status,active.botTurn,active.validWordCount){if(active.isBot&&active.botTurn&&active.status in listOf("playing","final","sudden_death")){delay(1600L+(active.validWordCount%4)*350L);runCatching{backend.botTakeTurn(active.id)}.onSuccess{room=it}.onFailure{notice=friendly(it.message.orEmpty())}}}
        ReferenceDuelArena(room=active,me=me,playerName=profile?.displayName?:"Sen",opponentName=if(active.isBot)"${active.botName?:"KelimeBot"} BOT" else opponentProfile?.displayName?:"Rakip",words=words,wordInput=wordInput,onWordInput={wordInput=it.take(40)},notice=notice,busy=busy,triviaRound=triviaRound,triviaQuestion=triviaQuestion,
            onSubmit={scope.launch{val submitted=wordInput.trim();if(submitted.isBlank())return@launch;wordInput="";busy=true;SonHarfSoundFx.tap();runCatching{backend.submitWord(active.id,submitted)}.onSuccess{result->room=result;if(failedEvent(result.lastEvent)&&result.lastEventPlayerId==me){notice=eventMessage(result.lastEvent);SonHarfSoundFx.warning()}else{notice="Kelime kabul edildi: ${submitted.uppercase()}";SonHarfSoundFx.wordAccepted()}}.onFailure{notice=friendly(it.message.orEmpty());SonHarfSoundFx.warning()};busy=false}},
            onTimeout={scope.launch{runCatching{backend.claimTurnTimeout(active.id)}.onSuccess{room=it}}},onTrivia={idx->scope.launch{val q=triviaRound?:return@launch;runCatching{backend.answerTrivia(q.id,idx)}.onSuccess{room=it;refreshQuiz(it)}.onFailure{notice=friendly(it.message.orEmpty())}}},onChat={showChat=true},onForfeit={scope.launch{runCatching{backend.forfeit(active.id)}.onSuccess{room=it}}},onExit={roomJob?.cancel();wordsJob?.cancel();chatJob?.cancel();room=null;words=emptyList();chat=emptyList();notice="Yeni düelloya hazırsın."},onRematch={scope.launch{runCatching{if(active.isBot)backend.restartBotMatch(active.id)else backend.requestRematch(active.id)}.onSuccess{room=it;words=emptyList();chat=emptyList();if(it.id!=active.id)observe(it)}.onFailure{notice=friendly(it.message.orEmpty())}}})
        if(showChat&&!active.isBot)ChatDialog(chat,me,chatInput,{chatInput=it.take(300)},{showChat=false}){scope.launch{runCatching{backend.sendChat(active.id,chatInput)}.onSuccess{chatInput=""}}}
    }
}

@Composable private fun ReferenceDuelLobby(playerName:String,language:String,matching:Boolean,notice:String,showPrivate:Boolean,showFriends:Boolean,privateCode:String,friends:List<Pair<FriendshipDto,ProfileDto>>,invites:List<GameInviteDto>,onLanguage:(String)->Unit,onPrivateCode:(String)->Unit,onRandom:()->Unit,onCancel:()->Unit,onPrivate:()->Unit,onFriends:()->Unit,onCreate:()->Unit,onJoin:()->Unit,onInvite:(String)->Unit,onInviteResponse:(String,Boolean)->Unit){
    LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("‹   DÜELLO",fontSize=22.sp,fontWeight=FontWeight.Black);Surface(color=SonHarfSurface2,shape=RoundedCornerShape(99.dp)){Text(playerName,Modifier.padding(12.dp,7.dp),color=SonHarfCyan,fontWeight=FontWeight.Bold)}}}
        item{Box(Modifier.fillMaxWidth().height(270.dp),contentAlignment=Alignment.Center){Surface(Modifier.size(200.dp),shape=CircleShape,color=SonHarfPurple.copy(.08f),border=BorderStroke(2.dp,SonHarfPurple.copy(.7f))){};Surface(Modifier.size(154.dp),shape=CircleShape,color=Color.Transparent,border=BorderStroke(2.dp,SonHarfCyan.copy(.55f))){};Column(horizontalAlignment=Alignment.CenterHorizontally){if(matching)CircularProgressIndicator(Modifier.size(42.dp),strokeWidth=3.dp);Spacer(Modifier.height(10.dp));Text(if(matching)"RAKİP\nARANIYOR" else "DÜELLOYA\nHAZIR",textAlign=TextAlign.Center,fontWeight=FontWeight.Black,fontSize=20.sp)}}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(language=="tr",{onLanguage("tr")},{Text("🇹🇷 Türkçe")},modifier=Modifier.weight(1f));FilterChip(language=="en",{onLanguage("en")},{Text("🇬🇧 English")},modifier=Modifier.weight(1f))}}
        item{if(matching)Button(onClick=onCancel,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF5A1830))){Text("✕  İPTAL",fontWeight=FontWeight.Black)}else Button(onClick=onRandom,modifier=Modifier.fillMaxWidth().height(60.dp)){Text("DÜELLOYA GİR",fontWeight=FontWeight.Black,fontSize=18.sp)}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick=onFriends,modifier=Modifier.weight(1f)){Text("👥 ARKADAŞ")};OutlinedButton(onClick=onPrivate,modifier=Modifier.weight(1f)){Text("♟ ÖZEL ODA")}}}
        item{Text(notice,color=SonHarfMuted,fontSize=12.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())}
        if(showPrivate)item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("ÖZEL ODA",fontWeight=FontWeight.Black);Button(onClick=onCreate,modifier=Modifier.fillMaxWidth()){Text("VIP ODA OLUŞTUR")};OutlinedTextField(privateCode,onPrivateCode,modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Oda kodu")});OutlinedButton(onClick=onJoin,enabled=privateCode.length==6,modifier=Modifier.fillMaxWidth()){Text("ODA KODUYLA KATIL")}}}}
        if(showFriends)item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){invites.forEach{i->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Maç daveti");Row{TextButton(onClick={onInviteResponse(i.id,true)}){Text("Kabul")};TextButton(onClick={onInviteResponse(i.id,false)}){Text("Reddet")}}}};friends.forEach{(_,p)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(p.displayName);Button(onClick={onInvite(p.id)},enabled=p.presenceStatus=="online"){Text("Davet")}}}}}}
    }
}

@Composable private fun ReferenceDuelArena(room:GameRoomDto,me:String?,playerName:String,opponentName:String,words:List<GameWordDto>,wordInput:String,onWordInput:(String)->Unit,notice:String,busy:Boolean,triviaRound:TriviaRoundDto?,triviaQuestion:TriviaQuestionDto?,onSubmit:()->Unit,onTimeout:()->Unit,onTrivia:(Int)->Unit,onChat:()->Unit,onForfeit:()->Unit,onExit:()->Unit,onRematch:()->Unit){
    val host=me==room.hostId;val myScore=if(host)room.hostScore else room.guestScore;val oppScore=if(host)room.guestScore else room.hostScore;val myRounds=if(host)room.hostRounds else room.guestRounds;val oppRounds=if(host)room.guestRounds else room.hostRounds;val myTurn=room.currentPlayerId==me&&room.status in listOf("playing","final","sudden_death");val last=words.lastOrNull()?.normalizedWord;val required=last?.lastOrNull()?.uppercaseChar()?.toString()?:"•";var seconds by remember(room.turnDeadline){mutableStateOf(45)};val focus=LocalFocusManager.current
    LaunchedEffect(room.turnDeadline,room.currentPlayerId,room.status){while(room.turnDeadline!=null&&room.status in listOf("playing","final","sudden_death")){seconds=runCatching{(Instant.parse(room.turnDeadline).epochSecond-Instant.now().epochSecond).toInt().coerceAtLeast(0)}.getOrDefault(45);if(seconds<=0){onTimeout();break};if(seconds in 1..5)SonHarfSoundFx.countdown();delay(1000)}}
    Column(Modifier.fillMaxSize().padding(horizontal=10.dp,vertical=8.dp).imePadding(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){CompactPlayer(playerName,myScore,myRounds,myTurn,Modifier.weight(1f),SonHarfPurple);Surface(Modifier.width(72.dp).height(76.dp),shape=RoundedCornerShape(22.dp),color=Color(0xFF121D31)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("$seconds",fontSize=26.sp,fontWeight=FontWeight.Black);Text("SANİYE",color=SonHarfMuted,fontSize=8.sp)}};CompactPlayer(opponentName,oppScore,oppRounds,!myTurn,Modifier.weight(1f),SonHarfPink)}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(if(room.status=="sudden_death")"ANİ ÖLÜM" else "ROUND ${room.roundNo}/3",fontWeight=FontWeight.Black,fontSize=13.sp);Column(horizontalAlignment=Alignment.CenterHorizontally){Text("${room.roundWordCount}/10",fontWeight=FontWeight.Black);LinearProgressIndicator(progress={ (room.roundWordCount/10f).coerceIn(0f,1f)},modifier=Modifier.width(88.dp).height(4.dp))};Text(if(myTurn)"SIRA SENDE" else if(room.isBot&&room.botTurn)"BOT" else "RAKİP",color=if(myTurn)SonHarfCyan else SonHarfMuted,fontSize=11.sp,fontWeight=FontWeight.Bold)}
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Surface(Modifier.size(112.dp),shape=CircleShape,color=SonHarfPurple.copy(.15f),border=BorderStroke(2.dp,Brush.sweepGradient(listOf(SonHarfPurple,SonHarfCyan,SonHarfPurple)))){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("SON HARF",color=SonHarfMuted,fontSize=9.sp);Text(required,color=SonHarfCyan,fontSize=48.sp,fontWeight=FontWeight.Black)}};Text(if(last==null)"İlk kelime serbest" else "“$required” ile başlayan bir kelime yaz",color=SonHarfMuted,fontSize=12.sp,modifier=Modifier.padding(top=5.dp))}
        if(words.isNotEmpty())Column{Text("KELİME ZİNCİRİ",color=SonHarfMuted,fontSize=9.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(words.takeLast(18)){w->Surface(shape=RoundedCornerShape(12.dp),color=SonHarfSurface2,border=BorderStroke(1.dp,Color.White.copy(.05f))){Text(w.word.uppercase(),Modifier.padding(horizontal=10.dp,vertical=7.dp),fontSize=11.sp,fontWeight=FontWeight.Bold)}}}}
        Surface(Modifier.fillMaxWidth(),color=if(notice.startsWith("Bu ")||notice.contains("doldu"))Color(0xFF3A1422)else Color(0xFF0D1526),shape=RoundedCornerShape(12.dp)){Text(notice,Modifier.padding(10.dp),color=if(notice.startsWith("Bu ")||notice.contains("doldu"))Color(0xFFFF7798)else SonHarfMuted,fontSize=11.sp,textAlign=TextAlign.Center)}
        Spacer(Modifier.weight(1f))
        if(room.status=="quiz"&&triviaRound!=null&&triviaQuestion!=null)Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF17152C)),border=BorderStroke(1.dp,SonHarfPurple.copy(.5f))){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("⭐ BONUS +${triviaRound.bonusPoints}",color=SonHarfGold,fontWeight=FontWeight.Black);Text(triviaQuestion.question,fontSize=13.sp,fontWeight=FontWeight.Bold);listOf(triviaQuestion.optionA,triviaQuestion.optionB,triviaQuestion.optionC,triviaQuestion.optionD).forEachIndexed{i,s->OutlinedButton(onClick={onTrivia(i)},modifier=Modifier.fillMaxWidth().heightIn(min=38.dp)){Text(s,fontSize=11.sp)}}}}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=wordInput,onValueChange=onWordInput,enabled=myTurn&&!busy,singleLine=true,modifier=Modifier.weight(1f),placeholder={Text(if(myTurn)"Kelimenizi yazın…" else "Rakibin sırası…")},shape=RoundedCornerShape(20.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={if(myTurn&&wordInput.isNotBlank()&&!busy){focus.clearFocus();onSubmit()}}));Button(onClick={focus.clearFocus();onSubmit()},enabled=myTurn&&wordInput.isNotBlank()&&!busy,modifier=Modifier.size(56.dp),shape=CircleShape,contentPadding=PaddingValues(0.dp)){Text("➤",fontSize=22.sp)}}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onChat,modifier=Modifier.weight(1f).height(42.dp)){Text("💬 SOHBET",fontSize=10.sp)};OutlinedButton(onClick={},enabled=room.status=="quiz",modifier=Modifier.weight(1f).height(42.dp)){Text("⭐ BONUS",fontSize=10.sp)};OutlinedButton(onClick=onForfeit,modifier=Modifier.weight(1f).height(42.dp),border=BorderStroke(1.dp,SonHarfPink)){Text("⚑ PES ET",color=SonHarfPink,fontSize=10.sp)}}
        if(room.status=="finished"){val won=room.winnerId==me;Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF11192A)),border=BorderStroke(1.dp,if(won)SonHarfGold else SonHarfPink),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(won)"KAZANDIN" else "MAÇ BİTTİ",color=if(won)SonHarfGold else SonHarfPink,fontWeight=FontWeight.Black);Text("$myRounds - $oppRounds   •   $myScore - $oppScore",fontWeight=FontWeight.Black,fontSize=18.sp);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onRematch,modifier=Modifier.weight(1f)){Text("TEKRAR OYNA")};OutlinedButton(onClick=onExit,modifier=Modifier.weight(1f)){Text("LOBİYE DÖN")}}}}}
    }
}

@Composable private fun CompactPlayer(name:String,score:Int,rounds:Int,active:Boolean,modifier:Modifier,accent:Color){Surface(modifier.height(76.dp),shape=RoundedCornerShape(20.dp),color=if(active)accent.copy(.12f)else SonHarfSurface,border=BorderStroke(1.dp,if(active)accent.copy(.55f)else Color.White.copy(.06f))){Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.SpaceBetween){Text(name,color=if(active)accent else SonHarfMuted,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){Text("$score",fontSize=23.sp,fontWeight=FontWeight.Black);Text("R$rounds",color=SonHarfMuted,fontSize=9.sp)}}}}
@Composable private fun ChatDialog(chat:List<ChatMessageDto>,me:String?,input:String,onInput:(String)->Unit,onDismiss:()->Unit,onSend:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("SOHBET")},text={Column(Modifier.heightIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){items(chat.takeLast(30)){m->Surface(color=if(m.senderId==me)SonHarfPurple.copy(.18f)else SonHarfSurface2,shape=RoundedCornerShape(12.dp)){Text(m.body,Modifier.padding(10.dp),fontSize=12.sp)}}};OutlinedTextField(input,onInput,modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Mesaj yaz…")})}},confirmButton={TextButton(onClick=onSend,enabled=input.isNotBlank()){Text("GÖNDER")}},dismissButton={TextButton(onClick=onDismiss){Text("KAPAT")}})}
