package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto

@Composable
internal fun PremiumHomeCommandDeck(profile: ProfileDto?, onProfile: () -> Unit, onSiege: () -> Unit, onTasks: () -> Unit, onCompetition: () -> Unit, onLeague: () -> Unit, onSocial: () -> Unit, onShop: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumIdentityStrip(profile, onProfile, onTasks)
        PremiumSiegeHero(onSiege)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PremiumCommandTile(Icons.Rounded.EmojiEvents, sh("LİG", "LEAGUE"), sh("Sıralamanı yükselt", "Climb the ranks"), SonHarfTheme.PremiumGold, Modifier.weight(1f), onLeague)
            PremiumCommandTile(Icons.Rounded.Groups, sh("SOSYAL", "SOCIAL"), sh("Rakiplerini bul", "Find rivals"), SonHarfTheme.Success, Modifier.weight(1f), onSocial)
            PremiumCommandTile(Icons.Rounded.Storefront, sh("MAĞAZA", "SHOP"), sh("Tarzını seç", "Choose your style"), SonHarfTheme.Primary, Modifier.weight(1f), onShop)
        }
        PremiumCompetitionBanner(onCompetition)
    }
}

@Composable private fun PremiumIdentityStrip(profile: ProfileDto?, onProfile: () -> Unit, onTasks: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(Modifier.fillMaxWidth().shadow(7.dp, shape).clickable(onClick = onProfile), shape, SonHarfTheme.Surface.copy(alpha=.97f), border=BorderStroke(1.dp, SonHarfTheme.Border)) {
        Row(Modifier.padding(horizontal=16.dp, vertical=13.dp), verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(SonHarfTheme.Primary, SonHarfTheme.Turquoise))), contentAlignment=Alignment.Center) { Text((profile?.displayName ?: "K").take(1).uppercase(), color=Color.White, fontWeight=FontWeight.Black, fontSize=20.sp) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(profile?.displayName ?: sh("KOMUTAN","COMMANDER"), color=SonHarfTheme.TextPrimary, fontWeight=FontWeight.Black, fontSize=16.sp); Text(sh("Kuşatma merkezine hoş geldin","Welcome to siege command"), color=SonHarfTheme.TextSecondary, fontSize=9.sp) }
            Surface(Modifier.size(42.dp).clickable(onClick=onTasks), CircleShape, SonHarfTheme.Primary.copy(alpha=.10f)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Rounded.Notifications, null, tint=SonHarfTheme.Primary, modifier=Modifier.size(21.dp)) } }
        }
    }
}

@Composable private fun PremiumSiegeHero(onClick: () -> Unit) {
    val shape=RoundedCornerShape(30.dp)
    Box(Modifier.fillMaxWidth().height(226.dp).shadow(14.dp,shape).clip(shape).background(Brush.linearGradient(listOf(SonHarfTheme.ForestDeep,SonHarfTheme.Forest,SonHarfTheme.Primary))).clickable(onClick=onClick).padding(22.dp)) {
        Column(Modifier.fillMaxHeight().fillMaxWidth(.73f), verticalArrangement=Arrangement.SpaceBetween) {
            Column { Surface(shape=RoundedCornerShape(50), color=Color.White.copy(alpha=.13f)) { Text(sh("ANA OYUN • TAKTİK ALAN SAVAŞI","MAIN GAME • TACTICAL TERRITORY BATTLE"), Modifier.padding(horizontal=11.dp,vertical=6.dp), color=Color.White.copy(alpha=.92f), fontSize=8.sp, fontWeight=FontWeight.Bold) }; Spacer(Modifier.height(12.dp)); Text(sh("KELİME\nKUŞATMASI","WORD\nSIEGE"), color=Color.White, fontWeight=FontWeight.Black, fontSize=29.sp, lineHeight=28.sp); Spacer(Modifier.height(7.dp)); Text(sh("Kelimeyi kur. Alanı ele geçir.\nRakibinin haritasını kuşat.","Build the word. Capture territory.\nSiege your rival's map."), color=Color.White.copy(alpha=.80f), fontSize=10.sp, lineHeight=14.sp) }
            Surface(shape=RoundedCornerShape(17.dp), color=Color.White) { Row(Modifier.padding(horizontal=19.dp,vertical=11.dp), verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Rounded.PlayArrow,null,tint=SonHarfTheme.ForestDeep,modifier=Modifier.size(21.dp)); Spacer(Modifier.width(6.dp)); Text(sh("OYNA","PLAY"),color=SonHarfTheme.ForestDeep,fontWeight=FontWeight.Black,fontSize=14.sp) } }
        }
        Box(Modifier.align(Alignment.CenterEnd).size(92.dp).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha=.09f)), contentAlignment=Alignment.Center) { Icon(Icons.Rounded.GridView,null,tint=Color.White.copy(alpha=.88f),modifier=Modifier.size(53.dp)) }
    }
}

@Composable private fun PremiumCommandTile(icon:ImageVector,title:String,subtitle:String,accent:Color,modifier:Modifier,onClick:()->Unit) { Surface(modifier.height(100.dp).clickable(onClick=onClick),RoundedCornerShape(21.dp),SonHarfTheme.Surface.copy(alpha=.97f),border=BorderStroke(1.dp,SonHarfTheme.Border),shadowElevation=2.dp) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { Icon(icon,null,tint=accent,modifier=Modifier.size(23.dp)); Text(title,color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Black,fontSize=11.sp); Text(subtitle,color=SonHarfTheme.TextSecondary,fontSize=7.5.sp,lineHeight=9.sp) } } }

@Composable private fun PremiumCompetitionBanner(onClick:()->Unit) { Surface(Modifier.fillMaxWidth().clickable(onClick=onClick),RoundedCornerShape(22.dp),SonHarfTheme.Surface.copy(alpha=.97f),border=BorderStroke(1.dp,SonHarfTheme.PremiumGold.copy(alpha=.32f)),shadowElevation=3.dp) { Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically) { Surface(shape=CircleShape,color=SonHarfTheme.PremiumGold.copy(alpha=.15f)) { Icon(Icons.Rounded.Bolt,null,tint=SonHarfTheme.PremiumGold,modifier=Modifier.padding(10.dp).size(22.dp)) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(sh("REKABET MERKEZİ","COMPETITION HUB"),color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Black,fontSize=12.sp); Text(sh("Turnuvalar • rövanşlar • haftalık hedefler","Tournaments • rematches • weekly goals"),color=SonHarfTheme.TextSecondary,fontSize=8.5.sp) }; Icon(Icons.Rounded.ChevronRight,null,tint=SonHarfTheme.Primary) } } }
