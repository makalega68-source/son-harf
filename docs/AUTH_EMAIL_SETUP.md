# Son Harf Auth e-posta kurulumu

## Confirm signup

**Konu:** `Son Harf • E-posta Adresini Doğrula`

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
1. Gönderici adı **Son Harf** olmalı.
2. Mümkünse markalı bir alan adı ve özel SMTP kullanılmalı.
3. SPF, DKIM ve DMARC kayıtları doğrulanmalı.
4. E-posta tracking, doğrulama linklerini bozuyorsa kapatılmalı.
5. Gmail ve yaygın Android e-posta istemcilerinde buton + OTP akışı test edilmeli.

> Not: Supabase'in varsayılan SMTP'si üretim markalama/teslim edilebilirlik açısından yeterli görülmemelidir.
