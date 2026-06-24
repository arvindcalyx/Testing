package com.test.safetyconnect.foreground.harsh

import SafetyConnectSDK
import SafetyConnectSDK.Companion.notifyAllHarshDrivingListener
import android.location.Location
import com.test.safetyconnect.foreground.util.convertToKmPerHr
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages harsh driving detection (acceleration and braking)
 * Uses speed difference over time window to detect harsh events
 */
class HarshDrivingDetector(
    private val accelerometerWindow: AccelerometerWindow
) {

    private val jerkMap = LinkedHashMap<Long, Float>()
    private val lastHarshDrivingDetected = AtomicLong(0L)

    fun analyze(location: Location?) {
        val timeInterval = SafetyConnectSDK.sensorFilters?.harshDrivingDurationFrequency ?: 3000L
        val stationarySpeedThreshold = SafetyConnectSDK.sensorFilters?.stationarySpeedKmh ?: 2f
        val currentTimeMillis = System.currentTimeMillis()

        val isValidSpeed = location?.hasSpeed() == true &&
                          location.accuracy <= 20f &&
                          location.speed >= 0f
        val currentSpeed = if (isValidSpeed) location.speed.convertToKmPerHr() else 0f

        // Ignore stationary speeds
        if (currentSpeed < stationarySpeedThreshold) {
            jerkMap.clear()
            return
        }

        jerkMap[currentTimeMillis] = currentSpeed
        val firstLogged = jerkMap.keys.firstOrNull() ?: currentTimeMillis

        if (currentTimeMillis - firstLogged >= timeInterval) {
            processHarshDriving(currentSpeed, currentTimeMillis)
        }
    }

    private fun processHarshDriving(currentSpeed: Float, currentTimeMillis: Long) {
        val stationarySpeedThreshold = SafetyConnectSDK.sensorFilters?.stationarySpeedKmh ?: 2f
        val validSpeeds = jerkMap.values.filter { it >= stationarySpeedThreshold }

        if (validSpeeds.isEmpty()) {
            jerkMap.clear()
            return
        }

        val averageSpeed = validSpeeds.sum() / validSpeeds.size
        val harshDrivingDiff = currentSpeed - averageSpeed

        val eventType = if (harshDrivingDiff < 0) "harshBreaking" else "harshAcceleration"
        val threshold = SafetyConnectSDK.sensorFilters?.harshDrivingThresholdInKm ?: 3f
        val callbackFrequency = SafetyConnectSDK.sensorFilters?.harshDrivingCallbackFrequency ?: 90000L
        val accelThreshold = SafetyConnectSDK.sensorFilters?.harshAccelMps2Threshold ?: 3.0f
        val peakAccel = accelerometerWindow.peakMagnitudeWithin(2000L)

        // Require GPS speed-delta AND accelerometer magnitude to agree within
        // the same 2s window, and that the vehicle is actually moving.
        val signalsAgree = kotlin.math.abs(harshDrivingDiff) >= threshold &&
            peakAccel >= accelThreshold &&
            currentSpeed >= stationarySpeedThreshold

        synchronized(lastHarshDrivingDetected) {
            if (signalsAgree) {
                if (currentTimeMillis - lastHarshDrivingDetected.get() >= callbackFrequency) {
                    lastHarshDrivingDetected.set(currentTimeMillis)
                    notifyAllHarshDrivingListener(
                        currentSpeed,
                        SafetyConnectSDK.sensorFilters?.harshDrivingEdge,
                        eventType
                    )
                    jerkMap.clear()
                    Timber.d("Harsh driving detected: $eventType, diff: $harshDrivingDiff km/h")
                }
            }
        }
    }

    fun initialize() {
        lastHarshDrivingDetected.set(System.currentTimeMillis())
    }

    fun clear() {
        jerkMap.clear()
    }
}

