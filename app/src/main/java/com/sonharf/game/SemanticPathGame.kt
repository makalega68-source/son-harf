package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.util.Locale

internal data class SemanticPathPuzzle(
    val id: String,
    val start: String,
    val target: String,
    val goldSteps: Int,
)

internal object SemanticPathEngine {
    private val tr = Locale.forLanguageTag("tr-TR")

    private val links: Map<String, Set<String>> = buildMap {
        fun connect(a: String, b: String) {
            val left = a.lowercase(tr)
            val right = b.lowercase(tr)
            put(left, getOrDefault(left, emptySet()) + right)
            put(right, getOrDefault(right, emptySet()) + left)
        }

        connect("tohum", "tarla")
        connect("tohum", "bitki")
        connect("bitki", "tarım")
        connect("bitki", "park")
        connect("tarla", "tarım")
        connect("tarla", "köy")
        connect("tarım", "köy")
        connect("köy", "şehir")
        connect("köy", "kasaba")
        connect("kasaba", "şehir")
        connect("park", "şehir")

        connect("kahve", "sabah")
        connect("kahve", "enerji")
        connect("enerji", "uyanıklık")
        connect("uyanıklık", "uyku")
        connect("sabah", "uyanmak")
        connect("uyanmak", "uyku")

        connect("yağmur", "kış")
        connect("yağmur", "bulut")
        connect("bulut", "gökyüzü")
        connect("gökyüzü", "güneş")
        connect("kış", "yaz")
        connect("güneş", "yaz")
        connect("yaz", "tatil")

        connect("kitap", "hikaye")
        connect("kitap", "bilgi")
        connect("hikaye", "karakter")
        connect("karakter", "insan")
        connect("bilgi", "insan")
        connect("insan", "dostluk")
    }

    val puzzles = listOf(
        SemanticPathPuzzle("seed-city", "TOHUM", "ŞEHİR", 3),
        SemanticPathPuzzle("coffee-sleep", "KAHVE", "UYKU", 3),
        SemanticPathPuzzle("rain-holiday", "YAĞMUR", "TATİL", 3),
        SemanticPathPuzzle("book-friendship", "KİTAP", "DOSTLUK", 3),
    )

    fun dailyPuzzle(): SemanticPathPuzzle =
        puzzles[(LocalDate.now().dayOfYear - 1).mod(puzzles.size)]

    fun normalize(word: String): String = word.trim().lowercase(tr)

    fun canConnect(from: String, to: String): Boolean =
        normalize(to) in links[normalize(from)].orEmpty()

    fun suggestions(from: String, used: Set<String>): List<String> =
        links[normalize(from)].orEmpty()
            .filterNot { it in used }
            .sorted()
            .take(3)

    fun score(steps: Int, goldSteps: Int): Int {
        val efficiencyBonus = ((goldSteps - steps).coerceAtLeast(0)) * 120
        return (1000 - steps * 110 + efficiencyBonus).coerceAtLeast(100)
    }
}

/**
 * Legacy semantic-path implementation kept for compatibility. The public product name is now
 * consistently HARF YOLU / LETTER PATH everywhere.
 */
@Composable
internal fun SemanticPathGameScreen(onExit: () -> Unit) {
    val tr = remember { Locale.forLanguageTag("tr-TR") }
    val puzzle = remember { SemanticPathEngine.dailyPuzzle() }
    var path by remember(puzzle.id) { mutableStateOf(listOf(puzzle.start)) }
    var input by remember(puzzle.id) { mutableStateOf("") }
    var message by remember(puzzle.id) { mutableStateOf(sh("Bağlantılı bir kelime yaz.", "Enter a connected word.")) }
    var completed by remember(puzzle.id) { mutableStateOf(false) }
    var hintVisible by remember(puzzle.id) { mutableStateOf(false) }

    val current = path.last()
    val steps = (path.size - 1).coerceAtLeast(0)
    val normalizedUsed = path.map { SemanticPathEngine.normalize(it) }.toSet()
    val suggestions = remember(current, normalizedUsed) {
        SemanticPathEngine.suggestions(current, normalizedUsed)
    }

    fun reset() {
        path = listOf(puzzle.start)
        input = ""
        message = sh("Bağlantılı bir kelime yaz.", "Enter a connected word.")
        completed = false
        hintVisible = false
    }

    fun submit() {
        val next = input.trim()
        when {
            next.isBlank() -> message = sh("Bir kelime yaz.", "Enter a word.")
            SemanticPathEngine.normalize(next) in normalizedUsed -> message = sh("Bu kelimeyi zaten kullandın.", "You already used this word.")
            !SemanticPathEngine.canConnect(current, next) -> message = sh("Bağlantı yeterince güçlü değil.", "The connection is not strong enough.")
            else -> {
                val display = next.uppercase(tr)
                path = path + display
                input = ""
                hintVisible = false
                if (SemanticPathEngine.normalize(next) == SemanticPathEngine.normalize(puzzle.target)) {
                    completed = true
                    message = sh("Hedefe ulaştın!", "Target reached!")
                } else {
                    message = sh("Güçlü bağlantı. Devam et.", "Strong connection. Keep going.")
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        SonHarfLeafBackdrop(Modifier.matchParentSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onExit,
                    shape = MainUiShape.Control,
                    color = MainUi.Surface,
                    border = BorderStroke(1.dp, MainUi.Border),
                    shadowElevation = 3.dp,
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = sh("Geri", "Back"),
                        tint = MainUi.Blue,
                        modifier = Modifier.padding(12.dp).size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("HARF YOLU", "LETTER PATH"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = MainUi.Text)
                    Text(sh("Bağlantılı kelimelerle hedefe ulaş.", "Reach the target through connected words."), fontSize = 11.sp, color = MainUi.Muted)
                }
                Surface(
                    onClick = { reset() },
                    shape = MainUiShape.Control,
                    color = MainUi.Surface,
                    border = BorderStroke(1.dp, MainUi.Border),
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = sh("Yeniden başlat", "Restart"),
                        tint = MainUi.Purple,
                        modifier = Modifier.padding(12.dp).size(20.dp),
                    )
                }
            }

            PremiumCard(modifier = Modifier.fillMaxWidth(), accent = MainUi.Blue) {
                horizontalAlignment = Alignment.CenterHorizontally
                Text(sh("BAŞLANGIÇ", "START"), fontSize = 9.sp, fontWeight = FontWeight.Black, color = MainUi.Muted)
                Text(puzzle.start, fontSize = 27.sp, fontWeight = FontWeight.Black, color = MainUi.Text)
                Text("↓", fontSize = 22.sp, color = MainUi.Cyan)
                Text(sh("HEDEF", "TARGET"), fontSize = 9.sp, fontWeight = FontWeight.Black, color = MainUi.Muted)
                Text(puzzle.target, fontSize = 31.sp, fontWeight = FontWeight.Black, color = MainUi.Purple)
                PremiumAccentPill(
                    text = sh("Altın hedef: ${puzzle.goldSteps} hamle", "Gold target: ${puzzle.goldSteps} moves"),
                    color = MainUi.Orange,
                )
            }

            Text(sh("YOLUN", "YOUR PATH"), fontSize = 10.sp, fontWeight = FontWeight.Black, color = MainUi.Muted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                itemsIndexed(path) { index, word ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (index == path.lastIndex) MainUi.BlueSoft else MainUi.Surface,
                            border = BorderStroke(1.dp, if (index == path.lastIndex) MainUi.Blue.copy(alpha = .30f) else MainUi.Border),
                            shadowElevation = if (index == path.lastIndex) 2.dp else 0.dp,
                        ) {
                            Text(
                                word.uppercase(tr),
                                Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                color = MainUi.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                        }
                        if (index < path.lastIndex) Text(" → ", color = MainUi.Muted, fontSize = 12.sp)
                    }
                }
            }

            if (!completed) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(24) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MainUiShape.Control,
                    label = { Text(sh("$current ile bağlantılı kelime", "Word connected to $current")) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MainUi.Blue,
                        unfocusedBorderColor = MainUi.Border,
                        focusedContainerColor = MainUi.Surface,
                        unfocusedContainerColor = MainUi.Surface,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { submit() }) {
                            Icon(Icons.Rounded.Send, contentDescription = sh("Gönder", "Submit"), tint = MainUi.Blue)
                        }
                    },
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message,
                        Modifier.weight(1f),
                        fontSize = 11.sp,
                        color = if (message.startsWith("Güçlü") || message.startsWith("Strong")) MainUi.Green else MainUi.Muted,
                    )
                    TextButton(onClick = { hintVisible = !hintVisible }) {
                        Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(17.dp), tint = MainUi.Orange)
                        Spacer(Modifier.width(4.dp))
                        Text(sh("İpucu", "Hint"), color = MainUi.Orange)
                    }
                }

                if (hintVisible) {
                    Surface(shape = MainUiShape.Control, color = SonHarfTheme.Sand) {
                        Text(
                            if (suggestions.isEmpty()) sh("Başka bir bağlantı dene.", "Try another connection.")
                            else sh("Düşünebileceğin yönler: ", "Possible directions: ") + suggestions.joinToString(" • ") { it.uppercase(tr) },
                            Modifier.fillMaxWidth().padding(12.dp),
                            color = MainUi.Text,
                            fontSize = 11.sp,
                        )
                    }
                }
            } else {
                val score = SemanticPathEngine.score(steps, puzzle.goldSteps)
                PremiumCard(modifier = Modifier.fillMaxWidth(), accent = MainUi.Cyan) {
                    horizontalAlignment = Alignment.CenterHorizontally
                    Text(sh("YOL TAMAMLANDI", "PATH COMPLETE"), color = MainUi.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(sh("$steps HAMLE", "$steps MOVES"), color = MainUi.Text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(sh("$score PUAN", "$score POINTS"), color = MainUi.Blue, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (steps <= puzzle.goldSteps)
                            sh("ALTIN YOL! Bu rotayı arkadaşlarına meydan okuma olarak gönderebilirsin.", "GOLD PATH! Share this route as a challenge.")
                        else
                            sh("Altın yol ${puzzle.goldSteps} hamle. Daha kısa bir rota bulabilir misin?", "Gold path is ${puzzle.goldSteps} moves. Can you find a shorter route?"),
                        color = MainUi.Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                    PremiumPrimaryButton(
                        text = sh("TEKRAR DENE", "TRY AGAIN"),
                        onClick = { reset() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                shape = MainUiShape.Control,
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
                shadowElevation = 2.dp,
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sh("Hamle", "Moves"), color = MainUi.Muted, fontSize = 10.sp)
                    Text(steps.toString(), color = MainUi.Text, fontWeight = FontWeight.Black)
                    Text(sh("Altın hedef", "Gold target"), color = MainUi.Muted, fontSize = 10.sp)
                    Text(puzzle.goldSteps.toString(), color = MainUi.Orange, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
