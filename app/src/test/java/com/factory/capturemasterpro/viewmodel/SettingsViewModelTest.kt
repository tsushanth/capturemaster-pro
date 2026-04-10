package com.factory.capturemasterpro.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import com.factory.capturemasterpro.data.repository.RecordingRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: RecordingRepository
    private lateinit var context: Context
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.recordingCount } returns flowOf(5)
        every { repository.totalStorageUsed } returns flowOf(10_000_000L)
        every { repository.totalRecordingTime } returns flowOf(300_000L)
        every { repository.allRecordings } returns flowOf(emptyList())
        every { repository.favoriteRecordings } returns flowOf(emptyList())

        context = RuntimeEnvironment.getApplication()
        // Clear DataStore before each test to prevent state leaking
        runBlocking { context.dataStore.edit { it.clear() } }
        viewModel = SettingsViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial State ---

    @Test
    fun `initial settings have correct defaults`() = runTest {
        viewModel.settings.test {
            val settings = awaitItem()
            assertEquals("HD_1080", settings.resolution)
            assertEquals(30, settings.frameRate)
            assertEquals(8_000_000, settings.bitRate)
            assertTrue(settings.audioEnabled)
            assertFalse(settings.microphoneEnabled)
            assertTrue(settings.showCountdown)
            assertEquals(3, settings.countdownSeconds)
            assertFalse(settings.showTouches)
            assertEquals("system", settings.darkMode)
            assertFalse(settings.dynamicColors)
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Repository Flows ---

    @Test
    fun `recordingCount emits repository value`() = runTest {
        viewModel.recordingCount.test {
            assertEquals(5, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `totalStorageUsed emits repository value`() = runTest {
        viewModel.totalStorageUsed.test {
            assertEquals(10_000_000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `totalRecordingTime emits repository value`() = runTest {
        viewModel.totalRecordingTime.test {
            assertEquals(300_000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Settings Updates (subscribe first, then mutate) ---

    @Test
    fun `updateResolution persists new resolution`() = runTest {
        viewModel.settings.test {
            awaitItem() // initial default
            viewModel.updateResolution("QHD_1440")
            assertEquals("QHD_1440", awaitItem().resolution)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updateFrameRate persists new frame rate`() = runTest {
        viewModel.settings.test {
            awaitItem() // initial
            viewModel.updateFrameRate(60)
            assertEquals(60, awaitItem().frameRate)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updateBitRate persists new bit rate`() = runTest {
        viewModel.settings.test {
            awaitItem() // initial
            viewModel.updateBitRate(16_000_000)
            assertEquals(16_000_000, awaitItem().bitRate)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `toggleAudio flips audio enabled`() = runTest {
        viewModel.settings.test {
            assertTrue(awaitItem().audioEnabled)
            viewModel.toggleAudio()
            assertFalse(awaitItem().audioEnabled)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `toggleMicrophone flips microphone enabled`() = runTest {
        viewModel.settings.test {
            assertFalse(awaitItem().microphoneEnabled)
            viewModel.toggleMicrophone()
            assertTrue(awaitItem().microphoneEnabled)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `toggleCountdown flips show countdown`() = runTest {
        viewModel.settings.test {
            assertTrue(awaitItem().showCountdown)
            viewModel.toggleCountdown()
            assertFalse(awaitItem().showCountdown)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updateCountdownSeconds persists new value`() = runTest {
        viewModel.settings.test {
            awaitItem() // initial
            viewModel.updateCountdownSeconds(5)
            assertEquals(5, awaitItem().countdownSeconds)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `toggleShowTouches flips show touches`() = runTest {
        viewModel.settings.test {
            assertFalse(awaitItem().showTouches)
            viewModel.toggleShowTouches()
            assertTrue(awaitItem().showTouches)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setDarkMode persists dark mode`() = runTest {
        viewModel.settings.test {
            assertEquals("system", awaitItem().darkMode)
            viewModel.setDarkMode("dark")
            assertEquals("dark", awaitItem().darkMode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `toggleDynamicColors flips dynamic colors`() = runTest {
        viewModel.settings.test {
            assertFalse(awaitItem().dynamicColors)
            viewModel.toggleDynamicColors()
            assertTrue(awaitItem().dynamicColors)
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Format Utilities ---

    @Test
    fun `formatFileSize handles null`() {
        assertEquals("0 B", viewModel.formatFileSize(null))
    }

    @Test
    fun `formatFileSize handles zero`() {
        assertEquals("0 B", viewModel.formatFileSize(0L))
    }

    @Test
    fun `formatFileSize formats bytes`() {
        assertEquals("500.0 B", viewModel.formatFileSize(500L))
    }

    @Test
    fun `formatFileSize formats kilobytes`() {
        assertEquals("1.0 KB", viewModel.formatFileSize(1024L))
    }

    @Test
    fun `formatFileSize formats megabytes`() {
        assertEquals("1.0 MB", viewModel.formatFileSize(1024L * 1024))
    }

    @Test
    fun `formatFileSize formats gigabytes`() {
        assertEquals("1.0 GB", viewModel.formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun `formatDuration handles null`() {
        assertEquals("0s", viewModel.formatDuration(null))
    }

    @Test
    fun `formatDuration handles zero`() {
        assertEquals("0s", viewModel.formatDuration(0L))
    }

    @Test
    fun `formatDuration formats minutes`() {
        assertEquals("1m", viewModel.formatDuration(60000L))
        assertEquals("5m", viewModel.formatDuration(300000L))
    }

    @Test
    fun `formatDuration formats hours and minutes`() {
        assertEquals("1h 0m", viewModel.formatDuration(3600000L))
        assertEquals("1h 30m", viewModel.formatDuration(5400000L))
        assertEquals("2h 15m", viewModel.formatDuration(8100000L))
    }

    // --- AppSettings data class ---

    @Test
    fun `AppSettings defaults are correct`() {
        val settings = AppSettings()
        assertEquals("HD_1080", settings.resolution)
        assertEquals(30, settings.frameRate)
        assertEquals(8_000_000, settings.bitRate)
        assertTrue(settings.audioEnabled)
        assertFalse(settings.microphoneEnabled)
        assertTrue(settings.showCountdown)
        assertEquals(3, settings.countdownSeconds)
        assertFalse(settings.showTouches)
        assertEquals("system", settings.darkMode)
        assertFalse(settings.dynamicColors)
    }

    // --- Factory ---

    @Test
    fun `Factory creates SettingsViewModel`() {
        val factory = SettingsViewModel.Factory(repository, context)
        val vm = factory.create(SettingsViewModel::class.java)
        assertEquals("0 B", vm.formatFileSize(null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Factory throws for unknown ViewModel class`() {
        val factory = SettingsViewModel.Factory(repository, context)
        factory.create(GalleryViewModel::class.java)
    }
}
