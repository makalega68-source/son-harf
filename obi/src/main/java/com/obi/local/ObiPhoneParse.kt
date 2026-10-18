package com.obi.local

import java.util.Locale

/** A phone action asked for in plain Turkish ("youtube aç", "feneri aç", "ekran ışığını kıs"). */
internal sealed class PhoneCommand {
    /** An installed app by name, e.g. "whatsapp"; [name] is still in the spoken form. */
    data class OpenApp(val name: String) : PhoneCommand()
    /** Search inside an app: "youtube'da tarkan aç" → app "youtube", query "tarkan". */
    data class AppSearch(val app: String, val query: String) : PhoneCommand()
    data class WebSearch(val query: String) : PhoneCommand()
    data class PlayMusic(val query: String) : PhoneCommand()
    data class Camera(val video: Boolean, val selfie: Boolean) : PhoneCommand()
    /** null = switch to the other state. */
    data class Torch(val on: Boolean?) : PhoneCommand()
    /** [percent] sets a level; otherwise [step] -1 dims and +1 brightens. */
    data class Brightness(val percent: Int? = null, val step: Int = 0, val auto: Boolean = false) : PhoneCommand()
    /** Media volume: -1 down, +1 up, 0 mute, 2 loudest. [ringer] "vibrate"/"normal" changes the ringer instead. */
    data class Volume(val step: Int, val ringer: String? = null) : PhoneCommand()
    /** A settings page: main, wifi, bluetooth, internet, location, airplane, dnd, sound, display, battery, nfc, storage. */
    data class SettingsPage(val page: String, val wantsToggle: Boolean) : PhoneCommand()
    /** Needs Obi's accessibility helper: lock, screenshot, notifications, recents. */
    data class Global(val action: String) : PhoneCommand()
    object Home : PhoneCommand()
}

/** How a spoken app name is found on the phone. */
internal sealed class AppTarget {
    /** A kind of app (gallery, music…) opened through Android's own category, whatever the brand. */
    data class Category(val category: String, val label: String) : AppTarget()
    /** Match against installed app names. */
    data class Named(val term: String) : AppTarget()
}

/** Pure parsing of phone commands; no Android types, so it is unit tested. */
internal object ObiPhoneParse {
    private val tr = Locale("tr", "TR")
    private const val OPEN = "(?:aç|ac|açar m[ıi]s[ıi]n|açabilir mis[ıi]n|açsana|açıver|aç bakalım|başlat|baslat|çalıştır|calistir)"
    private val openAtEnd = Regex("^(.{2,40}?)\\s+(?:uygulamas[ıi]n[ıi]\\s+|uygulamas[ıi]\\s+)?$OPEN(?:\\s+(?:lütfen|lutfen|obi))?[.!?]*$")
    private val inApp = Regex("^(youtube|yutup|yutub|spotify|spotifay|google|gugıl|haritalar|harita|maps)['’]?(?:da|de|ta|te|dan|den)\\s+(.{1,60}?)\\s+(?:aç|ac|ara|oynat|izle|çal|cal|dinle|bul|göster|goster)(?:\\s+(?:lütfen|lutfen))?[.!?]*$")
    private val webSearch = Regex("^(?:internette|webde|nette)\\s+(.{1,60}?)\\s+(?:ara|bul)[.!?]*$")
    private val music = Regex("^(.{2,50}?)\\s+(?:şarkısını\\s+|sarkisini\\s+|şarkılarını\\s+|müziğini\\s+)?(?:çal|cal|çalsana|dinlet|oynat)(?:\\s+(?:lütfen|lutfen))?[.!?]*$")
    private val percent = Regex("(?:%|yüzde\\s*|yuzde\\s*)(\\d{1,3})|(\\d{1,3})\\s*(?:%|yüzde|yuzde)")
    private val ownWords = listOf(
        "not", "alarm", "liste", "hatırlat", "hatirlat", "oyun", "dolap", "sohbet", "menü", "menu", "izin", "dinle",
        "ses", "obi", "kelime", "gemini", "odak", "pano", "şapka", "sapka", "harcama", "geri sayım", "su ", "mola",
    )

    fun lower(text: String): String = text.lowercase(tr).trim()

    private fun has(t: String, vararg words: String) = words.any { t.contains(it) }
    private fun word(t: String, pattern: String) = Regex("(?<!\\p{L})(?:$pattern)(?!\\p{L})").containsMatchIn(t)

    private val openVerb = Regex("(?<!\\p{L})(?:aç|ac|açar|açabilir|açsana|başlat|yak)(?!\\p{L})")
    private val offVerb = Regex("(?<!\\p{L})(?:kapat|kapa|söndür|sondur|kapatır)(?!\\p{L})")

    fun parse(raw: String): PhoneCommand? {
        val t = lower(raw).trimEnd('.', '!', '?').replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return null
        // A reminder or a note about something ("yarın feneri aç diye hatırlat") is not the action itself.
        if (has(t, "hatırlat", "hatirlat", "alarm kur", " diye", "not al", "not et", "anımsat")) return null

        if (word(t, "kilitle|kilitler misin|kilitleyebilir misin") || has(t, "tuş kilidi", "tus kilidi", "ekran kilidi")) {
            if (!has(t, "kilidi aç", "kilidini aç", "kilidi ac")) return PhoneCommand.Global("lock")
        }
        if (has(t, "ekran görüntüsü", "ekran goruntusu", "ekran resmi", "ss al")) return PhoneCommand.Global("screenshot")
        if (has(t, "son uygulamalar")) return PhoneCommand.Global("recents")
        if (has(t, "bildirimleri") && has(t, "göster", "goster", "aç", "indir") && !has(t, "izin")) return PhoneCommand.Global("notifications")
        if (has(t, "ana ekran", "anaekran") && has(t, "dön", "don", "git", "aç", "ac", "geç")) return PhoneCommand.Home

        // Torch before brightness: "feneri aç", "flaşı kapat".
        if (word(t, "fener\\p{L}*|el feneri\\p{L}*|flaş\\p{L}*|flas\\p{L}*") ||
            (word(t, "ışığ\\p{L}*|isig\\p{L}*") && !has(t, "ekran") && (openVerb.containsMatchIn(t) || offVerb.containsMatchIn(t)) &&
                !has(t, "kıs", "kis", "azalt", "düşür", "artır", "arttır"))
        ) {
            return PhoneCommand.Torch(when { offVerb.containsMatchIn(t) -> false; openVerb.containsMatchIn(t) -> true; else -> null })
        }

        val screenLight = has(t, "parlaklı", "parlakli", "ekran ışığ", "ekran isig", "ekranın ışığ", "ekranin isig", "ekranı karart", "ekrani karart", "ekranı aydınlat") ||
            (word(t, "ışığ\\p{L}*|isig\\p{L}*") && has(t, "kıs", "kis", "azalt", "düşür", "dusur", "artır", "arttır", "artir", "yükselt"))
        if (screenLight) {
            if (has(t, "otomatik")) return PhoneCommand.Brightness(auto = true)
            percent.find(t)?.let { m -> (m.groupValues[1].ifEmpty { m.groupValues[2] }).toIntOrNull()?.let { return PhoneCommand.Brightness(percent = it.coerceIn(1, 100)) } }
            Regex("(\\d{1,3})\\s*(?:yap|olsun|ayarla)").find(t)?.groupValues?.get(1)?.toIntOrNull()?.let { return PhoneCommand.Brightness(percent = it.coerceIn(1, 100)) }
            if (has(t, "en düşük", "en dusuk", "en az", "minimum", "en karanlık")) return PhoneCommand.Brightness(percent = 1)
            if (has(t, "en yüksek", "en yuksek", "en fazla", "maksimum", "sonuna kadar", "tam ")) return PhoneCommand.Brightness(percent = 100)
            val dim = has(t, "kıs", "kis", "azalt", "düşür", "dusur", "karart", "kapat")
            return PhoneCommand.Brightness(step = if (dim) -1 else 1)
        }

        // Volume ("sesi kıs", "sesi aç", "sessize al"). Obi's own voice commands ("sesini incelt") are handled earlier.
        if (has(t, "sessize al", "sessiz mod", "titreşime al", "titresime al", "titreşim mod")) return PhoneCommand.Volume(0, ringer = "vibrate")
        if (has(t, "zil sesini aç", "zil sesini ac", "sessizden çık", "sessiz modu kapat")) return PhoneCommand.Volume(1, ringer = "normal")
        if (word(t, "ses|sesi|sesini") && !has(t, "incelt", "kalın", "kalin", "değiştir", "degistir", "dene", "duy", "uyarı", "uyari", "durum", "telefon sesi", "obi", "kayıt", "kaydet", "ayar")) {
            when {
                has(t, "sonuna kadar", "en yüksek", "en yuksek", "full", "maksimum") -> return PhoneCommand.Volume(2)
                has(t, "kıs", "kis", "azalt", "düşür", "dusur", "alçalt") -> return PhoneCommand.Volume(-1)
                offVerb.containsMatchIn(t) -> return PhoneCommand.Volume(0)
                has(t, "aç", "artır", "arttır", "artir", "yükselt", "yukselt", "yükse") -> return PhoneCommand.Volume(1)
            }
        }

        if (word(t, "kamera\\p{L}*|selfie|özçekim|ozcekim") || Regex("(?:fotoğraf|fotograf|foto|resim|video)\\s+çek").containsMatchIn(t)) {
            if (!has(t, "izni", "izin")) return PhoneCommand.Camera(video = has(t, "video"), selfie = has(t, "selfie", "özçekim", "ozcekim", "ön kamera", "on kamera"))
        }

        settingsPage(t)?.let { return it }

        inApp.find(t)?.let { m ->
            val app = m.groupValues[1]
            val query = m.groupValues[2].trim()
            return when {
                app.startsWith("goo") || app.startsWith("gug") -> PhoneCommand.WebSearch(query)
                app.startsWith("har") || app == "maps" -> PhoneCommand.AppSearch("maps", query)
                app.startsWith("spo") -> PhoneCommand.AppSearch("spotify", query)
                else -> PhoneCommand.AppSearch("youtube", query)
            }
        }
        webSearch.find(t)?.let { return PhoneCommand.WebSearch(it.groupValues[1].trim()) }

        music.find(t)?.let { m ->
            val q = m.groupValues[1].trim().removePrefix("bana ").trim()
            if (q in setOf("müzik", "muzik", "bir şarkı", "bir sarki", "şarkı", "sarki", "bir şey", "biraz müzik", "bir müzik", "müzik bir şey")) return PhoneCommand.OpenApp("müzik")
            if (q.isNotEmpty() && !has(q, "alarm", "zil", "oyun")) return PhoneCommand.PlayMusic(q)
        }

        openAtEnd.find(t)?.let { m ->
            val name = m.groupValues[1].trim().removePrefix("hey ").trim()
            if (name.split(' ').size > 3) return null
            if (ownWords.any { name.startsWith(it) }) return null
            return PhoneCommand.OpenApp(name)
        }
        return null
    }

    private fun settingsPage(t: String): PhoneCommand? {
        // "ayarla" (set) is a different word: "alarm ayarla" must not open the settings.
        val settings = word(t, "ayarlar\\p{L}*|ayar[ıi]|ayar[ıi]n[ıi]|ayar sayfas\\p{L}*")
        val toggle = openVerb.containsMatchIn(t) || offVerb.containsMatchIn(t)
        val page = when {
            has(t, "wifi", "wi-fi", "wi fi", "kablosuz") -> "wifi"
            has(t, "bluetooth", "blutut", "bluetut") -> "bluetooth"
            has(t, "mobil veri", "hücresel veri", "interneti", "internet bağlantı") -> "internet"
            word(t, "konum\\p{L}*|gps") -> "location"
            has(t, "uçak modu", "ucak modu") -> "airplane"
            has(t, "rahatsız etme", "rahatsiz etme") -> "dnd"
            word(t, "nfc") -> "nfc"
            settings && has(t, "ses") -> "sound"
            settings && has(t, "ekran") -> "display"
            settings && has(t, "pil", "batarya", "şarj") || has(t, "pil tasarruf", "güç tasarruf") -> "battery"
            settings && has(t, "depolama", "hafıza") -> "storage"
            settings && !has(t, "uygulama ayar", "izin", "obi") -> "main"
            else -> null
        } ?: return null
        // A page named without "ayar" counts only with a clear on/off verb ("bluetooth'u aç").
        if (!settings && !toggle && page != "dnd" && page != "airplane") return null
        return PhoneCommand.SettingsPage(page, wantsToggle = toggle && !settings)
    }

    private val aliases = mapOf(
        "vatsap" to "whatsapp", "vatsapp" to "whatsapp", "watsap" to "whatsapp", "watsapp" to "whatsapp", "whatsap" to "whatsapp", "wp" to "whatsapp",
        "yutup" to "youtube", "yutub" to "youtube", "yutüp" to "youtube", "youtub" to "youtube", "yutubu" to "youtube", "yt" to "youtube",
        "insta" to "instagram", "ınstagram" to "instagram", "instegram" to "instagram",
        "feysbuk" to "facebook", "fesbuk" to "facebook", "face" to "facebook",
        "tik tok" to "tiktok", "spotifay" to "spotify", "krom" to "chrome", "cimeyl" to "gmail", "cimeil" to "gmail",
        "tivitır" to "twitter", "tvitır" to "twitter", "netfliks" to "netflix", "telegıram" to "telegram",
        "play store" to "play store", "play market" to "play store", "google play" to "play store",
    )

    private val categories = listOf(
        listOf("galeri", "fotoğraflar", "fotograflar", "resimler") to ("gallery" to "Galeri"),
        listOf("müzik", "muzik", "müzikler") to ("music" to "Müzik"),
        listOf("takvim") to ("calendar" to "Takvim"),
        listOf("hesap makinesi", "hesap makinası") to ("calculator" to "Hesap makinesi"),
        listOf("rehber", "kişiler", "kisiler") to ("contacts" to "Rehber"),
        listOf("mesajlar", "mesaj", "sms") to ("messaging" to "Mesajlar"),
        listOf("telefon", "arama", "tuş takımı") to ("dialer" to "Telefon"),
        listOf("harita", "haritalar") to ("maps" to "Haritalar"),
        listOf("tarayıcı", "tarayici", "internet tarayıcısı") to ("browser" to "Tarayıcı"),
        listOf("e-posta", "eposta", "posta", "mail", "e posta") to ("email" to "E-posta"),
        listOf("saat", "alarmlar", "kronometre") to ("clock" to "Saat"),
    )

    /** "instagramı" → instagram; "kamerayı" → kamera; "youtube'u" → youtube. */
    fun stem(name: String): String {
        val n = lower(name).replace('’', '\'')
        if (n.contains('\'')) return n.substringBefore('\'').trim()
        return n.replace(Regex("(?:y[ıiuü]|n[ıiuü]|[ıiuü])$"), "").takeIf { it.length >= 3 } ?: n
    }

    fun resolve(name: String): AppTarget {
        val n = lower(name).replace('’', '\'')
        for (candidate in listOf(n, stem(n))) {
            categories.firstOrNull { (words, _) -> candidate in words }?.let { return AppTarget.Category(it.second.first, it.second.second) }
            aliases[candidate]?.let { return AppTarget.Named(it) }
            aliases[candidate.replace(" ", "")]?.let { return AppTarget.Named(it) }
        }
        return AppTarget.Named(stem(n))
    }

    // Dotless ı counts as i: Turkish lower case turns "Instagram" into "ınstagram".
    private fun squash(text: String) = lower(text).replace('ı', 'i').replace(Regex("[^\\p{L}\\p{N}]"), "")

    /**
     * The best installed app for [term] among (label, id) pairs: the same name first, then a
     * name the term starts with ("instagramı" → Instagram), then one that starts with the term.
     */
    fun bestMatch(term: String, apps: List<Pair<String, String>>): String? {
        val want = squash(term)
        if (want.length < 2) return null
        val named = apps.map { squash(it.first) to it.second }.filter { it.first.isNotEmpty() }
        named.firstOrNull { it.first == want }?.let { return it.second }
        named.filter { want.startsWith(it.first) && it.first.length >= 3 && want.length - it.first.length <= 4 }
            .maxByOrNull { it.first.length }?.let { return it.second }
        named.filter { it.first.length >= 5 && want.contains(it.first) }.maxByOrNull { it.first.length }?.let { return it.second }
        if (want.length >= 4) named.filter { it.first.startsWith(want) }.minByOrNull { it.first.length }?.let { return it.second }
        if (want.length >= 5) named.filter { it.first.contains(want) }.minByOrNull { it.first.length }?.let { return it.second }
        return null
    }
}
