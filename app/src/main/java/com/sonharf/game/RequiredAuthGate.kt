package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun hasVerifiedMembershipSession(): Boolean =
    runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.email?.isNotBlank() == true }.getOrDefault(false)

@Serializable
private data class AuthIdentityProfile(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val gender: String? = null,
    @SerialName("identity_locked") val identityLocked: Boolean = false,
)

private suspend fun currentIdentityProfile(): AuthIdentityProfile? {
    val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return null
    return SupabaseProvider.client.from("profiles").select { filter { eq("id", uid) } }
        .decodeList<AuthIdentityProfile>().firstOrNull()
}

private suspend fun lockIdentity(name: String, gender: String): AuthIdentityProfile =
    SupabaseProvider.client.postgrest.rpc(
        "complete_profile_identity_v2",
        buildJsonObject { put("p_display_name", name.trim()); put("p_gender", gender) },
    ).decodeSingle()

private suspend fun displayNameAvailable(name: String): Boolean =
    SupabaseProvider.client.postgrest.rpc(
        "display_name_available",
        buildJsonObject { put("p_name", name.trim()) },
    ).decodeSingle()

@Composable
fun RequiredAuthGate(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(SonHarfPreferences.rememberedEmail(context)) }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPassword2 by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(SonHarfPreferences.rememberLogin(context)) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val pulse by rememberInfiniteTransition(label = "authLogo").animateFloat(
        initialValue = .96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "authLogoPulse",
    )

    fun friendly(raw: String): String = when {
        "Email not confirmed" in raw || "email_not_confirmed" in raw -> "E-posta adresini onaylamadan giriş yapamazsın. Gelen kutunu kontrol et."
        "Invalid login credentials" in raw -> "E-posta veya şifre hatalı."
        "already registered" in raw.lowercase() || "user_already_exists" in raw.lowercase() -> "Bu e-posta zaten kayıtlı. Giriş Yap sekmesini kullan."
        "Unable to validate email address" in raw || "validation_failed" in raw.lowercase() -> "E-posta adresi geçerli görünmüyor. Adresi kontrol edip tekrar dene."
        "display_name_taken" in raw -> "Bu oyuncu adı kullanımda. Başka bir ad seç."
        "invalid_display_name" in raw -> "Oyuncu adı 2-24 karakter olmalı."
        "invalid_gender" in raw -> "Cinsiyet seçimi gerekli."
        "password" in raw.lowercase() && "6" in raw -> "Şifre en az 6 karakter olmalı."
        else -> raw.take(170).ifBlank { "İşlem tamamlanamadı. Tekrar dene." }
    }

    val authColors = lightColorScheme(
        primary = SonHarfPurple,
        secondary = SonHarfCyan,
        background = Color.White,
        surface = Color.White,
        surfaceVariant = Color(0xFFF6F7FB),
        onBackground = Color(0xFF111522),
        onSurface = Color(0xFF111522),
        onSurfaceVariant = Color(0xFF596176),
    )

    MaterialTheme(colorScheme = authColors) {
        Box(
            Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding().imePadding(),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(88.dp).scale(pulse)
                        .background(Brush.radialGradient(listOf(SonHarfCyan, SonHarfPurple)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("S↻H", color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp) }
                Text("SON HARF", color = Color(0xFF111522), fontWeight = FontWeight.Black, fontSize = 36.sp, letterSpacing = 2.sp)
                Text("Kelime düellosuna girmek için doğrulanmış üyelik gerekiyor.", color = Color(0xFF697086), textAlign = TextAlign.Center, fontSize = 15.sp)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = register, onClick = { register = true; notice = "" }, label = { Text("ÜYE OL", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = !register, onClick = { register = false; notice = "" }, label = { Text("GİRİŞ YAP", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                        }
                        if (register) {
                            OutlinedTextField(displayName, { displayName = it.take(24) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Oyuncu adı") })
                            Text("Her oyuncu adı benzersizdir. Büyük/küçük harf farkı aynı ad sayılır.", color = Color(0xFF697086), fontSize = 13.sp)
                            Text("Cinsiyet", color = Color(0xFF111522), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("erkek" to "Erkek", "kadın" to "Kadın", "diğer" to "Diğer").forEach { (value, label) ->
                                    FilterChip(selected = gender == value, onClick = { gender = value }, label = { Text(label, fontSize = 14.sp) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        OutlinedTextField(email, { email = it.trim().take(120) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("E-posta") })
                        OutlinedTextField(
                            password,
                            { password = it.take(64) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Şifre") },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "GİZLE" else "GÖSTER", fontSize = 11.sp, fontWeight = FontWeight.Bold) } },
                        )
                        if (register) {
                            OutlinedTextField(
                                password2,
                                { password2 = it.take(64) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Şifre tekrar") },
                                visualTransformation = if (showPassword2) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = { TextButton(onClick = { showPassword2 = !showPassword2 }) { Text(if (showPassword2) "GİZLE" else "GÖSTER", fontSize = 11.sp, fontWeight = FontWeight.Bold) } },
                            )
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                                Text("Beni hatırla", color = Color(0xFF111522), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            }
                            TextButton(
                                onClick = {
                                    if (busy) return@TextButton
                                    if (!email.contains("@")) { notice = "Önce geçerli e-posta adresini gir."; success = false; return@TextButton }
                                    scope.launch {
                                        busy = true; notice = ""; success = false
                                        runCatching { SupabaseProvider.client.auth.resetPasswordForEmail(email.trim()) }
                                            .onSuccess { success = true; notice = "Şifre sıfırlama bağlantısı e-posta adresine gönderildi. Gelen kutunu ve spam klasörünü kontrol et." }
                                            .onFailure { notice = friendly(it.message.orEmpty()) }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.align(Alignment.End),
                            ) { Text("Şifremi unuttum", color = SonHarfPurple, fontWeight = FontWeight.Bold) }
                        }

                        Button(
                            onClick = {
                                if (busy) return@Button
                                if (!email.contains("@") || password.length < 6) { notice = "Geçerli e-posta ve en az 6 karakterli şifre gir."; return@Button }
                                if (register && displayName.trim().length < 2) { notice = "Oyuncu adı en az 2 karakter olmalı."; return@Button }
                                if (register && gender.isBlank()) { notice = "Kadın, Erkek veya Diğer seçeneklerinden birini seç."; return@Button }
                                if (register && password != password2) { notice = "Şifreler aynı değil."; return@Button }
                                scope.launch {
                                    busy = true; notice = ""; success = false
                                    if (register) {
                                        runCatching {
                                            check(displayNameAvailable(displayName)) { "display_name_taken" }
                                            SupabaseProvider.client.auth.signOut()
                                            SupabaseProvider.client.auth.signUpWith(Email) {
                                                this.email = email.trim(); this.password = password
                                                data = buildJsonObject { put("display_name", displayName.trim()); put("gender", gender) }
                                            }
                                        }.onSuccess {
                                            SonHarfPreferences.rememberPendingRegistration(context, email, displayName, gender)
                                            success = true
                                            notice = "Onay e-postası gönderildi. Bağlantıya dokun; sonra Giriş Yap sekmesinden oturum aç."
                                            register = false
                                        }.onFailure { notice = friendly(it.message.orEmpty()) }
                                    } else {
                                        runCatching {
                                            SupabaseProvider.client.auth.signOut()
                                            SupabaseProvider.client.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
                                            check(hasVerifiedMembershipSession()) { "Email not confirmed" }
                                            val profile = currentIdentityProfile()
                                            if (profile != null && !profile.identityLocked) {
                                                val pendingName = SonHarfPreferences.pendingRegistrationName(context, email)
                                                val pendingGender = SonHarfPreferences.pendingRegistrationGender(context, email)
                                                if (pendingName != null && pendingGender != null) lockIdentity(pendingName, pendingGender)
                                            }
                                            SonHarfPreferences.clearPendingRegistration(context, email)
                                            SonHarfPreferences.setRememberLogin(context, rememberMe, email)
                                        }.onSuccess { onAuthenticated() }.onFailure { notice = friendly(it.message.orEmpty()) }
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (register) SonHarfPurple else SonHarfCyan, contentColor = Color.White),
                        ) { Text(if (busy) "…" else if (register) "ÜYELİK OLUŞTUR" else "OYUNA GİR", fontWeight = FontWeight.Black, fontSize = 17.sp) }

                        if (notice.isNotBlank()) {
                            Surface(color = if (success) Color(0xFFE8F8EF) else Color(0xFFFFF4D7), shape = RoundedCornerShape(14.dp)) {
                                Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = Color(0xFF232735), fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Text("E-posta doğrulaması tamamlanmadan oyun ekranları açılmaz.", color = Color(0xFF697086), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
