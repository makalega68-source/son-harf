package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
        initialValue = .96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse), label = "authLogoPulse",
    )

    fun friendly(raw: String): String = when {
        "Email not confirmed" in raw || "email_not_confirmed" in raw -> "E-posta adresini onaylamadan giriş yapamazsın. Gelen kutunu kontrol et."
        "Invalid login credentials" in raw -> "E-posta veya şifre hatalı."
        "already registered" in raw.lowercase() || "user_already_exists" in raw.lowercase() -> "Bu e-posta zaten kayıtlı. Giriş Yap sekmesini kullan."
        "Unable to validate email address" in raw || "validation_failed" in raw.lowercase() -> "E-posta adresi geçerli görünmüyor. Adresi kontrol edip tekrar dene."
        "invalid_display_name" in raw -> "Oyuncu adı 2-24 karakter olmalı."
        "invalid_gender" in raw -> "Cinsiyet seçimi gerekli."
        "password" in raw.lowercase() && "6" in raw -> "Şifre en az 6 karakter olmalı."
        else -> raw.take(170).ifBlank { "İşlem tamamlanamadı. Tekrar dene." }
    }

    val authColors = darkColorScheme(
        primary = LetharaPalette.Gold,
        secondary = LetharaPalette.Cyan,
        background = LetharaPalette.Night,
        surface = LetharaPalette.PanelStrong,
        surfaceVariant = LetharaPalette.Panel,
        onPrimary = Color(0xFF211830),
        onBackground = LetharaPalette.Text,
        onSurface = LetharaPalette.Text,
        onSurfaceVariant = LetharaPalette.Muted,
    )

    MaterialTheme(colorScheme = authColors) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LetharaPalette.Night)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Image(
                painter = painterResource(R.drawable.lethara_login_bg),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x22030A18),
                            Color(0x33030A18),
                            Color(0x99100924),
                            Color(0xEE071229),
                        )
                    )
                )
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(225.dp))
                Surface(
                    color = LetharaPalette.PanelStrong.copy(alpha = .78f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha = .38f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("HATIRLATICI, MÜHÜRLER SENİ BEKLİYOR", "REMEMBRANCER, THE SEALS AWAIT"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(sh("Misafir girişi yoktur; ilerlemen, maskot hafızan ve Lethara arşivin üyeliğine bağlıdır.", "There is no guest mode; your progress, mascot memories and Lethara archive belong to your account."), color = LetharaPalette.Muted, textAlign = TextAlign.Center, fontSize = 10.sp)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = LetharaPalette.PanelStrong.copy(alpha = .96f)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha = .38f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = register, onClick = { register = true; notice = "" }, label = { Text("ÜYE OL", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = !register, onClick = { register = false; notice = "" }, label = { Text("GİRİŞ YAP", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                        }
                        if (register) {
                            OutlinedTextField(displayName, { displayName = it.take(24) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Oyuncu adı") })
                            Text(sh("Bu ad, Hatırlatıcı kimliğin olarak kalıcı olur.", "This becomes your permanent Remembrancer identity."), color = LetharaPalette.Muted, fontSize = 12.sp)
                            Text(sh("Profil seçimi", "Profile selection"), color = LetharaPalette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            trailingIcon = {
                                TextButton(onClick = { showPassword = !showPassword }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                    Text(if (showPassword) "GİZLE" else "GÖSTER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            },
                        )
                        if (register) {
                            OutlinedTextField(
                                password2,
                                { password2 = it.take(64) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Şifre tekrar") },
                                visualTransformation = if (showPassword2) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showPassword2 = !showPassword2 }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                        Text(if (showPassword2) "GİZLE" else "GÖSTER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                },
                            )
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                                    Text(sh("Bu cihazda beni hatırla", "Remember me on this device"), color = LetharaPalette.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                            TextButton(
                                onClick = {
                                    if (busy) return@TextButton
                                    if (!email.contains("@")) {
                                        notice = "Önce geçerli e-posta adresini gir."
                                        success = false
                                        return@TextButton
                                    }
                                    scope.launch {
                                        busy = true
                                        notice = ""
                                        success = false
                                        runCatching {
                                            SupabaseProvider.client.auth.resetPasswordForEmail(email.trim())
                                        }.onSuccess {
                                            success = true
                                            notice = "Şifre sıfırlama bağlantısı e-posta adresine gönderildi. Gelen kutunu ve spam klasörünü kontrol et."
                                        }.onFailure {
                                            notice = friendly(it.message.orEmpty())
                                        }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(sh("Şifremi unuttum", "Forgot password"), color = LetharaPalette.Cyan, fontWeight = FontWeight.Bold)
                            }
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
                                            SupabaseProvider.client.auth.signOut()
                                            SupabaseProvider.client.auth.signUpWith(Email) {
                                                this.email = email.trim(); this.password = password
                                                data = buildJsonObject { put("display_name", displayName.trim()); put("gender", gender) }
                                            }
                                        }.onSuccess {
                                            SonHarfPreferences.rememberPendingRegistration(context, email, displayName, gender)
                                            success = true
                                            notice = "Onay e-postası gönderildi. Bağlantıya dokun; sonra Giriş Yap sekmesinden oturum aç. Oyuncu adın ve cinsiyet seçimin korunacak."
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
                            colors = ButtonDefaults.buttonColors(containerColor = if (register) LetharaPalette.Violet else LetharaPalette.Gold, contentColor = if (register) Color.White else Color(0xFF211830)),
                        ) {
                            Text(
                                if (busy) "…" else if (register) sh("KAYIT OL", "REGISTER") else sh("OYNA", "PLAY"),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                            )
                        }
                        if (notice.isNotBlank()) {
                            Surface(color = if (success) LetharaPalette.Green.copy(alpha=.18f) else LetharaPalette.Gold.copy(alpha=.12f), shape = RoundedCornerShape(14.dp)) {
                                Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = LetharaPalette.Text, fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
