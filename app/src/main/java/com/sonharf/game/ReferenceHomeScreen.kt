package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Approval preview for the reference home screen.
 * This is real production Compose code; the debug preview activity only provides
 * deterministic sample data so the rendered result can be reviewed before merge.
 */
@Composable
fun ReferenceHomeScreen(
    modifier: Modifier = Modifier,
    name: String = "Ümit",
    rating: Int = 612,
    coin: Int = 1110,
    wins: Int = 1,
    losses: Int = 29,
    pro: Boolean = true,
) {
    val bg = Color(0xFFF8FAF4)
    val text = Color(0xFF173A2E)
    val muted = Color(0xFF6B7F74)
    val sage = Color(0xFF4F806B)
    val forest = Color(0xFF0F5A43)
    val gold = Color(0xFFD8AE55)

    Box(modifier.fillMaxSize().background(bg)) {
        BotanicalReferenceBackdrop(Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 17.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.Top) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.son_harf_app_icon_master),
                        contentDescription = "Son Harf",
                        modifier = Modifier.size(76.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Column(Modifier.padding(start = 5.dp, top = 6.dp)) {
                        Text(
                            "Son Harf",
                            color = Color(0xFF2B5E48),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = (-0.8).sp,
                        )
                        Text(
                            "Kelimeyi Sürdür, Rakibini Geç",
                            color = sage,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleAction(Icons.Rounded.Notifications)
                        CircleAction(Icons.Rounded.WorkspacePremium)
                    }
                    Text(
                        "Her kelime\nyeni bir meydan okuma!",
                        color = Color(0xFF315D49),
                        fontSize = 7.5.sp,
                        lineHeight = 8.sp,
                        textAlign = TextAlign.End,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp, end = 1.dp),
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .shadow(10.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4E806C), Color(0xFF4D7480), Color(0xFF6B6B8B))))
                    .border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(25.dp)),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(alpha=.035f), size.minDimension*.45f, Offset(size.width*.92f,size.height*.74f))
                    drawCircle(Color(0xFF173A2E).copy(alpha=.06f), size.minDimension*.34f, Offset(size.width*.08f,size.height*.92f))
                }
                Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFDFF8EF), border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF8EEBD9))) {
                            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                                Text("Ü", color = Color(0xFF285E4D), fontSize = 25.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFF4C84F), modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("$rating RP", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFFFFCF3)) {
                            Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF8A6430), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("SC $coin", color = Color(0xFF795628), fontSize = 15.sp, fontWeight = FontWeight.Black)
                                Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF90744B), modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Metric("$wins", "GALİBİYET", Modifier.weight(1f))
                        DividerMetric()
                        Metric("$losses", "MAĞLUBİYET", Modifier.weight(1f))
                        DividerMetric()
                        Metric(if (pro) "PRO" else "FREE", "ÜYELİK", Modifier.weight(1f))
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .shadow(7.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1B654D), forest))),
                contentAlignment = Alignment.Center,
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(33.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("OYNA", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("Premier 1v1 kelime düellosu", color = Color.White.copy(alpha = .86f), fontSize = 9.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = .88f), modifier = Modifier.size(26.dp))
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .shadow(9.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF103E34), Color(0xFF164C41), Color(0xFF173941))))
                    .border(1.dp, gold.copy(alpha=.60f), RoundedCornerShape(25.dp)),
            ) {
                PremiumPodiumGlow(Modifier.matchParentSize())
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(13.dp), color = gold.copy(alpha=.14f), border = androidx.compose.foundation.BorderStroke(1.dp,gold.copy(alpha=.32f))) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFF0CF70), modifier = Modifier.padding(8.dp).size(21.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("HAFTANIN ZİRVESİ", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text("Bu haftanın en güçlü oyuncuları", color = Color.White.copy(alpha=.68f), fontSize = 8.5.sp)
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha=.05f), border = androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=.14f))) {
                            Row(Modifier.padding(horizontal=10.dp,vertical=6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("TÜMÜ",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold)
                                Icon(Icons.Rounded.ChevronRight,null,tint=gold,modifier=Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                        PodiumPlace(2,"KaanY","498 RP",Color(0xFFD8E3EA),112.dp,Modifier.weight(.92f))
                        PodiumPlace(1,"WordMaster","732 RP",Color(0xFFF5CF62),139.dp,Modifier.weight(1.16f),champion=true)
                        PodiumPlace(3,"LaleS","410 RP",Color(0xFFD99A6D),105.dp,Modifier.weight(.92f))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("DİĞER OYUNLAR", color = text, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("Daha fazla kelime, daha fazla eğlence!", color = muted, fontSize = 8.dp.value.sp)
            }
            Row(Modifier.fillMaxWidth().height(103.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GameCard(R.drawable.kelime_kusatma_logo_hd,"KELİME\nKUŞATMASI","Alanı ele geçir,\nküpleri koru", listOf(Color(0xFFC8E7DA),Color(0xFFF0EFD9)),Modifier.weight(1f))
                GameCard(R.drawable.harf_yolu_logo,"KELİME YOLU","Her kelime\nseni hedefe yaklaştırır", listOf(Color(0xFFC8E7EE),Color(0xFFE6F2E6)),Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))
            BottomNavRow()
        }
    }
}

@Composable private fun CircleAction(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = CircleShape, color = Color(0xFFFBFCF7), border = androidx.compose.foundation.BorderStroke(1.dp,Color(0xFFBBCDC1))) {
        Box(Modifier.size(47.dp),contentAlignment=Alignment.Center){ Icon(icon,null,tint=Color(0xFF224C3B),modifier=Modifier.size(20.dp)) }
    }
}
@Composable private fun Metric(value:String,label:String,modifier:Modifier){ Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){ Text(value,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Black); Text(label,color=Color.White.copy(alpha=.75f),fontSize=7.5.sp,fontWeight=FontWeight.Bold) } }
@Composable private fun DividerMetric(){ Box(Modifier.width(1.dp).height(35.dp).background(Color.White.copy(alpha=.16f))) }

@Composable private fun PremiumPodiumGlow(modifier:Modifier){ Canvas(modifier){
    drawCircle(Brush.radialGradient(listOf(Color(0x55F6D375),Color.Transparent)),radius=size.minDimension*.44f,center=Offset(size.width*.5f,size.height*.72f))
    listOf(.10f to .38f,.18f to .52f,.28f to .31f,.40f to .43f,.62f to .31f,.74f to .44f,.86f to .34f,.92f to .52f).forEachIndexed{ i,p->drawCircle(if(i%2==0)Color(0xFFEBC85C) else Color(0xFFF6E6A1),if(i%3==0)2.6f else 1.7f,Offset(size.width*p.first,size.height*p.second))}
} }

@Composable private fun PodiumPlace(place:Int,name:String,rp:String,accent:Color,height:androidx.compose.ui.unit.Dp,modifier:Modifier,champion:Boolean=false){
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom){
        if(champion){ Icon(Icons.Rounded.WorkspacePremium,null,tint=accent,modifier=Modifier.size(23.dp)); Spacer(Modifier.height(1.dp)) }
        Surface(shape=CircleShape,color=Color(0xFF153A32),border=androidx.compose.foundation.BorderStroke(if(champion)2.5.dp else 2.dp,accent),shadowElevation=5.dp){ Box(Modifier.size(if(champion)48.dp else 40.dp),contentAlignment=Alignment.Center){ Icon(Icons.Rounded.Person,null,tint=Color.White.copy(alpha=.75f),modifier=Modifier.size(if(champion)28.dp else 23.dp)) } }
        Spacer(Modifier.height(2.dp)); Text(name,color=Color.White,fontSize=if(champion)9.5.sp else 8.sp,fontWeight=FontWeight.Black); Text(rp,color=accent,fontSize=if(champion)9.sp else 7.5.sp,fontWeight=FontWeight.Black); Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(topStart=16.dp,topEnd=16.dp)).background(Brush.verticalGradient(listOf(accent.copy(alpha=.42f),accent.copy(alpha=.16f),Color.White.copy(alpha=.04f)))).border(1.dp,accent.copy(alpha=.6f),RoundedCornerShape(topStart=16.dp,topEnd=16.dp)),contentAlignment=Alignment.TopCenter){ Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(top=7.dp)){ Text("$place",color=accent,fontSize=if(champion)20.sp else 17.sp,fontWeight=FontWeight.Black); if(champion) Text("ŞAMPİYON",color=Color.White.copy(alpha=.82f),fontSize=6.sp,fontWeight=FontWeight.Black) } }
    }
}

@Composable private fun GameCard(res:Int,title:String,subtitle:String,colors:List<Color>,modifier:Modifier){
    Row(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(colors)).border(1.dp,Color.White.copy(alpha=.74f),RoundedCornerShape(18.dp)).padding(7.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.weight(.95f).fillMaxHeight().clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha=.35f)),contentAlignment=Alignment.Center){ Image(painterResource(res),null,Modifier.fillMaxSize().padding(2.dp),contentScale=ContentScale.Fit) }
        Spacer(Modifier.width(6.dp)); Column(Modifier.weight(1.05f)){ Text(title,color=Color(0xFF1B382E),fontSize=10.sp,lineHeight=10.5.sp,fontWeight=FontWeight.Black); Spacer(Modifier.height(3.dp)); Text(subtitle,color=Color(0xFF61766B),fontSize=6.5.sp,lineHeight=8.sp); Spacer(Modifier.height(5.dp)); Surface(shape=RoundedCornerShape(99.dp),color=Color(0xFFFFFDF6)){ Row(Modifier.padding(horizontal=7.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){ Text("Hemen Oyna",color=Color(0xFF355D4D),fontSize=6.sp,fontWeight=FontWeight.Black); Icon(Icons.Rounded.ChevronRight,null,tint=Color(0xFF8A6D38),modifier=Modifier.size(10.dp)) } } }
    }
}

@Composable private fun BottomNavRow(){
    Surface(shape=RoundedCornerShape(22.dp),color=Color(0xFFF1F5EF).copy(alpha=.98f),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFFDDE5DF))){
        Row(Modifier.fillMaxWidth().height(61.dp),verticalAlignment=Alignment.CenterVertically){
            listOf(Icons.Rounded.Home to "ANA",Icons.Rounded.EmojiEvents to "LİG",Icons.Rounded.Groups to "SOSYAL",Icons.Rounded.Storefront to "MAĞAZA",Icons.Rounded.Person to "PROFİL").forEachIndexed{index,item->
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                    if(index==0){ Surface(shape=RoundedCornerShape(99.dp),color=Color(0xFFDDE9E1)){ Icon(item.first,null,tint=Color(0xFF3F745E),modifier=Modifier.padding(horizontal=16.dp,vertical=5.dp).size(21.dp)) } } else Icon(item.first,null,tint=Color(0xFF657A70),modifier=Modifier.size(20.dp))
                    Text(item.second,color=if(index==0)Color(0xFF3F745E) else Color(0xFF657A70),fontSize=7.sp,fontWeight=if(index==0)FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable private fun BotanicalReferenceBackdrop(modifier:Modifier){ Canvas(modifier){
    drawRect(Brush.verticalGradient(listOf(Color(0xFFFFFEF8),Color(0xFFFBFCF6),Color(0xFFE3F2E6))))
    val wave=Path().apply{moveTo(0f,size.height*.69f);cubicTo(size.width*.18f,size.height*.61f,size.width*.58f,size.height*.86f,size.width,size.height*.72f);lineTo(size.width,size.height);lineTo(0f,size.height);close()}; drawPath(wave,Brush.verticalGradient(listOf(Color(0x1378BB98),Color(0x437FC3A0)),startY=size.height*.64f,endY=size.height))
    fun leaf(cx:Float,cy:Float,w:Float,h:Float,a:Float){ drawOval(Color(0xFF6AAA86).copy(alpha=a),Offset(size.width*cx-w/2,size.height*cy-h/2),androidx.compose.ui.geometry.Size(w,h)) }
    leaf(.03f,.13f,50f,20f,.10f);leaf(.08f,.17f,62f,24f,.08f);leaf(.73f,.025f,65f,25f,.10f);leaf(.96f,.22f,52f,20f,.08f);leaf(.03f,.58f,66f,23f,.08f);leaf(.91f,.62f,70f,25f,.09f);leaf(.18f,.93f,56f,21f,.08f)
} }
