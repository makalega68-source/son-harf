package com.sonharf.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.claimVipDailyHelpers
import com.sonharf.game.data.getVipEntitlements
import kotlinx.coroutines.launch

private val UProBg = Color(0xFF020617)
private val UProSurface = Color(0xFF0F172A)
private val UProBorder = Color(0xFF334155)
private val UProText = Color(0xFFF8FAFC)
private val UProMuted = Color(0xFF94A3B8)
private val UProBlue = Color(0xFF3B82F6)
private val UProGold = Color(0xFFF59E0B)
private val UProGreen = Color(0xFF10B981)

@Composable
internal fun UnifiedProVipScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var claiming by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        entitlements = if (profile?.isVip == true) runCatching { backend.getVipEntitlements() }.getOrNull() else null
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    val active = profile?.isVip == true
    val e = entitlements

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = UProText) }
                Column(Modifier.weight(1f)) {
                    Text("SON HARF PRO", color = UProText, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(sh("Premier üyelik ve oyun yardımcıları", "Premier membership and game helpers"), color = UProBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Rounded.WorkspacePremium, null, tint = UProGold, modifier = Modifier.size(30.dp))
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UProBlue, trackColor = UProBorder) }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = UProSurface,
                border = BorderStroke(1.dp, if (active) UProGold else UProBorder),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(if (active) sh("PRO AKTİF", "PRO ACTIVE") else sh("FREE PLAN", "FREE PLAN"), color = if (active) UProGold else UProMuted, fontWeight = FontWeight.Black)
                    Text(profile?.displayName ?: sh("Oyuncu", "Player"), color = UProText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (active) sh("Reklamsız deneyim + sunucu doğrulamalı oyun yardımcıları", "Ad-free experience + server-validated game helpers")
                        else sh("PRO ile günlük takviyeleri ve premium özellikleri aç.", "Unlock daily boosters and premium features with PRO."),
                        color = UProMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProResourceCard("💡", sh("İPUCU", "HINT"), e?.hintCount ?: 0, UProBlue, Modifier.weight(1f))
                ProResourceCard("🔄", sh("DEĞİŞTİR", "SWAP"), e?.swapCount ?: 0, UProGreen, Modifier.weight(1f))
                ProResourceCard("×2", sh("SKOR", "SCORE"), e?.multiplierCount ?: 0, UProGold, Modifier.weight(1f))
            }
        }

        if (active) item {
            Button(
                onClick = {
                    scope.launch {
                        claiming = true
                        runCatching { backend.claimVipDailyHelpers() }
                            .onSuccess { result ->
                                notice = if (result.alreadyClaimed) {
                                    sh("Bugünkü PRO takviye paketini zaten aldın.", "You already claimed today's PRO booster pack.")
                                } else {
                                    sh("+1 İpucu, +1 Harf Değiştirici ve +1 2x Skor eklendi.", "+1 Hint, +1 Letter Swap and +1 2x Score added.")
                                }
                                reload()
                            }
                            .onFailure { notice = sh("Günlük PRO paketi alınamadı.", "Daily PRO pack could not be claimed.") }
                        claiming = false
                    }
                },
                enabled = !claiming && e?.dailyJokersClaimed != true,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UProGold, contentColor = Color(0xFF211600)),
            ) {
                Text(
                    when {
                        claiming -> "…"
                        e?.dailyJokersClaimed == true -> sh("BUGÜN ALINDI", "CLAIMED TODAY")
                        else -> sh("GÜNLÜK PRO TAKVİYELERİNİ AL", "CLAIM DAILY PRO BOOSTERS")
                    },
                    fontWeight = FontWeight.Black,
                )
            }
        }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = UProSurface, border = BorderStroke(1.dp, UProBorder)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProLine("🚫", sh("Reklamsız menü, profil ve mağaza", "Ad-free menus, profile and shop"))
                    ProLine("💡", sh("İpucu fısıltısı", "Hint whisper"))
                    ProLine("🔄", sh("Harf Değiştirici", "Letter Swap"))
                    ProLine("×2", sh("2x Skor Çarpanı", "2x Score Multiplier"))
                    ProLine("📊", sh("Gelişmiş istatistik ve maç analizi", "Advanced stats and match analysis"))
                    ProLine("♛", sh("Özel oda ve PRO Style ayrıcalıkları", "Private rooms and PRO Style benefits"))
                    Text(sh("Oyun etkileri istemciden değil, sunucu tarafından doğrulanır ve tüketilir.", "Gameplay effects are validated and consumed by the server, not the client."), color = UProMuted, fontSize = 9.sp)
                }
            }
        }

        item {
            if (active) {
                OutlinedButton(
                    onClick = {
                        val url = "https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, UProBlue),
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, tint = UProBlue)
                    Spacer(Modifier.width(7.dp))
                    Text(sh("GOOGLE PLAY'DE YÖNET", "MANAGE ON GOOGLE PLAY"), color = UProBlue, fontWeight = FontWeight.Black)
                }
            } else {
                Button(
                    onClick = { showPurchase = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UProBlue),
                ) { Text(sh("PRO PLANLARINI GÖR", "VIEW PRO PLANS"), fontWeight = FontWeight.Black) }
            }
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = UProBlue.copy(alpha = .12f)) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = UProText, fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPurchase) {
        VipPurchaseDialog(onVerified = { scope.launch { reload() } }, onDismiss = { showPurchase = false })
    }
}

@Composable
private fun ProResourceCard(icon: String, label: String, count: Int, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = UProSurface, border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("$count", color = UProText, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = UProMuted, fontSize = 7.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun ProLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(9.dp))
        Text(text, color = UProText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
