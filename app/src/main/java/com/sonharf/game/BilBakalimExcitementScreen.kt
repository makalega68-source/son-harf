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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private enum class ExcitementPhase { ANSWER, RESULT, MATCH_END }

@Composable
internal fun BilBakalimExcitementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
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
    var correctCount by remember { mutableIntStateOf(0) }
    var playedCount by remember { mutableIntStateOf(0) }
    var finalApplied by remember { mutableStateOf(false) }

    val q = deck[index]
    val questionNo = index + 1
    val league = BilBakalimCompetitionEngine.league(rating)
    val mastery = BilBakalimCompetitionEngine.categoryMastery(correctCount, playedCount)
    val isBoss = questionNo == 5
    val isFinal = questionNo == 10

    fun resetQuestion() {
        input = ""; seconds = 20; phase = ExcitementPhase.ANSWER; risk = false; hint = null; roundMessage = ""; lastTitle = null
    }

    fun resetMatch() {
        deck = bilBakalimQuestions.shuffled().take(10)
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
            rivalScore += rivalPoints
            matchStreak = 0
            roundMessage = "Rakip daha yakındı • +$rivalPoints"
        }
        phase = ExcitementPhase.RESULT
    }

    LaunchedEffect(index, phase) {
        if (phase != ExcitementPhase.ANSWER) return@LaunchedEffect
        seconds = 20
        while (seconds > 0 && phase == ExcitementPhase.ANSWER) { delay(1000); seconds -= 1 }
        if (seconds <= 0 && phase == ExcitementPhase.ANSWER) submit(null)
    }

    LaunchedEffect(phase, finalApplied) {
        if (phase != ExcitementPhase.MATCH_END || finalApplied) return@LaunchedEffect
        finalApplied = true
        val won = playerScore >= rivalScore
        if (won) { wins += 1; rating += 18 } else { losses += 1; rating = (rating - 12).coerceAtLeast(700) }
        prefs.edit().putInt("rating", rating).putInt("wins", wins).putInt("losses", losses).putInt("best_streak", bestStreak).apply()
    }

    BackHandler { onBack() }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF4FBFF), Color(0xFFEAF8FF))))
            .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ OYUNLAR") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BİL BAKALIM", color = Color(0xFF16324A), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("1v1 BİLGİ DÜELLOSU", color = SonHarfBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Text("$league\n$rating", color = Color(0xFF16324A), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MetaChip("🔥 Seri", matchStreak.toString(), Modifier.weight(1f))
            MetaChip("🧠 Ustalık", "%$mastery", Modifier.weight(1f))
            MetaChip("🏆 Skor", "$playerScore-$rivalScore", Modifier.weight(1f))
        }

        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEAF8FF), border = BorderStroke(1.dp, Color(0xFFB9E8F8))) {
            Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🏆 Günlük Arena", fontWeight = FontWeight.Black, color = Color(0xFF16324A))
                Text("Günün meydan okuması aktif", color = Color(0xFF6B8294), fontSize = 11.sp)
            }
        }

        when (phase) {
            ExcitementPhase.ANSWER -> {
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, if (isFinal) Color(0xFFE36573) else if (isBoss) Color(0xFFD09A32) else Color(0xFFB9E5F8))) {
                    Column(Modifier.fillMaxWidth().padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SORU $questionNo/10", fontWeight = FontWeight.Black, color = Color(0xFF6B8294))
                            Text("⏱ $seconds sn", fontWeight = FontWeight.Black, color = if (seconds <= 5) Color(0xFFE34D62) else Color(0xFF16324A))
                        }
                        if (isBoss) Text("👑 BOSS SORUSU • +5 TABAN BONUS", color = SonHarfBlue, fontWeight = FontWeight.Black)
                        if (isFinal) Text("⚡ FİNAL SORUSU • x2", color = Color(0xFFE34D62), fontWeight = FontWeight.Black)
                        Text(q.category.uppercase(), color = Color(0xFF2FA8DC), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(q.question, color = Color(0xFF16324A), fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        hint?.let { Text("💡 Joker ipucu: $it", color = Color(0xFF227D55), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.filter { c -> c.isDigit() || c == ',' || c == '.' || c == '-' } },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Tahminin") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit(input.replace(',', '.').toDoubleOrNull()) }),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = risk, onClick = { risk = !risk }, label = { Text(if (risk) "💣 RİSK x2 AÇIK" else "💣 RİSK x2") }, modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = {
                                    val r = BilBakalimCompetitionEngine.jokerHint(q.answer)
                                    hint = "${prettyEstimate(r.first)} – ${prettyEstimate(r.second)}"
                                    jokerUsed = true
                                },
                                enabled = !jokerUsed,
                                modifier = Modifier.weight(1f),
                            ) { Text(if (jokerUsed) "JOKER KULLANILDI" else "💡 JOKER") }
                        }
                        Button(onClick = { submit(input.replace(',', '.').toDoubleOrNull()) }, enabled = input.replace(',', '.').toDoubleOrNull() != null, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("CEVABI KİLİTLE", fontWeight = FontWeight.Black)
                        }
                        Text("Hızlı doğru cevap + seri = daha yüksek puan. Satın alınabilir güç yok.", color = Color(0xFF6B8294), fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            ExcitementPhase.RESULT -> {
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFF9EDBB8))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("DOĞRU CEVAP", color = Color(0xFF6B8294), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(q.displayAnswer, color = Color(0xFF16324A), fontSize = 27.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text(roundMessage, color = if (roundMessage.startsWith("KAZANDIN")) Color(0xFF1A9B60) else Color(0xFFE34D62), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        lastTitle?.let { Text("🎖 $it", color = SonHarfBlue, fontWeight = FontWeight.Bold) }
                        Text("Sen $playerScore • $rivalScore Rakip", color = Color(0xFF16324A), fontWeight = FontWeight.Black)
                        Button(onClick = { advance() }, modifier = Modifier.fillMaxWidth()) { Text(if (questionNo == 10) "MAÇ SONUCU" else "SONRAKİ SORU", fontWeight = FontWeight.Black) }
                    }
                }
            }
            ExcitementPhase.MATCH_END -> {
                val won = playerScore >= rivalScore
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, if (won) Color(0xFF39B978) else Color(0xFFE36573))) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (won) "🏆 MAÇI KAZANDIN" else "⚔ RÖVANŞ ZAMANI", color = Color(0xFF16324A), fontSize = 23.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text("$playerScore - $rivalScore", color = SonHarfBlue, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Text(BilBakalimCompetitionEngine.performanceText(playerScore, rivalScore), color = Color(0xFF6B8294))
                        Text("🎁 ${BilBakalimCompetitionEngine.surpriseReward(wins)}", color = Color(0xFF1A9B60), fontWeight = FontWeight.Bold)
                        Text("🔥 En iyi seri: $bestStreak • $league $rating", color = Color(0xFF16324A), fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { resetMatch() }, modifier = Modifier.weight(1f)) { Text("RÖVANŞ", fontWeight = FontWeight.Black) }
                            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("OYUNLAR") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFD6EAF4))) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color(0xFF16324A), fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(label, color = Color(0xFF6B8294), fontSize = 9.sp)
        }
    }
}

private fun prettyEstimate(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.2f", value)
