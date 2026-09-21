from pathlib import Path

ROOT = Path('app/src/main/java/com/sonharf/game')

# --- HOME ---
home_path = ROOT / 'PremiumHomePolish.kt'
home = home_path.read_text()
if 'import androidx.compose.foundation.clickable\n' not in home:
    home = home.replace('import androidx.compose.foundation.background\n', 'import androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable\n', 1)

start = home.index('@Composable\ninternal fun PremiumHomeCommandDeckPolished(')
weekly = home.index('@Composable\ninternal fun PremiumWeeklyBestPolished(', start)
new_home_head = r'''@Composable
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

'''
home = home[:start] + new_home_head + home[weekly:]

weekly_start = home.index('@Composable\ninternal fun PremiumWeeklyBestPolished(')
league_start = home.index('private fun polishedLeagueName', weekly_start)
new_weekly = r'''@Composable
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

'''
home = home[:weekly_start] + new_weekly + home[league_start:]
for token in ['PurchasedUiAsset.ICON_SWORDS', 'PurchasedButton(', 'PurchasedUiAsset.PODIUM_1', 'getWeeklyTopV210(limit = 3)']:
    if token not in home:
        raise SystemExit('Home contract missing: ' + token)
home_path.write_text(home)

# --- PROFILE FRAME STORE CARDS ---
frames_path = ROOT / 'ProfileFramesV2.kt'
frames = frames_path.read_text()
fn = frames.index('internal fun ProfileFramesV2StoreRow(')
tail_start = frames.index('    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {', fn)
new_frames_tail = r'''    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PurchasedSectionHeader(sh("PREMIUM PROFİL ÇERÇEVELERİ", "PREMIUM PROFILE FRAMES"))
        Text(
            sh("Her biri 150 TL • tek ödeme • kalıcı • yalnızca kozmetik", "150 TL each • one-time purchase • permanent • cosmetic only"),
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF765746),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(profileFrameStoreSpecs, key = { it.productId }) { spec ->
                val mine = spec.productId in owned
                val active = equipped?.profileFrameId == spec.productId && mine
                val product = products[spec.productId]
                val realPrice = product?.oneTimePurchaseOfferDetails?.formattedPrice
                val previewVisual = remember(spec.productId) { ProfileFrameV2Catalog.visual(spec.productId, false) }
                PurchasedPanel(
                    modifier = Modifier.width(184.dp),
                    asset = PurchasedUiAsset.PANEL_MEDIUM,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        PurchasedPanel(
                            modifier = Modifier.fillMaxWidth().height(116.dp),
                            asset = if (active) PurchasedUiAsset.REWARD_PANEL else PurchasedUiAsset.PANEL_SMALL,
                            contentPadding = PaddingValues(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(
                                    modifier = Modifier.size(104.dp * previewVisual.photoRatio),
                                    shape = CircleShape,
                                    color = spec.accent.copy(alpha = .14f),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("A", color = spec.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Image(
                                    painter = painterResource(previewVisual.drawable),
                                    contentDescription = null,
                                    modifier = Modifier.size(104.dp),
                                    contentScale = ContentScale.Fit,
                                )
                                if (active) PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.align(Alignment.TopEnd).size(28.dp))
                            }
                        }
                        Text(sh(spec.titleTr, spec.titleEn), color = Color(0xFF4A2D20), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(sh(spec.subtitleTr, spec.subtitleEn), color = Color(0xFF765746), fontSize = 8.sp, minLines = 2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PurchasedAsset(if (mine) PurchasedUiAsset.ICON_CHECK else PurchasedUiAsset.ICON_COIN, Modifier.size(25.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                when {
                                    active -> sh("KULLANILIYOR", "EQUIPPED")
                                    mine -> sh("SATIN ALINDI", "OWNED")
                                    realPrice != null -> realPrice
                                    else -> ProductCatalog.PROFILE_FRAME_FALLBACK_PRICE_TRY
                                },
                                color = if (mine) Color(0xFF2FAE68) else Color(0xFF6B3CA6),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        PurchasedButton(
                            text = if (busy == spec.productId) "…" else if (mine) sh("KULLAN", "EQUIP") else sh("SATIN AL", "BUY"),
                            onClick = action@ {
                                val b = backend ?: return@action
                                scope.launch {
                                    busy = spec.productId
                                    if (mine) {
                                        runCatching { b.equipShopItem(spec.productId) }
                                            .onSuccess {
                                                refresh()
                                                notice = sh("Çerçeve kullanılıyor.", "Frame equipped.")
                                            }
                                            .onFailure { notice = sh("Çerçeve uygulanamadı.", "Frame could not be equipped.") }
                                    } else {
                                        val host = activity
                                        if (host == null || product == null || product.oneTimePurchaseOfferDetails == null) {
                                            notice = sh("Google Play ürünü henüz hazır değil.", "Google Play product is not ready yet.")
                                        } else {
                                            val result = billing.launchProduct(host, product)
                                            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                                                notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not open.")
                                            }
                                        }
                                    }
                                    busy = null
                                }
                            },
                            enabled = backend != null && !active && busy == null && (mine || realPrice != null),
                            modifier = Modifier.fillMaxWidth(),
                            style = if (mine) PurchasedButtonStyle.SECONDARY else PurchasedButtonStyle.PRIMARY,
                            leadingAsset = if (mine) PurchasedUiAsset.ICON_CHECK else PurchasedUiAsset.ICON_COIN,
                        )
                    }
                }
            }
        }
        notice?.let {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(9.dp),
            ) {
                Text(it, modifier = Modifier.fillMaxWidth(), color = Color(0xFF765746), fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
'''
frames = frames[:tail_start] + new_frames_tail
for token in ['PlayPurchaseVerification.verify(productId, purchase.purchaseToken)', 'billing.launchProduct(host, product)', 'PurchasedUiAsset.PANEL_MEDIUM', 'PurchasedButton(']:
    if token not in frames:
        raise SystemExit('Frame store contract missing: ' + token)
frames_path.write_text(frames)

# --- PRO / VIP ---
pro_path = ROOT / 'UnifiedProVipScreen.kt'
pro = pro_path.read_text()
pro_start = pro.index('@Composable\ninternal fun UnifiedProVipScreen(')
new_pro = r'''@Composable
internal fun UnifiedProVipScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showPurchase by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        entitlements = runCatching { backend.getVipEntitlements() }.getOrNull()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    val e = entitlements
    val active = e?.isPro == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PurchasedIconButton(PurchasedUiAsset.ICON_CLOSE, onBack, contentDescription = sh("Geri", "Back"))
                Spacer(Modifier.width(8.dp))
                PurchasedSectionHeader("KELİME KUŞATMASI PRO", Modifier.weight(1f))
            }
        }

        if (loading) item {
            PurchasedProgress(0.45f, Modifier.fillMaxWidth())
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_LARGE,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PurchasedAsset(PurchasedUiAsset.SEASON_BANNER, Modifier.fillMaxWidth().height(86.dp))
                    PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(58.dp))
                    Text(if (active) sh("PRO AKTİF", "PRO ACTIVE") else sh("PRO'YA GEÇ", "GO PRO"), color = Color(0xFF4A2D20), fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(profile?.displayName ?: sh("Oyuncu", "Player"), color = Color(0xFF6B3CA6), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (active) sh(
                            "Reklamsız kullanım ve PRO erişimleri aktif. Ücretli kozmetikler ayrıca satın alınır.",
                            "Ad-free use and PRO access are active. Paid cosmetics remain separate purchases.",
                        ) else sh(
                            "Tek ödeme ile kalıcı PRO: kozmetik, konfor ve prestij. Rekabet avantajı vermez.",
                            "Lifetime PRO with one payment: cosmetics, convenience and prestige. No competitive advantage.",
                        ),
                        color = Color(0xFF765746),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ProAccessCard(PurchasedUiAsset.ICON_CLOSE, sh("REKLAMSIZ", "AD-FREE"), active, Modifier.weight(1f))
                ProAccessCard(PurchasedUiAsset.ICON_SWORDS, sh("SERİ OYUN", "SERIES GAME"), active && e?.seriesGameAccess == true, Modifier.weight(1f))
                ProAccessCard(PurchasedUiAsset.ICON_GAMES, sh("50 OYUN", "50 GAMES"), active && (e?.activeGameLimit ?: 10) >= 50, Modifier.weight(1f))
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_LARGE,
                contentPadding = PaddingValues(17.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(38.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(sh("PRO AYRICALIKLARI", "PRO BENEFITS"), color = Color(0xFF4A2D20), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    ProBenefitRow(PurchasedUiAsset.ICON_CLOSE, sh("Reklamsız kullanım", "Ad-free use"))
                    ProBenefitRow(PurchasedUiAsset.ICON_RANKING, sh("Puan Hesaplayıcı", "Score Calculator"))
                    ProBenefitRow(PurchasedUiAsset.ICON_GAMES, sh("Harf Tablosu", "Letter Table"))
                    ProBenefitRow(PurchasedUiAsset.ICON_SWORDS, sh("Seri Oyun", "Series Game"))
                    ProBenefitRow(PurchasedUiAsset.NAV_SOCIAL, sh("Arkadaş Listesi", "Friends List"))
                    ProBenefitRow(PurchasedUiAsset.ICON_REPEAT, sh("Son Harf tam kelime geçmişi", "Full Son Harf word history"))
                    ProBenefitRow(PurchasedUiAsset.ICON_TROPHY, sh("Aynı anda 50 aktif oyun", "Up to 50 active games"))
                    ProBenefitRow(PurchasedUiAsset.NAV_PROFILE, sh("PRO profil çerçevesi", "PRO profile frame"))
                    ProBenefitRow(PurchasedUiAsset.ICON_CROWN, sh("PRO rozeti ve prestij", "PRO badge and prestige"))
                    ProBenefitRow(PurchasedUiAsset.ICON_COIN, sh("İlk başarılı PRO aktivasyonunda bir kez 100 Son Coin", "100 Son Coins once on the first successful PRO activation"))
                }
            }
        }

        if (!active) {
            item {
                PurchasedButton(
                    text = sh("PRO LIFETIME’I GÖR", "VIEW PRO LIFETIME"),
                    onClick = { showPurchase = true },
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.PURPLE,
                    leadingAsset = PurchasedUiAsset.ICON_CROWN,
                )
            }
        } else {
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.REWARD_PANEL,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(30.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(sh("PRO ERİŞİMİN AKTİF", "YOUR PRO ACCESS IS ACTIVE"), color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }

        notice?.let { message ->
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(11.dp),
                ) {
                    Text(message, Modifier.fillMaxWidth(), color = Color(0xFF4A2D20), fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showPurchase) {
        VipPurchaseDialog(
            onVerified = {
                notice = sh("PRO erişimin doğrulandı.", "Your PRO access was verified.")
                scope.launch { reload() }
            },
            onDismiss = { showPurchase = false },
        )
    }
}

@Composable
private fun ProAccessCard(
    asset: PurchasedUiAsset,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 112.dp),
        asset = if (enabled) PurchasedUiAsset.REWARD_PANEL else PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PurchasedAsset(asset, Modifier.size(38.dp))
            Text(label, color = Color(0xFF4A2D20), fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(if (enabled) sh("AKTİF", "ACTIVE") else "PRO", color = if (enabled) Color(0xFF2FAE68) else Color(0xFF6B3CA6), fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ProBenefitRow(asset: PurchasedUiAsset, text: String) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PurchasedAsset(asset, Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, Modifier.weight(1f), color = Color(0xFF4A2D20), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(25.dp))
        }
    }
}
'''
pro = pro[:pro_start] + new_pro
for token in ['getVipEntitlements()', 'VipPurchaseDialog(', 'PurchasedUiAsset.SEASON_BANNER', 'PurchasedButtonStyle.PURPLE']:
    if token not in pro:
        raise SystemExit('PRO contract missing: ' + token)
pro_path.write_text(pro)

# --- NEW VISUAL CONTRACT TEST ---
test_path = Path('app/src/test/java/com/sonharf/game/PurchasedFinalSurfaceContractTest.kt')
test_path.write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedFinalSurfaceContractTest {
    @Test fun homeCommandDeckUsesPurchasedPanelsAndCtas() {
        val source = File("src/main/java/com/sonharf/game/PremiumHomePolish.kt").readText()
        val deck = source.substringAfter("internal fun PremiumHomeCommandDeckPolished(").substringBefore("internal fun PremiumWeeklyBestPolished(")
        assertTrue(deck.contains("PurchasedUiAsset.PANEL_LARGE"))
        assertTrue(deck.contains("PurchasedAvatarFrame("))
        assertTrue(deck.contains("PurchasedButton("))
        assertTrue(deck.contains("PurchasedProgress("))
        assertFalse(deck.contains("ButtonDefaults.buttonColors"))
        assertFalse(deck.contains("Brush.linearGradient"))
    }

    @Test fun paidProfileFrameStoreCardsUsePurchasedGamePanels() {
        val source = File("src/main/java/com/sonharf/game/ProfileFramesV2.kt").readText()
        val store = source.substringAfter("internal fun ProfileFramesV2StoreRow(")
        assertTrue(store.contains("PurchasedSectionHeader("))
        assertTrue(store.contains("PurchasedUiAsset.PANEL_MEDIUM"))
        assertTrue(store.contains("PurchasedButton("))
        assertTrue(store.contains("billing.launchProduct(host, product)"))
        assertFalse(store.contains("ButtonDefaults.buttonColors"))
    }

    @Test fun proBodyUsesPurchasedPackageInsteadOfMaterialHeroCards() {
        val source = File("src/main/java/com/sonharf/game/UnifiedProVipScreen.kt").readText()
        assertTrue(source.contains("PurchasedUiAsset.SEASON_BANNER"))
        assertTrue(source.contains("PurchasedUiAsset.ICON_CROWN"))
        assertTrue(source.contains("PurchasedButtonStyle.PURPLE"))
        assertTrue(source.contains("PurchasedUiAsset.REWARD_PANEL"))
        assertTrue(source.contains("VipPurchaseDialog("))
        assertFalse(source.contains("Brush.linearGradient"))
        assertFalse(source.contains("ButtonDefaults.buttonColors"))
    }
}
''')

print('Patched final active Home / Store frames / PRO purchased surfaces.')
