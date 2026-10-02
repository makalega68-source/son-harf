package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
 Surface(onClick=onOpen,shape=RoundedCornerShape(20.dp),color=SonHarfTheme.Surface,
  border=BorderStroke(1.dp,SonHarfTheme.PremiumGold.copy(alpha=.5f))) {
  Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
   Text(sh("Kelime Atölyesi Turnuvasına Kalan Zaman","Time to Word Workshop Tournament"),color=SonHarfTheme.TextPrimary,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
   Text(if(now==0L)"—:—:—" else tournamentClockText(tournamentNextRegular(now),now),color=SonHarfTheme.Primary,fontSize=26.sp,fontWeight=FontWeight.Bold)
   Text(if(event?.active==true)sh("Turnuva açık · Aşama ${event?.stage}/3 · ×${event?.multiplier} XP","Tournament open · Stage ${event?.stage}/3 · ×${event?.multiplier} XP")
    else sh("Her 2 saatte ×1,5 XP · 19:00 ve 22:00'da ×3 XP","Every 2 hours ×1.5 XP · 19:00 and 22:00 ×3 XP"),color=SonHarfTheme.TextSecondary,fontSize=11.sp)
   if(event!=null && tournamentTimeMillis(event!!.nextStart)<tournamentNextRegular(now))
    Text(sh("19:00 özel turnuva: ${tournamentClockText(tournamentTimeMillis(event!!.nextStart),now)}","19:00 special tournament: ${tournamentClockText(tournamentTimeMillis(event!!.nextStart),now)}"),fontSize=11.sp,color=SonHarfTheme.PremiumGold)
   val winners=event?.winners.orEmpty()
   Text(if(winners.isEmpty()) sh("Tamamlanan turnuvaların ilk üçü burada yayınlanacak.","Top three finishers will appear here.")
    else winners.joinToString("     •     "){"${it.rank}. ${it.name} · ${it.score}"},
    Modifier.fillMaxWidth().basicMarquee(iterations=Int.MAX_VALUE),color=SonHarfTheme.TextPrimary,fontSize=12.sp,maxLines=1)
   if(failed)Text(sh("Bağlantı yenileniyor; sayaç son sunucu saatinden ilerliyor.","Reconnecting; timer uses the last server time."),fontSize=10.sp,color=SonHarfTheme.TextSecondary)
  }
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
  item { Surface(shape=RoundedCornerShape(22.dp),color=SonHarfTheme.Surface,border=BorderStroke(1.dp,SonHarfTheme.PremiumGold)) {
   Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text(sh("TAHT SAHİBİ","THRONE OWNER"),color=SonHarfTheme.PremiumGold,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
    val owner=week?.rows?.firstOrNull()
    if(owner!=null)ProfilePhotoAvatarWithGender(owner.avatarPath,owner.gender,owner.name,72.dp,visible=owner.avatarVisibility!="hidden",userId=owner.userId)
    Text(owner?.name?:sh("Taht yeni sahibini bekliyor","The throne awaits its new owner"),color=SonHarfTheme.TextPrimary,fontSize=20.sp,fontWeight=FontWeight.Bold)
    Text("${owner?.xp?:0} XP",color=SonHarfTheme.Primary,fontSize=18.sp)
    Text(sh("Hafta yenilenmesine: ","Week resets in: ")+(if(now==0L)"—" else tournamentClockText(tournamentTimeMillis(week?.resetAt.orEmpty()),now)),color=SonHarfTheme.TextSecondary,fontSize=12.sp)
    week?.previousOwner?.let{Text(sh("Geçen haftanın sahibi: ${it.name}","Last week's owner: ${it.name}"),color=SonHarfTheme.TextSecondary,fontSize=11.sp)}
   }
  } }
  item { Text(sh("Bu hafta: ${week?.me?.xp?:0} XP · Sıra ${week?.me?.rank?.takeIf{it>0}?:"—"}","This week: ${week?.me?.xp?:0} XP · Rank ${week?.me?.rank?.takeIf{it>0}?:"—"}"),color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Bold) }
  items(week?.breakdown.orEmpty(),key={"total:${it.game}"}){r-> Text("${throneGameName(r.game)} · ${r.xp} XP · ${r.rounds} "+sh("oyun","games"),color=SonHarfTheme.TextSecondary,fontSize=13.sp) }
  item { Text(sh("HAFTALIK GÖREVLER","WEEKLY MISSIONS"),color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Bold) }
  items(week?.missions.orEmpty(),key={"${it.game}:${it.id}"}){m->Surface(shape=RoundedCornerShape(14.dp),color=SonHarfTheme.Surface){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
   Text(throneGameName(m.game)+" · "+throneMissionText(m),color=SonHarfTheme.TextPrimary,fontSize=12.sp)
   LinearProgressIndicator(progress={m.progress.toFloat()/m.target},modifier=Modifier.fillMaxWidth(),color=SonHarfTheme.Primary,trackColor=SonHarfTheme.SurfaceElevated)
   Text("${m.progress}/${m.target} · +${m.reward} XP"+if(m.awarded)sh(" · Kazanıldı"," · Earned")else "",color=SonHarfTheme.TextSecondary,fontSize=11.sp)
  }} }
  item { Text(sh("HAFTANIN SIRALAMASI","WEEKLY RANKING"),color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Bold) }
  items(week?.rows.orEmpty(),key={it.userId}){r->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("${r.rank}",Modifier.width(32.dp),color=SonHarfTheme.PremiumGold);Text(r.name,Modifier.weight(1f),color=SonHarfTheme.TextPrimary);Text("${r.xp} XP",color=SonHarfTheme.Primary)} }
  item { Text(sh("Resmî maç: 35 XP; galibiyet: +85 XP. Atölye puanı süreye göre XP'ye çevrilir. Turnuva çarpanı yalnız turnuva aşamalarında geçerlidir. Görev ödülleri otomatik eklenir. Antrenmanlar haftalık puan vermez.","Official match: 35 XP; win: +85 XP. Workshop score converts to XP by duration. Tournament multipliers apply only to tournament stages. Mission rewards are automatic. Practice does not award weekly XP."),color=SonHarfTheme.TextSecondary,fontSize=11.sp) }
  item { OutlinedButton(onClick=onLegacy,modifier=Modifier.fillMaxWidth()){Text(sh("Ligler · Kupa · Rakipler","Leagues · Cup · Rivals"))} }
  if(week==null)item { if(error)Text(sh("Taht yüklenemedi. Bağlantı tekrar deneniyor.","Unable to load the throne. Reconnecting."),color=SonHarfTheme.TextSecondary)else CircularProgressIndicator(color=SonHarfTheme.Primary) }
 }
}

@Composable internal fun AtelierTournamentPanel(event:AtelierTournament?,busy:Boolean,onJoin:()->Unit) {
 val now=serverNow(event?.serverTime.orEmpty(),event?.eventStart)
 Surface(shape=RoundedCornerShape(18.dp),color=SonHarfTheme.Surface,border=BorderStroke(1.dp,SonHarfTheme.PremiumGold)){
  Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(sh("ATÖLYE TURNUVASI","WORKSHOP TOURNAMENT"),color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Bold)
   Text(if(event?.active==true)sh("Aşama ${event.stage}/3 · ×${event.multiplier} XP","Stage ${event.stage}/3 · ×${event.multiplier} XP")else sh("Sıradaki turnuva: ","Next tournament: ")+(if(now==0L)"—"else tournamentClockText(tournamentTimeMillis(event?.nextStart.orEmpty()),now)),color=SonHarfTheme.Primary)
   Text(sh("30 dakika · Hazırlık 1 dk, yarı final 2 dk, final 3 dk. Her aşama 10 dakikalık pencerede bir kez oynanır. Önceki aşamayı bitiren herkes ilerler. Tamamlanan aşama sayısı, toplam oyun puanı ve bitirme zamanı sıralamayı belirler.","30 minutes · Warm-up 1 min, semifinal 2 min, final 3 min. Play once per 10-minute stage window. Everyone completing the previous stage advances. Completed stages, total game score and finishing time determine rank."),color=SonHarfTheme.TextSecondary,fontSize=11.sp)
   val already=event?.myStages?.any{it.stage==event.stage}==true
   val previous=event==null||event.stage==1||event.myStages.any{it.stage==event.stage-1&&it.finished}
   val canJoin=event?.active==true&&!already&&previous&&now>0&&tournamentTimeMillis(event.stageEnds)-now>(event.stage*60+10)*1000L
   Button(onClick=onJoin,enabled=canJoin&&!busy,modifier=Modifier.fillMaxWidth()){
    Text(if(busy)sh("Hazırlanıyor…","Preparing…")else if(already)sh("Bu aşama oynandı · Sonrakini bekle","Stage played · Wait for the next")else if(!previous)sh("Önceki aşama tamamlanmalı","Complete the previous stage first")else sh("TURNUVAYA KATIL","JOIN TOURNAMENT"))
   }
   event?.myStages?.filter{it.finished}?.forEach{Text(sh("${it.stage}. aşama: +${it.xp} XP","Stage ${it.stage}: +${it.xp} XP"),color=SonHarfTheme.TextSecondary,fontSize=11.sp)}
   if(event?.rows?.isNotEmpty()==true) {
    Text(sh("TURNUVA SIRALAMASI · İLK 10", "TOURNAMENT RANKING · TOP 10"),color=SonHarfTheme.TextPrimary,fontSize=12.sp,fontWeight=FontWeight.Bold)
    event.rows.take(10).forEach{row->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Text("${row.rank}.",Modifier.width(28.dp),color=SonHarfTheme.PremiumGold,fontSize=12.sp)
     Text(row.name,Modifier.weight(1f),color=SonHarfTheme.TextPrimary,fontSize=12.sp,maxLines=1)
     Text("${row.stages}/3 · ${row.score}",color=SonHarfTheme.TextSecondary,fontSize=11.sp)
    }}
   }
  }
 }
}
