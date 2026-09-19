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
    val active = e?.isPro == true || profile?.isVip == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = SonHarfTheme.Surface,
                    border = BorderStroke(1.dp, SonHarfTheme.Border),
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = SonHarfTheme.TextPrimary, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("KELİME KUŞATMASI PRO", color = SonHarfTheme.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(sh("Tek ödeme • Kalıcı PRO", "One payment • Lifetime PRO"), color = SonHarfTheme.Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfTheme.Purple, modifier = Modifier.size(30.dp))
            }
        }

        if (loading) item {
            LinearProgressIndicator(
                Modifier.fillMaxWidth(),
                color = SonHarfTheme.Turquoise,
                trackColor = SonHarfTheme.SurfaceSecondary,
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                shadowElevation = 6.dp,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(SonHarfTheme.Primary, SonHarfTheme.Purple, SonHarfTheme.Turquoise)
                            ),
                            RoundedCornerShape(28.dp),
                        )
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f)) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.padding(14.dp).size(32.dp))
                        }
                        Text(
                            if (active) sh("PRO AKTİF", "PRO ACTIVE") else sh("PRO'YA GEÇ", "GO PRO"),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            profile?.displayName ?: sh("Oyuncu", "Player"),
                            color = Color.White.copy(alpha = .92f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (active) sh(
                                "Premium araçlar, sosyal kolaylıklar, prestij ve reklamsız kullanım aktif.",
                                "Premium tools, social conveniences, prestige and ad-free use are active.",
                            ) else sh(
                                "Bilgi, kolaylık ve prestij odaklı kalıcı paket. Maç gücü satmaz.",
                                "A lifetime package focused on information, convenience and prestige. It never sells match power.",
                            ),
                            color = Color.White.copy(alpha = .84f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ProAccessCard(
                    icon = Icons.Rounded.Calculate,
                    label = sh("PUAN", "SCORE"),
                    enabled = e?.scoreCalculatorAccess == true,
                    accent = SonHarfTheme.Primary,
                    modifier = Modifier.weight(1f),
                )
                ProAccessCard(
                    icon = Icons.Rounded.GridView,
                    label = sh("HARFLER", "LETTERS"),
                    enabled = e?.letterTableAccess == true,
                    accent = SonHarfTheme.Turquoise,
                    modifier = Modifier.weight(1f),
                )
                ProAccessCard(
                    icon = Icons.Rounded.Timer,
                    label = sh("SERİ OYUN", "SERIES"),
                    enabled = e?.seriesGameAccess == true,
                    accent = SonHarfTheme.Purple,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = SonHarfTheme.Surface,
                border = BorderStroke(1.dp, SonHarfTheme.Border),
                shadowElevation = 2.dp,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(sh("PRO AYRICALIKLARI", "PRO BENEFITS"), color = SonHarfTheme.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    ProBenefitRow(Icons.Rounded.Block, sh("Reklamsız kullanım", "Ad-free use"), SonHarfTheme.Primary)
                    ProBenefitRow(Icons.Rounded.Calculate, sh("Puan Hesaplayıcı", "Score Calculator"), SonHarfTheme.Primary)
                    ProBenefitRow(Icons.Rounded.GridView, sh("Harf Tablosu", "Letter Table"), SonHarfTheme.Turquoise)
                    ProBenefitRow(Icons.Rounded.Timer, sh("Seri Oyun • 3 / 5 / 10 dakika", "Series Game • 3 / 5 / 10 minutes"), SonHarfTheme.Purple)
                    ProBenefitRow(Icons.Rounded.Groups, sh("Arkadaş listesi ve arkadaş davetleri", "Friends list and friend invites"), SonHarfTheme.Primary)
                    ProBenefitRow(Icons.Rounded.History, sh("Son Harf tam kelime geçmişi", "Full Son Harf word history"), SonHarfTheme.Turquoise)
                    ProBenefitRow(Icons.Rounded.SportsEsports, sh("Aynı anda 50 aktif oyun", "50 active games at once"), SonHarfTheme.ActionOrange)
                    ProBenefitRow(Icons.Rounded.WorkspacePremium, sh("Özel PRO profil çerçevesi ve rozeti", "Exclusive PRO profile frame and badge"), SonHarfTheme.Purple)
                    ProBenefitRow(Icons.Rounded.Stars, sh("İlk başarılı grant'te 100 Son Coin", "100 Son Coins on the first successful grant"), SonHarfTheme.ActionOrange)
                }
            }
        }

        if (!active) {
            item {
                Button(
                    onClick = { showPurchase = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Purple, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(sh("PRO'YU SATIN AL", "BUY PRO"), fontWeight = FontWeight.Black)
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    color = SonHarfTheme.Turquoise.copy(alpha = .10f),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = SonHarfTheme.Turquoise, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(sh("KALICI PRO AKTİF", "LIFETIME PRO ACTIVE"), color = SonHarfTheme.Turquoise, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = SonHarfTheme.PrimarySoft) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = SonHarfTheme.TextPrimary, fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showPurchase) {
        VipPurchaseDialog(
            onVerified = {
                notice = sh("Kalıcı PRO erişimin doğrulandı.", "Your lifetime PRO access was verified.")
                showPurchase = false
                scope.launch { reload() }
            },
            onDismiss = { showPurchase = false },
        )
    }
}

@Composable
private fun ProAccessCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    accent: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .24f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 7.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .10f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(19.dp))
            }
            Text(label, color = SonHarfTheme.TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(
                if (enabled) sh("AKTİF", "ACTIVE") else sh("PRO", "PRO"),
                color = if (enabled) SonHarfTheme.Turquoise else SonHarfTheme.TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ProBenefitRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(11.dp), color = accent.copy(alpha = .10f)) {
            Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Rounded.Check, null, tint = SonHarfTheme.Turquoise, modifier = Modifier.size(17.dp))
    }
}
