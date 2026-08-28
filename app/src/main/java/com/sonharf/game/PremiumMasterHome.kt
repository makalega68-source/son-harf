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

private val MasterInk = LetharaPalette.Text
private val MasterBlue = LetharaPalette.Cyan
private val MasterBlue2 = LetharaPalette.Violet
private val MasterSky = Color(0xFF15284A)
private val MasterPanel = Color(0xFF101D39)
private val MasterLine = Color(0xFF29486B)
private val MasterGold = LetharaPalette.Gold
private val MasterGreen = LetharaPalette.Green
private val MasterMuted = LetharaPalette.Muted

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
    onGoals: () -> Unit = onHub,
    onSeason: () -> Unit = onHub,
    onWardrobe: () -> Unit = onProfile,
    onNotifications: () -> Unit = onProfile,
    onDailyCipher: () -> Unit = onHub,
    onMastery: () -> Unit = onHub,
    onHistory: () -> Unit = onHub,
    onMascot: () -> Unit = onProfile,
    onRoom: () -> Unit = onMascot,
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

    val streak = growth?.currentWinStreak ?: 0

    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LetharaPalette.Night, Color(0xFF0B1730), LetharaPalette.Night2))),
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { MasterProfileHeader(profile, growth, isAdmin, onProfile, onAdmin, onNotifications) }
            item { MasterLeagueBanner(growth, onLeague) }
            item { MasterLetharaBanner(onHistory, onMascot) }
            item { MasterLiveStrip(streak) }
            item {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 600.dp
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(250.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Keep the selected mascot inside the same LazyColumn item as the main game card.
                                // It scrolls with this content instead of floating over menus.
                                MascotHomeCompanion(
                                    modifier = Modifier
                                        .width(132.dp)
                                        .fillMaxHeight(),
                                    playerName = profile?.displayName,
                                )
                                MasterSonHarfCard(
                                    Modifier.weight(1f).fillMaxHeight(),
                                    profile?.displayName ?: sh("Sen", "You"),
                                    profile?.avatarPath,
                                    profile?.gender,
                                    sh("Rakip", "Rival"),
                                    streak,
                                    onQuickGame,
                                )
                            }
                            MasterBilBakalimCard(Modifier.fillMaxWidth(), onBilBakalim)
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MascotHomeCompanion(
                                modifier = Modifier
                                    .width(148.dp)
                                    .height(250.dp),
                                playerName = profile?.displayName,
                            )
                            MasterSonHarfCard(
                                Modifier.weight(1.18f),
                                profile?.displayName ?: sh("Sen", "You"),
                                profile?.avatarPath,
                                profile?.gender,
                                sh("Rakip", "Rival"),
                                streak,
                                onQuickGame,
                            )
                            MasterBilBakalimCard(Modifier.weight(.82f), onBilBakalim)
                        }
                    }
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
                    MasterSeasonCard(Modifier.weight(.65f), onSeason)
                }
            }
            if (leaders.isNotEmpty()) item { MasterTopThree(leaders, onLeague) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MasterShortcut(Icons.Rounded.TrackChanges, sh("GÖREVLER", "GOALS"), 2, Modifier.weight(1f), onGoals)
                    MasterShortcut(Icons.Rounded.CardGiftcard, sh("GÜNLÜK ÖDÜL", "DAILY"), if (growth?.dailyClaimed == true) 0 else 1, Modifier.weight(1f)) {
                        val d = growth
                        if (d == null || d.dailyClaimed) return@MasterShortcut
                        scope.launch { runCatching { backend?.claimDailyCheckin() }; reload() }
                    }
                    MasterShortcut(Icons.Rounded.EmojiEvents, sh("MÜHÜR LİGLERİ", "SEAL LEAGUES"), 0, Modifier.weight(1f), onLeague)
                    MasterShortcut(Icons.Rounded.ShoppingCart, "STYLE", 0, Modifier.weight(1f), onShop)
                    MasterShortcut(Icons.Rounded.Checkroom, sh("DOLABIM", "WARDROBE"), 0, Modifier.weight(1f), onWardrobe)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MasterShortcut(Icons.Rounded.Lightbulb, sh("GÜNÜN ŞİFRESİ", "DAILY CIPHER"), 1, Modifier.weight(1f), onDailyCipher)
                    MasterShortcut(Icons.Rounded.MilitaryTech, sh("USTALIK YOLU", "MASTERY PATH"), 0, Modifier.weight(1f), onMastery)
                    MasterShortcut(Icons.Rounded.Whatshot, sh("SON MÜHÜR", "LAST SEAL"), 0, Modifier.weight(1f), onMastery)
                    MasterShortcut(Icons.Rounded.Groups, sh("EZELİ RAKİP", "ARCH RIVAL"), 0, Modifier.weight(1f), onMastery)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MasterShortcut(Icons.Rounded.AutoStories, sh("BÜYÜCÜLERİN GEÇMİŞİ", "PAST OF THE MAGES"), 0, Modifier.weight(1f), onHistory)
                    MasterShortcut(Icons.Rounded.Pets, sh("YOLDAŞIM", "COMPANION"), 0, Modifier.weight(1f), onMascot)
                    MasterShortcut(Icons.Rounded.Home, sh("MÜHÜR ODASI", "SEAL ROOM"), 0, Modifier.weight(1f), onRoom)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun MasterProfileHeader(
    profile: ProfileDto?,
    growth: GrowthDashboardDto?,
    isAdmin: Boolean,
    onProfile: () -> Unit,
    onAdmin: () -> Unit,
    onNotifications: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable(onClick = onProfile)) {
            ProfilePhotoAvatarWithGender(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = profile?.displayName ?: sh("Oyuncu", "Player"),
                size = 58.dp,
                accent = MasterBlue2,
            )
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
        MasterWallet(Icons.Rounded.Paid, "${profile?.diamonds ?: 0} SC", MasterGold)
        Spacer(Modifier.width(5.dp))
        Surface(onClick = if (isAdmin) onAdmin else onNotifications, shape = CircleShape, color = MasterPanel, border = BorderStroke(1.dp, MasterLine), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Notifications, null, tint = MasterInk, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable private fun MasterWallet(icon: ImageVector, value: String, tint: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = MasterPanel, border = BorderStroke(1.dp, MasterLine)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text(value, color = MasterInk, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}

@Composable private fun MasterLeagueBanner(growth: GrowthDashboardDto?, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MasterPanel), border = BorderStroke(1.dp, MasterLine)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = MasterGold.copy(alpha = .12f)) { Icon(Icons.Rounded.EmojiEvents, null, tint = MasterGold, modifier = Modifier.padding(12.dp).size(31.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(growth?.leagueName ?: sh("LİG", "LEAGUE"), color = MasterInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("🏆 ${growth?.wins ?: 0} ${sh("galibiyet", "wins")}", color = MasterInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(sh("Güncel sıralamanı ve lig detaylarını aç.", "Open your current ranking and league details."), color = MasterMuted, fontSize = 9.sp)
            }
            Spacer(Modifier.width(9.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MasterSky, border = BorderStroke(1.dp, MasterBlue2)) {
                Text(sh("LİDERLİK ›", "LEADERBOARD ›"), Modifier.padding(horizontal = 7.dp, vertical = 7.dp), color = MasterBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MasterLetharaBanner(onHistory: () -> Unit, onMascot: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LetharaPalette.Night2,
        border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha = .45f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = LetharaPalette.Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text("LETHARA", color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.4.sp)
                    Text(sh("Her son, yeni bir başlangıçtır.", "Every ending is a new beginning."), color = LetharaPalette.Text, fontSize = 10.sp)
                }
            }
            Text(
                sh("Altı Mühür'ün hafızası parçalandı. Maçlar ve yoldaşlık, Söz Dokusu'nu yeniden uyandırıyor.", "The Six Seals lost their memories. Matches and companionship are awakening the Word Weave again."),
                color = LetharaPalette.Muted,
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, LetharaPalette.Gold)) {
                    Text(sh("HİKÂYE", "STORY"), color = LetharaPalette.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Button(onClick = onMascot, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = LetharaPalette.Violet)) {
                    Text(sh("YOLDAŞIM", "COMPANION"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable private fun MasterLiveStrip(streak: Int) {
    Surface(shape = RoundedCornerShape(18.dp), color = MasterSky, border = BorderStroke(1.dp, MasterLine)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LiveCell("⚔", sh("Yeni düello", "New duel"), sh("Oyna sekmesinden başla", "Start from Play"), Modifier.weight(1f))
            LiveCell("🏆", sh("Haftalık lig", "Weekly league"), sh("Sıralamanı kontrol et", "Check your ranking"), Modifier.weight(1f))
            LiveCell("🔥", "$streak ${sh("maçlık seri", "win streak")}", if (streak > 0) sh("serin devam ediyor!", "keep it going!") else sh("yeni seri başlat", "start a new streak"), Modifier.weight(1f))
        }
    }
}

@Composable private fun LiveCell(icon:String, title:String, detail:String, modifier:Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp); Spacer(Modifier.width(5.dp)); Column { Text(title, color = MasterInk, fontWeight = FontWeight.Bold, fontSize = 9.sp); Text(detail, color = MasterBlue, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1) }
    }
}

@Composable private fun MasterSonHarfCard(modifier: Modifier, playerName:String, playerAvatarPath:String?, playerGender:String?, rivalName:String, streak:Int, onPlay:()->Unit) {
    Card(onClick = onPlay, modifier = modifier.height(250.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = BorderStroke(1.5.dp, MasterBlue2)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF172D59), Color(0xFF13234B), Color(0xFF241A4A)))).padding(13.dp)) {
            MasterLetterBackdrop()
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                LetharaGameMark()
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF182B55), border = BorderStroke(1.dp, LetharaPalette.Gold.copy(.55f))) { Text(sh("SÖZ DOKUSU DÜELLOSU", "WORD WEAVE DUEL"), Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = LetharaPalette.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                Text(sh("Her son harf, yeni bir mührü açar.", "Every final letter opens a new seal."), color = LetharaPalette.Text, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Text("KALEM → MASA → ARABA", color = Color(0xFF221A38), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.background(LetharaPalette.Gold, RoundedCornerShape(9.dp)).padding(horizontal = 8.dp, vertical = 7.dp))
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PlayerVsCell(playerName, Modifier.weight(1f), playerAvatarPath, playerGender); Text("VS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black); PlayerVsCell(rivalName, Modifier.weight(1f))
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Surface(Modifier.weight(.9f).height(54.dp), shape=RoundedCornerShape(16.dp), color=Color(0xFF0B5AA6)) { Column(Modifier.fillMaxSize().padding(horizontal=6.dp), horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("🔥 $streak",fontSize=15.sp);Text("GALİBİYET",color=Color.White,fontWeight=FontWeight.Black,fontSize=7.sp,maxLines=1,softWrap=false)} }
                    Button(onClick=onPlay, modifier=Modifier.weight(1.8f).height(54.dp), shape=RoundedCornerShape(19.dp), colors=ButtonDefaults.buttonColors(containerColor=MasterGold, contentColor=Color(0xFF221A38)),contentPadding=PaddingValues(horizontal=12.dp)) { Icon(Icons.Rounded.PlayArrow,null); Spacer(Modifier.width(5.dp)); Text("OYNA",fontWeight=FontWeight.Black,fontSize=16.sp,maxLines=1,softWrap=false) }
                    Surface(Modifier.weight(.65f), shape=RoundedCornerShape(16.dp), color=Color(0xFF0B5AA6)) { Column(Modifier.padding(8.dp), horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.GridView,null,tint=Color.White);Text("MOD",color=Color.White,fontSize=7.sp,fontWeight=FontWeight.Black)} }
                }
            }
        }
    }
}

@Composable
private fun LetharaGameMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = Color(0xFF0C1733),
            border = BorderStroke(2.dp, LetharaPalette.Gold),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = LetharaPalette.Cyan,
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    "S",
                    color = LetharaPalette.Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
                )
            }
        }
        Text("SON HARF", color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.8.sp)
    }
}

@Composable private fun MasterLetterBackdrop() { Canvas(Modifier.fillMaxSize()) { val c=Color.White.copy(.13f); listOf(.12f,.28f,.76f,.88f).forEachIndexed{i,x->drawCircle(c, radius=22f, center=Offset(size.width*x,size.height*(.18f+i*.13f))) } } }
@Composable private fun PlayerVsCell(name:String,modifier:Modifier,avatarPath:String?=null,gender:String?=null){ Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){ if(!avatarPath.isNullOrBlank()) ProfilePhotoAvatarWithGender(avatarPath=avatarPath,gender=gender,name=name,size=40.dp,accent=Color.White) else Surface(shape=CircleShape,color=Color.White.copy(.20f),border=BorderStroke(2.dp,Color.White)){Box(Modifier.size(36.dp),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),color=Color.White,fontWeight=FontWeight.Black)}};Text(name,color=Color.White,fontWeight=FontWeight.Black,fontSize=9.sp,maxLines=1)} }

@Composable private fun MasterBilBakalimCard(modifier:Modifier,onPlay:()->Unit){
    Card(onClick=onPlay,modifier=modifier.height(205.dp),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent),border=BorderStroke(1.5.dp,MasterBlue2)){
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF13254A),Color(0xFF101D39),Color(0xFF171638)))).padding(horizontal=14.dp,vertical=10.dp)){
            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){ Text("💡",fontSize=25.sp); Text("BİL BAKALIM",color=MasterInk,fontSize=24.sp,fontWeight=FontWeight.Black,maxLines=1,softWrap=false) }
                Text("Doğru cevaba en yakın tahmin kazanır!",modifier=Modifier.fillMaxWidth(),color=MasterInk,fontSize=10.sp,textAlign=TextAlign.Center,maxLines=1)
                Surface(shape=RoundedCornerShape(14.dp),color=MasterSky,border=BorderStroke(1.dp,MasterLine)){ Text("TÜRKÇE  •  ENGLISH",Modifier.padding(horizontal=12.dp,vertical=5.dp),color=MasterBlue,fontSize=8.sp,fontWeight=FontWeight.Black,maxLines=1) }
                Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFF252052)){ Text(sh("BUGÜNÜN SÖZ MÜHRÜ", "TODAY'S WORD SEAL"),Modifier.padding(horizontal=11.dp,vertical=6.dp),color=LetharaPalette.Gold,fontSize=8.sp,fontWeight=FontWeight.Black,maxLines=1,softWrap=false) }
                Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(42.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=MasterGold, contentColor=Color(0xFF221A38)),contentPadding=PaddingValues(horizontal=10.dp)){ Icon(Icons.Rounded.PlayArrow,null,modifier=Modifier.size(20.dp)); Spacer(Modifier.width(5.dp)); Text("HEMEN OYNA",fontWeight=FontWeight.Black,fontSize=12.sp,maxLines=1,softWrap=false) }
            }
        }
    }
}

@Composable private fun MasterDailySeries(modifier:Modifier,growth:GrowthDashboardDto?,message:String,onClaim:()->Unit){
    val claimed = growth?.dailyClaimed == true
    Card(onClick=onClaim,modifier=modifier.height(154.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MasterPanel),border=BorderStroke(1.dp,MasterLine)){
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(sh("GÜNLÜK ÖDÜL","DAILY REWARD"),color=MasterInk,fontWeight=FontWeight.Black,fontSize=14.sp)
                Text(if(message.isNotBlank())message else if(claimed)sh("ALINDI","CLAIMED") else sh("HAZIR","READY"),color=if(claimed)MasterGreen else MasterBlue,fontSize=9.sp,fontWeight=FontWeight.Black)
            }
            Text(sh("Her gün giriş yaparak günlük ödülünü topla.","Sign in each day to collect your daily reward."),color=MasterMuted,fontSize=9.sp)
            Surface(shape=RoundedCornerShape(14.dp),color=MasterSky,border=BorderStroke(1.dp,MasterLine)){
                Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text(if(claimed)"✅" else "🎁",fontSize=27.sp)
                    Text(if(claimed)sh("Bugünkü ödül alındı","Today's reward claimed") else "+${growth?.dailyReward ?: 40} SC",color=MasterInk,fontWeight=FontWeight.Black,fontSize=13.sp)
                }
            }
        }
    }
}

@Composable private fun MasterSeasonCard(modifier:Modifier,onClick:()->Unit){
    Card(onClick=onClick,modifier=modifier.height(154.dp),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MasterPanel),border=BorderStroke(1.dp,MasterLine)){
        Column(Modifier.fillMaxSize().padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){
            Text(sh("LETHARA SEZONU","LETHARA SEASON"),color=MasterInk,fontWeight=FontWeight.Black,fontSize=13.sp)
            Text("🎁",fontSize=39.sp)
            Text(sh("Söz Dokusu sezon ilerlemeni ve ödüllerini aç","Open Word Weave season progress and rewards"),color=MasterMuted,fontSize=8.sp,textAlign=TextAlign.Center)
            Surface(shape=RoundedCornerShape(10.dp),color=MasterSky,border=BorderStroke(1.dp,MasterLine)){Text(sh("DETAYLAR ›","DETAILS ›"),Modifier.padding(horizontal=9.dp,vertical=5.dp),color=MasterBlue,fontSize=8.sp,fontWeight=FontWeight.Black)}
        }
    }
}

@Composable private fun MasterTopThree(leaders:List<LeaderboardV2Row>,onClick:()->Unit){Card(onClick=onClick,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MasterPanel),border=BorderStroke(1.dp,MasterLine)){Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("🏆 ${sh("HAFTANIN EN İYİ 3 OYUNCUSU","TOP 3 THIS WEEK")}",color=MasterInk,fontWeight=FontWeight.Black,fontSize=11.sp);Text(sh("CANLI","LIVE"),color=MasterGreen,fontWeight=FontWeight.Black,fontSize=8.sp)};Spacer(Modifier.height(7.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){leaders.take(3).forEachIndexed{i,r->Surface(Modifier.weight(1f),shape=RoundedCornerShape(14.dp),color=MasterSky){Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(listOf("🥇","🥈","🥉").getOrElse(i){"#${i+1}"},fontSize=19.sp);Text(r.displayName,color=MasterInk,fontWeight=FontWeight.Black,fontSize=9.sp,maxLines=1);Text("${r.wins}W • ${r.winRate.toInt()}%",color=MasterMuted,fontSize=7.sp)}}}}}}}

@Composable private fun MasterShortcut(icon:ImageVector,label:String,badge:Int,modifier:Modifier,onClick:()->Unit){Card(onClick=onClick,modifier=modifier.height(88.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MasterPanel),border=BorderStroke(1.dp,MasterLine)){Box(Modifier.fillMaxSize().padding(6.dp)){Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=MasterBlue,modifier=Modifier.size(25.dp));Spacer(Modifier.height(5.dp));Text(label,color=MasterInk,fontSize=7.5.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center,maxLines=2)};if(badge>0)Surface(Modifier.align(Alignment.TopEnd).size(20.dp),shape=CircleShape,color=Color(0xFFE65E67)){Box(contentAlignment=Alignment.Center){Text("$badge",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Black)}}}}}
