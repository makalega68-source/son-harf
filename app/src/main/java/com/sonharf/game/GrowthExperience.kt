package com.sonharf.game

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class GrowthDashboardDto(
    @SerialName("display_name") val displayName: String,
    val xp: Int = 0,
    val level: Int = 1,
    @SerialName("level_progress") val levelProgress: Int = 0,
    @SerialName("level_target") val levelTarget: Int = 500,
    @SerialName("current_win_streak") val currentWinStreak: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("total_matches") val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    @SerialName("valid_words") val validWords: Int = 0,
    @SerialName("matches_today") val matchesToday: Int = 0,
    @SerialName("daily_reward") val dailyReward: Int = 40,
    @SerialName("daily_claimed") val dailyClaimed: Boolean = false,
    @SerialName("daily_challenge_claimed") val dailyChallengeClaimed: Boolean = false,
    @SerialName("league_name") val leagueName: String = "BRONZ",
    @SerialName("next_title") val nextTitle: String = "ÇAYLAK",
    @SerialName("achievements_unlocked") val achievementsUnlocked: Int = 0,
    @SerialName("achievement_total") val achievementTotal: Int = 10,
)

suspend fun OnlineGameBackend.getGrowthDashboard(): GrowthDashboardDto =
    SupabaseProvider.client.postgrest.rpc("get_growth_dashboard_v1").decodeSingle()

suspend fun OnlineGameBackend.claimDailyCheckin(): Int =
    SupabaseProvider.client.postgrest.rpc("claim_daily_checkin_v1").decodeSingle()

suspend fun OnlineGameBackend.claimDailyChallenge(): Int =
    SupabaseProvider.client.postgrest.rpc("claim_daily_challenge_v1").decodeSingle()

suspend fun OnlineGameBackend.logEvent(name: String, value: String? = null) {
    runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "log_app_event_v1",
            buildJsonObject {
                put("p_event_name", name.take(64))
                if (value != null) put("p_event_value", value.take(240))
            },
        )
    }
}

object SonHarfShare {
    private fun send(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Son Harf"))
    }

    fun challenge(context: Context, player: String, roomCode: String? = null) {
        val code = roomCode?.takeIf { it.isNotBlank() }?.let { "\nOda kodu: $it" }.orEmpty()
        send(context, "⚔️ $player seni SON HARF kelime düellosuna çağırıyor!$code\nSon harften kelime üret, serini koru ve beni yen.")
    }

    fun result(context: Context, player: String, mine: Int, opponent: Int, words: Int, streak: Int) {
        send(context, "🏆 SON HARF düello sonucu\n$player: $mine - $opponent\n🧠 $words kelime • 🔥 $streak seri\nBeni geçebilir misin?")
    }

    fun profile(context: Context, d: GrowthDashboardDto) {
        send(context, "👤 SON HARF profilim\n${d.displayName} • Seviye ${d.level}\n🏆 ${d.wins} galibiyet • 🔥 ${d.currentWinStreak} seri\n💠 ${d.leagueName} Lig • ${d.xp} XP\nBeni düelloya çağır!")
    }
}

private data class GrowthAchievement(val icon: String, val title: String, val unlocked: Boolean)

@Composable
fun GrowthCenterScreen(onPlay: (() -> Unit)? = null) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    suspend fun reload() {
        dashboard = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        friends = runCatching { backend?.getFriends().orEmpty() }.getOrDefault(emptyList())
    }

    LaunchedEffect(Unit) {
        reload()
        runCatching { backend?.logEvent("growth_center_open") }
    }

    val d = dashboard
    val achievements = if (d == null) emptyList() else listOf(
        GrowthAchievement("⚔", sh("İlk Düello", "First Duel"), d.totalMatches >= 1),
        GrowthAchievement("🏆", sh("İlk Zafer", "First Win"), d.wins >= 1),
        GrowthAchievement("🔥", sh("10 Zafer", "10 Wins"), d.wins >= 10),
        GrowthAchievement("👑", sh("50 Zafer", "50 Wins"), d.wins >= 50),
        GrowthAchievement("✦", sh("50 Kelime", "50 Words"), d.validWords >= 50),
        GrowthAchievement("💎", sh("250 Kelime", "250 Words"), d.validWords >= 250),
        GrowthAchievement("⚡", sh("5 Seri", "5 Streak"), d.bestStreak >= 5),
        GrowthAchievement("🌪", sh("Fırtına", "Storm"), d.bestStreak >= 10),
        GrowthAchievement("🛡", sh("Lig Oyuncusu", "League Player"), d.xp >= 1200),
        GrowthAchievement("⭐", sh("100 Düello", "100 Duels"), d.totalMatches >= 100),
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, SonHarfCyan.copy(alpha=.38f)),
                shape = RoundedCornerShape(26.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(SonHarfPurple.copy(alpha=.22f), SonHarfCyan.copy(alpha=.12f), SonHarfSurface))
                    ).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("🚀 ${sh("KARİYER MERKEZİ", "CAREER CENTER")}", fontWeight = FontWeight.Black, fontSize = 21.sp)
                    if (d == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(d.displayName, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                Text("${d.leagueName} • ${d.nextTitle}", color = SonHarfGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Surface(shape=CircleShape,color=SonHarfCyan.copy(alpha=.14f)) { Text("LV ${d.level}", Modifier.padding(12.dp), color=SonHarfCyan, fontWeight=FontWeight.Black) }
                        }
                        LinearProgressIndicator(progress={ d.levelProgress.toFloat()/d.levelTarget.coerceAtLeast(1) }, modifier=Modifier.fillMaxWidth())
                        Text("${d.xp} XP • ${d.levelProgress}/${d.levelTarget}", color=SonHarfMuted, fontSize=9.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            GrowthMetric("🏆", d.wins.toString(), sh("Zafer", "Wins"), Modifier.weight(1f))
                            GrowthMetric("🔥", d.currentWinStreak.toString(), sh("Seri", "Streak"), Modifier.weight(1f))
                            GrowthMetric("🧠", d.validWords.toString(), sh("Kelime", "Words"), Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (d != null) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                val reward = runCatching { backend?.claimDailyCheckin() ?: 0 }.getOrDefault(0)
                                notice = if (reward > 0) sh("Günlük ödül: +$reward elmas", "Daily reward: +$reward diamonds") else sh("Bugünün ödülünü zaten aldın.", "Today's reward is already claimed.")
                                reload(); busy = false
                            }
                        },
                        enabled = !busy && !d.dailyClaimed,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF2B1C00)),
                    ) { Text(if (d.dailyClaimed) "✓ ${sh("GÜNLÜK", "DAILY")}" else "🎁 +${d.dailyReward}", fontWeight=FontWeight.Black) }
                    OutlinedButton(onClick={ SonHarfShare.profile(context,d); scope.launch { backend?.logEvent("profile_share") } }, modifier=Modifier.weight(1f)) { Text("↗ ${sh("PROFİLİ PAYLAŞ", "SHARE PROFILE")}", fontSize=10.sp) }
                }
            }

            item {
                Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Column { Text("🎯 ${sh("GÜNLÜK MEYDAN OKUMA", "DAILY CHALLENGE")}",fontWeight=FontWeight.Black); Text(sh("Bugün 3 düello tamamla", "Complete 3 duels today"),color=SonHarfMuted,fontSize=9.sp) }
                            Text("💎 30",color=SonHarfCyan,fontWeight=FontWeight.Black)
                        }
                        LinearProgressIndicator(progress={ (d.matchesToday.coerceAtMost(3)/3f) },modifier=Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            Text("${d.matchesToday.coerceAtMost(3)}/3",fontWeight=FontWeight.Bold)
                            Button(onClick={ scope.launch { busy=true; val r=runCatching { backend?.claimDailyChallenge() ?: 0 }.getOrDefault(0); notice=if(r>0) "+$r 💎" else sh("Henüz tamamlanmadı veya ödül alındı.","Not completed yet or already claimed."); reload(); busy=false } },enabled=!busy && d.matchesToday>=3 && !d.dailyChallengeClaimed) { Text(if(d.dailyChallengeClaimed) sh("ALINDI","CLAIMED") else sh("TOPLA","CLAIM")) }
                        }
                    }
                }
            }

            item {
                Text("🏅 ${sh("BAŞARIMLAR", "ACHIEVEMENTS")} ${d.achievementsUnlocked}/${d.achievementTotal}", fontWeight=FontWeight.Black, fontSize=14.sp)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    achievements.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            row.forEach { a -> AchievementTile(a,Modifier.weight(1f)) }
                            if(row.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,SonHarfGold.copy(alpha=.25f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("🔥 ${sh("REKABET", "COMPETITION")}",fontWeight=FontWeight.Black)
                        Text(sh("Mevcut galibiyet serin: ${d.currentWinStreak} • En iyi seri: ${d.bestStreak}","Current win streak: ${d.currentWinStreak} • Best streak: ${d.bestStreak}"),color=SonHarfMuted,fontSize=10.sp)
                        if(onPlay!=null) Button(onClick=onPlay,modifier=Modifier.fillMaxWidth()) { Text(sh("ŞİMDİ DÜELLOYA GİR","PLAY A DUEL NOW"),fontWeight=FontWeight.Black) }
                    }
                }
            }
        }

        item { Text("👥 ${sh("ARKADAŞLAR", "FRIENDS")}",fontWeight=FontWeight.Black,fontSize=14.sp) }
        if(friends.isEmpty()) item { Text(sh("Henüz arkadaş yok. Oyna ekranından arkadaş ekleyebilir ve davet gönderebilirsin.","No friends yet. Add and invite friends from Play."),color=SonHarfMuted,fontSize=10.sp) }
        items(friends.size) { index ->
            val (_,p)=friends[index]
            Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(11.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column { Text(p.displayName,fontWeight=FontWeight.Bold); Text(if(p.presenceStatus=="online") "● ${sh("Çevrimiçi","Online")}" else sh("Çevrimdışı","Offline"),color=if(p.presenceStatus=="online") SonHarfGreen else SonHarfMuted,fontSize=8.sp) }
                    Row {
                        TextButton(onClick={ SonHarfShare.challenge(context,dashboard?.displayName ?: sh("Oyuncu","Player")); scope.launch { backend?.logEvent("friend_challenge_share",p.id) } }) { Text("↗") }
                        Button(onClick={ scope.launch { runCatching { backend?.inviteFriend(p.id,SonHarfUiState.language) }.onSuccess { notice=sh("Davet gönderildi.","Invite sent.") }.onFailure { notice=sh("Davet gönderilemedi.","Invite failed.") } } },enabled=p.presenceStatus=="online") { Text(sh("DÜELLO","DUEL"),fontSize=9.sp) }
                    }
                }
            }
        }

        if(notice.isNotBlank()) item { Text(notice,Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=SonHarfGold,fontSize=10.sp) }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable private fun GrowthMetric(icon:String,value:String,label:String,modifier:Modifier){
    Surface(modifier=modifier,shape=RoundedCornerShape(15.dp),color=SonHarfSurface.copy(alpha=.8f)) {
        Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){ Text(icon,fontSize=18.sp); Text(value,fontWeight=FontWeight.Black,fontSize=18.sp); Text(label,color=SonHarfMuted,fontSize=8.sp) }
    }
}

@Composable private fun AchievementTile(a:GrowthAchievement,modifier:Modifier){
    Surface(modifier=modifier,shape=RoundedCornerShape(15.dp),color=if(a.unlocked) SonHarfGold.copy(alpha=.10f) else SonHarfSurface,border=BorderStroke(1.dp,if(a.unlocked) SonHarfGold.copy(alpha=.45f) else SonHarfMuted.copy(alpha=.10f))) {
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Text(a.icon,fontSize=20.sp,color=if(a.unlocked) SonHarfGold else SonHarfMuted); Column { Text(a.title,fontWeight=FontWeight.Bold,fontSize=9.sp); Text(if(a.unlocked) sh("AÇILDI","UNLOCKED") else sh("KİLİTLİ","LOCKED"),color=if(a.unlocked) SonHarfGreen else SonHarfMuted,fontSize=7.sp) } }
    }
}
