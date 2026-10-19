package com.obi.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObiPhoneParseTest {
    private fun parse(text: String) = ObiPhoneParse.parse(text)

    @Test fun opensAppsByName() {
        assertEquals(PhoneCommand.OpenApp("youtube"), parse("YouTube aç"))
        assertEquals(PhoneCommand.OpenApp("whatsapp'ı"), parse("WhatsApp'ı açar mısın"))
        assertEquals(PhoneCommand.OpenApp("instagramı"), parse("instagramı aç"))
        assertEquals(PhoneCommand.OpenApp("spotify"), parse("spotify uygulamasını başlat"))
        assertEquals(AppTarget.Named("whatsapp"), ObiPhoneParse.resolve("whatsapp'ı"))
        assertEquals(AppTarget.Named("instagram"), ObiPhoneParse.resolve("instagramı"))
        assertEquals(AppTarget.Named("whatsapp"), ObiPhoneParse.resolve("vatsap"))
        assertEquals(AppTarget.Category("gallery", "Galeri"), ObiPhoneParse.resolve("galeriyi"))
        assertEquals(AppTarget.Category("calculator", "Hesap makinesi"), ObiPhoneParse.resolve("hesap makinesi"))
    }

    @Test fun matchesInstalledNames() {
        val apps = listOf("YouTube" to "yt", "YouTube Music" to "ytm", "WhatsApp" to "wa", "Instagram" to "ig", "Haritalar" to "maps")
        assertEquals("yt", ObiPhoneParse.bestMatch("youtube", apps))
        assertEquals("wa", ObiPhoneParse.bestMatch("whatsapp", apps))
        assertEquals("ig", ObiPhoneParse.bestMatch("instagramı", apps))
        assertEquals("ytm", ObiPhoneParse.bestMatch("youtube music", apps))
        assertEquals("maps", ObiPhoneParse.bestMatch("google haritalar", apps))
        assertNull(ObiPhoneParse.bestMatch("netflix", apps))
    }

    @Test fun phoneControls() {
        assertEquals(PhoneCommand.Camera(video = false, selfie = false), parse("kamerayı aç"))
        assertEquals(PhoneCommand.Camera(video = false, selfie = true), parse("selfie çek"))
        assertEquals(PhoneCommand.Torch(true), parse("feneri aç"))
        assertEquals(PhoneCommand.Torch(false), parse("el fenerini kapat"))
        assertEquals(PhoneCommand.Torch(null), parse("fener"))
        assertEquals(PhoneCommand.Brightness(step = -1), parse("ekran ışığını kıs"))
        assertEquals(PhoneCommand.Brightness(step = 1), parse("parlaklığı artır"))
        assertEquals(PhoneCommand.Brightness(percent = 40), parse("parlaklık %40"))
        assertEquals(PhoneCommand.Brightness(percent = 30), parse("parlaklığı 30 yap"))
        assertEquals(PhoneCommand.Global("lock"), parse("tuşu kilitle"))
        assertEquals(PhoneCommand.Global("lock"), parse("ekranı kilitle"))
        assertEquals(PhoneCommand.Global("screenshot"), parse("ekran görüntüsü al"))
        assertEquals(PhoneCommand.SettingsPage("main", false), parse("ayarları aç"))
        assertEquals(PhoneCommand.SettingsPage("wifi", true), parse("wifi aç"))
        assertEquals(PhoneCommand.SettingsPage("bluetooth", true), parse("bluetooth'u kapat"))
        assertEquals(PhoneCommand.Volume(-1), parse("sesi kıs"))
        assertEquals(PhoneCommand.Volume(1), parse("sesi aç"))
        assertEquals(PhoneCommand.Volume(0, ringer = "vibrate"), parse("telefonu sessize al"))
        assertEquals(PhoneCommand.AppSearch("youtube", "tarkan"), parse("youtube'da tarkan aç"))
        assertEquals(PhoneCommand.WebSearch("hava durumu yarın"), parse("google'da hava durumu yarın ara"))
        assertEquals(PhoneCommand.PlayMusic("sezen aksu"), parse("sezen aksu şarkısını çal"))
        assertEquals(PhoneCommand.OpenApp("müzik"), parse("bir şarkı çal"))
        assertEquals(PhoneCommand.Home, parse("ana ekrana dön"))
    }

    @Test fun leavesObisOwnCommandsAlone() {
        assertNull(parse("listemi aç"))
        assertNull(parse("notlarımı aç"))
        assertNull(parse("sabah 7'de alarm ayarla"))
        assertNull(parse("yarın 9'da feneri aç diye hatırlat"))
        assertNull(parse("dinlemeyi aç"))
        assertNull(parse("sesini incelt"))
        assertNull(parse("ses durumu"))
        assertNull(parse("bugün çok güzel bir gün geçirdim"))
        assertNull(parse("kamera izni"))
    }
}
