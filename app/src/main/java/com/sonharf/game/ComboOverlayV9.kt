package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
private data class ActionRoomV9(
    val id: String,
    val code: String = "",
    @SerialName("host_id") val hostId: String,
    @SerialName("guest_id") val guestId: String? = null,
    val status: String,
    @SerialName("is_bot") val isBot: Boolean = false,
    @SerialName("bot_name") val botName: String? = null,
    @SerialName("action_seq") val actionSeq: Long = 0,
    @SerialName("last_action_streak") val lastActionStreak: Int? = null,
    @SerialName("last_action_bonus") val lastActionBonus: Int? = null,
    @SerialName("last_action_player_id") val lastActionPlayerId: String? = null,
    @SerialName("last_action_is_bot") val lastActionIsBot: Boolean = false,
    @SerialName("host_score") val hostScore: Int = 0,
    @SerialName("guest_score") val guestScore: Int = 0,
    @SerialName("host_rounds") val hostRounds: Int = 0,
    @SerialName("guest_rounds") val guestRounds: Int = 0,
    @SerialName("winner_id") val winnerId: String? = null,
    val language: String = "tr",
)

private data class ComboV9(val title: String, val color: Color)
private fun comboV9(n: Int): ComboV9? = when (n) {
    3 -> ComboV9(sh("İSABET!", "NICE!"), SonHarfCyan)
    4 -> ComboV9(sh("AFERİN!", "WELL DONE!"), SonHarfGreen)
    5 -> ComboV9(sh("MÜKEMMEL!", "PERFECT!"), SonHarfGold)
    6 -> ComboV9(sh("KELİME KATİLİ!", "STREAK MASTER!"), SonHarfPink)
    7 -> ComboV9(sh("EFSANE!", "LEGENDARY!"), SonHarfPurple)
    8 -> ComboV9(sh("HARİKASIN!", "AMAZING!"), SonHarfCyan)
    9 -> ComboV9(sh("ŞOV ZAMANI!", "SHOWTIME!"), SonHarfGold)
    else -> if (n >= 10) ComboV9(sh("EFSANELER LİGİ!", "LEAGUE OF LEGENDS!"), SonHarfPink) else null
}

private data class ConfettiPiece(val x: Float, val delay: Float, val speed: Float, val size: Float, val angle: Float, val color: Color)

@Composable
fun ComboOverlayV9() {
    if (!SupabaseProvider.configured) return
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var combo by remember { mutableStateOf<ComboV9?>(null) }
    var streak by remember { mutableIntStateOf(0) }
    var bonus by remember { mutableIntStateOf(0) }
    var actor by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var activeRoom by remember { mutableStateOf<ActionRoomV9?>(null) }
    var finishedRoom by remember { mutableStateOf<ActionRoomV9?>(null) }
    var dismissedFinished by remember { mutableStateOf(SonHarfPreferences.dismissedMatchSummaryId(context)) }
    var resultWords by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var reaction by remember { mutableStateOf<String?>(null) }
    var reactionKey by remember { mutableStateOf<Long?>(null) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var selectedMeaning by remember { mutableStateOf<String?>(null) }
    val progress = remember { Animatable(1f) }
    val pieces = remember {
        val colors = listOf(SonHarfPink, SonHarfCyan, SonHarfGold, SonHarfGreen, SonHarfPurple, Color(0xFFFF6B35))
        List(64) { ConfettiPiece(Random.nextFloat(), Random.nextFloat() * .20f, .75f + Random.nextFloat() * .70f, 5f + Random.nextFloat() * 8f, Random.nextFloat() * 180f, colors[it % colors.size]) }
    }

    fun dismissSummary(roomId: String) {
        dismissedFinished = roomId
        finishedRoom = null
        SonHarfPreferences.setDismissedMatchSummaryId(context, roomId)
    }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()
            if (me != null) {
                val rooms = runCatching {
                    SupabaseProvider.client.from("game_rooms").select().decodeList<ActionRoomV9>()
                        .filter { it.hostId == me || it.guestId == me }
                }.getOrDefault(emptyList())
                val current = rooms.filter { it.status in listOf("playing","quiz","final","sudden_death","paused") }.maxByOrNull { it.actionSeq }
                activeRoom = current
                if (current != null) {
                    val key = current.id to current.actionSeq
                    val c = comboV9(current.lastActionStreak ?: 0)
                    if (c != null && current.actionSeq > 0 && key != shown) {
                        shown = key; combo = c; streak = current.lastActionStreak ?: 0; bonus = current.lastActionBonus ?: 0
                        actor = when { current.lastActionIsBot -> current.botName ?: "BOT"; current.lastActionPlayerId == me -> sh("SEN","YOU"); else -> sh("RAKİP","OPPONENT") }
                        SonHarfSoundFx.fireworks(); progress.snapTo(0f); progress.animateTo(1f, tween(1050)); combo = null
                    }
                    if (!current.isBot) {
                        val chat = runCatching { backend.getChat(current.id) }.getOrDefault(emptyList())
                        val last = chat.lastOrNull { it.body in setOf("🔥","👏","😎","⚡","💎","😂") }
                        if (last != null && last.id != reactionKey) { reactionKey = last.id; reaction = last.body }
                    }
                }
                val latestFinished = rooms.filter { it.status == "finished" }.maxByOrNull { it.actionSeq }
                val fin = latestFinished?.takeIf { it.id != dismissedFinished }
                if (fin != null && finishedRoom?.id != fin.id) {
                    finishedRoom = fin
                    resultWords = runCatching { backend.getWords(fin.id) }.getOrDefault(emptyList())
                    growth = runCatching { backend.getGrowthDashboard() }.getOrNull()
                    runCatching { backend.logEvent("match_finished_seen", fin.id) }
                }
            }
            delay(500)
        }
    }
    LaunchedEffect(reactionKey) { if (reaction != null) { delay(1400); reaction = null } }

    val me = backend.currentUserId()
    val c = combo
    if (c != null) {
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Canvas(Modifier.fillMaxSize()) {
                val p = progress.value
                pieces.forEach { piece ->
                    val local = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
                    if (local > 0f && local < 1f) {
                        val x = size.width * piece.x + kotlin.math.sin(local * 9f + piece.x * 12f) * 22f
                        val y = size.height * (.08f + local * piece.speed * .82f)
                        rotate(piece.angle + local * 280f, pivot = Offset(x, y)) {
                            drawRect(piece.color.copy(alpha = (1f - local * .65f).coerceAtLeast(.2f)), topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(piece.size, piece.size * 1.8f))
                        }
                    }
                }
            }
            Column(Modifier.padding(top = 86.dp, start = 20.dp, end = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(actor, color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(c.title, color = c.color, fontSize = 29.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("$streak ${sh("DOĞRU SERİ", "WORD STREAK")}" + if (bonus > 0) "  •  +$bonus" else "", color = SonHarfText, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    val active = activeRoom
    if (active != null && !active.isBot && active.status in listOf("playing","final","sudden_death")) {
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(top=8.dp,end=8.dp),contentAlignment=Alignment.TopEnd) {
            Surface(shape=RoundedCornerShape(18.dp),color=SonHarfSurface.copy(alpha=.90f),shadowElevation=3.dp) {
                Row(Modifier.padding(horizontal=5.dp,vertical=2.dp)) {
                    listOf("🔥","👏","😎","⚡").forEach { e -> TextButton(onClick={scope.launch{runCatching{backend.sendChat(active.id,e)};runCatching{backend.logEvent("quick_reaction",e)}}},contentPadding=PaddingValues(4.dp)){Text(e,fontSize=17.sp)} }
                }
            }
        }
    }
    reaction?.let { r ->
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(top=62.dp),contentAlignment=Alignment.TopCenter) {
            Surface(shape=RoundedCornerShape(22.dp),color=SonHarfPurple.copy(alpha=.92f)) { Text(r,Modifier.padding(horizontal=18.dp,vertical=8.dp),fontSize=28.sp) }
        }
    }

    val fin = finishedRoom
    if (fin != null && fin.id != dismissedFinished) {
        val host = me == fin.hostId
        val myScore = if(host) fin.hostScore else fin.guestScore
        val oppScore = if(host) fin.guestScore else fin.hostScore
        val myRounds = if(host) fin.hostRounds else fin.guestRounds
        val oppRounds = if(host) fin.guestRounds else fin.hostRounds
        val myWords = resultWords.count { it.playerId == me }
        val longest = resultWords.filter { it.playerId == me }.maxByOrNull { it.word.length }?.word?.uppercase() ?: "—"
        val won = fin.winnerId == me
        Box(Modifier.fillMaxSize().background(SonHarfBg).statusBarsPadding().navigationBarsPadding().padding(14.dp),contentAlignment=Alignment.Center) {
            Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(26.dp),border=BorderStroke(2.dp,if(won)SonHarfGold.copy(alpha=.75f) else SonHarfCyan.copy(alpha=.45f)),elevation=CardDefaults.cardElevation(defaultElevation=8.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Column { Text(if(won)"🏆 ${sh("ZAFER ÖZETİ","VICTORY SUMMARY")}" else "📊 ${sh("MAÇ ÖZETİ","MATCH SUMMARY")}",fontWeight=FontWeight.Black,fontSize=23.sp,color=SonHarfText);Text("$myRounds - $oppRounds  •  $myScore - $oppScore",color=SonHarfText,fontSize=14.sp,fontWeight=FontWeight.Bold) }
                        IconButton(onClick={dismissSummary(fin.id)},modifier=Modifier.size(48.dp)){Text("×",fontSize=28.sp,fontWeight=FontWeight.Black,color=SonHarfPurple)}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        SummaryMetric("🧠",myWords.toString(),sh("Kelime","Words"),Modifier.weight(1f));SummaryMetric("📏",longest,sh("En uzun","Longest"),Modifier.weight(1f));SummaryMetric("🔥",(growth?.currentWinStreak?:0).toString(),sh("Seri","Streak"),Modifier.weight(1f))
                    }
                    Text(sh("KELİMELER • ANLAM İÇİN DOKUN", "WORDS • TAP FOR MEANING"), color=SonHarfMuted, fontSize=9.sp, fontWeight=FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max=130.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        items(resultWords) { w ->
                            Surface(Modifier.fillMaxWidth().clickable { selectedWord=w.word; selectedMeaning=null; scope.launch { selectedMeaning=WordMeaningRuntime.meaning(w.word, fin.language) } }, shape=RoundedCornerShape(10.dp), color=SonHarfSurface2) {
                                Text(w.word.uppercase(), Modifier.padding(horizontal=10.dp,vertical=7.dp), fontWeight=FontWeight.Bold, fontSize=12.sp)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick={dismissSummary(fin.id); SonHarfUiState.homeRequest += 1},modifier=Modifier.weight(1f)){Text("← ${sh("GERİ","BACK")}",fontSize=12.sp,fontWeight=FontWeight.Black)}
                        Button(onClick={scope.launch { runCatching { if(fin.isBot) backend.restartBotMatch(fin.id) else backend.requestRematch(fin.id) }; dismissSummary(fin.id) }},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=SonHarfCyan)){Text(sh("RÖVANŞ","REMATCH"),fontWeight=FontWeight.Black,fontSize=13.sp,color=Color.White)}
                    }
                }
            }
        }
    }
    selectedWord?.let { word ->
        AlertDialog(
            onDismissRequest={selectedWord=null;selectedMeaning=null},
            title={Text("${word.uppercase()} • ${sh("ANLAMI","MEANING")}",fontWeight=FontWeight.Black,color=SonHarfCyan)},
            text={if(selectedMeaning==null) CircularProgressIndicator() else Text(selectedMeaning!!)},
            confirmButton={TextButton(onClick={selectedWord=null;selectedMeaning=null}){Text(sh("KAPAT","CLOSE"))}}
        )
    }
}

@Composable private fun SummaryMetric(icon:String,value:String,label:String,modifier:Modifier){
    Surface(modifier=modifier,shape=RoundedCornerShape(15.dp),color=SonHarfSurface2,border=BorderStroke(1.dp,SonHarfCyan.copy(alpha=.24f))){Column(Modifier.padding(vertical=12.dp,horizontal=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon,fontSize=20.sp);Text(value,maxLines=1,fontWeight=FontWeight.Black,fontSize=15.sp,color=SonHarfText);Text(label,color=SonHarfMuted,fontSize=9.sp,fontWeight=FontWeight.Bold)}}
}
