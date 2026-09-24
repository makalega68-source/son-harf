package com.sonharf.game

import androidx.annotation.DrawableRes
import com.sonharf.game.billing.ProductCatalog

/**
 * The one place that maps a Google Play product id to its store artwork. Every store card asks
 * this map, so the picture follows the product id rather than the screen that shows it.
 * Product ids, prices and billing behaviour are not changed here.
 */
internal object StoreProductArtwork {
    @Suppress("DEPRECATION")
    @DrawableRes
    fun forProduct(productId: String): Int? = when (productId) {
        ProductCatalog.COINS_500 -> R.drawable.store_product_coins_500
        ProductCatalog.COINS_1500 -> R.drawable.store_product_coins_1500
        ProductCatalog.COINS_3500 -> R.drawable.store_product_coins_3500
        ProductCatalog.COINS_8000 -> R.drawable.store_product_coins_8000
        ProductCatalog.VIP_MONTHLY -> R.drawable.store_product_vip_monthly
        ProductCatalog.VIP_YEARLY -> R.drawable.store_product_vip_yearly
        ProductCatalog.SEASON_PASS_MONTHLY -> R.drawable.store_product_season_pass_monthly
        ProductCatalog.STARTER_STYLE_PACK -> R.drawable.store_product_starter_style_pack
        ProductCatalog.THEME_NEON -> R.drawable.store_product_theme_neon
        else -> null
    }

    /** Artwork for [productId], or the existing vector [fallback] when no artwork is mapped. */
    @DrawableRes
    fun forProduct(productId: String, @DrawableRes fallback: Int): Int = forProduct(productId) ?: fallback
}
