package com.sonharf.game.mascot

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit

/**
 * SATIN ALINABİLİR ALTERNATİF MASKOTLAR (coin ile, mağazadan).
 * Her .riv dosyasının state machine adı ve mood input adı FARKLI OLABİLİR;
 * dosyayı Rive editöründe açıp gerçek adları BURAYA yaz. Ad tutmuyorsa
 * animasyon oynamaz ama uygulama çökmez, dosya idle karede kalır.
 *
 * key -> (asset dosya adı, state machine adı, mood input adı)
 */
private val RIVE_MAP: Map<String, Triple<String, String, String>> = mapOf(
    "gut"    to Triple("gut_expression.riv", "State Machine 1", "Expression"),
    "sprout" to Triple("sprout.riv", "State Machine 1", "mood"),
    "o11y"   to Triple("o11y.riv", "State Machine 1", "mood")
)

/** Mood -> her dosyadaki sayısal input değeri. Dosyayı Rive editöründe açıp
 *  hangi sayının hangi animasyona karşılık geldiğini KONTROL ET, gerekirse düzelt. */
private fun moodToNumber(mood: Mood): Float = when (mood) {
    Mood.IDLE -> 0f; Mood.HAPPY -> 1f; Mood.EXCITED -> 1f
    Mood.ANGRY -> 2f; Mood.PANIC -> 2f
    Mood.SAD -> 3f; Mood.CRYING -> 3f
    Mood.WINK -> 4f; Mood.TIRED -> 0f
}

@Composable
fun RiveMascotFigure(key: String, mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    val cfg = RIVE_MAP[key]
    if (cfg == null) { MageCatFigure(mood, MascotBrain.eyesVariant, size, modifier = modifier); return }
    val (asset, smName, inputName) = cfg

    val view = remember {
        RiveAnimationView.Builder()
            .setAutoplay(true)
            .build()
    }

    AndroidView(
        modifier = modifier.size(size),
        factory = { ctx ->
            RiveAnimationView(ctx).apply {
                runCatching {
                    setRiveResourceFromAsset(ctx, asset, fit = Fit.CONTAIN, stateMachineName = smName)
                }
            }
        },
        update = { v ->
            runCatching {
                v.setNumberState(smName, inputName, moodToNumber(mood))
            }
        }
    )
}

/** Mağazada kuşanılmış maskota göre doğru görünümü seçer.
 *  "magecat" ise vektör Mage Cat, diğerleri Rive dosyası. */
@Composable
fun EquippedMascotFigure(equippedKey: String, mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    if (equippedKey == "magecat" || equippedKey.isBlank()) {
        MageCatFigure(mood, MascotBrain.eyesVariant, size, modifier = modifier)
    } else {
        RiveMascotFigure(equippedKey, mood, size, modifier)
    }
}
