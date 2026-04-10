package com.factory.capturemasterpro.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.capturemasterpro.data.db.Recording
import com.factory.capturemasterpro.data.repository.RecordingRepository
import com.factory.capturemasterpro.service.ScreenRecordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class RecordingConfig(
    val resolution: Resolution = Resolution.HD_1080,
    val frameRate: Int = 30,
    val bitRate: Int = 8_000_000,
    val audioEnabled: Boolean = true,
    val microphoneEnabled: Boolean = false
)

enum class Resolution(val width: Int, val height: Int, val label: String) {
    SD_480(854, 480, "480p SD"),
    HD_720(1280, 720, "720p HD"),
    HD_1080(1920, 1080, "1080p Full HD"),
    QHD_1440(2560, 1440, "1440p QHD")
}

enum class RecordingState {
    IDLE, PREPARING, RECORDING, STOPPING
}

class HomeViewModel(private val repository: RecordingRepository) : ViewModel() {

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingConfig = MutableStateFlow(RecordingConfig())
    val recordingConfig: StateFlow<RecordingConfig> = _recordingConfig.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val recordingCount = repository.recordingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStorageUsed = repository.totalStorageUsed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentRecordings = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        if (ScreenRecordService.isServiceRunning) {
            _recordingState.value = RecordingState.RECORDING
            startTimer()
        }

        ScreenRecordService.onRecordingStarted = {
            _recordingState.value = RecordingState.RECORDING
            startTimer()
        }

        ScreenRecordService.onRecordingStopped = { filePath, duration ->
            _recordingState.value = RecordingState.IDLE
            stopTimer()
            saveRecording(filePath, duration)
        }

        ScreenRecordService.onRecordingError = { error ->
            _recordingState.value = RecordingState.IDLE
            _errorMessage.value = error
            stopTimer()
        }
    }

    fun updateConfig(config: RecordingConfig) {
        _recordingConfig.value = config
    }

    fun updateResolution(resolution: Resolution) {
        _recordingConfig.value = _recordingConfig.value.copy(resolution = resolution)
    }

    fun updateFrameRate(frameRate: Int) {
        _recordingConfig.value = _recordingConfig.value.copy(frameRate = frameRate)
    }

    fun updateBitRate(bitRate: Int) {
        _recordingConfig.value = _recordingConfig.value.copy(bitRate = bitRate)
    }

    fun toggleAudio() {
        _recordingConfig.value = _recordingConfig.value.copy(
            audioEnabled = !_recordingConfig.value.audioEnabled
        )
    }

    fun toggleMicrophone() {
        _recordingConfig.value = _recordingConfig.value.copy(
            microphoneEnabled = !_recordingConfig.value.microphoneEnabled
        )
    }

    fun startRecording(context: Context, resultCode: Int, data: Intent) {
        _recordingState.value = RecordingState.PREPARING
        val config = _recordingConfig.value

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
            putExtra(ScreenRecordService.EXTRA_WIDTH, config.resolution.width)
            putExtra(ScreenRecordService.EXTRA_HEIGHT, config.resolution.height)
            putExtra(ScreenRecordService.EXTRA_DPI, metrics.densityDpi)
            putExtra(ScreenRecordService.EXTRA_BIT_RATE, config.bitRate)
            putExtra(ScreenRecordService.EXTRA_FRAME_RATE, config.frameRate)
            putExtra(ScreenRecordService.EXTRA_AUDIO, config.audioEnabled)
            putExtra(ScreenRecordService.EXTRA_MICROPHONE, config.microphoneEnabled)
        }

        context.startForegroundService(intent)
    }

    fun stopRecording(context: Context) {
        _recordingState.value = RecordingState.STOPPING
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startTimer() {
        _elapsedTime.value = 0L
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _elapsedTime.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _elapsedTime.value = 0L
    }

    private fun saveRecording(filePath: String, duration: Long) {
        viewModelScope.launch {
            val file = File(filePath)
            val config = _recordingConfig.value
            val recording = Recording(
                fileName = file.name,
                filePath = filePath,
                duration = duration,
                fileSize = if (file.exists()) file.length() else 0L,
                width = config.resolution.width,
                height = config.resolution.height,
                frameRate = config.frameRate,
                bitRate = config.bitRate,
                hasAudio = config.audioEnabled,
                hasMicrophone = config.microphoneEnabled
            )
            repository.insertRecording(recording)
        }
    }

    fun formatTime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    fun formatFileSize(bytes: Long?): String {
        if (bytes == null || bytes == 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format("%.1f %s", size, units[unitIndex])
    }

    class Factory(private val repository: RecordingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
