package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameInviteDto
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReferenceGameEntry() {
    if (!SupabaseProvider.configured) { OnlineGameScreenV6(); return }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var active by remember { mutableStateOf<GameRoomDto?>(null) }
    var language by remember { mutableStateOf("tr") }
    var matching by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("Düelloya hazırsın.") }
    var showPrivate by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var privateCode by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<Pair<com.sonharf.game.data.FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }

    suspend fun findActive(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused") }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        if (backend.currentUserId() == null) runCatching { backend.ensurePlayer("Oyuncu") }
        profile = backend.currentUserId()?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        active = runCatching { findActive() }.getOrNull()
    }

    LaunchedEffect(matching) {
        while (matching && active == null) {
            val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
            if (found != null) { active = found; matching = false; SonHarfSoundFx.softNotify(); break }
            delay(900)
        }
    }

    if (active != null) { OnlineGameScreenV6(); return }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontSize = 30.sp, color = SonHarfText)
                Spacer(Modifier.width(8.dp))
                Text("DÜELLO", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF111A2A), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
                Text(if (language == "tr") "🇹🇷 Türkçe" else "🇬🇧 English", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 9.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF07111F)), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFF1B2A43))) {
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                repeat(4) { i -> Box(Modifier.size((235 - i * 38).dp).clip(CircleShape).background(Color.Transparent), contentAlignment = Alignment.Center) { Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, Color(0xFF1D2B50).copy(alpha = .55f))) {} } }
                Box(Modifier.size(168.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(SonHarfPurple, SonHarfCyan, SonHarfPurple))).padding(3.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF07111F)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (matching) "RAKİP" else "DÜELLO", fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Text(if (matching) "ARANIYOR" else "HAZIR", fontSize = 19.sp, fontWeight = FontWeight.Black)
                            if (matching) Text("•••", color = SonHarfCyan, letterSpacing = 3.sp)
                        }
                    }
                }
                if (matching) Text("Tahmini bekleme süresi: 5 - 10 sn", Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), color = SonHarfMuted, fontSize = 9.sp)
            }
        }

        if (matching) {
            Button(onClick = { scope.launch { matching = false; runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi." } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1837))) { Text("✕  İPTAL", fontWeight = FontWeight.Black) }
        } else {
            Button(onClick = {
                scope.launch {
                    runCatching { backend.startRandomMatchmaking(language) }
                        .onSuccess { matching = true; notice = "Rakip aranıyor…" }
                        .onFailure { notice = if (it.message.orEmpty().contains("player_already_in_game")) "Aktif maçına dönülüyor…" else "Bağlantı sorunu. Yeniden deneniyor." }
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)) { Text("DÜELLOYA GİR", fontWeight = FontWeight.Black) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { friends = runCatching { backend.getFriends() }.getOrDefault(emptyList()); invites = runCatching { backend.getIncomingGameInvites() }.getOrDefault(emptyList()); showFriends = !showFriends; showPrivate = false } }, modifier = Modifier.weight(1f).height(46.dp), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f))) { Text("👥 ARKADAŞ DAVET ET", fontSize = 9.sp) }
            OutlinedButton(onClick = { showPrivate = !showPrivate; showFriends = false }, modifier = Modifier.weight(1f).height(46.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .4f))) { Text("♛ ÖZEL ODA KATIL", fontSize = 9.sp) }
        }

        if (showPrivate) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF081322)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF1B2A43))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(9.dp), color = Color(0xFF2938A9)) { Text("Oda Oluştur", Modifier.padding(10.dp), textAlign = TextAlign.Center, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(6.dp))
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(9.dp), color = Color.Transparent) { Text("Oda Katıl", Modifier.padding(10.dp), textAlign = TextAlign.Center, fontSize = 9.sp) }
                    }
                    Text("ODA OLUŞTUR", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = language == "tr", onClick = { language = "tr" }, label = { Text("🇹🇷 Türkçe", fontSize = 8.sp) }, modifier = Modifier.weight(1f))
                        FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("🇬🇧 English", fontSize = 8.sp) }, modifier = Modifier.weight(1f))
                    }
                    Text("Round", color = SonHarfMuted, fontSize = 8.sp); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), color = SonHarfSurface2) { Text("3 Round⌄", Modifier.padding(10.dp), fontSize = 9.sp) }
                    Text("Kelime Sayısı (Her Round)", color = SonHarfMuted, fontSize = 8.sp); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), color = SonHarfSurface2) { Text("10 Kelime⌄", Modifier.padding(10.dp), fontSize = 9.sp) }
                    Button(onClick = { scope.launch { runCatching { backend.createPrivateRoom(language) }.onSuccess { active = it }.onFailure { notice = if (it.message.orEmpty().contains("vip_required")) "Özel oda açmak için VIP gerekli." else "Oda oluşturulamadı." } } }, modifier = Modifier.fillMaxWidth().height(45.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3AE5))) { Text("ODA OLUŞTUR", fontWeight = FontWeight.Black) }
                    HorizontalDivider(color = Color.White.copy(alpha = .06f))
                    OutlinedTextField(value = privateCode, onValueChange = { privateCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("ODA KODUNU GİR", fontSize = 9.sp) })
                    OutlinedButton(onClick = { scope.launch { runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { active = it }.onFailure { notice = "Odaya katılınamadı." } } }, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth()) { Text("ODA KODUYLA KATIL") }
                }
            }
        }

        if (showFriends) {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    invites.forEach { inv -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Maç daveti", fontSize = 9.sp); Row { TextButton(onClick = { scope.launch { runCatching { backend.respondGameInvite(inv.id, true) }.onSuccess { if (it != null) active = it } } }) { Text("Kabul", fontSize = 8.sp) }; TextButton(onClick = { scope.launch { runCatching { backend.respondGameInvite(inv.id, false) } } }) { Text("Reddet", fontSize = 8.sp) } } } }
                    friends.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(p.displayName, fontSize = 9.sp); Button(onClick = { scope.launch { runCatching { backend.inviteFriend(p.id, language) }; notice = "Davet gönderildi." } }, enabled = p.presenceStatus == "online") { Text("Davet", fontSize = 8.sp) } } }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF07111F)), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF1B2A43))) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Column { Text("İpucu", color = SonHarfGold, fontSize = 9.sp); Text("Uzun kelimeler daha fazla puan kazandırır!", color = SonHarfMuted, fontSize = 8.sp) }
            }
        }
        Text(notice, Modifier.fillMaxWidth(), color = SonHarfMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}
