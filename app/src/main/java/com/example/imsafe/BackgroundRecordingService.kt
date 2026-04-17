package com.example.imsafe

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackgroundRecordingService : Service() {
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var outputFile: File
    private var isRecording = false
    private var sosId: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sosId = intent?.getStringExtra("SOS_ID") ?: generateSOSId()
        startForegroundService()
        startRecording()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "recording_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Emergency Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🛡️ I'm Safe - Recording")
            .setContentText("Emergency recording in progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun startRecording() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(getExternalFilesDir(null), "emergency_${timestamp}_${sosId}.mp4")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(640, 480)
                setVideoFrameRate(30)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            Log.d("RECORDING", "📹 Background recording started: ${outputFile.absolutePath}")

            // Upload every 60 seconds
            startPeriodicUpload()

        } catch (e: Exception) {
            Log.e("RECORDING", "Failed to start recording: ${e.message}")
        }
    }

    private fun startPeriodicUpload() {
        val handler = Handler(Looper.getMainLooper())
        val uploadRunnable = object : Runnable {
            override fun run() {
                if (isRecording && outputFile.exists()) {
                    uploadToBackend()
                }
                handler.postDelayed(this, 60000) // Every 60 seconds
            }
        }
        handler.postDelayed(uploadRunnable, 60000)
    }

    private fun uploadToBackend() {
        // Upload file to your backend
        Log.d("RECORDING", "⬆️ Uploading recording: ${outputFile.name}")
        // Add your upload logic here
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder.stop()
                mediaRecorder.release()
                isRecording = false
                Log.d("RECORDING", "📹 Recording stopped")

                // Final upload
                uploadToBackend()
            } catch (e: Exception) {
                Log.e("RECORDING", "Error stopping recording: ${e.message}")
            }
        }
    }

    private fun generateSOSId(): String {
        return "SOS_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
    }
}