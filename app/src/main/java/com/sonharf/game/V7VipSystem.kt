package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val V7Purple = Color(0xFF7C3AED)
private val V7PurpleLight = Color(0xFFEDE9FE)
private val V7Gold = Color(0xFFF59E0B)
private val V7VipBrush = Brush.linearGradient(listOf(V7Gold, V7Purple))

@Serializable
data class V7VipEntitlementsDto(
    @SerialName("is_vip") val isVip: Boolean = false,
    @SerialName("daily_jokers_claimed") val dailyJokersClaimed: Boolean = false,
    @SerialName("freezer_count") val freezerCount: Int = 0,
    @SerialName("swap_count") val swapCount: Int = 0,
    @SerialName("hint_count") val hintCount: Int = 0,
    @SerialName("streak_shield_count") val streakShieldCount: Int = 0,
    @SerialName("xp_multiplier") val xpMultiplier: Int = 1,
    @SerialName("diamond_multiplier") val diamondMultiplier: Int = 1,
    @SerialName("rewarded_ad_bypass") val rewardedAdBypass: Boolean = false,
    @SerialName("used_words_access") val usedWordsAccess: Boolean = false,
    @SerialName("direct_messages_access") val directMessagesAccess: Boolean = false,
)

@Serializable
data class V7JokerClaimDto(
    val success: Boolean = false,
    @SerialName("freezer_count") val freezerCount: Int = 0,
    @SerialName("swap_count") val swapCount: Int = 0,
    @SerialName("hint_count") val hintCount: Int = 0,
    @SerialName("streak_shield_count") val streakShieldCount: Int = 0,
)

@Serializable
data class V7RewardClaimDto(
    val success: Boolean = false,
    @SerialName("reward_type") val rewardType: String = "",
    @SerialName("diamonds_awarded") val diamondsAwarded: Int? = null,
    val diamonds: Int? = null,
    @SerialName("chest_keys_awarded") val chestKeysAwarded: Int? = null,
    @SerialName("trial_item_id") val trialItemId: String? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
    @SerialName("vip_bypass") val vipBypass: Boolean = false,
)

suspend fun OnlineGameBackend.getVipEntitlementsV7(): V7VipEntitlementsDto =
    SupabaseProvider.client.postgrest.rpc("get_vip_entitlements_v7").decodeSingle()

suspend fun OnlineGameBackend.claimVipDailyJokersV7(): V7JokerClaimDto =
    SupabaseProvider.client.postgrest.rpc("claim_vip_daily_jokers_v7").decodeSingle()

suspend fun OnlineGameBackend.claimOptionalRewardV7(rewardType: String, adResponseId: String? = null): V7RewardClaimDto =
    SupabaseProvider.client.postgrest.rpc(
        "claim_optional_reward_v7",
        buildJsonObject {
            put("p_reward_type", rewardType)
            if (!adResponseId.isNullOrBlank()) put("p_ad_response_id", adResponseId)
        },
    ).decodeSingle()

private tailrec fun Context.v7FindActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.v7FindActivity()
    else -> null
}

@Composable
fun V7VipHubScreen(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val context = LocalContext.current
    val activity = remember(context) { context.v7FindActivity() }
    val adController = remember { RewardedAdController(context) }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<V7VipEntitlementsDto?>(null) }
    var rewardStatus by remember { mutableStateOf<RewardCenterStatusDto?>(null) }
    var adReady by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var showPurchase by remember { mutableStateOf(false) }

    suspend fun reload() {
        val uid = backend.currentUserId() ?: return
        profile = runCatching { backend.getProfile(uid) }.getOrNull()
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
                    .onSuccess { notice = "+${it.diamondsAwarded ?: 20} elmas VIP ayrıcalığıyla anında eklendi."; reload() }
                    .onFailure { e -> notice = if ("daily_limit_reached" in e.message.orEmpty()) "Bugünkü ödül kotası tamamlandı." else "Ödül alınamadı." }
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
                        .onSuccess { notice = "+${it.diamondsAwarded ?: 10} elmas eklendi."; reload() }
                        .onFailure { e -> notice = if ("daily_limit_reached" in e.message.orEmpty()) "Bugünkü ödül kotası tamamlandı." else "Ödül işlenemedi." }
                    busy = false
                }
            },
            onUnavailable = { notice = "Ödüllü video şu an kullanılamıyor."; busy = false; adReady = false },
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
                Text("SON HARF VIP", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp, color = V7Purple)
                Spacer(Modifier.width(48.dp))
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(if (vip) 2.dp else 1.dp, if (vip) V7Purple else V6Light.border),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V7Avatar(profile?.avatarUrl, profile?.displayName ?: "Oyuncu", 56, vip)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile?.displayName ?: "Oyuncu", fontWeight = FontWeight.Black, color = V6Light.text, fontSize = 17.sp)
                            if (vip) {
                                Spacer(Modifier.width(7.dp))
                                Surface(shape = RoundedCornerShape(7.dp), color = V7Purple) { Text("VIP", Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp) }
                            }
                        }
                        Text(if (vip) "2x XP + 2x Elmas • Reklamsız ödül" else "VIP ayrıcalıklarını etkinleştir", color = V6Light.muted, fontSize = 12.sp)
                    }
                    Text("💎 ${profile?.diamonds ?: 0}", color = V6Light.blueDark, fontWeight = FontWeight.Black)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = if (vip) V7PurpleLight else Color.White, border = BorderStroke(1.dp, if (vip) V7Purple.copy(alpha = .35f) else V6Light.border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎁 Günlük Ekstra Elmas", fontWeight = FontWeight.Black, color = V6Light.text)
                    Text(if (vip) "VIP: video izlemeden anında al. Ödül de 2x." else "İsteğe bağlı ödüllü videoyu tamamla ve elmas kazan.", color = V6Light.muted, fontSize = 12.sp)
                    Button(
                        onClick = ::claimDiamondReward,
                        enabled = !busy && (vip || adReady) && (rewardStatus?.diamondAdsUsed ?: 0) < (rewardStatus?.diamondAdsLimit ?: 3),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (vip) V7Purple else V6Light.green),
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
                    Text("🛡 Seri Kalkanı: ${entitlements?.streakShieldCount ?: 0}", color = V7Purple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Button(
                        onClick = {
                            if (!vip) { showPurchase = true; return@Button }
                            scope.launch {
                                busy = true
                                runCatching { backend.claimVipDailyJokersV7() }
                                    .onSuccess { notice = "Günlük VIP joker paketin hesabına eklendi."; reload() }
                                    .onFailure { e -> notice = if ("already_claimed" in e.message.orEmpty()) "Bugünkü VIP joker paketini zaten aldın." else "Joker paketi alınamadı." }
                                busy = false
                            }
                        },
                        enabled = !busy && (entitlements?.dailyJokersClaimed != true || !vip),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (vip) V7Purple else V6Light.blue),
                    ) { Text(if (vip) "GÜNLÜK PAKETİ AL" else "🔒 VIP İLE AÇ", fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, V6Light.border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("VIP AYRICALIKLARI", color = V7Purple, fontWeight = FontWeight.Black)
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
                    if (!vip) Button(onClick = { showPurchase = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = V7Purple)) { Text("VIP ÜYELİĞİ İNCELE", fontWeight = FontWeight.Black) }
                }
            }
        }
        if (notice.isNotBlank()) item { Text(notice, Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center, color = V6Light.muted, fontSize = 12.sp) }
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
fun V7FriendsScreen(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var meProfile by remember { mutableStateOf<SocialProfileDto?>(null) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, SocialProfileDto>>>(emptyList()) }
    var selected by remember { mutableStateOf<SocialProfileDto?>(null) }
    var messages by remember { mutableStateOf<List<DirectMessageDto>>(emptyList()) }
    var avatarUrls by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var showPurchase by remember { mutableStateOf(false) }

    suspend fun loadFriends() {
        val uid = backend.currentUserId() ?: return
        meProfile = runCatching { backend.getSocialProfile(uid) }.getOrNull()
        friends = runCatching { backend.getAcceptedFriendProfiles() }.getOrDefault(emptyList())
        avatarUrls = friends.associate { (_, p) -> p.id to runCatching { AvatarSignedUrl.resolve(p.avatarPath) }.getOrNull() }
    }

    LaunchedEffect(Unit) { loadFriends() }
    LaunchedEffect(selected?.id) {
        while (isActive && selected != null) {
            messages = runCatching { backend.getDirectMessages(selected!!.id) }.getOrDefault(messages)
            delay(800)
        }
    }

    val isVip = meProfile?.isVip == true
    Column(Modifier.fillMaxSize().background(V6Light.bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (selected != null) selected = null else onBack() }) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = V6Light.text) }
            Text(if (selected == null) "ARKADAŞLAR & ÖZEL SOHBET" else selected!!.displayName, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, color = V6Light.text)
            Spacer(Modifier.width(48.dp))
        }
        if (selected == null) {
            Surface(shape = RoundedCornerShape(14.dp), color = if (isVip) V7PurpleLight else V6Light.blueLight) {
                Text(if (isVip) "💬 VIP özel mesajlaşma aktif." else "🔒 Mesaj gönderme VIP ayrıcalığıdır. Arkadaş listeni ve gelen mesajları yine görebilirsin.", Modifier.fillMaxWidth().padding(10.dp), color = if (isVip) V7Purple else V6Light.blueDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(friends, key = { it.second.id }) { (_, friend) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { selected = friend },
                        shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, V6Light.border),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            V7Avatar(avatarUrls[friend.id], friend.displayName, 46, friend.isVip)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(friend.displayName, color = V6Light.text, fontWeight = FontWeight.Bold)
                                    if (friend.isVip) { Spacer(Modifier.width(6.dp)); Text("VIP", color = V7Purple, fontWeight = FontWeight.Black, fontSize = 9.sp) }
                                }
                                Text(if (friend.presenceStatus == "online") "● Çevrimiçi" else "Çevrimdışı", color = if (friend.presenceStatus == "online") V6Light.green else V6Light.muted, fontSize = 11.sp)
                            }
                            Icon(Icons.Rounded.ChatBubble, null, tint = if (isVip) V6Light.blue else V6Light.muted)
                        }
                    }
                }
            }
        } else {
            val friend = selected!!
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                V7Avatar(avatarUrls[friend.id], friend.displayName, 52, friend.isVip)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(friend.displayName, fontWeight = FontWeight.Black, color = V6Light.text)
                    Text("${friend.wins}W • ${friend.losses}L", color = V6Light.muted, fontSize = 11.sp)
                }
                Button(
                    onClick = { scope.launch { runCatching { backend.inviteFriend(friend.id, SonHarfUiState.language) }.onSuccess { notice = "Ücretsiz düello daveti gönderildi." }.onFailure { notice = "Davet gönderilemedi." } } },
                    enabled = friend.presenceStatus == "online",
                    colors = ButtonDefaults.buttonColors(containerColor = V6Light.blue),
                ) { Text("⚔ DÜELLO", fontSize = 11.sp) }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(messages.takeLast(100), key = { it.id }) { msg ->
                    val mine = msg.senderId == backend.currentUserId()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(shape = RoundedCornerShape(13.dp), color = if (mine) V6Light.blue else Color.White, border = if (mine) null else BorderStroke(1.dp, V6Light.border)) {
                            Text(msg.body, Modifier.padding(horizontal = 11.dp, vertical = 8.dp).widthIn(max = 270.dp), color = if (mine) Color.White else V6Light.text, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (!isVip) {
                Surface(modifier = Modifier.fillMaxWidth().clickable { showPurchase = true }, shape = RoundedCornerShape(12.dp), color = V7PurpleLight) {
                    Text("🔒 Mesaj göndermek için VIP üyeliği aç", Modifier.padding(12.dp), color = V7Purple, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = input, onValueChange = { input = it.take(300) }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Mesaj yaz…") })
                    Spacer(Modifier.width(7.dp))
                    IconButton(
                        onClick = {
                            val body = input.trim(); if (body.isBlank()) return@IconButton
                            input = ""
                            scope.launch {
                                runCatching { backend.sendDirectMessage(friend.id, body) }
                                    .onSuccess { messages = runCatching { backend.getDirectMessages(friend.id) }.getOrDefault(messages) }
                                    .onFailure { notice = "Mesaj gönderilemedi." }
                            }
                        },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(V6Light.blue),
                    ) { Icon(Icons.Rounded.Send, "Gönder", tint = Color.White) }
                }
            }
            if (notice.isNotBlank()) Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = V6Light.muted, fontSize = 11.sp)
        }
    }

    if (showPurchase) VipPurchaseDialog(onVerified = { scope.launch { loadFriends() }; showPurchase = false }, onDismiss = { showPurchase = false })
}

@Composable
fun V7BattleVipOverlay() {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var showWords by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = backend.currentUserId() ?: return@LaunchedEffect
        profile = runCatching { backend.getProfile(uid) }.getOrNull()
        while (isActive) {
            val rooms = runCatching { SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>() }.getOrDefault(emptyList())
            room = rooms.filter { (it.hostId == uid || it.guestId == uid) && it.status in listOf("waiting","playing","quiz","final","sudden_death","paused") }.maxByOrNull { it.validWordCount }
            room?.let { active -> words = runCatching { backend.getWords(active.id) }.getOrDefault(words) }
            delay(900)
        }
    }

    val vip = profile?.isVip == true
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 58.dp, end = 10.dp), contentAlignment = Alignment.TopEnd) {
        Surface(
            modifier = Modifier.clickable { if (vip) showWords = true else showPurchase = true },
            shape = RoundedCornerShape(12.dp),
            color = if (vip) V7Purple else Color.White,
            border = BorderStroke(1.dp, if (vip) V7Gold else V6Light.border),
            shadowElevation = 3.dp,
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (vip) Icons.Rounded.MenuBook else Icons.Rounded.Lock, null, tint = if (vip) Color.White else V6Light.blueDark, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Çıkan Kelimeler", color = if (vip) Color.White else V6Light.blueDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                if (vip) { Spacer(Modifier.width(5.dp)); Text("VIP", color = V7Gold, fontWeight = FontWeight.Black, fontSize = 9.sp) }
            }
        }
    }

    if (showWords && vip) {
        AlertDialog(
            onDismissRequest = { showWords = false },
            title = { Text("📜 Çıkan Kelimeler", color = V7Purple, fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (words.isEmpty()) item { Text("Henüz oynanmış kelime yok.", color = V6Light.muted) }
                    items(words, key = { it.id }) { w ->
                        Surface(shape = RoundedCornerShape(10.dp), color = V6Light.bg) { Text(w.word.uppercase(), Modifier.fillMaxWidth().padding(9.dp), fontWeight = FontWeight.Bold, color = V6Light.text) }
                    }
                }
            },
            confirmButton = { Button(onClick = { showWords = false }, colors = ButtonDefaults.buttonColors(containerColor = V7Purple)) { Text("KAPAT") } },
        )
    }
    if (showPurchase) VipPurchaseDialog(onVerified = { scope.launch { backend.currentUserId()?.let { profile = backend.getProfile(it) } }; showPurchase = false }, onDismiss = { showPurchase = false })
}

@Composable
private fun V7Avatar(url: String?, name: String, size: Int, isVip: Boolean) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(if (isVip) V7Gold else V6Light.blue).padding(if (isVip) 3.dp else 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            if (!url.isNullOrBlank()) {
                AsyncImage(model = url, contentDescription = "$name profil fotoğrafı", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Text(name.take(1).uppercase(), color = if (isVip) V7Purple else V6Light.blueDark, fontWeight = FontWeight.Black, fontSize = (size / 2.2).sp)
            }
        }
    }
}
