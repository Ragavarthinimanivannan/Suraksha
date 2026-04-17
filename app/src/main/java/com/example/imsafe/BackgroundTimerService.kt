package com.example.imsafe

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BackgroundTimerService : Service() {
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val CHANNEL_ID = "timer_background_channel"
    private val NOTIFICATION_ID = 104

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        startForegroundService()
        startContinuousMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safety Timer Background",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Suraksha")
            .setContentText("Safety monitoring active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startContinuousMonitoring() {
        runnable = object : Runnable {
            override fun run() {
                checkAllTimers()
                handler.postDelayed(this, 10000) // Check every 10 seconds
            }
        }
        handler.post(runnable)
    }

    private fun checkAllTimers() {
        val prefs = getSharedPreferences("CheckInPrefs", MODE_PRIVATE)
        val checkInId = prefs.getInt("check_in_id", -1)

        if (checkInId != -1) {
            val triggerTime = prefs.getLong("check_in_time", 0)
            val now = System.currentTimeMillis()

            if (now >= triggerTime) {
                // Timer expired - trigger safety check
                triggerSafetyCheck()
            }
        }
    }

    private fun triggerSafetyCheck() {
        // Send broadcast to show notification
        val intent = Intent(this, CheckInReceiver::class.java)
        sendBroadcast(intent)

        // Stop checking for this timer
        val prefs = getSharedPreferences("CheckInPrefs", MODE_PRIVATE)
        prefs.edit().remove("check_in_id").remove("check_in_time").apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}