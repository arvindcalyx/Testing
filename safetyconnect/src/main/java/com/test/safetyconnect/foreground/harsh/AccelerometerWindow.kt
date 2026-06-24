package com.test.safetyconnect.foreground.harsh

import kotlin.math.sqrt

/**
 * Bounded ring buffer of recent linear-acceleration samples.
 *
 * Fed by the foreground service's TYPE_LINEAR_ACCELERATION sensor callback
 * (single producer). HarshDrivingDetector queries [peakMagnitudeWithin] to
 * confirm a GPS-derived harsh event against the accelerometer signal.
 *
 * At SENSOR_DELAY_GAME (~50 Hz) the default capacity of 100 samples covers
 * roughly the last 2 seconds.
 */
class AccelerometerWindow(private val maxSamples: Int = 100) {

    private data class Sample(
        val timestamp: Long,
        val ax: Float,
        val ay: Float,
        val az: Float
    )

    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun add(ax: Float, ay: Float, az: Float, timestamp: Long = System.currentTimeMillis()) {
        samples.addLast(Sample(timestamp, ax, ay, az))
        while (samples.size > maxSamples) {
            samples.removeFirst()
        }
    }

    /**
     * @return the maximum sqrt(ax^2 + ay^2 + az^2) over samples recorded
     *         within the last [windowMs] milliseconds, or 0f if none.
     */
    @Synchronized
    fun peakMagnitudeWithin(windowMs: Long): Float {
        if (samples.isEmpty()) return 0f
        val cutoff = System.currentTimeMillis() - windowMs
        var peak = 0f
        for (sample in samples) {
            if (sample.timestamp >= cutoff) {
                val magnitude = sqrt(
                    sample.ax * sample.ax +
                        sample.ay * sample.ay +
                        sample.az * sample.az
                )
                if (magnitude > peak) peak = magnitude
            }
        }
        return peak
    }

    @Synchronized
    fun clear() {
        samples.clear()
    }
}
