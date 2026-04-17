package com.example.imsafe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imsafe.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

data class Contact(val name: String, val phone: String)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private val emergencyContacts = mutableListOf<Contact>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var checkinHandler = Handler(Looper.getMainLooper())
    private lateinit var checkinRunnable: Runnable
    private var timerDuration = 10 * 60 * 1000L // default 10 min
    private lateinit var sensorManager: SensorManager
    private var accel: Float = 0f
    private var accelCurrent: Float = 0f
    private var accelLast: Float = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()
        val intent = Intent(this, SosForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
        // RecyclerView setup
        recyclerView = binding.contactsRecyclerView
        contactAdapter = ContactAdapter(emergencyContacts) { saveContacts() }
        recyclerView.adapter = contactAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadContacts()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
        accel = 0.00f
        accelCurrent = SensorManager.GRAVITY_EARTH
        accelLast = SensorManager.GRAVITY_EARTH
        // Buttons
        binding.addContactButton.setOnClickListener { pickContact() }
        binding.sosButton.setOnClickListener { triggerSOS() }
        binding.shareLocationButton.setOnClickListener { shareLocationNTimes(2) }
        binding.recordButton.setOnClickListener { startVideoRecording() }

        // Safety check-in buttons
        binding.checkin10Button.setOnClickListener { setCheckinTimer(10) }
        binding.checkin15Button.setOnClickListener { setCheckinTimer(15) }
        binding.checkin20Button.setOnClickListener { setCheckinTimer(20) }
        binding.checkinButton.setOnClickListener { resetSafetyCheckinTimer() }
    }

    private fun setCheckinTimer(mins: Int) {
        timerDuration = mins * 60 * 1000L
        startSafetyCheckinTimer()
        Toast.makeText(this, "Timer set: $mins minutes", Toast.LENGTH_SHORT).show()
    }

    private fun startSafetyCheckinTimer() {
        checkinRunnable = Runnable {
            Toast.makeText(this, "Check-in missed! Triggering SOS...", Toast.LENGTH_LONG).show()
            triggerSOS()
        }
        checkinHandler.postDelayed(checkinRunnable, timerDuration)
    }

    private fun resetSafetyCheckinTimer() {
        checkinHandler.removeCallbacks(checkinRunnable)
        startSafetyCheckinTimer()
        Toast.makeText(this, "Check-in done! Timer reset.", Toast.LENGTH_SHORT).show()
    }
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorListener)
    }
    private var volumePressCount = 0
    private var volumePressHandler = Handler(Looper.getMainLooper())

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumePressCount++
            volumePressHandler.removeCallbacksAndMessages(null)
            volumePressHandler.postDelayed({
                if (volumePressCount >= 2) { // 2 presses triggers SOS
                    triggerSOS()
                }
                volumePressCount = 0
            }, 500) // 0.5s window
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    private fun startVideoRecording() {
        val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }
    // ---------------- PERMISSIONS ----------------
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    // ---------------- CONTACT PICKER ----------------
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val contactUri: Uri? = result.data?.data
            contactUri?.let { uri ->
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val name = if (nameIndex != -1) c.getString(nameIndex) else "Unknown"
                        val phone = if (phoneIndex != -1) c.getString(phoneIndex) ?: "" else ""
                        if (phone.isNotEmpty() && emergencyContacts.none { it.phone == phone }) {
                            emergencyContacts.add(Contact(name, phone))
                            saveContacts()
                            contactAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private fun pickContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Allow contacts permission", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun saveContacts() {
        val prefs = getSharedPreferences("EMERGENCY_CONTACTS", Context.MODE_PRIVATE)
        val set = emergencyContacts.map { "${it.name}|${it.phone}" }.toSet()
        prefs.edit().putStringSet("contacts", set).apply()
    }

    private fun loadContacts() {
        val prefs = getSharedPreferences("EMERGENCY_CONTACTS", Context.MODE_PRIVATE)
        emergencyContacts.clear()
        val saved = prefs.getStringSet("contacts", emptySet()) ?: emptySet()
        saved.forEach {
            val parts = it.split("|")
            if (parts.size == 2) emergencyContacts.add(Contact(parts[0], parts[1]))
        }
        contactAdapter.notifyDataSetChanged()
    }

    // ---------------- SOS Workflow ----------------
    private fun triggerSOS() {
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "Add at least one emergency contact!", Toast.LENGTH_SHORT).show()
            return
        }

        emergencyContacts.forEach { contact ->
            getCurrentLocation { locationUrl ->
                val message = "🚨 SOS! I need help. My location: $locationUrl"
                try { SmsManager.getDefault().sendTextMessage(contact.phone, null, message, null, null) }
                catch (e: Exception) { e.printStackTrace() }
            }
        }

        startVideoRecording()
        callContactsSequentially(emergencyContacts.map { it.phone })
    }

    private fun callContactsSequentially(contacts: List<String>, index: Int = 0) {
        if (index >= contacts.size) return
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:${contacts[index]}")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(callIntent)
        }
        Handler(Looper.getMainLooper()).postDelayed({ callContactsSequentially(contacts, index + 1) }, 5000)
    }

    private fun getCurrentLocation(callback: (String) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback("Location permission denied")
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            loc?.let {
                val locationUrl = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                callback(locationUrl)
            } ?: callback("Location unavailable")
        }
    }

    private fun shareLocationNTimes(times: Int) {
        var count = 0
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (count >= times) return
                getCurrentLocation { locationUrl ->
                    val message = "📍 Live location: $locationUrl"
                    emergencyContacts.forEach { phone ->
                        try { SmsManager.getDefault().sendTextMessage(phone.phone, null, message, null, null) }
                        catch (e: Exception) { e.printStackTrace() }
                    }
                }
                count++
                if (count < times) handler.postDelayed(this, 15000)
            }
        }
        handler.post(runnable)
    }


}