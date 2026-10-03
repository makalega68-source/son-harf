# Kelime Tahtı kayıt e-postası

## Confirm signup

**Konu:** `Kelime Tahtı · E-posta adresini doğrula`

Şablon: `supabase/templates/confirm-signup.html`

Şablon hem:
- `{{ .ConfirmationURL }}` ile tek tık doğrulama,
- `{{ .Token }}` ile 6 haneli yedek OTP

sunacak şekilde hazırlanmıştır.

## Mobil dönüş adresi

Android uygulaması `sonharf://auth` deep linkini kabul eder.
Supabase Dashboard → Authentication → URL Configuration bölümünde bu URI, izin verilen yönlendirme adreslerine eklenmelidir.

## Üretim e-posta standardı

Yayına çıkmadan önce:
1. Gönderici adı **Kelime Tahtı** olmalı.
2. Mümkünse markalı bir alan adı ve özel SMTP kullanılmalı.
3. SPF, DKIM ve DMARC kayıtları doğrulanmalı.
4. E-posta tracking, doğrulama linklerini bozuyorsa kapatılmalı.
5. Gmail ve yaygın Android e-posta istemcilerinde buton + OTP akışı test edilmeli.

> Not: Supabase'in varsayılan SMTP'si üretim markalama/teslim edilebilirlik açısından yeterli görülmemelidir.

Repo şablonunu değiştirmek canlı Supabase e-posta ayarlarını değiştirmez.
Yönetim API'sine auth_config_write yetkisi olan operatör, SUPABASE_ACCESS_TOKEN
ortam değişkenini sağladıktan sonra aşağıdaki komutu çalıştırabilir:

```sh
python scripts/configure_auth_email.py --project-ref bzdtftzdjtjoqhtcqtxb --apply
```

`--apply` olmadan yalnızca önerilen yapılandırma yazdırılır. Komut var olan SMTP
taşıma bilgilerine ve yönlendirme adreslerine dokunmaz; değiştirdiği dört alanı
tekrar okuyup doğrular. Token Android'e veya repoya konmaz, cevaplar loglanmaz.

Supabase e-posta kodu 6–10 hane destekler; 4 hane desteklenmez. Önerilen canlı
uzunluk 6 hanedir. Token kırpılmaz. Uygulama önceki 8 haneli e-postaları da kabul
eder; ana akış tek dokunuşta doğrulamadır. Gelen kutusuna teslimat alıcı
sağlayıcının kararıdır, garanti edilemez. Canlı SMTP/DNS kontrol edilmeden spam
sorununun giderildiği iddia edilmez.

Kaynaklar:
- https://supabase.com/docs/reference/api/v1-update-auth-service-config
- https://supabase.com/docs/guides/local-development/cli/config#auth.email.otp_length
