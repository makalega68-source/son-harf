package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay

private data class LiveCombo(val n:Int,val title:String,val sub:String,val mark:String,val color:Color)
private fun liveCombo(n:Int):LiveCombo?=when(n){
    3->LiveCombo(n,"İSABET!","GÜZEL BAŞLADIN!","◎",SonHarfCyan)
    4->LiveCombo(n,"AFERİN!","RİTMİN YERİNE OTURDU!","ϟ",SonHarfGreen)
    5->LiveCombo(n,"MÜKEMMEL!","DURDURULAMIYORSUN!","★",SonHarfGold)
    6->LiveCombo(n,"SERİ KATİL!","KELİMELER SENİNLE!","◉",SonHarfPink)
    7->LiveCombo(n,"EFSANE!","BÖYLESİNİ AZ GÖRÜRÜZ!","♛",SonHarfPurple)
    8->LiveCombo(n,"HARİKASIN!","ZİRVEYE YAKLAŞTIN!","◆",SonHarfCyan)
    9->LiveCombo(n,"ŞOV ZAMANI!","RAKİBİNİ SOLLADIN!","↗",SonHarfGold)
    else->if(n>=10&&n%5==0)LiveCombo(n,"EFSANELER LİGİ!","SEN BİR KELİME USTASISIN!","🏆",SonHarfPink)else null
}

@Composable
fun OnlineGameScreenComboOverlayOnly(){
    if(!SupabaseProvider.configured)return
    val backend=remember{OnlineGameBackend()}
    var combo by remember{mutableStateOf<LiveCombo?>(null)}
    var show by remember{mutableStateOf(false)}
    var shownKey by remember{mutableStateOf<Pair<String,Int>?>(null)}
    LaunchedEffect(Unit){
        while(true){
            val me=backend.currentUserId()
            if(me!=null){
                val r=runCatching{SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>().filter{(it.hostId==me||it.guestId==me)&&it.status in listOf("playing","final","sudden_death")}.maxByOrNull{it.validWordCount}}.getOrNull()
                if(r!=null){
                    val streak=if(r.hostId==me)r.hostStreak else r.guestStreak
                    val c=liveCombo(streak);val key=r.id to streak
                    if(c!=null&&key!=shownKey){shownKey=key;combo=c;show=true;SonHarfSoundFx.softNotify();delay(1450);show=false;delay(220)}
                    if(streak<3)shownKey=null
                }else{show=false;shownKey=null}
            }
            delay(450)
        }
    }
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter){
        AnimatedVisibility(show&&combo!=null,modifier=Modifier.padding(horizontal=22.dp,vertical=92.dp),enter=fadeIn(tween(120))+scaleIn(initialScale=.7f,animationSpec=tween(180)),exit=fadeOut(tween(220))+scaleOut(targetScale=1.08f,animationSpec=tween(220))){
            val c=combo?:return@AnimatedVisibility
            Surface(color=Color(0xF20A1020),shape=RoundedCornerShape(26.dp),border=BorderStroke(2.dp,c.color.copy(alpha=.78f)),shadowElevation=16.dp){
                Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(c.color.copy(alpha=.05f),c.color.copy(alpha=.24f),c.color.copy(alpha=.05f)))).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(c.mark,color=c.color,fontSize=30.sp,fontWeight=FontWeight.Black)
                    Text(c.title,color=c.color,fontSize=30.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                    Text("${c.n} DOĞRU SERİ!",color=SonHarfText,fontSize=14.sp,fontWeight=FontWeight.Black)
                    Text(c.sub,color=SonHarfMuted,fontSize=11.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                }
            }
        }
    }
}
