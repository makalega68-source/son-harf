package com.sonharf.game

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay

private enum class LightScreen { HOME, SON_HARF, KELIME_AVI, KELIME_SAVASI, LEAGUE, TASKS, PROFILE }

private val LightBg = Color(0xFFF7F9FC)
private val LightSurface = Color.White
private val LightSurface2 = Color(0xFFF0F4F8)
private val LightText = Color(0xFF182235)
private val LightMuted = Color(0xFF718096)
private val LightBlue = Color(0xFF1769E0)
private val LightGreen = Color(0xFF22B95F)
private val LightGold = Color(0xFFF3A81A)
private val LightBorder = Color(0xFFDDE5EE)

@Composable
fun LightWordThemeApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val context = LocalContext.current
    var screen by remember { mutableStateOf(LightScreen.HOME) }
    var gameKey by remember { mutableIntStateOf(0) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var lastHomeBack by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }
    LaunchedEffect(SonHarfUiState.homeRequest) {
        if (SonHarfUiState.homeRequest > 0) screen = LightScreen.HOME
    }

    BackHandler(enabled = authenticated) {
        if (screen == LightScreen.HOME) {
            val now = System.currentTimeMillis()
            if (now - lastHomeBack < 1800L) (context as? Activity)?.finish() else lastHomeBack = now
        } else screen = LightScreen.HOME
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(LightBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LightBlue)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = LightScreen.HOME }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (screen in setOf(LightScreen.HOME, LightScreen.LEAGUE, LightScreen.TASKS, LightScreen.PROFILE)) {
                LightBottomBar(
                    screen,
                    { screen = LightScreen.HOME },
                    { screen = LightScreen.LEAGUE },
                    { screen = LightScreen.TASKS },
                    { screen = LightScreen.PROFILE },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color.White, LightBg, Color(0xFFF3F7FC)))
            )
        ) {
            when (screen) {
                LightScreen.HOME -> LightHomeScreen(
                    backend,
                    onSonHarf = { gameKey += 1; screen = LightScreen.SON_HARF },
                    onKelimeAvi = { screen = LightScreen.KELIME_AVI },
                    onKelimeSavasi = { screen = LightScreen.KELIME_SAVASI },
                    onLeague = { screen = LightScreen.LEAGUE },
                    onTasks = { screen = LightScreen.TASKS },
                    onProfile = { screen = LightScreen.PROFILE },
                )
                LightScreen.SON_HARF -> key(gameKey) { TargetNeonGameScreen(autoStartMatchmaking = true) }
                LightScreen.KELIME_AVI -> DailyCipherScreen { screen = LightScreen.HOME }
                LightScreen.KELIME_SAVASI -> TrackedBilBakalimStandaloneScreen { screen = LightScreen.HOME }
                LightScreen.LEAGUE -> LeaderboardExperienceScreen { screen = LightScreen.HOME }
                LightScreen.TASKS -> LightTasksScreen(backend)
                LightScreen.PROFILE -> ProfileExperienceScreen(onBack = { screen = LightScreen.HOME })
            }
        }
    }
}

@Composable
private fun LightBottomBar(
    screen: LightScreen,
    onHome: () -> Unit,
    onLeague: () -> Unit,
    onTasks: () -> Unit,
    onProfile: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 10.dp, border = BorderStroke(1.dp, LightBorder)) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            LightBottomItem(Icons.Rounded.Home, "Ana Sayfa", screen == LightScreen.HOME, Modifier.weight(1f), onHome)
            LightBottomItem(Icons.Rounded.EmojiEvents, "Lig", screen == LightScreen.LEAGUE, Modifier.weight(1f), onLeague)
            LightBottomItem(Icons.Rounded.TaskAlt, "Görevler", screen == LightScreen.TASKS, Modifier.weight(1f), onTasks)
            LightBottomItem(Icons.Rounded.Person, "Profil", screen == LightScreen.PROFILE, Modifier.weight(1f), onProfile)
        }
    }
}

@Composable
private fun LightBottomItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) LightBlue else LightMuted, modifier = Modifier.size(23.dp))
        Text(label, color = if (selected) LightBlue else LightMuted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun LightHomeScreen(
    backend: OnlineGameBackend?,
    onSonHarf: () -> Unit,
    onKelimeAvi: () -> Unit,
    onKelimeSavasi: () -> Unit,
    onLeague: () -> Unit,
    onTasks: () -> Unit,
    onProfile: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var homeContextLoaded by remember { mutableStateOf(false) }
    var mascotMotion by remember { mutableStateOf(MascotMotion.GREETING) }
    var mascotSpeech by remember { mutableStateOf("Hazırsan başlayalım!") }

    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        homeContextLoaded = true
    }

    LaunchedEffect(homeContextLoaded) {
        if (!homeContextLoaded) return@LaunchedEffect

        if (MascotHomeAiDirector.hasFreshCache()) {
            mascotSpeech = MascotHomeAiDirector.cachedReply
            mascotMotion = MascotHomeAiDirector.cachedMotion
            return@LaunchedEffect
        }

        mascotMotion = MascotMotion.THINKING
        mascotSpeech = if (SonHarfUiState.isEnglish) "Thinking of a good start..." else "İyi bir başlangıç düşünüyorum..."

        val response = MascotAiChatService.chat(
            MascotHomeAiDirector.homeRequest(
                playerName = profile?.displayName,
                growth = growth,
                language = SonHarfUiState.language,
            )
        )
        MascotHomeAiDirector.cache(response)
        mascotSpeech = MascotHomeAiDirector.cachedReply
        mascotMotion = MascotHomeAiDirector.cachedMotion
    }

    LaunchedEffect(mascotMotion) {
        val duration = MascotMotionPolicy.durationMs(mascotMotion) ?: when (mascotMotion) {
            MascotMotion.WALK,
            MascotMotion.RUN,
            MascotMotion.THINKING,
            MascotMotion.LOOK_AT_PLAYER,
            MascotMotion.TURN_LEFT,
            MascotMotion.TURN_RIGHT -> 2_600L
            else -> null
        }
        if (duration != null) {
            delay(duration)
            mascotMotion = MascotMotion.IDLE
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = LightText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("Kelimeyi Sürdür, Rakibini Geç", color = LightMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Surface(onClick = onProfile, shape = CircleShape, color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Text((profile?.displayName ?: "O").take(1).uppercase(), color = LightBlue, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(19.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = LightGold, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(growth?.leagueName ?: "BRONZ", color = LightText, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("Seviye " + (growth?.level ?: 1), color = LightMuted, fontSize = 10.sp)
                    }
                    Text((growth?.currentWinStreak ?: 0).toString() + " 🔥", color = LightText, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD5E3F7)),
                shadowElevation = 5.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(250.dp)) {
                        MascotLive3DStage(
                            modifier = Modifier.fillMaxSize(),
                            mascotId = MascotCatalog.CHIBI_WIZARD_ID,
                            motion = mascotMotion,
                            displayScale = 1.55f,
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = LightSurface2,
                        border = BorderStroke(1.dp, LightBorder),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.ChatBubble, null, tint = LightBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(
                                mascotSpeech,
                                color = LightText,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text("Son Harf", color = LightText, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("Son harften yeni kelime üret, rakibini geç.", color = LightMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Button(
                        onClick = onSonHarf,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LightBlue, contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("OYNA", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item { Text("DİĞER OYUNLAR", color = LightMuted, fontSize = 11.sp, fontWeight = FontWeight.Black) }

        item {
            LightGameCard(Icons.Rounded.Search, "Kelime Avı", "Günün kelimesini ipuçlarıyla bul.", "BAŞLA", LightGreen, onKelimeAvi)
        }
        item {
            LightGameCard(Icons.Rounded.Bolt, "Kelime Savaşı", "Hızlı düşün, doğru tahminle öne geç.", "OYNA", LightGold, onKelimeSavasi)
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LightShortcut(Icons.Rounded.EmojiEvents, "Lig", Modifier.weight(1f), onLeague)
                LightShortcut(Icons.Rounded.TaskAlt, "Görevler", Modifier.weight(1f), onTasks)
                LightShortcut(Icons.Rounded.Person, "Profil", Modifier.weight(1f), onProfile)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun LightGameCard(icon: ImageVector, title: String, subtitle: String, buttonText: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = LightSurface,
        border = BorderStroke(1.dp, LightBorder),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .12f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(29.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = LightText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(subtitle, color = LightMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = accent) {
                Text(buttonText, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun LightShortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = LightBlue, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, color = LightText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LightTasksScreen(backend: OnlineGameBackend?) {
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    LaunchedEffect(Unit) { goals = runCatching { backend?.getGoals().orEmpty() }.getOrDefault(emptyList()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Görevler", color = LightText, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("Günlük ilerlemeni tek ekranda takip et.", color = LightMuted, fontSize = 10.sp)
        }
        if (goals.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                    Text("Yeni görevler hazırlanıyor.", Modifier.fillMaxWidth().padding(16.dp), color = LightMuted)
                }
            }
        } else {
            items(goals) { g ->
                val done = g.progress >= g.target
                Surface(shape = RoundedCornerShape(18.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.TrackChanges, null, tint = if (done) LightGreen else LightBlue)
                            Spacer(Modifier.width(9.dp))
                            Text(if (SonHarfUiState.isEnglish) g.titleEn else g.titleTr, color = LightText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("+" + g.rewardDiamonds, color = LightGold, fontWeight = FontWeight.Black)
                        }
                        LinearProgressIndicator(
                            progress = { (g.progress.toFloat() / g.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (done) LightGreen else LightBlue,
                            trackColor = LightSurface2,
                        )
                        Text(g.progress.coerceAtMost(g.target).toString() + "/" + g.target, color = LightMuted, fontSize = 9.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
