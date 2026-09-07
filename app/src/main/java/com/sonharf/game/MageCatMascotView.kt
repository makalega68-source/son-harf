package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Gerçek satın alınmış Mage Cat modelinin Android için düşük riskli geçici sunum katmanı.
 *
 * Kaynak model/texture değiştirilmez. Bu görünüm Unity paketindeki gerçek model önizlemesinden
 * türetilmiş antrasit varyantı kullanır. Tam iskelet animasyonlu renderer hazır olduğunda bu
 * composable'ın dış sözleşmesi korunarak iç renderer değiştirilebilir.
 */
@Composable
fun MageCatHomeMascot(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return

    val transition = rememberInfiniteTransition(label = "mageCatIdle")
    val offsetY = transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mageCatIdleOffset",
    )
    val rotation = transition.animateFloat(
        initialValue = -0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mageCatIdleRotation",
    )

    Image(
        painter = painterResource(R.drawable.mage_cat_preview_anthracite),
        contentDescription = "Mage Cat",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY.value
                rotationZ = rotation.value
            }
            .alpha(0.98f),
    )
}

val MageCatHomeDefaultSize = 112.dp
