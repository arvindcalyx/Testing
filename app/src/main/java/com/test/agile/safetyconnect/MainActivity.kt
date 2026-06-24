package com.test.agile.safetyconnect

import SafetyConnectCommunicator
import SafetyConnectSDK
import SensorFilters
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.test.safetyconnect.model.SensorResponse

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val liveDataView = findViewById<TextView>(R.id.harshAcceleration)

        // Check and request permissions
        checkAndRequestPermissions()

        val startService = findViewById<Button>(R.id.startService)
        SafetyConnectSDK.sdkVersion
        startService.setOnClickListener {
            SafetyConnectSDK.initSDK(
                SensorFilters(
                    isFallCrashDetectionEnabled = true,
                    isSpeedDetectionEnabled = true,
                    isEMFDetectionEnabled = true,
                    maxEmfThreshold = 0f,
                    speedCallBackFrequency = 10*1000L,
                    maxSpeedThreshold = 80F,
                    harshDrivingDurationFrequency = 3000L,
                    harshDrivingCallbackFrequency= 1*1000L,
                    speedDetectionEdge = null,
                    harshDrivingCaptureEnabled = true,
                    harshDrivingEdge = "300",
                    harshDrivingThresholdInKm = 10.0f, safetyType = SafetyTypes.SPEED_DETECTION
                    , emfDetectionEdge = "", isEMFACDetectionEnabled = true, maxACEmfThreshold = 75f
                ), this, object : SafetyConnectCommunicator {
                    override fun onCrashFallDetected(
                        sensorResponse: SensorResponse,
                        crashDetectionEdge: String?
                    ) {
                        println("Ganesh onCrashFallDetected" + Thread.currentThread().name)
                    }

                    override fun locationPermissionNotGranted() {
                        println("Ganesh locationPermissionNotGranted" + Thread.currentThread().name)
                    }

                    override fun getFeedbackCallback(sensorResponse: SensorResponse) {
                        println("Ganesh getFeedbackCallback" + Thread.currentThread().name)
                    }

                    override fun notificationPermissionsRequired() {
                        println("Ganesh notificationPermissionsRequired" + Thread.currentThread().name)
                    }

                    override fun turnOnGpsLocation() {
                        println("Ganesh turnOnGpsLocation" + Thread.currentThread().name)
                    }

                    override fun onSdkInitialized() {
                        println("Ganesh onSdkInitialized" + Thread.currentThread().name)
                    }

                    override fun overSpeedDetected(
                        location: Location?,
                        speedDetectionEdge: String?
                    ) {
                        println("Ganesh overSpeedDetected" + Thread.currentThread().name)
                        Log.d(
                            "Azhar ",
                            "overSpeedDetected() one called with: location = $location, speedDetectionEdge = $speedDetectionEdge"
                        )
                    }

                    override fun magneticFieldDetected(sqrt: Double, emfDetectionEdge: String?) {
                        println("Ganesh magneticFieldDetected" + Thread.currentThread().name)
                    }

                    override fun onHarshDrivingDetected(
                        speed: Float?,
                        harshDrivingEdge: String?,
                        eventType: String
                    ) {
                        Log.d("Azhar", "onHarshDrivingDetected() called with: speed = $speed, harshDrivingEdge = $harshDrivingEdge, eventType = $eventType")
                    }

//                    override fun activityRecognisitionPermissionNotGranted() {
//                    }
                }
            )

            SafetyConnectSDK.startService(this@MainActivity)
        }

        val stopService = findViewById<Button>(R.id.stopService)
        stopService.setOnClickListener {
            Toast.makeText(this, "stop button ", Toast.LENGTH_SHORT).show()
            SafetyConnectSDK.killService(this@MainActivity)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
//            android.Manifest.permission.ACTIVITY_RECOGNITION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
            android.Manifest.permission.REORDER_TASKS,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.POST_NOTIFICATIONS
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Permissions denied: $deniedPermissions", Toast.LENGTH_SHORT).show()
            }
        }
    }
}