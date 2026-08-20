package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboard

private val UIBg = Color(0xFF020711)
private val UIPanel = Color(0xFF07111E)
private val UIPanel2 = Color(0xFF0B1627)
private val UIStroke = Color(0xFF1A2B43)
private val UIText = Color(0xFFF7F9FF)
private val UIMuted = Color(0xFF8995AA)
private val UICyan = Color(0xFF20C7FF)
private val UIPurple = Color(0xFF7B37FF)
private val UIPink = Color(0xFFFF3B7E)
private val UIGold = Color(0xFFFFC247)
private val UIGreen = Color(0xFF2DDB7D)

enum class ProductionScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

@Composable
fun ProductionSonHarfApp() {
    var screen by remember { mutableStateOf(ProductionScreen.HOME) }
    Scaffold(
        containerColor = UIBg,
        bottomBar = { if (screen != ProductionScreen.LEADERBOARD) ProductionBottomBar(screen) { screen = it } }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).background(Brush.verticalGradient(listOf(Color(0xFF06101D), UIBg, Color(0xFF01040A))))) {
            when (screen) {
                ProductionScreen.HOME -> ProductionHome({ screen = ProductionScreen.GAME }, { screen = ProductionScreen.LEADERBOARD })
                ProductionScreen.GAME -> ReferenceGameEntry()
                ProductionScreen.SHOP -> ProductionShop()
                ProductionScreen.PROFILE -> ProductionProfileScreen()
                ProductionScreen.MORE -> ProductionSettings()
                ProductionScreen.LEADERBOARD -> ProductionLeaderboard { screen = ProductionScreen.HOME }
            }
        }
    }
}

@Composable
private fun ProductionBottomBar(screen: ProductionScreen, onChange: (ProductionScreen) -> Unit) {
    val context = LocalContext.current
    NavigationBar(containerColor = Color(0xFF06101C), tonalElevation = 0.dp, modifier = Modifier.height(66.dp)) {
        listOf(
            Triple(ProductionScreen.HOME, "⌂", "Ana Sayfa"), Triple(ProductionScreen.GAME, "⚔", "Oyna"),
            Triple(ProductionScreen.SHOP, "▱", "Mağaza"), Triple(ProductionScreen.PROFILE, "♙", "Profil"),
            Triple(ProductionScreen.MORE, "•••", "Daha Fazla")
        ).forEach { (target, icon, label) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { SonHarfSoundFx.tap(); SonHarfPreferences.hapticTap(context); onChange(target) },
                icon = { Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(if (screen == target) UIPurple.copy(alpha=.20f) else Color.Transparent), contentAlignment = Alignment.Center) { Text(icon, color = if (screen == target) UICyan else UIMuted, fontSize = 20.sp, fontWeight = FontWeight.Black) } },
                label = { Text(label, fontSize = 8.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedTextColor = UIText, unselectedTextColor = UIMuted)
            )
        }
    }
}

@Composable
private fun ProductionHome(onPlay: () -> Unit, onLeaderboard: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var leaders by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var weekLanguage by remember { mutableStateOf("tr") }
    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
        leaders = runCatching { b.getLeaderboard(3).map { it.profile } }.getOrDefault(emptyList())
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal=14.dp, vertical=10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallAvatar(profile?.displayName ?: "O", UIGold)
                    Spacer(Modifier.width(8.dp))
                    Column { Text(profile?.displayName ?: "Oyuncu", fontSize=12.sp, fontWeight=FontWeight.Black); Text("♥ Elmas: ${profile?.diamonds ?: 0}  💎", color=UIMuted, fontSize=8.sp) }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { TopIcon("♙"); TopIcon("♜"); TopIcon("⚙") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SON", fontSize=34.sp, fontWeight=FontWeight.Black)
                Text("HARF", fontSize=34.sp, fontWeight=FontWeight.Black)
                Text("GERÇEK ZAMANLI KELİME DÜELLOSU", fontSize=9.sp, fontWeight=FontWeight.Bold)
            }
        }
        item {
            Button(onClick=onPlay, modifier=Modifier.fillMaxWidth().height(56.dp), shape=RoundedCornerShape(16.dp), colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent), contentPadding=PaddingValues(0.dp)) {
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF28A8FF), Color(0xFF4A54FF), Color(0xFF8F23E8)))), contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("DÜELLOYA GİR", fontSize=18.sp, fontWeight=FontWeight.Black); Text("Rastgele rakip bul", fontSize=8.sp) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ActionCard("👥", "ARKADAŞLAR", "Çevrimiçi oyuncular", UICyan, Modifier.weight(1f), onPlay)
                ActionCard("♛", "ÖZEL ODA", "VIP oluştur / Katıl", UIPurple, Modifier.weight(1f), onPlay)
            }
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=UIPanel), shape=RoundedCornerShape(14.dp), border=BorderStroke(1.dp,UIStroke)) {
                Row(Modifier.fillMaxWidth().padding(vertical=10.dp), horizontalArrangement=Arrangement.SpaceEvenly) {
                    MiniStat("3 × 10","KELİME"); MiniStat("45 sn","SÜRE"); MiniStat("3","ROUND"); MiniStat("TR / EN","DİL")
                }
            }
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=UIPanel), shape=RoundedCornerShape(14.dp), border=BorderStroke(1.dp,UIStroke)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                        Text("HAFTANIN EN İYİLERİ", fontSize=11.sp, fontWeight=FontWeight.Black)
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected=weekLanguage=="tr",onClick={weekLanguage="tr"},label={Text("🇹🇷 TR",fontSize=8.sp)})
                            FilterChip(selected=weekLanguage=="en",onClick={weekLanguage="en"},label={Text("🇬🇧 EN",fontSize=8.sp)})
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(7.dp)) { repeat(3) { i -> TopPlayerCard(i,leaders.getOrNull(i),Modifier.weight(1f)) } }
                    Button(onClick=onLeaderboard, modifier=Modifier.fillMaxWidth().height(42.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF263ACB)), shape=RoundedCornerShape(12.dp)) { Text("TÜM LİDERLİK TABLOSU",fontSize=10.sp,fontWeight=FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun ProductionLeaderboard(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var rows by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var language by remember { mutableStateOf("tr") }
    var period by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { rows = runCatching { backend?.getLeaderboard(50)?.map { it.profile } ?: emptyList() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(14.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { Row(verticalAlignment=Alignment.CenterVertically) { TextButton(onClick=onBack){Text("‹",fontSize=30.sp,color=UIPurple)}; Text("LİDERLİK TABLOSU",fontSize=22.sp,fontWeight=FontWeight.Black) } }
        item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(UIPanel2).padding(4.dp)) { TabButton("🇹🇷 TÜRKÇE",language=="tr",Modifier.weight(1f)){language="tr"}; TabButton("🇬🇧 ENGLISH",language=="en",Modifier.weight(1f)){language="en"} } }
        item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(UIPanel2).padding(4.dp)) { listOf("Toplam","Bu Hafta","Bu Ay").forEachIndexed { i,t -> TabButton(t,period==i,Modifier.weight(1f)){period=i} } } }
        item { Row(Modifier.fillMaxWidth().padding(horizontal=8.dp)) { Text("#",Modifier.width(40.dp),color=UIMuted,fontSize=9.sp); Text("Oyuncu",Modifier.weight(1f),color=UIMuted,fontSize=9.sp); Text("Galibiyet",Modifier.width(72.dp),color=UIMuted,fontSize=9.sp,textAlign=TextAlign.Center); Text("Kazanma %",Modifier.width(72.dp),color=UIMuted,fontSize=9.sp,textAlign=TextAlign.Center) } }
        itemsIndexed(rows) { index,p ->
            val m=p.wins+p.losses; val wr=if(m==0)0 else p.wins*100/m
            Card(colors=CardDefaults.cardColors(containerColor=UIPanel),shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,if(index==0)UIGold.copy(alpha=.45f) else UIStroke)) {
                Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(index<3) listOf("♛","♜","♝")[index] else "${index+1}",Modifier.width(40.dp),color=if(index==0)UIGold else UIMuted,textAlign=TextAlign.Center)
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically){SmallAvatar(p.displayName,if(index==0)UIGold else UIPurple,30.dp);Spacer(Modifier.width(8.dp));Text(p.displayName,fontWeight=FontWeight.Bold,fontSize=12.sp,maxLines=1)}
                    Text(p.wins.toString(),Modifier.width(72.dp),textAlign=TextAlign.Center,fontSize=12.sp);Text("%$wr",Modifier.width(72.dp),textAlign=TextAlign.Center,fontSize=12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProductionShop() {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("MAĞAZA",fontSize=27.sp,fontWeight=FontWeight.Black);Text("Google Play Billing doğrulanmadan gerçek satın alma açılmaz.",color=UIMuted,fontSize=10.sp) }
        itemsIndexed(listOf("💎 Elmas paketleri","🎨 Premium temalar","✨ Emoji & kozmetik","♛ VIP üyelik")) { _,item -> Card(colors=CardDefaults.cardColors(containerColor=UIPanel),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,UIStroke)){Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(item,fontWeight=FontWeight.Bold);Text("YAKINDA",color=UIGold,fontSize=10.sp)}} }
    }
}

@Composable
private fun ProductionSettings() {
    val context=LocalContext.current
    var sound by remember{mutableStateOf(SonHarfPreferences.soundEnabled(context))}
    var vibration by remember{mutableStateOf(SonHarfPreferences.vibrationEnabled(context))}
    var notifications by remember{mutableStateOf(SonHarfPreferences.notificationsEnabled(context))}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item { Text("AYARLAR",fontSize=27.sp,fontWeight=FontWeight.Black) }
        item { SettingsCard("SES AYARLARI", listOf(
            Triple("Ses Efektleri",sound){v:Boolean->sound=v;SonHarfPreferences.setSoundEnabled(context,v)},
            Triple("Müzik",false){_:Boolean->},
            Triple("Titreşim",vibration){v:Boolean->vibration=v;SonHarfPreferences.setVibrationEnabled(context,v)}
        )) }
        item { SettingsCard("BİLDİRİMLER", listOf(
            Triple("Oyun Davetleri",notifications){v:Boolean->notifications=v;SonHarfPreferences.setNotificationsEnabled(context,v)},
            Triple("Arkadaşlık İstekleri",notifications){v:Boolean->notifications=v;SonHarfPreferences.setNotificationsEnabled(context,v)},
            Triple("Sistem Bildirimleri",notifications){v:Boolean->notifications=v;SonHarfPreferences.setNotificationsEnabled(context,v)}
        )) }
        item { Card(colors=CardDefaults.cardColors(containerColor=UIPanel),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,UIStroke)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){Text("DİĞER",color=UICyan,fontSize=11.sp,fontWeight=FontWeight.Black);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Dil");Text("Türkçe  ›",color=UIMuted)};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Engellenenler");Text("›",color=UIMuted)}}} }
    }
}

@Composable private fun SettingsCard(title:String,rows:List<Triple<String,Boolean,(Boolean)->Unit>>) { Card(colors=CardDefaults.cardColors(containerColor=UIPanel),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,UIStroke)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,color=UICyan,fontSize=11.sp,fontWeight=FontWeight.Black);rows.forEach{(label,checked,onChange)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(label);Switch(checked,onChange)}}}} }
@Composable private fun TabButton(text:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){Button(onClick=onClick,modifier=modifier,colors=ButtonDefaults.buttonColors(containerColor=if(selected)Color(0xFF3944D7) else Color.Transparent),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=4.dp)){Text(text,fontSize=9.sp)}}
@Composable private fun SmallAvatar(name:String,accent:Color,size:androidx.compose.ui.unit.Dp=36.dp){Surface(modifier=Modifier.size(size),shape=CircleShape,color=UIPanel2,border=BorderStroke(1.dp,accent)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),fontWeight=FontWeight.Black)}}}
@Composable private fun TopIcon(icon:String){Surface(shape=CircleShape,color=UIPanel2,border=BorderStroke(1.dp,UIStroke)){Text(icon,Modifier.padding(8.dp),fontSize=12.sp)}}
@Composable private fun MiniStat(value:String,label:String){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontWeight=FontWeight.Black,fontSize=11.sp);Text(label,color=UIMuted,fontSize=7.sp)}}
@Composable private fun ActionCard(icon:String,title:String,subtitle:String,accent:Color,modifier:Modifier,onClick:()->Unit){Card(onClick=onClick,modifier=modifier,colors=CardDefaults.cardColors(containerColor=UIPanel),shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,accent.copy(alpha=.35f))){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=20.sp);Spacer(Modifier.width(8.dp));Column{Text(title,fontSize=10.sp,fontWeight=FontWeight.Black);Text(subtitle,color=UIMuted,fontSize=7.sp)}}}}
@Composable private fun TopPlayerCard(index:Int,p:ProfileDto?,modifier:Modifier){Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=UIPanel2),shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,if(index==0)UIGold.copy(alpha=.5f) else UIStroke)){Column(Modifier.fillMaxWidth().padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(listOf("♛","♜","♝")[index],color=if(index==0)UIGold else UIMuted);SmallAvatar(p?.displayName ?: "-",if(index==0)UIGold else UIPurple,34.dp);Text(p?.displayName ?: "—",fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1);Text("${p?.wins ?: 0} GALİBİYET",fontSize=6.sp,color=UIMuted)}}}
