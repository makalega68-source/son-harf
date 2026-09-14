package com.sonharf.game

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WeeklyCardGreen = Color(0xFF0F5548)
private val WeeklyCardGreenDark = Color(0xFF0A4037)
private val WeeklyGold = Color(0xFFF2C85B)
private val WeeklySilver = Color(0xFFD6E0E5)
private val WeeklyBronze = Color(0xFFD99563)

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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WeeklyCardGreen,
        border = BorderStroke(1.dp, WeeklyGold.copy(alpha = .72f)),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = WeeklyGold,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = sh("HAFTANIN ZİRVESİ", "WEEKLY ELITE"),
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = onOpenLeague,
                    modifier = Modifier.height(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                ) {
                    Text(
                        text = sh("TÜMÜ", "ALL"),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(146.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = WeeklyGold,
                            strokeWidth = 2.5.dp,
                        )
                    }
                }

                failed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(146.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = sh("Sıralama yenilenemedi", "Ranking could not refresh"),
                            color = Color.White.copy(alpha = .86f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onRetry) {
                            Text(
                                text = sh("YENİLE", "RETRY"),
                                color = WeeklyGold,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        WeeklySimplePlayer(
                            place = 2,
                            player = players.getOrNull(1),
                            accent = WeeklySilver,
                            avatarSize = 68.dp,
                            modifier = Modifier.weight(1f),
                        )
                        WeeklySimplePlayer(
                            place = 1,
                            player = players.getOrNull(0),
                            accent = WeeklyGold,
                            avatarSize = 82.dp,
                            modifier = Modifier.weight(1f),
                            champion = true,
                        )
                        WeeklySimplePlayer(
                            place = 3,
                            player = players.getOrNull(2),
                            accent = WeeklyBronze,
                            avatarSize = 68.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySimplePlayer(
    place: Int,
    player: HomePodiumEntry?,
    accent: Color,
    avatarSize: Dp,
    modifier: Modifier,
    champion: Boolean = false,
) {
    val name = player?.row?.displayName?.ifBlank { sh("Oyuncu", "Player") } ?: "—"
    val avatarPath =
        if (player?.profile?.avatarVisibility == "hidden") null else player?.profile?.avatarPath
    val avatarVisible = player != null && player.profile?.avatarVisibility != "hidden"

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$place.",
            color = accent,
            fontSize = if (champion) 16.sp else 14.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(5.dp))
        WeeklyAvatarFrame(
            avatarPath = avatarPath,
            visible = avatarVisible,
            size = avatarSize,
            frameColor = accent,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = if (champion) 14.sp else 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = player?.let { "${it.row.rating} RP" } ?: "— RP",
            color = accent,
            fontSize = if (champion) 13.sp else 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WeeklyAvatarFrame(
    avatarPath: String?,
    visible: Boolean,
    size: Dp,
    frameColor: Color,
) {
    var bytes by remember(avatarPath) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(avatarPath, visible) {
        bytes = if (visible && !avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
    }
    val bitmap = remember(bytes) {
        bytes?.let { data ->
            runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull()
        }
    }

    // One frame only: the photo fills the inner circle edge-to-edge after a fixed 3dp ring.
    Box(
        modifier =
            Modifier.size(size)
                .background(frameColor, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(WeeklyCardGreenDark),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = .72f),
                modifier = Modifier.size(size * .52f),
            )
        }
    }
}
