package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

object SonHarfGameModeState { var mode by mutableStateOf("normal") }

@Composable
fun MetaHubScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var tab by remember { mutableIntStateOf(0) }
    val labels = listOf(sh("Kariyer","Career"), sh("Sezon","Season"), sh("Görevler","Goals"), sh("Lig","League"), sh("Oyunlarım","Games"), sh("Rehber","Guide"), sh("Ayarlar","Settings"))
    Column(Modifier.fillMaxSize()) {
        Text(sh("OYUNCU MERKEZİ","PLAYER HUB"),Modifier.padding(horizontal=16.dp,vertical=12.dp),fontSize=24.sp,fontWeight=FontWeight.Black)
        ScrollableTabRow(selectedTabIndex=tab,edgePadding=8.dp,containerColor=SonHarfSurface) {
            labels.forEachIndexed { i,s -> Tab(selected=tab==i,onClick={tab=i},text={Text(s,fontSize=10.sp)}) }
        }
        Box(Modifier.weight(1f)) {
            when(tab){
                0 -> GrowthCenterScreen()
                1 -> MetaProgressV2Screen()
                2 -> RetentionGoalsPanel(backend)
                3 -> RetentionLeaguePanel(backend)
                4 -> RetentionGamesPanel(backend)
                5 -> RetentionGuidePanel(backend)
                else -> RetentionSettingsPanel(backend)
            }
        }
    }
}

@Composable
private fun RetentionGoalsPanel(backend: OnlineGameBackend?) {
    val scope=rememberCoroutineScope(); var goals by remember{mutableStateOf<List<GoalRowDto>>(emptyList())}; var notice by remember{mutableStateOf("")}; var busy by remember{mutableStateOf<String?>(null)}
    suspend fun reload(){ goals=runCatching{backend?.getGoals().orEmpty()}.getOrDefault(emptyList()) }
    LaunchedEffect(Unit){reload();runCatching{backend?.logEvent("goals_open")}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item { HubBanner("🎯",sh("GÜNLÜK + HAFTALIK HEDEFLER","DAILY + WEEKLY GOALS"),sh("Kariyer sekmesindeki günlük meydan okuma her gün; buradaki hedefler her hafta yenilenir.","Daily challenge resets every day; these goals reset every week."),SonHarfGold) }
        items(goals,key={it.id}){g ->
            val title=if(SonHarfUiState.isEnglish)g.titleEn else g.titleTr; val desc=if(SonHarfUiState.isEnglish)g.descriptionEn else g.descriptionTr; val done=g.progress>=g.target
            Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(title,fontWeight=FontWeight.Black);Text("◈ ${g.rewardDiamonds} SC",color=SonHarfCyan,fontWeight=FontWeight.Black)}
                    Text(desc,color=SonHarfMuted,fontSize=10.sp)
                    LinearProgressIndicator(progress={g.progress.toFloat()/g.target.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                        Text("${g.progress.coerceAtMost(g.target)}/${g.target}",fontWeight=FontWeight.Bold)
                        Button(onClick={scope.launch{busy=g.id;runCatching{backend?.claimGoal(g.id)}.onSuccess{notice=sh("Ödül alındı.","Reward claimed.");reload()}.onFailure{notice=sh("Ödül henüz hazır değil.","Reward is not ready yet.")};busy=null}},enabled=done&&!g.claimed&&busy==null){Text(if(g.claimed)sh("ALINDI","CLAIMED") else sh("TOPLA","CLAIM"))}
                    }
                }
            }
        }
        if(notice.isNotBlank())item{Text(notice,Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=SonHarfGold,fontSize=10.sp)}
    }
}

@Composable
private fun RetentionLeaguePanel(backend: OnlineGameBackend?) {
    var rows by remember{mutableStateOf<List<LeaderboardV2Row>>(emptyList())}; var language by remember{mutableStateOf(SonHarfUiState.language)}
    LaunchedEffect(language){rows=runCatching{backend?.getLeaderboardV2(language,"week",50).orEmpty()}.getOrDefault(emptyList());runCatching{backend?.logEvent("league_open",language)}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{HubBanner("🏆",sh("HAFTALIK LİG","WEEKLY LEAGUE"),sh("Her hafta yeniden başlar. Galibiyet ve istikrar seni yukarı taşır.","Resets every week. Wins and consistency move you up."),SonHarfBlue)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=language=="tr",onClick={language="tr"},label={Text("🇹🇷 TÜRKÇE")});FilterChip(selected=language=="en",onClick={language="en"},label={Text("🇬🇧 ENGLISH")})}}
        if(rows.isEmpty())item{Text(sh("Bu hafta sıralama henüz oluşmadı.","No ranking yet this week."),Modifier.fillMaxWidth().padding(24.dp),textAlign=TextAlign.Center,color=SonHarfMuted)}
        items(rows.take(40)){r-> val rank=rows.indexOf(r)+1; Card(colors=CardDefaults.cardColors(containerColor=if(rank<=3)SonHarfGold.copy(alpha=.08f) else SonHarfSurface),shape=RoundedCornerShape(15.dp),border=BorderStroke(1.dp,if(rank<=3)SonHarfGold.copy(alpha=.35f) else SonHarfMuted.copy(alpha=.08f))){Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(if(rank==1)"🥇" else if(rank==2)"🥈" else if(rank==3)"🥉" else "#$rank",fontWeight=FontWeight.Black);Text(r.displayName,Modifier.weight(1f).padding(horizontal=10.dp),fontWeight=FontWeight.Bold);Text("${r.wins}W • ${r.winRate.toInt()}%",color=SonHarfCyan,fontSize=9.sp)}}}
    }
}

@Composable
private fun RetentionGamesPanel(backend: OnlineGameBackend?) {
    var games by remember{mutableStateOf<List<GameRoomDto>>(emptyList())}; var filter by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){games=runCatching{backend?.getMyGameHistory().orEmpty()}.getOrDefault(emptyList());runCatching{backend?.logEvent("history_open")}}
    val shown=when(filter){0->games.filter{it.status!="finished"&&it.status!="cancelled"};1->games.filter{it.status=="finished"};else->games}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(sh("Aktif","Active"),sh("Biten","Finished"),sh("Tümü","All")).forEachIndexed{i,s->FilterChip(selected=filter==i,onClick={filter=i},label={Text(s)})}}}
        if(shown.isEmpty())item{Text(sh("Bu bölümde maç yok.","No matches here."),Modifier.fillMaxWidth().padding(28.dp),textAlign=TextAlign.Center,color=SonHarfMuted)}
        items(shown.take(80),key={it.id}){r-> Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(16.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text(if(r.isBot)"🤖 ${r.botName?:"BOT"}" else "⚔ ${r.code}",fontWeight=FontWeight.Black);Text("${r.language.uppercase()} • ${r.status.uppercase()} • ${r.gameModeLabel()}",color=SonHarfMuted,fontSize=8.sp)};Text("${r.hostScore} - ${r.guestScore}",fontSize=18.sp,fontWeight=FontWeight.Black)}}}
    }
}

private fun GameRoomDto.gameModeLabel():String = if(roundWordCount>10 || roundNo>3) "UZMAN" else "NORMAL"

@Composable
private fun RetentionGuidePanel(backend: OnlineGameBackend?) {
    var news by remember{mutableStateOf<List<AppNewsDto>>(emptyList())}; LaunchedEffect(Unit){news=runCatching{backend?.getAppNews().orEmpty()}.getOrDefault(emptyList())}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{HubInfo("⚔",sh("30 SANİYEDE ÖĞREN","LEARN IN 30 SECONDS"),sh("Rakibin kelimesinin son harfiyle başlayan geçerli bir kelime yaz. Hızlı ol, serini bozma, 3 round sonunda öne geç.","Enter a valid word starting with the last letter of your opponent's word. Be quick, protect your streak and lead after 3 rounds."))}
        item{HubInfo("🔥",sh("SERİ & COMBO","STREAK & COMBO"),sh("Arka arkaya doğru hamleler seri oluşturur. Uzun seriler profilinde ve kariyerinde görünür.","Consecutive correct moves build streaks shown in your profile and career."))}
        item{HubInfo("👑",sh("UZMAN MODU","EXPERT MODE"),sh("15/15/15 kelime. 1. round son 1 harf, 2. round son 2 harf ×2, 3. round son 3 harf ×3.","15/15/15 words. Round 1 last 1 letter, round 2 last 2 letters ×2, round 3 last 3 letters ×3."))}
        item{HubInfo("👥",sh("ARKADAŞ DÜELLOSU","FRIEND DUEL"),sh("Oyna ekranından çevrimiçi arkadaşını seç, Düello butonuna bas veya paylaşım bağlantısı gönder.","Choose an online friend from Play, tap Duel or send a shared challenge."))}
        item{HubInfo("🎁",sh("ÖDÜLLER","REWARDS"),sh("Kariyer bölümünden günlük giriş ödülünü al. Günlük 3 maç meydan okumasını ve haftalık hedefleri tamamla.","Claim daily check-in rewards, complete the 3-match daily challenge and weekly goals."))}
        item{HubInfo("⚖",sh("PREMIUM ADİL KALIR","PREMIUM STAYS FAIR"),sh("Premium Style ve konfor sağlar; kelime gücü, skor, süre, rating veya lig avantajı vermez.","Premium adds Style and convenience, never word power, score, time, rating or league advantage."))}
        items(news.take(5)){n->HubInfo("📰",if(SonHarfUiState.isEnglish)n.titleEn else n.titleTr,if(SonHarfUiState.isEnglish)n.bodyEn else n.bodyTr)}
    }
}

@Composable
private fun RetentionSettingsPanel(backend: OnlineGameBackend?) {
    val context=LocalContext.current; val scope=rememberCoroutineScope(); var sound by remember{mutableStateOf(SonHarfPreferences.soundEnabled(context))};var vibration by remember{mutableStateOf(SonHarfPreferences.vibrationEnabled(context))};var dark by remember{mutableStateOf(SonHarfPreferences.darkModeEnabled(context))};var mode by remember{mutableStateOf(SonHarfGameModeState.mode)};var bot by remember{mutableStateOf(SonHarfPreferences.botDifficulty(context))}
    LaunchedEffect(Unit){runCatching{backend?.getPreferredGameMode()}.getOrNull()?.let{mode=it;SonHarfGameModeState.mode=it}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{HubBanner("⚙",sh("OYUN AYARLARI","GAME SETTINGS"),sh("Her ayar anında uygulanır ve cihazında saklanır.","Every setting applies immediately and is saved on your device."),SonHarfCyan)}
        item{SettingsCard(sh("Oyun modu","Game mode")){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=mode=="normal",onClick={mode="normal";SonHarfGameModeState.mode="normal";scope.launch{runCatching{backend?.setPreferredGameMode("normal")}}},label={Text("NORMAL")});FilterChip(selected=mode=="expert",onClick={mode="expert";SonHarfGameModeState.mode="expert";scope.launch{runCatching{backend?.setPreferredGameMode("expert")}}},label={Text(sh("UZMAN","EXPERT"))})}}}
        item{SettingsCard(sh("Bot zorluğu","Bot difficulty")){Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("easy" to sh("KOLAY","EASY"),"normal" to sh("NORMAL","NORMAL"),"hard" to sh("ZOR","HARD")).forEach{(v,t)->FilterChip(selected=bot==v,onClick={bot=v;SonHarfPreferences.setBotDifficulty(context,v)},label={Text(t,fontSize=9.sp)})}};Text(sh("Kolay bot daha yavaş, zor bot daha hızlı cevap verir.","Easy bot replies slower; hard bot replies faster."),color=SonHarfMuted,fontSize=8.sp)}}
        item{ToggleSetting(sh("Ses efektleri","Sound effects"),sound){sound=it;SonHarfPreferences.setSoundEnabled(context,it)}}
        item{ToggleSetting(sh("Titreşim","Vibration"),vibration){vibration=it;SonHarfPreferences.setVibrationEnabled(context,it)}}
        item{ToggleSetting(sh("Karanlık mod","Dark mode"),dark){dark=it;SonHarfPreferences.setDarkModeEnabled(context,it)}}
    }
}

@Composable private fun HubBanner(icon:String,title:String,text:String,color:Color){Card(colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.10f)),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,color.copy(alpha=.35f))){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=30.sp);Column{Text(title,fontWeight=FontWeight.Black,color=color);Text(text,color=SonHarfMuted,fontSize=9.sp)}}}}
@Composable private fun HubInfo(icon:String,title:String,text:String){Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top){Text(icon,fontSize=22.sp);Column{Text(title,fontWeight=FontWeight.Black);Text(text,color=SonHarfMuted,fontSize=10.sp)}}}}
@Composable private fun SettingsCard(title:String,content:@Composable ColumnScope.()->Unit){Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=SonHarfCyan,fontWeight=FontWeight.Black);content()}}}
@Composable private fun ToggleSetting(title:String,value:Boolean,onChange:(Boolean)->Unit){Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(17.dp)){Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(title,fontWeight=FontWeight.Bold);Switch(checked=value,onCheckedChange=onChange)}}}
