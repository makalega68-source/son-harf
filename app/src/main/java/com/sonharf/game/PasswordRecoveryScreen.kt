package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Surface(Modifier.fillMaxSize(), color = SonHarfTheme.Background) {
        Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                shape = MainUiShape.Hero,
                colors = CardDefaults.cardColors(containerColor = SonHarfTheme.Surface),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SonHarfBrandLogo(modifier = Modifier.fillMaxWidth(.55f), size = null)
                    Text(
                        sh("Yeni şifre belirle", "Set a new password"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = SonHarfTheme.TextPrimary,
                    )
                    Text(
                        if (updated) {
                            sh(
                                "Şifren güncellendi. Güvenlik için recovery oturumu kapatıldı; yeni şifrenle tekrar giriş yap.",
                                "Your password was updated. For security, the recovery session was signed out; sign in again with your new password.",
                            )
                        } else {
                            sh(
                                "E-postandaki sıfırlama bağlantısı doğrulandı. Hesabın için yeni bir şifre seç.",
                                "Your reset link was verified. Choose a new password for your account.",
                            )
                        },
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )

                    if (!updated) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.take(64) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(sh("Yeni şifre", "New password")) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showPassword = !showPassword }) {
                                    Text(if (showPassword) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it.take(64) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(sh("Yeni şifre tekrar", "Confirm new password")) },
                            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showConfirm = !showConfirm }) {
                                    Text(if (showConfirm) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                        )
                        Button(
                            onClick = {
                                if (busy) return@Button
                                when {
                                    password.length < 6 -> {
                                        notice = sh("Şifre en az 6 karakter olmalı.", "Password must be at least 6 characters.")
                                    }
                                    password != confirmPassword -> {
                                        notice = sh("Şifreler aynı değil.", "Passwords do not match.")
                                    }
                                    else -> scope.launch {
                                        busy = true
                                        notice = ""
                                        runCatching {
                                            val newPassword = password
                                            SupabaseProvider.client.auth.updateUser {
                                                this.password = newPassword
                                            }
                                            // The stored credential, if any, contains the old password and must be invalidated.
                                            RememberedCredentialVault.clear(context)
                                            SonHarfPreferences.setRememberLogin(context, false)
                                            SupabaseProvider.client.auth.signOut()
                                        }.onSuccess {
                                            updated = true
                                            password = ""
                                            confirmPassword = ""
                                        }.onFailure { error ->
                                            notice = error.message.orEmpty().take(180).ifBlank {
                                                sh("Şifre güncellenemedi. Tekrar dene.", "Password could not be updated. Try again.")
                                            }
                                        }
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(if (busy) "…" else sh("ŞİFREYİ GÜNCELLE", "UPDATE PASSWORD"), fontWeight = FontWeight.Black)
                        }
                    } else {
                        Button(
                            onClick = onFinished,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(sh("GİRİŞ EKRANINA DÖN", "BACK TO SIGN IN"), fontWeight = FontWeight.Black)
                        }
                    }

                    if (notice.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF5EEE1),
                        ) {
                            Text(
                                notice,
                                modifier = Modifier.padding(12.dp),
                                color = Color(0xFF26382F),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (!updated) {
                        TextButton(onClick = { leaveRecovery() }, enabled = !busy) {
                            Text(sh("İptal et ve giriş ekranına dön", "Cancel and return to sign in"))
                        }
                    }
                }
            }
        }
    }
}
