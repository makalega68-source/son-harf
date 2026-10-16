package com.obi.local

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** A scheduled alarm, reminder, timer or focus session as kept in preferences. */
internal data class ObiAlarm(val id: Int, val time: Long, val text: String, val kind: String, val repeat: String?, val hour: Int, val minute: Int)

/** Everything Obi keeps on the phone: notes, alarms, lists, money, dates, mood and bond. Nothing leaves the device. */
internal class ObiStore(private val context: Context) {
    companion object {
        const val ACTION_DAILY = "com.obi.local.DAILY"
        const val ACTION_EVENING = "com.obi.local.EVENING"
        const val ACTION_WATER = "com.obi.local.WATER"
        const val ACTION_SNOOZE = "com.obi.local.SNOOZE"
        const val ACTION_NUDGE = "com.obi.local.NUDGE"
        const val ACTION_ASK_MOOD = "com.obi.local.ASK_MOOD"
        const val ACTION_CELEBRATE = "com.obi.local.CELEBRATE"
        const val ACTION_LOOK_CHANGED = "com.obi.local.LOOK_CHANGED"
        private const val BOND_DAILY_CAP = 15
    }

    private val prefs = context.getSharedPreferences("obi_local", Context.MODE_PRIVATE)
    private val mind = ObiMind(context)
    private val tr = Locale("tr", "TR")

    /** Set by the last [handle] call when an alarm could only be scheduled inexactly. */
    var needsExactPermission = false
        private set

    fun canHandle(raw: String): Boolean {
        if (ObiParse.isLocalCommand(raw)) return true
        val t = ObiLife.lower(raw)
        val list = ObiLife.listCommand(t)
        if (list is ObiLife.ListCommand.Remove) return shopping().any { ObiLife.lower(it) == ObiLife.lower(list.item) }
        return list != null || ObiLife.isAlarmList(t) || ObiLife.alarmToDelete(t) != null ||
            ObiLife.noteToDelete(t) != null || ObiLife.noteSearch(t) != null || ObiLife.expense(raw) != null ||
            ObiLife.isExpenseSummary(t) || ObiLife.otherBirthday(t) != null || ObiLife.isBirthdayList(t) ||
            ObiLife.countdown(t) != null || ObiLife.countdownQuery(t) != null || ObiLife.savedCity(t) != null ||
            ObiLife.bedtime(t) != null || isToggle(t) || t.contains("haftam nasıl") || t.contains("haftam nasil") ||
            t.contains("bugünün kelime") || t.contains("bugunun kelime") || t.contains("kelimeleri göster")
    }

    private fun isToggle(t: String): Boolean =
        (t.contains("su hatırlat") || t.contains("su hatirlat") || t.contains("mola hatırlat") || t.contains("mola hatirlat")) &&
            (t.contains("aç") || t.contains("kapat") || t.contains("ac"))

    fun handle(raw: String): String {
        needsExactPermission = false
        val text = raw.trim()
        val normalized = ObiLife.lower(text)

        // Settings and toggles first: they contain words ("hatırlat") that other commands also use.
        if (isToggle(normalized)) {
            val on = !normalized.contains("kapat")
            return if (normalized.contains("su ")) {
                prefs.edit().putBoolean("water_enabled", on).apply()
                if (on) { scheduleWater(); "Tamam! 10.00 ile 20.00 arasında iki saatte bir su içmeyi hatırlatacağım 💧" }
                else "Su hatırlatıcısını kapattım."
            } else {
                prefs.edit().putBoolean("break_enabled", on).apply()
                if (on) "Uzun süre ekranda kalırsan sana kısa bir mola önereceğim 🧘" else "Mola önerilerini kapattım."
            }
        }
        ObiLife.savedCity(normalized)?.let { city ->
            prefs.edit().putString("city", city).apply()
            return "Şehrini ${ObiLife.title(city)} olarak kaydettim. Sabah mesajında havayı da söyleyeceğim ☀️"
        }
        ObiLife.bedtime(normalized)?.let { minutes ->
            prefs.edit().putInt("bedtime", minutes).apply()
            return "Yatma saatini ${String.format(tr, "%02d:%02d", minutes / 60, minutes % 60)} olarak kaydettim. O saatten sonra ben de uyuklarım 🌙"
        }

        // Birthdays (own first, then other people's).
        if (normalized.contains("doğum günüm") || normalized.contains("dogum gunum")) {
            val date = Regex("(\\d{1,2})\\s*(${ObiLife.months.joinToString("|")})").find(normalized)
                ?: return "Doğum gününü ‘Doğum günüm 12 Mayıs’ diye yazabilirsin."
            val day = date.groupValues[1].toInt()
            val month = ObiLife.months.indexOf(date.groupValues[2]) + 1
            if (day !in 1..31) return "Günü 1 ile 31 arasında yazabilir misin?"
            prefs.edit().putInt("birth_day", day).putInt("birth_month", month).apply()
            scheduleDaily()
            return "Harika! $day ${ObiLife.title(date.groupValues[2])} gününü kaydettim 🎂"
        }
        ObiLife.otherBirthday(normalized)?.let { b ->
            val list = jsonList("birthdays").filterNot { it.optString("label") == b.label }.toMutableList()
            list += JSONObject().put("label", b.label).put("day", b.day).put("month", b.month)
            putList("birthdays", list)
            scheduleDaily()
            return "Kaydettim! ${b.day} ${ObiLife.title(ObiLife.months[b.month - 1])} sabahı ${b.label} doğum gününü hatırlatacağım 🎁"
        }
        if (ObiLife.isBirthdayList(normalized)) {
            val list = jsonList("birthdays")
            if (list.isEmpty()) return "Henüz kayıtlı doğum günü yok. ‘Annemin doğum günü 5 Mart’ yazabilirsin."
            val today = LocalDate.now()
            return list.sortedBy { ObiLife.daysUntil(ObiLife.DatedLabel(it.optString("label"), it.optInt("day"), it.optInt("month")), today) }
                .joinToString("\n") { "🎂 ${ObiLife.title(it.optString("label"))} doğum günü: ${it.optInt("day")} ${ObiLife.title(ObiLife.months[it.optInt("month") - 1])}" }
        }

        if (normalized.contains("günaydın mesajı") || normalized.contains("sabah motive et")) {
            prefs.edit().putBoolean("morning_enabled", true).apply()
            scheduleDaily()
            return "Tamam! Sabah 08.00'de günaydın mesajı göndereceğim. ‘Şehrim İzmir’ yazarsan havayı da eklerim."
        }
        if (normalized.contains("sabah mesajını kapat") || normalized.contains("sabah mesajini kapat")) {
            prefs.edit().putBoolean("morning_enabled", false).apply()
            return "Sabah mesajlarını kapattım."
        }

        // Alarms and reminders: list, delete, then create.
        if (ObiLife.isAlarmList(normalized)) return alarmListText()
        ObiLife.alarmToDelete(normalized)?.let { index ->
            val alarms = alarms().filter { it.kind != "focus" }
            if (alarms.isEmpty()) return "Kurulu alarm ya da hatırlatıcı yok."
            if (index == 0) { alarms.forEach { cancel(it.id) }; return "Tüm alarm ve hatırlatıcıları sildim." }
            val target = alarms.getOrNull(index - 1) ?: return "Listede $index numara yok. ‘Alarmlarım’ yazıp numarasına bakabilirsin."
            cancel(target.id)
            return "${formatClock(target.time)} ‘${target.text}’ silindi."
        }
        val repeat = ObiLife.repeatRule(normalized)
        if (normalized.contains("alarm") || normalized.contains("uyandır") || normalized.contains("uyandir")) {
            val time = ObiParse.triggerTime(normalized) ?: return "Saati anlayamadım. Örnek: ‘Sabah 9'a alarm kur’, ‘her gün 7'de alarm’ veya ‘10 dakika sonra alarm kur’."
            schedule(time, "Obi alarmı: Uyanma zamanı!", "alarm", repeat)
            return scheduledReply("Alarmı", time, repeat)
        }

        // Notes.
        if (normalized.contains("notlarımı göster") || normalized.contains("notlarimi göster") || normalized == "notlarım" || normalized == "notlarim") {
            val notes = notes()
            if (notes.isEmpty()) return "Henüz notun yok. ‘Not al …’ yazabilirsin."
            val start = (notes.size - 10).coerceAtLeast(0)
            return notes.drop(start).mapIndexed { i, item -> "${start + i + 1}. $item" }.joinToString("\n") +
                "\n\nSilmek için: ‘2. notu sil’"
        }
        ObiLife.noteToDelete(normalized)?.let { index ->
            val notes = notes().toMutableList()
            if (index == 0) { prefs.edit().putString("notes", "[]").apply(); return "Tüm notları sildim." }
            if (index > notes.size) return "$index numaralı not yok."
            val removed = notes.removeAt(index - 1)
            prefs.edit().putString("notes", JSONArray(notes).toString()).apply()
            return "‘$removed’ notunu sildim."
        }
        ObiLife.noteSearch(normalized)?.let { query ->
            val found = notes().withIndex().filter { ObiLife.lower(it.value).contains(query) }
            return if (found.isEmpty()) "Notlarında ‘$query’ geçmiyor."
                else found.joinToString("\n") { "${it.index + 1}. ${it.value}" }
        }
        ObiParse.noteText(normalized, text)?.let { note -> save(note); return "Tamam, bunu hatırlayacağım: ‘$note’" }
        ObiParse.timerMinutes(normalized)?.let { minutes ->
            schedule(System.currentTimeMillis() + minutes * 60_000L, "Obi zamanlayıcısı: süre doldu!", "reminder", null)
            return "Tamam. $minutes dakika sonra sana haber vereceğim ⏱"
        }
        if (normalized.contains("hatırlat") || normalized.contains("hatirlat")) {
            val time = ObiParse.triggerTime(normalized) ?: return "Saat de yaz: örneğin ‘yarın 9'da annemi ara diye hatırlat’, ‘her gün 21'de ilacımı hatırlat’ veya ‘20 dakika sonra hatırlat’."
            val note = ObiParse.reminderNote(ObiLife.stripRepeat(text))
            schedule(time, note, "reminder", repeat)
            return scheduledReply("‘$note’ hatırlatıcısını", time, repeat)
        }

        // Shopping list.
        when (val list = ObiLife.listCommand(normalized)) {
            is ObiLife.ListCommand.Add -> {
                val items = shopping().toMutableList()
                list.items.forEach { item -> if (items.none { ObiLife.lower(it) == ObiLife.lower(item) }) items += ObiLife.title(item) }
                putStrings("shopping", items)
                return "Listeye ekledim 🛒 (${items.size} ürün)\n" + items.joinToString("\n") { "• $it" }
            }
            is ObiLife.ListCommand.Remove -> {
                val items = shopping().toMutableList()
                if (!items.removeAll { ObiLife.lower(it) == ObiLife.lower(list.item) }) return "Listede ‘${list.item}’ yok."
                putStrings("shopping", items)
                return if (items.isEmpty()) "Liste bitti, hepsi alındı! 🎉" else "Çıkardım ✓ Kalanlar:\n" + items.joinToString("\n") { "• $it" }
            }
            ObiLife.ListCommand.Show -> {
                val items = shopping()
                return if (items.isEmpty()) "Alışveriş listen boş. ‘Listeye süt, ekmek ekle’ yazabilirsin."
                    else "🛒 Alışveriş listen:\n" + items.joinToString("\n") { "• $it" } + "\n\nAlınca: ‘süt alındı’"
            }
            ObiLife.ListCommand.Clear -> { putStrings("shopping", emptyList()); return "Alışveriş listesini temizledim." }
            null -> Unit
        }

        // Money.
        if (ObiLife.isExpenseSummary(normalized)) return expenseSummary()
        ObiLife.expense(text)?.let { e ->
            val month = LocalDate.now().toString().take(7)
            val list = jsonList("expenses").filter { it.optString("month") >= previousMonth() }.toMutableList()
            list += JSONObject().put("month", month).put("label", e.label).put("amount", e.amount)
            putList("expenses", list)
            val total = list.filter { it.optString("month") == month }.sumOf { it.optDouble("amount") }
            return "${e.label}: ${ObiLife.money(e.amount)} yazdım 💸 Bu ay toplam ${ObiLife.money(total)}."
        }

        // Countdowns.
        ObiLife.countdown(normalized)?.let { c ->
            val list = jsonList("countdowns").filterNot { it.optString("label") == c.label }.toMutableList()
            list += JSONObject().put("label", c.label).put("day", c.day).put("month", c.month).apply { c.year?.let { put("year", it) } }
            putList("countdowns", list)
            return "${ObiLife.title(c.label)} için ${ObiLife.daysUntil(c, LocalDate.now())} gün kaldı ⏳ Her sabah hatırlatırım."
        }
        ObiLife.countdownQuery(normalized)?.let { word ->
            val list = countdowns()
            if (list.isEmpty()) return "Henüz geri sayım yok. ‘Geri sayım tatil 15 Temmuz’ yazabilirsin."
            val chosen = list.filter { ObiLife.matchesLabel(it.label, word) }.ifEmpty { list }
            return chosen.joinToString("\n") { "⏳ ${ObiLife.title(it.label)}: ${ObiLife.daysUntil(it, LocalDate.now())} gün" }
        }

        if (normalized.contains("haftam nasıl") || normalized.contains("haftam nasil")) return ObiLife.weeklyMood(moods(), LocalDate.now())
        if (normalized.contains("bugünün kelime") || normalized.contains("bugunun kelime") || normalized.contains("kelimeleri göster")) {
            return "📘 Bugünün kelimeleri:\n" + ObiLife.wordsOfDay(LocalDate.now()).joinToString("\n\n") { "${it.en} — ${it.tr}\n“${it.example}”" } +
                "\n\nAkşam ‘Oyunlar → Kelime testi’ ile kendini dene!"
        }
        return mind.respond(text)
    }

    private fun scheduledReply(what: String, time: Long, repeat: String?): String {
        val clock = formatClock(time)
        val day = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
        val whenText = when {
            repeat != null -> "${ObiLife.repeatLabel(repeat)} $clock"
            day == LocalDate.now() -> "bugün $clock"
            day == LocalDate.now().plusDays(1) -> "yarın $clock"
            else -> "${day.dayOfMonth} ${ObiLife.title(ObiLife.months[day.monthValue - 1])} $clock"
        }
        if (!canScheduleExact()) {
            needsExactPermission = true
            return "$what $whenText civarına kurdum. Tam saatinde çalması için bana ‘kesin alarm izni’ yaz."
        }
        return "$what $whenText için kurdum ✓"
    }

    // ---- Notes, lists and saved values --------------------------------------------------------

    fun notes(): List<String> = strings("notes")
    fun shopping(): List<String> = strings("shopping")
    private fun save(note: String) = putStrings("notes", notes() + note)

    private fun strings(key: String): List<String> = runCatching {
        val v = JSONArray(prefs.getString(key, "[]") ?: "[]"); List(v.length()) { v.getString(it) }
    }.getOrDefault(emptyList())

    private fun putStrings(key: String, values: List<String>) = prefs.edit().putString(key, JSONArray(values).toString()).apply()

    private fun jsonList(key: String): List<JSONObject> = runCatching {
        val v = JSONArray(prefs.getString(key, "[]") ?: "[]"); (0 until v.length()).mapNotNull { v.optJSONObject(it) }
    }.getOrDefault(emptyList())

    private fun putList(key: String, values: List<JSONObject>) =
        prefs.edit().putString(key, JSONArray().apply { values.forEach { put(it) } }.toString()).apply()

    private fun previousMonth(): String = LocalDate.now().minusMonths(1).toString().take(7)

    private fun expenseSummary(): String {
        val month = LocalDate.now().toString().take(7)
        val items = jsonList("expenses").filter { it.optString("month") == month }
        if (items.isEmpty()) return "Bu ay henüz harcama yazmadın. ‘Market 250 TL’ yazabilirsin."
        val byLabel = items.groupBy { it.optString("label") }.mapValues { (_, v) -> v.sumOf { it.optDouble("amount") } }
            .entries.sortedByDescending { it.value }
        return "💸 Bu ayki harcamaların:\n" + byLabel.joinToString("\n") { "• ${it.key}: ${ObiLife.money(it.value)}" } +
            "\nToplam: ${ObiLife.money(items.sumOf { it.optDouble("amount") })}"
    }

    fun countdowns(): List<ObiLife.DatedLabel> = jsonList("countdowns").map {
        ObiLife.DatedLabel(it.optString("label"), it.optInt("day"), it.optInt("month"), if (it.has("year")) it.optInt("year") else null)
    }.filter { ObiLife.daysUntil(it, LocalDate.now()) >= 0 }

    fun city(): String? = prefs.getString("city", null)
    fun bedtime(): Int = prefs.getInt("bedtime", 23 * 60)
    fun breakEnabled(): Boolean = prefs.getBoolean("break_enabled", true)

    // ---- Mood ---------------------------------------------------------------------------------

    fun moods(): Map<LocalDate, Int> = runCatching {
        val o = JSONObject(prefs.getString("moods", "{}") ?: "{}")
        o.keys().asSequence().associate { LocalDate.parse(it) to o.getInt(it) }
    }.getOrDefault(emptyMap())

    fun logMood(score: Int): String {
        val o = runCatching { JSONObject(prefs.getString("moods", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val first = !o.has(LocalDate.now().toString())
        o.put(LocalDate.now().toString(), score.coerceIn(1, 5))
        // Keep about two months.
        val cutoff = LocalDate.now().minusDays(60).toString()
        o.keys().asSequence().toList().filter { it < cutoff }.forEach { o.remove(it) }
        prefs.edit().putString("moods", o.toString()).apply()
        if (first) addBond(2)
        return ObiLife.moodReply(score)
    }

    fun moodLoggedToday(): Boolean = moods().containsKey(LocalDate.now())

    // ---- Bond and wardrobe ---------------------------------------------------------------------

    fun bond(): Int = prefs.getInt("bond", 0)

    /** Adds bond points (capped per day) and returns a newly unlocked item, if any. */
    fun addBond(points: Int): ObiLife.Unlock? {
        val today = LocalDate.now().toString()
        val usedToday = if (prefs.getString("bond_day", "") == today) prefs.getInt("bond_today", 0) else 0
        val gain = points.coerceAtMost(BOND_DAILY_CAP - usedToday).coerceAtLeast(0)
        if (gain == 0) return null
        val before = bond()
        prefs.edit().putInt("bond", before + gain).putString("bond_day", today).putInt("bond_today", usedToday + gain).apply()
        return ObiLife.justUnlocked(before, before + gain)
    }

    fun skin(): WordSiegeMascotSkin = WordSiegeMascotSkin.fromId(prefs.getString("skin", null))
        ?.takeIf { s -> ObiLife.unlocked(bond()).any { it.skin == s } } ?: WordSiegeMascotSkin.ORB

    fun chosenHat(): WordSiegeMascotHat = runCatching { WordSiegeMascotHat.valueOf(prefs.getString("hat", "NONE") ?: "NONE") }
        .getOrDefault(WordSiegeMascotHat.NONE)
        .takeIf { h -> h == WordSiegeMascotHat.NONE || ObiLife.unlocked(bond()).any { it.hat == h } } ?: WordSiegeMascotHat.NONE

    /** The worn hat: a special day lends its hat when the user has not chosen one. */
    fun hat(): WordSiegeMascotHat {
        val chosen = chosenHat()
        if (chosen != WordSiegeMascotHat.NONE) return chosen
        if (isBirthdayToday()) return WordSiegeMascotHat.CROWN
        return ObiLife.celebration(LocalDate.now())?.hat ?: WordSiegeMascotHat.NONE
    }

    fun wear(skin: WordSiegeMascotSkin? = null, hat: WordSiegeMascotHat? = null) {
        prefs.edit().apply {
            skin?.let { putString("skin", it.id) }
            hat?.let { putString("hat", it.name) }
        }.apply()
        context.sendBroadcast(Intent(ACTION_LOOK_CHANGED).setPackage(context.packageName))
        ObiWidget.refresh(context)
    }

    fun isBirthdayToday(): Boolean {
        val today = LocalDate.now()
        return prefs.getInt("birth_day", 0) == today.dayOfMonth && prefs.getInt("birth_month", 0) == today.monthValue
    }

    // ---- Morning message ------------------------------------------------------------------------

    /** Lines for today's morning message; empty when there is nothing to say. */
    fun morningLines(weather: String?): List<String> {
        val today = LocalDate.now()
        val lines = mutableListOf<String>()
        if (isBirthdayToday()) lines += "İyi ki doğdun! Bugün senin günün 🎂✨"
        ObiLife.celebration(today)?.let { lines += it.message }
        jsonList("birthdays").filter { it.optInt("day") == today.dayOfMonth && it.optInt("month") == today.monthValue }
            .forEach { lines += "Bugün ${it.optString("label")} doğum günü! Kutlamayı unutma 🎁" }
        if (prefs.getBoolean("morning_enabled", false)) {
            lines += "Günaydın! Bugün küçük bir adım da büyük bir başlangıç 💜"
            weather?.let { lines += it }
            countdowns().forEach { lines += "⏳ ${ObiLife.title(it.label)}: ${ObiLife.daysUntil(it, today)} gün kaldı" }
            ObiLife.wordsOfDay(today).first().let { lines += "📘 Günün kelimesi: ${it.en} — ${it.tr}" }
        }
        return lines
    }

    // ---- Alarms ---------------------------------------------------------------------------------

    fun formatClock(time: Long): String = Calendar.getInstance().run {
        timeInMillis = time; String.format(tr, "%02d:%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))
    }

    fun canScheduleExact(): Boolean = context.getSystemService(AlarmManager::class.java)?.let {
        Build.VERSION.SDK_INT < 31 || it.canScheduleExactAlarms()
    } ?: false

    fun alarms(): List<ObiAlarm> = jsonList("pending_alarms").map {
        ObiAlarm(it.optInt("id"), it.optLong("time"), it.optString("text", "Hatırlatıcı"), it.optString("kind", "reminder"),
            it.optString("repeat").ifBlank { null }, it.optInt("hour"), it.optInt("minute"))
    }.sortedBy { it.time }

    fun alarmListText(): String {
        val list = alarms().filter { it.kind != "focus" }
        if (list.isEmpty()) return "Kurulu alarm ya da hatırlatıcı yok. ‘Sabah 7'de alarm kur’ yazabilirsin."
        return "⏰ Kurulu olanlar:\n" + list.mapIndexed { i, a ->
            val day = Instant.ofEpochMilli(a.time).atZone(ZoneId.systemDefault()).toLocalDate()
            val whenText = a.repeat?.let { ObiLife.repeatLabel(it) } ?: when (day) {
                LocalDate.now() -> "bugün"; LocalDate.now().plusDays(1) -> "yarın"
                else -> "${day.dayOfMonth} ${ObiLife.title(ObiLife.months[day.monthValue - 1])}"
            }
            "${i + 1}. $whenText ${formatClock(a.time)} — ${if (a.kind == "alarm") "Alarm" else a.text}"
        }.joinToString("\n") + "\n\nSilmek için: ‘2. alarmı sil’"
    }

    /** Next upcoming item for the widget, e.g. "21:00 İlacımı iç". */
    fun nextAlarmLine(): String? = alarms().firstOrNull { it.time > System.currentTimeMillis() }?.let {
        "${formatClock(it.time)}  ${if (it.kind == "alarm") "Alarm" else it.text}"
    }

    fun schedule(time: Long, text: String, kind: String = "reminder", repeat: String? = null): Int {
        val id = (System.nanoTime() and 0x7fff_fffeL).toInt() + 1
        val clock = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
        val entry = JSONObject().put("id", id).put("time", time).put("text", text).put("kind", kind)
            .put("hour", clock.hour).put("minute", clock.minute)
        repeat?.let { entry.put("repeat", it) }
        putList("pending_alarms", jsonList("pending_alarms") + entry)
        arm(id, time, text, kind)
        // Make Obi's spoken line now, while online, so the alert can speak offline later.
        if (prefs.getBoolean("voice_alerts", true)) {
            val voice = ObiGeminiVoice(context)
            if (kind == "alarm") voice.prepareFixed()
            else if (!text.startsWith("Obi ")) voice.prepare("rem_$id", ObiGeminiVoice.reminderLine(text))
        }
        ObiWidget.refresh(context)
        return id
    }

    fun snooze(text: String, kind: String) {
        schedule(System.currentTimeMillis() + 10 * 60_000L, text, kind, null)
    }

    private fun pendingFor(id: Int, text: String, kind: String): PendingIntent {
        val intent = Intent(context, ObiReminderReceiver::class.java).putExtra("text", text)
            .putExtra("kind", kind).putExtra("id", id).setAction("com.obi.local.REMINDER.$id")
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun arm(id: Int, time: Long, text: String, kind: String) {
        val pending = pendingFor(id, text, kind)
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        if (canScheduleExact()) {
            val open = PendingIntent.getActivity(context, id, Intent(context, ObiEntryActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(time, open), pending)
        } else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }

    fun cancel(id: Int) {
        val alarm = alarms().firstOrNull { it.id == id }
        if (alarm != null) context.getSystemService(AlarmManager::class.java)?.cancel(pendingFor(id, alarm.text, alarm.kind))
        ObiGeminiVoice(context).forget("rem_$id")
        forgetAlarm(id)
    }

    fun forgetAlarm(id: Int) {
        putList("pending_alarms", jsonList("pending_alarms").filter { it.optInt("id") != id })
        ObiWidget.refresh(context)
    }

    /** After an alarm rings: a repeating one is armed for its next day, a one-off is forgotten. */
    fun onFired(id: Int) {
        val alarm = alarms().firstOrNull { it.id == id } ?: return
        val rule = alarm.repeat ?: return forgetAlarm(id)
        val next = ObiLife.nextOccurrence(rule, alarm.hour, alarm.minute, LocalDateTime.now())
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        putList("pending_alarms", jsonList("pending_alarms").map { if (it.optInt("id") == id) it.put("time", next) else it })
        arm(id, next, alarm.text, alarm.kind)
        ObiWidget.refresh(context)
    }

    /** Alarms do not survive a reboot or app update, so the saved list is armed again; recently missed ones ring now. */
    fun rearmSaved() {
        val now = System.currentTimeMillis()
        val kept = mutableListOf<JSONObject>()
        jsonList("pending_alarms").forEach { item ->
            var time = item.optLong("time")
            val rule = item.optString("repeat").ifBlank { null }
            if (rule != null && time < now) {
                time = ObiLife.nextOccurrence(rule, item.optInt("hour"), item.optInt("minute"), LocalDateTime.now())
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                item.put("time", time)
            }
            if (time < now - 6 * 60 * 60_000L) return@forEach
            kept += item
            arm(item.optInt("id"), maxOf(time, now + 5_000L), item.optString("text", "Hatırlatıcı"), item.optString("kind", "reminder"))
        }
        putList("pending_alarms", kept)
    }

    // ---- Focus (pomodoro) -------------------------------------------------------------------------

    fun startFocus(minutes: Int): Long {
        stopFocus()
        val end = System.currentTimeMillis() + minutes * 60_000L
        val id = schedule(end, "Odak süresi bitti! Harika çalıştın 🎉 Şimdi 5 dakika mola.", "focus", null)
        prefs.edit().putInt("focus_id", id).putLong("focus_end", end).apply()
        return end
    }

    fun stopFocus() {
        val id = prefs.getInt("focus_id", 0)
        if (id != 0) cancel(id)
        prefs.edit().remove("focus_id").remove("focus_end").apply()
    }

    fun focusEnd(): Long = prefs.getLong("focus_end", 0L).takeIf { it > System.currentTimeMillis() } ?: 0L

    // ---- Daily chains ---------------------------------------------------------------------------

    private fun chain(action: String, code: Int, time: Long) {
        val pending = PendingIntent.getBroadcast(context, code, Intent(context, ObiReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }

    private fun nextAt(hour: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    /** Morning message at 08:00, the evening check-in at 21:00 and, when on, the water reminders. */
    fun scheduleDaily() {
        chain(ACTION_DAILY, 808, nextAt(8))
        chain(ACTION_EVENING, 809, nextAt(21))
        if (prefs.getBoolean("water_enabled", false)) scheduleWater()
    }

    fun scheduleWater() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val slot = listOf(10, 12, 14, 16, 18, 20).firstOrNull { it > hour }
        chain(ACTION_WATER, 810, if (slot != null) nextAt(slot) else nextAt(10))
    }

    fun waterEnabled(): Boolean = prefs.getBoolean("water_enabled", false)
}

class ObiReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = ObiStore(context)
        when (intent.action) {
            ObiStore.ACTION_DAILY -> {
                val pending = goAsync()
                Thread {
                    try {
                        val weather = store.city()?.takeIf { context.getSharedPreferences("obi_local", Context.MODE_PRIVATE).getBoolean("morning_enabled", false) }
                            ?.let { ObiWeather().blocking(listOf(it)) }
                        val lines = store.morningLines(weather)
                        if (lines.isNotEmpty()) {
                            notify(context, lines.joinToString("\n"), "Obi günaydın diyor ☀️", null)
                            context.sendBroadcast(Intent(ObiStore.ACTION_CELEBRATE).setPackage(context.packageName).putExtra("text", lines.first()))
                        }
                        store.scheduleDaily()
                        ObiWidget.refresh(context)
                    } finally { pending.finish() }
                }.start()
                return
            }
            ObiStore.ACTION_EVENING -> {
                if (!store.moodLoggedToday()) context.sendBroadcast(Intent(ObiStore.ACTION_ASK_MOOD).setPackage(context.packageName))
                if (LocalDate.now().dayOfWeek == java.time.DayOfWeek.SUNDAY && store.moods().isNotEmpty()) {
                    notify(context, ObiLife.weeklyMood(store.moods(), LocalDate.now()), "Obi'den haftalık özet 💜", null)
                }
                store.scheduleDaily()
                return
            }
            ObiStore.ACTION_WATER -> {
                if (store.waterEnabled()) {
                    val line = listOf("Su içme vakti! Bir bardak su, bir gülümseme 💧", "Psst… Bir yudum su alalım mı? 💧", "Susadın mı? Ben susadım! Hadi su 💧").random()
                    context.sendBroadcast(Intent(ObiStore.ACTION_NUDGE).setPackage(context.packageName).putExtra("text", line).putExtra("action", "BOUNCE"))
                    notify(context, line, "Obi su hatırlatıcısı", null, quiet = true)
                    store.scheduleWater()
                }
                return
            }
            ObiStore.ACTION_SNOOZE -> {
                context.getSystemService(NotificationManager::class.java)?.cancel(intent.getIntExtra("notification", 0))
                context.stopService(Intent(context, ObiVoiceAlertService::class.java))
                store.snooze(intent.getStringExtra("text") ?: "Hatırlatıcı", intent.getStringExtra("kind") ?: "reminder")
                return
            }
        }
        val id = intent.getIntExtra("id", 0)
        val kind = intent.getStringExtra("kind") ?: "reminder"
        val text = intent.getStringExtra("text") ?: "Hatırlatıcı zamanı geldi."
        if (id != 0) store.onFired(id)
        val title = when (kind) { "alarm" -> "Obi alarmı ⏰"; "focus" -> "Obi: odak süresi bitti"; else -> "Obi hatırlatıyor" }
        notify(context, text, title, if (kind == "focus") null else kind)
        context.sendBroadcast(Intent(ObiStore.ACTION_CELEBRATE).setPackage(context.packageName).putExtra("text", text))
        if (context.getSharedPreferences("obi_local", Context.MODE_PRIVATE).getBoolean("voice_alerts", true)) {
            runCatching {
                context.startForegroundService(Intent(context, ObiVoiceAlertService::class.java)
                    .putExtra("kind", if (kind == "alarm") "alarm" else "reminder").putExtra("text", text).putExtra("id", id))
            }
        }
    }

    private fun notify(context: Context, text: String, title: String, snoozeKind: String?, quiet: Boolean = false) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = if (quiet) "obi_gentle" else "obi_reminders"
        manager.createNotificationChannel(NotificationChannel(channel, if (quiet) "Obi küçük hatırlatmalar" else "Obi hatırlatıcıları",
            if (quiet) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notificationId = (System.currentTimeMillis() and 0x7fff_ffffL).toInt()
        val open = PendingIntent.getActivity(context, 4, Intent(context, ObiEntryActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(context, channel).setSmallIcon(R.drawable.ic_obi_notification)
            .setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true)
        if (snoozeKind != null) {
            val snooze = PendingIntent.getBroadcast(context, notificationId,
                Intent(context, ObiReminderReceiver::class.java).setAction(ObiStore.ACTION_SNOOZE)
                    .putExtra("text", text).putExtra("kind", snoozeKind).putExtra("notification", notificationId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(null, "10 dk ertele", snooze).build())
        }
        manager.notify(notificationId, builder.build())
    }
}

class ObiBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        ObiStore(context).apply { scheduleDaily(); rearmSaved() }
        ObiWidget.refresh(context)
    }
}
