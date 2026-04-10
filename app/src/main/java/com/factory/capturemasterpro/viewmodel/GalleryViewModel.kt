package com.factory.capturemasterpro.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.capturemasterpro.data.db.Recording
import com.factory.capturemasterpro.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class SortOrder {
    DATE_DESC, DATE_ASC, SIZE_DESC, SIZE_ASC, DURATION_DESC, NAME_ASC
}

class GalleryViewModel(private val repository: RecordingRepository) : ViewModel() {

    val allRecordings = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteRecordings = repository.favoriteRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _selectedRecordings = MutableStateFlow<Set<Long>>(emptySet())
    val selectedRecordings: StateFlow<Set<Long>> = _selectedRecordings.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(recording: Recording) {
        viewModelScope.launch {
            repository.toggleFavorite(recording.id, !recording.isFavorite)
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            val file = File(recording.filePath)
            if (file.exists()) file.delete()
            recording.thumbnailPath?.let { path ->
                val thumbFile = File(path)
                if (thumbFile.exists()) thumbFile.delete()
            }
            repository.deleteRecording(recording)
        }
    }

    fun deleteSelectedRecordings(recordings: List<Recording>) {
        viewModelScope.launch {
            val selected = _selectedRecordings.value
            recordings.filter { it.id in selected }.forEach { recording ->
                val file = File(recording.filePath)
                if (file.exists()) file.delete()
                repository.deleteRecording(recording)
            }
            clearSelection()
        }
    }

    fun toggleSelection(recordingId: Long) {
        val current = _selectedRecordings.value.toMutableSet()
        if (current.contains(recordingId)) {
            current.remove(recordingId)
        } else {
            current.add(recordingId)
        }
        _selectedRecordings.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun clearSelection() {
        _selectedRecordings.value = emptySet()
        _isSelectionMode.value = false
    }

    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    fun shareRecording(context: Context, recording: Recording) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
    }

    fun sortRecordings(recordings: List<Recording>): List<Recording> {
        return when (_sortOrder.value) {
            SortOrder.DATE_DESC -> recordings.sortedByDescending { it.createdAt }
            SortOrder.DATE_ASC -> recordings.sortedBy { it.createdAt }
            SortOrder.SIZE_DESC -> recordings.sortedByDescending { it.fileSize }
            SortOrder.SIZE_ASC -> recordings.sortedBy { it.fileSize }
            SortOrder.DURATION_DESC -> recordings.sortedByDescending { it.duration }
            SortOrder.NAME_ASC -> recordings.sortedBy { it.fileName }
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes == 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format("%.1f %s", size, units[unitIndex])
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    class Factory(private val repository: RecordingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GalleryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
