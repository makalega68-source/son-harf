package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto

/**
 * Premium home intentionally keeps only three decisions visible: play the flagship game,
 * browse games, or check competition. The visual hierarchy is carried by the territory map,
 * not by extra dashboard cards.
 */
@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onGames: () -> Unit,
    onCompete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PremiumPlayerStrip(profile = profile, onClick = onProfile)
        PremiumSiegeHero(onPlay = onSiege)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            PremiumHomeDestination(
                icon = Icons.Rounded.SportsEsports,
                title = sh("OYUNLAR", "GAMES"),
                subtitle = sh("Son Harf ve Harf Yolu", "Last Letter and Letter Path"),
                modifier = Modifier.weight(1f),
                onClick = onGames,
            )
            PremiumHomeDestination(
                icon = Icons.Rounded.EmojiEvents,
                title = sh("REKABET", "COMPETE"),
                subtitle = sh("Lig ve haftalık sıra", "League and weekly rank"),
                modifier = Modifier.weight(1f),
                onClick = onCompete,
            )
        }
    }
}

@Composable
private fun PremiumPlayerStrip(profile: ProfileDto?, onClick: () -> Unit) {
    val rating = profile?.rating ?: 1000
    val league = ratingLeagueProgress(rating).leagueName

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FramedProfilePhotoAvatar(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = profile?.displayName ?: sh("Oyuncu", "Player"),
                size = 52.dp,
                frameId = SonHarfCosmetics.profileFrameId,
                accent = if (profile?.isVip == true) SonHarfTheme.PremiumGold else SonHarfTheme.Primary,
                visible = profile?.avatarVisibility != "hidden",
                showGenderBadge = false,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.displayName ?: sh("OYUNCU", "PLAYER"),
                    color = SonHarfCosmetics.playerNameColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "$league  •  $rating ${sh("PUAN", "RATING")}",
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (profile?.isVip == true) {
                Surface(shape = RoundedCornerShape(9.dp), color = SonHarfTheme.PremiumGold.copy(alpha = .13f)) {
                    Text(
                        "VIP",
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = SonHarfTheme.PremiumGold,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
        }
    }
}

@Composable
private fun PremiumSiegeHero(onPlay: () -> Unit) {
    val heroShape = RoundedCornerShape(32.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, heroShape),
        shape = heroShape,
        color = SonHarfTheme.ForestDeep,
        border = BorderStroke(1.dp, SonHarfTheme.PremiumGold.copy(alpha = .30f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            SonHarfTheme.ForestDeep,
                            SonHarfTheme.Forest,
                            SonHarfTheme.HeroMiddle,
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SonHarfTheme.PremiumGold.copy(alpha = .16f),
                        border = BorderStroke(1.dp, SonHarfTheme.PremiumGold.copy(alpha = .28f)),
                    ) {
                        Text(
                            sh("ANA OYUN", "MAIN GAME"),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            color = SonHarfTheme.PremiumGoldLight,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .10f)) {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(9.dp).size(20.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        sh("KELİME\nKUŞATMASI", "WORD\nSIEGE"),
                        color = Color.White,
                        fontSize = 31.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.4).sp,
                    )
                    Text(
                        sh(
                            "Kelime kur. Bölge kazan. Haritanın kontrolünü ele geçir.",
                            "Build words. Claim territory. Take control of the map.",
                        ),
                        color = Color.White.copy(alpha = .80f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                PremiumTerritoryPreview()

                Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonHarfTheme.PremiumGold,
                        contentColor = SonHarfTheme.ForestDeep,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumTerritoryPreview() {
    val cells = listOf(
        listOf(0, 0, 1, 1, 2, 2),
        listOf(0, 1, 1, 1, 2, 2),
        listOf(0, 0, 1, 2, 2, 2),
        listOf(0, 0, 0, 2, 2, 2),
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = .07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            cells.forEachIndexed { rowIndex, row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    row.forEachIndexed { columnIndex, owner ->
                        val isCritical = rowIndex == 1 && columnIndex == 3
                        val fill = when (owner) {
                            1 -> SonHarfTheme.Primary
                            2 -> SonHarfTheme.SoftBlue
                            else -> Color.White.copy(alpha = .16f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(25.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isCritical) SonHarfTheme.PremiumGold.copy(alpha = .92f)
                                    else fill.copy(alpha = if (owner == 0) .16f else .92f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCritical) {
                                Text("★", color = SonHarfTheme.ForestDeep, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumHomeDestination(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier
            .height(108.dp)
            .shadow(2.dp, shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = SonHarfTheme.Primary.copy(alpha = .10f),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = SonHarfTheme.Primary,
                        modifier = Modifier.padding(8.dp).size(19.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(19.dp))
            }
            Text(title, color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(
                subtitle,
                color = SonHarfTheme.TextSecondary,
                fontSize = 8.5.sp,
                lineHeight = 11.sp,
                maxLines = 2,
            )
        }
    }
}
