package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DefinitionBadgeBlue = Color(0xFF25AFCF)

@Composable
internal fun WordDefinitionDialog(
    word: String,
    language: String,
    onDismiss: () -> Unit,
) {
    var loading by remember(word, language) { mutableStateOf(true) }
    var definition by remember(word, language) { mutableStateOf<DictionaryDefinition?>(null) }

    LaunchedEffect(word, language) {
        loading = true
        definition = runCatching { DictionaryDefinitionService.lookup(word, language) }.getOrNull()
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MainUi.Surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = DefinitionBadgeBlue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = definition?.headword ?: word.lowercase(),
                    color = MainUi.Text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when {
                    loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = MainUi.Blue, strokeWidth = 3.dp)
                        }
                    }

                    definition != null -> {
                        definition!!.meanings.forEach { meaning ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    Modifier
                                        .padding(top = 8.dp, end = 9.dp)
                                        .size(8.dp)
                                        .background(MainUi.Blue, CircleShape),
                                )
                                Text(
                                    text = meaning,
                                    color = MainUi.Text,
                                    fontSize = 17.sp,
                                    lineHeight = 23.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = definition!!.sourceLabel,
                            color = MainUi.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    else -> {
                        Text(
                            text = if (language.lowercase() == "tr") {
                                sh(
                                    "Kelime oyun sözlüğünde geçerli. TDK anlam servisine şu anda ulaşılamadı. Biraz sonra tekrar deneyebilirsin.",
                                    "The word is valid in the game dictionary, but the TDK definition service is temporarily unavailable. Please try again shortly.",
                                )
                            } else {
                                sh(
                                    "Kelime anlamı özelliği şu anda Türkçe sözlük için kullanılabilir.",
                                    "Word definitions are currently available for the Turkish dictionary.",
                                )
                            },
                            color = MainUi.Muted,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(sh("Kapat", "Close"), fontWeight = FontWeight.Bold)
            }
        },
    )
}
