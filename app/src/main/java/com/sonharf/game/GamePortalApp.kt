package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

private enum class PortalGame { MENU, SON_HARF, BIL_BAKALIM }

private val PortalBg = Color(0xFFF5FBFF)
private val PortalCard = Color.White
private val PortalText = Color(0xFF16324A)
private val PortalMuted = Color(0xFF698296)
private val PortalBlue = Color(0xFF43B6E8)
private val PortalGold = Color(0xFFD8AC5C)
private val PortalGreen = Color(0xFF39B978)
private val PortalRed = Color(0xFFCE6470)

@Composable
fun GamePortalApp() {
    var game by remember { mutableStateOf(PortalGame.MENU) }
    BackHandler(enabled = game != PortalGame.MENU) { game = PortalGame.MENU }

    when (game) {
        PortalGame.MENU -> GamePortalMenu(
            onSonHarf = { game = PortalGame.SON_HARF },
            onBilBakalim = { game = PortalGame.BIL_BAKALIM },
        )
        PortalGame.SON_HARF -> Box(Modifier.fillMaxSize()) {
            ClassicPremiumApp()
            PortalReturnButton { game = PortalGame.MENU }
        }
        PortalGame.BIL_BAKALIM -> CompetitiveBilBakalimScreen { game = PortalGame.MENU }
    }
}

@Composable
private fun PortalReturnButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.statusBarsPadding().padding(start = 10.dp, top = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = .94f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFB9E5F8)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Games, null, tint = PortalBlue, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text("OYUNLAR", color = PortalText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun GamePortalMenu(onSonHarf: () -> Unit, onBilBakalim: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, PortalBg, Color(0xFFE8F6FF))))
            .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = PortalBlue.copy(alpha = .12f)) {
                Icon(Icons.Rounded.SportsEsports, null, tint = PortalBlue, modifier = Modifier.padding(12.dp).size(30.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("OYUN ARENASI", color = PortalText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("İki oyun, tek rekabet profili", color = PortalMuted, fontSize = 12.sp)
            }
        }

        Text("OYUNLAR", color = PortalMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp)

        GamePortalCard(
            title = "SON HARF",
            subtitle = "Canlı Kelime Düellosu",
            description = "Rakibinin kelimesinin son harfiyle yeni kelime üret. Süreyi yönet, serini koru, rating kazan ve liglerde yüksel.",
            icon = Icons.Rounded.Link,
            accent = PortalBlue,
            tags = listOf("Lig + Rating", "Galibiyet Serisi", "Rövanş", "Ezeli Rakip", "Turnuva"),
            button = "SON HARF OYNA",
            onClick = onSonHarf,
        )

        GamePortalCard(
            title = "BİL BAKALIM",
            subtitle = "Tahmin ve Bilgi Düellosu",
            description = "Sayısal bilgi sorularında doğru cevaba rakibinden daha çok yaklaş. Hız bonusu, risk soruları, seri ve rating ile zirveye çık.",
            icon = Icons.Rounded.AutoAwesome,
            accent = PortalGold,
            tags = listOf("1v1 Düello", "Hız Puanı", "Lig + Rating", "Seri", "Risk Sorusu", "Turnuva"),
            button = "BİL BAKALIM OYNA",
            onClick = onBilBakalim,
        )

        Surface(shape = RoundedCornerShape(18.dp), color = PortalCard, border = BorderStroke(1.dp, Color(0xFFB9E5F8))) {
            Column(Modifier.padding(14.dp)) {
                Text("ORTAK REKABET DÖNGÜSÜ", color = PortalText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Spacer(Modifier.height(5.dp))
                Text("Maç → Kazan → Ödül → İlerle → Rakip edin → Hedef gör → Tekrar maç", color = PortalMuted, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Pay-to-win yok. Ödüller prestij, Son Coin ve Style odaklıdır.", color = PortalGreen, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GamePortalCard(
    title: String,
    subtitle: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    tags: List<String>,
    button: String,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = PortalCard), border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = .14f)) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(12.dp).size(30.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = PortalText, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text(subtitle, color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ArrowForward, null, tint = accent)
            }
            Spacer(Modifier.height(12.dp))
            Text(description, color = PortalMuted, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            tags.chunked(3).forEach { rowTags ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowTags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = .10f)) {
                            Text(tag, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = PortalText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = if (accent == PortalGold) Color(0xFF2B1E0B) else Color.White)) {
                Text(button, fontWeight = FontWeight.Black)
            }
        }
    }
}

private enum class CompetitiveBilPhase { ANSWER, RESULT, MATCH_END }

@Composable
private fun CompetitiveBilBakalimScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("bil_bakalim_competition", 0) }
    var rating by remember { mutableIntStateOf(prefs.getInt("rating", 1000)) }
    var bestStreak by remember { mutableIntStateOf(prefs.getInt("best_streak", 0)) }
    var winStreak by remember { mutableIntStateOf(prefs.getInt("win_streak", 0)) }
    var wins by remember { mutableIntStateOf(prefs.getInt("wins", 0)) }
    var losses by remember { mutableIntStateOf(prefs.getInt("losses", 0)) }
    var rivalWins by remember { mutableIntStateOf(prefs.getInt("rival_wins", 0)) }
    var rivalLosses by remember { mutableIntStateOf(prefs.getInt("rival_losses", 0)) }

    var deck by remember { mutableStateOf(bilBakalimQuestions.shuffled().take(10)) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var playerScore by remember { mutableIntStateOf(0) }
    var rivalScore by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(20) }
    var input by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(CompetitiveBilPhase.ANSWER) }
    var resultText by remember { mutableStateOf("") }
    var riskMode by remember { mutableStateOf(false) }
    var lastSpeedBonus by remember { mutableIntStateOf(0) }
    var lastRoundWon by remember { mutableStateOf(false) }
    var correctAnswerText by remember { mutableStateOf("") }
    var finalApplied by remember { mutableStateOf(false) }

    val q = deck[questionIndex]
    val league = when {
        rating >= 1600 -> "BİLGE"
        rating >= 1400 -> "ELMAS"
        rating >= 1200 -> "ALTIN"
        rating >= 1000 -> "GÜMÜŞ"
        else -> "BRONZ"
    }

    fun nextMatch() {
        deck = bilBakalimQuestions.shuffled().take(10)
        questionIndex = 0; playerScore = 0; rivalScore = 0; seconds = 20; input = ""
        phase = CompetitiveBilPhase.ANSWER; resultText = ""; riskMode = false; lastSpeedBonus = 0; finalApplied = false
    }

    fun advance() {
        if (questionIndex >= deck.lastIndex) phase = CompetitiveBilPhase.MATCH_END
        else {
            questionIndex += 1; seconds = 20; input = ""; phase = CompetitiveBilPhase.ANSWER
            resultText = ""; riskMode = false; lastSpeedBonus = 0
        }
    }

    fun submit(raw: String?) {
        if (phase != CompetitiveBilPhase.ANSWER) return
        val playerAnswer = raw?.replace(',', '.')?.toDoubleOrNull()
        val spread = max(1.0, abs(q.answer) * Random.nextDouble(.08, .35))
        val rivalAnswer = if (q.answer == 0.0) Random.nextDouble(0.0, 4.0) else max(0.0, q.answer + if (Random.nextBoolean()) spread else -spread)
        val playerDiff = playerAnswer?.let { abs(it - q.answer) } ?: Double.POSITIVE_INFINITY
        val rivalDiff = abs(rivalAnswer - q.answer)
        lastRoundWon = playerDiff <= rivalDiff
        val base = if (lastRoundWon) 10 else 0
        lastSpeedBonus = if (lastRoundWon) (seconds / 4).coerceIn(0, 5) else 0
        val multiplier = if (riskMode) 2 else 1
        val gained = (base + lastSpeedBonus) * multiplier
        if (lastRoundWon) playerScore += gained else rivalScore += if (riskMode) 20 else 10
        correctAnswerText = q.displayAnswer
        resultText = if (lastRoundWon) "Turu kazandın! +$gained" else "Rakip daha yakındı."
        phase = CompetitiveBilPhase.RESULT
    }

    LaunchedEffect(questionIndex, phase) {
        if (phase != CompetitiveBilPhase.ANSWER) return@LaunchedEffect
        seconds = 20
        while (seconds > 0 && phase == CompetitiveBilPhase.ANSWER) { delay(1000); seconds -= 1 }
        if (seconds == 0 && phase == CompetitiveBilPhase.ANSWER) submit(null)
    }

    LaunchedEffect(phase, finalApplied) {
        if (phase != CompetitiveBilPhase.MATCH_END || finalApplied) return@LaunchedEffect
        finalApplied = true
        val won = playerScore >= rivalScore
        if (won) {
            wins += 1; rivalWins += 1; winStreak += 1; bestStreak = max(bestStreak, winStreak); rating += 18
        } else {
            losses += 1; rivalLosses += 1; winStreak = 0; rating = (rating - 12).coerceAtLeast(700)
        }
        prefs.edit().putInt("rating", rating).putInt("best_streak", bestStreak).putInt("win_streak", winStreak)
            .putInt("wins", wins).putInt("losses", losses).putInt("rival_wins", rivalWins).putInt("rival_losses", rivalLosses).apply()
    }

    BackHandler { onBack() }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, PortalBg, Color(0xFFE8F6FF))))
            .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = PortalText) }
            Column(Modifier.weight(1f)) {
                Text("BİL BAKALIM", color = PortalText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Rekabet Arenası", color = PortalGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = PortalGold.copy(alpha = .14f)) {
                Text("$league  •  $rating", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = PortalText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MetaStat("🔥", "Seri", "$winStreak", Modifier.weight(1f))
            MetaStat("🏆", "En İyi", "$bestStreak", Modifier.weight(1f))
            MetaStat("⚔️", "Rakiplik", "$rivalWins-$rivalLosses", Modifier.weight(1f))
            MetaStat("🎯", "Günlük", "10 Soru", Modifier.weight(1f))
        }

        Surface(shape = RoundedCornerShape(16.dp), color = PortalCard, border = BorderStroke(1.dp, Color(0xFFE5D2A9))) {
            Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("CANLI RATING HEDEFİ", color = PortalMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("Bir sonraki lige ${(nextLeagueTarget(rating) - rating).coerceAtLeast(0)} puan", color = PortalText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Turnuva • Günlük", color = PortalGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        when (phase) {
            CompetitiveBilPhase.ANSWER -> {
                Surface(shape = RoundedCornerShape(22.dp), color = PortalCard, border = BorderStroke(1.dp, PortalGold.copy(alpha = .55f))) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SORU ${questionIndex + 1}/10", color = PortalMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("⏱ $seconds sn", color = if (seconds <= 5) PortalRed else PortalText, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(q.category.uppercase(), color = PortalGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text(q.question, color = PortalText, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(15.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' || ch == '-' } },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tahminin") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit(input) }),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = riskMode, onClick = { riskMode = !riskMode }, label = { Text(if (riskMode) "💣 RİSK x2 AÇIK" else "💣 RİSK SORUSU x2", fontSize = 10.sp) }, modifier = Modifier.weight(1f))
                            Button(onClick = { submit(input) }, enabled = input.isNotBlank(), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PortalGold, contentColor = Color(0xFF2B1E0B))) {
                                Text("KİLİTLE", fontWeight = FontWeight.Black)
                            }
                        }
                        Text("Hızlı doğru tahmin ekstra puan verir.", color = PortalMuted, fontSize = 9.sp)
                    }
                }
            }
            CompetitiveBilPhase.RESULT -> {
                Surface(shape = RoundedCornerShape(22.dp), color = PortalCard, border = BorderStroke(1.dp, if (lastRoundWon) PortalGreen else PortalRed)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (lastRoundWon) Icons.Rounded.EmojiEvents else Icons.Rounded.Close, null, tint = if (lastRoundWon) PortalGreen else PortalRed, modifier = Modifier.size(48.dp))
                        Text(resultText, color = PortalText, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text("Doğru cevap: $correctAnswerText", color = PortalMuted, fontSize = 12.sp)
                        if (lastRoundWon && lastSpeedBonus > 0) Text("⚡ Hız bonusu +$lastSpeedBonus", color = PortalGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("Sen $playerScore  •  $rivalScore Rakip", color = PortalText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { advance() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PortalBlue)) {
                            Text(if (questionIndex == deck.lastIndex) "MAÇ SONUCUNU GÖR" else "SONRAKİ SORU", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            CompetitiveBilPhase.MATCH_END -> {
                val won = playerScore >= rivalScore
                Surface(shape = RoundedCornerShape(24.dp), color = PortalCard, border = BorderStroke(1.dp, if (won) PortalGreen else PortalRed)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = if (won) PortalGold else PortalMuted, modifier = Modifier.size(60.dp))
                        Text(if (won) "MAÇI KAZANDIN!" else "BU KEZ RAKİP KAZANDI", color = PortalText, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text("$playerScore - $rivalScore", color = PortalGold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Text(if (won) "+18 Rating • Seri $winStreak" else "-12 Rating • Rövanş zamanı", color = if (won) PortalGreen else PortalRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { nextMatch() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PortalGold, contentColor = Color(0xFF2B1E0B))) {
                                Text("RÖVANŞ", fontWeight = FontWeight.Black)
                            }
                            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("OYUNLAR") }
                        }
                    }
                }
            }
        }
    }
}

private fun nextLeagueTarget(rating: Int): Int = when {
    rating < 1000 -> 1000
    rating < 1200 -> 1200
    rating < 1400 -> 1400
    rating < 1600 -> 1600
    else -> rating
}

@Composable
private fun MetaStat(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = PortalCard, border = BorderStroke(1.dp, Color(0xFFD6EAF4))) {
        Column(Modifier.padding(vertical = 9.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 16.sp)
            Text(value, color = PortalText, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(label, color = PortalMuted, fontSize = 7.5.sp, textAlign = TextAlign.Center)
        }
    }
}
