package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*

private val PolishedHomeCardShape = RoundedCornerShape(20.dp)
private val PolishedHeroShape = RoundedCornerShape(24.dp)

@Composable
internal fun PremiumHomeCommandDeckPolished(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onSocial: () -> Unit,
) {
    val homeBackend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var dailyDashboard by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var dailyPlayStreak by remember { mutableIntStateOf(0) }
    var dailyLoading by remember { mutableStateOf(homeBackend != null) }

    LaunchedEffect(homeBackend) {
        val backend = homeBackend
        if (backend == null) {
            dailyLoading = false
            return@LaunchedEffect
        }
        dailyDashboard = runCatching { backend.getGrowthDashboard() }.getOrNull()
        dailyPlayStreak = runCatching { backend.getMetaProgressV2().dailyPlayStreak }.getOrDefault(0)
        dailyLoading = false
    }

    val playerName = profile?.displayName ?: sh("Oyuncu", "Player")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PurchasedPanel(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile),
            asset = PurchasedUiAsset.PANEL_LARGE,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 17.dp),
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAvatarFrame(Modifier.size(76.dp)) {
                        FramedProfilePhotoAvatar(
                            avatarPath = profile?.avatarPath,
                            gender = profile?.gender,
                            name = playerName,
                            size = 56.dp,
                            frameId = SonHarfCosmetics.profileFrameId,
                            accent = if (profile?.isVip == true) SonHarfTheme.Purple else SonHarfTheme.Primary,
                            visible = profile?.avatarVisibility != "hidden",
                            isPro = profile?.isVip == true,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                profile?.displayName ?: sh("Profilin", "Your profile"),
                                modifier = Modifier.weight(1f, fill = false),
                                color = Color(0xFF4A2D20),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (profile?.isVip == true) {
                                Spacer(Modifier.width(6.dp))
                                PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(30.dp))
                            }
                        }
                        Text(
                            profile?.let { polishedLeagueName(ratingLeagueProgress(it.rating).leagueName) }
                                ?: sh("Lig bilgisi", "League status"),
                            color = Color(0xFF765746),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    PurchasedIconButton(
                        asset = PurchasedUiAsset.NAV_SOCIAL,
                        onClick = onSocial,
                        contentDescription = sh("Bildirimler ve davetler", "Notifications and invites"),
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PolishedHomeStatChip(
                        asset = PurchasedUiAsset.ICON_TROPHY,
                        value = profile?.let { "${it.rating} RP" } ?: "— RP",
                        label = sh("Derece", "Rating"),
                        modifier = Modifier.weight(1f),
                    )
                    PolishedHomeStatChip(
                        asset = PurchasedUiAsset.ICON_COIN,
                        value = "${profile?.diamonds?.toString() ?: "—"} Coin",
                        label = sh("Bakiye", "Balance"),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        PurchasedPanel(
            modifier = Modifier.fillMaxWidth().heightIn(min = 230.dp),
            asset = PurchasedUiAsset.PANEL_LARGE,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 19.dp),
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.ICON_SWORDS, Modifier.size(54.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sh("ANA ARENA", "MAIN ARENA"), color = Color(0xFF7D4BB2), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                        Text("KELİME KUŞATMASI", color = Color(0xFF4A2D20), fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text(sh("Kelime oyunu + taktik alan savaşı", "Word game + tactical territory battle"), color = Color(0xFF765746), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sh("Kelimeyi kur. Alanı ele geçir. Rakibini geç.", "Build your word. Claim territory. Outplay your rival."),
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF654A3D),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(R.drawable.word_siege_home_badge),
                        contentDescription = sh("Kelime Kuşatması", "Kelime Kuşatması"),
                        modifier = Modifier.size(width = 102.dp, height = 76.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                PurchasedButton(
                    text = sh("HEMEN OYNA", "PLAY NOW"),
                    onClick = onSiege,
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.PRIMARY,
                    leadingAsset = PurchasedUiAsset.ICON_SWORDS,
                )
            }
        }

        PolishedDailyTasksStrip(dailyDashboard, dailyPlayStreak, dailyLoading)
    }
}

@Composable
private fun PolishedHomeStatChip(
    asset: PurchasedUiAsset,
    value: String,
    label: String,
    modifier: Modifier,
) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 64.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            PurchasedAsset(asset, Modifier.size(32.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(value, color = Color(0xFF4A2D20), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(label, color = Color(0xFF765746), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PolishedDailyTasksStrip(dashboard: GrowthDashboardDto?, streakDays: Int, loading: Boolean) {
    val matches = dashboard?.matchesToday?.coerceIn(0, 3) ?: 0
    val checkInDone = dashboard?.dailyClaimed == true
    val challengeDone = dashboard?.dailyChallengeClaimed == true || matches >= 3
    val completedTasks = (if (checkInDone) 1 else 0) + (if (challengeDone) 1 else 0)
    val progress = if (dashboard == null) 0f else (((if (checkInDone) 1f else 0f) + matches / 3f) / 2f).coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()

    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PurchasedAsset(PurchasedUiAsset.ICON_GIFT, Modifier.size(42.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sh("GÜNLÜK GÖREVLER", "DAILY TASKS"), color = Color(0xFF4A2D20), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(if (loading) "…" else "$percent%", color = Color(0xFF6B3CA6), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                PurchasedProgress(progress, Modifier.fillMaxWidth())
                Text(
                    if (loading) sh("Görevler yükleniyor", "Loading tasks") else sh("$completedTasks / 2 görev tamamlandı", "$completedTasks / 2 tasks completed"),
                    color = Color(0xFF765746),
                    fontSize = 9.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(32.dp))
                Text(sh("$streakDays gün", "$streakDays days"), color = Color(0xFF4A2D20), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun PremiumWeeklyBestPolished(onClick: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var players by remember { mutableStateOf<List<WeeklyTopPlayerV210>>(emptyList()) }
    var loading by remember { mutableStateOf(backend != null) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(backend, reloadKey) {
        val activeBackend = backend
        if (activeBackend == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        runCatching { activeBackend.getWeeklyTopV210(limit = 3) }
            .onSuccess { players = it.take(3) }
            .onFailure { failed = true }
        loading = false
    }

    PurchasedPanel(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.ICON_RANKING, Modifier.size(42.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("HAFTANIN EN İYİLERİ", "WEEKLY BEST"), color = Color(0xFF4A2D20), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(sh("Haftalık rekabet sıralaması", "Weekly competition ranking"), color = Color(0xFF765746), fontSize = 8.sp)
                }
                PurchasedAsset(PurchasedUiAsset.ICON_TROPHY, Modifier.size(34.dp))
            }

            when {
                loading -> Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
                }
                failed -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sh("Haftalık sıralama yenilenemedi.", "Weekly ranking could not refresh."), color = Color(0xFF765746), fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    PurchasedButton(
                        text = sh("YENİLE", "RETRY"),
                        onClick = { reloadKey += 1 },
                        modifier = Modifier.width(140.dp),
                        style = PurchasedButtonStyle.SECONDARY,
                        leadingAsset = PurchasedUiAsset.ICON_REPEAT,
                    )
                }
                else -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    listOf(2 to players.getOrNull(1), 1 to players.getOrNull(0), 3 to players.getOrNull(2)).forEach { (place, player) ->
                        val first = place == 1
                        val podiumAsset = when (place) {
                            1 -> PurchasedUiAsset.PODIUM_1
                            2 -> PurchasedUiAsset.PODIUM_2
                            else -> PurchasedUiAsset.PODIUM_3
                        }
                        PurchasedPanel(
                            modifier = Modifier.weight(1f).then(if (first) Modifier.padding(bottom = 4.dp) else Modifier),
                            asset = PurchasedUiAsset.PANEL_SMALL,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = if (first) 12.dp else 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PurchasedAsset(podiumAsset, Modifier.size(width = 34.dp, height = 54.dp))
                                Spacer(Modifier.height(3.dp))
                                if (player != null) {
                                    ProfilePhotoAvatarWithGender(
                                        avatarPath = player.avatarUrl,
                                        gender = null,
                                        name = player.username,
                                        size = if (first) 58.dp else 50.dp,
                                        accent = if (first) SonHarfTheme.Purple else SonHarfTheme.Primary,
                                        visible = true,
                                    )
                                } else {
                                    PurchasedAvatarFrame(Modifier.size(if (first) 58.dp else 50.dp)) {}
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: "—", color = Color(0xFF4A2D20), fontSize = if (first) 10.sp else 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(player?.let { "${it.rp} RP" } ?: "— RP", color = Color(0xFF6B3CA6), fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun polishedLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
