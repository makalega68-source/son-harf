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
internal val PortalBg = Color(0xFFF5FBFF)
internal val PortalCard = Color.White
internal val PortalText = Color(0xFF16324A)
internal val PortalMuted = Color(0xFF698296)
internal val PortalBlue = Color(0xFF43B6E8)
internal val PortalGold = Color(0xFFD8AC5C)
internal val PortalGreen = Color(0xFF39B978)
internal val PortalRed = Color(0xFFCE6470)

@Composable
fun GamePortalApp() {
    var game by remember { mutableStateOf(PortalGame.MENU) }
    BackHandler(enabled = game != PortalGame.MENU) { game = PortalGame.MENU }
    when (game) {
        PortalGame.MENU -> Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, PortalBg, Color(0xFFE8F6FF))))
                .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            HomeLobby(
                onQuickPlay = { game = PortalGame.SON_HARF },
                onSonHarf = { game = PortalGame.SON_HARF },
                onBilBakalim = { game = PortalGame.BIL_BAKALIM },
            )
        }
        PortalGame.SON_HARF -> ClassicPremiumApp()
        PortalGame.BIL_BAKALIM -> CompetitiveBilBakalimScreen { game = PortalGame.MENU }
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
    val league = BilBakalimCompetitionEngine.league(rating)

    fun nextMatch() {
        deck = bilBakalimQuestions.shuffled().take(10); questionIndex = 0; playerScore = 0; rivalScore = 0; seconds = 20; input = ""
        phase = CompetitiveBilPhase.ANSWER; resultText = ""; riskMode = false; lastSpeedBonus = 0; finalApplied = false
    }
    fun advance() {
        if (questionIndex >= deck.lastIndex) phase = CompetitiveBilPhase.MATCH_END else {
            questionIndex += 1; seconds = 20; input = ""; phase = CompetitiveBilPhase.ANSWER; resultText = ""; riskMode = false; lastSpeedBonus = 0
        }
    }
    fun submit(raw: String?) {
        if (phase != CompetitiveBilPhase.ANSWER) return
        val playerAnswer = raw?.replace(',', '.')?.toDoubleOrNull()
        val spread = max(1.0, abs(q.answer) * Random.nextDouble(.08, .35))
        val rivalAnswer = if (q.answer == 0.0) Random.nextDouble(0.0, 4.0) else max(0.0, q.answer + if (Random.nextBoolean()) spread else -spread)
        val outcome = BilBakalimCompetitionEngine.resolve(playerAnswer, rivalAnswer, q.answer, BilRoundRules(questionIndex + 1, seconds, riskMode, winStreak))
        lastRoundWon = outcome.won; lastSpeedBonus = outcome.speedBonus
        if (outcome.won) playerScore += outcome.points else rivalScore += if (riskMode) 20 else 10
        correctAnswerText = q.displayAnswer
        resultText = if (outcome.won) "Turu kazandın! +${outcome.points}" else "Rakip daha yakındı."
        phase = CompetitiveBilPhase.RESULT
    }
    LaunchedEffect(questionIndex, phase) {
        if (phase != CompetitiveBilPhase.ANSWER) return@LaunchedEffect
        seconds = 20; while (seconds > 0 && phase == CompetitiveBilPhase.ANSWER) { delay(1000); seconds -= 1 }
        if (seconds == 0 && phase == CompetitiveBilPhase.ANSWER) submit(null)
    }
    LaunchedEffect(phase, finalApplied) {
        if (phase != CompetitiveBilPhase.MATCH_END || finalApplied) return@LaunchedEffect
        finalApplied = true; val won = playerScore >= rivalScore
        if (won) { wins += 1; rivalWins += 1; winStreak += 1; bestStreak = max(bestStreak, winStreak); rating += 18 }
        else { losses += 1; rivalLosses += 1; winStreak = 0; rating = (rating - 12).coerceAtLeast(700) }
        prefs.edit().putInt("rating", rating).putInt("best_streak", bestStreak).putInt("win_streak", winStreak).putInt("wins", wins).putInt("losses", losses).putInt("rival_wins", rivalWins).putInt("rival_losses", rivalLosses).apply()
    }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, PortalBg, Color(0xFFE8F6FF)))).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = PortalText) }
            Column(Modifier.weight(1f)) { Text("BİL BAKALIM", color = PortalText, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("Rekabet Arenası", color = PortalGold, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            Text("$league • $rating", color = PortalText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MetaStat("🔥", "Seri", "$winStreak", Modifier.weight(1f)); MetaStat("⚔️", "Rakiplik", "$rivalWins-$rivalLosses", Modifier.weight(1f))
        }
        when (phase) {
            CompetitiveBilPhase.ANSWER -> Surface(shape = RoundedCornerShape(22.dp), color = PortalCard, border = BorderStroke(1.dp, PortalGold.copy(alpha=.55f))) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("SORU ${questionIndex+1}/10", color=PortalMuted); Text("⏱ $seconds sn", color=if(seconds<=5) PortalRed else PortalText, fontWeight=FontWeight.Black) }
                    if (questionIndex == 4) Text("👑 BOSS SORUSU", color=PortalGold, fontWeight=FontWeight.Black)
                    if (questionIndex == 9) Text("⚡ FİNAL • x2", color=PortalRed, fontWeight=FontWeight.Black)
                    Text(q.category.uppercase(), color=PortalGold, fontSize=10.sp, fontWeight=FontWeight.Black)
                    Spacer(Modifier.height(8.dp)); Text(q.question, color=PortalText, fontSize=19.sp, lineHeight=26.sp, fontWeight=FontWeight.Bold, textAlign=TextAlign.Center); Spacer(Modifier.height(15.dp))
                    OutlinedTextField(value=input,onValueChange={input=it.filter{ch->ch.isDigit()||ch==','||ch=='.'||ch=='-'}},modifier=Modifier.fillMaxWidth(),label={Text("Tahminin")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={submit(input)}))
                    FilterChip(selected=riskMode,onClick={riskMode=!riskMode},label={Text(if(riskMode) "💣 RİSK x2 AÇIK" else "💣 RİSK SORUSU x2")},modifier=Modifier.fillMaxWidth())
                    Button(onClick={submit(input)},enabled=input.isNotBlank(),modifier=Modifier.fillMaxWidth().height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=PortalGold,contentColor=Color(0xFF2B1E0B))){Text("TAHMİNİ KİLİTLE",fontWeight=FontWeight.Black)}
                }
            }
            CompetitiveBilPhase.RESULT -> Surface(shape=RoundedCornerShape(22.dp),color=PortalCard,border=BorderStroke(1.dp,if(lastRoundWon) PortalGreen else PortalRed)) {
                Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(if(lastRoundWon) Icons.Rounded.EmojiEvents else Icons.Rounded.Close,null,tint=if(lastRoundWon) PortalGreen else PortalRed,modifier=Modifier.size(48.dp));Text(resultText,color=PortalText,fontSize=20.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center);Text("Doğru cevap: $correctAnswerText",color=PortalMuted);if(lastSpeedBonus>0)Text("⚡ Hız bonusu +$lastSpeedBonus",color=PortalGold,fontWeight=FontWeight.Bold);Text("Sen $playerScore • $rivalScore Rakip",color=PortalText,fontWeight=FontWeight.Bold);Button(onClick={advance()},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=PortalBlue)){Text(if(questionIndex==deck.lastIndex)"MAÇ SONUCUNU GÖR" else "SONRAKİ SORU",fontWeight=FontWeight.Black)}}
            }
            CompetitiveBilPhase.MATCH_END -> { val won=playerScore>=rivalScore; Surface(shape=RoundedCornerShape(24.dp),color=PortalCard,border=BorderStroke(1.dp,if(won)PortalGreen else PortalRed)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.EmojiEvents,null,tint=if(won)PortalGold else PortalMuted,modifier=Modifier.size(60.dp));Text(if(won)"MAÇI KAZANDIN!" else "BU KEZ RAKİP KAZANDI",color=PortalText,fontSize=22.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center);Text("$playerScore - $rivalScore",color=PortalGold,fontSize=30.sp,fontWeight=FontWeight.Black);Text(BilBakalimCompetitionEngine.performanceText(playerScore,rivalScore),color=PortalMuted);Text("🎁 ${BilBakalimCompetitionEngine.surpriseReward(wins)}",color=PortalGreen,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={nextMatch()},modifier=Modifier.weight(1f)){Text("RÖVANŞ")};OutlinedButton(onClick=onBack,modifier=Modifier.weight(1f)){Text("OYUNLAR")}}}} }
        }
    }
}

@Composable
private fun MetaStat(icon:String,label:String,value:String,modifier:Modifier=Modifier){Surface(modifier,shape=RoundedCornerShape(14.dp),color=PortalCard,border=BorderStroke(1.dp,Color(0xFFD6EAF4))){Column(Modifier.padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon);Text(value,color=PortalText,fontWeight=FontWeight.Black);Text(label,color=PortalMuted,fontSize=9.sp)}}}
