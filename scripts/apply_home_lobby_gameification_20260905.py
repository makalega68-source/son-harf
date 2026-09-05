from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Missing expected pattern: {label}")
    return text.replace(old, new, 1)


def patch_shell(text: str) -> str:
    text = replace_once(
        text,
        "    var destination by remember { mutableStateOf(MonsterDestination.HOME) }\n    val homeRequest = SonHarfUiState.homeRequest",
        "    var destination by remember { mutableStateOf(MonsterDestination.HOME) }\n    var isPremium by remember { mutableStateOf(false) }\n    val homeRequest = SonHarfUiState.homeRequest",
        "premium state",
    )
    text = replace_once(
        text,
        "    LaunchedEffect(Unit) {\n        runCatching { SonHarfCosmetics.apply(backend.getEquippedCosmetics()) }\n    }",
        "    LaunchedEffect(Unit) {\n        runCatching { SonHarfCosmetics.apply(backend.getEquippedCosmetics()) }\n        val id = backend.currentUserId()\n        if (id != null) isPremium = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)\n    }",
        "premium lookup",
    )
    text = replace_once(
        text,
        "    val topLevel = destination in setOf(MonsterDestination.HOME, MonsterDestination.LEAGUE, MonsterDestination.SOCIAL, MonsterDestination.STYLE, MonsterDestination.PROFILE)\n    Scaffold(\n        containerColor = SonHarfTheme.Background,",
        "    val topLevel = destination in setOf(MonsterDestination.HOME, MonsterDestination.LEAGUE, MonsterDestination.SOCIAL, MonsterDestination.STYLE, MonsterDestination.PROFILE)\n    val isGameplay = destination in setOf(MonsterDestination.GAME, MonsterDestination.WORD_SIEGE, MonsterDestination.DAILY_CHALLENGE)\n    Scaffold(\n        containerColor = SonHarfTheme.Background,\n        topBar = { SonHarfTopAdBanner(visible = !isGameplay, isPremium = isPremium) },",
        "global non-game banner",
    )
    text = replace_once(
        text,
        "        Box(Modifier.fillMaxSize().padding(if (topLevel) padding else PaddingValues(0.dp)).background(SonHarfTheme.Background)) {",
        "        Box(Modifier.fillMaxSize().padding(padding).background(SonHarfTheme.Background)) {",
        "scaffold insets",
    )
    return text


def patch_home(text: str) -> str:
    pattern = re.compile(
        r"@Composable\nprivate fun MonsterHomeScreen\(.*?\n\}\n\n@Composable\nprivate fun MonsterLiveMatchCard",
        re.S,
    )
    replacement = r'''@Composable
private fun MonsterHomeScreen(
    backend: OnlineGameBackend,
    onPlay: () -> Unit,
    onSiege: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onStyle: () -> Unit,
    onProfile: () -> Unit,
    onTasks: () -> Unit,
    onVip: () -> Unit,
    onSettings: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var stats by remember { mutableStateOf(MonsterHomeStats()) }
    var weeklyTop by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        if (id != null) runCatching { backend.getProfile(id) }.onSuccess { profile = it }
        runCatching { backend.getLeaderboardV2(limit = 100) }.onSuccess { rows ->
            rows.firstOrNull { it.userId == backend.currentUserId() }?.let { row ->
                stats = stats.copy(rating = row.rating)
            }
        }
        weeklyTop = runCatching { backend.getLeaderboardV2(SonHarfUiState.language, "week", 3) }.getOrDefault(emptyList())
        goals = runCatching { backend.getGoals() }.getOrDefault(emptyList())
    }

    val activeGoal = goals.firstOrNull { !it.claimed } ?: goals.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = MonsterUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kelimeyi Sürdür, Rakibini Geç", "Keep the word going, beat your rival"), color = MonsterUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                MonsterIconButton(Icons.Rounded.Notifications, onTasks)
                Spacer(Modifier.width(7.dp))
                MonsterIconButton(Icons.Rounded.Settings, onSettings)
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile), shape = RoundedCornerShape(17.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MonsterUi.Accent.copy(alpha = .12f)) { Icon(Icons.Rounded.Person, null, tint = MonsterUi.Accent, modifier = Modifier.padding(8.dp).size(21.dp)) }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.displayName ?: sh("OYUNCU", "PLAYER"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                        Text("🏆 ${stats.rating} RP", color = MonsterUi.Muted, fontSize = 8.sp)
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = MonsterUi.Gold.copy(alpha = .12f)) {
                        Text("SC ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = MonsterUi.Text, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { MonsterLiveMatchCard(profile, stats, onPlay) }
        item {
            Text(sh("OYUN MODLARI", "GAME MODES"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonsterQuickCard(sh("KELİME\nKUŞATMASI", "WORD\nSIEGE"), sh("Alanı ele geçir", "Capture territory"), Icons.Rounded.GridView, MonsterUi.Gold, Modifier.weight(1f), onSiege)
                MonsterQuickCard(sh("LİG &\nRATING", "LEAGUE &\nRATING"), "${stats.rating} RP", Icons.Rounded.EmojiEvents, MonsterUi.Accent, Modifier.weight(1f), onLeague)
            }
        }
        item {
            MonsterSectionTitle(sh("HAFTANIN EN İYİLERİ", "BEST THIS WEEK"), sh("TÜM SIRALAMA", "FULL RANKING"), onLeague)
            Spacer(Modifier.height(7.dp))
            MonsterWeeklyTopThree(weeklyTop, onLeague)
        }
        if (activeGoal != null) {
            item {
                val title = if (SonHarfUiState.isEnglish) activeGoal.titleEn else activeGoal.titleTr
                Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onTasks), shape = RoundedCornerShape(17.dp), color = MonsterUi.Green.copy(alpha = .07f), border = BorderStroke(1.dp, MonsterUi.Green.copy(alpha = .24f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TrackChanges, null, tint = MonsterUi.Green, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("BUGÜNKÜ HEDEF", "TODAY'S GOAL"), color = MonsterUi.Green, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(title, color = MonsterUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${activeGoal.progress.coerceAtMost(activeGoal.target)}/${activeGoal.target}", color = MonsterUi.Muted, fontSize = 8.sp)
                        }
                        Text("SC ${activeGoal.rewardDiamonds}", color = MonsterUi.Text, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MonsterLiveMatchCard'''
    text, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise SystemExit("Missing expected pattern: MonsterHomeScreen")
    return text


def patch_play_card(text: str) -> str:
    old = '''            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MonsterUi.Accent, contentColor = MonsterUi.AccentText),
            ) { Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 15.sp) }'''
    new = '''            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MonsterUi.Accent, contentColor = MonsterUi.AccentText),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(7.dp))
                Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = .8.sp)
            }'''
    return replace_once(text, old, new, "primary play button")


def add_weekly_component(text: str) -> str:
    marker = "@Composable\nprivate fun MonsterStatCard"
    component = '''@Composable
private fun MonsterWeeklyTopThree(rows: List<LeaderboardV2Row>, onClick: () -> Unit) {
    if (rows.isEmpty()) {
        Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
            Text(sh("Bu hafta sıralama henüz oluşmadı. İlk galibiyetini al!", "Weekly ranking has not formed yet. Get the first win!"), Modifier.padding(15.dp), color = MonsterUi.Muted, fontSize = 9.sp)
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
        rows.take(3).forEachIndexed { index, row ->
            val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" }
            val accent = if (index == 0) MonsterUi.Gold else MonsterUi.Accent
            Surface(modifier = Modifier.weight(1f).height(if (index == 0) 112.dp else 104.dp).clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = accent.copy(alpha = if (index == 0) .12f else .06f), border = BorderStroke(1.dp, accent.copy(alpha = if (index == 0) .40f else .20f))) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(medal, fontSize = if (index == 0) 24.sp else 21.sp)
                    Text(row.displayName, color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1)
                    Text("${row.wins}W • ${row.rating} RP", color = MonsterUi.Muted, fontSize = 7.sp, maxLines = 1)
                }
            }
        }
    }
}

'''
    return replace_once(text, marker, component + marker, "weekly top component insertion")


def main() -> None:
    text = APP.read_text()
    text = patch_shell(text)
    text = patch_home(text)
    text = patch_play_card(text)
    text = add_weekly_component(text)
    APP.write_text(text)
    print("Home lobby gameification + global non-game banner patch applied successfully.")


if __name__ == "__main__":
    main()
