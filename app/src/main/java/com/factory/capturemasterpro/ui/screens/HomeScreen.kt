package com.factory.capturemasterpro.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.capturemasterpro.CaptureMasterApp
import com.factory.capturemasterpro.ui.theme.RecordingRed
import com.factory.capturemasterpro.viewmodel.HomeViewModel
import com.factory.capturemasterpro.viewmodel.RecordingState
import com.factory.capturemasterpro.viewmodel.Resolution
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    isPremium: Boolean = false,
    onNavigateToGallery: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToPaywall: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CaptureMasterApp
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.repository)
    )

    val recordingState by viewModel.recordingState.collectAsState()
    val config by viewModel.recordingConfig.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val recordingCount by viewModel.recordingCount.collectAsState()
    val totalStorage by viewModel.totalStorageUsed.collectAsState()
    val recentRecordings by viewModel.recentRecordings.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val notificationPermissionState = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRecording(context, result.resultCode, result.data!!)
        } else {
            Toast.makeText(context, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CaptureMaster",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pro",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGallery) {
                        Icon(
                            imageVector = Icons.Outlined.VideoLibrary,
                            contentDescription = "Open video gallery"
                        )
                    }
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Recordings",
                    value = "$recordingCount",
                    icon = Icons.Filled.Movie
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Storage",
                    value = viewModel.formatFileSize(totalStorage),
                    icon = Icons.Filled.Videocam
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Record button
            RecordButton(
                recordingState = recordingState,
                elapsedTime = viewModel.formatTime(elapsedTime),
                onStartRecord = {
                    if (!notificationPermissionState.status.isGranted) {
                        notificationPermissionState.launchPermissionRequest()
                    }
                    if (config.microphoneEnabled && !audioPermissionState.status.isGranted) {
                        audioPermissionState.launchPermissionRequest()
                    } else {
                        val projectionManager = context.getSystemService(
                            Context.MEDIA_PROJECTION_SERVICE
                        ) as MediaProjectionManager
                        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                    }
                },
                onStopRecord = {
                    viewModel.stopRecording(context)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick settings
            AnimatedVisibility(visible = recordingState == RecordingState.IDLE) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Resolution",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Resolution.entries.forEach { resolution ->
                            val requiresPro = resolution.name == "HD_1080" || resolution.name == "QHD_1440"
                            val locked = requiresPro && !isPremium

                            FilterChip(
                                selected = config.resolution == resolution,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (locked) onNavigateToPaywall()
                                    else viewModel.updateResolution(resolution)
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(resolution.label, style = MaterialTheme.typography.labelSmall)
                                        if (locked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Filled.Lock,
                                                contentDescription = "Premium feature",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = if (locked) "${resolution.label}, premium feature"
                                    else "${resolution.label}${if (config.resolution == resolution) ", selected" else ""}"
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Frame Rate",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(24, 30, 60).forEach { fps ->
                            val locked = fps == 60 && !isPremium

                            FilterChip(
                                selected = config.frameRate == fps,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (locked) onNavigateToPaywall()
                                    else viewModel.updateFrameRate(fps)
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${fps} FPS")
                                        if (locked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Filled.Lock,
                                                contentDescription = "Premium feature",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (!isPremium) onNavigateToPaywall()
                                    else viewModel.toggleAudio()
                                }
                                .semantics {
                                    contentDescription = if (!isPremium) "Internal Audio, premium feature"
                                    else "Internal Audio, ${if (config.audioEnabled) "enabled" else "disabled"}"
                                    role = Role.Switch
                                },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (config.audioEnabled && isPremium)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (config.audioEnabled) Icons.Filled.MusicNote
                                    else Icons.Filled.MusicOff,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Internal Audio",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = if (!isPremium) "Pro" else if (config.audioEnabled) "On" else "Off",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!isPremium) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = "Pro",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (!isPremium) onNavigateToPaywall()
                                    else viewModel.toggleMicrophone()
                                }
                                .semantics {
                                    contentDescription = if (!isPremium) "Microphone, premium feature"
                                    else "Microphone, ${if (config.microphoneEnabled) "enabled" else "disabled"}"
                                    role = Role.Switch
                                },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (config.microphoneEnabled && isPremium)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (config.microphoneEnabled) Icons.Filled.Mic
                                    else Icons.Filled.MicOff,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Microphone",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = if (!isPremium) "Pro" else if (config.microphoneEnabled) "On" else "Off",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!isPremium) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = "Pro",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent recordings
            if (recentRecordings.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Recordings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToGallery() }
                            .semantics { contentDescription = "See all recordings" }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = recentRecordings.take(10),
                        key = { it.id }
                    ) { recording ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onNavigateToEditor(recording.id) }
                                .semantics { contentDescription = "Recording: ${recording.fileName}" },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(java.io.File(recording.filePath))
                                            .videoFrameMillis(1000)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = recording.fileName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = recording.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = viewModel.formatTime(recording.duration / 1000),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = viewModel.formatFileSize(recording.fileSize),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordButton(
    recordingState: RecordingState,
    elapsedTime: String,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .then(
                    if (recordingState == RecordingState.RECORDING)
                        Modifier.scale(pulseScale)
                    else Modifier
                )
                .clip(CircleShape)
                .background(
                    when (recordingState) {
                        RecordingState.IDLE -> RecordingRed
                        RecordingState.RECORDING -> RecordingRed
                        RecordingState.PREPARING -> MaterialTheme.colorScheme.tertiary
                        RecordingState.STOPPING -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .semantics {
                    role = Role.Button
                    contentDescription = when (recordingState) {
                        RecordingState.IDLE -> "Start recording"
                        RecordingState.RECORDING -> "Stop recording, elapsed time $elapsedTime"
                        RecordingState.PREPARING -> "Preparing to record"
                        RecordingState.STOPPING -> "Saving recording"
                    }
                }
                .clickable(
                    enabled = recordingState == RecordingState.IDLE || recordingState == RecordingState.RECORDING
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (recordingState) {
                        RecordingState.IDLE -> onStartRecord()
                        RecordingState.RECORDING -> onStopRecord()
                        RecordingState.PREPARING,
                        RecordingState.STOPPING -> Unit
                    }
                }
        ) {
            AnimatedContent(
                targetState = recordingState,
                transitionSpec = {
                    (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "record_icon"
            ) { state ->
                when (state) {
                    RecordingState.IDLE -> Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = "Start Recording",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    RecordingState.RECORDING -> Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop Recording",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    RecordingState.PREPARING -> Text(
                        text = "...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    RecordingState.STOPPING -> Text(
                        text = "...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = recordingState,
            label = "status_text"
        ) { state ->
            when (state) {
                RecordingState.IDLE -> Text(
                    text = "Tap to Record",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RecordingState.RECORDING -> Text(
                    text = elapsedTime,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = RecordingRed
                )
                RecordingState.PREPARING -> Text(
                    text = "Preparing...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                RecordingState.STOPPING -> Text(
                    text = "Saving...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ElevatedCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$title: $value"
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
