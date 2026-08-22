from pathlib import Path

arena = Path('app/src/main/java/com/sonharf/game/SketchGameOverlayV10.kt')
text = arena.read_text()


def replace_once(old: str, new: str, label: str):
    global text
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'missing anchor: {label}')
    text = text.replace(old, new, 1)

# Parent callback: working quick social reaction.
replace_once(
'''            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onChat = {''',
'''            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onGoodWord = {
                SonHarfSoundFx.softNotify()
                notice = sh("👏 İyi kelime!", "👏 Nice word!")
                if (!active.isBot) scope.launch { runCatching { backend.sendChat(active.id, "👏 İyi kelime!") } }
            },
            onChat = {''',
'good-word parent callback')

replace_once(
'''    onForfeit: () -> Unit,
    onChat: () -> Unit,''',
'''    onForfeit: () -> Unit,
    onGoodWord: () -> Unit,
    onChat: () -> Unit,''',
'good-word arena signature')

# Derived live competition state from actual match values.
replace_once(
'''    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val displayLocale = if (room.language == "tr") java.util.Locale("tr", "TR") else java.util.Locale.ENGLISH''',
'''    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val currentRoundWords = words.takeLast(room.roundWordCount.coerceAtLeast(0))
    val myStreak = currentRoundWords.count { it.playerId == me }.coerceAtMost(9)
    val opponentId = if (me == room.hostId) room.guestId else room.hostId
    val oppStreak = currentRoundWords.count { it.playerId == opponentId }.coerceAtMost(9)
    val scoreDiff = myScore - oppScore
    val pressureLabel = when { scoreDiff <= -6 -> sh("Yüksek", "High"); scoreDiff < 3 -> sh("Orta", "Medium"); else -> sh("Düşük", "Low") }
    val ratingGain = (10 + (oppScore - myScore).coerceIn(-2, 6)).coerceIn(8, 16)
    val streakMultiplier = if (myStreak >= 5) "x1.5" else if (myStreak >= 3) "x1.2" else "x1.0"
    val displayLocale = if (room.language == "tr") java.util.Locale("tr", "TR") else java.util.Locale.ENGLISH''',
'competition derived state')

# Taller player header and stronger timer.
replace_once(
'''        Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {''',
'''        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {''',
'header height')
replace_once('''            Box(Modifier.size(64.dp).scale(timerScale).clip(CircleShape)''','''            Box(Modifier.size(70.dp).scale(timerScale).clip(CircleShape)''','timer size')

# Round header with live lead status, plus streak/rating band.
replace_once(
'''                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("${room.roundWordCount}/10", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }''',
'''                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            when { scoreDiff > 0 -> sh("ÖNDESİN +$scoreDiff", "YOU LEAD +$scoreDiff"); scoreDiff < 0 -> sh("RAKİP ÖNDE ${-scoreDiff}", "OPPONENT +${-scoreDiff}"); else -> sh("BAŞA BAŞ", "TIED") },
                            modifier = Modifier.weight(1f), color = if (scoreDiff >= 0) SonHarfCyan else SonHarfPink, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1,
                        )
                        Text("${room.roundWordCount}/10", modifier = Modifier.weight(1f), color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.End)
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥 ${sh("SERİ", "STREAK")}: $myStreak", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${sh("RAKİP SERİSİ", "OPP STREAK")}: $oppStreak", color = SonHarfPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(progress = { (myStreak / 5f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = SonHarfGold, trackColor = SonHarfSurface2)
                        Text("📊 Rating +$ratingGain ${sh("kazanırsan", "if you win")}", color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }''',
'round competition band')

# Keyboard delete label must be only SIL/DELETE.
replace_once('''        ) { Text("⌫", fontSize = 21.sp, fontWeight = FontWeight.Black) }''','''        ) { Text(sh("SİL", "DELETE"), fontSize = 14.sp, fontWeight = FontWeight.Black) }''','delete label')

# Bottom controls: Forfeit + working Good Word + Chat.
replace_once(
'''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onForfeit, modifier=Modifier.weight(1f).height(44.dp), border=BorderStroke(1.dp, SonHarfPink.copy(alpha=.55f))) { Text(sh("⚑ PES ET", "⚑ FORFEIT"), color=SonHarfPink, fontWeight=FontWeight.Bold, fontSize=14.sp) }
            OutlinedButton(onClick=onChat, modifier=Modifier.weight(1f).height(44.dp), border=BorderStroke(1.dp, SonHarfCyan.copy(alpha=.55f))) { Text(if (isVip) sh("● SOHBET", "● CHAT") else sh("🔒 SOHBET • VIP", "🔒 CHAT • VIP"), color=if (isVip) SonHarfCyan else SonHarfGold, fontWeight=FontWeight.Bold, fontSize=14.sp) }
        }''',
'''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick=onForfeit, modifier=Modifier.weight(1f).height(42.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfPink.copy(alpha=.55f))) { Text(sh("⚑ PES", "⚑ FORFEIT"), color=SonHarfPink, fontWeight=FontWeight.Bold, fontSize=12.sp, maxLines=1) }
            OutlinedButton(onClick=onGoodWord, modifier=Modifier.weight(1.18f).height(42.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfCyan.copy(alpha=.55f))) { Text(sh("👏 İYİ KELİME", "👏 NICE WORD"), color=SonHarfText, fontWeight=FontWeight.Bold, fontSize=11.sp, maxLines=1) }
            OutlinedButton(onClick=onChat, modifier=Modifier.weight(1.18f).height(42.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfCyan.copy(alpha=.55f))) { Text(if (isVip) sh("● SOHBET", "● CHAT") else sh("🔒 CHAT • VIP", "🔒 CHAT • VIP"), color=if (isVip) SonHarfCyan else SonHarfGold, fontWeight=FontWeight.Bold, fontSize=11.sp, maxLines=1) }
        }
        Surface(Modifier.fillMaxWidth().height(48.dp), shape=RoundedCornerShape(14.dp), color=SonHarfSurface.copy(alpha=.9f), border=BorderStroke(1.dp, SonHarfMuted.copy(alpha=.14f))) {
            Row(Modifier.fillMaxSize().padding(horizontal=8.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                StatusChipV10("❤", sh("KRİTİK MOD", "CRITICAL"), if (seconds <= 10) sh("AKTİF", "ACTIVE") else "${seconds} sn", if (seconds <= 10) SonHarfPink else SonHarfMuted)
                StatusChipV10("⚡", sh("SERİ BONUSU", "STREAK BONUS"), streakMultiplier, SonHarfGold)
                StatusChipV10("🎯", sh("RAKİP BASKISI", "PRESSURE"), pressureLabel, if (scoreDiff < 0) SonHarfPink else SonHarfCyan)
                StatusChipV10("🏆", sh("HEDEF", "TARGET"), sh("10 puan", "10 points"), SonHarfGreen)
            }
        }''',
'bottom controls and status')

# Larger, better-centered player avatars.
replace_once(
'''private fun PlayerV10(name: String, gender: String?, avatarPath: String?, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier, isBot: Boolean = false) {
    Card(modifier=modifier.fillMaxHeight(), colors=CardDefaults.cardColors(containerColor=if(active) accent.copy(alpha=.10f) else SonHarfSurface), shape=RoundedCornerShape(17.dp), border=BorderStroke(1.dp, if(active) accent.copy(alpha=.55f) else SonHarfMuted.copy(alpha=.13f))) {
        Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment=Alignment.CenterVertically) {
            if (isBot) Box(Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha=.15f)), contentAlignment=Alignment.Center) { Text("🤖", fontSize=24.sp) }
            else Box(Modifier.size(44.dp)) {
                ProfilePhotoAvatar(avatarPath, name, 42.dp, visible = true, accent = accent)''',
'''private fun PlayerV10(name: String, gender: String?, avatarPath: String?, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier, isBot: Boolean = false) {
    Card(modifier=modifier.fillMaxHeight(), colors=CardDefaults.cardColors(containerColor=if(active) accent.copy(alpha=.10f) else SonHarfSurface), shape=RoundedCornerShape(18.dp), border=BorderStroke(1.dp, if(active) accent.copy(alpha=.55f) else SonHarfMuted.copy(alpha=.13f))) {
        Row(Modifier.fillMaxSize().padding(horizontal=7.dp, vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
            if (isBot) Box(Modifier.size(width=60.dp, height=66.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha=.15f)), contentAlignment=Alignment.Center) { Text("🤖", fontSize=31.sp) }
            else Box(Modifier.size(width=62.dp, height=68.dp), contentAlignment=Alignment.Center) {
                ProfilePhotoAvatar(avatarPath, name, 60.dp, visible = true, accent = accent, shape = RoundedCornerShape(14.dp))''',
'larger player avatar')
replace_once('''            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) { Text(name, maxLines=1, color=if(active) accent else SonHarfMuted, fontSize=10.sp, fontWeight=FontWeight.Bold); Text(score.toString(), fontWeight=FontWeight.Black, fontSize=21.sp); Text("$rounds round", color=SonHarfMuted, fontSize=9.sp) }''','''            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), verticalArrangement=Arrangement.Center) { Text(name, maxLines=1, color=if(active) accent else SonHarfMuted, fontSize=11.sp, fontWeight=FontWeight.Bold); Text(score.toString(), fontWeight=FontWeight.Black, fontSize=24.sp); Text("$rounds round", color=SonHarfMuted, fontSize=10.sp) }''','player text sizing')

# Add compact status component before KeyboardV10.
insert_anchor = '''@Composable
private fun KeyboardV10(language: String, enabled: Boolean, input: String, onInput: (String) -> Unit, onSubmit: () -> Unit) {'''
status_component = '''@Composable
private fun StatusChipV10(icon: String, title: String, value: String, tone: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 64.dp, max = 92.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 13.sp)
            Spacer(Modifier.width(2.dp))
            Text(title, color = SonHarfText, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
        Text(value, color = tone, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun KeyboardV10(language: String, enabled: Boolean, input: String, onInput: (String) -> Unit, onSubmit: () -> Unit) {'''
replace_once(insert_anchor, status_component, 'status chip component')

arena.write_text(text)

# Profile avatar supports rounded rectangles while keeping circle as default for all old call sites.
profile = Path('app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt')
p = profile.read_text()
if 'shape: androidx.compose.ui.graphics.Shape = CircleShape' not in p:
    p = p.replace(
'''    visible: Boolean = true,
    accent: Color = SonHarfCyan,
) {''',
'''    visible: Boolean = true,
    accent: Color = SonHarfCyan,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {''', 1)
    p = p.replace('''Modifier.size(size).clip(CircleShape).background''','''Modifier.size(size).clip(shape).background''',1)
    p = p.replace('''Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)''','''Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().clip(shape), contentScale = ContentScale.Crop)''',1)
    p = p.replace('''Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White)''','''Box(Modifier.fillMaxSize().clip(shape).background(Color.White)''',1)
profile.write_text(p)

# Remove redundant black matchmaking header wherever the exact duplicated label exists; keep the large cyan label.
for path in Path('app/src/main/java/com/sonharf/game').glob('*.kt'):
    s = path.read_text()
    original = s
    lines = s.splitlines(True)
    filtered = []
    for line in lines:
        if 'Text(sh("RAKİP BULUNUYOR", "FINDING OPPONENT")' in line and 'SonHarfCyan' not in line:
            continue
        filtered.append(line)
    s = ''.join(filtered)
    if s != original:
        path.write_text(s)

print('arena reference upgrade applied')
