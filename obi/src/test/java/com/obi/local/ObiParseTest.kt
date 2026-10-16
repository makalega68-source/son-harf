package com.obi.local

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObiParseTest {
    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 16, 21, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    private fun hourMinute(time: Long): Pair<Int, Int> =
        Calendar.getInstance().run { timeInMillis = time; get(Calendar.HOUR_OF_DAY) to get(Calendar.MINUTE) }

    @Test fun minutesLaterIsRelativeNotAClockHour() {
        assertEquals(now + 5 * 60_000L, ObiParse.triggerTime("5 dakika sonra hatırlat", now))
        assertEquals(now + 10 * 60_000L, ObiParse.triggerTime("10 dakika sonra alarm kur", now))
        assertEquals(now + 120 * 60_000L, ObiParse.triggerTime("2 saat sonra hatırlat", now))
        assertNull(ObiParse.clockTime("5 dakika"))
    }

    @Test fun clockPhrasesStillWork() {
        assertEquals(9 to 0, ObiParse.clockTime("sabah 9'a alarm kur"))
        assertEquals(19 to 0, ObiParse.clockTime("akşam 7'de annemi ara diye hatırlat"))
        assertEquals(14 to 30, ObiParse.clockTime("saat 14.30 toplantı hatırlat"))
        assertEquals(8 to 0, ObiParse.clockTime("saat 8 alarm"))
        assertEquals(9 to 0, hourMinute(ObiParse.triggerTime("yarın saat 09.00 hatırlat", now)!!))
    }

    @Test fun reminderNoteKeepsTheWords() {
        assertEquals("ilacını iç", ObiParse.reminderNote("10 dakika sonra ilacını iç diye hatırlat"))
        assertEquals("annemi ara", ObiParse.reminderNote("yarın saat 09.00 annemi ara diye hatırlat"))
    }

    @Test fun everydaySentencesAreNotLocalCommands() {
        assertFalse(ObiParse.isLocalCommand("bugün bir adım attım"))
        assertFalse(ObiParse.isLocalCommand("bana not almayı öğret"))
        assertFalse(ObiParse.isLocalCommand("alarm saatimi kaçırdım"))
        assertTrue(ObiParse.isLocalCommand("Not al süt alınacak"))
        assertTrue(ObiParse.isLocalCommand("notlarımı göster"))
        assertTrue(ObiParse.isLocalCommand("sabah 7'de alarm"))
        assertTrue(ObiParse.isLocalCommand("alarm kur"))
        assertTrue(ObiParse.isLocalCommand("20 dakika zamanlayıcı başlat"))
        assertTrue(ObiParse.isLocalCommand("benim adım Ece"))
        assertTrue(ObiParse.isLocalCommand("adım Ece"))
    }

    @Test fun weatherCityParsing() {
        assertEquals(listOf("istanbul"), ObiParse.cityCandidates("İstanbul'da hava nasıl?"))
        assertEquals(listOf("istanbulda", "istanbul"), ObiParse.cityCandidates("istanbulda hava nasıl"))
        assertEquals(listOf("ankara"), ObiParse.cityCandidates("hava durumu ankara"))
        assertEquals(emptyList<String>(), ObiParse.cityCandidates("bugün hava nasıl"))
        assertFalse(ObiParse.isWeatherQuestion("su kaç derecede kaynar"))
        assertTrue(ObiParse.isWeatherQuestion("izmir kaç derece"))
    }
}
