package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

private val PodiumCardTop = Color(0xFF1B4132)
private val PodiumCardBottom = Color(0xFF0B1F17)
private val PodiumCardEdge = Color(0x33F2C14E)
private val PodiumGoldLight = Color(0xFFFFE9A8)
private val PodiumGold = Color(0xFFF2C14E)
private val PodiumGoldDeep = Color(0xFFB07D1A)
private val PodiumSilverLight = Color(0xFFF4F8FB)
private val PodiumSilver = Color(0xFFD6DEE6)
private val PodiumSilverDeep = Color(0xFF8E9AA6)
private val PodiumBronzeLight = Color(0xFFF0C79E)
private val PodiumBronze = Color(0xFFDDA173)
private val PodiumBronzeDeep = Color(0xFF8A5A2B)
private val PodiumTextMain = Color(0xFFF8FAFC)
private val PodiumTextDim = Color(0xFF9FB3A8)

@Composable
internal fun WeeklyPodiumCardV210(
    players: List<WeeklyPodiumPlayer>,
    loading: Boolean,
    onOpenLeague: () -> Unit,
) {
    if (players.size < 3) return

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, PodiumCardEdge),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(PodiumCardTop, PodiumCardBottom)))
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            WeeklyPodiumHeaderV210(onOpenLeague)
            Spacer(Modifier.height(14.dp))
            WeeklyPodiumStageV210(players)
            Spacer(Modifier.height(10.dp))
            WeeklyChampionPlaqueV210()
            if (loading) Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun WeeklyPodiumHeaderV210(onOpenLeague: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color(0x1AF2C14E),
            border = BorderStroke(1.5.dp, PodiumGold.copy(alpha = 0.55f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = PodiumGold, modifier = Modifier.size(23.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                sh("HAFTANIN ZİRVESİ", "WEEKLY PODIUM"),
                color = PodiumTextMain,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
            )
            Text(
                sh("Bu haftanın en güçlü oyuncuları", "This week's strongest players"),
                color = PodiumTextDim,
                fontSize = 12.sp,
            )
        }
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color(0x14FFFFFF),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.clickable(onClick = onOpenLeague),
        ) {
            Text(
                sh("TÜMÜ  ›", "ALL  ›"),
                color = PodiumTextMain,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun WeeklyPodiumStageV210(players: List<WeeklyPodiumPlayer>) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) { drawPodiumConfettiV210() }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.fillMaxWidth(0.62f).height(150.dp)) { drawPodiumLaurelV210() }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WeeklyPodiumColumnV210(
                player = players[1],
                rank = 2,
                slabHeight = 118.dp,
                light = PodiumSilverLight,
                mid = PodiumSilver,
                deep = PodiumSilverDeep,
                modifier = Modifier.weight(1f),
            )
            WeeklyPodiumColumnV210(
                player = players[0],
                rank = 1,
                slabHeight = 162.dp,
                light = PodiumGoldLight,
                mid = PodiumGold,
                deep = PodiumGoldDeep,
                crowned = true,
                modifier = Modifier.weight(1.18f),
            )
            WeeklyPodiumColumnV210(
                player = players[2],
                rank = 3,
                slabHeight = 100.dp,
                light = PodiumBronzeLight,
                mid = PodiumBronze,
                deep = PodiumBronzeDeep,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeeklyPodiumColumnV210(
    player: WeeklyPodiumPlayer,
    rank: Int,
    slabHeight: Dp,
    light: Color,
    mid: Color,
    deep: Color,
    crowned: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val topPad = if (crowned) 40.dp else 26.dp
    val profile = player.profile
    val displayName = player.row.displayName.ifBlank { sh("Oyuncu", "Player") }

    Box(modifier = modifier.height(slabHeight + topPad)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(slabHeight)
                .then(
                    if (crowned) Modifier.shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        ambientColor = PodiumGold,
                        spotColor = PodiumGold,
                    ) else Modifier,
                )
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            light.copy(alpha = 0.38f),
                            mid.copy(alpha = 0.20f),
                            deep.copy(alpha = 0.34f),
                        ),
                    ),
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(light.copy(alpha = 0.85f), deep.copy(alpha = 0.25f)),
                        ),
                    ),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(slabHeight * 0.4f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 30.dp, start = 4.dp, end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2B3B33),
                    border = BorderStroke(2.5.dp, mid),
                ) {
                    Box(
                        Modifier.size(if (crowned) 60.dp else 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProfilePhotoAvatarWithGender(
                            avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath,
                            gender = profile?.gender,
                            name = displayName,
                            size = if (crowned) 60.dp else 48.dp,
                            accent = mid,
                            visible = profile?.avatarVisibility != "hidden",
                            showGenderBadge = false,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    displayName,
                    color = PodiumTextMain,
                    fontSize = if (crowned) 15.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "${player.row.rating} RP",
                    color = if (crowned) PodiumGoldLight else PodiumTextMain.copy(alpha = 0.85f),
                    fontSize = if (crowned) 16.sp else 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        if (crowned) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Brush.verticalGradient(listOf(PodiumGold, PodiumGoldDeep))),
            )
        }

        WeeklyRankShieldV210(
            rank = rank,
            light = light,
            mid = mid,
            deep = deep,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = if (crowned) 18.dp else 4.dp),
        )

        if (crowned) {
            Text("👑", fontSize = 26.sp, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun WeeklyRankShieldV210(
    rank: Int,
    light: Color,
    mid: Color,
    deep: Color,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 15.dp, bottomEnd = 15.dp)
    Box(
        modifier = modifier
            .size(width = 30.dp, height = 34.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(light, mid, deep)))
            .border(1.dp, Color.White.copy(alpha = 0.6f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$rank", color = Color(0xFF26221A), fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WeeklyChampionPlaqueV210() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0x1AF2C14E),
            border = BorderStroke(1.dp, PodiumGold.copy(alpha = 0.6f)),
        ) {
            Text(
                sh("★  ŞAMPİYON  ★", "★  CHAMPION  ★"),
                color = PodiumGoldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 7.dp),
            )
        }
    }
}

private fun DrawScope.drawPodiumConfettiV210() {
    val rnd = Random(7)
    val palette = listOf(PodiumGold, PodiumGoldLight, Color(0xFFE8C87A), Color(0xFFFFF3C4))
    repeat(34) {
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat() * size.height * 0.75f
        val w = size.minDimension * 0.012f
        val h = w * (2f + rnd.nextFloat() * 2.5f)
        val c = palette[rnd.nextInt(palette.size)]
        rotate(degrees = rnd.nextFloat() * 360f, pivot = Offset(x, y)) {
            drawRect(
                color = c.copy(alpha = 0.25f + rnd.nextFloat() * 0.45f),
                topLeft = Offset(x, y),
                size = Size(w, h),
            )
        }
    }
}

private fun DrawScope.drawPodiumLaurelV210() {
    val w = size.width
    val h = size.height
    val stroke = w * 0.012f
    listOf(-1f, 1f).forEach { side ->
        val baseX = w / 2f + side * w * 0.34f
        val stem = Path().apply {
            moveTo(baseX, h * 0.95f)
            quadraticBezierTo(
                baseX - side * w * 0.16f,
                h * 0.55f,
                baseX - side * w * 0.06f,
                h * 0.12f,
            )
        }
        drawPath(stem, PodiumGold.copy(alpha = 0.55f), style = Stroke(stroke))
        repeat(7) { i ->
            val t = 0.12f + i * 0.115f
            val lx = baseX - side * w * (0.16f * sin(t * 3.1f))
            val ly = h * (0.95f - t * 0.82f)
            drawOval(
                color = PodiumGold.copy(alpha = 0.40f),
                topLeft = Offset(lx - side * w * 0.055f, ly - h * 0.035f),
                size = Size(w * 0.075f, h * 0.06f),
            )
        }
    }
}
