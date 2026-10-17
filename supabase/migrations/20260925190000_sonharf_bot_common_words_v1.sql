-- Son Harf bot plays everyday words. It used to draw from a random slice of all 61k TDK
-- entries and favour hard ending letters, so it played rare words players never heard of
-- (muhzır, ihzarlı, karıkma). It now picks from a curated list of common words and only
-- falls back to the full dictionary when that letter's common words are used up.
-- Player words are still validated against the full dictionary.

begin;

create table if not exists public.sonharf_bot_common_words (
  language text not null,
  normalized_word text not null,
  primary key (language, normalized_word)
);
alter table public.sonharf_bot_common_words enable row level security;
revoke all on public.sonharf_bot_common_words from public, anon, authenticated;
grant select on public.sonharf_bot_common_words to service_role;

insert into public.sonharf_bot_common_words(language, normalized_word) values
('tr','abla'), ('tr','acele'), ('tr','acemi'), ('tr','acil'), ('tr','acı'), ('tr','ad'), ('tr','ada'), ('tr','adalet'),
('tr','adam'), ('tr','aday'), ('tr','adres'), ('tr','af'), ('tr','afiyet'), ('tr','ahize'), ('tr','ahlak'), ('tr','ahtapot'),
('tr','ahır'), ('tr','ahşap'), ('tr','aile'), ('tr','ajan'), ('tr','ajanda'), ('tr','akide'), ('tr','akraba'), ('tr','akrep'),
('tr','aktar'), ('tr','aktör'), ('tr','akvaryum'), ('tr','akıl'), ('tr','akıllı'), ('tr','akım'), ('tr','akın'), ('tr','akış'),
('tr','akşam'), ('tr','akşamüstü'), ('tr','alan'), ('tr','alarm'), ('tr','albüm'), ('tr','alet'), ('tr','alev'), ('tr','alfabe'),
('tr','alkış'), ('tr','alt'), ('tr','altın'), ('tr','alçı'), ('tr','alıcı'), ('tr','ama'), ('tr','amaç'), ('tr','ambalaj'),
('tr','ambar'), ('tr','amca'), ('tr','ameliyat'), ('tr','ampul'), ('tr','an'), ('tr','ana'), ('tr','anahtar'), ('tr','ananas'),
('tr','anaokulu'), ('tr','anayasa'), ('tr','ani'), ('tr','anket'), ('tr','anlam'), ('tr','anne'), ('tr','anten'), ('tr','antrenör'),
('tr','anı'), ('tr','aptal'), ('tr','ara'), ('tr','araba'), ('tr','arabacı'), ('tr','aralık'), ('tr','arayış'), ('tr','arena'),
('tr','argo'), ('tr','arka'), ('tr','arkadaş'), ('tr','armağan'), ('tr','armut'), ('tr','aroma'), ('tr','arpa'), ('tr','arsa'),
('tr','artı'), ('tr','arzu'), ('tr','arı'), ('tr','arıza'), ('tr','arşiv'), ('tr','asansör'), ('tr','asfalt'), ('tr','asker'),
('tr','aslan'), ('tr','atkı'), ('tr','atlas'), ('tr','atlet'), ('tr','avcı'), ('tr','avlu'), ('tr','avuç'), ('tr','ayak'),
('tr','ayakkabı'), ('tr','ayar'), ('tr','aydın'), ('tr','ayna'), ('tr','aynalı'), ('tr','ayran'), ('tr','ayrıntı'), ('tr','ayva'),
('tr','ayçiçeği'), ('tr','ayı'), ('tr','az'), ('tr','azim'), ('tr','azık'), ('tr','azınlık'), ('tr','aç'), ('tr','açlık'),
('tr','açık'), ('tr','ağaç'), ('tr','ağlamak'), ('tr','ağrı'), ('tr','ağır'), ('tr','ağız'), ('tr','aşk'), ('tr','aşçı'),
('tr','baba'), ('tr','bacak'), ('tr','badem'), ('tr','bagaj'), ('tr','bahar'), ('tr','baharat'), ('tr','bahçe'), ('tr','bahçeli'),
('tr','bahçıvan'), ('tr','bakan'), ('tr','bakkal'), ('tr','bakım'), ('tr','bakır'), ('tr','bakış'), ('tr','bal'), ('tr','balina'),
('tr','balkon'), ('tr','balon'), ('tr','balta'), ('tr','balık'), ('tr','bambu'), ('tr','bamya'), ('tr','bank'), ('tr','banka'),
('tr','banyo'), ('tr','baraj'), ('tr','bardak'), ('tr','barış'), ('tr','basamak'), ('tr','basit'), ('tr','baskı'), ('tr','baston'),
('tr','basın'), ('tr','bataklık'), ('tr','battaniye'), ('tr','batı'), ('tr','bavul'), ('tr','bayan'), ('tr','bayrak'), ('tr','bayram'),
('tr','baş'), ('tr','başarı'), ('tr','başkan'), ('tr','başlık'), ('tr','bebek'), ('tr','beceri'), ('tr','beden'), ('tr','bekar'),
('tr','bekçi'), ('tr','bel'), ('tr','belediye'), ('tr','belge'), ('tr','bellek'), ('tr','ben'), ('tr','benzin'), ('tr','beraber'),
('tr','berber'), ('tr','bereket'), ('tr','beyaz'), ('tr','beyazlık'), ('tr','beyin'), ('tr','bez'), ('tr','bezelye'), ('tr','beşik'),
('tr','biber'), ('tr','bilet'), ('tr','bilezik'), ('tr','bilgi'), ('tr','bilgisayar'), ('tr','bilim'), ('tr','bilmece'), ('tr','bina'),
('tr','bir'), ('tr','bira'), ('tr','birey'), ('tr','biri'), ('tr','birlik'), ('tr','bisiklet'), ('tr','bisküvi'), ('tr','bitki'),
('tr','biyoloji'), ('tr','bolluk'), ('tr','bomba'), ('tr','boncuk'), ('tr','bora'), ('tr','bordo'), ('tr','boru'), ('tr','borç'),
('tr','bostan'), ('tr','boy'), ('tr','boya'), ('tr','boyun'), ('tr','bozkır'), ('tr','bozuk'), ('tr','boğa'), ('tr','boğaz'),
('tr','boş'), ('tr','boşluk'), ('tr','buhar'), ('tr','bulaşık'), ('tr','bulmaca'), ('tr','bulut'), ('tr','buluşma'), ('tr','buruk'),
('tr','burun'), ('tr','burç'), ('tr','butik'), ('tr','buz'), ('tr','buzdolabı'), ('tr','buğday'), ('tr','böbrek'), ('tr','böcek'),
('tr','bölge'), ('tr','bölüm'), ('tr','börek'), ('tr','büfe'), ('tr','bükme'), ('tr','bülbül'), ('tr','büro'), ('tr','bütçe'),
('tr','büyü'), ('tr','büyük'), ('tr','büyükanne'), ('tr','büyükbaba'), ('tr','bıyık'), ('tr','bıçak'), ('tr','cadde'), ('tr','cadı'),
('tr','cahil'), ('tr','cam'), ('tr','cami'), ('tr','can'), ('tr','canavar'), ('tr','ceket'), ('tr','cenaze'), ('tr','cennet'),
('tr','cep'), ('tr','cephe'), ('tr','cesaret'), ('tr','cetvel'), ('tr','cevap'), ('tr','ceviz'), ('tr','ceylan'), ('tr','ceza'),
('tr','cilt'), ('tr','cimri'), ('tr','cins'), ('tr','cisim'), ('tr','ciğer'), ('tr','cuma'), ('tr','cumartesi'), ('tr','cümle'),
('tr','cüzdan'), ('tr','cıvata'), ('tr','dal'), ('tr','dalga'), ('tr','dalgıç'), ('tr','dam'), ('tr','damak'), ('tr','damat'),
('tr','damla'), ('tr','dana'), ('tr','dans'), ('tr','dantel'), ('tr','davet'), ('tr','davetiye'), ('tr','davul'), ('tr','dayı'),
('tr','dağ'), ('tr','dağcı'), ('tr','dede'), ('tr','defne'), ('tr','defter'), ('tr','deha'), ('tr','dehliz'), ('tr','demir'),
('tr','deneme'), ('tr','deniz'), ('tr','depo'), ('tr','dere'), ('tr','derece'), ('tr','dergi'), ('tr','deri'), ('tr','derman'),
('tr','ders'), ('tr','dershane'), ('tr','derviş'), ('tr','destan'), ('tr','destek'), ('tr','deve'), ('tr','devir'), ('tr','devlet'),
('tr','değer'), ('tr','değirmen'), ('tr','dikiş'), ('tr','dikkat'), ('tr','dil'), ('tr','dilek'), ('tr','dilenci'), ('tr','dilim'),
('tr','dinamo'), ('tr','dinleyici'), ('tr','dinç'), ('tr','direk'), ('tr','divan'), ('tr','diz'), ('tr','dizi'), ('tr','diş'),
('tr','dişçi'), ('tr','doktor'), ('tr','dolap'), ('tr','dolma'), ('tr','dolmuş'), ('tr','domates'), ('tr','domuz'), ('tr','donanım'),
('tr','dondurma'), ('tr','dost'), ('tr','doz'), ('tr','doğa'), ('tr','doğum'), ('tr','dudak'), ('tr','dul'), ('tr','duman'),
('tr','dumanlı'), ('tr','durak'), ('tr','durum'), ('tr','dut'), ('tr','duvar'), ('tr','duvarcı'), ('tr','duygu'), ('tr','duş'),
('tr','dönem'), ('tr','düdük'), ('tr','dükkan'), ('tr','dünya'), ('tr','dürbün'), ('tr','düz'), ('tr','düzen'), ('tr','düğme'),
('tr','düğüm'), ('tr','düğün'), ('tr','düş'), ('tr','düşman'), ('tr','düşünce'), ('tr','ebe'), ('tr','ebru'), ('tr','ecdat'),
('tr','ecel'), ('tr','ecza'), ('tr','eczane'), ('tr','efsane'), ('tr','egzoz'), ('tr','ekim'), ('tr','ekin'), ('tr','ekip'),
('tr','ekler'), ('tr','ekmek'), ('tr','ekonomi'), ('tr','ekran'), ('tr','ekşi'), ('tr','el'), ('tr','elbise'), ('tr','eldiven'),
('tr','elek'), ('tr','elektrik'), ('tr','elma'), ('tr','elmas'), ('tr','elçi'), ('tr','emek'), ('tr','emir'), ('tr','emlak'),
('tr','emniyet'), ('tr','emzik'), ('tr','endişe'), ('tr','enerji'), ('tr','engel'), ('tr','enginar'), ('tr','enlem'), ('tr','ense'),
('tr','erik'), ('tr','erişte'), ('tr','erkek'), ('tr','erken'), ('tr','ertesi'), ('tr','erzak'), ('tr','eser'), ('tr','esinti'),
('tr','esir'), ('tr','eski'), ('tr','eskici'), ('tr','esnaf'), ('tr','esneme'), ('tr','et'), ('tr','etek'), ('tr','etiket'),
('tr','etki'), ('tr','etkinlik'), ('tr','etli'), ('tr','ev'), ('tr','evcil'), ('tr','evlat'), ('tr','evlilik'), ('tr','evrak'),
('tr','evren'), ('tr','eylem'), ('tr','eylül'), ('tr','ezber'), ('tr','ezgi'), ('tr','eğitim'), ('tr','eğlence'), ('tr','eş'),
('tr','eşarp'), ('tr','eşek'), ('tr','eşya'), ('tr','fabrika'), ('tr','fakir'), ('tr','fal'), ('tr','fare'), ('tr','fark'),
('tr','fasulye'), ('tr','fatura'), ('tr','fayans'), ('tr','fazla'), ('tr','felaket'), ('tr','fen'), ('tr','fener'), ('tr','ferah'),
('tr','fes'), ('tr','fesleğen'), ('tr','fidan'), ('tr','fikir'), ('tr','fil'), ('tr','film'), ('tr','fincan'), ('tr','firma'),
('tr','fiyat'), ('tr','fiziksel'), ('tr','fiş'), ('tr','form'), ('tr','fotoğraf'), ('tr','futbol'), ('tr','fındık'), ('tr','fırsat'),
('tr','fırça'), ('tr','fırın'), ('tr','fıstık'), ('tr','gaga'), ('tr','garaj'), ('tr','garson'), ('tr','gaz'), ('tr','gazete'),
('tr','gazi'), ('tr','gazoz'), ('tr','gece'), ('tr','gelenek'), ('tr','gelin'), ('tr','gelincik'), ('tr','gelir'), ('tr','gemi'),
('tr','gemici'), ('tr','genelge'), ('tr','genç'), ('tr','gençlik'), ('tr','gerdanlık'), ('tr','gerçek'), ('tr','geyik'), ('tr','gezegen'),
('tr','gezi'), ('tr','gidişat'), ('tr','gidon'), ('tr','giriş'), ('tr','girişim'), ('tr','gitar'), ('tr','giysi'), ('tr','gizem'),
('tr','gişe'), ('tr','gocuk'), ('tr','gofret'), ('tr','gonca'), ('tr','gurur'), ('tr','göbek'), ('tr','gök'), ('tr','gökkuşağı'),
('tr','göktaşı'), ('tr','gökyüzü'), ('tr','göl'), ('tr','gölet'), ('tr','gölge'), ('tr','gömlek'), ('tr','gömü'), ('tr','gönül'),
('tr','gönüllü'), ('tr','görev'), ('tr','görgü'), ('tr','görücü'), ('tr','görüntü'), ('tr','görüş'), ('tr','gövde'), ('tr','göz'),
('tr','gözcü'), ('tr','göze'), ('tr','gözlük'), ('tr','gözyaşı'), ('tr','göç'), ('tr','göğüs'), ('tr','gül'), ('tr','gülle'),
('tr','gülümseme'), ('tr','gümüş'), ('tr','gün'), ('tr','günah'), ('tr','güneş'), ('tr','güneşli'), ('tr','güreş'), ('tr','gürgen'),
('tr','gürültü'), ('tr','güven'), ('tr','güvercin'), ('tr','güz'), ('tr','güzel'), ('tr','güç'), ('tr','gıda'), ('tr','haber'),
('tr','hacim'), ('tr','hademe'), ('tr','hafta'), ('tr','hafıza'), ('tr','hak'), ('tr','hakem'), ('tr','hakikat'), ('tr','hal'),
('tr','hala'), ('tr','halat'), ('tr','halk'), ('tr','halı'), ('tr','hamak'), ('tr','hamal'), ('tr','hamam'), ('tr','hamur'),
('tr','hane'), ('tr','hangar'), ('tr','hançer'), ('tr','hapis'), ('tr','haraç'), ('tr','hardal'), ('tr','harf'), ('tr','harita'),
('tr','harman'), ('tr','harç'), ('tr','hasret'), ('tr','hasta'), ('tr','hastalık'), ('tr','hastane'), ('tr','hat'), ('tr','hata'),
('tr','hattat'), ('tr','hatır'), ('tr','hatıra'), ('tr','hava'), ('tr','havai'), ('tr','havlu'), ('tr','havuz'), ('tr','havuç'),
('tr','hayal'), ('tr','hayat'), ('tr','haylaz'), ('tr','hayvan'), ('tr','hazine'), ('tr','hazırlık'), ('tr','haşlama'), ('tr','hece'),
('tr','hedef'), ('tr','hediye'), ('tr','hekim'), ('tr','helva'), ('tr','hemşire'), ('tr','hendek'), ('tr','hesap'), ('tr','hesaplı'),
('tr','hevesli'), ('tr','heykel'), ('tr','hikaye'), ('tr','hindi'), ('tr','his'), ('tr','hisse'), ('tr','hobi'), ('tr','hoca'),
('tr','hokka'), ('tr','horoz'), ('tr','hortum'), ('tr','hoşaf'), ('tr','hurda'), ('tr','hurma'), ('tr','huzur'), ('tr','hükümet'),
('tr','hüzün'), ('tr','hırka'), ('tr','hırsız'), ('tr','hız'), ('tr','hızar'), ('tr','hızlı'), ('tr','iade'), ('tr','iblis'),
('tr','ibrik'), ('tr','icat'), ('tr','idare'), ('tr','iddia'), ('tr','ifade'), ('tr','iftar'), ('tr','ihale'), ('tr','ihracat'),
('tr','ihtiyaç'), ('tr','ikindi'), ('tr','ikiz'), ('tr','iklim'), ('tr','ikram'), ('tr','ilaç'), ('tr','ilgi'), ('tr','ilginç'),
('tr','ilham'), ('tr','ilik'), ('tr','ilim'), ('tr','ilk'), ('tr','ilkbahar'), ('tr','ilçe'), ('tr','imam'), ('tr','imkan'),
('tr','imza'), ('tr','inci'), ('tr','incir'), ('tr','indirim'), ('tr','inek'), ('tr','inkar'), ('tr','insan'), ('tr','inziva'),
('tr','inşaat'), ('tr','ipek'), ('tr','iplik'), ('tr','iptal'), ('tr','irade'), ('tr','irmik'), ('tr','isim'), ('tr','iskele'),
('tr','iskelet'), ('tr','iskemle'), ('tr','ispinoz'), ('tr','istasyon'), ('tr','istek'), ('tr','istiridye'), ('tr','itfaiye'), ('tr','itibar'),
('tr','ivme'), ('tr','iyi'), ('tr','iyilik'), ('tr','iz'), ('tr','izci'), ('tr','izin'), ('tr','izleyici'), ('tr','iç'),
('tr','içecek'), ('tr','içerik'), ('tr','içki'), ('tr','iğde'), ('tr','iğne'), ('tr','iş'), ('tr','işaret'), ('tr','işlem'),
('tr','işçi'), ('tr','jaguar'), ('tr','jambon'), ('tr','jandarma'), ('tr','jeneratör'), ('tr','jeoloji'), ('tr','jest'), ('tr','jet'),
('tr','jeton'), ('tr','jilet'), ('tr','jimnastik'), ('tr','joker'), ('tr','judo'), ('tr','jöle'), ('tr','jübile'), ('tr','jüri'),
('tr','kabak'), ('tr','kablo'), ('tr','kabuk'), ('tr','kadeh'), ('tr','kader'), ('tr','kadife'), ('tr','kadın'), ('tr','kafa'),
('tr','kafes'), ('tr','kafile'), ('tr','kahraman'), ('tr','kahvaltı'), ('tr','kahve'), ('tr','kalabalık'), ('tr','kale'), ('tr','kalem'),
('tr','kalp'), ('tr','kalıp'), ('tr','kamyon'), ('tr','kanal'), ('tr','kanat'), ('tr','kanepe'), ('tr','kantin'), ('tr','kapak'),
('tr','kaplan'), ('tr','kapı'), ('tr','kar'), ('tr','karakol'), ('tr','karanlık'), ('tr','kardeş'), ('tr','kargo'), ('tr','karne'),
('tr','karpuz'), ('tr','kartal'), ('tr','karton'), ('tr','karınca'), ('tr','kasa'), ('tr','kasap'), ('tr','kavanoz'), ('tr','kavga'),
('tr','kavun'), ('tr','kaya'), ('tr','kaymak'), ('tr','kayık'), ('tr','kayısı'), ('tr','kayıt'), ('tr','kazak'), ('tr','kazan'),
('tr','kazı'), ('tr','kaçak'), ('tr','kağıt'), ('tr','kaşık'), ('tr','kedi'), ('tr','kek'), ('tr','kelebek'), ('tr','kelime'),
('tr','keman'), ('tr','kemer'), ('tr','kemik'), ('tr','kenar'), ('tr','kent'), ('tr','kepçe'), ('tr','kerpiç'), ('tr','kese'),
('tr','keski'), ('tr','kestane'), ('tr','keçi'), ('tr','keşif'), ('tr','kilim'), ('tr','kilise'), ('tr','kilit'), ('tr','kira'),
('tr','kiraz'), ('tr','kireç'), ('tr','kitap'), ('tr','kokulu'), ('tr','kolay'), ('tr','koltuk'), ('tr','kolye'), ('tr','kombi'),
('tr','komedi'), ('tr','komşu'), ('tr','konak'), ('tr','konser'), ('tr','kontrol'), ('tr','konuk'), ('tr','kopya'), ('tr','kova'),
('tr','kovan'), ('tr','koyun'), ('tr','kral'), ('tr','kristal'), ('tr','kulak'), ('tr','kule'), ('tr','kulübe'), ('tr','kum'),
('tr','kumaş'), ('tr','kumbara'), ('tr','kumru'), ('tr','kundura'), ('tr','kupa'), ('tr','kurabiye'), ('tr','kurbağa'), ('tr','kurşun'),
('tr','kutu'), ('tr','kuyu'), ('tr','kuzey'), ('tr','kuzu'), ('tr','kuş'), ('tr','kök'), ('tr','köpek'), ('tr','köprü'),
('tr','köy'), ('tr','köşe'), ('tr','külah'), ('tr','küp'), ('tr','küpe'), ('tr','küre'), ('tr','kürek'), ('tr','kürk'),
('tr','kütüphane'), ('tr','küvet'), ('tr','küçük'), ('tr','kıl'), ('tr','kılıç'), ('tr','kırmızı'), ('tr','kıyı'), ('tr','kız'),
('tr','kızak'), ('tr','kış'), ('tr','laf'), ('tr','lahana'), ('tr','lale'), ('tr','lamba'), ('tr','lastik'), ('tr','lavabo'),
('tr','lazım'), ('tr','leke'), ('tr','levha'), ('tr','leylek'), ('tr','lezzet'), ('tr','lezzetli'), ('tr','lider'), ('tr','liman'),
('tr','limon'), ('tr','lise'), ('tr','liste'), ('tr','lokanta'), ('tr','lokma'), ('tr','lokum'), ('tr','lüks'), ('tr','maaş'),
('tr','macun'), ('tr','madalya'), ('tr','madde'), ('tr','maden'), ('tr','mahalle'), ('tr','mahkeme'), ('tr','makarna'), ('tr','makas'),
('tr','makine'), ('tr','mal'), ('tr','manav'), ('tr','mandalina'), ('tr','mangal'), ('tr','manto'), ('tr','manzara'), ('tr','marangoz'),
('tr','mart'), ('tr','marul'), ('tr','masa'), ('tr','masal'), ('tr','maske'), ('tr','masraf'), ('tr','matbaa'), ('tr','mavi'),
('tr','maydanoz'), ('tr','maymun'), ('tr','mazot'), ('tr','meclis'), ('tr','mecmua'), ('tr','mektup'), ('tr','melek'), ('tr','meme'),
('tr','mendil'), ('tr','merak'), ('tr','merdiven'), ('tr','merkez'), ('tr','mermer'), ('tr','mesafe'), ('tr','mesaj'), ('tr','meslek'),
('tr','metin'), ('tr','metro'), ('tr','mevsim'), ('tr','meydan'), ('tr','meyhane'), ('tr','meyve'), ('tr','mezar'), ('tr','mezun'),
('tr','midye'), ('tr','mikrop'), ('tr','mikser'), ('tr','mimar'), ('tr','minder'), ('tr','mineral'), ('tr','miras'), ('tr','misafir'),
('tr','misket'), ('tr','mobilya'), ('tr','moda'), ('tr','motor'), ('tr','mum'), ('tr','mutfak'), ('tr','mutluluk'), ('tr','muz'),
('tr','mücevher'), ('tr','müdür'), ('tr','mühendis'), ('tr','mürekkep'), ('tr','müze'), ('tr','müzik'), ('tr','müşteri'), ('tr','mısır'),
('tr','nabız'), ('tr','nadir'), ('tr','nakit'), ('tr','nal'), ('tr','namaz'), ('tr','nane'), ('tr','nar'), ('tr','nargile'),
('tr','nasihat'), ('tr','nazik'), ('tr','nefes'), ('tr','nefret'), ('tr','nehir'), ('tr','nem'), ('tr','nesil'), ('tr','nesne'),
('tr','net'), ('tr','nezle'), ('tr','neşe'), ('tr','nimet'), ('tr','nine'), ('tr','nitelik'), ('tr','nişan'), ('tr','nohut'),
('tr','nokta'), ('tr','not'), ('tr','nota'), ('tr','numara'), ('tr','nöbet'), ('tr','nüans'), ('tr','nüfus'), ('tr','oba'),
('tr','obur'), ('tr','ocak'), ('tr','oda'), ('tr','odacı'), ('tr','odun'), ('tr','ofis'), ('tr','oklava'), ('tr','okul'),
('tr','okuma'), ('tr','okur'), ('tr','okyanus'), ('tr','olay'), ('tr','olta'), ('tr','onay'), ('tr','onur'), ('tr','orak'),
('tr','ordu'), ('tr','organ'), ('tr','orkestra'), ('tr','orman'), ('tr','orta'), ('tr','ot'), ('tr','otel'), ('tr','otlak'),
('tr','otobüs'), ('tr','otomobil'), ('tr','otopark'), ('tr','oturak'), ('tr','ova'), ('tr','oy'), ('tr','oyun'), ('tr','oyuncak'),
('tr','oyuncu'), ('tr','ozan'), ('tr','oğul'), ('tr','paket'), ('tr','palto'), ('tr','pamuk'), ('tr','pancar'), ('tr','pano'),
('tr','pantolon'), ('tr','papatya'), ('tr','para'), ('tr','park'), ('tr','parmak'), ('tr','parti'), ('tr','parça'), ('tr','pasaport'),
('tr','pasta'), ('tr','patates'), ('tr','patika'), ('tr','patlıcan'), ('tr','patron'), ('tr','pazar'), ('tr','pazartesi'), ('tr','paça'),
('tr','pehlivan'), ('tr','pekmez'), ('tr','pelerin'), ('tr','pencere'), ('tr','pense'), ('tr','perde'), ('tr','perşembe'), ('tr','peynir'),
('tr','peçete'), ('tr','pide'), ('tr','pil'), ('tr','pilav'), ('tr','pilot'), ('tr','pipo'), ('tr','pire'), ('tr','pirinç'),
('tr','pislik'), ('tr','pişmaniye'), ('tr','plaj'), ('tr','plaka'), ('tr','polis'), ('tr','pompa'), ('tr','porsuk'), ('tr','portakal'),
('tr','posta'), ('tr','poğaça'), ('tr','poşet'), ('tr','puan'), ('tr','pul'), ('tr','pusula'), ('tr','radar'), ('tr','radyo'),
('tr','raf'), ('tr','rahat'), ('tr','rahatlık'), ('tr','rakam'), ('tr','rakip'), ('tr','randevu'), ('tr','ranza'), ('tr','rapor'),
('tr','ray'), ('tr','rehber'), ('tr','rehin'), ('tr','reklam'), ('tr','rekor'), ('tr','rengi'), ('tr','renk'), ('tr','resim'),
('tr','ressam'), ('tr','reçel'), ('tr','reçete'), ('tr','rota'), ('tr','ruh'), ('tr','ruj'), ('tr','rulet'), ('tr','rulo'),
('tr','rütbe'), ('tr','rüya'), ('tr','rüzgar'), ('tr','rıhtım'), ('tr','saat'), ('tr','sabah'), ('tr','sabun'), ('tr','sabır'),
('tr','sahil'), ('tr','sahip'), ('tr','sahne'), ('tr','sakal'), ('tr','saksı'), ('tr','sakız'), ('tr','salata'), ('tr','salon'),
('tr','salça'), ('tr','salı'), ('tr','saman'), ('tr','samur'), ('tr','sanat'), ('tr','sandalye'), ('tr','sandık'), ('tr','sapan'),
('tr','saray'), ('tr','sargı'), ('tr','sarık'), ('tr','sarımsak'), ('tr','sayfa'), ('tr','sayı'), ('tr','saç'), ('tr','saçak'),
('tr','sağlık'), ('tr','sebze'), ('tr','seccade'), ('tr','sedir'), ('tr','sefer'), ('tr','seher'), ('tr','sehpa'), ('tr','sel'),
('tr','selam'), ('tr','semer'), ('tr','sendika'), ('tr','sepet'), ('tr','sergi'), ('tr','serin'), ('tr','ses'), ('tr','sevgi'),
('tr','sevinç'), ('tr','seyahat'), ('tr','seçim'), ('tr','sihir'), ('tr','silah'), ('tr','silgi'), ('tr','simit'), ('tr','sinek'),
('tr','sinema'), ('tr','sirke'), ('tr','sis'), ('tr','soba'), ('tr','sofra'), ('tr','sohbet'), ('tr','sokak'), ('tr','somun'),
('tr','sonbahar'), ('tr','soru'), ('tr','soyadı'), ('tr','soğan'), ('tr','soğuk'), ('tr','spor'), ('tr','su'), ('tr','sucuk'),
('tr','sulama'), ('tr','sulu'), ('tr','sunucu'), ('tr','sözlük'), ('tr','süpürge'), ('tr','sürahi'), ('tr','süre'), ('tr','sürü'),
('tr','süt'), ('tr','sütlaç'), ('tr','sıcak'), ('tr','sıfır'), ('tr','sıla'), ('tr','sınıf'), ('tr','sıra'), ('tr','sırt'),
('tr','tabak'), ('tr','tabela'), ('tr','tablo'), ('tr','tabure'), ('tr','tahta'), ('tr','takvim'), ('tr','takım'), ('tr','talep'),
('tr','tane'), ('tr','tanık'), ('tr','tarak'), ('tr','tarif'), ('tr','tarih'), ('tr','tarla'), ('tr','tartı'), ('tr','tas'),
('tr','tasarım'), ('tr','tatil'), ('tr','tatlı'), ('tr','tava'), ('tr','tavan'), ('tr','tavla'), ('tr','tavuk'), ('tr','tavşan'),
('tr','taze'), ('tr','taş'), ('tr','tebeşir'), ('tr','tebrik'), ('tr','tekerlek'), ('tr','tekne'), ('tr','tel'), ('tr','telefon'),
('tr','temel'), ('tr','temmuz'), ('tr','tencere'), ('tr','tepe'), ('tr','tepsi'), ('tr','terazi'), ('tr','tereyağı'), ('tr','terlik'),
('tr','tespih'), ('tr','teyze'), ('tr','tilki'), ('tr','tiyatro'), ('tr','tohum'), ('tr','top'), ('tr','toprak'), ('tr','topuk'),
('tr','torba'), ('tr','torun'), ('tr','tost'), ('tr','tren'), ('tr','turşu'), ('tr','tutkal'), ('tr','tuz'), ('tr','tuğla'),
('tr','tüccar'), ('tr','tüfek'), ('tr','tülbent'), ('tr','tünel'), ('tr','türbe'), ('tr','türkü'), ('tr','tüy'), ('tr','tırnak'),
('tr','tırtıl'), ('tr','ucuz'), ('tr','ufuk'), ('tr','umut'), ('tr','un'), ('tr','uyku'), ('tr','uzay'), ('tr','uçak'),
('tr','uçurtma'), ('tr','vakit'), ('tr','vali'), ('tr','valiz'), ('tr','vana'), ('tr','vapur'), ('tr','varlık'), ('tr','vasıta'),
('tr','vatan'), ('tr','vatandaş'), ('tr','vazife'), ('tr','vazo'), ('tr','veda'), ('tr','vergi'), ('tr','veri'), ('tr','verim'),
('tr','vezne'), ('tr','vida'), ('tr','villa'), ('tr','vinç'), ('tr','virgül'), ('tr','vitamin'), ('tr','vitrin'), ('tr','vişne'),
('tr','vücut'), ('tr','yabancı'), ('tr','yaka'), ('tr','yakın'), ('tr','yakıt'), ('tr','yalan'), ('tr','yalı'), ('tr','yama'),
('tr','yamaç'), ('tr','yan'), ('tr','yanak'), ('tr','yangın'), ('tr','yanıt'), ('tr','yaprak'), ('tr','yara'), ('tr','yarar'),
('tr','yardım'), ('tr','yarım'), ('tr','yarış'), ('tr','yastık'), ('tr','yatak'), ('tr','yatırım'), ('tr','yavru'), ('tr','yay'),
('tr','yayla'), ('tr','yayın'), ('tr','yaz'), ('tr','yazar'), ('tr','yazlık'), ('tr','yazı'), ('tr','yağ'), ('tr','yağmur'),
('tr','yaş'), ('tr','yaşam'), ('tr','yedek'), ('tr','yelek'), ('tr','yelken'), ('tr','yemek'), ('tr','yemiş'), ('tr','yengeç'),
('tr','yeni'), ('tr','yer'), ('tr','yetenek'), ('tr','yeşil'), ('tr','yiğit'), ('tr','yol'), ('tr','yolcu'), ('tr','yorgan'),
('tr','yorum'), ('tr','yoğurt'), ('tr','yufka'), ('tr','yumurta'), ('tr','yuva'), ('tr','yön'), ('tr','yük'), ('tr','yükseklik'),
('tr','yüz'), ('tr','yüzme'), ('tr','yüzük'), ('tr','yıl'), ('tr','yılan'), ('tr','yıldız'), ('tr','yığın'), ('tr','zafer'),
('tr','zahmet'), ('tr','zam'), ('tr','zaman'), ('tr','zambak'), ('tr','zarar'), ('tr','zarf'), ('tr','zayıf'), ('tr','zeka'),
('tr','zemin'), ('tr','zengin'), ('tr','zevk'), ('tr','zeytin'), ('tr','zihin'), ('tr','zil'), ('tr','zincir'), ('tr','zindan'),
('tr','zirve'), ('tr','zor'), ('tr','zorluk'), ('tr','zurna'), ('tr','zümrüt'), ('tr','zürafa'), ('tr','çadır'), ('tr','çakı'),
('tr','çakıl'), ('tr','çalışma'), ('tr','çam'), ('tr','çamaşır'), ('tr','çamur'), ('tr','çanta'), ('tr','çapa'), ('tr','çare'),
('tr','çarşaf'), ('tr','çarşı'), ('tr','çatal'), ('tr','çatlak'), ('tr','çatı'), ('tr','çay'), ('tr','çaydanlık'), ('tr','çağ'),
('tr','çek'), ('tr','çekirdek'), ('tr','çekiç'), ('tr','çekmece'), ('tr','çelik'), ('tr','çember'), ('tr','çene'), ('tr','çengel'),
('tr','çerez'), ('tr','çerçeve'), ('tr','çerçi'), ('tr','çevre'), ('tr','çeşit'), ('tr','çeşme'), ('tr','çift'), ('tr','çiftlik'),
('tr','çiftçi'), ('tr','çikolata'), ('tr','çilek'), ('tr','çim'), ('tr','çimen'), ('tr','çinko'), ('tr','çirkin'), ('tr','çizelge'),
('tr','çizgi'), ('tr','çizim'), ('tr','çizme'), ('tr','çiçek'), ('tr','çoban'), ('tr','çocuk'), ('tr','çok'), ('tr','çorap'),
('tr','çorba'), ('tr','çukur'), ('tr','çuval'), ('tr','çöl'), ('tr','çöp'), ('tr','çöpçü'), ('tr','çözüm'), ('tr','çürük'),
('tr','çıban'), ('tr','çınar'), ('tr','çıra'), ('tr','çırak'), ('tr','çığ'), ('tr','ödev'), ('tr','ödül'), ('tr','ödünç'),
('tr','öfke'), ('tr','öksürük'), ('tr','ölçek'), ('tr','ölçü'), ('tr','ömür'), ('tr','ön'), ('tr','önder'), ('tr','önem'),
('tr','öneri'), ('tr','önlem'), ('tr','önlük'), ('tr','öpücük'), ('tr','ördek'), ('tr','örgü'), ('tr','örnek'), ('tr','örtü'),
('tr','örümcek'), ('tr','öykü'), ('tr','öz'), ('tr','özel'), ('tr','özet'), ('tr','özgürlük'), ('tr','özür'), ('tr','öğle'),
('tr','öğrenci'), ('tr','öğretmen'), ('tr','öğün'), ('tr','öğüt'), ('tr','ücret'), ('tr','ülke'), ('tr','ülkü'), ('tr','ümit'),
('tr','ün'), ('tr','üniforma'), ('tr','üniversite'), ('tr','ünlü'), ('tr','üretim'), ('tr','ürün'), ('tr','üslup'), ('tr','üst'),
('tr','üstat'), ('tr','ütü'), ('tr','üye'), ('tr','üyelik'), ('tr','üzüm'), ('tr','üzüntü'), ('tr','üçgen'), ('tr','ıhlamur'),
('tr','ılgın'), ('tr','ılık'), ('tr','ılıman'), ('tr','ırgat'), ('tr','ırk'), ('tr','ırmak'), ('tr','ıslak'), ('tr','ıslık'),
('tr','ıspanak'), ('tr','ısrar'), ('tr','ıssız'), ('tr','ıssızlık'), ('tr','ıstakoz'), ('tr','ısı'), ('tr','ısırgan'), ('tr','ısıtıcı'),
('tr','ızdırap'), ('tr','ızgara'), ('tr','ışık'), ('tr','ışıklı'), ('tr','şahin'), ('tr','şair'), ('tr','şaka'), ('tr','şal'),
('tr','şalgam'), ('tr','şalvar'), ('tr','şamdan'), ('tr','şampuan'), ('tr','şans'), ('tr','şapka'), ('tr','şarap'), ('tr','şarkı'),
('tr','şaşkın'), ('tr','şebeke'), ('tr','şeftali'), ('tr','şehir'), ('tr','şehit'), ('tr','şeker'), ('tr','şelale'), ('tr','şemsiye'),
('tr','şerbet'), ('tr','şezlong'), ('tr','şiir'), ('tr','şimşek'), ('tr','şimşir'), ('tr','şirket'), ('tr','şişe'), ('tr','şişman'),
('tr','şoför'), ('tr','şort'), ('tr','şube'), ('tr','şurup'), ('tr','şölen')
on conflict do nothing;

create or replace function public.bot_take_turn_normal_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  r public.game_rooms;
  previous_word text;
  expected text;
  v_letter text;
  v_letter_count integer := 0;
  v_max_count integer := 1;
  v_offset integer := 0;
  v_common_left integer := 0;
  v_choice_id public.dictionary_words.id%type;
  chosen public.dictionary_words;
  streak_value integer;
  add_points integer := 3;
  next_round integer;
  difficulty text := 'normal';
  requested text := 'normal';
  rating_value integer := 1000;
  total_value integer := 0;
  wins_value integer := 0;
  v_lead integer := 0;
  v_lead_words numeric := 0;
  v_skill numeric := .56;
  v_miss numeric := 0;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null or not r.is_bot then raise exception 'not_bot_room'; end if;
  if r.game_mode = 'expert' then raise exception 'expert_bot_use_expert_turn'; end if;
  if auth.uid() <> r.host_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing', 'final', 'sudden_death') or not r.bot_turn then return r; end if;

  if r.status <> 'sudden_death' and r.guest_round_words >= 10 then
    update public.game_rooms
    set current_player_id = host_id,
        bot_turn = false,
        turn_deadline = public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id = r.id
    returning * into r;
    return r;
  end if;

  select coalesce(p.rating, 1000), coalesce(p.total_matches, 0), coalesce(p.wins, 0), coalesce(p.bot_difficulty, 'normal')
  into rating_value, total_value, wins_value, requested
  from public.profiles p
  where p.id = r.host_id;

  difficulty := public.sonharf_adaptive_bot_tier_v1(rating_value, total_value, wins_value, requested);

  -- Skill: tier base, gentler for beginners, rubber-banded on the round and match score.
  v_lead := coalesce(r.guest_round_score, 0) - coalesce(r.host_round_score, 0);
  v_skill := case when difficulty = 'expert' then .82 else .56 end;
  if total_value < 6 then v_skill := v_skill - .12; end if;
  -- A correct word is worth about 10 points, so the lead is measured in words.
  v_lead_words := v_lead / 10.0;
  v_skill := v_skill
    - greatest(-3, least(3, v_lead_words)) * .09
    - (coalesce(r.guest_rounds, 0) - coalesce(r.host_rounds, 0)) * .06;
  v_skill := greatest(.12, least(.95, v_skill));

  -- A human-like slip: never in sudden death, never when the player has no turns left this round.
  if r.status <> 'sudden_death' and r.host_round_words < 10 then
    v_miss := .015 + (1 - v_skill) * .07 + case when v_lead_words >= 2 then .06 else 0 end;
    if random() < v_miss then
      update public.game_rooms
      set guest_score = guest_score - 1,
          guest_round_score = guest_round_score - 1,
          guest_streak = 0,
          current_player_id = host_id,
          bot_turn = false,
          turn_deadline = public.sonharf_turn_deadline_for_round_v1(r.round_no),
          last_event = 'bot_missed',
          last_event_player_id = null
      where id = r.id
      returning * into r;
      return r;
    end if;
  end if;

  select normalized_word into previous_word
  from public.game_words
  where room_id = r.id
  order by id desc
  limit 1;
  expected := case when previous_word is null then null else right(previous_word, 1) end;

  -- The opening word of a round starts from a random, well-populated letter.
  v_letter := expected;
  if v_letter is null then
    select o.letter into v_letter
    from public.sonharf_letter_openings o
    where o.language = r.language and o.word_count >= 800
    order by random()
    limit 1;
  end if;
  -- Always a concrete letter, so the prefix index is used even with a cached generic plan.
  v_letter := coalesce(v_letter, 'a');

  select coalesce(max(o.word_count), 1) into v_max_count
  from public.sonharf_letter_openings o
  where o.language = r.language;
  select coalesce(max(o.word_count), 0) into v_letter_count
  from public.sonharf_letter_openings o
  where o.language = r.language and o.letter = v_letter;
  -- Everyday words first: count the common words still unplayed for this letter.
  select count(*) into v_common_left
  from public.sonharf_bot_common_words c
  join public.dictionary_words d on d.language = c.language and d.normalized_word = c.normalized_word
  where c.language = r.language
    and left(c.normalized_word, 1) = v_letter
    and d.active and d.game_allowed and not d.is_abbreviation and not d.is_proper_noun
    and public.sonharf_bot_word_allowed(d.language, d.normalized_word)
    and not exists (
      select 1 from public.game_words w
      where w.room_id = r.id and w.normalized_word = d.normalized_word
    );

  v_offset := floor(random() * greatest(0, least(v_letter_count, 4000) - 220))::integer;

  -- Take a cheap index slice first, then run the costlier word filters only on that slice.
  -- The bot plays everyday words; the full dictionary is only a fallback for a letter whose
  -- common words are used up (players are still judged against the full dictionary).
  with slice as materialized (
    (
      select d.id, d.language, d.normalized_word
      from public.sonharf_bot_common_words c
      join public.dictionary_words d on d.language = c.language and d.normalized_word = c.normalized_word
      where v_common_left > 0
        and c.language = r.language
        and left(c.normalized_word, 1) = v_letter
        and d.active
        and d.game_allowed
        and not d.is_abbreviation
        and not d.is_proper_noun
      order by random()
      limit 220
    )
    union all
    (
      select d.id, d.language, d.normalized_word
      from public.dictionary_words d
      where v_common_left = 0
        and d.language = r.language
        and d.active
        and d.game_allowed
        and not d.is_abbreviation
        and not d.is_proper_noun
        and left(d.normalized_word, 1) = v_letter
      order by d.normalized_word
      offset v_offset limit 220
    )
  ),
  pool as (
    select s.id, s.normalized_word
    from slice s
    where public.sonharf_bot_word_allowed(s.language, s.normalized_word)
      and not exists (
        select 1 from public.game_words w
        where w.room_id = r.id and w.normalized_word = s.normalized_word
      )
  ),
  scored as (
    select p.id,
      -- How hard the next player's letter is: fewer words starting with it is better.
      1 - ln(1 + coalesce(o.word_count, 0)) / ln(1 + greatest(v_max_count, 1)) as trap,
      -- The long-word bonus the server awards (6+, 8+, 10+ letters).
      -- The normal bot plays everyday-length words like a person; the expert reaches for bonuses.
      case
        when difficulty = 'expert' then
          case
            when char_length(p.normalized_word) >= 10 then 1.0
            when char_length(p.normalized_word) >= 8 then .8
            when char_length(p.normalized_word) >= 6 then .55
            when char_length(p.normalized_word) >= 4 then .25
            else .05
          end
        else
          case
            when char_length(p.normalized_word) between 5 and 7 then .7
            when char_length(p.normalized_word) in (4, 8) then .45
            when char_length(p.normalized_word) >= 9 then .2
            else .1
          end
      end as length_value
    from pool p
    left join public.sonharf_letter_openings o
      on o.language = r.language and o.letter = right(p.normalized_word, 1)
  )
  select s.id into v_choice_id
  from scored s
  order by v_skill * (.55 * s.trap + .45 * s.length_value)
    + (1 - v_skill) * .9 * random()
    + .12 * random() desc
  limit 1;

  if v_choice_id is not null then
    select * into chosen from public.dictionary_words where id = v_choice_id;
  end if;

  if chosen.id is null then
    select d.* into chosen
    from public.dictionary_words d
    where d.language = r.language
      and d.active
      and d.game_allowed
      and not d.is_abbreviation
      and not d.is_proper_noun
      and (expected is null or left(d.normalized_word, 1) = expected)
      and public.sonharf_bot_word_allowed(d.language, d.normalized_word)
      and not exists (
        select 1 from public.game_words w
        where w.room_id = r.id and w.normalized_word = d.normalized_word
      )
    order by d.normalized_word
    limit 1;
  end if;

  if chosen.id is null then
    return public.sonharf_finish_room(r.id, r.host_id, false, 'bot_no_word');
  end if;

  insert into public.game_words(room_id, player_id, word, normalized_word, is_bot)
  values (r.id, null, chosen.word, chosen.normalized_word, true);

  if r.status = 'sudden_death' then
    return public.sonharf_finish_room(r.id, null, true, 'sudden_death_word');
  end if;

  streak_value := r.guest_streak + 1;
  if streak_value % 5 = 0 then add_points := 6; end if;

  update public.game_rooms
  set guest_score = guest_score + add_points,
      guest_round_score = guest_round_score + add_points,
      guest_streak = streak_value,
      guest_round_words = least(10, guest_round_words + 1),
      valid_word_count = valid_word_count + 1,
      round_word_count = round_word_count + 1,
      last_event = case when streak_value % 5 = 0 then 'streak_bonus' else 'valid_word' end,
      last_event_player_id = null
  where id = r.id
  returning * into r;

  if r.host_round_words >= 10 and r.guest_round_words >= 10 then
    update public.profiles
    set total_rounds = total_rounds + 1,
        rounds_won = rounds_won + case when r.host_round_score > r.guest_round_score then 1 else 0 end
    where id = r.host_id;

    update public.game_rooms
    set host_rounds = host_rounds + case when host_round_score > guest_round_score then 1 else 0 end,
        guest_rounds = guest_rounds + case when guest_round_score > host_round_score then 1 else 0 end
    where id = r.id
    returning * into r;

    if r.round_no >= 3 then
      if r.host_rounds > r.guest_rounds then
        return public.sonharf_finish_room(r.id, r.host_id, false, 'match_finished');
      elsif r.guest_rounds > r.host_rounds then
        return public.sonharf_finish_room(r.id, null, true, 'match_finished');
      else
        update public.game_rooms
        set status = 'sudden_death',
            round_word_count = 0,
            host_round_words = 0,
            guest_round_words = 0,
            host_round_score = 0,
            guest_round_score = 0,
            current_player_id = host_id,
            bot_turn = false,
            turn_deadline = public.sonharf_turn_deadline_for_round_v1(3),
            last_event = 'sudden_death_started'
        where id = r.id
        returning * into r;
        return r;
      end if;
    end if;

    next_round := r.round_no + 1;
    update public.game_rooms
    set round_no = next_round,
        round_word_count = 0,
        host_round_words = 0,
        guest_round_words = 0,
        host_round_score = 0,
        guest_round_score = 0,
        host_streak = 0,
        guest_streak = 0,
        current_player_id = case when next_round % 2 = 1 then host_id else null end,
        bot_turn = (next_round % 2 = 0),
        turn_deadline = case
          when next_round % 2 = 1 then public.sonharf_turn_deadline_for_round_v1(next_round)
          else null
        end,
        last_event = 'round_started'
    where id = r.id
    returning * into r;
    return r;
  end if;

  if r.host_round_words >= 10 then
    update public.game_rooms
    set current_player_id = null,
        bot_turn = true,
        turn_deadline = null
    where id = r.id
    returning * into r;
  else
    update public.game_rooms
    set current_player_id = host_id,
        bot_turn = false,
        turn_deadline = public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id = r.id
    returning * into r;
  end if;

  return r;
end
$$;

-- Only reachable through public.bot_take_turn (which checks the caller), as before.
revoke all on function public.bot_take_turn_normal_v1(uuid) from public, anon, authenticated;
grant execute on function public.bot_take_turn_normal_v1(uuid) to service_role;

commit;
