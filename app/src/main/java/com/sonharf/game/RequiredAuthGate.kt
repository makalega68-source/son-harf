package com.sonharf.game

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
    var pendingVerificationEmail by remember { mutableStateOf<String?>(null) }
    var otpCode by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    fun friendly(raw: String): String = when {
        "Email not confirmed" in raw || "email_not_confirmed" in raw -> sh("E-posta adresini onaylamadan giriş yapamazsın. Gelen kutunu kontrol et.", "You cannot sign in until your email is verified. Check your inbox.")
        "Invalid login credentials" in raw -> sh("E-posta veya şifre hatalı.", "Incorrect email or password.")
        "already registered" in raw.lowercase() || "user_already_exists" in raw.lowercase() -> sh("Bu e-posta zaten kayıtlı. Giriş Yap sekmesini kullan.", "This email is already registered. Use the Sign In tab.")
        "Unable to validate email address" in raw || "validation_failed" in raw.lowercase() -> sh("E-posta adresi geçerli görünmüyor. Adresi kontrol edip tekrar dene.", "The email address does not look valid. Check it and try again.")
        "invalid_display_name" in raw -> sh("Oyuncu adı 2-24 karakter olmalı.", "Player name must be 2–24 characters.")
        "invalid_gender" in raw -> sh("Cinsiyet seçimi gerekli.", "Please select a profile option.")
        "password" in raw.lowercase() && "6" in raw -> sh("Şifre en az 6 karakter olmalı.", "Password must be at least 6 characters.")
        else -> raw.take(170).ifBlank { sh("İşlem tamamlanamadı. Tekrar dene.", "The request could not be completed. Try again.") }
    }


    fun verifyPendingEmail() {
        val targetEmail = pendingVerificationEmail ?: return
        if (busy) return
        if (otpCode.length != 6) {
            notice = sh("E-postana gelen 6 haneli kodu gir.", "Enter the 6-digit code sent to your email.")
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
                notice = sh("E-posta doğrulandı. Hoş geldin!", "Email verified. Welcome!")
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
                SupabaseProvider.client.auth.resendEmail(OtpType.Email.SIGNUP, targetEmail)
            }.onSuccess {
                success = true
                notice = sh("Yeni doğrulama e-postası gönderildi. Gelen kutunu ve spam klasörünü kontrol et.", "A new verification email was sent. Check your inbox and spam folder.")
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
                    Spacer(Modifier.height(10.dp))
                    AuthLanguageSelector(
                        selected = SonHarfUiState.language,
                        onSelect = { SonHarfPreferences.setLanguage(context, it) },
                    )
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
                    Spacer(Modifier.height(18.dp))
                    Text(
                        sh("Zincir uzadıkça ustalık ortaya çıkar.", "As the chain grows, mastery reveals itself."),
                        modifier = Modifier.fillMaxWidth(.92f),
                        color = Color(0xFF4E5F84),
                        fontFamily = FontFamily.Cursive,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(22.dp))
                }
            } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = { showForm = false; notice = ""; success = false },
                    ) {
                        Text("‹ " + sh("Giriş ekranına dön", "Back to login"), color = Color(0xFF1769E0), fontWeight = FontWeight.Bold)
                    }
                    AuthLanguageSelector(
                        selected = SonHarfUiState.language,
                        onSelect = { SonHarfPreferences.setLanguage(context, it) },
                        compact = true,
                    )
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
                            FilterChip(selected = register, onClick = { register = true; notice = "" }, label = { Text(sh("ÜYE OL", "REGISTER"), fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = !register, onClick = { register = false; notice = "" }, label = { Text(sh("GİRİŞ YAP", "SIGN IN"), fontSize = 15.sp) }, modifier = Modifier.weight(1f))
                        }
                        if (register) {
                            OutlinedTextField(displayName, { displayName = it.take(24) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(sh("Oyuncu adı", "Player name")) })
                            Text(sh("Bu ad oyuncu profilinde kalıcı olarak görünür.", "This name will remain on your player profile."), color = authColors.onSurfaceVariant, fontSize = 12.sp)
                            Text(sh("Profil seçimi", "Profile selection"), color = authColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("erkek" to sh("Erkek", "Male"), "kadın" to sh("Kadın", "Female"), "diğer" to sh("Diğer", "Other")).forEach { (value, label) ->
                                    FilterChip(selected = gender == value, onClick = { gender = value }, label = { Text(label, fontSize = 14.sp) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        OutlinedTextField(email, { email = it.trim().take(120) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(sh("E-posta", "Email")) })
                        OutlinedTextField(
                            password,
                            { password = it.take(64) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(sh("Şifre", "Password")) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showPassword = !showPassword }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                    Text(if (showPassword) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            },
                        )
                        if (register) {
                            OutlinedTextField(
                                password2,
                                { password2 = it.take(64) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(sh("Şifre tekrar", "Confirm password")) },
                                visualTransformation = if (showPassword2) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showPassword2 = !showPassword2 }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                        Text(if (showPassword2) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                        notice = sh("Önce geçerli e-posta adresini gir.", "Enter a valid email address first.")
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
                                            notice = sh("Şifre sıfırlama bağlantısı e-posta adresine gönderildi. Gelen kutunu ve spam klasörünü kontrol et.", "A password reset link was sent to your email. Check your inbox and spam folder.")
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
                                if (!email.contains("@") || password.length < 6) { notice = sh("Geçerli e-posta ve en az 6 karakterli şifre gir.", "Enter a valid email and a password with at least 6 characters."); return@Button }
                                if (register && displayName.trim().length < 2) { notice = sh("Oyuncu adı en az 2 karakter olmalı.", "Player name must be at least 2 characters."); return@Button }
                                if (register && gender.isBlank()) { notice = sh("Kadın, Erkek veya Diğer seçeneklerinden birini seç.", "Select Female, Male, or Other."); return@Button }
                                if (register && password != password2) { notice = sh("Şifreler aynı değil.", "Passwords do not match."); return@Button }
                                scope.launch {
                                    busy = true; notice = ""; success = false
                                    if (register) {
                                        runCatching {
                                            SupabaseProvider.client.auth.signOut()
                                            SupabaseProvider.client.auth.signUpWith(Email, redirectUrl = "sonharf://auth") {
                                                this.email = email.trim(); this.password = password
                                                data = buildJsonObject { put("display_name", displayName.trim()); put("gender", gender) }
                                            }
                                        }.onSuccess {
                                            SonHarfPreferences.rememberPendingRegistration(context, email, displayName, gender)
                                            success = true
                                            pendingVerificationEmail = email.trim()
                                            otpCode = ""
                                            notice = sh("Doğrulama e-postası gönderildi. Maildeki E-postamı Doğrula butonuna dokun veya 6 haneli kodu buraya gir.", "Verification email sent. Tap the Verify My Email button in the message or enter the 6-digit code here.")
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
                }
                Spacer(Modifier.height(24.dp))
            }
            }
        }
    }
}


@Composable
private fun AuthLanguageSelector(
    selected: String,
    onSelect: (String) -> Unit,
    compact: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = .88f),
        border = BorderStroke(1.dp, Color(0xFFD4E3F7)),
    ) {
        Row(
            Modifier.padding(horizontal = if (compact) 5.dp else 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("tr" to "Türkçe", "en" to "English").forEach { (code, label) ->
                FilterChip(
                    selected = selected == code,
                    onClick = { onSelect(code) },
                    label = {
                        Text(
                            label,
                            fontSize = if (compact) 11.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                )
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
            Surface(color = Color(0xFFEAF3FF), shape = CircleShape) {
                Text("✉", Modifier.padding(14.dp), fontSize = 28.sp)
            }
            Text(
                sh("E-postanı doğrula", "Verify your email"),
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
                sh("Maildeki “E-postamı Doğrula” butonuna dokunduğunda Son Harf otomatik açılır. Buton çalışmazsa aşağıdaki alana e-postadaki 6 haneli kodu gir.", "Tap “Verify My Email” in the message to open Son Harf automatically. If the button does not work, enter the 6-digit code from the email below."),
                color = Color(0xFF607596),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(sh("6 haneli doğrulama kodu", "6-digit verification code")) },
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
                Text(if (busy) "…" else sh("KODU DOĞRULA", "VERIFY CODE"), fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                onClick = onResend,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(sh("KODU YENİDEN GÖNDER", "RESEND CODE"), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onChangeEmail, enabled = !busy) {
                Text(sh("E-POSTA ADRESİNİ DEĞİŞTİR", "CHANGE EMAIL ADDRESS"), fontWeight = FontWeight.Bold)
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
