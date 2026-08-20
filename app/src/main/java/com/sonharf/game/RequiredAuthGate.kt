package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

fun hasVerifiedMembershipSession(): Boolean =
    runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.email?.isNotBlank() == true }.getOrDefault(false)

@Composable
fun RequiredAuthGate(onAuthenticated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    val pulse by rememberInfiniteTransition(label = "authLogo").animateFloat(
        initialValue = .94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "authLogoPulse",
    )

    fun friendly(raw: String): String = when {
        "Email not confirmed" in raw || "email_not_confirmed" in raw -> "E-posta adresini onaylamadan giriş yapamazsın. Gelen kutunu kontrol et."
        "Invalid login credentials" in raw -> "E-posta veya şifre hatalı."
        "already registered" in raw.lowercase() || "user_already_exists" in raw.lowercase() -> "Bu e-posta zaten kayıtlı. Giriş Yap sekmesini kullan."
        "password" in raw.lowercase() && "6" in raw -> "Şifre en az 6 karakter olmalı."
        else -> raw.take(170).ifBlank { "İşlem tamamlanamadı. Tekrar dene." }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFDDF3FF), Color(0xFFEAF8FF), Color(0xFFD8EEFF))),
        ).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(92.dp).scale(pulse).background(
                    Brush.radialGradient(listOf(SonHarfCyan, SonHarfPurple)), CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text("S↻H", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
            }
            Text("SON HARF", fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 2.sp)
            Text("Kelime düellosuna girmek için doğrulanmış üyelik gerekiyor.", color = SonHarfMuted, textAlign = TextAlign.Center, fontSize = 12.sp)

            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .96f)),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .28f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = register, onClick = { register = true; notice = "" }, label = { Text("ÜYE OL") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = !register, onClick = { register = false; notice = "" }, label = { Text("GİRİŞ YAP") }, modifier = Modifier.weight(1f))
                    }

                    if (register) {
                        OutlinedTextField(displayName, { displayName = it.take(24) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Oyuncu adı") })
                    }
                    OutlinedTextField(email, { email = it.trim().take(120) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("E-posta") })
                    OutlinedTextField(password, { password = it.take(64) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Şifre") }, visualTransformation = PasswordVisualTransformation())
                    if (register) {
                        OutlinedTextField(password2, { password2 = it.take(64) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Şifre tekrar") }, visualTransformation = PasswordVisualTransformation())
                    }

                    Button(
                        onClick = {
                            if (busy) return@Button
                            if (!email.contains("@") || password.length < 6) { notice = "Geçerli e-posta ve en az 6 karakterli şifre gir."; return@Button }
                            if (register && password != password2) { notice = "Şifreler aynı değil."; return@Button }
                            scope.launch {
                                busy = true; notice = ""; success = false
                                if (register) {
                                    runCatching {
                                        SupabaseProvider.client.auth.signOut()
                                        SupabaseProvider.client.auth.signUpWith(Email) {
                                            this.email = email.trim()
                                            this.password = password
                                        }
                                    }.onSuccess {
                                        success = true
                                        notice = "Onay e-postası gönderildi. E-postadaki bağlantıya dokun; ardından Giriş Yap bölümünden giriş yap."
                                        register = false
                                    }.onFailure { notice = friendly(it.message.orEmpty()) }
                                } else {
                                    runCatching {
                                        SupabaseProvider.client.auth.signOut()
                                        SupabaseProvider.client.auth.signInWith(Email) {
                                            this.email = email.trim()
                                            this.password = password
                                        }
                                        check(hasVerifiedMembershipSession()) { "Email not confirmed" }
                                        val backend = OnlineGameBackend()
                                        val id = requireNotNull(backend.currentUserId())
                                        if (runCatching { backend.getProfile(id) }.getOrNull() == null) {
                                            backend.ensurePlayer(displayName.ifBlank { email.substringBefore('@').take(20).ifBlank { "Oyuncu" } })
                                        }
                                    }.onSuccess { onAuthenticated() }
                                        .onFailure { notice = friendly(it.message.orEmpty()) }
                                }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (register) SonHarfPurple else SonHarfCyan, contentColor = Color.White),
                    ) {
                        Text(if (busy) "…" else if (register) "ÜYELİK OLUŞTUR" else "OYUNA GİR", fontWeight = FontWeight.Black)
                    }

                    if (notice.isNotBlank()) {
                        Surface(color = if (success) SonHarfGreen.copy(alpha = .12f) else SonHarfGold.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                            Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfText, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Text("E-posta doğrulaması tamamlanmadan oyun ekranları açılmaz.", color = SonHarfMuted, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
