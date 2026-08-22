package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

internal data class BilBakalimQuestion(
    val question: String,
    val answer: Double,
    val displayAnswer: String,
    val category: String,
)

internal val bilBakalimQuestions = listOf(
    BilBakalimQuestion("Burj Khalifa’nın yüksekliği kaç metredir?", 828.0, "828 metre", "Mimari"),
    BilBakalimQuestion("Fillerin hamilelik süresi yaklaşık kaç aydır?", 22.0, "22 ay", "Hayvanlar"),
    BilBakalimQuestion("Ay’a ilk kez hangi yıl ayak basılmıştır?", 1969.0, "1969", "Uzay"),
    BilBakalimQuestion("Ay yüzeyinde ilk golf vuruşu hangi yıl yapılmıştır?", 1971.0, "1971", "Uzay"),
    BilBakalimQuestion("Yetişkin bir insanın vücudunda kaç kemik vardır?", 206.0, "206", "İnsan"),
    BilBakalimQuestion("Yetişkin bir insanın genellikle kaç dişi vardır?", 32.0, "32", "İnsan"),
    BilBakalimQuestion("İnsan kalbinde kaç odacık vardır?", 4.0, "4", "İnsan"),
    BilBakalimQuestion("Güneş Sistemi’nde kaç gezegen vardır?", 8.0, "8", "Uzay"),
    BilBakalimQuestion("Dünya’nın doğal uydusu kaç tanedir?", 1.0, "1", "Uzay"),
    BilBakalimQuestion("Bir artık yılda kaç gün vardır?", 366.0, "366 gün", "Genel"),
    BilBakalimQuestion("Bir düzinede kaç adet bulunur?", 12.0, "12", "Genel"),
    BilBakalimQuestion("Standart bir satranç tahtasında kaç kare vardır?", 64.0, "64", "Oyun"),
    BilBakalimQuestion("Olimpiyat sembolünde kaç halka vardır?", 5.0, "5", "Spor"),
    BilBakalimQuestion("Bir maratonun resmi uzunluğu kaç kilometredir?", 42.195, "42,195 km", "Spor"),
    BilBakalimQuestion("FIFA Dünya Kupası final turnuvası 2026’da kaç takımla oynanacaktır?", 48.0, "48 takım", "Spor"),
    BilBakalimQuestion("Periyodik tabloda bilinen kaç element vardır?", 118.0, "118", "Bilim"),
    BilBakalimQuestion("Işığın boşluktaki hızı yaklaşık saniyede kaç kilometredir?", 299792.0, "299.792 km/s", "Bilim"),
    BilBakalimQuestion("Dünya’nın ekvator çevresi yaklaşık kaç kilometredir?", 40075.0, "40.075 km", "Dünya"),
    BilBakalimQuestion("Dünya’nın yaşı yaklaşık kaç milyar yıldır?", 4.54, "4,54 milyar yıl", "Dünya"),
    BilBakalimQuestion("Everest’in deniz seviyesinden yüksekliği yaklaşık kaç metredir?", 8849.0, "8.849 metre", "Dünya"),
    BilBakalimQuestion("Mariana Çukuru’nun en derin noktası yaklaşık kaç metredir?", 10984.0, "10.984 metre", "Dünya"),
    BilBakalimQuestion("Eyfel Kulesi anteniyle birlikte yaklaşık kaç metre yüksekliğindedir?", 330.0, "330 metre", "Mimari"),
    BilBakalimQuestion("Özgürlük Heykeli kaidesiyle birlikte yaklaşık kaç metre yüksekliğindedir?", 93.0, "93 metre", "Mimari"),
    BilBakalimQuestion("Pisa Kulesi yaklaşık kaç metre yüksekliğindedir?", 57.0, "57 metre", "Mimari"),
    BilBakalimQuestion("İstanbul’un fethi hangi yıl gerçekleşmiştir?", 1453.0, "1453", "Tarih"),
    BilBakalimQuestion("Türkiye Cumhuriyeti hangi yıl ilan edilmiştir?", 1923.0, "1923", "Tarih"),
    BilBakalimQuestion("Birinci Dünya Savaşı hangi yıl başlamıştır?", 1914.0, "1914", "Tarih"),
    BilBakalimQuestion("İkinci Dünya Savaşı hangi yıl sona ermiştir?", 1945.0, "1945", "Tarih"),
    BilBakalimQuestion("Titanik hangi yıl batmıştır?", 1912.0, "1912", "Tarih"),
    BilBakalimQuestion("İlk modern Olimpiyat Oyunları hangi yıl düzenlenmiştir?", 1896.0, "1896", "Spor"),
    BilBakalimQuestion("İlk iPhone hangi yıl satışa çıkmıştır?", 2007.0, "2007", "Teknoloji"),
    BilBakalimQuestion("World Wide Web önerisi Tim Berners-Lee tarafından hangi yıl sunulmuştur?", 1989.0, "1989", "Teknoloji"),
    BilBakalimQuestion("Apollo 11 görevinde Ay’a kaç astronot inmiştir?", 2.0, "2", "Uzay"),
    BilBakalimQuestion("Uluslararası Uzay İstasyonu Dünya çevresini yaklaşık kaç dakikada dolaşır?", 90.0, "90 dakika", "Uzay"),
    BilBakalimQuestion("Dünya’nın Güneş çevresindeki bir turu yaklaşık kaç gün sürer?", 365.25, "365,25 gün", "Uzay"),
    BilBakalimQuestion("Sesin havadaki hızı yaklaşık saniyede kaç metredir?", 343.0, "343 m/s", "Bilim"),
    BilBakalimQuestion("Suyun deniz seviyesinde kaynama noktası kaç santigrat derecedir?", 100.0, "100 °C", "Bilim"),
    BilBakalimQuestion("Suyun donma noktası kaç santigrat derecedir?", 0.0, "0 °C", "Bilim"),
    BilBakalimQuestion("Bir insan hücresinde normalde kaç kromozom bulunur?", 46.0, "46", "İnsan"),
    BilBakalimQuestion("Bir ahtapotun kaç kalbi vardır?", 3.0, "3", "Hayvanlar"),
    BilBakalimQuestion("Bir örümceğin kaç bacağı vardır?", 8.0, "8", "Hayvanlar"),
    BilBakalimQuestion("Bir arının kaç bacağı vardır?", 6.0, "6", "Hayvanlar"),
    BilBakalimQuestion("Mavi balinanın kalbi yaklaşık kaç kilogram olabilir?", 180.0, "yaklaşık 180 kg", "Hayvanlar"),
    BilBakalimQuestion("Kaplanlar kısa mesafede saatte yaklaşık kaç kilometre hıza ulaşabilir?", 65.0, "yaklaşık 65 km/sa", "Hayvanlar"),
    BilBakalimQuestion("Yetişkin bir fil günde yaklaşık kaç litre su içebilir?", 160.0, "yaklaşık 160 litre", "Hayvanlar"),
    BilBakalimQuestion("Beyaz gergedanlar yaklaşık en fazla kaç kilogram ağırlığa ulaşabilir?", 3500.0, "yaklaşık 3.500 kg", "Hayvanlar"),
    BilBakalimQuestion("Filler yerden kaç metre yüksekliğe zıplayabilir?", 0.0, "0 metre", "Hayvanlar"),
    BilBakalimQuestion("Dünyanın en büyük okyanusu olan Pasifik’in yüzölçümü yaklaşık kaç milyon km²’dir?", 165.0, "yaklaşık 165 milyon km²", "Dünya"),
    BilBakalimQuestion("Türkiye’nin yüzölçümü yaklaşık kaç bin km²’dir?", 783.0, "yaklaşık 783 bin km²", "Türkiye"),
    BilBakalimQuestion("Türkiye’de kaç il vardır?", 81.0, "81", "Türkiye"),
)

@Composable
internal fun BilBakalimHomeCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF112A3E)),
        border = BorderStroke(1.dp, Color(0xFFD8AC5C).copy(alpha = .72f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(
                    Brush.linearGradient(listOf(Color(0xFFD8AC5C), Color(0xFFF1D79A)))
                ),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF2B1E0B), modifier = Modifier.size(31.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("BİL BAKALIM", color = Color(0xFFF7F4EC), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Doğru cevaba en yakın tahmin kazanır", color = Color(0xFF6C8293), fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text("20 sn • +10 puan", color = Color(0xFFF0D59A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.Bolt, null, tint = Color(0xFFD8AC5C), modifier = Modifier.size(28.dp))
        }
    }
}

private enum class BilPhase { ANSWER, LOCKED, RESULT, MATCH_END }

@Composable
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

@Composable
private fun NumericEstimatePad(value: String, onValue: (String) -> Unit, compact: Boolean = false) {
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
                        modifier = Modifier.weight(1f).height(if (compact) 33.dp else 40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7F6FD), contentColor = Color(0xFF17344A)),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(key, fontWeight = FontWeight.Black, fontSize = if (compact) 17.sp else 20.sp) }
                }
            }
        }
    }
}

@Composable
private fun ScoreBox(label: String, score: Int, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, accent.copy(alpha = .65f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFF6C8293), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("$score", color = accent, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AnswerLine(label: String, value: String, winner: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (winner) Color(0xFF0F3B2C) else Color(0xFF091723),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (winner) Color(0xFF39D875) else Color(0xFF29445E)),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFF6C8293), fontSize = 11.sp)
            Text(value, color = if (winner) Color(0xFF39D875) else Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = if (winner) 31.sp else 26.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun prettyNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

@Composable
internal fun VipLockedAction(label: String, isVip: Boolean, onAllowed: () -> Unit, onLocked: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = { if (isVip) onAllowed() else onLocked() },
        modifier = modifier,
        border = BorderStroke(1.dp, if (isVip) Color(0xFFD8AC5C) else Color(0xFF576677)),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (!isVip) {
            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
