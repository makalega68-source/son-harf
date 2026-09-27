package com.sonharf.game

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One of the week's top three. */
internal data class PodiumSeat(val userId: String, val name: String, val score: String, val avatarPath: String?)

/** A spot on the art, as fractions of the image: centre x, centre y and width (diameter for photos). */
internal data class WeeklyPodiumSpot(val x: Float, val y: Float, val w: Float)

/**
 * The two weekly podium designs:
 * - [HOME]: the blue card with silver, gold and bronze rings and name plates (home screen).
 * - [COMPETE]: the gold and ivory pedestals with ruby crowns (Rekabet).
 * Spots are measured from the art; index 0 is the winner, 1 second, 2 third.
 */
internal enum class WeeklyPodiumStyle(
    @DrawableRes val art: Int,
    val aspect: Float,
    private val photos: List<WeeklyPodiumSpot>,
    private val labels: List<WeeklyPodiumSpot>,
    val labelHeight: Float,
    val inks: List<Color>,
) {
    HOME(
        art = R.drawable.weekly_podium_blue,
        aspect = 1602f / 982f,
        photos = listOf(WeeklyPodiumSpot(.4975f, .4975f, .1941f), WeeklyPodiumSpot(.1966f, .5474f, .1592f), WeeklyPodiumSpot(.7946f, .5575f, .1542f)),
        labels = listOf(WeeklyPodiumSpot(.5009f, .8014f, .2900f), WeeklyPodiumSpot(.2019f, .7994f, .2600f), WeeklyPodiumSpot(.8021f, .7994f, .2580f)),
        labelHeight = .15f,
        inks = listOf(Color(0xFF4A2A00), Color(0xFF1F2F4F), Color(0xFF3A1A08)),
    ),
    COMPETE(
        art = R.drawable.weekly_podium_gold,
        aspect = 1402f / 1080f,
        photos = listOf(WeeklyPodiumSpot(.4993f, .4542f, .1776f), WeeklyPodiumSpot(.1954f, .5486f, .1419f), WeeklyPodiumSpot(.8038f, .5819f, .1320f)),
        labels = listOf(WeeklyPodiumSpot(.4921f, .9444f, .2800f), WeeklyPodiumSpot(.2083f, .9444f, .2500f), WeeklyPodiumSpot(.7946f, .9444f, .2500f)),
        labelHeight = .085f,
        inks = listOf(Color(0xFF6B4A0A), Color(0xFF4A4F57), Color(0xFF6A3A18)),
    ),
    ;

    fun photo(place: Int): Triple<Float, Float, Float> = photos[place].let { Triple(it.x, it.y, it.w) }
    fun label(place: Int): Triple<Float, Float, Float> = labels[place].let { Triple(it.x, it.y, it.w) }
}

/**
 * The weekly top three on the podium art: each photo fills its ring exactly, with no extra frame,
 * and the name and points sit on the player's plate.
 */
@Composable
internal fun WeeklyPodiumArt(
    style: WeeklyPodiumStyle,
    seats: List<PodiumSeat?>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(style.aspect)) {
        val w = maxWidth
        val h = maxHeight
        Image(painterResource(style.art), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        for (place in 0..2) {
            val seat = seats.getOrNull(place)
            val (px, py, pd) = style.photo(place)
            val size = w * pd
            Box(
                Modifier
                    .offset(x = w * px - size / 2, y = h * py - size / 2)
                    .size(size)
                    .clip(CircleShape),
            ) {
                if (seat != null) PodiumPhoto(seat, size)
            }
            val (lx, ly, lw) = style.label(place)
            val labelW = w * lw
            val labelH = h * style.labelHeight
            val ink = style.inks[place]
            Column(
                Modifier
                    .offset(x = w * lx - labelW / 2, y = h * ly - labelH / 2)
                    .size(labelW, labelH)
                    .then(
                        if (style == WeeklyPodiumStyle.COMPETE) {
                            Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .55f))
                        } else Modifier,
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val nameSize = (w.value * (if (place == 0) .040f else .034f)).sp
                Text(
                    seat?.name?.ifBlank { sh("Oyuncu", "Player") } ?: sh("Boş", "Open"),
                    color = if (seat != null) ink else ink.copy(alpha = .45f),
                    fontSize = nameSize,
                    lineHeight = nameSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                if (seat != null) {
                    val scoreSize = (w.value * .030f).sp
                    Text(
                        seat.score,
                        color = ink.copy(alpha = .85f),
                        fontSize = scoreSize,
                        lineHeight = scoreSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The player's photo filling the ring edge to edge; the initial when there is no photo. */
@Composable
private fun PodiumPhoto(seat: PodiumSeat, size: Dp) {
    var bytes by remember(seat.avatarPath) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(seat.avatarPath) {
        bytes = seat.avatarPath?.takeIf { it.isNotBlank() }?.let { ProfilePhotoRuntime.load(it) }
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), seat.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF4F6FA), Color(0xFFDCE2EC)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                seat.name.take(1).uppercase(),
                color = Color(0xFF3A4A66),
                fontSize = (size.value * .42f).sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
