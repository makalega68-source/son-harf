package com.obi.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.time.LocalDate
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Short, contextual local responses. Each group is shuffled before it repeats. */
internal class ObiMind(private val context: Context) {
    private val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)
    private val pools = mutableMapOf<String, ArrayDeque<String>>()
    private val words = mapOf(
        "hello" to "merhaba", "love" to "sevgi", "good" to "iyi", "morning" to "sabah",
        "teşekkür" to "Rica ederim!", "özür" to "Sorun değil, birlikte devam ederiz."
    )

    private fun next(group: String, options: List<String>): String {
        val queue = pools.getOrPut(group) { ArrayDeque() }
        if (queue.isEmpty()) {
            val previous = prefs.getString("last_$group", "") ?: ""
            queue.addAll(options.filterNot { it == previous }.shuffled())
            options.find { it == previous }?.let(queue::addLast)
        }
        val selected = queue.removeFirst()
        prefs.edit().putString("last_$group", selected).apply()
        return selected.replace("{isim}", prefs.getString("name", "")?.let { if (it.isBlank()) "" else "$it, " } ?: "")
    }

    fun touch(): String {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minuteOfDay = hour * 60 + calendar.get(java.util.Calendar.MINUTE)
        return when {
            ObiLife.isNight(minuteOfDay, prefs.getInt("bedtime", 23 * 60)) -> next("night", listOf(
                "Geç oldu; gözlerine biraz dinlenme borçluyuz 🌙", "Ben burada nöbetteyim, sen dinlenebilirsin.",
                "Bugünün yükünü burada bırakalım. Yarın yeni bir sayfa açarız.", "Uykun geldiyse ekranı kapat, rüyalarında görüşürüz ✨",
                "Psst! Gece sessiz, ben hâlâ buradayım.", "Ay ışığında bile parlıyorsun ✨",
                "Biraz esneyip yıldız sayalım mı?", "Gece keşiflerine çıktım; ilk durağım senin ekranın!",
                "Dokundun, uykum kaçtı. Merhaba!", "Az önce küçük bir rüya gördüm; sen de vardın.",
                "Karanlıkta bile gülümsemeni gördüm sanki.", "Benim için gece, fısıltıyla söylenen bir merhaba.",
                "Uyumadıysan iki kelime sohbet edebiliriz.", "Sana minik bir yıldız bıraktım ⭐",
                "Bir dokunuşla uyandım, şimdi gözlerim sende.", "Yarın için aklında güzel bir şey var mı?"))
            hour < 11 -> next("morning", listOf(
                "{isim}Bugün her şeyi yetiştirmek zorunda değilsin. Bir şey seç, onunla başla.",
                "Günaydın! İlk küçük adımı birlikte atalım mı?", "Güne bir nefes ve bir bardak suyla başlayalım ☀️",
                "Dünkü emeğini küçümseme; bugün onun üzerine ekleyebilirsin.",
                "Gün başladı! Küçük bir dans mı etsek?", "Bugünün ilk gülümsemesini yakaladım galiba!",
                "Merhaba sabah kuşu! Ben hazırım.", "Yeni güne bir selam: Obi, Obi!",
                "Gözlerini açtın mı? Benimkiler kocaman açık!", "Bugün nasıl bir macera seçiyorsun?"))
            else -> next("touch", listOf(
                "{isim}Bana dokundun, geldim! Ne var aklında?", "Bir fikrin mi var? Anlat, birlikte şekillendirelim.",
                "Yorulduysan iki dakika durabiliriz. Kaçmıyorum 💜", "Bugün kendin için yaptığın küçük bir şey var mı?",
                "Bazen en iyi plan, bir sonraki tek adımı seçmektir.", "Merak ettim: gününün en güzel anı neydi?",
                "Hadi omuzlarını gevşet. Ben de esneyeyim!", "Bir şey yolunda gitmediyse yeniden deneyebiliriz.",
                "Sen yazarken ben burada bekliyorum, acelem yok.", "İhtiyacın olursa notunu ve planını birlikte toparlarız.",
                "Haydi küçük bir mola. Sonra kaldığımız yerden devam ederiz.", "Bugün kendine söylediğin en güzel söz neydi?"))
        }
    }

    fun respond(raw: String): String {
        val t = raw.trim().lowercase(Locale("tr", "TR"))
        if (t.isBlank()) return touch()
        val name = Regex("(?:benim adım|adım)\\s+([\\p{L}]{2,25})").find(t)?.groupValues?.get(1)
        if (name != null) {
            prefs.edit().putString("name", name.replaceFirstChar { it.titlecase(Locale("tr", "TR")) }).apply()
            return "Tanıştığımıza sevindim ${name.replaceFirstChar { it.titlecase(Locale("tr", "TR")) }}! Sana böyle sesleneceğim."
        }
        if (t.contains("motiv") || t.contains("cesaret") || t.contains("güzel söz")) return next("motivation", listOf(
            "Bütün yolu bugün yürümene gerek yok. İlk adımı seçmen yeterli.",
            "Zorlandığın yerde durup nefes almak da ilerlemenin bir parçası.",
            "Bir işi iyi yapmak için önce onu kusursuz başlatman gerekmiyor.",
            "Düne göre küçücük bir gelişme bile emeğinin karşılığıdır.",
            "Bugün yapabileceğin tek şeyi seç. Gerisi sırasını bekleyebilir.",
            "Bir hata seni tanımlamaz; ondan sonra ne yaptığın önemlidir.",
            "Senin hızın sana ait. Başkasının takvimine yetişmek zorunda değilsin."))
        if (listOf("üzgün", "mutsuz", "kötü hissed", "canım sıkkın").any(t::contains)) return next("sad", listOf(
            "Üzüldüğünü duydum. İstersen ne olduğunu anlat; dinliyorum.",
            "Bugün zor geçmiş gibi. Tek bir şey düzeltmek gerekse hangisi olurdu?",
            "Şu an çözüm aramak istemiyorsan sadece yanında durabilirim."))
        if (listOf("yoruldum", "bunaldım", "stres", "bıktım").any(t::contains)) return next("tired", listOf(
            "Bir dakika mola verelim. Sonra en kolay işe birlikte bakalım.",
            "Yükün ağır gelmiş. Şu an erteleyebileceğin bir şey var mı?",
            "Önce nefes al. Yapacaklarını üç kısa maddeye ayırabiliriz."))
        if (listOf("başardım", "kazandım", "harika haber", "mutluyum").any(t::contains)) return next("happy", listOf(
            "İşte bu! Emeğinin karşılığını kutlayalım 🎉", "Vay canına! En çok hangi kısmıyla gurur duyuyorsun?",
            "Bunu duymak güzel. Küçük bir zafer dansı hak ettik!"))
        if (listOf("selam", "merhaba", "günaydın", "iyi akşamlar").any(t::contains)) return next("hello", listOf(
            "Merhaba! Bugün nasıl gidiyor?", "Selam! Bir şey planlamak mı istersin, biraz sohbet mi?",
            "Buradayım. Aklındaki ilk şeyi yazabilirsin."))
        if (listOf("teşekkür", "sağ ol", "eyvallah").any(t::contains)) return next("thanks", listOf(
            "Ne demek! İşine yaradıysa sevindim.", "Her zaman. Başka bir şeye bakalım mı?", "Rica ederim 💜"))
        if (t.contains("ne yapabilir") || t == "yardım") return "Not alırım; alarm, tekrarlayan hatırlatma ve odak sayacı kurarım; alışveriş listeni, harcamalarını, doğum günlerini ve geri sayımlarını tutarım; hava durumunu söylerim; her gün İngilizce kelime öğretirim; seninle oyun oynarım. Gemini bağlanırsa serbestçe yazışır, panodaki metni özetler ya da çeviririm."
        return next("open", listOf(
            "Bunu biraz açar mısın? Seni doğru anlamak istiyorum.",
            "İlginç! Bunun sende bıraktığı hissi anlatır mısın?",
            "Aklında bir hedef varsa onu küçük parçalara ayırabiliriz.",
            "Bu konuda ne yapmak istediğini söyle, birlikte başlayalım."))
    }
}

/** Personal-test key is supplied by the owner, encrypted with Android Keystore, never in the APK. */
internal data class ObiReply(
    val text: String,
    val action: WordSiegeMascotAction = WordSiegeMascotAction.NOD,
    /** A phone task Gemini recognised in free text (see [ObiGemini.TOOLS]); the app performs it locally. */
    val intent: String? = null,
    val args: JSONObject? = null,
)

internal class ObiGemini(private val context: Context) {
    companion object {
        const val DAILY_LIMIT = 150

        /** Phone tasks Gemini may hand back to the app; the app, not Gemini, carries them out. */
        const val TOOLS = "Telefonda yapılacak bir iş istenirse intent ve args alanlarını doldur, yoksa intent \"none\" olsun. " +
            "intent değerleri ve args: list_add {items:[metinler]}, list_show {}, list_remove {item}, list_clear {}, " +
            "note_add {text}, notes_show {}, reminder {datetime:\"YYYY-MM-DDTHH:MM\", text, repeat: \"daily\"|\"weekdays\"|null}, " +
            "alarm {datetime, repeat}, alarms_show {}, weather {city}, expense_add {label, amount}, expenses_show {}, " +
            "countdown_add {label, date:\"YYYY-MM-DD\"}, countdowns_show {}, birthday_add {person, day, month}, focus {minutes}, " +
            "game {name: \"guess\"|\"riddle\"|\"quiz\"|\"catch\"}, wardrobe {}, mood {score:1-5}, words_today {}. " +
            "Örnek: ‘süt ekmek al’ → list_add; ‘listem’ / ‘listemi getir’ → list_show; ‘Arnavutköy'de hava’ → weather. " +
            "Bir iş yapıldığında text kısa bir onay olsun; sonucu uygulama gösterecek. Çeviri, sohbet ve sorular için intent none."
    }

    private val prefs = context.getSharedPreferences("obi_gemini", Context.MODE_PRIVATE)
    private val alias = "obi_personal_gemini_key"
    private val model = "gemini-3.5-flash-lite"
    private val history = ArrayDeque<Pair<String, String>>().apply {
        // The last few turns survive restarts so a conversation can continue.
        runCatching {
            val saved = JSONArray(prefs.getString("history", "[]") ?: "[]")
            for (i in 0 until saved.length()) saved.optJSONObject(i)?.let { addLast(it.optString("role") to it.optString("text")) }
        }
    }

    private fun saveHistory() {
        val array = JSONArray()
        synchronized(history) { history.forEach { (role, text) -> array.put(JSONObject().put("role", role).put("text", text)) } }
        prefs.edit().putString("history", array.toString()).apply()
    }

    fun connected(): Boolean = readKey() != null

    /** For other on-device Gemini features (the voice); never logged or sent anywhere but Google. */
    internal fun apiKey(): String? = readKey()

    fun saveKey(raw: String): Boolean = runCatching {
        val key = raw.trim()
        require(key.length in 20..300 && key.none(Char::isWhitespace))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        prefs.edit().putString("key", Base64.encodeToString(cipher.doFinal(key.toByteArray()), Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).apply()
        true
    }.getOrDefault(false)

    fun disconnect() { prefs.edit().remove("key").remove("iv").remove("history").apply(); synchronized(history) { history.clear() } }

    fun forgetConversation() { synchronized(history) { history.clear() }; prefs.edit().remove("history").apply() }

    private fun usedToday(): Int = if (prefs.getString("day", "") == LocalDate.now().toString()) prefs.getInt("used", 0) else 0

    @Synchronized private fun countRequest() {
        prefs.edit().putString("day", LocalDate.now().toString()).putInt("used", usedToday() + 1).apply()
    }

    private fun post(key: String, body: JSONObject): Pair<Int, JSONObject?> {
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 6000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", key)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) return status to null
            return status to JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    private fun firstText(root: JSONObject): String {
        val parts = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        return (0 until parts.length()).mapNotNull { parts.getJSONObject(it).optString("text").takeIf(String::isNotBlank) }.joinToString("\n")
    }

    /**
     * When the local parser cannot read a time ("cumartesi öğleden sonra üç buçukta"), Gemini turns the sentence
     * into a date and a short label. The alarm itself is still set by the phone.
     */
    fun extractSchedule(command: String, done: (Long?, String?) -> Unit) {
        val key = readKey() ?: return done(null, null)
        if (usedToday() >= DAILY_LIMIT) return done(null, null)
        Thread {
            val result: Pair<Long?, String?> = runCatching {
                val now = java.time.LocalDateTime.now().withSecond(0).withNano(0)
                val instruction = "Şu an $now (${now.dayOfWeek}). Kullanıcı Türkçe bir alarm ya da hatırlatma istiyor: \"${command.take(300)}\". " +
                    "JSON döndür: {\"datetime\": \"YYYY-MM-DDTHH:MM\" ya da null, \"text\": kısa hatırlatma metni (alarm ise \"Alarm\")}. Geçmiş bir zaman verme."
                val body = JSONObject()
                    .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", instruction)))))
                    .put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 0.1)
                        .put("maxOutputTokens", 200).put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal")))
                val (_, root) = post(key, body)
                if (root == null) return@runCatching Pair<Long?, String?>(null, null)
                countRequest()
                val parsed = JSONObject(firstText(root))
                val time = parsed.optString("datetime").takeIf { it.length >= 16 }
                    ?.let { java.time.LocalDateTime.parse(it.take(16)) }
                    ?.takeIf { it.isAfter(now) }
                    ?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                Pair<Long?, String?>(time, parsed.optString("text").takeIf { it.isNotBlank() }?.take(80))
            }.getOrDefault(Pair<Long?, String?>(null, null))
            android.os.Handler(android.os.Looper.getMainLooper()).post { done(result.first, result.second) }
        }.start()
    }

    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private fun readKey(): String? = runCatching {
        val encoded = prefs.getString("key", null) ?: return null
        val iv = Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)))
    }.getOrNull()

    fun ask(prompt: String, tools: Boolean = false, done: (ObiReply) -> Unit) {
        val key = readKey() ?: return done(ObiReply("Gemini bağlanmadı. ‘Gemini bağla’ düğmesine bas."))
        val today = LocalDate.now().toString()
        val used = if (prefs.getString("day", "") == today) prefs.getInt("used", 0) else 0
        if (used >= DAILY_LIMIT) return done(ObiReply("Bugünlük Gemini hakkımızı kullandık. Yarın yeniden konuşuruz; not, liste ve alarmlar yine çalışır."))
        Thread {
            val answer = runCatching {
                val contents = JSONArray()
                synchronized(history) { history.toList() }.takeLast(6).forEach { (role, text) -> contents.put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", text)))) }
                contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt.take(1500)))))
                val body = JSONObject()
                    .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                        "Adın Obi. Türkçe yazışan şirin bir telefon maskotusun. Ses kullanma. Kullanıcının bağlamını önceki konuşmadan izle; farklı, doğal ve somut yanıtlar ver. Çeviri, açıklama, özetleme, yazı yazma, fikir üretme ve sohbet isteklerini karşıla. Sadece sohbet dışında telefondaki eylemleri yaptığını iddia etme; not/alarm/hatırlatıcıyı uygulama yerel olarak ele alır. Ekranı gördüğünü veya başka uygulamaları kontrol ettiğini söyleme. JSON nesnesi dön: text kullanıcıya yanıt, action ise yanıtın duygusuna uygun tek bir hareket. action değerleri: NOD, THINK, CHEER, DANCE, WAVE, LOVE, GIGGLE, SHY, WINK, SPARKLE, CLAP, SHRUG, YAWN, DOZE, POINT, BOUNCE. Kısa bir sohbet yanıtında da eylemi duyguya göre değiştir. Yanıtların kısa ve konuşma balonuna sığacak gibi olsun." +
                        (if (tools) " Şu an ${java.time.LocalDateTime.now().withSecond(0).withNano(0)} (${java.time.LocalDate.now().dayOfWeek}). " + TOOLS else "")))))
                    .put("contents", contents)
                    .put("generationConfig", JSONObject().put("maxOutputTokens", 900).put("temperature", 0.8)
                        .put("responseMimeType", "application/json")
                        .put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal")))
                val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 6000
                    connection.readTimeout = 15000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("x-goog-api-key", key)
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val status = connection.responseCode
                    if (status == 429) return@runCatching ObiReply("Gemini sınırına ulaştı. Bir süre sonra tekrar dene.", WordSiegeMascotAction.SHRUG)
                    if (status == 401 || status == 403) return@runCatching ObiReply("Gemini anahtarı kabul edilmedi. Bağlantıyı kontrol et.", WordSiegeMascotAction.SHRUG)
                    if (status !in 200..299) return@runCatching ObiReply("Gemini şu anda yanıt vermedi (kod $status).", WordSiegeMascotAction.SHRUG)
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val parts = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                    val rawReply = (0 until parts.length()).mapNotNull { parts.getJSONObject(it).optString("text").takeIf(String::isNotBlank) }.joinToString("\n")
                    val parsed = runCatching { JSONObject(rawReply) }.getOrNull()
                    val reply = parsed?.optString("text")?.takeIf(String::isNotBlank) ?: rawReply.ifBlank { "Şu an yanıt oluşturamadım." }
                    val allowed = setOf("NOD", "THINK", "CHEER", "DANCE", "WAVE", "LOVE", "GIGGLE", "SHY", "WINK", "SPARKLE", "CLAP", "SHRUG", "YAWN", "DOZE", "POINT", "BOUNCE")
                    val chosen = parsed?.optString("action")?.uppercase(Locale.ROOT)?.takeIf(allowed::contains)
                    val action = chosen?.let { WordSiegeMascotAction.valueOf(it) } ?: WordSiegeMascotAction.NOD
                    val intent = parsed?.optString("intent")?.takeIf { tools && it.isNotBlank() && it != "none" && it != "null" }
                    synchronized(history) {
                        history.addLast("user" to prompt.take(1500)); history.addLast("model" to reply.take(1000))
                        while (history.size > 6) history.removeFirst()
                    }
                    saveHistory()
                    prefs.edit().putString("day", today).putInt("used", used + 1).apply()
                    ObiReply(reply, action, intent, parsed?.optJSONObject("args"))
                } finally { connection.disconnect() }
            }.getOrElse { ObiReply("Bağlantı kurulamadı. İnterneti kontrol edip yeniden dene.", WordSiegeMascotAction.SHRUG) }
            android.os.Handler(android.os.Looper.getMainLooper()).post { done(answer) }
        }.start()
    }

    /** Prepare a small bank of distinct tap reactions in one request, keeping taps instant. */
    fun touchPack(hour: Int, recent: List<String>, done: (List<ObiReply>) -> Unit) {
        val key = readKey() ?: return done(emptyList())
        val today = LocalDate.now().toString()
        val sameDay = prefs.getString("day", "") == today
        val used = if (sameDay) prefs.getInt("used", 0) else 0
        val packs = if (prefs.getString("touch_day", "") == today) prefs.getInt("touch_packs", 0) else 0
        if (used >= DAILY_LIMIT || packs >= 4) return done(emptyList())
        Thread {
            val replies = runCatching {
                val instruction = "Obi adlı sevimli telefon maskotu için dokunma tepkileri üret. Saat $hour. Gece olsa bile sürekli uyku öğüdü verme. " +
                    "Sekiz farklı, kısa, oyuncu, sıcak Türkçe söz yaz; her biri en çok 90 karakter. Aynı fikri yineleme. " +
                    "Son kullanılan sözler: ${recent.takeLast(5).joinToString(" / ")}. " +
                    "JSON nesnesi döndür: replies dizisindeki her öğede text ve action olsun. " +
                    "action değerleri WAVE, BOUNCE, WINK, GIGGLE, LOVE, CHEER, DANCE, PEEK, SHY, HOP, SPARKLE, NOD."
                val body = JSONObject()
                    .put("contents", JSONArray().put(JSONObject().put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", instruction)))))
                    .put("generationConfig", JSONObject().put("responseMimeType", "application/json")
                        .put("temperature", 1.0).put("maxOutputTokens", 1400)
                        .put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal")))
                val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 6000; connection.readTimeout = 15000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("x-goog-api-key", key)
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    if (connection.responseCode !in 200..299) return@runCatching emptyList<ObiReply>()
                    val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val raw = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0).getString("text")
                    val items = JSONObject(raw).getJSONArray("replies")
                    val actions = setOf("WAVE", "BOUNCE", "WINK", "GIGGLE", "LOVE", "CHEER", "DANCE", "PEEK", "SHY", "HOP", "SPARKLE", "NOD")
                    val generated = (0 until items.length()).mapNotNull { index ->
                        val item = items.optJSONObject(index) ?: return@mapNotNull null
                        val phrase = item.optString("text").trim().take(90).takeIf { it.length in 5..90 } ?: return@mapNotNull null
                        val move = item.optString("action").uppercase(Locale.ROOT).takeIf(actions::contains) ?: "WAVE"
                        ObiReply(phrase, WordSiegeMascotAction.valueOf(move))
                    }.distinctBy { it.text.lowercase(Locale("tr", "TR")) }.take(8)
                    if (generated.isNotEmpty()) prefs.edit().putString("day", today)
                        .putString("touch_day", today).putInt("used", used + 1)
                        .putInt("touch_packs", packs + 1).apply()
                    generated
                } finally { connection.disconnect() }
            }.getOrDefault(emptyList())
            android.os.Handler(android.os.Looper.getMainLooper()).post { done(replies) }
        }.start()
    }
}
