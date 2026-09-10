package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingStep(
    val titleTr: String,
    val titleEn: String,
    val bodyTr: String,
    val bodyEn: String,
    val exampleTr: String,
    val exampleEn: String,
)

private val onboardingSteps = listOf(
    OnboardingStep(
        titleTr = "1 • SON HARF KURALI",
        titleEn = "1 • LAST LETTER RULE",
        bodyTr = "Rakibin kelimesinin son harfiyle kendi kelimeni başlat.",
        bodyEn = "Start your word with the final letter of your opponent's word.",
        exampleTr = "KİTAP → PAMUK → KALEM",
        exampleEn = "TABLE → EAGLE → EARTH",
    ),
    OnboardingStep(
        titleTr = "2 • TAŞ PUANLARI VE COMBO",
        titleEn = "2 • LETTER VALUE AND COMBO",
        bodyTr = "Zor harfler daha değerlidir. Hızlı ve arka arkaya doğru kelimeler seri oluşturur.",
        bodyEn = "Hard letters are worth more. Fast consecutive valid words build a streak.",
        exampleTr = "J • Ğ • Z = yüksek değer",
        exampleEn = "Q • X • Z = high value",
    ),
    OnboardingStep(
        titleTr = "3 • 20 SANİYE BASKISI",
        titleEn = "3 • 20 SECOND PRESSURE",
        bodyTr = "Her hamle için 20 saniyen var. Süre biterse sıra rakibe geçer. Önce doğru kelimeyi bul, sonra hızlan.",
        bodyEn = "You have 20 seconds per turn. When time expires, play passes on. Find the right word first, then build speed.",
        exampleTr = "Odaklan • Kelimeyi bul • Gönder",
        exampleEn = "Focus • Find the word • Send",
    ),
)

@Composable
internal fun FirstRunOnboarding(onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val current = onboardingSteps[step]
    val english = SonHarfUiState.language.lowercase() == "en"

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SonHarfBrandLogo(size = 72.dp)
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (english) "LEARN IN 30 SECONDS" else "30 SANİYEDE ÖĞREN",
                color = MainUi.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / onboardingSteps.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = MainUi.Blue,
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
                shadowElevation = 2.dp,
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (english) current.titleEn else current.titleTr,
                        color = MainUi.Blue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (english) current.bodyEn else current.bodyTr,
                        color = MainUi.Text,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Center,
                    )
                    Surface(shape = RoundedCornerShape(14.dp), color = MainUi.BlueSoft) {
                        Text(
                            text = if (english) current.exampleEn else current.exampleTr,
                            color = MainUi.Blue,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) {
                    Button(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MainUi.Surface, contentColor = MainUi.Text),
                        border = BorderStroke(1.dp, MainUi.Border),
                    ) {
                        Text(if (english) "BACK" else "GERİ", fontWeight = FontWeight.Black)
                    }
                }
                Button(
                    onClick = { if (step == onboardingSteps.lastIndex) onComplete() else step++ },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                ) {
                    Text(
                        if (step == onboardingSteps.lastIndex) {
                            if (english) "START" else "BAŞLA"
                        } else {
                            if (english) "NEXT" else "İLERİ"
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
