package com.factory.capturemasterpro.billing

import android.content.Context
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import com.factory.capturemasterpro.viewmodel.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PremiumManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var premiumManager: PremiumManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = RuntimeEnvironment.getApplication()
        // Clear DataStore before each test to prevent state leaking
        runBlocking { context.dataStore.edit { it.clear() } }
        premiumManager = PremiumManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial State ---

    @Test
    fun `isPremium defaults to false`() = runTest {
        premiumManager.isPremium.test {
            assertFalse(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `isAdsRemoved defaults to false`() = runTest {
        premiumManager.isAdsRemoved.test {
            assertFalse(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Premium Status Persistence ---

    @Test
    fun `setPremiumStatus true updates isPremium flow`() = runTest {
        premiumManager.isPremium.test {
            assertFalse(awaitItem()) // default

            premiumManager.setPremiumStatus(true)
            assertTrue(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setPremiumStatus false reverts isPremium flow`() = runTest {
        premiumManager.setPremiumStatus(true)
        premiumManager.setPremiumStatus(false)

        premiumManager.isPremium.test {
            assertFalse(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setPremiumStatus persists across new PremiumManager instance`() = runTest {
        premiumManager.setPremiumStatus(true)

        val newManager = PremiumManager(context)
        newManager.isPremium.test {
            assertTrue(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Ads Removed Persistence ---

    @Test
    fun `setAdsRemoved true updates isAdsRemoved flow`() = runTest {
        premiumManager.isAdsRemoved.test {
            assertFalse(awaitItem()) // default

            premiumManager.setAdsRemoved(true)
            assertTrue(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setAdsRemoved false reverts isAdsRemoved flow`() = runTest {
        premiumManager.setAdsRemoved(true)
        premiumManager.setAdsRemoved(false)

        premiumManager.isAdsRemoved.test {
            assertFalse(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setAdsRemoved persists across new PremiumManager instance`() = runTest {
        premiumManager.setAdsRemoved(true)

        val newManager = PremiumManager(context)
        newManager.isAdsRemoved.test {
            assertTrue(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Independent State ---

    @Test
    fun `premium and ads removed are independent`() = runTest {
        premiumManager.setPremiumStatus(true)
        premiumManager.setAdsRemoved(false)

        premiumManager.isPremium.test {
            assertTrue(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        premiumManager.isAdsRemoved.test {
            assertFalse(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setting ads removed does not affect premium status`() = runTest {
        premiumManager.setAdsRemoved(true)

        premiumManager.isPremium.test {
            assertFalse(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- PremiumFeature Enum ---

    @Test
    fun `PremiumFeature has all expected values`() {
        val features = PremiumFeature.values()
        assertEquals(8, features.size)
    }

    @Test
    fun `PremiumFeature display names are correct`() {
        assertEquals("1080p Recording", PremiumFeature.RESOLUTION_1080P.displayName)
        assertEquals("1440p Recording", PremiumFeature.RESOLUTION_1440P.displayName)
        assertEquals("60 FPS Recording", PremiumFeature.FPS_60.displayName)
        assertEquals("Ultra Quality (16 Mbps)", PremiumFeature.ULTRA_QUALITY.displayName)
        assertEquals("Video Trimming", PremiumFeature.VIDEO_TRIMMING.displayName)
        assertEquals("Microphone Recording", PremiumFeature.MICROPHONE.displayName)
        assertEquals("Show Touches", PremiumFeature.SHOW_TOUCHES.displayName)
        assertEquals("Internal Audio Recording", PremiumFeature.INTERNAL_AUDIO.displayName)
    }

    @Test
    fun `PremiumFeature valueOf works correctly`() {
        assertEquals(PremiumFeature.RESOLUTION_1080P, PremiumFeature.valueOf("RESOLUTION_1080P"))
        assertEquals(PremiumFeature.VIDEO_TRIMMING, PremiumFeature.valueOf("VIDEO_TRIMMING"))
    }
}
