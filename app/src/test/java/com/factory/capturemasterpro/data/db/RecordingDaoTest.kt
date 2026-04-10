package com.factory.capturemasterpro.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecordingDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RecordingDao

    private fun createRecording(
        fileName: String = "test.mp4",
        filePath: String = "/storage/test.mp4",
        duration: Long = 60000L,
        fileSize: Long = 1024000L,
        isFavorite: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) = Recording(
        fileName = fileName,
        filePath = filePath,
        duration = duration,
        fileSize = fileSize,
        isFavorite = isFavorite,
        createdAt = createdAt
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.recordingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insertRecording returns generated id`() = runTest {
        val recording = createRecording()
        val id = dao.insertRecording(recording)
        assertTrue(id > 0)
    }

    @Test
    fun `getRecordingById returns inserted recording`() = runTest {
        val recording = createRecording(fileName = "video1.mp4")
        val id = dao.insertRecording(recording)
        val result = dao.getRecordingById(id)
        assertNotNull(result)
        assertEquals("video1.mp4", result!!.fileName)
        assertEquals(id, result.id)
    }

    @Test
    fun `getRecordingById returns null for nonexistent id`() = runTest {
        val result = dao.getRecordingById(999L)
        assertNull(result)
    }

    @Test
    fun `getAllRecordings returns recordings ordered by createdAt DESC`() = runTest {
        dao.insertRecording(createRecording(fileName = "old.mp4", createdAt = 1000L))
        dao.insertRecording(createRecording(fileName = "new.mp4", createdAt = 2000L))
        dao.insertRecording(createRecording(fileName = "mid.mp4", createdAt = 1500L))

        dao.getAllRecordings().test {
            val recordings = awaitItem()
            assertEquals(3, recordings.size)
            assertEquals("new.mp4", recordings[0].fileName)
            assertEquals("mid.mp4", recordings[1].fileName)
            assertEquals("old.mp4", recordings[2].fileName)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getAllRecordings emits empty list when no recordings`() = runTest {
        dao.getAllRecordings().test {
            val recordings = awaitItem()
            assertTrue(recordings.isEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getFavoriteRecordings returns only favorites`() = runTest {
        dao.insertRecording(createRecording(fileName = "fav.mp4", isFavorite = true, createdAt = 2000L))
        dao.insertRecording(createRecording(fileName = "nonfav.mp4", isFavorite = false, createdAt = 1000L))

        dao.getFavoriteRecordings().test {
            val favorites = awaitItem()
            assertEquals(1, favorites.size)
            assertEquals("fav.mp4", favorites[0].fileName)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getLatestRecording returns most recent recording`() = runTest {
        dao.insertRecording(createRecording(fileName = "old.mp4", createdAt = 1000L))
        dao.insertRecording(createRecording(fileName = "newest.mp4", createdAt = 3000L))
        dao.insertRecording(createRecording(fileName = "mid.mp4", createdAt = 2000L))

        val latest = dao.getLatestRecording()
        assertNotNull(latest)
        assertEquals("newest.mp4", latest!!.fileName)
    }

    @Test
    fun `getLatestRecording returns null when empty`() = runTest {
        assertNull(dao.getLatestRecording())
    }

    @Test
    fun `updateRecording modifies existing recording`() = runTest {
        val id = dao.insertRecording(createRecording(fileName = "original.mp4"))
        val inserted = dao.getRecordingById(id)!!
        val updated = inserted.copy(fileName = "renamed.mp4", duration = 120000L)
        dao.updateRecording(updated)

        val result = dao.getRecordingById(id)
        assertEquals("renamed.mp4", result!!.fileName)
        assertEquals(120000L, result.duration)
    }

    @Test
    fun `deleteRecording removes recording`() = runTest {
        val recording = createRecording(fileName = "delete_me.mp4")
        val id = dao.insertRecording(recording)
        val inserted = dao.getRecordingById(id)!!
        dao.deleteRecording(inserted)

        assertNull(dao.getRecordingById(id))
    }

    @Test
    fun `deleteRecordingById removes recording by id`() = runTest {
        val id = dao.insertRecording(createRecording())
        assertNotNull(dao.getRecordingById(id))
        dao.deleteRecordingById(id)
        assertNull(dao.getRecordingById(id))
    }

    @Test
    fun `getRecordingCount emits correct count`() = runTest {
        dao.getRecordingCount().test {
            assertEquals(0, awaitItem())

            dao.insertRecording(createRecording(fileName = "a.mp4"))
            assertEquals(1, awaitItem())

            dao.insertRecording(createRecording(fileName = "b.mp4"))
            assertEquals(2, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTotalStorageUsed emits sum of file sizes`() = runTest {
        dao.getTotalStorageUsed().test {
            // null when no recordings
            assertNull(awaitItem())

            dao.insertRecording(createRecording(fileName = "a.mp4", fileSize = 1000L))
            assertEquals(1000L, awaitItem())

            dao.insertRecording(createRecording(fileName = "b.mp4", fileSize = 2000L))
            assertEquals(3000L, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTotalRecordingTime emits sum of durations`() = runTest {
        dao.getTotalRecordingTime().test {
            assertNull(awaitItem())

            dao.insertRecording(createRecording(fileName = "a.mp4", duration = 10000L))
            assertEquals(10000L, awaitItem())

            dao.insertRecording(createRecording(fileName = "b.mp4", duration = 20000L))
            assertEquals(30000L, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updateFavoriteStatus toggles favorite flag`() = runTest {
        val id = dao.insertRecording(createRecording(isFavorite = false))
        dao.updateFavoriteStatus(id, true)

        val result = dao.getRecordingById(id)
        assertTrue(result!!.isFavorite)
    }

    @Test
    fun `updateFavoriteStatus can unfavorite`() = runTest {
        val id = dao.insertRecording(createRecording(isFavorite = true))
        dao.updateFavoriteStatus(id, false)

        val result = dao.getRecordingById(id)
        assertEquals(false, result!!.isFavorite)
    }

    @Test
    fun `insertRecording with REPLACE strategy updates on conflict`() = runTest {
        val id = dao.insertRecording(createRecording(fileName = "original.mp4"))
        val original = dao.getRecordingById(id)!!

        val replacement = original.copy(fileName = "replaced.mp4", fileSize = 999L)
        dao.insertRecording(replacement)

        val result = dao.getRecordingById(id)
        assertEquals("replaced.mp4", result!!.fileName)
        assertEquals(999L, result.fileSize)
    }

    @Test
    fun `recording fields persist correctly`() = runTest {
        val recording = Recording(
            fileName = "full_test.mp4",
            filePath = "/storage/full_test.mp4",
            duration = 120000L,
            fileSize = 5000000L,
            width = 2560,
            height = 1440,
            frameRate = 60,
            bitRate = 16_000_000,
            hasAudio = true,
            hasMicrophone = true,
            thumbnailPath = "/thumbs/full_test.jpg",
            createdAt = 1234567890L,
            isFavorite = true,
            tags = "important test"
        )
        val id = dao.insertRecording(recording)
        val result = dao.getRecordingById(id)!!

        assertEquals("full_test.mp4", result.fileName)
        assertEquals("/storage/full_test.mp4", result.filePath)
        assertEquals(120000L, result.duration)
        assertEquals(5000000L, result.fileSize)
        assertEquals(2560, result.width)
        assertEquals(1440, result.height)
        assertEquals(60, result.frameRate)
        assertEquals(16_000_000, result.bitRate)
        assertTrue(result.hasAudio)
        assertTrue(result.hasMicrophone)
        assertEquals("/thumbs/full_test.jpg", result.thumbnailPath)
        assertEquals(1234567890L, result.createdAt)
        assertTrue(result.isFavorite)
        assertEquals("important test", result.tags)
    }
}
