package com.sonharf.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.TriviaQuestionDto
import com.sonharf.game.data.TriviaRoundDto
import java.time.Instant
import kotlinx.coroutines.delay

private val RefBlue = Color(0xFF148CFF)
private val RefCyan = Color(0xFF22D7FF)
private val RefRed = Color(0xFFFF365D)
private val RefGold = Color(0xFFFFB31A)
private val RefPurple = Color(0xFF9B4DFF)
private val RefWhite = Color(0xFFF7F8FF)
private val RefMuted = Color(0xFFAAB3CE)

@Composable
internal fun ReferenceDuelArena(
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
    wordInput: String,
    onWordInput: (String) -> Unit,
    notice: String,
    busy: Boolean,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
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
    val activeQuiz = room.status == "quiz" && triviaRound != null && triviaQuestion != null
    val myTurn = room.currentPlayerId == me && liveWordPhase
    val last = words.lastOrNull()?.normalizedWord?.trim().orEmpty()
    val required = last.lastOrNull()?.uppercaseChar()?.toString() ?: "•"

    var seconds by remember(room.turnDeadline, triviaRound?.answerDeadline, room.status) {
        mutableIntStateOf(if (activeQuiz) 20 else 7)
    }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(room.turnDeadline, triviaRound?.answerDeadline, room.currentPlayerId, room.status) {
        val deadline = if (activeQuiz) triviaRound?.answerDeadline else room.turnDeadline
        if (deadline == null) return@LaunchedEffect
        while (true) {
            seconds = runCatching {
                (Instant.parse(deadline).epochSecond - Instant.now().epochSecond)
                    .toInt().coerceAtLeast(0)
            }.getOrDefault(if (activeQuiz) 20 else 7)
            if (seconds <= 0) {
                if (activeQuiz) {
                    onTriviaTimeout()
                } else {
                    SonHarfSoundFx.explosion()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTimeout()
                }
                break
            }
            if (!activeQuiz && seconds in 1..5) SonHarfSoundFx.countdown()
            if (!activeQuiz && seconds in 1..3) {
                SonHarfSoundFx.heartbeat()
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            delay(1000)
        }
    }

    if (room.status == "finished") {
        ReferenceResultOverlay(
            won = room.winnerId == me,
            draw = room.winnerId == null,
            myRounds = myRounds,
            oppRounds = oppRounds,
            playerName = playerName,
            opponentName = opponentName,
            onRematch = onRematch,
            onExit = onExit,
        )
        return
    }

    val motion = rememberInfiniteTransition(label = "reference-duel-motion")
    val corePulse by motion.animateFloat(
        initialValue = .985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "core-pulse",
    )
    val urgentPulse by motion.animateFloat(
        initialValue = .95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
        label = "urgent-pulse",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight

            Image(
                painter = painterResource(R.drawable.duel_reference_skin),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )

            Box(
                Modifier
                    .offset(x = w * .145f, y = h * .015f)
                    .width(w * .245f)
                    .height(h * .105f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF03112D), Color(0xFF02091A))))
            )
            Box(
                Modifier
                    .offset(x = w * .590f, y = h * .015f)
                    .width(w * .250f)
                    .height(h * .105f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF090719), Color(0xFF170718))))
            )

            Box(
                Modifier
                    .offset(x = w * .018f, y = h * .016f)
                    .size(w * .150f)
                    .clip(CircleShape)
                    .background(Color(0xFF03102A))
            )

            Box(
                Modifier
                    .offset(x = w * .030f, y = h * .021f)
                    .size(w * .135f),
                contentAlignment = Alignment.Center,
            ) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = playerAvatarPath,
                    gender = playerGender,
                    name = playerName,
                    size = w * .118f,
                    accent = RefBlue,
                )
                ReferenceLevelBadge(
                    value = (playerRating / 80).coerceIn(1, 99),
                    color = RefBlue,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }

            if (!room.isBot) {
                Box(
                    Modifier
                        .offset(x = w * .816f, y = h * .016f)
                        .size(w * .150f)
                        .clip(CircleShape)
                        .background(Color(0xFF170718))
                )
                Box(
                    Modifier
                        .offset(x = w * .827f, y = h * .021f)
                        .size(w * .135f),
                    contentAlignment = Alignment.Center,
                ) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = opponentAvatarPath,
                        gender = opponentGender,
                        name = opponentName,
                        size = w * .118f,
                        accent = RefRed,
                    )
                    ReferenceLevelBadge(
                        value = (opponentRating / 80).coerceIn(1, 99),
                        color = RefRed,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }

            Text(
                playerName,
                modifier = Modifier.offset(x = w * .185f, y = h * .020f).width(w * .19f),
                color = RefWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                "🏆 ${formatRating(playerRating)}",
                modifier = Modifier.offset(x = w * .187f, y = h * .051f),
                color = RefGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                myScore.toString(),
                modifier = Modifier.offset(x = w * .190f, y = h * .072f),
                color = RefWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Box(
                Modifier
                    .offset(x = w * .337f, y = h * .025f)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (myTurn) RefBlue else RefBlue.copy(alpha = .35f))
            )

            Text(
                opponentName.removeSuffix(" BOT"),
                modifier = Modifier.offset(x = w * .622f, y = h * .020f).width(w * .19f),
                color = RefWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            if (room.isBot) {
                Text(
                    "BOT",
                    modifier = Modifier.offset(x = w * .760f, y = h * .023f),
                    color = RefRed,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                "🏆 ${formatRating(opponentRating)}",
                modifier = Modifier.offset(x = w * .700f, y = h * .051f),
                color = RefGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                oppScore.toString(),
                modifier = Modifier.offset(x = w * .706f, y = h * .072f),
                color = RefWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )

            Box(
                Modifier
                    .offset(x = w * .425f, y = h * .026f)
                    .size(w * .151f)
                    .graphicsLayer {
                        if (seconds <= 3) {
                            scaleX = urgentPulse
                            scaleY = urgentPulse
                        }
                    }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF08172E), Color(0xFF020718)))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        seconds.toString(),
                        color = RefWhite,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        sh("SN", "SEC"),
                        color = when {
                            activeQuiz -> RefGold
                            seconds <= 3 -> RefRed
                            else -> RefCyan
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            ReferenceMask(
                modifier = Modifier.offset(x = w * .030f, y = h * .136f).size(w * .185f, h * .064f)
            )
            Text(
                sh("ROUND", "ROUND"),
                modifier = Modifier.offset(x = w * .060f, y = h * .145f).width(w * .125f),
                color = RefMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                "${room.roundNo}/3",
                modifier = Modifier.offset(x = w * .060f, y = h * .162f).width(w * .125f),
                color = RefWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            ReferenceMask(
                modifier = Modifier.offset(x = w * .780f, y = h * .136f).size(w * .190f, h * .064f)
            )
            Text(
                sh("KELİME SAYISI", "WORD COUNT"),
                modifier = Modifier.offset(x = w * .790f, y = h * .145f).width(w * .165f),
                color = RefMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                "${room.roundWordCount}/10",
                modifier = Modifier.offset(x = w * .790f, y = h * .162f).width(w * .165f),
                color = RefWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )

            ReferenceMask(
                modifier = Modifier.offset(x = w * .270f, y = h * .135f).size(w * .47f, h * .045f)
            )
            Text(
                when {
                    activeQuiz -> sh("BONUS DÜELLOSU", "BONUS DUEL")
                    myTurn -> sh("SIRA SENDE", "YOUR TURN")
                    room.isBot && room.botTurn -> sh("BOT DÜŞÜNÜYOR", "BOT THINKING")
                    else -> sh("RAKİBİN HAMLESİ", "OPPONENT'S MOVE")
                },
                modifier = Modifier
                    .offset(x = w * .27f, y = h * .141f)
                    .width(w * .47f),
                color = when {
                    activeQuiz -> RefGold
                    myTurn -> RefCyan
                    else -> RefRed
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )

            ReferenceMask(
                modifier = Modifier.offset(x = w * .330f, y = h * .163f).size(w * .34f, h * .024f)
            )
            Row(
                modifier = Modifier.offset(x = w * .345f, y = h * .166f).width(w * .31f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(10) { i ->
                    Box(
                        Modifier
                            .size(if (i < room.roundWordCount) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < room.roundWordCount) {
                                    if (i % 3 == 0) RefPurple else RefBlue
                                } else Color(0xFF27324D)
                            )
                    )
                }
            }

            Box(
                Modifier
                    .offset(x = w * .333f, y = h * .232f)
                    .size(w * .335f)
                    .graphicsLayer {
                        scaleX = if (myTurn) corePulse else 1f
                        scaleY = if (myTurn) corePulse else 1f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF07152F).copy(alpha = .98f), Color(0xFF010611).copy(alpha = .98f))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        sh("SON HARF", "LAST LETTER"),
                        color = RefMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        required,
                        color = RefWhite,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (last.isBlank()) {
                            sh("İlk kelimeyi sen başlat!", "Start the first word!")
                        } else {
                            sh("$required ile başlayan\nkelimeyi kur!", "Build a word\nstarting with $required!")
                        },
                        color = RefWhite,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Box(
                Modifier
                    .offset(x = w * .027f, y = h * .435f)
                    .width(w * .944f)
                    .height(h * .047f)
                    .background(Color(0xFF020B1D).copy(alpha = .96f))
            )
            val recentWords = words.takeLast(5)
            val chipX = listOf(.040f, .184f, .328f, .497f, .648f)
            val chipW = listOf(.125f, .128f, .153f, .135f, .155f)
            recentWords.forEachIndexed { index, word ->
                ReferenceWordChip(
                    text = word.word.trim().ifBlank { word.normalizedWord.trim() }.uppercase(),
                    color = if (index == recentWords.lastIndex) RefGold else RefCyan,
                    modifier = Modifier
                        .offset(x = w * chipX[index], y = h * .443f)
                        .width(w * chipW[index])
                        .height(h * .033f),
                )
            }
            ReferenceWordChip(
                text = required,
                color = RefPurple,
                modifier = Modifier
                    .offset(x = w * .806f, y = h * .443f)
                    .width(w * .147f)
                    .height(h * .033f),
            )

            val warning = notice.contains("sorun", true) ||
                notice.contains("problem", true) ||
                notice.contains("geçerli", true) ||
                notice.contains("invalid", true) ||
                notice.contains("doldu", true) ||
                notice.contains("expired", true)
            Box(
                Modifier
                    .offset(x = w * .028f, y = h * .493f)
                    .width(w * .944f)
                    .height(h * .036f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (warning) Color(0xFF210817) else Color(0xFF06142A))
            )
            Text(
                notice,
                modifier = Modifier
                    .offset(x = w * .085f, y = h * .502f)
                    .width(w * .80f),
                color = if (warning) Color(0xFFFF7188) else RefCyan,
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Start,
            )

            ReferenceHitTarget(
                enabled = true,
                modifier = Modifier.offset(x = w * .028f, y = h * .539f).size(w * .297f, h * .052f),
                onClick = onForfeit,
            )
            ReferenceHitTarget(
                enabled = !room.isBot,
                modifier = Modifier.offset(x = w * .346f, y = h * .539f).size(w * .299f, h * .052f),
                onClick = onChat,
            )
            ReferenceHitTarget(
                enabled = true,
                modifier = Modifier.offset(x = w * .666f, y = h * .539f).size(w * .304f, h * .052f),
                onClick = { SonHarfSoundFx.bonus() },
            )

            Box(
                Modifier
                    .offset(x = w * .173f, y = h * .603f)
                    .width(w * .790f)
                    .height(h * .099f)
                    .background(Color(0xFF10072F).copy(alpha = .97f))
            )
            val bonusPoints = triviaRound?.bonusPoints ?: 3
            Text(
                "BONUS +$bonusPoints",
                modifier = Modifier.offset(x = w * .190f, y = h * .610f),
                color = RefGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                if (activeQuiz) triviaQuestion!!.question
                else sh("5 kelimede bir bonus düellosu açılır.", "A bonus duel opens every 5 words."),
                modifier = Modifier.offset(x = w * .190f, y = h * .636f).width(w * .72f),
                color = RefWhite,
                fontSize = 10.sp,
                maxLines = 2,
            )

            val optionValues = if (activeQuiz) {
                listOf(
                    triviaQuestion!!.optionA,
                    triviaQuestion.optionB,
                    triviaQuestion.optionC,
                    triviaQuestion.optionD,
                )
            } else emptyList()

            val optionXs = listOf(.190f, .385f, .580f, .776f)
            repeat(4) { index ->
                val raw = optionValues.getOrNull(index).orEmpty()
                ReferenceChoice(
                    text = if (raw.isBlank()) "—" else formatEstimateOption(raw),
                    enabled = activeQuiz && raw.toLongOrNull() != null,
                    modifier = Modifier
                        .offset(x = w * optionXs[index], y = h * .667f)
                        .width(w * .170f)
                        .height(h * .032f),
                    onClick = {
                        val value = raw.toLongOrNull()?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()
                        if (value != null) onTrivia(value)
                    },
                )
            }

            Box(
                Modifier
                    .offset(x = w * .030f, y = h * .723f)
                    .width(w * .805f)
                    .height(h * .045f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF03102A).copy(alpha = .97f)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    if (wordInput.isBlank()) {
                        if (myTurn) sh("Kelimenizi yazın...", "Type your word...")
                        else sh("Kelimeyi hazırlayabilirsin...", "Prepare your word...")
                    } else wordInput,
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = if (wordInput.isBlank()) Color(0xFF7E8AAE) else RefWhite,
                    fontSize = if (wordInput.isBlank()) 14.sp else 18.sp,
                    fontWeight = if (wordInput.isBlank()) FontWeight.Medium else FontWeight.Black,
                    maxLines = 1,
                )
            }
            ReferenceHitTarget(
                enabled = myTurn && wordInput.isNotBlank() && !busy && room.status != "quiz",
                modifier = Modifier.offset(x = w * .832f, y = h * .721f).size(w * .133f, h * .049f),
                onClick = onSubmit,
            )
            if (!(myTurn && wordInput.isNotBlank() && !busy && room.status != "quiz")) {
                Box(
                    Modifier
                        .offset(x = w * .836f, y = h * .724f)
                        .size(w * .125f, h * .043f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF02091A).copy(alpha = .48f))
                )
            }

            ReferenceKeyboardHitMap(
                w = w,
                h = h,
                language = room.language,
                value = wordInput,
                enabled = !busy && room.status != "quiz",
                submitEnabled = myTurn && wordInput.isNotBlank() && !busy && room.status != "quiz",
                onValueChange = onWordInput,
                onSubmit = onSubmit,
            )
        }
    }
}

@Composable
private fun ReferenceResultOverlay(
    won: Boolean,
    draw: Boolean,
    myRounds: Int,
    oppRounds: Int,
    playerName: String,
    opponentName: String,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF010611), Color(0xFF07142D))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color(0xFF06112A),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, if (won) RefBlue else RefRed),
            shadowElevation = 16.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    if (draw) sh("BERABERE", "DRAW") else if (won) sh("ZAFER", "VICTORY") else sh("MAÇ BİTTİ", "MATCH OVER"),
                    color = if (draw) RefGold else if (won) RefCyan else RefRed,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                )
                Text("$playerName  $myRounds : $oppRounds  $opponentName", color = RefWhite, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onRematch,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefGold, contentColor = Color(0xFF2A1700)),
                ) { Text(sh("RÖVANŞ", "REMATCH"), fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, RefBlue),
                ) { Text(sh("LOBİYE DÖN", "BACK TO LOBBY"), color = RefWhite) }
            }
        }
    }
}

@Composable
private fun ReferenceLevelBadge(value: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(value.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ReferenceMask(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF02091A).copy(alpha = .96f))
    )
}

@Composable
private fun ReferenceWordChip(text: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF03102A),
        border = BorderStroke(1.5.dp, color),
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (color == RefGold) Color(0xFFFFD66A) else RefWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReferenceChoice(
    text: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF160A3C),
        border = BorderStroke(1.dp, RefPurple),
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (enabled) RefWhite else RefMuted.copy(alpha = .55f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReferenceHitTarget(
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier.clickable(
            enabled = enabled,
            interactionSource = source,
            indication = null,
            onClick = {
                SonHarfSoundFx.tap()
                onClick()
            },
        )
    )
}

@Composable
private fun ReferenceKeyboardHitMap(
    w: Dp,
    h: Dp,
    language: String,
    value: String,
    enabled: Boolean,
    submitEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val rows = if (language.lowercase() == "en") {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P"),
            listOf("A","S","D","F","G","H","J","K","L"),
            listOf("Z","X","C","V","B","N","M"),
        )
    } else {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
            listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
            listOf("Z","X","C","V","B","N","M","Ö","Ç"),
        )
    }

    val rowY = listOf(.781f, .831f, .881f)
    val rowLeft = listOf(.027f, .061f, .112f)
    val rowRight = listOf(.973f, .940f, .886f)

    rows.forEachIndexed { rowIndex, keys ->
        val left = rowLeft[rowIndex]
        val right = rowRight[rowIndex]
        val available = right - left
        val cell = available / keys.size
        keys.forEachIndexed { index, key ->
            ReferenceHitTarget(
                enabled = enabled && value.length < 40,
                modifier = Modifier
                    .offset(x = w * (left + cell * index), y = h * rowY[rowIndex])
                    .width(w * cell)
                    .height(h * .045f),
                onClick = {
                    SonHarfSoundFx.typingClick()
                    onValueChange((value + key).take(40))
                },
            )
        }
    }

    ReferenceHitTarget(
        enabled = enabled && value.isNotEmpty(),
        modifier = Modifier.offset(x = w * .027f, y = h * .928f).size(w * .220f, h * .050f),
        onClick = { onValueChange(value.dropLast(1)) },
    )
    ReferenceHitTarget(
        enabled = enabled && value.isNotEmpty(),
        modifier = Modifier.offset(x = w * .255f, y = h * .928f).size(w * .320f, h * .050f),
        onClick = { onValueChange("") },
    )
    ReferenceHitTarget(
        enabled = submitEnabled,
        modifier = Modifier.offset(x = w * .586f, y = h * .928f).size(w * .385f, h * .050f),
        onClick = onSubmit,
    )

    if (!submitEnabled) {
        Box(
            Modifier
                .offset(x = w * .590f, y = h * .931f)
                .size(w * .377f, h * .044f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF241A03).copy(alpha = .44f))
        )
    }
}

private fun formatRating(value: Int): String {
    val raw = value.coerceAtLeast(0).toString()
    return raw.reversed().chunked(3).joinToString(".").reversed()
}

private fun formatEstimateOption(raw: String): String {
    val value = raw.toLongOrNull() ?: return raw
    return when {
        value >= 1_000_000 -> {
            val whole = value / 1_000_000.0
            val shown = if (whole % 1.0 == 0.0) {
                whole.toInt().toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", whole).replace('.', ',')
            }
            "$shown MİLYON"
        }
        value >= 1000 -> value.toString().reversed().chunked(3).joinToString(".").reversed()
        else -> value.toString()
    }
}
