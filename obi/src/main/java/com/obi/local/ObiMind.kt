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
        if (queue.isEmpty()) queue.addAll(options.shuffled())
        return queue.removeFirst().replace("{isim}", prefs.getString("name", "")?.let { if (it.isBlank()) "" else "$it, " } ?: "")
    }

    fun touch(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour >= 23 || hour < 7 -> next("night", listOf(
                "Geç oldu; gözlerine biraz dinlenme borçluyuz 🌙", "Ben burada nöbetteyim, sen dinlenebilirsin.",
                "Bugünün yükünü burada bırakalım. Yarın yeni bir sayfa açarız.", "Uykun geldiyse ekranı kapat, rüyalarında görüşürüz ✨"))
            hour < 11 -> next("morning", listOf(
                "{isim}Bugün her şeyi yetiştirmek zorunda değilsin. Bir şey seç, onunla başla.",
                "Günaydın! İlk küçük adımı birlikte atalım mı?", "Güne bir nefes ve bir bardak suyla başlayalım ☀️",
                "Dünkü emeğini küçümseme; bugün onun üzerine ekleyebilirsin."))
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
        if (t.contains("ne yapabilir") || t == "yardım") return "Not alırım, alarm ve hatırlatma kurarım, doğum gününü hatırlarım, metin çeviririm. Gemini bağlanırsa seninle serbestçe yazışırım."
        return next("open", listOf(
            "Bunu biraz açar mısın? Seni doğru anlamak istiyorum.",
            "İlginç! Bunun sende bıraktığı hissi anlatır mısın?",
            "Aklında bir hedef varsa onu küçük parçalara ayırabiliriz.",
            "Bu konuda ne yapmak istediğini söyle, birlikte başlayalım."))
    }
}

/** Personal-test key is supplied by the owner, encrypted with Android Keystore, never in the APK. */
internal data class ObiReply(val text: String, val action: WordSiegeMascotAction = WordSiegeMascotAction.NOD)

internal class ObiGemini(private val context: Context) {
    private val prefs = context.getSharedPreferences("obi_gemini", Context.MODE_PRIVATE)
    private val alias = "obi_personal_gemini_key"
    private val model = "gemini-3.5-flash-lite"
    private val history = ArrayDeque<Pair<String, String>>()

    fun connected(): Boolean = readKey() != null

    fun saveKey(raw: String): Boolean = runCatching {
        val key = raw.trim()
        require(key.length in 20..300 && key.none(Char::isWhitespace))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        prefs.edit().putString("key", Base64.encodeToString(cipher.doFinal(key.toByteArray()), Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).apply()
        true
    }.getOrDefault(false)

    fun disconnect() { prefs.edit().remove("key").remove("iv").apply(); history.clear() }

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

    fun ask(prompt: String, done: (ObiReply) -> Unit) {
        val key = readKey() ?: return done(ObiReply("Gemini bağlanmadı. ‘Gemini bağla’ düğmesine bas."))
        val today = LocalDate.now().toString()
        val used = if (prefs.getString("day", "") == today) prefs.getInt("used", 0) else 0
        if (used >= 20) return done(ObiReply("Bugünlük 20 Gemini isteğini kullandık. Yarın yeniden konuşuruz; yerel not ve alarmlar çalışmaya devam eder."))
        Thread {
            val answer = runCatching {
                val contents = JSONArray()
                history.takeLast(6).forEach { (role, text) -> contents.put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", text)))) }
                contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt.take(1500)))))
                val body = JSONObject()
                    .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                        "Adın Obi. Türkçe yazışan şirin bir telefon maskotusun. Ses kullanma. Kullanıcının bağlamını önceki konuşmadan izle; farklı, doğal ve somut yanıtlar ver. Çeviri, açıklama, özetleme, yazı yazma, fikir üretme ve sohbet isteklerini karşıla. Sadece sohbet dışında telefondaki eylemleri yaptığını iddia etme; not/alarm/hatırlatıcıyı uygulama yerel olarak ele alır. Ekranı gördüğünü veya başka uygulamaları kontrol ettiğini söyleme. JSON nesnesi dön: text kullanıcıya yanıt, action ise yanıtın duygusuna uygun tek bir hareket. action değerleri: NOD, THINK, CHEER, DANCE, WAVE, LOVE, GIGGLE, SHY, WINK, SPARKLE, CLAP, SHRUG, YAWN, DOZE, POINT, BOUNCE. Kısa bir sohbet yanıtında da eylemi duyguya göre değiştir."))))
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
                    synchronized(history) {
                        history.addLast("user" to prompt.take(1500)); history.addLast("model" to reply.take(1000))
                        while (history.size > 6) history.removeFirst()
                    }
                    prefs.edit().putString("day", today).putInt("used", used + 1).apply()
                    ObiReply(reply, action)
                } finally { connection.disconnect() }
            }.getOrElse { ObiReply("Bağlantı kurulamadı. İnterneti kontrol edip yeniden dene.", WordSiegeMascotAction.SHRUG) }
            android.os.Handler(android.os.Looper.getMainLooper()).post { done(answer) }
        }.start()
    }
}
