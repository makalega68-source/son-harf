package com.sonharf.game.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class BillingManager(
    context: Context,
    private val onPurchase: (Purchase) -> Unit = {},
    private val onPending: (Purchase) -> Unit = {},
    private val onMessage: (String) -> Unit = {},
) : PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun connect(onReady: () -> Unit = {}) {
        if (client.isReady) {
            onReady()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                } else {
                    onMessage("Google Play bağlantısı kurulamadı (${result.responseCode}).")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Auto reconnection is enabled. Do not grant locally while disconnected.
                onMessage("Google Play bağlantısı geçici olarak kesildi.")
            }
        })
    }

    fun querySubscriptions(productIds: List<String>, onResult: (Map<String, ProductDetails>) -> Unit) {
        queryProducts(productIds, BillingClient.ProductType.SUBS, onResult)
    }

    fun queryOneTimeProducts(productIds: List<String>, onResult: (Map<String, ProductDetails>) -> Unit) {
        queryProducts(productIds, BillingClient.ProductType.INAPP, onResult)
    }

    private fun queryProducts(
        productIds: List<String>,
        productType: String,
        onResult: (Map<String, ProductDetails>) -> Unit,
    ) {
        if (!client.isReady) {
            onMessage("Google Play henüz hazır değil.")
            onResult(emptyMap())
            return
        }

        val products = productIds.distinct().map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(productType)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        client.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onMessage("Google Play ürünleri alınamadı (${result.responseCode}).")
                onResult(emptyMap())
                return@queryProductDetailsAsync
            }
            onResult(detailsResult.productDetailsList.associateBy { it.productId })
        }
    }

    fun launchProduct(activity: Activity, productDetails: ProductDetails): BillingResult {
        val detailsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?.let(detailsBuilder::setOfferToken)

        return client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(detailsBuilder.build()))
                .build(),
        )
    }

    /**
     * Re-processes owned Play purchases through the same server verifier used by new purchases.
     * The backend purchase-token uniqueness constraint is the idempotency boundary, so restore
     * cannot grant Son Coin or inventory twice.
     */
    fun restorePurchases(onComplete: (Int) -> Unit = {}) {
        if (!client.isReady) {
            onMessage("Google Play henüz hazır değil.")
            onComplete(0)
            return
        }

        var remaining = 2
        var restored = 0
        fun finishOne() {
            remaining -= 1
            if (remaining == 0) onComplete(restored)
        }

        listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS).forEach { type ->
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            restored += 1
                            onPurchase(purchase)
                        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                            onPending(purchase)
                        }
                    }
                } else {
                    onMessage("Satın almalar geri yüklenemedi (${result.responseCode}).")
                }
                finishOne()
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> onPurchase(purchase)
                        Purchase.PurchaseState.PENDING -> {
                            onPending(purchase)
                            onMessage("Ödeme beklemede. Google Play onayından sonra etkinleşecek.")
                        }
                        else -> Unit
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> onMessage("Satın alma iptal edildi.")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> onMessage("Bu ürün zaten hesabında. Geri yükleme deneniyor.")
            BillingClient.BillingResponseCode.NETWORK_ERROR -> onMessage("Ağ bağlantısı nedeniyle ödeme tamamlanamadı. Yeniden deneyebilirsin.")
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> onMessage("Google Play ödeme hizmeti şu anda kullanılamıyor.")
            else -> onMessage("Google Play işlemi tamamlanamadı (${result.responseCode}).")
        }
    }

    fun close() = client.endConnection()
}
