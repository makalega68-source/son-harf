from pathlib import Path


def replace(path: str, old: str, new: str, count: int = 1):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:120]!r}")
    text = text.replace(old, new, count)
    p.write_text(text)
    print('patched', path)

# Login screen cleanup.
replace(
    'app/src/main/java/com/sonharf/game/RequiredAuthGate.kt',
    '                                Text(if (rememberMe) "Oturum korunur" else "Sonraki açılışta çıkış", color = Color(0xFF697086), fontSize = 12.sp)\n',
    ''
)
replace(
    'app/src/main/java/com/sonharf/game/RequiredAuthGate.kt',
    '                        Text("E-posta doğrulaması tamamlanmadan oyun ekranları açılmaz.", color = Color(0xFF697086), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())\n',
    ''
)

# Bil Bakalim: keep all bottom actions inside the safe, full-screen viewport.
replace(
    'app/src/main/java/com/sonharf/game/BilBakalimFeature.kt',
    '            Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),',
    '            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 34.dp),'
)

# Expert arena: native Android keyboard, reliable clearing, profile identity and heartbeat timer.
p = Path('app/src/main/java/com/sonharf/game/ExpertArenaOverlay.kt')
t = p.read_text()
t = t.replace('import androidx.compose.foundation.BorderStroke\n', 'import androidx.compose.animation.core.RepeatMode\nimport androidx.compose.animation.core.animateFloat\nimport androidx.compose.animation.core.infiniteRepeatable\nimport androidx.compose.animation.core.rememberInfiniteTransition\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.BorderStroke\n')
t = t.replace('import androidx.compose.ui.draw.clip\n', 'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.scale\nimport androidx.compose.ui.focus.FocusRequester\nimport androidx.compose.ui.focus.focusRequester\n')
t = t.replace('import androidx.compose.ui.text.font.FontWeight\n', 'import androidx.compose.ui.platform.LocalSoftwareKeyboardController\nimport androidx.compose.ui.text.font.FontWeight\n')
t = t.replace('import kotlinx.coroutines.launch\n', 'import kotlinx.coroutines.launch\nimport java.time.Instant\n')
t = t.replace('    var chatInput by remember { mutableStateOf("") }\n', '    var chatInput by remember { mutableStateOf("") }\n    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }\n    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }\n')
t = t.replace(
'''            } else if (next != null) {
                room = next
                words = runCatching { backend.getWords(next.id) }.getOrDefault(words)
                if (showChat && !next.isBot && isVip) chat = runCatching { backend.getChat(next.id) }.getOrDefault(chat)
            }
''',
'''            } else if (next != null) {
                room = next
                words = runCatching { backend.getWords(next.id) }.getOrDefault(words)
                val me = backend.currentUserId()
                if (me != null) {
                    playerProfile = runCatching { backend.getProfile(me) }.getOrNull()
                    val opponentId = if (next.hostId == me) next.guestId else next.hostId
                    opponentProfile = if (next.isBot) null else opponentId?.let { runCatching { backend.getProfile(it) }.getOrNull() }
                }
                if (showChat && !next.isBot && isVip) chat = runCatching { backend.getChat(next.id) }.getOrDefault(chat)
            }
''')
t = t.replace(
'''    val myTurn = active.currentPlayerId == me && active.status in listOf("playing", "sudden_death")
    val suffixLen = active.roundNo.coerceIn(1, 3)
''',
'''    val myTurn = active.currentPlayerId == me && active.status in listOf("playing", "sudden_death")
    var seconds by remember(active.turnDeadline) { mutableIntStateOf(45) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val timerPulse by rememberInfiniteTransition(label = "expertTimerPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (seconds <= 10) 1.09f else 1f,
        animationSpec = infiniteRepeatable(tween(if (seconds <= 10) 420 else 1000), RepeatMode.Reverse),
        label = "expertTimerBeat",
    )
    LaunchedEffect(active.turnDeadline, active.currentPlayerId, active.status) {
        while (active.turnDeadline != null && active.status in listOf("playing", "sudden_death")) {
            seconds = runCatching { (Instant.parse(active.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            delay(250)
        }
    }
    LaunchedEffect(myTurn, active.id) {
        if (myTurn) {
            delay(140)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
    val suffixLen = active.roundNo.coerceIn(1, 3)
''')
t = t.replace(
'''        scope.launch {
            busy = true
            runCatching { backend.submitWord(active.id, submitted) }
                .onSuccess { r ->
                    room = r
                    input = ""
''',
'''        input = ""
        scope.launch {
            busy = true
            runCatching { backend.submitWord(active.id, submitted) }
                .onSuccess { r ->
                    room = r
''')
t = t.replace(
'''                ExpertPlayerCard(sh("SEN", "YOU"), myScore, myRounds, myTurn, Modifier.weight(1f))
                Surface(shape = CircleShape, color = SonHarfGold.copy(alpha = .16f), border = BorderStroke(2.dp, SonHarfGold)) {
                    Column(Modifier.size(64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("×$suffixLen", color = SonHarfGold, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text(sh("PUAN", "SCORE"), color = SonHarfMuted, fontSize = 7.sp)
                    }
                }
                ExpertPlayerCard(if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else sh("RAKİP", "OPPONENT"), oppScore, oppRounds, !myTurn, Modifier.weight(1f))
''',
'''                ExpertPlayerCard(playerProfile?.displayName ?: sh("SEN", "YOU"), playerProfile?.avatarPath, playerProfile?.gender, myScore, myRounds, myTurn, Modifier.weight(1f))
                Surface(
                    modifier = Modifier.scale(timerPulse),
                    shape = CircleShape,
                    color = if (seconds <= 10) SonHarfPink.copy(alpha = .16f) else SonHarfGold.copy(alpha = .16f),
                    border = BorderStroke(2.dp, if (seconds <= 10) SonHarfPink else SonHarfGold),
                ) {
                    Column(Modifier.size(72.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("$seconds", color = if (seconds <= 10) SonHarfPink else SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("sn • ×$suffixLen", color = if (seconds <= 10) SonHarfPink else SonHarfGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                ExpertPlayerCard(if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: sh("RAKİP", "OPPONENT"), opponentProfile?.avatarPath, opponentProfile?.gender, oppScore, oppRounds, !myTurn, Modifier.weight(1f), isBot = active.isBot)
''')
t = t.replace(
'''                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${sh("UZMAN", "EXPERT")} • ROUND ${active.roundNo}/3", color = SonHarfGold, fontWeight = FontWeight.Black)
                        Text("${active.roundWordCount}/15", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    }
''',
'''                    Box(Modifier.fillMaxWidth()) {
                        Text("${sh("UZMAN", "EXPERT")} • ROUND ${active.roundNo}/3", color = SonHarfGold, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                        Text("${active.roundWordCount}/15", color = SonHarfCyan, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterEnd))
                    }
''')
t = t.replace('                modifier = Modifier.fillMaxWidth(),\n                placeholder = { Text(if (myTurn)', '                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),\n                placeholder = { Text(if (myTurn)', 1)
old_func = '''@Composable
private fun ExpertPlayerCard(name: String, score: Int, rounds: Int, active: Boolean, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (active) SonHarfCyan.copy(alpha = .11f) else SonHarfSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (active) SonHarfCyan.copy(alpha = .5f) else SonHarfMuted.copy(alpha = .12f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, maxLines = 1, fontSize = 9.sp, color = SonHarfMuted)
            Text("$score", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("$rounds ${sh("round", "round")}", fontSize = 7.sp, color = SonHarfMuted)
        }
    }
}
'''
new_func = '''@Composable
private fun ExpertPlayerCard(name: String, avatarPath: String?, gender: String?, score: Int, rounds: Int, active: Boolean, modifier: Modifier, isBot: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (active) SonHarfCyan.copy(alpha = .11f) else SonHarfSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (active) SonHarfCyan.copy(alpha = .5f) else SonHarfMuted.copy(alpha = .12f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (isBot) {
                Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = SonHarfPink.copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Text("🤖", fontSize = 23.sp) } }
            } else {
                Box {
                    ProfilePhotoAvatar(avatarPath, name, 42.dp, visible = true, accent = if (active) SonHarfCyan else SonHarfPurple)
                    val g = gender?.trim()?.lowercase()
                    val female = g in setOf("kadın", "kadin", "female", "woman")
                    val male = g in setOf("erkek", "male", "man")
                    if (female || male) {
                        Surface(Modifier.align(Alignment.BottomEnd).size(15.dp), shape = CircleShape, color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2), border = BorderStroke(1.dp, Color.White)) {
                            Box(contentAlignment = Alignment.Center) { Text(if (female) "♀" else "♂", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name, maxLines = 1, fontSize = 9.sp, color = SonHarfMuted, textAlign = TextAlign.Center)
                Text("$score", fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("$rounds ${sh("round", "round")}", fontSize = 7.sp, color = SonHarfMuted)
            }
        }
    }
}
'''
if old_func not in t:
    raise SystemExit('ExpertPlayerCard anchor missing')
t = t.replace(old_func, new_func)
p.write_text(t)
print('patched ExpertArenaOverlay.kt')

# Result summary: make it a clean opaque full-screen layer and improve hierarchy.
replace(
    'app/src/main/java/com/sonharf/game/ComboOverlayV9.kt',
    '        Box(Modifier.fillMaxSize().navigationBarsPadding().padding(12.dp),contentAlignment=Alignment.BottomCenter) {',
    '        Box(Modifier.fillMaxSize().background(SonHarfBg).statusBarsPadding().navigationBarsPadding().padding(14.dp),contentAlignment=Alignment.Center) {'
)
replace(
    'app/src/main/java/com/sonharf/game/ComboOverlayV9.kt',
    '            Card(colors=CardDefaults.cardColors(containerColor=SonHarfSurface.copy(alpha=.98f)),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,if(won)SonHarfGold.copy(alpha=.55f) else SonHarfCyan.copy(alpha=.25f))) {',
    '            Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=SonHarfSurface),shape=RoundedCornerShape(26.dp),border=BorderStroke(2.dp,if(won)SonHarfGold.copy(alpha=.75f) else SonHarfCyan.copy(alpha=.45f)),elevation=CardDefaults.cardElevation(defaultElevation=8.dp)) {'
)
replace(
    'app/src/main/java/com/sonharf/game/ComboOverlayV9.kt',
    '                        Column { Text(if(won)"🏆 ${sh("ZAFER ÖZETİ","VICTORY SUMMARY")}" else "📊 ${sh("MAÇ ÖZETİ","MATCH SUMMARY")}",fontWeight=FontWeight.Black);Text("$myRounds - $oppRounds  •  $myScore - $oppScore",color=SonHarfMuted,fontSize=10.sp) }',
    '                        Column { Text(if(won)"🏆 ${sh("ZAFER ÖZETİ","VICTORY SUMMARY")}" else "📊 ${sh("MAÇ ÖZETİ","MATCH SUMMARY")}",fontWeight=FontWeight.Black,fontSize=23.sp,color=SonHarfText);Text("$myRounds - $oppRounds  •  $myScore - $oppScore",color=SonHarfText,fontSize=14.sp,fontWeight=FontWeight.Bold) }'
)
replace(
    'app/src/main/java/com/sonharf/game/ComboOverlayV9.kt',
    '@Composable private fun SummaryMetric(icon:String,value:String,label:String,modifier:Modifier){\n    Surface(modifier=modifier,shape=RoundedCornerShape(13.dp),color=SonHarfSurface2){Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon,fontSize=16.sp);Text(value,maxLines=1,fontWeight=FontWeight.Black,fontSize=11.sp);Text(label,color=SonHarfMuted,fontSize=7.sp)}}\n}',
    '@Composable private fun SummaryMetric(icon:String,value:String,label:String,modifier:Modifier){\n    Surface(modifier=modifier,shape=RoundedCornerShape(15.dp),color=SonHarfSurface2,border=BorderStroke(1.dp,SonHarfCyan.copy(alpha=.24f))){Column(Modifier.padding(vertical=12.dp,horizontal=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon,fontSize=20.sp);Text(value,maxLines=1,fontWeight=FontWeight.Black,fontSize=15.sp,color=SonHarfText);Text(label,color=SonHarfMuted,fontSize=9.sp,fontWeight=FontWeight.Bold)}}\n}'
)

# Admin DTOs and RPC wrappers.
p = Path('app/src/main/java/com/sonharf/game/data/AdminConsole.kt')
t = p.read_text()
insert = '''\n@Serializable\ndata class AdminMonthlyRevenueDto(\n    val month: String,\n    @SerialName("revenue_minor") val revenueMinor: Long = 0,\n    val currency: String = "TRY",\n)\n\n@Serializable\ndata class AdminAnnouncementDto(\n    val message: String = "",\n    val enabled: Boolean = false,\n)\n'''
anchor = '@Serializable\ndata class AdminHealthDto('
if insert.strip() not in t:
    t = t.replace(anchor, insert + '\n' + anchor)
append = '''\n\nsuspend fun OnlineGameBackend.getAdminMonthlyRevenue(): List<AdminMonthlyRevenueDto> =\n    SupabaseProvider.client.postgrest.rpc("admin_monthly_revenue_v1").decodeList()\n\nsuspend fun OnlineGameBackend.getAdminAnnouncement(): AdminAnnouncementDto =\n    SupabaseProvider.client.postgrest.rpc("admin_get_announcement_v1").decodeSingle()\n\nsuspend fun OnlineGameBackend.adminSetAnnouncement(message: String, enabled: Boolean) {\n    SupabaseProvider.client.postgrest.rpc(\n        "admin_set_announcement_v1",\n        buildJsonObject { put("p_message", message.take(500)); put("p_enabled", enabled) },\n    )\n}\n'''
if 'getAdminMonthlyRevenue' not in t:
    t += append
p.write_text(t)
print('patched AdminConsole.kt')

# Admin UI: monthly revenue + editable announcement board.
p = Path('app/src/main/java/com/sonharf/game/AdminConsoleScreen.kt')
t = p.read_text()
t = t.replace('    var health by remember { mutableStateOf<List<AdminHealthDto>>(emptyList()) }\n', '    var health by remember { mutableStateOf<List<AdminHealthDto>>(emptyList()) }\n    var monthlyRevenue by remember { mutableStateOf<List<AdminMonthlyRevenueDto>>(emptyList()) }\n    var announcement by remember { mutableStateOf(AdminAnnouncementDto()) }\n    var announcementText by remember { mutableStateOf("") }\n    var announcementEnabled by remember { mutableStateOf(false) }\n')
t = t.replace('            health = backend.getAdminHealth()\n', '            health = backend.getAdminHealth()\n            monthlyRevenue = backend.getAdminMonthlyRevenue()\n            announcement = backend.getAdminAnnouncement()\n            announcementText = announcement.message\n            announcementEnabled = announcement.enabled\n')
anchor = '            item { AdminSectionTitle("OYUN TERCİHİ", Icons.Rounded.SportsEsports) }\n'
block = '''            item { AdminSectionTitle("AYLIK GELİR", Icons.Rounded.CalendarMonth) }\n            item {\n                AdminWideCard {\n                    val current = monthlyRevenue.firstOrNull()\n                    Text("Bu ay kayıtlı brüt gelir", color = AdminMuted, fontSize = 12.sp)\n                    Text(formatMoney(current?.revenueMinor ?: 0, current?.currency ?: "TRY"), color = AdminGold, fontSize = 28.sp, fontWeight = FontWeight.Black)\n                    monthlyRevenue.take(6).forEach { m -> AdminSimpleRow(m.month, formatMoney(m.revenueMinor, m.currency)) }\n                }\n            }\n\n            item { AdminSectionTitle("DUYURU PANOSU", Icons.Rounded.Campaign) }\n            item {\n                AdminWideCard {\n                    OutlinedTextField(announcementText, { announcementText = it.take(500) }, modifier = Modifier.fillMaxWidth(), label = { Text("Duyuru metni") }, minLines = 2, maxLines = 5)\n                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {\n                        Text("Duyuruyu yayınla", color = AdminText, fontWeight = FontWeight.Bold)\n                        Switch(checked = announcementEnabled, onCheckedChange = { announcementEnabled = it })\n                    }\n                    Button(onClick = {\n                        scope.launch {\n                            busy = true\n                            runCatching { backend.adminSetAnnouncement(announcementText.trim(), announcementEnabled) }\n                                .onSuccess { notice = "Duyuru panosu güncellendi." }\n                                .onFailure { error = it.message ?: "Duyuru güncellenemedi." }\n                            reload(); busy = false\n                        }\n                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("DUYURUYU KAYDET") }\n                }\n            }\n\n'''
if 'DUYURU PANOSU' not in t:
    if anchor not in t: raise SystemExit('admin UI anchor missing')
    t = t.replace(anchor, block + anchor)
p.write_text(t)
print('patched AdminConsoleScreen.kt')

print('Gameplay/admin polish patch applied.')
