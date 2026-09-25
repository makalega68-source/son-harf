package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

/**
 * Password-reset links create a short-lived authenticated recovery session. That session must
 * never be treated as a normal remembered login: the user first chooses a new password, then the
 * recovery session is signed out and the regular login screen is shown again.
 */
@Composable
internal fun PasswordRecoveryScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var updated by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    fun leaveRecovery() {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { SupabaseProvider.client.auth.signOut() }
            busy = false
            onFinished()
        }
    }

    BackHandler(enabled = true) { leaveRecovery() }

    GameTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GameColors.AppBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                GameTopBar(
                    title = sh("Şifre Kurtarma", "Password Recovery"),
                    subtitle = sh(
                        "Hesabın için güvenli bir yeni şifre belirle",
                        "Set a secure new password for your account",
                    ),
                    onBack = { leaveRecovery() },
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = GameSpacing.ScreenHorizontal,
                            vertical = 16.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = if (updated) {
                            GameColors.PlayGreen.copy(alpha = .14f)
                        } else {
                            GameColors.PrimaryBlue.copy(alpha = .14f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (updated) {
                                GameColors.PlayGreen.copy(alpha = .42f)
                            } else {
                                GameColors.PrimaryBlue.copy(alpha = .42f)
                            },
                        ),
                    ) {
                        Icon(
                            imageVector = if (updated) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (updated) GameColors.PlayGreen else GameColors.PrimaryBlue,
                            modifier = Modifier.padding(16.dp).size(34.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (updated) {
                            sh("Şifre güncellendi", "Password updated")
                        } else {
                            sh("Yeni şifre belirle", "Set a new password")
                        },
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (updated) {
                            sh(
                                "Güvenlik için kurtarma oturumu kapatıldı. Yeni şifrenle tekrar giriş yap.",
                                "For security, the recovery session was signed out. Sign in again with your new password.",
                            )
                        } else {
                            sh(
                                "E-postandaki sıfırlama bağlantısı doğrulandı. En az 6 karakterlik yeni bir şifre seç.",
                                "Your reset link was verified. Choose a new password with at least 6 characters.",
                            )
                        },
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(20.dp))

                    GameSurface(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = true,
                        borderColor = if (updated) {
                            GameColors.PlayGreen.copy(alpha = .35f)
                        } else {
                            GameColors.Border
                        },
                    ) {
                        if (!updated) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it.take(64) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(sh("Yeni şifre", "New password")) },
                                visualTransformation = if (showPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    TextButton(onClick = { showPassword = !showPassword }) {
                                        Text(
                                            if (showPassword) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                },
                                colors = professionalRecoveryFieldColors(),
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it.take(64) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(sh("Yeni şifre tekrar", "Confirm new password")) },
                                visualTransformation = if (showConfirm) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    TextButton(onClick = { showConfirm = !showConfirm }) {
                                        Text(
                                            if (showConfirm) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                },
                                colors = professionalRecoveryFieldColors(),
                            )

                            Spacer(Modifier.height(16.dp))

                            GamePrimaryButton(
                                text = if (busy) "…" else sh("ŞİFREYİ GÜNCELLE", "UPDATE PASSWORD"),
                                onClick = {
                                    if (busy) return@GamePrimaryButton
                                    when {
                                        password.length < 6 -> {
                                            notice = sh(
                                                "Şifre en az 6 karakter olmalı.",
                                                "Password must be at least 6 characters.",
                                            )
                                        }
                                        password != confirmPassword -> {
                                            notice = sh(
                                                "Şifreler aynı değil.",
                                                "Passwords do not match.",
                                            )
                                        }
                                        else -> scope.launch {
                                            busy = true
                                            notice = ""
                                            runCatching {
                                                val newPassword = password
                                                SupabaseProvider.client.auth.updateUser {
                                                    this.password = newPassword
                                                }
                                                // Stored credentials contain the old password and must be invalidated.
                                                RememberedCredentialVault.clear(context)
                                                SonHarfPreferences.setRememberLogin(context, false)
                                                SupabaseProvider.client.auth.signOut()
                                            }.onSuccess {
                                                updated = true
                                                password = ""
                                                confirmPassword = ""
                                            }.onFailure { error ->
                                                notice = error.message.orEmpty().take(180).ifBlank {
                                                    sh(
                                                        "Şifre güncellenemedi. Tekrar dene.",
                                                        "Password could not be updated. Try again.",
                                                    )
                                                }
                                            }
                                            busy = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                                icon = Icons.Rounded.Lock,
                            )
                        } else {
                            Text(
                                text = sh(
                                    "Yeni şifren hazır. Oturum güvenli şekilde sonlandırıldı.",
                                    "Your new password is ready. The recovery session was closed securely.",
                                ),
                                color = GameColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(14.dp))
                            GamePrimaryButton(
                                text = sh("GİRİŞ EKRANINA DÖN", "BACK TO SIGN IN"),
                                onClick = onFinished,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.CheckCircle,
                            )
                        }
                    }

                    if (notice.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = GameShapes.Medium,
                            color = GameColors.Danger.copy(alpha = .10f),
                            border = BorderStroke(1.dp, GameColors.Danger.copy(alpha = .32f)),
                        ) {
                            Text(
                                text = notice,
                                modifier = Modifier.padding(12.dp),
                                color = GameColors.TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (!updated) {
                        Spacer(Modifier.height(8.dp))
                        GameTertiaryButton(
                            text = sh("İPTAL ET VE GİRİŞE DÖN", "CANCEL AND RETURN TO SIGN IN"),
                            onClick = { leaveRecovery() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun professionalRecoveryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GameColors.TextPrimary,
    unfocusedTextColor = GameColors.TextPrimary,
    focusedContainerColor = GameColors.ElevatedBackground,
    unfocusedContainerColor = GameColors.ElevatedBackground,
    focusedBorderColor = GameColors.PrimaryBlue,
    unfocusedBorderColor = GameColors.Border,
    focusedLabelColor = GameColors.PrimaryBlue,
    unfocusedLabelColor = GameColors.TextSecondary,
    cursorColor = GameColors.PrimaryBlue,
)
