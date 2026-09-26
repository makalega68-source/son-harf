package com.sonharf.game

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

/**
 * Which mascot characters the player owns. The server (verified Google Play purchases) is the
 * source of truth; the device keeps a copy only so the mascot appears without waiting for the
 * network. Ordinary players own none, so they never see a mascot outside the first-run welcome.
 */
internal object WordSiegeMascotOwnership {
    private const val PREFS = "word_siege_mascot_ownership"
    private const val KEY_OWNED = "owned"

    var owned by mutableStateOf<Set<WordSiegeMascotSkin>>(emptySet())
        private set

    val hasAny: Boolean get() = owned.isNotEmpty()

    fun restore(context: Context) {
        val ids = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_OWNED, emptySet()).orEmpty()
        owned = ids.mapNotNull { WordSiegeMascotSkin.fromProductId(it) }.toSet()
    }

    suspend fun refresh(context: Context) {
        if (!SupabaseProvider.configured) return
        val keys = runCatching {
            SupabaseProvider.client.postgrest.rpc("get_my_mascots_v1").decodeAs<List<String>>()
        }.getOrNull() ?: return
        val next = keys.mapNotNull { WordSiegeMascotSkin.fromProductId(it) }.toSet()
        owned = next
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_OWNED, next.map { it.productId }.toSet())
            .apply()
    }
}

private fun WordSiegeMascotSkin.pitch(): String = when (this) {
    WordSiegeMascotSkin.ORB -> sh("Sıcacık ve sadık bir koç. Her hamlende yanında, seni hiç yalnız bırakmaz.", "A warm, loyal coach. Beside you on every move, never leaves you alone.")
    WordSiegeMascotSkin.PINK -> sh("Tatlı, duygusal ve sevgi dolu. Kalpler saçar, sevinçten uçar, üzülünce sarılır.", "Sweet, emotional and loving. Showers hearts, flies with joy, hugs when you're sad.")
    WordSiegeMascotSkin.DEVIL_BLUE -> sh("Buz gibi soğukkanlı. Az konuşur, çok izler; söylediği tek cümle yerini bulur.", "Ice-cold and composed. Says little, watches a lot; every line lands.")
    WordSiegeMascotSkin.DEVIL_RED -> sh("Ateşli, hırslı, yerinde duramaz. Rakibe laf atar, her kelimende coşar.", "Fiery, fierce and restless. Trash-talks the rival, erupts on every word.")
    WordSiegeMascotSkin.CAT -> sh("Tembel, cilveli ve biraz kibirli. Uçan harfleri kovalar, kazanınca mırlar.", "Lazy, sassy and a bit proud. Chases flying letters, purrs when you win.")
    WordSiegeMascotSkin.ROBOT -> sh("Her şeyi hesaplar, istatistikle konuşur. Mutluluk modülü sende aşırı yüklenir.", "Calculates everything, speaks in stats. Its joy module overloads for you.")
    WordSiegeMascotSkin.ASTRONAUT -> sh("Hayalperest bir kâşif. Sıfır yerçekiminde takla atar, seni yıldızlara taşır.", "A dreamy explorer. Flips in zero gravity and takes you to the stars.")
}

/** Card background: a soft gradient in the character's own colours. */
private fun WordSiegeMascotSkin.cardBrush(): Brush {
    val decor = WordSiegeMascotDecor.of(this)
    val light = Color(decor.handColors[0])
    val dark = Color(decor.handColors[1])
    return Brush.linearGradient(listOf(Hf.Ground, dark.copy(alpha = .55f), light.copy(alpha = .30f)))
}

/** The mascot shop: every character shown alive, sold as a permanent Google Play product. */
@Composable
internal fun MascotStoreSection() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    val bond = remember { WordSiegeMascotBond(context) }
    var chosen by remember { mutableStateOf(bond.skinChoice) }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                val skin = WordSiegeMascotSkin.fromProductId(productId)
                // Restores re-deliver old purchases; skip those the server already knows.
                if (productId != null && skin != null && skin !in WordSiegeMascotOwnership.owned) {
                    scope.launch {
                        busy = productId
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                WordSiegeMascotOwnership.refresh(context)
                                bond.skinChoice = skin
                                chosen = skin
                                notice = sh("${skin.titleTr} artık senin! Yanına uçuyor ✨", "${skin.titleEn} is yours! On its way ✨")
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() ->
                                        sh("Google Play sunucu doğrulaması henüz etkin değil.", "Google Play server verification is not enabled yet.")
                                    "product_disabled" in error.message.orEmpty() ->
                                        sh("Bu maskot henüz satışa açılmadı.", "This mascot is not on sale yet.")
                                    else -> sh("Ödeme doğrulaması tamamlanamadı; yeniden deneyebilirsin.", "Purchase verification failed; you can retry.")
                                }
                            }
                        busy = null
                    }
                }
            },
            onMessage = { message -> notice = message; busy = null },
        )
    }

    DisposableEffect(manager) {
        manager.connect {
            manager.queryOneTimeProducts(ProductCatalog.mascotProducts) { products = it }
            manager.restorePurchases(ProductCatalog.mascotProducts.toSet())
        }
        onDispose { manager.close() }
    }
    LaunchedEffect(Unit) { WordSiegeMascotOwnership.refresh(context) }

    fun buy(skin: WordSiegeMascotSkin) {
        val product = products[skin.productId]
        if (activity == null || product?.oneTimePurchaseOfferDetails == null) {
            notice = sh("Bu maskot Google Play'de henüz satışta değil.", "This mascot is not on Google Play yet.")
            return
        }
        busy = skin.productId
        val result = manager.launchProduct(activity, product)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            busy = null
            notice = sh("Google Play ödeme ekranı açılamadı (${result.responseCode}).", "Google Play billing could not open (${result.responseCode}).")
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MascotStoreHero()
        if (notice.isNotBlank()) {
            Text(notice, color = SonHarfTheme.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        WordSiegeMascotSkin.entries.forEach { skin ->
            val owned = skin in WordSiegeMascotOwnership.owned
            MascotStoreCard(
                skin = skin,
                offer = products[skin.productId]?.oneTimePurchaseOfferDetails,
                owned = owned,
                active = owned && (chosen ?: WordSiegeMascotOwnership.owned.firstOrNull()) == skin,
                busy = busy != null,
                onBuy = { buy(skin) },
                onUse = {
                    bond.skinChoice = skin
                    chosen = skin
                    notice = sh("${skin.titleTr} seçildi.", "${skin.titleEn} selected.")
                },
            )
        }
        Text(
            sh(
                "Maskotlar tek ödemeyle kalıcıdır ve yalnızca görünüm ve eşlik sağlar; oyunda puan, ipucu veya avantaj vermez.",
                "Mascots are a one-time, permanent purchase and are purely cosmetic companions; they give no score, hint or advantage.",
            ),
            color = SonHarfMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun MascotStoreHero() {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Hf.Gold.copy(alpha = .55f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(listOf(Hf.Surface, Hf.Ground, Color(0xFFFFF3D6))))
                .padding(16.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(sh("MASKOTLAR", "MASCOTS"), color = Hf.GoldDeep, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    sh("Seni tanıyan, sevinen, üzülen ve konuşan canlı bir oyun arkadaşı.", "A living game buddy that knows you, cheers, frets and talks."),
                    color = Hf.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(10.dp))
                // A small parade of characters, alive.
                Row(Modifier.fillMaxWidth().height(125.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    listOf(WordSiegeMascotSkin.PINK, WordSiegeMascotSkin.ORB, WordSiegeMascotSkin.CAT).forEachIndexed { index, skin ->
                        WordSiegeMascot(
                            moveId = null,
                            lastMoveMine = false,
                            pendingCells = emptyList(),
                            playerTurn = false,
                            modifier = Modifier.size(if (index == 1) 120.dp else 99.dp).offset(y = if (index % 2 == 0) 8.dp else 0.dp),
                            skin = skin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MascotStoreCard(
    skin: WordSiegeMascotSkin,
    offer: ProductDetails.OneTimePurchaseOfferDetails?,
    owned: Boolean,
    active: Boolean,
    busy: Boolean,
    onBuy: () -> Unit,
    onUse: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) Hf.Gold else Hf.Gold.copy(alpha = .55f)),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.background(skin.cardBrush()).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(156.dp).clip(RoundedCornerShape(22.dp)).background(Hf.Text.copy(alpha = .05f)),
                contentAlignment = Alignment.Center,
            ) {
                WordSiegeMascot(
                    moveId = null,
                    lastMoveMine = false,
                    pendingCells = emptyList(),
                    playerTurn = false,
                    modifier = Modifier.size(146.dp),
                    skin = skin,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sh(skin.titleTr, skin.titleEn), color = Hf.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(sh(skin.kindTr, skin.kindEn), color = Hf.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(3.dp))
                Text(skin.pitch(), color = Hf.Text.copy(alpha = .9f), fontSize = 11.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    sh("Uçar • konuşur • seni hatırlar", "Flies • talks • remembers you"),
                    color = Hf.Gold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                when {
                    owned -> Button(
                        onClick = onUse,
                        enabled = !active,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Hf.Gold,
                            contentColor = Hf.Ink,
                            disabledContainerColor = Hf.Disabled,
                            disabledContentColor = Hf.Text,
                        ),
                    ) {
                        Text(if (active) sh("SEÇİLİ", "SELECTED") else sh("SAHİPSİN • SEÇ", "OWNED • USE"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    offer != null -> Button(
                        onClick = onBuy,
                        enabled = !busy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Hf.Ivory, contentColor = Hf.Text),
                    ) {
                        Text(sh("SATIN AL • ", "BUY • ") + offer.formattedPrice, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    else -> Text(
                        // The list price is shown for information only until Google Play offers the product.
                        sh("${ProductCatalog.MASCOT_LIST_PRICE_TRY} • Google Play'de yakında", "${ProductCatalog.MASCOT_LIST_PRICE_TRY} • Coming soon on Google Play"),
                        color = Hf.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
