package com.obi.local

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObiLifeTest {
    @Test fun repeatingAlarms() {
        assertEquals("daily", ObiLife.repeatRule("her gün 21'de ilacımı hatırlat"))
        assertEquals("weekdays", ObiLife.repeatRule("hafta içi 7'de alarm kur"))
        assertEquals("weekly:1", ObiLife.repeatRule("her pazartesi 9'da toplantı hatırlat"))
        assertNull(ObiLife.repeatRule("yarın 9'da hatırlat"))
        assertEquals("21'de ilacımı hatırlat", ObiLife.stripRepeat("her gün 21'de ilacımı hatırlat"))
        val fridayEvening = LocalDateTime.of(2026, 10, 16, 21, 0) // a Friday
        assertEquals(LocalDateTime.of(2026, 10, 19, 7, 0), ObiLife.nextOccurrence("weekdays", 7, 0, fridayEvening))
        assertEquals(LocalDateTime.of(2026, 10, 17, 21, 0), ObiLife.nextOccurrence("daily", 21, 0, fridayEvening))
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 0), ObiLife.nextOccurrence("weekly:1", 9, 0, fridayEvening))
    }

    @Test fun alarmAndNoteCommands() {
        assertEquals(2, ObiLife.alarmToDelete("2. alarmı sil"))
        assertEquals(3, ObiLife.alarmToDelete("hatırlatıcı 3'ü iptal et"))
        assertEquals(0, ObiLife.alarmToDelete("tüm alarmları sil"))
        assertNull(ObiLife.alarmToDelete("alarm kur"))
        assertTrue(ObiLife.isAlarmList("alarmlarım"))
        assertEquals(3, ObiLife.noteToDelete("3. notu sil"))
        assertEquals(2, ObiLife.noteToDelete("not 2 sil"))
        assertEquals(0, ObiLife.noteToDelete("notları temizle"))
        assertEquals("süt", ObiLife.noteSearch("notlarımda süt ara"))
    }

    @Test fun shoppingList() {
        assertEquals(ObiLife.ListCommand.Add(listOf("süt", "ekmek", "yumurta")), ObiLife.listCommand("listeye süt, ekmek ve yumurta ekle"))
        assertEquals(ObiLife.ListCommand.Remove("süt"), ObiLife.listCommand("listeden süt sil"))
        assertEquals(ObiLife.ListCommand.Remove("ekmek"), ObiLife.listCommand("ekmek alındı"))
        assertEquals(ObiLife.ListCommand.Show, ObiLife.listCommand("listemi göster"))
        assertEquals(ObiLife.ListCommand.Clear, ObiLife.listCommand("listeyi temizle"))
        assertNull(ObiLife.listCommand("bugün çok yoruldum"))
    }

    @Test fun expenses() {
        assertEquals(ObiLife.Expense("Market", 250.0), ObiLife.expense("market 250 tl"))
        assertEquals(ObiLife.Expense("Kahve", 85.5), ObiLife.expense("kahve için 85,5 lira harcadım".replace("için ", "")))
        assertNull(ObiLife.expense("bu telefon kaç tl?"))
        assertNull(ObiLife.expense("dün 500 tl kazandım ve çok mutluyum"))
        assertEquals("250 TL", ObiLife.money(250.0))
    }

    @Test fun datesAndCountdowns() {
        val mom = ObiLife.otherBirthday("annemin doğum günü 5 mart")!!
        assertEquals("annemin", mom.label); assertEquals(5, mom.day); assertEquals(3, mom.month)
        assertNull(ObiLife.otherBirthday("doğum günüm 12 mayıs"))
        val trip = ObiLife.countdown("geri sayım tatil 15 temmuz")!!
        assertEquals("tatil", trip.label)
        val today = LocalDate.of(2026, 10, 16)
        assertEquals(272L, ObiLife.daysUntil(trip, today))
        assertEquals("tatile", ObiLife.countdownQuery("tatile kaç gün kaldı"))
        assertTrue(ObiLife.matchesLabel("tatil", "tatile"))
        assertFalse(ObiLife.matchesLabel("sınav", "tatile"))
    }

    @Test fun settingsAndNight() {
        assertEquals("izmir", ObiLife.savedCity("şehrim izmir"))
        assertEquals(23 * 60 + 30, ObiLife.bedtime("uyku saatim 23:30"))
        assertTrue(ObiLife.isNight(23 * 60 + 45, 23 * 60))
        assertTrue(ObiLife.isNight(3 * 60, 23 * 60))
        assertFalse(ObiLife.isNight(12 * 60, 23 * 60))
        assertTrue(ObiLife.isNight(2 * 60, 60))
    }

    @Test fun moodWordsAndGames() {
        val today = LocalDate.of(2026, 10, 16)
        assertTrue(ObiLife.weeklyMood(mapOf(today to 4, today.minusDays(1) to 5), today).contains("2 gün"))
        assertEquals(3, ObiLife.wordsOfDay(today).size)
        val quiz = ObiLife.quiz(today, 0, 7L)
        assertTrue(quiz.word.tr in quiz.options); assertEquals(3, quiz.options.distinct().size)
        assertTrue(ObiLife.guessWords.all { it.length == 5 })
        assertEquals("🟩🟩🟩🟩🟩", ObiLife.guessMarks("kalem", "kalem"))
        assertEquals("🟨🟨⬜⬜⬜", ObiLife.guessMarks("kalem", "akıcı"))
        assertTrue(ObiLife.riddleCorrect(ObiLife.riddles[0], "Bence radyo"))
    }

    @Test fun bondAndCelebrations() {
        assertEquals(1, ObiLife.unlocked(0).size)
        assertNotNull(ObiLife.nextUnlock(0))
        assertEquals(WordSiegeMascotHat.PARTY, ObiLife.justUnlocked(8, 12)?.hat)
        assertNull(ObiLife.justUnlocked(12, 20))
        assertEquals(WordSiegeMascotHat.PARTY, ObiLife.celebration(LocalDate.of(2026, 10, 29))?.hat)
        assertEquals(WordSiegeMascotHat.FLOWER, ObiLife.celebration(LocalDate.of(2026, 5, 10))?.hat)
        assertNull(ObiLife.celebration(LocalDate.of(2026, 10, 16)))
    }
}
