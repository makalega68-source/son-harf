package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val SonHarfBg = Color(0xFF070A12)
internal val SonHarfSurface = Color(0xFF101727)
internal val SonHarfSurface2 = Color(0xFF172136)
internal val SonHarfPurple = Color(0xFF7A5AF8)
internal val SonHarfCyan = Color(0xFF42D7FF)
internal val SonHarfGold = Color(0xFFFFC857)
internal val SonHarfText = Color(0xFFF8FAFF)
internal val SonHarfMuted = Color(0xFF94A3B8)

enum class AppScreen { HOME, GAME, SHOP, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme(colorScheme = darkColorScheme(primary=SonHarfPurple,secondary=SonHarfCyan,tertiary=SonHarfGold,background=SonHarfBg,surface=SonHarfSurface,onBackground=SonHarfText,onSurface=SonHarfText)) { SonHarfApp() } } }
}

@Composable private fun SonHarfApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    Scaffold(containerColor=SonHarfBg,bottomBar={ NavigationBar(containerColor=Color(0xFF0B1020),tonalElevation=0.dp) { NavItem("⌂","Ana Sayfa",screen==AppScreen.HOME){screen=AppScreen.HOME}; NavItem("⚔","Oyna",screen==AppScreen.GAME){screen=AppScreen.GAME}; NavItem("◆","Mağaza",screen==AppScreen.SHOP){screen=AppScreen.SHOP}; NavItem("●","Profil",screen==AppScreen.PROFILE){screen=AppScreen.PROFILE} } }) { padding -> Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(Color(0xFF0B1020),SonHarfBg,Color(0xFF05070D))))) { when(screen){ AppScreen.HOME->HomeScreen{SonHarfSoundFx.softNotify();screen=AppScreen.GAME}; AppScreen.GAME->OnlineGameScreenV2(); AppScreen.SHOP->ShopScreen(); AppScreen.PROFILE->ProfileScreen() } } }
}

@Composable private fun RowScope.NavItem(icon:String,label:String,selected:Boolean,onClick:()->Unit){ NavigationBarItem(selected=selected,onClick={SonHarfSoundFx.tap();onClick()},icon={ Surface(color=if(selected) SonHarfPurple.copy(alpha=.2f) else Color.Transparent,shape=RoundedCornerShape(16.dp),border=if(selected) BorderStroke(1.dp,SonHarfPurple.copy(alpha=.28f)) else null){Text(icon,Modifier.padding(horizontal=14.dp,vertical=5.dp),fontSize=19.sp,color=if(selected) SonHarfCyan else SonHarfMuted)}},label={Text(label,fontSize=11.sp)},colors=NavigationBarItemDefaults.colors(selectedTextColor=SonHarfText,unselectedTextColor=SonHarfMuted,indicatorColor=Color.Transparent)) }

@Composable private fun HomeScreen(onPlay:()->Unit){ LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
 item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ Column{Text("SON HARF",fontSize=32.sp,fontWeight=FontWeight.Black,letterSpacing=1.8.sp);Text("Gerçek zamanlı kelime düellosu",color=SonHarfMuted,fontSize=13.sp)}; Surface(color=SonHarfCyan.copy(alpha=.08f),shape=RoundedCornerShape(999.dp),border=BorderStroke(1.dp,SonHarfCyan.copy(alpha=.25f))){Text("● ONLINE",Modifier.padding(horizontal=12.dp,vertical=8.dp),color=SonHarfCyan,fontWeight=FontWeight.Black,fontSize=11.sp)} } }
 item { Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(30.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent)){ Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF7657FF),Color(0xFF4E6DFF),Color(0xFF16B8D9)))).padding(22.dp)){ Column(verticalArrangement=Arrangement.spacedBy(13.dp)){ Surface(color=Color.White.copy(alpha=.15f),shape=RoundedCornerShape(999.dp)){Text("⚡ 1v1 ONLINE DÜELLO",Modifier.padding(horizontal=12.dp,vertical=6.dp),fontSize=11.sp,fontWeight=FontWeight.Black)}; Text("Son harfi yakala.\nSüreyi yönet. Kazan.",fontSize=29.sp,lineHeight=33.sp,fontWeight=FontWeight.Black); Text("3 round × 10 kelime • Türkçe veya English • rastgele rakip • arkadaş daveti • canlı sohbet",color=Color.White.copy(alpha=.84f),lineHeight=20.sp); Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF151C34))){Text("DÜELLOYA GİR",fontWeight=FontWeight.Black,letterSpacing=.8.sp)} } } } }
 item { Text("Hızlı Bakış",fontSize=18.sp,fontWeight=FontWeight.Bold) }
 item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){StatCard("⏱","45 sn","Hamle",Modifier.weight(1f));StatCard("⚔","3","Round",Modifier.weight(1f));StatCard("✦","Canlı","Sohbet",Modifier.weight(1f))} }
 item { Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,Color.White.copy(alpha=.05f))){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("NASIL OYNANIR?",color=SonHarfCyan,fontSize=11.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp);Text("Dilini seç. Rastgele bir rakip bul veya çevrimiçi arkadaşını davet et. Her round 10 geçerli kelimede tamamlanır. Her kelime öncekinin son harfiyle başlamalı; süre dolarsa sıra rakibine geçer.",color=SonHarfText,lineHeight=22.sp);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MiniPill("1  Dil");MiniPill("2  Rakip");MiniPill("3  3 Round")}}} }
} }

@Composable private fun StatCard(icon:String,value:String,title:String,modifier:Modifier=Modifier){Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.045f))){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(icon,fontSize=20.sp);Text(value,fontWeight=FontWeight.Black,fontSize=18.sp);Text(title,color=SonHarfMuted,fontSize=11.sp)}}}
@Composable private fun MiniPill(text:String){Surface(color=SonHarfSurface2,shape=RoundedCornerShape(999.dp)){Text(text,Modifier.padding(horizontal=10.dp,vertical=6.dp),color=SonHarfMuted,fontSize=11.sp)}}

@Composable private fun ShopScreen(){ val products=listOf(Triple("◆ 100","Başlangıç Elmas Paketi","Yakında"),Triple("◆ 500","Güçlü Elmas Paketi","Yakında"),Triple("◈ Tema","Premium Tema Paketi","Yakında"),Triple("☺ Emoji","Özel Emoji Paketi","Yakında")); LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Text("MAĞAZA",fontSize=30.sp,fontWeight=FontWeight.Black);Text("Hesabını kişiselleştir",color=SonHarfMuted)};item{Card(shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent)){Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFFFB547),Color(0xFFFF7A59)))).padding(20.dp)){Column(verticalArrangement=Arrangement.spacedBy(9.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("VIP",fontSize=30.sp,fontWeight=FontWeight.Black,color=Color(0xFF24150E));Text("♛",fontSize=28.sp,color=Color(0xFF24150E))};Text("Reklamsız deneyim • VIP rozeti • özel temalar • gelişmiş istatistik",color=Color(0xFF2C1B13));Surface(color=Color.Black.copy(alpha=.15f),shape=RoundedCornerShape(14.dp)){Text("Google Play doğrulaması tamamlanınca aktif olacak",Modifier.fillMaxWidth().padding(12.dp),textAlign=TextAlign.Center,fontWeight=FontWeight.Bold,color=Color(0xFF24150E))}}}}};item{Text("Paketler",fontSize=18.sp,fontWeight=FontWeight.Bold)};items(products){p->Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.045f))){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.first,color=SonHarfCyan,fontWeight=FontWeight.Black);Text(p.second,fontWeight=FontWeight.SemiBold);Text("Google Play ödeme aşamasında etkinleşecek",color=SonHarfMuted,fontSize=11.sp)};Surface(color=SonHarfSurface2,shape=RoundedCornerShape(999.dp)){Text(p.third,Modifier.padding(horizontal=10.dp,vertical=6.dp),color=SonHarfMuted,fontSize=11.sp)}}}}} }

@Composable private fun ProfileScreen(){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Text("PROFİL",fontSize=30.sp,fontWeight=FontWeight.Black);Text("Oyuncu kimliğin, gizliliğin ve istatistiklerin",color=SonHarfMuted)};item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(26.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.05f))){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Surface(color=SonHarfPurple.copy(alpha=.18f),shape=RoundedCornerShape(999.dp),border=BorderStroke(1.dp,SonHarfPurple.copy(alpha=.25f))){Text("SH",Modifier.padding(20.dp),fontSize=26.sp,fontWeight=FontWeight.Black,color=SonHarfCyan)};Text("OYUNCU PROFİLİ",fontSize=20.sp,fontWeight=FontWeight.Black);Text("Oyuncu adın ve maç istatistiklerin burada tutulur",color=SonHarfMuted,textAlign=TextAlign.Center)}}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){ProfileMetric("0","Galibiyet",Modifier.weight(1f));ProfileMetric("0","Mağlubiyet",Modifier.weight(1f));ProfileMetric("0","Elmas",Modifier.weight(1f))}};item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.045f))){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Profil fotoğrafı ve gizlilik",fontWeight=FontWeight.Bold);Text("Fotoğrafını gizli tutabilir, maç sırasında rakibine veya izin verdiğin kişilere açabilirsin.",color=SonHarfMuted)}}};item{Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.045f))){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("İstatistikler",fontWeight=FontWeight.Bold);Text("Galibiyet, mağlubiyet, seri ve kelime performansı online maçlardan hesaplanır.",color=SonHarfMuted)}}}}}
@Composable private fun ProfileMetric(value:String,label:String,modifier:Modifier){Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.045f))){Column(Modifier.fillMaxWidth().padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontWeight=FontWeight.Black,fontSize=20.sp);Text(label,color=SonHarfMuted,fontSize=10.sp)}}}
