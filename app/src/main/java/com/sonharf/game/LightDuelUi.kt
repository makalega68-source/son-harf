package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

private val LBg: Color get() = SonHarfCosmetics.gamePalette.background
private val LCard = Color.White
private val LCard2 = Color(0xFFF0F4F8)
private val LText = Color(0xFF182235)
private val LMuted = Color(0xFF718096)
private val LBlue: Color get() = SonHarfCosmetics.gamePalette.accent
private val LBlueSoft: Color get() = SonHarfCosmetics.gamePalette.accent.copy(alpha = .10f)
private val LBlue2: Color get() = SonHarfCosmetics.gamePalette.secondary
private val LBorder = Color(0xFFDDE5EE)
private val LRed = Color(0xFFE24D6B)
private val LGold = Color(0xFFF3A81A)
private val LPurple: Color get() = SonHarfCosmetics.gamePalette.secondary
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
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, LBg, LBg.copy(alpha = .72f)))).statusBarsPadding()
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarWithGender(playerAvatarPath, playerGender, playerName, 48.dp, LBlue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playerName, color = LText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(sh("Düelloya hazırsın", "Ready to duel"), color = LMuted, fontSize = 10.sp)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = LBlueSoft, border = BorderStroke(1.dp, LBlue.copy(alpha = .25f))) {
                        Text(sh("DÜELLO", "DUEL"), Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = LBlue, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = LCard), shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, LBorder), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(335.dp).background(Brush.radialGradient(listOf(LBlueSoft, Color.White, Color(0xFFF8FAFD)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(225.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(LBlue, LPurple, LGold, LBlue2, LBlue))).padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (matching) {
                                        CircularProgressIndicator(Modifier.size(34.dp), color = LBlue, strokeWidth = 3.dp)
                                        Spacer(Modifier.height(12.dp))
                                        Text(sh("RAKİP ARANIYOR", "SEARCHING OPPONENT"), color = LText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    } else {
                                        Text("SON", color = LText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                                        Text("HARF", color = LBlue, fontSize = 44.sp, fontWeight = FontWeight.Black)
                                        Text(sh("DÜELLOYA HAZIR", "READY TO DUEL"), color = LText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        if (matching) sh("Önce gerçek oyuncu, sonra BOT", "Real player first, then BOT") else sh("Kelimeyi sürdür, rakibini geç", "Continue the word, beat your rival"),
                                        color = LMuted, fontSize = 10.sp, textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LightChoicePill(language == "tr", "🇹🇷  TÜRKÇE", Modifier.weight(1f)) { onLanguage("tr") }
                    LightChoicePill(language == "en", "🇬🇧  ENGLISH", Modifier.weight(1f)) { onLanguage("en") }
                }
            }

            item {
                Button(
                    onClick = if (matching) onCancel else onRandom,
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (matching) Color(0xFFFCE8ED) else LBlue, contentColor = if (matching) LRed else Color.White),
                    border = if (matching) BorderStroke(1.dp, LRed.copy(alpha = .35f)) else null,
                ) {
                    Text(if (matching) sh("EŞLEŞMEYİ İPTAL ET", "CANCEL MATCHMAKING") else sh("DÜELLOYA GİR  ⚡", "ENTER DUEL  ⚡"), fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LightLobbyAction(sh("ARKADAŞ", "FRIENDS"), sh("Davet et", "Invite"), "👥", Modifier.weight(1f), onFriends)
                    LightLobbyAction(sh("ÖZEL ODA", "PRIVATE ROOM"), sh("Kodla gir", "Join by code"), "♛", Modifier.weight(1f), onPrivate)
                }
            }

            item { LightNotice(notice) }

            if (showPrivate) item {
                Card(colors = CardDefaults.cardColors(containerColor = LCard), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(sh("ÖZEL ODA", "PRIVATE ROOM"), color = LText, fontWeight = FontWeight.Black)
                        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LBlue)) { Text(sh("VIP ODA OLUŞTUR", "CREATE VIP ROOM"), fontWeight = FontWeight.Bold) }
                        OutlinedTextField(
                            value = privateCode, onValueChange = onPrivateCode, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            placeholder = { Text(sh("6 haneli oda kodu", "6-character room code")) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LBlue, unfocusedBorderColor = LBorder, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                        )
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, LBlue.copy(alpha = .5f))) { Text(sh("ODA KODUYLA KATIL", "JOIN WITH ROOM CODE"), color = LBlue) }
                    }
                }
            }

            if (showFriends) item {
                Card(colors = CardDefaults.cardColors(containerColor = LCard), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(sh("ARKADAŞLAR", "FRIENDS"), color = LText, fontWeight = FontWeight.Black)
                        invites.forEach { i ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sh("Maç daveti", "Game invite"), color = LText)
                                Row { TextButton(onClick = { onInviteResponse(i.id, true) }) { Text(sh("Kabul", "Accept")) }; TextButton(onClick = { onInviteResponse(i.id, false) }) { Text(sh("Reddet", "Decline"), color = LRed) } }
                            }
                        }
                        friends.forEach { (_, p) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(p.displayName, color = LText, fontWeight = FontWeight.Bold); Text(if (p.presenceStatus == "online") sh("Çevrimiçi", "Online") else sh("Çevrimdışı", "Offline"), color = if (p.presenceStatus == "online") LBlue else LMuted, fontSize = 9.sp) }
                                Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online", colors = ButtonDefaults.buttonColors(containerColor = LBlue)) { Text(sh("Davet", "Invite")) }
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
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val liveWordPhase = room.status in listOf("playing", "final", "sudden_death")
    val quizActive = room.status == "quiz" && triviaRound != null && triviaQuestion != null
    val myTurn = room.currentPlayerId == me && liveWordPhase
    val lastItem = words.lastOrNull()
    val last = lastItem?.normalizedWord?.trim().orEmpty()
    val required = last.takeLast(1).takeIf { it.isNotBlank() }?.let { gameUppercase(it, room.language) } ?: "•"
    val shownLastWord = feedbackWord ?: gameUppercase(last, room.language)
    val shownLastWordColor = when { feedbackWord != null && feedbackCorrect == false -> LRed; shownLastWord.isNotBlank() -> LGreen; else -> LBlue }

    if (room.status == "waiting") { LightWaitingRoom(room.code, playerName, onExit); return }
    if (room.status == "finished") {
        LightResult(room.winnerId == me, room.winnerId == null, playerName, opponentName, myRounds, oppRounds, onRematch, onExit)
        return
    }

    val triviaResolved = quizActive && triviaRound?.resolvedAt != null
    val deadline = when { triviaResolved -> triviaRound?.resultUntil; quizActive -> triviaRound?.answerDeadline; else -> room.turnDeadline }
    var seconds by remember(deadline, room.status, triviaResolved) { mutableIntStateOf(when { deadline == null && !quizActive -> 0; triviaResolved -> 5; else -> 10 }) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(deadline, room.currentPlayerId, room.status) {
        val endMs = runCatching { deadline?.let { Instant.parse(it).toEpochMilli() } }.getOrNull() ?: return@LaunchedEffect
        var lastPulse = Int.MIN_VALUE
        while (true) {
            val remaining = endMs - Instant.now().toEpochMilli()
            if (remaining <= 0L) {
                seconds = 0
                if (quizActive && !triviaResolved) onTriviaTimeout() else if (!quizActive) { SonHarfSoundFx.explosion(); haptics.performHapticFeedback(HapticFeedbackType.LongPress); onTimeout() }
                break
            }
            val shown = ceil(remaining / 1000.0).toInt().coerceAtLeast(1)
            seconds = shown
            if (!quizActive && shown in 1..3 && shown != lastPulse) { lastPulse = shown; SonHarfSoundFx.heartbeat(); haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            delay(minOf(100L, remaining))
        }
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, LBg, LBg.copy(alpha = .72f)))).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LightPlayerCard(playerName, playerAvatarPath, playerGender, playerRating, myScore, myTurn, LBlue, false, Modifier.weight(1f))
            Surface(modifier = Modifier.size(78.dp), shape = CircleShape, color = Color.White, border = BorderStroke(3.dp, if (seconds <= 3 && !quizActive) LRed else LBlue), shadowElevation = 2.dp) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(seconds.toString(), color = LText, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(if (quizActive) "BONUS" else sh("sn", "sec"), color = if (quizActive) LGold else LBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            LightPlayerCard(opponentName.removeSuffix(" BOT"), opponentAvatarPath, opponentGender, opponentRating, oppScore, !myTurn && liveWordPhase, LRed, room.isBot, Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = LCard), shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, if (myTurn) LBlue.copy(alpha = .5f) else LBorder), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val shownCount = room.roundWordCount.coerceIn(0, 10)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", color = LText, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(10) { i -> Box(Modifier.size(if (i < shownCount) 7.dp else 5.dp).clip(CircleShape).background(if (i < shownCount) LBlue else LCard2)) } }
                    Text("$shownCount/10", color = LText, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when { quizActive -> sh("BONUS DÜELLOSU", "BONUS DUEL"); myTurn -> sh("SIRA SENDE", "YOUR TURN"); room.isBot && room.botTurn -> sh("BOT DÜŞÜNÜYOR", "BOT IS THINKING"); else -> sh("RAKİBİN HAMLESİ", "OPPONENT'S MOVE") },
                    color = when { quizActive -> LGold; myTurn -> LBlue; else -> LMuted }, fontSize = 13.sp, fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(.18f))
                Column(modifier = Modifier.height(170.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(sh("SON HARF", "LAST LETTER"), color = LMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(required, color = LText, fontSize = 62.sp, lineHeight = 64.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(3.dp))
                    Text(shownLastWord.ifBlank { sh("İLK KELİMEYİ YAZ", "ENTER FIRST WORD") }, color = shownLastWordColor, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text(if (last.isBlank()) sh("İlk kelimeyi sen başlat.", "Start with the first word.") else sh("$required ile başlayan bir kelime yaz", "Enter a word starting with $required"), color = LMuted, fontSize = 10.sp)
                }
                Spacer(Modifier.weight(.20f))
            }
        }

        LightVipWordHistory(isVip, words, room.language, Modifier.padding(horizontal = 12.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LightActionButton(sh("⚑ PES ET", "⚑ FORFEIT"), LRed, Modifier.weight(1f), onForfeit)
            LightActionButton(sh("● SOHBET", "● CHAT"), LBlue, Modifier.weight(1f), onChat)
            LightActionButton("★ BONUS", LGold, Modifier.weight(1f)) { }
        }

        if (quizActive) {
            val activeTrivia = requireNotNull(triviaRound)
            LightBonusCard(activeTrivia, requireNotNull(triviaQuestion), (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection, onTrivia, Modifier.padding(horizontal = 12.dp))
        }

        LightInputBar(wordInput, myTurn, busy, quizActive, onSubmit, Modifier.padding(horizontal = 12.dp))
        EmbeddedWordKeyboard(
            value = wordInput, language = room.language, enabled = !busy && !quizActive,
            submitEnabled = myTurn && wordInput.isNotBlank() && !busy && !quizActive,
            maxLength = 40, onValueChange = onWordInput, onSubmit = onSubmit, modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun LightPlayerCard(name: String, avatarPath: String?, gender: String?, rating: Int, score: Int, active: Boolean, accent: Color, bot: Boolean, modifier: Modifier) {
    Card(modifier = modifier.height(106.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp), border = BorderStroke(if (active) 3.dp else 1.dp, if (active) LGreen else LBorder), elevation = CardDefaults.cardElevation(defaultElevation = if (active) 3.dp else 1.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (bot) {
                Box(Modifier.size(56.dp, 74.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = .10f)), contentAlignment = Alignment.Center) { Text("BOT", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black) }
            } else {
                ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, width = 56.dp, height = 74.dp, accent = accent)
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(name, color = LText, maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(score.toString(), color = LText, fontSize = duelScoreFontSize(score).sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                Text("🏆 $rating", color = LGold, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LightVipWordHistory(isVip: Boolean, words: List<GameWordDto>, language: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(15.dp), color = Color.White, border = BorderStroke(1.dp, if (isVip) LBlue.copy(alpha = .28f) else LBorder)) {
        if (!isVip) {
            Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text(sh("🔒 Son kelimeleri sadece VIP üyeler görebilir.", "🔒 Only VIP members can see the latest words."), color = LMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        } else {
            Row(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("VIP", color = LGold, fontSize = 9.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(8.dp))
                if (words.isEmpty()) Text(sh("Henüz kelime yok.", "No words yet."), color = LMuted, fontSize = 10.sp)
                else LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(words.takeLast(6)) { word ->
                        Surface(shape = RoundedCornerShape(12.dp), color = LBlueSoft, border = BorderStroke(1.dp, LBlue.copy(alpha = .18f))) {
                            Text(gameUppercase(word.word.trim().ifBlank { word.normalizedWord.trim() }, language), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = LText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LightBonusCard(round: TriviaRoundDto, question: TriviaQuestionDto, myAnswer: Long?, onTrivia: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selectedAnswer = myAnswer
    val resolved = round.resolvedAt != null
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F2FF)), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, LPurple.copy(alpha = .35f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("★ BONUS +${round.bonusPoints}", color = LGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(question.question, color = LText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            val options = listOf(question.optionA, question.optionB, question.optionC, question.optionD)
            options.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { raw ->
                        val answerValue = raw.toLongOrNull()
                        val selected = answerValue != null && selectedAnswer == answerValue
                        val correct = resolved && answerValue != null && round.correctAnswer == answerValue
                        val containerColor = when { correct -> LGreen.copy(alpha = .14f); resolved && selected -> LRed.copy(alpha = .13f); selected -> LBlue.copy(alpha = .11f); else -> Color.Transparent }
                        val optionColor = when { correct -> LGreen; resolved && selected -> LRed; selected -> LBlue; else -> LText }
                        OutlinedButton(
                            onClick = { answerValue?.takeIf { it in 0L..Int.MAX_VALUE.toLong() }?.let { onTrivia(it.toInt()) } },
                            enabled = answerValue != null && selectedAnswer == null && !resolved,
                            modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                            border = BorderStroke(if (selected || correct) 2.dp else 1.dp, when { correct -> LGreen; resolved && selected -> LRed; selected -> LBlue; else -> LPurple.copy(alpha = .45f) }),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor, disabledContainerColor = containerColor, contentColor = optionColor, disabledContentColor = optionColor),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
                        ) { Text(raw, color = optionColor, fontSize = 9.sp, fontWeight = if (selected || correct) FontWeight.Black else FontWeight.Medium, maxLines = 1) }
                    }
                }
            }
            when {
                resolved && selectedAnswer == round.correctAnswer -> Text(sh("Doğru cevap!", "Correct answer!"), color = LGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                resolved -> Text(sh("Doğru cevap: ${round.correctAnswer ?: "—"}", "Correct answer: ${round.correctAnswer ?: "—"}"), color = LText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                selectedAnswer != null -> Text(sh("Cevabın alındı, sonuç bekleniyor…", "Answer received, waiting for result…"), color = LBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LightInputBar(value: String, myTurn: Boolean, busy: Boolean, quiz: Boolean, onSubmit: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.5.dp, if (myTurn && !quiz) LBlue else LBorder)) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(start = 15.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when { value.isNotBlank() -> value; quiz -> sh("Bonus turu devam ediyor…", "Bonus round in progress…"); myTurn -> sh("Kelimenizi yazın…", "Type your word…"); else -> sh("Kelimeyi hazırlayabilirsin…", "Prepare your word…") },
                color = if (value.isBlank()) LMuted else LText, fontSize = if (value.isBlank()) 14.sp else 18.sp, fontWeight = if (value.isBlank()) FontWeight.Medium else FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1,
            )
            Button(onClick = onSubmit, enabled = myTurn && value.isNotBlank() && !busy && !quiz, modifier = Modifier.height(40.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = LBlue, contentColor = Color.White, disabledContainerColor = LCard2, disabledContentColor = LMuted)) {
                Text("➤", fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun LightChoicePill(selected: Boolean, text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(52.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = if (selected) LBlueSoft else Color.White, border = BorderStroke(1.dp, if (selected) LBlue.copy(alpha = .5f) else LBorder)) {
        Box(contentAlignment = Alignment.Center) { Text(text, color = if (selected) LBlue else LText, fontWeight = FontWeight.Black, fontSize = 12.sp) }
    }
}

@Composable
private fun LightLobbyAction(title: String, subtitle: String, icon: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(104.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, LBorder)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 22.sp); Spacer(Modifier.height(7.dp)); Text(title, color = LText, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1); Spacer(Modifier.height(2.dp)); Text(subtitle, color = Color(0xFF4F6F95), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun LightActionButton(label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(38.dp), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, accent.copy(alpha = .45f)), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = accent), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun LightNotice(notice: String, modifier: Modifier = Modifier) {
    val error = notice.contains("sorun", true) || notice.contains("problem", true) || notice.contains("geçerli", true) || notice.contains("invalid", true) || notice.contains("doldu", true) || notice.contains("expired", true)
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (error) Color(0xFFFFF1F4) else Color.White, border = BorderStroke(1.dp, if (error) LRed.copy(alpha = .25f) else LBorder)) {
        Text(notice, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), color = if (error) LRed else LMuted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun LightWaitingRoom(code: String, playerName: String, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(LBg).statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth(.88f), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, LBorder)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(playerName, color = LText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(sh("RAKİP BEKLENİYOR", "WAITING FOR OPPONENT"), color = LBlue, fontWeight = FontWeight.Black)
                Text(sh("ODA KODU", "ROOM CODE"), color = LMuted, fontSize = 9.sp)
                Text(code, color = LText, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                CircularProgressIndicator(color = LBlue)
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text(sh("ODADAN ÇIK", "LEAVE ROOM"), color = LRed) }
            }
        }
    }
}

@Composable
private fun LightResult(won: Boolean, draw: Boolean, playerName: String, opponentName: String, myRounds: Int, oppRounds: Int, onRematch: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(LBg).statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth(.88f), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, if (draw) LGold.copy(alpha = .45f) else if (won) LBlue.copy(alpha = .45f) else LRed.copy(alpha = .35f))) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text(if (draw) sh("BERABERE", "DRAW") else if (won) sh("ZAFER", "VICTORY") else sh("MAÇ BİTTİ", "MATCH OVER"), color = if (draw) LGold else if (won) LBlue else LRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("$playerName  $myRounds : $oppRounds  $opponentName", color = LText, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Button(onClick = onRematch, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = LBlue)) { Text(sh("RÖVANŞ", "REMATCH"), fontWeight = FontWeight.Black) }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(sh("LOBİYE DÖN", "BACK TO LOBBY"), color = LText) }
            }
        }
    }
}
