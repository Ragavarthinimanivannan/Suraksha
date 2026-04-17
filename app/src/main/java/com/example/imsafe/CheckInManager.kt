package com.example.imsafe

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

class CheckInManager(private val context: Context) {
    private val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)!!
    private val prefs: SharedPreferences = context.getSharedPreferences("CheckInPrefs", Context.MODE_PRIVATE)
    private val CHECK_IN_ID_KEY = "check_in_id"
    private val CHECK_IN_TIME_KEY = "check_in_time"

    fun setCheckInTimer(minutes: Int) {
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val checkInId = System.currentTimeMillis().toInt()
        prefs.edit().putInt(CHECK_IN_ID_KEY, checkInId).apply()
        prefs.edit().putLong(CHECK_IN_TIME_KEY, triggerTime).apply()

        val intent = Intent(context, CheckInReceiver::class.java).apply {
            putExtra("check_in_id", checkInId)
            putExtra("trigger_time", triggerTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            checkInId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d("CHECKIN", "✅ Timer set for $minutes minutes")
        } catch (e: Exception) {
            Log.e("CHECKIN", "Error: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun checkIn() {
        val checkInId = prefs.getInt(CHECK_IN_ID_KEY, -1)
        if (checkInId != -1) {
            try {
                val intent = Intent(context, CheckInReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    checkInId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                Log.d("CHECKIN", "✅ Alarm cancelled")
            } catch (e: Exception) {
                Log.e("CHECKIN", "Error cancelling: ${e.message}")
            }
            prefs.edit().remove(CHECK_IN_ID_KEY).remove(CHECK_IN_TIME_KEY).apply()
        }
    }

    fun isCheckInActive(): Boolean {
        return prefs.contains(CHECK_IN_ID_KEY)
    }

    fun getRemainingTime(): Long {
        val triggerTime = prefs.getLong(CHECK_IN_TIME_KEY, 0)
        return maxOf(0, triggerTime - System.currentTimeMillis())
    }
}