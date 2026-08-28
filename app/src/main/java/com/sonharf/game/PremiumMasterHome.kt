package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GrowthDashboardDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.getAdminDashboard

private val ArenaHomeBg = Color(0xFFF7F9FC)
private val ArenaHomeSurface = Color.White
private val ArenaHomeSurface2 = Color(0xFFF0F4F8)
private val ArenaHomeText = Color(0xFF14213D)
private val ArenaHomeMuted = Color(0xFF718096)
private val ArenaHomeBlue = Color(0xFF1769E0)
private val ArenaHomeBlueDeep = Color(0xFF0D56C9)
private val ArenaHomeGold = Color(0xFFF3A81A)
private val ArenaHomeGreen = Color(0xFF22B95F)
private val ArenaHomeBorder = Color(0xFFDDE5EE)

@Composable
internal fun PremiumMasterHome(
    backend: OnlineGameBackend?,
    onPlay: () -> Unit,
    onQuickGame: () -> Unit,
    onBilBakalim: () -> Unit,
    onAdmin: () -> Unit,
    onHub: () -> Unit,
    onLeague: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
    onGoals: () -> Unit = onHub,
    onSeason: () -> Unit = onHub,
    onWardrobe: () -> Unit = onProfile,
    onNotifications: () -> Unit = onProfile,
    onDailyCipher: () -> Unit = onHub,
    onMastery: () -> Unit = onHub,
    onHistory: () -> Unit = onHub,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        isAdmin = if (backend == null) false else runCatching { backend.getAdminDashboard(); true }.getOrDefault(false)
        runCatching { backend?.logEvent("home_open_live_word_arena") }
    }

    val levelProgress = growth?.let {
        (it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
    } ?: 0f
    val streak = growth?.currentWinStreak ?: 0
    val playerName = profile?.displayName ?: sh("Oyuncu", "Player")
    val league = growth?.leagueName ?: sh("Başlangıç Ligi", "Starter League")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, ArenaHomeBg, Color(0xFFF2F6FC))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = ArenaHomeText, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text(
                        sh("Kelimeyi Sürdür, Rakibini Geç", "Keep the word going, beat your rival"),
                        color = ArenaHomeMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(onClick = onNotifications) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = ArenaHomeText)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile),
                shape = RoundedCornerShape(22.dp),
                color = ArenaHomeSurface,
                border = BorderStroke(1.dp, ArenaHomeBorder),
                shadowElevation = 4.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = profile?.avatarPath,
                        gender = profile?.gender,
                        name = playerName,
                        size = 54.dp,
                        accent = ArenaHomeBlue,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playerName, color = ArenaHomeText, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = ArenaHomeGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(league, color = ArenaHomeMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            sh("Seviye", "Level") + " " + (growth?.level ?: 1),
                            color = ArenaHomeBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(shape = RoundedCornerShape(99.dp), color = ArenaHomeBlue.copy(alpha = .08f)) {
                            Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Paid, null, tint = ArenaHomeGold, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${profile?.diamonds ?: 0} SC", color = ArenaHomeText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        if (isAdmin) {
                            TextButton(onClick = onAdmin, contentPadding = PaddingValues(0.dp)) {
                                Text("ADMIN", color = ArenaHomeMuted, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onQuickGame,
                modifier = Modifier.fillMaxWidth().height(112.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp, pressedElevation = 2.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(ArenaHomeBlue, ArenaHomeBlueDeep)),
                            RoundedCornerShape(24.dp),
                        )
                        .padding(horizontal = 22.dp),
                ) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        Text(sh("OYNA", "PLAY"), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text(sh("Rakibini Bul", "Find your rival"), color = Color.White.copy(alpha = .92f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(sh("Hızlı eşleşme", "Quick matchmaking"), color = Color.White.copy(alpha = .68f), fontSize = 10.sp)
                    }
                    Icon(
                        Icons.Rounded.ArrowForward,
                        null,
                        tint = Color.White.copy(alpha = .32f),
                        modifier = Modifier.align(Alignment.CenterEnd).size(62.dp),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ArenaHomeSurface,
                border = BorderStroke(1.dp, ArenaHomeBorder),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFFFF1E8)) {
                            Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFF97316), modifier = Modifier.padding(7.dp).size(22.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (streak > 0) "$streak " + sh("Maçlık Seri", "Win Streak") else sh("Seriyi Başlat", "Start a streak"),
                                color = ArenaHomeText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                sh("Bir sonraki seviyeye ilerle", "Progress toward the next level"),
                                color = ArenaHomeMuted,
                                fontSize = 9.sp,
                            )
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = ArenaHomeGold.copy(alpha = .12f)) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = ArenaHomeGold, modifier = Modifier.padding(8.dp).size(22.dp))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = ArenaHomeBlue,
                        trackColor = ArenaHomeSurface2,
                    )
                    Text(
                        "${growth?.levelProgress ?: 0} / ${growth?.levelTarget ?: 0} XP",
                        modifier = Modifier.align(Alignment.End),
                        color = ArenaHomeMuted,
                        fontSize = 8.sp,
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArenaHomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.History,
                    title = sh("Son Maçlar", "Recent Matches"),
                    subtitle = sh("Geçmişi incele", "Review history"),
                    accent = ArenaHomeBlue,
                    onClick = onHistory,
                )
                ArenaHomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.EmojiEvents,
                    title = sh("Lig", "League"),
                    subtitle = league,
                    accent = ArenaHomeGold,
                    onClick = onLeague,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArenaHomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.TrackChanges,
                    title = sh("Günlük Hedef", "Daily Goal"),
                    subtitle = if (growth?.dailyClaimed == true) sh("Bugün tamamlandı", "Done today") else sh("Hedefler hazır", "Goals ready"),
                    accent = ArenaHomeGreen,
                    onClick = onGoals,
                )
                ArenaHomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.MilitaryTech,
                    title = sh("Turnuva", "Tournament"),
                    subtitle = sh("Rekabeti aç", "Open competition"),
                    accent = Color(0xFF6B4FD3),
                    onClick = onSeason,
                )
            }

            Text(sh("DİĞER OYUNLAR", "OTHER GAMES"), color = ArenaHomeMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArenaMiniGameCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Psychology,
                    title = sh("Bil Bakalım", "Bil Bakalım"),
                    subtitle = sh("Bilgi düellosu", "Trivia duel"),
                    onClick = onBilBakalim,
                )
                ArenaMiniGameCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Search,
                    title = sh("Kelime Avı", "Word Hunt"),
                    subtitle = sh("Günün kelimesi", "Word of the day"),
                    onClick = onDailyCipher,
                )
            }

            OutlinedButton(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, ArenaHomeBlue.copy(alpha = .35f)),
            ) {
                Icon(Icons.Rounded.Tune, null, tint = ArenaHomeBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(sh("Oyun Modları ve Özel Oda", "Game Modes & Private Room"), color = ArenaHomeBlue, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ArenaHomeActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(96.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color = ArenaHomeSurface,
        border = BorderStroke(1.dp, ArenaHomeBorder),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(25.dp))
            Column {
                Text(title, color = ArenaHomeText, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(subtitle, color = ArenaHomeMuted, fontSize = 8.5.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ArenaMiniGameCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(78.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = ArenaHomeSurface,
        border = BorderStroke(1.dp, ArenaHomeBorder),
    ) {
        Row(Modifier.fillMaxSize().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(13.dp), color = ArenaHomeBlue.copy(alpha = .08f)) {
                Icon(icon, null, tint = ArenaHomeBlue, modifier = Modifier.padding(9.dp).size(23.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title, color = ArenaHomeText, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(subtitle, color = ArenaHomeMuted, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}
