package com.factory.capturemasterpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.factory.capturemasterpro.ui.navigation.AppNavigation
import com.factory.capturemasterpro.ui.theme.CaptureMasterProTheme
import com.factory.capturemasterpro.viewmodel.PreferenceKeys
import com.factory.capturemasterpro.viewmodel.dataStore
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkModePref by dataStore.data
                .map { it[PreferenceKeys.DARK_MODE] ?: "system" }
                .collectAsState(initial = "system")
            val dynamicColors by dataStore.data
                .map { it[PreferenceKeys.DYNAMIC_COLORS] ?: false }
                .collectAsState(initial = false)

            val isDarkTheme = when (darkModePref) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            CaptureMasterProTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
