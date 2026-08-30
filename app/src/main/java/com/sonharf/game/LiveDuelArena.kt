package com.sonharf.game

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.TriviaQuestionDto
import com.sonharf.game.data.TriviaRoundDto
import java.time.Instant
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val LiveBlue = Color(0xFF1C8CFF)
private val LiveCyan = Color(0xFF20D9FF)
private val LiveRed = Color(0xFFFF3A65)
private val LiveGold = Color(0xFFFFB51C)
private val LivePurple = Color(0xFF9B50FF)
private val LiveWhite = Color(0xFFF7F9FF)
private val LiveMuted = Color(0xFF9CA8C7)
private val LiveDark = Color(0xFF020713)
private val LivePanel = Color(0xFF061027)

@Composable
internal fun LiveDuelArena(
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
    val lastWord = words.lastOrNull()?.normalizedWord?.trim().orEmpty()
    val required = lastWord.lastOrNull()?.uppercaseChar()?.toString() ?: "•"

    if (room.status == "waiting") {
        LiveWaitingRoom(
            roomCode = room.code,
            playerName = playerName,
            onExit = onExit,
        )
        return
    }

    if (room.status == "finished") {
        LiveResultScreen(
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

    val deadlineText = if (activeQuiz) triviaRound?.answerDeadline else room.turnDeadline
    var seconds by remember(deadlineText, room.status) {
        mutableIntStateOf(if (activeQuiz) 20 else 7)
    }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(deadlineText, room.currentPlayerId, room.status) {
        val deadline = deadlineText ?: return@LaunchedEffect
        val deadlineMs = runCatching { Instant.parse(deadline).toEpochMilli() }.getOrNull() ?: return@LaunchedEffect
        var heartbeatSecond = Int.MIN_VALUE
        while (true) {
            val remainingMs = deadlineMs - Instant.now().toEpochMilli()
            if (remainingMs <= 0L) {
                seconds = 0
                if (activeQuiz) {
                    onTriviaTimeout()
                } else {
                    SonHarfSoundFx.explosion()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTimeout()
                }
                break
            }
            val shown = ceil(remainingMs / 1000.0).toInt().coerceAtLeast(1)
            seconds = shown
            if (!activeQuiz && shown in 1..3 && shown != heartbeatSecond) {
                heartbeatSecond = shown
                SonHarfSoundFx.heartbeat()
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            delay(minOf(100L, remainingMs))
        }
    }

    val motion = rememberInfiniteTransition(label = "live-duel-motion")
    val corePulse by motion.animateFloat(
        initialValue = .985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "core-pulse",
    )
    val urgentPulse by motion.animateFloat(
        initialValue = .96f,
        targetValue = 1.055f,
        animationSpec = infiniteRepeatable(
            animation = tween(330, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "urgent-pulse",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(LiveDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Image(
            painter = painterResource(R.drawable.duel_arena_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = .18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = .24f),
                        )
                    )
                )
        )

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            val h = maxHeight
            val gap = 5.dp
            val topH = h * .125f
            val arenaH = h * .360f
            val statusH = h * .040f
            val actionH = h * .060f
            val bonusH = h * .120f
            val inputH = h * .060f
            val keyboardH = h * .205f

            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(topH),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    LivePlayerCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        asset = R.drawable.hud_player_blue,
                        name = playerName,
                        avatarPath = playerAvatarPath,
                        gender = playerGender,
                        rating = playerRating,
                        score = myScore,
                        accent = LiveBlue,
                        active = myTurn,
                        bot = false,
                        avatarOnRight = false,
                    )
                    LiveTimer(
                        modifier = Modifier
                            .width(topH * .78f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                if (!activeQuiz && seconds <= 3) {
                                    scaleX = urgentPulse
                                    scaleY = urgentPulse
                                }
                            },
                        seconds = seconds,
                        quiz = activeQuiz,
                    )
                    LivePlayerCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        asset = R.drawable.hud_opponent_red,
                        name = opponentName.removeSuffix(" BOT"),
                        avatarPath = opponentAvatarPath,
                        gender = opponentGender,
                        rating = opponentRating,
                        score = oppScore,
                        accent = LiveRed,
                        active = !myTurn && liveWordPhase,
                        bot = room.isBot,
                        avatarOnRight = true,
                    )
                }

                Spacer(Modifier.height(gap))

                LiveMainArena(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(arenaH),
                    room = room,
                    myTurn = myTurn,
                    activeQuiz = activeQuiz,
                    required = required,
                    lastWord = lastWord,
                    words = words,
                    corePulse = corePulse,
                )

                Spacer(Modifier.height(gap))

                LiveNoticeBar(
                    modifier = Modifier.fillMaxWidth().height(statusH),
                    notice = notice,
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    Modifier.fillMaxWidth().height(actionH),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    LiveImageButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        asset = R.drawable.duel_button_red,
                        label = "⚑  ${sh("PES ET", "FORFEIT")}",
                        enabled = true,
                        onClick = onForfeit,
                    )
                    LiveImageButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        asset = R.drawable.duel_button_blue,
                        label = "●  ${sh("SOHBET", "CHAT")}",
                        enabled = !room.isBot,
                        onClick = onChat,
                    )
                    LiveImageButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        asset = R.drawable.duel_button_gold,
                        label = "★  BONUS",
                        enabled = activeQuiz,
                        onClick = { if (activeQuiz) SonHarfSoundFx.bonus() },
                    )
                }

                Spacer(Modifier.height(4.dp))

                LiveBonusPanel(
                    modifier = Modifier.fillMaxWidth().height(bonusH),
                    active = activeQuiz,
                    round = triviaRound,
                    question = triviaQuestion,
                    wordCount = room.roundWordCount,
                    onTrivia = onTrivia,
                )

                Spacer(Modifier.height(4.dp))

                LiveInputBar(
                    modifier = Modifier.fillMaxWidth().height(inputH),
                    value = wordInput,
                    myTurn = myTurn,
                    busy = busy,
                    quiz = activeQuiz,
                    onSubmit = onSubmit,
                )

                Spacer(Modifier.height(4.dp))

                LiveGameKeyboard(
                    modifier = Modifier.fillMaxWidth().height(keyboardH),
                    language = room.language,
                    value = wordInput,
                    enabled = !busy && !activeQuiz,
                    submitEnabled = myTurn && wordInput.isNotBlank() && !busy && !activeQuiz,
                    onValueChange = onWordInput,
                    onSubmit = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun LivePlayerCard(
    modifier: Modifier,
    asset: Int,
    name: String,
    avatarPath: String?,
    gender: String?,
    rating: Int,
    score: Int,
    accent: Color,
    active: Boolean,
    bot: Boolean,
    avatarOnRight: Boolean,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(asset),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )

        val avatar: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .fillMaxHeight(.80f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (bot) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = Color(0xFF0B1530),
                        border = BorderStroke(2.dp, accent.copy(alpha = .86f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("BOT", color = LiveWhite, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = avatarPath,
                        gender = gender,
                        name = name,
                        size = 58.dp,
                        accent = accent,
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(if (avatarOnRight) Alignment.BottomEnd else Alignment.BottomStart)
                        .size(22.dp),
                    shape = CircleShape,
                    color = accent,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .60f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            (rating / 80).coerceIn(1, 99).toString(),
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }

        val info: @Composable (Modifier) -> Unit = { infoModifier ->
            Column(
                infoModifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (avatarOnRight) Alignment.End else Alignment.Start,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (avatarOnRight) Arrangement.End else Arrangement.Start,
                ) {
                    Text(
                        name,
                        color = LiveWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    if (bot) {
                        Spacer(Modifier.width(4.dp))
                        Text("BOT", color = LiveRed, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                    if (active) {
                        Spacer(Modifier.width(5.dp))
                        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                    }
                }
                Text(
                    "🏆 ${formatLiveRating(rating)}",
                    color = LiveGold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    score.toString(),
                    color = LiveWhite,
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!avatarOnRight) {
                avatar()
                Spacer(Modifier.width(8.dp))
                info(Modifier.weight(1f))
            } else {
                info(Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                avatar()
            }
        }
    }
}

@Composable
private fun LiveTimer(
    modifier: Modifier,
    seconds: Int,
    quiz: Boolean,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.duel_timer_ring),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                seconds.toString(),
                color = LiveWhite,
                fontSize = 34.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                if (quiz) "BONUS" else sh("SN", "SEC"),
                color = if (quiz) LiveGold else LiveCyan,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun LiveMainArena(
    modifier: Modifier,
    room: GameRoomDto,
    myTurn: Boolean,
    activeQuiz: Boolean,
    required: String,
    lastWord: String,
    words: List<GameWordDto>,
    corePulse: Float,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.duel_main_frame),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight

            val displayWordCount = room.roundWordCount.coerceIn(0, 10)

            Column(
                Modifier
                    .offset(x = w * .035f, y = h * .035f)
                    .width(w * .18f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ROUND", color = LiveMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text("${room.roundNo}/3", color = LiveWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            Text(
                when {
                    activeQuiz -> sh("BONUS DÜELLOSU", "BONUS DUEL")
                    myTurn -> sh("SIRA SENDE", "YOUR TURN")
                    room.isBot && room.botTurn -> sh("BOT DÜŞÜNÜYOR", "BOT THINKING")
                    else -> sh("RAKİBİN HAMLESİ", "OPPONENT'S MOVE")
                },
                modifier = Modifier
                    .offset(x = w * .25f, y = h * .035f)
                    .width(w * .50f),
                color = when {
                    activeQuiz -> LiveGold
                    myTurn -> LiveCyan
                    else -> LiveRed
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Column(
                Modifier
                    .offset(x = w * .79f, y = h * .035f)
                    .width(w * .17f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(sh("KELİME SAYISI", "WORD COUNT"), color = LiveMuted, fontSize = 7.sp, fontWeight = FontWeight.Black)
                Text("$displayWordCount/10", color = LiveWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            Row(
                Modifier
                    .offset(x = w * .34f, y = h * .105f)
                    .width(w * .32f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(10) { i ->
                    Box(
                        Modifier
                            .size(if (i < displayWordCount) 6.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < displayWordCount) {
                                    if (i % 3 == 0) LivePurple else LiveBlue
                                } else Color(0xFF26314B)
                            )
                    )
                }
            }

            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = -(h * .015f))
                    .size(w * .52f)
                    .graphicsLayer {
                        scaleX = if (myTurn) corePulse else 1f
                        scaleY = if (myTurn) corePulse else 1f
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.duel_energy_core),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(.66f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        sh("SON HARF", "LAST LETTER"),
                        color = LiveMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        required,
                        color = LiveWhite,
                        fontSize = 60.sp,
                        lineHeight = 62.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (lastWord.isBlank()) {
                            sh("İlk kelimeyi sen başlat!", "Start the first word!")
                        } else {
                            sh("$required ile başlayan kelimeyi kur!", "Build a word starting with $required!")
                        },
                        color = LiveWhite,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }

            Text(
                sh("KELİME ZİNCİRİ", "WORD CHAIN"),
                modifier = Modifier.offset(x = w * .04f, y = h * .77f),
                color = LiveCyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
            )

            val recent = words.takeLast(5)
            LazyRow(
                modifier = Modifier
                    .offset(x = w * .035f, y = h * .815f)
                    .width(w * .93f)
                    .height(h * .145f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(recent) { index, item ->
                    LiveWordChip(
                        text = item.word.trim().ifBlank { item.normalizedWord.trim() }.uppercase(),
                        highlighted = index == recent.lastIndex,
                        modifier = Modifier.widthIn(min = 68.dp, max = 98.dp).fillMaxHeight(.90f),
                    )
                }
                item {
                    LiveWordChip(
                        text = required,
                        highlighted = false,
                        purple = true,
                        modifier = Modifier.width(68.dp).fillMaxHeight(.90f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveWordChip(
    text: String,
    highlighted: Boolean,
    modifier: Modifier,
    purple: Boolean = false,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (purple) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0A0C2B),
                border = BorderStroke(1.5.dp, LivePurple),
            ) {}
        } else {
            Image(
                painter = painterResource(if (highlighted) R.drawable.duel_word_chip_gold else R.drawable.duel_word_chip_blue),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
        Text(
            text,
            color = if (highlighted) Color(0xFFFFD96C) else LiveWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiveNoticeBar(
    modifier: Modifier,
    notice: String,
) {
    val error = notice.contains("sorun", true) ||
        notice.contains("hata", true) ||
        notice.contains("geçerli", true) ||
        notice.contains("bulunamadı", true) ||
        notice.contains("invalid", true) ||
        notice.contains("expired", true) ||
        notice.contains("doldu", true)

    val cleanNotice = when {
        notice.contains("aktif maçına dönüldü", true) -> sh("Düello aktif.", "Duel active.")
        notice.contains("returned to your active match", true) -> sh("Düello aktif.", "Duel active.")
        notice.contains("İşlem tekrar deneniyor", true) -> sh("Bağlantı yenileniyor…", "Refreshing connection…")
        else -> notice
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (error) Color(0xFF230916) else Color(0xFF04162B),
        border = BorderStroke(1.dp, if (error) LiveRed.copy(alpha = .65f) else LiveCyan.copy(alpha = .45f)),
    ) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                cleanNotice,
                color = if (error) Color(0xFFFF7B91) else LiveCyan,
                fontSize = 9.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LiveImageButton(
    modifier: Modifier,
    asset: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .clickable(
                enabled = enabled,
                interactionSource = source,
                indication = null,
            ) {
                SonHarfSoundFx.tap()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(asset),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = if (enabled) 1f else .42f,
        )
        Text(
            label,
            color = if (enabled) LiveWhite else LiveMuted.copy(alpha = .55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiveBonusPanel(
    modifier: Modifier,
    active: Boolean,
    round: TriviaRoundDto?,
    question: TriviaQuestionDto?,
    wordCount: Int,
    onTrivia: (Int) -> Unit,
) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.duel_bonus_panel),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = if (active) 1f else .72f,
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎁", fontSize = 24.sp)
                Text(
                    "+${round?.bonusPoints ?: 3}",
                    color = LiveGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "BONUS +${round?.bonusPoints ?: 3}",
                    color = LiveGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (active && question != null) {
                        question.question
                    } else {
                        val remaining = (5 - (wordCount % 5)).let { if (it == 5) 5 else it }
                        sh(
                            "Bonus düellosuna $remaining kelime kaldı.",
                            "$remaining words until the bonus duel.",
                        )
                    },
                    color = LiveWhite,
                    fontSize = 9.sp,
                    maxLines = 2,
                )
                Spacer(Modifier.height(5.dp))

                if (active && question != null) {
                    val options = listOf(question.optionA, question.optionB, question.optionC, question.optionD)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        options.forEach { raw ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clickable(enabled = raw.toLongOrNull() != null) {
                                        raw.toLongOrNull()
                                            ?.coerceIn(0, Int.MAX_VALUE.toLong())
                                            ?.toInt()
                                            ?.let(onTrivia)
                                    },
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF160A3A),
                                border = BorderStroke(1.dp, LivePurple),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        formatLiveEstimate(raw),
                                        color = LiveWhite,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { (wordCount % 5) / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = LivePurple,
                        trackColor = Color.White.copy(alpha = .08f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveInputBar(
    modifier: Modifier,
    value: String,
    myTurn: Boolean,
    busy: Boolean,
    quiz: Boolean,
    onSubmit: () -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Image(
                painter = painterResource(R.drawable.duel_input_panel),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Text(
                when {
                    value.isNotBlank() -> value
                    quiz -> sh("Bonus turu devam ediyor…", "Bonus round in progress…")
                    myTurn -> sh("Kelimenizi yazın…", "Type your word…")
                    else -> sh("Kelimeyi hazırlayabilirsin…", "Prepare your word…")
                },
                modifier = Modifier.padding(horizontal = 16.dp),
                color = if (value.isBlank()) LiveMuted else LiveWhite,
                fontSize = if (value.isBlank()) 12.sp else 17.sp,
                fontWeight = if (value.isBlank()) FontWeight.Medium else FontWeight.Black,
                maxLines = 1,
            )
        }

        LiveImageButton(
            modifier = Modifier.width(70.dp).fillMaxHeight(),
            asset = R.drawable.duel_button_gold,
            label = "➤",
            enabled = myTurn && value.isNotBlank() && !busy && !quiz,
            onClick = onSubmit,
        )
    }
}

@Composable
private fun LiveGameKeyboard(
    modifier: Modifier,
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

    Surface(
        modifier = modifier,
        color = Color(0xFF020817).copy(alpha = .92f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LiveBlue.copy(alpha = .30f)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = when (rowIndex) {
                            1 -> 8.dp
                            2 -> 22.dp
                            else -> 0.dp
                        }),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    row.forEach { key ->
                        LiveKey(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            label = key,
                            enabled = enabled && value.length < 40,
                            onClick = {
                                SonHarfSoundFx.typingClick()
                                onValueChange((value + key).take(40))
                            },
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().weight(1.12f).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                LiveSpecialKey(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = "⌫",
                    asset = R.drawable.duel_button_blue,
                    enabled = enabled && value.isNotEmpty(),
                    onClick = { onValueChange(value.dropLast(1)) },
                )
                LiveSpecialKey(
                    modifier = Modifier.weight(1.45f).fillMaxHeight(),
                    label = sh("TEMİZLE", "CLEAR"),
                    asset = R.drawable.duel_button_blue,
                    enabled = enabled && value.isNotEmpty(),
                    onClick = { onValueChange("") },
                )
                LiveSpecialKey(
                    modifier = Modifier.weight(1.7f).fillMaxHeight(),
                    label = "${sh("GÖNDER", "SEND")}  ➤",
                    asset = R.drawable.duel_button_gold,
                    enabled = submitEnabled,
                    onClick = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun LiveKey(
    modifier: Modifier,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier.clickable(
            enabled = enabled,
            interactionSource = source,
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.duel_keycap_blue),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = if (enabled) 1f else .42f,
        )
        Text(
            label,
            color = if (enabled) LiveWhite else LiveMuted.copy(alpha = .45f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun LiveSpecialKey(
    modifier: Modifier,
    label: String,
    asset: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier.clickable(
            enabled = enabled,
            interactionSource = source,
            indication = null,
        ) {
            SonHarfSoundFx.tap()
            onClick()
        },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(asset),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = if (enabled) 1f else .38f,
        )
        Text(
            label,
            color = if (enabled) LiveWhite else LiveMuted.copy(alpha = .45f),
            fontSize = if (label.length > 5) 10.sp else 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiveWaitingRoom(
    roomCode: String,
    playerName: String,
    onExit: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(LiveDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.duel_arena_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(.88f),
            shape = RoundedCornerShape(28.dp),
            color = LivePanel.copy(alpha = .96f),
            border = BorderStroke(2.dp, LivePurple),
            shadowElevation = 14.dp,
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(playerName, color = LiveWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(sh("RAKİP BEKLENİYOR", "WAITING FOR OPPONENT"), color = LiveCyan, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(sh("ODA KODU", "ROOM CODE"), color = LiveMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(roomCode, color = LiveGold, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                CircularProgressIndicator(color = LiveCyan, trackColor = Color.White.copy(alpha = .08f))
                LiveImageButton(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    asset = R.drawable.duel_button_red,
                    label = sh("ODADAN ÇIK", "LEAVE ROOM"),
                    enabled = true,
                    onClick = onExit,
                )
            }
        }
    }
}

@Composable
private fun LiveResultScreen(
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
            .background(LiveDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.duel_arena_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(.88f),
            shape = RoundedCornerShape(28.dp),
            color = LivePanel.copy(alpha = .96f),
            border = BorderStroke(2.dp, if (draw) LiveGold else if (won) LiveBlue else LiveRed),
            shadowElevation = 16.dp,
        ) {
            Column(
                Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    if (draw) sh("BERABERE", "DRAW") else if (won) sh("ZAFER", "VICTORY") else sh("MAÇ BİTTİ", "MATCH OVER"),
                    color = if (draw) LiveGold else if (won) LiveCyan else LiveRed,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "$playerName  $myRounds : $oppRounds  $opponentName",
                    color = LiveWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                LiveImageButton(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    asset = R.drawable.duel_button_gold,
                    label = "⚡ ${sh("RÖVANŞ", "REMATCH")}",
                    enabled = true,
                    onClick = onRematch,
                )
                LiveImageButton(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    asset = R.drawable.duel_button_blue,
                    label = sh("LOBİYE DÖN", "BACK TO LOBBY"),
                    enabled = true,
                    onClick = onExit,
                )
            }
        }
    }
}

private fun formatLiveRating(value: Int): String {
    val raw = value.coerceAtLeast(0).toString()
    return raw.reversed().chunked(3).joinToString(".").reversed()
}

private fun formatLiveEstimate(raw: String): String {
    val value = raw.toLongOrNull() ?: return raw
    return when {
        value >= 1_000_000 -> {
            val whole = value / 1_000_000.0
            val shown = if (whole % 1.0 == 0.0) {
                whole.toInt().toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", whole).replace('.', ',')
            }
            "$shown M"
        }
        value >= 1000 -> value.toString().reversed().chunked(3).joinToString(".").reversed()
        else -> value.toString()
    }
}
