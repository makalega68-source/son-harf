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
        sh("Sırayla kelime söylenir; her yeni kelime bir önceki kelimenin son harfiyle başlamalıdır. Örnek: KALEM → MASA → ARI. Maçta bir kez kullanılan kelime tekrar edilemez.",
            "Players take turns; each new word must start with the last letter of the previous one. Example: APPLE → EAGLE → EARTH. A word used once in the match cannot be repeated."),
        sh("Maç 3 raunttur; 2 raunt alan kazanır. Her rauntta iki oyuncunun da 10'ar hamlesi vardır ve her hamle için 15 saniye süre verilir. Rauntlar arasında 10 saniyelik hazırlık molası olur.",
            "A match has 3 rounds; the first to win 2 takes it. In each round both players get 10 moves, with 15 seconds per move. There is a 10-second break between rounds."),
        sh("Her geçerli kelime 3 puan getirir; art arda 5 doğru kelimede o kelime 6 puan değerindedir. Raundu, o rauntta daha çok puan toplayan alır.",
            "Each valid word scores 3 points; every 5th correct word in a row is worth 6. The player with more points in a round wins that round."),
        sh("Sözlükte olmayan, yanlış harfle başlayan ya da daha önce kullanılmış kelime 1 puan düşürür, seriyi sıfırlar ve sıra rakibe geçer. Süre dolarsa da sıra rakibe geçer.",
            "A word that is not in the dictionary, starts with the wrong letter or was already used costs 1 point, resets your streak and passes the turn. Running out of time also passes the turn."),
        sh("3 raunt sonunda eşitlik varsa Ani Ölüm başlar: ilk geçerli kelimeyi yazan kazanır, hata yapan kaybeder. Rakip bulunamazsa yapay zekâ (AI) rakiple oynarsın.",
            "If it is still level after 3 rounds, Sudden Death starts: the first valid word wins and a mistake loses. If no rival is found you play the AI."),
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
        if (GameAvailability.wordSiege || GameAvailability.wordWorkshop) sh("Taht, üç oyunun tamamını kapsayan haftalık yarıştır. Kelime Kuşatması, Son Harf ve Kelime Atölyesi'nde kazandığın tecrübe puanı (XP) tek bir sıralamada toplanır.",
            "The Throne is a weekly race across all three games. Experience (XP) earned in Word Siege, Last Letter and Word Workshop adds up in one ranking.")
        else sh("Taht, haftalık rekabet alanıdır. Son Harf maçlarında tecrübe puanı (XP) kazanır, haftanın sıralamasında yükselirsin.",
            "The Throne is your weekly competition. Earn experience (XP) in Last Letter matches and climb the weekly ranking."),
        sh("Haftalık görevleri tamamlayarak ek puan kazanırsın. Hafta sonunda zirvedeki oyuncu tahta oturur: 7 gün boyunca altın çerçeve, 1 numara rozeti ve yanında Altın Kral maskotu onundur; Taht sayfasında gösterilir.",
            "Weekly missions give extra points. The leader at the end of the week takes the throne: for 7 days they get the gold frame, the No. 1 badge and the Golden King mascot, and are shown on the Throne page."),
        sh("Haftanın ilk üçü Eve parçası kazanır: 1.'ye 3, 2.'ye 2, 3.'ye 1 parça. 12 parçayı toplayan, 3B orman dostu Eve'in sahibi olur.",
            "The week's top three earn Eve shards: 3 for 1st, 2 for 2nd, 1 for 3rd. Collect 12 shards to own Eve, the 3D forest friend."),
        sh("Sıralama her hafta sıfırlanır; geri sayım Taht sayfasında yer alır.",
            "The ranking resets every week; the countdown is on the Throne page."),
    )),
    LobbyGuideTopic("mascots", Icons.Rounded.Pets, sh("Maskotlar", "Mascots"), listOf(
        sh("Maskotun maçlarda yanında durur, hamlelerine tepki verir ve konuşma balonlarıyla seni yüreklendirir. Maskot sahipleri her maçta 3 ipucu kazanır; ipucu bittiğinde Mağaza > Video Ödülleri'nden video izleyerek +2 ipucu alabilirsin.",
            "Your mascot stands by you in matches, reacts to your moves and cheers you on in speech bubbles. Mascot owners get 3 hints every match; when they run out, watch a video in Store > Video Rewards for +2 hints."),
        sh("Obi mağazadan alınabilir. Eve, Taht'ta kazanılan 12 parçayla açılan 3B orman dostudur. Altın Kral ise yalnızca tahttaki oyuncuya bir haftalığına eşlik eder.",
            "Obi can be bought in the Store. Eve is the 3D forest friend unlocked with 12 shards won on the Throne. The Golden King accompanies only the throne holder, for one week."),
    )),
    LobbyGuideTopic("account", Icons.Rounded.ManageAccounts, sh("Hesap", "Account"), listOf(
        sh("HEMEN OYNA ile e-posta ya da şifre olmadan oyuncu adını yazıp hemen başlarsın. Altınların, puanın ve ürünlerin bu hesapta sunucuda saklanır.",
            "With PLAY NOW you just type a player name and start, no email or password. Your gold, rating and items are kept on the server under this account."),
        sh("Misafir hesap yalnız bu telefona bağlıdır. Telefon değişirse ya da uygulama silinirse kaybolmaması için ana sayfadaki ya da Ayarlar > Hesap'taki düğmeyle Google hesabına bağla; aynı hesap korunur, hiçbir şey silinmez.",
            "A guest account is tied to this phone. So it isn't lost if you change phones or delete the app, link it to Google from the lobby banner or Settings > Account; the same account is kept and nothing is erased."),
        sh("Google ile devam et düğmesiyle başka bir telefonda da hesabına geri dönersin. İstersen e-posta ile giriş de kullanılabilir.",
            "With Continue with Google you get your account back on any phone. Email sign-in is still available too."),
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
).filter { topic -> when (topic.id) {
    "siege" -> GameAvailability.wordSiege
    "workshop" -> GameAvailability.wordWorkshop
    else -> true
} }

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
