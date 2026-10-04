package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

internal enum class DailyWordEvent { MIX, MISSING, MEMORY }
internal data class EventWord(val word:String,val clue:String)
internal fun dailyEventWords(day:String,english:Boolean,event:DailyWordEvent):List<EventWord> {
    val words=if(english)listOf(
        "APPLE|A fruit", "OCEAN|A very large sea", "TIGER|A striped big cat", "CLOUD|It floats in the sky",
        "BREAD|A baked food", "CHAIR|You sit on it", "CLOCK|It tells the time", "HORSE|An animal you can ride",
        "LEMON|A sour yellow fruit", "MOUSE|A small rodent", "PIANO|An instrument with keys", "RIVER|Flowing water",
        "SNAKE|A reptile without legs", "SPOON|A utensil for soup", "TRAIN|It travels on rails", "WHALE|A large sea mammal",
        "ZEBRA|A striped African animal", "BRUSH|A tool for painting", "CROWN|A ruler wears it", "EARTH|Our planet",
        "GRAPE|A fruit that grows in bunches", "HEART|It pumps blood", "HOUSE|A place to live", "LIGHT|It removes darkness",
        "NIGHT|The dark part of the day", "PEACH|A fuzzy fruit", "SHEEP|It produces wool", "STONE|A piece of rock",
        "WATER|What we drink", "WHEAT|A grain used for flour") else listOf(
        "ELMA|Kırmızı veya yeşil bir meyve", "DENİZ|Tuzlu büyük su kütlesi", "KAPLAN|Çizgili büyük kedi", "BULUT|Gökyüzünde süzülür",
        "EKMEK|Fırında pişen temel yiyecek", "MASA|Üzerinde yemek yenir", "SAAT|Zamanı gösterir", "KALEM|Yazı yazma aracı",
        "LİMON|Ekşi sarı meyve", "FARE|Küçük bir kemirgen", "PİYANO|Tuşlu bir müzik aleti", "NEHİR|Akıp giden su yolu",
        "YILAN|Bacaksız sürüngen", "KAŞIK|Çorba içme aracı", "TREN|Ray üzerinde gider", "BALİNA|Büyük deniz memelisi",
        "ZEBRA|Çizgili Afrika hayvanı", "FIRÇA|Boya sürme aracı", "KİTAP|Okumak için sayfaları çevir", "DÜNYA|Yaşadığımız gezegen",
        "ÜZÜM|Salkım halinde yetişen meyve", "KALP|Kanı pompalar", "ORMAN|Ağaçlarla kaplı geniş alan", "IŞIK|Karanlığı aydınlatır",
        "GECE|Günün karanlık bölümü", "ARMUT|Altı geniş, üstü dar meyve", "KOYUN|Yün veren hayvan", "TOPRAK|Bitkilerin yetiştiği zemin",
        "YAĞMUR|Buluttan düşen su damlaları", "BUĞDAY|Un yapılan tahıl")
    return words.shuffled(Random((day+event.name+english).hashCode())).take(5).map { val pair=it.split('|');EventWord(pair[0],pair[1]) }
}
internal fun dailyEventDay():String=LocalDate.now(ZoneId.of("Europe/Istanbul")).toString()
internal fun dailyEventTitle(event:DailyWordEvent):String=when(event) {
    DailyWordEvent.MIX->sh("Harf Karışıklığı","Letter Shuffle")
    DailyWordEvent.MISSING->sh("Kayıp Harf","Missing Letter")
    DailyWordEvent.MEMORY->sh("Hafıza Turu","Memory Round")
}
internal fun dailyEventHint(event:DailyWordEvent):String=when(event) {
    DailyWordEvent.MIX->sh("İpucunu oku, karışık harfleri sırala.","Read the clue and arrange the shuffled letters.")
    DailyWordEvent.MISSING->sh("Kelimeyi tamamlayan harfi bul.","Find the letter that completes the word.")
    DailyWordEvent.MEMORY->sh("Kelimeye 4 saniye bak, sonra hatırla.","Study the word for 4 seconds, then recall it.")
}
internal fun eventProgressKey(user:String,day:String,english:Boolean,event:DailyWordEvent)="$user:$day:$english:${event.name}"
internal fun eventScore(answers:String)=answers.take(5).count { it=='1' }*20

@Composable
internal fun DailyWordEventScreen(event:DailyWordEvent,user:String,onExit:()->Unit) {
    val context=LocalContext.current
    val english=SonHarfUiState.isEnglish
    val day=remember { dailyEventDay() }
    val prefs=remember { context.getSharedPreferences("daily_word_events_v1",0) }
    val key=eventProgressKey(user,day,english,event)
    val words=remember(key) { dailyEventWords(day,english,event) }
    var answers by remember(key) { mutableStateOf(prefs.getString(key,"").orEmpty().take(5)) }
    var round by remember(key) { mutableIntStateOf(answers.length) }
    var picked by remember(key,round) { mutableStateOf<List<Int>>(emptyList()) }
    var feedback by remember(key,round) { mutableStateOf<Boolean?>(null) }
    var countdown by remember(key,round) { mutableIntStateOf(if(event==DailyWordEvent.MEMORY)4 else 0) }
    BackHandler(onBack=onExit)
    LaunchedEffect(key,round) { while(countdown>0) { delay(1_000);countdown-- } }
    val word=words.getOrNull(round)
    val missing=remember(key,round) { if(word==null)0 else Random((key+round).hashCode()).nextInt(word.word.length) }
    val letters=remember(key,round) {
        if(word==null)emptyList() else if(event==DailyWordEvent.MISSING) {
            val alphabet=if(english)"ABCDEFGHIJKLMNOPQRSTUVWXYZ" else "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"
            (alphabet.filter { it!=word.word[missing] }.toList().shuffled(Random((key+round).hashCode())).take(3)+word.word[missing]).shuffled(Random((key+round+"options").hashCode()))
        } else word.word.toList().shuffled(Random((key+round+"letters").hashCode())).let { if(it.joinToString("")==word.word)it.drop(1)+it.first() else it }
    }
    val answer=picked.joinToString("") { letters[it].toString() }
    val revealed=event==DailyWordEvent.MEMORY && countdown>0
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        MainScreenHeader(dailyEventTitle(event),sh("Günün 5 bulmacası","Today's 5 puzzles"),onBack=onExit)
        if(word==null) {
            LobbyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text(sh("Tur tamamlandı!","Round complete!"),fontSize=24.sp,fontWeight=FontWeight.Bold)
                    Text("${eventScore(answers)} / 100",fontSize=36.sp,fontWeight=FontWeight.Black,color=LobbyPalette.Accent)
                    Text(sh("Yarın 5 yeni bulmacayla tekrar oyna.","Come back tomorrow for 5 new puzzles."),textAlign=TextAlign.Center,color=LobbyPalette.Muted)
                }
            }
            Button(onClick=onExit,modifier=Modifier.fillMaxWidth()) { Text(sh("Etkinliklere dön","Back to events")) }
        } else {
            Text("${round+1} / 5",color=LobbyPalette.Muted,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={round/5f},modifier=Modifier.fillMaxWidth())
            Text(dailyEventHint(event),fontSize=17.sp,color=LobbyPalette.Ink)
            LobbyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    if(event!=DailyWordEvent.MEMORY)Text(word.clue,color=LobbyPalette.Muted,textAlign=TextAlign.Center)
                    Text(when { revealed->word.word;event==DailyWordEvent.MISSING->word.word.mapIndexed { i,c->if(i==missing)'_' else c }.joinToString(" ");else->answer.ifEmpty { "_ ".repeat(word.word.length).trim() } },
                        fontSize=28.sp,fontWeight=FontWeight.Black,color=LobbyPalette.Ink,textAlign=TextAlign.Center)
                    if(revealed)Text("$countdown",fontSize=24.sp,color=LobbyPalette.Accent)
                }
            }
            if(!revealed) {
                letters.indices.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach { i ->
                            OutlinedButton(onClick={picked=if(event==DailyWordEvent.MISSING)listOf(i) else picked+i},enabled=i !in picked && feedback==null,
                                modifier=Modifier.weight(1f).heightIn(min=56.dp),contentPadding=PaddingValues(4.dp)) { Text(letters[i].toString(),fontSize=23.sp,fontWeight=FontWeight.Bold) }
                        }
                        repeat(4-row.size){Spacer(Modifier.weight(1f))}
                    }
                }
                if(event==DailyWordEvent.MISSING && answer.isNotEmpty())Text(sh("Seçimin: $answer","Your choice: $answer"),color=LobbyPalette.Ink)
                if(feedback==null) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick={picked=emptyList()},modifier=Modifier.weight(1f),enabled=picked.isNotEmpty()) { Text(sh("Sil","Clear")) }
                        Button(onClick={
                            val correct=answer==if(event==DailyWordEvent.MISSING)word.word[missing].toString() else word.word
                            feedback=correct;answers+=(if(correct)"1" else "0");prefs.edit().putString(key,answers).apply()
                        },enabled=answer.length==(if(event==DailyWordEvent.MISSING)1 else word.word.length),modifier=Modifier.weight(1f)) { Text(sh("Kontrol et","Check")) }
                    }
                } else {
                    Text(if(feedback==true)sh("Doğru! +20 puan","Correct! +20 points") else sh("Doğru kelime: ${word.word}","The word was: ${word.word}"),color=if(feedback==true)LobbyPalette.Accent else Hf.Red,fontSize=18.sp)
                    Button(onClick={round++},modifier=Modifier.fillMaxWidth()) { Text(if(round==4)sh("Sonucu gör","See result")else sh("Sonraki","Next")) }
                }
            }
        }
    }
}
