package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Player profile rendered with the real purchased Casual Game UI asset system. */
@Composable
internal fun MainPlayerProfileScreen(
    backend: OnlineGameBackend,
    onEdit: () -> Unit,
    onVip: () -> Unit,
    onCollection: () -> Unit,
    onSettings: () -> Unit,
    onSocial: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var friendCount by remember { mutableIntStateOf(0) }
    var onlineFriendCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val entitlementTask = async { runCatching { backend.getVipEntitlements() }.getOrNull() }
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val friendsTask = async { runCatching { backend.getFriends() }.getOrDefault(emptyList()) }
        val cosmeticsTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }
        val inventoryTask = async { runCatching { backend.getInventory() }.getOrDefault(emptySet()) }

        profile = profileTask.await()
        entitlements = entitlementTask.await()
        growth = growthTask.await()
        friendsTask.await().let { friends ->
            friendCount = friends.size
            onlineFriendCount = friends.count { (_, friend) -> friend.presenceStatus == "online" }
        }
        SonHarfCosmetics.apply(cosmeticsTask.await(), inventoryTask.await())
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val p = profile
    val g = growth
    val matches = g?.totalMatches ?: ((p?.wins ?: 0) + (p?.losses ?: 0))
    val wins = g?.wins ?: p?.wins ?: 0
    val losses = g?.losses ?: p?.losses ?: 0
    val winRate = if (matches <= 0) 0 else wins * 100 / matches
    val rating = p?.rating ?: 1000
    val league = ratingLeagueProgress(rating)
    val level = g?.level ?: 1
    val xp = g?.xp ?: 0
    val levelProgress = g?.levelProgress ?: 0
    val levelTarget = g?.levelTarget?.coerceAtLeast(1) ?: 500
    val xpProgress = (levelProgress.toFloat() / levelTarget).coerceIn(0f, 1f)
    val isPro = entitlements?.isPro == true
    val playerName = p?.displayName ?: sh("Oyuncu", "Player")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                PurchasedSectionHeader(sh("OYUNCU PROFİLİ", "PLAYER PROFILE"), Modifier.fillMaxWidth().padding(horizontal = 48.dp))
                PurchasedIconButton(
                    asset = PurchasedUiAsset.ICON_SETTINGS,
                    onClick = onSettings,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    contentDescription = sh("Ayarlar", "Settings"),
                )
            }
        }

        if (loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = Color(0xFF58B957),
                    trackColor = Color(0xFFDEC59B),
                )
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_LARGE,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 21.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAvatarFrame(Modifier.size(116.dp)) {
                            FramedProfilePhotoAvatar(
                                avatarPath = p?.avatarPath,
                                gender = p?.gender,
                                name = playerName,
                                size = 94.dp,
                                frameId = SonHarfCosmetics.profileFrameId,
                                accent = if (isPro) SonHarfTheme.Purple else SonHarfTheme.Primary,
                                visible = p?.avatarVisibility != "hidden",
                                isPro = isPro,
                            )
                        }
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    playerName,
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFF4A2D20),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isPro) {
                                    PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(34.dp))
                                }
                            }
                            Text(
                                g?.nextTitle ?: sh("OYUNCU", "PLAYER"),
                                color = Color(0xFF7D4BB2),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                "${league.leagueName} • $rating",
                                color = Color(0xFF765746),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            PurchasedButton(
                                text = sh("PROFİLİ DÜZENLE", "EDIT PROFILE"),
                                onClick = onEdit,
                                modifier = Modifier.fillMaxWidth(),
                                style = PurchasedButtonStyle.SECONDARY,
                                leadingAsset = PurchasedUiAsset.NAV_PROFILE,
                            )
                        }
                    }

                    if (isPro) {
                        Row(
                            Modifier.fillMaxWidth().clickable(onClick = onVip),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(30.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("PRO", color = Color(0xFF7D4BB2), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(7.dp))
                            Text(
                                sh("Üyelik aktif • ayrıcalıkları aç", "Membership active • open benefits"),
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF765746),
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }
        }

        item { PurchasedSectionHeader(sh("İLERLEME", "PROGRESS")) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PurchasedProfileMetric(
                    value = rating.toString(),
                    label = sh("Puan", "Rating"),
                    asset = PurchasedUiAsset.ICON_TROPHY,
                    modifier = Modifier.weight(1f),
                )
                PurchasedProfileMetric(
                    value = league.leagueName,
                    label = sh("Lig", "League"),
                    asset = PurchasedUiAsset.ICON_RANKING,
                    modifier = Modifier.weight(1f),
                )
                PurchasedProfileMetric(
                    value = level.toString(),
                    label = sh("Seviye", "Level"),
                    asset = PurchasedUiAsset.ICON_CROWN,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_MEDIUM,
                contentPadding = PaddingValues(horizontal = 19.dp, vertical = 18.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("SEVİYE İLERLEMESİ", "LEVEL PROGRESS"), modifier = Modifier.weight(1f), color = Color(0xFF4A2D20), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("$xp XP", color = Color(0xFF6B3CA6), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    PurchasedProgress(xpProgress)
                    Text(
                        "$levelProgress / $levelTarget ${sh("sonraki seviyeye", "to next level")}",
                        color = Color(0xFF765746),
                        fontSize = 9.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PurchasedInlineStat(wins.toString(), sh("Galibiyet", "Wins"))
                        PurchasedInlineStat(losses.toString(), sh("Mağlubiyet", "Losses"))
                        PurchasedInlineStat("%$winRate", sh("Kazanma", "Win rate"))
                    }
                }
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSocial),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.NAV_SOCIAL, Modifier.size(54.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sh("ARKADAŞLAR", "FRIENDS"), color = Color(0xFF4A2D20), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(
                            "$friendCount ${sh("arkadaş", "friends")} • $onlineFriendCount ${sh("çevrimiçi", "online")}",
                            color = Color(0xFF765746),
                            fontSize = 9.sp,
                        )
                    }
                    PurchasedAsset(PurchasedUiAsset.ICON_CHAT, Modifier.size(40.dp))
                }
            }
        }

        item {
            PurchasedButton(
                text = sh("KOLEKSİYONUM", "MY COLLECTION"),
                onClick = onCollection,
                modifier = Modifier.fillMaxWidth(),
                style = PurchasedButtonStyle.PURPLE,
                leadingAsset = PurchasedUiAsset.ICON_CROWN,
            )
        }

        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun PurchasedProfileMetric(
    value: String,
    label: String,
    asset: PurchasedUiAsset,
    modifier: Modifier = Modifier,
) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 104.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PurchasedAsset(asset, Modifier.size(31.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color(0xFF4A2D20), fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = Color(0xFF765746), fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun PurchasedInlineStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 74.dp)) {
        Text(value, color = Color(0xFF4A2D20), fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color(0xFF765746), fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}
