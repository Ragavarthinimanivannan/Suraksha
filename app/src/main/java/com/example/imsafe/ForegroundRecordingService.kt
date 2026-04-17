package com.example.imsafe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ForegroundRecordingService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val CHANNEL_ID = "RecordingChannel"
    private val NOTIFICATION_ID = 101

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start recording
        startRecording()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recording emergency video in background"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ I'm Safe - Recording")
            .setContentText("Emergency video recording in progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startRecording() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(getExternalFilesDir(null), "emergency_${timestamp}.mp4")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(1280, 720)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(8000000)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
            }

            Log.d("RECORDING", "📹 Recording started: ${outputFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("RECORDING", "Failed to start recording: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            Log.d("RECORDING", "📹 Recording stopped")

            // Save file path to SharedPreferences for later access
            val prefs = getSharedPreferences("RecordingPrefs", MODE_PRIVATE)
            prefs.edit().putString("last_recording", outputFile?.absolutePath).apply()

        } catch (e: Exception) {
            Log.e("RECORDING", "Error stopping recording: ${e.message}")
        }
    }
}