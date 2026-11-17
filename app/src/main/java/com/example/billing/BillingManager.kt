package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.model.BillingPackage
import com.example.model.StoreCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BillingEvent {
    data class PurchaseVerified(val sku: String, val purchaseToken: String) : BillingEvent
    data class PurchaseMessage(val messageAr: String) : BillingEvent
}

sealed interface BillingLaunchResult {
    data object LaunchedPlaySheet : BillingLaunchResult
    data class RequiresSandboxCheckout(val pkg: BillingPackage) : BillingLaunchResult
}

/**
 * Google Play Billing Library manager.
 * Connects to Play Store Billing service, queries one-time SKUs, launches BillingFlow when
 * SKUs are configured in Play Console, and exposes a clean fallback trigger for sandbox testing
 * on emulator builds without published Play Console SKUs.
 */
class BillingManager(context: Context) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _events.tryEmit(BillingEvent.PurchaseMessage("تم إلغاء عملية الشراء"))
            }
            else -> {
                _events.tryEmit(
                    BillingEvent.PurchaseMessage("تعذر إتمام الدفع عبر المتجر (${result.responseCode})")
                )
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryCatalogProducts()
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    _isConnected.value = ok
                    if (ok) {
                        queryCatalogProducts()
                        queryExistingPurchases()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isConnected.value = false
                }
            })
        } catch (e: Exception) {
            Log.w("BillingManager", "Billing connection error: ${e.message}")
            _isConnected.value = false
        }
    }

    private fun queryCatalogProducts() {
        val productList = StoreCatalog.billingPackages.map { pkg ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(pkg.sku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = detailsList.associateBy { it.productId }
            }
        }
    }

    fun queryExistingPurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    fun launchPurchase(activity: Activity?, pkg: BillingPackage): BillingLaunchResult {
        val details = _productDetails.value[pkg.sku]
        if (activity != null && billingClient.isReady && details != null) {
            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                return BillingLaunchResult.LaunchedPlaySheet
            }
        }
        return BillingLaunchResult.RequiresSandboxCheckout(pkg)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val sku = purchase.products.firstOrNull() ?: return
        val isConsumable = sku != "ghars_vip_garden"

        if (isConsumable) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.consumeAsync(consumeParams) { result, _ ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _events.tryEmit(BillingEvent.PurchaseVerified(sku, purchase.purchaseToken))
                }
            }
        } else if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _events.tryEmit(BillingEvent.PurchaseVerified(sku, purchase.purchaseToken))
                }
            }
        }
    }

    fun endConnection() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
    }
}
