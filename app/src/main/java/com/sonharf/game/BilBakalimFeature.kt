package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
                Text("Doğru cevaba en yakın tahmin kazanır", color = Color(0xFFB6C0CA), fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text("20 sn • +10 puan", color = Color(0xFFF0D59A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.Bolt, null, tint = Color(0xFFD8AC5C), modifier = Modifier.size(28.dp))
        }
    }
}

private enum class BilPhase { ANSWER, LOCKED, RESULT }

@Composable
fun BilBakalimStandaloneScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var questionIndex by remember { mutableIntStateOf(Random.nextInt(bilBakalimQuestions.size)) }
    var questionNo by remember { mutableIntStateOf(1) }
    var playerScore by remember { mutableIntStateOf(0) }
    var botScore by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(20) }
    var phase by remember { mutableStateOf(BilPhase.ANSWER) }
    var playerAnswer by remember { mutableStateOf<Double?>(null) }
    var botAnswer by remember { mutableStateOf<Double?>(null) }
    var playerWon by remember { mutableStateOf<Boolean?>(null) }
    val focus = LocalFocusManager.current
    val q = bilBakalimQuestions[questionIndex]

    fun nextQuestion() {
        var next = Random.nextInt(bilBakalimQuestions.size)
        if (bilBakalimQuestions.size > 1) while (next == questionIndex) next = Random.nextInt(bilBakalimQuestions.size)
        questionIndex = next
        questionNo += 1
        input = ""
        seconds = 20
        phase = BilPhase.ANSWER
        playerAnswer = null
        botAnswer = null
        playerWon = null
    }

    fun finishRound(answer: Double?) {
        if (phase != BilPhase.ANSWER) return
        playerAnswer = answer
        phase = BilPhase.LOCKED
        focus.clearFocus()
        scope.launch {
            delay(800)
            val spread = max(1.0, abs(q.answer) * Random.nextDouble(.08, .42))
            val sign = if (Random.nextBoolean()) 1 else -1
            val generated = if (q.answer == 0.0) Random.nextDouble(0.0, 4.0) else max(0.0, q.answer + sign * spread)
            botAnswer = generated
            delay(500)
            val p = playerAnswer
            val pDiff = if (p == null) Double.POSITIVE_INFINITY else abs(p - q.answer)
            val bDiff = abs(generated - q.answer)
            playerWon = pDiff <= bDiff
            if (playerWon == true) playerScore += 10 else botScore += 10
            phase = BilPhase.RESULT
        }
    }

    LaunchedEffect(questionIndex, phase) {
        if (phase != BilPhase.ANSWER) return@LaunchedEffect
        seconds = 20
        while (seconds > 0 && phase == BilPhase.ANSWER) {
            delay(1000)
            seconds -= 1
        }
        if (seconds <= 0 && phase == BilPhase.ANSWER) finishRound(null)
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF071525), Color(0xFF0C1D2E), Color(0xFF07111D)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = Color(0xFFF7F4EC)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BİL BAKALIM", color = Color(0xFFF0D59A), fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text("Doğru cevaba en yakın cevap kazanır.", color = Color(0xFFB6C0CA), fontSize = 11.sp)
                }
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreBox("SEN", playerScore, Color(0xFF76A7C7), Modifier.weight(1f))
                ScoreBox("BOT", botScore, Color(0xFFB66A68), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))

            Surface(shape = RoundedCornerShape(100.dp), color = if (seconds <= 5) Color(0xFFB66A68) else Color(0xFFD8AC5C)) {
                Text("$seconds", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Color(0xFF071525), fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10263A)),
                border = BorderStroke(1.dp, Color(0xFF2A4962)),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${q.category.uppercase()} • SORU $questionNo", color = Color(0xFFD8AC5C), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(q.question, color = Color(0xFFF7F4EC), fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 31.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))

                    if (phase == BilPhase.ANSWER) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { raw -> input = raw.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(18) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                            placeholder = { Text("Tahminin", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { input.replace(',', '.').toDoubleOrNull()?.let(::finishRound) }),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD8AC5C),
                                unfocusedBorderColor = Color(0xFF496174),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { finishRound(input.replace(',', '.').toDoubleOrNull()) },
                            enabled = input.replace(',', '.').toDoubleOrNull() != null,
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8AC5C), contentColor = Color(0xFF2A1E0D)),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text("CEVABI KİLİTLE", fontWeight = FontWeight.Black, fontSize = 16.sp) }
                    } else {
                        AnswerLine("Senin cevabın", playerAnswer?.let(::prettyNumber) ?: "Cevap yok", phase == BilPhase.RESULT && playerWon == true)
                        Spacer(Modifier.height(10.dp))
                        AnswerLine("Bot cevabı", botAnswer?.let(::prettyNumber) ?: "Cevap bekleniyor…", phase == BilPhase.RESULT && playerWon == false)
                    }
                }
            }

            if (phase == BilPhase.LOCKED) {
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(color = Color(0xFFD8AC5C))
                Spacer(Modifier.height(8.dp))
                Text("İki cevap kilitleniyor…", color = Color(0xFFB6C0CA), fontSize = 12.sp)
            }

            if (phase == BilPhase.RESULT) {
                Spacer(Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2A22)),
                    border = BorderStroke(1.dp, Color(0xFF39D875)),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(38.dp))
                        Text("DOĞRU CEVAP", color = Color(0xFFB6C0CA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(q.displayAnswer, color = Color.White, fontWeight = FontWeight.Black, fontSize = 34.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        if (playerWon == true) {
                            Text("KAZANDIN!", color = Color(0xFF39D875), fontWeight = FontWeight.Black, fontSize = 30.sp)
                            Text("Doğru cevap • +10 puan", color = Color(0xFF39D875), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Text("YANLIŞ CEVAP", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Black, fontSize = 28.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = ::nextQuestion, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8AC5C), contentColor = Color(0xFF2A1E0D))) {
                            Text("SONRAKİ SORU", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBox(label: String, score: Int, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = Color(0xFF10263A), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, accent.copy(alpha = .65f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFFB6C0CA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            Text(label, color = Color(0xFFB6C0CA), fontSize = 11.sp)
            Text(value, color = if (winner) Color(0xFF39D875) else Color.White, fontWeight = FontWeight.Black, fontSize = if (winner) 31.sp else 26.sp, textAlign = TextAlign.Center)
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
