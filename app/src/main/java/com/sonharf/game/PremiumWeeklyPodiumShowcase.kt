package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
            Image(
                painter = painterResource(R.drawable.weekly_podium_showcase),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // The artwork carries the 3D podium, lighting, crown, confetti and metallic frames.
            // Header copy stays live so Turkish/English localization is never baked into gameplay UI.
            Box(
                Modifier.offset(x = maxWidth * .13f, y = maxHeight * .045f)
                    .width(maxWidth * .52f)
                    .height(maxHeight * .19f)
                    .background(ShowcaseDeepGreen.copy(alpha = .96f), RoundedCornerShape(8.dp))
            )
            Column(
                modifier =
                    Modifier.offset(x = maxWidth * .145f, y = maxHeight * .055f)
                        .width(maxWidth * .50f),
            ) {
                Text(
                    sh("HAFTANIN ZİRVESİ", "WEEKLY ELITE"),
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    sh("Bu haftanın en güçlü oyuncuları", "This week's strongest players"),
                    color = Color.White.copy(alpha = .82f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = ShowcaseDeepGreen.copy(alpha = .97f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .32f)),
                modifier =
                    Modifier.offset(x = maxWidth * .77f, y = maxHeight * .065f)
                        .width(maxWidth * .18f)
                        .height(maxHeight * .13f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        sh("TÜMÜ  ›", "ALL  ›"),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            ShowcasePlayer(
                place = 2,
                player = players.getOrNull(1),
                accent = ShowcaseSilver,
                x = maxWidth * .18f,
                avatarTop = maxHeight * .45f,
                avatarSize = maxWidth * .082f,
                nameTop = maxHeight * .665f,
                scoreTop = maxHeight * .755f,
                nameWidth = maxWidth * .19f,
            )
            ShowcasePlayer(
                place = 1,
                player = players.getOrNull(0),
                accent = ShowcaseGold,
                x = maxWidth * .50f,
                avatarTop = maxHeight * .335f,
                avatarSize = maxWidth * .105f,
                nameTop = maxHeight * .585f,
                scoreTop = maxHeight * .675f,
                nameWidth = maxWidth * .22f,
                champion = true,
            )
            ShowcasePlayer(
                place = 3,
                player = players.getOrNull(2),
                accent = ShowcaseBronze,
                x = maxWidth * .82f,
                avatarTop = maxHeight * .45f,
                avatarSize = maxWidth * .082f,
                nameTop = maxHeight * .665f,
                scoreTop = maxHeight * .755f,
                nameWidth = maxWidth * .19f,
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
private fun BoxWithConstraintsScope.ShowcasePlayer(
    place: Int,
    player: HomePodiumEntry?,
    accent: Color,
    x: Dp,
    avatarTop: Dp,
    avatarSize: Dp,
    nameTop: Dp,
    scoreTop: Dp,
    nameWidth: Dp,
    champion: Boolean = false,
) {
    val name = player?.row?.displayName?.ifBlank { sh("Oyuncu", "Player") } ?: "—"
    val description =
        player?.let {
            sh(
                "$place. sıra, $name, ${it.row.rating} haftalık RP",
                "Rank $place, $name, ${it.row.rating} weekly RP",
            )
        } ?: sh("$place. sıra henüz boş", "Rank $place is not filled yet")

    Box(
        modifier =
            Modifier.offset(x = x - avatarSize / 2f, y = avatarTop)
                .size(avatarSize)
                .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (player != null) {
            ProfilePhotoAvatarWithGender(
                avatarPath = if (player.profile?.avatarVisibility == "hidden") null else player.profile?.avatarPath,
                gender = player.profile?.gender,
                name = name,
                size = avatarSize,
                accent = accent,
                visible = player.profile?.avatarVisibility != "hidden",
                showGenderBadge = false,
            )
        }
    }

    Text(
        text = name,
        color = Color.White,
        fontSize = if (champion) 12.sp else 10.sp,
        lineHeight = if (champion) 13.sp else 11.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.offset(x = x - nameWidth / 2f, y = nameTop)
                .width(nameWidth),
    )
    Text(
        text = player?.let { "${it.row.rating} RP" } ?: "— RP",
        color = accent,
        fontSize = if (champion) 12.sp else 10.sp,
        lineHeight = if (champion) 13.sp else 11.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier =
            Modifier.offset(x = x - nameWidth / 2f, y = scoreTop)
                .width(nameWidth),
    )
}
