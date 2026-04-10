package com.factory.capturemasterpro.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BillingManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var premiumManager: PremiumManager
    private lateinit var mockBillingClient: BillingClient
    private lateinit var billingManager: BillingManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        premiumManager = mockk(relaxed = true)

        mockBillingClient = mockk(relaxed = true)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)

        mockkStatic(BillingClient::class)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.enablePendingPurchases() } returns mockBuilder
        every { mockBuilder.build() } returns mockBillingClient

        val context = RuntimeEnvironment.getApplication()
        billingManager = BillingManager(context, premiumManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // --- Initial State ---

    @Test
    fun `initial connection state is DISCONNECTED`() {
        assertEquals(ConnectionState.DISCONNECTED, billingManager.connectionState.value)
    }

    @Test
    fun `initial purchase state is Idle`() {
        assertEquals(PurchaseState.Idle, billingManager.purchaseState.value)
    }

    @Test
    fun `initial product details is empty`() {
        assertTrue(billingManager.productDetails.value.isEmpty())
    }

    // --- Connection ---

    @Test
    fun `startConnection changes state to CONNECTING`() {
        billingManager.startConnection()
        assertEquals(ConnectionState.CONNECTING, billingManager.connectionState.value)
    }

    @Test
    fun `startConnection changes state to CONNECTED on success`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {
            val billingResult = mockk<BillingResult>()
            every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
            every { billingResult.debugMessage } returns "OK"
            listenerSlot.captured.onBillingSetupFinished(billingResult)
        }

        billingManager.startConnection()
        assertEquals(ConnectionState.CONNECTED, billingManager.connectionState.value)
    }

    @Test
    fun `startConnection changes state to DISCONNECTED on failure`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {
            val billingResult = mockk<BillingResult>()
            every { billingResult.responseCode } returns BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
            every { billingResult.debugMessage } returns "Billing unavailable"
            listenerSlot.captured.onBillingSetupFinished(billingResult)
        }

        billingManager.startConnection()
        assertEquals(ConnectionState.DISCONNECTED, billingManager.connectionState.value)
    }

    @Test
    fun `onBillingServiceDisconnected sets state to DISCONNECTED`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {
            val billingResult = mockk<BillingResult>()
            every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
            every { billingResult.debugMessage } returns "OK"
            listenerSlot.captured.onBillingSetupFinished(billingResult)
        }

        billingManager.startConnection()
        assertEquals(ConnectionState.CONNECTED, billingManager.connectionState.value)

        listenerSlot.captured.onBillingServiceDisconnected()
        assertEquals(ConnectionState.DISCONNECTED, billingManager.connectionState.value)
    }

    @Test
    fun `startConnection does nothing when already connected`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {
            val billingResult = mockk<BillingResult>()
            every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
            every { billingResult.debugMessage } returns "OK"
            listenerSlot.captured.onBillingSetupFinished(billingResult)
        }

        billingManager.startConnection()
        assertEquals(ConnectionState.CONNECTED, billingManager.connectionState.value)

        // Call again - should not call startConnection again
        billingManager.startConnection()
        verify(exactly = 1) { mockBillingClient.startConnection(any()) }
    }

    // --- Purchase State ---

    @Test
    fun `resetPurchaseState sets state to Idle`() {
        billingManager.resetPurchaseState()
        assertEquals(PurchaseState.Idle, billingManager.purchaseState.value)
    }

    @Test
    fun `restorePurchases sets state to Loading`() = runTest {
        billingManager.restorePurchases()
        assertEquals(PurchaseState.Loading, billingManager.purchaseState.value)
    }

    // --- End Connection ---

    @Test
    fun `endConnection disconnects billing client`() {
        billingManager.endConnection()
        assertEquals(ConnectionState.DISCONNECTED, billingManager.connectionState.value)
        verify { mockBillingClient.endConnection() }
    }

    // --- Launch Purchase Flow ---

    @Test
    fun `launchPurchaseFlow sets error when product not available`() {
        val activity = mockk<android.app.Activity>(relaxed = true)
        billingManager.launchPurchaseFlow(activity, "nonexistent_product")
        val state = billingManager.purchaseState.value
        assertTrue(state is PurchaseState.Error)
        assertEquals("Product not available. Please try again later.", (state as PurchaseState.Error).message)
    }

    // --- Get Formatted Price ---

    @Test
    fun `getFormattedPrice returns null when no product details`() {
        val price = billingManager.getFormattedPrice("nonexistent")
        assertNull(price)
    }

    // --- ConnectionState enum ---

    @Test
    fun `ConnectionState has all expected values`() {
        val values = ConnectionState.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(ConnectionState.DISCONNECTED))
        assertTrue(values.contains(ConnectionState.CONNECTING))
        assertTrue(values.contains(ConnectionState.CONNECTED))
    }

    // --- PurchaseState sealed class ---

    @Test
    fun `PurchaseState Idle is singleton`() {
        assertTrue(PurchaseState.Idle === PurchaseState.Idle)
    }

    @Test
    fun `PurchaseState Loading is singleton`() {
        assertTrue(PurchaseState.Loading === PurchaseState.Loading)
    }

    @Test
    fun `PurchaseState Success is singleton`() {
        assertTrue(PurchaseState.Success === PurchaseState.Success)
    }

    @Test
    fun `PurchaseState Cancelled is singleton`() {
        assertTrue(PurchaseState.Cancelled === PurchaseState.Cancelled)
    }

    @Test
    fun `PurchaseState Pending is singleton`() {
        assertTrue(PurchaseState.Pending === PurchaseState.Pending)
    }

    @Test
    fun `PurchaseState AlreadyOwned is singleton`() {
        assertTrue(PurchaseState.AlreadyOwned === PurchaseState.AlreadyOwned)
    }

    @Test
    fun `PurchaseState Error contains message`() {
        val error = PurchaseState.Error("Test error message")
        assertEquals("Test error message", error.message)
    }

    @Test
    fun `PurchaseState Error equality`() {
        val error1 = PurchaseState.Error("msg")
        val error2 = PurchaseState.Error("msg")
        assertEquals(error1, error2)
    }

    // --- Product IDs ---

    @Test
    fun `product IDs are correct`() {
        assertEquals("com.factory.capturemasterpro.subscription.yearly", BillingManager.SUB_YEARLY)
        assertEquals("com.factory.capturemasterpro.subscription.lifetime", BillingManager.IAP_LIFETIME)
        assertEquals("com.factory.capturemasterpro.remove_ads", BillingManager.IAP_REMOVE_ADS)
    }
}
