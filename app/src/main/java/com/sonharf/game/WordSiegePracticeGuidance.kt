package com.sonharf.game

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object WordSiegePracticeTutorialPrefs {
    private const val PREFS = "word_siege_practice_tutorial"
    private const val COMPLETED_V1 = "completed_v1"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(COMPLETED_V1, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(COMPLETED_V1, true)
            .apply()
    }
}

internal fun wordSiegePracticeZoneExplanation(code: String, turkish: Boolean): String = when (code) {
    "2H" -> if (turkish) {
        "GÖZ • Bu hücreye koyduğun harfin puanını 2 katına çıkarır."
    } else {
        "WATCH • Doubles the letter score placed on this cell."
    }
    "3H" -> if (turkish) {
        "KRİT • Bu hücreye koyduğun harfin puanını 3 katına çıkarır."
    } else {
        "CRITICAL • Triples the letter score placed on this cell."
    }
    "2K" -> if (turkish) {
        "KALE • Bu hücreden geçen kelimenin puanını 2 katına çıkarır."
    } else {
        "FORT • Doubles the score of the word using this cell."
    }
    "3K" -> if (turkish) {
        "KUŞ • Kuşatma Noktasıdır; bu hücreden geçen kelimenin puanını 3 katına çıkarır."
    } else {
        "SIEGE • Triples the score of the word using this cell."
    }
    WordSiegeBoardSpec.CenterBonus -> if (turkish) {
        "TAÇ • İlk kelime bu bölgeden geçmelidir. Buradan geçen kelimenin puanını 4 katına çıkarır."
    } else {
        "CROWN • The first word must cross this zone. It multiplies that word score by 4."
    }
    WordSiegeBoardSpec.StarBonus -> if (turkish) {
        "ÖDÜL • Bu sürpriz bölge hamlene +${WordSiegeBoardSpec.StarBonusPoints} puan ekler."
    } else {
        "REWARD • This surprise zone adds +${WordSiegeBoardSpec.StarBonusPoints} points to the move."
    }
    else -> if (turkish) "Stratejik bölge." else "Strategic zone."
}

@Composable
internal fun WordSiegePracticeZoneInfoDialog(
    code: String,
    onDismiss: () -> Unit,
) {
    val turkish = !SonHarfUiState.isEnglish
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                WordSiegeBoardSpec.bonusLongName(code, turkish),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Text(
                wordSiegePracticeZoneExplanation(code, turkish),
                color = MainUi.Text,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (turkish) "ANLADIM" else "GOT IT", fontWeight = FontWeight.Black)
            }
        },
    )
}

@Composable
internal fun WordSiegePracticeTutorialCard(
    step: Int,
    compact: Boolean,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    val turkish = !SonHarfUiState.isEnglish
    val title = when (step) {
        0 -> if (turkish) "KELİME TAHTI NASIL OYNANIR?" else "HOW TO PLAY WORD THRONE"
        1 -> if (turkish) "1/4 • BİR HARF SEÇ" else "1/4 • PICK A TILE"
        2 -> if (turkish) "2/4 • TAHTAYA YERLEŞTİR" else "2/4 • PLACE IT"
        3 -> if (turkish) "3/4 • KELİMEYİ TAMAMLA" else "3/4 • COMPLETE THE WORD"
        else -> if (turkish) "4/4 • ALANI KONTROL ET" else "4/4 • CONTROL TERRITORY"
    }
    val body = when (step) {
        0 -> if (turkish) {
            "Harflerle kelime kur. Kelimenin geçtiği hücreleri ele geçir. Amaç hem kelime puanı toplamak hem haritada daha fazla alan kontrol etmek."
        } else {
            "Build words with tiles. Capture the cells your word crosses. Win through word score and map control together."
        }
        1 -> if (turkish) {
            "Alttaki raftan bir harfe dokun. Seçilen harf belirginleşecek."
        } else {
            "Tap a tile in the rack below. The selected tile will highlight."
        }
        2 -> if (turkish) {
            "Şimdi boş bir hücreye dokun. İlk kelimen TAÇ Bölgesi'nden geçmeli."
        } else {
            "Now tap an empty cell. Your first word must cross the CROWN Zone."
        }
        3 -> if (turkish) {
            "Harfleri aynı satır veya sütunda kelime olacak şekilde tamamla; sonra HAMLEYİ ONAYLA."
        } else {
            "Complete a word in one row or column, then tap CONFIRM MOVE."
        }
        else -> if (turkish) {
            "Yeşil hücreler senin bölgen. Her sahip olduğun hücre 2 bölge puanı verir; rakip bu hücreleri geri alabilir. Kelime puanın kalıcıdır. Bölge renklerine dokunarak özel alanları öğrenebilirsin."
        } else {
            "Green cells are your territory. Each owned cell gives 2 territory points and can be captured back; your word score stays permanent. Tap zone labels to learn their effects."
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8F4E8).copy(alpha = .98f),
        border = BorderStroke(1.5.dp, Color(0xFF567A64)),
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                title,
                color = Color(0xFF17372C),
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                body,
                color = Color(0xFF3F554A),
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = if (compact) 12.sp else 14.sp,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkip, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(if (turkish) "ATLA" else "SKIP", fontSize = 8.sp, color = MainUi.Muted, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                when (step) {
                    0 -> Button(onClick = onStart, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                        Text(if (turkish) "BAŞLA" else "START", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    4 -> Button(onClick = onFinish, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                        Text(if (turkish) "ANLADIM" else "GOT IT", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    else -> Text(
                        if (turkish) "Ekrandaki adımı yap" else "Complete the step on screen",
                        color = MainUi.Muted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
internal fun WordSiegePracticeStatusBar(
    message: String,
    compact: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF4F1E8),
        border = BorderStroke(1.dp, Color(0xFFB6C4BB)),
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
            color = Color(0xFF29483B),
            fontSize = if (compact) 8.sp else 9.sp,
            lineHeight = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
