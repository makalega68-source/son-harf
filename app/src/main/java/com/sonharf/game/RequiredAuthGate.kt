package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.github.jan.supabase.auth.OtpType
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
    val rememberedCredential = remember {
        if (SonHarfPreferences.rememberLogin(context)) RememberedCredentialVault.load(context) else null
    }
    var register by remember { mutableStateOf(true) }
    var registerStep by remember { mutableIntStateOf(1) }
    var displayName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var email by remember {
        mutableStateOf(rememberedCredential?.email ?: SonHarfPreferences.rememberedEmail(context))
    }
    var password by remember { mutableStateOf(rememberedCredential?.password.orEmpty()) }
    var password2 by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPassword2 by remember { mutableStateOf(false) }
    var rememberMe by remember {
        mutableStateOf(SonHarfPreferences.rememberLogin(context) || rememberedCredential != null)
    }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var pendingVerificationEmail by remember { mutableStateOf<String?>(null) }
    var otpCode by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    fun friendly(raw: String): String = when {
        "Email not confirmed" in raw || "email_not_confirmed" in raw -> "E-posta adresini onaylamadan giriş yapamazsın. Gelen kutunu kontrol et."
        "Invalid login credentials" in raw -> "E-posta veya şifre hatalı."
        "existing_confirmed_account" in raw -> "Bu e-posta zaten kayıtlı ve doğrulanmış. Giriş Yap bölümünü kullan; şifreni unuttuysan Şifremi unuttum'a dokun."
        "already registered" in raw.lowercase() || "user_already_exists" in raw.lowercase() -> "Bu e-posta zaten kayıtlı. Giriş Yap bölümünü kullan."
        "email rate limit" in raw.lowercase() || "over_email_send_rate_limit" in raw.lowercase() -> "Çok sık e-posta istendi. Birkaç dakika bekleyip tekrar dene."
        "Unable to validate email address" in raw || "validation_failed" in raw.lowercase() -> "E-posta adresi geçerli görünmüyor. Adresi kontrol edip tekrar dene."
        "invalid_display_name" in raw -> "Oyuncu adı 2-24 karakter olmalı."
        "invalid_gender" in raw -> "Cinsiyet seçimi gerekli."
        "password" in raw.lowercase() && "6" in raw -> "Şifre en az 6 karakter olmalı."
        else -> raw.take(170).ifBlank { "İşlem tamamlanamadı. Tekrar dene." }
    }


    fun verifyPendingEmail() {
        val targetEmail = pendingVerificationEmail ?: return
        if (busy) return
        if (otpCode.length != 6) {
            notice = "E-postana gelen 6 haneli kodu gir."
            success = false
            return
        }
        scope.launch {
            busy = true
            notice = ""
            success = false
            runCatching {
                SupabaseProvider.client.auth.verifyEmailOtp(
                    type = OtpType.Email.EMAIL,
                    email = targetEmail,
                    token = otpCode,
                )
                check(hasVerifiedMembershipSession()) { "Email not confirmed" }
                val profile = currentIdentityProfile()
                if (profile != null && !profile.identityLocked) {
                    val pendingName = SonHarfPreferences.pendingRegistrationName(context, targetEmail)
                    val pendingGender = SonHarfPreferences.pendingRegistrationGender(context, targetEmail)
                    if (pendingName != null && pendingGender != null) lockIdentity(pendingName, pendingGender)
                }
                SonHarfPreferences.clearPendingRegistration(context, targetEmail)
                SonHarfPreferences.setRememberLogin(context, true, targetEmail)
            }.onSuccess {
                success = true
                notice = "E-posta doğrulandı. Hoş geldin!"
                pendingVerificationEmail = null
                otpCode = ""
                onAuthenticated()
            }.onFailure {
                notice = friendly(it.message.orEmpty())
            }
            busy = false
        }
    }

    fun resendPendingCode() {
        val targetEmail = pendingVerificationEmail ?: return
        if (busy) return
        scope.launch {
            busy = true
            notice = ""
            success = false
            runCatching {
                SupabaseProvider.client.auth.resendEmail(
                    type = OtpType.Email.SIGNUP,
                    email = targetEmail,
                    redirectUrl = "sonharf://auth",
                )
            }.onSuccess {
                success = true
                notice = "Yeni doğrulama e-postası gönderildi. Gelen kutunu ve spam klasörünü kontrol et."
            }.onFailure {
                notice = friendly(it.message.orEmpty())
            }
            busy = false
        }
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
            // Generated directly in Compose so no stale bitmap can survive an app update.
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFCFDFF),
                            Color(0xFFF3F7FF),
                            Color(0xFFF8F5FF),
                            Color(0xFFEEF5FF),
                        )
                    )
                )
            )
            Box(
                Modifier
                    .size(360.dp)
                    .offset(x = 170.dp, y = (-105).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF1769E0).copy(alpha = .17f),
                                Color.Transparent,
                            )
                        ),
                        CircleShape,
                    )
            )
            Box(
                Modifier
                    .size(310.dp)
                    .offset(x = (-155).dp, y = 355.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF6A4FD8).copy(alpha = .13f),
                                Color.Transparent,
                            )
                        ),
                        CircleShape,
                    )
            )
            Box(
                Modifier
                    .fillMaxWidth(.78f)
                    .height(120.dp)
                    .align(Alignment.Center)
                    .offset(y = (-75).dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1769E0).copy(alpha = .06f),
                                Color(0xFF6A4FD8).copy(alpha = .05f),
                                Color.Transparent,
                            )
                        ),
                        RoundedCornerShape(42.dp),
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
                if (pendingVerificationEmail != null) {
                    EmailVerificationCard(
                        email = pendingVerificationEmail.orEmpty(),
                        otpCode = otpCode,
                        onOtpChange = { otpCode = it.filter(Char::isDigit).take(6) },
                        busy = busy,
                        notice = notice,
                        success = success,
                        onVerify = ::verifyPendingEmail,
                        onResend = ::resendPendingCode,
                        onChangeEmail = {
                            pendingVerificationEmail = null
                            otpCode = ""
                            notice = ""
                            success = false
                            register = true
                        },
                    )
                } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .94f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFB8D4F7)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = register, onClick = { register = true; registerStep = 1; notice = "" }, label = { Text("ÜYE OL", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = !register, onClick = { register = false; registerStep = 1; notice = "" }, label = { Text("GİRİŞ YAP", fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                        }
                        if (register) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(sh("KAYIT", "REGISTER"), color = authColors.primary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.weight(1f))
                                Text("$registerStep / 2", color = authColors.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { registerStep / 2f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = authColors.secondary,
                                trackColor = authColors.surfaceVariant,
                            )
                        }
                        if (register && registerStep == 1) {
                            OutlinedTextField(displayName, { displayName = it.take(24) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Oyuncu adı") })
                            Text(sh("Bu ad oyuncu profilinde kalıcı olarak görünür.", "This name will remain on your player profile."), color = authColors.onSurfaceVariant, fontSize = 12.sp)
                            Text(sh("Profil seçimi", "Profile selection"), color = authColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("erkek" to "Erkek", "kadın" to "Kadın", "diğer" to "Diğer").forEach { (value, label) ->
                                    FilterChip(selected = gender == value, onClick = { gender = value }, label = { Text(label, fontSize = 14.sp) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (!register || registerStep == 2) {
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
                                    Text(if (showPassword) "GİZLE" else "GÖSTER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                        Text(if (showPassword2) "GİZLE" else "GÖSTER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                },
                            )
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = rememberMe,
                                        onCheckedChange = {
                                            rememberMe = it
                                            if (!it) {
                                                RememberedCredentialVault.clear(context)
                                                SonHarfPreferences.setRememberLogin(context, false)
                                            }
                                        },
                                    )
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
                                            SupabaseProvider.client.auth.resetPasswordForEmail(
                                                email = email.trim(),
                                                redirectUrl = "sonharf://auth",
                                            )
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
                        }
                        Button(
                            onClick = {
                                if (busy) return@Button
                                if (register && registerStep == 1) {
                                    if (displayName.trim().length < 2) { notice = "Oyuncu adı en az 2 karakter olmalı."; return@Button }
                                    if (gender.isBlank()) { notice = "Kadın, Erkek veya Diğer seçeneklerinden birini seç."; return@Button }
                                    notice = ""
                                    registerStep = 2
                                    return@Button
                                }
                                if (!email.contains("@") || password.length < 6) { notice = "Geçerli e-posta ve en az 6 karakterli şifre gir."; return@Button }
                                if (register && displayName.trim().length < 2) { notice = "Oyuncu adı en az 2 karakter olmalı."; return@Button }
                                if (register && gender.isBlank()) { notice = "Kadın, Erkek veya Diğer seçeneklerinden birini seç."; return@Button }
                                if (register && password != password2) { notice = "Şifreler aynı değil."; return@Button }
                                scope.launch {
                                    busy = true; notice = ""; success = false
                                    if (register) {
                                        val targetEmail = email.trim()

                                        // First try the credentials. This prevents Supabase's
                                        // repeated-signup privacy response from being mistaken
                                        // for a newly sent verification email.
                                        val existingLogin = runCatching {
                                            SupabaseProvider.client.auth.signOut()
                                            SupabaseProvider.client.auth.signInWith(Email) {
                                                this.email = targetEmail
                                                this.password = password
                                            }
                                            check(hasVerifiedMembershipSession()) { "Email not confirmed" }
                                        }

                                        if (existingLogin.isSuccess) {
                                            val profile = currentIdentityProfile()
                                            if (profile != null && !profile.identityLocked) {
                                                val pendingName = SonHarfPreferences.pendingRegistrationName(context, targetEmail)
                                                val pendingGender = SonHarfPreferences.pendingRegistrationGender(context, targetEmail)
                                                if (pendingName != null && pendingGender != null) lockIdentity(pendingName, pendingGender)
                                            }
                                            SonHarfPreferences.clearPendingRegistration(context, targetEmail)
                                            SonHarfPreferences.setRememberLogin(context, true, targetEmail)
                                            success = true
                                            notice = "Hesabın zaten vardı; giriş yapıldı."
                                            busy = false
                                            onAuthenticated()
                                            return@launch
                                        }

                                        val existingError = existingLogin.exceptionOrNull()?.message.orEmpty()
                                        if ("Email not confirmed" in existingError || "email_not_confirmed" in existingError) {
                                            runCatching {
                                                SupabaseProvider.client.auth.signOut()
                                                SupabaseProvider.client.auth.resendEmail(
                                                    type = OtpType.Email.SIGNUP,
                                                    email = targetEmail,
                                                    redirectUrl = "sonharf://auth",
                                                )
                                            }.onSuccess {
                                                SonHarfPreferences.rememberPendingRegistration(context, targetEmail, displayName, gender)
                                                success = true
                                                pendingVerificationEmail = targetEmail
                                                otpCode = ""
                                                notice = "Doğrulama e-postası yeniden gönderildi. Gelen kutusu ve spam klasörünü kontrol et."
                                            }.onFailure {
                                                notice = friendly(it.message.orEmpty())
                                            }
                                        } else {
                                            runCatching {
                                                SupabaseProvider.client.auth.signOut()
                                                SupabaseProvider.client.auth.signUpWith(Email, redirectUrl = "sonharf://auth") {
                                                    this.email = targetEmail
                                                    this.password = password
                                                    data = buildJsonObject {
                                                        put("display_name", displayName.trim())
                                                        put("gender", gender)
                                                    }
                                                }
                                            }.onSuccess { newUser ->
                                                if (newUser == null || newUser.identities.isNullOrEmpty()) {
                                                    success = false
                                                    notice = friendly("existing_confirmed_account")
                                                    register = false
                                                } else {
                                                    SonHarfPreferences.rememberPendingRegistration(context, targetEmail, displayName, gender)
                                                    success = true
                                                    pendingVerificationEmail = targetEmail
                                                    otpCode = ""
                                                    notice = "Doğrulama e-postası gönderildi. Maildeki doğrulama bağlantısına dokun veya 6 haneli kodu buraya gir."
                                                }
                                            }.onFailure {
                                                notice = friendly(it.message.orEmpty())
                                            }
                                        }
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
                                            if (rememberMe) {
                                                RememberedCredentialVault.save(context, email, password)
                                            } else {
                                                RememberedCredentialVault.clear(context)
                                            }
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
                                if (busy) "…" else if (register && registerStep == 1) sh("DEVAM ET", "CONTINUE") else if (register) sh("KAYIT OL", "REGISTER") else sh("GİRİŞ YAP", "SIGN IN"),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                            )
                        }
                        if (register && registerStep == 2) {
                            TextButton(onClick = { registerStep = 1; notice = "" }, modifier = Modifier.align(Alignment.Start)) {
                                Icon(Icons.Rounded.ArrowBack, null)
                                Spacer(Modifier.width(5.dp))
                                Text(sh("Oyuncu bilgilerine dön", "Back to player details"), fontWeight = FontWeight.Bold)
                            }
                        }
                        if (notice.isNotBlank()) {
                            Surface(color = if (success) Color(0xFFE8F7EE) else Color(0xFFFFF4E5), shape = RoundedCornerShape(14.dp)) {
                                Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = authColors.onSurface, fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
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


@Composable
private fun EmailVerificationCard(
    email: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    busy: Boolean,
    notice: String,
    success: Boolean,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .96f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFB8D4F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                color = Color(0xFFEAF3FF),
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MarkEmailUnread,
                        contentDescription = null,
                        tint = Color(0xFF1769E0),
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Text(
                "E-postanı doğrula",
                color = Color(0xFF142B4F),
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
            Text(
                email,
                color = Color(0xFF1769E0),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "Maildeki doğrulama bağlantısına dokunduğunda Son Harf otomatik açılır. Bağlantı çalışmazsa e-postadaki 6 haneli kodu gir.",
                color = Color(0xFF607596),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("6 haneli doğrulama kodu") },
                placeholder = { Text("000000") },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 6.sp,
                ),
            )
            Button(
                onClick = onVerify,
                enabled = !busy && otpCode.length == 6,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(if (busy) "…" else "KODU DOĞRULA", fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                onClick = onResend,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("KODU YENİDEN GÖNDER", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onChangeEmail, enabled = !busy) {
                Text("E-POSTA ADRESİNİ DEĞİŞTİR", fontWeight = FontWeight.Bold)
            }
            if (notice.isNotBlank()) {
                Surface(
                    color = if (success) Color(0xFFE8F7EE) else Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        notice,
                        Modifier.fillMaxWidth().padding(12.dp),
                        color = Color(0xFF142B4F),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
