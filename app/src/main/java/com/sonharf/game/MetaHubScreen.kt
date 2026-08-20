package com.sonharf.game

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
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

object SonHarfGameModeState {
    var mode by mutableStateOf("normal")
}

@Composable
fun MetaHubScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var tab by remember { mutableIntStateOf(0) }
    val labels = listOf(sh("Hedefler", "Goals"), sh("Lig", "League"), sh("Oyunlarım", "Games"), sh("Rehber", "Guide"), sh("Ayarlar", "Settings"))
    Column(Modifier.fillMaxSize()) {
        Text(sh("OYUNCU MERKEZİ", "PLAYER HUB"), Modifier.padding(16.dp), fontSize = 24.sp, fontWeight = FontWeight.Black)
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp, containerColor = SonHarfSurface) {
            labels.forEachIndexed { i, s -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(s, fontSize = 11.sp) }) }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> GoalsPanel(backend)
                1 -> LeaguePanel(backend)
                2 -> GamesPanel(backend)
                3 -> GuidePanel(backend)
                else -> HubSettingsPanel(backend)
            }
        }
    }
}

@Composable
private fun GoalsPanel(backend: OnlineGameBackend?) {
    val scope = rememberCoroutineScope()
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf<String?>(null) }
    suspend fun reload() { goals = runCatching { backend?.getGoals().orEmpty() }.getOrDefault(emptyList()) }
    LaunchedEffect(Unit) { reload() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfGold.copy(alpha = .10f)), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .4f))) {
                Column(Modifier.padding(14.dp)) {
                    Text("🎯 ${sh("HAFTALIK HEDEFLER", "WEEKLY GOALS")}", color = SonHarfGold, fontWeight = FontWeight.Black)
                    Text(sh("Her pazartesi yenilenir. Tamamla, elmasını al.", "Refreshes every Monday. Complete goals and claim diamonds."), color = SonHarfMuted, fontSize = 10.sp)
                }
            }
        }
        items(goals, key = { it.id }) { g ->
            val title = if (SonHarfUiState.isEnglish) g.titleEn else g.titleTr
            val desc = if (SonHarfUiState.isEnglish) g.descriptionEn else g.descriptionTr
            val done = g.progress >= g.target
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, fontWeight = FontWeight.Black)
                        Text("💎 ${g.rewardDiamonds}", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    }
                    Text(desc, color = SonHarfMuted, fontSize = 10.sp)
                    LinearProgressIndicator(progress = { (g.progress.toFloat() / g.target.coerceAtLeast(1)).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${g.progress.coerceAtMost(g.target)}/${g.target}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Button(onClick = {
                            scope.launch {
                                busy = g.id
                                runCatching { backend?.claimGoal(g.id) }
                                    .onSuccess { notice = sh("Hedef ödülü alındı.", "Goal reward claimed."); reload() }
                                    .onFailure { notice = sh("Ödül henüz alınamıyor.", "Reward is not available yet.") }
                                busy = null
                            }
                        }, enabled = done && !g.claimed && busy == null) {
                            Text(if (g.claimed) sh("ALINDI", "CLAIMED") else if (busy == g.id) "…" else sh("TOPLA", "CLAIM"))
                        }
                    }
                }
            }
        }
        if (notice.isNotBlank()) item { Text(notice, Modifier.fillMaxWidth(), color = SonHarfMuted, textAlign = TextAlign.Center, fontSize = 10.sp) }
    }
}

@Composable
private fun LeaguePanel(backend: OnlineGameBackend?) {
    var rows by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var language by remember { mutableStateOf(SonHarfUiState.language) }
    LaunchedEffect(language) { rows = runCatching { backend?.getLeaderboardV2(language, "week", 50).orEmpty() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfBlue.copy(alpha = .12f)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆 ${sh("SÜPER LİG", "SUPER LEAGUE")}", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(sh("Haftalık sıralama • Pazartesi yenilenir", "Weekly ranking • Resets Monday"), color = SonHarfMuted, fontSize = 10.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = language == "tr", onClick = { language = "tr" }, label = { Text("🇹🇷 TÜRKÇE") })
                FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("🇬🇧 ENGLISH") })
            }
        }
        if (rows.isEmpty()) item { Text(sh("Bu hafta henüz lig sonucu yok.", "No league results yet this week."), Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = SonHarfMuted) }
        items(rows.take(30)) { r ->
            val rank = rows.indexOf(r) + 1
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("#$rank", color = if (rank <= 3) SonHarfGold else SonHarfMuted, fontWeight = FontWeight.Black)
                    Text(r.displayName, Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                    Text("${r.wins}W • ${r.winRate.toInt()}%", color = SonHarfCyan, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun GamesPanel(backend: OnlineGameBackend?) {
    var games by remember { mutableStateOf<List<GameRoomDto>>(emptyList()) }
    var filter by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { games = runCatching { backend?.getMyGameHistory().orEmpty() }.getOrDefault(emptyList()) }
    val shown = when (filter) {
        0 -> games.filter { it.status != "finished" && it.status != "cancelled" }
        1 -> games.filter { it.status == "finished" }
        else -> games
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(sh("Aktif", "Active"), sh("Biten", "Finished"), sh("Tümü", "All")).forEachIndexed { i, s -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(s) }) }
            }
        }
        if (shown.isEmpty()) item { Text(sh("Bu bölümde oyun bulunmuyor.", "No games in this section."), Modifier.fillMaxWidth().padding(28.dp), textAlign = TextAlign.Center, color = SonHarfMuted) }
        items(shown.take(60), key = { it.id }) { r ->
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (r.isBot) "🤖 ${r.botName ?: "BOT"}" else "⚔ ${r.code}", fontWeight = FontWeight.Black)
                        Text("${r.language.uppercase()} • ${r.status.uppercase()} • R${r.roundNo}", color = SonHarfMuted, fontSize = 9.sp)
                    }
                    Text("${r.hostScore} - ${r.guestScore}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun GuidePanel(backend: OnlineGameBackend?) {
    var news by remember { mutableStateOf<List<AppNewsDto>>(emptyList()) }
    LaunchedEffect(Unit) { news = runCatching { backend?.getAppNews().orEmpty() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { HubInfoCard("📰", sh("HABERLER", "NEWS"), sh("Yeni modlar, sezonlar ve sistem duyuruları burada.", "New modes, seasons and system announcements appear here.")) }
        items(news) { n -> HubInfoCard("•", if (SonHarfUiState.isEnglish) n.titleEn else n.titleTr, if (SonHarfUiState.isEnglish) n.bodyEn else n.bodyTr) }
        item { HubInfoCard("ℹ", sh("KURALLAR", "RULES"), sh("Normal Mod: 3 round × 10 kelime. Uzman Modu: 15/15/15 kelime; round 1 son 1 harf, round 2 son 2 harf ×2, round 3 son 3 harf ×3. Türkçede Ğ ile başlayan veya Ğ ile biten kelimeler kabul edilmez.", "Normal Mode: 3 rounds × 10 words. Expert Mode: 15/15/15 words; round 1 uses the last 1 letter, round 2 the last 2 letters ×2, round 3 the last 3 letters ×3. Turkish words starting or ending with Ğ are not accepted.")) }
        item { HubInfoCard("?", sh("NASIL OYNANIR?", "HOW TO PLAY?"), sh("Sıra sende olduğunda istenen harflerle başlayan geçerli bir kelime yaz. Kelime tekrarı, yanlış başlangıç ve süre aşımı −1 puandır.", "On your turn, enter a valid word beginning with the required letters. Repeats, wrong prefixes and timeouts cost −1 point.")) }
        item { HubInfoCard("👥", sh("ARKADAŞLAR & DAVET", "FRIENDS & INVITES"), sh("Oyna ekranından arkadaş listeni açabilir, çevrimiçi arkadaşlarına maç daveti gönderebilirsin.", "Open your friends list from Play and invite online friends to a match.")) }
        item { HubInfoCard("✉", sh("BİZE ULAŞIN", "CONTACT"), sh("Geri bildirim ve hata raporlarını uygulama içindeki bildirim/rapor araçlarıyla iletebilirsin.", "Use the in-app report and feedback tools to send issues and suggestions.")) }
    }
}

@Composable
private fun HubSettingsPanel(backend: OnlineGameBackend?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var dark by remember { mutableStateOf(SonHarfPreferences.darkModeEnabled(context)) }
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }
    var notice by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val remote = runCatching { backend?.getPreferredGameMode() }.getOrNull()
        if (remote != null) { mode = remote; SonHarfGameModeState.mode = remote }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("OYUN MODU", "GAME MODE"), color = SonHarfCyan, fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == "normal", onClick = { mode = "normal"; scope.launch { runCatching { backend?.setPreferredGameMode("normal") }; SonHarfGameModeState.mode = "normal"; notice = sh("Normal Mod seçildi.", "Normal Mode selected.") } }, label = { Text(sh("NORMAL", "NORMAL")) })
                        FilterChip(selected = mode == "expert", onClick = { mode = "expert"; scope.launch { runCatching { backend?.setPreferredGameMode("expert") }; SonHarfGameModeState.mode = "expert"; notice = sh("Uzman Modu seçildi.", "Expert Mode selected.") } }, label = { Text(sh("UZMAN", "EXPERT")) })
                    }
                    Text(if (mode == "expert") sh("15/15/15 kelime • 1/2/3 harf zinciri • ×1/×2/×3 puan", "15/15/15 words • 1/2/3-letter chains • ×1/×2/×3 score") else sh("Klasik 3 × 10 kelimelik Son Harf", "Classic 3 × 10-word Last Letter"), color = SonHarfMuted, fontSize = 10.sp)
                }
            }
        }
        item { SettingToggleCard(sh("Ses Efektleri", "Sound Effects"), sound) { sound = it; SonHarfPreferences.setSoundEnabled(context, it) } }
        item { SettingToggleCard(sh("Titreşim", "Vibration"), vibration) { vibration = it; SonHarfPreferences.setVibrationEnabled(context, it) } }
        item { SettingToggleCard(sh("Karanlık Mod", "Dark Mode"), dark) { dark = it; SonHarfPreferences.setDarkModeEnabled(context, it) } }
        if (notice.isNotBlank()) item { Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = SonHarfMuted, fontSize = 10.sp) }
    }
}

@Composable private fun HubInfoCard(icon: String, title: String, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Text(icon, fontSize = 22.sp)
            Column { Text(title, fontWeight = FontWeight.Black); Text(text, color = SonHarfMuted, fontSize = 10.sp) }
        }
    }
}

@Composable private fun SettingToggleCard(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
