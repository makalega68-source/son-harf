package com.sonharf.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class GameHelpType { SIEGE, LAST_LETTER, LETTER_PATH }

@Composable
internal fun GameHelpButton(
    type: GameHelpType,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(
        onClick = { open = true },
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            Icons.Rounded.HelpOutline,
            contentDescription = sh("Nasıl oynanır?", "How to play?"),
            tint = SonHarfTheme.TextPrimary,
        )
    }
    if (!open) return

    val items = when (type) {
        GameHelpType.SIEGE -> listOf(
            sh("Amaç: kelimeler kurarak haritada alan hâkimiyeti sağla ve toplam skorda öne geç.", "Goal: build words, control territory, and lead the total score."),
            sh("Harflerini geçerli bir kelime oluşturacak şekilde haritaya yerleştir; hamle yeni bölgeler ele geçirebilir.", "Place letters to form a valid word; a move can capture new territory."),
            sh("Kelime Puanı kelimelerden kazanılır ve kalıcıdır. Bölge Puanı sahip olduğun küplerden gelir; her küp 2 puandır.", "Word Score comes from words and stays earned. Territory Score comes from owned cells; each cell is worth 2 points."),
            sh("Rakip senin alanını alırsa yalnız kaybettiğin Bölge Puanı düşer; daha önce kazandığın Kelime Puanı silinmez.", "If an opponent captures your area, only the lost Territory Score is deducted; previously earned Word Score remains."),
            sh("Harita kontrol yüzdesi alan hâkimiyetini gösterir. Birden çok kritik hücreyi zincirleme almak kuşatma baskısı yaratır.", "Map control shows territory dominance. Chained captures of critical cells create siege pressure."),
            sh("Kale, kritik bölge veya diğer özel noktalar görünüyorsa bunlar harita stratejisinin parçasıdır; herkese aynı kurallar uygulanır.", "If castles, critical zones, or other special points appear, they are shared strategic map elements with equal rules for both players."),
            sh("Maç sonunda Kelime Puanı + Bölge Puanı toplamı yüksek olan oyuncu kazanır.", "At match end, the player with the higher Word Score + Territory Score total wins."),
        )
        GameHelpType.LAST_LETTER -> listOf(
            sh("Rakibin yazdığı kelimenin son harfiyle başlayan yeni bir kelime üret.", "Create a new word that starts with the final letter of your opponent's word."),
            sh("Kelime sözlükte geçerli olmalı; maçta daha önce kullanılan kelime tekrar kullanılamaz.", "The word must be valid in the dictionary; a word already used in the match cannot be repeated."),
            sh("Hamleni süre dolmadan gönder. Tur süresi oyun ilerledikçe kısalabilir.", "Submit before the turn timer expires. The turn window may shrink as the match progresses."),
            sh("Geçerli hamleler skor kazandırır. Geçersiz hamle veya süre bitimi mevcut can/hamle kurallarına göre ceza verir.", "Valid moves score points. Invalid moves or timeouts apply the current life/turn penalty."),
            sh("Canı/hamle hakkı tükenen veya maçın bitiş koşulunda geride kalan oyuncu kaybeder; sonuç ekranda gösterilir.", "A player who exhausts the applicable lives/turn allowance, or trails at the match end condition, loses; the result is shown on screen."),
        )
        GameHelpType.LETTER_PATH -> listOf(
            sh("Amaç: bağlantılı harfleri kullanarak geçerli kelime rotaları oluşturmak ve hedefi tamamlamak.", "Goal: connect letters into valid word paths and complete the objective."),
            sh("Harfler izin verilen komşuluk/bağlantı yönleriyle birbirine bağlanır; kopuk rota geçerli hamle sayılmaz.", "Letters must follow the allowed adjacency/connection rules; a broken path is not a valid move."),
            sh("Geçerli kelimeler puan kazandırır; daha verimli ve uzun rotalar daha güçlü skor fırsatları yaratır.", "Valid words earn points; efficient and longer paths create stronger scoring opportunities."),
            sh("Rotayı ve ekrandaki hedefleri tamamladığında bölüm/oyun tamamlanır; sonuç mevcut skor kurallarına göre hesaplanır.", "Complete the route and on-screen objectives to finish; the result follows the current scoring rules."),
        )
    }

    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(sh("Nasıl Oynanır?", "How to Play"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items.forEach { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text("•", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(line, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { open = false }) {
                Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Bold)
            }
        },
    )
}
