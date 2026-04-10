package com.factory.capturemasterpro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.factory.capturemasterpro.MainActivity
import com.factory.capturemasterpro.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFilePath: String = ""
    private var startTime: Long = 0L

    private var recordWidth = 1920
    private var recordHeight = 1080
    private var recordDpi = 1
    private var recordBitRate = 8_000_000
    private var recordFrameRate = 30
    private var recordAudio = true
    private var recordMicrophone = false

    companion object {
        private const val TAG = "ScreenRecordService"
        const val CHANNEL_ID = "screen_record_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.factory.capturemasterpro.ACTION_START"
        const val ACTION_STOP = "com.factory.capturemasterpro.ACTION_STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_DPI = "dpi"
        const val EXTRA_BIT_RATE = "bit_rate"
        const val EXTRA_FRAME_RATE = "frame_rate"
        const val EXTRA_AUDIO = "audio"
        const val EXTRA_MICROPHONE = "microphone"

        var onRecordingStarted: (() -> Unit)? = null
        var onRecordingStopped: ((filePath: String, duration: Long) -> Unit)? = null
        var onRecordingError: ((error: String) -> Unit)? = null
        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

                recordWidth = intent.getIntExtra(EXTRA_WIDTH, 1920)
                recordHeight = intent.getIntExtra(EXTRA_HEIGHT, 1080)
                recordDpi = intent.getIntExtra(EXTRA_DPI, 1)
                recordBitRate = intent.getIntExtra(EXTRA_BIT_RATE, 8_000_000)
                recordFrameRate = intent.getIntExtra(EXTRA_FRAME_RATE, 30)
                recordAudio = intent.getBooleanExtra(EXTRA_AUDIO, true)
                recordMicrophone = intent.getBooleanExtra(EXTRA_MICROPHONE, false)

                if (resultData != null) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    startRecording(resultCode, resultData)
                }
            }
            ACTION_STOP -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                Log.w(TAG, "Received unknown action: ${intent?.action}")
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification channel for screen recording"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunchIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(getString(R.string.recording_notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingLaunchIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.stop_recording),
                pendingStopIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startRecording(resultCode: Int, resultData: Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (mediaProjection == null) {
                onRecordingError?.invoke("Failed to obtain media projection")
                stopSelf()
                return
            }

            setupMediaRecorder()
            createVirtualDisplay()

            mediaRecorder?.start()
            isRecording = true
            isServiceRunning = true
            startTime = System.currentTimeMillis()
            onRecordingStarted?.invoke()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            onRecordingError?.invoke("Failed to start recording: ${e.message}")
            cleanup()
            stopSelf()
        }
    }

    private fun setupMediaRecorder() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "CM_${timestamp}.mp4"

        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val appDir = File(moviesDir, "CaptureMasterPro")
        if (!appDir.exists()) appDir.mkdirs()

        outputFilePath = File(appDir, fileName).absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            if (recordMicrophone) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (recordMicrophone) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
            }
            setVideoSize(recordWidth, recordHeight)
            setVideoFrameRate(recordFrameRate)
            setVideoEncodingBitRate(recordBitRate)
            setOutputFile(outputFilePath)
            prepare()
        }
    }

    private fun createVirtualDisplay() {
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecordDisplay",
            recordWidth,
            recordHeight,
            recordDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )
    }

    private fun stopRecording() {
        if (!isRecording) return

        val duration = System.currentTimeMillis() - startTime

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        }

        cleanup()
        isRecording = false
        isServiceRunning = false

        saveToMediaStore()

        onRecordingStopped?.invoke(outputFilePath, duration)
    }

    private fun saveToMediaStore() {
        val file = File(outputFilePath)
        if (!file.exists()) return

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CaptureMasterPro")
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }

        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    override fun onDestroy() {
        cleanup()
        isServiceRunning = false
        super.onDestroy()
    }
}
