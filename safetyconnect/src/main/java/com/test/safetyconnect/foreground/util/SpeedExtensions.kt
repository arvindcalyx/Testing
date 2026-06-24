package com.test.safetyconnect.foreground.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Utility extensions for speed conversion
 */

/**
 * Converts speed from m/s to km/h and rounds to 2 decimal places
 * @return speed in kilometers per hour, rounded to 2 decimal places
 */
fun Float.convertToKmPerHr(): Float {
    val speedKmh = this.times(18).div(5)
    return BigDecimal(speedKmh.toDouble())
        .setScale(2, RoundingMode.HALF_UP)
        .toFloat()
}

/**
 * Converts speed from km/h to m/s
 * @return speed in meters per second
 */
fun Float.convertToMetersPerSec(): Float {
    return this.times(5).div(18)
}

