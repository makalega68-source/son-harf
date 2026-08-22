package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val G10Bg = Color(0xFFF2EFE6)
private val G10Card = Color(0xFFFFFCF4)
private val G10Ink = Color(0xFF263238)
private val G10Muted = Color(0xFF68736D)
private val G10Teal = Color(0xFF1C8C8C)
private val G10TealDark = Color(0xFF126A6A)
private val G10Gold = Color(0xFFF1B83B)
private val G10Green = Color(0xFF4E9A62)
private val G10Coral = Color(0xFFD96B57)
private val G10Purple = Color(0xFF8066A8)
private val G10Key = Color(0xFFFFF5CE)
private val G10KeyBorder = Color(0xFFE5C967)
private val G10ChatKey = Color(0xFFFFFBEE)
private val G10ChatBg = Color(0xFFE2E9E4)

@Composable
fun V10BattleScreen(onLeaveBattle: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var meProfile by remember { mutableStateOf<V6ProfileDto?>(null) }
    var meAvatar by remember { mutableStateOf<String?>(null) }
    var meVip by remember { mutableStateOf(false) }
    var opponent by remember { mutableStateOf<V6ProfileDto?>(null) }
    var opponentAvatar by remember { mutableStateOf<String?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var matching by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var showWords by remember { mutableStateOf(false) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }

    suspend fun loadSelf() {
        val uid = backend.currentUserId() ?: return
        meProfile = runCatching { v6LoadProfile(uid) }.getOrNull()
        meAvatar = runCatching { AvatarSignedUrl.resolve(meProfile?.avatarPath) }.getOrNull()
        meVip = runCatching { backend.getProfile(uid).isVip }.getOrDefault(false)
    }
    suspend fun loadOpponent(r: GameRoomDto) {
        if (r.isBot) { opponent = null; opponentAvatar = null; return }
        val uid = backend.currentUserId()
        val oid = if (r.hostId == uid) r.guestId else r.hostId
        opponent = oid?.let { runCatching { v6LoadProfile(it) }.getOrNull() }
        opponentAvatar = runCatching { AvatarSignedUrl.resolve(opponent?.avatarPath) }.getOrNull()
    }
    suspend fun findActive(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == uid || it.guestId == uid) && it.status in listOf("waiting","playing","quiz","final","sudden_death","paused") }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) { loadSelf(); room = runCatching { findActive() }.getOrNull(); room?.let { loadOpponent(it) } }
    val active = room
    if (active == null) {
        V10BattleLobby(meProfile, meAvatar, matching, notice, onLeaveBattle,
            onRandom = {
                scope.launch {
                    if (busy) return@launch
                    busy = true
                    runCatching { backend.startRandomMatchmaking(if (SonHarfUiState.language == "en") "en" else "tr") }
                        .onSuccess { matching = true; notice = "Ratingine yakın rakip aranıyor…" }
                        .onFailure { notice = "Eşleşme başlatılamadı." }
                    busy = false
                    while (matching && room == null) {
                        val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                        if (found != null) { room = found; loadOpponent(found); matching = false; break }
                        delay(800)
                    }
                }
            },
            onCancel = { scope.launch { matching = false; runCatching { backend.cancelRandomMatchmaking() } } }
        )
        return
    }

    val me = backend.currentUserId()
    LaunchedEffect(active.id) {
        var heartbeatTick = 0
        while (isActive) {
            runCatching { backend.getRoom(active.id) }.onSuccess { room = it; loadOpponent(it) }
            runCatching { backend.getWords(active.id) }.onSuccess { words = it }
            runCatching { backend.getChat(active.id) }.onSuccess { chat = it }
            heartbeatTick++
            if (heartbeatTick % 7 == 0 && !active.isBot) runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }
            delay(700)
        }
    }
    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo, words.size) { input = "" }
    LaunchedEffect(active.turnDeadline, active.currentPlayerId, active.status) {
        if (active.status in listOf("playing","final","sudden_death") && active.turnDeadline != null) {
            while (isActive) {
                val left = runCatching { Instant.parse(active.turnDeadline).epochSecond - Instant.now().epochSecond }.getOrDefault(1)
                if (left <= 0) { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it }; break }
                delay(700)
            }
        }
    }
    LaunchedEffect(active.id, active.botTurn, active.status, active.validWordCount) {
        if (active.isBot && active.botTurn && active.status in listOf("playing","final","sudden_death")) {
            delay(750)
            runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }.onFailure { delay(700); runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it } }
        }
    }
    LaunchedEffect(active.id, active.status, active.validWordCount) {
        if (active.status == "quiz") {
            triviaRound = runCatching { backend.getActiveTriviaRound(active.id) }.getOrNull()
            triviaQuestion = triviaRound?.let { runCatching { backend.getTriviaQuestion(it.questionId) }.getOrNull() }
            if (active.isBot) { delay(1000); runCatching { backend.botAnswerTrivia(active.id) }.onSuccess { room = it } }
        } else { triviaRound = null; triviaQuestion = null }
    }

    if (active.status == "finished") {
        V10BattleFinished(active, me, backend, onLeaveBattle) { next -> room = next }
        return
    }

    V10BattleArena(
        room = active, me = me,
        myName = meProfile?.displayName ?: "Sen", myAvatar = meAvatar,
        oppName = if(active.isBot) active.botName ?: "KelimeBot" else opponent?.displayName ?: "Rakip", oppAvatar = opponentAvatar,
        words = words, input = input, notice = notice, busy = busy, vip = meVip,
        onInput = { input = it.take(40) },
        onSubmit = {
            val submitted = input.trim()
            if (submitted.length < 2) return@V10BattleArena
            scope.launch {
                busy = true
                val beforeCount = active.validWordCount
                val result = runCatching { backend.submitWord(active.id, submitted) }
                if (result.isSuccess) {
                    room = result.getOrThrow(); input = ""; notice = "✨ ${submitted.uppercase()} kabul edildi"
                } else {
                    delay(180)
                    val refreshedRoom = runCatching { backend.getRoom(active.id) }.getOrNull()
                    val refreshedWords = runCatching { backend.getWords(active.id) }.getOrDefault(words)
                    val last = refreshedWords.lastOrNull()
                    val lastAcceptedByMe = last?.let { it.playerId == me && it.word.trim().equals(submitted, true) } == true
                    val acceptedOnServer = (refreshedRoom?.validWordCount ?: beforeCount) > beforeCount || lastAcceptedByMe
                    if (acceptedOnServer) {
                        if(refreshedRoom!=null) room=refreshedRoom; words=refreshedWords; input=""; notice="✨ ${submitted.uppercase()} kabul edildi"
                    } else {
                        val raw=result.exceptionOrNull()?.message.orEmpty()
                        notice=when { "not_your_turn" in raw->"Sıra rakibinde"; "wrong_start_letter" in raw->"Doğru başlangıç harfini kullan"; "word_already_used" in raw->"Bu kelime kullanıldı"; "not_in_dictionary" in raw->"Kelime sözlükte yok"; "turn_expired" in raw->"Süren doldu"; else->"Hamle doğrulanamadı" }
                        if(refreshedRoom!=null) room=refreshedRoom; words=refreshedWords
                    }
                }
                busy=false
            }
        },
        onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room=it } } },
        onExit = onLeaveBattle, onChat = { showChat=true }, onWords = { if(meVip)showWords=true else notice="Çıkan Kelimeler VIP özelliğidir" },
        trivia = triviaQuestion,
        onTrivia = { index -> scope.launch { val r=triviaRound?:return@launch; busy=true; runCatching { backend.answerTrivia(r.id,index) }.onSuccess { room=it; notice="Bilgi sorusu yanıtlandı" }; busy=false } }
    )

    if(showChat) V10BattleChat(chat,me,active.language,meProfile?.allowMatchChat!=false,{showChat=false}) { text -> scope.launch { runCatching { backend.sendChat(active.id,text) }.onSuccess { chat=runCatching { backend.getChat(active.id) }.getOrDefault(chat) }.onFailure { notice="Mesaj gönderilemedi" } } }
    if(showWords) AlertDialog(onDismissRequest={showWords=false},title={Text("Çıkan Kelimeler")},text={LazyColumn(Modifier.heightIn(max=420.dp)){items(words,key={it.id}){Text(it.word.uppercase(),Modifier.fillMaxWidth().padding(8.dp))}}},confirmButton={TextButton(onClick={showWords=false}){Text("Kapat")}})
}

@Composable private fun V10BattleLobby(profile:V6ProfileDto?,avatar:String?,matching:Boolean,notice:String,onBack:()->Unit,onRandom:()->Unit,onCancel:()->Unit){
    LazyColumn(Modifier.fillMaxSize().background(G10Bg),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"Geri")};Text("SON HARF",Modifier.weight(1f),textAlign=TextAlign.Center,fontWeight=FontWeight.Black,fontSize=24.sp,color=G10TealDark);Spacer(Modifier.width(48.dp))}}
        item{Surface(shape=RoundedCornerShape(20.dp),color=G10Card,border=BorderStroke(1.dp,G10Gold.copy(alpha=.45f))){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){V10BattleAvatar(avatar,profile?.displayName?:"Oyuncu",58);Spacer(Modifier.width(12.dp));Column{Text(if(matching)"Rakip aranıyor…" else "Düelloya hazırsın",fontWeight=FontWeight.Black,color=G10Ink);Text("Ratinge yakın akıllı eşleştirme",fontSize=11.sp,color=G10Muted)}}}}
        item{if(matching)OutlinedButton(onClick=onCancel,modifier=Modifier.fillMaxWidth().height(56.dp)){Text("EŞLEŞMEYİ İPTAL ET")}else Button(onClick=onRandom,modifier=Modifier.fillMaxWidth().height(64.dp),colors=ButtonDefaults.buttonColors(containerColor=G10Teal),shape=RoundedCornerShape(17.dp)){Icon(Icons.Rounded.Bolt,null);Spacer(Modifier.width(8.dp));Text("1v1 HIZLI KARŞILAŞMA",fontWeight=FontWeight.Black)}}
        item{Text(notice,Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=G10Muted)}
    }
}

@Composable private fun V10BattleArena(room:GameRoomDto,me:String?,myName:String,myAvatar:String?,oppName:String,oppAvatar:String?,words:List<GameWordDto>,input:String,notice:String,busy:Boolean,vip:Boolean,onInput:(String)->Unit,onSubmit:()->Unit,onForfeit:()->Unit,onExit:()->Unit,onChat:()->Unit,onWords:()->Unit,trivia:TriviaQuestionDto?,onTrivia:(Int)->Unit){
    val host=me==room.hostId; val myScore=if(host)room.hostScore else room.guestScore; val oppScore=if(host)room.guestScore else room.hostScore
    val myTurn=room.currentPlayerId==me&&room.status in listOf("playing","final","sudden_death"); val last=words.lastOrNull()?.word?.uppercase().orEmpty(); val req=words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    var seconds by remember(room.turnDeadline){mutableIntStateOf(45)}
    LaunchedEffect(room.turnDeadline,room.currentPlayerId,room.status){while(isActive&&room.turnDeadline!=null&&room.status in listOf("playing","final","sudden_death")){seconds=runCatching{(Instant.parse(room.turnDeadline).epochSecond-Instant.now().epochSecond).toInt().coerceAtLeast(0)}.getOrDefault(45);delay(1000)}}
    val pulse by animateFloatAsState(if(myTurn)1f else .94f,label="turnPulse")
    Column(Modifier.fillMaxSize().background(G10Bg).padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text("SON HARF",Modifier.weight(1f),fontWeight=FontWeight.Black,fontSize=19.sp,color=G10TealDark);Surface(onClick=onWords,shape=RoundedCornerShape(11.dp),color=if(vip)G10Purple else G10Card,border=BorderStroke(1.dp,G10Purple)){Text(if(vip)"📜 Kelimeler" else "🔒 Kelimeler",Modifier.padding(8.dp),color=if(vip)Color.White else G10Purple,fontSize=10.sp,fontWeight=FontWeight.Bold)};IconButton(onClick=onExit){Icon(Icons.Rounded.Close,"Çık")}}
        Surface(shape=RoundedCornerShape(14.dp),color=G10Teal){Row(Modifier.fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.CenterVertically){Text("CANLI 1v1 • SON HARF",Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Black,fontSize=11.sp);TextButton(onClick=onChat){Icon(Icons.Rounded.ChatBubble,null,tint=G10Gold);Text(" Sohbet",color=Color.White)}}}
        Surface(shape=RoundedCornerShape(20.dp),color=G10Card,border=BorderStroke(1.dp,G10Gold.copy(alpha=.4f)),shadowElevation=2.dp){Column(Modifier.fillMaxWidth().padding(13.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){V10Player(myName,myAvatar,myScore,myTurn,Modifier.weight(1f));Box(Modifier.size(66.dp).clip(CircleShape).background(if(seconds<=5)G10Coral else G10Gold),contentAlignment=Alignment.Center){Text("$seconds",color=Color.White,fontWeight=FontWeight.Black,fontSize=25.sp)};V10Player(oppName,oppAvatar,oppScore,!myTurn,Modifier.weight(1f))};Spacer(Modifier.height(8.dp));Text(when{room.status=="quiz"->"BİLGİ SORUSU";room.status=="paused"->"BAĞLANTI BEKLENİYOR";myTurn->"⚡ SIRA SİZDE";else->"RAKİP DÜŞÜNÜYOR…"},fontWeight=FontWeight.Black,color=if(myTurn)G10Green else G10Muted,fontSize=(16*pulse).sp);Spacer(Modifier.height(6.dp));if(last.isBlank())Text("İlk kelimeyi siz başlatın",color=G10Muted)else{Text("SON KELİME",color=G10Muted,fontSize=11.sp);Text(last,color=Color(0xFF243B64),fontSize=30.sp,fontWeight=FontWeight.Black)}}}
        AnimatedVisibility(notice.isNotBlank()){Surface(shape=RoundedCornerShape(12.dp),color=if("kabul" in notice)Color(0xFFE2F2E5) else Color(0xFFFBE8E2)){Text(notice,Modifier.fillMaxWidth().padding(8.dp),textAlign=TextAlign.Center,color=if("kabul" in notice)G10Green else G10Coral,fontWeight=FontWeight.Bold,fontSize=11.sp)}}
        if(room.status=="quiz") Surface(shape=RoundedCornerShape(16.dp),color=G10Card,border=BorderStroke(1.dp,G10Gold)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(trivia?.question?:"Soru yükleniyor…",fontWeight=FontWeight.Black,color=G10Ink);listOf(trivia?.optionA,trivia?.optionB,trivia?.optionC,trivia?.optionD).forEachIndexed{i,o->if(o!=null)OutlinedButton(onClick={onTrivia(i)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(o)}}}}
        else{Surface(Modifier.fillMaxWidth().heightIn(min=78.dp),shape=RoundedCornerShape(14.dp),color=G10Card,border=BorderStroke(2.dp,G10Teal.copy(alpha=.45f))){Column(Modifier.fillMaxWidth().padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(if(input.isBlank())if(req==null)"Kelime yazın…" else "$req ile başlayan kelime yazın…" else input,Modifier.fillMaxWidth(),fontWeight=FontWeight.Black,fontSize=if(input.isBlank())18.sp else 29.sp,textAlign=TextAlign.Center,color=if(input.isBlank())G10Muted else G10TealDark)}};Spacer(Modifier.weight(1f));V10GameKeyboard(myTurn&&!busy,myTurn&&!busy&&input.length>=2&&(req==null||input.firstOrNull()?.uppercaseChar()==req),{onInput(input+it)},{if(input.isNotEmpty())onInput(input.dropLast(1))},onSubmit)}
        TextButton(onClick=onForfeit,modifier=Modifier.align(Alignment.CenterHorizontally)){Icon(Icons.Rounded.Flag,null,tint=G10Coral);Text(" Pes Et",color=G10Coral,fontWeight=FontWeight.Bold)}
    }
}

@Composable private fun V10Player(name:String,url:String?,score:Int,active:Boolean,modifier:Modifier){Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){V10BattleAvatar(url,name,50);Text(name,maxLines=1,fontWeight=FontWeight.Bold,fontSize=11.sp,color=G10Ink);Text("$score puan",fontSize=10.sp,color=if(active)G10Green else G10Muted)}}

@Composable private fun V10GameKeyboard(enabled:Boolean,submitEnabled:Boolean,onKey:(Char)->Unit,onDelete:()->Unit,onSubmit:()->Unit){
    val r1=listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü');val r2=listOf('A','S','D','F','G','H','J','K','L','Ş','İ');val r3=listOf('Z','X','C','V','B','N','M','Ö','Ç')
    Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFFDCE7E1),shadowElevation=2.dp){Column(Modifier.padding(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){listOf(r1,r2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){row.forEach{c->V10Key(c,Modifier.weight(1f),enabled){onKey(c)}}}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){Button(onClick=onDelete,enabled=enabled,modifier=Modifier.weight(1.7f).height(46.dp),colors=ButtonDefaults.buttonColors(containerColor=G10Coral),contentPadding=PaddingValues(0.dp)){Text("⌫ SİL",fontSize=10.sp)};r3.forEach{c->V10Key(c,Modifier.weight(1f),enabled){onKey(c)}};Button(onClick=onSubmit,enabled=submitEnabled,modifier=Modifier.weight(1.9f).height(46.dp),colors=ButtonDefaults.buttonColors(containerColor=G10Teal),contentPadding=PaddingValues(0.dp)){Text("✓ ONAY",fontSize=10.sp,fontWeight=FontWeight.Black)}}}}
}
@Composable private fun V10Key(c:Char,modifier:Modifier,enabled:Boolean,onClick:()->Unit){Surface(onClick=onClick,enabled=enabled,modifier=modifier.height(46.dp),shape=RoundedCornerShape(8.dp),color=G10Key,border=BorderStroke(1.dp,G10KeyBorder)){Box(contentAlignment=Alignment.Center){Text(c.toString(),fontWeight=FontWeight.Black,fontSize=14.sp,color=G10Ink)}}}

@Composable private fun V10BattleChat(messages:List<ChatMessageDto>,me:String?,language:String,enabled:Boolean,onDismiss:()->Unit,onSend:(String)->Unit){
    var input by remember{mutableStateOf("")};var shifted by remember{mutableStateOf(false)};var emojiOpen by remember{mutableStateOf(false)};val listState=rememberLazyListState();val en=language.lowercase().startsWith("en")
    val lower=if(en)listOf(listOf("q","w","e","r","t","y","u","i","o","p"),listOf("a","s","d","f","g","h","j","k","l"),listOf("z","x","c","v","b","n","m"))else listOf(listOf("q","w","e","r","t","y","u","ı","o","p","ğ","ü"),listOf("a","s","d","f","g","h","j","k","l","ş","i"),listOf("z","x","c","v","b","n","m","ö","ç"));val upper=if(en)lower.map{r->r.map{it.uppercase()}}else listOf(listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),listOf("Z","X","C","V","B","N","M","Ö","Ç"));val rows=if(shifted)upper else lower
    LaunchedEffect(messages.size){if(messages.isNotEmpty())listState.animateScrollToItem(messages.lastIndex)}
    Surface(Modifier.fillMaxSize(),color=G10Card){Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().background(G10Teal).padding(start=14.dp,end=5.dp,top=7.dp,bottom=7.dp),verticalAlignment=Alignment.CenterVertically){Text("SON HARF",Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Black,fontSize=20.sp,textAlign=TextAlign.Center);IconButton(onClick=onDismiss){Icon(Icons.Rounded.Close,if(en)"Close" else "Kapat",tint=Color.White)}}
        LazyColumn(state=listState,modifier=Modifier.weight(1f).fillMaxWidth().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(vertical=8.dp)){items(messages,key={it.id}){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.senderId==me)Arrangement.End else Arrangement.Start){Surface(shape=RoundedCornerShape(17.dp),color=if(m.senderId==me)G10Teal else Color(0xFFE8E3D8)){Text(m.body,Modifier.widthIn(max=300.dp).padding(horizontal=12.dp,vertical=9.dp),color=if(m.senderId==me)Color.White else G10Ink,fontSize=14.sp,lineHeight=19.sp)}}}}
        Surface(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp).heightIn(min=48.dp,max=82.dp),shape=RoundedCornerShape(14.dp),color=Color.White,border=BorderStroke(1.2.dp,G10Teal.copy(alpha=.5f))){Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={emojiOpen=!emojiOpen}){Text("😊",fontSize=22.sp)};Text(if(input.isBlank())if(en)"Type a message…" else "Mesaj yaz…" else input,Modifier.weight(1f).padding(vertical=10.dp),color=if(input.isBlank())G10Muted else G10Ink,maxLines=3,fontSize=14.sp)}}
        AnimatedVisibility(emojiOpen){val emojis=listOf("😀","😂","😍","😎","🤔","👏","🔥","🎉","👍","💪","🏆","🤝","😅","😮","😢","❤️","🙂","🥳","😉","😁","🤩","🙏","✨","💎","🥇","🥈","🥉","🚀","⚡","💯","😜","😇");LazyRow(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFFFFF8E4)),contentPadding=PaddingValues(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){items(emojis){e->Surface(onClick={if(input.length+e.length<=300)input+=e},shape=CircleShape,color=Color.White,border=BorderStroke(1.dp,Color(0xFFE4D9AE))){Text(e,Modifier.padding(horizontal=10.dp,vertical=7.dp),fontSize=23.sp)}}}}
        Surface(color=G10ChatBg){Column(Modifier.fillMaxWidth().padding(5.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){rows.take(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){row.forEach{k->V10ChatKey(k,Modifier.weight(1f),enabled&&input.length<300){input+=k}}}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){V10ChatAction("⇧",Modifier.weight(1.35f),enabled,shifted){shifted=!shifted};rows[2].forEach{k->V10ChatKey(k,Modifier.weight(1f),enabled&&input.length<300){input+=k}};V10ChatAction("⌫",Modifier.weight(1.45f),enabled&&input.isNotEmpty()){if(input.isNotEmpty())input=input.dropLast(1)}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){V10ChatAction("😊",Modifier.weight(.9f),enabled){emojiOpen=!emojiOpen};V10ChatAction(if(en)"SPACE" else "BOŞLUK",Modifier.weight(4.4f),enabled&&input.length<300){if(input.isNotEmpty()&&!input.endsWith(" "))input+=" "};Button(onClick={val t=input.trim();if(t.isNotBlank()){onSend(t);input="";shifted=false}},enabled=enabled&&input.isNotBlank(),modifier=Modifier.weight(1.8f).height(46.dp),colors=ButtonDefaults.buttonColors(containerColor=G10Teal),shape=RoundedCornerShape(9.dp),contentPadding=PaddingValues(horizontal=3.dp)){Text(if(en)"SEND" else "GÖNDER",fontSize=9.sp,fontWeight=FontWeight.Black)}}}}
    }}
}
@Composable private fun V10ChatKey(label:String,modifier:Modifier,enabled:Boolean,onClick:()->Unit){Surface(onClick=onClick,enabled=enabled,modifier=modifier.height(45.dp),shape=RoundedCornerShape(7.dp),color=G10ChatKey,border=BorderStroke(1.dp,Color(0xFFD5CDAF))){Box(contentAlignment=Alignment.Center){Text(label,color=G10Ink,fontSize=15.sp,fontWeight=FontWeight.Bold)}}}
@Composable private fun V10ChatAction(label:String,modifier:Modifier,enabled:Boolean,active:Boolean=false,onClick:()->Unit){Surface(onClick=onClick,enabled=enabled,modifier=modifier.height(45.dp),shape=RoundedCornerShape(7.dp),color=if(active)G10Gold else Color(0xFFC8D8D0)){Box(contentAlignment=Alignment.Center){Text(label,color=G10Ink,fontSize=12.sp,fontWeight=FontWeight.Black)}}}

@Composable private fun V10BattleFinished(room:GameRoomDto,me:String?,backend:OnlineGameBackend,onExit:()->Unit,onRoom:(GameRoomDto)->Unit){
    val scope=rememberCoroutineScope();val won=room.winnerId==me;var result by remember(room.id){mutableStateOf<MatchResultV10Dto?>(null)};var busy by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")}
    LaunchedEffect(room.id){result=runCatching{backend.claimMatchResultV10(room.id)}.getOrNull()}
    Box(Modifier.fillMaxSize().background(G10Bg).padding(20.dp),contentAlignment=Alignment.Center){Surface(shape=RoundedCornerShape(24.dp),color=G10Card,border=BorderStroke(2.dp,if(won)G10Gold else G10Teal.copy(alpha=.4f))){Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){Text(if(won)"🏆 ZAFER!" else "DÜELLO TAMAMLANDI",fontWeight=FontWeight.Black,fontSize=27.sp,color=if(won)G10TealDark else G10Ink);Text("${room.hostScore}  -  ${room.guestScore}",fontSize=25.sp,fontWeight=FontWeight.Black,color=G10Ink);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){V10ResultMetric("+${result?.xpGain?:0}","XP",Modifier.weight(1f));V10ResultMetric("+${result?.diamondsAwarded?:0}","💎",Modifier.weight(1f));V10ResultMetric("${if((result?.leaguePoints?:0)>=0)"+" else ""}${result?.leaguePoints?:0}","Lig",Modifier.weight(1f))};Text("🔥 Seri: ${result?.currentStreak?:0}   •   Rating: ${result?.currentRating?:0}",color=G10TealDark,fontWeight=FontWeight.Bold);Text("Görev ilerlemen ve sezon istatistiklerin ana sayfada güncellendi.",color=G10Muted,fontSize=11.sp,textAlign=TextAlign.Center);Button(onClick={scope.launch{busy=true;val attempt=if(room.isBot)runCatching{backend.restartBotMatch(room.id)}else runCatching{backend.requestRematch(room.id)};attempt.onSuccess{next->onRoom(next);note=if(room.isBot)"Yeni maç başlatıldı" else "Rövanş isteği gönderildi"}.onFailure{note="Rövanş başlatılamadı: ${it.message.orEmpty().take(70)}"};busy=false}},enabled=!busy,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=G10Teal)){Icon(Icons.Rounded.Refresh,null);Text(" TEKRAR OYNA",fontWeight=FontWeight.Black)};OutlinedButton(onClick=onExit,modifier=Modifier.fillMaxWidth()){Text("ANA SAYFAYA DÖN")};if(note.isNotBlank())Text(note,color=G10Muted,fontSize=11.sp)}}}
}
@Composable private fun V10ResultMetric(value:String,label:String,modifier:Modifier){Surface(modifier,shape=RoundedCornerShape(14.dp),color=Color(0xFFFFF3C7)){Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontWeight=FontWeight.Black,fontSize=18.sp,color=G10Ink);Text(label,fontSize=10.sp,color=G10Muted)}}}
@Composable private fun V10BattleAvatar(url:String?,name:String,size:Int){var failed by remember(url){mutableStateOf(false)};if(!url.isNullOrBlank()&&!failed)AsyncImage(model=url,contentDescription="$name profil fotoğrafı",contentScale=ContentScale.Crop,modifier=Modifier.size(size.dp).clip(CircleShape).background(Color.White),onError={failed=true})else Box(Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFD9EEE8)),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),fontWeight=FontWeight.Black,fontSize=(size/2.2).sp,color=G10TealDark)}}
