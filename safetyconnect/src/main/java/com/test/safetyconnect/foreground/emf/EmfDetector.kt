package com.test.safetyconnect.foreground.emf

import SafetyConnectSDK
import SafetyConnectSDK.Companion.activity
import SafetyConnectSDK.Companion.notifyAllMagneticFieldDetectedListener
import android.hardware.SensorEvent
import android.location.Location
import android.media.MediaPlayer
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Manages Electromagnetic Field (EMF) detection
 * Monitors magnetic field sensor and triggers alerts
 */
class EmfDetector(private val mediaPlayer: MediaPlayer?) {

    private val magneticFieldWindow = CopyOnWriteArrayList<Double>()
    private val windowSize = 50 // Increased for better baseline calculation
    private val lastEmfDetected = AtomicLong(0L)
    private val lastEmfBeepPlayed = AtomicLong(0L)
    private var lastEmfLocation: Location? = null
    private var dcBaseline: Double = 48.0 // Earth's magnetic field baseline
    private var isBaselineCalibrated = false

    // For variance-based AC detection
    private val recentReadings = CopyOnWriteArrayList<Double>()
    private val varianceWindowSize = 20
    private var logCounter = 0

    // Phone's own EMF baseline offset (typically ~1 µT when on lap/stationary)
    private val phoneBaselineOffset = 0.3 // µT

    // Dataset for best value calculation before firing AC event
    private val acComponentDataset = CopyOnWriteArrayList<Double>()
    private val acDatasetSize = 5 // Collect 5 readings before firing event

    fun processSensorEvent(
        sensorEvent: SensorEvent,
        currentLocation: Location?,
        disableEmfSpeedInKmHr: Float?
    ): String? {
        if (SafetyConnectSDK.sensorFilters?.isEMFDetectionEnabled != true) {
            return null
        }

        val minSpeed = SafetyConnectSDK.sensorFilters?.disableEmfMinimumThresholdInKm ?: 0f
        if (minSpeed < (disableEmfSpeedInKmHr ?: 0f)) {
            return null
        }

        return if (SafetyConnectSDK.sensorFilters?.isEMFACDetectionEnabled == true) {
            processAcEmf(sensorEvent, currentLocation)
        } else {
            processDcEmf(sensorEvent, currentLocation)
        }
    }

    private fun processAcEmf(sensorEvent: SensorEvent, currentLocation: Location?): String {
        val currentTimeMillis = System.currentTimeMillis()
        val fArr = sensorEvent.values
        val totalField = sqrt((fArr[0] * fArr[0] + fArr[1] * fArr[1] + fArr[2] * fArr[2]).toDouble())

        // Build sliding window for baseline calculation
        magneticFieldWindow.add(totalField)
        if (magneticFieldWindow.size > windowSize) {
            magneticFieldWindow.removeAt(0)
        }

        // Calculate DC baseline (Earth's magnetic field) using median to avoid outlier influence
        if (magneticFieldWindow.size >= windowSize && !isBaselineCalibrated) {
            dcBaseline = calculateMedian(magneticFieldWindow)
            isBaselineCalibrated = true
        } else if (magneticFieldWindow.size >= windowSize) {
            // Continuously update baseline slowly to account for location changes
            val currentMedian = calculateMedian(magneticFieldWindow)
            dcBaseline = dcBaseline * 0.95 + currentMedian * 0.05 // Smooth update
        }

        // Extract AC component (deviation from Earth's baseline)
        val rawAcComponent = (totalField - dcBaseline).absoluteValue

        // Subtract phone's own baseline offset (1 µT) and treat values below it as 0
        val acComponent = (rawAcComponent - phoneBaselineOffset).coerceAtLeast(0.0)

        // Track recent readings for variance-based AC detection
        recentReadings.add(totalField)
        if (recentReadings.size > varianceWindowSize) {
            recentReadings.removeAt(0)
        }

        // Add AC component to dataset for best value calculation
        acComponentDataset.add(acComponent)
        if (acComponentDataset.size > acDatasetSize) {
            acComponentDataset.removeAt(0)
        }

        // Calculate best value from dataset (using median to filter outliers)
        val bestAcValue = if (acComponentDataset.size >= acDatasetSize) {
            calculateMedian(acComponentDataset)
        } else {
            acComponent // Use current value if dataset not full yet
        }

        // Enhanced AC detection using both absolute value and variance
        val acSoundThreshold = SafetyConnectSDK.sensorFilters?.maxACEmfThreshold ?: 3.0f

        // Use variance-based detection for actual AC EMF (from devices/power lines)
        if ((bestAcValue > acSoundThreshold) && shouldPlayEmfSound(currentTimeMillis, currentLocation)) {
            lastEmfBeepPlayed.set(currentTimeMillis)
            lastEmfLocation = currentLocation
            mediaPlayer?.start()
        }
         val AverageAcValue = if (bestAcValue < (SafetyConnectSDK.sensorFilters?.maxAcEmfValue?.toDouble() ?: 5000.0)) bestAcValue else SafetyConnectSDK.sensorFilters?.maxAcEmfValue?.toDouble()
        // Trigger callback only if dataset is full and best value exceeds threshold
        val threshold = SafetyConnectSDK.sensorFilters?.maxACEmfThreshold ?: 70.0f
        if (acComponentDataset.size >= acDatasetSize) {
            AverageAcValue?.toFloat()?.let {
                if (it >= threshold) {
                    triggerEmfCallback(it.toDouble(), currentTimeMillis)
                    Log.d("Azhar", "processAcEmf() emfValue = $AverageAcValue")
                }
            }
        }

        return "EMF (AC): %.2f µT".format(AverageAcValue)
    }

    /**
     * Calculate variance to detect actual AC fluctuations
     * High variance = actual AC EMF present (devices, power lines)
     * Low variance = static field (Earth's magnetic field)
     */
    private fun calculateVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val squaredDifferences = values.map { (it - mean) * (it - mean) }
        return squaredDifferences.average()
    }

    private fun calculateMedian(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun processDcEmf(sensorEvent: SensorEvent, currentLocation: Location?): String {
        val currentTimeMillis = System.currentTimeMillis()
        val fArr = sensorEvent.values
        val totalField = sqrt((fArr[0] * fArr[0] + fArr[1] * fArr[1] + fArr[2] * fArr[2]).toDouble())

        if (totalField > 60.0 && shouldPlayEmfSound(currentTimeMillis, currentLocation)) {
            lastEmfBeepPlayed.set(currentTimeMillis)
            lastEmfLocation = currentLocation
            mediaPlayer?.start()
        }

        val roundedValue = (totalField * 100.0).roundToInt() / 100.0
        val threshold = SafetyConnectSDK.sensorFilters?.maxEmfThreshold ?: 0f

        if (threshold <= roundedValue.toFloat()) {
            triggerEmfCallback(roundedValue, currentTimeMillis)
        }

        return "EMF: %.2f µT".format(roundedValue)
    }

    private fun shouldPlayEmfSound(currentTimeMillis: Long, currentLocation: Location?): Boolean {
        val isBeepAllowed = AtomicBoolean(false)

        synchronized(isBeepAllowed) {
            val beepInterval = SafetyConnectSDK.sensorFilters?.configuredEmfBeepInterval ?: 3000L
            if ((currentTimeMillis - lastEmfBeepPlayed.get()).absoluteValue >= beepInterval) {
                isBeepAllowed.set(true)
            }

            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation?.latitude ?: 0.0,
                currentLocation?.longitude ?: 0.0,
                lastEmfLocation?.latitude ?: 0.0,
                lastEmfLocation?.longitude ?: 0.0,
                results
            )
            val distance = results.first()
            val minDistance = SafetyConnectSDK.sensorFilters?.configuredEmfBeepDistance ?: 10.0

            return if (currentLocation == null || lastEmfLocation == null) {
                isBeepAllowed.get()
            } else {
                isBeepAllowed.get() && (distance.absoluteValue > minDistance)
            }
        }
    }

    private fun triggerEmfCallback(value: Double, currentTimeMillis: Long) {
        val callbackFrequency = SafetyConnectSDK.sensorFilters?.emfCallBackFrequency ?: 30000L

        if (lastEmfDetected.get() == 0L || currentTimeMillis - lastEmfDetected.get() >= callbackFrequency) {
            lastEmfDetected.set(currentTimeMillis)
            activity?.get()?.runOnUiThread {
                notifyAllMagneticFieldDetectedListener(
                    value,
                    SafetyConnectSDK.sensorFilters?.emfDetectionEdge
                )
                Log.d("Azhar", "triggerEmfCallback() called")
            }
        }
    }

    fun initialize() {
        lastEmfDetected.set(System.currentTimeMillis())
        isBaselineCalibrated = false
        dcBaseline = 0.0
        logCounter = 0
        acComponentDataset.clear()
    }

    fun clear() {
        magneticFieldWindow.clear()
        recentReadings.clear()
        acComponentDataset.clear()
        lastEmfLocation = null
        isBaselineCalibrated = false
        dcBaseline = 0.0
        logCounter = 0
    }
}

