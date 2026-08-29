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
import androidx.compose.ui.graphics.Color
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

private object SemanticPathEngine {
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

        connect("kitap", "hikâye")
        connect("kitap", "bilgi")
        connect("hikâye", "karakter")
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

@Composable
internal fun SemanticPathGameScreen(onExit: () -> Unit) {
    val tr = remember { Locale.forLanguageTag("tr-TR") }
    val puzzle = remember { SemanticPathEngine.dailyPuzzle() }
    var path by remember(puzzle.id) { mutableStateOf(listOf(puzzle.start)) }
    var input by remember(puzzle.id) { mutableStateOf("") }
    var message by remember(puzzle.id) { mutableStateOf("Bağlantılı bir kelime yaz.") }
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
        message = "Bağlantılı bir kelime yaz."
        completed = false
        hintVisible = false
    }

    fun submit() {
        val next = input.trim()
        when {
            next.isBlank() -> message = "Bir kelime yaz."
            SemanticPathEngine.normalize(next) in normalizedUsed -> message = "Bu kelimeyi zaten kullandın."
            !SemanticPathEngine.canConnect(current, next) -> message = "Bağlantı yeterince güçlü değil."
            else -> {
                val display = next.uppercase(tr)
                path = path + display
                input = ""
                hintVisible = false
                if (SemanticPathEngine.normalize(next) == SemanticPathEngine.normalize(puzzle.target)) {
                    completed = true
                    message = "Hedefe ulaştın!"
                } else {
                    message = "Güçlü bağlantı. Devam et."
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri")
            }
            Column(Modifier.weight(1f)) {
                Text("KELİME YOLU", fontSize = 22.sp, fontWeight = FontWeight.Black, color = PortalText)
                Text("Bağlantılı kelimelerle hedefe ulaş.", fontSize = 11.sp, color = PortalMuted)
            }
            IconButton(onClick = { reset() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Yeniden başlat")
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFEAF3FF),
            border = BorderStroke(1.dp, PortalBlue.copy(alpha = .20f)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("BAŞLANGIÇ", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PortalMuted)
                Text(puzzle.start, fontSize = 26.sp, fontWeight = FontWeight.Black, color = PortalText)
                Text("↓", fontSize = 22.sp, color = PortalBlue)
                Text("HEDEF", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PortalMuted)
                Text(puzzle.target, fontSize = 30.sp, fontWeight = FontWeight.Black, color = PortalBlue)
                Text("Altın hedef: " + puzzle.goldSteps + " hamle", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PortalGold)
            }
        }

        Text("YOLUN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PortalMuted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            itemsIndexed(path) { index, word ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (index == path.lastIndex) PortalBlue.copy(alpha = .12f) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (index == path.lastIndex) PortalBlue.copy(alpha = .30f) else Color(0xFFDDE5EE)
                        ),
                    ) {
                        Text(
                            word.uppercase(tr),
                            Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            color = PortalText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                    if (index < path.lastIndex) Text(" → ", color = PortalMuted, fontSize = 12.sp)
                }
            }
        }

        if (!completed) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(24) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(current + " ile bağlantılı kelime") },
                trailingIcon = {
                    IconButton(onClick = { submit() }) {
                        Icon(Icons.Rounded.Send, contentDescription = "Gönder")
                    }
                },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    message,
                    Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = if (message.startsWith("Güçlü")) PortalGreen else PortalMuted,
                )
                TextButton(onClick = { hintVisible = !hintVisible }) {
                    Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("İpucu")
                }
            }

            if (hintVisible) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF7E8)) {
                    Text(
                        if (suggestions.isEmpty()) "Başka bir bağlantı dene."
                        else "Düşünebileceğin yönler: " + suggestions.joinToString(" • ") { it.uppercase(tr) },
                        Modifier.fillMaxWidth().padding(12.dp),
                        color = PortalText,
                        fontSize = 11.sp,
                    )
                }
            }
        } else {
            val score = SemanticPathEngine.score(steps, puzzle.goldSteps)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFEAFBF0),
                border = BorderStroke(1.dp, PortalGreen.copy(alpha = .28f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("YOL TAMAMLANDI", color = PortalGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(steps.toString() + " HAMLE", color = PortalText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(score.toString() + " PUAN", color = PortalBlue, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (steps <= puzzle.goldSteps)
                            "ALTIN YOL! Bu rotayı arkadaşlarına meydan okuma olarak gönderebilirsin."
                        else
                            "Altın yol " + puzzle.goldSteps + " hamle. Daha kısa bir rota bulabilir misin?",
                        color = PortalMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { reset() }) { Text("TEKRAR DENE") }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Hamle", color = PortalMuted, fontSize = 10.sp)
                Text(steps.toString(), color = PortalText, fontWeight = FontWeight.Black)
                Text("Altın hedef", color = PortalMuted, fontSize = 10.sp)
                Text(puzzle.goldSteps.toString(), color = PortalGold, fontWeight = FontWeight.Black)
            }
        }
    }
}
