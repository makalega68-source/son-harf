package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal enum class FirstPlayerTutorialKind { SON_HARF, WORD_SIEGE }

internal enum class TutorialVisual {
    KALEM,
    MASA,
    TIMER,
    READY_SON_HARF,
    RACK_TO_BOARD,
    TAM,
    INVALID_EXTENSION,
    VALID_EXTENSION,
    CROSS_WORDS,
    CAPTURE,
    READY_WORD_SIEGE,
}

internal data class FirstPlayerTutorialStep(
    val titleTr: String,
    val titleEn: String,
    val bodyTr: String,
    val bodyEn: String,
    val visual: TutorialVisual,
)

internal val sonHarfTutorialSteps = listOf(
    FirstPlayerTutorialStep(
        titleTr = "SON HARFİ YAKALA",
        titleEn = "CATCH THE LAST LETTER",
        bodyTr = "Rakibinin kelimesinin son harfiyle başlayan yeni bir kelime yaz.",
        bodyEn = "Enter a new word beginning with the last letter of your rival's word.",
        visual = TutorialVisual.KALEM,
    ),
    FirstPlayerTutorialStep(
        titleTr = "ZİNCİRİ SÜRDÜR",
        titleEn = "KEEP THE CHAIN GOING",
        bodyTr = "Harika! Şimdi rakibin A harfiyle devam edecek.",
        bodyEn = "Great! Now your rival must continue with A.",
        visual = TutorialVisual.MASA,
    ),
    FirstPlayerTutorialStep(
        titleTr = "SÜREYİ KAÇIRMA",
        titleEn = "BEAT THE CLOCK",
        bodyTr = "10 saniyede kelimeni yaz. Süre dolarsa −1 puan. Bonus düellosu ayrı turda gelir.",
        bodyEn = "Enter your word in 10 seconds. Timeout costs −1 point. Bonus duels appear as separate rounds.",
        visual = TutorialVisual.TIMER,
    ),
    FirstPlayerTutorialStep(
        titleTr = "HAZIRSIN!",
        titleEn = "YOU'RE READY!",
        bodyTr = "Kelimeyi Sürdür, Rakibini Geç",
        bodyEn = "Keep the word going, beat your rival",
        visual = TutorialVisual.READY_SON_HARF,
    ),
)

internal val wordSiegeTutorialSteps = listOf(
    FirstPlayerTutorialStep(
        titleTr = "TAŞI TAHTAYA KOY",
        titleEn = "PLACE TILES ON THE BOARD",
        bodyTr = "Elindeki harfleri doğrudan tahtaya yerleştir.",
        bodyEn = "Place the letters from your rack directly on the board.",
        visual = TutorialVisual.RACK_TO_BOARD,
    ),
    FirstPlayerTutorialStep(
        titleTr = "TAHTADAKİ HARFİ KULLAN",
        titleEn = "USE BOARD LETTERS",
        bodyTr = "Tahtadaki harfleri yeni kelimelerinde kullanabilirsin.",
        bodyEn = "You can use letters already on the board in your new words.",
        visual = TutorialVisual.TAM,
    ),
    FirstPlayerTutorialStep(
        titleTr = "ANLAMI BOZMA",
        titleEn = "DON'T BREAK WORDS",
        bodyTr = "Yeni hamlen mevcut kelimeleri anlamsız hale getiremez.",
        bodyEn = "Your move cannot turn existing words into invalid ones.",
        visual = TutorialVisual.INVALID_EXTENSION,
    ),
    FirstPlayerTutorialStep(
        titleTr = "GEÇERLİ UZATMA",
        titleEn = "VALID EXTENSION",
        bodyTr = "Geçerli bir harfle mevcut kelimeyi büyütebilirsin.",
        bodyEn = "You can extend an existing word when the result stays valid.",
        visual = TutorialVisual.VALID_EXTENSION,
    ),
    FirstPlayerTutorialStep(
        titleTr = "ÇAPRAZLARI DA KONTROL ET",
        titleEn = "CHECK CROSS WORDS TOO",
        bodyTr = "Hamlede oluşan bütün kelimeler geçerli olmak zorunda.",
        bodyEn = "Every word formed by a move must be valid.",
        visual = TutorialVisual.CROSS_WORDS,
    ),
    FirstPlayerTutorialStep(
        titleTr = "ALANI KUŞAT",
        titleEn = "CAPTURE TERRITORY",
        bodyTr = "Rakibin harfini kelimene kattığında o alanı ele geçirebilirsin.",
        bodyEn = "Use a rival tile in your word to capture that territory.",
        visual = TutorialVisual.CAPTURE,
    ),
    FirstPlayerTutorialStep(
        titleTr = "HAZIRSIN",
        titleEn = "YOU'RE READY",
        bodyTr = "Hazırsın. Kuşatmayı başlat!",
        bodyEn = "You're ready. Start the siege!",
        visual = TutorialVisual.READY_WORD_SIEGE,
    ),
)

internal fun tutorialSteps(kind: FirstPlayerTutorialKind): List<FirstPlayerTutorialStep> = when (kind) {
    FirstPlayerTutorialKind.SON_HARF -> sonHarfTutorialSteps
    FirstPlayerTutorialKind.WORD_SIEGE -> wordSiegeTutorialSteps
}

internal fun shouldAutoShowTutorial(completed: Boolean): Boolean = !completed

@Composable
internal fun FirstPlayerTutorial(
    kind: FirstPlayerTutorialKind,
    onSkip: () -> Unit,
    onDone: () -> Unit,
) {
    val steps = remember(kind) { tutorialSteps(kind) }
    var index by remember(kind) { mutableIntStateOf(0) }
    val step = steps[index]
    val last = index == steps.lastIndex
    val accent = if (kind == FirstPlayerTutorialKind.SON_HARF) MainUi.Blue else MainUi.Purple

    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .46f))
                .padding(horizontal = 18.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
                color = MainUi.Surface,
                shadowElevation = 10.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (kind == FirstPlayerTutorialKind.SON_HARF) "SON HARF" else sh("KELİME KUŞATMASI", "WORD SIEGE"),
                            color = accent,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onSkip) {
                            Text(sh("ATLA", "SKIP"), color = MainUi.Muted, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(steps.size) { dot ->
                            Box(
                                Modifier
                                    .size(if (dot == index) 9.dp else 6.dp)
                                    .background(if (dot <= index) accent else MainUi.Border, CircleShape)
                            )
                        }
                    }

                    TutorialVisualCard(
                        visual = step.visual,
                        accent = accent,
                        onTap = {
                            if (last) onDone() else index += 1
                        },
                    )

                    Text(
                        if (SonHarfUiState.isEnglish) step.titleEn else step.titleTr,
                        color = MainUi.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (SonHarfUiState.isEnglish) step.bodyEn else step.bodyTr,
                        color = MainUi.Muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (index > 0) {
                            OutlinedButton(
                                onClick = { index -= 1 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text(sh("GERİ", "BACK"), fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { if (last) onDone() else index += 1 },
                            modifier = Modifier.weight(1.4f).height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                        ) {
                            Text(
                                if (last) sh("BAŞLA", "START") else sh("DEVAM", "NEXT"),
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialVisualCard(
    visual: TutorialVisual,
    accent: Color,
    onTap: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable(onClick = onTap),
        color = accent.copy(alpha = .07f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .22f)),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            when (visual) {
                TutorialVisual.KALEM -> TutorialWord("KALEM", 4, accent)
                TutorialVisual.MASA -> TutorialWord("MASA", 3, MainUi.Green)
                TutorialVisual.TIMER -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Timer, null, tint = MainUi.Red, modifier = Modifier.size(48.dp))
                    Text("10 sn", color = MainUi.Text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("−1  •  ★ BONUS", color = MainUi.Gold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                TutorialVisual.READY_SON_HARF -> ReadyVisual("M → MASA → A", accent)
                TutorialVisual.RACK_TO_BOARD -> RackToBoardVisual(accent)
                TutorialVisual.TAM -> WordEquationVisual(listOf("T", "A", "M"), middleExisting = true, result = "TAM", valid = true, accent = accent)
                TutorialVisual.INVALID_EXTENSION -> ExtensionVisual("MAKALE", "B", "MAKALEB", valid = false, accent = accent)
                TutorialVisual.VALID_EXTENSION -> ExtensionVisual("ARA", "Ç", "ARAÇ", valid = true, accent = accent)
                TutorialVisual.CROSS_WORDS -> CrossWordVisual(accent)
                TutorialVisual.CAPTURE -> CaptureVisual(accent)
                TutorialVisual.READY_WORD_SIEGE -> ReadyVisual("▦  ✓", accent)
            }
            Text(
                sh("Görsele dokun →", "Tap the visual →"),
                color = MainUi.Muted,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun TutorialWord(word: String, highlight: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        word.forEachIndexed { i, c ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (i == highlight) accent else Color.White,
                border = BorderStroke(1.5.dp, if (i == highlight) accent else MainUi.Border),
                shadowElevation = if (i == highlight) 4.dp else 0.dp,
            ) {
                Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                    Text(c.toString(), color = if (i == highlight) Color.White else MainUi.Text, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ReadyVisual(text: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Icon(Icons.Rounded.CheckCircle, null, tint = MainUi.Green, modifier = Modifier.size(48.dp))
        Text(text, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RackToBoardVisual(accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("T", "A", "M", "Ç").forEach { TutorialTile(it, Color.White, accent) }
        }
        Text("↓", color = accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i -> TutorialTile(if (i in 1..3) listOf("T", "A", "M")[i - 1] else "", MainUi.SurfaceSoft, MainUi.Border) }
        }
    }
}

@Composable
private fun WordEquationVisual(parts: List<String>, middleExisting: Boolean, result: String, valid: Boolean, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            parts.forEachIndexed { index, s ->
                TutorialTile(
                    s,
                    if (middleExisting && index == 1) MainUi.Purple.copy(alpha = .16f) else Color.White,
                    if (middleExisting && index == 1) MainUi.Purple else accent,
                )
            }
        }
        Text("=  $result  ${if (valid) "✓" else "✕"}", color = if (valid) MainUi.Green else MainUi.Red, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ExtensionVisual(base: String, added: String, result: String, valid: Boolean, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(base, color = MainUi.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("+", color = MainUi.Muted, fontSize = 20.sp)
            TutorialTile(added, Color.White, accent)
        }
        Text("$result  ${if (valid) "✅" else "❌"}", color = if (valid) MainUi.Green else MainUi.Red, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CrossWordVisual(accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TutorialTile("T", Color.White, accent)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TutorialTile("T", Color.White, accent)
            TutorialTile("A", MainUi.Purple.copy(alpha = .16f), MainUi.Purple)
            TutorialTile("M", Color.White, accent)
        }
        TutorialTile("", MainUi.SurfaceSoft, MainUi.Border)
        Text("TAM ✓   •   TA ✓", color = MainUi.Green, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CaptureVisual(accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            TutorialTile("K", Color.White, accent)
            TutorialTile("A", MainUi.Purple.copy(alpha = .22f), MainUi.Purple)
            TutorialTile("L", Color.White, accent)
        }
        Text("↓", color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            TutorialTile("K", accent.copy(alpha = .16f), accent)
            TutorialTile("A", accent.copy(alpha = .16f), accent)
            TutorialTile("L", accent.copy(alpha = .16f), accent)
        }
        Text(sh("Rakip alanı → senin alanın", "Rival territory → your territory"), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TutorialTile(text: String, background: Color, border: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(1.2.dp, border),
    ) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Text(text, color = MainUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun TutorialHelpChooser(
    onDismiss: () -> Unit,
    onSonHarf: () -> Unit,
    onWordSiege: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.HelpOutline, null, tint = MainUi.Blue) },
        title = { Text(sh("Nasıl Oynanır?", "How to Play?"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSonHarf, modifier = Modifier.fillMaxWidth()) { Text("SON HARF") }
                OutlinedButton(onClick = onWordSiege, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.GridOn, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sh("KELİME KUŞATMASI", "WORD SIEGE"))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(sh("KAPAT", "CLOSE"))
            }
        },
    )
}
