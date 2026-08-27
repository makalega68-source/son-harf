package com.sonharf.game

import androidx.compose.ui.graphics.Color

internal object LetharaPalette {
    val Night = Color(0xFF071229)
    val Night2 = Color(0xFF0D1A3A)
    val Panel = Color(0xE6142447)
    val PanelStrong = Color(0xF20E1A36)
    val Cyan = Color(0xFF56D6FF)
    val Violet = Color(0xFF9C7CFF)
    val Gold = Color(0xFFFFD36A)
    val Text = Color(0xFFF4F0FF)
    val Muted = Color(0xFFB8B5D4)
    val Green = Color(0xFF62D9A7)
    val Red = Color(0xFFFF7D96)
}

internal data class WizardLoreCharacter(
    val key: String,
    val name: String,
    val titleTr: String,
    val titleEn: String,
    val nameMeaningTr: String,
    val nameMeaningEn: String,
    val archetypeTr: String,
    val archetypeEn: String,
    val temperamentTr: String,
    val temperamentEn: String,
    val color: Color,
    val mascotId: String?,
    val whisperTr: List<String>,
    val whisperEn: List<String>,
)

internal data class WizardLoreChapter(
    val id: String,
    val order: Int,
    val titleTr: String,
    val titleEn: String,
    val unlockLevel: Int,
    val summaryTr: String,
    val summaryEn: String,
    val bodyTr: String,
    val bodyEn: String,
)

internal object LetharaLore {
    const val WORLD = "Lethara"
    const val ENEMY = "Varkhor"
    const val PLAYER_ROLE_TR = "Hatırlatıcı"
    const val PLAYER_ROLE_EN = "Remembrancer"

    val characters = listOf(
        WizardLoreCharacter(
            key = "lyra",
            name = "Lyra",
            titleTr = "Yıldız Büyücüsü",
            titleEn = "Star Mage",
            nameMeaningTr = "Lyra, gökteki lir takımyıldızını ve yıldızların ezgisini çağrıştırır; umut ile merakı temsil eder.",
            nameMeaningEn = "Lyra evokes the lyre constellation and the music of stars; she represents hope and curiosity.",
            archetypeTr = "Merak • Umut • Yıldız Yankıları",
            archetypeEn = "Curiosity • Hope • Star Echoes",
            temperamentTr = "Sıcak, meraklı ve heyecanlı.",
            temperamentEn = "Warm, curious and excitable.",
            color = Color(0xFF7EDBFF),
            mascotId = MascotCatalog.CHIBI_WIZARD_ID,
            whisperTr = listOf("Yıldızlar... bu mühürü daha önce gördü.", "Mor Ay Kulesi... neden adını hatırlıyorum?", "Bir son harf... başka bir kapıyı açıyordu."),
            whisperEn = listOf("The stars... have seen this seal before.", "The Violet Moon Tower... why do I remember that name?", "A final letter... used to open another gate."),
        ),
        WizardLoreCharacter(
            key = "kael",
            name = "Kael",
            titleTr = "Koruyucu",
            titleEn = "The Guardian",
            nameMeaningTr = "Kael, sert ve kadim bir koruyucu adı olarak sadakat ile cesareti simgeler.",
            nameMeaningEn = "Kael carries the sound of an old guardian name, symbolizing loyalty and courage.",
            archetypeTr = "Sadakat • Cesaret • Mühür Kalkanları",
            archetypeEn = "Loyalty • Courage • Seal Shields",
            temperamentTr = "Cesur, korumacı ve inatçı.",
            temperamentEn = "Brave, protective and stubborn.",
            color = Color(0xFFFFC66B),
            mascotId = null,
            whisperTr = listOf("Bu kalkanı... birini korumak için kaldırmıştım.", "Altın kapı düşmemeliydi.", "Varkhor... o isim neden öfkelendiriyor beni?"),
            whisperEn = listOf("I raised this shield... to protect someone.", "The golden gate was not meant to fall.", "Varkhor... why does that name anger me?"),
        ),
        WizardLoreCharacter(
            key = "neris",
            name = "Neris",
            titleTr = "Gölge Bilgesi",
            titleEn = "Shadow Sage",
            nameMeaningTr = "Neris, derin sular ve karanlıkta saklı bilgi hissi taşır; sezgi ile sırları temsil eder.",
            nameMeaningEn = "Neris suggests deep waters and knowledge hidden in darkness; she represents intuition and secrets.",
            archetypeTr = "Sezgi • Sırlar • Unutulmuş Anlamlar",
            archetypeEn = "Intuition • Secrets • Forgotten Meanings",
            temperamentTr = "Sakin, gizemli ve mesafeli.",
            temperamentEn = "Calm, mysterious and reserved.",
            color = Color(0xFFA493FF),
            mascotId = null,
            whisperTr = listOf("Gölgeler yalan söylemez... sadece eksik konuşur.", "Birimiz gerçeği biliyordu.", "Sessiz Taç'ın izi hâlâ burada."),
            whisperEn = listOf("Shadows do not lie... they merely speak incompletely.", "One of us knew the truth.", "The Silent Crown still left a trace here."),
        ),
        WizardLoreCharacter(
            key = "ryvan",
            name = "Ryvan",
            titleTr = "Fırtına Ustası",
            titleEn = "Storm Master",
            nameMeaningTr = "Ryvan, hız ve keskin gök gürültüsü çağrışımıyla enerji ile öfkenin dengesini temsil eder.",
            nameMeaningEn = "Ryvan evokes speed and sharp thunder, representing the balance between energy and anger.",
            archetypeTr = "Heyecan • Öfke • Yıldırım Ritmi",
            archetypeEn = "Excitement • Anger • Lightning Rhythm",
            temperamentTr = "Enerjik, sabırsız ve asabi.",
            temperamentEn = "Energetic, impatient and hot-tempered.",
            color = Color(0xFF69A9FF),
            mascotId = null,
            whisperTr = listOf("Gökyüzü o gece kırılmıştı.", "Fırtına benim adımı biliyordu.", "Bir daha asla geç kalmayacağım."),
            whisperEn = listOf("The sky broke that night.", "The storm used to know my name.", "I will never be late again."),
        ),
        WizardLoreCharacter(
            key = "mivo",
            name = "Mivo",
            titleTr = "Neşe Büyücüsü",
            titleEn = "Joy Mage",
            nameMeaningTr = "Mivo, hafif ve oyunbaz tınısıyla mizahı, yaratıcılığı ve beklenmedik çözümleri temsil eder.",
            nameMeaningEn = "Mivo has a light, playful sound representing humor, creativity and unexpected solutions.",
            archetypeTr = "Mizah • Yaratıcılık • Oyun Büyüsü",
            archetypeEn = "Humor • Creativity • Play Magic",
            temperamentTr = "Komik, yaramaz ve yaratıcı.",
            temperamentEn = "Funny, mischievous and creative.",
            color = Color(0xFFFF8BCB),
            mascotId = null,
            whisperTr = listOf("Savaşta bile güldürmüştüm onları... kimi?", "Bu mühür ters duruyor. Belki de özellikle.", "Bir kahkaha bazen büyüden güçlüdür."),
            whisperEn = listOf("I made them laugh even in battle... who were they?", "This seal is upside down. Perhaps deliberately.", "Sometimes a laugh is stronger than a spell."),
        ),
        WizardLoreCharacter(
            key = "selen",
            name = "Selen",
            titleTr = "Sessiz Kâhin",
            titleEn = "Silent Seer",
            nameMeaningTr = "Selen, ay ışığı ve sessiz gökyüzü çağrışımıyla bilgelik, utangaçlık ve geleceğin yankılarını temsil eder.",
            nameMeaningEn = "Selen evokes moonlight and a quiet sky, representing wisdom, shyness and echoes of the future.",
            archetypeTr = "Bilgelik • Kehanet • Söylenmemiş Kelimeler",
            archetypeEn = "Wisdom • Prophecy • Unspoken Words",
            temperamentTr = "Utangaç, sakin, derin ve sezgisel.",
            temperamentEn = "Shy, calm, deep and intuitive.",
            color = Color(0xFFC6B7FF),
            mascotId = null,
            whisperTr = listOf("Bunu daha önce görmedim... ama hatırlıyorum.", "Son mühür kırıldığında bir ses duydum.", "Gelecek bazen geçmişten fısıldar."),
            whisperEn = listOf("I have never seen this... yet I remember it.", "I heard a voice when the final seal broke.", "Sometimes the future whispers from the past."),
        ),
    )

    val chapters = listOf(
        WizardLoreChapter(
            id = "son_muhur_savasi", order = 1,
            titleTr = "Son Mühür Savaşı", titleEn = "The Last Seal War", unlockLevel = 1,
            summaryTr = "Altı Mühür, Söz Dokusu'nu ele geçirmek isteyen Varkhor'un ordusuna karşı son kez birleşti.",
            summaryEn = "The Six Seals united one final time against Varkhor's army and his attempt to seize the Word Weave.",
            bodyTr = "Lethara'da büyü sözcüklerden doğardı. Her ilk harf bir kapı, her son harf yeni bir mührün başlangıcıydı. Varkhor özgür sözcüklerin kaos yarattığına inanarak Sessiz Taç'ı yarattı. Altı Mühür onu durdurmak için Son Mühür Savaşı'nda birleşti ve ordusunu neredeyse tamamen yok etti.",
            bodyEn = "In Lethara, magic was born from words. Every first letter was a gate and every final letter the beginning of another seal. Believing free words created chaos, Varkhor forged the Silent Crown. The Six Seals united in the Last Seal War and nearly destroyed his army.",
        ),
        WizardLoreChapter(
            id = "unutulus_yemini", order = 2,
            titleTr = "Unutuluş Yemini", titleEn = "The Oath of Oblivion", unlockLevel = 3,
            summaryTr = "Yenileceğini anlayan Varkhor, öldürmek yerine hafızayı ve gücü hedefleyen yasak büyüyü kullandı.",
            summaryEn = "Facing defeat, Varkhor used a forbidden spell that attacked memory and power instead of life.",
            bodyTr = "Varkhor savaş alanındaki son kara büyüyü tek bir yeminde topladı: Unutuluş Yemini. Büyü Altı Mühür'ü öldürmedi; güçlerini mühürledi, anılarını parçaladı ve hafıza kırıntılarını Lethara'nın dört bir yanına savurdu. O günden sonra büyücüler zaman zaman geçmişlerinden cümleler mırıldanır, fakat nedenini hatırlayamaz.",
            bodyEn = "Varkhor gathered the remaining dark magic into one oath: the Oath of Oblivion. It did not kill the Six Seals; it sealed their powers, shattered their memories and scattered fragments across Lethara. Since then, the mages sometimes murmur lines from their past without knowing why.",
        ),
        WizardLoreChapter(
            id = "hatirlatici", order = 3,
            titleTr = "Hatırlatıcı", titleEn = "The Remembrancer", unlockLevel = 7,
            summaryTr = "Oyuncunun sözcükleri, kırılmış hafıza mühürlerini yeniden birbirine bağlamaya başlar.",
            summaryEn = "The player's words begin reconnecting the shattered memory seals.",
            bodyTr = "Eski metinler, Söz Dokusu'nun kopan parçalarını yeniden bağlayabilen kişilere Hatırlatıcı der. Her maç, her XP ve her dostluk seviyesi maskotun hafızasında yeni bir kıvılcım uyandırır. Oyuncunun görevi yalnızca kazanmak değil, Altı Mühür'ün kim olduğunu yeniden keşfetmektir.",
            bodyEn = "Ancient texts call those who reconnect broken strands of the Word Weave Remembrancers. Every match, XP gain and friendship level awakens another spark in a mascot's memory. The player's purpose is not only to win, but to rediscover who the Six Seals were.",
        ),
        WizardLoreChapter(
            id = "guc_uyanisi", order = 4,
            titleTr = "Güç Uyanışı", titleEn = "Power Awakening", unlockLevel = 12,
            summaryTr = "Hafıza geri döndükçe eski büyüler, görünüşler ve özel tepkiler yeniden uyanır.",
            summaryEn = "As memories return, old magic, appearances and unique reactions awaken again.",
            bodyTr = "Güç Uyanışı rekabet adaletini bozmaz. Eski güçler PvP üstünlüğü yerine yeni animasyonlar, büyü efektleri, görünüm dönüşümleri, prestij unvanları, hikâye parçaları ve özel konuşmalar açar. Hatırlamak maskotu güçlü değil, yeniden kendisi yapar.",
            bodyEn = "Power Awakening never breaks competitive fairness. Old powers unlock animations, magical effects, visual transformations, prestige titles, story fragments and unique dialogue rather than PvP advantage. Remembering does not make the mascot unfairly stronger; it makes them themselves again.",
        ),
        WizardLoreChapter(
            id = "varkhorun_parcalari", order = 5,
            titleTr = "Varkhor'un Parçaları", titleEn = "Fragments of Varkhor", unlockLevel = 20,
            summaryTr = "Büyücüler hatırladıkça, Unutuluş Yemini içinde saklanan başka bir varlık da uyanmaya başlar.",
            summaryEn = "As the mages remember, something else hidden inside the Oath of Oblivion begins to awaken.",
            bodyTr = "Varkhor savaşta tamamen yok olmadı. Unutuluş Yemini'nin bedeli olarak kendi varlığını da Söz Dokusu'na parçalamıştı. Maskotların hafızaları geri döndükçe oyuncu istemeden Varkhor'un parçalarını da uyandırır. Geçmişi geri getirmek artık bir kurtarma görevi kadar bir risk haline gelir.",
            bodyEn = "Varkhor was not completely destroyed. As the price of the Oath of Oblivion, he fragmented his own existence into the Word Weave. As the mascots recover memories, the player unknowingly awakens Varkhor's fragments too. Restoring the past becomes a risk as much as a rescue.",
        ),
        WizardLoreChapter(
            id = "yeni_soz_dokusu", order = 6,
            titleTr = "Yeni Söz Dokusu", titleEn = "The New Word Weave", unlockLevel = 30,
            summaryTr = "Son seçim, eski düzeni yeniden kurmak değil; geçmişin hatalarını tekrarlamadan yeni bir bağ yaratmaktır.",
            summaryEn = "The final choice is not to restore the old order, but to create a new bond without repeating its mistakes.",
            bodyTr = "Bütün hafıza parçaları birleştiğinde gerçek ortaya çıkar: Altı Mühür kusursuz değildi ve Varkhor'un korkuları tamamen temelsiz değildi. Oyuncu ile maskotlar eski sistemi kopyalamak yerine yeni bir Söz Dokusu kurar. Böylece Lethara'nın en eski yasası yeniden anlam kazanır: Her son, yeni bir başlangıçtır.",
            bodyEn = "When all memory fragments unite, the truth emerges: the Six Seals were not flawless and Varkhor's fears were not entirely baseless. Instead of copying the old system, the player and mascots build a new Word Weave. Lethara's oldest law gains new meaning: every ending is a new beginning.",
        ),
    )

    val introTr = "Lethara'da büyü sözcüklerden doğar. Varkhor'un Unutuluş Yemini, Altı Mühür'ün güçlerini ve hafızalarını parçaladı. Sen bir Hatırlatıcı'sın; maçlar, XP ve dostlukla onların geçmişini yeniden uyandıracaksın."
    val introEn = "In Lethara, magic is born from words. Varkhor's Oath of Oblivion shattered the powers and memories of the Six Seals. You are a Remembrancer; matches, XP and friendship will awaken their past."

    fun character(key: String?): WizardLoreCharacter =
        characters.firstOrNull { it.key == key } ?: characters.first()

    fun characterForMascot(mascotId: String?): WizardLoreCharacter =
        characters.firstOrNull { it.mascotId == mascotId } ?: characters.first()

    fun randomWhisper(character: WizardLoreCharacter, language: String, seed: Int): String {
        val list = if (language == "en") character.whisperEn else character.whisperTr
        return list[Math.floorMod(seed, list.size)]
    }
}
