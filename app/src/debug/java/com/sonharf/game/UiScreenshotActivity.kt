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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.WordSiegeCellDto

/** Debug-only host used by CI to render the real production Compose shell for screenshots. */
class UiScreenshotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfUiState.language = "tr"
        SonHarfCosmetics.restore(this)
        setContent {
            if (intent.getBooleanExtra("walnut_ivory_board", false)) WalnutIvoryScreenshotFixture()
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
