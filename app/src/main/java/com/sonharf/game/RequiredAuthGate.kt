package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Context
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun hasVerifiedMembershipSession(): Boolean =
    runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.email?.isNotBlank() == true }.getOrDefault(false)

/**
 * Restores the player's session at startup so a signed-in player goes straight into the game.
 * The stored session loads asynchronously, so this waits for it first; a session that could not
 * be refreshed (e.g. offline) still counts, and remembered credentials allow a silent re-login.
 */
suspend fun restoreMembershipSession(context: Context): Boolean {
    if (!SupabaseProvider.configured) return false
    val auth = SupabaseProvider.client.auth
    withTimeoutOrNull(5_000L) { auth.sessionStatus.first { it !is SessionStatus.Initializing } }
    if (hasVerifiedMembershipSession()) return true
    if (auth.currentSessionOrNull()?.user?.email?.isNotBlank() == true) return true
    if (!SonHarfPreferences.rememberLogin(context)) return false
    val saved = RememberedCredentialVault.load(context) ?: return false
    return runCatching {
        auth.signInWith(Email) {
            this.email = saved.email
            this.password = saved.password
        }
        hasVerifiedMembershipSession()
    }.getOrDefault(false)
}

@Serializable
private data class AuthIdentityProfile(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val gender: String? = null,
    @SerialName("identity_locked") val identityLocked: Boolean = false,
)

/**
 * Pre-authentication palette.
 *
 * Kimlik ekranı da uygulamanın yetişkin zümrüt-altın sistemiyle aynı görünür.
 * Profil teması yüklenmeden önce sabit token kullanılması ilk karede renk sıçramasını önler.
 */
private object AuthUi {
    val Background get() = SonHarfTheme.Background
    val BackgroundTop get() = Color(0xFFFFFCF6)
    val Surface get() = SonHarfTheme.Surface
    val SurfaceSoft get() = SonHarfTheme.SurfaceSecondary
    val SurfaceRaised get() = SonHarfTheme.SurfaceElevated
    val Modal get() = SonHarfTheme.ModalSurface
    val Primary get() = SonHarfTheme.Primary
    val PrimarySoft get() = SonHarfTheme.PrimarySoft
    val SoftBlue get() = SonHarfTheme.PremiumGold
    val Turquoise get() = SonHarfTheme.Turquoise
    val Lavender get() = SonHarfTheme.Lavender
    val Sand get() = SonHarfTheme.Sand
    val Text get() = SonHarfTheme.TextPrimary
    val Muted get() = SonHarfTheme.TextSecondary
    val Border get() = SonHarfTheme.Border
    val BorderSoft get() = SonHarfTheme.Border
    val Success get() = SonHarfTheme.Success
    val SuccessSoft get() = SonHarfTheme.SuccessSoft
    val Warning get() = SonHarfTheme.Warning
    val WarningSoft get() = Color(0xFFF6EACC)
    val Error get() = SonHarfTheme.Error
    val Ivory = Color(0xFFF0EDE3)
    val Ink = Color(0xFF202C27)
}

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
    // A device that has signed in before starts on "sign in"; new players start on "register".
    var register by remember { mutableStateOf(rememberedCredential == null && SonHarfPreferences.rememberedEmail(context).isBlank()) }
    var uiLanguage by remember { mutableStateOf(SonHarfUiState.language) }
    var displayName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var email by remember {
        mutableStateOf(rememberedCredential?.email ?: SonHarfPreferences.rememberedEmail(context))
    }
    var password by remember { mutableStateOf(rememberedCredential?.password.orEmpty()) }
    var password2 by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPassword2 by remember { mutableStateOf(false) }
    // Staying signed in is the default: one login, then the app icon opens the game directly.
    var rememberMe by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    var pendingVerificationEmail by remember { mutableStateOf<String?>(null) }
    var otpCode by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    fun friendly(raw: String): String = when {
        "Email not confirmed" in raw || "email_not_confirmed" in raw -> sh("E-posta adresini onaylamadan giriş yapamazsın. Gelen kutunu kontrol et.", "Confirm your email before signing in. Check your inbox.")
        "Invalid login credentials" in raw -> sh("E-posta veya şifre hatalı.", "Incorrect email or password.")
        "existing_confirmed_account" in raw -> sh("Bu e-posta zaten kayıtlı ve doğrulanmış. Giriş Yap bölümünü kullan; şifreni unuttuysan Şifremi unuttum'a dokun.", "This email is already registered and verified. Use Sign In, or Forgot password if needed.")
        "already registered" in raw.lowercase() || "user_already_exists" in raw.lowercase() -> sh("Bu e-posta zaten kayıtlı. Giriş Yap bölümünü kullan.", "This email is already registered. Use Sign In.")
        "email rate limit" in raw.lowercase() || "over_email_send_rate_limit" in raw.lowercase() -> sh("Çok sık e-posta istendi. Birkaç dakika bekleyip tekrar dene.", "Too many emails were requested. Wait a few minutes and try again.")
        "Unable to validate email address" in raw || "validation_failed" in raw.lowercase() -> sh("E-posta adresi geçerli görünmüyor. Adresi kontrol edip tekrar dene.", "The email address does not look valid. Check it and try again.")
        "invalid_display_name" in raw -> sh("Oyuncu adı 2-24 karakter olmalı.", "Player name must be 2-24 characters.")
        "invalid_gender" in raw -> sh("Profil seçimi gerekli.", "Profile selection is required.")
        "password" in raw.lowercase() && "6" in raw -> sh("Şifre en az 6 karakter olmalı.", "Password must be at least 6 characters.")
        else -> raw.take(170).ifBlank { sh("İşlem tamamlanamadı. Tekrar dene.", "The action could not be completed. Try again.") }
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
                if (password.length >= 6) RememberedCredentialVault.save(context, targetEmail, password)
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
                SupabaseProvider.client.auth.resendEmail(
                    type = OtpType.Email.SIGNUP,
                    email = targetEmail,
                    redirectUrl = "sonharf://auth",
                )
            }.onSuccess {
                success = true
                notice = sh("Yeni doğrulama e-postası gönderildi. Gelen kutunu ve spam klasörünü kontrol et.", "A new verification email was sent. Check your inbox and spam folder.")
            }.onFailure {
                notice = friendly(it.message.orEmpty())
            }
            busy = false
        }
    }

    val authColors = (if (SonHarfTheme.IsDark) darkColorScheme() else lightColorScheme()).copy(
        primary = AuthUi.Primary,
        onPrimary = AuthUi.Ivory,
        primaryContainer = AuthUi.PrimarySoft,
        onPrimaryContainer = AuthUi.Text,
        secondary = AuthUi.Turquoise,
        onSecondary = AuthUi.Ivory,
        secondaryContainer = AuthUi.SurfaceSoft,
        onSecondaryContainer = AuthUi.Primary,
        tertiary = AuthUi.Lavender,
        onTertiary = AuthUi.Ivory,
        background = AuthUi.Background,
        surface = AuthUi.Surface,
        surfaceVariant = AuthUi.SurfaceSoft,
        onBackground = AuthUi.Text,
        onSurface = AuthUi.Text,
        onSurfaceVariant = AuthUi.Muted,
        outline = AuthUi.Border,
        outlineVariant = AuthUi.BorderSoft,
        error = AuthUi.Error,
    )

    fun submit() {
        if (busy) return
        if (!email.contains("@") || password.length < 6) { notice = sh("Geçerli e-posta ve en az 6 karakterli şifre gir.", "Enter a valid email and a password of at least 6 characters."); return }
        if (register && displayName.trim().length < 2) { notice = sh("Oyuncu adı en az 2 karakter olmalı.", "Player name must be at least 2 characters."); return }
        if (register && gender.isBlank()) { notice = sh("Kadın, Erkek veya Diğer seçeneklerinden birini seç.", "Select Female, Male, or Other."); return }
        if (register && password != password2) { notice = sh("Şifreler aynı değil.", "Passwords do not match."); return }
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
                    RememberedCredentialVault.save(context, targetEmail, password)
                    success = true
                    notice = sh("Hesabın zaten vardı; giriş yapıldı.", "Your account already existed; you were signed in.")
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
                        notice = sh("Doğrulama e-postası yeniden gönderildi. Gelen kutusu ve spam klasörünü kontrol et.", "The verification email was sent again. Check your inbox and spam folder.")
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
                            notice = sh("Doğrulama e-postası gönderildi. Maildeki doğrulama bağlantısına dokun veya 6 haneli kodu buraya gir.", "Verification email sent. Tap the verification link or enter the 6-digit code here.")
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
    }

    MaterialTheme(colorScheme = authColors) {
        Box(
            Modifier
                .fillMaxSize()
                .background(authColors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            SonHarfLeafBackdrop(Modifier.matchParentSize())
            // Generated directly in Compose so no stale bitmap can survive an app update.
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            AuthUi.BackgroundTop,
                            AuthUi.Background,
                            AuthUi.Modal,
                            AuthUi.SurfaceSoft,
                        )
                    )
                )
            )
            Box(
                Modifier
                    .size(360.dp)
                    .offset(x = 170.dp, y = (-105).dp)
                    .background(Brush.radialGradient(listOf(AuthUi.Primary.copy(alpha = .16f), Color.Transparent)), CircleShape)
            )
            Box(
                Modifier
                    .size(310.dp)
                    .offset(x = (-155).dp, y = 355.dp)
                    .background(Brush.radialGradient(listOf(AuthUi.Lavender.copy(alpha = .12f), Color.Transparent)), CircleShape)
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Language can still be switched here; the email screen is the only step.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AuthLanguagePill(language = uiLanguage, onLanguage = { next ->
                        uiLanguage = next
                        SonHarfUiState.language = next
                        SonHarfPreferences.setLanguage(context, next)
                    })
                }
                AuthBrandHeader()
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
                        colors = CardDefaults.cardColors(containerColor = AuthUi.Surface.copy(alpha = .97f)),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, AuthUi.Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Segmented switch between signing in and registering.
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(AuthUi.SurfaceSoft, RoundedCornerShape(18.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(false to sh("GİRİŞ YAP", "SIGN IN"), true to sh("ÜYE OL", "REGISTER")).forEach { (isRegister, label) ->
                                    val selected = register == isRegister
                                    Surface(
                                        onClick = { register = isRegister; notice = "" },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) AuthUi.Surface else Color.Transparent,
                                        shadowElevation = if (selected) 3.dp else 0.dp,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(label, color = if (selected) AuthUi.Primary else AuthUi.Muted, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                            Text(
                                if (register) sh("Yeni hesabını oluştur; bir kez gir, hep hatırlansın.", "Create your account; sign in once and stay signed in.")
                                else sh("Tekrar hoş geldin! Bir kez giriş yap, uygulama seni hatırlar.", "Welcome back! Sign in once and the app remembers you."),
                                color = AuthUi.Muted,
                                fontSize = 13.sp,
                            )
                            if (register) {
                                OutlinedTextField(
                                    displayName,
                                    { displayName = it.take(24) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    leadingIcon = { Icon(Icons.Rounded.Person, null, tint = AuthUi.Primary) },
                                    label = { Text(sh("Oyuncu adı", "Player name")) },
                                )
                                Text(sh("Bu ad oyuncu profilinde kalıcı olarak görünür.", "This name will remain on your player profile."), color = authColors.onSurfaceVariant, fontSize = 12.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("erkek" to sh("Erkek", "Male"), "kadın" to sh("Kadın", "Female"), "diğer" to sh("Diğer", "Other")).forEach { (value, label) ->
                                        val selected = gender == value
                                        FilterChip(
                                            selected = selected,
                                            onClick = { gender = value },
                                            label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold) } },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = AuthUi.Surface,
                                                labelColor = AuthUi.Muted,
                                                selectedContainerColor = AuthUi.PrimarySoft,
                                                selectedLabelColor = AuthUi.Primary,
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selected,
                                                borderColor = AuthUi.Border,
                                                selectedBorderColor = AuthUi.Primary.copy(alpha = .45f),
                                            ),
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                email,
                                { email = it.trim().take(120) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = { Icon(Icons.Rounded.Email, null, tint = AuthUi.Primary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                label = { Text(sh("E-posta", "Email")) },
                            )
                            OutlinedTextField(
                                password,
                                { password = it.take(64) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = AuthUi.Primary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (register) ImeAction.Next else ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { submit() }),
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
                                    shape = RoundedCornerShape(16.dp),
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = AuthUi.Primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { submit() }),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                                        Text(sh("Beni hatırla", "Remember me"), color = authColors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                                                    SupabaseProvider.client.auth.resetPasswordForEmail(
                                                        email = email.trim(),
                                                        redirectUrl = "sonharf://auth",
                                                    )
                                                }.onSuccess {
                                                    success = true
                                                    notice = sh("Şifre sıfırlama bağlantısı e-posta adresine gönderildi. Gelen kutunu ve spam klasörünü kontrol et.", "A password reset link was sent. Check your inbox and spam folder.")
                                                }.onFailure {
                                                    notice = friendly(it.message.orEmpty())
                                                }
                                                busy = false
                                            }
                                        },
                                        enabled = !busy,
                                    ) {
                                        Text(sh("Şifremi unuttum", "Forgot password"), color = AuthUi.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                            Button(
                                onClick = { submit() },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (register) AuthUi.Turquoise else AuthUi.Primary,
                                    contentColor = AuthUi.Ivory,
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = AuthUi.Ivory, strokeWidth = 2.5.dp)
                                } else {
                                    Text(
                                        if (register) sh("KAYIT OL", "REGISTER") else sh("GİRİŞ YAP", "SIGN IN"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(24.dp))
                                }
                            }
                            if (notice.isNotBlank()) {
                                Surface(color = if (success) AuthUi.SuccessSoft else AuthUi.WarningSoft, shape = RoundedCornerShape(14.dp)) {
                                    Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = authColors.onSurface, fontSize = 14.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    Text(
                        sh("Bir kez giriş yaptıktan sonra uygulama simgesine dokunman yeterli.", "After signing in once, just tap the app icon to play."),
                        color = AuthUi.Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/** Brand block: the name spelled on letter tiles, the English name and the tagline. */
@Composable
private fun AuthBrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            "KELİME".forEachIndexed { index, letter ->
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = AuthUi.Ivory,
                    border = BorderStroke(1.dp, AuthUi.Sand.copy(alpha = .55f)),
                    shadowElevation = 3.dp,
                ) {
                    Text(letter.toString(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = AuthUi.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "KELİME TAHTI",
            color = AuthUi.Text,
            fontSize = 27.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            "WORD BOARD",
            color = AuthUi.Sand,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            sh("Kelime oyunu • taktik alan savaşı • sosyal rekabet", "Word game • tactical territory battle • social competition"),
            color = AuthUi.Primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthLanguagePill(language: String, onLanguage: (String) -> Unit) {
    Row(
        Modifier.background(AuthUi.Surface.copy(alpha = .9f), RoundedCornerShape(99.dp)).padding(3.dp),
    ) {
        listOf("tr" to "🇹🇷 TR", "en" to "🇬🇧 EN").forEach { (code, label) ->
            val selected = language == code
            Surface(
                onClick = { onLanguage(code) },
                shape = RoundedCornerShape(99.dp),
                color = if (selected) AuthUi.Primary else Color.Transparent,
            ) {
                Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (selected) Color.White else AuthUi.Muted, fontSize = 12.sp, fontWeight = FontWeight.Black)
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
        colors = CardDefaults.cardColors(containerColor = AuthUi.Surface.copy(alpha = .97f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AuthUi.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                color = AuthUi.PrimarySoft,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MarkEmailUnread,
                        contentDescription = null,
                        tint = AuthUi.Primary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Text(
                sh("E-postanı doğrula", "Verify your email"),
                color = AuthUi.Text,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
            Text(
                email,
                color = AuthUi.SoftBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                sh(
                    "Maildeki doğrulama bağlantısına dokunduğunda Kelime Kuşatması otomatik açılır. Bağlantı çalışmazsa e-postadaki 6 haneli kodu gir.",
                    "Tap the verification link in the email to reopen Word Siege. If the link does not work, enter the 6-digit code from the email.",
                ),
                color = AuthUi.Muted,
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
                colors = ButtonDefaults.buttonColors(containerColor = AuthUi.Primary, contentColor = AuthUi.Ivory),
            ) {
                Text(if (busy) "…" else sh("KODU DOĞRULA", "VERIFY CODE"), fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                onClick = onResend,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, AuthUi.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthUi.Turquoise),
            ) {
                Text(sh("KODU YENİDEN GÖNDER", "RESEND CODE"), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onChangeEmail, enabled = !busy) {
                Text(sh("E-POSTA ADRESİNİ DEĞİŞTİR", "CHANGE EMAIL ADDRESS"), color = AuthUi.Primary, fontWeight = FontWeight.Bold)
            }
            if (notice.isNotBlank()) {
                Surface(
                    color = if (success) AuthUi.SuccessSoft else AuthUi.WarningSoft,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        notice,
                        Modifier.fillMaxWidth().padding(12.dp),
                        color = AuthUi.Text,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
