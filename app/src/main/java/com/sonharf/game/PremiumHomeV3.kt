package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay


private val HomeHeroShape = RoundedCornerShape(18.dp)
private val HomeCardShape = RoundedCornerShape(16.dp)
private val HomeControlShape = RoundedCornerShape(12.dp)
private val HomeMicroShape = RoundedCornerShape(10.dp)
private val HomeHairline = Color(0xFFD2DBE5)

@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onShop: () -> Unit,
    onPro: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("KELİME TAHTI", Modifier.weight(1f), color = Hf.Text,
                fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Settings, sh("Ayarlar", "Settings"), tint = Hf.TextMuted)
            }
        }
        HomeStatusRow(profile, onProfile, onShop)
    }
}

@Composable
private fun HomeStatusRow(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onShop: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clip(HomeControlShape)
            .clickable(onClickLabel = sh("Profili aç", "Open profile"), onClick = onProfile)
            .heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            FramedProfilePhotoAvatar(
                avatarPath = profile?.avatarPath, gender = profile?.gender,
                name = profile?.displayName ?: sh("Oyuncu", "Player"), size = 46.dp,
                frameId = rememberPlayerFrame(profile?.id), accent = Hf.Gold,
                visible = profile?.avatarVisibility != "hidden", isPro = profile?.isVip == true,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(profile?.displayName?.ifBlank { null } ?: sh("Oyuncu", "Player"),
                    color = Hf.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile?.let { homeLeagueName(ratingLeagueProgress(it.rating).leagueName) + sh(" Lig", " League") }
                    ?: sh("Profilim", "My profile"), color = Hf.TextMuted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        Surface(onClick = onShop, shape = HomeControlShape, color = Hf.Surface,
            border = BorderStroke(1.dp, Hf.Border)) {
            Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                HfCoin(20.dp)
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(profile?.diamonds?.let { homeGrouped(it) } ?: "—", color = Hf.Text,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Son Coin", color = Hf.TextMuted, fontSize = 10.sp)
                }
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Rounded.Add, sh("Mağazayı aç", "Open store"), tint = Hf.TextMuted,
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun homeGrouped(value: Long): String = String.format(java.util.Locale("tr", "TR"), "%,d", value)
private fun homeGrouped(value: Int): String = homeGrouped(value.toLong())

@Composable
internal fun HomeSiegeHero(onSiege: () -> Unit) {
    Surface(onClick = onSiege, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = Hf.GreenPressed, border = BorderStroke(1.dp, Hf.GreenLight.copy(alpha = .55f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HfGameArt(R.drawable.kelime_tahti_game_icon, 56.dp, 56.dp, description = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sh("Kelime Kuşatması", "Word Siege"), color = Color.White,
                        fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kelimeni kur, tahtayı ele geçir.", "Build words. Capture the board."),
                        color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                }
            }
            // One click target for the entire card, with a clear visual play affordance.
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(HomeControlShape)
                .background(Color(0xFFF3E8CF)).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF214D37), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(sh("OYNA", "PLAY"), color = Color(0xFF214D37), fontSize = 16.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
internal fun HomeGameRow(title: String, subtitle: String, art: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = HomeCardShape,
        color = Hf.Surface, border = BorderStroke(1.dp, Hf.Border)) {
        Row(Modifier.heightIn(min = 80.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            HfGameArt(art, 56.dp, 52.dp, description = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Hf.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Hf.TextMuted, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Hf.TextMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun HomeQuickMenu(isPro: Boolean, onPro: () -> Unit, onMascots: () -> Unit,
    onActivity: () -> Unit, onEvents: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeQuickAction(if (isPro) sh("PRO üyeliğim", "My PRO membership") else sh("PRO üyelik", "PRO membership"),
                Icons.Rounded.WorkspacePremium, SonHarfTheme.PremiumGold, Modifier.weight(1f).fillMaxHeight(), onPro)
            HomeQuickAction(sh("Maskotlar", "Mascots"), Icons.Rounded.Pets, Hf.Green, Modifier.weight(1f).fillMaxHeight(), onMascots)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onActivity, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.NotificationsNone, null, tint = Hf.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(sh("Aktivite", "Activity"), color = Hf.TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onEvents, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Event, null, tint = Hf.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(sh("Etkinlikler", "Events"), color = Hf.TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HomeQuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = HomeCardShape,
        color = Hf.Surface, border = BorderStroke(1.dp, Hf.Border)) {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Text(label, color = Hf.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun PremiumHomeDailyTasks(onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var dashboard by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var streakDays by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(backend != null) }
    LaunchedEffect(backend) {
        val active = backend
        if (active == null) {
            loading = false
            return@LaunchedEffect
        }
        dashboard = runCatching { active.getGrowthDashboard() }.getOrNull()
        streakDays = runCatching { active.getMetaProgressV2().dailyPlayStreak }.getOrDefault(0)
        ReminderNotifications.rememberStreak(context, streakDays)
        loading = false
    }
    val matches = dashboard?.matchesToday?.coerceIn(0, 3) ?: 0
    val checkInDone = dashboard?.dailyClaimed == true
    val challengeDone = dashboard?.dailyChallengeClaimed == true || matches >= 3
    val completedTasks = (if (checkInDone) 1 else 0) + (if (challengeDone) 1 else 0)
    val progress = if (dashboard == null) 0f else (((if (checkInDone) 1f else 0f) + matches / 3f) / 2f).coerceIn(0f, 1f)

    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = HomeCardShape,
        color = Hf.Surface, border = BorderStroke(1.dp, Hf.Border)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AssignmentTurnedIn, null, tint = Hf.Green, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("Günlük Görevler", "Daily Tasks"), Modifier.weight(1f), color = Hf.Text,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(if (loading || dashboard == null) "—" else "$completedTasks/2", color = Hf.TextMuted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(if (streakDays > 0) sh("$streakDays günlük seri", "$streakDays-day streak")
                    else sh("Bugünün ödüllerini keşfet", "Discover today's rewards"),
                    color = Hf.TextMuted, fontSize = 12.sp)
                LinearProgressIndicator(progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = Hf.Green, trackColor = SonHarfTheme.SurfaceSecondary)
            }
        }
    }
}



@Composable
internal fun PremiumLeagueProgress(profile: ProfileDto?, onLeague: () -> Unit) {
    val progress = profile?.let { ratingLeagueProgress(it.rating) }
    Surface(
        onClick = onLeague,
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.style_icon_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        progress?.let { homeLeagueName(it.leagueName) } ?: sh("Lig durumun", "Your league"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(profile?.let { "${it.rating} RP" } ?: "— RP", color = SonHarfTheme.TextSecondary, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ChevronRight, sh("Lige Git", "Go to League"), tint = SonHarfTheme.TextSecondary)
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = SonHarfTheme.Primary,
                    trackColor = SonHarfTheme.SurfaceElevated,
                )
            }
        }
    }
}

@Composable
internal fun PremiumOtherGames(onLastLetter: () -> Unit, onWorkshop: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeGameRow(sh("Son Harf", "Last Letter"), sh("1v1 kelime düellosu", "1v1 word duel"), R.drawable.son_harf_game_icon, onLastLetter)
        HomeTournamentCard(onOpen = onWorkshop, compact = true)
    }
}

@Composable
private fun PremiumHomeModeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    art: @Composable () -> Unit,
    colors: List<Color>,
    onPlay: () -> Unit,
) {
    HfGamePanel(colors, modifier.heightIn(min = 214.dp), onClick = onPlay) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(96.dp).background(Brush.radialGradient(listOf(Color.White.copy(alpha = .55f), Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center,
            ) { art() }
            // One line for every title: long names get a smaller size so both cards stay symmetric.
            // (No BoxWithConstraints here: the card row measures intrinsics, which subcompose can't answer.)
            Text(
                title,
                color = Color.White,
                fontSize = if (title.length > 10) 16.sp else 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = .9f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 30.dp),
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))
            HfPanelPill(sh("OYNA ▶", "PLAY ▶"), ink = colors[2])
        }
    }
}


@Composable
internal fun PremiumDailyObjective(onClick: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var players by remember { mutableStateOf<List<com.sonharf.game.data.ThroneRow>>(emptyList()) }
    var loading by remember { mutableStateOf(backend != null) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    // The week's ranking changes on its own: it is refreshed every minute while the home is open,
    // and the server starts a new week every Monday.
    LaunchedEffect(backend, reloadKey) {
        val activeBackend = backend
        if (activeBackend == null) {
            loading = false
            return@LaunchedEffect
        }
        while (true) {
            failed = false
            gameRequestResult { com.sonharf.game.data.ThroneBackend.week().rows }
                .onSuccess { next -> next.take(3).let { if (it != players) players = it } }
                .onFailure { if (players.isEmpty()) failed = true }
            loading = false
            delay(WEEKLY_PODIUM_REFRESH_MS)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GameWeeklyPodium(players)

        when {
            loading -> CircularProgressIndicator(Modifier.size(22.dp), color = Hf.GoldLight, strokeWidth = 2.dp)
            failed -> HfPill(onClick = { reloadKey += 1 }) {
                Text(sh("YENİLE", "RETRY"), color = Hf.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

internal const val WEEKLY_PODIUM_REFRESH_MS = 60_000L



@Composable
private fun HomeLoadingRow() {
    Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(18.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
    }
}

@Composable
private fun HomeSectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.style_icon_trophy),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(7.dp))
        Text(title, Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun PremiumHomeExtras(profile: ProfileDto?, onShop: () -> Unit, onSocial: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CardGiftcard, null, tint = SonHarfTheme.Primary)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Günlük ücretsiz hediye", "Daily free gift"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SonHarfTheme.TextPrimary)
                    Text(sh("Bugünkü ödülünü mağazada kontrol et.", "Check today's reward in the shop."), fontSize = 11.sp, color = SonHarfTheme.TextSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
        }
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfTheme.Lavender)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (profile?.isVip == true) sh("PRO koleksiyonun", "Your PRO collection") else sh("PRO'yu keşfet", "Explore PRO"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(sh("Reklamsız kullanım · Kozmetik · Konfor", "Ad-free · Cosmetics · Comfort"), color = SonHarfTheme.TextSecondary, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
        }
        HomePlayTogether(profile, onSocial)
    }
}

private fun homeLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
