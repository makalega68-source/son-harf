package com.sonharf.game.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import kotlinx.coroutines.delay

/**
 * G5.3 — Oyun alanı hareketleri.
 *
 * Bu dosya 4 küçük hareket parçası içerir; oyun ekranları bunları
 * doğrudan bileşen olarak veya Modifier extension olarak kullanır.
 * Animasyonların hiçbiri oyun state'ini beklemez (G3.0 kuralı).
 */

// ---------------------------------------------------------------------
// 1) MatchOpenSlideIn — maç açılışında aşağıdan kayarak gelme
// ---------------------------------------------------------------------
/**
 * İçeriği [triggerKey] değeri değiştiğinde aşağıdan yukarıya
 * kaydırarak gösterir. En fazla 600ms; reduced-motion açıksa
 * doğrudan görünür (animasyonsuz).
 */
@Composable
fun MatchOpenSlideIn(
    triggerKey: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    var visible by remember(triggerKey) { mutableStateOf(reducedMotion) }
    LaunchedEffect(triggerKey) {
        if (!reducedMotion) {
            visible = false
            delay(20)
            visible = true
        }
    }
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(600)) { it / 4 },
            exit = fadeOut(),
        ) {
            content()
        }
    }
}

// ---------------------------------------------------------------------
// 2) TurnBanner — "SENİN SIRAN" / "RAKİBİN SIRASI" 600ms şerit
// ---------------------------------------------------------------------
/**
 * [turnOwner] değiştiğinde ekran ortasından kısa (600ms) kayıp
 * kaybolan şerit. Reduced-motion açıksa bir kez fadeIn/Out ile
 * gösterilir.
 */
@Composable
fun TurnBanner(
    turnOwner: TurnOwner,
    language: String = "tr",
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    var visible by remember(turnOwner) { mutableStateOf(false) }
    LaunchedEffect(turnOwner) {
        visible = true
        delay(600)
        visible = false
    }
    val label = when (turnOwner) {
        TurnOwner.MINE -> if (language == "en") "YOUR TURN" else "SENİN SIRAN"
        TurnOwner.RIVAL -> if (language == "en") "RIVAL'S TURN" else "RAKİBİN SIRASI"
        TurnOwner.NONE -> return
    }
    val accent = when (turnOwner) {
        TurnOwner.MINE -> SonHarfTheme.GoldBright
        TurnOwner.RIVAL -> SonHarfTheme.Lavender
        TurnOwner.NONE -> SonHarfTheme.PremiumTextSecondary
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reducedMotion) fadeIn() else fadeIn(tween(150)) + slideInHorizontally(tween(300)) { -it / 3 },
            exit = if (reducedMotion) fadeOut() else fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { it / 3 },
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent.copy(alpha = 0.22f))
                    .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                    ),
                    color = accent,
                )
            }
        }
    }
}

enum class TurnOwner { MINE, RIVAL, NONE }

// ---------------------------------------------------------------------
// 3) TimerRing — yeşil → turuncu → kırmızı yumuşak geçiş
// ---------------------------------------------------------------------
/**
 * Süre halkasının renk hesaplaması. [secondsLeft] / [totalSeconds]
 * oranına göre yeşil→turuncu→kırmızı üzerine bir renk döner.
 * Ring çizimi caller'a bırakılır; burası yalnızca renk kontratı.
 */
@Composable
fun rememberTimerRingColor(
    secondsLeft: Int,
    totalSeconds: Int,
): Color {
    val frac = if (totalSeconds <= 0) 0f
    else (secondsLeft.toFloat() / totalSeconds).coerceIn(0f, 1f)
    // 0..0.33 kırmızı, 0.33..0.66 turuncu, 0.66..1 yeşil.
    val target = when {
        frac > 0.66f -> Color(0xFF35C878)   // green
        frac > 0.33f -> SonHarfTheme.SonHarfOrange
        else -> Color(0xFFE85555)           // red
    }
    // Yumuşak geçiş için animateColorAsState kullanmak isterdik ama
    // burada bir animasyon spec'ine gerek yok — caller kendi
    // graphicsLayer'ıyla animate edebilir. Sabit color değerlerimiz
    // zaten üç aşamalı; ara geçişi caller LaunchedEffect ile yapabilir.
    return target
}

// ---------------------------------------------------------------------
// 4) TapPressScale — taş dokununca büyüme + gölge
// ---------------------------------------------------------------------
/**
 * Basıldığında %105 ölçek + [pressedElevation] gölge, bırakınca
 * yumuşak geri döner (spring). Reduced-motion açıksa sabit.
 * Sürükleme dönme efekti bu bileşene dahil değildir; caller ayrı
 * bir Modifier.rotate ekleyebilir.
 */
@Composable
fun Modifier.tilePressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 1.05f,
    pressedElevation: Dp = 6.dp,
    reducedMotion: Boolean = rememberReducedMotion(),
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val target = if (pressed && !reducedMotion) pressedScale else 1f
    val scale by animateFloatAsState(target, spring(), label = "tile-scale")
    val elevation = if (pressed) pressedElevation else 0.dp
    this
        .shadow(elevation = elevation, shape = RoundedCornerShape(10.dp), clip = false)
        .graphicsLayer(scaleX = scale, scaleY = scale)
}
