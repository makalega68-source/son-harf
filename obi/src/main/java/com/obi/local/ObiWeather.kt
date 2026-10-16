package com.obi.local

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Weather is fetched only for a city the user names; no device location permission. */
internal class ObiWeather {
    fun canHandle(text: String): Boolean = ObiParse.isWeatherQuestion(text)

    fun ask(text: String, savedCity: String?, done: (String) -> Unit) {
        val candidates = ObiParse.cityCandidates(text).ifEmpty { listOfNotNull(savedCity) }
        if (candidates.isEmpty()) return done("Hangi şehirdeki havayı merak ettin? Örnek: ‘İstanbul'da hava nasıl?’ (‘Şehrim İzmir’ yazarsan hep onu kullanırım.)")
        Thread {
            val answer = blocking(candidates) ?: "‘${candidates.first()}’ için hava bilgisi alamadım. Şehir adını ve bağlantını kontrol edip yeniden dene."
            Handler(Looper.getMainLooper()).post { done(answer) }
        }.start()
    }

    /** Current weather for the first city that resolves, or null. Runs on the calling (background) thread. */
    fun blocking(candidates: List<String>): String? = runCatching {
        val location = candidates.firstNotNullOfOrNull { name ->
            read("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(name, "UTF-8")}&count=1&language=tr&format=json")
                .optJSONArray("results")?.optJSONObject(0)
        } ?: return@runCatching null
        val lat = location.getDouble("latitude")
        val lon = location.getDouble("longitude")
        val current = read("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,apparent_temperature,weather_code&timezone=auto")
            .getJSONObject("current")
        val degrees = current.getDouble("temperature_2m").toInt()
        val feels = current.getDouble("apparent_temperature").toInt()
        val code = current.getInt("weather_code")
        val (sky, tip) = when (code) {
            0 -> "açık ☀️" to "Güneş gözlüğünü unutma."
            1, 2 -> "az bulutlu ⛅" to ""
            3 -> "kapalı ☁️" to ""
            45, 48 -> "sisli 🌫" to "Yolda dikkatli ol."
            in 51..67, in 80..82 -> "yağışlı 🌧" to "Şemsiyeni al ☔"
            in 71..77, 85, 86 -> "karlı ❄️" to "Sıkı giyin!"
            95, 96, 99 -> "gök gürültülü ⛈" to "Mümkünse içeride kal."
            else -> "değişken" to ""
        }
        "${location.optString("name", candidates.first())}: şimdi $degrees°C, $sky. Hissedilen $feels°C. $tip".trim() + "\nKaynak: Open-Meteo"
    }.getOrNull()

    private fun read(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            check(connection.responseCode in 200..299)
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }
}
