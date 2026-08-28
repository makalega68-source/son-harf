package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private val MasteryBg = Color(0xFFF7F9FC)
private val MasteryPanel = Color.White
private val MasteryCyan = Color(0xFF1769E0)
private val MasteryGold = Color(0xFFF3A81A)
private val MasteryGreen = Color(0xFF22B95F)
private val MasteryText = Color(0xFF182235)
private val MasteryMuted = Color(0xFF718096)

@Composable
fun MasteryPathScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onLeague: () -> Unit,
) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var milestones by remember { mutableStateOf<List<MasteryMilestoneDto>>(emptyList()) }
    var rival by remember { mutableStateOf<ArchRivalDto?>(null) }
    var pod by remember { mutableStateOf<List<WeeklyPodRowDto>>(emptyList()) }
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload() {
        val b = backend ?: return
        milestones = runCatching { b.getMasteryPath() }.getOrDefault(emptyList())
        rival = runCatching { b.getArchRival() }.getOrNull()
        pod = runCatching { b.getWeeklyPod(SonHarfUiState.language) }.getOrDefault(emptyList())
        meta = runCatching { b.getMetaProgressV2() }.getOrNull()
    }

    LaunchedEffect(Unit) {
        reload()
        runCatching { backend?.logEvent("mastery_path_open") }
    }

    val completed = milestones.count { it.claimed }
    val unlocked = milestones.count { it.unlocked }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, MasteryBg, Color(0xFFF0F4F8)))
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MasteryText) }
                Column(Modifier.weight(1f)) {
                    Text(sh("KELİME USTALIĞI", "WORD MASTERY"), color = MasteryText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text(sh("Her maç kelime ustalığını ve rekabet seviyeni geliştirir.", "Every match improves your word mastery and competitive level."), color = MasteryMuted, fontSize = 10.sp)
                }
            }
        }

        item { MasteryHero(completed, unlocked, milestones.size) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MasteryCyan, contentColor = MasteryBg),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(5.dp))
                    Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onLeague,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, MasteryGold),
                ) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = MasteryGold)
                    Spacer(Modifier.width(5.dp))
                    Text(sh("LİG", "LEAGUE"), color = MasteryGold, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            SectionTitle("⚔", sh("EZELİ RAKİP", "ARCH RIVAL"))
            Spacer(Modifier.height(7.dp))
            val r = rival
            if (r == null) {
                MasteryInfoCard(sh("Henüz ezeli rakibin oluşmadı. Aynı oyuncularla yaptığın düellolar arttıkça burada belirecek.", "No arch rival yet. Repeated duels with the same players will build one here."))
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MasteryPanel),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MasteryCyan.copy(alpha = .35f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.displayName, color = MasteryText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("${r.matches} ${sh("maç", "matches")}", color = MasteryMuted, fontWeight = FontWeight.Bold)
                        }
                        Text("${r.wins} - ${r.losses}", color = MasteryGold, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("${sh("Toplam puan", "Total points")}: ${r.myPoints} - ${r.theirPoints}", color = MasteryMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            SectionTitle("🏆", sh("20'Lİ HAFTALIK GRUP", "WEEKLY GROUP OF 20"))
            Text(
                sh("Benzer haftalık performanstaki 20 oyuncu. Her hafta yeniden oluşur.", "20 players with similar weekly performance. Rebuilt every week."),
                color = MasteryMuted,
                fontSize = 9.sp,
            )
        }

        if (pod.isEmpty()) {
            item { MasteryInfoCard(sh("Haftalık grup henüz oluşmadı.", "Weekly group is not ready yet.")) }
        } else {
            items(pod, key = { it.userId }) { row ->
                Surface(
                    color = if (row.isMe) MasteryCyan.copy(alpha = .15f) else MasteryPanel,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, if (row.isMe) MasteryCyan else Color.Transparent),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#${row.podRank}", color = if (row.podRank <= 3) MasteryGold else MasteryMuted, fontWeight = FontWeight.Black)
                        Text(row.displayName, Modifier.weight(1f).padding(horizontal = 10.dp), color = MasteryText, fontWeight = if (row.isMe) FontWeight.Black else FontWeight.Bold)
                        Text("${row.wins}W • ${row.rating}", color = if (row.isMe) MasteryCyan else MasteryMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SectionTitle("🎯", sh("HAFTALIK TURNUVA", "WEEKLY TOURNAMENT"))
            Spacer(Modifier.height(7.dp))
            val m = meta
            Card(
                colors = CardDefaults.cardColors(containerColor = MasteryPanel),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MasteryGold.copy(alpha = .38f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(sh("SON HARF KUPASI", "SON HARF CUP"), color = MasteryGold, fontWeight = FontWeight.Black)
                    Text("${m?.cupPoints ?: 0} ${sh("puan", "points")} • #${m?.cupRank ?: 0}", color = MasteryText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (m?.cupQualified == true) sh("İlk 16'dasın. Hafta sonu final heyecanı seni bekliyor.", "You're in the Top 16. Weekend finals await.")
                        else sh("İlk 16 için haftalık maçlarını kazanmaya devam et.", "Keep winning weekly matches to reach the Top 16."),
                        color = if (m?.cupQualified == true) MasteryGreen else MasteryMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item { SectionTitle("✦", sh("USTALIK BASAMAKLARI", "MASTERY MILESTONES")) }

        items(milestones, key = { it.id }) { milestone ->
            val title = if (SonHarfUiState.isEnglish) milestone.titleEn else milestone.titleTr
            val desc = if (SonHarfUiState.isEnglish) milestone.descriptionEn else milestone.descriptionTr
            val fraction = (milestone.progress.toFloat() / milestone.target.coerceAtLeast(1)).coerceIn(0f, 1f)
            Card(
                colors = CardDefaults.cardColors(containerColor = MasteryPanel),
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, if (milestone.claimed) MasteryGreen.copy(alpha = .5f) else MasteryCyan.copy(alpha = .18f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, color = MasteryText, fontWeight = FontWeight.Black)
                        Text("+${milestone.rewardCoins} SC", color = MasteryGold, fontWeight = FontWeight.Black)
                    }
                    Text(desc, color = MasteryMuted, fontSize = 9.sp)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (milestone.unlocked) MasteryGreen else MasteryCyan,
                        trackColor = Color(0xFFE2E7EE),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${milestone.progress.coerceAtMost(milestone.target)}/${milestone.target}", color = MasteryMuted, fontSize = 9.sp)
                        Button(
                            onClick = {
                                val b = backend ?: return@Button
                                scope.launch {
                                    busy = milestone.id
                                    val reward = runCatching { b.claimMasteryReward(milestone.id) }.getOrDefault(0)
                                    notice = if (reward > 0) "+$reward Son Coin" else sh("Ödül daha önce alınmış olabilir.", "Reward may already be claimed.")
                                    reload()
                                    busy = null
                                }
                            },
                            enabled = milestone.unlocked && !milestone.claimed && busy == null,
                            colors = ButtonDefaults.buttonColors(containerColor = if (milestone.unlocked) MasteryGreen else MasteryPanel),
                        ) {
                            Text(
                                when {
                                    milestone.claimed -> "✓"
                                    busy == milestone.id -> "…"
                                    else -> sh("TOPLA", "CLAIM")
                                },
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }

        if (notice.isNotBlank()) {
            item { Text(notice, Modifier.fillMaxWidth(), color = MasteryGold, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun MasteryHero(completed: Int, unlocked: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MasteryPanel),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MasteryCyan.copy(alpha = .35f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sh("HESAP GELİŞİMİ", "ACCOUNT PROGRESS"), color = MasteryCyan, fontWeight = FontWeight.Black)
                Text("$completed/$total", color = MasteryGold, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else completed.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
                color = MasteryGold,
                trackColor = Color(0xFFE2E7EE),
            )
            Text("$unlocked ${sh("basamak açıldı", "milestones unlocked")}", color = MasteryMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SectionTitle(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 21.sp)
        Spacer(Modifier.width(7.dp))
        Text(title, color = MasteryText, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MasteryInfoCard(text: String) {
    Surface(color = MasteryPanel, shape = RoundedCornerShape(16.dp)) {
        Text(text, Modifier.fillMaxWidth().padding(13.dp), color = MasteryMuted, fontSize = 10.sp)
    }
}
