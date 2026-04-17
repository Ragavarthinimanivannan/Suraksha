package com.example.imsafe

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "suraksha_safety_channel"
    private const val NOTIFICATION_ID = 102

    fun createSafetyCheckNotification(context: Context) {
        createNotificationChannel(context)

        // Safe button intent
        val safeIntent = Intent(context, SafeActionReceiver::class.java)
        val safePendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            safeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // SOS button intent
        val sosIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("TRIGGER_SOS", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val sosPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt() + 1,
            sosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🛡️ Suraksha - Safety Check")
            .setContentText("Your check-in time has expired. Are you safe?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(1000, 500, 1000, 500))
            .setLights(Color.RED, 1000, 1000)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "✅ I'm Safe",
                safePendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "🚨 SOS",
                sosPendingIntent
            )
            .build()

        // Show notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, notification)
        }

        Log.d("NOTIFICATION", "Safety check notification shown")

        // Vibrate
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(2000,
                android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(2000)
        }
    }

    fun cancelSafetyNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Suraksha Safety Alerts"
            val description = "Notifications for safety check-ins and emergencies"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            channel.enableLights(true)
            channel.lightColor = Color.RED

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}