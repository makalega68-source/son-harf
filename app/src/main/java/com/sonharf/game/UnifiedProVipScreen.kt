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
                    Text(sh("Premium üyelik", "Premium membership"), color = SonHarfTheme.Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                listOf(
                                    SonHarfTheme.Primary,
                                    SonHarfTheme.Purple,
                                    SonHarfTheme.Turquoise,
                                )
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
                            Icon(
                                Icons.Rounded.WorkspacePremium,
                                null,
                                tint = Color.White,
                                modifier = Modifier.padding(14.dp).size(32.dp),
                            )
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
                                "Reklamsız kullanım ve PRO erişimleri aktif. Ücretli kozmetikler ayrıca satın alınır.",
                                "Ad-free use and PRO access are active. Paid cosmetics remain separate purchases.",
                            ) else sh(
                                "Daha temiz, daha kişisel ve daha premium bir oyun deneyimi.",
                                "A cleaner, more personal and more premium game experience.",
                            ),
                            color = Color.White.copy(alpha = .82f),
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
                    icon = Icons.Rounded.Block,
                    label = sh("REKLAMSIZ", "AD-FREE"),
                    enabled = active && e?.rewardedAdBypass == true,
                    accent = SonHarfTheme.Primary,
                    modifier = Modifier.weight(1f),
                )
                ProAccessCard(
                    icon = Icons.Rounded.BarChart,
                    label = sh("ANALİZ", "ANALYSIS"),
                    enabled = active && e?.postMatchAnalysis == true,
                    accent = SonHarfTheme.Turquoise,
                    modifier = Modifier.weight(1f),
                )
                ProAccessCard(
                    icon = Icons.Rounded.MeetingRoom,
                    label = sh("ÖZEL ODA", "PRIVATE ROOM"),
                    enabled = active && e?.privateRooms == true,
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
                    Text(
                        sh("PRO AYRICALIKLARI", "PRO BENEFITS"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    ProBenefitRow(Icons.Rounded.Palette, sh("PRO Style ve profil ayrıcalıkları", "PRO Style and profile benefits"), SonHarfTheme.Purple)
                    ProBenefitRow(Icons.Rounded.AutoGraph, sh("Gelişmiş istatistik ve maç analizi", "Advanced stats and match analysis"), SonHarfTheme.Turquoise)
                    ProBenefitRow(Icons.Rounded.Groups, sh("Sosyal ve arkadaş ayrıcalıkları", "Social and friend benefits"), SonHarfTheme.Primary)
                    ProBenefitRow(Icons.Rounded.DoorFront, sh("Özel oda erişimi", "Private room access"), SonHarfTheme.ActionOrange)
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
                    Text(sh("PRO LIFETIME’I GÖR", "VIEW PRO LIFETIME"), fontWeight = FontWeight.Black)
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
                        Text(sh("PRO ÜYELİĞİN AKTİF", "YOUR PRO MEMBERSHIP IS ACTIVE"), color = SonHarfTheme.Turquoise, fontWeight = FontWeight.Black, fontSize = 11.sp)
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
                notice = sh("PRO üyeliğin doğrulandı.", "Your PRO membership was verified.")
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
