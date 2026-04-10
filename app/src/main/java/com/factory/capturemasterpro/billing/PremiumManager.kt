package com.factory.capturemasterpro.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.factory.capturemasterpro.viewmodel.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PremiumManager(private val context: Context) {

    companion object {
        private val IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val ADS_REMOVED = booleanPreferencesKey("ads_removed")
    }

    val isPremium: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_PREMIUM] ?: false }

    val isAdsRemoved: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[ADS_REMOVED] ?: false }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_PREMIUM] = isPremium
        }
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ADS_REMOVED] = removed
        }
    }
}

enum class PremiumFeature(val displayName: String) {
    RESOLUTION_1080P("1080p Recording"),
    RESOLUTION_1440P("1440p Recording"),
    FPS_60("60 FPS Recording"),
    ULTRA_QUALITY("Ultra Quality (16 Mbps)"),
    VIDEO_TRIMMING("Video Trimming"),
    MICROPHONE("Microphone Recording"),
    SHOW_TOUCHES("Show Touches"),
    INTERNAL_AUDIO("Internal Audio Recording")
}
