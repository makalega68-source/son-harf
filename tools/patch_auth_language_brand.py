from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, found {count}: {old[:80]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


auth = "app/src/main/java/com/sonharf/game/RequiredAuthGate.kt"

replace_once(
    auth,
    '''    fun friendly(raw: String): String = when {
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
    }''',
    '''    fun friendly(raw: String): String = when {
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
    }''',
)

replacements = {
    'notice = "E-postana gelen 6 haneli kodu gir."': 'notice = sh("E-postana gelen 6 haneli kodu gir.", "Enter the 6-digit code sent to your email.")',
    'notice = "E-posta doğrulandı. Hoş geldin!"': 'notice = sh("E-posta doğrulandı. Hoş geldin!", "Email verified. Welcome!")',
    'notice = "Yeni doğrulama e-postası gönderildi. Gelen kutunu ve spam klasörünü kontrol et."': 'notice = sh("Yeni doğrulama e-postası gönderildi. Gelen kutunu ve spam klasörünü kontrol et.", "A new verification email was sent. Check your inbox and spam folder.")',
    'label = { Text("ÜYE OL", fontSize = 15.sp) }': 'label = { Text(sh("ÜYE OL", "REGISTER"), fontSize = 15.sp) }',
    'label = { Text("GİRİŞ YAP", fontSize = 15.sp) }': 'label = { Text(sh("GİRİŞ YAP", "SIGN IN"), fontSize = 15.sp) }',
    'label = { Text("Oyuncu adı") }': 'label = { Text(sh("Oyuncu adı", "Player name")) }',
    'Text(sh("Bu ad oyuncu profilinde kalıcı olarak görünür.", "This name will remain on your player profile."),': 'Text(sh("Bu ad oyuncu profilinde kalıcı olarak görünür.", "This name will remain on your player profile."),',
    'Text(sh("Profil seçimi", "Profile selection"),': 'Text(sh("Profil seçimi", "Profile selection"),',
    'listOf("erkek" to "Erkek", "kadın" to "Kadın", "diğer" to "Diğer")': 'listOf("erkek" to sh("Erkek", "Male"), "kadın" to sh("Kadın", "Female"), "diğer" to sh("Diğer", "Other"))',
    'label = { Text("E-posta") }': 'label = { Text(sh("E-posta", "Email")) }',
    'label = { Text("Şifre") }': 'label = { Text(sh("Şifre", "Password")) }',
    'label = { Text("Şifre tekrar") }': 'label = { Text(sh("Şifre tekrar", "Confirm password")) }',
    'Text(if (showPassword) "GİZLE" else "GÖSTER", fontWeight = FontWeight.Bold, fontSize = 11.sp)': 'Text(if (showPassword) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"), fontWeight = FontWeight.Bold, fontSize = 11.sp)',
    'Text(if (showPassword2) "GİZLE" else "GÖSTER", fontWeight = FontWeight.Bold, fontSize = 11.sp)': 'Text(if (showPassword2) sh("GİZLE", "HIDE") else sh("GÖSTER", "SHOW"), fontWeight = FontWeight.Bold, fontSize = 11.sp)',
    'notice = "Önce geçerli e-posta adresini gir."': 'notice = sh("Önce geçerli e-posta adresini gir.", "Enter a valid email address first.")',
    'notice = "Şifre sıfırlama bağlantısı e-posta adresine gönderildi. Gelen kutunu ve spam klasörünü kontrol et."': 'notice = sh("Şifre sıfırlama bağlantısı e-posta adresine gönderildi. Gelen kutunu ve spam klasörünü kontrol et.", "A password reset link was sent. Check your inbox and spam folder.")',
    'notice = "Geçerli e-posta ve en az 6 karakterli şifre gir."': 'notice = sh("Geçerli e-posta ve en az 6 karakterli şifre gir.", "Enter a valid email and a password of at least 6 characters.")',
    'notice = "Oyuncu adı en az 2 karakter olmalı."': 'notice = sh("Oyuncu adı en az 2 karakter olmalı.", "Player name must be at least 2 characters.")',
    'notice = "Kadın, Erkek veya Diğer seçeneklerinden birini seç."': 'notice = sh("Kadın, Erkek veya Diğer seçeneklerinden birini seç.", "Select Female, Male, or Other.")',
    'notice = "Şifreler aynı değil."': 'notice = sh("Şifreler aynı değil.", "Passwords do not match.")',
    'notice = "Hesabın zaten vardı; giriş yapıldı."': 'notice = sh("Hesabın zaten vardı; giriş yapıldı.", "Your account already existed; you were signed in.")',
    'notice = "Doğrulama e-postası yeniden gönderildi. Gelen kutusu ve spam klasörünü kontrol et."': 'notice = sh("Doğrulama e-postası yeniden gönderildi. Gelen kutusu ve spam klasörünü kontrol et.", "The verification email was sent again. Check your inbox and spam folder.")',
    'notice = "Doğrulama e-postası gönderildi. Maildeki doğrulama bağlantısına dokun veya 6 haneli kodu buraya gir."': 'notice = sh("Doğrulama e-postası gönderildi. Maildeki doğrulama bağlantısına dokun veya 6 haneli kodu buraya gir.", "Verification email sent. Tap the verification link or enter the 6-digit code here.")',
}

p = Path(auth)
text = p.read_text(encoding="utf-8")
for old, new in replacements.items():
    if old == new:
        continue
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{auth}: expected 1 match, found {count}: {old!r}")
    text = text.replace(old, new, 1)
p.write_text(text, encoding="utf-8")

replace_once(
    auth,
    '''                    Image(
                        painter = painterResource(R.drawable.son_harf_gold_teal_logo),
                        contentDescription = "Son Harf",
                        modifier = Modifier.fillMaxWidth(.94f).heightIn(max = 255.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        sh("Kelimeyi Sürdür, Rakibini Geç", "Continue the Word, Beat Your Rival"),
                        color = AuthUi.Primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )''',
    '''                    Image(
                        painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                        contentDescription = sh("Kelime Kuşatması logosu", "Word Siege logo"),
                        modifier = Modifier.fillMaxWidth(.72f).heightIn(max = 150.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        sh("KELİME KUŞATMASI", "WORD SIEGE"),
                        color = AuthUi.Text,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        sh("Kelime oyunu • taktik alan savaşı • sosyal rekabet", "Word game • tactical territory battle • social competition"),
                        color = AuthUi.Primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )''',
)

stable = "app/src/main/java/com/sonharf/game/StableV1App.kt"
replace_once(stable, "import androidx.compose.foundation.BorderStroke\n", "import androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.Image\n")
replace_once(stable, "import androidx.compose.ui.platform.LocalContext\n", "import androidx.compose.ui.layout.ContentScale\nimport androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.res.painterResource\n")
replace_once(
    stable,
    '                SonHarfOfficialLogo(modifier = Modifier.fillMaxWidth(.88f).height(168.dp))',
    '''                Image(
                    painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                    contentDescription = "Kelime Kuşatması / Word Siege",
                    modifier = Modifier.fillMaxWidth(.72f).height(132.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = "KELİME KUŞATMASI / WORD SIEGE",
                    color = MainUi.Text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )''',
)

print("auth language and brand patch applied")
