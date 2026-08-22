package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.DailyGoalV10Dto
import com.sonharf.game.data.MetaDashboardV10Dto
import com.sonharf.game.ui.home.FullHomeUiState

private val H10Bg = Color(0xFFF4F1E8)
private val H10Card = Color(0xFFFFFCF4)
private val H10Ink = Color(0xFF263238)
private val H10Muted = Color(0xFF6D756F)
private val H10Teal = Color(0xFF1C8C8C)
private val H10TealDark = Color(0xFF126A6A)
private val H10Gold = Color(0xFFF1B83B)
private val H10Green = Color(0xFF4E9A62)
private val H10Purple = Color(0xFF8066A8)
private val H10Coral = Color(0xFFD96B57)
private val H10Border = Color(0xFFD7D0C3)

@Composable
fun V10HomeScreen(
    state: FullHomeUiState,
    meta: MetaDashboardV10Dto?,
    dailyGoals: List<DailyGoalV10Dto>,
    onStartGameMode: (String) -> Unit,
    onClaimDailyReward: () -> Unit,
    onOpenVipModal: () -> Unit,
    onInviteFriend: () -> Unit,
    onOpenFriendsList: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
    onClaimWeeklyGoal: (String) -> Unit,
    onClaimDailyGoal: (String) -> Unit,
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(H10Bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = H10Teal)
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().background(H10Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(onClick = onOpenProfile, shape = RoundedCornerShape(20.dp), color = H10Card, border = BorderStroke(1.dp, H10Border)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V8Avatar(state.userPhotoUrl, state.userName, 56)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.userName, color = H10Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("${meta?.seasonLeague ?: state.league} • ${meta?.rating ?: 1000} rating", color = H10Muted, fontSize = 12.sp)
                        Text("Seviye ${state.level} • %${meta?.winRate ?: 0} galibiyet", color = H10TealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF0B7)) {
                        Text("💎 ${state.diamonds}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = H10Ink, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = H10Teal) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SON HARF ARENASI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("Benzer rating • gerçek oyuncu • server kuralları", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                        }
                        Icon(Icons.Rounded.Bolt, null, tint = H10Gold, modifier = Modifier.size(34.dp))
                    }
                    Button(
                        onClick = { onStartGameMode("1v1_RANKED") },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = H10Gold, contentColor = H10Ink),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text("1v1 HIZLI KARŞILAŞMA", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f)); Icon(Icons.Rounded.PlayArrow, null)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onStartGameMode("EXPERT_MATCH") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.White.copy(alpha=.55f))) { Text("UZMAN", fontWeight = FontWeight.Black) }
                        OutlinedButton(onClick = { onStartGameMode("PRACTICE_BOT") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.White.copy(alpha=.55f))) { Text("BOT PRATİK", fontWeight = FontWeight.Black) }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V10MiniCard("🔥 ${meta?.checkinStreak ?: 0}/7", "Giriş Serisi", H10Coral, Modifier.weight(1f))
                V10MiniCard("🏆 ${meta?.achievementsUnlocked ?: 0}/${meta?.achievementTotal ?: 10}", "Başarımlar", H10Purple, Modifier.weight(1f))
                V10MiniCard("⚡ ${meta?.bestStreak ?: 0}", "En İyi Seri", H10Green, Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = onClaimDailyReward, enabled = state.isDailyRewardAvailable && !state.isActionBusy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = if (state.isDailyRewardAvailable) Color(0xFFFFF0B7) else H10Card, border = BorderStroke(1.dp,H10Border)) {
                    Column(Modifier.padding(12.dp)) { Text("🎁 Günlük Ödül", fontWeight = FontWeight.Black, color=H10Ink); Text(if(state.isDailyRewardAvailable) "+${state.dailyRewardDiamonds} elmas" else "Bugün alındı", color=H10Muted,fontSize=12.sp) }
                }
                Surface(onClick = onOpenVipModal, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Color(0xFFEDE3F5), border = BorderStroke(1.dp,H10Purple.copy(alpha=.35f))) {
                    Column(Modifier.padding(12.dp)) { Text("👑 VIP", fontWeight = FontWeight.Black, color=H10Purple); Text("2x ödül • kozmetik", color=H10Muted,fontSize=12.sp) }
                }
            }
        }

        item {
            Text("BUGÜN", fontWeight = FontWeight.Black, fontSize = 18.sp, color = H10Ink)
            Spacer(Modifier.height(6.dp))
            if (dailyGoals.isEmpty()) Text("Günlük görevler yükleniyor…", color = H10Muted)
            else Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                dailyGoals.forEach { g -> V10GoalCard(g.titleTr,g.progress,g.target,g.rewardDiamonds,g.claimed,H10Teal) { onClaimDailyGoal(g.id) } }
            }
        }

        item {
            Text("BU HAFTA", fontWeight = FontWeight.Black, fontSize = 18.sp, color = H10Ink)
            Spacer(Modifier.height(6.dp))
            if (state.tasks.isEmpty()) Text("Haftalık görev yok.", color=H10Muted)
            else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.tasks, key={it.id}) { g ->
                    Box(Modifier.width(230.dp)) { V10GoalCard(g.title,g.current,g.target,g.rewardDiamonds,g.isClaimed,H10Green) { onClaimWeeklyGoal(g.id) } }
                }
            }
        }

        item {
            Text("SEZON & REKABET", fontWeight = FontWeight.Black, fontSize = 18.sp, color=H10Ink)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V10ActionCard("Lig", "${meta?.seasonLeague ?: "BRONZ"} • ${meta?.rating ?: 1000}", Icons.Rounded.Shield, H10Gold, Modifier.weight(1f), onOpenLeaderboard)
                V10ActionCard("Arkadaşlar", "${state.onlineFriendsCount} çevrimiçi", Icons.Rounded.Groups, H10Teal, Modifier.weight(1f), onOpenFriendsList)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V10ActionCard("Düello Daveti", "Arkadaşını çağır", Icons.Rounded.PersonAdd, H10Green, Modifier.weight(1f), onInviteFriend)
                V10ActionCard("Profil", "Rozet ve istatistikler", Icons.Rounded.EmojiEvents, H10Purple, Modifier.weight(1f), onOpenProfile)
            }
        }

        item {
            Surface(shape=RoundedCornerShape(18.dp),color=H10Card,border=BorderStroke(1.dp,H10Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("OYUNCU İSTATİSTİKLERİ",fontWeight=FontWeight.Black,color=H10Ink)
                    Row { Text("${meta?.totalMatches ?: 0} maç",Modifier.weight(1f),color=H10Muted); Text("${meta?.validWords ?: 0} kelime",Modifier.weight(1f),textAlign=TextAlign.Center,color=H10Muted); Text("%${meta?.winRate ?: 0}",Modifier.weight(1f),textAlign=TextAlign.End,color=H10Muted) }
                    Text("En uzun: ${meta?.longestWord?.uppercase() ?: "-"}   •   Favori başlangıç: ${meta?.favoriteStartLetter ?: "-"}",color=H10TealDark,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Text("Kozmetik çerçeve, unvan, klavye ve zafer efektlerini Mağaza'dan elmasla kişiselleştirebilirsin.",color=H10Muted,fontSize=11.sp,lineHeight=15.sp)
                }
            }
        }

        if (state.notice.isNotBlank()) item {
            Surface(shape=RoundedCornerShape(14.dp),color=Color.White,border=BorderStroke(1.dp,H10Teal.copy(alpha=.35f))) {
                Text(state.notice,Modifier.fillMaxWidth().padding(11.dp),textAlign=TextAlign.Center,color=H10TealDark,fontWeight=FontWeight.Bold,fontSize=12.sp)
            }
        }
    }
}

@Composable private fun V10MiniCard(value:String,label:String,accent:Color,modifier:Modifier){
    Surface(modifier=modifier,shape=RoundedCornerShape(14.dp),color=H10Card,border=BorderStroke(1.dp,H10Border)) { Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){ Text(value,color=accent,fontWeight=FontWeight.Black,fontSize=15.sp); Text(label,color=H10Muted,fontSize=9.sp,textAlign=TextAlign.Center) } }
}

@Composable private fun V10GoalCard(title:String,current:Int,target:Int,reward:Int,claimed:Boolean,accent:Color,onClaim:()->Unit){
    val done=current>=target
    Surface(shape=RoundedCornerShape(15.dp),color=H10Card,border=BorderStroke(1.dp,H10Border)) { Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){ Text(title,Modifier.weight(1f),fontWeight=FontWeight.Bold,color=H10Ink,maxLines=1); Text("💎 $reward",fontSize=11.sp,color=H10Muted) }
        LinearProgressIndicator(progress={ if(target<=0)0f else(current.toFloat()/target).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),color=if(done)H10Green else accent,trackColor=Color(0xFFE6E0D5))
        Row(verticalAlignment=Alignment.CenterVertically){ Text("$current/$target",Modifier.weight(1f),fontSize=11.sp,color=H10Muted); if(done) Button(onClick=onClaim,enabled=!claimed,modifier=Modifier.height(34.dp),contentPadding=PaddingValues(horizontal=10.dp),colors=ButtonDefaults.buttonColors(containerColor=H10Green)){ Text(if(claimed)"ALINDI" else "ÖDÜLÜ AL",fontSize=10.sp,fontWeight=FontWeight.Black) } }
    } }
}

@Composable private fun V10ActionCard(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,accent:Color,modifier:Modifier,onClick:()->Unit){
    Surface(onClick=onClick,modifier=modifier.heightIn(min=92.dp),shape=RoundedCornerShape(16.dp),color=H10Card,border=BorderStroke(1.dp,H10Border)) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){ Icon(icon,null,tint=accent); Text(title,fontWeight=FontWeight.Black,color=H10Ink); Text(subtitle,color=H10Muted,fontSize=11.sp) } }
}
