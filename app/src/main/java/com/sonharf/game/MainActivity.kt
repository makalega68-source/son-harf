package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.domain.GameState
import com.sonharf.game.domain.WordChainEngine
import com.sonharf.game.ui.theme.SonHarfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SonHarfTheme { SonHarfApp() } }
    }
}

@Composable
private fun SonHarfApp() {
    val engine = remember { WordChainEngine() }
    var state by remember { mutableStateOf(GameState()) }
    var input by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SON HARF", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Kelime zinciri", style = MaterialTheme.typography.titleLarge)
                    Text(state.message)
                    Text("Sıra: Oyuncu ${state.currentPlayer}", fontWeight = FontWeight.SemiBold)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.chain) { entry ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.word.uppercase())
                            Text("Oyuncu ${entry.player}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Kelime") },
                enabled = state.winner == null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val next = engine.submit(state, input)
                        if (next.chain.size > state.chain.size) input = ""
                        state = next
                    },
                    enabled = state.winner == null && input.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Gönder") }
                OutlinedButton(
                    onClick = { state = engine.forfeit(state) },
                    enabled = state.winner == null,
                    modifier = Modifier.weight(1f)
                ) { Text("Pes et") }
            }

            if (state.winner != null) {
                Button(
                    onClick = { state = GameState(); input = "" },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Rövanş") }
            }
        }
    }
}
