package com.sonharf.game.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import java.util.Calendar

/**
 * G4.6 — Doğum yılı seçici.
 *
 * Full-screen scrim + centered card. Küçük bir listeden yıl seçilir;
 * ONAYLA butonu callback tetikler. TXT: "18 yaş altı olduğu bilinen
 * hesaplarda serbest sohbet KAPALI". Kullanıcı kendini yaşlı olarak
 * bildirmek zorunda değildir — TEMİZLE seçeneği ile boş bırakabilir.
 *
 * Bu dialog KONSANTRE onay içindir; hesap oluşturmada değil, profil/
 * ayarlar ekranında opsiyonel bir eylem olarak sunulur (spec:
 * "bilinen minörler").
 */
@Composable
fun BirthYearPickerDialog(
    initial: Int?,
    onDismiss: () -> Unit,
    onConfirm: (birthYear: Int?) -> Unit,
    language: String = "tr",
) {
    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = remember(thisYear) { (thisYear downTo 1930).toList() }
    var selected by remember { mutableStateOf(initial) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SonHarfTheme.PremiumPanel)
                .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {} // absorb clicks
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (language == "en") "BIRTH YEAR" else "DOĞUM YILI",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                ),
                color = SonHarfTheme.PremiumTextSecondary,
            )
            Text(
                if (language == "en")
                    "We only ask for this to protect younger players. Leave it empty if you'd rather not say."
                else
                    "Bunu yalnızca 18 yaş altındaki oyuncuları korumak için soruyoruz. Söylememek istersen boş bırakabilirsin.",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = SonHarfTheme.PremiumTextSecondary,
            )
            // Scrollable year list. Initial scrolls to the current
            // selection so the user sees it.
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = years.indexOf(selected ?: (thisYear - 25)).coerceAtLeast(0),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.30f)),
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(8.dp),
                ) {
                    items(years) { y ->
                        val chosen = y == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (chosen) SonHarfTheme.GoldBright.copy(alpha = 0.22f)
                                    else Color.Transparent,
                                )
                                .clickable { selected = y }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                y.toString(),
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = if (chosen) FontWeight.Black else FontWeight.SemiBold,
                                ),
                                color = if (chosen) SonHarfTheme.GoldBright
                                else SonHarfTheme.PremiumTextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (chosen) {
                                Text(
                                    "✓",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Black),
                                    color = SonHarfTheme.GoldBright,
                                )
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton(
                    text = if (language == "en") "CLEAR" else "TEMİZLE",
                    onClick = {
                        selected = null
                        onConfirm(null)
                    },
                    accent = SonHarfTheme.Lavender,
                    modifier = Modifier.weight(1f),
                )
                GameButton(
                    text = if (language == "en") "CONFIRM" else "ONAYLA",
                    onClick = { onConfirm(selected) },
                    primary = true,
                    enabled = selected != null,
                    accent = SonHarfTheme.GoldDeep,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
