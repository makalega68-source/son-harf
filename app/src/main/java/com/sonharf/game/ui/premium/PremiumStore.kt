package com.sonharf.game.ui.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.R
import com.sonharf.game.SonHarfTheme

/**
 * G5.7 — Premium mağaza için yeniden kullanılabilir bileşenler.
 *
 * Billing / satın alma mantığına DOKUNULMAZ. Bu bileşenler sadece
 * görsel katmandır; callback ile mevcut billing kodu çağırılır.
 *
 * Yasaklar (TXT): sahte geri sayım YOK, sahte "son 2 adet" YOK,
 * gizli ücret YOK, yanlışlıkla satın aldıran düğme yeri YOK.
 * Buradaki hiçbir composable bu kalıpları desteklemez.
 */

// ---------------------------------------------------------------------
// TAB HEADER
// ---------------------------------------------------------------------
enum class StoreTab { FEATURED, DIAMONDS, PRO, OTHER }

@Composable
fun PremiumStoreTabs(
    selected: StoreTab,
    onSelect: (StoreTab) -> Unit,
    visibleTabs: List<StoreTab> = StoreTab.entries,
    modifier: Modifier = Modifier,
    language: String = "tr",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(99.dp))
            .background(SonHarfTheme.PremiumPanel.copy(alpha = 0.85f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        visibleTabs.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (active) SonHarfTheme.GoldBright.copy(alpha = 0.25f) else Color.Transparent)
                    .border(
                        if (active) 1.dp else 0.dp,
                        if (active) SonHarfTheme.GoldBright.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(99.dp),
                    )
                    .clickable(onClick = { onSelect(tab) })
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label(language),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                    ),
                    color = if (active) SonHarfTheme.GoldBright else SonHarfTheme.PremiumTextSecondary,
                )
            }
        }
    }
}

private fun StoreTab.label(language: String): String = when (this) {
    StoreTab.FEATURED -> if (language == "en") "FEATURED" else "ÖNE ÇIKAN"
    StoreTab.DIAMONDS -> if (language == "en") "DIAMONDS" else "ELMAS"
    StoreTab.PRO -> "PRO"
    StoreTab.OTHER -> if (language == "en") "MORE" else "DİĞER"
}

// ---------------------------------------------------------------------
// PRO SHOWCASE CARD  (Öne Çıkan sekmesinin en üstünde büyük kart)
// ---------------------------------------------------------------------
@Composable
fun PremiumProShowcaseCard(
    features: List<String>,
    priceLabel: String,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
    language: String = "tr",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        SonHarfTheme.PremiumPanel,
                        SonHarfTheme.PremiumPanel.copy(alpha = 0.65f),
                    ),
                ),
            )
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    listOf(SonHarfTheme.GoldEdge, SonHarfTheme.GoldBright, SonHarfTheme.GoldPale, SonHarfTheme.GoldDeep),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_pro_badge),
                contentDescription = null,
                modifier = Modifier.size(width = 60.dp, height = 30.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (language == "en") "SON HARF PRO" else "SON HARF PRO",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                    ),
                    color = SonHarfTheme.GoldBright,
                )
                Text(
                    if (language == "en")
                        "Reklamsız + altın çerçeve + ayrıcalıklar"
                    else
                        "Reklamsız + altın çerçeve + ayrıcalıklar",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = SonHarfTheme.PremiumTextSecondary,
                )
            }
        }
        features.forEach { feature ->
            Row(verticalAlignment = Alignment.Top) {
                Text("✓ ", style = TextStyle(fontSize = 14.sp), color = SonHarfTheme.GoldBright)
                Text(
                    feature,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = SonHarfTheme.PremiumTextPrimary,
                )
            }
        }
        GameButton(
            text = if (language == "en") "GET PRO — $priceLabel" else "PRO AL — $priceLabel",
            onClick = onBuy,
            primary = true,
            accent = SonHarfTheme.GoldDeep,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------
// DIAMOND PACKAGE CARD  (2 sütunlu grid'de kullanılır)
// ---------------------------------------------------------------------
enum class DiamondPackageHighlight { NONE, MOST_POPULAR, BEST_VALUE }

@Composable
fun PremiumDiamondPackageCard(
    amount: Int,
    priceLabel: String,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: DiamondPackageHighlight = DiamondPackageHighlight.NONE,
    language: String = "tr",
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SonHarfTheme.PremiumPanel)
            .border(
                width = if (highlight != DiamondPackageHighlight.NONE) 2.dp else 1.dp,
                color = when (highlight) {
                    DiamondPackageHighlight.MOST_POPULAR -> SonHarfTheme.SonHarfOrange
                    DiamondPackageHighlight.BEST_VALUE -> SonHarfTheme.GoldBright
                    else -> SonHarfTheme.PremiumPanelBorder
                },
                shape = RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (highlight != DiamondPackageHighlight.NONE) {
            Text(
                text = when (highlight) {
                    DiamondPackageHighlight.MOST_POPULAR ->
                        if (language == "en") "MOST POPULAR" else "EN POPÜLER"
                    DiamondPackageHighlight.BEST_VALUE ->
                        if (language == "en") "BEST VALUE" else "EN İYİ DEĞER"
                    else -> ""
                },
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                ),
                color = when (highlight) {
                    DiamondPackageHighlight.MOST_POPULAR -> SonHarfTheme.SonHarfOrange
                    DiamondPackageHighlight.BEST_VALUE -> SonHarfTheme.GoldBright
                    else -> SonHarfTheme.PremiumTextSecondary
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        when (highlight) {
                            DiamondPackageHighlight.MOST_POPULAR -> SonHarfTheme.SonHarfOrange.copy(alpha = 0.18f)
                            DiamondPackageHighlight.BEST_VALUE -> SonHarfTheme.GoldBright.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text("💎", style = TextStyle(fontSize = 36.sp))
        CountUpText(
            value = amount,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black),
            color = SonHarfTheme.DiamondBlue,
        )
        GameButton(
            text = priceLabel,
            onClick = onBuy,
            accent = SonHarfTheme.DiamondBlue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------
// CONFIRM PURCHASE DIALOG  (kısa onay penceresi)
// ---------------------------------------------------------------------
/**
 * Very small confirm sheet — displayed as inline card, not a system
 * dialog, so it fits inside store screens.
 */
@Composable
fun PremiumConfirmPurchaseCard(
    productName: String,
    priceLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    language: String = "tr",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SonHarfTheme.PremiumPanel)
            .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (language == "en") "CONFIRM PURCHASE" else "SATIN ALMAYI ONAYLA",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            ),
            color = SonHarfTheme.PremiumTextSecondary,
        )
        Text(
            productName,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
            color = SonHarfTheme.PremiumTextPrimary,
        )
        Text(
            priceLabel,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black),
            color = SonHarfTheme.GoldBright,
        )
        Text(
            if (language == "en")
                "Google Play will charge your account."
            else
                "Google Play hesabından tahsil edilir.",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = SonHarfTheme.PremiumTextSecondary,
            textAlign = TextAlign.Start,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameButton(
                text = if (language == "en") "CANCEL" else "İPTAL",
                onClick = onCancel,
                accent = SonHarfTheme.Lavender,
                modifier = Modifier.weight(1f),
            )
            GameButton(
                text = if (language == "en") "CONFIRM" else "ONAYLA",
                onClick = onConfirm,
                primary = true,
                accent = SonHarfTheme.GoldDeep,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---------------------------------------------------------------------
// RESTORE PURCHASES LINK  (alta konur)
// ---------------------------------------------------------------------
@Composable
fun RestorePurchasesLink(
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    language: String = "tr",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRestore)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (language == "en") "Restore purchases" else "Satın alımları geri yükle",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = SonHarfTheme.PremiumTextSecondary,
        )
    }
}
