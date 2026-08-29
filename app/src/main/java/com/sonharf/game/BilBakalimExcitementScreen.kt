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
    val focus = LocalFocusManager.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val answerFocusRequester = remember { FocusRequester() }
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    val prefs = remember { context.getSharedPreferences("bil_bakalim_excitement", 0) }
    var rating by remember { mutableIntStateOf(prefs.getInt("rating", 1000)) }
    var wins by remember { mutableIntStateOf(prefs.getInt("wins", 0)) }
    var losses by remember { mutableIntStateOf(prefs.getInt("losses", 0)) }
    var bestStreak by remember { mutableIntStateOf(prefs.getInt("best_streak", 0)) }
    var matchStreak by remember { mutableIntStateOf(0) }
    val questionPool = if (SonHarfUiState.isEnglish) bilBakalimQuestionsEn else bilBakalimQuestions
    var deck by remember(SonHarfUiState.language) { mutableStateOf(questionPool.shuffled().take(10)) }
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
    var correctCount by remember { mutableIntStateOf(0) }
    var playedCount by remember { mutableIntStateOf(0) }
    var finalApplied by remember { mutableStateOf(false) }

    val q = deck[index]
    val questionNo = index + 1
    val league = when (BilBakalimCompetitionEngine.league(rating)) {
        "BİLGE" -> sh("BİLGE", "SAGE")
        "ELMAS" -> sh("ELMAS", "DIAMOND")
        "ALTIN" -> sh("ALTIN", "GOLD")
        "GÜMÜŞ" -> sh("GÜMÜŞ", "SILVER")
        else -> sh("BRONZ", "BRONZE")
    }
    fun localizedTitle(title: String?): String? = when (title) {
        "Final Ustası" -> sh("Final Ustası", "Final Master")
        "Boss Avcısı" -> sh("Boss Avcısı", "Boss Hunter")
        "Hız Ustası" -> sh("Hız Ustası", "Speed Master")
        else -> title
    }
    fun localizedPerformance(): String = when (BilBakalimCompetitionEngine.performanceText(playerScore, rivalScore)) {
        "Baskın galibiyet" -> sh("Baskın galibiyet", "Dominant win")
        "Net galibiyet" -> sh("Net galibiyet", "Clear win")
        "Kıl payı galibiyet" -> sh("Kıl payı galibiyet", "Narrow win")
        "Başa baş" -> sh("Başa baş", "Neck and neck")
        "Kıl payı mağlubiyet" -> sh("Kıl payı mağlubiyet", "Narrow loss")
        else -> sh("Rövanş zamanı", "Time for a rematch")
    }
    fun localizedReward(): String = when (BilBakalimCompetitionEngine.surpriseReward(wins)) {
        "Style Sandığı" -> sh("Style Sandığı", "Style Chest")
        "Prestij Unvanı" -> sh("Prestij Unvanı", "Prestige Title")
        "Son Coin Sandığı" -> sh("Son Coin Sandığı", "Son Coin Chest")
        else -> "XP"
    }
    val mastery = BilBakalimCompetitionEngine.categoryMastery(correctCount, playedCount)
    val isBoss = questionNo == 5
    val isFinal = questionNo == 10

    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        val me = b.currentUserId() ?: return@LaunchedEffect
        playerProfile = runCatching { b.getProfile(me) }.getOrNull()
    }

    fun resetQuestion() {
        input = ""; seconds = 20; phase = ExcitementPhase.ANSWER; risk = false; hint = null; roundMessage = ""; lastTitle = null
    }

    fun resetMatch() {
        deck = (if (SonHarfUiState.isEnglish) bilBakalimQuestionsEn else bilBakalimQuestions).shuffled().take(10)
        index = 0; playerScore = 0; rivalScore = 0; matchStreak = 0; correctCount = 0; playedCount = 0; jokerUsed = false; finalApplied = false
        resetQuestion()
    }

    fun advance() {
        if (index >= deck.lastIndex) phase = ExcitementPhase.MATCH_END else { index += 1; resetQuestion() }
    }

    fun submit(answer: Double?) {
        if (phase != ExcitementPhase.ANSWER) return
        focus.clearFocus()
        val spread = max(1.0, abs(q.answer) * Random.nextDouble(.08, .38))
        val rivalAnswer = if (q.answer == 0.0) Random.nextDouble(0.0, 4.0) else max(0.0, q.answer + if (Random.nextBoolean()) spread else -spread)
        val rules = BilRoundRules(questionNo = questionNo, secondsLeft = seconds, riskEnabled = risk, streak = matchStreak)
        val outcome = BilBakalimCompetitionEngine.resolve(answer, rivalAnswer, q.answer, rules)
        val roundMultiplier = rules.multiplier
        val rivalPoints = (10 + if (isBoss) 5 else 0) * roundMultiplier
        playedCount += 1
        if (outcome.won) {
            SonHarfSoundFx.wordAccepted()
            playerScore += outcome.points
            matchStreak += 1
            bestStreak = max(bestStreak, matchStreak)
            correctCount += 1
            lastTitle = localizedTitle(outcome.title)
            roundMessage = buildString {
                append(sh("KAZANDIN • +${outcome.points}", "YOU WON • +${outcome.points}"))
                if (outcome.speedBonus > 0) append(sh(" • Hız +${outcome.speedBonus}", " • Speed +${outcome.speedBonus}"))
                if (outcome.streakBonus > 0) append(sh(" • Seri +${outcome.streakBonus}", " • Streak +${outcome.streakBonus}"))
            }
        } else {
            SonHarfSoundFx.warning()
            rivalScore += rivalPoints
            matchStreak = 0
            roundMessage = sh("Rakip daha yakındı • +$rivalPoints", "Rival was closer • +$rivalPoints")
        }
        phase = ExcitementPhase.RESULT
    }

    LaunchedEffect(index, phase) {
        if (phase != ExcitementPhase.ANSWER) return@LaunchedEffect
        delay(100)
        runCatching { answerFocusRequester.requestFocus() }
        softwareKeyboard?.show()
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
        if (won) { wins += 1; rating += 18 } else { losses += 1; rating = (rating - 12).coerceAtLeast(700) }
        prefs.edit().putInt("rating", rating).putInt("wins", wins).putInt("losses", losses).putInt("best_streak", bestStreak).apply()
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
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, BilBg, Color(0xFFF1F6FC))))
            .statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(sh("‹ OYUNLAR", "‹ GAMES"), color = BilCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(sh("BİL BAKALIM", "TRIVIA DUEL"), color = BilText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text(
                    sh("BİLGİ YARIŞMASI • EN YAKIN TAHMİN", "TRIVIA • CLOSEST GUESS"),
                    color = BilCyan,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BilPanel2,
                border = BorderStroke(1.dp, BilCyan.copy(alpha = .28f)),
            ) {
                Column(
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(sh("BİL LİGİ", "TRIVIA LEAGUE"), color = BilMuted, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(league, color = BilText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("$rating", color = BilCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
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

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MetaChip("🔥 Seri", matchStreak.toString(), Modifier.weight(1f))
            MetaChip(sh("🧠 Ustalık", "🧠 Mastery"), "%$mastery", Modifier.weight(1f))
            MetaChip("🏆 Skor", "$playerScore-$rivalScore", Modifier.weight(1f))
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BilPanel2,
            border = BorderStroke(1.dp, BilCyan.copy(alpha = .24f)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    sh("✦ Günlük Meydan Okuma", "✦ Daily Challenge"),
                    fontWeight = FontWeight.Black,
                    color = BilText,
                    fontSize = 16.sp,
                )
                Text(
                    sh("Günün meydan okuması aktif.", "Today's challenge is active."),
                    color = BilMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        when (phase) {
            ExcitementPhase.ANSWER -> {
                Surface(shape = RoundedCornerShape(22.dp), color = BilPanel, border = BorderStroke(1.dp, if (isFinal) BilRed else if (isBoss) BilGold else BilCyan.copy(alpha = .30f))) {
                    Column(Modifier.fillMaxWidth().padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sh("SORU $questionNo/10", "QUESTION $questionNo/10"), fontWeight = FontWeight.Black, color = BilMuted, fontSize = 16.sp)
                            Text(sh("⏱ $seconds sn", "⏱ ${seconds}s"), fontWeight = FontWeight.Black, color = if (seconds <= 5) BilRed else BilText, fontSize = 16.sp)
                        }
                        if (isBoss) Text(sh("👑 BOSS SORUSU • +5 TABAN BONUS", "👑 BOSS QUESTION • +5 BASE BONUS"), color = BilCyan, fontWeight = FontWeight.Black)
                        if (isFinal) Text(sh("⚡ FİNAL SORUSU • x2", "⚡ FINAL QUESTION • x2"), color = BilRed, fontWeight = FontWeight.Black)
                        Text(q.category.uppercase(), color = BilCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(q.question, color = BilText, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        hint?.let { Text(sh("💡 Joker ipucu: $it", "💡 Hint: $it"), color = BilGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                val next = it.filter { c -> c.isDigit() || c == ',' || c == '.' || c == '-' }
                                if (next.length > input.length) SonHarfSoundFx.typingClick()
                                input = next
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).focusRequester(answerFocusRequester),
                            singleLine = true,
                            label = { Text(sh("Tahminin", "Your guess"), fontSize = 12.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                            shape = RoundedCornerShape(15.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit(input.replace(',', '.').toDoubleOrNull()) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BilCyan,
                                unfocusedBorderColor = Color(0xFFD4DEE9),
                                cursorColor = BilCyan,
                            ),
                        )
                        Button(
                            onClick = { submit(input.replace(',', '.').toDoubleOrNull()) },
                            enabled = input.replace(',', '.').toDoubleOrNull() != null,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BilCyan,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE2E7EE),
                                disabledContentColor = Color(0xFF8A94A3),
                            ),
                        ) {
                            Text(sh("CEVABI KİLİTLE", "LOCK ANSWER"), fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = risk,
                                onClick = { risk = !risk },
                                label = { Text(if (risk) sh("💣 RİSK x2 AÇIK", "💣 RISK x2 ON") else sh("💣 RİSK x2", "💣 RISK x2"), fontSize = 12.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            )
                            OutlinedButton(
                                onClick = {
                                    val r = BilBakalimCompetitionEngine.jokerHint(q.answer)
                                    hint = "${prettyEstimate(r.first)} – ${prettyEstimate(r.second)}"
                                    jokerUsed = true
                                },
                                enabled = !jokerUsed,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(if (jokerUsed) "KULLANILDI" else "💡 JOKER", fontSize = 12.sp, maxLines = 1) }
                        }
                        Text(sh("Hızlı doğru cevap + seri = daha yüksek puan. Satın alınabilir güç yok.", "Fast correct answers + streaks = higher score. No purchasable power."), color = BilMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            ExcitementPhase.RESULT -> {
                Surface(shape = RoundedCornerShape(22.dp), color = BilPanel, border = BorderStroke(1.dp, BilGreen.copy(alpha = .45f))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(sh("DOĞRU CEVAP", "CORRECT ANSWER"), color = BilMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(q.displayAnswer, color = BilText, fontSize = 27.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text(roundMessage, color = if (roundMessage.startsWith("KAZANDIN")) BilGreen else BilRed, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        lastTitle?.let { Text("🎖 $it", color = BilCyan, fontWeight = FontWeight.Bold) }
                        Text(sh("Sen $playerScore • $rivalScore Rakip", "You $playerScore • $rivalScore Rival"), color = BilText, fontWeight = FontWeight.Black)
                        Button(onClick = { advance() }, modifier = Modifier.fillMaxWidth()) { Text(if (questionNo == 10) sh("MAÇ SONUCU", "MATCH RESULT") else sh("SONRAKİ SORU", "NEXT QUESTION"), fontWeight = FontWeight.Black) }
                    }
                }
            }
            ExcitementPhase.MATCH_END -> {
                val won = playerScore >= rivalScore
                Surface(shape = RoundedCornerShape(24.dp), color = BilPanel, border = BorderStroke(1.dp, if (won) BilGreen else BilRed)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (won) sh("🏆 MAÇI KAZANDIN", "🏆 YOU WON THE MATCH") else sh("⚔ RÖVANŞ ZAMANI", "⚔ REMATCH TIME"), color = BilText, fontSize = 23.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text("$playerScore - $rivalScore", color = BilCyan, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Text(localizedPerformance(), color = BilMuted)
                        Text("🎁 ${localizedReward()}", color = BilGreen, fontWeight = FontWeight.Bold)
                        Text(sh("🔥 En iyi seri: $bestStreak • $league $rating", "🔥 Best streak: $bestStreak • $league $rating"), color = BilText, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { resetMatch() }, modifier = Modifier.weight(1f)) { Text(sh("RÖVANŞ", "REMATCH"), fontWeight = FontWeight.Black) }
                            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text(sh("OYUNLAR", "GAMES")) }
                        }
                    }
                }
            }
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
