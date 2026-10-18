package com.obi.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObiFriendTest {
    @Test fun remembersWhatTheUserTells() {
        assertEquals(ObiLife.Fact("en sevdiğin renk", "mavi"), ObiLife.fact("En sevdiğim renk mavidir"))
        assertEquals(ObiLife.Fact("en sevdiğin yemek", "mantı"), ObiLife.fact("benim en sevdiğim yemek mantı"))
        assertEquals(ObiLife.Fact("memleketin", "trabzon"), ObiLife.fact("memleketim trabzon"))
        assertEquals(ObiLife.Fact("yaşın", "34"), ObiLife.fact("34 yaşındayım"))
        assertEquals(ObiLife.Fact("kedinin adı", "Pamuk"), ObiLife.fact("kedimin adı pamuk"))
        assertEquals(ObiLife.Fact("sevdiğin", "kahve"), ObiLife.fact("kahve çok severim"))
        assertEquals(ObiLife.Fact("sevmediğin", "kış"), ObiLife.fact("kış sevmem"))
        assertNull(ObiLife.fact("seni seviyorum"))
        assertNull(ObiLife.fact("en sevdiğim renk ne"))
        assertEquals("en sevdiğin renk", ObiLife.factQuestion("en sevdiğim renk ne?"))
        assertEquals("*", ObiLife.factQuestion("hakkımda ne biliyorsun"))
    }

    @Test fun feelingsAndRequests() {
        assertEquals(ObiLife.Feeling.LOVE, ObiLife.feeling("Seni seviyorum Obi"))
        assertEquals(ObiLife.Feeling.SAD, ObiLife.feeling("bugün moralim bozuk"))
        assertEquals(ObiLife.Feeling.BORED, ObiLife.feeling("çok sıkıldım"))
        assertEquals(ObiLife.Feeling.HUG, ObiLife.feeling("bana sarıl"))
        assertNull(ObiLife.feeling("yarın hava nasıl"))
        assertEquals("CONFETTI", ObiLife.effectCommand("konfeti at"))
        assertEquals("SNOW", ObiLife.effectCommand("kar yağdır"))
        assertEquals("HEART_EYES", ObiLife.faceCommand("kalp gözlü ol"))
        assertEquals("ALL", ObiLife.faceCommand("mimiklerini göster"))
        assertEquals("Can dostu 💜", ObiLife.bondLevel(250))
        assertEquals("Tam 1 haftadır arkadaşız! 🎉", ObiLife.friendMilestone(7))
        assertNull(ObiLife.friendMilestone(8))
    }
}
