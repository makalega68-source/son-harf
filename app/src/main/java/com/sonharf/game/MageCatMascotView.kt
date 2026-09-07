package com.sonharf.game

import androidx.compose.animation.core.animateFloatAsState
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
 * composable'ların dış sözleşmesi korunarak iç renderer değiştirilebilir.
 */
@Composable
fun MageCatHomeMascot(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return

    // Ana giriş/lobi yolu en hassas uygulama yüzeyidir. Burada sonsuz animasyon yerine
    // deterministik statik sunum kullanılır; maç içi olay tepkileri ayrı renderer'da kalır.
    MageCatImage(modifier = modifier)
}

/** Maç ekranında küçük ve sakin; yalnızca director bir cue ürettiğinde kısa tepki verir. */
@Composable
internal fun MageCatMatchMascot(
    cue: MageCatCue?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    MageCatReactiveMascot(cue = cue, modifier = modifier, resultMode = false)
}

/** Sonuç ekranında zafer/mağlubiyet cue'sunu daha belirgin HERO sunumuyla gösterir. */
@Composable
internal fun MageCatResultMascot(
    cue: MageCatCue?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) return
    MageCatReactiveMascot(cue = cue, modifier = modifier, resultMode = true)
}

@Composable
private fun MageCatReactiveMascot(
    cue: MageCatCue?,
    modifier: Modifier,
    resultMode: Boolean,
) {
    val prominence = cue?.prominence ?: MageCatProminence.AMBIENT
    val targetScale = when (prominence) {
        MageCatProminence.AMBIENT -> 1f
        MageCatProminence.REACTION -> if (resultMode) 1.06f else 1.10f
        MageCatProminence.HERO -> if (resultMode) 1.14f else 1.08f
    }
    val targetRotation = when (cue?.motion) {
        MageCatMotion.EXCITED, MageCatMotion.MAGIC_BURST, MageCatMotion.WAND_CELEBRATE -> -4f
        MageCatMotion.HAPPY, MageCatMotion.PROUD -> 2.5f
        MageCatMotion.SAD, MageCatMotion.TIRED -> -2.5f
        else -> 0f
    }
    val targetOffsetY = when (cue?.motion) {
        MageCatMotion.EXCITED, MageCatMotion.MAGIC_BURST, MageCatMotion.WAND_CELEBRATE -> -8f
        MageCatMotion.SAD, MageCatMotion.TIRED -> 4f
        else -> 0f
    }

    val scale = animateFloatAsState(targetScale, tween(260), label = "mageCatScale")
    val rotation = animateFloatAsState(targetRotation, tween(300), label = "mageCatRotation")
    val offsetY = animateFloatAsState(targetOffsetY, tween(240), label = "mageCatOffsetY")

    MageCatImage(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            rotationZ = rotation.value
            translationY = offsetY.value
        },
    )
}

@Composable
private fun MageCatImage(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.mage_cat_preview_anthracite),
        contentDescription = "Mage Cat",
        contentScale = ContentScale.Fit,
        modifier = modifier.alpha(0.98f),
    )
}

val MageCatHomeDefaultSize = 112.dp
internal val MageCatMatchDefaultSize = 72.dp
internal val MageCatResultDefaultSize = 148.dp
