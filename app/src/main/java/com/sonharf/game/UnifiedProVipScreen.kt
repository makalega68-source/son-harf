package com.sonharf.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.getVipEntitlements
import kotlinx.coroutines.launch

@Composable
internal fun UnifiedProVipScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit = {},
    onPrivateRoom: () -> Unit = {},
    onSeries: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var entitlementError by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

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

    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = "KELİME KUŞATMASI PRO",
            subtitle = sh(
                "Premium görünüm, konfor ve sosyal ayrıcalıklar",
                "Premium appearance, comfort and social benefits",
            ),
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = GameSpacing.ScreenHorizontal,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = GameColors.PrimaryBlue,
                        trackColor = GameColors.SecondarySurface,
                    )
                }
            }

            item {
                GameSurface(
                    elevated = true,
                    borderColor = if (active) {
                        GameColors.RewardAmber.copy(alpha = .58f)
                    } else {
                        GameColors.PrimaryBlue.copy(alpha = .32f)
                    },
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (active) {
                                GameColors.RewardAmber.copy(alpha = .15f)
                            } else {
                                GameColors.PrimaryBlue.copy(alpha = .12f)
                            },
                        ) {
                            Icon(
                                Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = if (active) GameColors.RewardAmber else GameColors.PrimaryBlue,
                                modifier = Modifier.padding(12.dp).size(30.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (active) sh("PRO AKTİF", "PRO ACTIVE") else sh("ÜCRETSİZ PLAN", "FREE PLAN"),
                                color = if (active) GameColors.RewardAmber else GameColors.TextTertiary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                profile?.displayName ?: sh("Oyuncu", "Player"),
                                color = GameColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                if (active) {
                                    sh(
                                        "Reklamsız deneyim, PRO profil, seri oyunlar, özel oda ve maç sonu analiz hakların açık.",
                                        "Ad-free experience, PRO profile, series games, private rooms and post-match analysis are unlocked.",
                                    )
                                } else {
                                    sh(
                                        "PRO; oyun gücü vermeden sosyal, profil ve analiz özelliklerini genişletir.",
                                        "PRO expands social, profile and analysis features without gameplay power.",
                                    )
                                },
                                color = GameColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                GameSectionHeader(sh("PRO Ayrıcalıkları", "PRO Benefits"))
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProAccessCard(
                        label = sh("Reklamsız", "Ad-free"),
                        enabled = active && e?.rewardedAdBypass == true,
                        accent = GameColors.PrimaryBlue,
                        modifier = Modifier.weight(1f),
                    )
                    ProAccessCard(
                        label = sh("Analiz", "Analysis"),
                        enabled = active && e?.postMatchAnalysis == true,
                        accent = GameColors.PlayGreen,
                        modifier = Modifier.weight(1f),
                    )
                    ProAccessCard(
                        label = sh("Özel Oda", "Private Room"),
                        enabled = active && e?.privateRooms == true,
                        accent = GameColors.RewardAmber,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (active) {
                item {
                    GameSurface(
                        borderColor = GameColors.Lavender.copy(alpha = .34f),
                        elevated = true,
                    ) {
                        Text(
                            sh("PRO ARAÇLARI", "PRO TOOLS"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            sh(
                                "Satın aldığın özellikleri doğrudan kullan.",
                                "Use your purchased benefits directly.",
                            ),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(12.dp))

                        if (e?.postMatchAnalysis == true) {
                            PremiumAnalysisCenterLauncher(Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                        }

                        if (e?.seriesGameAccess == true) {
                            Button(
                                onClick = onSeries,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = GameShapes.Medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GameColors.PrimaryBlue,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    sh("SERİ OYUNLARI", "SERIES GAMES"),
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        if (e?.privateRooms == true) {
                            Button(
                                onClick = onPrivateRoom,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = GameShapes.Medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GameColors.RewardAmber,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(Icons.Rounded.MeetingRoom, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    sh("ÖZEL ODA AÇ / KATIL", "CREATE / JOIN PRIVATE ROOM"),
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }

                        if (entitlementError || e == null) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { scope.launch { reload() } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = GameShapes.Medium,
                                border = BorderStroke(1.dp, GameColors.PrimaryBlue),
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = GameColors.PrimaryBlue,
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    sh("PRO HAKLARINI YENİLE", "REFRESH PRO BENEFITS"),
                                    color = GameColors.PrimaryBlue,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
            }

            item {
                GameSurface(borderColor = GameColors.Border) {
                    Text(
                        sh("ÜYELİĞE DAHİL", "INCLUDED WITH MEMBERSHIP"),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(10.dp))
                    ProLine(sh("Reklamsız menü, profil ve mağaza", "Ad-free menus, profile and shop"))
                    ProLine(sh("PRO rozeti ve profil ayrıcalıkları", "PRO badge and profile benefits"))
                    ProLine(sh("Tamamlanmış maçlar için gelişmiş analiz", "Advanced analysis for completed matches"))
                    ProLine(sh("Kelime Kuşatması seri oyun erişimi", "Word Siege series game access"))
                    ProLine(sh("Son Harf davet kodlu özel oda", "Last Letter invite-code private room"))
                    ProLine(sh("Kaydedilmiş arkadaş listesi", "Saved friend list"))
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.PlayGreen.copy(alpha = .09f),
                        border = BorderStroke(1.dp, GameColors.PlayGreen.copy(alpha = .30f)),
                    ) {
                        Text(
                            sh(
                                "ADİL REKABET: PRO; dereceli maçlarda skor, hedef harf, kelime ipucu, ek süre veya rating avantajı vermez.",
                                "FAIR PLAY: PRO gives no score, target-letter, word-hint, extra-time, or rating advantage in ranked matches.",
                            ),
                            modifier = Modifier.fillMaxWidth().padding(11.dp),
                            color = GameColors.PlayGreen,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item {
                if (active) {
                    OutlinedButton(
                        onClick = {
                            val url = "https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = GameShapes.Medium,
                        border = BorderStroke(1.dp, GameColors.PrimaryBlue),
                    ) {
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = GameColors.PrimaryBlue,
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            sh("GOOGLE PLAY'DE YÖNET", "MANAGE ON GOOGLE PLAY"),
                            color = GameColors.PrimaryBlue,
                            fontWeight = FontWeight.Black,
                        )
                    }
                } else {
                    Button(
                        onClick = { showPurchase = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = GameShapes.Medium,
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.PrimaryBlue),
                    ) {
                        Text(
                            sh("PRO PLANLARINI GÖR", "VIEW PRO PLANS"),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            notice?.let { message ->
                item {
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.PrimaryBlue.copy(alpha = .10f),
                        border = BorderStroke(1.dp, GameColors.PrimaryBlue.copy(alpha = .22f)),
                    ) {
                        Text(
                            message,
                            modifier = Modifier.fillMaxWidth().padding(11.dp),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
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
    label: String,
    enabled: Boolean,
    accent: Color,
    modifier: Modifier,
) {
    GameSurface(
        modifier = modifier,
        borderColor = accent.copy(alpha = .34f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = .12f),
            ) {
                Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                if (enabled) sh("AKTİF", "ACTIVE") else sh("KAPALI", "LOCKED"),
                color = if (enabled) GameColors.PlayGreen else GameColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProLine(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = GameColors.Lavender.copy(alpha = .12f),
        ) {
            Icon(
                Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = GameColors.Lavender,
                modifier = Modifier.padding(6.dp).size(16.dp),
            )
        }
        Text(
            text,
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
