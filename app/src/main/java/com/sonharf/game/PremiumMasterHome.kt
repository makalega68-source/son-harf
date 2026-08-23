package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private val MasterInk = Color(0xFF163B58)
private val MasterBlue = Color(0xFF20AEE5)
private val MasterBlue2 = Color(0xFF48C8F2)
private val MasterSky = Color(0xFFEAF8FF)
private val MasterLine = Color(0xFFB8E6F7)
private val MasterGold = Color(0xFFFFC13B)
private val MasterGreen = Color(0xFF36C981)
private val MasterMuted = Color(0xFF6D879A)

@Composable
internal fun PremiumMasterHome(
    backend: OnlineGameBackend?,
    onPlay: () -> Unit,
    onQuickGame: () -> Unit,
    onBilBakalim: () -> Unit,
    onAdmin: () -> Unit,
    onHub: () -> Unit,
    onLeague: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var leaders by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var isAdmin by remember { mutableStateOf(false) }
    var dailyMessage by remember { mutableStateOf("") }

    suspend fun reload() {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        leaders = runCatching { backend?.getLeaderboardV2(SonHarfUiState.language, "week", 3).orEmpty() }.getOrDefault(emptyList())
        isAdmin = if (backend == null) false else runCatching { backend.getAdminDashboard(); true }.getOrDefault(false)
        runCatching { backend?.logEvent("home_open_master_reference") }
    }
    LaunchedEffect(Unit) { reload() }

    val rating = 1000 + (profile?.wins ?: 0) * 18
    val nextLeague = 3000
    val leagueProgress = (rating / nextLeague.toFloat()).coerceIn(0f, 1f)
    val streak = growth?.bestStreak ?: 0

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF5FCFF), MasterSky))),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item { MasterProfileHeader(profile, growth, isAdmin, onProfile, onAdmin) }
        item { MasterLeagueBanner(rating, leagueProgress, onLeague) }
        item { MasterLiveStrip(leaders.firstOrNull()?.displayName, streak) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MasterSonHarfCard(
                    modifier = Modifier.weight(1.18f),
                    playerName = profile?.displayName ?: sh("Sen", "You"),
                    rivalName = leaders.firstOrNull()?.displayName ?: sh("Rakip", "Rival"),
                    streak = streak.coerceAtLeast(4),
                    onPlay = onQuickGame,
                )
                MasterBilBakalimCard(Modifier.weight(.82f), onBilBakalim)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MasterDailySeries(Modifier.weight(1.35f), growth, dailyMessage) {
                    val d = growth
                    if (d == null || d.dailyClaimed) return@MasterDailySeries
                    scope.launch {
                        val reward = runCatching { backend?.claimDailyCheckin() ?: 0 }.getOrDefault(0)
                        dailyMessage = if (reward > 0) "+$reward" else sh("Alındı", "Claimed")
                        reload()
                    }
                }
                MasterSeasonCard(Modifier.weight(.65f), onHub)
            }
        }
        if (leaders.isNotEmpty()) item { MasterTopThree(leaders, onLeague) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MasterShortcut(Icons.Rounded.TrackChanges, sh("GÖREVLER", "GOALS"), 2, Modifier.weight(1f), onHub)
                MasterShortcut(Icons.Rounded.CardGiftcard, sh("GÜNLÜK ÖDÜL", "DAILY"), if (growth?.dailyClaimed == true) 0 else 1, Modifier.weight(1f)) {
                    val d = growth
                    if (d == null || d.dailyClaimed) return@MasterShortcut
                    scope.launch { runCatching { backend?.claimDailyCheckin() }; reload() }
                }
                MasterShortcut(Icons.Rounded.EmojiEvents, sh("LİGLER", "LEAGUES"), 0, Modifier.weight(1f), onLeague)
                MasterShortcut(Icons.Rounded.ShoppingCart, sh("MAĞAZA", "SHOP"), 0, Modifier.weight(1f), onShop)
                MasterShortcut(Icons.Rounded.Checkroom, sh("DOLABIM", "WARDROBE"), 0, Modifier.weight(1f), onProfile)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MasterProfileHeader(profile: ProfileDto?, growth: GrowthDashboardDto?, isAdmin: Boolean, onProfile: () -> Unit, onAdmin: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(58.dp).clickable(onClick = onProfile), shape = CircleShape, color = MasterSky, border = BorderStroke(3.dp, MasterBlue2)) {
            Box(contentAlignment = Alignment.Center) { Text(profile?.displayName?.take(1)?.uppercase() ?: "S", color = MasterInk, fontWeight = FontWeight.Black, fontSize = 25.sp) }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile?.displayName ?: sh("Oyuncu", "Player"), color = MasterInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(5.dp)); Icon(Icons.Rounded.Verified, null, tint = MasterBlue, modifier = Modifier.size(18.dp))
            }
            Text("${sh("Seviye", "Level")} ${growth?.level ?: 1}", color = MasterInk, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐ ${growth?.xp ?: 0} XP", color = MasterInk, fontSize = 10.sp)
                Spacer(Modifier.width(7.dp))
                LinearProgressIndicator(progress = { growth?.let { it.levelProgress.toFloat()/it.levelTarget.coerceAtLeast(1) } ?: 0f }, modifier = Modifier.width(72.dp).height(5.dp).clip(CircleShape), color = MasterBlue, trackColor = MasterLine)
            }
        }
        MasterWallet(Icons.Rounded.Paid, "${(growth?.xp ?: 0) * 2}", MasterGold)
        Spacer(Modifier.width(5.dp))
        MasterWallet(Icons.Rounded.Diamond, "${profile?.diamonds ?: 0}", MasterBlue)
        Spacer(Modifier.width(5.dp))
        Surface(onClick = if (isAdmin) onAdmin else ({}), shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, MasterLine), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Notifications, null, tint = MasterInk, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable private fun MasterWallet(icon: ImageVector, value: String, tint: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, MasterLine)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text(value, color = MasterInk, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}

@Composable private fun MasterLeagueBanner(rating: Int, progress: Float, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, MasterLine)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF3E3)) { Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFB66D31), modifier = Modifier.padding(12.dp).size(31.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("BRONZ II", color = MasterInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("🏆 $rating / 3.000", color = MasterInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(sh("Gümüş Lig'e sadece 2 galibiyet!", "Only 2 wins to Silver League!"), color = MasterMuted, fontSize = 9.sp)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(6.dp).clip(CircleShape), color = MasterBlue, trackColor = MasterLine)
            }
            Spacer(Modifier.width(9.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(sh("Sıralamada", "Ranking"), color = MasterMuted, fontSize = 8.sp)
                Text("#12.450", color = MasterInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Surface(shape = RoundedCornerShape(12.dp), color = MasterSky, border = BorderStroke(1.dp, MasterBlue2)) { Text(sh("LİDERLİK ›", "LEADERBOARD ›"), Modifier.padding(horizontal = 7.dp, vertical = 5.dp), color = MasterBlue, fontSize = 8.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable private fun MasterLiveStrip(rival: String?, streak: Int) {
    Surface(shape = RoundedCornerShape(18.dp), color = MasterSky, border = BorderStroke(1.dp, MasterLine)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LiveCell("👥", sh("Ezeli rakibin", "Arch-rival"), "${rival ?: sh("Rakip", "Rival")} ${sh("çevrimiçi!", "online!")}", Modifier.weight(1f))
            LiveCell("🏆", sh("Turnuva başlıyor!", "Tournament starts!"), "18 dk", Modifier.weight(1f))
            LiveCell("🔥", "${streak.coerceAtLeast(4)} ${sh("maçlık galibiyet", "win streak")}", sh("serin devam ediyor!", "keep it going!"), Modifier.weight(1f))
        }
    }
}

@Composable private fun LiveCell(icon:String, title:String, detail:String, modifier:Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp); Spacer(Modifier.width(5.dp)); Column { Text(title, color = MasterInk, fontWeight = FontWeight.Bold, fontSize = 9.sp); Text(detail, color = MasterBlue, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1) }
    }
}

@Composable private fun MasterSonHarfCard(modifier: Modifier, playerName:String, rivalName:String, streak:Int, onPlay:()->Unit) {
    Card(onClick = onPlay, modifier = modifier.height(360.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = BorderStroke(1.5.dp, MasterBlue2)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF12A8EE), Color(0xFF0870C9), Color(0xFF064D9B)))).padding(13.dp)) {
            MasterLetterBackdrop()
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👑", fontSize = 33.sp)
                Text("SON", color = MasterGold, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
                Text("HARF", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, lineHeight = 42.sp)
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF0E4E96), border = BorderStroke(1.dp, Color.White.copy(.55f))) { Text(sh("CANLI KELİME ARENASI", "LIVE WORD ARENA"), Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                Text(sh("Kelimenin son harfiyle zafer senin!", "Victory with the last letter!"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Text("KALEM → MASA → ARABA", color = Color(0xFF583D26), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.background(Color(0xFFFFE4B3), RoundedCornerShape(9.dp)).padding(horizontal = 8.dp, vertical = 7.dp))
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PlayerVsCell(playerName, "2150", Modifier.weight(1f)); Text("VS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black); PlayerVsCell(rivalName, "2186", Modifier.weight(1f))
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Surface(Modifier.weight(.75f), shape=RoundedCornerShape(16.dp), color=Color(0xFF0B5AA6)) { Column(Modifier.padding(8.dp), horizontalAlignment=Alignment.CenterHorizontally){Text("🔥",fontSize=19.sp);Text("$streak ${sh("GALİBİYET", "WINS")}",color=Color.White,fontWeight=FontWeight.Black,fontSize=7.sp)} }
                    Button(onClick=onPlay, modifier=Modifier.weight(1.65f).height(54.dp), shape=RoundedCornerShape(19.dp), colors=ButtonDefaults.buttonColors(containerColor=MasterBlue2)) { Icon(Icons.Rounded.PlayArrow,null); Spacer(Modifier.width(4.dp)); Text(sh("OYNA", "PLAY"),fontWeight=FontWeight.Black,fontSize=17.sp) }
                    Surface(Modifier.weight(.65f), shape=RoundedCornerShape(16.dp), color=Color(0xFF0B5AA6)) { Column(Modifier.padding(8.dp), horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.GridView,null,tint=Color.White);Text(sh("MOD", "MODE"),color=Color.White,fontSize=7.sp,fontWeight=FontWeight.Black)} }
                }
            }
        }
    }
}

@Composable private fun MasterLetterBackdrop() { Canvas(Modifier.fillMaxSize()) { val c=Color.White.copy(.13f); listOf(.12f,.28f,.76f,.88f).forEachIndexed{i,x->drawCircle(c, radius=22f, center=Offset(size.width*x,size.height*(.18f+i*.13f))) } } }
@Composable private fun PlayerVsCell(name:String,rating:String,modifier:Modifier){ Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){Surface(shape=CircleShape,color=Color.White.copy(.20f),border=BorderStroke(2.dp,Color.White)){Box(Modifier.size(36.dp),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),color=Color.White,fontWeight=FontWeight.Black)}};Text(name,color=Color.White,fontWeight=FontWeight.Black,fontSize=9.sp,maxLines=1);Text("🏆 $rating",color=MasterGold,fontSize=8.sp)} }

@Composable private fun MasterBilBakalimCard(modifier:Modifier,onPlay:()->Unit){
    Card(onClick=onPlay,modifier=modifier.height(360.dp),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent),border=BorderStroke(1.5.dp,MasterBlue2)){
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF4FDFF),Color(0xFFDDF6FF),Color.White))).padding(13.dp)){
            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){
                Text("💡",fontSize=37.sp); Text("BİL",color=MasterInk,fontSize=31.sp,fontWeight=FontWeight.Black,lineHeight=28.sp);Text("BAKALIM",color=MasterInk,fontSize=31.sp,fontWeight=FontWeight.Black,lineHeight=30.sp)
                Text(sh("Doğru cevaba en yakın tahmin kazanır!", "Closest estimate wins!"),color=MasterInk,fontSize=8.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(7.dp)); Text("?",color=MasterGold,fontSize=74.sp,fontWeight=FontWeight.Black,lineHeight=74.sp)
                Spacer(Modifier.weight(1f)); Surface(shape=RoundedCornerShape(16.dp),color=MasterBlue){Text(sh("BUGÜNKÜ MEYDAN OKUMA", "TODAY'S CHALLENGE"),Modifier.padding(horizontal=9.dp,vertical=5.dp),color=Color.White,fontSize=7.sp,fontWeight=FontWeight.Black)}
                Spacer(Modifier.height(5.dp)); Text(sh("En hızlı cevap", "Fastest answer"),color=MasterMuted,fontSize=8.sp);Text("6,4 sn",color=MasterInk,fontSize=18.sp,fontWeight=FontWeight.Black)
                Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=MasterBlue)){Icon(Icons.Rounded.PlayArrow,null);Text(sh(" HEMEN OYNA", " PLAY NOW"),fontWeight=FontWeight.Black,fontSize=13.sp)}
            }
        }
    }
}

@Composable private fun MasterDailySeries(modifier:Modifier,growth:GrowthDashboardDto?,message:String,onClaim:()->Unit){
    Card(onClick=onClaim,modifier=modifier.height(154.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,MasterLine)){
        Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(sh("GÜNLÜK SERİ","DAILY STREAK"),color=MasterInk,fontWeight=FontWeight.Black,fontSize=14.sp);Text(if(message.isNotBlank())message else "⏱ 18 saat",color=MasterMuted,fontSize=8.sp)};Text(sh("Her gün oyna, ödülleri kaçırma!","Play daily, don't miss rewards!"),color=MasterMuted,fontSize=8.sp);Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){(1..7).forEach{d->Surface(Modifier.weight(1f).height(65.dp),shape=RoundedCornerShape(10.dp),color=if(d==3)Color(0xFFDFF5FF) else MasterSky,border=BorderStroke(1.dp,if(d==3)MasterBlue else MasterLine)){Column(Modifier.fillMaxSize().padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){Text("Gün $d",fontSize=7.sp,color=MasterInk);Text(if(d<=2)"✅" else if(d==7)"🎁" else "⭐",fontSize=15.sp);Text(listOf("100","150","200","5","300","10","500")[d-1],fontSize=8.sp,fontWeight=FontWeight.Black,color=MasterInk)}}}}
        }
    }
}

@Composable private fun MasterSeasonCard(modifier:Modifier,onClick:()->Unit){Card(onClick=onClick,modifier=modifier.height(154.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,MasterLine)){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("SEZON 12",color=MasterInk,fontWeight=FontWeight.Black,fontSize=14.sp);Text("🎁",fontSize=39.sp);LinearProgressIndicator(progress={.72f},modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),color=MasterBlue,trackColor=MasterLine);Text("7.250 / 10.000",color=MasterInk,fontWeight=FontWeight.Bold,fontSize=8.sp);Text("24 gün 18 saat",color=MasterMuted,fontSize=7.sp)}}}

@Composable private fun MasterTopThree(leaders:List<LeaderboardV2Row>,onClick:()->Unit){Card(onClick=onClick,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,MasterLine)){Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("🏆 ${sh("HAFTANIN EN İYİ 3 OYUNCUSU","TOP 3 THIS WEEK")}",color=MasterInk,fontWeight=FontWeight.Black,fontSize=11.sp);Text(sh("CANLI","LIVE"),color=MasterGreen,fontWeight=FontWeight.Black,fontSize=8.sp)};Spacer(Modifier.height(7.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){leaders.take(3).forEachIndexed{i,r->Surface(Modifier.weight(1f),shape=RoundedCornerShape(14.dp),color=MasterSky){Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(listOf("🥇","🥈","🥉").getOrElse(i){"#${i+1}"},fontSize=19.sp);Text(r.displayName,color=MasterInk,fontWeight=FontWeight.Black,fontSize=9.sp,maxLines=1);Text("${r.wins}W • ${r.winRate.toInt()}%",color=MasterMuted,fontSize=7.sp)}}}}}}}

@Composable private fun MasterShortcut(icon:ImageVector,label:String,badge:Int,modifier:Modifier,onClick:()->Unit){Card(onClick=onClick,modifier=modifier.height(88.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,MasterLine)){Box(Modifier.fillMaxSize().padding(6.dp)){Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=MasterBlue,modifier=Modifier.size(25.dp));Spacer(Modifier.height(5.dp));Text(label,color=MasterInk,fontSize=7.5.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center,maxLines=2)};if(badge>0)Surface(Modifier.align(Alignment.TopEnd).size(20.dp),shape=CircleShape,color=Color(0xFFE65E67)){Box(contentAlignment=Alignment.Center){Text("$badge",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Black)}}}}}
