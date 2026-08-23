package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Premium word-and-knowledge lobby. Gameplay/navigation contracts stay unchanged. */
@Composable
internal fun HomeLobby(onQuickPlay: () -> Unit, onSonHarf: () -> Unit, onBilBakalim: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlayerHeader()
        LeagueCard()
        LiveStrip()
        GameHeroCard("SON HARF", "Kelime Arenası", "KALEM  →  MASA", "🔥 4 galibiyet serisi", PortalBlue, Icons.Rounded.Link, onSonHarf)
        GameHeroCard("BİL BAKALIM", "Bilgi Düellosu", "?  Bugünün meydan okuması", "⏱ En hızlı cevap: 6,4 sn", Color(0xFF66CBEF), Icons.Rounded.Psychology, onBilBakalim)
        DailyStreak()
        SeasonCard()
        WeeklyTopThree()
        QuickActions(onQuickPlay)
        BottomNav()
    }
}

@Composable private fun PlayerHeader() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = PortalBlue.copy(.14f), border = BorderStroke(2.dp, PortalBlue.copy(.35f))) {
            Icon(Icons.Rounded.Person, null, tint = PortalBlue, modifier = Modifier.padding(10.dp).size(28.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Ümit", color = PortalText, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Seviye 3  •  1.084 XP", color = PortalMuted, fontSize = 10.sp)
            LinearProgressIndicator(progress = { .62f }, modifier = Modifier.width(112.dp).height(5.dp).clip(CircleShape), color = PortalBlue, trackColor = PortalBlue.copy(.12f))
        }
        ResourcePill("🪙", "1.240")
        Spacer(Modifier.width(6.dp))
        ResourcePill("💎", "35")
    }
}

@Composable private fun ResourcePill(icon:String, value:String) = Surface(shape=RoundedCornerShape(14.dp), color=Color.White, border=BorderStroke(1.dp, Color(0xFFD9EEF8))) {
    Text("$icon $value", Modifier.padding(horizontal=9.dp, vertical=7.dp), color=PortalText, fontSize=10.sp, fontWeight=FontWeight.Bold)
}

@Composable private fun LeagueCard() {
    Surface(shape=RoundedCornerShape(20.dp), color=Color.White, border=BorderStroke(1.dp, PortalBlue.copy(.28f)), shadowElevation=2.dp) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=CircleShape, color=Color(0xFFEAF8FF)) { Icon(Icons.Rounded.EmojiEvents,null,tint=Color(0xFFB87845),modifier=Modifier.padding(10.dp).size(27.dp)) }
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) {
                Text("BRONZ II", color=PortalText, fontWeight=FontWeight.Black, fontSize=15.sp)
                Text("2.150 Rating  •  İlk 3'e 86 puan", color=PortalMuted, fontSize=9.sp)
                LinearProgressIndicator(progress={.74f}, modifier=Modifier.fillMaxWidth().padding(top=5.dp).height(6.dp).clip(CircleShape), color=PortalBlue, trackColor=Color(0xFFE6F4FA))
            }
        }
    }
}

@Composable private fun LiveStrip() {
    Surface(shape=RoundedCornerShape(15.dp), color=Color(0xFFEAF8FF), border=BorderStroke(1.dp, PortalBlue.copy(.22f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement=Arrangement.SpaceEvenly) {
            Text("🔥 Seri: 4", color=PortalText, fontSize=10.sp, fontWeight=FontWeight.Bold)
            Text("•", color=PortalMuted)
            Text("🏆 Turnuva: 18 dk", color=PortalText, fontSize=10.sp, fontWeight=FontWeight.Bold)
            Text("•", color=PortalMuted)
            Text("⚔ Ezeli rakip", color=PortalText, fontSize=10.sp, fontWeight=FontWeight.Bold)
        }
    }
}

@Composable private fun GameHeroCard(title:String, subtitle:String, visual:String, status:String, accent:Color, icon:ImageVector, onClick:()->Unit) {
    Card(onClick=onClick, shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.Transparent), border=BorderStroke(1.dp, accent.copy(.35f))) {
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color.White, accent.copy(.10f), Color.White))).padding(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Surface(shape=RoundedCornerShape(18.dp), color=accent.copy(.14f)) { Icon(icon,null,tint=accent,modifier=Modifier.padding(13.dp).size(34.dp)) }
                Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) {
                    Text(title, color=PortalText, fontSize=21.sp, fontWeight=FontWeight.Black)
                    Text(subtitle, color=accent, fontSize=10.sp, fontWeight=FontWeight.Bold)
                    Text(visual, color=PortalText, fontSize=13.sp, fontWeight=FontWeight.ExtraBold, modifier=Modifier.padding(top=7.dp))
                    Text(status, color=PortalMuted, fontSize=9.sp, modifier=Modifier.padding(top=3.dp))
                }
                Surface(shape=RoundedCornerShape(16.dp), color=accent) { Text("OYNA", Modifier.padding(horizontal=14.dp,vertical=12.dp), color=Color.White, fontWeight=FontWeight.Black, fontSize=11.sp) }
            }
        }
    }
}

@Composable private fun DailyStreak() {
    Surface(shape=RoundedCornerShape(20.dp), color=Color.White, border=BorderStroke(1.dp,Color(0xFFD9EEF8))) { Column(Modifier.padding(13.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ Text("🔥 GÜNLÜK SERİ",color=PortalText,fontWeight=FontWeight.Black); Text("4 gün",color=PortalBlue,fontWeight=FontWeight.Bold,fontSize=11.sp) }
        Spacer(Modifier.height(9.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { (1..7).forEach { day ->
            val active=day<=4; Surface(shape=CircleShape,color=if(active) PortalBlue else Color(0xFFEAF2F6)) { Text(if(active) "✓" else "$day",Modifier.size(31.dp).wrapContentSize(),color=if(active) Color.White else PortalMuted,fontWeight=FontWeight.Black,fontSize=10.sp) }
        }}
        Text("7. gün: 🎁 Style ödülü",color=PortalMuted,fontSize=9.sp,modifier=Modifier.padding(top=8.dp))
    }}
}

@Composable private fun SeasonCard() {
    Surface(shape=RoundedCornerShape(20.dp), color=Color(0xFFEAF8FF), border=BorderStroke(1.dp,PortalBlue.copy(.25f))) { Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("🎁",fontSize=29.sp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){ Text("SEZON 12",color=PortalText,fontWeight=FontWeight.Black); Text("Sonraki ödüle 1 maç",color=PortalMuted,fontSize=9.sp); LinearProgressIndicator(progress={.68f},modifier=Modifier.fillMaxWidth().padding(top=5.dp).height(6.dp).clip(CircleShape),color=PortalBlue,trackColor=Color.White) }
    }}
}

@Composable private fun WeeklyTopThree() {
    Surface(shape=RoundedCornerShape(20.dp),color=Color.White,border=BorderStroke(1.dp,Color(0xFFD9EEF8))) { Column(Modifier.padding(13.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ Text("🏆 HAFTANIN EN İYİ 3 OYUNCUSU",color=PortalText,fontWeight=FontWeight.Black,fontSize=12.sp); Text("CANLI",color=PortalGreen,fontSize=9.sp,fontWeight=FontWeight.Black) }
        Spacer(Modifier.height(9.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            TopPlayer("🥇","Ayaz","2.980",Modifier.weight(1f)); TopPlayer("🥈","Ece","2.740",Modifier.weight(1f)); TopPlayer("🥉","Mert","2.610",Modifier.weight(1f))
        }
    }}
}

@Composable private fun TopPlayer(medal:String,name:String,rating:String,modifier:Modifier){ Surface(modifier,shape=RoundedCornerShape(14.dp),color=Color(0xFFF5FBFF)){Column(Modifier.padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(medal,fontSize=20.sp);Text(name,color=PortalText,fontWeight=FontWeight.Black,fontSize=11.sp);Text("$rating ⭐",color=PortalMuted,fontSize=8.sp)}} }

@Composable private fun QuickActions(onQuickPlay:()->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        QuickTile("🎯","Görevler",Modifier.weight(1f),onQuickPlay); QuickTile("🎁","Ödül",Modifier.weight(1f),onQuickPlay); QuickTile("🏆","Ligler",Modifier.weight(1f),onQuickPlay); QuickTile("🛍","Style",Modifier.weight(1f),onQuickPlay)
    }
}
@Composable private fun QuickTile(icon:String,label:String,modifier:Modifier,onClick:()->Unit){ Card(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Color(0xFFD9EEF8))){Column(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon,fontSize=18.sp);Text(label,color=PortalText,fontSize=8.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)}} }

@Composable private fun BottomNav(){ NavigationBar(containerColor=Color.White,tonalElevation=0.dp){ listOf("Ana Sayfa" to Icons.Rounded.Home,"Oyna" to Icons.Rounded.SportsEsports,"Arena" to Icons.Rounded.EmojiEvents,"Sosyal" to Icons.Rounded.Group,"Profil" to Icons.Rounded.Person).forEachIndexed{i,item->NavigationBarItem(selected=i==0,onClick={},icon={Icon(item.second,item.first)},label={Text(item.first,fontSize=8.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=PortalBlue,indicatorColor=PortalBlue.copy(.12f))) } } }
