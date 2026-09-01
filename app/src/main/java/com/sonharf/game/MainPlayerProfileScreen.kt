package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
internal fun MainPlayerProfileScreen(
    backend: OnlineGameBackend,
    onEdit: () -> Unit,
    onVip: () -> Unit,
    onSettings: () -> Unit,
    onSocial: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var season by remember { mutableStateOf<CompetitiveSeasonDto?>(null) }
    var records by remember { mutableStateOf<PersonalRecordsDto?>(null) }
    var achievements by remember { mutableStateOf<List<AchievementProgressDto>>(emptyList()) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val metaTask = async { runCatching { backend.getMetaProgressV2() }.getOrNull() }
        val seasonTask = async { runCatching { backend.getCompetitiveSeason() }.getOrNull() }
        val recordsTask = async { runCatching { backend.getPersonalRecords() }.getOrNull() }
        val achievementsTask = async { runCatching { backend.getAchievements() }.getOrDefault(emptyList()) }
        val friendsTask = async { runCatching { backend.getFriends() }.getOrDefault(emptyList()) }
        profile = profileTask.await()
        growth = growthTask.await()
        meta = metaTask.await()
        season = seasonTask.await()
        records = recordsTask.await()
        achievements = achievementsTask.await()
        friends = friendsTask.await()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val p = profile
    val g = growth
    val matches = g?.totalMatches ?: ((p?.wins ?: 0) + (p?.losses ?: 0))
    val wins = g?.wins ?: p?.wins ?: 0
    val losses = g?.losses ?: p?.losses ?: 0
    val rate = if (matches <= 0) 0 else wins * 100 / matches
    val rating = season?.rating ?: p?.rating ?: 1000
    val league = ratingLeagueProgress(rating)
    val onlineFriends = friends.count { (_, friend) -> friend.presenceStatus == "online" }
    val xpProgress = g?.let {
        (it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
    } ?: 0f

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Profil", "Profile"),
                subtitle = sh("Kimliğin, ilerlemen ve başarıların", "Your identity, progress and achievements"),
                actionIcon = Icons.Rounded.Settings,
                actionDescription = sh("Ayarlar", "Settings"),
                onAction = onSettings,
            )
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        ProfilePhotoAvatarWithGender(
                            avatarPath = p?.avatarPath,
                            gender = p?.gender,
                            name = p?.displayName ?: sh("Oyuncu", "Player"),
                            size = 106.dp,
                            accent = if (p?.isVip == true) MainUi.Gold else MainUi.Blue,
                        )
                        Surface(
                            modifier = Modifier.size(37.dp).clickable(onClick = onEdit),
                            shape = CircleShape,
                            color = MainUi.Blue,
                            border = BorderStroke(2.dp, Color.White),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Text(
                        p?.displayName ?: sh("Oyuncu profili", "Player profile"),
                        color = MainUi.Text,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Surface(shape = RoundedCornerShape(9.dp), color = MainUi.BlueSoft) {
                            Text(
                                meta?.selectedTitle ?: g?.nextTitle ?: sh("ÇAYLAK", "ROOKIE"),
                                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                color = MainUi.Blue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        if (p?.isVip == true) {
                            Surface(shape = RoundedCornerShape(9.dp), color = MainUi.Gold.copy(alpha = .14f)) {
                                Text("VIP", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    TextButton(onClick = onEdit) {
                        Text(sh("PROFİLİ DÜZENLE", "EDIT PROFILE"), color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MainUi.BlueSoft,
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${sh("SEVİYE", "LEVEL")} ${g?.level ?: 1}", color = MainUi.Blue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("${g?.xp ?: 0} XP", color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = MainUi.Blue,
                        trackColor = Color.White,
                    )
                    Text(
                        "${g?.levelProgress ?: 0}/${g?.levelTarget ?: 500} ${sh("sonraki seviyeye", "to next level")}",
                        color = MainUi.Muted,
                        fontSize = 9.sp,
                    )
                }
            }
        }

        item {
            MainSectionTitle(sh("REKABET PROFİLİ", "COMPETITIVE PROFILE"))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainMetricCard(rating.toString(), "Rating", Modifier.weight(1f))
                MainMetricCard(league.leagueName, sh("Lig", "League"), Modifier.weight(1f))
                MainMetricCard("◈ ${p?.diamonds ?: 0}", "Son Coin", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainMetricCard(wins.toString(), sh("Galibiyet", "Wins"), Modifier.weight(1f))
                MainMetricCard(losses.toString(), sh("Mağlubiyet", "Losses"), Modifier.weight(1f))
                MainMetricCard("%$rate", sh("Galibiyet", "Win rate"), Modifier.weight(1f))
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MainInlineStat("${g?.currentWinStreak ?: 0}", sh("Mevcut seri", "Current streak"), Color(0xFFF97316))
                    VerticalDivider(Modifier.height(42.dp), color = MainUi.Border)
                    MainInlineStat("${g?.bestStreak ?: records?.bestStreak ?: 0}", sh("En uzun seri", "Best streak"), MainUi.Gold)
                    VerticalDivider(Modifier.height(42.dp), color = MainUi.Border)
                    MainInlineStat(matches.toString(), sh("Toplam maç", "Total matches"), MainUi.Blue)
                }
            }
        }

        records?.let { r ->
            item {
                MainSectionTitle(sh("KİŞİSEL REKORLAR", "PERSONAL RECORDS"))
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MainUi.Surface,
                    border = BorderStroke(1.dp, MainUi.Border),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MainRecordLine("🔤", sh("En uzun kelime", "Longest word"), r.longestWord.ifBlank { "—" }.uppercase(), "${r.longestWordLength} ${sh("harf", "letters")}")
                        MainRecordLine("⚡", sh("En iyi skor", "Best score"), r.bestClassicScore.toString(), sh("Son Harf düellosu", "Son Harf duel"))
                        MainRecordLine("↗", sh("En büyük fark", "Biggest margin"), "+${r.biggestWinMargin}", sh("Galibiyet farkı", "Winning margin"))
                    }
                }
            }
        }

        item {
            MainSectionTitle(
                sh("ARKADAŞLAR", "FRIENDS"),
                sh("TÜMÜNÜ GÖR", "VIEW ALL"),
                onSocial,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSocial),
                shape = RoundedCornerShape(18.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MainUi.BlueSoft) {
                        Icon(Icons.Rounded.Groups, null, tint = MainUi.Blue, modifier = Modifier.padding(9.dp).size(21.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${friends.size} ${sh("arkadaş", "friends")}", color = MainUi.Text, fontWeight = FontWeight.Black)
                        Text("$onlineFriends ${sh("çevrimiçi", "online")}", color = if (onlineFriends > 0) MainUi.Green else MainUi.Muted, fontSize = 10.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MainUi.Muted)
                }
            }
        }

        item {
            MainSectionTitle(sh("BAŞARIMLAR", "ACHIEVEMENTS"))
            Spacer(Modifier.height(8.dp))
            if (achievements.isEmpty()) {
                Text(sh("Başarım ilerlemesi henüz oluşmadı.", "Achievement progress is not available yet."), color = MainUi.Muted, fontSize = 10.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    achievements.take(4).forEach { achievement ->
                        val progress = (achievement.currentValue.toFloat() / achievement.target.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MainUi.Surface,
                            border = BorderStroke(1.dp, if (achievement.unlocked) MainUi.Gold.copy(alpha = .45f) else MainUi.Border),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(achievement.icon, fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (SonHarfUiState.isEnglish) achievement.titleEn else achievement.titleTr,
                                        color = MainUi.Text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(if (achievement.unlocked) "✓" else "${achievement.currentValue}/${achievement.target}", color = if (achievement.unlocked) MainUi.Green else MainUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                    color = if (achievement.unlocked) MainUi.Gold else MainUi.Blue,
                                    trackColor = MainUi.SurfaceSoft,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            MainSectionTitle(sh("TEMA & GÖRÜNÜM", "THEME & APPEARANCE"))
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Box(Modifier.fillMaxWidth().padding(14.dp)) {
                    ProfileThemeSelector(backend)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(
                    onClick = onVip,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .55f)),
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = MainUi.Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("VIP", color = MainUi.Text, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .35f)),
                ) {
                    Icon(Icons.Rounded.Settings, null, tint = MainUi.Blue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(sh("AYARLAR", "SETTINGS"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
        item { Spacer(Modifier.height(5.dp)) }
    }
}

@Composable
private fun MainInlineStat(value: String, label: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 72.dp)) {
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, color = MainUi.Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MainRecordLine(icon: String, label: String, value: String, detail: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = MainUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(detail, color = MainUi.Muted, fontSize = 9.sp)
    }
}
