from pathlib import Path
import re


def patch(path, fn):
    p = Path(path)
    text = p.read_text()
    new = fn(text)
    if new == text:
        print(f'no-change {path}')
    else:
        p.write_text(new)
        print(f'patched {path}')


def once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'missing target: {label}')
    return text.replace(old, new, 1)

# Shared runtime state: reliable home navigation and match back handling.
def ui_state(text):
    return once(text,
        '    var language by mutableStateOf("tr")\n\n    val isEnglish: Boolean get() = language == "en"',
        '    var language by mutableStateOf("tr")\n    var inMatch by mutableStateOf(false)\n    var homeRequest by mutableStateOf(0)\n\n    val isEnglish: Boolean get() = language == "en"',
        'ui navigation state')
patch('app/src/main/java/com/sonharf/game/SonHarfUiState.kt', ui_state)

# White + sky-blue application palette.
def main_activity(text):
    repl = {
        'Color(0xFF071525)': 'Color(0xFFF7FBFF)',
        'Color(0xFF10253A)': 'Color(0xFFF7FBFF)',
        'Color(0xFF0D2033)': 'Color(0xFFFFFFFF)',
        'Color(0xFF173149)': 'Color(0xFFFFFFFF)',
        'Color(0xFF132A40)': 'Color(0xFFEAF7FF)',
        'Color(0xFF1C3953)': 'Color(0xFFEAF7FF)',
        'Color(0xFF9B8667)': 'Color(0xFF56BDE8)',
        'Color(0xFF84AFCB)': 'Color(0xFF55C2F0)',
        'Color(0xFF6F94B0)': 'Color(0xFF3DAFE0)',
        'Color(0xFFD8AD62)': 'Color(0xFF55C2F0)',
        'Color(0xFF7DA887)': 'Color(0xFF39B978)',
        'Color(0xFFF4F6F8)': 'Color(0xFF16324A)',
        'Color(0xFFA9B6C3)': 'Color(0xFF698296)',
        'Color(0xFFB98B8B)': 'Color(0xFFEF7C8E)',
    }
    for a,b in repl.items(): text=text.replace(a,b)
    return text
patch('app/src/main/java/com/sonharf/game/MainActivity.kt', main_activity)

# Profile DTO exposes the private storage path so every game surface can render it.
def backend(text):
    return once(text,
        '    @SerialName("avatar_url") val avatarUrl: String? = null,\n    @SerialName("avatar_visibility") val avatarVisibility: String = "hidden",',
        '    @SerialName("avatar_url") val avatarUrl: String? = null,\n    @SerialName("avatar_path") val avatarPath: String? = null,\n    @SerialName("avatar_visibility") val avatarVisibility: String = "visible",',
        'profile avatar path')
patch('app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt', backend)

# Compress uploads to a small high-quality WEBP and make newly uploaded photos visible by default.
def profile(text):
    text = text.replace('@SerialName("avatar_visibility") val avatarVisibility: String = "hidden",', '@SerialName("avatar_visibility") val avatarVisibility: String = "visible",')
    old = '''                        val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("photo_read_failed") }
                        require(bytes.isNotEmpty() && bytes.size <= 8 * 1024 * 1024) { "photo_size" }
                        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                        require(type.startsWith("image/")) { "photo_type" }
                        val path = ProfilePhotoStorageV2.upload(bytes, type)
                        profile = saveAvatarV2(path)
                        avatarBytes = bytes'''
    new = '''                        val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("photo_read_failed") }
                        require(bytes.isNotEmpty()) { "photo_size" }
                        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                        require(type.startsWith("image/")) { "photo_type" }
                        val compact = ProfilePhotoRuntime.compactForUpload(bytes)
                        val path = ProfilePhotoStorageV2.upload(compact, "image/webp")
                        saveAvatarV2(path)
                        profile = setAvatarHiddenV2(false)
                        avatarBytes = compact'''
    text = once(text, old, new, 'compressed avatar upload')
    text = text.replace('JPG, PNG veya WEBP • en fazla 8 MB', 'Her boyut kabul edilir • otomatik yüksek kaliteli WEBP küçültme')
    text = text.replace('JPG, PNG or WEBP • max 8 MB', 'Any image size • automatic high-quality WEBP compression')
    return text
patch('app/src/main/java/com/sonharf/game/ProfileExperienceV2.kt', profile)

# Main shell: light palette, clear admin entry, back-to-home and double-back exit.
def classic(text):
    text = once(text, 'import androidx.compose.foundation.BorderStroke\n', 'import android.app.Activity\nimport androidx.activity.compose.BackHandler\nimport androidx.compose.foundation.BorderStroke\n', 'classic back imports')
    text = once(text, 'import androidx.compose.ui.graphics.vector.ImageVector\n', 'import androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.platform.LocalContext\n', 'classic context import')
    palette = {
        'private val ClassicBg = Color(0xFF071525)': 'private val ClassicBg = Color(0xFFF7FBFF)',
        'private val ClassicBgDeep = Color(0xFF04101D)': 'private val ClassicBgDeep = Color(0xFFE8F6FF)',
        'private val ClassicPanel = Color(0xFF0D2136)': 'private val ClassicPanel = Color(0xFFFFFFFF)',
        'private val ClassicPanel2 = Color(0xFF122A43)': 'private val ClassicPanel2 = Color(0xFFEAF7FF)',
        'private val ClassicBorder = Color(0xFF29445E)': 'private val ClassicBorder = Color(0xFFB9E5F8)',
        'private val ClassicGold = Color(0xFFD8AC5C)': 'private val ClassicGold = Color(0xFF56BDE8)',
        'private val ClassicGoldSoft = Color(0xFFF0D59A)': 'private val ClassicGoldSoft = Color(0xFF299FD3)',
        'private val ClassicCream = Color(0xFFF3E8CF)': 'private val ClassicCream = Color(0xFF16324A)',
        'private val ClassicText = Color(0xFFF7F4EC)': 'private val ClassicText = Color(0xFF16324A)',
        'private val ClassicMuted = Color(0xFFB6C0CA)': 'private val ClassicMuted = Color(0xFF698296)',
        'private val ClassicBlue = Color(0xFF76A7C7)': 'private val ClassicBlue = Color(0xFF43B6E8)',
    }
    for a,b in palette.items(): text=text.replace(a,b)
    text = once(text,
        '    val lobbyRequest = SonHarfGameNavigation.lobbyRequest\n',
        '    val lobbyRequest = SonHarfGameNavigation.lobbyRequest\n    val context = LocalContext.current\n    var lastHomeBack by remember { mutableLongStateOf(0L) }\n',
        'classic back state')
    text = once(text,
        '    LaunchedEffect(screen, gameKey) {\n        if (authenticated && screen == ClassicScreen.GAME) {\n            runCatching { backend?.logEvent("son_harf_open") }\n        }\n    }\n',
        '''    LaunchedEffect(screen, gameKey) {
        if (authenticated && screen == ClassicScreen.GAME) {
            runCatching { backend?.logEvent("son_harf_open") }
        }
    }
    LaunchedEffect(SonHarfUiState.homeRequest) {
        if (SonHarfUiState.homeRequest > 0) screen = ClassicScreen.HOME
    }
    BackHandler(enabled = authenticated) {
        if (screen != ClassicScreen.HOME) {
            screen = ClassicScreen.HOME
        } else {
            val now = System.currentTimeMillis()
            if (now - lastHomeBack < 1800L) (context as? Activity)?.finish() else lastHomeBack = now
        }
    }
''', 'classic back behavior')
    text = once(text,
        '        item { ClassicHeader(profile, growth, onProfile, isAdmin, onAdmin) }\n        item { ClassicHero(onQuickGame) }',
        '''        item { ClassicHeader(profile, growth, onProfile, isAdmin, onAdmin) }
        if (isAdmin) item {
            Button(onClick = onAdmin, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ClassicBlue, contentColor = Color.White), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Rounded.AdminPanelSettings, null); Spacer(Modifier.width(8.dp)); Text("YÖNETİCİ PANELİ", fontWeight = FontWeight.Black)
            }
        }
        item { ClassicHero(onQuickGame) }''', 'visible admin entry')
    old_avatar = '''        Surface(
            onClick = onProfile,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = ClassicPanel2,
            border = BorderStroke(2.dp, ClassicGold),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text((profile?.displayName ?: "S").take(1).uppercase(), color = ClassicCream, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
        }'''
    new_avatar = '''        Box(Modifier.clickable(onClick = onProfile)) {
            ProfilePhotoAvatar(profile?.avatarPath, profile?.displayName ?: "S", 56.dp, visible = true, accent = ClassicBlue)
        }'''
    text = once(text, old_avatar, new_avatar, 'classic avatar')
    # Hero overlay no longer darkens a light theme.
    text=text.replace('Brush.horizontalGradient(listOf(Color(0xE6071525), Color(0xA2071525), Color(0x30071525)))', 'Brush.horizontalGradient(listOf(Color(0xF7FFFFFF), Color(0xD9FFFFFF), Color(0x80E8F6FF)))')
    return text
patch('app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt', classic)

# Son Harf arena: light palette, reliable Turkish switch, dynamic word type, real photos, guarded exit.
def target(text):
    text = once(text, 'import androidx.compose.foundation.BorderStroke\n', 'import androidx.activity.compose.BackHandler\nimport androidx.compose.foundation.BorderStroke\n', 'target back import')
    palette = {
        'private val TGbg = Color(0xFF090F1A)': 'private val TGbg = Color(0xFFF7FBFF)',
        'private val TGpanel = Color(0xFF10172B)': 'private val TGpanel = Color(0xFFFFFFFF)',
        'private val TGpanel2 = Color(0xFF131D35)': 'private val TGpanel2 = Color(0xFFEAF7FF)',
        'private val TGcyan = Color(0xFF00E5FF)': 'private val TGcyan = Color(0xFF46BFEF)',
        'private val TGpurple = Color(0xFF7B2FFF)': 'private val TGpurple = Color(0xFF6CC8ED)',
        'private val TGpink = Color(0xFFFF4D6D)': 'private val TGpink = Color(0xFFEA7484)',
        'private val TGgold = Color(0xFFFFC107)': 'private val TGgold = Color(0xFF52BCE8)',
        'private val TGblue = Color(0xFF168CFF)': 'private val TGblue = Color(0xFF2FA8DC)',
        'private val TGtext = Color(0xFFF5F7FF)': 'private val TGtext = Color(0xFF16324A)',
        'private val TGmuted = Color(0xFF91A1BE)': 'private val TGmuted = Color(0xFF6B8294)',
    }
    for a,b in palette.items(): text=text.replace(a,b)
    text=text.replace('Brush.verticalGradient(listOf(Color(0xFF080D19), TGbg, Color(0xFF060A13)))', 'Brush.verticalGradient(listOf(Color.White, TGbg, Color(0xFFE7F6FF)))')
    text = once(text, '                onLanguage = { language = it },', '                onLanguage = { next -> language = next; SonHarfUiState.language = next },', 'reliable language switch')
    # Pass image paths into arena.
    text = once(text,
        '                playerName = profile?.displayName ?: "Sen",\n                opponentName = if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: "Rakip",\n                isVip = profile?.isVip == true,',
        '                playerName = profile?.displayName ?: "Sen",\n                opponentName = if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: "Rakip",\n                playerAvatarPath = profile?.avatarPath,\n                opponentAvatarPath = opponentProfile?.avatarPath,\n                opponentAvatarVisible = active.isBot || opponentProfile?.avatarVisibility != "hidden",\n                isVip = profile?.isVip == true,',
        'arena avatar args')
    text = once(text,
        '    opponentName: String,\n    isVip: Boolean,',
        '    opponentName: String,\n    playerAvatarPath: String?,\n    opponentAvatarPath: String?,\n    opponentAvatarVisible: Boolean,\n    isVip: Boolean,',
        'arena avatar signature')
    text = once(text,
        '    var chatInput by remember { mutableStateOf("") }\n',
        '    var chatInput by remember { mutableStateOf("") }\n    var confirmForfeit by remember { mutableStateOf(false) }\n    DisposableEffect(room.id) { SonHarfUiState.inMatch = true; onDispose { SonHarfUiState.inMatch = false } }\n    BackHandler { confirmForfeit = true }\n',
        'forfeit state')
    text = once(text,
        '            TargetArenaPlayer(playerName, myScore, myRounds, myTurn, TGcyan, Modifier.weight(1f))',
        '            TargetArenaPlayer(playerName, playerAvatarPath, true, myScore, myRounds, myTurn, TGcyan, Modifier.weight(1f))',
        'player avatar')
    text = once(text,
        '            TargetArenaPlayer(opponentName, oppScore, oppRounds, !myTurn, TGpink, Modifier.weight(1f))',
        '            TargetArenaPlayer(opponentName, opponentAvatarPath, opponentAvatarVisible, oppScore, oppRounds, !myTurn, TGpink, Modifier.weight(1f))',
        'opponent avatar')
    text = once(text,
        '                            Text(w.word.uppercase(), color = TGtext, fontSize = 23.sp, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))',
        '                            val wordSize = when { w.word.length >= 18 -> 15.sp; w.word.length >= 14 -> 17.sp; w.word.length >= 10 -> 20.sp; else -> 23.sp }\n                            Text(w.word.uppercase(), color = TGtext, fontSize = wordSize, letterSpacing = if (w.word.length >= 14) .3.sp else 1.2.sp, maxLines = 1, modifier = Modifier.weight(1f))',
        'adaptive word size')
    text = once(text,
        '            OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, TGpink)) { Text("⚑ PES ET", color = TGpink, fontSize = 10.sp) }',
        '            OutlinedButton(onClick = { confirmForfeit = true }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, TGpink)) { Text("⚑ PES ET", color = TGpink, fontSize = 10.sp) }',
        'forfeit button')
    marker = '        if (showVipNotice) {'
    dialog = '''        if (confirmForfeit) {
            AlertDialog(
                onDismissRequest = { confirmForfeit = false },
                title = { Text(sh("PES ETMEK İSTEDİĞİNE EMİN MİSİN?", "ARE YOU SURE YOU WANT TO FORFEIT?"), fontWeight = FontWeight.Black) },
                text = { Text(sh("Maç devam ederken çıkış yapılamaz. Çıkmak için maçı pes ederek bitirmen gerekir.", "You cannot leave during a live match. Forfeit the match to exit.")) },
                confirmButton = { Button(onClick = { confirmForfeit = false; onForfeit() }, colors = ButtonDefaults.buttonColors(containerColor = TGpink)) { Text(sh("EVET, PES ET", "YES, FORFEIT")) } },
                dismissButton = { TextButton(onClick = { confirmForfeit = false }) { Text(sh("OYUNA DÖN", "RETURN TO GAME")) } },
            )
        }
'''
    if dialog not in text:
        if marker not in text: raise SystemExit('missing target: forfeit dialog marker')
        text=text.replace(marker,dialog+marker,1)
    text = once(text,
        '@Composable private fun TargetArenaPlayer(name: String, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {\n    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {\n        TargetAvatar(name, accent, 48.dp)',
        '@Composable private fun TargetArenaPlayer(name: String, avatarPath: String?, avatarVisible: Boolean, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {\n    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {\n        ProfilePhotoAvatar(avatarPath, name, 48.dp, visible = avatarVisible, accent = accent)',
        'arena player photo')
    return text
patch('app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt', target)

# Bil Bakalım: exactly 15 questions, fixed numeric keypad, match end and play-again.
def bil(text):
    text = text.replace('private enum class BilPhase { ANSWER, LOCKED, RESULT }', 'private enum class BilPhase { ANSWER, LOCKED, RESULT, MATCH_END }')
    pattern = re.compile(r'@Composable\nfun BilBakalimStandaloneScreen\(onBack: \(\) -> Unit\) \{.*?\n\}\n\n@Composable\nprivate fun ScoreBox', re.S)
    if 'BİR OYUN DAHA' in text:
        return text
    replacement = r'''@Composable
fun BilBakalimStandaloneScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var deck by remember { mutableStateOf(bilBakalimQuestions.shuffled().take(15)) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var playerScore by remember { mutableIntStateOf(0) }
    var botScore by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(20) }
    var phase by remember { mutableStateOf(BilPhase.ANSWER) }
    var playerAnswer by remember { mutableStateOf<Double?>(null) }
    var botAnswer by remember { mutableStateOf<Double?>(null) }
    var playerWon by remember { mutableStateOf<Boolean?>(null) }
    val q = deck[questionIndex]
    val questionNo = questionIndex + 1

    fun resetQuestion() {
        input = ""; seconds = 20; phase = BilPhase.ANSWER
        playerAnswer = null; botAnswer = null; playerWon = null
    }
    fun resetMatch() {
        deck = bilBakalimQuestions.shuffled().take(15)
        questionIndex = 0; playerScore = 0; botScore = 0; resetQuestion()
    }
    fun advance() {
        if (questionIndex >= 14) phase = BilPhase.MATCH_END
        else { questionIndex += 1; resetQuestion() }
    }
    fun finishRound(answer: Double?) {
        if (phase != BilPhase.ANSWER) return
        playerAnswer = answer; phase = BilPhase.LOCKED
        scope.launch {
            delay(550)
            val spread = max(1.0, abs(q.answer) * Random.nextDouble(.08, .42))
            val sign = if (Random.nextBoolean()) 1 else -1
            val generated = if (q.answer == 0.0) Random.nextDouble(0.0, 4.0) else max(0.0, q.answer + sign * spread)
            botAnswer = generated
            delay(350)
            val pDiff = playerAnswer?.let { abs(it - q.answer) } ?: Double.POSITIVE_INFINITY
            val bDiff = abs(generated - q.answer)
            playerWon = pDiff <= bDiff
            if (playerWon == true) playerScore += 10 else botScore += 10
            phase = BilPhase.RESULT
        }
    }
    LaunchedEffect(questionIndex, phase) {
        if (phase != BilPhase.ANSWER) return@LaunchedEffect
        seconds = 20
        while (seconds > 0 && phase == BilPhase.ANSWER) { delay(1000); seconds -= 1 }
        if (seconds <= 0 && phase == BilPhase.ANSWER) finishRound(null)
    }

    BackHandler { onBack() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF5FBFF), Color(0xFFE8F6FF))))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = Color(0xFF18344A)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BİL BAKALIM", color = Color(0xFF2CA9DC), fontWeight = FontWeight.Black, fontSize = 25.sp)
                    Text("Doğru cevaba en yakın cevap kazanır.", color = Color(0xFF6C8293), fontSize = 11.sp)
                }
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreBox("SEN", playerScore, Color(0xFF2CA9DC), Modifier.weight(1f))
                ScoreBox("BOT", botScore, Color(0xFFEA7484), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))

            if (phase == BilPhase.MATCH_END) {
                Spacer(Modifier.weight(1f))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, Color(0xFF69C9EF)), shape = RoundedCornerShape(26.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFF45B8E5), modifier = Modifier.size(52.dp))
                        Text(if (playerScore >= botScore) "KAZANDIN!" else "MAÇ BİTTİ", color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = 32.sp)
                        Text("15 SORU TAMAMLANDI", color = Color(0xFF6C8293), fontWeight = FontWeight.Bold)
                        Text("$playerScore  -  $botScore", color = Color(0xFF2CA9DC), fontSize = 42.sp, fontWeight = FontWeight.Black)
                        Button(onClick = ::resetMatch, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8))) { Text("BİR OYUN DAHA", fontWeight = FontWeight.Black) }
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("ANA MENÜ") }
                    }
                }
                Spacer(Modifier.weight(1f))
                return@Column
            }

            Surface(shape = RoundedCornerShape(100.dp), color = if (seconds <= 5) Color(0xFFEA7484) else Color(0xFF65C7EE)) {
                Text("$seconds", Modifier.padding(horizontal = 20.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 23.sp)
            }
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFB9E5F8))) {
                Column(Modifier.fillMaxWidth().padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${q.category.uppercase()} • SORU $questionNo/15", color = Color(0xFF2CA9DC), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(q.question, color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    if (phase == BilPhase.ANSWER) {
                        Surface(Modifier.fillMaxWidth(), color = Color(0xFFF0F9FE), shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, Color(0xFF69C9EF))) {
                            Text(input.ifBlank { "Tahminin" }, Modifier.fillMaxWidth().padding(13.dp), textAlign = TextAlign.Center, color = if (input.isBlank()) Color(0xFF8EA2B1) else Color(0xFF17344A), fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(10.dp))
                        NumericEstimatePad(input, { input = it })
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { finishRound(input.replace(',', '.').toDoubleOrNull()) }, enabled = input.replace(',', '.').toDoubleOrNull() != null, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8)), shape = RoundedCornerShape(15.dp)) { Text("CEVABI KİLİTLE", fontWeight = FontWeight.Black) }
                    } else {
                        AnswerLine("Senin cevabın", playerAnswer?.let(::prettyNumber) ?: "Cevap yok", phase == BilPhase.RESULT && playerWon == true)
                        Spacer(Modifier.height(8.dp))
                        AnswerLine("Bot cevabı", botAnswer?.let(::prettyNumber) ?: "Cevap bekleniyor…", phase == BilPhase.RESULT && playerWon == false)
                    }
                }
            }
            if (phase == BilPhase.LOCKED) { Spacer(Modifier.height(12.dp)); CircularProgressIndicator(color = Color(0xFF42B7E5)) }
            if (phase == BilPhase.RESULT) {
                Spacer(Modifier.height(9.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1FFF7)), border = BorderStroke(1.dp, Color(0xFF39D875)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DOĞRU CEVAP", color = Color(0xFF6C8293), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(q.displayAnswer, color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = 30.sp, textAlign = TextAlign.Center)
                        Text(if (playerWon == true) "KAZANDIN! • +10 PUAN" else "YANLIŞ CEVAP", color = if (playerWon == true) Color(0xFF18B864) else Color(0xFFDD5968), fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = ::advance, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8))) { Text(if (questionNo == 15) "MAÇI BİTİR" else "SONRAKİ SORU", fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericEstimatePad(value: String, onValue: (String) -> Unit) {
    val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf(",","0","⌫"))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            val next = when (key) {
                                "⌫" -> value.dropLast(1)
                                "," -> if (value.contains(',') || value.contains('.')) value else if (value.isBlank()) "0," else value + ","
                                else -> if (value.length >= 15) value else value + key
                            }
                            onValue(next)
                        },
                        modifier = Modifier.weight(1f).height(43.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7F6FD), contentColor = Color(0xFF17344A)),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(key, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                }
            }
        }
    }
}

@Composable
private fun ScoreBox'''
    new, n = pattern.subn(replacement, text, count=1)
    if n != 1: raise SystemExit('missing target: BilBakalimStandaloneScreen block')
    # Lighten shared score and answer cards.
    new=new.replace('color = Color(0xFF10263A)', 'color = Color.White')
    new=new.replace('color = Color(0xFFB6C0CA)', 'color = Color(0xFF6C8293)')
    new=new.replace('color = Color(0xFF091723)', 'color = Color(0xFFF0F9FE)')
    new=new.replace('else Color.White, fontWeight', 'else Color(0xFF17344A), fontWeight')
    return new
patch('app/src/main/java/com/sonharf/game/BilBakalimFeature.kt', bil)

# Match summary: word meanings on tap + rematch and back buttons.
def combo(text):
    text = once(text, 'import androidx.compose.foundation.background\n', 'import androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable\n', 'combo clickable import')
    text = once(text,
        '    @SerialName("winner_id") val winnerId: String? = null,\n)',
        '    @SerialName("winner_id") val winnerId: String? = null,\n    val language: String = "tr",\n)',
        'summary language')
    text = once(text,
        '    var reactionKey by remember { mutableStateOf<Long?>(null) }\n',
        '    var reactionKey by remember { mutableStateOf<Long?>(null) }\n    var selectedWord by remember { mutableStateOf<String?>(null) }\n    var selectedMeaning by remember { mutableStateOf<String?>(null) }\n',
        'meaning state')
    target = '''                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick={SonHarfShare.result(context,growth?.displayName?:sh("Oyuncu","Player"),myScore,oppScore,myWords,growth?.currentWinStreak?:0);scope.launch{backend.logEvent("match_result_share",fin.id)}},modifier=Modifier.weight(1f)){Text("↗ ${sh("SONUCU PAYLAŞ","SHARE RESULT")}",fontSize=9.sp)}
                        Button(onClick={SonHarfShare.challenge(context,growth?.displayName?:sh("Oyuncu","Player"),if(fin.isBot)null else fin.code);scope.launch{backend.logEvent("challenge_share",fin.id)}},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=SonHarfGold,contentColor=Color(0xFF261700))){Text("⚔ ${sh("MEYDAN OKU","CHALLENGE")}",fontWeight=FontWeight.Black,fontSize=9.sp)}
                    }'''
    replacement = '''                    Text(sh("KELİMELER • ANLAM İÇİN DOKUN", "WORDS • TAP FOR MEANING"), color=SonHarfMuted, fontSize=9.sp, fontWeight=FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max=130.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        items(resultWords) { w ->
                            Surface(Modifier.fillMaxWidth().clickable { selectedWord=w.word; selectedMeaning=null; scope.launch { selectedMeaning=WordMeaningRuntime.meaning(w.word, fin.language) } }, shape=RoundedCornerShape(10.dp), color=SonHarfSurface2) {
                                Text(w.word.uppercase(), Modifier.padding(horizontal=10.dp,vertical=7.dp), fontWeight=FontWeight.Bold, fontSize=12.sp)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick={dismissSummary(fin.id); SonHarfUiState.homeRequest += 1},modifier=Modifier.weight(1f)){Text("← ${sh("GERİ","BACK")}",fontSize=10.sp,fontWeight=FontWeight.Bold)}
                        Button(onClick={scope.launch { runCatching { if(fin.isBot) backend.restartBotMatch(fin.id) else backend.requestRematch(fin.id) }; dismissSummary(fin.id) }},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=SonHarfCyan)){Text(sh("AYNI OYUNCUYLA TEKRAR","REMATCH"),fontWeight=FontWeight.Black,fontSize=9.sp)}
                    }'''
    text = once(text, target, replacement, 'summary controls')
    marker='}\n\n@Composable private fun SummaryMetric'
    dialog='''    selectedWord?.let { word ->
        AlertDialog(
            onDismissRequest={selectedWord=null;selectedMeaning=null},
            title={Text(word.uppercase(),fontWeight=FontWeight.Black)},
            text={if(selectedMeaning==null) CircularProgressIndicator() else Text(selectedMeaning!!)},
            confirmButton={TextButton(onClick={selectedWord=null;selectedMeaning=null}){Text(sh("KAPAT","CLOSE"))}}
        )
    }
}

@Composable private fun SummaryMetric'''
    if 'selectedWord?.let { word ->' not in text:
        if marker not in text: raise SystemExit('missing target: summary dialog marker')
        text=text.replace(marker,dialog,1)
    return text
patch('app/src/main/java/com/sonharf/game/ComboOverlayV9.kt', combo)
