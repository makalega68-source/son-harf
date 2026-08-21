package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonharf.game.data.AvatarSignedUrl
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import kotlinx.coroutines.launch

private val V9VipPurple = Color(0xFF7C3AED)
private val V9VipPurpleLight = Color(0xFFEDE9FE)
private val V9VipGold = Color(0xFFF59E0B)

private tailrec fun Context.v9FindActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.v9FindActivity()
    else -> null
}

/**
 * Active VIP hub using the same server-authoritative VIP/reward operations as V7,
 * while resolving the current profile photo from profiles.avatar_path.
 */
@Composable
fun V9VipHubScreen(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val context = LocalContext.current
    val activity = remember(context) { context.v9FindActivity() }
    val adController = remember { RewardedAdController(context) }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var entitlements by remember { mutableStateOf<V7VipEntitlementsDto?>(null) }
    var rewardStatus by remember { mutableStateOf<com.sonharf.game.data.RewardCenterStatusDto?>(null) }
    var adReady by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var showPurchase by remember { mutableStateOf(false) }

    suspend fun reload() {
        val uid = backend.currentUserId() ?: return
        profile = runCatching { backend.getProfile(uid) }.getOrNull()
        val photoProfile = runCatching { v6LoadProfile(uid) }.getOrNull()
        avatarUrl = runCatching { AvatarSignedUrl.resolve(photoProfile?.avatarPath) }.getOrNull()
        entitlements = runCatching { backend.getVipEntitlementsV7() }.getOrNull()
        rewardStatus = runCatching { backend.getRewardCenterStatus() }.getOrNull()
    }

    LaunchedEffect(Unit) {
        reload()
        adController.load { adReady = adController.ready }
    }

    fun claimDiamondReward() {
        if (busy) return
        val vip = entitlements?.isVip == true
        if (vip) {
            scope.launch {
                busy = true
                runCatching { backend.claimOptionalRewardV7("diamonds") }
                    .onSuccess {
                        notice = "+${it.diamondsAwarded ?: 20} elmas VIP ayrıcalığıyla anında eklendi."
                        reload()
                    }
                    .onFailure { e ->
                        notice = if ("daily_limit_reached" in e.message.orEmpty()) "Bugünkü ödül kotası tamamlandı." else "Ödül alınamadı."
                    }
                busy = false
            }
            return
        }
        val a = activity
        if (a == null || !adReady) {
            notice = "Ödüllü video henüz hazır değil. Biraz sonra tekrar dene."
            adController.load { adReady = adController.ready }
            return
        }
        busy = true
        adController.show(
            a,
            onEarned = { proof ->
                scope.launch {
                    runCatching { backend.claimOptionalRewardV7("diamonds", proof) }
                        .onSuccess {
                            notice = "+${it.diamondsAwarded ?: 10} elmas eklendi."
                            reload()
                        }
                        .onFailure { e ->
                            notice = if ("daily_limit_reached" in e.message.orEmpty()) "Bugünkü ödül kotası tamamlandı." else "Ödül işlenemedi."
                        }
                    busy = false
                }
            },
            onUnavailable = {
                notice = "Ödüllü video şu an kullanılamıyor."
                busy = false
                adReady = false
            },
            onClosed = { adController.load { adReady = adController.ready } },
        )
    }

    val vip = entitlements?.isVip == true
    LazyColumn(
        Modifier.fillMaxSize().background(V6Light.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = V6Light.text) }
                Text("SON HARF VIP", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp, color = V9VipPurple)
                Spacer(Modifier.width(48.dp))
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(if (vip) 2.dp else 1.dp, if (vip) V9VipPurple else V6Light.border),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V9VipAvatar(avatarUrl, profile?.displayName ?: "Oyuncu", 56, vip)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile?.displayName ?: "Oyuncu", fontWeight = FontWeight.Black, color = V6Light.text, fontSize = 17.sp)
                            if (vip) {
                                Spacer(Modifier.width(7.dp))
                                Surface(shape = RoundedCornerShape(7.dp), color = V9VipPurple) {
                                    Text("VIP", Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }
                        Text(if (vip) "2x XP + 2x Elmas • Reklamsız ödül" else "VIP ayrıcalıklarını etkinleştir", color = V6Light.muted, fontSize = 12.sp)
                    }
                    Text("💎 ${profile?.diamonds ?: 0}", color = V6Light.blueDark, fontWeight = FontWeight.Black)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = if (vip) V9VipPurpleLight else Color.White, border = BorderStroke(1.dp, if (vip) V9VipPurple.copy(alpha = .35f) else V6Light.border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎁 Günlük Ekstra Elmas", fontWeight = FontWeight.Black, color = V6Light.text)
                    Text(if (vip) "VIP: video izlemeden anında al. Ödül de 2x." else "İsteğe bağlı ödüllü videoyu tamamla ve elmas kazan.", color = V6Light.muted, fontSize = 12.sp)
                    Button(
                        onClick = ::claimDiamondReward,
                        enabled = !busy && (vip || adReady) && (rewardStatus?.diamondAdsUsed ?: 0) < (rewardStatus?.diamondAdsLimit ?: 3),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (vip) V9VipPurple else V6Light.green),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(if (vip) Icons.Rounded.Bolt else Icons.Rounded.PlayCircle, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (vip) "VIP İLE ANINDA AL" else "REKLAM İZLE VE AL", fontWeight = FontWeight.Black)
                    }
                    Text("Bugün: ${rewardStatus?.diamondAdsUsed ?: 0}/${rewardStatus?.diamondAdsLimit ?: 3}", color = V6Light.muted, fontSize = 11.sp)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, V6Light.border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎁 Günlük VIP Joker Paketi", fontWeight = FontWeight.Black, color = V6Light.text)
                    Text("❄️ Dondurucu  ${entitlements?.freezerCount ?: 0}   •   🔀 Değiştirici  ${entitlements?.swapCount ?: 0}   •   💡 İpucu  ${entitlements?.hintCount ?: 0}", color = V6Light.text, fontSize = 12.sp)
                    Text("🛡 Seri Kalkanı: ${entitlements?.streakShieldCount ?: 0}", color = V9VipPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Button(
                        onClick = {
                            if (!vip) {
                                showPurchase = true
                                return@Button
                            }
                            scope.launch {
                                busy = true
                                runCatching { backend.claimVipDailyJokersV7() }
                                    .onSuccess {
                                        notice = "Günlük VIP joker paketin hesabına eklendi."
                                        reload()
                                    }
                                    .onFailure { e ->
                                        notice = if ("already_claimed" in e.message.orEmpty()) "Bugünkü VIP joker paketini zaten aldın." else "Joker paketi alınamadı."
                                    }
                                busy = false
                            }
                        },
                        enabled = !busy && (entitlements?.dailyJokersClaimed != true || !vip),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (vip) V9VipPurple else V6Light.blue),
                    ) { Text(if (vip) "GÜNLÜK PAKETİ AL" else "🔒 VIP İLE AÇ", fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, V6Light.border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("VIP AYRICALIKLARI", color = V9VipPurple, fontWeight = FontWeight.Black)
                    listOf(
                        "⚡ VIP olduğun sürede kazanılan XP için 2x ilerleme",
                        "💎 Günlük, görev ve ödül sandığı elmaslarında 2x",
                        "📜 Maç içinde çıkan tüm kelimeleri canlı görme",
                        "💬 Arkadaşlarla birebir özel mesaj gönderme",
                        "🛡 Günlük Seri Koruma Kalkanı",
                        "👑 Altın/Mor VIP profil çerçevesi",
                        "⚔ Arkadaş düelloları ücretsiz ve limitsiz",
                        "🎬 Zorunlu reklam yok; ödüllü reklamlarda VIP anında geçer",
                    ).forEach { Text(it, color = V6Light.text, fontSize = 12.sp) }
                    if (!vip) {
                        Button(onClick = { showPurchase = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = V9VipPurple)) {
                            Text("VIP ÜYELİĞİ İNCELE", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        if (notice.isNotBlank()) {
            item { Text(notice, Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center, color = V6Light.muted, fontSize = 12.sp) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showPurchase) {
        VipPurchaseDialog(
            onVerified = { scope.launch { reload() }; showPurchase = false },
            onDismiss = { showPurchase = false },
        )
    }
}

@Composable
private fun V9VipAvatar(url: String?, name: String, size: Int, isVip: Boolean) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(if (isVip) V9VipGold else V6Light.blue).padding(if (isVip) 3.dp else 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            if (!url.isNullOrBlank() && !failed) {
                AsyncImage(
                    model = url,
                    contentDescription = "$name profil fotoğrafı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    onError = { failed = true },
                )
            } else {
                Text(name.take(1).uppercase(), color = if (isVip) V9VipPurple else V6Light.blueDark, fontWeight = FontWeight.Black, fontSize = (size / 2.2).sp)
            }
        }
    }
}
