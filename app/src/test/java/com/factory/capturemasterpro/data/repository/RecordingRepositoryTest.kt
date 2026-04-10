package com.factory.capturemasterpro.data.repository

import app.cash.turbine.test
import com.factory.capturemasterpro.data.db.Recording
import com.factory.capturemasterpro.data.db.RecordingDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RecordingRepositoryTest {

    private lateinit var dao: RecordingDao
    private lateinit var repository: RecordingRepository

    private val testRecording = Recording(
        id = 1L,
        fileName = "test.mp4",
        filePath = "/storage/test.mp4",
        duration = 60000L,
        fileSize = 1024000L,
        createdAt = 1000L
    )

    private val testRecording2 = Recording(
        id = 2L,
        fileName = "test2.mp4",
        filePath = "/storage/test2.mp4",
        duration = 30000L,
        fileSize = 512000L,
        isFavorite = true,
        createdAt = 2000L
    )

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        every { dao.getAllRecordings() } returns flowOf(listOf(testRecording, testRecording2))
        every { dao.getFavoriteRecordings() } returns flowOf(listOf(testRecording2))
        every { dao.getRecordingCount() } returns flowOf(2)
        every { dao.getTotalStorageUsed() } returns flowOf(1536000L)
        every { dao.getTotalRecordingTime() } returns flowOf(90000L)
        repository = RecordingRepository(dao)
    }

    @Test
    fun `allRecordings emits all recordings from dao`() = runTest {
        repository.allRecordings.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("test.mp4", result[0].fileName)
            assertEquals("test2.mp4", result[1].fileName)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `favoriteRecordings emits only favorites`() = runTest {
        repository.favoriteRecordings.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("test2.mp4", result[0].fileName)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `recordingCount emits correct count`() = runTest {
        repository.recordingCount.test {
            assertEquals(2, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `totalStorageUsed emits sum of file sizes`() = runTest {
        repository.totalStorageUsed.test {
            assertEquals(1536000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `totalRecordingTime emits sum of durations`() = runTest {
        repository.totalRecordingTime.test {
            assertEquals(90000L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getRecordingById returns recording when found`() = runTest {
        coEvery { dao.getRecordingById(1L) } returns testRecording
        val result = repository.getRecordingById(1L)
        assertEquals(testRecording, result)
        coVerify { dao.getRecordingById(1L) }
    }

    @Test
    fun `getRecordingById returns null when not found`() = runTest {
        coEvery { dao.getRecordingById(99L) } returns null
        val result = repository.getRecordingById(99L)
        assertNull(result)
    }

    @Test
    fun `getLatestRecording returns most recent recording`() = runTest {
        coEvery { dao.getLatestRecording() } returns testRecording2
        val result = repository.getLatestRecording()
        assertEquals(testRecording2, result)
    }

    @Test
    fun `getLatestRecording returns null when no recordings`() = runTest {
        coEvery { dao.getLatestRecording() } returns null
        assertNull(repository.getLatestRecording())
    }

    @Test
    fun `insertRecording delegates to dao and returns id`() = runTest {
        coEvery { dao.insertRecording(testRecording) } returns 1L
        val id = repository.insertRecording(testRecording)
        assertEquals(1L, id)
        coVerify { dao.insertRecording(testRecording) }
    }

    @Test
    fun `updateRecording delegates to dao`() = runTest {
        repository.updateRecording(testRecording)
        coVerify { dao.updateRecording(testRecording) }
    }

    @Test
    fun `deleteRecording delegates to dao`() = runTest {
        repository.deleteRecording(testRecording)
        coVerify { dao.deleteRecording(testRecording) }
    }

    @Test
    fun `deleteRecordingById delegates to dao`() = runTest {
        repository.deleteRecordingById(1L)
        coVerify { dao.deleteRecordingById(1L) }
    }

    @Test
    fun `toggleFavorite delegates to dao with correct params`() = runTest {
        repository.toggleFavorite(1L, true)
        coVerify { dao.updateFavoriteStatus(1L, true) }
    }

    @Test
    fun `toggleFavorite can set to not favorite`() = runTest {
        repository.toggleFavorite(2L, false)
        coVerify { dao.updateFavoriteStatus(2L, false) }
    }
}
