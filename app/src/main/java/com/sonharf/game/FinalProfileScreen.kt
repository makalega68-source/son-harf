package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
fun FinalProfileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var blocked by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var leaders by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var notifications by remember { mutableStateOf(SonHarfPreferences.notificationsEnabled(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteBusy by remember { mutableStateOf(false) }
    var deleteNotice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        val b = backend
        val userId = b?.currentUserId()
        if (b != null && userId != null) {
            profile = runCatching { b.getProfile(userId) }.getOrNull()
            blocked = runCatching { b.getBlockedUsers() }.getOrDefault(emptyList())
            leaders = runCatching { b.getLeaderboard(20) }.getOrDefault(emptyList())
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        SonHarfPreferences.syncSound(context)
        refresh()
    }

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
                borderColor = GameColors.PrimaryBlue.copy(alpha = .32f),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath,
                        name = profile?.displayName ?: sh("Oyuncu", "Player"),
                        size = 104.dp,
                        visible = profile?.avatarVisibility != "hidden",
                        accent = GameColors.PrimaryBlue,
                    )
                    Text(
                        profile?.displayName ?: sh("Oyuncu Profili", "Player Profile"),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        when {
                            profile == null -> sh(
                                "İlk maça girdiğinde oyuncu profilin hazırlanır.",
                                "Your player profile is prepared when you enter your first match.",
                            )
                            profile?.isVip == true -> sh("PRO OYUNCU", "PRO PLAYER")
                            else -> sh("OYUNCU", "PLAYER")
                        },
                        color = if (profile?.isVip == true) {
                            GameColors.RewardAmber
                        } else {
                            GameColors.TextSecondary
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            val wins = profile?.wins ?: 0
            val losses = profile?.losses ?: 0
            val matches = wins + losses
            val rate = if (matches == 0) 0 else wins * 100 / matches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FinalMetric(
                    wins.toString(),
                    sh("Galibiyet", "Wins"),
                    Modifier.weight(1f),
                )
                FinalMetric(
                    losses.toString(),
                    sh("Mağlubiyet", "Losses"),
                    Modifier.weight(1f),
                )
                FinalMetric(
                    "%$rate",
                    sh("Kazanma", "Win rate"),
                    Modifier.weight(1f),
                )
            }
        }

        item {
            GameSectionHeader(sh("Ses ve Bildirim", "Sound & Notifications"))
            Spacer(Modifier.height(6.dp))
            GameSurface(borderColor = GameColors.Border) {
                SettingSwitch(
                    sh("Ses efektleri", "Sound effects"),
                    sh(
                        "Dokunuş ve oyun sesleri",
                        "Tap and gameplay sounds",
                    ),
                    sound,
                ) {
                    sound = it
                    SonHarfPreferences.setSoundEnabled(context, it)
                    if (it) SonHarfSoundFx.tap()
                }
                HorizontalDivider(color = GameColors.Divider)
                SettingSwitch(
                    sh("Titreşim", "Vibration"),
                    sh(
                        "Kısa ve hafif dokunsal geri bildirim",
                        "Short and subtle haptic feedback",
                    ),
                    vibration,
                ) {
                    vibration = it
                    SonHarfPreferences.setVibrationEnabled(context, it)
                    if (it) SonHarfPreferences.hapticTap(context)
                }
                HorizontalDivider(color = GameColors.Divider)
                SettingSwitch(
                    sh("Bildirimler", "Notifications"),
                    sh(
                        "Oyun daveti ve eşleşme bildirimleri",
                        "Game invite and matchmaking notifications",
                    ),
                    notifications,
                ) {
                    notifications = it
                    SonHarfPreferences.setNotificationsEnabled(context, it)
                }
            }
        }

        item {
            GameSurface(
                borderColor = GameColors.TacticalTurquoise.copy(alpha = .28f),
            ) {
                Text(
                    sh("PROFİL FOTOĞRAFI VE GİZLİLİK", "PROFILE PHOTO & PRIVACY"),
                    color = GameColors.TacticalTurquoise,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    sh(
                        "Profil fotoğrafın oyun içinde oyuncu kimliğinin göründüğü alanlarda kullanılır. Engellediğin oyuncularla etkileşim sınırlandırılır.",
                        "Your profile photo appears where your player identity is shown. Interactions with blocked players are restricted.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (blocked.isNotEmpty()) {
            item {
                GameSectionHeader(sh("Engellenenler", "Blocked Players"))
                Spacer(Modifier.height(6.dp))
                GameSurface(borderColor = GameColors.Border) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        blocked.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ProfilePhotoAvatar(
                                        avatarPath = p.avatarPath,
                                        name = p.displayName,
                                        size = 36.dp,
                                        visible = p.avatarVisibility != "hidden",
                                        accent = GameColors.TacticalTurquoise,
                                    )
                                    Text(
                                        p.displayName,
                                        color = GameColors.TextPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val b = backend ?: return@TextButton
                                        scope.launch {
                                            runCatching { b.unblockUser(p.id) }
                                            refresh()
                                        }
                                    },
                                ) {
                                    Text(
                                        sh("Engeli kaldır", "Unblock"),
                                        color = GameColors.PrimaryBlue,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (leaders.isNotEmpty()) {
            item {
                GameSectionHeader(sh("Oyuncu Sıralaması", "Player Ranking"))
                Spacer(Modifier.height(6.dp))
                GameSurface(
                    borderColor = GameColors.RewardAmber.copy(alpha = .28f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        leaders.take(10).forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${index + 1}.",
                                    modifier = Modifier.width(24.dp),
                                    color = GameColors.TextTertiary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                ProfilePhotoAvatar(
                                    avatarPath = row.profile.avatarPath,
                                    name = row.profile.displayName,
                                    size = 36.dp,
                                    visible = row.profile.avatarVisibility != "hidden",
                                    accent = if (row.profile.id == profile?.id) {
                                        GameColors.PrimaryBlue
                                    } else {
                                        GameColors.Lavender
                                    },
                                )
                                Text(
                                    row.profile.displayName,
                                    modifier = Modifier.weight(1f),
                                    color = GameColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (row.profile.id == profile?.id) {
                                        FontWeight.Black
                                    } else {
                                        FontWeight.Medium
                                    },
                                )
                                Text(
                                    sh(
                                        "${row.profile.wins} G • %${row.winRate}",
                                        "${row.profile.wins} W • ${row.winRate}%",
                                    ),
                                    color = GameColors.TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            GameSurface(borderColor = GameColors.Border) {
                Text(
                    sh("GİZLİLİK VE HAKKINDA", "PRIVACY & ABOUT"),
                    color = GameColors.PrimaryBlue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    sh(
                        "Kelime Kuşatması; maç, sohbet, arkadaşlık, raporlama ve profil verilerini çevrimiçi oyun özelliklerini çalıştırmak için kullanır. Kritik skor ve oyun kuralları sunucuda doğrulanır.",
                        "Kelime Kuşatması uses match, chat, friendship, reporting and profile data to operate online game features. Critical scoring and gameplay rules are validated on the server.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    sh(
                        "Sürüm ${BuildConfig.VERSION_NAME} • Android",
                        "Version ${BuildConfig.VERSION_NAME} • Android",
                    ),
                    color = GameColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        item {
            GameSurface(
                borderColor = GameColors.Danger.copy(alpha = .42f),
            ) {
                Text(
                    sh("HESAP YÖNETİMİ", "ACCOUNT MANAGEMENT"),
                    color = GameColors.Danger,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    sh(
                        "Hesabını silersen üyeliğin ve hesabına bağlı oyun verileri kalıcı olarak silinir. Bu işlem geri alınamaz.",
                        "Deleting your account permanently removes your membership and account-linked game data. This action cannot be undone.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = true
                        deleteNotice = null
                    },
                    enabled = !deleteBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = GameShapes.Medium,
                    border = BorderStroke(1.dp, GameColors.Danger.copy(alpha = .55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GameColors.Danger),
                ) {
                    Text(
                        sh("HESABIMI SİL", "DELETE MY ACCOUNT"),
                        fontWeight = FontWeight.Black,
                    )
                }
                if (!deleteNotice.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        deleteNotice!!,
                        color = GameColors.Danger,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!deleteBusy) showDeleteDialog = false
            },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
            title = {
                Text(
                    sh(
                        "Hesabı kalıcı olarak sil?",
                        "Delete account permanently?",
                    ),
                )
            },
            text = {
                Text(
                    sh(
                        "Profilin, ilerlemen ve hesabına bağlı veriler silinecek. Bu işlemi geri alamazsın.",
                        "Your profile, progress and account-linked data will be deleted. This cannot be undone.",
                    ),
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !deleteBusy,
                ) {
                    Text(
                        sh("VAZGEÇ", "CANCEL"),
                        color = GameColors.TextSecondary,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            deleteBusy = true
                            runCatching { AccountDeletion.deleteCurrentAccount() }
                                .onSuccess {
                                    RememberedCredentialVault.clear(context)
                                    SonHarfPreferences.setRememberLogin(context, false)
                                    showDeleteDialog = false
                                    (context as? Activity)?.recreate()
                                }
                                .onFailure {
                                    deleteNotice = sh(
                                        "Hesap silinemedi. Bağlantını kontrol edip tekrar dene.",
                                        "Account could not be deleted. Check your connection and try again.",
                                    )
                                    showDeleteDialog = false
                                }
                            deleteBusy = false
                        }
                    },
                    enabled = !deleteBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.Danger,
                        contentColor = GameColors.TextPrimary,
                    ),
                ) {
                    Text(
                        if (deleteBusy) {
                            sh("SİLİNİYOR…", "DELETING…")
                        } else {
                            sh("KALICI OLARAK SİL", "DELETE PERMANENTLY")
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            },
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FinalMetric(
    value: String,
    label: String,
    modifier: Modifier,
) {
    GameSurface(
        modifier = modifier,
        borderColor = GameColors.Border,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
