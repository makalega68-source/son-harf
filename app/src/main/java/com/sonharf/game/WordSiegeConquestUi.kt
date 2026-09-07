package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
internal fun WordSiegeLiveRivalryBar(
    myScore: Int,
    opponentScore: Int,
    modifier: Modifier = Modifier,
) {
    val total = (myScore.coerceAtLeast(0) + opponentScore.coerceAtLeast(0)).coerceAtLeast(1)
    val myRatio = myScore.coerceAtLeast(0).toFloat() / total.toFloat()
    val neckAndNeck = abs(myScore - opponentScore) <= 5

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MainUi.Red.copy(alpha = .88f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(myRatio.coerceIn(0f, 1f))
                    .background(MainUi.Green),
            )
        }
        Text(
            text = if (neckAndNeck) {
                sh("⚡ BAŞ BAŞA", "⚡ NECK AND NECK")
            } else {
                sh("SEN $myScore  •  $opponentScore RAKİP", "YOU $myScore  •  $opponentScore RIVAL")
            },
            color = if (neckAndNeck) MainUi.Gold else MainUi.Muted,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
        )
    }
}

@Composable
internal fun WordSiegeConquestMeter(
    meter: Int,
    onslaughtActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (onslaughtActive) {
            Text("🔥 ×2", color = MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(3.dp))
        }
        repeat(WordSiegeZoneRules.ConquestMeterMax) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < meter) MainUi.Gold
                        else MainUi.Muted.copy(alpha = .28f),
                    ),
            )
        }
    }
}

@Composable
internal fun WordSiegeTempoBanner(
    isMyTurn: Boolean,
    secondsLeft: Int,
    opponentLabel: String,
    modifier: Modifier = Modifier,
) {
    val urgent = isMyTurn && secondsLeft <= WordSiegeZoneRules.UrgentTurnSeconds
    val background = when {
        urgent -> MainUi.Red
        isMyTurn -> MainUi.Gold
        else -> MainUi.Surface
    }
    val foreground = when {
        urgent -> Color.White
        isMyTurn -> Color.Black
        else -> MainUi.Muted
    }
    Surface(
        modifier = modifier,
        color = background,
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isMyTurn) {
                    sh("SIRA SENDE • ${secondsLeft.coerceAtLeast(0)} sn", "YOUR TURN • ${secondsLeft.coerceAtLeast(0)} s")
                } else {
                    sh("${opponentLabel.uppercase()} OYNUYOR • ${secondsLeft.coerceAtLeast(0)} sn", "${opponentLabel.uppercase()} IS PLAYING • ${secondsLeft.coerceAtLeast(0)} s")
                },
                color = foreground,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}
