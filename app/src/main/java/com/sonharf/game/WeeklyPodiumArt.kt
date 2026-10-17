package com.sonharf.game

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
 * The photo wells are cut out of the art, so photos are drawn behind it: the rings, crowns and the
 * 1-2-3 badges stay on top and the photo edge tucks under the ring. Spots are measured from the art
 * (fractions of its width/height); index 0 is the winner, 1 second, 2 third.
 */
internal enum class WeeklyPodiumStyle(
    @DrawableRes val art: Int,
    val aspect: Float,
    private val photos: List<WeeklyPodiumSpot>,
    private val labels: List<WeeklyPodiumSpot>,
    private val labelHeights: List<Float>,
    val inks: List<Color>,
) {
    HOME(
        art = R.drawable.weekly_podium_blue,
        aspect = 1494f / 843f,
        photos = listOf(WeeklyPodiumSpot(.4973f, .4994f, .2055f), WeeklyPodiumSpot(.1747f, .5575f, .1680f), WeeklyPodiumSpot(.8159f, .5694f, .1627f)),
        labels = listOf(WeeklyPodiumSpot(.5007f, .8577f, .2811f), WeeklyPodiumSpot(.1760f, .8553f, .2544f), WeeklyPodiumSpot(.8240f, .8553f, .2544f)),
        labelHeights = listOf(.140f, .130f, .130f),
        inks = listOf(Color(0xFF4A2A00), Color(0xFF1F2F4F), Color(0xFF3A1A08)),
    ),
    COMPETE(
        art = R.drawable.weekly_podium_gold,
        aspect = 1273f / 1067f,
        photos = listOf(WeeklyPodiumSpot(.4996f, .4508f, .1925f), WeeklyPodiumSpot(.1650f, .5464f, .1532f), WeeklyPodiumSpot(.8350f, .5801f, .1422f)),
        labels = listOf(WeeklyPodiumSpot(.4918f, .9494f, .2592f), WeeklyPodiumSpot(.1791f, .9522f, .2357f), WeeklyPodiumSpot(.8248f, .9550f, .2357f)),
        labelHeights = listOf(.066f, .060f, .060f),
        inks = listOf(Color(0xFF6B4A0A), Color(0xFF4A4F57), Color(0xFF6A3A18)),
    ),
    ;

    fun photo(place: Int): Triple<Float, Float, Float> = photos[place].let { Triple(it.x, it.y, it.w) }
    fun label(place: Int): Triple<Float, Float, Float> = labels[place].let { Triple(it.x, it.y, it.w) }
    fun labelHeight(place: Int): Float = labelHeights[place]
}

/**
 * The weekly top three on the podium art: each photo fills its ring well edge to edge with no extra
 * frame, sitting under the ring and its number badge; the name and points fit on the player's plate.
 */
@Composable
internal fun WeeklyPodiumArt(
    style: WeeklyPodiumStyle,
    seats: List<PodiumSeat?>,
    modifier: Modifier = Modifier,
) {
    if (style == WeeklyPodiumStyle.HOME) {
        HomeWeeklyPodium(seats, modifier)
        return
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(style.aspect)) {
        val w = maxWidth
        val h = maxHeight
        // 1) Photos (or a soft empty well) behind the art; slightly larger than the well so the
        //    edge hides under the ring.
        for (place in 0..2) {
            val seat = seats.getOrNull(place)
            val (px, py, pd) = style.photo(place)
            val size = w * pd * 1.04f
            Box(
                Modifier
                    .offset(x = w * px - size / 2, y = h * py - size / 2)
                    .size(size)
                    .clip(CircleShape),
            ) {
                if (seat != null) PodiumPhoto(seat, size) else EmptyWell()
            }
        }
        // 2) The art on top: rings, crowns and the 1-2-3 badges cover the photo edges.
        Image(rememberArtPainter(style.art, 1200), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        // 3) Names and points, sized to their plate so they always fit.
        for (place in 0..2) {
            val seat = seats.getOrNull(place)
            val (lx, ly, lw) = style.label(place)
            val labelW = w * lw
            val labelH = h * style.labelHeight(place)
            val ink = style.inks[place]
            val compete = style == WeeklyPodiumStyle.COMPETE
            val nameSize = with(density) { (labelH * (if (compete) .46f else .36f)).toSp() }
            val scoreSize = with(density) { (labelH * (if (compete) .34f else .27f)).toSp() }
            val name = seat?.name?.ifBlank { sh("Oyuncu", "Player") } ?: sh("Boş", "Open")
            val nameColor = if (seat != null) ink else ink.copy(alpha = .45f)
            val box = Modifier
                .offset(x = w * lx - labelW / 2, y = h * ly - labelH / 2)
                .size(labelW, labelH)
            if (compete) {
                // Pedestal plate: one line, "name · points", on a soft ivory pill.
                Row(
                    box.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .6f)).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        name,
                        color = nameColor,
                        fontSize = nameSize,
                        lineHeight = nameSize,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (seat != null) {
                        Text(" · ${seat.score}", color = ink.copy(alpha = .85f), fontSize = scoreSize, lineHeight = scoreSize, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            } else {
                // Name plate: name above, points below.
                Column(
                    box.padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        name,
                        color = nameColor,
                        fontSize = nameSize,
                        lineHeight = nameSize,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (seat != null) {
                        Text(seat.score, color = ink.copy(alpha = .85f), fontSize = scoreSize, lineHeight = scoreSize, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWell() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3E8F0)))))
}

/** The player's photo filling the ring edge to edge; the initial when there is no photo. */
@Composable
private fun PodiumPhoto(seat: PodiumSeat, size: Dp) {
    var bytes by remember(seat.userId, seat.avatarPath) { mutableStateOf<ByteArray?>(null) }
    var gender by remember(seat.userId) { mutableStateOf<String?>(null) }
    LaunchedEffect(seat.userId, seat.avatarPath) {
        val profile = runCatching { com.sonharf.game.data.OnlineGameBackend().getProfile(seat.userId) }.getOrNull()
        gender = profile?.gender
        bytes = if (profile?.avatarVisibility != "hidden") {
            profile?.avatarPath?.takeIf { it.isNotBlank() }?.let { ProfilePhotoRuntime.load(it) }
        } else null
    }
    val bitmap = rememberProfileBitmap(bytes)
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), seat.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        DefaultProfilePortrait(gender, Modifier.fillMaxSize())
    }
}

@Composable
private fun HomeWeeklyPodium(seats: List<PodiumSeat?>, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(SonHarfTheme.Surface, SonHarfTheme.SurfaceSecondary)))
            .border(.8.dp, SonHarfTheme.PremiumGold.copy(alpha = .35f), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(sh("HAFTALIK SIRALAMA", "WEEKLY RANKING"), color = SonHarfTheme.TextPrimary,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom) {
            listOf(1, 0, 2).forEach { place ->
                val seat = seats.getOrNull(place)
                val winner = place == 0
                val medal = when (place) { 0 -> SonHarfTheme.PremiumGold; 1 -> Color(0xFF89908E); else -> Color(0xFFAF8261) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${place + 1}", color = medal, fontSize = if (winner) 16.sp else 13.sp, fontWeight = FontWeight.Bold)
                    val size = if (winner) 62.dp else 48.dp
                    Box(Modifier.size(size).clip(CircleShape).border(if (winner) 2.dp else 1.dp, medal, CircleShape)) {
                        if (seat != null) PodiumPhoto(seat, size) else DefaultProfilePortrait(null, Modifier.fillMaxSize())
                    }
                    Text(seat?.name ?: sh("Boş", "Open"), color = SonHarfTheme.TextPrimary, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(seat?.score ?: "—", color = SonHarfTheme.TextSecondary, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}
