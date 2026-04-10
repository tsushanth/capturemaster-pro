package com.factory.capturemasterpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.capturemasterpro.CaptureMasterApp
import com.factory.capturemasterpro.ui.theme.GoldAccent
import com.factory.capturemasterpro.viewmodel.Resolution
import com.factory.capturemasterpro.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isPremium: Boolean = false,
    onNavigateToPaywall: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CaptureMasterApp
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.repository, context)
    )

    val settings by viewModel.settings.collectAsState()
    val recordingCount by viewModel.recordingCount.collectAsState()
    val totalStorage by viewModel.totalStorageUsed.collectAsState()
    val totalTime by viewModel.totalRecordingTime.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Upgrade to Pro card (only show if not premium)
            if (!isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPaywall() }
                        .semantics { contentDescription = "Upgrade to Pro, unlock all premium features" },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upgrade to Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Unlock all premium features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        ProBadge()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recording Settings Section
            SectionHeader(title = "Recording")

            SettingsCard {
                // Resolution
                var showResolutionOptions by remember { mutableStateOf(false) }

                SettingsItem(
                    icon = Icons.Filled.HighQuality,
                    title = "Resolution",
                    subtitle = Resolution.entries.find { it.name == settings.resolution }?.label
                        ?: "1080p Full HD",
                    onClick = { showResolutionOptions = !showResolutionOptions }
                )

                if (showResolutionOptions) {
                    Column(
                        modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                    ) {
                        Resolution.entries.forEach { resolution ->
                            val requiresPremium = resolution.name == "QHD_1440" || resolution.name == "HD_1080"
                            val locked = requiresPremium && !isPremium

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (locked) {
                                            onNavigateToPaywall()
                                        } else {
                                            viewModel.updateResolution(resolution.name)
                                            showResolutionOptions = false
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.resolution == resolution.name,
                                    onClick = {
                                        if (locked) {
                                            onNavigateToPaywall()
                                        } else {
                                            viewModel.updateResolution(resolution.name)
                                            showResolutionOptions = false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = resolution.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (locked) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ProBadge()
                                }
                            }
                        }
                    }
                }

                // Frame Rate
                var showFrameRateOptions by remember { mutableStateOf(false) }

                SettingsItem(
                    icon = Icons.Filled.Speed,
                    title = "Frame Rate",
                    subtitle = "${settings.frameRate} FPS",
                    onClick = { showFrameRateOptions = !showFrameRateOptions }
                )

                if (showFrameRateOptions) {
                    Column(
                        modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                    ) {
                        listOf(24, 30, 60).forEach { fps ->
                            val locked = fps == 60 && !isPremium

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (locked) {
                                            onNavigateToPaywall()
                                        } else {
                                            viewModel.updateFrameRate(fps)
                                            showFrameRateOptions = false
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.frameRate == fps,
                                    onClick = {
                                        if (locked) {
                                            onNavigateToPaywall()
                                        } else {
                                            viewModel.updateFrameRate(fps)
                                            showFrameRateOptions = false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$fps FPS",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (locked) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ProBadge()
                                }
                            }
                        }
                    }
                }

                // Bit Rate
                var showBitRateOptions by remember { mutableStateOf(false) }

                SettingsItem(
                    icon = Icons.Filled.DataUsage,
                    title = "Video Quality",
                    subtitle = when (settings.bitRate) {
                        4_000_000 -> "Standard (4 Mbps)"
                        8_000_000 -> "High (8 Mbps)"
                        16_000_000 -> "Ultra (16 Mbps)"
                        else -> "${settings.bitRate / 1_000_000} Mbps"
                    },
                    onClick = { showBitRateOptions = !showBitRateOptions }
                )

                if (showBitRateOptions) {
                    Column(
                        modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                    ) {
                        listOf(
                            4_000_000 to "Standard (4 Mbps)",
                            8_000_000 to "High (8 Mbps)",
                            16_000_000 to "Ultra (16 Mbps)"
                        ).forEach { (bitRate, label) ->
                            val locked = bitRate == 16_000_000 && !isPremium

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (locked) {
                                            onNavigateToPaywall()
                                        } else {
                                            viewModel.updateBitRate(bitRate)
                                            showBitRateOptions = false
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.bitRate == bitRate,
                                    onClick = {
                                        if (locked) {
                                            onNavigateToPaywall()
                                        } else {
                                            viewModel.updateBitRate(bitRate)
                                            showBitRateOptions = false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (locked) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ProBadge()
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Settings
            SectionHeader(title = "Audio")

            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Filled.MusicNote,
                    title = "Internal Audio",
                    subtitle = "Record game and app sounds",
                    checked = settings.audioEnabled,
                    isPremiumFeature = true,
                    isPremium = isPremium,
                    onCheckedChange = {
                        if (!isPremium) onNavigateToPaywall() else viewModel.toggleAudio()
                    }
                )

                SettingsToggleItem(
                    icon = Icons.Filled.Mic,
                    title = "Microphone",
                    subtitle = "Record voice commentary",
                    checked = settings.microphoneEnabled,
                    isPremiumFeature = true,
                    isPremium = isPremium,
                    onCheckedChange = {
                        if (!isPremium) onNavigateToPaywall() else viewModel.toggleMicrophone()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Behavior Settings
            SectionHeader(title = "Behavior")

            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Filled.Timer,
                    title = "Countdown Timer",
                    subtitle = "${settings.countdownSeconds} second countdown before recording",
                    checked = settings.showCountdown,
                    onCheckedChange = { viewModel.toggleCountdown() }
                )

                SettingsToggleItem(
                    icon = Icons.Filled.TouchApp,
                    title = "Show Touches",
                    subtitle = "Display touch indicators while recording",
                    checked = settings.showTouches,
                    isPremiumFeature = true,
                    isPremium = isPremium,
                    onCheckedChange = {
                        if (!isPremium) onNavigateToPaywall() else viewModel.toggleShowTouches()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance
            SectionHeader(title = "Appearance")

            SettingsCard {
                var showThemeOptions by remember { mutableStateOf(false) }

                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = "Theme",
                    subtitle = when (settings.darkMode) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System Default"
                    },
                    onClick = { showThemeOptions = !showThemeOptions }
                )

                if (showThemeOptions) {
                    Column(
                        modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                    ) {
                        listOf(
                            "system" to "System Default",
                            "light" to "Light",
                            "dark" to "Dark"
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setDarkMode(mode)
                                        showThemeOptions = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.darkMode == mode,
                                    onClick = {
                                        viewModel.setDarkMode(mode)
                                        showThemeOptions = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                SettingsToggleItem(
                    icon = Icons.Filled.Palette,
                    title = "Dynamic Colors",
                    subtitle = "Use Material You colors from your wallpaper",
                    checked = settings.dynamicColors,
                    onCheckedChange = { viewModel.toggleDynamicColors() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics
            SectionHeader(title = "Statistics")

            SettingsCard {
                SettingsItem(
                    icon = Icons.Filled.Movie,
                    title = "Total Recordings",
                    subtitle = "$recordingCount recordings"
                )

                SettingsItem(
                    icon = Icons.Filled.DataUsage,
                    title = "Storage Used",
                    subtitle = viewModel.formatFileSize(totalStorage)
                )

                SettingsItem(
                    icon = Icons.Filled.Videocam,
                    title = "Total Recording Time",
                    subtitle = viewModel.formatDuration(totalTime)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About
            SectionHeader(title = "About")

            SettingsCard {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "CaptureMaster Pro",
                    subtitle = if (isPremium) "Version 1.0.0 - Premium" else "Version 1.0.0 - Free"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title: $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    isPremiumFeature: Boolean = false,
    isPremium: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, ${if (checked) "enabled" else "disabled"}" +
                        if (isPremiumFeature && !isPremium) ", premium feature" else ""
                role = Role.Switch
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (isPremiumFeature && !isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ProBadge()
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ProBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(GoldAccent)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "PRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface
        )
    }
}
