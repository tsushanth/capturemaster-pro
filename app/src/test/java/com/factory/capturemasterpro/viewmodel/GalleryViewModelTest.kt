package com.factory.capturemasterpro.viewmodel

import app.cash.turbine.test
import com.factory.capturemasterpro.data.db.Recording
import com.factory.capturemasterpro.data.repository.RecordingRepository
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: RecordingRepository
    private lateinit var viewModel: GalleryViewModel

    private val recording1 = Recording(
        id = 1L, fileName = "video_a.mp4", filePath = "/a.mp4",
        duration = 60000L, fileSize = 2000L, createdAt = 1000L, isFavorite = false
    )
    private val recording2 = Recording(
        id = 2L, fileName = "video_b.mp4", filePath = "/b.mp4",
        duration = 30000L, fileSize = 5000L, createdAt = 2000L, isFavorite = true
    )
    private val recording3 = Recording(
        id = 3L, fileName = "video_c.mp4", filePath = "/c.mp4",
        duration = 90000L, fileSize = 1000L, createdAt = 3000L, isFavorite = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.allRecordings } returns flowOf(listOf(recording1, recording2, recording3))
        every { repository.favoriteRecordings } returns flowOf(listOf(recording2))
        every { repository.recordingCount } returns flowOf(3)
        every { repository.totalStorageUsed } returns flowOf(8000L)
        every { repository.totalRecordingTime } returns flowOf(180000L)
        viewModel = GalleryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial State ---

    @Test
    fun `initial sort order is DATE_DESC`() = runTest {
        assertEquals(SortOrder.DATE_DESC, viewModel.sortOrder.value)
    }

    @Test
    fun `initial showFavoritesOnly is false`() = runTest {
        assertFalse(viewModel.showFavoritesOnly.value)
    }

    @Test
    fun `initial selectedRecordings is empty`() = runTest {
        assertTrue(viewModel.selectedRecordings.value.isEmpty())
    }

    @Test
    fun `initial isSelectionMode is false`() = runTest {
        assertFalse(viewModel.isSelectionMode.value)
    }

    // --- Sort Order ---

    @Test
    fun `setSortOrder updates sort order`() = runTest {
        viewModel.setSortOrder(SortOrder.SIZE_DESC)
        assertEquals(SortOrder.SIZE_DESC, viewModel.sortOrder.value)
    }

    @Test
    fun `sortRecordings by DATE_DESC sorts newest first`() {
        viewModel.setSortOrder(SortOrder.DATE_DESC)
        val sorted = viewModel.sortRecordings(listOf(recording1, recording2, recording3))
        assertEquals(3L, sorted[0].id)
        assertEquals(2L, sorted[1].id)
        assertEquals(1L, sorted[2].id)
    }

    @Test
    fun `sortRecordings by DATE_ASC sorts oldest first`() {
        viewModel.setSortOrder(SortOrder.DATE_ASC)
        val sorted = viewModel.sortRecordings(listOf(recording1, recording2, recording3))
        assertEquals(1L, sorted[0].id)
        assertEquals(2L, sorted[1].id)
        assertEquals(3L, sorted[2].id)
    }

    @Test
    fun `sortRecordings by SIZE_DESC sorts largest first`() {
        viewModel.setSortOrder(SortOrder.SIZE_DESC)
        val sorted = viewModel.sortRecordings(listOf(recording1, recording2, recording3))
        assertEquals(2L, sorted[0].id) // 5000
        assertEquals(1L, sorted[1].id) // 2000
        assertEquals(3L, sorted[2].id) // 1000
    }

    @Test
    fun `sortRecordings by SIZE_ASC sorts smallest first`() {
        viewModel.setSortOrder(SortOrder.SIZE_ASC)
        val sorted = viewModel.sortRecordings(listOf(recording1, recording2, recording3))
        assertEquals(3L, sorted[0].id) // 1000
        assertEquals(1L, sorted[1].id) // 2000
        assertEquals(2L, sorted[2].id) // 5000
    }

    @Test
    fun `sortRecordings by DURATION_DESC sorts longest first`() {
        viewModel.setSortOrder(SortOrder.DURATION_DESC)
        val sorted = viewModel.sortRecordings(listOf(recording1, recording2, recording3))
        assertEquals(3L, sorted[0].id) // 90000
        assertEquals(1L, sorted[1].id) // 60000
        assertEquals(2L, sorted[2].id) // 30000
    }

    @Test
    fun `sortRecordings by NAME_ASC sorts alphabetically`() {
        viewModel.setSortOrder(SortOrder.NAME_ASC)
        val sorted = viewModel.sortRecordings(listOf(recording3, recording1, recording2))
        assertEquals("video_a.mp4", sorted[0].fileName)
        assertEquals("video_b.mp4", sorted[1].fileName)
        assertEquals("video_c.mp4", sorted[2].fileName)
    }

    // --- Favorites Filter ---

    @Test
    fun `toggleFavoritesFilter toggles state`() = runTest {
        assertFalse(viewModel.showFavoritesOnly.value)
        viewModel.toggleFavoritesFilter()
        assertTrue(viewModel.showFavoritesOnly.value)
        viewModel.toggleFavoritesFilter()
        assertFalse(viewModel.showFavoritesOnly.value)
    }

    @Test
    fun `toggleFavorite calls repository`() = runTest {
        viewModel.toggleFavorite(recording1)
        coVerify { repository.toggleFavorite(1L, true) }
    }

    @Test
    fun `toggleFavorite unfavorites a favorite recording`() = runTest {
        viewModel.toggleFavorite(recording2)
        coVerify { repository.toggleFavorite(2L, false) }
    }

    // --- Selection Mode ---

    @Test
    fun `toggleSelection adds recording to selection`() = runTest {
        viewModel.toggleSelection(1L)
        assertTrue(viewModel.selectedRecordings.value.contains(1L))
        assertTrue(viewModel.isSelectionMode.value)
    }

    @Test
    fun `toggleSelection removes recording from selection`() = runTest {
        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(1L)
        assertFalse(viewModel.selectedRecordings.value.contains(1L))
        assertFalse(viewModel.isSelectionMode.value)
    }

    @Test
    fun `toggleSelection with multiple recordings`() = runTest {
        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)
        assertEquals(setOf(1L, 2L), viewModel.selectedRecordings.value)
        assertTrue(viewModel.isSelectionMode.value)
    }

    @Test
    fun `clearSelection empties selection and exits selection mode`() = runTest {
        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)
        viewModel.clearSelection()
        assertTrue(viewModel.selectedRecordings.value.isEmpty())
        assertFalse(viewModel.isSelectionMode.value)
    }

    @Test
    fun `enterSelectionMode sets isSelectionMode to true`() = runTest {
        viewModel.enterSelectionMode()
        assertTrue(viewModel.isSelectionMode.value)
    }

    // --- Delete ---

    @Test
    fun `deleteRecording calls repository delete`() = runTest {
        viewModel.deleteRecording(recording1)
        coVerify { repository.deleteRecording(recording1) }
    }

    // --- Formatters ---

    @Test
    fun `formatDuration formats minutes and seconds`() {
        assertEquals("1:00", viewModel.formatDuration(60000L))
        assertEquals("0:30", viewModel.formatDuration(30000L))
    }

    @Test
    fun `formatDuration formats hours`() {
        assertEquals("1:00:00", viewModel.formatDuration(3600000L))
        assertEquals("1:30:00", viewModel.formatDuration(5400000L))
    }

    @Test
    fun `formatDuration formats zero`() {
        assertEquals("0:00", viewModel.formatDuration(0L))
    }

    @Test
    fun `formatFileSize formats bytes`() {
        assertEquals("0 B", viewModel.formatFileSize(0L))
        assertEquals("500.0 B", viewModel.formatFileSize(500L))
    }

    @Test
    fun `formatFileSize formats kilobytes`() {
        assertEquals("1.0 KB", viewModel.formatFileSize(1024L))
    }

    @Test
    fun `formatFileSize formats megabytes`() {
        assertEquals("1.0 MB", viewModel.formatFileSize(1024L * 1024L))
    }

    @Test
    fun `formatFileSize formats gigabytes`() {
        assertEquals("1.0 GB", viewModel.formatFileSize(1024L * 1024L * 1024L))
    }

    // --- Flow emissions ---

    @Test
    fun `allRecordings flow emits repository data`() = runTest {
        viewModel.allRecordings.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `favoriteRecordings flow emits only favorites`() = runTest {
        viewModel.favoriteRecordings.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("video_b.mp4", result[0].fileName)
            cancelAndConsumeRemainingEvents()
        }
    }
}
