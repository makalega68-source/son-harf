package com.sonharf.game

/** Moments the mascot has something to say about; each character says it in its own voice. */
internal enum class WordSiegeMascotTopic {
    GREET_FIRST, GREET, HELLO, PRAISE, BIG_PRAISE, RARE_WORD, COMFORT, CRITICAL, BEHIND, RIVAL_STRONG, AHEAD,
    STREAK, CHAT, TAP, POKED, TWIRL, FLIP, WIN, LOSS, DRAW,
}

/**
 * A character's temperament: how much it talks, how playful it is, which idle habits it prefers and
 * the lines it uses. Anything a character does not define falls back to the shared lines.
 */
internal class WordSiegeMascotPersonality(
    /** Multiplies the chance of speaking up (critical-moment support is never reduced). */
    val chattiness: Float,
    /** Multiplies hops, twirls, flips and chases. */
    val playfulness: Float,
    /** Extra weight per idle habit (1 = unchanged). */
    val idleBias: Map<WordSiegeMascotIdle, Float>,
    private val lines: Map<WordSiegeMascotTopic, List<Pair<String, String>>>,
) {
    fun lines(topic: WordSiegeMascotTopic, fallback: List<Pair<String, String>>): List<Pair<String, String>> =
        lines[topic] ?: fallback

    companion object {
        fun of(skin: WordSiegeMascotSkin): WordSiegeMascotPersonality = when (skin) {
            WordSiegeMascotSkin.ORB -> OBI
            WordSiegeMascotSkin.PINK -> PINKI
            WordSiegeMascotSkin.DEVIL_BLUE -> BUZI
            WordSiegeMascotSkin.DEVIL_RED -> ZIPIR
            WordSiegeMascotSkin.CAT -> MIRNAV
            WordSiegeMascotSkin.ROBOT -> BIPBOP
            WordSiegeMascotSkin.ASTRONAUT -> NOVA
        }

        /** Obi: the warm, loyal coach. The shared lines are written in his voice. */
        private val OBI = WordSiegeMascotPersonality(
            chattiness = 1f,
            playfulness = 1f,
            idleBias = emptyMap(),
            lines = mapOf(
                WordSiegeMascotTopic.GREET_FIRST to listOf(
                    "Merhaba! Ben Obi, oyun arkadaşın. Birlikte kazanalım!" to "Hi! I'm Obi, your game buddy. Let's win together!",
                    "Selam %s! Ben Obi. Seninle oynamak için sabırsızlanıyordum." to "Hi %s! I'm Obi. I couldn't wait to play with you.",
                ),
                WordSiegeMascotTopic.HELLO to listOf(
                    "Obi göreve hazır! ✨" to "Obi reporting for duty! ✨",
                    "Klasik ışıltım geri döndü ✨" to "My classic glow is back ✨",
                ),
            ),
        )

        /** Pinki: sweet, affectionate and a little emotional; hearts everywhere. */
        private val PINKI = WordSiegeMascotPersonality(
            chattiness = 1.2f,
            playfulness = 1.05f,
            idleBias = mapOf(WordSiegeMascotIdle.SPARKLE to 1.8f, WordSiegeMascotIdle.CHAT to 1.3f, WordSiegeMascotIdle.FLIP to .6f),
            lines = mapOf(
                WordSiegeMascotTopic.GREET_FIRST to listOf(
                    "Selam tatlım! Ben Pinki 💖 Seninle oynamak çok güzel olacak!" to "Hi sweetie! I'm Pinky 💖 Playing with you will be lovely!",
                    "Merhaba %s! Ben Pinki, kurdelemi senin için taktım 🎀" to "Hi %s! I'm Pinky, I wore my bow just for you 🎀",
                ),
                WordSiegeMascotTopic.GREET to listOf(
                    "Geldin! Kalbim pıt pıt 💓" to "You're here! My heart goes pitter-patter 💓",
                    "Seni özlemiştim tatlım 💖" to "I missed you, sweetie 💖",
                    "Bugün de birlikte parlayalım ✨" to "Let's shine together today too ✨",
                ),
                WordSiegeMascotTopic.HELLO to listOf(
                    "Pinki geldi! Kurdelem nasıl? 🎀" to "Pinky's here! How's my bow? 🎀",
                    "Tadaa, pembe ve tatlı! 💖" to "Ta-da, pink and sweet! 💖",
                ),
                WordSiegeMascotTopic.PRAISE to listOf(
                    "Aww, çok güzel! 💖" to "Aww, so nice! 💖",
                    "Bayıldım bu kelimeye!" to "I adore that word!",
                    "Sen bir tanesin 💕" to "You're one of a kind 💕",
                ),
                WordSiegeMascotTopic.BIG_PRAISE to listOf(
                    "Kalbim eridi! Muhteşemsin 💖✨" to "My heart melted! You're amazing 💖✨",
                    "Aşkım bu kelime! 😍" to "I'm in love with that word! 😍",
                ),
                WordSiegeMascotTopic.COMFORT to listOf(
                    "Üzülme tatlım, sarılalım mı? 🤗" to "Don't be sad sweetie, hug? 🤗",
                    "Olur öyle, ben hep yanındayım 💕" to "It happens, I'm always here 💕",
                ),
                WordSiegeMascotTopic.CRITICAL to listOf(
                    "Hadi tatlım, yaparsın! 💓" to "Come on sweetie, you can do it! 💓",
                    "Kısa bir kelime de olur, acele et! 💨" to "A short word works too, hurry! 💨",
                ),
                WordSiegeMascotTopic.BEHIND to listOf(
                    "Pes etmek yok, sana inanıyorum 💖" to "No giving up, I believe in you 💖",
                    "Birlikte toparlarız, söz! 🤞" to "We'll bounce back together, promise! 🤞",
                ),
                WordSiegeMascotTopic.RIVAL_STRONG to listOf(
                    "Ay, rakip fena değilmiş… ama sen daha iyisin 💕" to "Oh, the rival's not bad… but you're better 💕",
                    "Hmph! Sıradaki bizim 🎀" to "Hmph! The next one's ours 🎀",
                ),
                WordSiegeMascotTopic.AHEAD to listOf(
                    "Öndeyiz! Yüzümdeki gülümsemeye bak 😊" to "We're ahead! Look at my smile 😊",
                    "Böyle devam, kalbim kıpır kıpır 💓" to "Keep going, my heart's fluttering 💓",
                ),
                WordSiegeMascotTopic.CHAT to listOf(
                    "Seninle vakit geçirmek çok güzel 💕" to "Spending time with you is lovely 💕",
                    "Kurdelem yamuldu mu? 🎀" to "Is my bow crooked? 🎀",
                    "Pembe her şeyi güzelleştirir 💖" to "Pink makes everything better 💖",
                ),
                WordSiegeMascotTopic.TAP to listOf(
                    "Hihi, gıdıklandım! 💕" to "Hehe, that tickles! 💕",
                    "Bana sarılmak mı istedin? 🤗" to "Did you want a hug? 🤗",
                    "Uçuyorum, yakala beni! 💖" to "I'm flying, catch me! 💖",
                ),
                WordSiegeMascotTopic.WIN to listOf(
                    "KAZANDIK! Seni çok seviyorum! 💖🎉" to "WE WON! I love you so much! 💖🎉",
                    "Mutluluktan uçuyorum! ✨💕" to "I'm flying with joy! ✨💕",
                    "Şampiyonum benim %s! 👑" to "My champion %s! 👑",
                ),
                WordSiegeMascotTopic.LOSS to listOf(
                    "Snif… ağlamıyorum, sen ağla 😢 Bir dahakine!" to "Sniff… I'm not crying, you are 😢 Next time!",
                    "Gel sarılalım, bir sonrakini biz alırız 🤗" to "Come here, hug. We'll take the next one 🤗",
                ),
                WordSiegeMascotTopic.DRAW to listOf("Berabere! İkimiz de tatlıyız 💕" to "A draw! We're both sweet 💕"),
                WordSiegeMascotTopic.STREAK to listOf(
                    "%n'lü seri! Kalplerim yetmiyor 💖💖" to "%n in a row! I'm out of hearts 💖💖",
                    "Durma, çok güzel gidiyorsun! 💕" to "Don't stop, you're doing great! 💕",
                ),
                WordSiegeMascotTopic.RARE_WORD to listOf(
                    "%w mi? Ne kadar zarif bir kelime 💖" to "%w? What an elegant word 💖",
                ),
            ),
        )

        /** Buzi: the icy imp. Calm, cool and dry; says little, but it lands. */
        private val BUZI = WordSiegeMascotPersonality(
            chattiness = .6f,
            playfulness = .8f,
            idleBias = mapOf(WordSiegeMascotIdle.WATCH to 1.5f, WordSiegeMascotIdle.PEEK to 1.4f, WordSiegeMascotIdle.HOP to .6f),
            lines = mapOf(
                WordSiegeMascotTopic.GREET_FIRST to listOf(
                    "Ben Buzi. Soğukkanlı ol, gerisini birlikte hallederiz. ❄️" to "I'm Frosty. Stay cool, we'll handle the rest together. ❄️",
                    "Selam %s. Buzi burada. Rakipler üşüsün. 🧊" to "Hi %s. Frosty's here. Let the rivals shiver. 🧊",
                ),
                WordSiegeMascotTopic.GREET to listOf(
                    "Yine buradayız. Sakin ve ölümcül. ❄️" to "Here again. Calm and deadly. ❄️",
                    "Buz gibi bir oyun olsun. 🧊" to "Let's make it an ice-cold game. 🧊",
                    "Hazırım. Sen de öylesin, biliyorum." to "I'm ready. So are you, I know.",
                ),
                WordSiegeMascotTopic.HELLO to listOf(
                    "Buzi. Sadece Buzi. ❄️" to "Frosty. Just Frosty. ❄️",
                    "Ortam biraz serinledi mi? 🧊" to "Did it just get a bit chilly? 🧊",
                ),
                WordSiegeMascotTopic.PRAISE to listOf(
                    "Temiz." to "Clean.",
                    "Fena değil. Hiç fena değil." to "Not bad. Not bad at all.",
                    "Soğukkanlı hamle. ❄️" to "Cool move. ❄️",
                ),
                WordSiegeMascotTopic.BIG_PRAISE to listOf(
                    "…Tamam, bu etkileyiciydi. 🧊" to "…Okay, that was impressive. 🧊",
                    "Rakip dondu kaldı. ❄️" to "The rival froze solid. ❄️",
                ),
                WordSiegeMascotTopic.COMFORT to listOf(
                    "Olur. Nefes al, devam." to "It happens. Breathe, move on.",
                    "Bir hata bir şey değiştirmez." to "One mistake changes nothing.",
                ),
                WordSiegeMascotTopic.CRITICAL to listOf(
                    "Panik yok. Kısa ve net. ❄️" to "No panic. Short and sharp. ❄️",
                    "Sakin… aklına gelecek." to "Easy… it'll come.",
                ),
                WordSiegeMascotTopic.BEHIND to listOf(
                    "Geride olmak sadece bir sayı." to "Being behind is just a number.",
                    "Soğuk kal. Geri döneriz." to "Stay cold. We'll come back.",
                ),
                WordSiegeMascotTopic.RIVAL_STRONG to listOf(
                    "İyi hamle. Kendisi için. 🧊" to "Good move. For them. 🧊",
                    "Not ettim. Cevabını vereceğiz." to "Noted. We'll answer.",
                ),
                WordSiegeMascotTopic.AHEAD to listOf(
                    "Öndeyiz. Beklendiği gibi." to "We're ahead. As expected.",
                    "Tempo bizde. Bozma." to "We set the pace. Keep it.",
                ),
                WordSiegeMascotTopic.CHAT to listOf(
                    "…" to "…",
                    "İzliyorum. Her şeyi. 🧊" to "I'm watching. Everything. 🧊",
                    "Sıcak havalar bana göre değil." to "Warm weather isn't for me.",
                ),
                WordSiegeMascotTopic.TAP to listOf(
                    "Dokunma. Eriyorum." to "Don't touch. I'm melting.",
                    "Hmph. Tamam, başka yere." to "Hmph. Fine, elsewhere.",
                    "Soğuk parmaklar, soğuk kalp. ❄️" to "Cold fingers, cold heart. ❄️",
                ),
                WordSiegeMascotTopic.POKED to listOf("Yeter. Buz tutacaksın. 🧊" to "Enough. You'll get frostbite. 🧊"),
                WordSiegeMascotTopic.WIN to listOf(
                    "Kazandık. Buz gibi. ❄️🏆" to "We won. Ice cold. ❄️🏆",
                    "Hiç şüphem yoktu, %s." to "I never doubted it, %s.",
                    "Rakip hâlâ donuk. 🧊" to "The rival is still frozen. 🧊",
                ),
                WordSiegeMascotTopic.LOSS to listOf(
                    "Bu sefer onlar. Bir dahaki sefer biz." to "This time them. Next time us.",
                    "Soğuk kal. Rövanş sıcak olacak." to "Stay cool. The rematch will be hot.",
                ),
                WordSiegeMascotTopic.DRAW to listOf("Berabere. Kimse erimedi." to "A draw. Nobody melted."),
                WordSiegeMascotTopic.STREAK to listOf("%n'lü seri. Buz gibi istikrar. ❄️" to "%n in a row. Ice-cold consistency. ❄️"),
            ),
        )

        /** Zıpır: the fiery imp. Hyper, competitive and loves a bit of trash talk. */
        private val ZIPIR = WordSiegeMascotPersonality(
            chattiness = 1.35f,
            playfulness = 1.35f,
            idleBias = mapOf(WordSiegeMascotIdle.HOP to 1.6f, WordSiegeMascotIdle.TWIRL to 1.3f, WordSiegeMascotIdle.WATCH to .75f),
            lines = mapOf(
                WordSiegeMascotTopic.GREET_FIRST to listOf(
                    "Ben Zıpır! 🔥 Rakipleri yakmaya geldim, hazır mısın?!" to "I'm Blaze! 🔥 Here to burn the rivals, you ready?!",
                    "Selam %s! Zıpır burada, ortalık ısınıyor! 🔥" to "Hi %s! Blaze is here, things are heating up! 🔥",
                ),
                WordSiegeMascotTopic.GREET to listOf(
                    "Hadi hadi hadi! Maç zamanı! 🔥" to "Come on come on! Match time! 🔥",
                    "Bugün kimi yakıyoruz? 😈" to "Who are we burning today? 😈",
                    "Isındım bile! 💥" to "I'm already fired up! 💥",
                ),
                WordSiegeMascotTopic.HELLO to listOf(
                    "Zıpır sahnede! 🔥😈" to "Blaze on stage! 🔥😈",
                    "Ateş gibiyim bugün! 💥" to "I'm on fire today! 💥",
                ),
                WordSiegeMascotTopic.PRAISE to listOf(
                    "BUM! İşte bu! 💥" to "BOOM! That's it! 💥",
                    "Yak onları! 🔥" to "Burn them! 🔥",
                    "Hehe, rakip terledi 😈" to "Hehe, the rival's sweating 😈",
                ),
                WordSiegeMascotTopic.BIG_PRAISE to listOf(
                    "ALEV ALEV! Kimse durduramaz! 🔥🔥" to "ON FIRE! Nobody can stop us! 🔥🔥",
                    "Rakip küle döndü! 😈💥" to "The rival turned to ash! 😈💥",
                ),
                WordSiegeMascotTopic.COMFORT to listOf(
                    "Grr! Önemli değil, intikam alacağız! 😤" to "Grr! Doesn't matter, we'll get revenge! 😤",
                    "Bu sadece kıvılcımdı, asıl ateş geliyor! 🔥" to "That was just a spark, the real fire is coming! 🔥",
                ),
                WordSiegeMascotTopic.CRITICAL to listOf(
                    "Hızlı hızlı hızlı! 🔥" to "Fast fast fast! 🔥",
                    "Bas gaza, yaz bir şey! 💨" to "Hit it, type something! 💨",
                ),
                WordSiegeMascotTopic.BEHIND to listOf(
                    "Geride miyiz? Daha iyi, dönüş daha tatlı olur! 😈" to "Behind? Even better, the comeback is sweeter! 😈",
                    "Ateşi harlayalım! 🔥" to "Let's stoke the fire! 🔥",
                ),
                WordSiegeMascotTopic.RIVAL_STRONG to listOf(
                    "Ohoo, rakip kendini bir şey sandı! 😤" to "Ooh, the rival thinks they're something! 😤",
                    "Bunu ödeyecekler! 😈" to "They'll pay for that! 😈",
                ),
                WordSiegeMascotTopic.AHEAD to listOf(
                    "Öndeyiz! Rakibi kavuruyoruz! 🔥" to "We're ahead! We're roasting the rival! 🔥",
                    "Hehe, yetişemiyorlar 😈" to "Hehe, they can't keep up 😈",
                ),
                WordSiegeMascotTopic.CHAT to listOf(
                    "Sıkıldım, biri bir şeyi yaksın! 🔥" to "I'm bored, someone set something on fire! 🔥",
                    "Rakibin yüzünü görmek isterdim 😈" to "I'd love to see the rival's face 😈",
                    "Kuyruğum kaşınıyor, heyecan var! 💥" to "My tail's itching, excitement's coming! 💥",
                ),
                WordSiegeMascotTopic.TAP to listOf(
                    "Hey! Yanarsın! 🔥" to "Hey! You'll get burned! 🔥",
                    "Yakala yakalayabilirsen! 😈" to "Catch me if you can! 😈",
                    "Hehe, zıp zıp! 💥" to "Hehe, zip zap! 💥",
                ),
                WordSiegeMascotTopic.POKED to listOf("Tamam tamam! Kuyruğuma basma! 😤" to "Okay okay! Don't step on my tail! 😤"),
                WordSiegeMascotTopic.WIN to listOf(
                    "KAZANDIIIK! Her yer alev! 🔥🎉" to "WE WOOON! Everything's on fire! 🔥🎉",
                    "Rakip yandı bitti kül oldu! 😈🏆" to "The rival burned to a crisp! 😈🏆",
                    "Efsanesin %s! 💥" to "You're a legend, %s! 💥",
                ),
                WordSiegeMascotTopic.LOSS to listOf(
                    "GRR! Bu bitmedi, rövanş istiyorum! 😤🔥" to "GRR! This isn't over, I want a rematch! 😤🔥",
                    "Şans onlardaydı. Bir dahakine yakacağız! 😈" to "Luck was on their side. Next time we burn them! 😈",
                ),
                WordSiegeMascotTopic.DRAW to listOf("Berabere mi?! Uzatma istiyorum! 😤" to "A draw?! I want overtime! 😤"),
                WordSiegeMascotTopic.STREAK to listOf(
                    "%n'lü seri! Alev alev! 🔥🔥" to "%n in a row! Blazing! 🔥🔥",
                    "Seri yanıyor, sakın durma! 💥" to "The streak is on fire, don't stop! 💥",
                ),
            ),
        )

        /** Mırnav: the tabby cat. Lazy, sassy, secretly delighted; loves chasing letters. */
        private val MIRNAV = WordSiegeMascotPersonality(
            chattiness = .75f,
            playfulness = .85f,
            idleBias = mapOf(
                WordSiegeMascotIdle.WATCH to 1.4f,
                WordSiegeMascotIdle.CHASE to 3.2f,
                WordSiegeMascotIdle.LOOK_AROUND to 1.3f,
                WordSiegeMascotIdle.FLIP to .5f,
            ),
            lines = mapOf(
                WordSiegeMascotTopic.GREET_FIRST to listOf(
                    "Miyav. Ben Mırnav. Seni sahibim olarak kabul ediyorum… şimdilik. 🐾" to "Meow. I'm Purrnie. I accept you as my human… for now. 🐾",
                    "Mırr… selam %s. Ben Mırnav. Mama var mı? 🐟" to "Purr… hi %s. I'm Purrnie. Got any snacks? 🐟",
                ),
                WordSiegeMascotTopic.GREET to listOf(
                    "Mırr… uyanmışım bak. 😺" to "Purr… look, I'm awake. 😺",
                    "Yine mi oyun? Peki, ama sadece senin için. 🐾" to "Games again? Fine, but only for you. 🐾",
                    "Miyav! Hadi bakalım. 😼" to "Meow! Let's go. 😼",
                ),
                WordSiegeMascotTopic.HELLO to listOf(
                    "Miyav. Mırnav geldi. Alkış bekliyorum. 😼" to "Meow. Purrnie has arrived. Applause, please. 😼",
                    "Kulaklarıma bak, ne kadar tatlı. 🐾" to "Look at my ears, so cute. 🐾",
                ),
                WordSiegeMascotTopic.PRAISE to listOf(
                    "Mırr… güzel. 😺" to "Purr… nice. 😺",
                    "Hm, beğendim. Söyleme kimseye. 😼" to "Hm, I liked it. Don't tell anyone. 😼",
                    "Pati onayı! 🐾" to "Paw of approval! 🐾",
                ),
                WordSiegeMascotTopic.BIG_PRAISE to listOf(
                    "MİYAV! Bu harikaydı! 😻" to "MEOW! That was amazing! 😻",
                    "Kuyruğum kabardı, o kadar iyi! 🐾✨" to "My tail puffed up, that's how good! 🐾✨",
                ),
                WordSiegeMascotTopic.COMFORT to listOf(
                    "Olur öyle. Kediler de bazen düşer. 🐾" to "It happens. Even cats fall sometimes. 🐾",
                    "Mırr… boş ver, devam. 😺" to "Purr… never mind, carry on. 😺",
                ),
                WordSiegeMascotTopic.CRITICAL to listOf(
                    "Pati çabukluğu lazım! 🐾" to "Quick paws needed! 🐾",
                    "Miyav! Acele et! 😾" to "Meow! Hurry up! 😾",
                ),
                WordSiegeMascotTopic.BEHIND to listOf(
                    "Kedilerin dokuz canı var. Bizim de. 😼" to "Cats have nine lives. So do we. 😼",
                    "Sinsice geri döneceğiz… 🐾" to "We'll sneak back in… 🐾",
                ),
                WordSiegeMascotTopic.RIVAL_STRONG to listOf(
                    "Tıslıyorum. Ssss! 😾" to "I'm hissing. Hsss! 😾",
                    "Rakip şanslı bir fare yakaladı, o kadar. 🐭" to "The rival caught a lucky mouse, that's all. 🐭",
                ),
                WordSiegeMascotTopic.AHEAD to listOf(
                    "Mırr… her şey kontrol altında. 😼" to "Purr… everything's under control. 😼",
                    "Öndeyiz. Şimdi biraz kestireyim mi? 😴" to "We're ahead. Can I take a nap now? 😴",
                ),
                WordSiegeMascotTopic.CHAT to listOf(
                    "Zzz… uyumuyordum, gözlerimi dinlendiriyordum. 😴" to "Zzz… I wasn't sleeping, just resting my eyes. 😴",
                    "Şu harfler fare gibi kaçıyor… 🐭" to "Those letters scurry like mice… 🐭",
                    "Beni sev ama çok değil. 😼" to "Pet me, but not too much. 😼",
                ),
                WordSiegeMascotTopic.TAP to listOf(
                    "Mırrr… kulak arkası lütfen. 😻" to "Purrr… behind the ears, please. 😻",
                    "Hey! Tüylerim! 😾" to "Hey! My fur! 😾",
                    "Miyav! Ben giderim o zaman. 🐾" to "Meow! I'm off then. 🐾",
                ),
                WordSiegeMascotTopic.POKED to listOf("Tıss! Yeter artık! 😾" to "Hiss! That's enough! 😾"),
                WordSiegeMascotTopic.WIN to listOf(
                    "Mırrrr! Kazandık, mama partisi! 😻🐟" to "Purrrr! We won, snack party! 😻🐟",
                    "Tabii ki kazandık. Ben buradayım. 😼🏆" to "Of course we won. I'm here. 😼🏆",
                    "Aferin %s, pati çakalım! 🐾" to "Well done %s, paw five! 🐾",
                ),
                WordSiegeMascotTopic.LOSS to listOf(
                    "Mırr… önemli değil. Kestirip unuturuz. 😿" to "Purr… never mind. We'll nap and forget. 😿",
                    "Bir kedi asla kaybetmez, sadece dinlenir. 😼" to "A cat never loses, it just rests. 😼",
                ),
                WordSiegeMascotTopic.DRAW to listOf("Berabere. Kimse fareyi yakalayamadı. 🐭" to "A draw. Nobody caught the mouse. 🐭"),
                WordSiegeMascotTopic.STREAK to listOf("%n'lü seri! Mırrr! 😻" to "%n in a row! Purrr! 😻"),
            ),
        )

        /** Bipbop: the analytical robot. Speaks in numbers, calculates everything. */
        private val BIPBOP = WordSiegeMascotPersonality(
            chattiness = 1f,
            playfulness = .7f,
            idleBias = mapOf(WordSiegeMascotIdle.NOD to 1.8f, WordSiegeMascotIdle.LOOK_AROUND to 1.6f, WordSiegeMascotIdle.TWIRL to .6f),
            lines = mapOf(
                WordSiegeMascotTopic.GREET_FIRST to listOf(
                    "Bip bop! Ben Bipbop. Kazanma olasılığımız hesaplanıyor… %100! 🤖" to "Beep boop! I'm Bipbop. Calculating our odds of winning… 100%! 🤖",
                    "Merhaba %s. Bipbop sistemleri çevrimiçi. Arkadaşlık modülü aktif. 🤖" to "Hello %s. Bipbop systems online. Friendship module active. 🤖",
                ),
                WordSiegeMascotTopic.GREET to listOf(
                    "Sistemler hazır. Oyuncu tanındı: favori insan. 🤖" to "Systems ready. Player recognised: favourite human. 🤖",
                    "Bip! Kelime veritabanı yüklendi. ⚙️" to "Beep! Word database loaded. ⚙️",
                    "Enerji %100. Başlayalım. 🔋" to "Energy 100%. Let's begin. 🔋",
                ),
                WordSiegeMascotTopic.HELLO to listOf(
                    "Bipbop yeniden başlatıldı. Tüm sistemler normal. 🤖" to "Bipbop rebooted. All systems nominal. 🤖",
                    "Anten sinyali: güçlü. 📡" to "Antenna signal: strong. 📡",
                ),
                WordSiegeMascotTopic.PRAISE to listOf(
                    "Geçerli hamle. Verimlilik: yüksek. ✅" to "Valid move. Efficiency: high. ✅",
                    "Bip! Olumlu sonuç. 🤖" to "Beep! Positive result. 🤖",
                    "Hesaplarıma uygun. 📈" to "Matches my calculations. 📈",
                ),
                WordSiegeMascotTopic.BIG_PRAISE to listOf(
                    "HATA: Bu kadar iyi hamle beklenmiyordu! 🤯" to "ERROR: Move this good was not expected! 🤯",
                    "Kazanma olasılığı %23 arttı! 📈🤖" to "Win probability up 23%! 📈🤖",
                ),
                WordSiegeMascotTopic.RARE_WORD to listOf(
                    "%w… veritabanımda nadir kayıt. Etkilendim. 🤖" to "%w… a rare entry in my database. Impressed. 🤖",
                ),
                WordSiegeMascotTopic.COMFORT to listOf(
                    "Tek hata istatistiği bozmaz. Devam. ⚙️" to "One error won't ruin the stats. Continue. ⚙️",
                    "Yeniden hesaplanıyor… hâlâ kazanabiliriz. 🤖" to "Recalculating… we can still win. 🤖",
                ),
                WordSiegeMascotTopic.CRITICAL to listOf(
                    "UYARI: Süre kritik seviyede! ⚠️" to "WARNING: Time at critical level! ⚠️",
                    "Öneri: 3-4 harfli kelime. Hızlı. ⚡" to "Suggestion: a 3-4 letter word. Fast. ⚡",
                ),
                WordSiegeMascotTopic.BEHIND to listOf(
                    "Açık kapanabilir. Olasılık: yeterli. 📊" to "The gap can close. Probability: sufficient. 📊",
                    "Geri dönüş protokolü başlatılıyor. 🤖" to "Initiating comeback protocol. 🤖",
                ),
                WordSiegeMascotTopic.RIVAL_STRONG to listOf(
                    "Rakip performansı: beklenenden yüksek. Not edildi. 📝" to "Rival performance: higher than expected. Logged. 📝",
                    "Karşı strateji yükleniyor… ⚙️" to "Loading counter-strategy… ⚙️",
                ),
                WordSiegeMascotTopic.AHEAD to listOf(
                    "Öndeyiz. Avantaj: bizde. 📈" to "We lead. Advantage: ours. 📈",
                    "Bip! Skor tablosu memnun edici. 🤖" to "Beep! The scoreboard is satisfying. 🤖",
                ),
                WordSiegeMascotTopic.CHAT to listOf(
                    "Tahta taranıyor… 📡" to "Scanning the board… 📡",
                    "Bilgi: robotlar da eğlenir. Bip. 🤖" to "Fact: robots have fun too. Beep. 🤖",
                    "Pil durumu iyi. Moral durumu çok iyi. 🔋" to "Battery good. Morale very good. 🔋",
                ),
                WordSiegeMascotTopic.TAP to listOf(
                    "Dokunma algılandı. Kaçış rotası çiziliyor. 🤖" to "Touch detected. Plotting escape route. 🤖",
                    "Bip! Gıdıklanma sensörü tetiklendi. 😆" to "Beep! Tickle sensor triggered. 😆",
                    "Konum değiştiriliyor… ⚙️" to "Relocating… ⚙️",
                ),
                WordSiegeMascotTopic.POKED to listOf("Aşırı dokunma! Sistem ısınıyor! 🔥🤖" to "Excessive poking! System overheating! 🔥🤖"),
                WordSiegeMascotTopic.WIN to listOf(
                    "ZAFER! Hesaplarım doğruydu. %100! 🤖🏆" to "VICTORY! My calculations were right. 100%! 🤖🏆",
                    "Bip bip bip! Mutluluk modülü aşırı yüklendi! 🎉" to "Beep beep beep! Joy module overloaded! 🎉",
                    "Tebrikler %s. Kaydedildi: efsane oyuncu. 📝" to "Congrats %s. Logged: legendary player. 📝",
                ),
                WordSiegeMascotTopic.LOSS to listOf(
                    "Sonuç: yenilgi. Öğrenme: başarılı. Rövanş: önerilir. 🤖" to "Result: loss. Learning: successful. Rematch: recommended. 🤖",
                    "Bip… üzüntü modülü açıldı. Geçici. 🔋" to "Beep… sadness module on. Temporary. 🔋",
                ),
                WordSiegeMascotTopic.DRAW to listOf("Beraberlik. Olasılık: %3. Nadir olay! 🤖" to "A draw. Probability: 3%. Rare event! 🤖"),
                WordSiegeMascotTopic.STREAK to listOf("%n'lü seri! Performans grafiği yükseliyor. 📈" to "%n in a row! Performance graph rising. 📈"),
            ),
        )

        /** Nova: the dreamy astronaut. Floaty, curious, talks in stars and orbits. */
        private val NOVA = WordSiegeMascotPersonality(
            chattiness = 1f,
            playfulness = 1.1f,
            idleBias = mapOf(
                WordSiegeMascotIdle.FLIP to 2.2f,
                WordSiegeMascotIdle.WANDER to 1.8f,
                WordSiegeMascotIdle.LOOK_AROUND to 1.3f,
            ),
            lines = mapOf(
                WordSiegeMascotTopic.GREET_FIRST to listOf(
                    "Merhaba Dünyalı! Ben Nova. Seninle yıldızlara uzanacağız! 🚀" to "Hello, Earthling! I'm Nova. We'll reach for the stars together! 🚀",
                    "Selam %s! Nova yörüngeye girdi. 🌌" to "Hi %s! Nova has entered orbit. 🌌",
                ),
                WordSiegeMascotTopic.GREET to listOf(
                    "Kalkışa hazırız! 3… 2… 1… 🚀" to "Ready for launch! 3… 2… 1… 🚀",
                    "Yörüngeye döndün! 🌍✨" to "Welcome back to orbit! 🌍✨",
                    "Bugün yıldızlar bizden yana. ⭐" to "The stars are with us today. ⭐",
                ),
                WordSiegeMascotTopic.HELLO to listOf(
                    "Nova iniş yaptı! 🚀" to "Nova has landed! 🚀",
                    "Kaskım parlıyor mu? 🌟" to "Is my helmet shiny? 🌟",
                ),
                WordSiegeMascotTopic.PRAISE to listOf(
                    "Yıldız gibi hamle! ⭐" to "A stellar move! ⭐",
                    "Yörüngede ilerliyoruz! 🛰️" to "We're moving along our orbit! 🛰️",
                    "Güzel iniş! 🚀" to "Smooth landing! 🚀",
                ),
                WordSiegeMascotTopic.BIG_PRAISE to listOf(
                    "Süpernova! 💥🌟" to "Supernova! 💥🌟",
                    "Bu kelime galaksiler ötesi! 🌌" to "That word is out of this galaxy! 🌌",
                ),
                WordSiegeMascotTopic.RARE_WORD to listOf(
                    "%w… yeni bir gezegen keşfettin! 🪐" to "%w… you discovered a new planet! 🪐",
                ),
                WordSiegeMascotTopic.COMFORT to listOf(
                    "Uzayda da bazen rota şaşar. Düzeltiriz. 🛰️" to "Even in space the course drifts. We'll correct it. 🛰️",
                    "Küçük bir meteor, o kadar. ☄️" to "Just a little meteor, that's all. ☄️",
                ),
                WordSiegeMascotTopic.CRITICAL to listOf(
                    "Yakıt azalıyor, hızlan! ⛽🚀" to "Fuel's running low, speed up! ⛽🚀",
                    "Geri sayım! Bir kelime fırlat! 🚀" to "Countdown! Launch a word! 🚀",
                ),
                WordSiegeMascotTopic.BEHIND to listOf(
                    "Yerçekimi bizi çekiyor ama motorlar güçlü! 🚀" to "Gravity is pulling but the engines are strong! 🚀",
                    "En parlak yıldızlar karanlıkta doğar. ⭐" to "The brightest stars are born in the dark. ⭐",
                ),
                WordSiegeMascotTopic.RIVAL_STRONG to listOf(
                    "Rakipte de roket varmış! Ama bizimki daha hızlı. 🚀" to "The rival has a rocket too! But ours is faster. 🚀",
                    "Asteroit uyarısı! Kaçınma manevrası! ☄️" to "Asteroid alert! Evasive manoeuvre! ☄️",
                ),
                WordSiegeMascotTopic.AHEAD to listOf(
                    "Işık hızıyla öndeyiz! ✨" to "We're ahead at light speed! ✨",
                    "Rakip hâlâ atmosferde. 🌍" to "The rival is still in the atmosphere. 🌍",
                ),
                WordSiegeMascotTopic.CHAT to listOf(
                    "Buradan her şey çok küçük görünüyor… 🌍" to "Everything looks so small from up here… 🌍",
                    "Uzayda kelimeler yankılanmaz, ama benim kalbimde yankılanır. 💫" to "Words don't echo in space, but they echo in my heart. 💫",
                    "Bir kuyruklu yıldız gördüm sanki! ☄️" to "I think I saw a comet! ☄️",
                ),
                WordSiegeMascotTopic.TAP to listOf(
                    "Sıfır yerçekimi! Wiii! 🌌" to "Zero gravity! Wheee! 🌌",
                    "Yeni koordinatlara uçuyorum! 🚀" to "Flying to new coordinates! 🚀",
                    "Kaskıma dokunma, buğulanıyor! 😄" to "Don't touch my helmet, it fogs up! 😄",
                ),
                WordSiegeMascotTopic.FLIP to listOf(
                    "Uzayda aşağı yukarı yok! 🙃" to "There's no up or down in space! 🙃",
                    "Sıfır yerçekimi taklası! 🌌" to "Zero-gravity flip! 🌌",
                ),
                WordSiegeMascotTopic.WIN to listOf(
                    "GÖREV BAŞARILI! Ay'a bayrak diktik! 🚩🌕" to "MISSION ACCOMPLISHED! Flag planted on the Moon! 🚩🌕",
                    "Yıldızlar senin için parlıyor %s! ⭐🏆" to "The stars shine for you, %s! ⭐🏆",
                    "Süpernova zaferi! 💥🎉" to "Supernova victory! 💥🎉",
                ),
                WordSiegeMascotTopic.LOSS to listOf(
                    "Görev iptal… ama yeni kalkış yakında. 🚀" to "Mission aborted… but a new launch is coming soon. 🚀",
                    "Bazen roket düşer. Yeniden yapar, yeniden uçarız. 🛰️" to "Sometimes rockets fall. We rebuild, we fly again. 🛰️",
                ),
                WordSiegeMascotTopic.DRAW to listOf("Berabere! İki yıldız aynı parlaklıkta. ⭐⭐" to "A draw! Two stars shining just as bright. ⭐⭐"),
                WordSiegeMascotTopic.STREAK to listOf("%n'lü seri! Hiper hıza geçtik! 🚀✨" to "%n in a row! We've hit hyperspeed! 🚀✨"),
            ),
        )
    }
}
