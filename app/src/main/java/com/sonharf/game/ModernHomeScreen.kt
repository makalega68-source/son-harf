package com.sonharf.game

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GrowthDashboardDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.WeeklyTopPlayerV210

private val ModernHomeBg = Color(0xFFF4F7F6)
private val ModernHomeCard = Color(0xFFFFFFFF)
private val ModernHomeInk = Color(0xFF18211E)
private val ModernHomeMuted = Color(0xFF6B7772)
private val ModernHomeBorder = Color(0xFFDDE6E2)
private val ModernHomeTeal = Color(0xFF176C61)
private val ModernHomeTealDark = Color(0xFF104F48)
private val ModernHomeTealSoft = Color(0xFFE1F1ED)
private val ModernHomeBlue = Color(0xFF356C9D)
private val ModernHomeBlueSoft = Color(0xFFE7F0F8)
private val ModernHomeGold = Color(0xFFC49335)
private val ModernHomeGoldSoft = Color(0xFFF7EEDB)
private val ModernHomePurple = Color(0xFF7564A7)
private val ModernHomePurpleSoft = Color(0xFFEEEAF7)
private val ModernHomeCoral = Color(0xFFC76551)
private val ModernHomeCoralSoft = Color(0xFFF8E8E4)
private val ModernHomeShape = RoundedCornerShape(22.dp)
private val ModernHomeSmallShape = RoundedCornerShape(16.dp)

@Composable
internal fun ModernAdultHome(
    backend: OnlineGameBackend,
    profile: ProfileDto?,
    siegeLanguage: String,
    lastLetterLanguage: String,
    letterPathLanguage: String,
    seriesLanguage: String,
    seriesAccess: Boolean,
    onSiegeLanguage: (String) -> Unit,
    onLastLetterLanguage: (String) -> Unit,
    onLetterPathLanguage: (String) -> Unit,
    onSeriesLanguage: (String) -> Unit,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
    onSeries: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
    onSocial: () -> Unit,
    onPro: () -> Unit,
    onCollection: () -> Unit,
) {
    var dashboard by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var streakDays by remember { mutableIntStateOf(0) }
    var weeklyTop by remember { mutableStateOf<List<WeeklyTopPlayerV210>>(emptyList()) }
    var weeklyLoading by remember { mutableStateOf(true) }

    LaunchedEffect(backend) {
        dashboard = runCatching { backend.getGrowthDashboard() }.getOrNull()
        streakDays = runCatching { backend.getMetaProgressV2().dailyPlayStreak }.getOrDefault(0)
        weeklyTop = runCatching { backend.getWeeklyTopV210(limit = 3) }.getOrDefault(emptyList()).take(3)
        weeklyLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ModernHomeBg),
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 640.dp).align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ModernProfileBar(profile = profile, onProfile = onProfile, onSocial = onSocial)
            }
            item {
                ModernSiegeHero(
                    language = siegeLanguage,
                    onLanguage = onSiegeLanguage,
                    onPlay = onSiege,
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModernModeCard(
                        modifier = Modifier.weight(1f),
                        title = sh("Son Harf", "Last Letter"),
                        subtitle = sh("Hızlı 1v1 düello", "Fast 1v1 duel"),
                        icon = Icons.Rounded.TextFields,
                        accent = ModernHomeBlue,
                        soft = ModernHomeBlueSoft,
                        language = lastLetterLanguage,
                        onLanguage = onLastLetterLanguage,
                        onPlay = onLastLetter,
                    )
                    ModernModeCard(
                        modifier = Modifier.weight(1f),
                        title = sh("Harf Yolu", "Letter Path"),
                        subtitle = sh("5 harfli rota", "5-letter route"),
                        icon = Icons.Rounded.Route,
                        accent = ModernHomePurple,
                        soft = ModernHomePurpleSoft,
                        language = letterPathLanguage,
                        onLanguage = onLetterPathLanguage,
                        onPlay = onLetterPath,
                    )
                }
            }
            if (seriesAccess) {
                item {
                    ModernSeriesStrip(
                        language = seriesLanguage,
                        onLanguage = onSeriesLanguage,
                        onPlay = onSeries,
                    )
                }
            }
            item {
                ModernDailyProgress(dashboard = dashboard, streakDays = streakDays)
            }
            item {
                ModernWeeklyBest(players = weeklyTop, loading = weeklyLoading, onClick = onCompete)
            }
            item {
                ModernQuickAccess(onCompete = onCompete, onPro = onPro, onCollection = onCollection)
            }
        }
    }
}

@Composable
private fun ModernProfileBar(profile: ProfileDto?, onProfile: () -> Unit, onSocial: () -> Unit) {
    val name = profile?.displayName?.ifBlank { null } ?: sh("Oyuncu", "Player")
    val league = profile?.let { modernLeagueName(ratingLeagueProgress(it.rating).leagueName) } ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(58.dp).clickable(onClick = onProfile),
            shape = CircleShape,
            color = ModernHomeCard,
            border = BorderStroke(2.dp, if (profile?.isVip == true) ModernHomeGold else ModernHomeTealSoft),
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                FramedProfilePhotoAvatar(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = name,
                    size = 52.dp,
                    frameId = null,
                    accent = ModernHomeTeal,
                    visible = profile?.avatarVisibility != "hidden",
                    isPro = profile?.isVip == true,
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = ModernHomeInk,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile?.isVip == true) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = ModernHomeGold, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (profile == null) sh("Profil yükleniyor", "Loading profile") else "$league  •  ${profile.rating} RP",
                color = ModernHomeMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        ModernBalancePill(profile?.diamonds)
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onSocial,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = ModernHomeCard,
            border = BorderStroke(1.dp, ModernHomeBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Groups, sh("Sosyal", "Social"), tint = ModernHomeTealDark, modifier = Modifier.size(21.dp))
            }
        }
    }
}

@Composable
private fun ModernBalancePill(value: Int?) {
    Surface(shape = RoundedCornerShape(99.dp), color = ModernHomeCard, border = BorderStroke(1.dp, ModernHomeBorder)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Diamond, null, tint = ModernHomeBlue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(value?.toString() ?: "—", color = ModernHomeInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModernSiegeHero(language: String, onLanguage: (String) -> Unit, onPlay: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ModernHomeTealDark,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .10f)) {
                    Icon(
                        Icons.Rounded.GridView,
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(11.dp).size(25.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("ANA OYUN", "MAIN GAME"),
                        color = Color(0xFFA7D8D0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Kelime Kuşatması",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        sh("Kelime oyunu + taktik alan savaşı", "Word game + tactical territory battle"),
                        color = Color.White.copy(alpha = .76f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
                ModernLanguageSegment(language, onLanguage, dark = true)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("Kelimeyi kur. Alanı ele geçir.", "Build the word. Claim territory."),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        sh("Rakibini haritada geride bırak.", "Outplay your rival on the map."),
                        color = Color.White.copy(alpha = .67f),
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onPlay,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF68B985), contentColor = Color(0xFF0E342D)),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                ) {
                    Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ModernModeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    soft: Color,
    language: String,
    onLanguage: (String) -> Unit,
    onPlay: () -> Unit,
) {
    Surface(
        modifier = modifier.heightIn(min = 178.dp),
        shape = ModernHomeShape,
        color = ModernHomeCard,
        border = BorderStroke(1.dp, ModernHomeBorder),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = soft) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(21.dp))
                }
                Spacer(Modifier.weight(1f))
                ModernLanguageSegment(language, onLanguage, dark = false)
            }
            Spacer(Modifier.height(13.dp))
            Text(title, color = ModernHomeInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = ModernHomeMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = soft,
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(sh("Oyna", "Play"), color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.ArrowForward, null, tint = accent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ModernLanguageSegment(language: String, onLanguage: (String) -> Unit, dark: Boolean) {
    val bg = if (dark) Color.White.copy(alpha = .10f) else ModernHomeBg
    Surface(shape = RoundedCornerShape(10.dp), color = bg) {
        Row(Modifier.padding(3.dp)) {
            ModernLanguageChoice("T", language == "tr", dark) { onLanguage("tr") }
            ModernLanguageChoice("E", language == "en", dark) { onLanguage("en") }
        }
    }
}

@Composable
private fun ModernLanguageChoice(text: String, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val selectedBg = if (dark) Color.White else ModernHomeTeal
    val selectedText = if (dark) ModernHomeTealDark else Color.White
    val idleText = if (dark) Color.White.copy(alpha = .72f) else ModernHomeMuted
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (selected) selectedBg else Color.Transparent) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            color = if (selected) selectedText else idleText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun ModernSeriesStrip(language: String, onLanguage: (String) -> Unit, onPlay: () -> Unit) {
    Surface(shape = ModernHomeSmallShape, color = ModernHomeCard, border = BorderStroke(1.dp, ModernHomeBorder)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = ModernHomeCoralSoft) {
                Icon(Icons.Rounded.MilitaryTech, null, tint = ModernHomeCoral, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Kuşatma Serisi", "Siege Series"), color = ModernHomeInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(sh("Seri rekabet modu", "Competitive series mode"), color = ModernHomeMuted, fontSize = 10.sp)
            }
            ModernLanguageSegment(language, onLanguage, dark = false)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onPlay) { Icon(Icons.Rounded.ArrowForward, null, tint = ModernHomeCoral) }
        }
    }
}

@Composable
private fun ModernDailyProgress(dashboard: GrowthDashboardDto?, streakDays: Int) {
    val matches = dashboard?.matchesToday?.coerceIn(0, 3) ?: 0
    val checkInDone = dashboard?.dailyClaimed == true
    val challengeDone = dashboard?.dailyChallengeClaimed == true || matches >= 3
    val completed = (if (checkInDone) 1 else 0) + (if (challengeDone) 1 else 0)
    val progress = if (dashboard == null) 0f else (((if (checkInDone) 1f else 0f) + matches / 3f) / 2f).coerceIn(0f, 1f)

    Surface(shape = ModernHomeShape, color = ModernHomeCard, border = BorderStroke(1.dp, ModernHomeBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = ModernHomeGoldSoft) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = ModernHomeGold, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Bugünkü ilerleme", "Today's progress"), color = ModernHomeInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (dashboard == null) sh("Görevler yükleniyor", "Loading tasks") else sh("$completed / 2 görev tamamlandı", "$completed / 2 tasks completed"),
                        color = ModernHomeMuted,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    sh("$streakDays gün seri", "$streakDays day streak"),
                    color = ModernHomeCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = ModernHomeTeal,
                trackColor = ModernHomeTealSoft,
            )
        }
    }
}

@Composable
private fun ModernWeeklyBest(players: List<WeeklyTopPlayerV210>, loading: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ModernHomeShape,
        color = ModernHomeCard,
        border = BorderStroke(1.dp, ModernHomeBorder),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = ModernHomeGoldSoft) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = ModernHomeGold, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Haftanın en iyileri", "Weekly best"), color = ModernHomeInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(sh("Haftalık rekabet sıralaması", "Weekly competition ranking"), color = ModernHomeMuted, fontSize = 10.sp)
                }
                Icon(Icons.Rounded.ArrowForward, null, tint = ModernHomeMuted, modifier = Modifier.size(18.dp))
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ModernHomeTeal, strokeWidth = 2.dp)
                }
            } else if (players.isEmpty()) {
                Text(sh("Henüz haftalık sıralama verisi yok.", "No weekly ranking data yet."), color = ModernHomeMuted, fontSize = 11.sp)
            } else {
                players.forEachIndexed { index, player ->
                    if (index > 0) HorizontalDivider(color = ModernHomeBorder.copy(alpha = .7f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", color = if (index == 0) ModernHomeGold else ModernHomeMuted, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.width(22.dp))
                        ProfilePhotoAvatarWithGender(
                            avatarPath = player.avatarUrl,
                            gender = null,
                            name = player.username,
                            size = 34.dp,
                            accent = if (index == 0) ModernHomeGold else ModernHomeTeal,
                            visible = true,
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(player.username.ifBlank { sh("Oyuncu", "Player") }, color = ModernHomeInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${player.rp} RP", color = ModernHomeMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernQuickAccess(onCompete: () -> Unit, onPro: () -> Unit, onCollection: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(sh("Hızlı erişim", "Quick access"), color = ModernHomeInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernQuickTile(Modifier.weight(1f), sh("Lig", "League"), Icons.Rounded.EmojiEvents, ModernHomeBlue, ModernHomeBlueSoft, onCompete)
            ModernQuickTile(Modifier.weight(1f), "PRO", Icons.Rounded.WorkspacePremium, ModernHomeGold, ModernHomeGoldSoft, onPro)
            ModernQuickTile(Modifier.weight(1f), sh("Koleksiyon", "Collection"), Icons.Rounded.Inventory2, ModernHomePurple, ModernHomePurpleSoft, onCollection)
        }
    }
}

@Composable
private fun ModernQuickTile(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    accent: Color,
    soft: Color,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier, shape = ModernHomeSmallShape, color = ModernHomeCard, border = BorderStroke(1.dp, ModernHomeBorder)) {
        Column(Modifier.padding(vertical = 13.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = soft) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(19.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(label, color = ModernHomeInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun modernLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
