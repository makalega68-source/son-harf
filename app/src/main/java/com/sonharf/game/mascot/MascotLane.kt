package com.sonharf.game.mascot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * MAÇ İÇİ MASKOT ŞERİDİ.
 *
 * TASARIM KURALI: Bu bileşen bir OVERLAY DEĞİLDİR. Kendi sabit yüksekliğinde
 * (LANE_HEIGHT) bir satırdır ve oyun ekranındaki diğer bileşenlerle aynı
 * Column içinde yer alır. Bu yüzden hiçbir şeyin üstünü kapatması FİZİKSEL
 * OLARAK MÜMKÜN DEĞİLDİR - tahta, harf taşı, yazı alanı ve klavye bu şeridin
 * dışında, kendi alanlarında dururlar.
 */
val LANE_HEIGHT = 96.dp

@Composable
fun MascotLane(
    secondsLeft: Int,
    mascotKey: String = "magecat",
    modifier: Modifier = Modifier
) {
    val mood = MascotBrain.mood
    val bubble = MascotBrain.bubble

    val t = rememberInfiniteTransition(label = "lane")
    val float by t.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(if (mood == Mood.PANIC) 170 else 1300, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "float"
    )
    val breathe by t.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val shake by t.animateFloat(
        initialValue = -2.5f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse),
        label = "shake"
    )

    // göz kırpma
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2600..5200).random().toLong())
            if (MascotBrain.idleBlinkAllowed()) { blink = true; delay(130); blink = false }
        }
    }

    // balon otomatik kapanır
    LaunchedEffect(bubble) {
        if (bubble != null) { delay(2200); MascotBrain.clearBubble() }
    }

    Row(
        modifier = modifier.fillMaxWidth().height(LANE_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val figureModifier = Modifier
            .graphicsLayer {
                translationY = if (mood == Mood.EXCITED) float * 2.4f else float
                translationX = if (mood == Mood.PANIC) shake else 0f
                scaleX = breathe; scaleY = breathe
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { MascotBrain.onPoke() }

        if (mascotKey == "magecat") {
            MageCatFigure(mood = mood, eyesVariant = MascotBrain.eyesVariant,
                size = 78.dp, blink = blink, modifier = figureModifier)
        } else {
            RiveMascotFigure(key = mascotKey, mood = mood, size = 78.dp, modifier = figureModifier)
        }

        Spacer(Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            AnimatedVisibility(
                visible = bubble != null,
                enter = fadeIn(tween(140)) + scaleIn(tween(160), initialScale = 0.86f),
                exit = fadeOut(tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp,
                            bottomEnd = 14.dp, bottomStart = 14.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = bubble ?: "",
                        color = Color(0xFF1E293B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
