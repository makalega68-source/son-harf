package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
internal fun CompetitionVsCard(
    myName: String,
    opponentName: String,
    myAvatarPath: String?,
    opponentAvatarPath: String?,
    myGender: String? = null,
    opponentGender: String? = null,
    myRating: Int? = null,
    opponentRating: Int? = null,
    myStreak: Int = 0,
    opponentStreak: Int = 0,
    centerText: String = "VS",
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = myAvatarPath,
                    gender = myGender,
                    name = myName,
                    size = 38.dp,
                    accent = PortalBlue,
                )
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(myName, color = PortalText, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(
                        buildString {
                            if (myRating != null) append("${ratingLeagueProgress(myRating).leagueName} • ${myRating}")
                            if (myStreak > 0) {
                                if (isNotEmpty()) append(" • ")
                                append("🔥${myStreak}")
                            }
                        }.ifBlank { "SEN" },
                        color = PortalMuted,
                        fontSize = 8.sp,
                        maxLines = 1,
                    )
                }
            }
            Text(centerText, color = PortalGold, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(opponentName, color = PortalText, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(
                        buildString {
                            if (opponentRating != null) append("${ratingLeagueProgress(opponentRating).leagueName} • ${opponentRating}")
                            if (opponentStreak > 0) {
                                if (isNotEmpty()) append(" • ")
                                append("🔥${opponentStreak}")
                            }
                        }.ifBlank { "RAKİP" },
                        color = PortalMuted,
                        fontSize = 8.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(7.dp))
                ProfilePhotoAvatarWithGender(
                    avatarPath = opponentAvatarPath,
                    gender = opponentGender,
                    name = opponentName,
                    size = 38.dp,
                    accent = PortalPink,
                )
            }
        }
    }
}

@Composable
internal fun CompetitionLeadStrip(
    myScore: Int,
    opponentScore: Int,
    myStreak: Int = 0,
    opponentStreak: Int = 0,
    myAction: String? = null,
    opponentAction: String? = null,
    modifier: Modifier = Modifier,
) {
    val leader = when {
        myScore > opponentScore -> 1
        myScore < opponentScore -> -1
        else -> 0
    }
    var previousLeader by remember { mutableIntStateOf(leader) }
    var announcement by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(myScore, opponentScore) {
        if (leader != 0 && leader != previousLeader) {
            announcement = if (leader > 0) "ÖNE GEÇTİN!" else "RAKİP ÖNE GEÇTİ!"
            SonHarfSoundFx.leadChange()
            delay(1050)
            announcement = null
        } else if (leader == 0 && previousLeader != 0) {
            announcement = "SKOR EŞİTLENDİ!"
            SonHarfSoundFx.scoreTick()
            delay(850)
            announcement = null
        }
        previousLeader = leader
    }

    val total = (myScore.coerceAtLeast(0) + opponentScore.coerceAtLeast(0))
    val progress = if (total <= 0) .5f else (myScore.coerceAtLeast(0).toFloat() / total).coerceIn(.06f, .94f)
    val diff = abs(myScore - opponentScore)
    val critical = when {
        leader < 0 && diff <= 5 -> "Rakibi yakalamana ${diff.coerceAtLeast(1)} puan"
        leader > 0 && diff <= 5 -> "Üstünlüğü koru • fark ${diff.coerceAtLeast(1)}"
        leader == 0 -> "Şimdi öne geçme zamanı"
        else -> null
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SEN  ${myScore}", color = PortalBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(
                when {
                    myStreak >= 2 -> "🔥 ${myStreak} seri"
                    opponentStreak >= 2 -> "Rakip 🔥${opponentStreak}"
                    else -> critical.orEmpty()
                },
                color = if (opponentStreak >= 2) PortalPink else PortalMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text("${opponentScore}  RAKİP", color = PortalPink, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = PortalBlue,
            trackColor = PortalPink.copy(alpha = .22f),
        )
        val action = myAction ?: opponentAction
        if (!action.isNullOrBlank()) {
            Text(action, modifier = Modifier.fillMaxWidth(), color = PortalMuted, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
        AnimatedVisibility(visible = announcement != null, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (leader >= 0) PortalBlue.copy(alpha = .10f) else PortalPink.copy(alpha = .10f),
            ) {
                Text(
                    announcement.orEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    color = if (leader >= 0) PortalBlue else PortalPink,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
internal fun CompetitionMatchIntro(
    key: Any,
    myName: String,
    opponentName: String,
    myAvatarPath: String?,
    opponentAvatarPath: String?,
    myGender: String? = null,
    opponentGender: String? = null,
    myRating: Int? = null,
    opponentRating: Int? = null,
) {
    var visible by remember(key) { mutableStateOf(true) }
    LaunchedEffect(key) {
        SonHarfSoundFx.softNotify()
        delay(1250)
        visible = false
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFF8FBFF),
            border = BorderStroke(2.dp, PortalGold.copy(alpha = .38f)),
            shadowElevation = 5.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3  •  2  •  1  •  BAŞLA", color = PortalGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                CompetitionVsCard(
                    myName = myName,
                    opponentName = opponentName,
                    myAvatarPath = myAvatarPath,
                    opponentAvatarPath = opponentAvatarPath,
                    myGender = myGender,
                    opponentGender = opponentGender,
                    myRating = myRating,
                    opponentRating = opponentRating,
                )
                Text("KELİMEYİ SÜRDÜR • RAKİBİNİ GEÇ", color = PortalMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
internal fun ModeEntryOverlay(
    key: Any,
    title: String,
    subtitle: String,
    competitive: Boolean = true,
) {
    var visible by remember(key) { mutableStateOf(true) }
    LaunchedEffect(key) {
        SonHarfSoundFx.softNotify()
        delay(980)
        visible = false
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = .84f),
            exit = fadeOut() + scaleOut(targetScale = 1.08f),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xF8FFFFFF),
                border = BorderStroke(2.dp, if (competitive) PortalGold.copy(alpha = .55f) else PortalBlue.copy(alpha = .45f)),
                shadowElevation = 12.dp,
            ) {
                Column(
                    Modifier.widthIn(min = 250.dp, max = 330.dp).padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(if (competitive) "⚔  VS  ⚔" else "⚡  MEYDAN OKUMA", color = if (competitive) PortalGold else PortalBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(title, color = PortalText, fontSize = 23.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(subtitle, color = PortalMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("3  •  2  •  1  •  BAŞLA", color = PortalBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
