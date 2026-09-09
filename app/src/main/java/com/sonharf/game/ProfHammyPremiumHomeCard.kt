package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PremiumHammyCoachLine(
    val tr: String,
    val en: String,
)

private const val PREMIUM_HAMMY_LEAD_THRESHOLD = 3

private val premiumHammyTapLines = listOf(
    PremiumHammyCoachLine(
        tr = "Hazırsan kelimeleri konuşturalım! 🌟",
        en = "Ready? Let’s make the words come alive! 🌟",
    ),
    PremiumHammyCoachLine(
        tr = "Sakin düşün, doğru kelimeyi yakala.",
        en = "Stay calm and catch the right word.",
    ),
    PremiumHammyCoachLine(
        tr = "Bugün yeni bir seri başlatabiliriz! 🔥",
        en = "We can start a new streak today! 🔥",
    ),
)

private fun premiumHammyCoachLine(wins: Int, losses: Int): PremiumHammyCoachLine {
    val safeWins = wins.coerceAtLeast(0)
    val safeLosses = losses.coerceAtLeast(0)
    val totalMatches = safeWins + safeLosses

    return when {
        totalMatches <= 3 -> PremiumHammyCoachLine(
            tr = "İlk maçlarda hızdan önce doğru kelime. Sakin başla!",
            en = "In your first matches, accuracy comes before speed. Start calm!",
        )
        safeWins >= safeLosses + PREMIUM_HAMMY_LEAD_THRESHOLD -> PremiumHammyCoachLine(
            tr = "Güzel bir ritim yakaladın. Aynı sakinlikle devam et! 🌟",
            en = "You’ve found a good rhythm. Keep that same calm focus! 🌟",
        )
        safeLosses >= safeWins + PREMIUM_HAMMY_LEAD_THRESHOLD -> PremiumHammyCoachLine(
            tr = "Bugün yeni bir sayfa. Sakin başla, ritmini kur.",
            en = "Today is a fresh start. Begin calm and find your rhythm.",
        )
        else -> PremiumHammyCoachLine(
            tr = "Hazırsan başlayalım. Bugün güzel bir kelime bul!",
            en = "Ready? Let’s find a great word today!",
        )
    }
}

/**
 * Premium lobby presentation for Prof. Hammy.
 *
 * Hotfix note: the artwork is intentionally static here. The previous continuous graphics-layer
 * animation was the only new runtime work introduced exactly when the authenticated home screen
 * began closing on a real device. Keeping the same local artwork without a perpetual animation
 * isolates that regression while preserving the premium mascot visual. Gameplay and fair-play
 * state are not read or modified here.
 */
@Composable
fun ProfHammyHomeCard(
    wins: Int,
    losses: Int,
) {
    val coachLine = remember(wins, losses) { premiumHammyCoachLine(wins, losses) }
    var tapIndex by remember { mutableIntStateOf(-1) }
    val visibleLine = if (tapIndex < 0) {
        coachLine
    } else {
        premiumHammyTapLines[tapIndex % premiumHammyTapLines.size]
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = sh("Prof. Hammy ile etkileş", "Interact with Prof. Hammy"),
            ) {
                tapIndex = (tapIndex + 1) % premiumHammyTapLines.size
            },
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .padding(start = 7.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.prof_hammy_hero),
                contentDescription = sh("Prof. Hammy", "Prof. Hammy"),
                modifier = Modifier
                    .width(138.dp)
                    .height(160.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "PROF. HAMMY",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = sh("Kelime öğretmenin • Dokun ve selamlaş", "Your word coach • Tap to say hi"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sh(visibleLine.tr, visibleLine.en),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
