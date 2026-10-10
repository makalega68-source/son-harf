package com.sonharf.game

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Large artwork (the brand logo, game icons, podiums) decoded once, off the main thread and at
 * about the size it is shown, then kept in memory. painterResource decoded the full-size image on
 * the main thread every time a page appeared, which stuttered page transitions.
 */
internal object ArtCache {
    private const val MAX_BYTES = 24 * 1024 * 1024
    private val cache = object : LruCache<Long, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: Long, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** Images are decoded no wider than this; the largest art is shown well under it. */
    const val DEFAULT_MAX_WIDTH_PX = 900

    /** Full-width page heroes (profile podium, throne) keep more detail. */
    const val HERO_MAX_WIDTH_PX = 1200

    private fun key(res: Int, maxWidthPx: Int): Long = (res.toLong() shl 16) or (maxWidthPx.toLong() and 0xFFFF)

    fun cached(@DrawableRes res: Int, maxWidthPx: Int = DEFAULT_MAX_WIDTH_PX): ImageBitmap? = cache.get(key(res, maxWidthPx))

    suspend fun load(context: Context, @DrawableRes res: Int, maxWidthPx: Int = DEFAULT_MAX_WIDTH_PX): ImageBitmap? {
        cached(res, maxWidthPx)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val resources = context.applicationContext.resources
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
                BitmapFactory.decodeResource(resources, res, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxWidthPx) sample *= 2
                BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inScaled = false; inSampleSize = sample })
                    ?.asImageBitmap()
                    ?.also { cache.put(key(res, maxWidthPx), it) }
            }.getOrNull()
        }
    }

    /** Warms the cache so the first page that shows this art has it at once. */
    suspend fun prefetch(context: Context, vararg res: Int) {
        res.forEach { load(context, it) }
    }

    /** Same, for art shown at [maxWidthPx]. */
    suspend fun prefetchAt(context: Context, maxWidthPx: Int, vararg res: Int) {
        res.forEach { load(context, it, maxWidthPx) }
    }
}

/** A painter for large artwork: cached at once, otherwise transparent for the moment it decodes. */
@Composable
internal fun rememberArtPainter(@DrawableRes res: Int, maxWidthPx: Int = ArtCache.DEFAULT_MAX_WIDTH_PX): Painter {
    val context = LocalContext.current
    var bitmap by remember(res, maxWidthPx) { mutableStateOf(ArtCache.cached(res, maxWidthPx)) }
    LaunchedEffect(res, maxWidthPx) {
        if (bitmap == null) bitmap = ArtCache.load(context, res, maxWidthPx)
    }
    val image = bitmap
    return remember(image) { if (image != null) BitmapPainter(image) else ColorPainter(Color.Transparent) }
}
