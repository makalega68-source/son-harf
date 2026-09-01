package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.FriendshipDto
import com.sonharf.game.data.GameInviteDto
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.TriviaQuestionDto
import com.sonharf.game.data.TriviaRoundDto
import java.time.Instant
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val LBg = Color(0xFFF7F9FC)
private val LCard = Color.White
private val LCard2 = Color(0xFFF0F4F8)
private val LText = Color(0xFF182235)
private val LMuted = Color(0xFF5E6C84)
private val LBlue = Color(0xFF1769E0)
private val LBlueSoft = Color(0xFFE8F2FF)
private val LBorder = Color(0xFFDDE5EE)
private val LRed = Color(0xFFE24D6B)
private val LGold = Color(0xFFF3A81A)
private val LPurple = Color(0xFF7658D6)
private val LGreen = Color(0xFF22A85A)

@Composable
internal fun LightDuelLobby(
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    language: String,
    matching: Boolean,
    notice: String,
    showPrivate: Boolean,
    showFriends: Boolean,
    privateCode: String,
    friends: List<Pair<FriendshipDto, ProfileDto>>,
    invites: List<GameInviteDto>,
    onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit,
    onRandom: () -> Unit,
    onCancel: () -> Unit,
    onPrivate: () -> Unit,
    onFriends: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onInvite: (String) -> Unit,
    onInviteResponse: (String, Boolean) -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, LBg))).statusBarsPadding()
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarWithGender(playerAvatarPath, playerGender, playerName, 48.dp, LBlue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playerName, color = LText, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(sh("Düelloya hazırsın", "Ready to duel"), color = LMuted, fontSize = 10.sp)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = LBlueSoft, border = BorderStroke(1.dp, LBlue.copy(alpha = .25f))) {
                        Text(sh("DÜELLO", "DUEL"), Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = LBlue, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LCard), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (matching) CircularProgressIndicator(color = LBlue)
                        Text(if (matching) sh("RAKİP ARANIYOR", "SEARCHING OPPONENT") else "SON HARF", color = LText, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text(sh("Kelimeyi Sürdür, Rakibini Geç", "Continue the word, beat your rival"), color = LMuted, textAlign = TextAlign.Center)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(language == "tr", "🇹🇷 TÜRKÇE", Modifier.weight(1f)) { onLanguage("tr") }
                    ChoicePill(language == "en", "🇬🇧 ENGLISH", Modifier.weight(1f)) { onLanguage("en") }
                }
            }
            item {
                Button(
                    onClick = if (matching) onCancel else onRandom,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (matching) Color(0xFFFCE8ED) else LBlue, contentColor = if (matching) LRed else Color.White),
                ) { Text(if (matching) sh("EŞLEŞMEYİ İPTAL ET", "CANCEL MATCHMAKING") else sh("DÜELLOYA GİR", "ENTER DUEL"), fontWeight = FontWeight.Black) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LobbyAction(sh("ARKADAŞ", "FRIENDS"), sh("Davet et", "Invite"), Modifier.weight(1f), onFriends)
                    LobbyAction(sh("ÖZEL ODA", "PRIVATE ROOM"), sh("Kodla gir", "Join by code"), Modifier.weight(1f), onPrivate)
                }
            }
            item { Notice(notice) }
            if (showPrivate) item {
                Card(colors = CardDefaults.cardColors(containerColor = LCard), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text(sh("VIP ODA OLUŞTUR", "CREATE VIP ROOM")) }
                        OutlinedTextField(value = privateCode, onValueChange = onPrivateCode, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(sh("6 haneli oda kodu", "6-character room code")) })
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth()) { Text(sh("ODA KODUYLA KATIL", "JOIN WITH ROOM CODE")) }
                    }
                }
            }
            if (showFriends) item {
                Card(colors = CardDefaults.cardColors(containerColor = LCard), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        invites.forEach { invite ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(sh("Maç daveti", "Game invite"), Modifier.weight(1f), color = LText)
                                TextButton(onClick = { onInviteResponse(invite.id, true) }) { Text(sh("Kabul", "Accept")) }
                                TextButton(onClick = { onInviteResponse(invite.id, false) }) { Text(sh("Reddet", "Decline"), color = LRed) }
                            }
                        }
                        friends.forEach { (_, p) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(p.displayName, Modifier.weight(1f), color = LText, fontWeight = FontWeight.Bold, maxLines = 1)
                                Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online") { Text(sh("Davet", "Invite")) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LightDuelArena(
    room: GameRoomDto,
    me: String?,
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    playerRating: Int,
    opponentName: String,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentRating: Int,
    words: List<GameWordDto>,
    isVip: Boolean,
    feedbackWord: String?,
    feedbackCorrect: Boolean?,
    wordInput: String,
    onWordInput: (String) -> Unit,
    notice: String,
    busy: Boolean,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    triviaSelection: Long?,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onTrivia: (Int) -> Unit,
    onTriviaTimeout: () -> Unit,
    onChat: () -> Unit,
    onForfeit: () -> Unit,
    onExit: () -> Unit,
    onRematch: () -> Unit,
) {
    if (room.status == "waiting") { WaitingRoom(room.code, playerName, onExit); return }
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    if (room.status == "finished") { ResultCard(room.winnerId == me, room.winnerId == null, playerName, opponentName, myRounds, oppRounds, onRematch, onExit); return }

    val liveWordPhase = room.status in listOf("playing", "final", "sudden_death")
    val quizActive = room.status == "quiz" && triviaRound != null && triviaQuestion != null
    val myTurn = room.currentPlayerId == me && liveWordPhase
    val last = words.lastOrNull()?.normalizedWord?.trim().orEmpty()
    val required = last.takeLast(1).takeIf { it.isNotBlank() }?.let { gameUppercase(it, room.language) } ?: "•"
    val shownLastWord = feedbackWord ?: gameUppercase(last, room.language)
    var showHelp by remember(room.id) { mutableStateOf(false) }
    val deadline = if (quizActive) triviaRound?.answerDeadline else room.turnDeadline
    var seconds by remember(deadline, room.status) { mutableIntStateOf(10) }
    LaunchedEffect(deadline, room.currentPlayerId, room.status) {
        val end = runCatching { deadline?.let { Instant.parse(it).toEpochMilli() } }.getOrNull() ?: return@LaunchedEffect
        while (true) {
            val left = end - Instant.now().toEpochMilli()
            if (left <= 0) { seconds = 0; if (quizActive) onTriviaTimeout() else onTimeout(); break }
            seconds = ceil(left / 1000.0).toInt().coerceAtLeast(1)
            delay(100)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, LBg))).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Reserved header row: player cards and help never overlap.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PlayerCard(playerName, playerAvatarPath, playerGender, playerRating, myScore, myTurn, LBlue, false, Modifier.weight(1f))
            Surface(Modifier.size(64.dp), CircleShape, Color.White, border = BorderStroke(2.dp, if (seconds <= 3) LRed else LBlue)) {
                Box(contentAlignment = Alignment.Center) { Text(seconds.toString(), color = LText, fontSize = 24.sp, fontWeight = FontWeight.Black) }
            }
            PlayerCard(opponentName.removeSuffix(" BOT"), opponentAvatarPath, opponentGender, opponentRating, oppScore, !myTurn && liveWordPhase, LRed, room.isBot, Modifier.weight(1f))
            FilledTonalIconButton(
                onClick = { showHelp = true },
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = LBlueSoft, contentColor = LBlue),
            ) { Icon(Icons.Rounded.HelpOutline, sh("Nasıl oynanır", "How to play"), Modifier.size(20.dp)) }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = LCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (myTurn) LBlue.copy(alpha = .5f) else LBorder),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (quizActive) sh("BONUS DÜELLOSU", "BONUS DUEL") else if (myTurn) sh("SIRA SENDE", "YOUR TURN") else sh("RAKİBİN HAMLESİ", "OPPONENT'S MOVE"), color = if (myTurn) LBlue else LMuted, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Text(sh("SON HARF", "LAST LETTER"), color = LMuted, fontSize = 10.sp)
                Text(required, color = LText, fontSize = 62.sp, fontWeight = FontWeight.Black)
                Text(shownLastWord.ifBlank { sh("İLK KELİMEYİ YAZ", "ENTER FIRST WORD") }, color = if (feedbackCorrect == false) LRed else LGreen, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }

        if (isVip) {
            LazyRow(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                items(words.takeLast(6)) { word -> Surface(shape = RoundedCornerShape(10.dp), color = LBlueSoft) { Text(gameUppercase(word.word.trim().ifBlank { word.normalizedWord.trim() }, room.language), Modifier.padding(8.dp), color = LText, fontSize = 9.sp) } }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ActionButton(sh("⚑ PES ET", "⚑ FORFEIT"), LRed, Modifier.weight(1f), onForfeit)
            ActionButton(sh("● SOHBET", "● CHAT"), LBlue, Modifier.weight(1f), onChat)
        }

        if (quizActive) {
            TriviaCard(requireNotNull(triviaRound), requireNotNull(triviaQuestion), triviaSelection, onTrivia, Modifier.padding(horizontal = 12.dp))
        }
        InputBar(wordInput, myTurn, busy, quizActive, onSubmit, Modifier.padding(horizontal = 12.dp))
        GameKeyboard(wordInput, room.language, !busy && !quizActive, myTurn && wordInput.isNotBlank() && !busy && !quizActive, onWordInput, onSubmit, Modifier.navigationBarsPadding())
        if (notice.isNotBlank()) Text(notice, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), color = LMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2)
    }

    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        title = { Text(sh("Son Harf Nasıl Oynanır?", "How to Play Son Harf"), fontWeight = FontWeight.Black) },
        text = { Text(sh("Sırandaki sürede, önceki kelimenin son harfiyle başlayan geçerli bir kelime yaz. Aynı kelime tekrar kullanılamaz. Üç raund sonunda daha yüksek skor kazanır.", "During your turn, enter a valid word starting with the final letter of the previous word. Words cannot be reused. The higher score after three rounds wins."), color = LText) },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text(sh("ANLADIM", "GOT IT"), color = LBlue, fontWeight = FontWeight.Black) } },
    )
}

@Composable private fun PlayerCard(name:String, avatarPath:String?, gender:String?, rating:Int, score:Int, active:Boolean, accent:Color, bot:Boolean, modifier:Modifier) {
    Card(modifier.height(92.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(if(active) 2.dp else 1.dp, if(active) LGreen else LBorder)) {
        Row(Modifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (bot) Box(Modifier.size(44.dp,58.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha=.10f)), contentAlignment=Alignment.Center) { Text("BOT", color=accent, fontWeight=FontWeight.Black) }
            else ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, 44.dp, 58.dp, accent)
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) { Text(name,color=LText,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis); Text(score.toString(),color=LText,fontSize=23.sp,fontWeight=FontWeight.Black); Text("🏆 $rating",color=LGold,fontSize=8.sp,maxLines=1) }
        }
    }
}

@Composable private fun GameKeyboard(value:String, language:String, enabled:Boolean, submitEnabled:Boolean, onValueChange:(String)->Unit, onSubmit:()->Unit, modifier:Modifier) {
    val rows = if(language.lowercase()=="en") listOf("QWERTYUIOP","ASDFGHJKL","ZXCVBNM") else listOf("QWERTYUIOPĞÜ","ASDFGHJKLŞİ","ZXCVBNMÖÇ")
    Surface(modifier.fillMaxWidth(), color=LCard2, border=BorderStroke(1.dp,LBorder)) {
        Column(Modifier.padding(5.dp), verticalArrangement=Arrangement.spacedBy(3.dp)) {
            rows.forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(2.dp)) { row.forEach { c -> OutlinedButton(onClick={onValueChange((value+c).take(40))},enabled=enabled,modifier=Modifier.weight(1f).height(36.dp),contentPadding=PaddingValues(0.dp),shape=RoundedCornerShape(8.dp)){Text(c.toString(),fontSize=11.sp,fontWeight=FontWeight.Bold)} } } }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick={onValueChange(value.dropLast(1))},enabled=enabled&&value.isNotEmpty(),modifier=Modifier.weight(1f).height(40.dp)){Text("⌫")}
                Button(onClick=onSubmit,enabled=submitEnabled,modifier=Modifier.weight(2f).height(40.dp)){Text(sh("GÖNDER","SEND"),fontWeight=FontWeight.Black)}
            }
        }
    }
}

@Composable private fun InputBar(value:String,myTurn:Boolean,busy:Boolean,quiz:Boolean,onSubmit:()->Unit,modifier:Modifier){Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=Color.White,border=BorderStroke(1.dp,if(myTurn&&!quiz)LBlue else LBorder)){Row(Modifier.fillMaxWidth().height(46.dp).padding(start=12.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically){Text(value.ifBlank{if(quiz)sh("Bonus turu…","Bonus round…") else sh("Kelimenizi yazın…","Type your word…")},Modifier.weight(1f),color=if(value.isBlank())LMuted else LText,maxLines=1);Button(onClick=onSubmit,enabled=myTurn&&value.isNotBlank()&&!busy&&!quiz,modifier=Modifier.height(38.dp)){Text("➤")}}}}

@Composable private fun TriviaCard(round:TriviaRoundDto,question:TriviaQuestionDto,selection:Long?,onTrivia:(Int)->Unit,modifier:Modifier){Card(modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFFF6F2FF)),border=BorderStroke(1.dp,LPurple.copy(alpha=.35f))){Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(question.question,color=LText,fontWeight=FontWeight.Bold,fontSize=10.sp);listOf(question.optionA,question.optionB,question.optionC,question.optionD).chunked(2).forEach{pair->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){pair.forEach{raw->OutlinedButton(onClick={raw.toIntOrNull()?.let(onTrivia)},enabled=selection==null&&round.resolvedAt==null,modifier=Modifier.weight(1f).height(34.dp),contentPadding=PaddingValues(2.dp)){Text(raw,fontSize=9.sp)}}}}}}}

@Composable private fun ActionButton(label:String,accent:Color,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,modifier=modifier.height(38.dp),border=BorderStroke(1.dp,accent.copy(alpha=.5f)),contentPadding=PaddingValues(4.dp)){Text(label,color=accent,fontSize=9.sp,fontWeight=FontWeight.Black,maxLines=1)}}
@Composable private fun ChoicePill(selected:Boolean,text:String,modifier:Modifier,onClick:()->Unit){Surface(modifier.height(50.dp).clickable(onClick=onClick),shape=RoundedCornerShape(15.dp),color=if(selected)LBlueSoft else Color.White,border=BorderStroke(1.dp,if(selected)LBlue else LBorder)){Box(contentAlignment=Alignment.Center){Text(text,color=if(selected)LBlue else LText,fontWeight=FontWeight.Black,fontSize=11.sp)}}}
@Composable private fun LobbyAction(title:String,subtitle:String,modifier:Modifier,onClick:()->Unit){Card(modifier.height(88.dp).clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,LBorder)){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.Center){Text(title,color=LText,fontWeight=FontWeight.Black);Text(subtitle,color=LMuted,fontSize=10.sp)}}}
@Composable private fun Notice(text:String){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(13.dp),color=Color.White,border=BorderStroke(1.dp,LBorder)){Text(text,Modifier.padding(10.dp),color=LMuted,fontSize=10.sp,textAlign=TextAlign.Center)}}
@Composable private fun WaitingRoom(code:String,name:String,onExit:()->Unit){Box(Modifier.fillMaxSize().background(LBg).statusBarsPadding(),contentAlignment=Alignment.Center){Card(Modifier.fillMaxWidth(.88f),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){Text(name,color=LText,fontWeight=FontWeight.Black);Text(sh("RAKİP BEKLENİYOR","WAITING FOR OPPONENT"),color=LBlue);Text(code,color=LText,fontSize=30.sp,fontWeight=FontWeight.Black);CircularProgressIndicator(color=LBlue);OutlinedButton(onClick=onExit){Text(sh("ODADAN ÇIK","LEAVE ROOM"),color=LRed)}}}}}
@Composable private fun ResultCard(won:Boolean,draw:Boolean,player:String,opponent:String,myRounds:Int,oppRounds:Int,onRematch:()->Unit,onExit:()->Unit){Box(Modifier.fillMaxSize().background(LBg).statusBarsPadding(),contentAlignment=Alignment.Center){Card(Modifier.fillMaxWidth(.88f),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){Text(if(draw)sh("BERABERE","DRAW") else if(won)sh("ZAFER","VICTORY") else sh("MAÇ BİTTİ","MATCH OVER"),color=if(won)LBlue else if(draw)LGold else LRed,fontSize=26.sp,fontWeight=FontWeight.Black);Text("$player  $myRounds : $oppRounds  $opponent",color=LText,textAlign=TextAlign.Center);Button(onClick=onRematch,modifier=Modifier.fillMaxWidth()){Text(sh("RÖVANŞ","REMATCH"))};OutlinedButton(onClick=onExit,modifier=Modifier.fillMaxWidth()){Text(sh("LOBİYE DÖN","BACK TO LOBBY"))}}}}}
