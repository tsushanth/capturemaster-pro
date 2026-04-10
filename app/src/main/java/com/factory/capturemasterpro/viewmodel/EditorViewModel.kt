package com.factory.capturemasterpro.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.capturemasterpro.data.db.Recording
import com.factory.capturemasterpro.data.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditorState(
    val recording: Recording? = null,
    val isLoading: Boolean = true,
    val videoDurationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class EditorViewModel(private val repository: RecordingRepository) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    fun loadRecording(recordingId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val recording = repository.getRecordingById(recordingId)
            if (recording != null) {
                val duration = getVideoDuration(recording.filePath)
                _state.value = _state.value.copy(
                    recording = recording,
                    isLoading = false,
                    videoDurationMs = duration,
                    trimStartMs = 0L,
                    trimEndMs = duration
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Recording not found"
                )
            }
        }
    }

    fun setTrimStart(ms: Long) {
        val end = _state.value.trimEndMs
        if (ms < end) {
            _state.value = _state.value.copy(trimStartMs = ms)
        }
    }

    fun setTrimEnd(ms: Long) {
        val start = _state.value.trimStartMs
        if (ms > start) {
            _state.value = _state.value.copy(trimEndMs = ms)
        }
    }

    fun setPlaying(playing: Boolean) {
        _state.value = _state.value.copy(isPlaying = playing)
    }

    fun updatePosition(positionMs: Long) {
        _state.value = _state.value.copy(currentPositionMs = positionMs)
    }

    fun trimVideo(context: Context) {
        val recording = _state.value.recording ?: return
        val startMs = _state.value.trimStartMs
        val endMs = _state.value.trimEndMs

        if (startMs == 0L && endMs == _state.value.videoDurationMs) {
            _state.value = _state.value.copy(successMessage = "No trimming needed - full video selected")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true, processingProgress = 0f)

            try {
                val outputPath = withContext(Dispatchers.IO) {
                    performTrim(recording.filePath, startMs, endMs)
                }

                if (outputPath != null) {
                    val outputFile = File(outputPath)
                    val newRecording = recording.copy(
                        id = 0,
                        fileName = outputFile.name,
                        filePath = outputPath,
                        duration = endMs - startMs,
                        fileSize = outputFile.length(),
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertRecording(newRecording)

                    _state.value = _state.value.copy(
                        isProcessing = false,
                        processingProgress = 1f,
                        successMessage = "Video trimmed successfully"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    errorMessage = "Trim failed: ${e.message}"
                )
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun performTrim(inputPath: String, startUs: Long, endUs: Long): String? {
        val startMicro = startUs * 1000
        val endMicro = endUs * 1000

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "CaptureMasterPro"
        )
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputPath = File(outputDir, "CM_trimmed_${timestamp}.mp4").absolutePath

        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val trackIndexMap = mutableMapOf<Int, Int>()

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val muxerTrackIndex = muxer.addTrack(format)
            trackIndexMap[i] = muxerTrackIndex
        }

        muxer.start()

        val buffer = ByteBuffer.allocate(1024 * 1024)
        val bufferInfo = android.media.MediaCodec.BufferInfo()

        for (i in 0 until extractor.trackCount) {
            extractor.selectTrack(i)
            extractor.seekTo(startMicro, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endMicro) break

                if (sampleTime >= startMicro) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTime - startMicro
                    bufferInfo.flags = extractor.sampleFlags

                    val muxerTrackIndex = trackIndexMap[i] ?: continue
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                    val progress = ((sampleTime - startMicro).toFloat() / (endMicro - startMicro)).coerceIn(0f, 1f)
                    _state.value = _state.value.copy(processingProgress = progress)
                }

                extractor.advance()
            }

            extractor.unselectTrack(i)
        }

        muxer.stop()
        muxer.release()
        extractor.release()

        return outputPath
    }

    private suspend fun getVideoDuration(filePath: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                retriever.release()
                duration
            } catch (e: Exception) {
                Log.e("EditorViewModel", "Failed to retrieve video duration for: $filePath", e)
                0L
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = (ms % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, millis)
    }

    class Factory(private val repository: RecordingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EditorViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
