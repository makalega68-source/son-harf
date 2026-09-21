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
import androidx.compose.ui.window.Dialog

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
        "HARF ×2 • Bu hücreye koyduğun harfin puanını 2 katına çıkarır."
    } else {
        "LETTER ×2 • Doubles the letter score placed on this cell."
    }
    "3H" -> if (turkish) {
        "HARF ×3 • Bu hücreye koyduğun harfin puanını 3 katına çıkarır."
    } else {
        "LETTER ×3 • Triples the letter score placed on this cell."
    }
    "2K" -> if (turkish) {
        "KELİME ×2 • Bu hücreden geçen kelimenin puanını 2 katına çıkarır."
    } else {
        "WORD ×2 • Doubles the score of the word using this cell."
    }
    "3K" -> if (turkish) {
        "KELİME ×3 • bu hücreden geçen kelimenin puanını 3 katına çıkarır."
    } else {
        "WORD ×3 • Triples the score of the word using this cell."
    }
    WordSiegeBoardSpec.CenterBonus -> if (turkish) {
        "BAŞLANGIÇ • İlk kelime bu bölgeden geçmelidir. Buradan geçen kelimenin puanını 4 katına çıkarır."
    } else {
        "START • The first word must cross this zone. It multiplies that word score by 4."
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
    Dialog(onDismissRequest = onDismiss) {
        PurchasedPanel(
            modifier = Modifier.fillMaxWidth(),
            asset = PurchasedUiAsset.PANEL_MEDIUM,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PurchasedSectionHeader(
                    title = WordSiegeBoardSpec.bonusLongName(code, turkish),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    wordSiegePracticeZoneExplanation(code, turkish),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF654A3D),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                PurchasedButton(
                    text = if (turkish) "ANLADIM" else "GOT IT",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.PRIMARY,
                )
            }
        }
    }
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
        0 -> if (turkish) "KELİME KUŞATMASI NASIL OYNANIR?" else "HOW TO PLAY WORD SIEGE"
        1 -> if (turkish) "1/4 • BİR HARF SEÇ" else "1/4 • PICK A TILE"
        2 -> if (turkish) "2/4 • TAHTAYA YERLEŞTİR" else "2/4 • PLACE IT"
        3 -> if (turkish) "3/4 • KELİMEYİ TAMAMLA" else "3/4 • COMPLETE THE WORD"
        else -> if (turkish) "4/4 • SIRA RAKİBİNDE" else "4/4 • YOUR RIVAL PLAYS"
    }
    val body = when (step) {
        0 -> if (turkish) {
            "Harf seç, tahtaya yerleştir ve kelimeni onayla. Sonraki kelimeler tahtadaki harflere yatay veya dikey bağlanır. En yüksek toplam puan kazanır."
        } else {
            "Pick tiles, place your word and confirm. Connect later words horizontally or vertically to letters on the board. Highest total score wins."
        }
        1 -> if (turkish) {
            "Alttaki raftan bir harfe dokun. Seçilen harf belirginleşecek."
        } else {
            "Tap a tile in the rack below. The selected tile will highlight."
        }
        2 -> if (turkish) {
            "Şimdi boş bir hücreye dokun. İlk kelimen merkezden geçmeli."
        } else {
            "Now tap an empty cell. Your first word must cross the center."
        }
        3 -> if (turkish) {
            "Harfleri aynı satır veya sütunda kelime olacak şekilde tamamla; sonra HAMLEYİ ONAYLA."
        } else {
            "Complete a word in one row or column, then tap CONFIRM MOVE."
        }
        else -> if (turkish) {
            "Harf bonusu yalnız harfi, kelime bonusu tüm kelimeyi çarpar. +25 ödülü hamlene eklenir. Skordaki bölge puanı mevcut maç kuralıdır: sahip olduğun hücre başına 2 puan; kelime puanı kalıcıdır."
        } else {
            "Letter bonuses multiply one letter; word bonuses multiply the word. +25 adds to the move. Existing matches also award 2 points per owned cell; word points are permanent."
        }
    }

    PurchasedPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = if (compact) 10.dp else 12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PurchasedSectionHeader(
                title = title,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                body,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF654A3D),
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PurchasedButton(
                    text = if (turkish) "ATLA" else "SKIP",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    style = PurchasedButtonStyle.SECONDARY,
                )
                when (step) {
                    0 -> PurchasedButton(
                        text = if (turkish) "BAŞLA" else "START",
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        style = PurchasedButtonStyle.PRIMARY,
                    )
                    4 -> PurchasedButton(
                        text = if (turkish) "ANLADIM" else "GOT IT",
                        onClick = onFinish,
                        modifier = Modifier.weight(1f),
                        style = PurchasedButtonStyle.PRIMARY,
                    )
                    else -> PurchasedPanel(
                        modifier = Modifier.weight(1f),
                        asset = PurchasedUiAsset.PANEL_SMALL,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (turkish) "Ekrandaki adımı yap" else "Complete the step on screen",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF765746),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
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
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF654A3D),
            fontSize = if (compact) 8.sp else 9.sp,
            lineHeight = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
