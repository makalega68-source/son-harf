package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import java.time.Instant

/** Debug-only host used by CI to render the real production Compose shell for screenshots. */
class UiScreenshotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfUiState.language = "tr"
        SonHarfCosmetics.restore(this)
        if (intent.hasExtra("review_stage")) SonHarfCosmetics.gameThemeId =
            if (intent.getStringExtra("review_stage").orEmpty().contains("dark")) "theme_black" else null
        setContent {
            if (intent.hasExtra("review_stage")) EntryAndStoreReview(intent.getStringExtra("review_stage").orEmpty())
            else if (intent.hasExtra("game_stage")) GameStageFixture(intent.getStringExtra("game_stage").orEmpty())
            else if (intent.getBooleanExtra("walnut_ivory_board", false)) WalnutIvoryScreenshotFixture()
            else PremiumUnifiedProApp(onSignedOut = {})
        }
    }
}

/** Debug-only seeded state; production board and rack composables render every pixel. */
@Composable
private fun WalnutIvoryScreenshotFixture() {
    val board = List(WordSiegeBoardSpec.CellCount) { index ->
        WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index))
    }.toMutableList().apply {
        this[95] = WordSiegeCellDto(letter = "K", owner = 2)
        this[96] = WordSiegeCellDto(letter = "A", owner = 2)
        this[97] = WordSiegeCellDto(letter = "L", owner = 2)
        this[110] = WordSiegeCellDto(letter = "M", owner = 1)
        this[111] = WordSiegeCellDto(letter = "E", owner = 1)
        this[125] = WordSiegeCellDto(letter = "İ", owner = 1)
        this[126] = WordSiegeCellDto(letter = "R", owner = 2)
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF2EEE6)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("KELİME TAHTI", color = WordSiegeWalnutIvory.ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text("CEVİZ & FİLDİŞİ  ·  KELİME KUŞATMASI", color = WordSiegeWalnutIvory.secondaryInk, fontSize = 11.sp)
        WordSiegePracticeBoard(
            board = board,
            rack = "KALEMİR",
            placements = mapOf(112 to 2),
            myOwner = 1,
            enabled = true,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            onCell = {},
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            "KALEMİR".forEachIndexed { index, letter ->
                WordSiegePracticeRackTile(
                    letter = letter,
                    selected = index == 2,
                    used = index == 2,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    onClick = {},
                )
            }
        }
        Text("YEŞİL  ·  SEN      KIRMIZI  ·  RAKİP", color = WordSiegeWalnutIvory.secondaryInk, fontSize = 11.sp)
    }
}

/** Seeded debug-only data; these are the same composables used by the live screens. */
@Composable private fun GameStageFixture(stage:String) {
 val now=Instant.parse("2026-10-02T09:34:00Z").toEpochMilli()
 val event=AtelierTournament(serverTime="2026-10-02T09:34:00Z",nextStart="2026-10-02T11:00:00Z",eventStart="2026-10-02T09:00:00Z",active=false)
 val players=listOf(ThroneRow(1,"preview-1","Ümit",1189,gender="erkek"),ThroneRow(2,"preview-2","Selin",357,gender="kadın"),ThroneRow(3,"preview-3","Arda",120,gender="erkek"))
 val me=ProfileDto("preview-1","Ümit",gender="erkek",rating=617)
 when(stage) {
  "fallback"->FallbackRuntimeFixture()
  "search"->PremierSearching("tr",me,{})
  "duel"->PremierLobby("tr",me,"",false,{}, {}, {})
  "versus"->PremierVsScreen("tr",me,null,GameRoomDto("preview","PREVIEW","preview-1",status="playing",isBot=true,botName="Selin"))
  else->Column(Modifier.fillMaxSize().background(SonHarfTheme.Background).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   when(stage) {
    "throne"->{ThroneOwnerStage(players.first(),now+60*60*60*1000L,now);GameWeeklyPodium(players)}
    "atelier"->{AtelierTournamentPanel(event,false){};AtelierLobby(true,null,false,false,null,null,false,180,{}, {}, {}, {}, {})}
    else->{PremiumOtherGames({},{});TournamentHomeStage(event,now,onOpen={});GameWeeklyPodium(players)}
   }
  }
 }
}


/** Real production fallback timer; only rival arrival is seeded at 25 seconds. No account writes. */
@Composable private fun FallbackRuntimeFixture() {
    var rivalArrived by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(25_000); rivalArrived=true }
    val game=WordSiegeGameDto("00000000-0000-0000-0000-000000000123","preview-1",
        playerTwoId=if(rivalArrived) "preview-2" else null,status=if(rivalArrived) "playing" else "waiting",
        currentPlayerId="preview-1",board=List(225) { WordSiegeCellDto() },playerOneRack="KALEMİR")
    WordSiegePanMatch(game,"preview-1",mapOf("preview-1" to ProfileDto("preview-1","Ümit"),"preview-2" to ProfileDto("preview-2","Selin")),
        emptyList(),emptyMap(),null,false,null,{}, {}, {}, {}, {}, {}, {}, {}, {})
}
