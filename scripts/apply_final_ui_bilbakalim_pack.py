from pathlib import Path

ROOT = Path('app/src/main/java/com/sonharf/game')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f'missing anchor: {label}')
    return text.replace(old, new, 1)

# 1) Main UI opens in Turkish. Individual games can still own their own language selector.
prefs = ROOT / 'SonHarfPreferences.kt'
t = prefs.read_text(encoding='utf-8')
t = replace_once(
    t,
    'fun syncUi(context: Context) { SonHarfUiState.darkMode = darkModeEnabled(context); SonHarfUiState.language = language(context) }',
    'fun syncUi(context: Context) { SonHarfUiState.darkMode = darkModeEnabled(context); SonHarfUiState.language = "tr" }',
    'turkish app ui default',
)
prefs.write_text(t, encoding='utf-8')

# 2) Logo: remote logo is optional; a bad/empty remote payload must always fall back to packaged logo.
logo = ROOT / 'SonHarfBrandLogo.kt'
t = logo.read_text(encoding='utf-8')
old = '''    val logo = remember(remoteBytes) {
        runCatching {
            val bytes = remoteBytes ?: run {
                val encoded = context.assets.open("son_harf_brand_logo.b64")
                    .bufferedReader()
                    .use { it.readText() }
                    .trim()
                Base64.decode(encoded, Base64.DEFAULT)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
'''
new = '''    val logo = remember(remoteBytes) {
        fun decode(bytes: ByteArray?): androidx.compose.ui.graphics.ImageBitmap? {
            if (bytes == null || bytes.isEmpty()) return null
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
        decode(remoteBytes) ?: runCatching {
            val encoded = context.assets.open("son_harf_brand_logo.b64")
                .bufferedReader()
                .use { it.readText() }
                .trim()
            decode(Base64.decode(encoded, Base64.DEFAULT))
        }.getOrNull()
    }
'''
t = replace_once(t, old, new, 'logo hard fallback')
logo.write_text(t, encoding='utf-8')

# 3) Home: keep cards stacked/full width on phones, shorten only vertically, keep Turkish home labels.
home = ROOT / 'PremiumMasterHome.kt'
t = home.read_text(encoding='utf-8')
t = t.replace('modifier = modifier.height(360.dp)', 'modifier = modifier.height(300.dp)', 1)
t = t.replace('size = 146.dp,', 'size = 82.dp,', 1)
t = t.replace('Text(sh("CANLI KELİME ARENASI", "LIVE WORD ARENA")', 'Text("CANLI KELİME ARENASI"', 1)
t = t.replace('Text(sh("Kelimenin son harfiyle zafer senin!", "Victory with the last letter!")', 'Text("Kelimenin son harfiyle zafer senin!"', 1)
t = t.replace('Text(sh("GALİBİYET", "WINS")', 'Text("GALİBİYET"', 1)
t = t.replace('Text(sh("OYNA", "PLAY")', 'Text("OYNA"', 1)
t = t.replace('Text(sh("MOD", "MODE")', 'Text("MOD"', 1)

start = t.find('@Composable private fun MasterBilBakalimCard(')
end = t.find('\n@Composable private fun MasterDailySeries', start)
if start < 0 or end < 0:
    raise SystemExit('missing anchor: MasterBilBakalimCard block')
new_bil_card = '''@Composable private fun MasterBilBakalimCard(modifier:Modifier,onPlay:()->Unit){
    Card(onClick=onPlay,modifier=modifier.height(250.dp),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent),border=BorderStroke(1.5.dp,MasterBlue2)){
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF4FDFF),Color(0xFFDDF6FF),Color.White))).padding(horizontal=12.dp,vertical=10.dp)){
            Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                MascotCardPreview(Modifier.width(112.dp).fillMaxHeight())
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                    Text("💡",fontSize=26.sp)
                    Text("BİL BAKALIM",color=MasterInk,fontSize=25.sp,fontWeight=FontWeight.Black,lineHeight=27.sp,maxLines=1,softWrap=false)
                    Spacer(Modifier.height(3.dp))
                    Text("Doğru cevaba en yakın tahmin kazanır!",modifier=Modifier.fillMaxWidth(),color=MasterInk,fontSize=10.sp,lineHeight=13.sp,textAlign=TextAlign.Center,maxLines=2)
                    Spacer(Modifier.height(7.dp))
                    Surface(shape=RoundedCornerShape(14.dp),color=MasterSky,border=BorderStroke(1.dp,MasterLine)){
                        Text("TÜRKÇE  •  ENGLISH",Modifier.padding(horizontal=10.dp,vertical=5.dp),color=MasterBlue,fontSize=8.sp,fontWeight=FontWeight.Black,maxLines=1)
                    }
                    Spacer(Modifier.height(7.dp))
                    Surface(shape=RoundedCornerShape(16.dp),color=MasterBlue){
                        Text("BUGÜNÜN MEYDAN OKUMASI",Modifier.padding(horizontal=10.dp,vertical=6.dp),color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Black,maxLines=1,softWrap=false)
                    }
                    Spacer(Modifier.height(7.dp))
                    Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(43.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=MasterBlue),contentPadding=PaddingValues(horizontal=10.dp)){
                        Icon(Icons.Rounded.PlayArrow,null,modifier=Modifier.size(20.dp));Spacer(Modifier.width(5.dp));Text("HEMEN OYNA",fontWeight=FontWeight.Black,fontSize=12.sp,maxLines=1,softWrap=false)
                    }
                }
            }
        }
    }
}
'''
t = t[:start] + new_bil_card + t[end:]
home.write_text(t, encoding='utf-8')

# 4) Bil Bakalim: independent TR/EN content mode, while shell UI stays Turkish by default.
bil = ROOT / 'BilBakalimFeature.kt'
t = bil.read_text(encoding='utf-8')
marker = ')\n\n@Composable\ninternal fun BilBakalimHomeCard'
if 'internal val bilBakalimQuestionsEn' not in t:
    english = ''')

internal val bilBakalimQuestionsEn = listOf(
    BilBakalimQuestion("How tall is the Burj Khalifa in metres?", 828.0, "828 metres", "Architecture"),
    BilBakalimQuestion("About how many months is an elephant pregnant?", 22.0, "22 months", "Animals"),
    BilBakalimQuestion("In what year did humans first land on the Moon?", 1969.0, "1969", "Space"),
    BilBakalimQuestion("How many bones are in a typical adult human body?", 206.0, "206", "Human Body"),
    BilBakalimQuestion("How many teeth does a typical adult human have?", 32.0, "32", "Human Body"),
    BilBakalimQuestion("How many chambers does the human heart have?", 4.0, "4", "Human Body"),
    BilBakalimQuestion("How many planets are in the Solar System?", 8.0, "8", "Space"),
    BilBakalimQuestion("How many days are in a leap year?", 366.0, "366 days", "General"),
    BilBakalimQuestion("How many squares are on a standard chessboard?", 64.0, "64", "Games"),
    BilBakalimQuestion("How many rings are in the Olympic symbol?", 5.0, "5", "Sports"),
    BilBakalimQuestion("What is the official marathon distance in kilometres?", 42.195, "42.195 km", "Sports"),
    BilBakalimQuestion("How many teams play in the 2026 FIFA World Cup finals?", 48.0, "48 teams", "Sports"),
    BilBakalimQuestion("How many confirmed chemical elements are in the periodic table?", 118.0, "118", "Science"),
    BilBakalimQuestion("About how fast does light travel in vacuum in kilometres per second?", 299792.0, "299,792 km/s", "Science"),
    BilBakalimQuestion("About how long is Earth's equatorial circumference in kilometres?", 40075.0, "40,075 km", "Earth"),
    BilBakalimQuestion("About how old is Earth in billions of years?", 4.54, "4.54 billion years", "Earth"),
    BilBakalimQuestion("About how high is Mount Everest above sea level in metres?", 8849.0, "8,849 metres", "Earth"),
    BilBakalimQuestion("About how deep is the deepest point of the Mariana Trench in metres?", 10984.0, "10,984 metres", "Earth"),
    BilBakalimQuestion("About how tall is the Eiffel Tower including its antenna in metres?", 330.0, "330 metres", "Architecture"),
    BilBakalimQuestion("In what year did World War I begin?", 1914.0, "1914", "History"),
    BilBakalimQuestion("In what year did World War II end?", 1945.0, "1945", "History"),
    BilBakalimQuestion("In what year did the Titanic sink?", 1912.0, "1912", "History"),
    BilBakalimQuestion("In what year were the first modern Olympic Games held?", 1896.0, "1896", "Sports"),
    BilBakalimQuestion("In what year did the first iPhone go on sale?", 2007.0, "2007", "Technology"),
    BilBakalimQuestion("How many astronauts walked on the Moon during Apollo 11?", 2.0, "2", "Space"),
    BilBakalimQuestion("About how many minutes does the International Space Station take to orbit Earth?", 90.0, "90 minutes", "Space"),
    BilBakalimQuestion("About how many days does Earth take to orbit the Sun?", 365.25, "365.25 days", "Space"),
    BilBakalimQuestion("About how fast does sound travel through air in metres per second?", 343.0, "343 m/s", "Science"),
    BilBakalimQuestion("At sea level, at what Celsius temperature does water boil?", 100.0, "100 °C", "Science"),
    BilBakalimQuestion("How many chromosomes are normally found in a human cell?", 46.0, "46", "Human Body"),
    BilBakalimQuestion("How many hearts does an octopus have?", 3.0, "3", "Animals"),
    BilBakalimQuestion("How many legs does a spider have?", 8.0, "8", "Animals"),
    BilBakalimQuestion("How many legs does a bee have?", 6.0, "6", "Animals"),
    BilBakalimQuestion("About how many kilograms can a blue whale's heart weigh?", 180.0, "about 180 kg", "Animals"),
    BilBakalimQuestion("About how fast can a tiger run over a short distance in km/h?", 65.0, "about 65 km/h", "Animals"),
    BilBakalimQuestion("About how many litres of water can an adult elephant drink in a day?", 160.0, "about 160 litres", "Animals"),
)

@Composable
internal fun BilBakalimHomeCard'''
    if marker not in t:
        raise SystemExit('missing anchor: English questions insertion')
    t = t.replace(marker, english, 1)

# Add independent language state before deck creation.
t = replace_once(
    t,
    '    var deck by remember { mutableStateOf(bilBakalimQuestions.shuffled().take(15)) }',
    '    var bilLanguage by remember { mutableStateOf("tr") }\n    fun bil(tr: String, en: String): String = if (bilLanguage == "en") en else tr\n    fun questionPool(): List<BilBakalimQuestion> = if (bilLanguage == "en") bilBakalimQuestionsEn else bilBakalimQuestions\n    var deck by remember(bilLanguage) { mutableStateOf(questionPool().shuffled().take(15)) }',
    'Bil language state',
)
t = t.replace('deck = bilBakalimQuestions.shuffled().take(15)', 'deck = questionPool().shuffled().take(15)', 1)

# Reset score/round when language changes.
anchor = '    val q = deck[questionIndex]\n    val questionNo = questionIndex + 1\n'
insert = '''    val q = deck[questionIndex]
    val questionNo = questionIndex + 1

    LaunchedEffect(bilLanguage) {
        questionIndex = 0
        playerScore = 0
        botScore = 0
        input = ""
        seconds = 20
        phase = BilPhase.ANSWER
        playerAnswer = null
        botAnswer = null
        playerWon = null
    }
'''
t = replace_once(t, anchor, insert, 'Bil language reset')

# Add selector below header row.
header_anchor = '''                Spacer(Modifier.width(42.dp))
            }

            Row(Modifier.fillMaxWidth().height(if (tiny) 68.dp else 78.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {'''
selector = '''                Spacer(Modifier.width(42.dp))
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = bilLanguage == "tr",
                    onClick = { bilLanguage = "tr" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("TÜRKÇE", fontWeight = FontWeight.Black, fontSize = if (tiny) 10.sp else 12.sp) }
                SegmentedButton(
                    selected = bilLanguage == "en",
                    onClick = { bilLanguage = "en" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("ENGLISH", fontWeight = FontWeight.Black, fontSize = if (tiny) 10.sp else 12.sp) }
            }

            Row(Modifier.fillMaxWidth().height(if (tiny) 68.dp else 78.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {'''
t = replace_once(t, header_anchor, selector, 'Bil selector')

# Localize gameplay labels by selected Bil language.
repls = {
    'ScoreBox("SEN", playerScore': 'ScoreBox(bil("SEN", "YOU"), playerScore',
    'val winnerName = if (playerIsWinner) playerProfile?.displayName ?: "Sen" else "KelimeBot BOT"': 'val winnerName = if (playerIsWinner) playerProfile?.displayName ?: bil("Sen", "You") else "KelimeBot BOT"',
    'Text(if (playerIsWinner) "KAZANDIN!" else "MAÇ BİTTİ"': 'Text(if (playerIsWinner) bil("KAZANDIN!", "YOU WON!") else bil("MAÇ BİTTİ", "MATCH OVER")',
    'Text("BİR OYUN DAHA", fontWeight = FontWeight.Black)': 'Text(bil("BİR OYUN DAHA", "PLAY AGAIN"), fontWeight = FontWeight.Black)',
    'Text("ANA MENÜ")': 'Text(bil("ANA MENÜ", "MAIN MENU"))',
    '"${q.category.uppercase()} • SORU $questionNo/15"': '"${q.category.uppercase()} • ${bil("SORU", "QUESTION")} $questionNo/15"',
    'input.ifBlank { "Tahminin" }': 'input.ifBlank { bil("Tahminin", "Your guess") }',
    'Text("CEVABI KİLİTLE"': 'Text(bil("CEVABI KİLİTLE", "LOCK ANSWER")',
    'AnswerLine("Senin cevabın", playerAnswer?.let(::prettyNumber) ?: "Cevap yok"': 'AnswerLine(bil("Senin cevabın", "Your answer"), playerAnswer?.let(::prettyNumber) ?: bil("Cevap yok", "No answer")',
    'AnswerLine("Bot cevabı", botAnswer?.let(::prettyNumber) ?: "Cevap bekleniyor…"': 'AnswerLine(bil("Bot cevabı", "Bot answer"), botAnswer?.let(::prettyNumber) ?: bil("Cevap bekleniyor…", "Waiting for answer…")',
    'Text("DOĞRU CEVAP"': 'Text(bil("DOĞRU CEVAP", "CORRECT ANSWER")',
    'Text(if (playerWon == true) "KAZANDIN! • +10 PUAN" else "YANLIŞ CEVAP"': 'Text(if (playerWon == true) bil("KAZANDIN! • +10 PUAN", "YOU WON! • +10 POINTS") else bil("YANLIŞ CEVAP", "WRONG ANSWER")',
    'Text(if (questionNo == 15) "MAÇI BİTİR" else "SONRAKİ SORU"': 'Text(if (questionNo == 15) bil("MAÇI BİTİR", "FINISH MATCH") else bil("SONRAKİ SORU", "NEXT QUESTION")',
}
for old, new in repls.items():
    if old not in t:
        raise SystemExit(f'missing Bil label anchor: {old[:50]}')
    t = t.replace(old, new, 1)

bil.write_text(t, encoding='utf-8')

print('final UI + Bil Bakalim TR/EN pack applied')
