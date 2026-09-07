# Mage Cat Mascot V1

Bu dal, Son Harf için satın alınmış Mage Cat maskotunun güvenli ve kademeli entegrasyonu içindir.

## Hedef
- Orijinal asset korunur.
- Karanlık temada kaybolmaması için yalnızca düşük parlaklıktaki tüy bölgeleri koyu antrasit/gri seviyesine kaldırılır; mor kıyafet, altın detaylar, gözler ve değnek korunur.
- Maskot olay güdümlü çalışır; aynı animasyonu art arda spam etmez.
- V1'de yalnızca kontrollü tepkiler kullanılır: idle, doğru kelime/başarı, zafer, yenilgi.

## Yerleşim
- Ana menü: sağ alt güvenli alan, yaklaşık ekran yüksekliğinin %18-22'si.
- Maç: sağ kenar/güvenli üst alan, normalde %10-13; tepki anında en fazla %15-17.
- Maç sonu: sonuç kartı yanında %22-28.
- Günlük görev/streak/lig yükselmesi: kart yanında %16-20.
- Style/mağaza: önizleme alanında %24-30.

## Davranış seçici
Maskot seçimi yapay zekâ servisine bağımlı değildir. İlk sürümde deterministik, yerel ve düşük gecikmeli bir davranış seçici kullanılacaktır:
1. Oyun olayı sınıflandırılır (idle, correct-word, streak, victory, defeat, promotion vb.).
2. Olay için uygun animasyon havuzundan seçim yapılır.
3. Son oynatılan animasyon kısa dönem hafızada tutulur ve arka arkaya tekrar engellenir.
4. Cooldown ve ekranda görünme sıklığı uygulanır.
5. Kritik kullanıcı etkileşimleri sırasında maskot oyun alanını veya CTA'ları kapatmaz.

AI ancak ileride metin tonu/kişiselleştirme gibi düşük riskli katmanlarda değerlendirilecektir; temel hareket seçimi AI'ya bırakılmayacaktır. Bu sayede hareketler tutarlı, test edilebilir ve çevrimdışı çalışır.

## Kaynak asset
Kullanıcı tarafından sağlanan Mage Cat paketi model, texture ve animasyon kaynaklarını içerir. Android çalışma zamanı için uygun formata dönüştürme işlemi kaynak asset'i değiştirmeden yapılmalıdır.

## Güvenlik / regresyon
- Doğrudan main üzerinde değişiklik yapılmaz.
- Feature flag ile kapatılabilir.
- Build ve regresyon doğrulanmadan main'e alınmaz.
- Mevcut Son Harf oyun akışı, OYNA CTA'sı ve kuşatma/maç mekaniği etkilenmemelidir.
