# Canlı Supabase migration geçmişi

Bu klasör yalnız denetim ve devir (due diligence) içindir. Buradaki SQL dosyaları `supabase/migrations/` dizininin parçası değildir ve **çalıştırılmamalıdır**.

Canlı proje `bzdtftzdjtjoqhtcqtxb` üzerinde bazı migration'lar `apply_migration` ile uygulanırken üretim sürüm numarası, repodaki kaynak dosyanın yazım zamanından farklı oluşmuştur. Ayrıca bazı küçük production hotfix'leri daha sonra daha kapsamlı kaynak migration'larının içine birleştirilmiştir.

Kurallar:

1. Canlıda uygulanmış bir sürümü yalnız timestamp farklı diye tekrar `supabase/migrations/` altına kopyalama.
2. Bu klasördeki snapshot'ları production'a replay etme.
3. Yeni DDL değişiklikleri yeni, ileri tarihli ve tekil bir migration olarak yazılmalı; mevcut canlı geçmiş değiştirilmemeli.
4. Canlıda hangi sürümün ne zaman uygulandığı için `supabase_migrations.schema_migrations`; güncel istenen kaynak davranışı için `supabase/migrations/` ve uygulama regresyon testleri birlikte source-of-truth kabul edilir.
5. Tarihsel eşleme `docs/LIVE_SUPABASE_MIGRATION_LEDGER.md` dosyasında tutulur.
6. `Live statement MD5` header'ı bulunan snapshot'larda header'dan sonraki SQL gövdesi canlı `schema_migrations.statements` metninden alınır. Kontrat testi bu gövdenin MD5'ini production fingerprintiyle doğrular; böylece authored source ile live-applied statement sessizce karıştırılamaz.

Bu klasörde snapshot bulunması, ilgili eski davranışın bugün hâlâ doğru olduğu anlamına gelmez. Örneğin 20260906160407 sürümündeki `submit_word_v3` bot auto-continue davranışı daha sonra kaldırılmıştır; 20260904083323 sürümündeki tarihsel 81-cell Kelime Kuşatması alan skoru da güncel v8/v9 topoloji ve current-territory sözleşmesinin yerine geçmez. Güncel kaynak ve daha yeni canlı migration zinciri üstündür.
