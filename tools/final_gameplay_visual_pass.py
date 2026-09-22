from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def text(path):
    return (ROOT / path).read_text(encoding="utf-8")

def write(path, value):
    (ROOT / path).write_text(value, encoding="utf-8")

def replace_once(path, old, new):
    s = text(path)
    count = s.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one exact match, got {count}: {old[:90]!r}")
    write(path, s.replace(old, new, 1))

def replace_all(path, old, new, expected_min=1):
    s = text(path)
    count = s.count(old)
    if count < expected_min:
        raise RuntimeError(f"{path}: expected >= {expected_min} matches, got {count}: {old[:90]!r}")
    write(path, s.replace(old, new))

def regex_once(path, pattern, replacement):
    s = text(path)
    out, count = re.subn(pattern, replacement, s, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"{path}: regex expected one match, got {count}: {pattern[:90]!r}")
    write(path, out)

# -----------------------------------------------------------------------------
# KELIME KUSATMASI: premium physical board, wood rack, premium typography.
# Territory/bonus colors and all gameplay/scoring logic remain unchanged.
# -----------------------------------------------------------------------------
p = "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt"
replace_once(p, "import androidx.compose.ui.graphics.Color\n", "import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color\n")
replace_once(p, "import androidx.compose.ui.text.font.FontWeight\n", "import androidx.compose.ui.text.font.FontFamily\nimport androidx.compose.ui.text.font.FontWeight\n")
replace_once(p,
'''    Surface(
        modifier = modifier,
        color = PanSiegeFrameNavy,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, PanSiegeFrameEdge),
        shadowElevation = 8.dp,
    ) {''',
'''    Surface(
        modifier = modifier,
        color = Color(0xFF2F1D13),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, Color(0xFFC6A56B)),
        shadowElevation = 14.dp,
    ) {''')
replace_once(p,
'''.clip(RoundedCornerShape(15.dp))
                .background(PanSiegeBoardSurface)
                .border(1.dp, PanSiegeFrameInner, RoundedCornerShape(15.dp))''',
'''.clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF3B2518), Color(0xFF6A452B), Color(0xFF4A2F1E), Color(0xFF785238))
                    )
                )
                .border(1.dp, Color(0xFFB88C55), RoundedCornerShape(14.dp))''')
replace_once(p,
'''    val baseColor = when {
        pending -> PanSiegeTile
        letter != null -> territoryColor
        bonusSurface != null -> bonusSurface
        else -> PanSiegeNeutral
    }
    val border = when {''',
'''    val baseColor = when {
        pending -> PanSiegeTile
        letter != null -> territoryColor
        bonusSurface != null -> bonusSurface
        else -> PanSiegeNeutral
    }
    val displayBase = if (pending) Color(0xFFF2DFC0) else baseColor
    val border = when {''')
replace_once(p,
'''.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(baseColor, Color.White, .12f), baseColor)))''',
'''.background(
                Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(displayBase, Color.White, .18f),
                        displayBase,
                        androidx.compose.ui.graphics.lerp(displayBase, Color.Black, .07f),
                    )
                )
            )''')
replace_once(p,
'''                    Text(letter, color = Color(0xFF17372C), fontSize = 21.sp, fontWeight = FontWeight.Black)''',
'''                    Text(
                        letter,
                        color = Color(0xFF2A1B13),
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .35.sp,
                    )''')
replace_once(p,
'''        color = when {
            used -> WordSiegeGameUi.SurfaceSoft
            selected -> Color(0xFFE1ECE4)
            else -> PanSiegeTile
        },
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PanSiegeMineBorder else PanSiegeTileBorder.copy(alpha = .7f)),
        shadowElevation = if (selected) 3.dp else 2.dp,''',
'''        color = when {
            used -> WordSiegeGameUi.SurfaceSoft
            selected -> Color(0xFFEBCB92)
            else -> Color(0xFFF2DFC0)
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PanSiegeMineBorder else Color(0xFF8A6841)),
        shadowElevation = if (selected) 7.dp else 4.dp,''')
replace_once(p,
'''            Text(letter.toString(), color = if (used) WordSiegeGameUi.Muted.copy(alpha = .45f) else WordSiegeGameUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)''',
'''            Text(
                letter.toString(),
                color = if (used) WordSiegeGameUi.Muted.copy(alpha = .45f) else Color(0xFF2A1B13),
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                letterSpacing = .35.sp,
            )''')

p = "app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt"
replace_once(p, "import androidx.compose.ui.graphics.Color\n", "import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color\n")
replace_once(p, "import androidx.compose.ui.text.font.FontWeight\n", "import androidx.compose.ui.text.font.FontFamily\nimport androidx.compose.ui.text.font.FontWeight\n")
replace_once(p,
'''    Surface(
        modifier = modifier,
        color = PracticeSiegeBoardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .70f)),
        shadowElevation = 1.dp,
    ) {''',
'''    Surface(
        modifier = modifier,
        color = Color(0xFF2F1D13),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, Color(0xFFC6A56B)),
        shadowElevation = 12.dp,
    ) {''')
replace_once(p,
'''            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .clipToBounds()''',
'''            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3B2518), Color(0xFF6A452B), Color(0xFF4A2F1E), Color(0xFF785238))))
                .clipToBounds()''')
replace_once(p,
'''    val cellColor = when {
        pending -> PracticeSiegeTile
        letter != null -> territory
        zoneSurface != null -> zoneSurface
        else -> PracticeSiegeEmpty
    }
    val borderColor = when {''',
'''    val cellColor = when {
        pending -> PracticeSiegeTile
        letter != null -> territory
        zoneSurface != null -> zoneSurface
        else -> PracticeSiegeEmpty
    }
    val displayCellColor = if (pending) Color(0xFFF2DFC0) else cellColor
    val borderColor = when {''')
replace_once(p,
'''.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(cellColor, Color.White, .12f), cellColor)))''',
'''.background(
                Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.White, .18f),
                        displayCellColor,
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.Black, .07f),
                    )
                )
            )''')
replace_once(p,
'''            Text(letter, color = PracticeSiegeLightTileText, fontSize = 21.sp, fontWeight = FontWeight.Black)''',
'''            Text(
                letter,
                color = Color(0xFF2A1B13),
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                letterSpacing = .35.sp,
            )''')
replace_once(p,
'''            else -> PracticeSiegeTile
        },
        shape = RoundedCornerShape(11.dp),''',
'''            else -> Color(0xFFF2DFC0)
        },
        shape = RoundedCornerShape(9.dp),''')
replace_once(p,
'''                fontSize = 20.sp,
                fontWeight = FontWeight.Black,''',
'''                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                letterSpacing = .35.sp,''')

# Premium tools: server-authoritative behavior stays intact; presentation becomes explicit.
p = "app/src/main/java/com/sonharf/game/WordSiegePremiumPanel.kt"
replace_all(p, 'sh("PUAN HESAPLAMA", "SCORE PREVIEW")', 'sh("TOPLAM PUAN", "TOTAL SCORE")')
replace_all(p, 'sh("PRO ile açılır", "Unlocks with PRO")', 'sh("PRO özelliği", "PRO feature")')
replace_once(p,
'''                                    preview != null -> sh(
                                        "Kelime +${preview!!.wordScore} • Bölge +${preview!!.areaScore} • Toplam +${preview!!.totalScore}",
                                        "Word +${preview!!.wordScore} • Territory +${preview!!.areaScore} • Total +${preview!!.totalScore}",
                                    )''',
'''                                    preview != null -> sh(
                                        "Toplam ${preview!!.totalScore} • Kelime ${preview!!.wordScore} • Bölge ${preview!!.areaScore}",
                                        "Total ${preview!!.totalScore} • Word ${preview!!.wordScore} • Territory ${preview!!.areaScore}",
                                    )''')
replace_all(p, 'sh("HARFLER", "LETTERS")', 'sh("KALAN HARFLER", "REMAINING")')
replace_once(p, 'fontSize = 10.sp,\n                                fontWeight = FontWeight.Bold,', 'fontSize = 11.sp,\n                                fontWeight = FontWeight.Black,')

# -----------------------------------------------------------------------------
# SON HARF: full competitive arena visual language. Logic/networking untouched.
# -----------------------------------------------------------------------------
p = "app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt"
regex_once(p, r'''private object PremierUi \{.*?\n\}\n\nprivate fun pt''', '''private object PremierUi {
    val Background = Color(0xFF06101D)
    val Surface = Color(0xFF0D1B2A)
    val Ink = Color(0xFFF4F7FB)
    val Muted = Color(0xFF9AAFC4)
    val Ocean = Color(0xFF00D6C9)
    val OceanDeep = Color(0xFF2F6BFF)
    val Sky = Color(0xFF8B5CF6)
    val Ice = Color(0xFF13273A)
    val Border = Color(0xFF27445E)
    val Green = Color(0xFF24D6A3)
    val GreenSoft = Color(0xFF0D3A32)
    val Red = Color(0xFFFF5C5C)
    val RedSoft = Color(0xFF3A1A23)
    val Gold = Color(0xFFFFB64A)
    val GoldSoft = Color(0xFF3A2A13)
}

private fun pt''')
replace_once(p,
'''        Column(Modifier.fillMaxSize().statusBarsPadding()) {''',
'''        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF050B14), Color(0xFF0A1930), Color(0xFF07111F))
                    )
                )
                .statusBarsPadding()
        ) {''')
replace_once(p,
'''                if (reconnectGraceActive) {
                    PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                } else {
                    PremierTurnBadge(language, myTurn, room.status)
                }
                Spacer(Modifier.height(primaryGap))
                PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)''',
'''                if (reconnectGraceActive) {
                    PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                } else {
                    PremierTurnBadge(language, myTurn, room.status)
                }
                if (!veryCompact) {
                    Spacer(Modifier.height(4.dp))
                    PremierPressureStrip(language, myScore, rivalScore, myStreak, rivalStreak, turnSeconds)
                }
                Spacer(Modifier.height(primaryGap))
                PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)''')
replace_once(p,
'''    val danger = seconds in 1..5
    val timerStart = if (danger) PremierUi.Red else Color(0xFF2A63D0)''',
'''    val danger = seconds in 1..5
    val urgencyTransition = rememberInfiniteTransition(label = "duel-urgency")
    val urgencyPulse by urgencyTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
        label = "duel-urgency-pulse",
    )
    val timerStart = if (danger) PremierUi.Red else Color(0xFF2A63D0)''')
replace_once(p,
'''                Surface(shape = CircleShape, color = Color.Transparent) {
                    Box(Modifier.size(60.dp).background(Brush.radialGradient(listOf(timerStart, if (danger) PremierUi.Red else PremierUi.OceanDeep, timerEnd)), CircleShape), contentAlignment = Alignment.Center) {''',
'''                Surface(shape = CircleShape, color = Color.Transparent) {
                    Box(
                        Modifier
                            .size(if (danger) (68f * urgencyPulse).dp else 68.dp)
                            .background(Brush.radialGradient(listOf(timerStart, if (danger) PremierUi.Red else PremierUi.OceanDeep, timerEnd)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {''')
regex_once(p, r'''@Composable\nprivate fun PremierTurnBadge\(.*?\n\}\n\n@Composable\nprivate fun PremierReconnectBanner''', '''@Composable
private fun PremierTurnBadge(language: String, myTurn: Boolean, status: String) {
    val active = status in setOf("playing", "final", "sudden_death")
    val accent = if (myTurn) PremierUi.Ocean else PremierUi.Gold
    val label = when {
        !active -> pt(language, "ARENA SENKRONİZE EDİLİYOR", "SYNCING ARENA")
        myTurn -> pt(language, "⚡ HAMLE SENDE • SALDIR", "⚡ YOUR MOVE • STRIKE")
        else -> pt(language, "◉ RAKİP HAMLESİ • HAZIR OL", "◉ RIVAL MOVE • STAY READY")
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .55f)),
        shadowElevation = if (myTurn) 5.dp else 0.dp,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .7.sp,
        )
    }
}

@Composable
private fun PremierReconnectBanner''')
insert_marker = '''@Composable
private fun PremierTargetCard(language: String, required: String, gameMode: String, round: Int, size: Dp) {'''
pressure = '''@Composable
private fun PremierPressureStrip(
    language: String,
    myScore: Int,
    rivalScore: Int,
    myStreak: Int,
    rivalStreak: Int,
    seconds: Int,
) {
    val lead = myScore - rivalScore
    val danger = seconds in 1..5
    val accent = when {
        danger -> PremierUi.Red
        lead > 0 -> PremierUi.Green
        lead < 0 -> PremierUi.Gold
        else -> PremierUi.Ocean
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF081624),
        border = BorderStroke(1.dp, accent.copy(alpha = .42f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                when {
                    danger -> pt(language, "KRİTİK 5 SANİYE", "CRITICAL 5 SECONDS")
                    lead > 0 -> pt(language, "BASKI SENDE +$lead", "YOUR PRESSURE +$lead")
                    lead < 0 -> pt(language, "GERİ DÖNÜŞ FIRSATI ${-lead}", "COMEBACK WINDOW ${-lead}")
                    else -> pt(language, "DENGE NOKTASI", "DEAD EVEN")
                },
                color = accent,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .5.sp,
            )
            Text(
                pt(language, "SERİ $myStreak : $rivalStreak", "STREAK $myStreak : $rivalStreak"),
                color = PremierUi.Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

'''
replace_once(p, insert_marker, pressure + insert_marker)
regex_once(p, r'''@Composable\nprivate fun PremierTargetCard\(.*?\n\}\n\n@Composable\nprivate fun PremierWordTrail''', '''@Composable
private fun PremierTargetCard(language: String, required: String, gameMode: String, round: Int, size: Dp) {
    val transition = rememberInfiniteTransition(label = "target-reactor")
    val glow by transition.animateFloat(.72f, 1f, infiniteRepeatable(tween(620), RepeatMode.Reverse), label = "reactor-glow")
    val targetBadge = when {
        required == "★" -> "FREE"
        gameMode == "expert" -> "x${round.coerceIn(1, 3)}"
        required.length == 1 -> "${com.sonharf.game.data.DictionaryEngine.getLetterPoint(required.first(), language)}P"
        else -> "x${round.coerceIn(1, 3)}"
    }
    Box(
        Modifier
            .size(size)
            .shadow(22.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        PremierUi.Ocean.copy(alpha = .35f * glow),
                        Color(0xFF17487C),
                        Color(0xFF08111F),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize(.88f)
                .clip(CircleShape)
                .background(Color(0xFF07111F).copy(alpha = .74f))
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF245EA8), Color(0xFF0A203A), Color(0xFF050B14)))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        required,
                        color = Color.White,
                        fontSize = (size.value * if (required.length > 1) .28f else .41f).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        if (required == "★") pt(language, "SERBEST VURUŞ", "FREE STRIKE") else pt(language, "HEDEF HARF", "TARGET LETTER"),
                        color = PremierUi.Ocean,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
            shape = RoundedCornerShape(7.dp),
            color = PremierUi.Gold,
        ) {
            Text(targetBadge, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = Color(0xFF171006), fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremierWordTrail''')
regex_once(p, r'''@Composable\nprivate fun PremierInputBar\(.*?\n\}\n\n@Composable\nprivate fun PremierKeyboard''', '''@Composable
private fun PremierInputBar(language: String, input: String, required: String, myTurn: Boolean, busy: Boolean, modifier: Modifier = Modifier) {
    val requiredLetterBadge = myTurn && input.isBlank() && required.isNotBlank() && required != "★"
    val accent = if (myTurn) PremierUi.Ocean else PremierUi.Border
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF091725),
        border = BorderStroke(if (myTurn) 2.dp else 1.dp, accent),
        shadowElevation = if (myTurn) 9.dp else 1.dp,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = if (myTurn) PremierUi.Ocean else PremierUi.Muted, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            if (requiredLetterBadge) {
                Surface(shape = RoundedCornerShape(7.dp), color = PremierUi.Gold.copy(alpha = .18f), border = BorderStroke(1.5.dp, PremierUi.Gold)) {
                    Text(required.uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = PremierUi.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                when {
                    busy -> pt(language, "DOĞRULANIYOR…", "VERIFYING…")
                    input.isNotBlank() -> input
                    required == "★" -> pt(language, "KELİMENİ ATEŞLE…", "FIRE YOUR WORD…")
                    else -> pt(language, "HEDEF HARFLE KELİME KUR…", "BUILD FROM THE TARGET…")
                },
                color = if (input.isBlank()) PremierUi.Muted else PremierUi.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = .3.sp,
            )
        }
    }
}

@Composable
private fun PremierKeyboard''')
replace_once(p,
'''    Surface(color = palette.background, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), border = BorderStroke(1.dp, palette.border), shadowElevation = 10.dp) {''',
'''    Surface(
        color = Color(0xFF050C15),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        border = BorderStroke(1.dp, PremierUi.Border),
        shadowElevation = 14.dp,
    ) {''')
replace_once(p, 'shape = RoundedCornerShape(9.dp),', 'shape = RoundedCornerShape(6.dp),')
replace_once(p, 'elevation = ButtonDefaults.buttonElevation(defaultElevation = if (action) 4.dp else 1.dp),', 'elevation = ButtonDefaults.buttonElevation(defaultElevation = if (action) 7.dp else 2.dp),')

# -----------------------------------------------------------------------------
# KELIME YOLU: clean header, 4-step puzzle, 4 progress lights, New Game only.
# -----------------------------------------------------------------------------
p = "app/src/main/java/com/sonharf/game/LetterLadderGame.kt"
replace_once(p, 'const val MOVE_COUNT = 5', 'const val MOVE_COUNT = 4')
replace_once(p, 'In a five-move puzzle each of the five positions is changed exactly once.', 'In a four-move puzzle four of the five positions are changed exactly once.')
replace_once(p, 'maximum depth five', 'maximum depth four')
replace_once(p,
'''        if (used.size != WORD_LENGTH) return null
        val start = path.first()
        val target = path.last()
        if ((0 until WORD_LENGTH).any { start[it] == target[it] }) return null''',
'''        if (used.size != MOVE_COUNT) return null
        val start = path.first()
        val target = path.last()
        if ((0 until WORD_LENGTH).count { start[it] == target[it] } != WORD_LENGTH - MOVE_COUNT) return null''')
replace_all(p, 'Hedefe ulaştın! Beş harfin tamamı kilitlendi.', 'Hedefe ulaştın! Dört değişim tamamlandı.')
replace_all(p, 'Target reached! All five positions are locked.', 'Target reached! All four changes are complete.')
replace_once(p,
'''                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.harf_yolu_logo),
                            contentDescription = sh("Harf Yolu logosu", "Letter Path logo"),
                            modifier = Modifier.width(116.dp).height(54.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(LetterLadderUi.AccentStrong),
                        )
                        Text(
                            sh("5 hamle • Her kutu yalnızca 1 kez değişir", "5 moves • Each position changes only once"),
                            color = LetterLadderUi.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )''',
'''                        Text(
                            sh("KELİME YOLU", "WORD PATH"),
                            color = LetterLadderUi.Text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .6.sp,
                        )
                        Text(
                            sh("4 hamle • Her değişim hedefe yaklaşır", "4 moves • Every change closes the gap"),
                            color = LetterLadderUi.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )''')
replace_once(p, '"${usedPositions.size}/5"', '"${usedPositions.size}/${LetterLadderEngine.MOVE_COUNT}"')
regex_once(p, r'''                        Row\(horizontalArrangement = Arrangement\.spacedBy\(4\.dp\)\) \{\n                            repeat\(5\) \{ index ->\n                                val locked = index in usedPositions.*?\n                            \}\n                        \}''', '''                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            val progressCount = usedPositions.size.coerceIn(0, LetterLadderEngine.MOVE_COUNT)
                            repeat(LetterLadderEngine.MOVE_COUNT) { index ->
                                val reached = index < progressCount
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = CircleShape,
                                    color = if (reached) LetterLadderUi.Green else LetterLadderUi.SurfaceSoft,
                                    border = BorderStroke(1.dp, if (reached) LetterLadderUi.Green else LetterLadderUi.Border),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (reached) {
                                            Icon(Icons.Rounded.Check, null, tint = LetterLadderUi.AccentText, modifier = Modifier.size(11.dp))
                                        } else {
                                            Surface(modifier = Modifier.size(4.dp), shape = CircleShape, color = LetterLadderUi.Muted.copy(alpha = .45f)) {}
                                        }
                                    }
                                }
                            }
                        }''')
regex_once(p, r'''                        OutlinedButton\(\n                            enabled = path\.size > 1,\n                            onClick = \{.*?\n                        \) \{\n                            Text\(sh\("GERİ AL", "UNDO"\), fontWeight = FontWeight\.Black, fontSize = 10\.sp, maxLines = 1\)\n                        \}''', '''                        OutlinedButton(
                            enabled = false,
                            onClick = {},
                            modifier = Modifier.weight(1f).height(40.dp).sonHarfPressScale(pressedScale = 0.94f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, LetterLadderUi.Accent.copy(alpha = .72f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LetterLadderUi.AccentStrong,
                                disabledContentColor = LetterLadderUi.Muted.copy(alpha = .34f),
                            ),
                        ) {
                            Text(sh("YENİ OYUN", "NEW GAME"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                        }''')
replace_all(p, 'sh("Son hamleyi geri al ve farklı bir yol dene.", "Undo the last move and try a different route.")', 'sh("Bu rota kapanmış görünüyor. Oyunu tamamlayıp yeni oyuna geç.", "This route is closed. Finish the puzzle, then start a new game.")')
replace_all(p, 'sh("5 hamlede hedef kelimeye ulaş", "Reach the target in 5 moves")', 'sh("4 hamlede hedef kelimeye ulaş", "Reach the target in 4 moves")')

# Clean the top of the Letter Path backdrop: no decorative marks behind the header.
p = "app/src/main/java/com/sonharf/game/HarfYoluBackdrop.kt"
replace_once(p, '        glow(blue, .93f + .015f * phase, .14f + .010f * phase, .58f, .17f)\n', '')
regex_once(p, r'''        val tiles = listOf\(.*?\n        \)''', '''        val tiles = listOf(
            Tile(.02f, .34f, .030f, orange),
            Tile(.97f, .46f, .034f, purple),
            Tile(.035f, .67f, .052f, turquoise),
            Tile(.96f, .72f, .046f, blue),
            Tile(.10f, .89f, .034f, purple),
            Tile(.88f, .93f, .040f, orange),
            Tile(.02f, .93f, .026f, turquoise),
            Tile(.98f, .60f, .024f, orange),
        )''')

print("Final gameplay visual pass applied successfully")
