package com.sonharf.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.getVipEntitlements
import kotlinx.coroutines.launch

private val UProBg: Color get() = SonHarfTheme.Background
private val UProSurface: Color get() = SonHarfTheme.Surface
private val UProBorder: Color get() = SonHarfTheme.Border
private val UProText: Color get() = SonHarfTheme.TextPrimary
private val UProMuted: Color get() = SonHarfTheme.TextSecondary
private val UProBlue: Color get() = SonHarfTheme.Primary
private val UProGold: Color get() = SonHarfTheme.PremiumGold
private val UProGreen: Color get() = SonHarfTheme.Success

@Composable
internal fun UnifiedProVipScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit = {},
    onPrivateRoom: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var entitlementError by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var proVfxKey by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        entitlementError = false
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        runCatching { backend.getVipEntitlements() }
            .onSuccess { entitlements = it }
            .onFailure {
                entitlementError = true
                notice = sh(
                    "PRO hakları şu anda doğrulanamadı. Satın alımın silinmedi; yeniden deneyebilirsin.",
                    "PRO benefits could not be verified. Your purchase was not removed; you can retry.",
                )
            }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    val e = entitlements
    val active = e?.isPro == true || profile?.isVip == true

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = UProText) }
                    Column(Modifier.weight(1f)) {
                        Text("KELİME KUŞATMASI PRO", color = UProText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text(
                            sh("Premier üyelik ve fair-play ayrıcalıkları", "Premier membership and fair-play benefits"),
                            color = UProBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = UProGold, modifier = Modifier.size(30.dp))
                }
            }

            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UProBlue, trackColor = SonHarfTheme.SurfaceElevated) }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MainUiShape.Hero,
                    color = UProSurface,
                    border = BorderStroke(1.dp, if (active) UProGold.copy(alpha = .7f) else UProBorder),
                    shadowElevation = 1.dp,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        MainBadge(
                            text = if (active) sh("PRO AKTİF", "PRO ACTIVE") else sh("FREE PLAN", "FREE PLAN"),
                            accent = if (active) UProGold else UProMuted,
                        )
                        Text(profile?.displayName ?: sh("Oyuncu", "Player"), color = UProText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (active) sh(
                                "Reklamsız deneyim + PRO profil + özel oda + maç-sonu analiz",
                                "Ad-free experience + PRO profile + private rooms + post-match analysis",
                            ) else sh(
                                "PRO ile sosyal, profil ve analiz özelliklerini aç.",
                                "Unlock social, profile, and analysis features with PRO.",
                            ),
                            color = UProMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProAccessCard("🚫", sh("REKLAMSIZ", "AD-FREE"), active && e?.rewardedAdBypass == true, UProBlue, Modifier.weight(1f))
                    ProAccessCard("📊", sh("ANALİZ", "ANALYSIS"), active && e?.postMatchAnalysis == true, UProGreen, Modifier.weight(1f))
                    ProAccessCard("♛", sh("ÖZEL ODA", "PRIVATE ROOM"), active && e?.privateRooms == true, UProGold, Modifier.weight(1f))
                }
            }

            if (active) {
                item {
                    Surface(shape = MainUiShape.Card, color = UProSurface, border = BorderStroke(1.dp, UProBorder)) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MainSectionTitle(sh("PRO ARAÇLARI", "PRO TOOLS"))
                            Text(
                                sh("Satın aldığın özellikleri buradan doğrudan kullanabilirsin.", "Use your purchased benefits directly from here."),
                                color = UProMuted,
                                fontSize = 11.sp,
                            )
                            if (e?.postMatchAnalysis == true) PremiumAnalysisCenterLauncher(Modifier.fillMaxWidth())
                            if (e?.privateRooms == true) {
                                MainGameButton(
                                    text = sh("ÖZEL ODA AÇ / KATIL", "CREATE / JOIN PRIVATE ROOM"),
                                    onClick = onPrivateRoom,
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Rounded.MeetingRoom,
                                )
                            }
                            if (entitlementError || e == null) {
                                MainSecondaryButton(
                                    text = sh("PRO HAKLARINI YENİLE", "REFRESH PRO BENEFITS"),
                                    onClick = { scope.launch { reload() } },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(shape = MainUiShape.Card, color = UProSurface, border = BorderStroke(1.dp, UProBorder)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProLine("🚫", sh("Reklamsız menü, profil ve mağaza", "Ad-free menus, profile and shop"))
                        ProLine("🏷️", sh("PRO rozeti ve profil ayrıcalıkları", "PRO badge and profile benefits"))
                        ProLine("📊", sh("Tamamlanmış maçlar için gelişmiş analiz", "Advanced analysis for completed matches"))
                        ProLine("♛", sh("Son Harf davet kodlu özel oda", "Last Letter invite-code private room"))
                        ProLine("👥", sh("Kaydedilmiş arkadaş listesi", "Saved friend list"))
                        Surface(shape = RoundedCornerShape(12.dp), color = UProGreen.copy(alpha = .09f), border = BorderStroke(1.dp, UProGreen.copy(alpha = .30f))) {
                            Text(
                                sh(
                                    "ADİL REKABET: PRO, dereceli maçlarda skor, hedef harf, kelime ipucu, ek süre veya rating avantajı vermez.",
                                    "FAIR PLAY: PRO gives no score, target-letter, word-hint, extra-time, or rating advantage in ranked matches.",
                                ),
                                Modifier.fillMaxWidth().padding(10.dp),
                                color = UProGreen,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            item {
                if (active) {
                    OutlinedButton(
                        onClick = {
                            val url = "https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MainUiShape.Control,
                        border = BorderStroke(1.dp, UProBlue),
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, tint = UProBlue)
                        Spacer(Modifier.width(7.dp))
                        Text(sh("GOOGLE PLAY'DE YÖNET", "MANAGE ON GOOGLE PLAY"), color = UProBlue, fontWeight = FontWeight.Bold)
                    }
                } else {
                    MainGameButton(
                        text = sh("PRO PLANLARINI GÖR", "VIEW PRO PLANS"),
                        onClick = { showPurchase = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.WorkspacePremium,
                    )
                }
            }

            notice?.let { message ->
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = UProBlue.copy(alpha = .08f), border = BorderStroke(1.dp, UProBorder)) {
                        Text(message, Modifier.fillMaxWidth().padding(11.dp), color = UProText, fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        proVfxKey?.let { key ->
            PurchasedMomentVfx(
                eventKey = key,
                kind = PurchasedMomentVfxKind.PRO,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showPurchase) {
        VipPurchaseDialog(
            onVerified = {
                notice = sh("PRO üyeliğin doğrulandı.", "Your PRO membership was verified.")
                proVfxKey = "pro:${profile?.id ?: "user"}:${System.currentTimeMillis()}"
                SonHarfSoundFx.scoreTick()
                scope.launch { reload() }
            },
            onDismiss = { showPurchase = false },
        )
    }
}

@Composable
private fun ProAccessCard(icon: String, label: String, enabled: Boolean, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = UProSurface, border = BorderStroke(1.dp, accent.copy(alpha = .30f))) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(if (enabled) "✓" else "—", color = if (enabled) UProGreen else UProMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = UProMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ProLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(9.dp))
        Text(text, color = UProText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
