package com.example.imsafe

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class SosForegroundService : Service(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var accel = 0f
    private var accelCurrent = 0f
    private var accelLast = 0f
    private val emergencyContacts = mutableListOf<Contact>()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Shake sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)

        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "SOS_SERVICE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SOS Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ImSafe SOS Active")
            .setContentText("Monitoring emergency triggers...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your app icon
            .build()

        startForeground(1, notification)
    }

    // Shake detection
    override fun onSensorChanged(event: android.hardware.SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        accelLast = accelCurrent
        accelCurrent = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()
        val delta = accelCurrent - accelLast
        accel = accel * 0.9f + delta

        if (accel > 15) { // adjust sensitivity
            triggerSOS()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerSOS() {
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts!", Toast.LENGTH_SHORT).show()
            return
        }

        emergencyContacts.forEach { contact ->
            getCurrentLocation { locationUrl ->
                val message = "🚨 SOS! I need help. My location: $locationUrl"
                try { SmsManager.getDefault().sendTextMessage(contact.phone, null, message, null, null) }
                catch (e: Exception) { e.printStackTrace() }
            }
        }

        // Optionally trigger calls here (with permissions)
    }

    private fun getCurrentLocation(callback: (String) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            loc?.let {
                val locationUrl = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                callback(locationUrl)
            } ?: callback("Location unavailable")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}