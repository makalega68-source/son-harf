package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
fun MetaProgressV2Screen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    suspend fun reload() { data = runCatching { backend?.getMetaProgressV2() }.getOrNull() }

    LaunchedEffect(Unit) { reload(); runCatching { backend?.logEvent("meta_progress_v2_open") } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val d = data
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .35f))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("✦ ${sh("SON HARF SEZONU", "SON HARF SEASON")}", color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    if (d == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(d.seasonName.uppercase(), fontWeight = FontWeight.Black); Text("${d.seasonDay}/${d.seasonDays}", color = SonHarfMuted) }
                        Text("LV ${d.seasonLevel} • ${d.seasonXp} XP", color = SonHarfCyan, fontWeight = FontWeight.Black)
                        LinearProgressIndicator(progress = { d.seasonProgress.toFloat() / d.seasonTarget.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                        Text(sh("Maç +35 XP • Galibiyet ekstra +85 XP", "Match +35 XP • Win extra +85 XP"), color = SonHarfMuted, fontSize = 9.sp)
                    }
                }
            }
        }

        data?.let { d ->
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetaMetric("🔥", d.dailyPlayStreak.toString(), sh("Günlük Seri", "Daily Streak"), Modifier.weight(1f)); MetaMetric("🏆", "#${d.cupRank}", sh("Kupa", "Cup"), Modifier.weight(1f)); MetaMetric("📚", d.uniqueWords.toString(), sh("Kelime", "Words"), Modifier.weight(1f)) } }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("🔥 ${sh("GÜNLÜK OYUN SERİSİ", "DAILY PLAY STREAK")}", fontWeight = FontWeight.Black)
                        Text(sh("Bugünkü en az 1 tamamlanmış maç seriyi korur.", "At least 1 finished match today protects the streak."), color = SonHarfMuted, fontSize = 10.sp)
                        Text("${sh("Mevcut", "Current")}: ${d.dailyPlayStreak} • ${sh("Rekor", "Best")}: ${d.bestDailyPlayStreak}", color = SonHarfGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .3f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("◇ ${sh("HAFTALIK KUPA", "WEEKLY CUP")}", color = LetharaPalette.Gold, fontWeight = FontWeight.Black)
                        Text("${d.cupPoints} ${sh("puan", "points")} • #${d.cupRank}", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(if (d.cupQualified) sh("✓ İlk 16 içindesin", "✓ You are in the Top 16") else sh("İlk 16 için maç kazanmaya devam et.", "Keep winning to reach the Top 16."), color = if (d.cupQualified) SonHarfGreen else SonHarfMuted)
                        Text(if (d.cupActive) sh("Kupa haftasonu aktif.", "Cup weekend is active.") else sh("Sıralama hafta boyunca oluşur; final heyecanı haftasonu.", "Ranking builds all week; finals peak on the weekend."), color = SonHarfMuted, fontSize = 9.sp)
                    }
                }
            }

            item {
                Text("📈 ${sh("OYUNCU REKORLARI", "PLAYER RECORDS")}", fontWeight = FontWeight.Black); Spacer(Modifier.height(7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecordLine(sh("Farklı kelime", "Unique words"), d.uniqueWords.toString())
                    RecordLine(sh("En uzun kelime", "Longest word"), if (d.longestWord.isBlank()) "—" else "${d.longestWord} (${d.longestWordLength})")
                    RecordLine(sh("En yüksek maç skoru", "Highest match score"), d.highestScore.toString())
                    RecordLine(sh("En iyi doğru seri", "Best answer streak"), d.bestStreak.toString())
                }
            }

            item {
                Text("🎖 ${sh("PRESTİJ UNVANLARI", "PRESTIGE TITLES")}", color = LetharaPalette.Gold, fontWeight = FontWeight.Black)
                Text(sh("Profilinde göstermek istediğin kazanılmış unvanı seç.", "Choose an unlocked title to display on your profile."), color = SonHarfMuted, fontSize = 9.sp); Spacer(Modifier.height(7.dp))
                val titles = listOf(Triple("ÇAYLAK", true, sh("Başlangıç", "Starter")), Triple("YÜKSELEN", d.availableTitles >= 2, "5W"), Triple("DÜELLOCU", d.availableTitles >= 3, "20W"), Triple("USTA", d.availableTitles >= 4, "50W"), Triple("EFSANE", d.availableTitles >= 5, "100W"), Triple("KELİME AVCISI", d.uniqueWords >= 250, "250 ${sh("kelime", "words")}"), Triple("SERİ USTASI", d.bestStreak >= 10, "10 🔥"))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    titles.forEach { (title, unlocked, req) ->
                        OutlinedButton(onClick = { scope.launch { busy = true; runCatching { backend?.setSelectedTitle(title) }.onSuccess { notice = sh("Unvan seçildi: $title", "Title selected: $title"); reload() }.onFailure { notice = sh("Bu unvan henüz kilitli.", "This title is still locked.") }; busy = false } }, enabled = unlocked && !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (d.selectedTitle == title) SonHarfGold else SonHarfText)) { Text(if (d.selectedTitle == title) "✓ $title" else "$title • $req", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            item { SeasonPassCard(active = d.seasonPassActive, onPurchased = { scope.launch { reload() } }, onNotice = { notice = it }) }
            item { Text("🎁 ${sh("SEZON ÖDÜLLERİ", "SEASON REWARDS")}", fontWeight = FontWeight.Black); Text(sh("Ücretsiz yol herkese açık. Premium yol ekstra Style/Son Coin sağlar; maç gücü sağlamaz.", "Free track is for everyone. Premium adds Style/Son Coin, never match power."), color = SonHarfMuted, fontSize = 9.sp) }
            items(10) { index ->
                val tier = index + 1; val unlocked = d.seasonLevel >= tier; val freeClaimed = tier in d.freeClaimedTiers; val premiumClaimed = tier in d.premiumClaimedTiers
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(17.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("LV $tier", fontWeight = FontWeight.Black); Text(if (unlocked) sh("AÇIK", "OPEN") else sh("KİLİTLİ", "LOCKED"), color = if (unlocked) SonHarfGreen else SonHarfMuted, fontSize = 9.sp) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { busy = true; val r = runCatching { backend?.claimSeasonReward(tier, false) ?: 0 }.getOrDefault(0); notice = if (r > 0) "+$r Son Coin" else sh("Ödül daha önce alındı.", "Reward already claimed."); reload(); busy = false } }, enabled = unlocked && !freeClaimed && !busy, modifier = Modifier.weight(1f)) { Text(if (freeClaimed) "✓" else "FREE • ◈ ${20 + tier * 5} SC", fontSize = 9.sp) }
                            OutlinedButton(onClick = { scope.launch { busy = true; val r = runCatching { backend?.claimSeasonReward(tier, true) ?: 0 }.getOrDefault(0); notice = if (r > 0) "+$r Son Coin" else sh("Premium ödül alınamadı.", "Premium reward unavailable."); reload(); busy = false } }, enabled = unlocked && d.seasonPassActive && !premiumClaimed && !busy, modifier = Modifier.weight(1f)) { Text(if (premiumClaimed) "✓" else "PASS • ◈ ${40 + tier * 10} SC", fontSize = 9.sp) }
                        }
                    }
                }
            }
        }
        if (notice.isNotBlank()) item { Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = SonHarfGold, fontSize = 10.sp) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SeasonPassCard(active: Boolean, onPurchased: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current; val activity = context as? Activity; val scope = rememberCoroutineScope(); var product by remember { mutableStateOf<ProductDetails?>(null) }; var busy by remember { mutableStateOf(false) }
    val manager = remember { BillingManager(context, onPurchase = { purchase -> scope.launch { busy = true; runCatching { PlayPurchaseVerification.verify(ProductCatalog.SEASON_PASS_MONTHLY, purchase.purchaseToken) }.onSuccess { onNotice(sh("Sezon Pass aktif.", "Season Pass activated.")); onPurchased() }.onFailure { onNotice(sh("Sezon Pass doğrulanamadı.", "Season Pass verification failed.")) }; busy = false } }, onMessage = onNotice) }
    DisposableEffect(manager) { manager.connect { manager.querySubscriptions(listOf(ProductCatalog.SEASON_PASS_MONTHLY)) { product = it[ProductCatalog.SEASON_PASS_MONTHLY] } }; onDispose { manager.close() } }
    Card(colors = CardDefaults.cardColors(containerColor = SonHarfGold.copy(alpha = .08f)), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .4f))) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("⚡ SEZON BİLETİ", color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 19.sp); Text(sh("Her sezon ekstra ödül yolu", "Extra reward track each season"), color = SonHarfMuted, fontSize = 9.sp) }; Text(if (active) sh("AKTİF", "ACTIVE") else product?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "PLAY", color = if (active) SonHarfGreen else SonHarfGold, fontWeight = FontWeight.Black) }
            Text(sh("Son Coin + ileride özel Style ödülleri. Skor, süre, rating veya lig avantajı vermez.", "Son Coin + future exclusive Style rewards. No score, time, rating or league advantage."), color = SonHarfText, fontSize = 10.sp)
            if (!active) Button(onClick = { val p = product; if (activity == null || p == null) onNotice(sh("Season Pass Google Play'de henüz yayınlanmamış olabilir.", "Season Pass may not be published on Google Play yet.")) else { busy = true; val result = manager.launchProduct(activity, p); if (result.responseCode != BillingClient.BillingResponseCode.OK) { busy = false; onNotice(sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not open.")) } } }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF251900))) { Text(if (busy) "…" else sh("SEZON BİLETİ AL", "GET SEASON PASS"), fontWeight = FontWeight.Black) }
        }
    }
}

@Composable private fun MetaMetric(icon: String, value: String, label: String, modifier: Modifier) { Surface(modifier = modifier, color = SonHarfSurface, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 20.sp); Text(value, fontWeight = FontWeight.Black); Text(label, color = SonHarfMuted, fontSize = 8.sp) } } }
@Composable private fun RecordLine(label: String, value: String) { Surface(color = SonHarfSurface, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = SonHarfMuted, fontSize = 10.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 10.sp) } } }
