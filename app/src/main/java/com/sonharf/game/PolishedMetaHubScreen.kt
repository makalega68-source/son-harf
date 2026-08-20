package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
fun PolishedMetaHubScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🎯" to sh("Hedefler", "Goals"), "🏆" to sh("Lig", "League"), "🎮" to sh("Oyunlarım", "Games"), "📰" to sh("Rehber", "Guide"), "⚙" to sh("Ayarlar", "Settings"))
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFD9F0FF), SonHarfBg, Color(0xFFEAF8FF))))) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .96f)),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .22f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(sh("OYUNCU MERKEZİ", "PLAYER HUB"), fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text(sh("Hedeflerini takip et, ligde yüksel, geçmişini gör ve oyunu kişiselleştir.", "Track goals, climb the league, review games and personalize the experience."), color = SonHarfMuted, fontSize = 10.sp)
            }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp, containerColor = Color.Transparent, divider = {}) {
            tabs.forEachIndexed { i, (icon, title) ->
                Tab(selected = tab == i, onClick = { tab = i }, text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 18.sp); Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> PolishedGoals(backend)
                1 -> PolishedLeague(backend)
                2 -> PolishedGames(backend)
                3 -> PolishedGuide(backend)
                else -> PolishedSettings(backend)
            }
        }
    }
}

@Composable
private fun PolishedGoals(backend: OnlineGameBackend?) {
    val scope = rememberCoroutineScope()
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    suspend fun reload() { goals = runCatching { backend?.getGoals().orEmpty() }.getOrDefault(emptyList()) }
    LaunchedEffect(Unit) { reload() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { HubHero("🎯", sh("HAFTALIK HEDEFLER", "WEEKLY GOALS"), sh("Her pazartesi yenilenir. Tamamla ve elmaslarını topla.", "Refreshes every Monday. Complete and claim diamonds."), SonHarfGold) }
        items(goals, key = { it.id }) { g ->
            val title = if (SonHarfUiState.isEnglish) g.titleEn else g.titleTr
            val desc = if (SonHarfUiState.isEnglish) g.descriptionEn else g.descriptionTr
            val done = g.progress >= g.target
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (done) SonHarfGreen.copy(alpha=.35f) else SonHarfMuted.copy(alpha=.10f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.Black); Text("💎 ${g.rewardDiamonds}", color = SonHarfCyan, fontWeight = FontWeight.Black) }
                    Text(desc, color = SonHarfMuted, fontSize = 9.sp)
                    LinearProgressIndicator(progress = { (g.progress.toFloat()/g.target.coerceAtLeast(1)).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${g.progress.coerceAtMost(g.target)}/${g.target}", fontWeight = FontWeight.Bold)
                        Button(onClick = { scope.launch { busy=g.id; runCatching { backend?.claimGoal(g.id) }.onSuccess { notice=sh("Ödül alındı.", "Reward claimed."); reload() }.onFailure { notice=sh("Ödül henüz hazır değil.", "Reward is not ready yet.") }; busy=null } }, enabled = done && !g.claimed && busy == null, shape = RoundedCornerShape(13.dp)) { Text(if (g.claimed) sh("ALINDI", "CLAIMED") else if (busy==g.id) "…" else sh("TOPLA", "CLAIM"), fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
        if (notice.isNotBlank()) item { Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = SonHarfMuted, fontSize = 10.sp) }
    }
}

@Composable
private fun PolishedLeague(backend: OnlineGameBackend?) {
    var rows by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var language by remember { mutableStateOf(SonHarfUiState.language) }
    LaunchedEffect(language) { rows = runCatching { backend?.getLeaderboardV2(language, "week", 50).orEmpty() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { HubHero("🏆", sh("HAFTALIK SÜPER LİG", "WEEKLY SUPER LEAGUE"), sh("Pazartesi yenilenir • İlk üç oyuncu ana sayfada görünür.", "Resets Monday • Top three appear on the home screen."), SonHarfGold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected=language=="tr", onClick={language="tr"}, label={Text("🇹🇷 TÜRKÇE")}); FilterChip(selected=language=="en", onClick={language="en"}, label={Text("🇬🇧 ENGLISH")}) } }
        if (rows.isEmpty()) item { EmptyHub(sh("Bu hafta henüz sonuç yok.", "No results yet this week.")) }
        items(rows.take(40)) { r ->
            val rank = rows.indexOf(r)+1
            Card(colors = CardDefaults.cardColors(containerColor = if(rank<=3) SonHarfGold.copy(alpha=.09f) else SonHarfSurface), shape = RoundedCornerShape(18.dp), border=BorderStroke(1.dp, if(rank<=3) SonHarfGold.copy(alpha=.30f) else SonHarfMuted.copy(alpha=.10f))) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(when(rank){1->"🥇";2->"🥈";3->"🥉";else->"#$rank"}, fontWeight=FontWeight.Black, fontSize=16.sp)
                    Text(r.displayName, Modifier.weight(1f).padding(horizontal=11.dp), fontWeight=FontWeight.Bold)
                    Column(horizontalAlignment=Alignment.End) { Text("${r.wins}W", color=SonHarfCyan, fontWeight=FontWeight.Black); Text("${r.winRate.toInt()}%", color=SonHarfMuted, fontSize=8.sp) }
                }
            }
        }
    }
}

@Composable
private fun PolishedGames(backend: OnlineGameBackend?) {
    var games by remember { mutableStateOf<List<GameRoomDto>>(emptyList()) }
    var filter by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { games = runCatching { backend?.getMyGameHistory().orEmpty() }.getOrDefault(emptyList()) }
    val shown = when(filter){0->games.filter{it.status !in setOf("finished","cancelled")};1->games.filter{it.status=="finished"};else->games}
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(14.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
        item { HubHero("🎮", sh("OYUN GEÇMİŞİ", "GAME HISTORY"), sh("Aktif maçlarını ve biten düellolarını tek yerde gör.", "See active matches and completed duels in one place."), SonHarfCyan) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) { listOf(sh("Aktif","Active"),sh("Biten","Finished"),sh("Tümü","All")).forEachIndexed{i,s->FilterChip(selected=filter==i,onClick={filter=i},label={Text(s)})} } }
        if(shown.isEmpty()) item { EmptyHub(sh("Bu bölümde oyun bulunmuyor.", "No games in this section.")) }
        items(shown.take(60), key={it.id}) { r -> Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(13.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ Column{Text(if(r.isBot)"🤖 ${r.botName?:"BOT"}" else "⚔ ${r.code}",fontWeight=FontWeight.Black);Text("${r.language.uppercase()} • ${r.status.uppercase()} • R${r.roundNo}",color=SonHarfMuted,fontSize=8.sp)};Text("${r.hostScore} - ${r.guestScore}",fontSize=20.sp,fontWeight=FontWeight.Black)} } }
    }
}

@Composable
private fun PolishedGuide(backend: OnlineGameBackend?) {
    var news by remember { mutableStateOf<List<AppNewsDto>>(emptyList()) }
    LaunchedEffect(Unit) { news = runCatching { backend?.getAppNews().orEmpty() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(14.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { HubHero("📰", sh("HABERLER & REHBER", "NEWS & GUIDE"), sh("Güncellemeler, kurallar ve oyun rehberi.", "Updates, rules and gameplay guide."), SonHarfPurple) }
        items(news) { n -> HubInfo("🆕", if(SonHarfUiState.isEnglish)n.titleEn else n.titleTr, if(SonHarfUiState.isEnglish)n.bodyEn else n.bodyTr) }
        item { HubInfo("ℹ", sh("KURALLAR","RULES"), sh("Normal Mod 3×10 kelime. Uzman Modu 15/15/15; 1., 2. ve 3. round sırasıyla son 1/2/3 harfle başlar ve ×1/×2/×3 puan verir. Ğ ile başlayan veya biten Türkçe kelimeler kabul edilmez.", "Normal Mode is 3×10. Expert is 15/15/15 using the last 1/2/3 letters with ×1/×2/×3 score. Turkish words starting or ending with Ğ are rejected.")) }
        item { HubInfo("?", sh("NASIL OYNANIR?","HOW TO PLAY?"), sh("Sıra sende olduğunda istenen harflerle başlayan geçerli bir kelime yaz. Tekrar, yanlış başlangıç ve süre aşımı −1 puandır. Genel kültür sorularında doğru cevap bonus kazandırır.", "Enter a valid word beginning with the required letters. Repeats, wrong prefixes and timeouts cost −1. Correct trivia answers award a bonus.")) }
        item { HubInfo("💎", sh("KOZMETİKLER","COSMETICS"), sh("Satın aldığın çerçeve, isim stili, Aurora arena, neon klavye, zafer efekti ve VIP emoji paketini Mağaza'dan KULLAN diyerek etkinleştirebilirsin.", "Equip purchased frames, name styles, Aurora arena, neon keyboard, victory effect and VIP emoji pack from the Shop.")) }
    }
}

@Composable
private fun PolishedSettings(backend: OnlineGameBackend?) {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var sound by remember{mutableStateOf(SonHarfPreferences.soundEnabled(context))}; var vibration by remember{mutableStateOf(SonHarfPreferences.vibrationEnabled(context))}; var dark by remember{mutableStateOf(SonHarfPreferences.darkModeEnabled(context))}; var mode by remember{mutableStateOf(SonHarfGameModeState.mode)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { HubHero("⚙",sh("AYARLAR","SETTINGS"),sh("Oyun modunu ve deneyimini düzenle.","Tune your mode and experience."),SonHarfCyan) }
        item { Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(sh("OYUN MODU","GAME MODE"),fontWeight=FontWeight.Black,color=SonHarfCyan);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=mode=="normal",onClick={mode="normal";SonHarfGameModeState.mode="normal";scope.launch{runCatching{backend?.setPreferredGameMode("normal")}}},label={Text("NORMAL")});FilterChip(selected=mode=="expert",onClick={mode="expert";SonHarfGameModeState.mode="expert";scope.launch{runCatching{backend?.setPreferredGameMode("expert")}}},label={Text(sh("UZMAN","EXPERT"))})}}} }
        item { HubToggle(sh("Ses Efektleri","Sound Effects"),sound){sound=it;SonHarfPreferences.setSoundEnabled(context,it)} }
        item { HubToggle(sh("Titreşim","Vibration"),vibration){vibration=it;SonHarfPreferences.setVibrationEnabled(context,it)} }
        item { HubToggle(sh("Karanlık Mod","Dark Mode"),dark){dark=it;SonHarfPreferences.setDarkModeEnabled(context,it)} }
    }
}

@Composable private fun HubHero(icon:String,title:String,text:String,accent:Color){Card(colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.09f)),shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,accent.copy(alpha=.28f))){Row(Modifier.fillMaxWidth().padding(15.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=34.sp);Column{Text(title,fontWeight=FontWeight.Black,fontSize=18.sp,color=accent);Text(text,color=SonHarfMuted,fontSize=9.sp)}}}}
@Composable private fun HubInfo(icon:String,title:String,text:String){Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top){Text(icon,fontSize=22.sp);Column{Text(title,fontWeight=FontWeight.Black);Text(text,color=SonHarfMuted,fontSize=10.sp)}}}}
@Composable private fun EmptyHub(text:String){Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){Text(text,Modifier.fillMaxWidth().padding(25.dp),textAlign=TextAlign.Center,color=SonHarfMuted)}}
@Composable private fun HubToggle(title:String,value:Boolean,onChange:(Boolean)->Unit){Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(title,fontWeight=FontWeight.Bold);Switch(checked=value,onCheckedChange=onChange)}}}
