package com.example.imsafe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("CHECKIN", "⏰ Timer expired! Showing safety notification...")

        // Show safety check notification
        NotificationHelper.createSafetyCheckNotification(context)

        // Mark that timer has expired
        val prefs = context.getSharedPreferences("CheckInPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("timer_expired", true).apply()
        prefs.edit().putBoolean("safety_responded", false).apply()

        // Schedule auto-SOS in 60 seconds
        scheduleAutoSOS(context)
    }

    private fun scheduleAutoSOS(context: Context) {
        val handler = android.os.Handler(context.mainLooper)
        handler.postDelayed({
            val prefs = context.getSharedPreferences("CheckInPrefs", Context.MODE_PRIVATE)
            val responded = prefs.getBoolean("safety_responded", false)

            if (!responded) {
                Log.d("AUTO_SOS", "⏰ No response - triggering auto SOS")
                triggerAutoSOS(context)
            }
        }, 60000) // 60 seconds
    }

    private fun triggerAutoSOS(context: Context) {
        // Start MainActivity with auto-SOS flag
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("AUTO_SOS", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}