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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.serialization.json.put
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
        // Keep the server's copy of "the mascot I bring" in step with what this device shows.
        val pick = WordSiegeMascotBond(context).skinChoice?.takeIf { it in next } ?: next.minByOrNull { it.ordinal }
        PlayerMascots.publish(pick)
    }
}

/**
 * The mascot a player brings to games. Your pick is saved on the server (only an owned mascot is
 * accepted) so opponents can see it; seeing a rival's mascot needs no mascot of your own.
 */
internal object PlayerMascots {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun publish(skin: WordSiegeMascotSkin?) {
        if (!SupabaseProvider.configured) return
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "set_my_mascot_v1",
                kotlinx.serialization.json.buildJsonObject { put("p_key", skin?.productId) },
            )
        }
    }

    suspend fun of(userId: String): WordSiegeMascotSkin? {
        cache[userId]?.let { return WordSiegeMascotSkin.fromProductId(it) }
        if (!SupabaseProvider.configured) return null
        val key = runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "get_player_mascot_v1",
                kotlinx.serialization.json.buildJsonObject { put("p_user_id", userId) },
            ).decodeAs<String?>()
        }.getOrNull()
        cache[userId] = key.orEmpty()
        return WordSiegeMascotSkin.fromProductId(key)
    }
}

/** The mascot [userId] brings to the game, or null (bots and players without one). */
@androidx.compose.runtime.Composable
internal fun rememberRivalMascot(userId: String?): WordSiegeMascotSkin? {
    if (userId.isNullOrBlank()) return null
    var skin by androidx.compose.runtime.remember(userId) { androidx.compose.runtime.mutableStateOf<WordSiegeMascotSkin?>(null) }
    androidx.compose.runtime.LaunchedEffect(userId) {
        // A rival who holds the throne this week brings the Golden King, whatever they picked.
        val throne = runCatching { PublicFrames.get(userId) }.getOrNull() == ProfileFrameCollection.throneFrame.id
        skin = if (throne) WordSiegeMascotSkin.GOLD_KING else PlayerMascots.of(userId)
    }
    return skin
}

private fun WordSiegeMascotSkin.pitch(): String = when (this) {
    WordSiegeMascotSkin.ORB -> sh("Sıcacık ve sadık bir koç. Her hamlende yanında, seni hiç yalnız bırakmaz.", "A warm, loyal coach. Beside you on every move, never leaves you alone.")
    WordSiegeMascotSkin.PINK -> sh("Tatlı, duygusal ve sevgi dolu. Kalpler saçar, sevinçten uçar, üzülünce sarılır.", "Sweet, emotional and loving. Showers hearts, flies with joy, hugs when you're sad.")
    WordSiegeMascotSkin.DEVIL_BLUE -> sh("Buz gibi soğukkanlı. Az konuşur, çok izler; söylediği tek cümle yerini bulur.", "Ice-cold and composed. Says little, watches a lot; every line lands.")
    WordSiegeMascotSkin.DEVIL_RED -> sh("Ateşli, hırslı, yerinde duramaz. Rakibe laf atar, her kelimende coşar.", "Fiery, fierce and restless. Trash-talks the rival, erupts on every word.")
    WordSiegeMascotSkin.CAT -> sh("Tembel, cilveli ve biraz kibirli. Uçan harfleri kovalar, kazanınca mırlar.", "Lazy, sassy and a bit proud. Chases flying letters, purrs when you win.")
    WordSiegeMascotSkin.ROBOT -> sh("Her şeyi hesaplar, istatistikle konuşur. Mutluluk modülü sende aşırı yüklenir.", "Calculates everything, speaks in stats. Its joy module overloads for you.")
    WordSiegeMascotSkin.ASTRONAUT -> sh("Hayalperest bir kâşif. Sıfır yerçekiminde takla atar, seni yıldızlara taşır.", "A dreamy explorer. Flips in zero gravity and takes you to the stars.")
    WordSiegeMascotSkin.GOLD_KING -> sh("Maskotların kralı. Yalnızca taht sahibine bir hafta hizmet eder.", "King of mascots. Serves only the throne owner, for one week.")
}

/** One calm palette for the whole mascot catalogue, the same on every app theme. */
private object MascotShop {
    val CardTop = Color(0xFF16223D)
    val CardBottom = Color(0xFF223457)
    val Border = Color(0xFFE0A82E)
    val Title = Color(0xFFFFF6E0)
    val Body = Color(0xFFC9D3E3)
    val Faint = Color(0xFF8E9BB2)
    val Gold = Color(0xFFF2C14E)
    val Chip = Color(0x1FFFFFFF)
}

/**
 * Hides a coming-soon character: it is drawn as a soft, one-colour silhouette (works on every
 * Android version) and, where the system supports it (Android 12+), blurred on top.
 */
private fun Modifier.mascotMist(radius: androidx.compose.ui.unit.Dp): Modifier = this
    .blur(radius)
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(Color(0xFF9FB0CC), blendMode = BlendMode.SrcIn)
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
                                notice = sh("${skin.titleTr} artık senin! Profil > Obi'den seçebilirsin ✨", "${skin.titleEn} is yours! Pick it in Profile > Obi ✨")
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
        if (!skin.onSale) return
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

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "hero") { MascotStoreHero() }
        if (notice.isNotBlank()) {
            item(key = "notice") {
            Text(notice, color = Hf.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        items(WordSiegeMascotSkin.entries.filter { it.listed }, key = { it.id }) { skin ->
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
        item(key = "purchase_info") { Text(
            sh(
                "Maskotlar tek ödemeyle kalıcıdır. Her maçta 3 ipucu verir; puan satın alınmaz.",
                "Mascots are a one-time, permanent purchase. They give 3 hints every match; score is never sold.",
            ),
            color = Hf.TextMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        ) }
    }
}

@Composable
private fun MascotStoreHero() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, MascotShop.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MascotShop.CardTop, MascotShop.CardBottom)))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(sh("MASKOTLAR", "MASCOTS"), color = MascotShop.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                sh("Maçlarına eşlik eden oyun arkadaşın.", "Your companion for every match."),
                color = MascotShop.Body,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            // Obi in front and sharp; the coming-soon friends stay misted beside him.
            Box(Modifier.fillMaxWidth().height(156.dp)) {
                listOf(WordSiegeMascotSkin.PINK, WordSiegeMascotSkin.CAT, WordSiegeMascotSkin.ORB).forEach { skin ->
                    if (!skin.onSale) {
                        Icon(Icons.Rounded.Pets, null, tint = MascotShop.Faint.copy(alpha = .4f),
                            modifier = Modifier.align(if (skin == WordSiegeMascotSkin.PINK) Alignment.CenterStart else Alignment.CenterEnd).size(84.dp))
                    } else {
                    WordSiegeMascot(
                        moveId = null,
                        lastMoveMine = false,
                        pendingCells = emptyList(),
                        playerTurn = false,
                        modifier = Modifier.align(when (skin) {
                            WordSiegeMascotSkin.ORB -> Alignment.Center
                            WordSiegeMascotSkin.PINK -> Alignment.CenterStart
                            else -> Alignment.CenterEnd
                        }).size(if (skin.onSale) 150.8.dp else 111.8.dp)
                            .then(if (skin.onSale) Modifier else Modifier.mascotMist(8.dp)),
                        skin = skin,
                    )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // The mascot's advantage, stated up front.
            Surface(shape = RoundedCornerShape(50), color = MascotShop.Gold.copy(alpha = .16f), border = BorderStroke(1.dp, MascotShop.Gold.copy(alpha = .6f))) {
                Text(
                    sh("💡 Her maçta 3 ipucu", "💡 3 hints every match"),
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MascotShop.Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
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
    // Only Obi is on sale for now; the others are shown misted with "Yakında".
    val comingSoon = !owned && !skin.onSale
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (comingSoon) MascotShop.Faint.copy(alpha = .45f) else MascotShop.Border),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MascotShop.CardTop, MascotShop.CardBottom)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(153.4.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .06f)),
                contentAlignment = Alignment.Center,
            ) {
                if (comingSoon) Icon(Icons.Rounded.Pets, null, tint = MascotShop.Faint.copy(alpha = .4f), modifier = Modifier.size(92.dp))
                else WordSiegeMascot(
                    moveId = null,
                    lastMoveMine = false,
                    pendingCells = emptyList(),
                    playerTurn = false,
                    modifier = Modifier.size(140.4.dp).then(if (comingSoon) Modifier.mascotMist(10.dp) else Modifier),
                    skin = skin,
                )
                if (comingSoon) {
                    Surface(shape = RoundedCornerShape(50), color = MascotShop.CardTop.copy(alpha = .9f), border = BorderStroke(1.dp, MascotShop.Gold)) {
                        Text(
                            sh("ÇOK YAKINDA", "COMING SOON"),
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MascotShop.Gold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                // A coming-soon friend keeps its secret: no name, pitch or price yet.
                Text(
                    if (comingSoon) "???" else sh(skin.titleTr, skin.titleEn),
                    color = MascotShop.Title,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (comingSoon) sh("YENİ MASKOT", "NEW MASCOT") else sh(skin.kindTr, skin.kindEn),
                    color = MascotShop.Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (comingSoon) sh("Yeni bir oyun arkadaşı yolda. Çok yakında burada!", "A new game buddy is on the way. Here very soon!") else skin.pitch(),
                    color = MascotShop.Body,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                when {
                    comingSoon -> Surface(shape = RoundedCornerShape(12.dp), color = MascotShop.Chip) {
                        Text(
                            sh("YAKINDA", "COMING SOON"),
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MascotShop.Faint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                        )
                    }
                    // Bought mascots are chosen in the profile; the store only says it is yours.
                    owned -> Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Hf.Green,
                            disabledContentColor = Hf.OnAccent,
                        ),
                    ) {
                        Text(sh("SATIN ALINDI", "PURCHASED"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    offer != null -> Button(
                        onClick = onBuy,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MascotShop.Gold, contentColor = Color(0xFF3A2400)),
                    ) {
                        Text(sh("SATIN AL • ", "BUY • ") + offer.formattedPrice, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    else -> Text(
                        sh("Google Play'de yakında", "Coming soon on Google Play"),
                        color = MascotShop.Faint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
