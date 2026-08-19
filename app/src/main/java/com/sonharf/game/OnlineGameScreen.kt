package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
fun OnlineGameScreen() {
    if (!SupabaseProvider.configured) { MissingBackendConfig(); return }

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var playerName by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("tr") }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var requests by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var message by remember { mutableStateOf("Dilini seç ve rakibini bul.") }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    fun readableError(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            "not_your_turn" in raw -> "Sıra rakibinde."
            "answers_locked" in raw -> "Şıklar 3 saniyelik okuma süresinden sonra açılır."
            "friend_in_game" in raw -> "Arkadaşın şu anda oyunda."
            "friend_offline" in raw -> "Arkadaşın şu anda çevrimdışı."
            "player_already_in_game" in raw -> "Zaten aktif bir maçın var."
            "blocked_relationship" in raw -> "Bu oyuncuyla etkileşim kapalı."
            else -> raw.substringBefore("URL:").trim().ifBlank { "Bağlantı hatası oluştu." }
        }
    }

    suspend fun refreshQuiz(active: GameRoomDto) {
        if (active.status == "quiz") {
            val q = backend.getActiveTriviaRound(active.id)
            triviaRound = q
            triviaQuestion = q?.let { backend.getTriviaQuestion(it.questionId) }
        } else { triviaRound = null; triviaQuestion = null }
    }

    fun startObservers(active: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel()
        matching = false
        roomJob = scope.launch {
            backend.observeRoom(active.id).catch { message = readableError(it) }.collect {
                room = it; refreshQuiz(it)
                if (it.status == "finished") runCatching { backend.setPresence("online") }
            }
        }
        wordsJob = scope.launch { backend.observeWords(active.id).catch { message = readableError(it) }.collect { words = it } }
        chatJob = scope.launch { backend.observeChat(active.id).catch { message = readableError(it) }.collect { chat = it } }
    }

    suspend fun refreshFriends() {
        friends = backend.getFriends()
        requests = backend.getIncomingFriendRequests()
        invites = backend.getIncomingGameInvites()
    }

    fun ensureAndThen(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            runCatching { backend.ensurePlayer(playerName); block() }
                .onFailure { message = readableError(it) }
            busy = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel()
            scope.launch { runCatching { backend.cancelRandomMatchmaking() }; runCatching { backend.setPresence("offline") } }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1020), SonHarfBg, Color(0xFF090A11))))) {
        if (room == null) {
            LobbyScreen(
                playerName = playerName,
                onPlayerNameChange = { playerName = it.take(24) },
                selectedLanguage = selectedLanguage,
                onLanguageChange = { selectedLanguage = it },
                busy = busy,
                matching = matching,
                message = message,
                showFriends = showFriends,
                friends = friends,
                requests = requests,
                invites = invites,
                onRandom = {
                    ensureAndThen {
                        matching = true; showFriends = false
                        message = "Rakip aranıyor…"
                        backend.startRandomMatchmaking(selectedLanguage)
                        matchJob?.cancel()
                        matchJob = scope.launch {
                            while (matching && room == null) {
                                runCatching { backend.startRandomMatchmaking(selectedLanguage) }
                                val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                                if (found != null) {
                                    room = found
                                    message = "Rakip bulundu. Düello başlıyor!"
                                    startObservers(found)
                                    break
                                }
                                delay(1800)
                            }
                        }
                    }
                },
                onCancelRandom = {
                    scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; message = "Eşleşme iptal edildi." }
                },
                onOpenFriends = {
                    ensureAndThen { showFriends = !showFriends; if (showFriends) refreshFriends() }
                },
                onInviteFriend = { id -> ensureAndThen { backend.inviteFriend(id, selectedLanguage); message = "Oyun daveti gönderildi."; refreshFriends() } },
                onAcceptFriend = { id -> ensureAndThen { backend.respondFriendRequest(id, true); refreshFriends(); message = "Arkadaşlık kabul edildi." } },
                onRespondInvite = { inviteId, accept ->
                    ensureAndThen {
                        val joined = backend.respondGameInvite(inviteId, accept)
                        if (joined != null) { room = joined; startObservers(joined); message = "Davet kabul edildi. Maç başlıyor." }
                        else { refreshFriends(); message = "Davet reddedildi." }
                    }
                }
            )
        } else {
            val activeRoom = room ?: return@Box
            val me = backend.currentUserId()
            val myTurn = activeRoom.currentPlayerId == me && activeRoom.status in listOf("playing", "final", "sudden_death")
            val opponentId = if (me == activeRoom.hostId) activeRoom.guestId else activeRoom.hostId

            ActiveGameScreen(
                activeRoom, me, myTurn, words, chat, triviaRound, triviaQuestion,
                wordInput, { wordInput = it.take(40) }, chatInput, { chatInput = it.take(300) }, message, busy,
                onSubmitWord = {
                    scope.launch {
                        busy = true
                        runCatching { backend.submitWord(activeRoom.id, wordInput) }
                            .onSuccess { room = it; wordInput = ""; message = eventText(it, me) }
                            .onFailure { message = readableError(it) }
                        busy = false
                    }
                },
                onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(activeRoom.id) }.onSuccess { room = it; message = eventText(it, me) } } },
                onAnswerTrivia = { index ->
                    val id = triviaRound?.id ?: return@ActiveGameScreen
                    scope.launch { runCatching { backend.answerTrivia(id, index) }.onSuccess { room = it; refreshQuiz(it); message = eventText(it, me) } }
                },
                onForfeit = { scope.launch { runCatching { backend.forfeit(activeRoom.id) }.onSuccess { room = it } } },
                onSendChat = { scope.launch { runCatching { backend.sendChat(activeRoom.id, chatInput) }.onSuccess { chatInput = "" }.onFailure { message = readableError(it) } } },
                onBlockOpponent = { if (opponentId != null) scope.launch { runCatching { backend.blockUser(opponentId) }; message = "Oyuncu engellendi." } },
                onReportOpponent = { if (opponentId != null) scope.launch { runCatching { backend.reportUser(opponentId, activeRoom.id, "Uygunsuz sohbet") }; message = "Rapor kaydedildi." } },
                onAddFriend = { if (opponentId != null) scope.launch { runCatching { backend.sendFriendRequest(opponentId) }; message = "Arkadaşlık isteği gönderildi." } },
                onAllowPhoto = { if (opponentId != null) scope.launch { runCatching { backend.setPhotoAccess(opponentId, true) }; message = "Fotoğraf bu oyuncuya açıldı." } }
            )
        }
    }
}

private fun eventText(room: GameRoomDto, me: String?): String = when (room.lastEvent) {
    "streak_bonus" -> if (room.lastEventPlayerId == me) "🔥 SÖZ FIRTINASI! +3 ekstra" else "Rakip seri yaptı!"
    "invalid_word", "not_in_dictionary", "wrong_start_letter", "word_already_used" -> "Geçersiz hamle: −1 ve sıra değişti."
    "turn_expired" -> "Süre doldu: −1 ve sıra rakibe geçti."
    "quiz_started" -> "🧠 Bonus soru! 3 saniye oku."
    "quiz_won" -> "⚡ Bonus kapıldı!"
    "final_started" -> "🏁 FİNAL ZİNCİRİ başladı!"
    else -> "Hamle işlendi."
}

@Composable
private fun LobbyScreen(
    playerName: String, onPlayerNameChange: (String) -> Unit,
    selectedLanguage: String, onLanguageChange: (String) -> Unit,
    busy: Boolean, matching: Boolean, message: String, showFriends: Boolean,
    friends: List<Pair<FriendshipDto, ProfileDto>>, requests: List<Pair<FriendshipDto, ProfileDto>>, invites: List<GameInviteDto>,
    onRandom: () -> Unit, onCancelRandom: () -> Unit, onOpenFriends: () -> Unit,
    onInviteFriend: (String) -> Unit, onAcceptFriend: (String) -> Unit, onRespondInvite: (String, Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("ONLINE DÜELLO", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Rastgele rakip bul veya arkadaşını davet et.", color = SonHarfMuted)
        Surface(color = SonHarfPurple.copy(alpha=.12f), shape=RoundedCornerShape(18.dp), border=BorderStroke(1.dp, SonHarfPurple.copy(alpha=.35f))) {
            Text(message, Modifier.fillMaxWidth().padding(14.dp), color=SonHarfText, fontSize=13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected=selectedLanguage=="tr", onClick={onLanguageChange("tr")}, label={Text("🇹🇷 Türkçe")})
            FilterChip(selected=selectedLanguage=="en", onClick={onLanguageChange("en")}, label={Text("🇬🇧 English")})
        }
        DarkTextField(playerName, onPlayerNameChange, "Oyuncu adı", "Oyuncu")

        if (!matching) {
            Button(onClick=onRandom, enabled=!busy && playerName.trim().length>=2, modifier=Modifier.fillMaxWidth().height(62.dp), shape=RoundedCornerShape(20.dp), colors=ButtonDefaults.buttonColors(containerColor=SonHarfPurple)) {
                Text("⚡ RASTGELE RAKİP BUL", fontWeight=FontWeight.Black, fontSize=16.sp)
            }
        } else {
            Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface), shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text("RAKİP ARANIYOR…", fontWeight=FontWeight.Black)
                    Text(if(selectedLanguage=="tr") "Türkçe havuzunda bekliyorsun" else "English matchmaking queue", color=SonHarfMuted)
                    TextButton(onClick=onCancelRandom) { Text("ARAMAYI İPTAL ET") }
                }
            }
        }

        OutlinedButton(onClick=onOpenFriends, enabled=!busy && playerName.trim().length>=2, modifier=Modifier.fillMaxWidth().height(54.dp), shape=RoundedCornerShape(18.dp), border=BorderStroke(1.dp, SonHarfCyan)) {
            Text("👥 ARKADAŞLARINLA OYNA", fontWeight=FontWeight.Black)
        }

        if (showFriends) {
            Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface), shape=RoundedCornerShape(22.dp), modifier=Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if (invites.isNotEmpty()) Text("OYUN DAVETLERİ", color=SonHarfGold, fontWeight=FontWeight.Black)
                    invites.forEach { inv ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                            Text("${if(inv.language=="tr") "TR" else "EN"} oyun daveti")
                            Row { TextButton(onClick={onRespondInvite(inv.id,true)}){Text("Kabul")}; TextButton(onClick={onRespondInvite(inv.id,false)}){Text("Reddet")} }
                        }
                    }
                    if (requests.isNotEmpty()) Text("ARKADAŞLIK İSTEKLERİ", color=SonHarfGold, fontWeight=FontWeight.Black)
                    requests.forEach { (_, p) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                            Text(p.displayName)
                            TextButton(onClick={onAcceptFriend(p.id)}) { Text("Kabul et") }
                        }
                    }
                    Text("ARKADAŞLAR", color=SonHarfCyan, fontWeight=FontWeight.Black)
                    if (friends.isEmpty()) Text("Henüz arkadaş yok. Maç sonlarında oyuncuları arkadaş ekleyebilirsin.", color=SonHarfMuted, fontSize=12.sp)
                    friends.forEach { (_, p) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                            Column { Text(p.displayName, fontWeight=FontWeight.Bold); Text(when(p.presenceStatus){"online"->"Çevrimiçi";"in_game"->"Oyunda";else->"Çevrimdışı"}, color=SonHarfMuted, fontSize=11.sp) }
                            Button(onClick={onInviteFriend(p.id)}, enabled=p.presenceStatus=="online") { Text("Davet") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkTextField(value:String,onValueChange:(String)->Unit,label:String,placeholder:String){
    OutlinedTextField(value,onValueChange,label={Text(label)},placeholder={Text(placeholder,color=SonHarfMuted.copy(alpha=.65f))},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=SonHarfCyan,unfocusedBorderColor=SonHarfSurface2,focusedLabelColor=SonHarfCyan,cursorColor=SonHarfCyan,focusedContainerColor=Color(0xFF0D1322),unfocusedContainerColor=Color(0xFF0D1322)))
}

@Composable
private fun ActiveGameScreen(
    activeRoom:GameRoomDto, me:String?, myTurn:Boolean, words:List<GameWordDto>, chat:List<ChatMessageDto>, triviaRound:TriviaRoundDto?, triviaQuestion:TriviaQuestionDto?,
    wordInput:String,onWordInputChange:(String)->Unit,chatInput:String,onChatInputChange:(String)->Unit,message:String,busy:Boolean,
    onSubmitWord:()->Unit,onTimeout:()->Unit,onAnswerTrivia:(Int)->Unit,onForfeit:()->Unit,onSendChat:()->Unit,onBlockOpponent:()->Unit,onReportOpponent:()->Unit,onAddFriend:()->Unit,onAllowPhoto:()->Unit
){
    val isHost=me==activeRoom.hostId
    val myScore=if(isHost)activeRoom.hostScore else activeRoom.guestScore
    val opponentScore=if(isHost)activeRoom.guestScore else activeRoom.hostScore
    val myStreak=if(isHost)activeRoom.hostStreak else activeRoom.guestStreak
    val required=words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    var secondsLeft by remember(activeRoom.turnDeadline){mutableStateOf(45)}
    LaunchedEffect(activeRoom.turnDeadline,activeRoom.currentPlayerId,activeRoom.status){
        while(activeRoom.turnDeadline!=null && activeRoom.status in listOf("playing","final","sudden_death")){
            val deadline=runCatching{Instant.parse(activeRoom.turnDeadline)}.getOrNull()
            val left=deadline?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0)?:45
            secondsLeft=left
            if(left<=0){onTimeout();break}
            delay(1000)
        }
    }
    Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(22.dp)){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column{Text(if(activeRoom.language=="tr")"TÜRKÇE DÜELLO" else "ENGLISH DUEL",color=SonHarfCyan,fontSize=11.sp,fontWeight=FontWeight.Black);Text(if(myTurn)"SIRA SENDE" else "RAKİBİN SIRASI",fontSize=20.sp,fontWeight=FontWeight.Black)}
                    Surface(color=if(secondsLeft<=10)Color(0xFF5A202C) else SonHarfPurple,shape=RoundedCornerShape(999.dp)){Text("$secondsLeft sn",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontWeight=FontWeight.Black)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("SEN  $myScore",fontWeight=FontWeight.Black);Text("Kelime ${activeRoom.validWordCount}/45",color=SonHarfMuted);Text("$opponentScore  RAKİP",fontWeight=FontWeight.Black)}
                if(myStreak>=4)Text("🔥 Seri: $myStreak doğru",color=SonHarfGold,fontWeight=FontWeight.Bold)
            }
        }
        if(activeRoom.status=="quiz") TriviaPanel(triviaRound,triviaQuestion,onAnswerTrivia)
        else if(activeRoom.status!="finished"){
            Surface(color=SonHarfSurface,shape=RoundedCornerShape(24.dp),modifier=Modifier.weight(1f)){
                Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
                    Text("SON HARF",color=SonHarfMuted,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Text(required?.toString()?:"•",fontSize=64.sp,fontWeight=FontWeight.Black,color=SonHarfCyan)
                    Spacer(Modifier.height(12.dp))
                    Text(if(required==null)"İlk kelimeyi yaz" else "$required ile başlayan yeni bir kelime yaz",color=SonHarfMuted,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(16.dp));DarkTextField(wordInput,onWordInputChange,if(myTurn)"Kelime" else "Rakibin hamlesini bekle","Kelime");Spacer(Modifier.height(10.dp))
                    Button(onClick=onSubmitWord,enabled=myTurn&&wordInput.trim().length>=2&&!busy,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=SonHarfPurple)){Text("GÖNDER",fontWeight=FontWeight.Black)}
                    Text("Geçerli +3 • Hatalı/Süre −1 • Her 5 kusursuz doğru +3",color=SonHarfMuted,fontSize=10.sp,modifier=Modifier.padding(top=10.dp))
                }
            }
        }
        if(activeRoom.status=="finished"){
            val won=activeRoom.winnerId==me
            Card(colors=CardDefaults.cardColors(containerColor=if(won)Color(0xFF193A32) else Color(0xFF3A1D27)),shape=RoundedCornerShape(20.dp)){
                Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(if(won)"KAZANDIN ✦" else "MAÇ BİTTİ",fontSize=22.sp,fontWeight=FontWeight.Black);TextButton(onClick=onAddFriend){Text("+ ARKADAŞ EKLE")}}
            }
        }
        if(message.isNotBlank())Text(message,color=SonHarfMuted,fontSize=11.sp)
        Surface(color=SonHarfSurface,shape=RoundedCornerShape(18.dp)){
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text("CANLI SOHBET",color=SonHarfCyan,fontSize=11.sp,fontWeight=FontWeight.Black)
                    Row{TextButton(onClick=onAllowPhoto){Text("Fotoğraf",fontSize=10.sp)};TextButton(onClick=onReportOpponent){Text("Rapor",fontSize=10.sp)};TextButton(onClick=onBlockOpponent){Text("Engelle",color=Color(0xFFFF8894),fontSize=10.sp)}}
                }
                if(chat.isNotEmpty())Text(chat.takeLast(3).joinToString("\n"){(if(it.senderId==me)"Sen: " else "Rakip: ")+it.body},fontSize=12.sp,color=SonHarfMuted)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(chatInput,onChatInputChange,placeholder={Text("Mesaj yaz",color=SonHarfMuted.copy(alpha=.6f))},singleLine=true,modifier=Modifier.weight(1f),shape=RoundedCornerShape(14.dp));Button(onClick=onSendChat,enabled=chatInput.isNotBlank(),shape=RoundedCornerShape(14.dp)){Text("➤")}}
            }
        }
        OutlinedButton(onClick=onForfeit,modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,Color(0xFF6C3340))){Text("PES ET",color=Color(0xFFFF8894))}
    }
}

@Composable
private fun TriviaPanel(round:TriviaRoundDto?,question:TriviaQuestionDto?,onAnswer:(Int)->Unit){
    if(round==null||question==null){Surface(color=SonHarfSurface,shape=RoundedCornerShape(24.dp),modifier=Modifier.weight(1f)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}};return}
    var unlocked by remember(round.revealAt){mutableStateOf(false)}
    var readSeconds by remember(round.revealAt){mutableStateOf(3)}
    LaunchedEffect(round.revealAt){while(true){val reveal=runCatching{Instant.parse(round.revealAt)}.getOrNull();val left=reveal?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0)?:0;readSeconds=left;unlocked=left<=0;if(unlocked)break;delay(250)}}
    Surface(color=SonHarfSurface,shape=RoundedCornerShape(24.dp),modifier=Modifier.weight(1f)){
        Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.Center){Text("🧠 GENEL KÜLTÜR • +${round.bonusPoints}",color=SonHarfGold,fontWeight=FontWeight.Black);Spacer(Modifier.height(14.dp));Text(question.question,fontSize=22.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(14.dp));if(!unlocked)Text("Şıklar $readSeconds saniye sonra açılacak…",color=SonHarfCyan,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth()) else listOf(question.optionA,question.optionB,question.optionC,question.optionD).forEachIndexed{index,option->OutlinedButton(onClick={onAnswer(index)},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),shape=RoundedCornerShape(14.dp)){Text("${'A'+index})  $option",modifier=Modifier.fillMaxWidth())}}
        }
    }
}

@Composable
private fun MissingBackendConfig(){Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text("Supabase bağlantısı yapılandırılmalı.",textAlign=TextAlign.Center)}}
