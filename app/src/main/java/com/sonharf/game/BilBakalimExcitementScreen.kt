package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private enum class ExcitementPhase { ANSWER, RESULT, MATCH_END }

private val BilBg = Color(0xFFF7F9FC)
private val BilPanel = Color.White
private val BilPanel2 = Color(0xFFF0F4F8)
private val BilText = Color(0xFF182235)
private val BilMuted = Color(0xFF718096)
private val BilCyan = Color(0xFF1769E0)
private val BilGold = Color(0xFFF3A81A)
private val BilGreen = Color(0xFF22B95F)
private val BilRed = Color(0xFFE64B55)

@Composable
internal fun BilBakalimExcitementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    val prefs = remember { context.getSharedPreferences("bil_bakalim_excitement", 0) }
    var rating by remember { mutableIntStateOf(prefs.getInt("rating", 1000)) }
    var wins by remember { mutableIntStateOf(prefs.getInt("wins", 0)) }
    var losses by remember { mutableIntStateOf(prefs.getInt("losses", 0)) }
    var bestStreak by remember { mutableIntStateOf(prefs.getInt("best_streak", 0)) }
    var matchStreak by remember { mutableIntStateOf(0) }
    var deck by remember { mutableStateOf(bilBakalimQuestions.shuffled().take(10)) }
    var index by remember { mutableIntStateOf(0) }
    var playerScore by remember { mutableIntStateOf(0) }
    var rivalScore by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(20) }
    var input by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(ExcitementPhase.ANSWER) }
    var risk by remember { mutableStateOf(false) }
    var jokerUsed by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var roundMessage by remember { mutableStateOf("") }
    var lastTitle by remember { mutableStateOf<String?>(null) }
    var lastPlayerAnswer by remember { mutableStateOf<Double?>(null) }
    var lastRivalAnswer by remember { mutableStateOf<Double?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var playedCount by remember { mutableIntStateOf(0) }
    var finalApplied by remember { mutableStateOf(false) }

    val q = deck[index]
    val questionNo = index + 1
    val league = BilBakalimCompetitionEngine.league(rating)
    val mastery = BilBakalimCompetitionEngine.categoryMastery(correctCount, playedCount)
    val isBoss = questionNo == 5
    val isFinal = questionNo == 10

    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        val me = b.currentUserId() ?: return@LaunchedEffect
        playerProfile = runCatching { b.getProfile(me) }.getOrNull()
    }

    fun resetQuestion() {
        input = ""
        seconds = 20
        phase = ExcitementPhase.ANSWER
        risk = false
        hint = null
        roundMessage = ""
        lastTitle = null
        lastPlayerAnswer = null
        lastRivalAnswer = null
    }

    fun resetMatch() {
        deck = bilBakalimQuestions.shuffled().take(10)
        index = 0
        playerScore = 0
        rivalScore = 0
        matchStreak = 0
        correctCount = 0
        playedCount = 0
        jokerUsed = false
        finalApplied = false
        resetQuestion()
    }

    fun advance() {
        if (index >= deck.lastIndex) phase = ExcitementPhase.MATCH_END
        else {
            index += 1
            resetQuestion()
        }
    }

    fun submit(answer: Double?) {
        if (phase != ExcitementPhase.ANSWER) return
        val spread = max(1.0, abs(q.answer) * Random.nextDouble(.08, .38))
        val rivalAnswer = if (q.answer == 0.0) Random.nextDouble(0.0, 4.0)
        else max(0.0, q.answer + if (Random.nextBoolean()) spread else -spread)

        lastPlayerAnswer = answer
        lastRivalAnswer = rivalAnswer

        val rules = BilRoundRules(questionNo = questionNo, secondsLeft = seconds, riskEnabled = risk, streak = matchStreak)
        val outcome = BilBakalimCompetitionEngine.resolve(answer, rivalAnswer, q.answer, rules)
        val rivalPoints = (10 + if (isBoss) 5 else 0) * rules.multiplier
        playedCount += 1
        if (outcome.won) {
            SonHarfSoundFx.wordAccepted()
            playerScore += outcome.points
            matchStreak += 1
            bestStreak = max(bestStreak, matchStreak)
            correctCount += 1
            lastTitle = outcome.title
            roundMessage = buildString {
                append("KAZANDIN • +${outcome.points}")
                if (outcome.speedBonus > 0) append(" • Hız +${outcome.speedBonus}")
                if (outcome.streakBonus > 0) append(" • Seri +${outcome.streakBonus}")
            }
        } else {
            SonHarfSoundFx.warning()
            rivalScore += rivalPoints
            matchStreak = 0
            roundMessage = "Rakip daha yakındı • +$rivalPoints"
        }
        phase = ExcitementPhase.RESULT
    }

    LaunchedEffect(index, phase) {
        if (phase != ExcitementPhase.ANSWER) return@LaunchedEffect
        seconds = 20
        while (seconds > 0 && phase == ExcitementPhase.ANSWER) {
            delay(1000)
            seconds -= 1
            if (seconds in 1..5) SonHarfSoundFx.countdown()
        }
        if (seconds <= 0 && phase == ExcitementPhase.ANSWER) submit(null)
    }

    LaunchedEffect(phase, finalApplied) {
        if (phase != ExcitementPhase.MATCH_END || finalApplied) return@LaunchedEffect
        finalApplied = true
        val won = playerScore >= rivalScore
        if (won) {
            wins += 1
            rating += 18
        } else {
            losses += 1
            rating = (rating - 12).coerceAtLeast(700)
        }
        prefs.edit()
            .putInt("rating", rating)
            .putInt("wins", wins)
            .putInt("losses", losses)
            .putInt("best_streak", bestStreak)
            .apply()
        runCatching {
            backend?.logUnifiedEvent(
                "bil_bakalim_match_finished",
                "score=$playerScore-$rivalScore;won=$won",
            )
        }
        if (won) SonHarfSoundFx.victory() else SonHarfSoundFx.defeat()
    }

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, BilBg, Color(0xFFF1F6FC))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
                Text("‹ OYUNLAR", color = BilCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BİL BAKALIM", color = BilText, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("EN YAKIN TAHMİN", color = BilCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(league, color = BilText, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text("$rating", color = BilCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        CompetitionVsCard(
            myName = playerProfile?.displayName ?: sh("Sen", "You"),
            opponentName = sh("Bil Rakibi", "Trivia Rival"),
            myAvatarPath = playerProfile?.avatarPath,
            opponentAvatarPath = null,
            myGender = playerProfile?.gender,
            myRating = playerProfile?.rating,
            centerText = "$playerScore–$rivalScore",
        )

        CompetitionLeadStrip(
            myScore = playerScore,
            opponentScore = rivalScore,
            myStreak = matchStreak,
            myAction = if (roundMessage.startsWith("KAZANDIN")) sh("Soruyu sen aldın.", "You won the question.") else null,
            opponentAction = if (roundMessage.startsWith("Rakip")) sh("Rakip soruyu aldı.", "Rival won the question.") else null,
        )

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = BilPanel,
            border = BorderStroke(
                1.dp,
                when {
                    phase == ExcitementPhase.RESULT -> BilGreen.copy(alpha = .45f)
                    isFinal -> BilRed.copy(alpha = .45f)
                    isBoss -> BilGold.copy(alpha = .45f)
                    else -> BilCyan.copy(alpha = .25f)
                },
            ),
        ) {
            when (phase) {
                ExcitementPhase.ANSWER -> Column(
                    Modifier.fillMaxSize().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SORU $questionNo/10", color = BilMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("⏱ $seconds sn", color = if (seconds <= 5) BilRed else BilText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    if (isBoss) Text("👑 BOSS +5", color = BilGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    if (isFinal) Text("⚡ FİNAL x2", color = BilRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(q.category.uppercase(), color = BilCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(
                        q.question,
                        color = BilText,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    hint?.let { Text("💡 $it", color = BilGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    OutlinedTextField(
                        value = input,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        label = { Text("Tahminin", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BilCyan,
                            unfocusedBorderColor = BilCyan.copy(alpha = .45f),
                            cursorColor = Color.Transparent,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = risk,
                            onClick = { risk = !risk },
                            label = { Text(if (risk) "💣 RİSK x2 AÇIK" else "💣 RİSK x2", fontSize = 9.sp) },
                            modifier = Modifier.weight(1f).height(40.dp),
                        )
                        OutlinedButton(
                            onClick = {
                                val r = BilBakalimCompetitionEngine.jokerHint(q.answer)
                                hint = "${prettyEstimate(r.first)} – ${prettyEstimate(r.second)}"
                                jokerUsed = true
                            },
                            enabled = !jokerUsed,
                            modifier = Modifier.weight(1f).height(40.dp),
                        ) {
                            Text(if (jokerUsed) "KULLANILDI" else "💡 JOKER", fontSize = 9.sp)
                        }
                    }
                }

                ExcitementPhase.RESULT -> Column(
                    Modifier.fillMaxSize().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("DOĞRU CEVAP", color = BilMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(q.displayAnswer, color = BilText, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = BilCyan.copy(alpha = .08f)) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SENİN CEVABIN", color = BilCyan, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                Text(lastPlayerAnswer?.let(::prettyEstimate) ?: "—", color = BilText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = BilRed.copy(alpha = .07f)) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("RAKİBİN CEVABI", color = BilRed, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                Text(lastRivalAnswer?.let(::prettyEstimate) ?: "—", color = BilText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(roundMessage, color = if (roundMessage.startsWith("KAZANDIN")) BilGreen else BilRed, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    lastTitle?.let { Text("🎖 $it", color = BilCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    Text("Sen $playerScore • $rivalScore Rakip", color = BilText, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { advance() }, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                        Text(if (questionNo == 10) "MAÇ SONUCU" else "SONRAKİ SORU", fontWeight = FontWeight.Black)
                    }
                }

                ExcitementPhase.MATCH_END -> {
                    val won = playerScore >= rivalScore
                    Column(
                        Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(if (won) "🏆 MAÇI KAZANDIN" else "⚔ RÖVANŞ ZAMANI", color = BilText, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("$playerScore - $rivalScore", color = BilCyan, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        Text(BilBakalimCompetitionEngine.performanceText(playerScore, rivalScore), color = BilMuted)
                        Text("🔥 En iyi seri: $bestStreak • %$mastery ustalık", color = BilText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { resetMatch() }, modifier = Modifier.weight(1f)) { Text("RÖVANŞ", fontWeight = FontWeight.Black) }
                            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("OYUNLAR") }
                        }
                    }
                }
            }
        }

        if (phase != ExcitementPhase.MATCH_END) {
            EmbeddedNumberKeyboard(
                value = input,
                enabled = phase == ExcitementPhase.ANSWER,
                onValueChange = { next ->
                    input = next.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(12)
                },
                onSubmit = { submit(input.replace(',', '.').toDoubleOrNull()) },
            )
        }
    }
}

@Composable
private fun MetaChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = BilPanel, border = BorderStroke(1.dp, BilCyan.copy(alpha = .20f))) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = BilText, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(label, color = BilMuted, fontSize = 10.sp)
        }
    }
}

private fun prettyEstimate(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.2f", value)
