package com.sonharf.game

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
    var showForm by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
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

    val authColors = lightColorScheme(
        primary = Color(0xFF1769E0),
        secondary = Color(0xFF6A4FD8),
        background = Color(0xFFF4FAFF),
        surface = Color.White,
        surfaceVariant = Color(0xFFEAF3FF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF142B4F),
        onSurface = Color(0xFF142B4F),
        onSurfaceVariant = Color(0xFF607596),
    )

    MaterialTheme(colorScheme = authColors) {
        Box(
            Modifier
                .fillMaxSize()
                .background(authColors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Image(
                painter = painterResource(R.drawable.son_harf_login_bg),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = .05f),
                            Color.White.copy(alpha = .10f),
                            Color(0xFFEAF3FF).copy(alpha = .22f),
                            Color(0xFFDCEBFF).copy(alpha = .38f),
                        )
                    )
                )
            )
            if (!showForm) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(34.dp))
                    SonHarfBrandLogo(
                        modifier = Modifier.fillMaxWidth(.84f).height(190.dp),
                        size = null,
                    )
                    Spacer(Modifier.height(56.dp))
                    Button(
                        onClick = { register = false; notice = ""; showForm = true },
                        modifier = Modifier.fillMaxWidth(.90f).height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1769E0),
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    ) {
                        Text(sh("Giriş Yap", "Sign In"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { register = true; notice = ""; showForm = true },
                        modifier = Modifier.fillMaxWidth(.90f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF8CB8F3)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = .92f),
                            contentColor = Color(0xFF173B77),
                        ),
                    ) {
                        Text(sh("Kayıt Ol", "Register"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(28.dp))
                }
            } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = { showForm = false; notice = ""; success = false },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text("‹ " + sh("Giriş ekranına dön", "Back to login"), color = Color(0xFF1769E0), fontWeight = FontWeight.Bold)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .94f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFB8D4F7)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = register, onClick = { register = true; notice = "" }, label = { Text("ÜYE OL", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = !register, onClick = { register = false; notice = "" }, label = { Text("GİRİŞ YAP", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                        }
                        if (register) {
                            OutlinedTextField(displayName, { displayName = it.take(24) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Oyuncu adı") })
                            Text(sh("Bu ad oyuncu profilinde kalıcı olarak görünür.", "This name will remain on your player profile."), color = authColors.onSurfaceVariant, fontSize = 12.sp)
                            Text(sh("Profil seçimi", "Profile selection"), color = authColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                    Text(sh("Bu cihazda beni hatırla", "Remember me on this device"), color = authColors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                                Text(sh("Şifremi unuttum", "Forgot password"), color = Color(0xFF1769E0), fontWeight = FontWeight.Bold)
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
                                            notice = "Onay e-postası gönderildi. Bağlantıyı açtıktan sonra Giriş Yap sekmesinden oturum aç. Oyuncu adın ve profil seçimin korunacak."
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
                            colors = ButtonDefaults.buttonColors(containerColor = if (register) Color(0xFF6A4FD8) else Color(0xFF1769E0), contentColor = Color.White),
                        ) {
                            Text(
                                if (busy) "…" else if (register) sh("KAYIT OL", "REGISTER") else sh("GİRİŞ YAP", "SIGN IN"),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                            )
                        }
                        if (notice.isNotBlank()) {
                            Surface(color = if (success) Color(0xFFE8F7EE) else Color(0xFFFFF4E5), shape = RoundedCornerShape(14.dp)) {
                                Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = authColors.onSurface, fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            }
        }
    }
}
