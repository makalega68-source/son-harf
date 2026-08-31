package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.MetaProgressV2Dto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.getMetaProgressV2
import com.sonharf.game.data.claimSeasonReward
import kotlinx.coroutines.launch

@Composable
internal fun SeasonCenterScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        runCatching { backend.getMetaProgressV2() }
            .onSuccess { meta = it }
            .onFailure { notice = sh("Sezon verileri yüklenemedi.", "Season data could not be loaded.") }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val m = meta
    val remainingXp = ((m?.seasonTarget ?: 300) - (m?.seasonProgress ?: 0)).coerceAtLeast(0)
    val daysLeft = ((m?.seasonDays ?: 30) - (m?.seasonDay ?: 1)).coerceAtLeast(0)
    val progress = ((m?.seasonProgress ?: 0).toFloat() / (m?.seasonTarget ?: 300).coerceAtLeast(1)).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Sezon", "Season"),
                subtitle = sh("İlerle, ödülleri aç ve sezon yolunu tamamla", "Progress, unlock rewards and complete the season path"),
                onBack = onBack,
            )
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MainUi.Surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(m?.seasonName ?: sh("Aktif Sezon", "Active Season"), color = MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(
                                sh("${daysLeft} gün kaldı • ${m?.seasonDay ?: 1}/${m?.seasonDays ?: 30}", "$daysLeft days left • ${m?.seasonDay ?: 1}/${m?.seasonDays ?: 30}"),
                                color = MainUi.Muted,
                                fontSize = 10.sp,
                            )
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = if (m?.seasonPassActive == true) MainUi.Gold.copy(alpha = .14f) else MainUi.SurfaceSoft) {
                            Text(
                                if (m?.seasonPassActive == true) sh("BİLET AKTİF", "PASS ACTIVE") else sh("ÜCRETSİZ YOL", "FREE TRACK"),
                                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                color = if (m?.seasonPassActive == true) MainUi.Gold else MainUi.Muted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${sh("SEZON SEVİYESİ", "SEASON LEVEL")} ${m?.seasonLevel ?: 1}", color = MainUi.Blue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("${m?.seasonXp ?: 0} XP", color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MainUi.Blue,
                        trackColor = MainUi.SurfaceSoft,
                    )
                    Text(
                        sh("Sezon ${m?.seasonLevel?.plus(1) ?: 2} için $remainingXp XP kaldı", "$remainingXp XP to Season ${m?.seasonLevel?.plus(1) ?: 2}"),
                        color = MainUi.Muted,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MainUi.BlueSoft,
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = MainUi.Blue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        sh("Sezon Bileti görünüm ve ilerleme ödülleri verir; maç gücü vermez.", "The Season Pass gives progression and collection rewards; it never gives match power."),
                        color = MainUi.Text,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item { SeasonPassPurchaseCard { scope.launch { reload() } } }

        item { MainSectionTitle(sh("ÖDÜL YOLU", "REWARD TRACK")) }

        items((1..10).toList(), key = { it }) { tier ->
            val unlocked = (m?.seasonLevel ?: 1) >= tier
            val freeClaimed = tier in (m?.freeClaimedTiers ?: emptyList())
            val premiumClaimed = tier in (m?.premiumClaimedTiers ?: emptyList())
            val freeReward = 20 + tier * 5
            val premiumReward = 40 + tier * 10

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MainUi.Surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (unlocked) MainUi.Blue.copy(alpha = .25f) else MainUi.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = if (unlocked) MainUi.BlueSoft else MainUi.SurfaceSoft) {
                                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                    if (unlocked) Text(tier.toString(), color = MainUi.Blue, fontWeight = FontWeight.Black)
                                    else Icon(Icons.Rounded.Lock, null, tint = MainUi.Muted, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text("${sh("SEVİYE", "LEVEL")} $tier", color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text(if (unlocked) sh("Ödül açıldı", "Reward unlocked") else sh("Sezon seviyesi $tier gerekli", "Season level $tier required"), color = MainUi.Muted, fontSize = 8.sp)
                            }
                        }
                    }

                    SeasonRewardRow(
                        label = sh("Ücretsiz", "Free"),
                        reward = "+$freeReward SC",
                        claimed = freeClaimed,
                        enabled = unlocked && !freeClaimed && busy == null,
                        premium = false,
                        busy = busy == "free_$tier",
                        onClaim = {
                            scope.launch {
                                busy = "free_$tier"
                                runCatching { backend.claimSeasonReward(tier, false) }
                                    .onSuccess { amount -> notice = if (amount > 0) sh("+$amount Son Coin cüzdanına eklendi.", "+$amount Son Coin added to your wallet.") else sh("Bu ödül daha önce alındı.", "This reward was already claimed.") }
                                    .onFailure { notice = sh("Ödül alınamadı.", "Reward could not be claimed.") }
                                reload()
                                busy = null
                            }
                        },
                    )
                    SeasonRewardRow(
                        label = sh("Sezon Bileti", "Season Pass"),
                        reward = "+$premiumReward SC",
                        claimed = premiumClaimed,
                        enabled = unlocked && m?.seasonPassActive == true && !premiumClaimed && busy == null,
                        premium = true,
                        busy = busy == "premium_$tier",
                        onClaim = {
                            scope.launch {
                                busy = "premium_$tier"
                                runCatching { backend.claimSeasonReward(tier, true) }
                                    .onSuccess { amount -> notice = if (amount > 0) sh("+$amount Son Coin cüzdanına eklendi.", "+$amount Son Coin added to your wallet.") else sh("Bu ödül daha önce alındı.", "This reward was already claimed.") }
                                    .onFailure { notice = sh("Premium ödül alınamadı. Sezon Bileti aktif olmalı.", "Premium reward could not be claimed. An active Season Pass is required.") }
                                reload()
                                busy = null
                            }
                        },
                    )
                }
            }
        }

        item {
            Text(
                sh("Bu sezondaki mevcut ödül yolu yalnızca Son Coin verir. Uygulamada gerçekten verilmeyen çerçeve, unvan veya Style ödülü vaat edilmez.", "The current season reward track grants Son Coin only. Frames, titles or Style items are not advertised unless they are actually granted."),
                color = MainUi.Muted,
                fontSize = 9.sp,
            )
        }

        notice?.let {
            item {
                Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(14.dp)) {
                    Text(it, Modifier.fillMaxWidth().padding(12.dp), color = MainUi.Text, fontSize = 10.sp)
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun SeasonRewardRow(
    label: String,
    reward: String,
    claimed: Boolean,
    enabled: Boolean,
    premium: Boolean,
    busy: Boolean,
    onClaim: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (premium) MainUi.Gold else MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(reward, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onClaim,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (premium) MainUi.Gold else MainUi.Blue,
                contentColor = if (premium) Color(0xFF3C2700) else Color.White,
                disabledContainerColor = if (claimed) MainUi.Green.copy(alpha = .14f) else MainUi.SurfaceSoft,
                disabledContentColor = if (claimed) MainUi.Green else MainUi.Muted,
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Text(
                when {
                    busy -> "…"
                    claimed -> sh("ALINDI", "CLAIMED")
                    enabled -> sh("ÖDÜLÜ AL", "CLAIM")
                    else -> sh("KİLİTLİ", "LOCKED")
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
