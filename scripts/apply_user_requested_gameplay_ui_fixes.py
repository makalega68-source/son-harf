from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel, text):
    (ROOT / rel).write_text(text, encoding="utf-8")
    print(f"patched {rel}")


def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f"missing anchor: {label}")
    return text.replace(old, new, 1)


# 1) Main shell: show private-room waiting layer, remove winner/firework overlay,
#    initialize persistent word-meaning cache.
rel = "app/src/main/java/com/sonharf/game/MainActivity.kt"
t = read(rel)
t = replace_once(
    t,
    "        SonHarfPreferences.syncUi(this)\n",
    "        SonHarfPreferences.syncUi(this)\n        WordMeaningRuntime.init(this)\n",
    "MainActivity meaning cache init",
)
t = replace_once(
    t,
    "                    GamePortalApp()\n                    WinnerFireworkOverlay()\n                    FriendsQuickAccessOverlay()",
    "                    GamePortalApp()\n                    PrivateRoomWaitingLayer()\n                    FriendsQuickAccessOverlay()",
    "MainActivity overlay stack",
)
write(rel, t)

# 2) Home: remove duplicate full-width admin button. Header admin icon remains.
rel = "app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt"
t = read(rel)
admin_block = '''        if (isAdmin) item {
            Button(onClick = onAdmin, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ClassicBlue, contentColor = Color.White), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Rounded.AdminPanelSettings, null); Spacer(Modifier.width(8.dp)); Text("YÖNETİCİ PANELİ", fontWeight = FontWeight.Black)
            }
        }
'''
t = replace_once(t, admin_block, "", "Classic full admin button")
write(rel, t)

# 3) Private room flow: surface room code immediately after successful create.
rel = "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt"
t = read(rel)
t = replace_once(
    t,
    '.onSuccess { room = it; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },',
    '.onSuccess { room = it; notice = "Özel oda oluşturuldu: ${it.code}"; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },',
    "Target private create feedback",
)
t = t.replace('"vip_required" in raw -> "Özel oda için VIP gerekli."', '"vip_required" in raw -> "Özel oda oluşturmak için aktif VIP üyeliği gerekli."')
t = t.replace('        else -> "Bağlantı sorunu. Yeniden deneniyor."', '        "player_already_in_game" in raw -> "Devam eden bir maçın varken yeni oda oluşturamazsın."\n        "room_not_available" in raw -> "Oda bulunamadı veya artık müsait değil."\n        else -> "Bağlantı sorunu. Yeniden deneniyor."')
write(rel, t)

# 4) Profile photo + gender badge composable.
rel = "app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt"
t = read(rel)
append = r'''

private fun profileGenderSymbol(gender: String?): String = when (gender?.lowercase()) {
    "kadın", "kadin", "female", "woman" -> "♀"
    "erkek", "male", "man" -> "♂"
    "diğer", "diger", "other" -> "⚧"
    else -> "•"
}

@Composable
internal fun ProfilePhotoAvatarWithGender(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    accent: Color = SonHarfCyan,
) {
    Box(Modifier.size(size + 6.dp), contentAlignment = Alignment.Center) {
        ProfilePhotoAvatar(avatarPath = avatarPath, name = name, size = size, visible = true, accent = accent)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size((size.value * .34f).coerceAtLeast(15f).dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profileGenderSymbol(gender),
                color = accent,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * .20f).coerceAtLeast(9f).sp,
            )
        }
    }
}
'''
if "ProfilePhotoAvatarWithGender" not in t:
    t += append
write(rel, t)

# 5) Word meanings: persistent memory + multiple dictionary fallbacks.
rel = "app/src/main/java/com/sonharf/game/WordMeaningRuntime.kt"
t = r'''package com.sonharf.game

import android.content.Context
import android.content.SharedPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.util.Locale

/**
 * Dictionary runtime with two-level cache. Meanings that are successfully resolved are
 * kept in RAM for the session and persisted on-device so a later lookup does not depend
 * on network availability. The match summary also preloads every used word.
 */
internal object WordMeaningRuntime {
    private val http = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }
    private val memory = LinkedHashMap<String, String>()
    private var prefs: SharedPreferences? = null

    private val verifiedOffline = mapOf(
        "tr:telefon" to "Sesin uzak mesafelere elektriksel veya elektronik yollarla iletilmesini sağlayan haberleşme aracı.",
        "tr:navigasyon" to "Bir yerden başka bir yere ulaşmak için konum ve rota belirleme işi; yol bulma.",
        "tr:masa" to "Üzerinde çalışma, yemek yeme veya eşya koyma amacıyla kullanılan, ayaklı düz yüzeyli mobilya.",
        "tr:araba" to "İnsan veya yük taşımaya yarayan tekerlekli taşıt.",
        "tr:kalem" to "Yazı yazmak veya çizim yapmak için kullanılan araç.",
        "tr:armut" to "Gülgillerden, tatlı ve sulu meyvesi bulunan ağaç ve bu ağacın meyvesi.",
        "en:apple" to "A round fruit with firm flesh and a skin that is commonly red, green, or yellow.",
        "en:table" to "A piece of furniture with a flat top supported by legs.",
        "en:water" to "A clear liquid essential for life, chemically composed of hydrogen and oxygen.",
        "en:rabbit" to "A small mammal with long ears and powerful hind legs."
    )

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("word_meaning_cache_v2", Context.MODE_PRIVATE)
        verifiedOffline.forEach { (k, v) -> memory.putIfAbsent(k, v) }
    }

    private fun normalize(word: String, language: String): String {
        val locale = if (language == "tr") Locale("tr", "TR") else Locale.ENGLISH
        return word.trim().lowercase(locale)
    }

    private fun store(key: String, value: String) {
        if (value.isBlank()) return
        synchronized(memory) {
            memory[key] = value
            while (memory.size > 600) memory.remove(memory.keys.first())
        }
        prefs?.edit()?.putString(key, value)?.apply()
    }

    suspend fun meaning(word: String, language: String): String {
        val normalized = normalize(word, language)
        val key = "$language:$normalized"
        synchronized(memory) { memory[key] }?.let { return it }
        prefs?.getString(key, null)?.takeIf { it.isNotBlank() }?.let {
            synchronized(memory) { memory[key] = it }
            return it
        }
        verifiedOffline[key]?.let { store(key, it); return it }

        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name()).replace("+", "%20")
        val host = if (language == "en") "en.wiktionary.org" else "tr.wiktionary.org"

        val dictionaryApi = if (language == "en") runCatching {
            val body = http.get("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject
            root?.get("meanings")?.jsonArray.orEmpty().asSequence().mapNotNull { meaning ->
                meaning.jsonObject["definitions"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("definition")?.jsonPrimitive?.content?.trim()
            }.firstOrNull { !it.isNullOrBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val restSummary = if (dictionaryApi.isBlank()) runCatching {
            val body = http.get("https://$host/api/rest_v1/page/summary/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            root["extract"]?.jsonPrimitive?.content?.trim().orEmpty()
        }.getOrDefault("") else ""

        val queryExtract = if (dictionaryApi.isBlank() && restSummary.isBlank()) runCatching {
            val url = "https://$host/w/api.php?action=query&format=json&prop=extracts&explaintext=1&redirects=1&titles=$encoded"
            val root = json.parseToJsonElement(http.get(url).bodyAsText()).jsonObject
            val pages = root["query"]?.jsonObject?.get("pages")?.jsonObject
            pages?.values?.asSequence()?.mapNotNull { it.jsonObject["extract"]?.jsonPrimitive?.content?.trim() }
                ?.firstOrNull { it.isNotBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val raw = dictionaryApi.ifBlank { restSummary }.ifBlank { queryExtract }
        val concise = raw
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^${Regex.escape(normalized)}\\s*", RegexOption.IGNORE_CASE), "")
            .trim(' ', '-', ':', ';')
            .let { if (it.length > 520) it.take(517).trimEnd() + "…" else it }

        if (concise.isNotBlank()) {
            store(key, concise)
            return concise
        }

        return if (language == "en")
            "Bu kelime oyun sözlüğünde geçerli. Kısa İngilizce tanımı sözlük kaynağından alınamadı."
        else
            "Bu kelime oyun sözlüğünde geçerli. Kısa Türkçe tanımı sözlük kaynağından alınamadı."
    }
}
'''
write(rel, t)

# 6) Son Harf sound palette: add a dedicated final-10-seconds heartbeat.
rel = "app/src/main/java/com/sonharf/game/SonHarfSoundFx.kt"
t = read(rel)
t = replace_once(
    t,
    "    fun countdown() = click(13, 0.08, 0.38)\n",
    "    fun countdown() = click(13, 0.08, 0.38)\n    fun heartbeat() { click(58, 0.15, 0.07); delayedClick(118, 46, 0.11, 0.05) }\n",
    "heartbeat function",
)
write(rel, t)

# 7) Active Son Harf arena: responsive words, actual profile photos, gender badge,
#    compact notifications, heartbeat timer, no duplicate send button or action sounds.
rel = "app/src/main/java/com/sonharf/game/SketchGameOverlayV10.kt"
t = read(rel)
if "import androidx.compose.animation.core.animateFloatAsState" not in t:
    t = t.replace("package com.sonharf.game\n\n", "package com.sonharf.game\n\nimport androidx.compose.animation.core.animateFloatAsState\nimport androidx.compose.animation.core.tween\n")
if "import androidx.compose.ui.draw.scale" not in t:
    t = t.replace("import androidx.compose.ui.draw.clip\n", "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.scale\n")

# Remove ordinary success/error/action sounds from this arena. Countdown heartbeat is retained.
t = t.replace("                            if (rejected) SonHarfSoundFx.warning() else SonHarfSoundFx.wordAccepted()\n", "")
t = t.replace("                                if (acceptedOnServer) SonHarfSoundFx.wordAccepted()\n", "")
t = t.replace("                                SonHarfSoundFx.warning()\n", "")
t = t.replace("                            if (correct) SonHarfSoundFx.bonus() else SonHarfSoundFx.warning()\n", "")
t = t.replace("                        .onFailure { triviaFeedback = false to sh(\"Cevap gönderilemedi\", \"Answer could not be sent\"); SonHarfSoundFx.warning() }", "                        .onFailure { triviaFeedback = false to sh(\"Cevap gönderilemedi\", \"Answer could not be sent\") }")
t = t.replace("        LaunchedEffect(room.id, won) { if (won) SonHarfSoundFx.victory() else SonHarfSoundFx.defeat() }\n", "")
t = t.replace("SonHarfSoundFx.countdown()", "SonHarfSoundFx.heartbeat()")

# Timer pulse state.
t = replace_once(
    t,
    "    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }\n\n    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {",
    "    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }\n    val timerScale by animateFloatAsState(\n        targetValue = if (seconds in 1..10 && seconds % 2 == 0) 1.10f else 1f,\n        animationSpec = tween(170),\n        label = \"finalTenHeartbeat\",\n    )\n\n    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {",
    "timer pulse state",
)

# Player card invocations now carry avatar paths.
t = replace_once(
    t,
    '            PlayerV10(myProfile?.displayName ?: sh("SEN", "YOU"), myProfile?.gender, myScore, myRounds, myTurn, SonHarfCyan, Modifier.weight(1f))',
    '            PlayerV10(myProfile?.displayName ?: sh("SEN", "YOU"), myProfile?.gender, myProfile?.avatarPath, myScore, myRounds, myTurn, SonHarfCyan, Modifier.weight(1f))',
    "my player avatar path",
)
t = replace_once(
    t,
    '            PlayerV10(if (room.isBot) "${room.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: sh("RAKİP", "OPPONENT"), if (room.isBot) "other" else opponentProfile?.gender, oppScore, oppRounds, !myTurn, SonHarfPink, Modifier.weight(1f), room.isBot)',
    '            PlayerV10(if (room.isBot) "${room.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: sh("RAKİP", "OPPONENT"), if (room.isBot) "other" else opponentProfile?.gender, if (room.isBot) null else opponentProfile?.avatarPath, oppScore, oppRounds, !myTurn, SonHarfPink, Modifier.weight(1f), room.isBot)',
    "opponent avatar path",
)

# Heartbeat visual + red countdown number.
t = replace_once(
    t,
    "            Box(Modifier.size(64.dp).clip(CircleShape).background(if (seconds <= 10) SonHarfPink else SonHarfGold).padding(3.dp), contentAlignment = Alignment.Center) {\n                Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface), contentAlignment = Alignment.Center) {\n                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(\"$seconds\", fontSize = 24.sp, fontWeight = FontWeight.Black); Text(sh(\"sn\", \"sec\"), fontSize = 12.sp, color = SonHarfMuted) }\n                }\n            }",
    "            Box(Modifier.size(64.dp).scale(timerScale).clip(CircleShape).background(if (seconds <= 10) SonHarfPink else SonHarfGold).padding(3.dp), contentAlignment = Alignment.Center) {\n                Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface), contentAlignment = Alignment.Center) {\n                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(\"$seconds\", color = if (seconds <= 10) SonHarfPink else SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black); Text(sh(\"sn\", \"sec\"), fontSize = 12.sp, color = if (seconds <= 10) SonHarfPink else SonHarfMuted) }\n                }\n            }",
    "heartbeat timer visual",
)

# Responsive long-word typography.
t = replace_once(
    t,
    "    val lastWord = words.lastOrNull()?.normalizedWord?.uppercase()\n    val required = lastWord?.lastOrNull()?.toString().orEmpty()",
    "    val lastWord = words.lastOrNull()?.normalizedWord?.uppercase()\n    val required = lastWord?.lastOrNull()?.toString().orEmpty()\n    val lastWordFont = when {\n        lastWord == null -> 36.sp\n        lastWord.length >= 22 -> 21.sp\n        lastWord.length >= 18 -> 24.sp\n        lastWord.length >= 14 -> 29.sp\n        lastWord.length >= 10 -> 35.sp\n        else -> 44.sp\n    }",
    "responsive word font",
)
t = replace_once(
    t,
    '                        else Text(buildAnnotatedString { append(lastWord.dropLast(1)); withStyle(SpanStyle(color = SonHarfPink)) { append(lastWord.takeLast(1)) } }, fontSize = 44.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)',
    '                        else Text(buildAnnotatedString { append(lastWord.dropLast(1)); withStyle(SpanStyle(color = SonHarfPink)) { append(lastWord.takeLast(1)) } }, modifier = Modifier.fillMaxWidth(), fontSize = lastWordFont, lineHeight = (lastWordFont.value * 1.08f).sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)',
    "long word text",
)

# Compact non-blocking correct/wrong notification.
old_feedback = '''                feedback?.let { f ->
                    val tone = if (f.correct) SonHarfGreen else SonHarfPink
                    Surface(Modifier.align(Alignment.Center).padding(16.dp), color = tone.copy(alpha = .96f), shape = RoundedCornerShape(22.dp), shadowElevation = 10.dp) {
                        Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(f.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 29.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(5.dp))
                            if (f.duplicateWord != null) {
                                Text(f.duplicateWord, color = Color.White, fontWeight = FontWeight.Black, fontSize = 38.sp, textAlign = TextAlign.Center)
                                Text(sh("DAHA ÖNCE ÇIKTI", "ALREADY USED"), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            } else Text(f.detail, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
'''
new_feedback = '''                feedback?.let { f ->
                    val tone = if (f.correct) SonHarfGreen else SonHarfPink
                    Surface(
                        Modifier.align(Alignment.TopCenter).padding(top = 42.dp, start = 10.dp, end = 10.dp).fillMaxWidth(.94f),
                        color = tone.copy(alpha = .96f),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 4.dp,
                    ) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(f.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (f.duplicateWord != null) "${f.duplicateWord} • ${sh("daha önce çıktı", "already used")}" else f.detail,
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                            )
                        }
                    }
                }
'''
t = replace_once(t, old_feedback, new_feedback, "compact feedback")

# Remove second SEND button beneath custom keyboard; keep backspace only.
old_bottom = '''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick={ if(input.isNotEmpty()) onInput(input.dropLast(1)) }, enabled=enabled && input.isNotEmpty(), modifier=Modifier.weight(1f).height(44.dp), shape=RoundedCornerShape(9.dp)) { Text("⌫", fontSize=22.sp, fontWeight=FontWeight.Black) }
            Button(onClick=onSubmit, enabled=enabled && input.length>=2, modifier=Modifier.weight(2.1f).height(44.dp), shape=RoundedCornerShape(9.dp), colors=ButtonDefaults.buttonColors(containerColor=if(neon) SonHarfCyan else SonHarfBlue, contentColor=Color.White)) { Text(sh("GÖNDER", "SEND"), fontWeight=FontWeight.Black, fontSize=14.sp) }
        }
'''
new_bottom = '''        OutlinedButton(
            onClick = { if (input.isNotEmpty()) onInput(input.dropLast(1)) },
            enabled = enabled && input.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text("⌫", fontSize = 21.sp, fontWeight = FontWeight.Black) }
'''
t = replace_once(t, old_bottom, new_bottom, "remove keyboard send")

# Real profile photo + gender badge.
old_player = '''@Composable
private fun PlayerV10(name: String, gender: String?, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier, isBot: Boolean = false) {
    Card(modifier=modifier.fillMaxHeight(), colors=CardDefaults.cardColors(containerColor=if(active) accent.copy(alpha=.10f) else SonHarfSurface), shape=RoundedCornerShape(17.dp), border=BorderStroke(1.dp, if(active) accent.copy(alpha=.55f) else SonHarfMuted.copy(alpha=.13f))) {
        Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment=Alignment.CenterVertically) {
            if (isBot) Box(Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha=.15f)), contentAlignment=Alignment.Center) { Text("🤖", fontSize=24.sp) }
            else SocialAvatar(gender, name, 42.dp, accent = accent)
            Spacer(Modifier.width(7.dp))
            Column { Text(name, maxLines=1, color=if(active) accent else SonHarfMuted, fontSize=11.sp, fontWeight=FontWeight.Bold); Text(score.toString(), fontWeight=FontWeight.Black, fontSize=22.sp); Text("$rounds round", color=SonHarfMuted, fontSize=10.sp) }
        }
    }
}'''
new_player = '''@Composable
private fun PlayerV10(name: String, gender: String?, avatarPath: String?, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier, isBot: Boolean = false) {
    Card(modifier=modifier.fillMaxHeight(), colors=CardDefaults.cardColors(containerColor=if(active) accent.copy(alpha=.10f) else SonHarfSurface), shape=RoundedCornerShape(17.dp), border=BorderStroke(1.dp, if(active) accent.copy(alpha=.55f) else SonHarfMuted.copy(alpha=.13f))) {
        Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment=Alignment.CenterVertically) {
            if (isBot) Box(Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha=.15f)), contentAlignment=Alignment.Center) { Text("🤖", fontSize=24.sp) }
            else ProfilePhotoAvatarWithGender(avatarPath = avatarPath, gender = gender, name = name, size = 42.dp, accent = accent)
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) { Text(name, maxLines=1, color=if(active) accent else SonHarfMuted, fontSize=10.sp, fontWeight=FontWeight.Bold); Text(score.toString(), fontWeight=FontWeight.Black, fontSize=21.sp); Text("$rounds round", color=SonHarfMuted, fontSize=9.sp) }
        }
    }
}'''
t = replace_once(t, old_player, new_player, "PlayerV10 photo")
write(rel, t)

# 8) Combo/result overlay: keep match summary and word meanings, disable AFERIN/combo
#    text, confetti and combo sound. Preload every used match word meaning.
rel = "app/src/main/java/com/sonharf/game/ComboOverlayV9.kt"
t = read(rel)
start = t.index("private fun comboV9(n: Int): ComboV9? = when (n) {")
end = t.index("\n\nprivate data class ConfettiPiece", start)
t = t[:start] + "private fun comboV9(n: Int): ComboV9? = null" + t[end:]
t = replace_once(
    t,
    "                    resultWords = runCatching { backend.getWords(fin.id) }.getOrDefault(emptyList())\n                    growth = runCatching { backend.getGrowthDashboard() }.getOrNull()",
    "                    resultWords = runCatching { backend.getWords(fin.id) }.getOrDefault(emptyList())\n                    resultWords.take(40).forEach { w -> launch { runCatching { WordMeaningRuntime.meaning(w.word, fin.language) } } }\n                    growth = runCatching { backend.getGrowthDashboard() }.getOrNull()",
    "preload match meanings",
)
t = t.replace('Text(value,maxLines=1,fontWeight=FontWeight.Black,fontSize=15.sp,color=SonHarfText)', 'Text(value,maxLines=2,fontWeight=FontWeight.Black,fontSize=12.sp,lineHeight=14.sp,textAlign=TextAlign.Center,color=SonHarfText)')
write(rel, t)

# 9) Portal/home: remove top-left OYUNLAR overlay, add weekly top 3,
#    improve Bil Bakalim compact layouts.
rel = "app/src/main/java/com/sonharf/game/GamePortalApp.kt"
t = read(rel)
if "import com.sonharf.game.data.SupabaseProvider" not in t:
    t = t.replace("import kotlinx.coroutines.delay\n", "import kotlinx.coroutines.delay\nimport com.sonharf.game.data.SupabaseProvider\nimport io.github.jan.supabase.postgrest.postgrest\nimport kotlinx.serialization.SerialName\nimport kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.buildJsonObject\nimport kotlinx.serialization.json.put\n")

# Remove floating OYUNLAR button from Son Harf game.
t = replace_once(
    t,
    '''        PortalGame.SON_HARF -> Box(Modifier.fillMaxSize()) {
            ClassicPremiumApp()
            PortalReturnButton { game = PortalGame.MENU }
        }
''',
    '''        PortalGame.SON_HARF -> ClassicPremiumApp()
''',
    "portal return overlay",
)
# Remove function completely.
if "private fun PortalReturnButton" in t:
    s = t.index("@Composable\nprivate fun PortalReturnButton")
    e = t.index("\n@Composable\nprivate fun GamePortalMenu", s)
    t = t[:s] + t[e+1:]

weekly_defs = r'''
@Serializable
private data class WeeklyPlayerDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val wins: Long = 0,
    @SerialName("win_rate") val winRate: Int = 0,
    val rating: Int = 0,
)

@Composable
private fun WeeklyTopThreeCard() {
    var players by remember { mutableStateOf<List<WeeklyPlayerDto>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        players = if (!SupabaseProvider.configured) emptyList() else runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "get_weekly_leaderboard",
                buildJsonObject { put("p_limit", 3) },
            ).decodeList<WeeklyPlayerDto>()
        }.getOrDefault(emptyList())
        loaded = true
    }
    Surface(shape = RoundedCornerShape(20.dp), color = PortalCard, border = BorderStroke(1.dp, Color(0xFFB9E5F8))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = PortalGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text("HAFTANIN EN İYİ 3 OYUNCUSU", color = PortalText, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth())
            else if (players.isEmpty()) Text("Bu hafta henüz sıralama oluşmadı.", color = PortalMuted, fontSize = 11.sp)
            else players.take(3).forEachIndexed { index, p ->
                Surface(shape = RoundedCornerShape(12.dp), color = if (index == 0) PortalGold.copy(alpha = .10f) else PortalBg) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(listOf("🥇", "🥈", "🥉")[index], fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(p.displayName, modifier = Modifier.weight(1f), color = PortalText, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        Text("${p.wins} G • %${p.winRate}", color = PortalMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

'''
anchor = "@Composable\nfun GamePortalApp()"
if "WeeklyPlayerDto" not in t:
    t = t.replace(anchor, weekly_defs + anchor)

# Insert weekly card after second game card, before common loop card.
needle = '''            button = "BİL BAKALIM OYNA",
            onClick = onBilBakalim,
        )

        Surface(shape = RoundedCornerShape(18.dp), color = PortalCard, border = BorderStroke(1.dp, Color(0xFFB9E5F8))) {'''
t = replace_once(
    t,
    needle,
    '''            button = "BİL BAKALIM OYNA",
            onClick = onBilBakalim,
        )

        WeeklyTopThreeCard()

        Surface(shape = RoundedCornerShape(18.dp), color = PortalCard, border = BorderStroke(1.dp, Color(0xFFB9E5F8))) {''',
    "weekly leaderboard placement",
)

# Game tags: 2 columns rather than 3 to prevent edge clipping.
t = t.replace("            tags.chunked(3).forEach { rowTags ->", "            tags.chunked(2).forEach { rowTags ->")
t = t.replace(
    "                        Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = .10f)) {\n                            Text(tag, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = PortalText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)\n                        }",
    "                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = .10f)) {\n                            Text(tag, Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp), color = PortalText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)\n                        }",
)

# Bil Bakalim meta stats: 2x2 for readable type.
old_stats = '''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MetaStat("🔥", "Seri", "$winStreak", Modifier.weight(1f))
            MetaStat("🏆", "En İyi", "$bestStreak", Modifier.weight(1f))
            MetaStat("⚔️", "Rakiplik", "$rivalWins-$rivalLosses", Modifier.weight(1f))
            MetaStat("🎯", "Günlük", "10 Soru", Modifier.weight(1f))
        }
'''
new_stats = '''        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MetaStat("🔥", "Seri", "$winStreak", Modifier.weight(1f))
                MetaStat("🏆", "En İyi", "$bestStreak", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MetaStat("⚔️", "Rakiplik", "$rivalWins-$rivalLosses", Modifier.weight(1f))
                MetaStat("🎯", "Günlük", "10 Soru", Modifier.weight(1f))
            }
        }
'''
t = replace_once(t, old_stats, new_stats, "Bil stats layout")

# Risk/lock controls stacked to avoid cramped labels.
old_risk = '''                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = riskMode, onClick = { riskMode = !riskMode }, label = { Text(if (riskMode) "💣 RİSK x2 AÇIK" else "💣 RİSK SORUSU x2", fontSize = 10.sp) }, modifier = Modifier.weight(1f))
                            Button(onClick = { submit(input) }, enabled = input.isNotBlank(), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PortalGold, contentColor = Color(0xFF2B1E0B))) {
                                Text("KİLİTLE", fontWeight = FontWeight.Black)
                            }
                        }
'''
new_risk = '''                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            FilterChip(selected = riskMode, onClick = { riskMode = !riskMode }, label = { Text(if (riskMode) "💣 RİSK x2 AÇIK" else "💣 RİSK SORUSU x2", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { submit(input) }, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = PortalGold, contentColor = Color(0xFF2B1E0B))) {
                                Text("TAHMİNİ KİLİTLE", fontWeight = FontWeight.Black)
                            }
                        }
'''
t = replace_once(t, old_risk, new_risk, "Bil risk controls")
t = t.replace('Text(label, color = PortalMuted, fontSize = 7.5.sp, textAlign = TextAlign.Center)', 'Text(label, color = PortalMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2)')
write(rel, t)

print("ALL REQUESTED PATCHES APPLIED")
