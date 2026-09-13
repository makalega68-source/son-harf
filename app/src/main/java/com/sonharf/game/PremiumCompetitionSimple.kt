package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.getLeaderboardV2

/**
 * One competition surface. League, rating and weekly ranking live here together; there are no
 * separate League / Competition Center / ranking destinations for the player to discover.
 */
@Composable
fun PremiumCompetitionScreen(
    backend: OnlineGameBackend,
) {
    val language = if (SonHarfUiState.language == "en") "en" else "tr"
    val me = remember { backend.currentUserId() }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var rows by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(language, me) {
        loading = true
        failed = false
        profile = me?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
        runCatching { backend.getLeaderboardV2(language, "week", 30) }
            .onSuccess { rows = it }
            .onFailure {
                rows = emptyList()
                failed = true
            }
        runCatching { backend.logEvent("competition_simple_open", language) }
        loading = false
    }

    val rating = profile?.rating ?: rows.firstOrNull { it.userId == me }?.rating ?: 1000
    val league = ratingLeagueProgress(rating)
    val myRank = rows.indexOfFirst { it.userId == me }.let { if (it >= 0) it + 1 else null }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SonHarfTheme.Background,
                        SonHarfTheme.SurfaceSecondary.copy(alpha = .70f),
                        SonHarfTheme.Background,
                    )
                )
            ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                sh("REKABET", "COMPETE"),
                color = SonHarfTheme.TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                sh(
                    "Ligin, puanın ve haftalık sıralaman tek yerde.",
                    "Your league, rating and weekly ranking in one place.",
                ),
                color = SonHarfTheme.TextSecondary,
                fontSize = 11.sp,
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = SonHarfTheme.Surface,
                border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .24f)),
                shadowElevation = 4.dp,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    SonHarfTheme.Primary.copy(alpha = .16f),
                                    SonHarfTheme.Surface,
                                    SonHarfTheme.Turquoise.copy(alpha = .09f),
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SonHarfTheme.Primary.copy(alpha = .14f),
                            ) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = SonHarfTheme.Primary,
                                    modifier = Modifier.padding(13.dp).size(27.dp),
                                )
                            }
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (SonHarfUiState.isEnglish) "${league.leagueName} LEAGUE" else "${league.leagueName} LİGİ",
                                    color = SonHarfTheme.TextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    "$rating ${sh("PUAN", "RATING")}",
                                    color = SonHarfTheme.Primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    myRank?.let { "#$it" } ?: "—",
                                    color = SonHarfTheme.TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    sh("BU HAFTA", "THIS WEEK"),
                                    color = SonHarfTheme.TextSecondary,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { league.progress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = SonHarfTheme.Primary,
                            trackColor = SonHarfTheme.SurfaceSecondary,
                        )

                        Text(
                            if (league.nextAt == null) {
                                sh("En üst ligdesin.", "You are in the top league.")
                            } else {
                                sh(
                                    "Sonraki lige ${league.pointsToNext} puan kaldı.",
                                    "${league.pointsToNext} rating to the next league.",
                                )
                            },
                            color = SonHarfTheme.TextSecondary,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = SonHarfTheme.Primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("HAFTALIK SIRALAMA", "WEEKLY RANKING"),
                        color = SonHarfTheme.TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                    )
                    Text(
                        sh("Pazartesi yenilenir", "Resets Monday"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 8.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SonHarfTheme.Primary.copy(alpha = .10f),
                ) {
                    Text(
                        language.uppercase(),
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = SonHarfTheme.Primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                    )
                }
            }
        }

        if (loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = SonHarfTheme.Primary,
                    trackColor = SonHarfTheme.SurfaceSecondary,
                )
            }
        }

        itemsIndexed(rows, key = { _, row -> row.userId }) { index, row ->
            val mine = row.userId == me
            val podium = index < 3
            val medal = when (index) {
                0 -> "1"
                1 -> "2"
                2 -> "3"
                else -> "${index + 1}"
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = if (mine) SonHarfTheme.Primary.copy(alpha = .10f) else SonHarfTheme.Surface,
                border = BorderStroke(
                    1.dp,
                    if (mine) SonHarfTheme.Primary.copy(alpha = .42f) else SonHarfTheme.Border,
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (podium) SonHarfTheme.Primary.copy(alpha = .12f) else SonHarfTheme.SurfaceSecondary,
                    ) {
                        Box(
                            Modifier.size(38.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (podium) {
                                Icon(
                                    Icons.Rounded.MilitaryTech,
                                    contentDescription = null,
                                    tint = SonHarfTheme.Primary.copy(alpha = .24f),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                medal,
                                color = SonHarfTheme.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.displayName,
                                color = SonHarfTheme.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (mine) FontWeight.Black else FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (mine) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    sh("SEN", "YOU"),
                                    color = SonHarfTheme.Primary,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                        Text(
                            row.leagueName,
                            color = SonHarfTheme.TextSecondary,
                            fontSize = 8.sp,
                            maxLines = 1,
                        )
                    }
                    Text(
                        row.rating.toString(),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        if (!loading && rows.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SonHarfTheme.Surface,
                    border = BorderStroke(1.dp, SonHarfTheme.Border),
                ) {
                    Text(
                        if (failed) {
                            sh("Sıralama şu anda yüklenemedi.", "Ranking could not be loaded right now.")
                        } else {
                            sh("Bu hafta sıralama henüz oluşmadı.", "No weekly ranking yet.")
                        },
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
