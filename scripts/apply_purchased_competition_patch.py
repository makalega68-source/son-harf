from pathlib import Path

PATH = Path("app/src/main/java/com/sonharf/game/CompetitionHubScreen.kt")
text = PATH.read_text()


def slice_replace(source: str, start_marker: str, end_marker: str, replacement: str, label: str) -> str:
    start = source.find(start_marker)
    if start < 0:
        raise RuntimeError(f"{label}: start marker missing")
    end = source.find(end_marker, start + len(start_marker))
    if end < 0:
        raise RuntimeError(f"{label}: end marker missing")
    return source[:start] + replacement.rstrip() + "\n" + source[end:]


top = r'''@Composable
fun CompetitionHubScreen(onBack: () -> Unit, clubEntry: Boolean = false) {
    // clubEntry retained for source compatibility; the club surface remains hidden.
    @Suppress("UNUSED_PARAMETER") val ignoredClubEntry = clubEntry
    var tab by remember { mutableIntStateOf(1) }

    Box(Modifier.fillMaxSize()) {
        PurchasedGameBackdrop(Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MainScreenHeader(
                title = sh("Rekabet Merkezi", "Competition Hub"),
                subtitle = sh("Haftalık Kupa • Rakipler", "Weekly Cup • Rivals"),
                onBack = onBack,
            )

            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PurchasedButton(
                        text = sh("KUPA", "CUP"),
                        onClick = { tab = 1 },
                        modifier = Modifier.weight(1f),
                        style = if (tab == 1) PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.SECONDARY,
                        leadingAsset = PurchasedUiAsset.ICON_TROPHY,
                    )
                    PurchasedButton(
                        text = sh("RAKİPLER", "RIVALS"),
                        onClick = { tab = 2 },
                        modifier = Modifier.weight(1f),
                        style = if (tab == 2) PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.SECONDARY,
                        leadingAsset = PurchasedUiAsset.ICON_SWORDS,
                    )
                }
            }

            Box(Modifier.weight(1f)) {
                when (tab) {
                    1 -> WeeklyTournamentTab()
                    else -> RivalHistoryTab()
                }
            }
        }
    }
}
'''
text = slice_replace(
    text,
    "@Composable\nfun CompetitionHubScreen(onBack: () -> Unit, clubEntry: Boolean = false) {",
    "\n@Composable\nprivate fun ClubCompetitionTab()",
    top,
    "competition shell",
)

weekly = r'''@Composable
private fun WeeklyTournamentTab() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var tournament by remember { mutableStateOf<WeeklyTournamentDto?>(null) }
    var leaderboard by remember { mutableStateOf<List<WeeklyTournamentLeaderboardRowDto>>(emptyList()) }
    var leaderboardProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var history by remember { mutableStateOf<List<WeeklyTournamentHistoryDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload() {
        val b = backend ?: return
        loading = true
        runCatching {
            tournament = b.getWeeklyTournament()
            val nextLeaderboard = b.getWeeklyTournamentLeaderboard(50)
            leaderboard = nextLeaderboard
            val nextProfiles = leaderboardProfiles.toMutableMap()
            for (row in nextLeaderboard) {
                if (!nextProfiles.containsKey(row.userId)) {
                    nextProfiles[row.userId] = runCatching { b.getProfile(row.userId) }.getOrNull()
                }
            }
            val activeLeaderboardIds = nextLeaderboard.mapTo(mutableSetOf()) { it.userId }
            leaderboardProfiles = nextProfiles.filterKeys { it in activeLeaderboardIds }
            history = b.getWeeklyTournamentHistory(12)
        }.onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (loading) {
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_TROPHY, Modifier.size(34.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(sh("Kupa sıralaması yükleniyor…", "Loading cup standings…"), color = MainUi.Text, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val t = tournament
        if (t != null) {
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_LARGE,
                    contentPadding = PaddingValues(17.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        PurchasedAsset(PurchasedUiAsset.ICON_TROPHY, Modifier.size(58.dp))
                        Text(t.name.uppercase(), color = MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text(
                            sh("Katılım ücretsiz • PvP galibiyet +3 • mağlubiyet +1", "Free entry • PvP win +3 • loss +1"),
                            color = MainUi.Green,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text("${t.weekStart} • ${t.playerCount} ${sh("oyuncu", "players")}", color = MainUi.Muted, fontSize = 9.sp)

                        if (!t.joined) {
                            PurchasedButton(
                                text = sh("ÜCRETSİZ KATIL", "JOIN FREE"),
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        runCatching { backend?.joinWeeklyTournament() }
                                            .onSuccess {
                                                notice = sh(
                                                    "Haftalık Kupaya katıldın. Bundan sonraki PvP maçların puan kazandırır.",
                                                    "You joined the Weekly Cup. Your next PvP matches earn points.",
                                                )
                                                reload()
                                            }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                style = PurchasedButtonStyle.WARNING,
                                leadingAsset = PurchasedUiAsset.ICON_TROPHY,
                            )
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                CompetitionMetric("${t.myPoints}", sh("PUAN", "POINTS"), Modifier.weight(1f))
                                CompetitionMetric("#${if (t.myRank == 0L) "—" else t.myRank}", sh("SIRAN", "RANK"), Modifier.weight(1f))
                                CompetitionMetric("${t.myWins}-${t.myLosses}", "W-L", Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.REWARD_PANEL,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(sh("KUPA ÖDÜLLERİ", "CUP REWARDS"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            CompetitionRankReward(PurchasedUiAsset.RANK_ONE, "1.000 SC")
                            CompetitionRankReward(PurchasedUiAsset.RANK_TWO, "600 SC")
                            CompetitionRankReward(PurchasedUiAsset.RANK_THREE, "400 SC")
                        }
                        Text(
                            sh("4–10: 150 SC • En az 1 maç oynayan diğer oyuncular: 50 SC", "4–10: 150 SC • Other players with at least 1 match: 50 SC"),
                            color = MainUi.Muted,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            sh("Maç oynamadan sıralama ve ödül kazanılmaz.", "No ranking or reward is earned without playing a match."),
                            color = MainUi.Muted,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                PurchasedButton(
                    text = if (history.any { it.rewardEligible }) sh("KUPA ÖDÜLÜNÜ AL", "CLAIM CUP REWARD") else sh("ALINABİLİR ÖDÜL YOK", "NO REWARD TO CLAIM"),
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backend?.claimPreviousWeeklyTournamentReward() }
                                .onSuccess { reward ->
                                    if (reward != null) {
                                        notice = sh(
                                            "Ödül: #${reward.rank} • +${reward.rewardCoins} Son Coin",
                                            "Reward: #${reward.rank} • +${reward.rewardCoins} Son Coin",
                                        )
                                        reload()
                                    }
                                }
                                .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                            busy = false
                        }
                    },
                    enabled = !busy && history.any { it.rewardEligible },
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.WARNING,
                    leadingAsset = PurchasedUiAsset.ICON_GIFT,
                )
            }
        }

        if (notice.isNotBlank()) {
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(11.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(28.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(notice, Modifier.weight(1f), color = MainUi.Text, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item { MainSectionTitle(sh("CANLI SIRALAMA", "LIVE RANKING")) }
        items(leaderboard, key = { it.userId }) { row ->
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    when (row.rank) {
                        1L -> PurchasedAsset(PurchasedUiAsset.RANK_ONE, Modifier.size(38.dp))
                        2L -> PurchasedAsset(PurchasedUiAsset.RANK_TWO, Modifier.size(38.dp))
                        3L -> PurchasedAsset(PurchasedUiAsset.RANK_THREE, Modifier.size(38.dp))
                        else -> Text("#${row.rank}", Modifier.width(38.dp), color = MainUi.Muted, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(6.dp))
                    ProfilePhotoAvatar(
                        avatarPath = leaderboardProfiles[row.userId]?.avatarPath,
                        name = row.displayName,
                        size = 38.dp,
                        accent = MainUi.Gold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("${row.leagueName} • ${row.rating} rating • ${row.wins}W/${row.losses}L", color = MainUi.Muted, fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${row.points}", color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(sh("PUAN", "PTS"), color = MainUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { MainSectionTitle(sh("KUPA GEÇMİŞİM", "MY CUP HISTORY")) }

        if (history.isEmpty()) {
            item {
                PurchasedPanel(modifier = Modifier.fillMaxWidth(), asset = PurchasedUiAsset.PANEL_SMALL, contentPadding = PaddingValues(14.dp)) {
                    Text(sh("Henüz tamamlanmış kupa geçmişin yok.", "You do not have completed cup history yet."), modifier = Modifier.fillMaxWidth(), color = MainUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(history, key = { it.tournamentId }) { h ->
                val played = h.matches > 0
                val rankText = if (h.finalRank > 0) "#${h.finalRank}" else "—"
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val rankAsset = when (h.finalRank) {
                                1L -> PurchasedUiAsset.RANK_ONE
                                2L -> PurchasedUiAsset.RANK_TWO
                                3L -> PurchasedUiAsset.RANK_THREE
                                else -> PurchasedUiAsset.ICON_TROPHY
                            }
                            PurchasedAsset(rankAsset, Modifier.size(40.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text("${h.weekStart} • ${h.participantCount} ${sh("aktif oyuncu", "active players")}", color = MainUi.Muted, fontSize = 8.sp)
                            }
                            Text(rankText, color = if (played) MainUi.Gold else MainUi.Muted, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            CompetitionMetric("${h.points}", sh("PUAN", "POINTS"), Modifier.weight(1f))
                            CompetitionMetric("${h.wins}-${h.losses}", "W-L", Modifier.weight(1f))
                            CompetitionMetric("${h.matches}", sh("MAÇ", "MATCHES"), Modifier.weight(1f))
                        }
                        Text(
                            when {
                                !played -> sh("Maç oynamadığın için sıralama ve ödül oluşmadı.", "No ranking or reward because no match was played.")
                                h.rewardClaimed -> sh("✓ +${h.rewardCoins} Son Coin alındı", "✓ +${h.rewardCoins} Son Coin claimed")
                                h.rewardEligible -> sh("+${h.rewardCoins} Son Coin alınabilir", "+${h.rewardCoins} Son Coin available")
                                else -> sh("Ödül durumu kapalı.", "Reward unavailable.")
                            },
                            color = when {
                                h.rewardEligible -> MainUi.Gold
                                h.rewardClaimed -> MainUi.Green
                                else -> MainUi.Muted
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}
'''
text = slice_replace(
    text,
    "@Composable\nprivate fun WeeklyTournamentTab() {",
    "\n@Composable\nprivate fun RivalHistoryTab()",
    weekly,
    "weekly cup",
)

rivals = r'''@Composable
private fun RivalHistoryTab() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var rivals by remember { mutableStateOf<List<RivalHistoryDto>>(emptyList()) }
    var matchHistory by remember { mutableStateOf<List<MatchHistoryDto>>(emptyList()) }
    var playerProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var busyOpponent by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload(showLoading: Boolean = true) {
        val b = backend ?: return
        if (showLoading) loading = true
        runCatching {
            val nextRivals = b.getRivalHistory(30)
            val nextMatches = b.getMatchHistory(30)
            rivals = nextRivals
            matchHistory = nextMatches
            val nextProfiles = playerProfiles.toMutableMap()
            val activeIds = (nextRivals.map { it.opponentId } + nextMatches.map { it.opponentId }).toSet()
            for (userId in activeIds) {
                if (!nextProfiles.containsKey(userId)) {
                    nextProfiles[userId] = runCatching { b.getProfile(userId) }.getOrNull()
                }
            }
            playerProfiles = nextProfiles.filterKeys { it in activeIds }
        }.onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
        if (showLoading) loading = false
    }

    LaunchedEffect(Unit) {
        reload()
        while (true) {
            delay(12_000)
            reload(showLoading = false)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) {
            item {
                PurchasedPanel(modifier = Modifier.fillMaxWidth(), asset = PurchasedUiAsset.PANEL_SMALL, contentPadding = PaddingValues(12.dp)) {
                    Text(sh("Rakip geçmişi yükleniyor…", "Loading rival history…"), color = MainUi.Text, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_LARGE,
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.ICON_SWORDS, Modifier.size(58.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sh("RÖVANŞ HATTI", "REMATCH LINE"), color = MainUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text(
                            sh("Son Harf ve Kelime Arenası rakiplerin tek geçmişte. Çevrimiçi arkadaşına yeniden meydan oku.", "Son Harf and Word Arena rivals in one history. Challenge an online friend again."),
                            color = MainUi.Muted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }

        if (notice.isNotBlank()) {
            item {
                PurchasedPanel(modifier = Modifier.fillMaxWidth(), asset = PurchasedUiAsset.PANEL_SMALL, contentPadding = PaddingValues(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_CHAT, Modifier.size(28.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(notice, Modifier.weight(1f), color = MainUi.Text, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (!loading && rivals.isEmpty()) {
            item {
                PurchasedPanel(modifier = Modifier.fillMaxWidth(), asset = PurchasedUiAsset.PANEL_SMALL, contentPadding = PaddingValues(18.dp)) {
                    Text(sh("Henüz gerçek PvP rakip geçmişin yok.", "You do not have real PvP rival history yet."), modifier = Modifier.fillMaxWidth(), color = MainUi.Muted, textAlign = TextAlign.Center)
                }
            }
        }

        items(rivals, key = { it.opponentId }) { rival ->
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_MEDIUM,
                contentPadding = PaddingValues(13.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatar(
                            avatarPath = playerProfiles[rival.opponentId]?.avatarPath,
                            name = rival.displayName,
                            size = 46.dp,
                            accent = if (rival.canChallenge) MainUi.Blue else MainUi.Muted,
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rival.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("${rival.matches} ${sh("maç", "matches")} • ${rival.wins}W/${rival.losses}L" + if (rival.draws > 0) "/${rival.draws}D" else "", color = MainUi.Muted, fontSize = 9.sp)
                            Text(
                                sh("Son mod: ${if (rival.lastMode == "arena") "Kelime Arenası" else "Son Harf"}", "Last mode: ${if (rival.lastMode == "arena") "Word Arena" else "Son Harf"}"),
                                color = MainUi.Muted,
                                fontSize = 8.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${rival.myPoints}:${rival.theirPoints}", color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text(
                                if (rival.presenceStatus == "online") "● ${sh("Çevrimiçi", "Online")}" else sh("Çevrimdışı", "Offline"),
                                color = if (rival.presenceStatus == "online") MainUi.Green else MainUi.Muted,
                                fontSize = 8.sp,
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CompetitionMetric("${rival.classicMatches}", "SON HARF", Modifier.weight(1f))
                        CompetitionMetric("${rival.arenaMatches}", "ARENA", Modifier.weight(1f))
                    }

                    if (rival.canChallenge) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            PurchasedButton(
                                text = "SON HARF",
                                onClick = {
                                    scope.launch {
                                        busyOpponent = rival.opponentId
                                        runCatching { backend?.inviteFriend(rival.opponentId, SonHarfUiState.language) }
                                            .onSuccess {
                                                notice = sh("${rival.displayName}: Son Harf daveti gönderildi.", "${rival.displayName}: Son Harf invite sent.")
                                                SonHarfSoundFx.softNotify()
                                            }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busyOpponent = null
                                    }
                                },
                                enabled = busyOpponent == null,
                                modifier = Modifier.weight(1f),
                                style = PurchasedButtonStyle.SECONDARY,
                                leadingAsset = PurchasedUiAsset.ICON_SWORDS,
                            )
                            PurchasedButton(
                                text = "ARENA",
                                onClick = {
                                    scope.launch {
                                        busyOpponent = rival.opponentId
                                        runCatching { backend?.inviteFriendToWordArena(rival.opponentId, SonHarfUiState.language) }
                                            .onSuccess {
                                                notice = sh("${rival.displayName}: Arena daveti gönderildi.", "${rival.displayName}: Arena invite sent.")
                                                SonHarfSoundFx.softNotify()
                                            }
                                            .onFailure { notice = friendlyCompetitionError(it.message.orEmpty()) }
                                        busyOpponent = null
                                    }
                                },
                                enabled = busyOpponent == null,
                                modifier = Modifier.weight(1f),
                                style = PurchasedButtonStyle.WARNING,
                                leadingAsset = PurchasedUiAsset.ICON_GAMES,
                            )
                        }
                    } else {
                        Text(
                            when {
                                !rival.isFriend -> sh("Canlı meydan okuma için önce arkadaş olmalısınız.", "Become friends first to send a live challenge.")
                                rival.presenceStatus != "online" -> sh("Arkadaşın çevrimiçi olduğunda meydan okuyabilirsin.", "You can challenge this friend when they are online.")
                                else -> sh("Bu rakibe şu anda meydan okunamıyor.", "This rival cannot be challenged right now.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MainUi.Muted,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        item { MainSectionTitle(sh("SON MAÇLAR", "RECENT MATCHES")) }

        if (matchHistory.isEmpty()) {
            item {
                PurchasedPanel(modifier = Modifier.fillMaxWidth(), asset = PurchasedUiAsset.PANEL_SMALL, contentPadding = PaddingValues(14.dp)) {
                    Text(sh("Henüz tamamlanmış gerçek PvP maçın yok.", "You do not have completed real PvP matches yet."), modifier = Modifier.fillMaxWidth(), color = MainUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(matchHistory, key = { "${it.mode}-${it.matchId}" }) { match ->
                val resultColor = when (match.result) {
                    "win" -> MainUi.Green
                    "loss" -> MainUi.Red
                    else -> MainUi.Gold
                }
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(11.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatar(
                            avatarPath = playerProfiles[match.opponentId]?.avatarPath,
                            name = match.displayName,
                            size = 40.dp,
                            accent = resultColor,
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(match.displayName, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text("${match.playedAt.take(10)} • ${match.language.uppercase()} • " + if (match.mode == "arena") "Arena" else "Son Harf", color = MainUi.Muted, fontSize = 8.sp)
                            Text(
                                when (match.result) {
                                    "win" -> sh("GALİBİYET", "WIN")
                                    "loss" -> sh("MAĞLUBİYET", "LOSS")
                                    else -> sh("BERABERE", "DRAW")
                                },
                                color = resultColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${match.myScore}:${match.theirScore}", color = MainUi.Text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text((if (match.ratingDelta > 0) "+" else "") + match.ratingDelta + " rating", color = if (match.ratingDelta >= 0) MainUi.Green else MainUi.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}
'''
text = slice_replace(
    text,
    "@Composable\nprivate fun RivalHistoryTab() {",
    "\n@Composable\nprivate fun CompetitionHero(",
    rivals,
    "rival history",
)

metric_start = "@Composable\nprivate fun CompetitionMetric(value: String, label: String, modifier: Modifier) {"
metric_end = "\nprivate fun friendlyCompetitionError(raw: String): String = when {"
metric = r'''@Composable
private fun CompetitionMetric(value: String, label: String, modifier: Modifier) {
    PurchasedPanel(
        modifier = modifier,
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
            Text(label, color = MainUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun CompetitionRankReward(asset: PurchasedUiAsset, amount: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        PurchasedAsset(asset, Modifier.size(42.dp))
        Text(amount, color = MainUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}
'''
text = slice_replace(text, metric_start, metric_end, metric, "competition metric")

required = [
    "getWeeklyTournamentLeaderboard(50)",
    "getWeeklyTournamentHistory(12)",
    "joinWeeklyTournament()",
    "claimPreviousWeeklyTournamentReward()",
    "getRivalHistory(30)",
    "getMatchHistory(30)",
    "inviteFriend(rival.opponentId, SonHarfUiState.language)",
    "inviteFriendToWordArena(rival.opponentId, SonHarfUiState.language)",
    "WeeklyTournamentTab()",
    "RivalHistoryTab()",
    "PurchasedUiAsset.RANK_ONE",
    "PurchasedUiAsset.ICON_TROPHY",
    "PurchasedUiAsset.ICON_SWORDS",
]
for token in required:
    if token not in text:
        raise RuntimeError(f"protected competition contract missing: {token}")

# The hidden Club implementation must remain in source, but must not be routed by the active shell.
shell_end = text.find("@Composable\nprivate fun ClubCompetitionTab()")
shell = text[:shell_end]
if "ClubCompetitionTab()" in shell:
    raise RuntimeError("hidden ClubCompetitionTab was accidentally routed")

PATH.write_text(text)
print("Purchased competition shell, Weekly Cup and Rivals patch applied; backend contracts preserved")
