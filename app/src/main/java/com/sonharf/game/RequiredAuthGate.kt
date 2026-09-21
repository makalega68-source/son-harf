package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private object AuthUi {
    val Background: Color get() = SonHarfTheme.Background
    val Surface: Color get() = SonHarfTheme.Surface
    val SurfaceSoft: Color get() = SonHarfTheme.SurfaceSecondary
    val Primary: Color get() = SonHarfTheme.Primary
    val PrimarySoft: Color get() = SonHarfTheme.PrimarySoft
    val SoftBlue: Color get() = SonHarfTheme.SoftBlue
    val Turquoise: Color get() = SonHarfTheme.Turquoise
    val Lavender: Color get() = SonHarfTheme.Purple
    val Text: Color get() = SonHarfTheme.TextPrimary
    val Muted: Color get() = SonHarfTheme.TextSecondary
    val Border: Color get() = SonHarfTheme.Border
    val SuccessSoft: Color get() = SonHarfTheme.SuccessSoft
    val WarningSoft: Color get() = SonHarfTheme.Sand
    val Error: Color get() = SonHarfTheme.Error
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
    var register by remember { mutableStateOf(true) }
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
        primary = AuthUi.Primary,
        onPrimary = Color.White,
        primaryContainer = AuthUi.PrimarySoft,
        onPrimaryContainer = AuthUi.Text,
        secondary = AuthUi.Turquoise,
        onSecondary = Color.White,
        secondaryContainer = AuthUi.SurfaceSoft,
        onSecondaryContainer = AuthUi.Primary,
        tertiary = AuthUi.Lavender,
        onTertiary = Color.White,
        background = AuthUi.Background,
        surface = AuthUi.Surface,
        surfaceVariant = AuthUi.SurfaceSoft,
        onBackground = AuthUi.Text,
        onSurface = AuthUi.Text,
        onSurfaceVariant = AuthUi.Muted,
        outline = AuthUi.Border,
        error = AuthUi.Error,
    )

    MaterialTheme(colorScheme = authColors) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            PurchasedGameBackdrop(Modifier.matchParentSize())

            if (!showForm) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(.30f))
                    PurchasedPanel(
                        modifier = Modifier.fillMaxWidth(),
                        asset = PurchasedUiAsset.PANEL_LARGE,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                                contentDescription = sh("Kelime Kuşatması", "Word Siege"),
                                modifier = Modifier.fillMaxWidth(.88f).heightIn(max = 190.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                sh("Kelime oyunu • Taktik alan savaşı • Sosyal rekabet", "Word game • Tactical territory battle • Social competition"),
                                color = Color(0xFF6D4A35),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(22.dp))
                            PurchasedButton(
                                text = sh("GİRİŞ YAP", "SIGN IN"),
                                onClick = { register = false; notice = ""; showForm = true },
                                modifier = Modifier.fillMaxWidth().height(62.dp),
                                style = PurchasedButtonStyle.PRIMARY,
                                leadingAsset = PurchasedUiAsset.ICON_CHECK,
                            )
                            Spacer(Modifier.height(10.dp))
                            PurchasedButton(
                                text = sh("KAYIT OL", "REGISTER"),
                                onClick = { register = true; notice = ""; showForm = true },
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                style = PurchasedButtonStyle.PURPLE,
                                leadingAsset = PurchasedUiAsset.NAV_PROFILE,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    PurchasedButton(
                        text = sh("GERİ", "BACK"),
                        onClick = { showForm = false; notice = ""; success = false },
                        modifier = Modifier.align(Alignment.Start).width(132.dp).height(44.dp),
                        style = PurchasedButtonStyle.SECONDARY,
                        leadingAsset = PurchasedUiAsset.NAV_HOME,
                    )

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
                        PurchasedPanel(
                            modifier = Modifier.fillMaxWidth(),
                            asset = PurchasedUiAsset.PANEL_LARGE,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                        ) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PurchasedSectionHeader(
                                    title = if (register) sh("YENİ OYUNCU", "NEW PLAYER") else sh("OYUNCU GİRİŞİ", "PLAYER SIGN IN"),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PurchasedButton(
                                        text = sh("ÜYE OL", "REGISTER"),
                                        selectedStyle(register, PurchasedButtonStyle.PURPLE, PurchasedButtonStyle.SECONDARY),
                                        onClick = { register = true; notice = "" },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                    )
                                    PurchasedButton(
                                        text = sh("GİRİŞ YAP", "SIGN IN"),
                                        style = selectedStyle(!register, PurchasedButtonStyle.PRIMARY, PurchasedButtonStyle.SECONDARY),
                                        onClick = { register = false; notice = "" },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                    )
                                }

                                if (register) {
                                    PurchasedAuthTextField(
                                        value = displayName,
                                        onValueChange = { displayName = it.take(24) },
                                        label = sh("Oyuncu adı", "Player name"),
                                        leadingAsset = PurchasedUiAsset.NAV_PROFILE,
                                    )
                                    Text(
                                        sh("Bu ad oyuncu profilinde kalıcı olarak görünür.", "This name remains visible on your player profile."),
                                        color = AuthUi.Muted,
                                        fontSize = 11.sp,
                                    )
                                    Text(sh("Profil seçimi", "Profile selection"), color = AuthUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        listOf(
                                            "erkek" to sh("ERKEK", "MALE"),
                                            "kadın" to sh("KADIN", "FEMALE"),
                                            "diğer" to sh("DİĞER", "OTHER"),
                                        ).forEach { (value, label) ->
                                            PurchasedButton(
                                                text = label,
                                                onClick = { gender = value },
                                                modifier = Modifier.weight(1f).height(42.dp),
                                                style = selectedStyle(gender == value, PurchasedButtonStyle.PURPLE, PurchasedButtonStyle.SECONDARY),
                                            )
                                        }
                                    }
                                }

                                PurchasedAuthTextField(
                                    value = email,
                                    onValueChange = { email = it.trim().take(120) },
                                    label = sh("E-posta", "Email"),
                                    leadingAsset = PurchasedUiAsset.LOGIN_MAIL,
                                )
                                PurchasedAuthTextField(
                                    value = password,
                                    onValueChange = { password = it.take(64) },
                                    label = sh("Şifre", "Password"),
                                    leadingAsset = PurchasedUiAsset.LOGIN_KEY,
                                    secret = !showPassword,
                                    trailingLabel = if (showPassword) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"),
                                    onTrailing = { showPassword = !showPassword },
                                )

                                if (register) {
                                    PurchasedAuthTextField(
                                        value = password2,
                                        onValueChange = { password2 = it.take(64) },
                                        label = sh("Şifre tekrar", "Repeat password"),
                                        leadingAsset = PurchasedUiAsset.LOGIN_KEY,
                                        secret = !showPassword2,
                                        trailingLabel = if (showPassword2) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"),
                                        onTrailing = { showPassword2 = !showPassword2 },
                                    )
                                } else {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        PurchasedAuthToggle(
                                            checked = rememberMe,
                                            onCheckedChange = {
                                                rememberMe = it
                                                if (!it) {
                                                    RememberedCredentialVault.clear(context)
                                                    SonHarfPreferences.setRememberLogin(context, false)
                                                }
                                            },
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(sh("Bu cihazda beni hatırla", "Remember me on this device"), color = AuthUi.Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                                        Text(sh("Şifremi unuttum", "Forgot password"), color = AuthUi.Primary, fontWeight = FontWeight.Black)
                                    }
                                }

                                PurchasedButton(
                                    text = if (busy) "…" else if (register) sh("KAYIT OL", "REGISTER") else sh("GİRİŞ YAP", "SIGN IN"),
                                    onClick = {
                                        if (busy) return@PurchasedButton
                                        if (!email.contains("@") || password.length < 6) {
                                            notice = "Geçerli e-posta ve en az 6 karakterli şifre gir."
                                            return@PurchasedButton
                                        }
                                        if (register && displayName.trim().length < 2) {
                                            notice = "Oyuncu adı en az 2 karakter olmalı."
                                            return@PurchasedButton
                                        }
                                        if (register && gender.isBlank()) {
                                            notice = "Kadın, Erkek veya Diğer seçeneklerinden birini seç."
                                            return@PurchasedButton
                                        }
                                        if (register && password != password2) {
                                            notice = "Şifreler aynı değil."
                                            return@PurchasedButton
                                        }
                                        scope.launch {
                                            busy = true
                                            notice = ""
                                            success = false
                                            if (register) {
                                                val targetEmail = email.trim()
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
                                                    SupabaseProvider.client.auth.signInWith(Email) {
                                                        this.email = email.trim()
                                                        this.password = password
                                                    }
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
                                    style = if (register) PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.PRIMARY,
                                    leadingAsset = PurchasedUiAsset.ICON_CHECK,
                                )

                                if (notice.isNotBlank()) {
                                    PurchasedPanel(
                                        modifier = Modifier.fillMaxWidth(),
                                        asset = PurchasedUiAsset.PANEL_SMALL,
                                        contentPadding = PaddingValues(10.dp),
                                    ) {
                                        Text(
                                            notice,
                                            Modifier.fillMaxWidth(),
                                            color = if (success) Color(0xFF2C6D42) else Color(0xFF7B4A25),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                        )
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

private fun selectedStyle(
    selected: Boolean,
    selectedStyle: PurchasedButtonStyle,
    normalStyle: PurchasedButtonStyle,
): PurchasedButtonStyle = if (selected) selectedStyle else normalStyle

@Composable
private fun PurchasedAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingAsset: PurchasedUiAsset,
    secret: Boolean = false,
    trailingLabel: String? = null,
    onTrailing: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
        PurchasedAsset(PurchasedUiAsset.LOGIN_FIELD, Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PurchasedAsset(leadingAsset, Modifier.size(32.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(label, fontSize = 11.sp) },
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = if (trailingLabel != null && onTrailing != null) {
                    {
                        TextButton(onClick = onTrailing, contentPadding = PaddingValues(horizontal = 5.dp)) {
                            Text(trailingLabel, fontWeight = FontWeight.Black, fontSize = 9.sp, color = AuthUi.Primary)
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = AuthUi.Text,
                    unfocusedTextColor = AuthUi.Text,
                    focusedLabelColor = AuthUi.Primary,
                    unfocusedLabelColor = AuthUi.Muted,
                    cursorColor = AuthUi.Primary,
                ),
            )
        }
    }
}

@Composable
private fun PurchasedAuthToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        Modifier
            .width(66.dp)
            .height(32.dp)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        PurchasedAsset(
            if (checked) PurchasedUiAsset.TOGGLE_ON else PurchasedUiAsset.TOGGLE_OFF,
            Modifier.matchParentSize(),
        )
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
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PurchasedAsset(PurchasedUiAsset.LOGIN_MAIL, Modifier.size(64.dp))
            PurchasedSectionHeader(
                title = sh("E-POSTANI DOĞRULA", "VERIFY YOUR EMAIL"),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(email, color = AuthUi.SoftBlue, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text(
                sh(
                    "Maildeki doğrulama bağlantısına dokun. Bağlantı çalışmazsa e-postadaki 6 haneli kodu gir.",
                    "Tap the verification link in the email. If the link does not work, enter the six-digit code from the email.",
                ),
                color = AuthUi.Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            PurchasedAuthTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                label = sh("6 haneli doğrulama kodu", "6-digit verification code"),
                leadingAsset = PurchasedUiAsset.LOGIN_KEY,
            )
            PurchasedButton(
                text = if (busy) "…" else sh("KODU DOĞRULA", "VERIFY CODE"),
                onClick = onVerify,
                enabled = !busy && otpCode.length == 6,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                style = PurchasedButtonStyle.PRIMARY,
                leadingAsset = PurchasedUiAsset.ICON_CHECK,
            )
            PurchasedButton(
                text = sh("KODU YENİDEN GÖNDER", "RESEND CODE"),
                onClick = onResend,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                style = PurchasedButtonStyle.SECONDARY,
                leadingAsset = PurchasedUiAsset.ICON_REPEAT,
            )
            TextButton(onClick = onChangeEmail, enabled = !busy) {
                Text(sh("E-POSTA ADRESİNİ DEĞİŞTİR", "CHANGE EMAIL ADDRESS"), color = AuthUi.Primary, fontWeight = FontWeight.Black)
            }
            if (notice.isNotBlank()) {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(10.dp),
                ) {
                    Text(
                        notice,
                        Modifier.fillMaxWidth(),
                        color = if (success) Color(0xFF2C6D42) else Color(0xFF7B4A25),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
