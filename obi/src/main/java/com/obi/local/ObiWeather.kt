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

    fun ask(text: String, done: (String) -> Unit) {
        val candidates = ObiParse.cityCandidates(text)
        val city = candidates.firstOrNull()
        if (city == null) return done("Hangi şehirdeki havayı merak ettin? Örnek: ‘İstanbul'da hava nasıl?’")
        Thread {
            val answer = runCatching {
                val location = candidates.firstNotNullOfOrNull { name ->
                    read("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(name, "UTF-8")}&count=1&language=tr&format=json")
                        .optJSONArray("results")?.optJSONObject(0)
                } ?: return@runCatching "‘$city’ için bir konum bulamadım. Şehir adını yeniden yazabilir misin?"
                val lat = location.getDouble("latitude")
                val lon = location.getDouble("longitude")
                val current = read("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,apparent_temperature,weather_code&timezone=auto")
                    .getJSONObject("current")
                val degrees = current.getDouble("temperature_2m").toInt()
                val feels = current.getDouble("apparent_temperature").toInt()
                val code = current.getInt("weather_code")
                val sky = when (code) {
                    0 -> "açık"
                    1, 2 -> "az bulutlu"
                    3 -> "kapalı"
                    45, 48 -> "sisli"
                    in 51..67, in 80..82 -> "yağışlı"
                    in 71..77, 85, 86 -> "karlı"
                    95, 96, 99 -> "gök gürültülü"
                    else -> "değişken"
                }
                "${location.optString("name", city)} için şimdi $degrees°C, $sky. Hissedilen $feels°C. ☁\nKaynak: Open-Meteo"
            }.getOrElse { "Canlı hava verisine erişemedim. Bağlantını kontrol edip yeniden dene." }
            Handler(Looper.getMainLooper()).post { done(answer) }
        }.start()
    }

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
