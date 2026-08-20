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

class BillingManager(
    context: Context,
    private val onPurchase: (Purchase) -> Unit = {},
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

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> onPurchase(purchase)
                        Purchase.PurchaseState.PENDING -> onMessage("Ödeme beklemede. Google Play onayından sonra etkinleşecek.")
                        else -> Unit
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> onMessage("Satın alma iptal edildi.")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> onMessage("Bu ürün zaten hesabında görünüyor.")
            else -> onMessage("Google Play işlemi tamamlanamadı (${result.responseCode}).")
        }
    }

    fun close() = client.endConnection()
}
