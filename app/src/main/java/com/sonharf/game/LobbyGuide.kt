package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One help topic of the lobby menu: a title and short paragraphs. */
internal data class LobbyGuideTopic(val id: String, val icon: ImageVector, val title: String, val paragraphs: List<String>)

internal fun lobbyGuideTopics(): List<LobbyGuideTopic> = listOf(
    LobbyGuideTopic("last_letter", Icons.Rounded.Link, sh("Son Harf", "Last Letter"), listOf(
        sh("Sırayla kelime söylenir; her yeni kelime bir önceki kelimenin son harfiyle başlamalıdır. Örnek: KALEM → MASA → ARI.",
            "Players take turns; each new word must start with the last letter of the previous one. Example: APPLE → EAGLE → EARTH."),
        sh("Her hamle için 15 saniyen var ve oyun 10 hamle sürer. Oyunda daha önce kullanılan kelime tekrar edilemez.",
            "You have 15 seconds per move and a game lasts 10 turns. A word already used in the game cannot be repeated."),
        sh("Sözlükte olmayan kelime kabul edilmez; süren bitmeden başka bir kelime deneyebilirsin. Süre dolarsa sıra rakibe geçer. Uzun kelimeler daha çok puan getirir.",
            "A word that is not in the dictionary is rejected; you can try another before your time ends. If time runs out, the turn passes to your rival. Longer words earn more points."),
    )),
    LobbyGuideTopic("siege", Icons.Rounded.GridOn, sh("Kelime Kuşatması", "Word Siege"), listOf(
        sh("Elindeki taşlarla tahtaya kelime kur ve tahtada toprak kazan.", "Build words with your tiles and win ground on the board."),
        sh("Açılış kelimesi tahtanın ortasındaki Başlangıç Mührü'nden geçmelidir. Sonraki her kelime tek satırda ya da tek sütunda dizilir ve tahtadaki bir harfe dokunur.",
            "The opening word must pass through the Starting Seal in the middle. Every later word sits in one row or column and touches a letter on the board."),
        sh("Kelime puanı kalıcıdır. Ele geçirdiğin her küp +2 küp puanı yazar; rakipten kopardığın küp onun küp puanını 1 azaltır. Bonus kareler yalnız üzerine ilk taş konduğunda çalışır.",
            "Word points are permanent. Each cube you take is worth +2 territory points; a cube pulled from your rival lowers theirs by 1. A bonus square works only for the first tile placed on it."),
        sh("Oyun sonunda kelime ve küp puanı toplamı yüksek olan kazanır. Süresi dolan ya da pes eden oyuncu hükmen kaybeder.",
            "Highest word plus territory total wins. A player who runs out of time or resigns loses by forfeit."),
    )),
    LobbyGuideTopic("workshop", Icons.Rounded.Construction, sh("Kelime Atölyesi", "Word Workshop"), listOf(
        sh("Sana verilen 7 harften süre bitmeden en az 3 harfli kelimeler kur. Her harf 10 puan, tamamlanan her görev 50 puan değerindedir.",
            "Build words of at least 3 letters from the 7 letters you get before time runs out. Each letter is worth 10 points and each completed task 50."),
        sh("Görevler üçerli setler hâlinde gelir; bir kelime uyduğu bütün görevleri aynı anda tamamlar.",
            "Tasks come in sets of three; one word completes every task it fits at once."),
        sh("Atölye turnuvaları her 2 saatte bir ×1,5 XP ile açılır. Her akşam 19.00–22.00 arasında tüm oyunlar ×2 XP verir; bonuslar birleşmez. Kürsüye girenler ana sayfada gösterilir.",
            "Workshop tournaments open every 2 hours with ×1.5 XP. Every evening from 19:00–22:00 all games award ×2 XP; bonuses do not stack. Podium finishers are shown in the lobby."),
    )),
    LobbyGuideTopic("league", Icons.Rounded.WorkspacePremium, sh("Lig", "League"), listOf(
        sh("Ligin, dereceli maçlardaki puanına (RP) göre belirlenir. Kazandıkça puanın artar, kaybettikçe azalır.",
            "Your league follows your rating (RP) in ranked matches. Wins raise it and losses lower it."),
        sh("Bronz: 1100 altı · Gümüş: 1100 · Altın: 1250 · Platin: 1400 · Elmas: 1600 · Efsane: 1800 ve üzeri.",
            "Bronze: below 1100 · Silver: 1100 · Gold: 1250 · Platinum: 1400 · Diamond: 1600 · Legend: 1800 and above."),
        sh("Yapay zekâ rakibe karşı oynanan antrenman oyunları lig puanını değiştirmez.",
            "Practice games against the AI do not change your rating."),
    )),
    LobbyGuideTopic("throne", Icons.Rounded.EmojiEvents, sh("Taht", "Throne"), listOf(
        sh("Taht, üç oyunun tamamını kapsayan haftalık yarıştır. Kelime Tahtı, Son Harf ve Kelime Atölyesi'nde kazandığın tecrübe puanı (XP) tek bir sıralamada toplanır.",
            "The Throne is a weekly race across all three games. Experience (XP) earned in Word Throne, Last Letter and Word Workshop adds up in one ranking."),
        sh("Haftalık görevleri tamamlayarak ek puan kazanırsın. Hafta sonunda zirvedeki oyuncu tahta oturur ve bir hafta boyunca Taht sayfasında gösterilir.",
            "Weekly missions give extra points. The leader at the end of the week takes the throne and is shown on the Throne page for a week."),
        sh("Sıralama her hafta sıfırlanır; geri sayım Taht sayfasında yer alır.",
            "The ranking resets every week; the countdown is on the Throne page."),
    )),
    LobbyGuideTopic("dictionary", Icons.Rounded.Book, sh("Sözlük", "Dictionary"), listOf(
        sh("Türkçe sözlük: Oyunlarda geçerli sayılan Türkçe kelimeler, Türk Dil Kurumu (TDK) Güncel Türkçe Sözlük'ün resmî madde başlıklarından derlenmiştir ve TDK yazımıyla uyumludur. Bir kelimenin geçerliliği bu sözlükte madde başı olarak yer almasına bağlıdır.",
            "Turkish dictionary: Turkish words accepted in the games are compiled from the official headwords of the Turkish Language Association (TDK) Current Turkish Dictionary and follow TDK spelling. A word is valid if it appears there as a headword."),
        sh("Yalnızca özel isim olarak kullanılan sözcükler (kişi, şehir, ülke, kurum ve marka adları) ile kısaltmalar oyun içinde geçersizdir. Bir isim aynı zamanda TDK sözlüğünde anlamı olan bir sözcükse geçerlidir; örneğin «deniz» hem kişi adı hem de bildiğimiz deniz olduğundan özel isim sayılmaz ve kabul edilir. Aynı durum «çiçek», «umut» ve «güneş» gibi sözcükler için de geçerlidir.",
            "Words used only as proper nouns (names of people, cities, countries, organisations and brands) and abbreviations are invalid in the games. A name that is also a meaningful TDK word is valid; for example «deniz» is both a given name and the word for sea, so it is not treated as a proper noun and is accepted. The same holds for words like «çiçek», «umut» and «güneş»."),
        sh("Şapkalı harfler (â, î, û) düz harf olarak yazılabilir. TDK'nın yabancı kelime olarak gösterdiği ve Türkçe yazımı bulunmayan sözcükler (ör. marketing) kabul edilmez.",
            "Circumflexed letters (â, î, û) can be typed as plain letters. Foreign words that TDK lists without a Turkish spelling (e.g. marketing) are not accepted."),
        sh("İngilizce sözlük: Oyunlarda geçerli sayılan İngilizce kelimeler, İngilizce yazım denetleyicilerinin ortak kaynağı olan SCOWL (Spell Checker Oriented Word Lists) ve English Speller Database (ESDB) Amerikan İngilizcesi listesinden alınmıştır. İngilizcede TDK'ya karşılık gelen tek bir resmî dil kurumu bulunmadığından, bu liste uluslararası ölçüde kabul görmüş açık kaynaklı yazım standardı olarak esas alınmıştır.",
            "English dictionary: English words accepted in the games come from the American English list of SCOWL (Spell Checker Oriented Word Lists) and the English Speller Database (ESDB), the common source of English spell checkers. English has no single official language academy, so this widely adopted open spelling standard is used."),
        sh("İngilizcede de özel isimler, kısaltmalar ve tek başına harfler geçersizdir.",
            "In English too, proper nouns, abbreviations and single letters are invalid."),
    )),
)

@Composable
internal fun LobbyGuideDialog(topic: LobbyGuideTopic, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(topic.icon, null, tint = LobbyBrand.Chip) },
        title = { Text(topic.title, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topic.paragraphs.forEach { Text(it, fontSize = 14.sp, lineHeight = 20.sp, color = Color.Unspecified) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("TAMAM", "OK")) } },
    )
}
