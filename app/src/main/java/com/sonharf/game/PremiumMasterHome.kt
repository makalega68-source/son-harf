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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GrowthDashboardDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.getAdminDashboard

private val HomePanel = Color(0xD9142447)
private val HomePanelStrong = Color(0xEE0E1A36)
private val HomeBorder = Color(0xFF315B87)
private val HomeGold = LetharaPalette.Gold
private val HomeCyan = LetharaPalette.Cyan
private val HomeText = LetharaPalette.Text
private val HomeMuted = LetharaPalette.Muted

/**
 * Canonical mobile home for Son Harf.
 *
 * The painted background is deliberately UI-free. All buttons, labels and panels are
 * live Compose layers so they remain clickable, localizable and responsive.
 */
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
        runCatching { backend?.logEvent("home_open_wordgame_canonical") }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.son_harf_ana_menu_arka_plan),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x33030A18),
                        Color(0x16030A18),
                        Color(0x33030A18),
                        Color(0xCC030A18),
                    )
                )
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeTopBar(
                profile = profile,
                growth = growth,
                isAdmin = isAdmin,
                onProfile = onProfile,
                onAdmin = onAdmin,
            )

            Spacer(Modifier.height(4.dp))
            SonHarfBrandLogo(
                modifier = Modifier.fillMaxWidth(.68f).height(62.dp),
                size = null,
            )

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HomeSideAction(Icons.Rounded.CardGiftcard, sh("Etkinlik", "Events"), onSeason)
                    HomeSideAction(Icons.Rounded.AutoStories, sh("Haberler", "News"), onDailyCipher)
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 34.dp)
                        .widthIn(min = 102.dp, max = 132.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = HomePanelStrong.copy(alpha = .90f),
                    border = BorderStroke(1.dp, HomeCyan.copy(alpha = .70f)),
                    shadowElevation = 8.dp,
                ) {
                    Text(
                        text = sh(
                            "Yeni bir kelime zinciri hazır.\nBir maç daha?",
                            "A new word chain is ready.\nOne more match?",
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        color = HomeText,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(.82f).height(64.dp),
                shape = RoundedCornerShape(21.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFFE9A8)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD89A2B),
                    contentColor = Color(0xFF241607),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(7.dp))
                Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 25.sp, letterSpacing = 1.6.sp)
            }

            Text(
                text = "✦ " + sh("Kelimeyi sürdür, rakibini geç", "Keep the word going, beat your rival") + " ✦",
                modifier = Modifier.padding(top = 5.dp, bottom = 8.dp),
                color = HomeGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                Modifier.fillMaxWidth().height(116.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                HomeInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.EmojiEvents,
                    title = sh("Lig Sezonu", "League Season"),
                    subtitle = growth?.leagueName ?: sh("Başlangıç Ligi", "Starter League"),
                    accent = HomeGold,
                    onClick = onLeague,
                )
                HomeInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.TrackChanges,
                    title = sh("Günlük Görevler", "Daily Goals"),
                    subtitle = if (growth?.dailyClaimed == true) sh("Bugün tamamlandı", "Done today") else sh("Görevler hazır", "Goals ready"),
                    accent = HomeCyan,
                    onClick = onGoals,
                )
                HomeInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AutoStories,
                    title = sh("Maç\nGeçmişi", "Match\nHistory"),
                    subtitle = sh("Son maçlarını incele", "Review recent matches"),
                    accent = LetharaPalette.Violet,
                    onClick = onHistory,
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    profile: ProfileDto?,
    growth: GrowthDashboardDto?,
    isAdmin: Boolean,
    onProfile: () -> Unit,
    onAdmin: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clickable(onClick = onProfile)) {
            ProfilePhotoAvatarWithGender(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = profile?.displayName ?: sh("Oyuncu", "Player"),
                size = 46.dp,
                accent = HomeCyan,
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                sh("Oyuncu", "Player"),
                color = HomeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                sh("Seviye", "Level") + " " + (growth?.level ?: 1),
                color = HomeCyan,
                fontSize = 8.sp,
            )
        }
        HomeWallet(Icons.Rounded.Paid, (growth?.xp ?: 0).toString(), HomeGold)
        Spacer(Modifier.width(5.dp))
        HomeWallet(Icons.Rounded.Diamond, (profile?.diamonds ?: 0).toString(), HomeCyan)
        Spacer(Modifier.width(5.dp))
        Surface(
            onClick = if (isAdmin) onAdmin else onProfile,
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = HomePanelStrong,
            border = BorderStroke(1.dp, HomeBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = if (isAdmin) HomeGold else HomeText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeWallet(icon: ImageVector, value: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = HomePanelStrong,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, color = HomeText, fontWeight = FontWeight.Black, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HomeSideAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(64.dp),
        shape = RoundedCornerShape(15.dp),
        color = Color(0xB20C1934),
        border = BorderStroke(1.dp, HomeBorder.copy(alpha = .85f)),
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = HomeGold, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, color = HomeText, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HomeInfoCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = HomePanel),
        border = BorderStroke(1.dp, accent.copy(alpha = .58f)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(29.dp))
            Text(
                title,
                color = HomeText,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(
                subtitle,
                color = HomeMuted,
                fontSize = 7.5.sp,
                lineHeight = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
