package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.getVipEntitlements
import kotlinx.coroutines.launch

@Composable
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
            PurchasedPanel(modifier = Modifier.fillMaxWidth(), asset = PurchasedUiAsset.PANEL_SMALL, contentPadding = PaddingValues(12.dp)) { Text(sh("PRO bilgileri yükleniyor…", "Loading PRO details…"), modifier = Modifier.fillMaxWidth(), color = Color(0xFF765746), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
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
