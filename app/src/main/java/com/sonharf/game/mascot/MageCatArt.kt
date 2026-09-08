package com.sonharf.game.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ruh hâli -> drawable dosya adı eşlemesi. Eksik dosya olursa 0 döner, yedek çizim devreye girer. */
object FaceAssets {
    fun eyeRes(ctx: android.content.Context, mood: Mood, variant: String): Int {
        val shared = when (mood) {
            Mood.HAPPY -> "eye_shared_happy"
            Mood.PANIC -> "eye_shared_panic"
            Mood.TIRED -> "eye_shared_tired"
            Mood.WINK  -> "eye_shared_wink"
            else -> null
        }
        val perVariant = when (mood) {
            Mood.IDLE -> "eye_${variant}_basic"
            Mood.EXCITED -> "eye_${variant}_excited"
            Mood.ANGRY -> "eye_${variant}_angry"
            Mood.SAD -> "eye_${variant}_sad"
            Mood.CRYING -> "eye_${variant}_crying"
            else -> null
        }
        val name = shared ?: perVariant ?: "eye_${variant}_basic"
        return id(ctx, name).takeIf { it != 0 } ?: id(ctx, "eye_${variant}_basic")
    }

    fun closedEyeRes(ctx: android.content.Context) = id(ctx, "eye_shared_closed")

    fun mouthRes(ctx: android.content.Context, mood: Mood): Int {
        val name = when (mood) {
            Mood.HAPPY, Mood.EXCITED -> "mouth_happy"
            Mood.ANGRY -> "mouth_angry"
            Mood.SAD, Mood.CRYING -> "mouth_sad"
            Mood.PANIC -> "mouth_surprised"
            Mood.TIRED -> "mouth_serious"
            else -> "mouth_basic"
        }
        return id(ctx, name).takeIf { it != 0 } ?: id(ctx, "mouth_basic")
    }

    private fun id(ctx: android.content.Context, n: String): Int =
        ctx.resources.getIdentifier(n, "drawable", ctx.packageName)
}

/**
 * Mage Cat: gövde/kafa/kulak/şapka vektörle çizilir, göz ve ağız senin
 * orijinal texture'larından gelir. 3D yok, sprite sheet yok, ~0 bellek maliyeti.
 */
@Composable
fun MageCatFigure(
    mood: Mood,
    eyesVariant: String,
    size: Dp,
    blink: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val eyeRes = remember(mood, eyesVariant, blink) {
        if (blink) FaceAssets.closedEyeRes(ctx).takeIf { it != 0 } ?: FaceAssets.eyeRes(ctx, mood, eyesVariant)
        else FaceAssets.eyeRes(ctx, mood, eyesVariant)
    }
    val mouthRes = remember(mood) { FaceAssets.mouthRes(ctx, mood) }

    Box(modifier = modifier.size(size)) {

        Canvas(modifier = Modifier.fillMaxSize()) { drawCatBody(mood) }

        val s = size.value
        if (eyeRes != 0) {
            Image(
                painter = painterResource(eyeRes), contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size((s * 0.20f).dp)
                    .graphicsLayer {
                        translationX = s * 0.255f * density
                        translationY = s * 0.400f * density
                    }
            )
            Image(
                painter = painterResource(eyeRes), contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size((s * 0.20f).dp)
                    .graphicsLayer {
                        scaleX = -1f
                        translationX = s * 0.545f * density
                        translationY = s * 0.400f * density
                    }
            )
        }
        if (mouthRes != 0) {
            Image(
                painter = painterResource(mouthRes), contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size((s * 0.26f).dp)
                    .graphicsLayer {
                        translationX = s * 0.370f * density
                        translationY = s * 0.610f * density
                    }
            )
        }
    }
}

private val FUR = Color(0xFFF7EDE6)
private val FUR_SHADE = Color(0xFFE7D6C9)
private val EAR_IN = Color(0xFFF4B3C4)
private val HAT = Color(0xFF7C6BE8)
private val HAT_DARK = Color(0xFF5A4BC0)
private val STAR = Color(0xFFFFD36B)

private fun DrawScope.drawCatBody(mood: Mood) {
    val w = size.width; val h = size.height
    val cx = w / 2f

    // kuyruk
    val tail = Path().apply {
        moveTo(w * 0.70f, h * 0.86f)
        quadraticBezierTo(w * 0.98f, h * 0.86f, w * 0.93f, h * 0.66f)
    }
    drawPath(tail, HAT, style = Stroke(width = w * 0.045f))

    // gövde
    val body = Path().apply {
        moveTo(w * 0.30f, h * 0.90f)
        quadraticBezierTo(w * 0.26f, h * 0.70f, cx, h * 0.70f)
        quadraticBezierTo(w * 0.74f, h * 0.70f, w * 0.70f, h * 0.90f)
        quadraticBezierTo(cx, h * 0.99f, w * 0.30f, h * 0.90f)
        close()
    }
    drawPath(body, HAT)

    // kulaklar
    val earL = Path().apply {
        moveTo(w * 0.30f, h * 0.34f); lineTo(w * 0.24f, h * 0.12f); lineTo(w * 0.46f, h * 0.22f); close()
    }
    val earR = Path().apply {
        moveTo(w * 0.70f, h * 0.34f); lineTo(w * 0.76f, h * 0.12f); lineTo(w * 0.54f, h * 0.22f); close()
    }
    drawPath(earL, FUR); drawPath(earR, FUR)
    val inL = Path().apply {
        moveTo(w * 0.32f, h * 0.31f); lineTo(w * 0.28f, h * 0.17f); lineTo(w * 0.42f, h * 0.24f); close()
    }
    val inR = Path().apply {
        moveTo(w * 0.68f, h * 0.31f); lineTo(w * 0.72f, h * 0.17f); lineTo(w * 0.58f, h * 0.24f); close()
    }
    drawPath(inL, EAR_IN); drawPath(inR, EAR_IN)

    // kafa
    drawOval(color = FUR,
        topLeft = Offset(w * 0.16f, h * 0.26f),
        size = Size(w * 0.68f, h * 0.48f))
    drawOval(color = FUR_SHADE.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.16f, h * 0.56f),
        size = Size(w * 0.68f, h * 0.18f))

    // şapka
    drawOval(color = HAT_DARK,
        topLeft = Offset(w * 0.13f, h * 0.235f),
        size = Size(w * 0.74f, h * 0.10f))
    val cone = Path().apply {
        moveTo(cx, h * 0.02f)
        quadraticBezierTo(w * 0.70f, h * 0.14f, w * 0.78f, h * 0.27f)
        quadraticBezierTo(cx, h * 0.21f, w * 0.22f, h * 0.27f)
        quadraticBezierTo(w * 0.30f, h * 0.14f, cx, h * 0.02f)
        close()
    }
    drawPath(cone, HAT)
    drawCircle(color = STAR, radius = w * 0.035f, center = Offset(cx, h * 0.045f))

    // yanak allığı
    val blush = when (mood) {
        Mood.HAPPY, Mood.EXCITED, Mood.WINK -> 0.55f
        Mood.PANIC -> 0.15f
        else -> 0.3f
    }
    drawOval(color = EAR_IN.copy(alpha = blush),
        topLeft = Offset(w * 0.205f, h * 0.545f), size = Size(w * 0.13f, h * 0.055f))
    drawOval(color = EAR_IN.copy(alpha = blush),
        topLeft = Offset(w * 0.665f, h * 0.545f), size = Size(w * 0.13f, h * 0.055f))

    // bıyıklar
    val wc = Color(0xFFD9C7BB)
    drawLine(wc, Offset(w * 0.20f, h * 0.60f), Offset(w * 0.06f, h * 0.575f), strokeWidth = w * 0.012f)
    drawLine(wc, Offset(w * 0.20f, h * 0.645f), Offset(w * 0.07f, h * 0.655f), strokeWidth = w * 0.012f)
    drawLine(wc, Offset(w * 0.80f, h * 0.60f), Offset(w * 0.94f, h * 0.575f), strokeWidth = w * 0.012f)
    drawLine(wc, Offset(w * 0.80f, h * 0.645f), Offset(w * 0.93f, h * 0.655f), strokeWidth = w * 0.012f)

    // panik teri / gözyaşı
    if (mood == Mood.PANIC) {
        drawCircle(Color(0xFF79D2F2), w * 0.035f, Offset(w * 0.79f, h * 0.30f))
    }
    if (mood == Mood.CRYING) {
        drawOval(Color(0xFF79D2F2), Offset(w * 0.26f, h * 0.55f), Size(w * 0.05f, h * 0.09f))
        drawOval(Color(0xFF79D2F2), Offset(w * 0.69f, h * 0.57f), Size(w * 0.05f, h * 0.09f))
    }
}
