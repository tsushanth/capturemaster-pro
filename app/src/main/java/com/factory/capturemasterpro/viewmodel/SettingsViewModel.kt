package com.factory.capturemasterpro.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.capturemasterpro.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceKeys {
    val RESOLUTION = stringPreferencesKey("resolution")
    val FRAME_RATE = intPreferencesKey("frame_rate")
    val BIT_RATE = intPreferencesKey("bit_rate")
    val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
    val MICROPHONE_ENABLED = booleanPreferencesKey("microphone_enabled")
    val SHOW_COUNTDOWN = booleanPreferencesKey("show_countdown")
    val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
    val SHOW_TOUCHES = booleanPreferencesKey("show_touches")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
}

data class AppSettings(
    val resolution: String = "HD_1080",
    val frameRate: Int = 30,
    val bitRate: Int = 8_000_000,
    val audioEnabled: Boolean = true,
    val microphoneEnabled: Boolean = false,
    val showCountdown: Boolean = true,
    val countdownSeconds: Int = 3,
    val showTouches: Boolean = false,
    val darkMode: String = "system",
    val dynamicColors: Boolean = false
)

class SettingsViewModel(
    private val repository: RecordingRepository,
    private val context: Context
) : ViewModel() {

    val settings: StateFlow<AppSettings> = context.dataStore.data
        .map { prefs ->
            AppSettings(
                resolution = prefs[PreferenceKeys.RESOLUTION] ?: "HD_1080",
                frameRate = prefs[PreferenceKeys.FRAME_RATE] ?: 30,
                bitRate = prefs[PreferenceKeys.BIT_RATE] ?: 8_000_000,
                audioEnabled = prefs[PreferenceKeys.AUDIO_ENABLED] ?: true,
                microphoneEnabled = prefs[PreferenceKeys.MICROPHONE_ENABLED] ?: false,
                showCountdown = prefs[PreferenceKeys.SHOW_COUNTDOWN] ?: true,
                countdownSeconds = prefs[PreferenceKeys.COUNTDOWN_SECONDS] ?: 3,
                showTouches = prefs[PreferenceKeys.SHOW_TOUCHES] ?: false,
                darkMode = prefs[PreferenceKeys.DARK_MODE] ?: "system",
                dynamicColors = prefs[PreferenceKeys.DYNAMIC_COLORS] ?: false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val recordingCount = repository.recordingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStorageUsed = repository.totalStorageUsed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalRecordingTime = repository.totalRecordingTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateResolution(resolution: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.RESOLUTION] = resolution
            }
        }
    }

    fun updateFrameRate(frameRate: Int) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.FRAME_RATE] = frameRate
            }
        }
    }

    fun updateBitRate(bitRate: Int) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.BIT_RATE] = bitRate
            }
        }
    }

    fun toggleAudio() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.AUDIO_ENABLED] = !(prefs[PreferenceKeys.AUDIO_ENABLED] ?: true)
            }
        }
    }

    fun toggleMicrophone() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.MICROPHONE_ENABLED] = !(prefs[PreferenceKeys.MICROPHONE_ENABLED] ?: false)
            }
        }
    }

    fun toggleCountdown() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.SHOW_COUNTDOWN] = !(prefs[PreferenceKeys.SHOW_COUNTDOWN] ?: true)
            }
        }
    }

    fun updateCountdownSeconds(seconds: Int) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.COUNTDOWN_SECONDS] = seconds
            }
        }
    }

    fun toggleShowTouches() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.SHOW_TOUCHES] = !(prefs[PreferenceKeys.SHOW_TOUCHES] ?: false)
            }
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.DARK_MODE] = mode
            }
        }
    }

    fun toggleDynamicColors() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.DYNAMIC_COLORS] = !(prefs[PreferenceKeys.DYNAMIC_COLORS] ?: false)
            }
        }
    }

    fun formatFileSize(bytes: Long?): String {
        if (bytes == null || bytes == 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format("%.1f %s", size, units[unitIndex])
    }

    fun formatDuration(millis: Long?): String {
        if (millis == null || millis == 0L) return "0s"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    class Factory(
        private val repository: RecordingRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
