package com.example.imsafe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class SafeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SAFETY", "✅ User marked as safe")

        // Cancel the notification
        NotificationHelper.cancelSafetyNotification(context)

        // Clear check-in timer
        val checkInManager = CheckInManager(context)
        checkInManager.checkIn()

        // Clear expired flag
        val prefs = context.getSharedPreferences("CheckInPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("timer_expired", false).apply()
        prefs.edit().putBoolean("safety_responded", true).apply()

        // Show confirmation
        Toast.makeText(context, "✅ You're safe! Timer cleared.", Toast.LENGTH_LONG).show()
    }
}