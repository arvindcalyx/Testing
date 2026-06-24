package com.test.safetyconnect.foreground.speed

import SafetyConnectSDK
import android.location.Location
import com.test.safetyconnect.foreground.util.convertToKmPerHr
import com.test.safetyconnect.model.SpeedReading
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages speed readings and filtering logic
 * Uses a rolling window of 5 locations to calculate average speed and validate current location
 */
class SpeedManager {

    companion object {
        private const val TAG = "SpeedManager"
        private const val MAX_LOCATIONS = 5
        private const val MAX_ACCURACY_THRESHOLD = 50f
        private const val MAX_REALISTIC_SPEED = 140f
        private const val MIN_TIME_DELTA_SECONDS = 0.5f
        private const val MEDIAN_SPEED_VARIANCE_THRESHOLD = 1.5f
        private const val MAX_BEARING_CHANGE_DEGREES = 60f
        private const val MIN_DISTANCE_FOR_BEARING_CHECK = 50f
    }

    private val locationHistory = CopyOnWriteArrayList<LocationSnapshot>()
    private val speedReadings = CopyOnWriteArrayList<SpeedReading>()

    data class LocationSnapshot(
        val location: Location,
        val timestamp: Long,
        val speed: Float,
        val latitude: Double,
        val longitude: Double
    )

    fun processLocation(location: Location): SpeedResult {
        Timber.tag(TAG).d(
            "Processing location: lat=%.6f, lon=%.6f, accuracy=%.1fm, hasSpeed=%s",
            location.latitude, location.longitude, location.accuracy, location.hasSpeed()
        )

        // STEP 1: Basic validation
        if (location.accuracy > MAX_ACCURACY_THRESHOLD) {
            Timber.tag(TAG).w("Rejected: Poor accuracy %.1fm", location.accuracy)
            return SpeedResult.Rejected("Poor accuracy: ${location.accuracy}m")
        }

        if (!location.hasSpeed()) {
            Timber.tag(TAG).w("Rejected: No speed data")
            return SpeedResult.Rejected("No speed data")
        }

        val currentSpeed = location.speed.convertToKmPerHr()
        Timber.tag(TAG).d("GPS speed: %.2f km/h", currentSpeed)

        // STEP 2: Handle stationary
        val stationarySpeedThreshold = SafetyConnectSDK.sensorFilters?.stationarySpeedKmh ?: 2f
        if (currentSpeed < stationarySpeedThreshold) {
            Timber.tag(TAG).i("Stationary: %.2f km/h", currentSpeed)
            locationHistory.clear()
            speedReadings.clear()
            addLocationToHistory(location, currentSpeed)
            return SpeedResult.Stationary(location)
        }

        // STEP 3: Validate against location history using median speed
        if (locationHistory.isNotEmpty()) {
            val validationResult = validateLocationJump(location)
            if (!validationResult.isValid) {
                Timber.tag(TAG).w("Rejected: %s", validationResult.reason)
                return SpeedResult.Rejected(validationResult.reason)
            }
        }


        // STEP 4: Location is valid, accept it
        return acceptLocation(location, currentSpeed)
    }

    private fun acceptLocation(location: Location, currentSpeed: Float): SpeedResult {
        addLocationToHistory(location, currentSpeed)

        val reading = SpeedReading(
            speed = currentSpeed,   
            timestamp = System.currentTimeMillis(),
            accuracy = location.accuracy,
            latitude = location.latitude,
            longitude = location.longitude
        )

        speedReadings.add(reading)
        if (speedReadings.size > MAX_LOCATIONS) {
            speedReadings.removeAt(0)
        }

        if (speedReadings.size < MAX_LOCATIONS) {
            Timber.tag(TAG).i("Collecting: %d/%d readings", speedReadings.size, MAX_LOCATIONS)
            return SpeedResult.Collecting(currentSpeed, location)
        }

        val medianSpeed = calculateMedianSpeed(speedReadings)
        Timber.tag(TAG).i("Valid: current=%.2f km/h, median=%.2f km/h", currentSpeed, medianSpeed)
        return SpeedResult.Valid(currentSpeed, medianSpeed, location)
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val reason: String = ""
    )

    private fun validateLocationJump(location: Location): ValidationResult {
        // Use median location from history instead of just last
        val referenceSnapshot = getMedianLocationSnapshot()
            ?: return ValidationResult(false, "No reference location available")

        // Calculate distance and time delta
        val distance = referenceSnapshot.location.distanceTo(location)
        val timeDelta = (location.time - referenceSnapshot.timestamp) / 1000f

        // Check minimum time delta
        if (timeDelta < MIN_TIME_DELTA_SECONDS) {
            return ValidationResult(false, "Time delta too small: %.2fs".format(timeDelta))
        }

        // Calculate speed from distance and time
        val calculatedSpeed = (distance / timeDelta) * 3.6f // Convert m/s to km/h

        Timber.tag(TAG).d(
            "Jump check: distance=%.2fm, timeDelta=%.2fs, calculatedSpeed=%.2f km/h",
            distance, timeDelta, calculatedSpeed
        )

        // Only perform detailed validation if calculated speed seems unrealistic
        if (calculatedSpeed > MAX_REALISTIC_SPEED) {
            Timber.tag(TAG).w("Unrealistic calculated speed detected: %.2f km/h", calculatedSpeed)

            // Calculate median speed from history
            if (locationHistory.size >= 3) {
                val medianSpeed = calculateMedianSpeedFromHistory(location)

                Timber.tag(TAG).d(
                    "Median comparison: medianSpeed=%.2f km/h, calculatedSpeed=%.2f km/h",
                    medianSpeed, calculatedSpeed
                )

                // If median speed is also unrealistic, it's a GPS jump
                if (medianSpeed > MAX_REALISTIC_SPEED) {
                    return ValidationResult(false, "GPS jump detected: median=%.2f km/h".format(medianSpeed))
                }

                // Check bearing change if we have enough distance
                if (distance > MIN_DISTANCE_FOR_BEARING_CHECK) {
                    if (referenceSnapshot.location.hasBearing() && location.hasBearing()) {
                        val bearingDelta = calculateBearingDelta(
                            referenceSnapshot.location.bearing,
                            location.bearing
                        )

                        Timber.tag(TAG).d(
                            "Bearing check: median=%.2f°, current=%.2f°, delta=%.2f°, medianSpeed=%.2f km/h",
                            referenceSnapshot.location.bearing, location.bearing, bearingDelta, medianSpeed
                        )

                        // At higher speeds, allow less bearing change (physics constraint)
                        // At 100 km/h, max ~20°; at 50 km/h, max ~30°; at lower speeds, max 45°
                        val maxAllowedBearing = when {
                            medianSpeed > 80f -> 30f
                            medianSpeed > 50f -> 45f
                            else -> MAX_BEARING_CHANGE_DEGREES
                        }

                        if (bearingDelta > maxAllowedBearing) {
                            return ValidationResult(
                                false,
                                "Unrealistic turn: %.2f° at %.2f km/h (max allowed: %.2f°)".format(
                                    bearingDelta, medianSpeed, maxAllowedBearing
                                )
                            )
                        }
                    }
                }

                // Check if calculated speed deviates significantly from median
                if (medianSpeed > 10f) {
                    val speedRatio = calculatedSpeed / medianSpeed
                    if (speedRatio > MEDIAN_SPEED_VARIANCE_THRESHOLD) {
                        return ValidationResult(
                            false,
                            "Speed jump: calculated=%.2f km/h is %.1fx median=%.2f km/h".format(
                                calculatedSpeed, speedRatio, medianSpeed
                            )
                        )
                    }
                }
            } else {
                // Not enough history, reject unrealistic speeds
                return ValidationResult(
                    false,
                    "Insufficient history for validation (calculatedSpeed=%.2f km/h)".format(calculatedSpeed)
                )
            }
        }

        return ValidationResult(true)
    }

    private fun getMedianLocationSnapshot(): LocationSnapshot? {
        if (locationHistory.isEmpty()) {
            return null
        }

        if (locationHistory.size == 1) {
            return locationHistory.firstOrNull()
        }

        // Get median index
        val medianIndex = locationHistory.size / 2
        return locationHistory.getOrNull(medianIndex)
    }

    private fun calculateBearingDelta(bearing1: Float, bearing2: Float): Float {
        var delta = kotlin.math.abs(bearing1 - bearing2)
        if (delta > 180f) {
            delta = 360f - delta
        }
        return delta
    }

    private fun addLocationToHistory(location: Location, speed: Float) {
        val snapshot = LocationSnapshot(
            location = location,
            timestamp = location.time,
            speed = speed,
            latitude = location.latitude,
            longitude = location.longitude
        )

        locationHistory.add(snapshot)
        if (locationHistory.size > MAX_LOCATIONS) {
            locationHistory.removeAt(0)
        }

        Timber.tag(TAG).d("Location history: %d/%d entries", locationHistory.size, MAX_LOCATIONS)
    }

    private fun calculateMedianSpeedFromHistory(currentLocation: Location): Float {
        if (locationHistory.isEmpty()) return 0f

        val speeds = mutableListOf<Float>()

        // Calculate speed between consecutive locations in history
        for (i in 0 until locationHistory.size - 1) {
            val from = locationHistory[i]
            val to = locationHistory[i + 1]

            val distance = from.location.distanceTo(to.location)
            val timeDelta = (to.timestamp - from.timestamp) / 1000f

            if (timeDelta > MIN_TIME_DELTA_SECONDS) {
                val speed = (distance / timeDelta) * 3.6f // Convert m/s to km/h
                speeds.add(speed)
            }
        }

        // Add speed from last history point to current location
        val lastSnapshot = locationHistory.last()
        val finalDistance = lastSnapshot.location.distanceTo(currentLocation)
        val finalTimeDelta = (currentLocation.time - lastSnapshot.timestamp) / 1000f

        if (finalTimeDelta > MIN_TIME_DELTA_SECONDS) {
            val finalSpeed = (finalDistance / finalTimeDelta) * 3.6f
            speeds.add(finalSpeed)
        }

        if (speeds.isEmpty()) return 0f

        // Calculate median
        val sortedSpeeds = speeds.sorted()
        val medianSpeed = if (sortedSpeeds.size % 2 == 0) {
            (sortedSpeeds[sortedSpeeds.size / 2 - 1] + sortedSpeeds[sortedSpeeds.size / 2]) / 2f
        } else {
            sortedSpeeds[sortedSpeeds.size / 2]
        }

        Timber.tag(TAG).d(
            "Median from history: segments=%d, median=%.2f km/h, speeds=%s",
            speeds.size, medianSpeed, speeds.joinToString(",") { "%.1f".format(it) }
        )

        return medianSpeed
    }


    private fun calculateMedianSpeed(readings: List<SpeedReading>): Float {
        if (readings.isEmpty()) return 0f
        val speeds = readings.map { it.speed }.sorted()
        return if (speeds.size % 2 == 0) {
            (speeds[speeds.size / 2 - 1] + speeds[speeds.size / 2]) / 2f
        } else {
            speeds[speeds.size / 2]
        }
    }

    fun clear() {
        Timber.tag(TAG).d("Clearing speed manager")
        locationHistory.clear()
        speedReadings.clear()
    }
}

sealed class SpeedResult {
    data class Stationary(val location: Location) : SpeedResult()
    data class Collecting(val currentSpeed: Float, val location: Location) : SpeedResult()
    data class Valid(val currentSpeed: Float, val medianSpeed: Float, val location: Location) : SpeedResult()
    data class Rejected(val reason: String) : SpeedResult()
}

