package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ClubDirectoryRowDto
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.UnifiedMissionDto
import com.sonharf.game.data.WeeklyTournamentLeaderboardRowDto

@Composable
internal fun WeeklyClubPodiumCard(clubs: List<ClubDirectoryRowDto>) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("HAFTANIN EN İYİ 3 KULÜBÜ", color = PortalText, fontSize = 13.sp, fontWeight = FontWeight.Black)
            if (clubs.isEmpty()) {
                Text("Bu haftanın kulüp sıralaması oluşuyor.", color = PortalMuted, fontSize = 10.sp)
            } else {
                clubs.take(3).forEachIndexed { index, club ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (index) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" },
                            fontSize = 21.sp,
                            modifier = Modifier.width(32.dp),
                        )
                        ClubGeneratedEmblem(club, 40.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(club.name, color = PortalText, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                            Text("[${club.tag}] • ${club.memberCount}/${club.maxMembers} oyuncu", color = PortalMuted, fontSize = 9.sp)
                        }
                        Text("${club.weeklyPoints}", color = PortalGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun WeeklyPlayerPodiumCard(
    players: List<WeeklyTournamentLeaderboardRowDto>,
    profiles: Map<String, ProfileDto?>,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("HAFTANIN EN İYİ 3 OYUNCUSU", color = PortalText, fontSize = 13.sp, fontWeight = FontWeight.Black)
            if (players.isEmpty()) {
                Text("Bu haftanın oyuncu sıralaması oluşuyor.", color = PortalMuted, fontSize = 10.sp)
            } else {
                players.take(3).forEachIndexed { index, player ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (index) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" },
                            fontSize = 21.sp,
                            modifier = Modifier.width(32.dp),
                        )
                        ProfilePhotoAvatar(
                            avatarPath = profiles[player.userId]?.avatarPath,
                            name = player.displayName,
                            size = 40.dp,
                            visible = true,
                            accent = PortalBlue,
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(player.displayName, color = PortalText, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                            Text("${player.leagueName} • ${player.rating} rating • ${player.wins} galibiyet", color = PortalMuted, fontSize = 9.sp)
                        }
                        Text("${player.points}", color = PortalBlue, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun UnifiedRouteCard(
    mission: UnifiedMissionDto?,
    onOpenMode: (String) -> Unit,
) {
    if (mission == null) return
    val progress = (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = mission.modeKey != "route") {
            onOpenMode(mission.modeKey)
        },
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalBlue.copy(alpha = .25f)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BUGÜNÜN ROTASI", color = PortalBlue, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(if (SonHarfUiState.isEnglish) mission.titleEn else mission.titleTr, color = PortalText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Text("+${mission.rewardCoins} ◆", color = PortalGold, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = if (mission.completed) PortalGreen else PortalBlue,
                trackColor = Color(0xFFEAF0F7),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${mission.progress.coerceAtMost(mission.target)}/${mission.target}", color = PortalMuted, fontSize = 9.sp)
                Text(
                    when {
                        mission.claimed -> "ÖDÜL ALINDI"
                        mission.completed -> "ÖDÜL HAZIR"
                        mission.modeKey == "route" -> "ROTAYI TAMAMLA"
                        else -> "OYNA →"
                    },
                    color = if (mission.completed) PortalGreen else PortalBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
internal fun UnifiedMissionCard(
    mission: UnifiedMissionDto,
    busy: Boolean,
    onPlay: (String) -> Unit,
    onClaim: () -> Unit,
) {
    val progress = (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(17.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, if (mission.completed) PortalGreen.copy(alpha = .28f) else Color(0xFFDDE5EE)),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (mission.scope == "daily") "GÜNLÜK" else "HAFTALIK",
                    color = if (mission.scope == "daily") PortalBlue else PortalGold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (SonHarfUiState.isEnglish) mission.titleEn else mission.titleTr,
                    modifier = Modifier.weight(1f),
                    color = PortalText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("+${mission.rewardCoins} ◆", color = PortalGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (mission.completed) PortalGreen else PortalBlue,
                trackColor = Color(0xFFEAF0F7),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${mission.progress.coerceAtMost(mission.target)}/${mission.target}", color = PortalMuted, fontSize = 9.sp, modifier = Modifier.weight(1f))
                when {
                    mission.claimed -> Text("ALINDI ✓", color = PortalGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    mission.completed -> Button(
                        onClick = onClaim,
                        enabled = !busy,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PortalGreen),
                    ) { Text("ÖDÜLÜ AL", fontSize = 9.sp, fontWeight = FontWeight.Black) }
                    mission.modeKey != "route" -> TextButton(
                        onClick = { onPlay(mission.modeKey) },
                        enabled = !busy,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text("OYNA", fontSize = 9.sp, fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun ClubGeneratedEmblem(club: ClubDirectoryRowDto, size: androidx.compose.ui.unit.Dp) {
    val accent = when ((club.clubId.hashCode() and 3)) {
        0 -> PortalBlue
        1 -> PortalGold
        2 -> PortalGreen
        else -> Color(0xFF6B4FD3)
    }
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = accent.copy(alpha = .12f),
        border = BorderStroke(2.dp, accent.copy(alpha = .55f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("♜", color = accent, fontSize = 13.sp, lineHeight = 13.sp)
                Text(club.tag.take(3).uppercase(), color = PortalText, fontSize = 7.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        }
    }
}
