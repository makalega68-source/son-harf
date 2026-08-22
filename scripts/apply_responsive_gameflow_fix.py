from pathlib import Path


def replace(path: str, old: str, new: str, count: int = 1):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:160]!r}")
    text = text.replace(old, new, count)
    p.write_text(text)
    print('patched', path)

# -----------------------------------------------------------------------------
# Classic home: prevent text clipping and make season/league truly equal squares.
# -----------------------------------------------------------------------------
p = Path('app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt')
t = p.read_text()
t = t.replace(
'''            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClassicSeasonCard(Modifier.weight(1f), onHub)
                ClassicLeagueCard(growth, Modifier.weight(1f), onLeague)
            }
''',
'''            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClassicSeasonCard(Modifier.weight(1f).aspectRatio(1f), onHub)
                ClassicLeagueCard(growth, Modifier.weight(1f).aspectRatio(1f), onLeague)
            }
''')
t = t.replace('modifier = modifier.height(88.dp).clickable(onClick = onClick),', 'modifier = modifier.height(96.dp).clickable(onClick = onClick),')
t = t.replace('Icon(icon, null, tint = ClassicGoldSoft, modifier = Modifier.size(26.dp))', 'Icon(icon, null, tint = ClassicGoldSoft, modifier = Modifier.size(24.dp))')
t = t.replace('Text(label, color = ClassicText, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)', 'Text(label, color = ClassicText, fontSize = 7.5.sp, lineHeight = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)')
t = t.replace(
'''    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(sh("SEZON 12", "SEASON 12"), color = ClassicGoldSoft, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sh("ARENA ŞAMPİYONASI", "ARENA CHAMPIONSHIP"), color = ClassicText, fontSize = 10.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfessionalLogo(54.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(sh("24 gün 18 saat kaldı", "24 days 18 hours left"), color = ClassicMuted, fontSize = 9.sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { .72f }, modifier = Modifier.width(100.dp).height(5.dp).clip(CircleShape), color = ClassicGold, trackColor = Color.White.copy(alpha = .08f))
                    Text("7.250 / 10.000", color = ClassicMuted, fontSize = 8.sp)
                }
            }
        }
    }
''',
'''    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(sh("SEZON 12", "SEASON 12"), color = ClassicGoldSoft, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(sh("ARENA ŞAMPİYONASI", "ARENA CHAMPIONSHIP"), color = ClassicText, fontSize = 8.5.sp, lineHeight = 11.sp, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfessionalLogo(45.dp)
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("24 gün 18 saat kaldı", "24 days 18 hours left"), color = ClassicMuted, fontSize = 7.5.sp, lineHeight = 10.sp, maxLines = 2)
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = ClassicGold, trackColor = Color.White.copy(alpha = .08f))
                    Text("7.250 / 10.000", color = ClassicMuted, fontSize = 7.5.sp, maxLines = 1)
                }
            }
        }
    }
''')
t = t.replace(
'''    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(sh("LİGİN", "YOUR LEAGUE"), color = ClassicGoldSoft, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = ClassicGold, modifier = Modifier.size(45.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(growth?.leagueName ?: "ALTIN I", color = ClassicText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("2.150 / 3.000", color = ClassicMuted, fontSize = 9.sp)
                }
            }
            LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = ClassicGold, trackColor = Color.White.copy(alpha = .08f))
            Text(sh("Sıralamanı yükselt", "Climb the ranking"), color = ClassicMuted, fontSize = 8.sp)
        }
    }
''',
'''    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(sh("LİGİN", "YOUR LEAGUE"), color = ClassicGoldSoft, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = ClassicGold, modifier = Modifier.size(39.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(growth?.leagueName ?: "ALTIN I", color = ClassicText, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    Text("2.150 / 3.000", color = ClassicMuted, fontSize = 8.sp, maxLines = 1)
                }
            }
            LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = ClassicGold, trackColor = Color.White.copy(alpha = .08f))
            Text(sh("Sıralamanı yükselt", "Climb the ranking"), color = ClassicMuted, fontSize = 7.5.sp, maxLines = 1)
        }
    }
''')
p.write_text(t)
print('patched ClassicPremiumApp.kt')

# -----------------------------------------------------------------------------
# Bil Bakalim: one-screen adaptive layout; no vertical scrolling during play.
# -----------------------------------------------------------------------------
p = Path('app/src/main/java/com/sonharf/game/BilBakalimFeature.kt')
t = p.read_text()
start = t.index('@Composable\nfun BilBakalimStandaloneScreen(onBack: () -> Unit) {')
end = t.index('\n@Composable\nprivate fun NumericEstimatePad', start)
new_fun = r'''@Composable
fun BilBakalimStandaloneScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        val id = b.currentUserId() ?: return@LaunchedEffect
        playerProfile = runCatching { b.getProfile(id) }.getOrNull()
    }
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
        if (questionIndex >= 14) phase = BilPhase.MATCH_END else { questionIndex += 1; resetQuestion() }
    }
    fun finishRound(answer: Double?) {
        if (phase != BilPhase.ANSWER) return
        playerAnswer = answer; phase = BilPhase.LOCKED
        scope.launch {
            delay(450)
            val spread = max(1.0, abs(q.answer) * Random.nextDouble(.08, .42))
            val sign = if (Random.nextBoolean()) 1 else -1
            val generated = if (q.answer == 0.0) Random.nextDouble(0.0, 4.0) else max(0.0, q.answer + sign * spread)
            botAnswer = generated
            delay(250)
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
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF5FBFF), Color(0xFFE8F6FF))))) {
        val compact = maxHeight < 720.dp
        val tiny = maxHeight < 620.dp
        val side = if (tiny) 10.dp else 14.dp
        val gap = if (tiny) 4.dp else 6.dp
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = side, vertical = if (tiny) 5.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = if (tiny) 42.dp else 48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = Color(0xFF18344A)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BİL BAKALIM", color = Color(0xFF2CA9DC), fontWeight = FontWeight.Black, fontSize = if (tiny) 20.sp else 23.sp, maxLines = 1)
                    if (!tiny) Text("Doğru cevaba en yakın cevap kazanır.", color = Color(0xFF6C8293), fontSize = 10.sp, maxLines = 1)
                }
                Spacer(Modifier.width(42.dp))
            }

            Row(Modifier.fillMaxWidth().height(if (tiny) 68.dp else 78.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ScoreBox("SEN", playerScore, Color(0xFF2CA9DC), Modifier.weight(1f))
                ScoreBox("BOT", botScore, Color(0xFFEA7484), Modifier.weight(1f))
            }

            if (phase == BilPhase.MATCH_END) {
                val playerIsWinner = playerScore >= botScore
                val winnerName = if (playerIsWinner) playerProfile?.displayName ?: "Sen" else "KelimeBot BOT"
                Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, Color(0xFF69C9EF)), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxSize().padding(if (tiny) 14.dp else 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFF45B8E5), modifier = Modifier.size(if (tiny) 36.dp else 44.dp))
                        Text(if (playerIsWinner) "KAZANDIN!" else "MAÇ BİTTİ", color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = if (tiny) 25.sp else 30.sp)
                        if (playerIsWinner) ProfilePhotoAvatar(playerProfile?.avatarPath, winnerName, if (tiny) 62.dp else 76.dp, visible = true, accent = Color(0xFF2CA9DC))
                        else Surface(modifier = Modifier.size(if (tiny) 62.dp else 76.dp), shape = CircleShape, color = Color(0xFFFFEEF2), border = BorderStroke(2.dp, Color(0xFFEA7484))) { Box(contentAlignment = Alignment.Center) { Text("🤖", fontSize = if (tiny) 34.sp else 40.sp) } }
                        Text(winnerName, color = if (playerIsWinner) Color(0xFF2CA9DC) else Color(0xFFEA7484), fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("$playerScore  -  $botScore", color = Color(0xFF2CA9DC), fontSize = if (tiny) 34.sp else 40.sp, fontWeight = FontWeight.Black)
                        Button(onClick = ::resetMatch, modifier = Modifier.fillMaxWidth().height(if (tiny) 46.dp else 52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8)), shape = RoundedCornerShape(16.dp)) { Text("BİR OYUN DAHA", fontWeight = FontWeight.Black) }
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(if (tiny) 44.dp else 48.dp), shape = RoundedCornerShape(16.dp)) { Text("ANA MENÜ") }
                    }
                }
                return@Column
            }

            Surface(shape = RoundedCornerShape(100.dp), color = if (seconds <= 5) Color(0xFFEA7484) else Color(0xFF65C7EE)) {
                Text("$seconds", Modifier.padding(horizontal = 18.dp, vertical = if (tiny) 4.dp else 6.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = if (tiny) 19.sp else 22.sp)
            }

            Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFB9E5F8))) {
                Column(Modifier.fillMaxSize().padding(if (tiny) 11.dp else 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                    Text("${q.category.uppercase()} • SORU $questionNo/15", color = Color(0xFF2CA9DC), fontWeight = FontWeight.Bold, fontSize = if (tiny) 9.sp else 10.sp, maxLines = 1)
                    Text(q.question, color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = if (tiny) 17.sp else if (compact) 19.sp else 21.sp, lineHeight = if (tiny) 21.sp else 25.sp, textAlign = TextAlign.Center, maxLines = 3)
                    if (phase == BilPhase.ANSWER) {
                        Surface(Modifier.fillMaxWidth(), color = Color(0xFFF0F9FE), shape = RoundedCornerShape(14.dp), border = BorderStroke(2.dp, Color(0xFF69C9EF))) {
                            Text(input.ifBlank { "Tahminin" }, Modifier.fillMaxWidth().padding(vertical = if (tiny) 7.dp else 9.dp, horizontal = 10.dp), textAlign = TextAlign.Center, color = if (input.isBlank()) Color(0xFF8EA2B1) else Color(0xFF17344A), fontSize = if (tiny) 23.sp else 27.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                        NumericEstimatePad(input, { input = it }, compact = compact || tiny)
                        Button(onClick = { finishRound(input.replace(',', '.').toDoubleOrNull()) }, enabled = input.replace(',', '.').toDoubleOrNull() != null, modifier = Modifier.fillMaxWidth().height(if (tiny) 42.dp else 47.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8)), shape = RoundedCornerShape(14.dp)) { Text("CEVABI KİLİTLE", fontWeight = FontWeight.Black, fontSize = if (tiny) 12.sp else 14.sp) }
                    } else {
                        AnswerLine("Senin cevabın", playerAnswer?.let(::prettyNumber) ?: "Cevap yok", phase == BilPhase.RESULT && playerWon == true)
                        AnswerLine("Bot cevabı", botAnswer?.let(::prettyNumber) ?: "Cevap bekleniyor…", phase == BilPhase.RESULT && playerWon == false)
                        if (phase == BilPhase.LOCKED) CircularProgressIndicator(color = Color(0xFF42B7E5), modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
            }

            if (phase == BilPhase.RESULT) {
                Card(modifier = Modifier.fillMaxWidth().height(if (tiny) 130.dp else 150.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1FFF7)), border = BorderStroke(1.dp, Color(0xFF39D875)), shape = RoundedCornerShape(19.dp)) {
                    Column(Modifier.fillMaxSize().padding(if (tiny) 9.dp else 11.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                        Text("DOĞRU CEVAP", color = Color(0xFF6C8293), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(q.displayAnswer, color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = if (tiny) 22.sp else 27.sp, textAlign = TextAlign.Center, maxLines = 2)
                        Text(if (playerWon == true) "KAZANDIN! • +10 PUAN" else "YANLIŞ CEVAP", color = if (playerWon == true) Color(0xFF18B864) else Color(0xFFDD5968), fontWeight = FontWeight.Black, fontSize = if (tiny) 14.sp else 17.sp, maxLines = 1)
                        Button(onClick = ::advance, modifier = Modifier.fillMaxWidth().height(if (tiny) 38.dp else 43.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8)), shape = RoundedCornerShape(14.dp)) { Text(if (questionNo == 15) "MAÇI BİTİR" else "SONRAKİ SORU", fontWeight = FontWeight.Black, fontSize = if (tiny) 12.sp else 14.sp) }
                    }
                }
            }
        }
    }
}
'''
t = t[:start] + new_fun + t[end:]
t = t.replace('private fun NumericEstimatePad(value: String, onValue: (String) -> Unit) {', 'private fun NumericEstimatePad(value: String, onValue: (String) -> Unit, compact: Boolean = false) {')
t = t.replace('modifier = Modifier.weight(1f).height(43.dp),', 'modifier = Modifier.weight(1f).height(if (compact) 33.dp else 40.dp),')
t = t.replace(') { Text(key, fontWeight = FontWeight.Black, fontSize = 20.sp) }', ') { Text(key, fontWeight = FontWeight.Black, fontSize = if (compact) 17.sp else 20.sp) }')
p.write_text(t)
print('patched BilBakalimFeature.kt')

# -----------------------------------------------------------------------------
# Lobby: compact private-room mode + IME-aware layout so join/create controls never hide.
# -----------------------------------------------------------------------------
p = Path('app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt')
t = p.read_text()
if 'import androidx.compose.ui.platform.LocalDensity\n' not in t:
    t = t.replace('import androidx.compose.ui.platform.LocalFocusManager\n', 'import androidx.compose.ui.platform.LocalDensity\nimport androidx.compose.ui.platform.LocalFocusManager\n')
start = t.index('@Composable\nprivate fun TargetLobby(')
end = t.index('\n@Composable\nprivate fun TargetMatchCard', start)
new_lobby = r'''@Composable
private fun TargetLobby(
    playerName: String,
    language: String,
    matching: Boolean,
    notice: String,
    privateCode: String,
    showPrivate: Boolean,
    onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit,
    onPrivateToggle: () -> Unit,
    onRandom: () -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val privateCompact = showPrivate || imeVisible
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(horizontal = 18.dp, vertical = if (privateCompact) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (privateCompact) 8.dp else 12.dp),
    ) {
        Text(if (matching) "RAKİP BULUNUYOR" else "DÜELLO", color = TGtext, fontWeight = FontWeight.Black, fontSize = 18.sp)
        if (matching) {
            Text("RAKİP\nBULUNUYOR!", color = TGcyan, fontWeight = FontWeight.Black, fontSize = 36.sp, textAlign = TextAlign.Center, lineHeight = 38.sp)
            TargetMatchCard(playerName, "Usta", "1250", TGcyan)
            Text("VS", color = TGpurple, fontWeight = FontWeight.Black, fontSize = 42.sp)
            TargetMatchCard("RAKİP ARANIYOR", "…", "", TGpink)
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(color = TGcyan, strokeWidth = 3.dp)
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(50.dp), border = BorderStroke(1.dp, TGpink), shape = RoundedCornerShape(16.dp)) { Text("İPTAL", color = TGpink, fontWeight = FontWeight.Black) }
        } else {
            if (!imeVisible) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, TGpurple.copy(alpha = .55f))) {
                    Box(Modifier.fillMaxWidth().height(if (privateCompact) 138.dp else 205.dp).background(Brush.radialGradient(listOf(TGpurple.copy(alpha = .28f), TGpanel))), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SON HARF", color = TGcyan, fontSize = if (privateCompact) 34.sp else 43.sp, fontWeight = FontWeight.Black)
                            Text("NEON KELİME DÜELLOSU", color = TGtext, fontSize = 9.sp, letterSpacing = 1.1.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = language == "tr", onClick = { onLanguage("tr") }, label = { Text("🇹🇷 TÜRKÇE", maxLines = 1) }, modifier = Modifier.weight(1f))
                    FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("🇬🇧 ENGLISH", maxLines = 1) }, modifier = Modifier.weight(1f))
                }
                if (!showPrivate) {
                    Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = TGgold, contentColor = Color(0xFF211500)), shape = RoundedCornerShape(17.dp)) { Text("HEMEN OYNA", fontSize = 17.sp, fontWeight = FontWeight.Black) }
                }
                Button(onClick = onPrivateToggle, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = TGblue), shape = RoundedCornerShape(17.dp)) { Text(if (showPrivate) "ÖZEL ODAYI KAPAT" else "ODA KUR / ODAYA KATIL", fontWeight = FontWeight.Black, maxLines = 1) }
            }
            if (showPrivate) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), colors = CardDefaults.cardColors(containerColor = TGpanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, TGpurple.copy(alpha = .45f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ÖZEL ODA", color = TGtext, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = TGpurple)) { Text("VIP ODA OLUŞTUR", fontWeight = FontWeight.Black, maxLines = 1) }
                        OutlinedTextField(
                            privateCode,
                            onPrivateCode,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Oda kodu") },
                            placeholder = { Text("6 haneli kod") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        )
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth().height(46.dp)) { Text("KATIL / ONAYLA", fontWeight = FontWeight.Black) }
                    }
                }
            }
            if (!imeVisible) Text(notice, color = TGmuted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
'''
t = t[:start] + new_lobby + t[end:]
p.write_text(t)
print('patched TargetNeonGameScreen.kt')

# -----------------------------------------------------------------------------
# False move-error reconciliation: never show a generic HATA if server accepted move.
# -----------------------------------------------------------------------------
p = Path('app/src/main/java/com/sonharf/game/SketchGameOverlayV10.kt')
t = p.read_text()
old = '''                        .onFailure { e ->
                            input = ""
                            val f = failureFeedback(e.message.orEmpty(), submitted)
                            feedback = if (f.duplicateWord != null && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f
                            SonHarfSoundFx.warning()
                        }
'''
new = '''                        .onFailure { e ->
                            input = ""
                            val reconciledRoom = runCatching { backend.getRoom(active.id) }.getOrNull()
                            val reconciledWords = runCatching { backend.getWords(active.id) }.getOrDefault(words)
                            val acceptedOnServer = reconciledWords.any {
                                it.playerId == me && (it.word.equals(submitted, ignoreCase = true) || it.normalizedWord.equals(submitted, ignoreCase = true))
                            } && ((reconciledRoom?.validWordCount ?: active.validWordCount) >= active.validWordCount)
                            val stateAdvanced = reconciledRoom != null && (
                                reconciledRoom.validWordCount > active.validWordCount || reconciledRoom.currentPlayerId != active.currentPlayerId
                            )
                            if (acceptedOnServer || stateAdvanced) {
                                if (reconciledRoom != null) room = reconciledRoom
                                words = reconciledWords
                                feedback = if (acceptedOnServer) messageForEvent(null, submitted) else null
                                if (acceptedOnServer) SonHarfSoundFx.wordAccepted()
                            } else {
                                val f = failureFeedback(e.message.orEmpty(), submitted)
                                feedback = if (f.duplicateWord != null && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f
                                SonHarfSoundFx.warning()
                            }
                        }
'''
if old not in t:
    raise SystemExit('SketchGameOverlayV10 failure anchor missing')
t = t.replace(old, new)
p.write_text(t)
print('patched SketchGameOverlayV10.kt')

# -----------------------------------------------------------------------------
# Victory summary: persist until explicit user action; rename rematch; stronger text.
# -----------------------------------------------------------------------------
p = Path('app/src/main/java/com/sonharf/game/ComboOverlayV9.kt')
t = p.read_text()
t = t.replace('''                } else if (fin == null) {
                    finishedRoom = null
                }
''', '''                }
''')
t = t.replace('Text(sh("AYNI OYUNCUYLA TEKRAR","REMATCH"),fontWeight=FontWeight.Black,fontSize=9.sp)', 'Text(sh("RÖVANŞ","REMATCH"),fontWeight=FontWeight.Black,fontSize=13.sp,color=Color.White)')
t = t.replace('Text("← ${sh("GERİ","BACK")}",fontSize=10.sp,fontWeight=FontWeight.Bold)', 'Text("← ${sh("GERİ","BACK")}",fontSize=12.sp,fontWeight=FontWeight.Black)')
t = t.replace('title={Text(word.uppercase(),fontWeight=FontWeight.Black)},', 'title={Text("${word.uppercase()} • ${sh("ANLAMI","MEANING")}",fontWeight=FontWeight.Black,color=SonHarfCyan)},')
p.write_text(t)
print('patched ComboOverlayV9.kt')

# -----------------------------------------------------------------------------
# Dictionary: use a real English dictionary API first, then Wiktionary; localized fallback.
# -----------------------------------------------------------------------------
Path('app/src/main/java/com/sonharf/game/WordMeaningRuntime.kt').write_text(r'''package com.sonharf.game

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

internal object WordMeaningRuntime {
    private val http = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, String>()

    suspend fun meaning(word: String, language: String): String {
        val normalized = word.trim().lowercase()
        val key = "$language:$normalized"
        cache[key]?.let { return it }
        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name()).replace("+", "%20")

        val dictionaryApi = if (language == "en") runCatching {
            val body = http.get("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject
            val meanings = root?.get("meanings")?.jsonArray.orEmpty()
            meanings.asSequence().mapNotNull { meaning ->
                meaning.jsonObject["definitions"]?.jsonArray?.firstOrNull()?.jsonObject?.get("definition")?.jsonPrimitive?.content?.trim()
            }.firstOrNull { !it.isNullOrBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val wiktionary = if (dictionaryApi.isBlank()) runCatching {
            val host = if (language == "en") "en.wiktionary.org" else "tr.wiktionary.org"
            val url = "https://$host/w/api.php?action=query&format=json&prop=extracts&exintro=1&explaintext=1&redirects=1&titles=$encoded"
            val root = json.parseToJsonElement(http.get(url).bodyAsText()).jsonObject
            val pages = root["query"]?.jsonObject?.get("pages")?.jsonObject
            pages?.values?.asSequence()?.mapNotNull { it.jsonObject["extract"]?.jsonPrimitive?.content?.trim() }?.firstOrNull { it.isNotBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val raw = dictionaryApi.ifBlank { wiktionary }
        val concise = raw.replace(Regex("\\s+"), " ").trim().let { if (it.length > 420) it.take(417).trimEnd() + "…" else it }
        val value = concise.ifBlank {
            if (language == "en") "Bu İngilizce kelimenin kısa sözlük anlamı şu anda alınamadı. Daha sonra tekrar deneyebilirsin."
            else "Bu kelimenin kısa sözlük anlamı şu anda alınamadı. Daha sonra tekrar deneyebilirsin."
        }
        cache[key] = value
        return value
    }
}
''')
print('patched WordMeaningRuntime.kt')
