package com.factory.capturemasterpro.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(
    context: Context,
    private val premiumManager: PremiumManager
) {
    companion object {
        private const val TAG = "BillingManager"

        // Subscription product IDs
        const val SUB_YEARLY = "com.factory.capturemasterpro.subscription.yearly"

        // In-app purchase product IDs
        const val IAP_LIFETIME = "com.factory.capturemasterpro.subscription.lifetime"
        const val IAP_REMOVE_ADS = "com.factory.capturemasterpro.remove_ads"

        private val SUB_PRODUCT_IDS = listOf(SUB_YEARLY)
        private val IAP_PRODUCT_IDS = listOf(IAP_LIFETIME, IAP_REMOVE_ADS)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        scope.launch {
            handlePurchaseResult(billingResult, purchases)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        if (_connectionState.value == ConnectionState.CONNECTED) return

        _connectionState.value = ConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = ConnectionState.CONNECTED
                    scope.launch {
                        queryProductDetails()
                        queryExistingPurchases()
                    }
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private suspend fun queryProductDetails() {
        val details = mutableMapOf<String, ProductDetails>()

        // Query subscriptions
        if (SUB_PRODUCT_IDS.isNotEmpty()) {
            val subProducts = SUB_PRODUCT_IDS.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
            val subParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subProducts)
                .build()

            val subResult = suspendCancellableCoroutine { continuation ->
                billingClient.queryProductDetailsAsync(subParams) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        productDetailsList.forEach { details[it.productId] = it }
                    } else {
                        Log.e(TAG, "Sub query failed: ${billingResult.debugMessage}")
                    }
                    continuation.resume(Unit)
                }
            }
        }

        // Query in-app purchases
        if (IAP_PRODUCT_IDS.isNotEmpty()) {
            val iapProducts = IAP_PRODUCT_IDS.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
            val iapParams = QueryProductDetailsParams.newBuilder()
                .setProductList(iapProducts)
                .build()

            suspendCancellableCoroutine { continuation ->
                billingClient.queryProductDetailsAsync(iapParams) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        productDetailsList.forEach { details[it.productId] = it }
                    } else {
                        Log.e(TAG, "IAP query failed: ${billingResult.debugMessage}")
                    }
                    continuation.resume(Unit)
                }
            }
        }

        _productDetails.value = details
        Log.d(TAG, "Loaded ${details.size} products: ${details.keys}")
    }

    suspend fun queryExistingPurchases() {
        // Check subscriptions
        val subParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                scope.launch {
                    processPurchases(purchases)
                }
            }
        }

        // Check in-app purchases
        val iapParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(iapParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                scope.launch {
                    processPurchases(purchases)
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = _productDetails.value[productId]
        if (details == null) {
            _purchaseState.value = PurchaseState.Error("Product not available. Please try again later.")
            return
        }

        val productDetailsParamsList = if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                _purchaseState.value = PurchaseState.Error("Subscription offer not available.")
                return
            }
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _purchaseState.value = PurchaseState.Loading
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseState.Error("Could not start purchase flow.")
            Log.e(TAG, "Launch billing flow failed: ${result.debugMessage}")
        }
    }

    private suspend fun handlePurchaseResult(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { processPurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchaseState.value = PurchaseState.AlreadyOwned
                queryExistingPurchases()
            }
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                _purchaseState.value = PurchaseState.Error("Network error. Please check your connection.")
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(
                    "Purchase failed: ${billingResult.debugMessage}"
                )
            }
        }
    }

    private suspend fun processPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }

                    val products = purchase.products
                    val isSubscription = products.any { it in SUB_PRODUCT_IDS }
                    val isLifetime = products.contains(IAP_LIFETIME)
                    val isRemoveAds = products.contains(IAP_REMOVE_ADS)

                    if (isSubscription || isLifetime) {
                        premiumManager.setPremiumStatus(true)
                    }
                    if (isRemoveAds) {
                        premiumManager.setAdsRemoved(true)
                    }

                    _purchaseState.value = PurchaseState.Success
                }
                Purchase.PurchaseState.PENDING -> {
                    _purchaseState.value = PurchaseState.Pending
                }
                else -> {
                    // UNSPECIFIED_STATE - do nothing
                }
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully")
                } else {
                    Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
                }
                continuation.resume(Unit)
            }
        }
    }

    fun restorePurchases() {
        scope.launch {
            _purchaseState.value = PurchaseState.Loading
            queryExistingPurchases()
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun endConnection() {
        billingClient.endConnection()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun getFormattedPrice(productId: String): String? {
        val details = _productDetails.value[productId] ?: return null
        return if (details.productType == BillingClient.ProductType.SUBS) {
            details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                ?.formattedPrice
        } else {
            details.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED
}

sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Loading : PurchaseState()
    data object Success : PurchaseState()
    data object Cancelled : PurchaseState()
    data object Pending : PurchaseState()
    data object AlreadyOwned : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
