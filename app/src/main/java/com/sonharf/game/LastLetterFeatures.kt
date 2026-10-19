package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.isPremierFinished
import java.util.Locale

internal data class LastLetterSeries(val matchNumber: Int, val played: Int, val myWins: Int, val rivalWins: Int) {
    val complete: Boolean get() = played == 3
}

/** Three distinct matches linked by the server's existing, mutually accepted rematch flow. */
internal fun lastLetterSeries(current: GameRoomDto, rooms: List<GameRoomDto>, me: String): LastLetterSeries? {
    val rival = if (current.hostId == me) current.guestId else current.hostId.takeIf { current.guestId == me }
    if (current.isBot || rival == null || rival == me) return null
    val indexed = rooms.associateBy { it.id } + (current.id to current)
    val visited = mutableSetOf<String>()
    val chain = mutableListOf<GameRoomDto>()
    var cursor = current
    while (true) {
        if (!visited.add(cursor.id) || cursor.isBot || setOf(cursor.hostId, cursor.guestId) != setOf(me, rival)) return null
        chain.add(cursor)
        val parent = cursor.rematchOf ?: break
        cursor = indexed[parent] ?: return null
    }
    val ordered = chain.asReversed()
    val group = ordered.drop((ordered.lastIndex / 3) * 3)
    val finished = group.filter { it.isPremierFinished() }
    return LastLetterSeries(group.size, finished.size, finished.count { it.winnerId == me }, finished.count { it.winnerId == rival })
}

internal fun lastLetterLongest(words: List<GameWordDto>, me: String?): String? = words
    .filter { me != null && it.playerId == me && !it.isBot }
    .map { it.normalizedWord.ifBlank { it.word } }
    .maxByOrNull { it.length }

internal fun lastLetterObservation(word: String, previousWords: List<String>, streak: Int, language: String): String? {
    val english = language == "en"
    val shown = word.uppercase(if (english) Locale.ENGLISH else Locale.forLanguageTag("tr-TR"))
    return when {
        word.length >= 6 && word.length > (previousWords.maxOfOrNull { it.length } ?: 0) ->
            if (english) "$shown is your longest word in this match: ${word.length} letters."
            else "$shown, bu maçtaki en uzun kelimen: ${word.length} harf."
        streak >= 5 && streak % 5 == 0 -> if (english) "$streak valid words in a row. Keep the chain going!"
            else "Art arda $streak geçerli kelime. Zinciri koru!"
        else -> null
    }
}

/** Short original explanations; word acceptance always comes from the main game dictionary. */
internal object LastLetterDiscoveries {
    private val tr = mapOf(
        "kalem" to "Yazı yazmaya veya çizim yapmaya yarayan araç.",
        "masa" to "Üzerinde çalışılan, yemek yenilen veya eşyalar konulan mobilya.",
        "kitap" to "Yazılı metinleri bir araya getiren basılı ya da elektronik eser.",
        "deniz" to "Yeryüzündeki geniş tuzlu su alanı.",
        "bahar" to "İlkbahar ve sonbahar mevsimlerinin ortak adı.",
        "yol" to "Bir yerden başka bir yere gitmeye yarayan geçiş alanı.",
        "orman" to "Çok sayıda ağacın ve başka canlıların birlikte bulunduğu geniş doğal alan.",
        "umut" to "İyi bir şeyin gerçekleşebileceğine duyulan güven.",
        "güneş" to "Dünya'ya ışık ve ısı sağlayan yıldız.",
        "ırmak" to "Suları denize, göle veya başka bir akarsuya ulaşan büyük akarsu.",
        "çiçek" to "Birçok bitkide üremeyi sağlayan, çoğunlukla renkli bölüm.",
        "rüya" to "Uyku sırasında zihinde yaşanan görüntü ve olaylar.",
        "yaren" to "Yakın dost, arkadaş.",
        "ahenk" to "Parçalar veya sesler arasındaki uyum.",
        "seher" to "Günün ağarmasından hemen önceki zaman.",
        "huzur" to "Rahatlık, dinginlik ve iç ferahlığı.",
        "merak" to "Bir şeyi öğrenme veya anlama isteği.",
        "emek" to "Bir işi gerçekleştirmek için harcanan beden veya zihin gücü.",
        "sabır" to "Zorluklar karşısında dayanma ve bekleyebilme gücü.",
        "dost" to "Güvenilen ve yakınlık duyulan kimse.",
        "zaman" to "Olayların oluş sırasını ve süresini anlatan kavram.",
        "sanat" to "Duygu ve düşüncelerin yaratıcı biçimlerle ifade edilmesi.",
        "bilgi" to "Öğrenme, araştırma veya gözlem yoluyla edinilen birikim.",
        "renk" to "Işığın gözde oluşturduğu, cisimleri ayırt etmeye yarayan özellik.",
        "nehir" to "Irmak; büyük akarsu.",
        "gölge" to "Işığın bir engelle kesilmesiyle oluşan karanlık alan.",
        "hayal" to "Zihinde canlandırılan görüntü, düşünce veya gerçekleşmesi istenen durum.",
        "yıldız" to "Kendi ışığını üreten gök cismi.",
        "masal" to "Olağanüstü olayların da yer alabildiği hayal ürünü anlatı.",
        "kelime" to "Dilde bir anlam veya görev taşıyan ses ya da harf birliği; sözcük.",
        "taht" to "Hükümdarın oturduğu, egemenliği simgeleyen koltuk.",
        "zeka" to "Anlama, düşünme ve sorun çözme yeteneği.",
        "armut" to "İnce kabuklu, sulu bir meyve ve bu meyveyi veren ağaç.",
        "elma" to "Yuvarlak biçimli, kabuklu ve çekirdekli bir meyve.",
        "toprak" to "Bitkilerin yetiştiği, yeryüzünü kaplayan gevşek doğal örtü.",
        "bulut" to "Havada asılı duran küçük su damlacıkları veya buz kristalleri topluluğu.",
        "yağmur" to "Bulutlardan damlalar hâlinde düşen su.",
        "ışık" to "Görmeyi sağlayan elektromanyetik enerji.",
        "özlem" to "Bir kimseyi, yeri veya durumu yeniden görme ya da yaşama isteği.",
        "kıvılcım" to "Yanan bir maddeden sıçrayan küçük ateş parçası.",
    )
    private val en = mapOf(
        "apple" to "A round fruit with a thin skin and seeds inside.",
        "river" to "A natural stream of water flowing towards a lake, sea or another river.",
        "stone" to "A small piece of rock.", "earth" to "The planet we live on; also soil or ground.",
        "light" to "Energy that allows us to see.", "dream" to "Images and experiences during sleep; also a cherished hope.",
        "cloud" to "A visible collection of tiny water droplets or ice crystals in the air.",
        "leaf" to "A usually flat part of a plant that helps it make food from sunlight.",
        "forest" to "A large area covered mainly by trees.", "moon" to "Earth's natural satellite.",
        "water" to "The clear liquid essential to life.", "star" to "A celestial body that produces its own light.",
        "friend" to "Someone you know well and trust.", "word" to "A unit of language with a meaning or grammatical role.",
        "book" to "A written work published as pages or in electronic form.",
        "hope" to "A feeling that something good may happen.", "time" to "The sequence and duration of events.",
        "garden" to "An area where plants are grown.", "quiet" to "Making little or no noise.",
        "throne" to "A ceremonial seat used by a ruler.",
    )
    fun meaning(word: String, language: String): String? = (if (language == "en") en else tr)[word.lowercase(
        if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR"))]
}
