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
        "HARF GÜCÜ • Bu hücreye yerleşen taşın değeri iki kat sayılır."
    } else {
        "LETTER BOOST • The tile placed here counts at double value."
    }
    "3H" -> if (turkish) {
        "HARF GÜCÜ+ • Bu hücreye yerleşen taşın değeri üç kat sayılır."
    } else {
        "LETTER BOOST+ • The tile placed here counts at triple value."
    }
    "2K" -> if (turkish) {
        "KELİME AKIMI • Bu hücreye uzanan kelimenin toplamı iki kat yazılır."
    } else {
        "WORD SURGE • A word reaching this cell scores double in total."
    }
    "3K" -> if (turkish) {
        "KELİME AKIMI+ • Bu hücreye uzanan kelimenin toplamı üç kat yazılır."
    } else {
        "WORD SURGE+ • A word reaching this cell scores triple in total."
    }
    WordSiegeBoardSpec.CenterBonus -> if (turkish) {
        "MERKEZ • Açılış kelimesi merkezden geçer ve toplamı dört kat yazılır."
    } else {
        "STARTING SEAL • The opening word passes through the seal and scores four times its total."
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    wordSiegePracticeZoneExplanation(code, turkish),
                    color = MainUi.Text,
                )
                // All bonuses side by side, as they look on the board.
                Text(if (turkish) "Bonus yalnız hücreye ilk taş yerleştiğinde işler." else "A bonus applies only to the first tile placed in that cell.")
            }
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
        0 -> if (turkish) "KELİME TAHTI • KISA TUR" else "WORD THRONE • QUICK TOUR"
        1 -> if (turkish) "1/4 • TAŞINI AL" else "1/4 • TAKE A TILE"
        2 -> if (turkish) "2/4 • MERKEZDEN BAŞLA" else "2/4 • OPEN THE SEAL"
        3 -> if (turkish) "3/4 • KELİMEYİ KİLİTLE" else "3/4 • LOCK IN THE WORD"
        else -> if (turkish) "4/4 • BÖLGE VE BONUSLAR" else "4/4 • GROUND AND BONUSES"
    }
    val body = when (step) {
        0 -> if (turkish) {
            "Taşlarınla kelime kur, tahtada toprak kazan. Her yeni kelime tahtadaki bir harfe satır ya da sütun boyunca dokunmalı. Maç sonunda kelime ve bölge puanı toplamı yüksek olan kazanır."
        } else {
            "Build words with your tiles and win ground on the board. Every new word must touch a letter already on the board along a row or column. Highest word plus territory total wins."
        }
        1 -> if (turkish) {
            "Elinden bir harf seç."
        } else {
            "Select a tile from your rack."
        }
        2 -> if (turkish) {
            "Açılış kelimesini tahta merkezinden geçecek şekilde yerleştir."
        } else {
            "Now tap an empty cell. Your opening word must pass through the Starting Seal."
        }
        3 -> if (turkish) {
            "Taşları tek satırda ya da tek sütunda kelime olacak şekilde diz, sonra HAMLEYİ ONAYLA."
        } else {
            "Line the tiles up as a word in a single row or column, then tap CONFIRM MOVE."
        }
        else -> if (turkish) {
            "Ele geçirdiğin her küp sana +2 Bölge Puanı yazar; rakipten kopardığın küp onun Bölge Puanını 1 azaltır. Kelime puanı kalıcıdır. Bonuslar yalnız üstüne ilk taş konunca çalışır:"
        } else {
            "Each cube you take writes +2 Territory Points for you; a cube pulled from your rival lowers their Territory Points by 1. Word points are permanent. A bonus works only for the first tile placed on it:"
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
            // The last step shows every bonus as it looks on the board, with one short line each.
            if (step >= 4) Text(
                if (turkish) "Harf bonusu: harf değeri iki veya üç kat. Kelime bonusu: kelime toplamı iki veya üç kat. Açılış: kelime toplamı dört kat. Ödül bölgesi: 25 ek puan."
                else "Letter bonus: double or triple tile value. Word bonus: double or triple word total. Opening: quadruple word total. Reward cell: 25 extra points.",
                color = Color(0xFF3F554A), fontSize = if (compact) 9.sp else 10.sp,
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
