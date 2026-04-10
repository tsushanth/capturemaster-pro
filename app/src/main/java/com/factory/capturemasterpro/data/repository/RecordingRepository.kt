package com.factory.capturemasterpro.data.repository

import com.factory.capturemasterpro.data.db.Recording
import com.factory.capturemasterpro.data.db.RecordingDao
import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val recordingDao: RecordingDao) {

    val allRecordings: Flow<List<Recording>> = recordingDao.getAllRecordings()

    val favoriteRecordings: Flow<List<Recording>> = recordingDao.getFavoriteRecordings()

    val recordingCount: Flow<Int> = recordingDao.getRecordingCount()

    val totalStorageUsed: Flow<Long?> = recordingDao.getTotalStorageUsed()

    val totalRecordingTime: Flow<Long?> = recordingDao.getTotalRecordingTime()

    suspend fun getRecordingById(id: Long): Recording? {
        return recordingDao.getRecordingById(id)
    }

    suspend fun getLatestRecording(): Recording? {
        return recordingDao.getLatestRecording()
    }

    suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun updateRecording(recording: Recording) {
        recordingDao.updateRecording(recording)
    }

    suspend fun deleteRecording(recording: Recording) {
        recordingDao.deleteRecording(recording)
    }

    suspend fun deleteRecordingById(id: Long) {
        recordingDao.deleteRecordingById(id)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        recordingDao.updateFavoriteStatus(id, isFavorite)
    }
}
