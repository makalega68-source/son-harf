package com.sonharf.game

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val ShowcaseGold = Color(0xFFF5C85A)
private val ShowcaseDeepGreen = Color(0xFF073B32)
private val ShowcaseSilver = Color(0xFFD9E5EE)
private val ShowcaseBronze = Color(0xFFE1A277)

@Composable
internal fun PremiumWeeklyPodiumShowcase(
    players: List<HomePodiumEntry>,
    loading: Boolean,
    failed: Boolean,
    onOpenLeague: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(
        onClick = onOpenLeague,
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, ShowcaseGold.copy(alpha = .58f)),
        shadowElevation = 4.dp,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(22.dp)),
        ) {
            val cardWidth = maxWidth
            val cardHeight = maxHeight

            Image(
                painter = painterResource(R.drawable.weekly_podium_showcase),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Mask the baked header copy and render one clean live title.
            Box(
                Modifier.offset(x = cardWidth * .13f, y = cardHeight * .035f)
                    .width(cardWidth * .56f)
                    .height(cardHeight * .205f)
                    .background(ShowcaseDeepGreen.copy(alpha = .97f), RoundedCornerShape(9.dp))
            )
            Text(
                text = sh("HAFTANIN ZİRVESİ", "WEEKLY ELITE"),
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.offset(x = cardWidth * .145f, y = cardHeight * .075f)
                        .width(cardWidth * .52f),
            )

            // The artwork already contains a decorative pill. Cover it first so only one live button remains.
            Box(
                Modifier.offset(x = cardWidth * .75f, y = cardHeight * .035f)
                    .width(cardWidth * .23f)
                    .height(cardHeight * .19f)
                    .background(ShowcaseDeepGreen.copy(alpha = .98f), RoundedCornerShape(100.dp))
            )
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF0B5146),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .42f)),
                modifier =
                    Modifier.offset(x = cardWidth * .79f, y = cardHeight * .07f)
                        .width(cardWidth * .17f)
                        .height(cardHeight * .12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        sh("TÜMÜ ›", "ALL ›"),
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            ShowcasePlayer(
                player = players.getOrNull(1),
                accent = ShowcaseSilver,
                centerX = cardWidth * .18f,
                avatarTop = cardHeight * .395f,
                avatarSize = cardWidth * .13f,
                nameTop = cardHeight * .69f,
                scoreTop = cardHeight * .79f,
                nameWidth = cardWidth * .22f,
                champion = false,
            )
            ShowcasePlayer(
                player = players.getOrNull(0),
                accent = ShowcaseGold,
                centerX = cardWidth * .50f,
                avatarTop = cardHeight * .275f,
                avatarSize = cardWidth * .16f,
                nameTop = cardHeight * .62f,
                scoreTop = cardHeight * .73f,
                nameWidth = cardWidth * .27f,
                champion = true,
            )
            ShowcasePlayer(
                player = players.getOrNull(2),
                accent = ShowcaseBronze,
                centerX = cardWidth * .82f,
                avatarTop = cardHeight * .395f,
                avatarSize = cardWidth * .13f,
                nameTop = cardHeight * .69f,
                scoreTop = cardHeight * .79f,
                nameWidth = cardWidth * .22f,
                champion = false,
            )

            if (loading) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = ShowcaseGold,
                        strokeWidth = 2.dp,
                    )
                }
            } else if (failed) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .46f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ShowcaseDeepGreen.copy(alpha = .96f),
                        border = BorderStroke(1.dp, ShowcaseGold.copy(alpha = .5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                sh("Sıralama yenilenemedi", "Ranking could not refresh"),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            TextButton(onClick = onRetry) {
                                Text(
                                    sh("YENİLE", "RETRY"),
                                    color = ShowcaseGold,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowcasePlayer(
    player: HomePodiumEntry?,
    accent: Color,
    centerX: Dp,
    avatarTop: Dp,
    avatarSize: Dp,
    nameTop: Dp,
    scoreTop: Dp,
    nameWidth: Dp,
    champion: Boolean,
) {
    val name = player?.row?.displayName?.ifBlank { sh("Oyuncu", "Player") } ?: "—"

    Box(
        modifier =
            Modifier.offset(x = centerX - avatarSize / 2f, y = avatarTop)
                .size(avatarSize),
        contentAlignment = Alignment.Center,
    ) {
        ShowcaseFramelessAvatar(
            avatarPath = if (player?.profile?.avatarVisibility == "hidden") null else player?.profile?.avatarPath,
            visible = player != null && player.profile?.avatarVisibility != "hidden",
            size = avatarSize,
        )
    }

    Text(
        text = name,
        color = Color.White,
        fontSize = if (champion) 14.sp else 12.sp,
        lineHeight = if (champion) 15.sp else 13.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.offset(x = centerX - nameWidth / 2f, y = nameTop)
                .width(nameWidth),
    )
    Text(
        text = player?.let { "${it.row.rating} RP" } ?: "— RP",
        color = accent,
        fontSize = if (champion) 14.sp else 12.sp,
        lineHeight = if (champion) 15.sp else 13.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier =
            Modifier.offset(x = centerX - nameWidth / 2f, y = scoreTop)
                .width(nameWidth),
    )
}

@Composable
private fun ShowcaseFramelessAvatar(
    avatarPath: String?,
    visible: Boolean,
    size: Dp,
) {
    var bytes by remember(avatarPath) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(avatarPath, visible) {
        bytes = if (visible && !avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
    }
    val bitmap = remember(bytes) {
        bytes?.let { data -> runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull() }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier =
                Modifier.size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF3B4945)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Face,
                contentDescription = null,
                tint = Color.White.copy(alpha = .72f),
                modifier = Modifier.size(size * .62f),
            )
        }
    }
}
