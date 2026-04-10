package com.factory.capturemasterpro.viewmodel

import app.cash.turbine.test
import com.factory.capturemasterpro.data.db.Recording
import com.factory.capturemasterpro.data.repository.RecordingRepository
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: RecordingRepository
    private lateinit var viewModel: EditorViewModel

    private val testRecording = Recording(
        id = 1L,
        fileName = "test.mp4",
        filePath = "/storage/test.mp4",
        duration = 120000L,
        fileSize = 5000000L,
        createdAt = 1000L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.allRecordings } returns flowOf(emptyList())
        every { repository.recordingCount } returns flowOf(0)
        every { repository.totalStorageUsed } returns flowOf(null)
        every { repository.totalRecordingTime } returns flowOf(null)
        viewModel = EditorViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial State ---

    @Test
    fun `initial state has correct defaults`() {
        val state = viewModel.state.value
        assertNull(state.recording)
        assertTrue(state.isLoading)
        assertEquals(0L, state.videoDurationMs)
        assertEquals(0L, state.trimStartMs)
        assertEquals(0L, state.trimEndMs)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.currentPositionMs)
        assertFalse(state.isProcessing)
        assertEquals(0f, state.processingProgress)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }

    // --- Load Recording ---

    @Test
    fun `loadRecording sets error when recording not found`() = runTest {
        coEvery { repository.getRecordingById(99L) } returns null

        viewModel.state.test {
            skipItems(1) // initial state
            viewModel.loadRecording(99L)

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Recording not found", state.errorMessage)
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Trim Controls ---

    @Test
    fun `setTrimStart updates trim start when less than end`() = runTest {
        // Manually set state with a duration
        viewModel.setTrimEnd(10000L) // won't work because start (0) < end needs to be set
        // Set a known state first
        viewModel.setTrimEnd(50000L) // end = 50000 since 50000 > 0 (start)
        viewModel.setTrimStart(5000L) // 5000 < 50000

        assertEquals(5000L, viewModel.state.value.trimStartMs)
    }

    @Test
    fun `setTrimStart does not update when greater than or equal to end`() = runTest {
        // trimEnd defaults to 0, so setting start >= 0 won't update
        viewModel.setTrimStart(5000L) // 5000 >= 0 (trimEnd), should not update
        assertEquals(0L, viewModel.state.value.trimStartMs)
    }

    @Test
    fun `setTrimEnd updates trim end when greater than start`() = runTest {
        viewModel.setTrimEnd(30000L)
        assertEquals(30000L, viewModel.state.value.trimEndMs)
    }

    @Test
    fun `setTrimEnd does not update when less than or equal to start`() = runTest {
        viewModel.setTrimEnd(50000L)
        viewModel.setTrimStart(20000L)
        viewModel.setTrimEnd(10000L) // 10000 <= 20000 (start), should not update
        assertEquals(50000L, viewModel.state.value.trimEndMs)
    }

    // --- Playback Controls ---

    @Test
    fun `setPlaying updates playing state`() = runTest {
        viewModel.setPlaying(true)
        assertTrue(viewModel.state.value.isPlaying)
        viewModel.setPlaying(false)
        assertFalse(viewModel.state.value.isPlaying)
    }

    @Test
    fun `updatePosition updates current position`() = runTest {
        viewModel.updatePosition(15000L)
        assertEquals(15000L, viewModel.state.value.currentPositionMs)
    }

    @Test
    fun `updatePosition emits through StateFlow`() = runTest {
        viewModel.state.test {
            skipItems(1) // initial

            viewModel.updatePosition(5000L)
            assertEquals(5000L, awaitItem().currentPositionMs)

            viewModel.updatePosition(10000L)
            assertEquals(10000L, awaitItem().currentPositionMs)

            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Error / Success Handling ---

    @Test
    fun `clearError clears error message`() = runTest {
        coEvery { repository.getRecordingById(99L) } returns null
        viewModel.loadRecording(99L)

        assertEquals("Recording not found", viewModel.state.value.errorMessage)
        viewModel.clearError()
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `clearSuccess clears success message`() = runTest {
        // Can't easily trigger success without file system, so test clearing directly
        viewModel.clearSuccess()
        assertNull(viewModel.state.value.successMessage)
    }

    // --- Format Utilities ---

    @Test
    fun `formatTime formats zero`() {
        assertEquals("00:00.00", viewModel.formatTime(0L))
    }

    @Test
    fun `formatTime formats seconds and centiseconds`() {
        assertEquals("00:01.00", viewModel.formatTime(1000L))
        assertEquals("00:30.50", viewModel.formatTime(30500L))
    }

    @Test
    fun `formatTime formats minutes`() {
        assertEquals("01:00.00", viewModel.formatTime(60000L))
        assertEquals("02:30.00", viewModel.formatTime(150000L))
    }

    @Test
    fun `formatTime formats with centiseconds`() {
        assertEquals("00:01.55", viewModel.formatTime(1550L))
        assertEquals("01:23.45", viewModel.formatTime(83450L))
    }

    // --- EditorState data class ---

    @Test
    fun `EditorState defaults are correct`() {
        val state = EditorState()
        assertNull(state.recording)
        assertTrue(state.isLoading)
        assertEquals(0L, state.videoDurationMs)
        assertEquals(0L, state.trimStartMs)
        assertEquals(0L, state.trimEndMs)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.currentPositionMs)
        assertFalse(state.isProcessing)
        assertEquals(0f, state.processingProgress)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun `EditorState copy preserves other fields`() {
        val state = EditorState(
            recording = testRecording,
            isLoading = false,
            videoDurationMs = 120000L,
            trimStartMs = 5000L,
            trimEndMs = 100000L
        )
        val copied = state.copy(isPlaying = true)
        assertEquals(testRecording, copied.recording)
        assertEquals(120000L, copied.videoDurationMs)
        assertEquals(5000L, copied.trimStartMs)
        assertTrue(copied.isPlaying)
    }

    // --- Factory ---

    @Test
    fun `Factory creates EditorViewModel`() {
        val factory = EditorViewModel.Factory(repository)
        val vm = factory.create(EditorViewModel::class.java)
        assertTrue(vm.state.value.isLoading)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Factory throws for unknown ViewModel class`() {
        val factory = EditorViewModel.Factory(repository)
        factory.create(GalleryViewModel::class.java)
    }
}
