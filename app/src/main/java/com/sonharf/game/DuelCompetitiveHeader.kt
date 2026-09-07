package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HeadPurpleStart = Color(0xFF1A0033)
private val HeadPurpleMid = Color(0xFF4A006F)
private val HeadGold = Color(0xFFFFC84A)
private val HeadText = Color.White
private val HeadMuted = Color(0xFFC8BDD2)
private val IdentitySurface get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF111722) else Color.White
private val IdentityText get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFF5F7FC) else Color(0xFF172033)
private val IdentityMuted get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFAEB9C9) else Color(0xFF677386)
private val IdentityAccent get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFFF0B84D) else Color(0xFF238BFF)
private val IdentityBorder get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF536073) else Color(0xFFD4DCE7)

/**
 * Compact scoreboard that lives inside the arena layout rather than floating over it.
 * Score and countdown have one authoritative visual home, so profile cards can focus on identity.
 */
@Composable
internal fun DuelCompetitiveHeader(
    myScore: Int,
    opponentScore: Int,
    myStreak: Int,
    opponentStreak: Int,
    status: String,
    seconds: Int,
    language: String,
    modifier: Modifier = Modifier,
) {
    val tr = language == "tr"
    val delta = myScore - opponentScore
    val headline = when {
        status == "sudden_death" -> if (tr) "⚔ ANİ ÖLÜM" else "⚔ SUDDEN DEATH"
        kotlin.math.abs(delta) <= 2 -> if (tr) "⚡ BAŞ BAŞA" else "⚡ NECK & NECK"
        delta > 0 -> if (tr) "👑 ÖNDESİN" else "👑 YOU LEAD"
        else -> if (tr) "🔥 RAKİP ÖNDE" else "🔥 OPPONENT LEADS"
    }
    val timerValue = when (status) {
        "paused" -> "…"
        "finished" -> "✓"
        else -> seconds.toString().padStart(2, '0')
    }
    val timerLabel = when (status) {
        "paused" -> if (tr) "BAĞLANTI" else "LINK"
        "finished" -> if (tr) "BİTTİ" else "DONE"
        else -> if (tr) "SN" else "SEC"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(HeadPurpleStart, HeadPurpleMid, HeadPurpleStart),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreColumn(
                label = if (tr) "SEN" else "YOU",
                score = myScore,
                streak = myStreak,
                modifier = Modifier.weight(1f),
            )

            Column(
                modifier = Modifier.width(130.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    headline,
                    color = HeadGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        timerValue,
                        color = if (seconds in 1..3) Color(0xFFFF5964) else HeadText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        timerLabel,
                        color = HeadMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }

            ScoreColumn(
                label = if (tr) "RAKİP" else "OPPONENT",
                score = opponentScore,
                streak = opponentStreak,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScoreColumn(
    label: String,
    score: Int,
    streak: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = HeadMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            score.toString(),
            color = if (streak >= 3) HeadGold else HeadText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
        if (streak >= 3) {
            Text("🔥 $streak", color = HeadGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Profile-only card: no duplicate match score. */
@Composable
internal fun DuelIdentityCard(
    name: String,
    streak: Int,
    active: Boolean,
    avatarPath: String?,
    gender: String?,
    frameId: String?,
    modifier: Modifier = Modifier,
) {
    val border = if (active) IdentityAccent else IdentityBorder
    val background = if (active) IdentityAccent.copy(alpha = .09f) else IdentitySurface

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = background,
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, border),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FramedProfilePhotoAvatar(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = 40.dp,
                frameId = frameId,
                accent = if (active) IdentityAccent else IdentityBorder,
                showGenderBadge = false,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    name,
                    color = IdentityText,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (active) sh("SIRADA", "TURN") else if (streak >= 3) sh("🔥 SERİ $streak", "🔥 STREAK $streak") else sh("HAZIR", "READY"),
                    color = if (active) IdentityAccent else IdentityMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
