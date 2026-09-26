package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Plays whole Kelime Atölyesi rounds on real dictionary words, in Turkish and English. */
class KelimeAtolyesiEngineTest {
    private fun AtelierState.lay(word: String): AtelierState? {
        var state = this
        val taken = mutableSetOf<Long>()
        for (ch in word) {
            val tile = state.pool.firstOrNull { it.letter == ch && it.id !in taken } ?: return null
            taken += tile.id
            state = state.pick(tile.id)
        }
        return state
    }

    private fun playRounds(language: String, dictionary: Set<String>) {
        val engine = KelimeAtolyesiEngine(dictionary, language, Random(5))
        assertTrue(engine.playable)
        repeat(40) {
            var state = engine.newRound()
            assertEquals(7, state.pool.size)
            assertEquals(3, state.tasks.size)
            assertTrue("new round tasks solvable", engine.solvable(state.pool.map { it.letter }, state.tasks, emptySet()))

            // Pick, a second tap on a used tile does nothing, undo from the slot, clear.
            val first = state.pool[0].id
            assertEquals(1, state.pick(first).pick(first).picked.size)
            assertTrue(state.pick(first).unpickAt(0).picked.isEmpty())
            assertTrue(state.pick(first).pick(state.pool[1].id).clearPicks().picked.isEmpty())
            assertEquals(AtelierReject.TOO_SHORT, engine.submit(state.pick(first)).reject)

            // An invalid word is rejected and the letters stay in the slot.
            var gibberish = state
            state.pool.take(3).forEach { gibberish = gibberish.pick(it.id) }
            if (!engine.isWord(gibberish.word)) {
                val rejected = engine.submit(gibberish)
                assertEquals(AtelierReject.NOT_IN_DICTIONARY, rejected.reject)
                assertEquals(gibberish, rejected.state)
            }

            var guard = 0
            while (!state.allTasksDone && guard++ < 12) {
                val open = state.tasks.filter { !it.done }
                val options = engine.formable(state.pool.map { it.letter }, state.words.toSet())
                val word = options.firstOrNull { w -> open.any { it.matches(w) } } ?: options.first()
                val laid = state.lay(word)!!
                assertEquals(word, laid.word)
                val result = engine.submit(laid)
                assertNull(result.reject)
                assertEquals(word.length * KelimeAtolyesiEngine.WORD_LETTER_POINTS + result.completed.size * KelimeAtolyesiEngine.TASK_POINTS, result.gained)
                assertEquals(state.score + result.gained, result.state.score)
                assertEquals(7, result.state.pool.size)
                assertTrue(result.state.picked.isEmpty())
                assertTrue("remaining tasks stay solvable after the refill",
                    engine.solvable(result.state.pool.map { it.letter }, result.state.tasks, result.state.words.toSet()))
                state = result.state
                // The same word never scores twice in a round.
                state.lay(word)?.let { assertEquals(AtelierReject.ALREADY_USED, engine.submit(it).reject) }
            }
            assertTrue("all three tasks completed", state.allTasksDone)
            val finished = engine.finish(state, 12)
            assertTrue(finished.over)
            assertEquals(state.score + 12 * KelimeAtolyesiEngine.TIME_BONUS_PER_SECOND, finished.score)
            assertEquals(AtelierReject.ROUND_OVER, engine.submit(finished).reject)

            // Time running out ends the round with no bonus.
            val timedOut = engine.finish(engine.newRound(), 0)
            assertTrue(timedOut.over)
            assertEquals(0, timedOut.score)
        }
    }

    @Test fun turkishRoundsArePlayableEndToEnd() = playRounds("tr", TR_WORDS)

    @Test fun englishRoundsArePlayableEndToEnd() = playRounds("en", EN_WORDS)

    @Test fun oneWordCompletesEveryTaskItFits() {
        val engine = KelimeAtolyesiEngine(TR_WORDS, "tr", Random(1))
        val state = AtelierState(
            pool = "kalemsu".mapIndexed { i, c -> AtelierTile(i + 1L, c) },
            tasks = listOf(
                AtelierTask(AtelierTaskKind.LENGTH, length = 5),
                AtelierTask(AtelierTaskKind.LETTER, letter = 'k'),
                AtelierTask(AtelierTaskKind.LENGTH, length = 3),
            ),
            nextTileId = 8,
        )
        val result = engine.submit(state.lay("kalem")!!)
        assertEquals(listOf(0, 1), result.completed)
        assertEquals(50 + 2 * KelimeAtolyesiEngine.TASK_POINTS, result.gained)
        assertFalse(result.state.tasks[2].done)
    }

    @Test fun turkishLettersKeepTheirOwnCase() {
        assertEquals("İ", KelimeAtolyesiEngine.display('i', "tr"))
        assertEquals("I", KelimeAtolyesiEngine.display('ı', "tr"))
        assertEquals("ĞÜŞÖÇ", KelimeAtolyesiEngine.display("ğüşöç", "tr"))
        assertEquals("I", KelimeAtolyesiEngine.display('i', "en"))
        val tr = KelimeAtolyesiEngine(TR_WORDS, "tr", Random(3)).newRound()
        assertTrue(tr.pool.all { it.letter in KelimeAtolyesiEngine.TR_ALPHABET })
        val en = KelimeAtolyesiEngine(EN_WORDS, "en", Random(3)).newRound()
        assertTrue(en.pool.all { it.letter in KelimeAtolyesiEngine.EN_ALPHABET })
    }

    private companion object {
        val TR_WORDS: Set<String> = (
            "abla acele acemi acil acı ada adalet adam aday adres afiyet ahize ahlak ahtapot ahır ahşap " +
            "aile ajan ajanda akide akraba akrep aktar aktör akıl akıllı akım akın akış akşam alan alarm " +
            "albüm alet alev alfabe alkış alt altın alçı alıcı ama amaç ambalaj ambar amca ampul ana " +
            "anahtar ananas anayasa ani anket anlam anne anten anı aptal ara araba arabacı aralık arayış arena " +
            "argo arka arkadaş armağan armut aroma arpa arsa artı arzu arı arıza arşiv asansör asfalt asker " +
            "aslan atkı atlas atlet avcı avlu avuç ayak ayar aydın ayna aynalı ayran ayrıntı ayva ayı " +
            "azim azık azınlık açlık açık ağaç ağlamak ağrı ağır ağız aşk aşçı baba bacak badem bagaj " +
            "bahar baharat bahçe bahçeli bakan bakkal bakım bakır bakış bal balina balkon balon balta balık bambu " +
            "bamya bank banka banyo baraj bardak barış basamak basit baskı baston basın batı bavul bayan bayrak " +
            "bayram baş başarı başkan başlık bebek beceri beden bekar bekçi bel belge bellek ben benzin beraber " +
            "berber bereket beyaz beyin bez bezelye beşik biber bilet bilezik bilgi bilim bilmece bina bir bira " +
            "birey biri birlik bisküvi bitki bolluk bomba boncuk bora bordo boru borç bostan boy boya boyun " +
            "bozkır bozuk boğa boğaz boş boşluk buhar bulaşık bulmaca bulut buluşma buruk burun burç butik buz " +
            "buğday böbrek böcek bölge bölüm börek büfe bükme bülbül büro bütçe büyü büyük bıyık bıçak cadde " +
            "cadı cahil cam cami can canavar ceket cenaze cennet cep cephe cesaret cetvel cevap ceviz ceylan " +
            "ceza cilt cimri cins cisim ciğer cuma cümle cüzdan cıvata dal dalga dalgıç dam damak damat " +
            "damla dana dans dantel davet davul dayı dağ dağcı dede defne defter deha dehliz demir deneme " +
            "deniz depo dere derece dergi deri derman ders derviş destan destek deve devir devlet değer dikiş " +
            "dikkat dil dilek dilenci dilim dinamo dinç direk divan diz dizi diş dişçi doktor dolap dolma " +
            "dolmuş domates domuz donanım dost doz doğa doğum dudak dul duman dumanlı durak durum dut duvar " +
            "duvarcı duygu duş dönem düdük dükkan dünya dürbün düz düzen düğme düğüm düğün düş düşman düşünce " +
            "ebe ebru ecdat ecel ecza eczane efsane egzoz ekim ekin ekip ekler ekmek ekonomi ekran ekşi " +
            "elbise eldiven elek elma elmas elçi emek emir emlak emniyet emzik endişe enerji engel enginar enlem " +
            "ense erik erişte erkek erken ertesi erzak eser esinti esir eski eskici esnaf esneme etek etiket " +
            "etki etli evcil evlat evlilik evrak evren eylem eylül ezber ezgi eğitim eğlence eşarp eşek eşya " +
            "fabrika fakir fal fare fark fasulye fatura fayans fazla felaket fen fener ferah fes fidan fikir " +
            "fil film fincan firma fiyat fiş form futbol fındık fırsat fırça fırın fıstık gaga garaj garson " +
            "gaz gazete gazi gazoz gece gelenek gelin gelir gemi gemici genelge genç gençlik gerçek geyik gezegen " +
            "gezi gidişat gidon giriş girişim gitar giysi gizem gişe gocuk gofret gonca gurur göbek gök gökyüzü " +
            "göl gölet gölge gömlek gömü gönül gönüllü görev görgü görücü görüntü görüş gövde göz gözcü göze " +
            "gözlük gözyaşı göç göğüs gül gülle gümüş gün günah güneş güneşli güreş gürgen gürültü güven güz " +
            "güzel güç gıda haber hacim hademe hafta hafıza hak hakem hakikat hal hala halat halk halı " +
            "hamak hamal hamam hamur hane hangar hançer hapis haraç hardal harf harita harman harç hasret hasta " +
            "hastane hat hata hattat hatır hatıra hava havai havlu havuz havuç hayal hayat haylaz hayvan hazine " +
            "haşlama hece hedef hediye hekim helva hemşire hendek hesap hesaplı hevesli heykel hikaye hindi his hisse " +
            "hobi hoca hokka horoz hortum hoşaf hurda hurma huzur hüzün hırka hırsız hız hızar hızlı iade " +
            "iblis ibrik icat idare iddia ifade iftar ihale ihracat ihtiyaç ikindi ikiz iklim ikram ilaç ilgi " +
            "ilginç ilham ilik ilim ilk ilçe imam imkan imza inci incir indirim inek inkar insan inziva " +
            "inşaat ipek iplik iptal irade irmik isim iskele iskelet iskemle ispinoz istek itfaiye itibar ivme iyi " +
            "iyilik izci izin içecek içerik içki iğde iğne işaret işlem işçi jaguar jambon jeoloji jest jet " +
            "jeton jilet joker judo jöle jübile jüri kabak kablo kabuk kadeh kader kadife kadın kafa kafes " +
            "kafile kahve kale kalem kalp kalıp kamyon kanal kanat kanepe kantin kapak kaplan kapı kar karakol " +
            "kardeş kargo karne karpuz kartal karton karınca kasa kasap kavanoz kavga kavun kaya kaymak kayık kayısı " +
            "kayıt kazak kazan kazı kaçak kağıt kaşık kedi kek kelebek kelime keman kemer kemik kenar kent " +
            "kepçe kerpiç kese keski kestane keçi keşif kilim kilise kilit kira kiraz kireç kitap kokulu kolay " +
            "koltuk kolye kombi komedi komşu konak konser kontrol konuk kopya kova kovan koyun kral kristal kulak " +
            "kule kulübe kum kumaş kumbara kumru kundura kupa kurbağa kurşun kutu kuyu kuzey kuzu kuş kök " +
            "köpek köprü köy köşe külah küp küpe küre kürek kürk küvet küçük kıl kılıç kırmızı kıyı " +
            "kız kızak kış laf lahana lale lamba lastik lavabo lazım leke levha leylek lezzet lider liman " +
            "limon lise liste lokanta lokma lokum lüks maaş macun madalya madde maden mahalle mahkeme makarna makas " +
            "makine mal manav mangal manto manzara mart marul masa masal maske masraf matbaa mavi maymun mazot " +
            "meclis mecmua mektup melek meme mendil merak merkez mermer mesafe mesaj meslek metin metro mevsim meydan " +
            "meyhane meyve mezar mezun midye mikrop mikser mimar minder mineral miras misafir misket mobilya moda motor " +
            "mum mutfak muz müdür müze müzik müşteri mısır nabız nadir nakit nal namaz nane nar nargile " +
            "nasihat nazik nefes nefret nehir nem nesil nesne net nezle neşe nimet nine nitelik nişan nohut " +
            "nokta not nota numara nöbet nüans nüfus oba obur ocak oda odacı odun ofis oklava okul " +
            "okuma okur okyanus olay olta onay onur orak ordu organ orman orta otel otlak otobüs otopark " +
            "oturak ova oyun oyuncak oyuncu ozan oğul paket palto pamuk pancar pano papatya para park parmak " +
            "parti parça pasta patates patika patron pazar paça pekmez pelerin pencere pense perde peynir peçete pide " +
            "pil pilav pilot pipo pire pirinç pislik plaj plaka polis pompa porsuk posta poğaça poşet puan " +
            "pul pusula radar radyo raf rahat rakam rakip randevu ranza rapor ray rehber rehin reklam rekor " +
            "renk resim ressam reçel reçete rota ruh ruj rulet rulo rütbe rüya rüzgar rıhtım saat sabah " +
            "sabun sabır sahil sahip sahne sakal saksı sakız salata salon salça salı saman samur sanat sandık " +
            "sapan saray sargı sarık sayfa sayı saç saçak sağlık sebze seccade sedir sefer seher sehpa sel " +
            "selam semer sendika sepet sergi serin ses sevgi sevinç seyahat seçim sihir silah silgi simit sinek " +
            "sinema sirke sis soba sofra sohbet sokak somun soru soyadı soğan soğuk spor sucuk sulama sulu " +
            "sunucu sözlük süpürge sürahi süre sürü süt sütlaç sıcak sıfır sıla sınıf sıra sırt tabak tabela " +
            "tablo tabure tahta takvim takım talep tane tanık tarak tarif tarih tarla tartı tas tasarım tatil " +
            "tatlı tava tavan tavla tavuk tavşan taze taş tebeşir tebrik tekne tel telefon temel temmuz tencere " +
            "tepe tepsi terazi terlik tespih teyze tilki tiyatro tohum top toprak topuk torba torun tost tren " +
            "turşu tutkal tuz tuğla tüccar tüfek tülbent tünel türbe türkü tüy tırnak tırtıl ucuz ufuk umut " +
            "uyku uzay uçak uçurtma vakit vali valiz vana vapur varlık vasıta vatan vazife vazo veda vergi " +
            "veri verim vezne vida villa vinç virgül vitamin vitrin vişne vücut yabancı yaka yakın yakıt yalan " +
            "yalı yama yamaç yan yanak yangın yanıt yaprak yara yarar yardım yarım yarış yastık yatak yatırım " +
            "yavru yay yayla yayın yaz yazar yazlık yazı yağ yağmur yaş yaşam yedek yelek yelken yemek " +
            "yemiş yengeç yeni yer yetenek yeşil yiğit yol yolcu yorgan yorum yoğurt yufka yumurta yuva yön " +
            "yük yüz yüzme yüzük yıl yılan yıldız yığın zafer zahmet zam zaman zambak zarar zarf zayıf " +
            "zeka zemin zengin zevk zeytin zihin zil zincir zindan zirve zor zorluk zurna zümrüt zürafa çadır " +
            "çakı çakıl çalışma çam çamaşır çamur çanta çapa çare çarşaf çarşı çatal çatlak çatı çay çağ " +
            "çek çekiç çekmece çelik çember çene çengel çerez çerçeve çerçi çevre çeşit çeşme çift çiftlik çiftçi " +
            "çilek çim çimen çinko çirkin çizelge çizgi çizim çizme çiçek çoban çocuk çok çorap çorba çukur " +
            "çuval çöl çöp çöpçü çözüm çürük çıban çınar çıra çırak çığ ödev ödül ödünç öfke öksürük " +
            "ölçek ölçü ömür önder önem öneri önlem önlük öpücük ördek örgü örnek örtü örümcek öykü özel " +
            "özet özür öğle öğrenci öğün öğüt ücret ülke ülkü ümit ünlü üretim ürün üslup üst üstat " +
            "ütü üye üyelik üzüm üzüntü üçgen ıhlamur ılgın ılık ılıman ırgat ırk ırmak ıslak ıslık ıspanak " +
            "ısrar ıssız ıstakoz ısı ısırgan ısıtıcı ızdırap ızgara ışık ışıklı şahin şair şaka şal şalgam şalvar " +
            "şamdan şampuan şans şapka şarap şarkı şaşkın şebeke şeftali şehir şehit şeker şelale şemsiye şerbet şezlong " +
            "şiir şimşek şimşir şirket şişe şişman şoför şort şube şurup şölen"
        ).split(' ').filter { it.isNotBlank() }.toSet()

        val EN_WORDS: Set<String> = (
            "abates abbe abjured accused acidify acorns addling advisor aerate aerator affixed afire afoul afters again agar " +
            "agates agents agony aids ailed airily aka alchemy alga aliased align aligns alive alluded almoner along " +
            "altered alum ambient amerces amid amnions anemia animist anion anions annexed annexes anodyne antis ants antsier " +
            "anytime appeals apron apt apter arches argon arming armpits arms arouses arrases ascend askance assigns atomize " +
            "atoms attains attar auburn audit auger aureus auroras authors avenged avoid awed ayes baba back bad " +
            "badge baggier balder balked ballsy balsas bangle banking bantams barb barrow barter bash basin basins baton " +
            "batters bawdily bawling began begging behest belie bellhop belongs bench beneath besets bestial better bib bibles " +
            "bilge bindi bingo biofuel bistro bitmap blames blanket blimey blindly blinds bling blintz blitzes blocked blocky " +
            "blooms blossom blotch blowers blowjob blues blurted blurts blushes boatmen bodegas bodged bodkin boggy bond booed " +
            "booger booming boosts bootee booting boozer boozers boring borzoi bossed both botnets bottled bottles bowl bowwows " +
            "boy bpm bra brag braid braises bravura brazers breakup breve breves brewing brimful brindle bringer brogan " +
            "brook broths brr brushed buckles buffer buffing bugfix buggery bugling bulling bullion bummed buncoed bunkum buns " +
            "burden burgeon burners bursa bury bussing butts buzzing bypass cab cabana cadre caesura caffs caitiff cajoled " +
            "calcium calculi callers calms canteen canters cap capped captors carboys carcass carders cardio careers carers cargo " +
            "caroms carpel carter cascara case caters catkin cecum cedar censor certs chain chaises chariot chary chatty " +
            "cheater chest chg chiefer chiefs chime chirrup chitin chocked choicer chorea chucked chugged churls chyrons ciders " +
            "cilium cinch city civil claimer clamped clanged clangs claque cleaves cliches clinics cloaked clock clocks clonal " +
            "cloned close cloud clouded clunks clxix coats cobnut cochair coed coercer coffer col college colons comment " +
            "commie concurs conjure consul convert convey cooled cooler copper copula cor cordage cordons core corms cornets " +
            "corning coronas corsage cosplay costing cot council cow crackup craning crazy creels cringes crinkle croak crusted " +
            "crypts curable curies curlews curtest cuspid cuteys cutouts cyborgs cycling cymbal dabbed dailies dairy dally damned " +
            "dandle darkest dashes dashiki data dates datum dbl deathly debauch decaf decamps decayed decibel decoded decoys " +
            "decrees decried decrypt deed deeps defamed defiant deigns deistic deluxe demise demo demonym demotic dengue denier " +
            "dented deploy deprave dequeue derrick desalts descry designs desist despots deviate devout dhotis dhow dialog diatoms " +
            "dibble digest dime dimming dimple dine dined diners dining dip disbars discern distill ditch ditty divulge " +
            "dizzier dobbin doctor dodder doles dolt domino dona donas donate donated donkey doofus doomers dots dotty " +
            "dowers downed doyen dozed dozen drabber dramedy dreamed drew drift drifted drills droplet drunken dryads dryer " +
            "duly dumber dune dusk duskier dyers ear ebbs ecology edify eds effs egret elicit elixirs elks " +
            "elms embark emf eminent ency endnote endures enigmas enrolls entices envenom envied envy epee epithet equerry " +
            "equinox eras escapes essence esteem etches evasive eve ever every exons exotics extent exurbs eyefuls fabled " +
            "fabric face fact faculty failing fainest faint fairing fallows falsely fancily fanged fanzine fastest fated fence " +
            "fencers fibers fibril fiddle fiddled fidget figs filled finals finials fireman firing firmly fishily fist fittest " +
            "fiver fixings flaky flatus flaunt flaw fleapit fleetly flexing flipper floored floors florid flouter flumes flunky " +
            "flux foaling focal folks form fouler fouling founds foxhunt foxier frats friable friar fringe fryer ftping " +
            "fuddle funks funny furor gadfly gain gametic gamine gamins gamy gangway gaping garble garrote gases gaudier " +
            "gee geeks gelding genii genome gentle gentles gerund gesso getting geyser ghetto giants gibber gibbets gigged " +
            "giggles girded glacier glades glance gland glazing gleans glides gloom glottis gloves gnats gnu gods godsend " +
            "golden good goofed gooks gotta gout gram graph grasp gratify gratis gravels great griffin griming gripes " +
            "gristle groomer grooms grosser grounds grubber grudges grumble gulags gulped gust guttier guzzles gypster gypsy hails " +
            "hallway halon hamming hammock hamper handily handing handoff hangdog haploid hardens hardtop hatband hayloft headier hear " +
            "heckler hedges heists helices helve heroes heroin hetero hicks hides hind hipping hipster hiss hobbles hobo " +
            "hocks hoecake hollers hombres homier homing hoodies hooding hoop horsing hos hotcake howl hows hubcap huffing " +
            "huffs huge hugger hull humeral hummus hurries hurting hymn hymnal idols idyll iffy igloo ignore illus " +
            "imam imbued imitate impaled impels inane inducts inform infuse ingenue inky inquire insider intact intake intends " +
            "intents intone intones inwards ionize ionizes irately irked issued itch ivied jab jabbers jabbing jag jangle " +
            "jasper jelling jested jester jet jibbing jiggled jihadis jinni johnny joints josh jousts jowly jugs juices " +
            "juicy junco jurist just justify jutting keener kegs kenning ketone kettles keynote khaki kibble kicks killed " +
            "killers kings kit knee knifed knights knoll knurl kraut laciest laconic laddie ladies lampoon lams lander " +
            "lapped lapsed lateral lathing lattes lavs laws lawyer lax layover lays layup lazied leaded leakers ledger " +
            "lefter legacy lend lepers letdown lianas licit lick liefest lignite limbos liming links lino lisped loafs " +
            "lochs logging loll lolled long looked looking loon loops looted loss louse lowball lubed lucre lumber " +
            "lummox lunacy lupus lure madder madrasa mafioso mages maimed makeup makings malt maltier malty mammal manages " +
            "manged maniac mansard mantas markers maroon marrows marshy masala maser masher maxi may maybes meal measly " +
            "meddled median medium megs melee menace merger mermaid metered meting mettle midden middy midways mildew mile " +
            "milk milking millers mimic minders mindset minuter mirier mirrors miserly missed mittens mobs moderns modify moires " +
            "molt molters molting mommy monomer monthly moods mooing mooning moped moral moreish mortar mortise motto mousers " +
            "movies mucosa muftis murkier murkily mussing mutants muzak mynas mystic naans nabob nadirs naivete narco natters " +
            "naval nave neatens needful nephews nerds nervier nets niche niftier niggaz niggler nights nitrate nobble nobbles " +
            "noises nominal noshed notate nova novene nub numbers nutting nymphos oarsman oath obi oboists ocelots octavo " +
            "ocular oddball odors offer offices offline offs offset oft often ohm oms ones onrush oohed openest " +
            "opens opining opposed orating orc orgies orphan other ourself outed outlays outwit outwore overdue overs overtax " +
            "ovules oxen oxtails paced pacing pact paddies paddle paella paged palpate palsied panned papacy papers parades " +
            "parent parolee parsec pasha passe past pasta pastel pastels pathway patinas paunchy pause pawpaws payable payload " +
            "paywall peat pecked pecking peckish pedalo peeing peel peering pelmet pends peons peptide perks perky persist " +
            "pesos peters petite pettier pettish pewits phases phisher picots piled pimp pinier pique pissing pistes pitcher " +
            "pithily pixies plaints plating platoon platted playboy pleat plectra pledged plenum plonks plumply podding pods pointy " +
            "polenta police poll poly polyps pommels pong popgun popper porker porno post posts potfuls praises premeds " +
            "presser prices prides primula prion prissy prob probe prodigy pronged prophet prosaic protect prudes psycho psychos " +
            "puddles puds pulleys punnets purge purify purpose pussy quakes quarts quested queued quibble quines quipped quizzed " +
            "races raceway racking rafter rag ragga railing ramify rams ranched rancid rape raping rascals raucous razing " +
            "reached ready reals rearing rears rebuild rebuts recasts recces recess rectory recycle redneck redraws reduce reducer " +
            "reeds reenter reeving reffed refile refiner regnant regrow related relays removed reneged repaint repast repeal repent " +
            "replay reports reprise rerun rescind rescue resewn reside respray restudy resume retells retie retinal retort retying " +
            "reunite revamp reveal revers reverts revived rewind rewords richly ricin ricked riffle rifle rifled rig righter " +
            "rime ripe ripping rises rivets roam roar roars robotic rocker rod rogers rogue roguery roman rook " +
            "room root rotted rousted ruffian rules runlet runoff rusks rye sacked sacking sags sailors salines saliva " +
            "sally salons salsas salt samosas samurai sand sassed sateen satiric sawyer scabies scalar scale scales scalier " +
            "scenes sch scherzo scoop scrips scrolls scubas scud scumbag seabeds sealers sears seine seltzer semis senors " +
            "serving severs sexily shades shakes shale shame shammed shew shinned shiny shire shirks shoddy shorter shouted " +
            "shovel showman showy shred shrewd shrieks shrinks shushes shyness sibling sibyl sibyls signal signora sines singed " +
            "singing sixth sizing skates skeptic skiffle skimp slam slaw sleeper slicers slick sliver slob slogan sloop " +
            "slow smalls smarty smiled smileys smiling smite snail snare snaring snidely snip snipes snob snoop snored " +
            "snubbed snuffed sobs sodden softy solo solos song sonic sordid sorrels souls soupier soups sow spacers " +
            "spaces spank spasms spate sped spewer spikes spines spiral spirea spits splicer spliff split sporran sports " +
            "spouses sprier sprung spunk squeak squirt staid stalled stamps stand stared starter stay stenos stetted stogies " +
            "stoic stoical stoke stoups stouter stouts straits strange streaks stripey strop stubbly sty subjoin subsume subways " +
            "suffice suit sulkily summon sundaes sunny sunspot suppers surfs surreys sweller swift swine swoop swooped swotted " +
            "sylphic synod syrupy tableau tacks tag tailed taint taints takeout tallest tallied talons tapered taproot tares " +
            "tarot tatters taupe taus taxers taxied teargas teased techie teem telnet tempter tenancy tendon tenon tenuous " +
            "terror testing tetchy tetra thaw theft thin threats throat thruway ticks tics tidbits tideway tidings tidying " +
            "tikka til timbers tinnier tint toed toenail tootles topees topics topspin torches tortes tossing tote toughed " +
            "tour towers tows tracer traffic trainee tread tribe trigger trilby trim trimmed triple tripos troll tropism " +
            "trowed trucker trump tryst tubas tubing tugboat tums tundra tuning turban turbot turkeys turtle turtles tweaked " +
            "tweed twinkle twins ugliest uhf ult ululate umiaks umped umps unbars uncurl unlit unmeant unnamed unrated " +
            "unsays unsold unties unwise updater upriver urges urinary use usurer usury uteri vaguer valance valets valise " +
            "var vaunt vaunts veejay veiled verges verity vestry vet viably victims videos vilely villus vino viol " +
            "virile visits vol voters votes voyages vulpine wadis wag wagers wailer waldo walked wankers wanness warden " +
            "warrens washout wastage waver weaken weals weans webbing weep weepers were whacked wheaten whelms whilst whimsy " +
            "whiskey whites whizzes whore whorl why width wielded wiggled wiggles wintry wirier witters wokest wolfish womanly " +
            "wonted work world worries wrapper wryest xciv xrefs yakking yellow yobbos young yrs yummier zooming zoom"
        ).split(' ').filter { it.isNotBlank() }.toSet()
    }
}
