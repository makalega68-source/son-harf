package com.sonharf.game

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SonHarfBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val context = LocalContext.current
    val remoteBytes = RemoteExperience.brandLogoBytes
    val logo = remember(remoteBytes) {
        fun decode(bytes: ByteArray?): androidx.compose.ui.graphics.ImageBitmap? {
            if (bytes == null || bytes.isEmpty()) return null
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
        decode(remoteBytes) ?: runCatching {
            val encoded = context.assets.open("son_harf_brand_logo.b64")
                .bufferedReader()
                .use { it.readText() }
                .trim()
            decode(Base64.decode(encoded, Base64.DEFAULT))
        }.getOrNull()
    }

    if (logo != null) {
        Image(
            bitmap = logo,
            contentDescription = "Son Harf Maskot",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
        )
    }
}
