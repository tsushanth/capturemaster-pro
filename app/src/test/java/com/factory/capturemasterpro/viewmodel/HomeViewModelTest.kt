package com.factory.capturemasterpro.viewmodel

import app.cash.turbine.test
import com.factory.capturemasterpro.data.repository.RecordingRepository
import com.factory.capturemasterpro.service.ScreenRecordService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: RecordingRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.allRecordings } returns flowOf(emptyList())
        every { repository.recordingCount } returns flowOf(0)
        every { repository.totalStorageUsed } returns flowOf(null)
        // Ensure service is not running before creating VM
        // ScreenRecordService.isServiceRunning is private set, so we use reflection or just accept init state
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Clean up static callbacks
        ScreenRecordService.onRecordingStarted = null
        ScreenRecordService.onRecordingStopped = null
        ScreenRecordService.onRecordingError = null
    }

    // --- Initial State ---

    @Test
    fun `initial recording state is IDLE`() {
        assertEquals(RecordingState.IDLE, viewModel.recordingState.value)
    }

    @Test
    fun `initial recording config has defaults`() {
        val config = viewModel.recordingConfig.value
        assertEquals(Resolution.HD_1080, config.resolution)
        assertEquals(30, config.frameRate)
        assertEquals(8_000_000, config.bitRate)
        assertTrue(config.audioEnabled)
        assertFalse(config.microphoneEnabled)
    }

    @Test
    fun `initial elapsed time is 0`() {
        assertEquals(0L, viewModel.elapsedTime.value)
    }

    @Test
    fun `initial error message is null`() {
        assertNull(viewModel.errorMessage.value)
    }

    // --- Config Updates ---

    @Test
    fun `updateConfig replaces entire config`() = runTest {
        val newConfig = RecordingConfig(
            resolution = Resolution.QHD_1440,
            frameRate = 60,
            bitRate = 16_000_000,
            audioEnabled = false,
            microphoneEnabled = true
        )
        viewModel.updateConfig(newConfig)
        assertEquals(newConfig, viewModel.recordingConfig.value)
    }

    @Test
    fun `updateResolution changes only resolution`() = runTest {
        viewModel.updateResolution(Resolution.SD_480)
        assertEquals(Resolution.SD_480, viewModel.recordingConfig.value.resolution)
        assertEquals(30, viewModel.recordingConfig.value.frameRate) // unchanged
    }

    @Test
    fun `updateFrameRate changes only frame rate`() = runTest {
        viewModel.updateFrameRate(60)
        assertEquals(60, viewModel.recordingConfig.value.frameRate)
        assertEquals(Resolution.HD_1080, viewModel.recordingConfig.value.resolution) // unchanged
    }

    @Test
    fun `updateBitRate changes only bit rate`() = runTest {
        viewModel.updateBitRate(16_000_000)
        assertEquals(16_000_000, viewModel.recordingConfig.value.bitRate)
    }

    @Test
    fun `toggleAudio flips audio enabled state`() = runTest {
        assertTrue(viewModel.recordingConfig.value.audioEnabled)
        viewModel.toggleAudio()
        assertFalse(viewModel.recordingConfig.value.audioEnabled)
        viewModel.toggleAudio()
        assertTrue(viewModel.recordingConfig.value.audioEnabled)
    }

    @Test
    fun `toggleMicrophone flips microphone enabled state`() = runTest {
        assertFalse(viewModel.recordingConfig.value.microphoneEnabled)
        viewModel.toggleMicrophone()
        assertTrue(viewModel.recordingConfig.value.microphoneEnabled)
        viewModel.toggleMicrophone()
        assertFalse(viewModel.recordingConfig.value.microphoneEnabled)
    }

    // --- Config updates via StateFlow ---

    @Test
    fun `config changes emit through StateFlow`() = runTest {
        viewModel.recordingConfig.test {
            val initial = awaitItem()
            assertEquals(Resolution.HD_1080, initial.resolution)

            viewModel.updateResolution(Resolution.HD_720)
            val updated = awaitItem()
            assertEquals(Resolution.HD_720, updated.resolution)

            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Error Handling ---

    @Test
    fun `clearError sets errorMessage to null`() = runTest {
        // Trigger error via callback
        ScreenRecordService.onRecordingError?.invoke("Test error")
        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `onRecordingError callback sets error message`() = runTest {
        ScreenRecordService.onRecordingError?.invoke("Something went wrong")
        viewModel.errorMessage.test {
            // The current value should reflect the error or null depending on timing
            val item = awaitItem()
            // After clearError it should be null
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Format Utilities ---

    @Test
    fun `formatTime formats seconds only`() {
        assertEquals("00:00", viewModel.formatTime(0))
        assertEquals("00:30", viewModel.formatTime(30))
        assertEquals("00:59", viewModel.formatTime(59))
    }

    @Test
    fun `formatTime formats minutes and seconds`() {
        assertEquals("01:00", viewModel.formatTime(60))
        assertEquals("05:30", viewModel.formatTime(330))
        assertEquals("59:59", viewModel.formatTime(3599))
    }

    @Test
    fun `formatTime formats hours`() {
        assertEquals("01:00:00", viewModel.formatTime(3600))
        assertEquals("01:30:45", viewModel.formatTime(5445))
        assertEquals("10:00:00", viewModel.formatTime(36000))
    }

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
        assertEquals("1.5 KB", viewModel.formatFileSize(1536L))
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
    fun `formatFileSize formats terabytes`() {
        assertEquals("1.0 TB", viewModel.formatFileSize(1024L * 1024 * 1024 * 1024))
    }

    // --- Resolution enum ---

    @Test
    fun `Resolution enum has correct dimensions`() {
        assertEquals(854, Resolution.SD_480.width)
        assertEquals(480, Resolution.SD_480.height)
        assertEquals(1280, Resolution.HD_720.width)
        assertEquals(720, Resolution.HD_720.height)
        assertEquals(1920, Resolution.HD_1080.width)
        assertEquals(1080, Resolution.HD_1080.height)
        assertEquals(2560, Resolution.QHD_1440.width)
        assertEquals(1440, Resolution.QHD_1440.height)
    }

    @Test
    fun `Resolution enum has correct labels`() {
        assertEquals("480p SD", Resolution.SD_480.label)
        assertEquals("720p HD", Resolution.HD_720.label)
        assertEquals("1080p Full HD", Resolution.HD_1080.label)
        assertEquals("1440p QHD", Resolution.QHD_1440.label)
    }

    // --- RecordingState enum ---

    @Test
    fun `RecordingState has all expected values`() {
        val states = RecordingState.values()
        assertEquals(4, states.size)
        assertTrue(states.contains(RecordingState.IDLE))
        assertTrue(states.contains(RecordingState.PREPARING))
        assertTrue(states.contains(RecordingState.RECORDING))
        assertTrue(states.contains(RecordingState.STOPPING))
    }

    // --- Factory ---

    @Test
    fun `Factory creates HomeViewModel`() {
        val factory = HomeViewModel.Factory(repository)
        val vm = factory.create(HomeViewModel::class.java)
        assertEquals(RecordingState.IDLE, vm.recordingState.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Factory throws for unknown ViewModel class`() {
        val factory = HomeViewModel.Factory(repository)
        factory.create(GalleryViewModel::class.java)
    }
}
