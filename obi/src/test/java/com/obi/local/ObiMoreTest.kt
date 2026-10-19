package com.obi.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObiMoreTest {
    @Test fun callsAndMessages() {
        assertEquals(PhoneCommand.Call("annemi", "annemi ara"), ObiPhoneParse.parse("Annemi ara"))
        assertEquals(PhoneCommand.Call("ali'yi", "ali'yi arar mısın"), ObiPhoneParse.parse("Ali'yi arar mısın"))
        assertEquals(PhoneCommand.Message("ali", "geliyorum", "whatsapp"), ObiPhoneParse.parse("Ali'ye WhatsApp'tan geliyorum yaz"))
        assertEquals(PhoneCommand.Message("ayşe", "akşam görüşürüz", null), ObiPhoneParse.parse("Ayşe'ye akşam görüşürüz diye mesaj at"))
        assertEquals(PhoneCommand.Message("annem", "eve geldim", "sms"), ObiPhoneParse.parse("anneme sms ile eve geldim yaz"))
        assertNull(ObiPhoneParse.parse("listeye süt yaz"))
        assertEquals(PhoneCommand.WebSearch("kedi"), ObiPhoneParse.parse("google'da kedi ara"))
        assertEquals(listOf("annem", "anne", "annemi"), ObiPhoneParse.contactCandidates("annemi"))
        assertEquals(listOf("ali"), ObiPhoneParse.contactCandidates("ali'ye").take(1))
        assertEquals("905321234567", ObiPhoneParse.internationalNumber("0532 123 45 67"))
        assertEquals("905321234567", ObiPhoneParse.internationalNumber("+90 532 123 4567"))
        assertEquals("491701234567", ObiPhoneParse.internationalNumber("+49 170 1234567"))
    }

    @Test fun nightChildAndWords() {
        assertEquals(23 * 60 to 7 * 60, ObiLife.nightRange("gece modu 23-7"))
        assertEquals(22 * 60 + 30 to 6 * 60 + 30, ObiLife.nightRange("gece modu 22:30 06:30"))
        assertTrue(ObiLife.isInRange(60, 23 * 60, 7 * 60))
        assertTrue(!ObiLife.isInRange(12 * 60, 23 * 60, 7 * 60))
        val q = ObiLife.mathQuestion(5, 0)
        assertTrue(q.answer in 0..20)
        assertEquals(12, ObiLife.numberAnswer("on iki"))
        assertEquals(7, ObiLife.numberAnswer("7"))
        assertEquals("elma", ObiLife.pronounceWord("elma nasıl okunur?"))
        assertEquals("necessary", ObiLife.pronounceWord("telaffuz: necessary"))
        assertEquals(listOf("ar", "ka", "daş"), ObiLife.syllables("arkadaş"))
        assertEquals(listOf("türk", "çe"), ObiLife.syllables("Türkçe"))
        assertEquals(listOf("sa", "at"), ObiLife.syllables("saat"))
    }
}
