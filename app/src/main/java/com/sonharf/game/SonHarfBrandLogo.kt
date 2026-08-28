package com.sonharf.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun removeConnectedBlackBackground(source: Bitmap): Bitmap {
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    val width = bitmap.width
    val height = bitmap.height
    val count = width * height
    val pixels = IntArray(count)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val visited = BooleanArray(count)
    val queue = IntArray(count)
    var head = 0
    var tail = 0

    fun looksLikeBackground(pixel: Int): Boolean {
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha == 0) return false
        val red = (pixel ushr 16) and 0xFF
        val green = (pixel ushr 8) and 0xFF
        val blue = pixel and 0xFF
        return maxOf(red, green, blue) < 52 && red + green + blue < 118
    }

    fun enqueue(index: Int) {
        if (index !in 0 until count || visited[index] || !looksLikeBackground(pixels[index])) return
        visited[index] = true
        queue[tail++] = index
    }

    for (x in 0 until width) {
        enqueue(x)
        enqueue((height - 1) * width + x)
    }
    for (y in 0 until height) {
        enqueue(y * width)
        enqueue(y * width + width - 1)
    }

    while (head < tail) {
        val index = queue[head++]
        val x = index % width
        val y = index / width
        if (x > 0) enqueue(index - 1)
        if (x + 1 < width) enqueue(index + 1)
        if (y > 0) enqueue(index - width)
        if (y + 1 < height) enqueue(index + width)
    }

    for (i in 0 until tail) {
        val index = queue[i]
        pixels[index] = pixels[index] and 0x00FFFFFF
    }

    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

@Composable
private fun rememberTransparentSonHarfLogo(): ImageBitmap? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val decoded = BitmapFactory.decodeResource(context.resources, R.drawable.son_harf_splash_logo)
            removeConnectedBlackBackground(decoded).asImageBitmap()
        }.getOrNull()
    }
}

@Composable
fun SonHarfBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp? = 52.dp,
) {
    val transparentLogo = rememberTransparentSonHarfLogo() ?: return

    Image(
        bitmap = transparentLogo,
        contentDescription = "Son Harf",
        contentScale = ContentScale.Fit,
        modifier = if (size != null) modifier.size(size) else modifier,
    )
}
