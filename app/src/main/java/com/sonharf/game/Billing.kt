package com.sonharf.game

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

/** PRO abonelik. Satın alma doğrulanınca Supabase'de activate_pro çağrılır. */
class Billing(private val ctx: Context, private val onPro: (Int) -> Unit) {

    private var client: BillingClient? = null

    private val listener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { p ->
                if (p.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!p.isAcknowledged) {
                        client?.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(p.purchaseToken).build()
                        ) {}
                    }
                    onPro(30)
                }
            }
        }
    }

    fun connect() {
        client = BillingClient.newBuilder(ctx)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {}
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun buyPro(activity: Activity) {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Config.PRO_SUB_ID)
                    .setProductType(BillingClient.ProductType.SUBS).build()
            )
        ).build()
        c.queryProductDetailsAsync(params) { _, list ->
            val pd = list.firstOrNull() ?: return@queryProductDetailsAsync
            val offer = pd.subscriptionOfferDetails?.firstOrNull() ?: return@queryProductDetailsAsync
            val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(pd)
                        .setOfferToken(offer.offerToken).build()
                )
            ).build()
            c.launchBillingFlow(activity, flow)
        }
    }
}
