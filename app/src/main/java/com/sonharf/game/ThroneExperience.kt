package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay

@Composable private fun serverNow(server:String,key:Any?):Long {
 var anchor by remember(key) { mutableStateOf(0L to 0L) }
 var now by remember(key) { mutableStateOf(0L) }
 LaunchedEffect(server,key) {
  val instant=tournamentTimeMillis(server)
  if(instant>0)anchor=instant to android.os.SystemClock.elapsedRealtime()
 }
 LaunchedEffect(key) {
  while(true){now=if(anchor.first>0)anchor.first+android.os.SystemClock.elapsedRealtime()-anchor.second else 0L;delay(1000)}
 }
 return now
}

@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun HomeTournamentCard(onOpen:()->Unit) {
 var event by remember { mutableStateOf<AtelierTournament?>(null) }
 var failed by remember { mutableStateOf(false) }
 LaunchedEffect(Unit){
  if(!SupabaseProvider.configured)return@LaunchedEffect
  while(true){gameRequestResult{ThroneBackend.tournament()}.onSuccess{event=it;failed=false}.onFailure{failed=true};delay(15_000)}
 }
 val now=serverNow(event?.serverTime.orEmpty(),Unit)
 TournamentHomeStage(event,now,failed,onOpen)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun TournamentHomeStage(event:AtelierTournament?,now:Long,failed:Boolean=false,onOpen:()->Unit) {
 GameEventStage(onClick=onOpen) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   HfGameArt(R.drawable.kelime_atolyesi_game_icon,64.dp,64.dp,description=sh("Kelime Atölyesi","Word Workshop"))
   Spacer(Modifier.width(10.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    EventTag(if(event?.active==true)sh("TURNUVA AÇIK","TOURNAMENT LIVE")else sh("SIRADAKİ MÜCADELE","NEXT CHALLENGE"))
    Text(sh("Kelime Atölyesi","Word Workshop"),color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Black)
    Text(sh("Turnuvasına Kalan Zaman","Time to Tournament"),color=Color.White.copy(alpha=.85f),fontSize=12.sp)
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
   EventCountdown(if(now==0L)"—:—:—" else tournamentClockText(tournamentNextRegular(now),now))
   Column(horizontalAlignment=Alignment.End) {
    Text(if(event?.active==true)"×${event?.multiplier}"else "×1,5",color=EventGold,fontSize=26.sp,fontWeight=FontWeight.Black)
    Text(sh("TECRÜBE","EXPERIENCE"),color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold)
   }
  }
  Text(if(event?.active==true)sh("${event?.stage}. aşama başladı · Herkes katılabilir","Stage ${event?.stage} has started · Open to everyone")else sh("Her 2 saatte yarış · 19:00 ve 22:00 ×3 XP","Race every 2 hours · 19:00 and 22:00 ×3 XP"),color=Color.White.copy(alpha=.9f),fontSize=11.sp)
  if(event!=null && tournamentTimeMillis(event!!.nextStart)<tournamentNextRegular(now))
   Text(sh("19:00 özel turnuva: ${tournamentClockText(tournamentTimeMillis(event!!.nextStart),now)}","19:00 special tournament: ${tournamentClockText(tournamentTimeMillis(event!!.nextStart),now)}"),fontSize=11.sp,color=EventGold)
  EventAction(if(event?.active==true)sh("TURNUVAYA GİR","ENTER TOURNAMENT")else sh("ATÖLYEYE GİR","ENTER WORKSHOP"),onOpen)
  val winners=event?.winners.orEmpty()
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   Icon(Icons.Rounded.EmojiEvents,null,tint=EventGold,modifier=Modifier.size(16.dp))
   Text(if(winners.isEmpty())sh("Kürsü seni bekliyor. Bir sonraki yarışta yerini al!","The podium awaits. Join the next race!")else winners.joinToString("     •     "){"${it.rank}. ${it.name} · ${it.score}"},Modifier.weight(1f).basicMarquee(iterations=Int.MAX_VALUE),color=Color.White,fontSize=11.sp,maxLines=1)
  }
  if(failed)Text(sh("Bağlantı yenileniyor…","Reconnecting…"),fontSize=10.sp,color=EventGold)
 }
}

internal fun throneGameName(game:String)=when(game){"siege"->sh("Kuşatma","Siege");"last_letter"->sh("Son Harf","Last Letter");else->sh("Kelime Atölyesi","Word Workshop")}
internal fun throneMissionText(m:ThroneMission)=when(m.id){
 "rounds"->sh("5 resmî oyun tamamla","Complete 5 official games")
 "mastery"->if(m.game=="atelier")sh("12 atölye görevi tamamla","Complete 12 workshop tasks")else sh("3 maç kazan","Win 3 matches")
 else->sh("1.000 oyun XP kazan","Earn 1,000 game XP")
}
@Composable internal fun ThroneScreen(onBack:()->Unit,onLegacy:()->Unit) {
 var week by remember{mutableStateOf<ThroneWeek?>(null)}
 var error by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){if(!SupabaseProvider.configured){error=true;return@LaunchedEffect};while(true){gameRequestResult{ThroneBackend.week()}.onSuccess{week=it;error=false}.onFailure{error=true};delay(20_000)}}
 val now=serverNow(week?.serverTime.orEmpty(),Unit)
 LazyColumn(Modifier.fillMaxSize().background(SonHarfTheme.Background),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { MainScreenHeader(title=sh("TAHT","THRONE"),subtitle=sh("Üç oyun · Tek haftalık yarış","Three games · One weekly race"),onBack=onBack) }
  item { ThroneOwnerStage(week?.previousOwner?.let { ThroneRow(rank=1,userId=it.userId,name=it.name,xp=it.xp,avatarPath=it.avatarPath,gender=it.gender,avatarVisibility=it.avatarVisibility) },tournamentTimeMillis(week?.resetAt.orEmpty()),now) }
  item { GameEventStage {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
    Column { Text(sh("SENİN YARIŞIN","YOUR RACE"),color=EventGold,fontSize=11.sp,fontWeight=FontWeight.Black);Text("${week?.me?.xp?:0} XP",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Black) }
    EventTag(sh("SIRA ","RANK ")+(week?.me?.rank?.takeIf{it>0}?:"—"))
   }
   week?.breakdown.orEmpty().forEach { r ->
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     HfGameArt(when(r.game){"siege"->R.drawable.kelime_tahti_brand_logo;"last_letter"->R.drawable.son_harf_game_icon;else->R.drawable.kelime_atolyesi_game_icon},38.dp,38.dp,description=null)
     Column(Modifier.weight(1f)){Text(throneGameName(r.game),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold);Text(sh("${r.rounds} resmî oyun","${r.rounds} official games"),color=Color.White.copy(alpha=.7f),fontSize=10.sp)}
     Text("${r.xp} XP",color=EventGold,fontSize=14.sp,fontWeight=FontWeight.Black)
    }
   }
  } }
  item { Text(sh("HAFTALIK GÖREVLER","WEEKLY MISSIONS"),color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Bold) }
  items(week?.missions.orEmpty(),key={"${it.game}:${it.id}"}){m->Surface(shape=RoundedCornerShape(18.dp),color=SonHarfTheme.Surface,border=BorderStroke(1.dp,SonHarfTheme.PremiumGold.copy(alpha=.4f))){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    Icon(if(m.awarded)Icons.Rounded.Verified else Icons.Rounded.Flag,null,tint=SonHarfTheme.Primary,modifier=Modifier.size(28.dp))
    Column(Modifier.weight(1f)){Text(throneGameName(m.game).uppercase(),color=SonHarfTheme.TextSecondary,fontSize=9.sp,fontWeight=FontWeight.Black);Text(throneMissionText(m),color=SonHarfTheme.TextPrimary,fontSize=13.sp,fontWeight=FontWeight.Bold)}
    EventTag("+${m.reward} XP")
   }
   LinearProgressIndicator(progress={(m.progress.toFloat()/m.target.coerceAtLeast(1)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),color=SonHarfTheme.Primary,trackColor=SonHarfTheme.SurfaceElevated)
   Text("${m.progress}/${m.target}"+if(m.awarded)sh(" · ÖDÜL KAZANILDI"," · REWARD EARNED")else "",color=SonHarfTheme.TextSecondary,fontSize=10.sp,fontWeight=FontWeight.Bold)
  }} }
  item { GameWeeklyPodium(week?.rows.orEmpty().take(3)) }
  items(week?.rows.orEmpty().drop(3),key={it.userId}){r->Surface(shape=RoundedCornerShape(14.dp),color=SonHarfTheme.Surface){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Text("${r.rank}",Modifier.width(24.dp),color=SonHarfTheme.PremiumGold,fontWeight=FontWeight.Black)
   ProfilePhotoAvatarWithGender(r.avatarPath,r.gender,r.name,36.dp,visible=r.avatarVisibility!="hidden",userId=r.userId)
   Text(r.name,Modifier.weight(1f),color=SonHarfTheme.TextPrimary,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text("${r.xp} XP",color=SonHarfTheme.Primary,fontSize=12.sp,fontWeight=FontWeight.Black)
  }} }
  item { Text(sh("Maç 35 XP · Galibiyet +85 XP · Görevler 100/150/200 XP. Sıfırlama: pazartesi 00:00.","Official match: 35 XP; win: +85 XP. Workshop score converts to XP by duration. Tournament multipliers apply only to tournament stages. Mission rewards are automatic. Practice does not award weekly XP."),color=SonHarfTheme.TextSecondary,fontSize=11.sp) }
  item { OutlinedButton(onClick=onLegacy,modifier=Modifier.fillMaxWidth()){Text(sh("Ligler · Kupa · Rakipler","Leagues · Cup · Rivals"))} }
  if(week==null)item { if(error)Text(sh("Taht yüklenemedi. Bağlantı tekrar deneniyor.","Unable to load the throne. Reconnecting."),color=SonHarfTheme.TextSecondary)else CircularProgressIndicator(color=SonHarfTheme.Primary) }
 }
}

@Composable internal fun AtelierTournamentPanel(event:AtelierTournament?,busy:Boolean,onJoin:()->Unit) {
 val now=serverNow(event?.serverTime.orEmpty(),event?.eventStart)
 val already=event?.myStages?.any{it.stage==event.stage}==true
 val previous=event==null||event.stage==1||event.myStages.any{it.stage==event.stage-1&&it.finished}
 val canJoin=event?.active==true&&!already&&previous&&now>0&&tournamentTimeMillis(event.stageEnds)-now>(event.stage*60+10)*1000L
 GameEventStage {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   HfGameArt(R.drawable.kelime_atolyesi_game_icon,68.dp,68.dp,description=null)
   Column(Modifier.weight(1f)){EventTag(if(event?.active==true)sh("CANLI TURNUVA","LIVE TOURNAMENT")else sh("KELİME ARENASI","WORD ARENA"));Text(sh("Üç aşama. Tek kürsü.","Three stages. One podium."),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Black)}
  }
  if(event?.active==true) {
   Text(sh("${event.stage}. aşama · ×${event.multiplier} XP","Stage ${event.stage} · ×${event.multiplier} XP"),color=EventGold,fontWeight=FontWeight.Black)
   EventCountdown(if(now==0L)"—:—:—"else tournamentClockText(tournamentTimeMillis(event.stageEnds),now))
  } else {
   Text(sh("SIRADAKİ TURNUVAYA","NEXT TOURNAMENT IN"),color=Color.White.copy(alpha=.85f),fontSize=10.sp,fontWeight=FontWeight.Bold)
   EventCountdown(if(now==0L)"—:—:—"else tournamentClockText(tournamentTimeMillis(event?.nextStart.orEmpty()),now))
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   listOf(sh("HAZIRLIK","WARM-UP"),sh("YARI FİNAL","SEMIFINAL"),sh("FİNAL","FINAL")).forEachIndexed{i,label->
    val done=event?.myStages?.any{it.stage==i+1&&it.finished}==true
    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(done)EventGold else Color.White.copy(alpha=.1f)).padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
     Icon(if(done)Icons.Rounded.CheckCircle else if(i==2)Icons.Rounded.EmojiEvents else Icons.Rounded.Flag,null,tint=if(done)EventInk else EventGold,modifier=Modifier.size(24.dp))
     Text(label,color=if(done)EventInk else Color.White,fontWeight=FontWeight.Black,fontSize=8.sp,maxLines=1)
     Text(sh("${i+1} DK","${i+1} MIN"),color=if(done)EventInk else EventGold,fontSize=11.sp,fontWeight=FontWeight.Bold)
    }
   }
  }
  Text(sh("30 dakikalık yarış · Her aşama 10 dakika açık. Önceki aşamayı tamamla, finale ilerle. En çok aşama ve puan kürsüyü belirler.","30-minute race · Each stage opens for 10 minutes. Complete each stage to advance. Completed stages and score decide the podium."),color=Color.White.copy(alpha=.85f),fontSize=11.sp,lineHeight=16.sp)
  EventAction(when {
   busy->sh("HAZIRLANIYOR…","PREPARING…")
   event?.active!=true->sh("TURNUVA BAŞLANGICI BEKLENİYOR","WAITING FOR TOURNAMENT")
   already->sh("AŞAMA TAMAM · SONRAKİNİ BEKLE","STAGE PLAYED · WAIT FOR NEXT")
   !previous->sh("ÖNCEKİ AŞAMAYI TAMAMLA","COMPLETE THE PREVIOUS STAGE")
   !canJoin->sh("BU AŞAMANIN GİRİŞİ KAPANDI","STAGE ENTRY CLOSED")
   else->sh("TURNUVAYA KATIL","JOIN TOURNAMENT")
  },onJoin,canJoin&&!busy)
  event?.myStages?.filter{it.finished}?.forEach{Text(sh("${it.stage}. aşama: +${it.xp} XP","Stage ${it.stage}: +${it.xp} XP"),color=EventGold,fontSize=11.sp)}
  if(event?.rows?.isNotEmpty()==true) {
   Text(sh("CANLI SIRALAMA · İLK 10","LIVE RANKING · TOP 10"),color=EventGold,fontSize=11.sp,fontWeight=FontWeight.Black)
   event.rows.take(10).forEach{row->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Text("${row.rank}.",Modifier.width(28.dp),color=EventGold,fontSize=12.sp)
    Text(row.name,Modifier.weight(1f),color=Color.White,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    Text("${row.stages}/3 · ${row.score}",color=Color.White,fontSize=11.sp)
   }}
  }
 }
}

@Composable internal fun GameWeeklyPodium(players:List<ThroneRow>,onOpen:(()->Unit)?=null) {
 GameEventStage(onClick=onOpen) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Column{Text(sh("HAFTANIN TAHT YARIŞI","WEEKLY THRONE RACE"),color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Black);Text(sh("Üç oyun · Tek zirve","Three games · One summit"),color=EventGold,fontSize=10.sp)}
   Icon(Icons.Rounded.EmojiEvents,null,tint=EventGold,modifier=Modifier.size(30.dp))
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.Bottom) {
   listOf(1,0,2).forEach { index->
    val p=players.getOrNull(index);val winner=index==0
    val medal=when(index){0->EventGold;1->Color(0xFFE1E9E6);else->Color(0xFFE8B589)}
    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
     if(winner)Icon(Icons.Rounded.WorkspacePremium,null,tint=EventGold,modifier=Modifier.size(28.dp))
     if(p!=null)ProfilePhotoAvatarWithGender(p.avatarPath,p.gender,p.name,if(winner)68.dp else 50.dp,accent=medal,visible=p.avatarVisibility!="hidden",userId=p.userId)
     else DefaultProfilePortrait(null,Modifier.size(if(winner)68.dp else 50.dp).clip(CircleShape))
     Text(p?.name?:sh("Yerini al","Claim a spot"),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
     Column(Modifier.fillMaxWidth().height(if(winner)84.dp else if(index==1)64.dp else 54.dp).clip(RoundedCornerShape(topStart=12.dp,topEnd=12.dp))
      .background(Brush.verticalGradient(listOf(medal,medal.copy(alpha=.65f)))).border(1.dp,Color.White.copy(alpha=.5f),RoundedCornerShape(topStart=12.dp,topEnd=12.dp)),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
      Text("${index+1}",color=EventInk,fontSize=25.sp,fontWeight=FontWeight.Black)
      Text(p?.let{"${it.xp} XP"}?:"—",color=EventInk,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)
     }
    }
   }
  }
  Text(sh("TAHTI ELE GEÇİR  ›","CLAIM THE THRONE  ›"),Modifier.align(Alignment.End),color=EventGold,fontSize=11.sp,fontWeight=FontWeight.Black)
 }
}

@Composable internal fun ThroneOwnerStage(owner:ThroneRow?,resetAt:Long,now:Long,previousOwner:ThroneOwner?=null) {
GameEventStage(gold=true) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    EventTag(sh("HAFTANIN HÜKÜMDARI","WEEKLY RULER"))
    Icon(Icons.Rounded.EmojiEvents,null,tint=EventInk,modifier=Modifier.size(24.dp))
   }
   Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
    ThroneSeat {
     if(owner!=null)ProfilePhotoAvatarWithGender(owner.avatarPath,owner.gender,owner.name,78.dp,visible=owner.avatarVisibility!="hidden",userId=owner.userId)
     else DefaultProfilePortrait(null,Modifier.size(78.dp).clip(CircleShape))
    }
    Text(sh("TAHT SAHİBİ","THRONE OWNER"),color=EventInk,fontSize=12.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)
    Text(owner?.name?:sh("İlk şampiyon bekleniyor","Awaiting the first champion"),color=EventInk,fontSize=28.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
    Text("${owner?.xp?:0} XP",color=EventInk,fontSize=21.sp,fontWeight=FontWeight.Black)
    Text(sh("Ödül süresi: ","Reward expires in: ")+(if(now==0L)"—"else tournamentClockText(resetAt,now)),color=EventInk,fontSize=11.sp)
    Text(sh("Altın çerçeve · 1 rozeti · 7 gün","Gold frame · No. 1 badge · 7 days"),color=EventInk,fontSize=11.sp,fontWeight=FontWeight.Bold)
    previousOwner?.let{Text(sh("Geçen haftanın sahibi: ${it.name}","Last week's owner: ${it.name}"),color=EventInk.copy(alpha=.75f),fontSize=10.sp)}
   }
  }
}
