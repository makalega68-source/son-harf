package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable internal fun serverNow(server:String,key:Any?):Long {
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

/** Live tournament state for the lobby's Kelime Atölyesi card: one short line, polled every 15 s. */
@Composable internal fun rememberWorkshopStatus():String {
 var event by remember { mutableStateOf<AtelierTournament?>(null) }
 var failed by remember { mutableStateOf(false) }
 LaunchedEffect(Unit){
  if(!SupabaseProvider.configured)return@LaunchedEffect
  while(true){gameRequestResult{ThroneBackend.tournament()}.onSuccess{event=it;failed=false}.onFailure{failed=true};delay(15_000)}
 }
 val now=serverNow(event?.serverTime.orEmpty(),Unit)
 return when {
  failed -> sh("Turnuva · Bağlantı yenileniyor", "Tournament · Reconnecting")
  event?.active==true -> sh("Turnuva açık · ×${event?.multiplier} XP", "Tournament live · ×${event?.multiplier} XP")
  now>0 -> sh("Turnuva: ", "Tournament: ")+tournamentClockText(tournamentNextRegular(now),now)
  else -> sh("Kelime bul · Turnuvaya katıl", "Find words · Join tournaments")
 }
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
@Composable internal fun ThroneScreen(onBack:()->Unit,onLegacy:(Int)->Unit) {
 var week by remember{mutableStateOf<ThroneWeek?>(null)}
 var error by remember{mutableStateOf(false)}
 var section by remember{mutableIntStateOf(0)}
 val foreground=rememberAppForeground()
 LaunchedEffect(foreground){if(!foreground)return@LaunchedEffect;if(!SupabaseProvider.configured){error=true;return@LaunchedEffect};while(true){gameRequestResult{ThroneBackend.week()}.onSuccess{week=it;error=false}.onFailure{error=true};delay(20_000)}}
 val now=serverNow(week?.serverTime.orEmpty(),Unit)
 LazyColumn(Modifier.fillMaxSize().menuGround(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { ThroneHero(onBack,tournamentTimeMillis(week?.resetAt.orEmpty()),now) }
  item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   listOf(sh("Ligler","Leagues"),sh("Kupa","Cup"),sh("Rakipler","Rivals")).forEachIndexed { i,label ->
    Surface(onClick={onLegacy(i)},modifier=Modifier.weight(1f).premiumPanel(RoundedCornerShape(16.dp)),shape=RoundedCornerShape(16.dp),color=Color.Transparent) {
     Column(Modifier.padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Icon(when(i){0->Icons.Rounded.WorkspacePremium;1->Icons.Rounded.EmojiEvents;else->Icons.Rounded.Groups},null,tint=LobbyBrand.Gold,modifier=Modifier.size(26.dp))
      Text(label,color=Color.White,fontWeight=FontWeight.Black,fontSize=13.sp)
     }
    }
   }
  } }
  item { LobbyTabs(listOf(sh("Özet","Overview"),sh("Görevler","Missions"),sh("Sıralama","Ranking")),section,{section=it}) }
  if(section==0) {
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
  }
  if(section==1) {
  item { Text(sh("HAFTALIK GÖREVLER","WEEKLY MISSIONS"),color=LobbyPalette.Ink,fontWeight=FontWeight.Bold) }
  items(week?.missions.orEmpty(),key={"${it.game}:${it.id}"}){m->Surface(shape=RoundedCornerShape(18.dp),color=LobbyPalette.Paper,border=BorderStroke(1.dp,SonHarfTheme.PremiumGold.copy(alpha=.4f))){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    Icon(if(m.awarded)Icons.Rounded.Verified else Icons.Rounded.Flag,null,tint=SonHarfTheme.Primary,modifier=Modifier.size(28.dp))
    Column(Modifier.weight(1f)){Text(throneGameName(m.game).uppercase(),color=LobbyPalette.Muted,fontSize=9.sp,fontWeight=FontWeight.Black);Text(throneMissionText(m),color=LobbyPalette.Ink,fontSize=13.sp,fontWeight=FontWeight.Bold)}
    EventTag("+${m.reward} XP")
   }
   LinearProgressIndicator(progress={(m.progress.toFloat()/m.target.coerceAtLeast(1)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),color=SonHarfTheme.Primary,trackColor=LobbyPalette.Soft)
   Text("${m.progress}/${m.target}"+if(m.awarded)sh(" · ÖDÜL KAZANILDI"," · REWARD EARNED")else "",color=LobbyPalette.Muted,fontSize=10.sp,fontWeight=FontWeight.Bold)
  }} }
  }
  if(section==2) {
  item { GameWeeklyPodium(week?.rows.orEmpty().take(3)) }
  items(week?.rows.orEmpty().drop(3),key={it.userId}){r->Surface(shape=RoundedCornerShape(14.dp),color=LobbyPalette.Paper){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Text("${r.rank}",Modifier.width(24.dp),color=SonHarfTheme.PremiumGold,fontWeight=FontWeight.Black)
   ProfilePhotoAvatarWithGender(r.avatarPath,r.gender,r.name,36.dp,visible=r.avatarVisibility!="hidden",userId=r.userId)
   Text(r.name,Modifier.weight(1f),color=LobbyPalette.Ink,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text("${r.xp} XP",color=SonHarfTheme.Primary,fontSize=12.sp,fontWeight=FontWeight.Black)
  }} }
  }
  if(section==0) item { Text(sh("Maç 35 XP · Galibiyet +85 XP · Görevler 100/150/200 XP. Sıfırlama: pazartesi 00:00.","Official match: 35 XP; win: +85 XP. Workshop score converts to XP by duration. Tournament multipliers apply only to tournament stages. Mission rewards are automatic. Practice does not award weekly XP."),color=LobbyPalette.Muted,fontSize=11.sp) }
  if(week==null)item { if(error)Text(sh("Taht yüklenemedi. Bağlantı tekrar deneniyor.","Unable to load the throne. Reconnecting."),color=LobbyPalette.Muted)else CircularProgressIndicator(color=SonHarfTheme.Primary) }
 }
}

/**
 * The workshop lobby's tournament row: one line saying when it starts (or which stage is live),
 * one button, and a "Nasıl?" link for the rules. Same entry rules as [AtelierTournamentPanel].
 */
@Composable internal fun AtelierTournamentCompact(event:AtelierTournament?,busy:Boolean,onJoin:()->Unit,ready:Boolean,onReady:()->Unit) {
 val now=serverNow(event?.serverTime.orEmpty(),event?.eventStart)
 val already=event?.myStages?.any{it.stage==event.stage}==true
 val previous=event==null||event.stage==1||event.myStages.any{it.stage==event.stage-1&&it.finished}
 val canJoin=event?.active==true&&!already&&previous&&now>0&&tournamentTimeMillis(event.stageEnds)-now>(event.stage*60+10)*1000L
 val waiting=event?.active!=true
 val missed=!waiting&&!already&&!previous
 var showRules by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
  .background(Brush.verticalGradient(listOf(Color(0xFF1F6F80),Color(0xFF12485A)))).padding(14.dp),
  verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   Icon(Icons.Rounded.EmojiEvents,null,tint=EventGold,modifier=Modifier.size(28.dp))
   Spacer(Modifier.width(10.dp))
   Column(Modifier.weight(1f)) {
    Text(if(waiting)sh("TURNUVA","TOURNAMENT")else sh("TURNUVA · CANLI","TOURNAMENT · LIVE"),color=EventGold,fontSize=11.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp)
    val clock=if(now==0L)"—:—:—" else if(waiting)tournamentClockText(tournamentTimeMillis(event?.nextStart.orEmpty()),now) else tournamentClockText(tournamentTimeMillis(event!!.stageEnds),now)
    Text(if(waiting)sh("Başlamasına $clock","Starts in $clock")else sh("${event!!.stage}/3. aşama · $clock kaldı","Stage ${event!!.stage}/3 · $clock left"),
     color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=1)
   }
   Text(sh("Nasıl?","How?"),color=EventGold,fontSize=13.sp,fontWeight=FontWeight.Bold,
    modifier=Modifier.clip(RoundedCornerShape(10.dp)).clickable{showRules=!showRules}.padding(horizontal=8.dp,vertical=6.dp))
  }
  if(showRules) Text(sh("Turnuva 3 kısa aşamadır: 1, 2 ve 3 dakika. \"Hazırım\"a bas, turnuva başlayınca otomatik girersin; sonraki aşamalar da kendiliğinden gelir. En çok aşamayı bitirip en çok puanı toplayan kürsüye çıkar.",
   "The tournament has 3 short stages: 1, 2 and 3 minutes. Tap \"I'm ready\" and you join automatically when it starts; the next stages follow by themselves. Most stages and points take the podium."),
   color=Color.White.copy(alpha=.85f),fontSize=12.sp,lineHeight=17.sp)
  val label=when {
   busy->sh("Hazırlanıyor…","Preparing…")
   (waiting||missed)&&ready->sh("Hazırsın ✓ Başlayınca otomatik gireceksin","Ready ✓ You'll join automatically")
   waiting||missed->sh("Hazırım","I'm ready")
   already->sh("Aşama tamam ✓ Sonraki kendiliğinden gelir","Stage done ✓ The next one comes by itself")
   !canJoin->sh("Bu aşamanın girişi kapandı","This stage is closed")
   else->sh("Turnuvaya Gir","Join the Tournament")
  }
  val enabled=(canJoin||((waiting||missed)&&!ready))&&!busy
  Button(onClick={ if(waiting||missed) onReady() else onJoin() },enabled=enabled,
   modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(14.dp),
   colors=ButtonDefaults.buttonColors(containerColor=EventGold,contentColor=EventInk,
    disabledContainerColor=Color.White.copy(alpha=.16f),disabledContentColor=Color.White.copy(alpha=.85f))) {
   Text(label,fontSize=15.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
  }
 }
}

@Composable internal fun AtelierTournamentPanel(event:AtelierTournament?,busy:Boolean,onJoin:()->Unit,ready:Boolean=false,onReady:(()->Unit)?=null) {
 val now=serverNow(event?.serverTime.orEmpty(),event?.eventStart)
 val already=event?.myStages?.any{it.stage==event.stage}==true
 val previous=event==null||event.stage==1||event.myStages.any{it.stage==event.stage-1&&it.finished}
 val canJoin=event?.active==true&&!already&&previous&&now>0&&tournamentTimeMillis(event.stageEnds)-now>(event.stage*60+10)*1000L
 GameEventStage {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   HfGameArt(R.drawable.kelime_atolyesi_game_icon,68.dp,68.dp,description=null)
   Column(Modifier.weight(1f)){EventTag(if(event?.active==true)sh("CANLI TURNUVA","LIVE TOURNAMENT")else sh("KELİME ARENASI","WORD ARENA"));Text(sh("Üç aşama. Tek kürsü.","Three stages. One podium."),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Black)}
  }
  val waiting=event?.active!=true
  // The clock and the one button that matters sit together at the top of the card.
  if(!waiting) {
   Text(sh("${event!!.stage}. aşama · ×${event.multiplier} XP · bitişe","Stage ${event!!.stage} · ×${event.multiplier} XP · ends in"),color=EventGold,fontWeight=FontWeight.Black)
   EventCountdown(if(now==0L)"—:—:—"else tournamentClockText(tournamentTimeMillis(event.stageEnds),now))
  } else {
   Text(sh("SIRADAKİ TURNUVAYA","NEXT TOURNAMENT IN"),color=Color.White.copy(alpha=.85f),fontSize=10.sp,fontWeight=FontWeight.Bold)
   EventCountdown(if(now==0L)"—:—:—"else tournamentClockText(tournamentTimeMillis(event?.nextStart.orEmpty()),now))
  }
  // One tap is enough: "Hazırım" joins automatically when the next tournament opens, and once in,
  // every next stage starts by itself. A missed tournament offers the next one instead of a dead button.
  val missed=!waiting&&!already&&!previous
  EventAction(when {
   busy->sh("HAZIRLANIYOR…","PREPARING…")
   (waiting||missed)&&ready->sh("HAZIRSIN ✓ · BAŞLAYINCA OTOMATİK GİRİLECEK","READY ✓ · YOU JOIN AUTOMATICALLY")
   waiting->sh("HAZIRIM · BAŞLAYINCA BENİ AL","I'M READY · JOIN ME AT THE START")
   missed->sh("SIRADAKİ TURNUVAYA HAZIRIM","READY FOR THE NEXT TOURNAMENT")
   already->sh("AŞAMA TAMAM · SONRAKİ OTOMATİK GELECEK","STAGE DONE · NEXT ONE STARTS BY ITSELF")
   !canJoin->sh("BU AŞAMANIN GİRİŞİ KAPANDI","STAGE ENTRY CLOSED")
   else->sh("TURNUVAYA BAŞLA","START THE TOURNAMENT")
  },{ if(waiting||missed) onReady?.invoke() else onJoin() },(canJoin||((waiting||missed)&&onReady!=null&&!ready))&&!busy)
  if(missed) Text(sh("Bu turnuva 1. aşamadan başladı. Sıradaki turnuvada otomatik olarak yarışa alınırsın.","This one began at stage 1. You'll be entered automatically in the next one."),
   color=Color.White.copy(alpha=.8f),fontSize=11.sp,lineHeight=15.sp)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   listOf(sh("HAZIRLIK","WARM-UP"),sh("YARI FİNAL","SEMIFINAL"),sh("FİNAL","FINAL")).forEachIndexed{i,label->
    val done=event?.myStages?.any{it.stage==i+1&&it.finished}==true
    val current=event?.active==true&&event.stage==i+1
    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(done)EventGold else if(current)Color.White.copy(alpha=.22f) else Color.White.copy(alpha=.1f)).padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
     Icon(if(done)Icons.Rounded.CheckCircle else if(i==2)Icons.Rounded.EmojiEvents else Icons.Rounded.Flag,null,tint=if(done)EventInk else EventGold,modifier=Modifier.size(20.dp))
     Text(label,color=if(done)EventInk else Color.White,fontWeight=FontWeight.Black,fontSize=8.sp,maxLines=1)
     Text(sh("${i+1} DK","${i+1} MIN"),color=if(done)EventInk else EventGold,fontSize=11.sp,fontWeight=FontWeight.Bold)
    }
   }
  }
  Text(sh("Her aşama 10 dk açık · aşamalar sırayla oynanır · en çok aşama ve puan kürsüye çıkar","Each stage opens for 10 min · played in order · most stages and points take the podium"),
   color=Color.White.copy(alpha=.75f),fontSize=10.sp,lineHeight=14.sp)
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
    // The ruler's photo inside the crowned gold ring of the podium artwork.
    PodiumPortrait(Modifier.clip(RoundedCornerShape(18.dp))) { photo ->
     if(owner!=null)ProfilePhotoAvatarWithGender(owner.avatarPath,owner.gender,owner.name,photo,visible=owner.avatarVisibility!="hidden",showGenderBadge=false)
     else DefaultProfilePortrait(null,Modifier.size(photo).clip(CircleShape))
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
